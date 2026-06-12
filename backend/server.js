import "dotenv/config";
import express from "express";
import cors from "cors";
import { connectDb } from "./src/db.js";

import authRoutes     from "./src/routes/auth.js";
import usersRoutes    from "./src/routes/users.js";
import profilesRoutes from "./src/routes/profiles.js";
import productsRoutes from "./src/routes/products.js";
import qrTagsRoutes   from "./src/routes/qrtags.js";
import ordersRoutes   from "./src/routes/orders.js";
import couponsRoutes  from "./src/routes/coupons.js";
import uploadsRoutes  from "./src/routes/uploads.js";
import publicProfileRoutes from "./src/routes/public-profile.js";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const PORT = Number(process.env.PORT) || 4000;
const app = express();

app.use(cors({ origin: process.env.CORS_ORIGIN || "*" }));
app.use(express.json({ limit: "1mb" }));

// Tiny request logger — useful when debugging from the phone.
app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// Health check so you can confirm reachability from the phone's browser.
app.get("/", (_req, res) => res.json({ ok: true, service: "qrhealthcare-backend" }));
app.get("/health", (_req, res) => res.json({ ok: true }));

// All API routes under /api/v1 to match the existing ApiClient base URL.
app.use("/api/v1/auth",     authRoutes);
app.use("/api/v1/users",    usersRoutes);
app.use("/api/v1/profiles", profilesRoutes);
app.use("/api/v1/products", productsRoutes);
app.use("/api/v1/qrtags",   qrTagsRoutes);
app.use("/api/v1/orders",   ordersRoutes);
app.use("/api/v1/coupons",  couponsRoutes);
app.use("/api/v1/uploads",  uploadsRoutes);

// Static file hosting for uploaded health documents. The same path is used by
// both the Android client (which prepends the API host) and the public-profile
// page (which embeds <img src="/uploads/...">).
app.use("/uploads", express.static(path.resolve(__dirname, "uploads")));

// Public-facing HTML page — scanned QR codes encode a URL into this route, so
// any phone's camera app can open the profile in a browser without our app.
app.use("/p", publicProfileRoutes);

app.use((err, _req, res, _next) => {
  console.error("[unhandled]", err);
  res.status(500).json({ error: "Internal server error" });
});

connectDb(process.env.MONGODB_URI)
  .then(() => {
    // Bind to 0.0.0.0 so devices on the same WiFi can reach the server
    // via the host's LAN IP, not just localhost.
    app.listen(PORT, "0.0.0.0", () => {
      console.log(`[api] http://0.0.0.0:${PORT}/api/v1/ — ready`);
      console.log(`[api] phones on the same WiFi: http://<your-LAN-IP>:${PORT}/api/v1/`);
      console.log(`[api] Android emulator: http://10.0.2.2:${PORT}/api/v1/`);
    });
  })
  .catch((err) => {
    console.error("[startup] failed to connect to MongoDB:", err.message);
    process.exit(1);
  });
