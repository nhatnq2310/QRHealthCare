import { Router } from "express";
import Coupon from "../models/Coupon.js";

const router = Router();

/**
 * POST /coupons/validate
 * Body: { code: string, subtotal: number }
 * Returns 200 with either:
 *   - { valid: true, coupon, discountAmount, finalAmount }
 *   - { valid: false, error: "<reason>" }
 *
 * We always return 200 so the Android client only has to parse the JSON
 * shape rather than juggle HTTP error codes. The `valid` boolean is the
 * single source of truth on the client side.
 */
router.post("/validate", async (req, res) => {
  try {
    const { code, subtotal } = req.body || {};
    if (!code || typeof code !== "string") {
      return res.json({ valid: false, error: "Vui lòng nhập mã giảm giá" });
    }
    const sub = Number(subtotal) || 0;

    const coupon = await Coupon.findOne({ code: code.toUpperCase().trim() });
    if (!coupon) return res.json({ valid: false, error: "Mã giảm giá không tồn tại" });
    if (!coupon.active) return res.json({ valid: false, error: "Mã giảm giá đã ngừng hoạt động" });
    if (coupon.expiresAt && coupon.expiresAt < Date.now()) {
      return res.json({ valid: false, error: "Mã giảm giá đã hết hạn" });
    }
    if (coupon.usageLimit != null && coupon.usageCount >= coupon.usageLimit) {
      return res.json({ valid: false, error: "Mã giảm giá đã hết lượt sử dụng" });
    }
    if (sub < coupon.minOrderAmount) {
      const need = coupon.minOrderAmount.toLocaleString("vi-VN");
      return res.json({ valid: false, error: `Đơn hàng tối thiểu ${need}đ` });
    }

    // Discount calculation — never goes below zero, percent discounts capped if maxDiscount set.
    let discountAmount = coupon.discountType === "percent"
      ? Math.floor((sub * coupon.discountValue) / 100)
      : coupon.discountValue;
    if (coupon.maxDiscount != null) discountAmount = Math.min(discountAmount, coupon.maxDiscount);
    discountAmount = Math.min(discountAmount, sub);
    const finalAmount = Math.max(0, sub - discountAmount);

    res.json({ valid: true, coupon: coupon.toJSON(), discountAmount, finalAmount });
  } catch (err) {
    console.error("[coupons.validate]", err);
    res.status(500).json({ valid: false, error: "Lỗi máy chủ" });
  }
});

// ── Admin / utility routes ───────────────────────────────────────────────────

router.get("/", async (req, res) => {
  const list = await Coupon.find().sort({ createdAt: -1 });
  res.json(list.map((c) => c.toJSON()));
});

router.post("/", async (req, res) => {
  try {
    // Normalise the code to uppercase before insert so lookups are case-insensitive.
    const body = { ...req.body, code: String(req.body?.code || "").toUpperCase().trim() };
    const c = await Coupon.create(body);
    res.status(201).json(c.toJSON());
  } catch (err) {
    if (err.code === 11000) return res.status(409).json({ error: "Mã giảm giá đã tồn tại" });
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", async (req, res) => {
  const c = await Coupon.findByIdAndUpdate(req.params.id, req.body, { new: true }).catch(() => null);
  if (!c) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(c.toJSON());
});

export default router;
