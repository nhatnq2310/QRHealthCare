# QR Healthcare — Session Handoff

## Status: feature-complete MVP, now on Express + MongoDB

The MockAPI prototype has been retired (it caps free accounts at 2
resources, but this app needs 5). The repo is now self-contained:
Android client + Express/Mongo backend in one project.

## Repository layout

```
qrhealthcare-android/
├── app/                  ← the Android Studio project
├── backend/              ← Express + MongoDB server (new this session)
│   ├── server.js
│   ├── src/
│   ├── smoke-test.mjs    ← optional integration test
│   └── README.md         ← run instructions
├── seed/                 ← demo-data seeder (works against either backend)
│   ├── seed.mjs
│   └── products.json
├── GUIDE.md
├── BUILD_APK.md
└── PROGRESS.md           ← you are here
```

---

## Run order

```bash
# 1. Start MongoDB (if not already running). On macOS with Homebrew:
brew services start mongodb-community
# Or on Linux:  sudo systemctl start mongod

# 2. Start the backend
cd backend
cp .env.example .env
npm install
npm run dev
# → http://0.0.0.0:4000/api/v1/ — ready

# 3. Seed demo data (in a second terminal)
node seed/seed.mjs http://127.0.0.1:4000/api/v1/

# 4. Build & run the Android app
#    - Open the project in Android Studio
#    - For emulator, BASE_URL is already set to http://10.0.2.2:4000/api/v1/
#    - For a physical phone, change BASE_URL in ApiClient.kt to your LAN IP
#    - Sync Gradle → Run
```

Login with `user@qrhealthcare.vn` / `user123` (or `admin@…` / `admin123`).

---

## What changed in this migration

### Backend (new)
- Express 4 + Mongoose 8 + JWT (`jsonwebtoken`) + bcrypt (`bcryptjs`)
- REST shape matches the previous MockAPI exactly (so the only Android
  change for the data routes is the base URL)
- New endpoints: `POST /auth/register`, `POST /auth/login` — return
  `{ user, token }` with a real JWT
- Server-enforced 5-profile-per-user cap
- Standard MongoDB indexes on `email`, `slug`, `tagCode`, `profileId`, `userId`

### Android
- `data/api/ApiClient.kt` — base URL flipped to the local backend; auth
  interceptor enabled so every authenticated request gets
  `Authorization: Bearer <jwt>`
- `data/api/ApiService.kt` — added `LoginRequest`, `RegisterRequest`,
  `AuthResponse`, plus the two new auth endpoints
- `data/repository/AppRepository.kt` — `login`/`register` now hit the
  real backend, persist the JWT, and update `ApiClient.authToken`
- `data/local/SessionManager.kt` — token field is now a real JWT;
  `AppRepository.init {}` collects from this flow to keep the
  interceptor hydrated across process restarts

### Seeder
- Now posts users via `/auth/register` so passwords are bcrypt-hashed
- Renamed env var `MOCKAPI_URL` → `API_BASE_URL`
- Works unchanged against the Express backend

---

## ⚠️ Known limitations to fix before real users

1. **Data routes don't require auth yet.** The JWT is issued and sent on
   every request, but `/profiles`, `/qrtags`, `/orders` don't validate
   it server-side. Add `requireAuth` from `backend/src/auth.js` to those
   routes (and ownership checks) before any real launch. See
   `backend/README.md` for the exact change.
2. **HTTP only.** Cleartext is enabled in the manifest for dev. Move to
   HTTPS in production.
3. **JWT secret in `.env`.** Generate a real one: `openssl rand -hex 64`.
4. **Payment reconciliation.** VietQR is generated and shown to the
   customer, but detecting that a bank transfer cleared still needs a
   webhook integration (SePay / Casso are the common Vietnamese options).

---

## Out of scope (intentionally)

- Real Google Play Billing — VietQR replaces the placeholder for now
- Push notifications when a QR is scanned (FCM)
- Profile avatar upload to cloud storage
- Hosted deployment (Render, Railway, Fly, etc.)
