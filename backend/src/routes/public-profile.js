import { Router } from "express";
import QrTag from "../models/QrTag.js";
import Profile from "../models/Profile.js";

const router = Router();

// ─── Public-safe field projection ─────────────────────────────────────────────
// Safety-critical fields are shown unconditionally: who the person is, blood
// type, who to call, and allergies. Everything else respects the privacy model:
//   • isPrivate = false                  → everything public
//   • isPrivate = true,  hiddenFields=[] → ONLY always-public fields shown
//   • isPrivate = true,  hiddenFields=[x]→ field x hidden, everything else public
const ALWAYS_PUBLIC = new Set([
  "fullName", "gender", "bloodGroup", "emergencyContacts", "allergies", "profileType",
]);

function pickPublic(profile) {
  if (!profile) return null;
  const p = profile.toJSON();
  const hidden = new Set(profile.hiddenFields || []);
  const emptyHidden = (profile.hiddenFields || []).length === 0;

  const out = { isPrivate: !!profile.isPrivate };

  for (const [k, v] of Object.entries(p)) {
    if (ALWAYS_PUBLIC.has(k)) {
      out[k] = v;                       // always visible
    } else if (!profile.isPrivate) {
      out[k] = v;                       // public profile → everything
    } else if (emptyHidden) {
      // private + no specific boxes → hide everything non-essential
      continue;
    } else if (!hidden.has(k)) {
      out[k] = v;                       // private + boxes set → only hide the boxed ones
    }
  }

  // Organ donation: respect the per-field show toggle independent of privacy.
  // Only surface it when registered AND the user chose to show it AND it
  // wasn't explicitly hidden.
  if (profile.organDonor && profile.showOrganDonor && !hidden.has("organDonor")) {
    out.organDonor = true;
  } else {
    delete out.organDonor;
  }

  return out;
}

// ─── HTML escaping (no template engine) ───────────────────────────────────────
const escapeHtml = (s) =>
  String(s ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");

const row = (label, value) => {
  if (value === undefined || value === null || value === "") return "";
  return `<div class="row"><div class="lbl">${escapeHtml(label)}</div><div class="val">${escapeHtml(value)}</div></div>`;
};

function renderArray(label, arr, fmt) {
  if (!Array.isArray(arr) || arr.length === 0) return "";
  const items = arr.map((x) => `<li>${escapeHtml(fmt(x))}</li>`).join("");
  return `<div class="section"><h3>${escapeHtml(label)}</h3><ul>${items}</ul></div>`;
}

function renderPage(p, tagCode, host) {
  const isPet = p.profileType === "pet";
  const title = isPet ? "Hồ Sơ Thú Cưng" : "Hồ Sơ Y Tế";
  const docsHtml = !p.healthDocuments?.length ? "" :
    `<div class="section"><h3>Tài liệu y tế</h3><div class="docs">` +
    p.healthDocuments.map((u) => {
      const abs = u.startsWith("http") ? u : `${host}${u}`;
      return `<a href="${escapeHtml(abs)}" target="_blank" rel="noopener"><img src="${escapeHtml(abs)}" alt="Tài liệu"></a>`;
    }).join("") +
    `</div></div>`;

  return `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
<title>${escapeHtml(title)} · ${escapeHtml(p.fullName || tagCode)}</title>
<style>
  :root { --red:#E53935; --bg:#f5f5f7; --card:#ffffff; --text:#111; --muted:#666; --line:#eaeaea; }
  * { box-sizing: border-box; }
  body { margin:0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
         background: var(--bg); color: var(--text); }
  .hdr { background: var(--red); color: white; padding: 28px 20px 24px; }
  .hdr .badge { display:inline-block; background: rgba(255,255,255,.18); padding: 4px 10px;
                border-radius: 999px; font-size: 12px; letter-spacing: .5px; }
  .hdr h1 { margin: 8px 0 4px; font-size: 26px; }
  .hdr .sub { opacity: .85; font-size: 14px; }
  .wrap { max-width: 600px; margin: -16px auto 24px; padding: 0 16px; }
  .card { background: var(--card); border-radius: 14px; padding: 16px 18px;
          margin-bottom: 14px; box-shadow: 0 1px 4px rgba(0,0,0,.06); }
  .row { display:flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid var(--line); gap: 12px; }
  .row:last-child { border-bottom: none; }
  .lbl { color: var(--muted); flex-shrink: 0; }
  .val { font-weight: 600; text-align: right; word-break: break-word; }
  .section { margin-top: 18px; }
  .section h3 { margin: 0 0 8px; font-size: 14px; text-transform: uppercase; letter-spacing: .5px; color: var(--red); }
  ul { margin: 0; padding-left: 20px; }
  li { padding: 4px 0; }
  .private-warn { background: #fff3cd; color: #856404; padding: 10px 14px; border-radius: 8px;
                  font-size: 13px; margin-bottom: 14px; }
  .notes { white-space: pre-wrap; background: #fafafa; padding: 12px; border-radius: 8px;
           font-size: 14px; color: #444; }
  .docs { display:grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 8px; }
  .docs img { width: 100%; height: 120px; object-fit: cover; border-radius: 8px;
              border: 1px solid var(--line); }
  .footer { text-align: center; color: var(--muted); font-size: 12px; padding: 8px 20px 24px; }
  .footer strong { color: var(--red); }
</style>
</head>
<body>
  <div class="hdr">
    <span class="badge">${escapeHtml(isPet ? "🐾 Thú cưng" : "👤 Người")}</span>
    <h1>${escapeHtml(p.fullName || "—")}</h1>
    <div class="sub">Mã: ${escapeHtml(tagCode)}</div>
  </div>
  <div class="wrap">
    ${p.isPrivate ? `<div class="private-warn">🔒 Hồ sơ riêng tư — chỉ hiển thị thông tin cơ bản. Liên hệ chủ hồ sơ để biết thêm chi tiết.</div>` : ""}

    <div class="card">
      ${row("Giới tính", p.gender)}
      ${row("Nhóm máu", p.bloodGroup)}
      ${row("Ngày sinh", p.birthDate)}
      ${row("Chiều cao", p.height)}
      ${row("Cân nặng", p.weight)}
      ${row("Hiến tạng", p.organDonor ? "Đã đăng ký" : undefined)}
      ${row("Số CMND/CCCD", p.personalNumber)}
    </div>

    ${renderArray("Liên hệ khẩn cấp", p.emergencyContacts || [],
       (c) => `${c.name || ""} — ${c.phone || ""} (${c.relationship || ""})`)}
    ${renderArray("Dị ứng", p.allergies || [],
       (a) => `${a.name || ""} (${a.severity || ""})${a.reaction ? " — " + a.reaction : ""}`)}
    ${renderArray("Thuốc đang dùng", p.medications || [],
       (m) => `${m.name || ""} — ${m.dosage || ""} (${m.frequency || ""})`)}
    ${renderArray("Bệnh lý nền", p.medicalConditions || [],
       (m) => `${m.name || ""}${m.diagnosedDate ? " (chẩn đoán: " + m.diagnosedDate + ")" : ""}${m.notes ? " — " + m.notes : ""}`)}
    ${renderArray("Bảo hiểm y tế", p.healthInsurance || [],
       (i) => `${i.provider || ""} — ${i.policyNumber || ""}${i.expiryDate ? " (hết hạn: " + i.expiryDate + ")" : ""}`)}
    ${renderArray("Bảo hiểm nhân thọ", p.lifeInsurance || [],
       (i) => `${i.provider || ""} — ${i.policyNumber || ""}${i.expiryDate ? " (hết hạn: " + i.expiryDate + ")" : ""}`)}

    ${p.notes ? `<div class="section"><h3>Ghi chú</h3><div class="notes">${escapeHtml(p.notes)}</div></div>` : ""}
    ${docsHtml}
  </div>
  <div class="footer">Được cung cấp bởi <strong>QR Healthcare</strong></div>
</body>
</html>`;
}

/**
 * GET /p/:tagCode
 * Returns the public HTML view of the profile linked to this tag.
 * Called by any phone's camera app — no auth, no JSON.
 */
router.get("/:tagCode", async (req, res) => {
  try {
    const tagCode = String(req.params.tagCode || "").toUpperCase().trim();
    const tag = await QrTag.findOne({ tagCode });
    if (!tag) {
      return res.status(404).type("html").send(notFoundPage("Mã QR không tồn tại"));
    }
    if (!tag.profileId) {
      return res.status(200).type("html").send(notFoundPage("Mã QR này chưa được liên kết với hồ sơ nào"));
    }

    // Atomically bump scanCount — fire-and-forget so it doesn't slow the response.
    QrTag.updateOne({ _id: tag._id }, { $inc: { scanCount: 1 } }).catch(() => {});

    const profile = await Profile.findById(tag.profileId);
    if (!profile) {
      return res.status(404).type("html").send(notFoundPage("Hồ sơ không còn tồn tại"));
    }
    Profile.updateOne({ _id: profile._id }, { $inc: { viewCount: 1 } }).catch(() => {});

    const safe = pickPublic(profile);
    const host = `${req.protocol}://${req.get("host")}`;
    res.type("html").send(renderPage(safe, tagCode, host));
  } catch (err) {
    console.error("[public-profile]", err);
    res.status(500).type("html").send(notFoundPage("Lỗi máy chủ"));
  }
});

function notFoundPage(message) {
  return `<!doctype html><html lang="vi"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>QR Healthcare</title>
<style>body{font-family:-apple-system,BlinkMacSystemFont,sans-serif;background:#f5f5f7;color:#111;
display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;padding:24px;}
.box{background:white;padding:32px;border-radius:14px;max-width:400px;text-align:center;
box-shadow:0 2px 8px rgba(0,0,0,.06);} .e{font-size:48px;margin-bottom:12px;}
h2{color:#E53935;margin:0 0 8px;} p{color:#666;margin:0;}</style></head>
<body><div class="box"><div class="e">⚠️</div><h2>Không thể hiển thị</h2><p>${escapeHtml(message)}</p></div></body></html>`;
}

export default router;
