import type { Logger } from "pino";
import admin from "firebase-admin";
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
  if (tokens.length === 0) return;
  // Firebase Admin SDK is required because legacy server keys are deprecated.
  getFirebaseApp(env);
  await Promise.all(
    tokens.map(async (token) => {
      try {
        await admin.messaging().send({
          token,
          notification: {
            title: payload.title,
            body: payload.body
          },
          data: payload.data
        });
      } catch (error) {
        const err = error as { code?: string; message?: string };
        const isInvalidToken = err.code === "messaging/invalid-argument" || err.code === "messaging/registration-token-not-registered";
        if (isInvalidToken) {
          log.warn({ err: error, token }, "FCM token invalid");
        } else {
          log.error({ err: error, token }, "FCM send failed");
        }
      }
    })
  );
}
