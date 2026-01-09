import type { Logger } from "pino";
import type { RedisClient } from "../store/redis.js";
import { CoinbaseWsClient } from "../coinbase/ws.js";
import type { Env } from "../config/env.js";
import { RollingVolume } from "./rollingVolume.js";

const DEPTH_LEVELS = 10;

type OrderSide = "bids" | "asks";

type OrderBook = {
  bids: Map<number, number>;
  asks: Map<number, number>;
};

export class Collector {
  private ws: CoinbaseWsClient;
  private orderBooks: Map<string, OrderBook> = new Map();
  private roll5m: Map<string, RollingVolume> = new Map();
  private roll15m: Map<string, RollingVolume> = new Map();
  private roll1h: Map<string, RollingVolume> = new Map();
  private trackedProducts: string[] = [];
  private wsConnected = false;

  constructor(
    private readonly env: Env,
    private readonly redis: RedisClient,
    private readonly log: Logger
  ) {
    this.ws = new CoinbaseWsClient(env, log, {
      onMessage: (msg) => this.handleMessage(msg),
      onOpen: () => (this.wsConnected = true),
      onClose: () => (this.wsConnected = false)
    });
  }

  start(productIds: string[]) {
    this.trackedProducts = productIds;
    this.ws.setProducts(productIds);
    this.ws.connect();
  }

  isWsConnected() {
    return this.wsConnected;
  }

  getTrackedCount() {
    return this.trackedProducts.length;
  }

  updateProducts(productIds: string[]) {
    this.trackedProducts = productIds;
    this.ws.setProducts(productIds);
  }

  private async handleMessage(message: Record<string, unknown> & { type: string }) {
    switch (message.type) {
      case "ticker":
        await this.handleTicker(message);
        break;
      case "match":
        await this.handleMatch(message);
        break;
      case "snapshot":
      case "l2update":
        await this.handleLevel2(message);
        break;
      default:
        break;
    }
  }

  private async handleTicker(message: Record<string, unknown>) {
    const productId = String(message.product_id ?? "");
    if (!productId) return;
    const price = Number(message.price ?? 0);
    const open24h = Number(message.open_24h ?? 0);
    const volume24h = Number(message.volume_24h ?? 0);
    const change24hPct = open24h ? ((price - open24h) / open24h) * 100 : 0;

    await this.redis.set(`m:${productId}:lastPrice`, price);
    await this.redis.set(`m:${productId}:change24hPct`, change24hPct);
    await this.redis.set(`m:${productId}:vol24hUsd`, volume24h * price);
    await this.redis.set(`m:${productId}:lastUpdateMs`, Date.now());

    if (productId === "BTC-USD") {
      const trendState = change24hPct < -3 ? "FALLING" : change24hPct > 1 ? "TRENDING" : "BASING";
      await this.redis.set("m:BTC-USD:trendState", trendState);
    }
  }

  private getRolling(productId: string, windowMs: number, store: Map<string, RollingVolume>) {
    const existing = store.get(productId);
    if (existing) return existing;
    const created = new RollingVolume(windowMs);
    store.set(productId, created);
    return created;
  }

  private async handleMatch(message: Record<string, unknown>) {
    const productId = String(message.product_id ?? "");
    if (!productId) return;
    const size = Number(message.size ?? 0);
    const price = Number(message.price ?? 0);
    const ts = Date.now();
    const usd = size * price;

    this.getRolling(productId, 5 * 60 * 1000, this.roll5m).add(usd, ts);
    this.getRolling(productId, 15 * 60 * 1000, this.roll15m).add(usd, ts);
    this.getRolling(productId, 60 * 60 * 1000, this.roll1h).add(usd, ts);

    await this.redis.set(`m:${productId}:rollVol5mUsd`, this.roll5m.get(productId)?.sum(ts) ?? 0);
    await this.redis.set(`m:${productId}:rollVol15mUsd`, this.roll15m.get(productId)?.sum(ts) ?? 0);
    await this.redis.set(`m:${productId}:rollVol1hUsd`, this.roll1h.get(productId)?.sum(ts) ?? 0);
  }

  private getBook(productId: string) {
    const existing = this.orderBooks.get(productId);
    if (existing) return existing;
    const created: OrderBook = { bids: new Map(), asks: new Map() };
    this.orderBooks.set(productId, created);
    return created;
  }

  private async handleLevel2(message: Record<string, unknown> & { type: string }) {
    const productId = String(message.product_id ?? "");
    if (!productId) return;
    const book = this.getBook(productId);

    if (message.type === "snapshot") {
      book.bids.clear();
      book.asks.clear();
      const bids = message.bids as string[][] | undefined;
      const asks = message.asks as string[][] | undefined;
      bids?.forEach(([price, size]) => book.bids.set(Number(price), Number(size)));
      asks?.forEach(([price, size]) => book.asks.set(Number(price), Number(size)));
    } else if (message.type === "l2update") {
      const changes = message.changes as string[][] | undefined;
      changes?.forEach(([side, price, size]) => {
        const map = side === "buy" ? book.bids : book.asks;
        const p = Number(price);
        const s = Number(size);
        if (s === 0) {
          map.delete(p);
        } else {
          map.set(p, s);
        }
      });
    }

    const bidsSorted = Array.from(book.bids.entries())
      .sort((a, b) => b[0] - a[0])
      .slice(0, DEPTH_LEVELS);
    const asksSorted = Array.from(book.asks.entries())
      .sort((a, b) => a[0] - b[0])
      .slice(0, DEPTH_LEVELS);

    if (bidsSorted.length === 0 || asksSorted.length === 0) return;
    const bestBid = bidsSorted[0][0];
    const bestAsk = asksSorted[0][0];
    const mid = (bestBid + bestAsk) / 2;
    const spreadBps = mid ? ((bestAsk - bestBid) / mid) * 10000 : 0;
    const depthUsdTop =
      bidsSorted.reduce((sum, [price, size]) => sum + price * size, 0) +
      asksSorted.reduce((sum, [price, size]) => sum + price * size, 0);

    await this.redis.set(`m:${productId}:spreadBps`, spreadBps);
    await this.redis.set(`m:${productId}:depthUsdTop`, depthUsdTop);
  }
}
