import jwt from "jsonwebtoken";

const SECRET = process.env.JWT_SECRET || "dev-secret";
const EXPIRES_IN = process.env.JWT_EXPIRES_IN || "30d";

export function signToken(user) {
  return jwt.sign(
    { sub: user.id, role: user.role, email: user.email },
    SECRET,
    { expiresIn: EXPIRES_IN }
  );
}

/**
 * Express middleware that validates the Authorization: Bearer header.
 * On success it attaches `req.auth = { sub, role, email }`.
 * Routes can call `requireAuth` (any logged-in user) or `requireAdmin`.
 */
export function requireAuth(req, res, next) {
  const header = req.headers.authorization || "";
  const match = header.match(/^Bearer\s+(.+)$/i);
  if (!match) return res.status(401).json({ error: "Missing Authorization header" });
  try {
    req.auth = jwt.verify(match[1], SECRET);
    next();
  } catch {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
}

export function requireAdmin(req, res, next) {
  requireAuth(req, res, () => {
    if (req.auth.role !== "admin") return res.status(403).json({ error: "Admin only" });
    next();
  });
}
