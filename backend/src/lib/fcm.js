// Sends push notifications to a family member's device when someone scans a
// profile's QR code, IF the profile owner has an active maintenance
// subscription (this is a paid-plan perk). Requires a Firebase project of
// your own — this file does nothing (safely) until you provide credentials.
//
// SETUP (you must do this yourself — these are placeholders, not real keys):
//   1. Create a Firebase project at https://console.firebase.google.com
//   2. Project Settings -> Service Accounts -> Generate new private key
//      (downloads a JSON file).
//   3. Set the environment variable FIREBASE_SERVICE_ACCOUNT_JSON to the
//      FULL JSON CONTENTS of that file (as a single-line string), on Render
//      (or wherever this backend is deployed) under Environment variables.
//   4. Add the matching google-services.json to the Android app and enable
//      Cloud Messaging in the same Firebase project — the Android project
//      must belong to the same Firebase project as this service account.
//
// Until step 3 is done, every function below silently no-ops and logs a
// one-time warning — the rest of the app (orders, subscriptions, etc.)
// continues to work normally either way.

import admin from "firebase-admin";

let app = null;
let initTried = false;

function getApp() {
  if (initTried) return app;
  initTried = true;
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) {
    console.warn("[fcm] FIREBASE_SERVICE_ACCOUNT_JSON not set — push notifications are disabled (this is fine, just not configured yet).");
    return null;
  }
  try {
    const serviceAccount = JSON.parse(raw);
    app = admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    console.log("[fcm] Firebase Admin initialized — push notifications enabled.");
    return app;
  } catch (err) {
    console.error("[fcm] Failed to initialize Firebase Admin (push notifications disabled):", err.message);
    return null;
  }
}

/**
 * Sends the same notification to a list of FCM device tokens. Invalid/expired
 * tokens are ignored (best-effort) — this never throws, so it's always safe
 * to call from a request handler without extra try/catch at the call site.
 */
export async function sendToTokens(tokens, { title, body, data = {} }) {
  const a = getApp();
  if (!a || !tokens || tokens.length === 0) return { sent: 0, disabled: !a };
  try {
    const message = {
      notification: { title, body },
      data: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, String(v)])),
      tokens,
    };
    const result = await admin.messaging().sendEachForMulticast(message);
    const errors = result.responses
      .map((r, i) => (r.success ? null : { token: tokens[i].slice(0, 12) + "...", error: r.error?.message }))
      .filter(Boolean);
    if (errors.length) console.warn("[fcm] some sends failed:", errors);
    return { sent: result.successCount, disabled: false, failed: result.failureCount, errors };
  } catch (err) {
    console.error("[fcm] send failed:", err.message);
    return { sent: 0, disabled: false, error: err.message };
  }
}

export { getApp };
