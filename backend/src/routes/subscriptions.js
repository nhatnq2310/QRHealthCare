import { Router } from "express";
import Subscription from "../models/Subscription.js";
import Profile from "../models/Profile.js";
import Order from "../models/Order.js";
import QrTag from "../models/QrTag.js";

const router = Router();

function randomTagCode() {
  const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  let s = "QRH-";
  for (let i = 0; i < 6; i++) s += chars[Math.floor(Math.random() * chars.length)];
  return s;
}
function randomPin() {
  return String(Math.floor(1000 + Math.random() * 9000));
}

const DAY = 24 * 60 * 60 * 1000;
const TRIAL_DAYS = 30;
const MONTHLY_BASE = 20000;      // 20k / 30 days
const YEARLY_BASE = 199000;      // 199k / 365 days
const EXTRA_PROFILE_PRICE = 5000;        // monthly & flexible: +5k / profile / month
const EXTRA_PROFILE_PRICE_YEARLY = 4000; // yearly: discounted rate, +4k/month-equivalent x12 = 48k/profile/year

// Total price for a plan + a given number of extra profile slots.
// Exported via query endpoint below so the Android client can double check
// the number it renders, but the client also computes this locally for the
// live total shown while the user types.
// NOTE: yearly gets a discounted extra-profile rate (4k/month-equivalent,
// billed as 12 months upfront = 48k/profile/year) vs. monthly/flexible's
// plain 5k/profile/month — this rewards committing to the yearly plan.
function computeAmount(plan, extraProfiles) {
  const extra = Math.max(0, Number(extraProfiles) || 0);
  if (plan === "yearly") return YEARLY_BASE + extra * EXTRA_PROFILE_PRICE_YEARLY * 12;
  return MONTHLY_BASE + extra * EXTRA_PROFILE_PRICE;
}

function periodLengthMs(plan) {
  return plan === "yearly" ? 365 * DAY : 30 * DAY;
}

// Lazily bring a subscription's status up to date — no cron needed. Called
// on every read. If a paid/trial period has lapsed, mark expired and freeze
// every profile the user owns (privatizes them on the public QR page).
async function reconcile(sub) {
  const now = Date.now();
  if ((sub.status === "trial" || sub.status === "active") && now > sub.periodEnd) {
    sub.status = "expired";
    sub.history.push({ event: "expired", plan: sub.plan, at: now });
    sub.updatedAt = now;
    await sub.save();
    await Profile.updateMany({ userId: sub.userId }, { subscriptionFrozen: true });
  }
  return sub;
}

// GET /subscriptions?userId=xxx
router.get("/", async (req, res) => {
  const userId = String(req.query.userId || "");
  if (!userId) return res.status(400).json({ error: "userId là bắt buộc" });
  let sub = await Subscription.findOne({ userId });
  if (!sub) return res.json(null); // no profiles created yet -> no trial started
  sub = await reconcile(sub);
  res.json(sub.toJSON());
});

// POST /subscriptions/renew — pay for/extend the maintenance plan.
// body: { userId, plan: "monthly"|"flexible"|"yearly", extraProfiles, paymentRef }
router.post("/renew", async (req, res) => {
  try {
    const { userId, plan, paymentRef } = req.body;
    const extraProfiles = Math.max(0, Number(req.body.extraProfiles) || 0);
    if (!userId) return res.status(400).json({ error: "userId là bắt buộc" });
    if (!["monthly", "flexible", "yearly"].includes(plan)) {
      return res.status(400).json({ error: "Gói không hợp lệ" });
    }

    const now = Date.now();
    const amount = computeAmount(plan, extraProfiles);
    let sub = await Subscription.findOne({ userId });
    const wasNeverPaid = !sub || sub.status === "trial" || sub.status === "expired" || sub.status === "cancelled";

    if (!sub) {
      sub = new Subscription({ userId });
    }
    sub.status = "active";
    sub.plan = plan;
    sub.extraProfiles = extraProfiles;
    sub.periodStart = now;
    sub.periodEnd = now + periodLengthMs(plan);
    sub.lastAmount = amount;
    sub.paymentRef = paymentRef || "";
    sub.history.push({
      event: wasNeverPaid ? "subscribed" : "renewed",
      plan, amount, extraProfiles, at: now,
    });
    sub.updatedAt = now;
    await sub.save();

    // Paid up -> unfreeze everything again.
    await Profile.updateMany({ userId }, { subscriptionFrozen: false });

    // First-ever paid month promo: +1 free physical QR tag, free shipping.
    // Only fires on a genuine first conversion (trial/expired/cancelled -> active),
    // never on an ordinary renewal of an already-active plan.
    let promoTag = null;
    if (wasNeverPaid) {
      try {
        let tag = null;
        for (let attempt = 0; attempt < 5 && !tag; attempt++) {
          try {
            tag = await QrTag.create({ tagCode: randomTagCode(), pin: randomPin(), profileId: null, productType: "tag" });
          } catch (e) {
            if (e.code !== 11000) throw e; // retry only on tagCode collision
          }
        }
        if (tag) {
          const promoOrder = await Order.create({
            userId,
            items: [{ productId: "promo-free-tag", productName: "Tag Y Tế (Quà Tặng Đăng Ký Gói Đầu Tiên)", price: 0, quantity: 1 }],
            totalAmount: 0,
            paymentMethod: "promo",
            status: "pending",
            qrTagIds: [tag.id],
            isPromo: true,
            shippingFee: 0,
          });
          tag.orderId = promoOrder.id;
          await tag.save();
          promoTag = tag.toJSON();
        }
      } catch (err) {
        console.error("[subscriptions.renew] free-tag promo failed (non-fatal)", err);
      }
    }

    const result = sub.toJSON();
    if (promoTag) result.promoTag = promoTag;
    res.json(result);
  } catch (err) {
    console.error("[subscriptions.renew]", err);
    res.status(500).json({ error: "Không thể xử lý đăng ký" });
  }
});

// POST /subscriptions/cancel — body: { userId }
// Cancels immediately and freezes profiles right away (no grace period —
// "duy trì" means the moment you stop paying for maintenance, it stops).
router.post("/cancel", async (req, res) => {
  try {
    const userId = req.body?.userId;
    if (!userId) return res.status(400).json({ error: "userId là bắt buộc" });
    const sub = await Subscription.findOne({ userId });
    if (!sub) return res.status(404).json({ error: "Không tìm thấy gói đăng ký" });

    const now = Date.now();
    sub.status = "cancelled";
    sub.history.push({ event: "cancelled", plan: sub.plan, at: now });
    sub.updatedAt = now;
    await sub.save();
    await Profile.updateMany({ userId }, { subscriptionFrozen: true });

    res.json(sub.toJSON());
  } catch (err) {
    console.error("[subscriptions.cancel]", err);
    res.status(500).json({ error: "Không thể hủy đăng ký" });
  }
});

// GET /subscriptions/admin/stats — admin dashboard: subscribers & cancellations
// bucketed into week / month / year / lifetime, counted from history events
// across every user's subscription document.
router.get("/admin/stats", async (req, res) => {
  try {
    const all = await Subscription.find({});
    const now = Date.now();
    const buckets = { week: 7 * DAY, month: 30 * DAY, year: 365 * DAY };

    const stats = {
      subscribersWeek: 0, subscribersMonth: 0, subscribersYear: 0, subscribersLifetime: 0,
      cancellationsWeek: 0, cancellationsMonth: 0, cancellationsYear: 0, cancellationsLifetime: 0,
      activeCount: 0, trialCount: 0, expiredCount: 0, cancelledCount: 0,
    };

    for (const sub of all) {
      if (sub.status === "active") stats.activeCount++;
      else if (sub.status === "trial") stats.trialCount++;
      else if (sub.status === "expired") stats.expiredCount++;
      else if (sub.status === "cancelled") stats.cancelledCount++;

      for (const h of sub.history) {
        const age = now - h.at;
        if (h.event === "subscribed" || h.event === "renewed") {
          stats.subscribersLifetime++;
          if (age <= buckets.week) stats.subscribersWeek++;
          if (age <= buckets.month) stats.subscribersMonth++;
          if (age <= buckets.year) stats.subscribersYear++;
        } else if (h.event === "cancelled") {
          stats.cancellationsLifetime++;
          if (age <= buckets.week) stats.cancellationsWeek++;
          if (age <= buckets.month) stats.cancellationsMonth++;
          if (age <= buckets.year) stats.cancellationsYear++;
        }
      }
    }

    res.json(stats);
  } catch (err) {
    console.error("[subscriptions.admin.stats]", err);
    res.status(500).json({ error: "Không thể tải thống kê đăng ký" });
  }
});

export default router;
