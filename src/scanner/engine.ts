import type { Logger } from "pino";
import type { RedisClient } from "../store/redis.js";
import { mean, clamp } from "../utils/math.js";
import { normalizeCandles } from "../utils/candles.js";
import { findSupportResistance, isNear } from "../utils/levels.js";
import type { ScanResult, RegimeState, ProductMetrics } from "./types.js";
import type { Env } from "../config/env.js";

const WINDOWS = [7, 14, 21, 28, 30];

export class ScannerEngine {
  constructor(
    private readonly redis: RedisClient,
    private readonly env: Env,
    private readonly log: Logger
  ) {}

  async scan(productIds: string[]): Promise<ScanResult> {
    const btcChange = Number((await this.redis.get("m:BTC-USD:change24hPct")) ?? 0);
    const btcTrend = (await this.redis.get("m:BTC-USD:trendState")) as RegimeState | null;
    const btcRegime = btcTrend ?? this.classifyRegime(btcChange);

    const veto = await this.checkVeto(btcRegime, btcChange, productIds);
    if (veto) {
      const result: ScanResult = {
        state: "NO_TRADE",
        output: "NO TRADE — EV is negative (market regime veto)",
        timestamp: Date.now(),
        reasons: ["market regime veto"]
      };
      return result;
    }

    const products = await this.loadMetrics(productIds);
    const guardrail = await this.getBuyGuardrail();

    const candidates = products
      .map((metrics) => this.scoreProduct(metrics, btcRegime, guardrail))
      .filter((c) => c !== null);

    const ranked = candidates.sort((a, b) => b.score - a.score).slice(0, 3);

    for (const windowDays of WINDOWS) {
      const found = ranked.find((c) => c.windowDays <= windowDays && c.state === "BUY");
      if (found) {
        return this.buildBuyResult(found);
      }
    }

    const best = ranked[0];
    if (best && best.score >= 50) {
      return this.buildSetupResult(best);
    }

    return {
      state: "NO_TRADE",
      output: "NO TRADE — EV is negative across all windows.",
      timestamp: Date.now()
    };
  }

  private classifyRegime(btcChange: number): RegimeState {
    if (btcChange < -2) return "FALLING";
    if (btcChange > 1) return "TRENDING";
    return "BASING";
  }

  private async checkVeto(regime: RegimeState, btcChange: number, productIds: string[]) {
    if (regime === "FALLING" && btcChange < -3) {
      return true;
    }
    const aggregateVolume = await this.aggregateVolume(productIds);
    const baseline = Number((await this.redis.get("market:volume:baseline")) ?? 0);
    await this.redis.set("market:volume:baseline", aggregateVolume);
    const medianChange = await this.medianChange(productIds);
    if (baseline > 0 && aggregateVolume < baseline * 0.9 && medianChange >= 0) {
      return true;
    }
    return false;
  }

  private async aggregateVolume(productIds: string[]) {
    const values = await Promise.all(
      productIds.map(async (id) => Number((await this.redis.get(`m:${id}:vol24hUsd`)) ?? 0))
    );
    return values.reduce((a, b) => a + b, 0);
  }

  private async medianChange(productIds: string[]) {
    const values = await Promise.all(
      productIds.map(async (id) => Number((await this.redis.get(`m:${id}:change24hPct`)) ?? 0))
    );
    const sorted = values.sort((a, b) => a - b);
    const mid = Math.floor(sorted.length / 2);
    return sorted.length % 2 === 0
      ? (sorted[mid - 1] + sorted[mid]) / 2
      : sorted[mid];
  }

  private async loadMetrics(productIds: string[]): Promise<ProductMetrics[]> {
    const metrics: ProductMetrics[] = [];
    for (const id of productIds) {
      const [
        lastPrice,
        change24hPct,
        vol24hUsd,
        rollVol5mUsd,
        rollVol15mUsd,
        rollVol1hUsd,
        spreadBps,
        depthUsdTop,
        candles1h,
        candles1d
      ] = await Promise.all([
        this.redis.get(`m:${id}:lastPrice`),
        this.redis.get(`m:${id}:change24hPct`),
        this.redis.get(`m:${id}:vol24hUsd`),
        this.redis.get(`m:${id}:rollVol5mUsd`),
        this.redis.get(`m:${id}:rollVol15mUsd`),
        this.redis.get(`m:${id}:rollVol1hUsd`),
        this.redis.get(`m:${id}:spreadBps`),
        this.redis.get(`m:${id}:depthUsdTop`),
        this.redis.get(`c:${id}:1h`),
        this.redis.get(`c:${id}:1d`)
      ]);

      if (!lastPrice) continue;
      metrics.push({
        productId: id,
        lastPrice: Number(lastPrice),
        change24hPct: Number(change24hPct ?? 0),
        vol24hUsd: Number(vol24hUsd ?? 0),
        rollVol5mUsd: Number(rollVol5mUsd ?? 0),
        rollVol15mUsd: Number(rollVol15mUsd ?? 0),
        rollVol1hUsd: Number(rollVol1hUsd ?? 0),
        spreadBps: Number(spreadBps ?? 0),
        depthUsdTop: Number(depthUsdTop ?? 0),
        candles1h: candles1h ? (JSON.parse(String(candles1h)) as number[][]) : [],
        candles1d: candles1d ? (JSON.parse(String(candles1d)) as number[][]) : []
      });
    }
    return metrics;
  }

  private scoreProduct(metrics: ProductMetrics, regime: RegimeState, guardrail: number) {
    const candles1d = normalizeCandles(metrics.candles1d);
    if (candles1d.length < 20) return null;

    const levels = findSupportResistance(candles1d);
    const nearSupport = isNear(metrics.lastPrice, levels.support, 5);
    const nearBreakout = isNear(metrics.lastPrice, levels.resistance, 3);

    const volumeSeries = candles1d.map((c) => c.volume);
    const avgVolume = mean(volumeSeries.slice(-7));
    const volConfirmed = metrics.vol24hUsd > avgVolume * metrics.lastPrice;
    const volumeUp = metrics.vol24hUsd > avgVolume * metrics.lastPrice * 0.7;
    const volumeAccel =
      metrics.rollVol5mUsd > metrics.rollVol15mUsd * 0.4 &&
      metrics.rollVol15mUsd > metrics.rollVol1hUsd * 0.2;

    const liquidityPass =
      metrics.spreadBps > 0 &&
      metrics.spreadBps <= this.env.LIQ_SPREAD_BPS_MAX &&
      metrics.depthUsdTop >= this.env.LIQ_DEPTH_USD_MIN;

    if (!liquidityPass) return null;

    const entryQuality = nearSupport || nearBreakout;
    const lateEntry = nearBreakout && metrics.change24hPct > 12;

    const regimeScore = regime === "TRENDING" ? 20 : regime === "BASING" ? 10 : 0;
    const liquidityScore = liquidityPass ? 15 : 0;
    const structureScore = entryQuality ? 20 : 0;
    const volumeRiseScore = volumeUp ? 15 : 0;
    const volumeConfirmScore = volConfirmed ? 25 : 0;
    const entryScore = entryQuality && !lateEntry ? 15 : 0;
    const readinessScore = clamp(
      regimeScore +
        liquidityScore +
        structureScore +
        volumeRiseScore +
        volumeConfirmScore +
        entryScore -
        guardrail,
      0,
      100
    );

    const windowDays = metrics.change24hPct > 10 ? 7 : metrics.change24hPct > 5 ? 14 : 21;
    const state =
      readinessScore >= 80 && !lateEntry && volumeAccel && volConfirmed
        ? "BUY"
        : readinessScore >= 50
          ? "SETUP_FORMING"
          : "NO_TRADE";

    return {
      metrics,
      score: readinessScore,
      state,
      windowDays,
      nearSupport,
      nearBreakout,
      lateEntry,
      volumeAccel,
      volumeConfirm: volConfirmed
    };
  }

  private async getBuyGuardrail() {
    const buyCount = Number((await this.redis.get("scan:buys:last7d")) ?? 0);
    const noTradeStreak = Number((await this.redis.get("scan:noTradeStreak")) ?? 0);
    if (buyCount >= 2) return 10;
    if (noTradeStreak >= 3) return 5;
    return 0;
  }

  private buildBuyResult(candidate: {
    metrics: ProductMetrics;
    score: number;
    windowDays: number;
    nearSupport: boolean;
  }): ScanResult {
    const { metrics, score, windowDays, nearSupport } = candidate;
    const levels = findSupportResistance(normalizeCandles(metrics.candles1d));
    const buyZone = nearSupport
      ? `${levels.support.toFixed(4)} - ${(levels.support * 1.03).toFixed(4)}`
      : `${metrics.lastPrice.toFixed(4)} - ${(metrics.lastPrice * 1.02).toFixed(4)}`;
    const tp1 = (metrics.lastPrice * 1.1).toFixed(4);
    const tp2 = (metrics.lastPrice * 1.2).toFixed(4);
    const stop = (levels.support * 0.97).toFixed(4);
    const output = [
      `Coin & Ticker: ${metrics.productId}`,
      `Current Price: ${metrics.lastPrice.toFixed(4)}`,
      `Why This Trade Has Positive EV: Liquidity is strong, structure is clean, and volume confirms momentum.`,
      `Exact Buy Zone: ${buyZone}`,
      `Exact Sell Targets: TP1 ${tp1} (sell 25-50%), TP2 ${tp2} (trail/ratchet remainder)`,
      `Exact Stop / Exit Level if Wrong: ${stop}`,
      `Best-Case ROI % and Realistic ROI %: ${windowDays <= 7 ? "20%" : "35%"} / ${windowDays <= 7 ? "12%" : "25%"}`,
      `Time Window (days): ${windowDays}`,
      `Final Action: BUY`,
      `After you close this trade, record entry/exit and whether you followed the rules.`
    ].join("\n");

    return {
      state: "BUY",
      output,
      productId: metrics.productId,
      readinessScore: score,
      timestamp: Date.now()
    };
  }

  private buildSetupResult(candidate: {
    metrics: ProductMetrics;
    score: number;
    nearSupport: boolean;
    nearBreakout: boolean;
    lateEntry: boolean;
    volumeConfirm: boolean;
  }): ScanResult {
    const { metrics, score, nearSupport, nearBreakout, lateEntry, volumeConfirm } = candidate;
    const levels = findSupportResistance(normalizeCandles(metrics.candles1d));
    const missing: string[] = [];
    if (!volumeConfirm) missing.push("Volume confirmation above 7-day average");
    if (!nearSupport && !nearBreakout) missing.push("Clear pullback or breakout level");
    if (lateEntry) missing.push("Avoid late entry; wait for reset");
    const confirm = nearBreakout
      ? levels.resistance.toFixed(4)
      : (levels.support * 1.02).toFixed(4);
    const output = [
      "SETUP FORMING — WAIT",
      `EV Readiness Score: ${Math.round(score)}`,
      `What's missing: ${missing.slice(0, 3).join("; ") || "Nothing material"}`,
      `Confirm or invalidate level: ${confirm}`
    ].join("\n");

    return {
      state: "SETUP_FORMING",
      output,
      productId: metrics.productId,
      readinessScore: Math.round(score),
      timestamp: Date.now()
    };
  }
}
