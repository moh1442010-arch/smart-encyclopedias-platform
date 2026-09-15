const ALLOW_METHODS = "GET, POST, OPTIONS";
const MAX_TTL_DAYS = 30;
const DEFAULT_MAX_DOWNLOADS = 3;

function origin(env) { return env.PUBLIC_ORIGIN || "https://moh1442010-arch.github.io/smart-encyclopedias-platform"; }
function headers(env, extra = {}) {
  return { "access-control-allow-origin": origin(env), "access-control-allow-methods": ALLOW_METHODS, "access-control-allow-headers": "content-type, authorization, x-admin-key", "vary": "Origin", ...extra };
}
function json(data, status, env) { return new Response(JSON.stringify(data), { status, headers: headers(env, { "content-type": "application/json; charset=utf-8" }) }); }
function preflight(env) { return new Response(null, { status: 204, headers: headers(env) }); }
function randomToken() { return `${crypto.randomUUID()}${crypto.randomUUID().replaceAll("-", "")}`; }
async function sha256(text) {
  const bytes = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, "0")).join("");
}
function validEmail(v) { return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(v || "")); }
function safeId(v) { return /^[A-Za-z0-9_-]{6,80}$/.test(String(v || "")); }
function clampInt(v, min, max, fallback) { const n = Number.parseInt(v, 10); return Number.isInteger(n) ? Math.min(max, Math.max(min, n)) : fallback; }
function isAdmin(request, env) { const expected = env.LICENSE_ADMIN_KEY; const supplied = request.headers.get("x-admin-key") || ""; return Boolean(expected && supplied && supplied === expected); }

async function createLicense(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  const body = await request.json().catch(() => null);
  if (!body || !safeId(body.licenseId) || !String(body.customerName || "").trim() || !validEmail(body.customerEmail) || !String(body.objectKey || "").trim()) return json({ ok: false, error: "invalid_license_data" }, 400, env);
  const days = clampInt(body.expiresDays, 1, MAX_TTL_DAYS, 7);
  const maxDownloads = clampInt(body.maxDownloads, 1, 10, DEFAULT_MAX_DOWNLOADS);
  const token = randomToken();
  const tokenHash = await sha256(token);
  const expiresAt = new Date(Date.now() + days * 86400000).toISOString();
  try {
    await env.DB.prepare(`INSERT INTO licenses (id, order_id, customer_name, customer_email, object_key, token_hash, expires_at, max_downloads) VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind(body.licenseId, body.orderId || null, String(body.customerName).trim(), String(body.customerEmail).trim().toLowerCase(), String(body.objectKey).trim(), tokenHash, expiresAt, maxDownloads).run();
  } catch (e) { return json({ ok: false, error: "license_create_failed" }, 409, env); }
  return json({ ok: true, licenseId: body.licenseId, expiresAt, maxDownloads, token, deliveryUrl: `${origin(env)}/delivery.html?token=${encodeURIComponent(token)}` }, 201, env);
}

async function lookup(token, env) {
  if (!token || token.length < 30) return null;
  const tokenHash = await sha256(token);
  return env.DB.prepare(`SELECT id, order_id, customer_name, customer_email, object_key, status, expires_at, max_downloads, download_count, last_download_at FROM licenses WHERE token_hash = ?`).bind(tokenHash).first();
}
function active(row) { return row && row.status === "active" && new Date(row.expires_at).getTime() > Date.now() && Number(row.download_count) < Number(row.max_downloads); }

async function info(request, env, token) {
  const row = await lookup(token, env);
  if (!row) return json({ ok: false, error: "invalid_or_expired_link" }, 404, env);
  return json({ ok: true, licenseId: row.id, buyer: row.customer_name, expiresAt: row.expires_at, downloadsUsed: Number(row.download_count), downloadsRemaining: Math.max(0, Number(row.max_downloads) - Number(row.download_count)), active: active(row) }, 200, env);
}

async function download(request, env, token) {
  const row = await lookup(token, env);
  if (!row || !active(row)) return json({ ok: false, error: "link_expired_or_limit_reached" }, 403, env);
  const object = await env.PAID_BOOKS.get(row.object_key);
  if (!object) return json({ ok: false, error: "file_not_found" }, 404, env);
  await env.DB.prepare(`UPDATE licenses SET download_count = download_count + 1, last_download_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'active' AND download_count < max_downloads`).bind(row.id).run();
  const h = headers(env, { "content-type": "application/pdf", "content-length": String(object.size), "content-disposition": `attachment; filename="encyclopedia-${row.id}.pdf"`, "cache-control": "private, no-store, max-age=0", "x-content-type-options": "nosniff" });
  return new Response(object.body, { status: 200, headers: h });
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return preflight(env);
    const url = new URL(request.url);
    try {
      if (url.pathname === "/api/license/create" && request.method === "POST") return createLicense(request, env);
      if (url.pathname === "/api/delivery/info" && request.method === "GET") return info(request, env, url.searchParams.get("token"));
      if (url.pathname === "/api/delivery/download" && request.method === "GET") return download(request, env, url.searchParams.get("token"));
      return json({ ok: false, error: "not_found" }, 404, env);
    } catch (e) { return json({ ok: false, error: "server_error" }, 500, env); }
  }
};
