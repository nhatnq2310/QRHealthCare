# Setting up push notifications (Family Scan Alerts)

The "notify a family member when this profile's QR is scanned" feature needs
your own Firebase project. The code is already wired up on both the backend
and the app — you just need to plug in real credentials in two places.

## 1. Create the Firebase project

1. Go to https://console.firebase.google.com and create a new project (or
   reuse an existing one).
2. Add an **Android app** to it with package name `com.qrhealthcare.app`
   (must match `applicationId` in `app/build.gradle.kts` exactly).
3. Download the generated **`google-services.json`** file.
4. Place it at `app/google-services.json` (same folder as `build.gradle.kts`
   for the app module).

## 2. Enable the Gradle plugin

Two files have the plugin commented out — uncomment both lines:

**Root `build.gradle.kts`:**
```kotlin
id("com.google.gms.google-services") version "4.4.2" apply false
```

**`app/build.gradle.kts`:**
```kotlin
id("com.google.gms.google-services")
```

Then sync Gradle. From this point, the app will initialize Firebase
automatically and `FirebaseMessaging.getInstance().token` will succeed.

## 3. Get a service account key for the backend

1. In Firebase Console: **Project Settings → Service Accounts → Generate new
   private key**. This downloads a JSON file — keep it secret, don't commit it.
2. On your backend host (e.g. Render → your service → Environment), add an
   environment variable:
   - **Key:** `FIREBASE_SERVICE_ACCOUNT_JSON`
   - **Value:** the full contents of that JSON file, pasted as one value.
3. Redeploy the backend. On startup you should see:
   `[fcm] Firebase Admin initialized — push notifications enabled.`
   If you instead see a warning that it's not set, the environment variable
   didn't save correctly — check for it being truncated or improperly quoted.

## 4. Test it

1. In the app, open a profile → **"Thông Báo Cho Người Thân Khi Bị Quét"** →
   share the code, or just tap **"Đăng Ký Thiết Bị Này"** on your own device
   to register yourself for testing.
2. Make sure that profile's owner account has an **active** subscription
   (trial counts as active too) — notifications are a paid-plan perk and
   won't fire otherwise.
3. Open the profile's public QR link in a browser
   (`https://YOUR-BACKEND/api/v1/public/<tagCode>`) from a different
   device/browser session (not the ?family= link) to simulate a scan.
4. The registered device should get a push notification within a few seconds.

## Notes

- Everything works today (orders, subscriptions, admin dashboard, etc.)
  **without** doing any of the above — this setup only affects the family
  scan-notification feature specifically. Nothing else depends on Firebase.
- The location-sharing button on the public scan page uses the **scanner's
  own mobile browser** geolocation (an opt-in prompt on that web page) — it
  has nothing to do with Android app permissions, so no extra Android setup
  is needed for that part.
- If a device's FCM token goes stale (app reinstalled, etc.), the old token
  in `Profile.familyFcmTokens` will just silently fail to deliver — there's
  no automatic cleanup of dead tokens in this MVP. Re-registering replaces it.
