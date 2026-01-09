import Fastify from "fastify";
import rateLimit from "@fastify/rate-limit";
import type { Logger } from "pino";
import { z } from "zod";
import type { Env } from "../config/env.js";
import type { ScannerService } from "../scanner/service.js";
import type { Collector } from "../collector/index.js";
import type { RedisClient } from "../store/redis.js";
import { sendFcm } from "../notify/fcm.js";

const deviceSchema = z.object({
  token: z.string().min(10)
});

export function createServer(
  env: Env,
  log: Logger,
  redis: RedisClient,
  scanner: ScannerService,
  collector: Collector,
  getProductIds: () => string[]
) {
  const app = Fastify({ logger: log });

  app.register(rateLimit, {
    max: 60,
    timeWindow: "1 minute"
  });

  app.addHook("onRequest", async (req, reply) => {
    if (req.method === "POST") {
      const apiKey = req.headers["x-api-key"];
      if (apiKey !== env.API_KEY) {
        reply.status(401).send({ error: "Unauthorized" });
      }
    }
  });

  app.get("/health", async () => ({
    status: "ok",
    uptime: process.uptime(),
    wsConnected: collector.isWsConnected(),
    trackedProductsCount: collector.getTrackedCount()
  }));

  app.get("/scan/latest", async () => {
    const latest = await scanner.latest();
    return {
      latest,
      formatted: latest?.output ?? null
    };
  });

  app.post("/scan/run", async () => {
    const result = await scanner.run(getProductIds());
    if (result.state === "BUY" || (result.state === "SETUP_FORMING" && (result.readinessScore ?? 0) >= 70)) {
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
    return result;
  });

  app.post("/device/register", async (req, reply) => {
    const parsed = deviceSchema.safeParse(req.body);
    if (!parsed.success) {
      reply.status(400).send({ error: "Invalid token" });
      return;
    }
    await redis.sadd("devices:tokens", parsed.data.token);
    reply.send({ status: "ok" });
  });

  app.post("/device/unregister", async (req, reply) => {
    const parsed = deviceSchema.safeParse(req.body);
    if (!parsed.success) {
      reply.status(400).send({ error: "Invalid token" });
      return;
    }
    await redis.srem("devices:tokens", parsed.data.token);
    reply.send({ status: "ok" });
  });

  return app;
}
