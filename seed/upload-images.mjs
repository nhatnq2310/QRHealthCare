#!/usr/bin/env node
// Uploads PNGs from seed/images/ to your backend's /uploads endpoint,
// then updates each product's imageUrl in the database to the new URL.
//
//   node seed/upload-images.mjs                              ← localhost default
//   node seed/upload-images.mjs http://127.0.0.1:4000/api/v1/
//   node seed/upload-images.mjs https://your-app.onrender.com/api/v1/

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const BASE = (process.argv[2] || "http://127.0.0.1:4000/api/v1/").trim();
const base = BASE.endsWith("/") ? BASE : BASE + "/";

// Map each product slug → PNG filename (relative to seed/images/)
const IMAGE_MAP = {
  "combo-sticker-y-te":     "stickerCombo.png",
  "sticker-y-te":           "sticker.png",
  "the-y-te":               "card.png",
  "sticker-che-phu-them":   "stickerCover.png",
  "tag-y-te":               "tag.png",
  "tag-thu-cung":           "petTag.png",
  "sticker-tim-chu":        "item.png",
};

async function uploadFile(filePath) {
  const buf  = fs.readFileSync(filePath);
  const name = path.basename(filePath);
  const form = new FormData();
  form.append("file", new Blob([buf], { type: "image/png" }), name);
  const res = await fetch(base + "uploads", { method: "POST", body: form });
  if (!res.ok) throw new Error(`upload failed ${res.status}: ${await res.text()}`);
  return (await res.json()).url;
}

async function getAllProducts() {
  const res = await fetch(base + "products");
  if (!res.ok) throw new Error(`GET products failed: ${res.status}`);
  return res.json();
}

async function updateProduct(id, body) {
  const res = await fetch(base + "products/" + id, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`PUT failed ${res.status}: ${await res.text()}`);
  return res.json();
}

async function main() {
  console.log(`→ Target: ${base}\n`);
  const products = await getAllProducts();
  if (!products.length) {
    console.error("No products in DB. Run `node seed/seed.mjs` first.");
    process.exit(1);
  }

  const imageDir = path.join(__dirname, "images");
  let ok = 0, skipped = 0, failed = 0;

  for (const product of products) {
    const filename = IMAGE_MAP[product.slug];
    if (!filename) { console.log(`⊘ ${product.slug} — no mapping, skipped`); skipped++; continue; }

    const filePath = path.join(imageDir, filename);
    if (!fs.existsSync(filePath)) { console.log(`✗ ${product.slug} — ${filename} not found`); failed++; continue; }

    try {
      process.stdout.write(`↑ ${product.slug.padEnd(24)} ← ${filename.padEnd(20)} ... `);
      const url = await uploadFile(filePath);
      await updateProduct(product.id, { ...product, imageUrl: url });
      console.log("✓");
      console.log(`   → ${url}`);
      ok++;
    } catch (e) {
      console.log(`✗ ${e.message}`);
      failed++;
    }
  }

  console.log(`\nDone. ${ok} updated, ${skipped} skipped, ${failed} failed.`);
}

main().catch(e => { console.error(e); process.exit(1); });