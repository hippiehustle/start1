// Modern FCM sender (HTTP v1) via Firebase Admin SDK.
// Kept in fcm.ts to preserve existing import paths ("../notify/fcm.js")
// while removing legacy server-key based messaging.

import type { Message } from "firebase-admin/messaging";
import { getMessaging } from "./firebase.js";

/**
 * Backwards-compatible function name used elsewhere in the codebase.
 * Sends to a single device token.
 */
export async function sendFcm(token: string, title: string, body: string, data?: Record<string, string>) {
  const message: Message = {
    token,
    notification: { title, body },
    data
  };

  try {
    await getMessaging().send(message);
  } catch (err: any) {
    // Don't crash the whole process on push failures.
    // Consider removing invalid tokens if you store them.
    const code = err?.errorInfo?.code || err?.code;
    console.error("FCM send error:", code, err?.message || err);

    // Optional: swallow invalid/expired tokens quietly
    // messaging/registration-token-not-registered is common
  }
}
