import type { Logger } from "pino";
import type { RedisClient } from "../store/redis.js";
import type { Env } from "../config/env.js";
import { ScannerEngine } from "./engine.js";
import type { ScanResult } from "./types.js";

export class ScannerService {
  private engine: ScannerEngine;

  constructor(
    private readonly redis: RedisClient,
    private readonly env: Env,
    private readonly log: Logger
  ) {
    this.engine = new ScannerEngine(redis, env, log);
  }

  async run(productIds: string[]): Promise<ScanResult> {
    const result = await this.engine.scan(productIds);
    await this.redis.set("scan:latest", JSON.stringify(result));
    await this.redis.lpush("scan:history", JSON.stringify(result));
    await this.redis.ltrim("scan:history", 0, 199);
    if (result.state === "BUY") {
      await this.redis.incr("scan:buys:last7d");
      await this.redis.expire("scan:buys:last7d", 7 * 24 * 60 * 60);
      await this.redis.set("scan:noTradeStreak", 0);
    } else if (result.state === "NO_TRADE") {
      await this.redis.incr("scan:noTradeStreak");
    } else {
      await this.redis.set("scan:noTradeStreak", 0);
    }
    return result;
  }

  async latest(): Promise<ScanResult | null> {
    const raw = await this.redis.get("scan:latest");
    if (!raw) return null;
    return JSON.parse(String(raw)) as ScanResult;
  }
}
