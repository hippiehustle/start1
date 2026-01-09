import type { Logger } from "pino";
import type { Env } from "../config/env.js";

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
  if (tokens.length === 0) return;
  const res = await fetch("https://fcm.googleapis.com/fcm/send", {
    method: "POST",
    headers: {
      Authorization: `key=${env.FCM_SERVER_KEY}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      registration_ids: tokens,
      notification: {
        title: payload.title,
        body: payload.body
      },
      data: payload.data
    })
  });
  if (!res.ok) {
    const text = await res.text();
    log.error({ status: res.status, text }, "FCM send failed");
  }
}
