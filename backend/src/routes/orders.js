import { Router } from "express";
import Order from "../models/Order.js";

const router = Router();

router.get("/", async (req, res) => {
  const filter = {};
  if (req.query.userId) filter.userId = String(req.query.userId);
  const orders = await Order.find(filter).sort({ createdAt: -1 });
  res.json(orders.map((o) => o.toJSON()));
});

router.post("/", async (req, res) => {
  try {
    const o = await Order.create(req.body);
    res.status(201).json(o.toJSON());
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", async (req, res) => {
  const o = await Order.findByIdAndUpdate(req.params.id, req.body, { new: true }).catch(() => null);
  if (!o) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(o.toJSON());
});

export default router;
