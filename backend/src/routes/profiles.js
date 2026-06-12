import { Router } from "express";
import Profile from "../models/Profile.js";

const router = Router();

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

    // Enforce 5-profile cap server-side too — the Android client already
    // checks but we don't want a malicious or buggy client bypassing it.
    const count = await Profile.countDocuments({ userId });
    if (count >= 5) return res.status(403).json({ error: "Bạn đã đạt giới hạn 5 hồ sơ" });

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
