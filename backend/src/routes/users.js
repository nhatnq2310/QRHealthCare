import { Router } from "express";
import User from "../models/User.js";

const router = Router();

// GET /users — admin-style: returns all users. Supports ?email= for the
// legacy lookup, kept so older Android builds still work during rollout.
router.get("/", async (req, res) => {
  const filter = {};
  if (req.query.email) filter.email = String(req.query.email).toLowerCase().trim();
  const users = await User.find(filter);
  res.json(users.map((u) => u.toJSON()));
});

router.get("/:id", async (req, res) => {
  const u = await User.findById(req.params.id).catch(() => null);
  if (!u) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(u.toJSON());
});

router.put("/:id", async (req, res) => {
  // Block direct password edits via this endpoint; force them through /auth.
  const { passwordHash, password, ...safe } = req.body || {};
  const u = await User.findByIdAndUpdate(req.params.id, safe, { new: true }).catch(() => null);
  if (!u) return res.status(404).json({ error: "Không tìm thấy" });
  res.json(u.toJSON());
});

export default router;
