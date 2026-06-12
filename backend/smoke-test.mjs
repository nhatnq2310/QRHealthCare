// Smoke test — spins up an in-memory MongoDB, starts the server,
// exercises register -> login -> create profile -> list profiles ->
// link a QR tag -> public profile lookup, then exits.

import { MongoMemoryServer } from "mongodb-memory-server";
import { spawn } from "node:child_process";

const mongod = await MongoMemoryServer.create();
const uri = mongod.getUri();
console.log("smoke: mongo at", uri);

const env = {
  ...process.env,
  MONGODB_URI: uri + "qrhealthcare_smoke",
  JWT_SECRET: "smoke-secret",
  PORT: "4555",
  CORS_ORIGIN: "*",
};

const proc = spawn("node", ["server.js"], { env, stdio: ["ignore", "pipe", "pipe"] });
proc.stdout.on("data", (b) => process.stdout.write("[srv] " + b));
proc.stderr.on("data", (b) => process.stderr.write("[srv!] " + b));

// Wait for the "ready" log line
await new Promise((resolve, reject) => {
  const t = setTimeout(() => reject(new Error("server didn't start in 10s")), 10000);
  proc.stdout.on("data", (b) => { if (b.toString().includes("ready")) { clearTimeout(t); resolve(); } });
});

const BASE = "http://127.0.0.1:4555/api/v1";

async function req(path, opts = {}) {
  const r = await fetch(BASE + path, {
    ...opts,
    headers: { "Content-Type": "application/json", ...(opts.headers || {}) },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
  });
  const text = await r.text();
  let body; try { body = JSON.parse(text); } catch { body = text; }
  return { status: r.status, body };
}

let fails = 0;
function check(label, cond, detail = "") {
  if (cond) console.log("  ✓", label);
  else { console.log("  ✗", label, detail); fails++; }
}

console.log("\n→ register");
const reg = await req("/auth/register", { method: "POST", body: { email: "a@b.com", password: "secret1", fullName: "Test User" } });
check("register status 201", reg.status === 201, `got ${reg.status}`);
check("returns user.id", !!reg.body.user?.id);
check("returns token", typeof reg.body.token === "string" && reg.body.token.length > 20);
check("no passwordHash leaked", !("passwordHash" in (reg.body.user || {})));
const { user, token } = reg.body;

console.log("\n→ login");
const login = await req("/auth/login", { method: "POST", body: { email: "a@b.com", password: "secret1" } });
check("login status 200", login.status === 200, `got ${login.status}`);
check("login returns token", typeof login.body.token === "string");

console.log("\n→ wrong password");
const wrong = await req("/auth/login", { method: "POST", body: { email: "a@b.com", password: "WRONG" } });
check("wrong password is 401", wrong.status === 401);

console.log("\n→ create profile");
const profRes = await req("/profiles", { method: "POST", body: {
  userId: user.id, profileType: "human", fullName: "Test User", gender: "Nam", bloodGroup: "O+",
  emergencyContacts: [{ name: "Mom", phone: "0900000000", relationship: "Mother" }],
}});
check("profile status 201", profRes.status === 201, `got ${profRes.status}`);
check("profile has id", !!profRes.body.id);
check("profile keeps nested array", profRes.body.emergencyContacts?.length === 1);

console.log("\n→ list profiles by userId");
const list = await req(`/profiles?userId=${user.id}`);
check("list status 200", list.status === 200);
check("list has 1 entry", Array.isArray(list.body) && list.body.length === 1);

console.log("\n→ create + link QR tag");
const tag = await req("/qrtags", { method: "POST", body: { tagCode: "QRH-TEST", pin: "1234", productType: "sticker" }});
check("tag status 201", tag.status === 201);
const linked = await req(`/qrtags/${tag.body.id}`, { method: "PUT", body: { ...tag.body, profileId: profRes.body.id }});
check("tag link status 200", linked.status === 200);
check("tag now has profileId", linked.body.profileId === profRes.body.id);

console.log("\n→ public lookup via tagCode");
const lookup = await req("/qrtags?tagCode=QRH-TEST");
check("lookup returns array of 1", Array.isArray(lookup.body) && lookup.body.length === 1);

console.log("\n→ duplicate email rejected");
const dupe = await req("/auth/register", { method: "POST", body: { email: "a@b.com", password: "another1" }});
check("dupe email is 409", dupe.status === 409);

console.log("\n→ 5-profile cap");
for (let i = 0; i < 4; i++) {
  await req("/profiles", { method: "POST", body: { userId: user.id, fullName: `P${i}` }});
}
const overflow = await req("/profiles", { method: "POST", body: { userId: user.id, fullName: "P5" }});
check("6th profile is 403", overflow.status === 403, `got ${overflow.status}`);

console.log(`\n${fails === 0 ? "✓ all checks passed" : "✗ " + fails + " check(s) failed"}`);

proc.kill("SIGTERM");
await mongod.stop();
process.exit(fails);
