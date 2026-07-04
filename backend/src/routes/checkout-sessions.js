import { Router } from "express";
import CheckoutSession from "../models/CheckoutSession.js";

const router = Router();

// GET /checkout-sessions — admin use, powers the drop-out / abandonment report.
// Optional ?since=<epoch ms> to limit the window (matches the admin dashboard's
// day/week/month tiles).
router.get("/", async (req, res) => {
  const filter = {};
  if (req.query.since) filter.startedAt = { $gte: Number(req.query.since) };
  const sessions = await CheckoutSession.find(filter).sort({ startedAt: -1 });
  res.json(sessions.map((s) => s.toJSON()));
});

// POST /checkout-sessions — called once, the moment a user opens the
// shipping-info screen (i.e. "checkout started"). Returns the session id so
// the client can PATCH it as the user progresses.
router.post("/", async (req, res) => {
  try {
    const s = await CheckoutSession.create({
      userId: req.body.userId || "",
      cartValue: req.body.cartValue || 0,
      itemCount: req.body.itemCount || 0,
      step: 1,
    });
    res.status(201).json(s.toJSON());
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// PATCH /checkout-sessions/:id — called whenever the user reaches a further
// step, and once more (with completed:true + orderId) right after the order
// is placed. `step` should only ever move forward; we take the max.
router.patch("/:id", async (req, res) => {
  try {
    const existing = await CheckoutSession.findById(req.params.id);
    if (!existing) return res.status(404).json({ error: "Không tìm thấy" });

    const nextStep = Math.max(existing.step, Number(req.body.step) || existing.step);
    existing.step = nextStep;
    if (req.body.paymentMethod) existing.paymentMethod = req.body.paymentMethod;
    if (req.body.completed) existing.completed = true;
    if (req.body.orderId) existing.orderId = req.body.orderId;
    existing.updatedAt = Date.now();
    await existing.save();

    res.json(existing.toJSON());
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

export default router;
