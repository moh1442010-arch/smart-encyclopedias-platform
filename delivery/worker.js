const ALLOW_METHODS = "GET, POST, PUT, OPTIONS";
const MAX_TTL_DAYS = 30;
const DEFAULT_MAX_DOWNLOADS = 3;
const BOOK_KEY = "paid/encyclopedia-250-pages.pdf";

function origin(env) { return env.PUBLIC_ORIGIN || "https://moh1442010-arch.github.io/smart-encyclopedias-platform"; }
function headers(env, extra = {}) {
  return { "access-control-allow-origin": origin(env), "access-control-allow-methods": ALLOW_METHODS, "access-control-allow-headers": "content-type, authorization, x-admin-key, x-device-id", "vary": "Origin", ...extra };
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
function deviceId(request) { return String(request.headers.get("x-device-id") || "").trim(); }

async function createLicense(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "payment_approval_required" }, 403, env);
  const body = await request.json().catch(() => null);
  if (!body || body.approved !== true || !safeId(body.licenseId) || !String(body.customerName || "").trim() || !validEmail(body.customerEmail) || String(body.objectKey || BOOK_KEY).trim() !== BOOK_KEY) {
    return json({ ok: false, error: "explicit_payment_approval_required" }, 400, env);
  }
  const days = clampInt(body.expiresDays, 1, MAX_TTL_DAYS, 7);
  const maxDownloads = clampInt(body.maxDownloads, 1, 10, DEFAULT_MAX_DOWNLOADS);
  const token = randomToken();
  const tokenHash = await sha256(token);
  const expiresAt = new Date(Date.now() + days * 86400000).toISOString();
  const approvedBy = String(body.approvedBy || "owner").trim().slice(0, 120);
  try {
    await env.DB.prepare(`INSERT INTO licenses (id, order_id, customer_name, customer_email, object_key, token_hash, expires_at, max_downloads, payment_verified_at, approved_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)`)
      .bind(body.licenseId, body.orderId || null, String(body.customerName).trim(), String(body.customerEmail).trim().toLowerCase(), BOOK_KEY, tokenHash, expiresAt, maxDownloads, approvedBy).run();
  } catch (e) { return json({ ok: false, error: "license_create_failed" }, 409, env); }
  return json({ ok: true, licenseId: body.licenseId, paymentApproved: true, expiresAt, maxDownloads, token, deliveryUrl: `${origin(env)}/delivery.html?token=${encodeURIComponent(token)}` }, 201, env);
}

async function lookup(token, env) {
  if (!token || token.length < 30) return null;
  const tokenHash = await sha256(token);
  return env.DB.prepare(`SELECT id, order_id, customer_name, customer_email, object_key, status, expires_at, max_downloads, download_count, last_download_at, payment_verified_at, approved_by, device_hash, device_bound_at FROM licenses WHERE token_hash = ?`).bind(tokenHash).first();
}
function active(row) { return row && row.status === "active" && row.payment_verified_at && new Date(row.expires_at).getTime() > Date.now() && Number(row.download_count) < Number(row.max_downloads); }

async function checkDevice(request, env, row, bindIfEmpty = false) {
  const id = deviceId(request);
  if (!id || id.length < 16 || id.length > 256) return { ok: false, error: "device_activation_required" };
  const hash = await sha256(id);
  if (row.device_hash && row.device_hash !== hash) return { ok: false, error: "license_bound_to_another_device" };
  if (!row.device_hash && bindIfEmpty) {
    const result = await env.DB.prepare(`UPDATE licenses SET device_hash = ?, device_bound_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'active' AND device_hash IS NULL`).bind(hash, row.id).run();
    if (!result.meta?.changes) {
      const fresh = await lookupById(row.id, env);
      if (!fresh || fresh.device_hash !== hash) return { ok: false, error: "license_bound_to_another_device" };
    }
  }
  return { ok: true };
}
async function lookupById(id, env) { return env.DB.prepare(`SELECT id, device_hash FROM licenses WHERE id = ?`).bind(id).first(); }

async function info(request, env, token) {
  const row = await lookup(token, env);
  if (!row) return json({ ok: false, error: "invalid_or_expired_link" }, 404, env);
  if (!active(row)) return json({ ok: false, error: "payment_not_approved_or_link_expired" }, 403, env);
  const device = await checkDevice(request, env, row, true);
  if (!device.ok) return json({ ok: false, error: device.error }, 403, env);
  const refreshed = await lookup(token, env);
  return json({ ok: true, licenseId: refreshed.id, buyer: refreshed.customer_name, expiresAt: refreshed.expires_at, downloadsUsed: Number(refreshed.download_count), downloadsRemaining: Math.max(0, Number(refreshed.max_downloads) - Number(refreshed.download_count)), active: active(refreshed), deviceBound: Boolean(refreshed.device_hash) }, 200, env);
}

async function download(request, env, token) {
  const row = await lookup(token, env);
  if (!row || !active(row)) return json({ ok: false, error: "payment_not_approved_or_link_expired" }, 403, env);
  const device = await checkDevice(request, env, row, true);
  if (!device.ok) return json({ ok: false, error: device.error }, 403, env);
  const key = String(row.object_key || BOOK_KEY).trim();
  if (key !== BOOK_KEY) return json({ ok: false, error: "invalid_book_key" }, 400, env);
  const object = await env.PAID_BOOKS.get(key, { type: "stream" });
  if (!object) return json({ ok: false, error: "file_not_found" }, 404, env);
  const update = await env.DB.prepare(`UPDATE licenses SET download_count = download_count + 1, last_download_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'active' AND payment_verified_at IS NOT NULL AND download_count < max_downloads`).bind(row.id).run();
  if (!update.meta?.changes) return json({ ok: false, error: "download_limit_reached" }, 403, env);
  const h = headers(env, { "content-type": "application/pdf", "content-disposition": `attachment; filename="encyclopedia-${row.id}.pdf"`, "cache-control": "private, no-store, max-age=0", "x-content-type-options": "nosniff" });
  return new Response(object, { status: 200, headers: h });
}

async function uploadBook(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  if (request.method !== "PUT") return json({ ok: false, error: "method_not_allowed" }, 405, env);
  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength && contentLength > 25 * 1024 * 1024) return json({ ok: false, error: "file_too_large" }, 413, env);
  const contentType = request.headers.get("content-type") || "";
  if (!contentType.toLowerCase().includes("application/pdf")) return json({ ok: false, error: "pdf_required" }, 400, env);
  await env.PAID_BOOKS.put(BOOK_KEY, request.body, { metadata: { contentType: "application/pdf", pages: 250 } });
  return json({ ok: true, key: BOOK_KEY, message: "paid_book_uploaded" }, 201, env);
}

function uploadPage(env) {
  const page = `<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow,noarchive"><title>رفع النسخة المدفوعة | منصة الموسوعات الذكية</title><style>body{font-family:system-ui,sans-serif;background:#f6f3ed;margin:0;color:#222}.wrap{max-width:650px;margin:35px auto;padding:18px}.card{background:#fff;border-radius:20px;padding:24px;box-shadow:0 10px 35px #0001}label{display:block;font-weight:700;margin:16px 0 7px}input,button{width:100%;box-sizing:border-box;padding:14px;border-radius:10px;font:inherit}input{border:1px solid #ccc}button{margin-top:18px;border:0;background:#111;color:#fff;font-weight:800}.status{margin-top:18px;padding:14px;border-radius:10px;background:#f3f3f3;line-height:1.8}.ok{background:#edf9ef}.bad{background:#fff0f0}.small{color:#666;line-height:1.8}</style></head><body><main class="wrap"><section class="card"><h1>📚 رفع الموسوعة المدفوعة</h1><p class="small">هذه الصفحة خاصة بصاحب الموسوعة. سيتم حفظ الملف تحت المفتاح المحمي للنسخة الكاملة ذات 250 صفحة.</p><form id="f"><label>مفتاح المشرف</label><input id="key" type="password" autocomplete="off" required><label>ملف PDF الكامل</label><input id="file" type="file" accept="application/pdf,.pdf" required><button>رفع النسخة الآن</button></form><div id="s" class="status">جاهز للرفع.</div></section></main><script>const f=document.getElementById('f'),s=document.getElementById('s');f.addEventListener('submit',async e=>{e.preventDefault();const file=document.getElementById('file').files[0],key=document.getElementById('key').value;if(!file){s.className='status bad';s.textContent='اختر ملف PDF أولاً.';return}if(file.size>25*1024*1024){s.className='status bad';s.textContent='الملف أكبر من الحد المسموح 25 ميجابايت.';return}s.className='status';s.textContent='جارٍ رفع الملف… لا تغلق الصفحة.';try{const r=await fetch('/api/admin/upload-book',{method:'PUT',headers:{'content-type':'application/pdf','x-admin-key':key},body:file});const d=await r.json().catch(()=>({}));if(!r.ok||!d.ok)throw new Error(d.error||'upload_failed');s.className='status ok';s.textContent='✅ تم رفع النسخة الكاملة بنجاح. المفتاح: '+d.key;}catch(err){s.className='status bad';s.textContent='❌ لم يتم الرفع: '+err.message}});</script></body></html>`;
  return new Response(page, { status: 200, headers: { "content-type": "text/html; charset=utf-8", "cache-control": "no-store", "x-content-type-options": "nosniff" } });
}

async function resetDevice(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  const body = await request.json().catch(() => null);
  if (!body || !safeId(body.licenseId)) return json({ ok: false, error: "invalid_license_id" }, 400, env);
  const result = await env.DB.prepare(`UPDATE licenses SET device_hash = NULL, device_bound_at = NULL WHERE id = ?`).bind(body.licenseId).run();
  return json({ ok: true, licenseId: body.licenseId, reset: Boolean(result.meta?.changes) }, 200, env);
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return preflight(env);
    const url = new URL(request.url);
    try {
      if (url.pathname === "/admin/upload-book" && request.method === "GET") return uploadPage(env);
      if (url.pathname === "/api/license/create" && request.method === "POST") return createLicense(request, env);
      if (url.pathname === "/api/license/reset-device" && request.method === "POST") return resetDevice(request, env);
      if (url.pathname === "/api/admin/upload-book" && request.method === "PUT") return uploadBook(request, env);
      if (url.pathname === "/api/delivery/info" && request.method === "GET") return info(request, env, url.searchParams.get("token"));
      if (url.pathname === "/api/delivery/download" && request.method === "GET") return download(request, env, url.searchParams.get("token"));
      return json({ ok: false, error: "not_found" }, 404, env);
    } catch (e) { return json({ ok: false, error: "server_error" }, 500, env); }
  }
};
