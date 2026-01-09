import { z } from "zod";
import dotenv from "dotenv";

const envSchema = z.object({
  NODE_ENV: z.string().default("production"),
  PORT: z.coerce.number().default(8080),
  API_KEY: z.string().min(8),
  UPSTASH_REDIS_REST_URL: z.string().url(),
  UPSTASH_REDIS_REST_TOKEN: z.string().min(10),
  FIREBASE_SERVICE_ACCOUNT_JSON: z.string().min(20),
  COINBASE_WS_URL: z.string().url().default("wss://ws-feed.exchange.coinbase.com"),
  COINBASE_REST_BASE: z.string().url().default("https://api.exchange.coinbase.com"),
  LIQ_SPREAD_BPS_MAX: z.coerce.number().default(50),
  LIQ_DEPTH_USD_MIN: z.coerce.number().default(50000),
  TRACK_TOP_N: z.coerce.number().default(80)
});

export type Env = z.infer<typeof envSchema>;

export function loadEnv(): Env {
  dotenv.config();
  const baseEnv = process.env.NODE_ENV ?? "development";
  const merged = {
    ...process.env,
    NODE_ENV: baseEnv,
    API_KEY: process.env.API_KEY ?? (baseEnv !== "production" ? "dev-api-key" : undefined),
    UPSTASH_REDIS_REST_URL:
      process.env.UPSTASH_REDIS_REST_URL ??
      (baseEnv !== "production" ? "https://dev-upstash.example" : undefined),
    UPSTASH_REDIS_REST_TOKEN:
      process.env.UPSTASH_REDIS_REST_TOKEN ?? (baseEnv !== "production" ? "dev-upstash-token" : undefined),
    FIREBASE_SERVICE_ACCOUNT_JSON: process.env.FIREBASE_SERVICE_ACCOUNT_JSON
  };
  const parsed = envSchema.safeParse(merged);
  if (!parsed.success) {
    throw new Error(`Invalid environment: ${parsed.error.message}`);
  }
  return parsed.data;
}
