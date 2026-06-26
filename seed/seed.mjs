#!/usr/bin/env node
/**
 * Seeder for the QR Healthcare backend.
 *
 * Works against either backend (the route shapes are identical):
 *   node seed.mjs http://127.0.0.1:4000/api/v1/            (Express + Mongo)
 *   node seed.mjs https://YOUR_ID.mockapi.io/api/v1/       (legacy MockAPI)
 *
 * Requires Node 18+ (uses the built-in global fetch — no npm install needed).
 *
 * Order matters: users -> profiles -> products -> qrtags -> orders,
 * because profiles reference a user id and the linked qr tag / order
 * reference ids that the backend only assigns on insert. The script
 * captures those returned ids and wires the relationships up for you, so
 * right after seeding you get a fully working demo (login, profiles, a QR
 * tag you can scan, and one order that shows up in the admin dashboard).
 */

import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

const BASE = (process.argv[2] || process.env.API_BASE_URL || "").trim();
if (!BASE) {
  console.error(
    "\n✗ Missing API base URL.\n" +
      "  Usage: node seed.mjs http://127.0.0.1:4000/api/v1/\n"
  );
  process.exit(1);
}
const base = BASE.endsWith("/") ? BASE : BASE + "/";

const now = () => Date.now();

async function post(resource, body) {
  const res = await fetch(base + resource, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`POST ${resource} -> ${res.status} ${res.statusText}\n${text}`);
  }
  return res.json();
}

function tagCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "";
  for (let i = 0; i < 4; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return "QRH-" + s;
}
const pin = () => String(Math.floor(1000 + Math.random() * 9000));

async function main() {
  console.log(`\nSeeding ${base}\n`);

  // ── 1. USERS (register via /auth/register so passwords are bcrypt-hashed) ─
  const adminResp = await post("auth/register", {
    email: "admin@qrhealthcare.vn",
    password: "admin123",
    fullName: "Quản Trị Viên",
    role: "admin",
  });
  const admin = adminResp.user;
  const userResp = await post("auth/register", {
    email: "user@qrhealthcare.vn",
    password: "user123",
    fullName: "Nguyễn Văn A",
    role: "user",
  });
  const user = userResp.user;
  console.log(`✓ users        (admin id=${admin.id}, user id=${user.id})`);

  // ── 2. PROFILES (belong to the demo user) ───────────────────
  const humanProfile = await post("profiles", {
    userId: user.id,
    profileType: "human",
    fullName: "Nguyễn Văn A",
    gender: "Nam",
    birthDate: "1990-05-15",
    bloodGroup: "O+",
    height: "172cm",
    weight: "68kg",
    hairColor: "Đen",
    eyeColor: "Nâu",
    identificationMark: "Sẹo nhỏ ở cằm",
    personalNumber: "079090001234",
    organDonor: true,
    showOrganDonor: true,
    isPrivate: false,
    hiddenFields: ["personalNumber"],
    emergencyContacts: [
      { name: "Nguyễn Thị B", phone: "0901234567", relationship: "Vợ" },
    ],
    allergies: [{ name: "Penicillin", severity: "Nặng", reaction: "Phát ban, khó thở" }],
    medications: [{ name: "Aspirin", dosage: "81mg", frequency: "1 lần/ngày" }],
    medicalConditions: [{ name: "Cao huyết áp", diagnosedDate: "2020-01-10", notes: "Kiểm soát bằng thuốc" }],
    addresses: [
      { street: "123 Lê Lợi", ward: "Bến Nghé", district: "Quận 1", city: "TP. Hồ Chí Minh", country: "Việt Nam" },
    ],
    healthInsurance: [{ provider: "Bảo hiểm Y tế VN", policyNumber: "HS40790001", expiryDate: "2027-12-31" }],
    lifeInsurance: [{ provider: "Prudential", policyNumber: "PRU-2024-8812", expiryDate: "2040-06-01" }],
    viewCount: 0,
    createdAt: now(),
  });
  const petProfile = await post("profiles", {
    userId: user.id,
    profileType: "pet",
    fullName: "Milu",
    gender: "Đực",
    birthDate: "2021-03-01",
    bloodGroup: "",
    organDonor: false,
    isPrivate: false,
    hiddenFields: [],
    emergencyContacts: [{ name: "Nguyễn Văn A", phone: "0901234567", relationship: "Chủ nuôi" }],
    allergies: [],
    medications: [],
    medicalConditions: [],
    addresses: [],
    healthInsurance: [],
    lifeInsurance: [],
    viewCount: 0,
    createdAt: now(),
  });
  console.log(`✓ profiles     (human id=${humanProfile.id}, pet id=${petProfile.id})`);

  // ── 3. PRODUCTS ─────────────────────────────────────────────
  const products = JSON.parse(await readFile(join(__dirname, "products.json"), "utf8"));
  let productCount = 0;
  for (const p of products) {
    await post("products", p);
    productCount++;
  }
  console.log(`✓ products     (${productCount} seeded)`);

  // ── 3b. COUPONS ────────────────────────────────────────────
  const demoCoupons = [
    {
      code: "WELCOME10",
      description: "Giảm 10% cho khách hàng mới",
      discountType: "percent",
      discountValue: 10,
      minOrderAmount: 0,
      maxDiscount: 50000,
      active: true,
    },
    {
      code: "FREESHIP",
      description: "Giảm 30.000đ phí ship cho đơn từ 100k",
      discountType: "fixed",
      discountValue: 30000,
      minOrderAmount: 100000,
      active: true,
    },
    {
      code: "VIP50K",
      description: "Giảm 50.000đ cho đơn từ 200k",
      discountType: "fixed",
      discountValue: 50000,
      minOrderAmount: 200000,
      active: true,
      usageLimit: 100,
    },
  ];
  for (const c of demoCoupons) await post("coupons", c);
  console.log(`✓ coupons      (${demoCoupons.length} seeded)`);

  // ── 4. QR TAGS ──────────────────────────────────────────────
  // One linked to the human profile (so the scan/lookup flow works
  // right away) plus two unlinked tags to test the linking flow.
  const linkedCode = tagCode();
  const linkedPin = pin();
  const linkedTag = await post("qrtags", {
    tagCode: linkedCode,
    pin: linkedPin,
    profileId: humanProfile.id,
    productType: "sticker",
    orderId: "",
    scanCount: 3,
    createdAt: now(),
  });
  const unlinked = [];
  for (let i = 0; i < 2; i++) {
    const c = tagCode();
    const p = pin();
    await post("qrtags", {
      tagCode: c,
      pin: p,
      profileId: null,
      productType: "tag",
      orderId: "",
      scanCount: 0,
      createdAt: now(),
    });
    unlinked.push({ code: c, pin: p });
  }
  console.log(`✓ qrtags       (1 linked + 2 unlinked)`);

  // ── 5. ORDER (so the admin dashboard has revenue to show) ───
  const combo = products[0];
  await post("orders", {
    userId: user.id,
    profileId: humanProfile.id,
    items: [
      {
        productId: "1",
        productName: combo.name,
        price: combo.price,
        quantity: 1,
        selectedBlood: "O+",
      },
    ],
    totalAmount: combo.price,
    paymentMethod: "vietqr",
    status: "paid",
    qrTagIds: [linkedTag.id],
    createdAt: now(),
  });
  console.log(`✓ orders       (1 paid order)`);

  // ── DONE ────────────────────────────────────────────────────
  console.log("\n──────────────────────────────────────────────");
  console.log(" SEED COMPLETE — demo credentials");
  console.log("──────────────────────────────────────────────");
  console.log(" User  login : user@qrhealthcare.vn  / user123");
  console.log(" Admin login : admin@qrhealthcare.vn / admin123");
  console.log("");
  console.log(" Test the QR scan/lookup flow with this linked tag:");
  console.log(`   tagCode : ${linkedCode}`);
  console.log(`   pin     : ${linkedPin}`);
  console.log("");
  console.log(" Test the 'link a tag to a profile' flow with these:");
  unlinked.forEach((t, i) => console.log(`   tag ${i + 1}: ${t.code} / pin ${t.pin}`));
  console.log("");
  console.log(" Coupon codes you can try at checkout:");
  console.log("   WELCOME10  → 10% off, capped at 50k");
  console.log("   FREESHIP   → 30k off, min order 100k");
  console.log("   VIP50K     → 50k off, min order 200k");
  console.log("──────────────────────────────────────────────\n");
}

main().catch((err) => {
  console.error("\n✗ Seeding failed:\n" + err.message + "\n");
  console.error(
    "Common causes:\n" +
      " • The backend isn't running (start it: cd backend && npm run dev).\n" +
      " • Wrong base URL (use http://127.0.0.1:4000/api/v1/ for the Express backend).\n" +
      " • If you're re-running and emails already exist, drop the qrhealthcare db in Mongo first:\n" +
      "     mongosh mongodb://127.0.0.1:27017 --eval 'db.getSiblingDB(\"qrhealthcare\").dropDatabase()'\n"
  );
  process.exit(1);
});
