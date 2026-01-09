import { z } from "zod";

const envSchema = z.object({
  NODE_ENV: z.string().default("production"),
  PORT: z.coerce.number().default(8080),
  API_KEY: z.string().min(8),
  UPSTASH_REDIS_REST_URL: z.string().url(),
  UPSTASH_REDIS_REST_TOKEN: z.string().min(10),
  FIREBASE_SERVICE_ACCOUNT_JSON: z.string().min(50),
  COINBASE_WS_URL: z.string().url().default("wss://ws-feed.exchange.coinbase.com"),
  COINBASE_REST_BASE: z.string().url().default("https://api.exchange.coinbase.com"),
  LIQ_SPREAD_BPS_MAX: z.coerce.number().default(50),
  LIQ_DEPTH_USD_MIN: z.coerce.number().default(50000),
  TRACK_TOP_N: z.coerce.number().default(80)
});

export type Env = z.infer<typeof envSchema>;

export function loadEnv(): Env {
  const parsed = envSchema.safeParse(process.env);
  if (!parsed.success) {
    throw new Error(`Invalid environment: ${parsed.error.message}`);
  }
  return parsed.data;
}
