import { Router } from "express";
import QrTag from "../models/QrTag.js";
import Profile from "../models/Profile.js";
import Subscription from "../models/Subscription.js";
import { sendToTokens } from "../lib/fcm.js";

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

function pickPublic(profile, bypassPrivacy = false) {
  if (!profile) return null;
  const p = profile.toJSON();
  const hidden = new Set(profile.hiddenFields || []);
  const emptyHidden = (profile.hiddenFields || []).length === 0;
  // Frozen for non-payment of the maintenance subscription behaves exactly
  // like the user's own isPrivate toggle — it's a second, independent reason
  // to force the same "only always-public fields" view. A valid family
  // access token (see /profiles/:id/family-register) overrides both, since
  // the owner explicitly granted that specific device full visibility.
  const forcedPrivate = !bypassPrivacy && (!!profile.isPrivate || !!profile.subscriptionFrozen);

  const out = { isPrivate: !!profile.isPrivate, subscriptionFrozen: !!profile.subscriptionFrozen };

  for (const [k, v] of Object.entries(p)) {
    if (ALWAYS_PUBLIC.has(k)) {
      out[k] = v;                       // always visible
    } else if (!forcedPrivate) {
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

function renderPage(p, tagCode, host, opts = {}) {
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
    ${p.subscriptionFrozen
        ? `<div class="private-warn">🔒 Hồ sơ tạm khóa do gói duy trì lưu trữ đã hết hạn — chỉ hiển thị thông tin cơ bản. Chủ hồ sơ cần gia hạn để mở lại đầy đủ thông tin.</div>`
        : p.isPrivate
          ? `<div class="private-warn">🔒 Hồ sơ riêng tư — chỉ hiển thị thông tin cơ bản. Liên hệ chủ hồ sơ để biết thêm chi tiết.</div>`
          : ""}

    ${opts.showLocationOptIn ? `
    <div class="card" id="loc-card">
      <div style="font-weight:600; margin-bottom:6px;">📍 Thông báo vị trí cho người thân?</div>
      <div style="color:var(--muted); font-size:13px; margin-bottom:10px;">
        Chủ hồ sơ đã bật thông báo khẩn cấp. Bạn có thể chia sẻ vị trí hiện tại để người thân của họ biết bạn đang ở đâu.
      </div>
      <button id="loc-btn" onclick="shareLocation()"
        style="background:var(--red); color:white; border:none; padding:10px 16px; border-radius:8px; font-size:14px; width:100%; cursor:pointer;">
        Chia Sẻ Vị Trí Của Tôi
      </button>
      <div id="loc-status" style="margin-top:8px; font-size:13px; color:var(--muted);"></div>
    </div>
    <script>
      function shareLocation() {
        var btn = document.getElementById('loc-btn');
        var status = document.getElementById('loc-status');
        if (!navigator.geolocation) { status.textContent = 'Trình duyệt không hỗ trợ định vị.'; return; }
        btn.disabled = true;
        status.textContent = 'Đang lấy vị trí...';
        navigator.geolocation.getCurrentPosition(function(pos) {
          fetch(${JSON.stringify(opts.notifyEndpoint || "")}, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ lat: pos.coords.latitude, lng: pos.coords.longitude })
          }).then(function() {
            status.textContent = '✅ Đã gửi vị trí cho người thân.';
          }).catch(function() {
            status.textContent = 'Không thể gửi vị trí — vui lòng thử lại.';
            btn.disabled = false;
          });
        }, function() {
          status.textContent = 'Bạn đã từ chối chia sẻ vị trí.';
          btn.disabled = false;
        }, { enableHighAccuracy: true, timeout: 10000 });
      }
    </script>` : ""}

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

    // A family member's own bypass link (?family=<token>) shows them the full
    // profile regardless of privacy/freeze — and since it's THEM viewing, we
    // don't send them a "someone scanned your profile" notification about it.
    const familyToken = String(req.query.family || "");
    const bypassPrivacy = !!familyToken && !!profile.familyAccessToken && familyToken === profile.familyAccessToken;

    const host = `${req.protocol}://${req.get("host")}`;

    // Family notification (subscription perk): only fires for a genuine
    // third-party scan, only while the owner's maintenance plan is active,
    // and only if at least one family device has registered for this profile.
    let showLocationOptIn = false;
    if (!bypassPrivacy && profile.familyFcmTokens?.length) {
      const sub = await Subscription.findOne({ userId: profile.userId });
      if (sub && (sub.status === "active" || sub.status === "trial")) {
        showLocationOptIn = true;
        const fullViewUrl = `${host}/p/${tagCode}?family=${profile.familyAccessToken}`;
        sendToTokens(profile.familyFcmTokens, {
          title: `Có người vừa quét mã QR của ${profile.fullName || "hồ sơ"}`,
          body: "Nhấn để xem đầy đủ thông tin hồ sơ.",
          data: { type: "qr_scan", profileId: String(profile._id), fullViewUrl },
        }).catch(() => {});
      }
    }

    const safe = pickPublic(profile, bypassPrivacy);
    res.type("html").send(renderPage(safe, tagCode, host, {
      showLocationOptIn,
      notifyEndpoint: `${host}/p/${tagCode}/scan-location`,
    }));
  } catch (err) {
    console.error("[public-profile]", err);
    res.status(500).type("html").send(notFoundPage("Lỗi máy chủ"));
  }
});

// POST /public/:tagCode/scan-location — called by the opt-in button's JS on
// the scan page itself, after the scanner grants geolocation permission.
// Sends a follow-up push with a Google Maps link, same gating as above.
router.post("/:tagCode/scan-location", async (req, res) => {
  try {
    const tagCode = String(req.params.tagCode || "").toUpperCase().trim();
    const { lat, lng } = req.body || {};
    if (typeof lat !== "number" || typeof lng !== "number") {
      return res.status(400).json({ error: "lat/lng không hợp lệ" });
    }
    const tag = await QrTag.findOne({ tagCode });
    if (!tag?.profileId) return res.status(404).json({ error: "Không tìm thấy" });
    const profile = await Profile.findById(tag.profileId);
    if (!profile?.familyFcmTokens?.length) return res.json({ sent: 0 });

    const sub = await Subscription.findOne({ userId: profile.userId });
    if (!sub || (sub.status !== "active" && sub.status !== "trial")) return res.json({ sent: 0 });

    const mapsUrl = `https://www.google.com/maps?q=${lat},${lng}`;
    const result = await sendToTokens(profile.familyFcmTokens, {
      title: `Vị trí người quét mã QR của ${profile.fullName || "hồ sơ"}`,
      body: "Nhấn để xem vị trí trên bản đồ.",
      data: { type: "qr_scan_location", profileId: String(profile._id), mapsUrl },
    });
    res.json({ sent: result.sent });
  } catch (err) {
    console.error("[public-profile.scan-location]", err);
    res.status(500).json({ error: "Không thể gửi vị trí" });
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
