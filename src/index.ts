import pino from "pino";
import { loadEnv } from "./config/env.js";
import { createRedis } from "./store/redis.js";
import { CoinbaseRestClient } from "./coinbase/rest.js";
import { ProductUniverse } from "./coinbase/universe.js";
import { Collector } from "./collector/index.js";
import { ScannerService } from "./scanner/service.js";
import { createServer } from "./api/server.js";
import { startScheduler } from "./scheduler/index.js";

const env = loadEnv();
const log = pino({ level: env.NODE_ENV === "production" ? "info" : "debug" });
const redis = createRedis(env);
const rest = new CoinbaseRestClient(env);
const universe = new ProductUniverse(rest, log, env.TRACK_TOP_N);
const collector = new Collector(env, redis, log);
const scanner = new ScannerService(redis, env, log);

let trackedProducts: string[] = [];

async function refreshUniverse() {
  const snapshot = await universe.refresh();
  trackedProducts = snapshot.productIds;
  collector.updateProducts(trackedProducts);
  await redis.set("market:tracked:count", trackedProducts.length);
}

async function refreshCandles() {
  for (const productId of trackedProducts) {
    try {
      const candles1h = await rest.getCandles(productId, 3600);
      const candles1d = await rest.getCandles(productId, 86400);
      await redis.set(`c:${productId}:1h`, JSON.stringify(candles1h.slice(-200)));
      await redis.set(`c:${productId}:1d`, JSON.stringify(candles1d.slice(-120)));
    } catch (error) {
      log.warn({ err: error, productId }, "Failed to refresh candles");
    }
  }
}

async function bootstrap() {
  await refreshUniverse();
  collector.start(trackedProducts);
  await refreshCandles();

  setInterval(refreshUniverse, 30 * 60 * 1000);
  setInterval(refreshCandles, 6 * 60 * 60 * 1000);

  startScheduler(log, async () => {
    const result = await scanner.run(trackedProducts);
    if (result.state === "BUY" || (result.state === "SETUP_FORMING" && (result.readinessScore ?? 0) >= 70)) {
      const { sendFcm } = await import("./notify/fcm.js");
      const tokens = (await redis.smembers<string>("devices:tokens")) ?? [];
      await sendFcm(env, log, tokens, {
        title: "EV Crypto Scan",
        body: result.output.split("\n")[0],
        data: {
          state: result.state,
          productId: result.productId ?? "",
          readinessScore: String(result.readinessScore ?? 0)
        }
      });
    }
  });

  const app = createServer(env, log, redis, scanner, collector, () => trackedProducts);
  await app.listen({ port: env.PORT, host: "0.0.0.0" });
  log.info({ port: env.PORT }, "API server started");
}

bootstrap().catch((error) => {
  log.error({ err: error }, "Fatal error");
  process.exit(1);
});
