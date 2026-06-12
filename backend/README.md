# QR Healthcare — Backend (Express + MongoDB)

Replaces the MockAPI prototype. Same REST shape, plus real `/auth/login`
and `/auth/register` endpoints with bcrypt password hashing and JWT tokens.

---

## Quick start (local development)

You need:
- **Node 18+** — `node -v` should print v18 or higher
- **MongoDB running** at the URI in your `.env` (the default is your local
  install at `mongodb://127.0.0.1:27017/qrhealthcare`)

```bash
# from the repo root
cd backend
cp .env.example .env          # the defaults work for local dev as-is
npm install                   # ~10s, no native compilation (uses bcryptjs)
npm run dev                   # auto-restarts on file changes
```

You should see:

```
[db] connected to mongodb://127.0.0.1:27017/qrhealthcare
[api] http://0.0.0.0:4000/api/v1/ — ready
[api] phones on the same WiFi: http://<your-LAN-IP>:4000/api/v1/
[api] Android emulator: http://10.0.2.2:4000/api/v1/
```

Open `http://127.0.0.1:4000/health` in a browser — `{"ok":true}` means it works.

---

## Seed demo data (one command)

```bash
node ../seed/seed.mjs http://127.0.0.1:4000/api/v1/
```

This creates the demo accounts (`user@qrhealthcare.vn`/`user123`,
`admin@qrhealthcare.vn`/`admin123`), the 7 real products, two profiles, a
linked QR tag, and one paid order. It prints the test tag code at the end.

To re-seed cleanly (if you change schemas during development):

```bash
mongosh mongodb://127.0.0.1:27017 --eval 'db.getSiblingDB("qrhealthcare").dropDatabase()'
node ../seed/seed.mjs http://127.0.0.1:4000/api/v1/
```

---

## Point the Android app at this server

Open `app/src/main/kotlin/com/qrhealthcare/app/data/api/ApiClient.kt` and
set `BASE_URL`:

| Device                              | URL                                       |
| ----------------------------------- | ----------------------------------------- |
| Android emulator on this computer   | `http://10.0.2.2:4000/api/v1/`            |
| Physical phone on the same WiFi     | `http://<your-LAN-IP>:4000/api/v1/`       |
| Deployed backend                    | `https://your-domain.com/api/v1/`         |

To find your LAN IP:
- **macOS**: `ipconfig getifaddr en0` (Wi-Fi) or `en1`
- **Linux**: `hostname -I` (first value)
- **Windows**: `ipconfig` → look for "IPv4 Address" under your Wi-Fi adapter

The phone must be on the same Wi-Fi as the computer. Cleartext HTTP works
because the manifest already sets `android:usesCleartextTraffic="true"` —
fine for dev, but switch to HTTPS when you deploy.

---

## Verify the backend on its own (optional)

```bash
npm install -D mongodb-memory-server
node smoke-test.mjs
```

Spins up an in-memory MongoDB, runs the full register → login → profile →
QR-link → public-lookup flow, prints `✓ all checks passed`. Useful when
you're changing routes and want a quick safety net.

---

## API reference

All routes are under `/api/v1/`.

### Auth (public)

| Method | Path             | Body                                          | Returns                       |
| ------ | ---------------- | --------------------------------------------- | ----------------------------- |
| POST   | `/auth/register` | `{ email, password, fullName, role? }`        | `{ user, token }` (201)       |
| POST   | `/auth/login`    | `{ email, password }`                         | `{ user, token }` (200) / 401 |

### Users
| Method | Path          | Notes                                              |
| ------ | ------------- | -------------------------------------------------- |
| GET    | `/users`      | Optional `?email=` filter                          |
| GET    | `/users/:id`  | —                                                  |
| PUT    | `/users/:id`  | Password fields are ignored — use `/auth` for that |

### Profiles
| Method | Path             | Notes                                                            |
| ------ | ---------------- | ---------------------------------------------------------------- |
| GET    | `/profiles`      | Optional `?userId=` filter                                       |
| GET    | `/profiles/:id`  | —                                                                |
| POST   | `/profiles`      | `userId` required; server-enforced 5-profile cap per user        |
| PUT    | `/profiles/:id`  | Full replacement                                                 |
| DELETE | `/profiles/:id`  | —                                                                |

### Products / QR tags / Orders
Standard REST. Same filters as MockAPI:
- `GET /products?slug=combo-sticker-y-te`
- `GET /qrtags?tagCode=QRH-A1B2`
- `GET /qrtags?profileId=...`
- `GET /orders?userId=...`

---

## ⚠️ Security note before you ship

Right now the data routes (`/profiles`, `/qrtags`, `/orders`) **do not
enforce JWT** — anyone who knows the URL can read or modify data. This
matches the prototype behaviour so the Android client works without
further changes, and is fine while you're developing on localhost.

Before you take real users, add `requireAuth` from `src/auth.js` to those
routes and add ownership checks (e.g. `if (profile.userId !== req.auth.sub
&& req.auth.role !== "admin") return res.status(403)`). The middleware
is already written and the Android client already sends `Authorization:
Bearer <jwt>` on every request — you just need to enable enforcement.

---

## Production checklist

Before you accept real users (especially with health data):

1. **Secrets**: set a long random `JWT_SECRET` (`openssl rand -hex 64`).
2. **HTTPS**: deploy behind a TLS terminator (Caddy, Nginx, or a PaaS
   like Render / Railway that gives you HTTPS automatically).
3. **CORS**: lock `CORS_ORIGIN` to your real frontends only.
4. **Database**: use MongoDB Atlas or a managed instance, not localhost.
5. **Rate limiting**: add `express-rate-limit` on `/auth/*` endpoints.
6. **Backups**: enable automated backups on your MongoDB instance.
7. **Schema indexes**: the schemas already index `userId`, `tagCode`,
   `profileId`, `email`, and `slug`. Verify they were created
   (`db.qrtags.getIndexes()`).

---

## File layout

```
backend/
├── package.json
├── .env.example          ← copy to .env, edit if needed
├── server.js             ← entry: connects Mongo, starts Express
├── smoke-test.mjs        ← optional in-memory integration test
└── src/
    ├── db.js
    ├── auth.js           ← signToken, requireAuth, requireAdmin
    ├── models/
    │   ├── User.js       ← strips passwordHash from JSON output
    │   ├── Profile.js    ← nested sub-arrays for contacts/allergies/...
    │   ├── Product.js
    │   ├── QrTag.js
    │   └── Order.js
    └── routes/
        ├── auth.js
        ├── users.js
        ├── profiles.js
        ├── products.js
        ├── qrtags.js
        └── orders.js
```
