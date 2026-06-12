import { Router } from "express";
import Product from "../models/Product.js";

const router = Router();

router.get("/", async (req, res) => {
  const filter = {};
  if (req.query.slug) filter.slug = String(req.query.slug);
  if (req.query.category) filter.category = String(req.query.category);
  const products = await Product.find(filter);
  res.json(products.map((p) => p.toJSON()));
});

router.get("/:id", async (req, res) => {
  const p = await Product.findById(req.params.id).catch(() => null);
  if (!p) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(p.toJSON());
});

router.post("/", async (req, res) => {
  try {
    const p = await Product.create(req.body);
    res.status(201).json(p.toJSON());
  } catch (err) {
    if (err.code === 11000) return res.status(409).json({ error: "Slug đã tồn tại" });
    res.status(500).json({ error: err.message });
  }
});

router.put("/:id", async (req, res) => {
  const p = await Product.findByIdAndUpdate(req.params.id, req.body, { new: true }).catch(() => null);
  if (!p) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(p.toJSON());
});

export default router;
