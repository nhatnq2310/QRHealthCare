import { Router } from "express";
import Profile from "../models/Profile.js";
import Subscription from "../models/Subscription.js";

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

export default router;
