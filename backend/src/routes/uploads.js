import { Router } from "express";
import multer from "multer";
import path from "node:path";
import fs from "node:fs";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
// Files land in backend/uploads/ — relative to the project root.
const UPLOAD_DIR = path.resolve(__dirname, "../../uploads");
if (!fs.existsSync(UPLOAD_DIR)) fs.mkdirSync(UPLOAD_DIR, { recursive: true });

const storage = multer.diskStorage({
  destination: (_req, _file, cb) => cb(null, UPLOAD_DIR),
  filename:    (_req, file, cb) => {
    // Random name + original extension. We don't trust the original name.
    const ext = (path.extname(file.originalname) || ".bin").toLowerCase().slice(0, 8);
    const id  = Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
    cb(null, id + ext);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 }, // 5 MB cap — health-doc photos are usually well under
  fileFilter: (_req, file, cb) => {
    const ok = /^image\/(jpe?g|png|webp|heic|heif)$/i.test(file.mimetype);
    if (!ok) return cb(new Error("Chỉ chấp nhận ảnh (JPG, PNG, WEBP, HEIC)"));
    cb(null, true);
  },
});

const router = Router();

/**
 * POST /uploads
 * multipart/form-data with field name "file".
 * Returns: { url: "/uploads/<filename>", filename, size, mimetype }
 *
 * The returned URL is RELATIVE — the Android client prepends the API host
 * to display the image. This is so the same file path works whether the
 * server is reached via 10.0.2.2, LAN IP, or a real domain.
 */
router.post("/", upload.single("file"), (req, res) => {
  if (!req.file) return res.status(400).json({ error: "Vui lòng chọn ảnh" });
  res.status(201).json({
    url: `/uploads/${req.file.filename}`,
    filename: req.file.filename,
    size: req.file.size,
    mimetype: req.file.mimetype,
  });
});

// Multer errors surface as plain Errors — turn them into nice JSON.
router.use((err, _req, res, _next) => {
  if (err instanceof multer.MulterError) {
    if (err.code === "LIMIT_FILE_SIZE") return res.status(413).json({ error: "Ảnh quá lớn (tối đa 5 MB)" });
    return res.status(400).json({ error: err.message });
  }
  if (err) return res.status(400).json({ error: err.message });
});

export default router;
