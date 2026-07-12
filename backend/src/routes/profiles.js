import { Router } from "express";
import crypto from "crypto";
import Profile from "../models/Profile.js";
import Subscription from "../models/Subscription.js";
import User from "../models/User.js";
import { sendToTokens } from "../lib/fcm.js";

const router = Router();

const BASE_FREE_PROFILES = 5;
const TRIAL_DAYS = 30;

router.get("/", async (req, res) => {
  const filter = {};
  if (req.query.userId) filter.userId = String(req.query.userId);
  const profiles = await Profile.find(filter);
  res.json(profiles.map((p) => p.toJSON()));
});

router.get("/:id", async (req, res) => {
  const p = await Profile.findById(req.params.id).catch(() => null);
  if (!p) return res.status(404).json({ error: "Không tìm thấy hồ sơ" });
  res.json(p.toJSON());
});

router.post("/", async (req, res) => {
  try {
    const userId = req.body?.userId;
    if (!userId) return res.status(400).json({ error: "userId là bắt buộc" });

    // Admins are exempt from the maintenance-subscription system entirely —
    // no trial, no slot limit, no freezing. This is a paywall for paying
    // customers, not something that should ever block internal/admin use.
    const user = await User.findById(userId).catch(() => null);
    const isAdmin = user?.role === "admin";

    if (!isAdmin) {
      const count = await Profile.countDocuments({ userId });
      let sub = await Subscription.findOne({ userId });

      if (!sub) {
        // First profile ever for this user — start the 30-day free trial of
        // the "gói duy trì lưu trữ hồ sơ" (profile-storage maintenance plan).
        const now = Date.now();
        sub = await Subscription.create({
          userId,
          status: "trial",
          plan: "trial",
          periodStart: now,
          periodEnd: now + TRIAL_DAYS * 24 * 60 * 60 * 1000,
          history: [{ event: "trial_started", plan: "trial", at: now }],
        });
      } else if ((sub.status === "trial" || sub.status === "active") && Date.now() > sub.periodEnd) {
        // Lazy expiry check, same as GET /subscriptions.
        sub.status = "expired";
        sub.history.push({ event: "expired", plan: sub.plan, at: Date.now() });
        await sub.save();
        await Profile.updateMany({ userId }, { subscriptionFrozen: true });
      }

      // Effective slot limit: base 5, plus any extra profiles purchased via the
      // flexible plan — but only while the subscription is actually active
      // (an expired/cancelled plan loses the extra slots, and blocks creation
      // outright until renewed, regardless of count).
      const isBlocked = sub.status === "expired" || sub.status === "cancelled";
      const totalSlots = BASE_FREE_PROFILES + (sub.status === "active" ? sub.extraProfiles : 0);

      if (isBlocked || count >= totalSlots) {
        return res.status(403).json({
          error: isBlocked
            ? "Gói duy trì lưu trữ hồ sơ đã hết hạn. Vui lòng gia hạn để tạo thêm hồ sơ."
            : "Bạn đã đạt giới hạn hồ sơ hiện tại. Nâng cấp gói duy trì để thêm hồ sơ.",
          needsSubscription: true,
        });
      }
    }

    const p = await Profile.create(req.body);
    res.status(201).json(p.toJSON());
  } catch (err) {
    console.error("[profiles.create]", err);
    res.status(500).json({ error: "Không thể tạo hồ sơ" });
  }
});

router.put("/:id", async (req, res) => {
  const p = await Profile.findByIdAndUpdate(req.params.id, req.body, { new: true }).catch(() => null);
  if (!p) return res.status(404).json({ error: "Không tìm thấy hồ sơ" });
  res.json(p.toJSON());
});

router.delete("/:id", async (req, res) => {
  const p = await Profile.findByIdAndDelete(req.params.id).catch(() => null);
  if (!p) return res.status(404).json({ error: "Không tìm thấy hồ sơ" });
  res.json(p.toJSON());
});

// ─── Family notification device registration (subscription perk) ───────────
// A family member opens a share link from the profile owner, taps "register",
// and their device's FCM token gets attached here. From then on, while the
// owner's subscription is active, that device gets a push every time this
// profile's QR is scanned — plus a link that shows the FULL profile
// regardless of the owner's privacy/freeze settings, via familyAccessToken.

router.post("/:id/family-register", async (req, res) => {
  try {
    const { fcmToken } = req.body;
    if (!fcmToken) return res.status(400).json({ error: "fcmToken là bắt buộc" });
    const p = await Profile.findById(req.params.id).catch(() => null);
    if (!p) return res.status(404).json({ error: "Không tìm thấy hồ sơ" });

    if (!p.familyAccessToken) {
      p.familyAccessToken = crypto.randomBytes(24).toString("hex");
    }
    if (!p.familyFcmTokens.includes(fcmToken)) {
      p.familyFcmTokens.push(fcmToken);
    }
    await p.save();
    res.json({ familyAccessToken: p.familyAccessToken });
  } catch (err) {
    console.error("[profiles.family-register]", err);
    res.status(500).json({ error: "Không thể đăng ký thiết bị" });
  }
});

router.post("/:id/family-unregister", async (req, res) => {
  try {
    const { fcmToken } = req.body;
    const p = await Profile.findById(req.params.id).catch(() => null);
    if (!p) return res.status(404).json({ error: "Không tìm thấy hồ sơ" });
    p.familyFcmTokens = (p.familyFcmTokens || []).filter((t) => t !== fcmToken);
    await p.save();
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: "Không thể hủy đăng ký thiết bị" });
  }
});

// POST /profiles/:id/family-test-notify — sends an immediate test push to
// every device registered for this profile, bypassing the "someone scanned"
// trigger and the subscription-active gate entirely. Built purely for
// diagnosing setup problems: the JSON response tells you exactly which half
// is broken (see the `disabled` / `sent` / `tokenCount` fields), instead of
// having to guess from a real scan or dig through server logs.
router.post("/:id/family-test-notify", async (req, res) => {
  try {
    const p = await Profile.findById(req.params.id).catch(() => null);
    if (!p) return res.status(404).json({ error: "Không tìm thấy hồ sơ" });
    const tokenCount = (p.familyFcmTokens || []).length;
    if (tokenCount === 0) {
      return res.json({ tokenCount: 0, sent: 0, disabled: false, note: "Chưa có thiết bị nào đăng ký cho hồ sơ này." });
    }
    const result = await sendToTokens(p.familyFcmTokens, {
      title: `[TEST] Thông báo thử cho ${p.fullName || "hồ sơ"}`,
      body: "Đây là thông báo thử nghiệm — nếu bạn nhận được, Firebase đã được cấu hình đúng.",
      data: { type: "test" },
    });
    res.json({ tokenCount, ...result });
  } catch (err) {
    res.status(500).json({ error: "Không thể gửi thông báo thử" });
  }
});

export default router;
