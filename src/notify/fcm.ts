import type { Logger } from "pino";
import type { Env } from "../config/env.js";
import { getFirebaseApp } from "./firebase.js";

export type FcmPayload = {
  title: string;
  body: string;
  data: Record<string, string>;
};

export async function sendFcm(
  env: Env,
  log: Logger,
  tokens: string[],
  payload: FcmPayload
) {
  if (tokens.length === 0) {
    log.info("No FCM tokens to send to");
    return;
  }

  try {
    const app = getFirebaseApp(env);
    const messaging = app.messaging();

    const message = {
      notification: {
        title: payload.title,
        body: payload.body
      },
      data: payload.data
    };

    // Send to multiple tokens
    const responses = await messaging.sendEachForMulticast({
      tokens,
      ...message
    });

    if (responses.failureCount > 0) {
      const failedTokens: string[] = [];
      responses.responses.forEach((resp, idx) => {
        if (!resp.success) {
          failedTokens.push(tokens[idx]);
          log.error({ error: resp.error?.message }, `Failed to send FCM to token ${idx}`);
        }
      });
      log.warn({ failedCount: responses.failureCount, failedTokens }, "Some FCM messages failed");
    } else {
      log.info({ successCount: responses.successCount }, "All FCM messages sent successfully");
    }
  } catch (error) {
    log.error({ error }, "Failed to send FCM messages");
  }
}
