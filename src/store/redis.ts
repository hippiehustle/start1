import { Redis } from "@upstash/redis";
import type { Env } from "../config/env.js";

export function createRedis(env: Env) {
  return new Redis({
    url: env.UPSTASH_REDIS_REST_URL,
    token: env.UPSTASH_REDIS_REST_TOKEN
  });
}

export type RedisClient = ReturnType<typeof createRedis>;
