import { Router } from "express";
import QrTag from "../models/QrTag.js";

const router = Router();

router.get("/", async (req, res) => {
  const filter = {};
  if (req.query.tagCode)   filter.tagCode   = String(req.query.tagCode).toUpperCase();
  if (req.query.profileId) filter.profileId = String(req.query.profileId);
  if (req.query.orderId)   filter.orderId   = String(req.query.orderId);
  const tags = await QrTag.find(filter);
  res.json(tags.map((t) => t.toJSON()));
});

router.get("/:id", async (req, res) => {
  const t = await QrTag.findById(req.params.id).catch(() => null);
  if (!t) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(t.toJSON());
});

router.post("/", async (req, res) => {
  try {
    const t = await QrTag.create(req.body);
    res.status(201).json(t.toJSON());
  } catch (err) {
    if (err.code === 11000) return res.status(409).json({ error: "Mã tag đã tồn tại" });
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", async (req, res) => {
  const t = await QrTag.findByIdAndUpdate(req.params.id, req.body, { new: true }).catch(() => null);
  if (!t) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(t.toJSON());
});

export default router;
