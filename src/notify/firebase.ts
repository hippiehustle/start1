import admin from "firebase-admin";
import { loadEnv } from "../config/env.js";

let initialized = false;

function initFirebase() {
  if (initialized) return;

  const env = loadEnv();

  const serviceAccount = JSON.parse(
    env.FIREBASE_SERVICE_ACCOUNT_JSON
  ) as admin.ServiceAccount;

  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });

  initialized = true;
}

/**
 * Returns the Firebase Messaging instance.
 * Ensures Firebase is initialized exactly once.
 */
export function getMessaging() {
  initFirebase();
  return admin.messaging();
}
