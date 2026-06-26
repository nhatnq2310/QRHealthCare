import { Router } from "express";
import bcrypt from "bcryptjs";
import User from "../models/User.js";
import { signToken } from "../auth.js";

const router = Router();

/**
 * POST /api/v1/auth/register
 * Body: { email, password, fullName, role? }
 * Returns: { user, token }
 *
 * `role` is accepted from the body so the seeder can create the admin
 * account in one call. In production you would gate this server-side
 * (e.g. only allow promoting to admin if the caller is already admin).
 */
router.post("/register", async (req, res) => {
  try {
    const { email, password, fullName = "", role = "user" } = req.body || {};
    if (!email || !password) return res.status(400).json({ error: "email và password là bắt buộc" });
    if (password.length < 6)   return res.status(400).json({ error: "Mật khẩu phải có ít nhất 6 ký tự" });

    const existing = await User.findOne({ email: email.toLowerCase().trim() });
    if (existing) return res.status(409).json({ error: "Email đã được sử dụng" });

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await User.create({
      email: email.toLowerCase().trim(),
      passwordHash,
      fullName,
      role: role === "admin" ? "admin" : "user",
    });

    const token = signToken({ id: user.id, role: user.role, email: user.email });
    res.status(201).json({ user: user.toJSON(), token });
  } catch (err) {
    console.error("[auth.register]", err);
    res.status(500).json({ error: "Lỗi máy chủ" });
  }
});

/**
 * POST /api/v1/auth/login
 * Body: { email, password }
 * Returns: { user, token } — or 401 with { error }
 */
router.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) return res.status(400).json({ error: "email và password là bắt buộc" });

    const user = await User.findOne({ email: email.toLowerCase().trim() });
    if (!user) return res.status(401).json({ error: "Email không tồn tại" });

    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ error: "Mật khẩu không đúng" });

    const token = signToken({ id: user.id, role: user.role, email: user.email });
    res.json({ user: user.toJSON(), token });
  } catch (err) {
    console.error("[auth.login]", err);
    res.status(500).json({ error: "Lỗi máy chủ" });
  }
});

/**
 * POST /api/v1/auth/reset-password
 * Body: { email, fullName, newPassword }
 *
 * Email-less password reset for the MVP: the user proves ownership by
 * supplying the email AND the exact full name on the account. This is a
 * lightweight verification (not as strong as an emailed token) — fine for a
 * prototype, but should be replaced with emailed reset links before a real
 * public launch.
 */
router.post("/reset-password", async (req, res) => {
  try {
    const { email, fullName = "", newPassword } = req.body || {};
    if (!email || !newPassword) return res.status(400).json({ error: "email và mật khẩu mới là bắt buộc" });
    if (newPassword.length < 6) return res.status(400).json({ error: "Mật khẩu phải có ít nhất 6 ký tự" });

    const user = await User.findOne({ email: email.toLowerCase().trim() });
    if (!user) return res.status(404).json({ error: "Email không tồn tại" });

    // Verify identity by matching the full name (case-insensitive, trimmed).
    const given = fullName.trim().toLowerCase();
    const actual = (user.fullName || "").trim().toLowerCase();
    if (!actual || given !== actual) {
      return res.status(401).json({ error: "Họ tên không khớp với tài khoản này" });
    }

    user.passwordHash = await bcrypt.hash(newPassword, 10);
    await user.save();
    res.json({ ok: true, message: "Đặt lại mật khẩu thành công" });
  } catch (err) {
    console.error("[auth.reset-password]", err);
    res.status(500).json({ error: "Lỗi máy chủ" });
  }
});

/**
 * POST /api/v1/auth/change-password
 * Body: { email, currentPassword, newPassword }
 *
 * For logged-in users changing their own password. Verifies the current
 * password before applying the new one.
 */
router.post("/change-password", async (req, res) => {
  try {
    const { email, currentPassword, newPassword } = req.body || {};
    if (!email || !currentPassword || !newPassword)
      return res.status(400).json({ error: "Thiếu thông tin bắt buộc" });
    if (newPassword.length < 6) return res.status(400).json({ error: "Mật khẩu mới phải có ít nhất 6 ký tự" });

    const user = await User.findOne({ email: email.toLowerCase().trim() });
    if (!user) return res.status(404).json({ error: "Email không tồn tại" });

    const ok = await bcrypt.compare(currentPassword, user.passwordHash);
    if (!ok) return res.status(401).json({ error: "Mật khẩu hiện tại không đúng" });

    user.passwordHash = await bcrypt.hash(newPassword, 10);
    await user.save();
    res.json({ ok: true, message: "Đổi mật khẩu thành công" });
  } catch (err) {
    console.error("[auth.change-password]", err);
    res.status(500).json({ error: "Lỗi máy chủ" });
  }
});

export default router;
