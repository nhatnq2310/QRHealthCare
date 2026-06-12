# Building & Installing the APK

There are three ways to get the APK onto your phone. Pick the one that fits.

---

## Option A — GitHub Actions (no Android Studio needed)

This is the fastest path if you don't want to install Android Studio. The
project includes a workflow that compiles a debug APK on every push.

1. **Create a GitHub repo** (free) and push this folder to it:
   ```bash
   cd qrhealthcare-android
   git init
   git add .
   git commit -m "Initial commit"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<repo>.git
   git push -u origin main
   ```
2. Open the repo on GitHub → **Actions** tab → wait ~5 minutes for the
   "Build Debug APK" run to finish (green check).
3. Click the finished run → scroll down to **Artifacts** → download
   **`app-debug-apk`**. Inside is `app-debug.apk`.
4. Transfer the APK to your phone (USB, email, Google Drive — anything).
5. On the phone, open the APK file. Android will ask you to allow
   "Install unknown apps" from your file manager — say yes once. Install.
6. Open the app, set MockAPI URL once (see `seed/MOCKAPI_SETUP.md`),
   and it's ready.

Re-builds are automatic on every `git push`.

---

## Option B — Android Studio (recommended if you have it)

1. Open Android Studio → **File → Open** → pick the `qrhealthcare-android`
   folder. First sync downloads dependencies (~5 min).
2. In `app/src/main/kotlin/com/qrhealthcare/app/data/api/ApiClient.kt`,
   replace `YOUR_PROJECT_ID` with your MockAPI project ID.
3. Plug in your phone with USB debugging enabled
   (Settings → About → tap Build Number 7× to enable Developer mode,
   then Developer options → USB debugging).
4. Click the green ▶ **Run** button. Android Studio installs the
   debug APK and launches it.

To get a standalone APK file from Android Studio:
**Build → Build Bundle(s) / APK(s) → Build APK(s)** →
the popup shows the file at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Option C — Command line (if you have Android SDK + Gradle already)

```bash
cd qrhealthcare-android
gradle wrapper --gradle-version 8.10.2 --distribution-type all
./gradlew assembleDebug
# APK appears at app/build/outputs/apk/debug/app-debug.apk
```

---

## Before you publish to Google Play (later)

You'll need to switch from a *debug* APK to a *signed release* AAB:

1. Generate a signing keystore once:
   ```bash
   keytool -genkey -v -keystore qrhealthcare.jks -keyalg RSA \
     -keysize 2048 -validity 10000 -alias qrhealthcare
   ```
   **Back this file up.** If you lose it you can never update your app on
   Play Store under the same listing.
2. Add a `signingConfigs` block + `release { signingConfig = … }` in
   `app/build.gradle.kts`.
3. Run `./gradlew bundleRelease` → produces an `.aab` you upload to
   Play Console.
4. Migrate from MockAPI to Express+MongoDB (see `GUIDE.md` Part 3) and
   hash passwords with bcrypt — required for any app that holds health data.

---

## Sideload caveat

A *debug* APK signed with the debug keystore can be installed on any
phone but **not** uploaded to Play Store. The CI artifact is fine for
testing on your own and your team's devices, and for showing demos.
