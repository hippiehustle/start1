import { describe, it, expect } from "vitest";
import { ScannerEngine } from "../src/scanner/engine.js";
import type { Env } from "../src/config/env.js";

class MockRedis {
  store = new Map<string, string>();
  async get(key: string) {
    return this.store.get(key) ?? null;
  }
  async set(key: string, value: unknown) {
    this.store.set(key, String(value));
  }
  async incr(key: string) {
    const value = Number(this.store.get(key) ?? 0) + 1;
    this.store.set(key, String(value));
    return value;
  }
}

const env: Env = {
  NODE_ENV: "test",
  PORT: 0,
  API_KEY: "test-key-123",
  UPSTASH_REDIS_REST_URL: "https://example.com",
  UPSTASH_REDIS_REST_TOKEN: "token",
  FIREBASE_SERVICE_ACCOUNT_JSON: "{\"type\":\"service_account\",\"project_id\":\"dev\"}",
  COINBASE_WS_URL: "wss://example.com",
  COINBASE_REST_BASE: "https://example.com",
  LIQ_SPREAD_BPS_MAX: 50,
  LIQ_DEPTH_USD_MIN: 50000,
  TRACK_TOP_N: 80
};

const log = { info: () => {}, warn: () => {}, error: () => {} } as any;

function makeCandles(count: number, startPrice: number) {
  const candles: number[][] = [];
  for (let i = 0; i < count; i += 1) {
    const price = startPrice + i;
    candles.push([i, price - 1, price + 1, price - 0.5, price, 1000]);
  }
  return candles;
}

describe("ScannerEngine", () => {
  it("applies regime veto", async () => {
    const redis = new MockRedis();
    await redis.set("m:BTC-USD:change24hPct", -4);
    await redis.set("m:BTC-USD:trendState", "FALLING");

    const engine = new ScannerEngine(redis as any, env, log);
    const result = await engine.scan(["BTC-USD"]);

    expect(result.output).toContain("market regime veto");
    expect(result.state).toBe("NO_TRADE");
  });

  it("falls back to wider window to find BUY", async () => {
    const redis = new MockRedis();
    const productId = "SOL-USD";
    await redis.set(`m:${productId}:lastPrice`, 82);
    await redis.set(`m:${productId}:change24hPct`, 6);
    await redis.set(`m:${productId}:vol24hUsd`, 2000000);
    await redis.set(`m:${productId}:rollVol5mUsd`, 50000);
    await redis.set(`m:${productId}:rollVol15mUsd`, 60000);
    await redis.set(`m:${productId}:rollVol1hUsd`, 80000);
    await redis.set(`m:${productId}:spreadBps`, 10);
    await redis.set(`m:${productId}:depthUsdTop`, 100000);
    await redis.set(`c:${productId}:1d`, JSON.stringify(makeCandles(30, 80)));
    await redis.set(`c:${productId}:1h`, JSON.stringify(makeCandles(50, 80)));

    const engine = new ScannerEngine(redis as any, env, log);
    const result = await engine.scan([productId]);

    expect(result.state).toBe("BUY");
    expect(result.output).toContain("Time Window (days): 14");
  });

  it("returns setup forming when readiness is midrange", async () => {
    const redis = new MockRedis();
    const productId = "AVAX-USD";
    await redis.set(`m:${productId}:lastPrice`, 50);
    await redis.set(`m:${productId}:change24hPct`, 2);
    await redis.set(`m:${productId}:vol24hUsd`, 400000);
    await redis.set(`m:${productId}:rollVol5mUsd`, 10000);
    await redis.set(`m:${productId}:rollVol15mUsd`, 12000);
    await redis.set(`m:${productId}:rollVol1hUsd`, 14000);
    await redis.set(`m:${productId}:spreadBps`, 20);
    await redis.set(`m:${productId}:depthUsdTop`, 60000);
    await redis.set(`c:${productId}:1d`, JSON.stringify(makeCandles(30, 40)));
    await redis.set(`c:${productId}:1h`, JSON.stringify(makeCandles(50, 40)));

    const engine = new ScannerEngine(redis as any, env, log);
    const result = await engine.scan([productId]);

    expect(result.state).toBe("SETUP_FORMING");
    expect(result.output).toContain("SETUP FORMING — WAIT");
  });

  it("rejects late entries near breakout", async () => {
    const redis = new MockRedis();
    const productId = "SUI-USD";
    await redis.set(`m:${productId}:lastPrice`, 120);
    await redis.set(`m:${productId}:change24hPct`, 15);
    await redis.set(`m:${productId}:vol24hUsd`, 2500000);
    await redis.set(`m:${productId}:rollVol5mUsd`, 60000);
    await redis.set(`m:${productId}:rollVol15mUsd`, 70000);
    await redis.set(`m:${productId}:rollVol1hUsd`, 90000);
    await redis.set(`m:${productId}:spreadBps`, 12);
    await redis.set(`m:${productId}:depthUsdTop`, 120000);
    await redis.set(`c:${productId}:1d`, JSON.stringify(makeCandles(30, 80)));
    await redis.set(`c:${productId}:1h`, JSON.stringify(makeCandles(50, 80)));

    const engine = new ScannerEngine(redis as any, env, log);
    const result = await engine.scan([productId]);

    expect(result.state).not.toBe("BUY");
  });

  it("filters out illiquid products", async () => {
    const redis = new MockRedis();
    const productId = "ILLQ-USD";
    await redis.set(`m:${productId}:lastPrice`, 5);
    await redis.set(`m:${productId}:change24hPct`, 4);
    await redis.set(`m:${productId}:vol24hUsd`, 50000);
    await redis.set(`m:${productId}:rollVol5mUsd`, 5000);
    await redis.set(`m:${productId}:rollVol15mUsd`, 5000);
    await redis.set(`m:${productId}:rollVol1hUsd`, 5000);
    await redis.set(`m:${productId}:spreadBps`, 120);
    await redis.set(`m:${productId}:depthUsdTop`, 1000);
    await redis.set(`c:${productId}:1d`, JSON.stringify(makeCandles(30, 5)));
    await redis.set(`c:${productId}:1h`, JSON.stringify(makeCandles(50, 5)));

    const engine = new ScannerEngine(redis as any, env, log);
    const result = await engine.scan([productId]);

    expect(result.state).toBe("NO_TRADE");
  });

  it("applies trade frequency guardrail and confidence decay", async () => {
    const redis = new MockRedis();
    const productId = "ARB-USD";
    await redis.set("scan:buys:last7d", 2);
    await redis.set("scan:noTradeStreak", 3);
    await redis.set(`m:${productId}:lastPrice`, 100);
    await redis.set(`m:${productId}:change24hPct`, 4);
    await redis.set(`m:${productId}:vol24hUsd`, 2000000);
    await redis.set(`m:${productId}:rollVol5mUsd`, 60000);
    await redis.set(`m:${productId}:rollVol15mUsd`, 70000);
    await redis.set(`m:${productId}:rollVol1hUsd`, 90000);
    await redis.set(`m:${productId}:spreadBps`, 10);
    await redis.set(`m:${productId}:depthUsdTop`, 100000);
    await redis.set(`c:${productId}:1d`, JSON.stringify(makeCandles(30, 90)));
    await redis.set(`c:${productId}:1h`, JSON.stringify(makeCandles(50, 90)));

    const engine = new ScannerEngine(redis as any, env, log);
    const result = await engine.scan([productId]);

    expect(result.state).not.toBe("BUY");
  });
});
