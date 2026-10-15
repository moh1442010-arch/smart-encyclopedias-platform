const ALLOW_METHODS = "GET, POST, PUT, OPTIONS";
const MAX_TTL_DAYS = 30;
const DEFAULT_MAX_DOWNLOADS = 3;
const MAX_UPLOAD_BYTES = 25 * 1024 * 1024;
const UPLOAD_CHUNK_BYTES = 1024 * 1024;
const MAX_UPLOAD_CHUNKS = 25;
const BOOK_KEY = "paid/encyclopedia-260-pages.pdf";

function origin(env) {
  return env.PUBLIC_ORIGIN || "https://moh1442010-arch.github.io/smart-encyclopedias-platform";
}

function corsOrigin(env) {
  try {
    return new URL(origin(env)).origin;
  } catch {
    return "https://moh1442010-arch.github.io";
  }
}

function headers(env, extra = {}) {
  return {
    "access-control-allow-origin": corsOrigin(env),
    "access-control-allow-methods": ALLOW_METHODS,
    "access-control-allow-headers": "content-type, authorization, x-admin-key, x-device-id",
    "vary": "Origin",
    ...extra
  };
}

function json(data, status, env) {
  return new Response(JSON.stringify(data), {
    status,
    headers: headers(env, {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store"
    })
  });
}

function preflight(env) {
  return new Response(null, { status: 204, headers: headers(env) });
}

function randomToken() {
  return crypto.randomUUID() + crypto.randomUUID().replaceAll("-", "");
}

async function sha256(text) {
  const bytes = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map(b => b.toString(16).padStart(2, "0")).join("");
}

function validEmail(v) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(v || ""));
}

function safeId(v) {
  return /^[A-Za-z0-9_-]{6,80}$/.test(String(v || ""));
}

function clampInt(v, min, max, fallback) {
  const n = Number.parseInt(v, 10);
  return Number.isInteger(n) ? Math.min(max, Math.max(min, n)) : fallback;
}

function isAdmin(request, env) {
  const expected = String(env.LICENSE_ADMIN_KEY || "").trim();
  const suppliedHeader = String(request.headers.get("x-admin-key") || "").trim();
  const bearer = String(request.headers.get("authorization") || "");
  const suppliedBearer = bearer.replace(/^Bearer\s+/i, "").trim();
  const supplied = suppliedHeader || suppliedBearer;
  return Boolean(expected && supplied && supplied === expected);
}

function deviceId(request) {
  return String(request.headers.get("x-device-id") || "").trim();
}

async function lookup(token, env) {
  if (!token || token.length < 30) return null;
  const tokenHash = await sha256(token);
  return env.DB.prepare(
    `SELECT id, order_id, customer_name, customer_email, object_key, status,
            expires_at, max_downloads, download_count, last_download_at,
            payment_verified_at, approved_by, device_hash, device_bound_at
     FROM licenses WHERE token_hash = ?`
  ).bind(tokenHash).first();
}

function active(row) {
  return row &&
    row.status === "active" &&
    row.payment_verified_at &&
    new Date(row.expires_at).getTime() > Date.now() &&
    Number(row.download_count) < Number(row.max_downloads);
}

async function lookupById(id, env) {
  return env.DB.prepare(
    "SELECT id, device_hash FROM licenses WHERE id = ?"
  ).bind(id).first();
}

async function checkDevice(request, env, row, bindIfEmpty = false) {
  const id = deviceId(request);
  if (!id || id.length < 16 || id.length > 256) {
    return { ok: false, error: "device_activation_required" };
  }

  const hash = await sha256(id);

  if (row.device_hash && row.device_hash !== hash) {
    return { ok: false, error: "license_bound_to_another_device" };
  }

  if (!row.device_hash && bindIfEmpty) {
    const result = await env.DB.prepare(
      "UPDATE licenses SET device_hash = ?, device_bound_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'active' AND device_hash IS NULL"
    ).bind(hash, row.id).run();

    if (!result.meta?.changes) {
      const fresh = await lookupById(row.id, env);
      if (!fresh || fresh.device_hash !== hash) {
        return { ok: false, error: "license_bound_to_another_device" };
      }
    }
  }

  return { ok: true };
}

async function ensureLicenseTable(env) {
  await env.DB.prepare(`
    CREATE TABLE IF NOT EXISTS licenses (
      id TEXT PRIMARY KEY, order_id TEXT, customer_name TEXT NOT NULL,
      customer_email TEXT NOT NULL, object_key TEXT NOT NULL,
      token_hash TEXT NOT NULL UNIQUE, status TEXT NOT NULL DEFAULT 'active',
      expires_at TEXT NOT NULL, max_downloads INTEGER NOT NULL DEFAULT 3,
      download_count INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      last_download_at TEXT, payment_verified_at TEXT, approved_by TEXT,
      device_hash TEXT, device_bound_at TEXT
    )
  `).run();
}

async function createLicense(request, env) {
  if (!isAdmin(request, env)) {
    return json({ ok: false, error: "payment_approval_required" }, 403, env);
  }

  const body = await request.json().catch(() => null);

  if (
    !body ||
    body.approved !== true ||
    !safeId(body.licenseId) ||
    !String(body.customerName || "").trim() ||
    !validEmail(body.customerEmail) ||
    String(body.objectKey || BOOK_KEY).trim() !== BOOK_KEY
  ) {
    return json({ ok: false, error: "explicit_payment_approval_required" }, 400, env);
  }

  await ensureLicenseTable(env);

  const days = clampInt(body.expiresDays, 1, MAX_TTL_DAYS, 30);
  const maxDownloads = clampInt(
    body.maxDownloads, 1, 10, DEFAULT_MAX_DOWNLOADS
  );

  const token = randomToken();
  const tokenHash = await sha256(token);
  const expiresAt = new Date(
    Date.now() + days * 86400000
  ).toISOString();

  const approvedBy = String(body.approvedBy || "owner")
    .trim()
    .slice(0, 120);

  try {
    await env.DB.prepare(
      `INSERT INTO licenses
       (id, order_id, customer_name, customer_email, object_key,
        token_hash, expires_at, max_downloads, payment_verified_at, approved_by)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?)`
    ).bind(
      body.licenseId,
      body.orderId || null,
      String(body.customerName).trim(),
      String(body.customerEmail).trim().toLowerCase(),
      BOOK_KEY,
      tokenHash,
      expiresAt,
      maxDownloads,
      approvedBy
    ).run();
  } catch (e) {
    return json({ ok: false, error: "license_create_failed" }, 409, env);
  }

  return json({
    ok: true,
    licenseId: body.licenseId,
    paymentApproved: true,
    expiresAt,
    maxDownloads,
    token,
    deliveryUrl:
      `${origin(env)}/delivery.html?token=${encodeURIComponent(token)}`
  }, 201, env);
}


async function ensureAppLicenseTables(env) {
  await env.DB.prepare(`
    CREATE TABLE IF NOT EXISTS app_licenses (
      id TEXT PRIMARY KEY,
      license_code_hash TEXT NOT NULL UNIQUE,
      plan TEXT NOT NULL,
      term_type TEXT NOT NULL,
      customer_name TEXT NOT NULL,
      customer_email TEXT,
      order_id TEXT,
      status TEXT NOT NULL DEFAULT 'active',
      device_hash TEXT UNIQUE,
      starts_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      expires_at TEXT,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
      approved_by TEXT NOT NULL
    )
  `).run();
  await env.DB.prepare(`
    CREATE TABLE IF NOT EXISTS app_trials (
      device_hash TEXT PRIMARY KEY,
      started_at TEXT NOT NULL,
      expires_at TEXT NOT NULL,
      created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
  `).run();
}

function appDeviceId(request) {
  return String(request.headers.get("x-app-device-id") || "").trim();
}

async function appDeviceHash(request) {
  const id = appDeviceId(request);
  if (!id || id.length < 8 || id.length > 256) return null;
  return sha256("smart-company-app-v1:" + id);
}

function appLicenseActive(row) {
  return Boolean(row && row.status === "active" &&
    (!row.expires_at || new Date(row.expires_at).getTime() > Date.now()));
}

async function appLicenseStatus(request, env) {
  const deviceHash = await appDeviceHash(request);
  if (!deviceHash) return json({ ok: false, error: "device_id_required" }, 400, env);
  await ensureAppLicenseTables(env);

  const license = await env.DB.prepare(
    "SELECT id, plan, term_type, status, starts_at, expires_at FROM app_licenses WHERE device_hash = ?"
  ).bind(deviceHash).first();

  if (appLicenseActive(license)) {
    return json({
      ok: true, mode: "licensed", licenseId: license.id, plan: license.plan,
      termType: license.term_type, expiresAt: license.expires_at, serverTime: new Date().toISOString()
    }, 200, env);
  }

  // Do not convert a revoked, suspended, or expired paid license into a fresh trial.
  // Keep the device bound to its existing license record and return a locked state.
  if (license) {
    return json({
      ok: true, mode: "expired", licenseId: license.id, plan: license.plan,
      termType: license.term_type, expiresAt: license.expires_at,
      licenseStatus: license.status, serverTime: new Date().toISOString()
    }, 200, env);
  }

  let trial = await env.DB.prepare(
    "SELECT started_at, expires_at FROM app_trials WHERE device_hash = ?"
  ).bind(deviceHash).first();

  if (!trial) {
    const startedAt = new Date().toISOString();
    const expiresAt = new Date(Date.now() + 7 * 86400000).toISOString();
    await env.DB.prepare(
      "INSERT OR IGNORE INTO app_trials (device_hash, started_at, expires_at) VALUES (?, ?, ?)"
    ).bind(deviceHash, startedAt, expiresAt).run();
    trial = await env.DB.prepare(
      "SELECT started_at, expires_at FROM app_trials WHERE device_hash = ?"
    ).bind(deviceHash).first();
  }

  const active = new Date(trial.expires_at).getTime() > Date.now();
  return json({
    ok: true, mode: active ? "trial" : "expired", trialStartedAt: trial.started_at,
    expiresAt: trial.expires_at, remainingMs: Math.max(0, new Date(trial.expires_at).getTime() - Date.now()),
    serverTime: new Date().toISOString()
  }, 200, env);
}

async function activateAppLicense(request, env) {
  const deviceHash = await appDeviceHash(request);
  if (!deviceHash) return json({ ok: false, error: "device_id_required" }, 400, env);
  const body = await request.json().catch(() => null);
  const code = String(body && body.activationCode || "").trim().toUpperCase();
  if (!/^[A-Z0-9-]{12,64}$/.test(code)) {
    return json({ ok: false, error: "invalid_activation_code" }, 400, env);
  }
  await ensureAppLicenseTables(env);
  const codeHash = await sha256(code);
  const license = await env.DB.prepare(
    "SELECT id, plan, term_type, status, device_hash, starts_at, expires_at FROM app_licenses WHERE license_code_hash = ?"
  ).bind(codeHash).first();

  if (!license || license.status !== "active" ||
      (license.expires_at && new Date(license.expires_at).getTime() <= Date.now())) {
    return json({ ok: false, error: "license_invalid_or_expired" }, 403, env);
  }
  if (license.device_hash && license.device_hash !== deviceHash) {
    return json({ ok: false, error: "license_bound_to_another_device" }, 403, env);
  }
  if (!license.device_hash) {
    const result = await env.DB.prepare(
      "UPDATE app_licenses SET device_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND device_hash IS NULL AND status = 'active'"
    ).bind(deviceHash, license.id).run();
    if (!result.meta?.changes) {
      const fresh = await env.DB.prepare("SELECT device_hash FROM app_licenses WHERE id = ?").bind(license.id).first();
      if (!fresh || fresh.device_hash !== deviceHash) return json({ ok: false, error: "license_bound_to_another_device" }, 403, env);
    }
  }
  return json({
    ok: true, mode: "licensed", licenseId: license.id, plan: license.plan,
    termType: license.term_type, expiresAt: license.expires_at, serverTime: new Date().toISOString()
  }, 200, env);
}

async function createAppLicense(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  const body = await request.json().catch(() => null);
  const plan = String(body && body.plan || "").toLowerCase();
  const termType = String(body && body.termType || "").toLowerCase();
  const customerName = String(body && body.customerName || "").trim().slice(0, 160);
  const approvedBy = String(body && body.approvedBy || "owner").trim().slice(0, 120);
  if (!body || body.approved !== true || !/^(basic|pro|business|enterprise)$/.test(plan) ||
      !/^(perpetual|monthly|annual)$/.test(termType) || customerName.length < 2) {
    return json({ ok: false, error: "explicit_payment_approval_and_valid_plan_required" }, 400, env);
  }
  await ensureAppLicenseTables(env);
  const id = String(body.licenseId || ("SC-" + crypto.randomUUID().replaceAll("-", "").slice(0, 16))).toUpperCase();
  if (!/^[A-Z0-9_-]{6,80}$/.test(id)) return json({ ok: false, error: "invalid_license_id" }, 400, env);
  const activationCode = ("SC-" + crypto.randomUUID() + "-" + crypto.randomUUID().slice(0, 8)).toUpperCase();
  const codeHash = await sha256(activationCode);
  let expiresAt = null;
  if (termType === "monthly") expiresAt = new Date(Date.now() + 30 * 86400000).toISOString();
  if (termType === "annual") expiresAt = new Date(Date.now() + 365 * 86400000).toISOString();
  try {
    await env.DB.prepare(
      "INSERT INTO app_licenses (id, license_code_hash, plan, term_type, customer_name, customer_email, order_id, expires_at, approved_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
    ).bind(id, codeHash, plan, termType, customerName,
      String(body.customerEmail || "").trim().toLowerCase() || null,
      String(body.orderId || "").trim() || null, expiresAt, approvedBy).run();
  } catch (e) {
    return json({ ok: false, error: "license_create_failed" }, 409, env);
  }
  return json({
    ok: true, licenseId: id, activationCode, plan, termType, expiresAt,
    paymentApproved: true, note: "Show this code to the buyer once; only its hash is stored."
  }, 201, env);
}

async function listAppLicenses(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  await ensureAppLicenseTables(env);
  const rows = await env.DB.prepare(
    `SELECT id, plan, term_type, customer_name, customer_email, order_id,
            status, starts_at, expires_at, created_at, updated_at,
            CASE WHEN device_hash IS NULL THEN 0 ELSE 1 END AS device_bound
     FROM app_licenses ORDER BY created_at DESC LIMIT 500`
  ).all();
  const counts = await env.DB.prepare(
    `SELECT COUNT(*) AS total,
            SUM(CASE WHEN status='active' AND (expires_at IS NULL OR julianday(expires_at) > julianday('now')) THEN 1 ELSE 0 END) AS active
     FROM app_licenses`
  ).first();
  const trials = await env.DB.prepare(
    "SELECT COUNT(*) AS total, SUM(CASE WHEN julianday(expires_at) > julianday('now') THEN 1 ELSE 0 END) AS active FROM app_trials"
  ).first();
  return json({
    ok: true,
    summary: {
      licenses: Number(counts?.total || 0),
      activeLicenses: Number(counts?.active || 0),
      trials: Number(trials?.total || 0),
      activeTrials: Number(trials?.active || 0)
    },
    licenses: rows.results || []
  }, 200, env);
}

async function changeAppLicenseStatus(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  const body = await request.json().catch(() => null);
  const id = String(body?.licenseId || "").trim();
  const status = String(body?.status || "").trim().toLowerCase();
  if (!/^[A-Z0-9_-]{6,80}$/i.test(id) || !["active", "suspended", "revoked"].includes(status)) {
    return json({ ok: false, error: "invalid_license_id_or_status" }, 400, env);
  }
  await ensureAppLicenseTables(env);
  const result = await env.DB.prepare(
    "UPDATE app_licenses SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?"
  ).bind(status, id).run();
  if (!result.meta?.changes) return json({ ok: false, error: "license_not_found" }, 404, env);
  return json({ ok: true, licenseId: id, status, dataPreserved: true }, 200, env);
}

async function info(request, env, token) {
  const row = await lookup(token, env);

  if (!row) {
    return json({ ok: false, error: "invalid_or_expired_link" }, 404, env);
  }

  if (!active(row)) {
    return json({
      ok: false,
      error: "payment_not_approved_or_link_expired"
    }, 403, env);
  }

  const device = await checkDevice(request, env, row, true);

  if (!device.ok) {
    return json({ ok: false, error: device.error }, 403, env);
  }

  const refreshed = await lookup(token, env);

  return json({
    ok: true,
    licenseId: refreshed.id,
    buyer: refreshed.customer_name,
    expiresAt: refreshed.expires_at,
    downloadsUsed: Number(refreshed.download_count),
    downloadsRemaining:
      Math.max(
        0,
        Number(refreshed.max_downloads) - Number(refreshed.download_count)
      ),
    active: active(refreshed),
    deviceBound: Boolean(refreshed.device_hash)
  }, 200, env);
}

async function download(request, env, token) {
  const row = await lookup(token, env);

  if (!row || !active(row)) {
    return json({
      ok: false,
      error: "payment_not_approved_or_link_expired"
    }, 403, env);
  }

  const device = await checkDevice(request, env, row, true);

  if (!device.ok) {
    return json({ ok: false, error: device.error }, 403, env);
  }

  const key = String(row.object_key || BOOK_KEY).trim();

  if (key !== BOOK_KEY) {
    return json({ ok: false, error: "invalid_book_key" }, 400, env);
  }

  let object = null;
  let contentLength = null;

  // The PDF is stored as 1 MiB KV chunks. The active manifest lives in D1,
  // so finalization never has to write the canonical BOOK_KEY in KV.
  const manifest = await env.DB.prepare(
    "SELECT object_key, session, total, size, pages FROM book_files WHERE id = ?"
  ).bind(BOOK_KEY).first();

  if (manifest) {
    if (!manifest.session || !Number.isInteger(Number(manifest.total)) || !Number.isInteger(Number(manifest.size))) {
      return json({ ok: false, error: "invalid_book_manifest" }, 500, env);
    }

    const chunkStreams = [];
    for (let i = 0; i < manifest.total; i++) {
      const chunk = await env.PAID_BOOKS.get(`upload/${manifest.session}/${i}`, { type: "stream" });
      if (!chunk) return json({ ok: false, error: "file_chunk_not_found" }, 404, env);
      chunkStreams.push(chunk);
    }

    let streamIndex = 0;
    let currentReader = null;
    object = new ReadableStream({
      async pull(controller) {
        try {
          while (true) {
            if (!currentReader) {
              if (streamIndex >= chunkStreams.length) {
                controller.close();
                return;
              }
              currentReader = chunkStreams[streamIndex++].getReader();
            }
            const part = await currentReader.read();
            if (part.done) {
              currentReader = null;
              continue;
            }
            controller.enqueue(part.value);
            return;
          }
        } catch (error) {
          controller.error(error);
        }
      },
      cancel() {
        if (currentReader) currentReader.cancel().catch(() => {});
      }
    });
    contentLength = String(manifest.size);
  } else {
    // Legacy direct-KV fallback for any older single-object upload.
    object = await env.PAID_BOOKS.get(BOOK_KEY, { type: "stream" });
  }

  if (!object) return json({ ok: false, error: "file_not_found" }, 404, env);

  const update = await env.DB.prepare(`UPDATE licenses
     SET download_count = download_count + 1,
         last_download_at = CURRENT_TIMESTAMP
     WHERE id = ?
       AND status = 'active'
       AND payment_verified_at IS NOT NULL
       AND download_count < max_downloads`
  ).bind(row.id).run();

  if (!update.meta?.changes) {
    return json({ ok: false, error: "download_limit_reached" }, 403, env);
  }

  return new Response(object, {
    status: 200,
    headers: headers(env, {
      "content-type": "application/pdf",
      "content-disposition": `attachment; filename="encyclopedia-${row.id}.pdf"`,
      ...(contentLength ? { "content-length": contentLength } : {}),
      "cache-control": "private, no-store, max-age=0",
      "x-content-type-options": "nosniff"
    })
  });
}

async function uploadBookChunk(request, env, url) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  if (request.method !== "PUT") return json({ ok: false, error: "method_not_allowed" }, 405, env);

  const session = String(url.searchParams.get("session") || "");
  const index = Number.parseInt(url.searchParams.get("index") || "", 10);
  const total = Number.parseInt(url.searchParams.get("total") || "", 10);
  const size = Number.parseInt(url.searchParams.get("size") || "", 10);

  if (!/^[A-Za-z0-9_-]{12,80}$/.test(session) ||
      !Number.isInteger(index) || index < 0 ||
      !Number.isInteger(total) || total < 1 || total > MAX_UPLOAD_CHUNKS ||
      index >= total || !Number.isInteger(size) || size < 1 || size > MAX_UPLOAD_BYTES) {
    return json({ ok: false, error: "invalid_upload_parameters" }, 400, env);
  }

  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength && contentLength > UPLOAD_CHUNK_BYTES) {
    return json({ ok: false, error: "chunk_too_large" }, 413, env);
  }

  const chunk = await request.arrayBuffer();
  if (!chunk.byteLength || chunk.byteLength > UPLOAD_CHUNK_BYTES) {
    return json({ ok: false, error: "invalid_chunk_size" }, 400, env);
  }

  await env.PAID_BOOKS.put(`upload/${session}/${index}`, chunk);
  return json({ ok: true, index, total }, 200, env);
}

async function finalizeUpload(request, env, url) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  if (request.method !== "POST") return json({ ok: false, error: "method_not_allowed" }, 405, env);

  const session = String(url.searchParams.get("session") || "");
  const total = Number.parseInt(url.searchParams.get("total") || "", 10);
  const size = Number.parseInt(url.searchParams.get("size") || "", 10);

  if (!/^[A-Za-z0-9_-]{12,80}$/.test(session) ||
      !Number.isInteger(total) || total < 1 || total > MAX_UPLOAD_CHUNKS ||
      !Number.isInteger(size) || size < 1 || size > MAX_UPLOAD_BYTES) {
    return json({ ok: false, error: "invalid_upload_parameters" }, 400, env);
  }

  const keys = Array.from({ length: total }, (_, i) => `upload/${session}/${i}`);
  for (const key of keys) {
    const exists = await env.PAID_BOOKS.get(key, { type: "arrayBuffer" });
    if (!exists) return json({ ok: false, error: "missing_upload_chunk", key }, 409, env);
  }

  // Keep the PDF as 1 MiB KV chunks. The manifest is stored in D1, avoiding
  // the final KV write that has repeatedly returned HTTP 500 in this account.
  await env.DB.prepare(`
    CREATE TABLE IF NOT EXISTS book_files (
      id TEXT PRIMARY KEY,
      object_key TEXT NOT NULL,
      session TEXT NOT NULL,
      total INTEGER NOT NULL,
      size INTEGER NOT NULL,
      pages INTEGER NOT NULL,
      updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
    )
  `).run();

  await env.DB.prepare(`
    INSERT INTO book_files (id, object_key, session, total, size, pages, updated_at)
    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
    ON CONFLICT(id) DO UPDATE SET
      object_key = excluded.object_key,
      session = excluded.session,
      total = excluded.total,
      size = excluded.size,
      pages = excluded.pages,
      updated_at = CURRENT_TIMESTAMP
  `).bind(BOOK_KEY, BOOK_KEY, session, total, size, 260).run();

  return json({ ok: true, key: BOOK_KEY, size, pages: 260, message: "paid_book_uploaded" }, 201, env);
}

async function uploadBook(request, env) {
  if (!isAdmin(request, env)) return json({ ok: false, error: "unauthorized" }, 401, env);
  if (request.method !== "PUT") return json({ ok: false, error: "method_not_allowed" }, 405, env);

  const contentLength = Number(request.headers.get("content-length") || 0);
  if (contentLength && contentLength > MAX_UPLOAD_BYTES) return json({ ok: false, error: "file_too_large" }, 413, env);

  const contentType = request.headers.get("content-type") || "";
  if (!contentType.toLowerCase().includes("application/pdf")) return json({ ok: false, error: "pdf_required" }, 400, env);

  await env.PAID_BOOKS.put(BOOK_KEY, request.body, {
    metadata: { contentType: "application/pdf", pages: 260 }
  });

  return json({ ok: true, key: BOOK_KEY, message: "paid_book_uploaded" }, 201, env);
}

function uploadPage() {
  const page = `<!doctype html>\n<html lang=\"ar\" dir=\"rtl\">\n<head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><meta name=\"robots\" content=\"noindex,nofollow,noarchive\"><title>رفع الموسوعة المدفوعة</title>\n<style>\nbody{font-family:system-ui,sans-serif;background:#f6f3ed;margin:0;color:#222}.wrap{max-width:650px;margin:18px auto;padding:14px}.card{background:#fff;border-radius:20px;padding:22px;box-shadow:0 10px 35px #0001}label{display:block;font-weight:700;margin:16px 0 7px}input,button{width:100%;box-sizing:border-box;padding:14px;border-radius:10px;font:inherit}input{border:1px solid #ccc}button{margin-top:18px;border:0;background:#111;color:#fff;font-weight:800}button:disabled{opacity:.5}.status{margin-top:18px;padding:14px;border-radius:10px;background:#f3f3f3;line-height:1.8}.ok{background:#edf9ef}.bad{background:#fff0f0}.small{color:#666;line-height:1.8}.hint{background:#fff8df;padding:12px;border-radius:10px;line-height:1.8}\n</style></head>\n<body><main class=\"wrap\"><section class=\"card\" id=\"top\">\n<h1>📚 رفع الموسوعة المدفوعة</h1>\n<p class=\"hint\"><b>الترتيب المهم:</b> اختر ملف PDF أولاً، ثم اكتب مفتاح المشرف، ثم اضغط الرفع. إذا أعاد الهاتف الصفحة إلى أعلى الشاشة بعد اختيار الملف فهذا طبيعي؛ الملف والمفتاح محفوظان محليًا ولن يضيع الرفع.</p>\n<label>1) ملف PDF الكامل</label><input id=\"file\" type=\"file\" accept=\"application/pdf,.pdf\">\n<label>2) مفتاح المشرف</label><input id=\"key\" type=\"password\" autocomplete=\"off\">\n<button id=\"uploadBtn\" type=\"button\">رفع النسخة الآن</button><div id=\"s\" class=\"status\">جاهز للرفع.</div>\n</section></main>\n<script>\nconst DB_NAME="smart-encyclopedias-upload",STORE="state";\nconst btn=document.getElementById("uploadBtn"),s=document.getElementById("s"),keyEl=document.getElementById("key"),fileEl=document.getElementById("file");\nfunction openDB(){return new Promise((resolve,reject)=>{const r=indexedDB.open(DB_NAME,1);r.onupgradeneeded=()=>r.result.createObjectStore(STORE);r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error);});}\nasync function saveState(key,file){const db=await openDB();await new Promise((res,rej)=>{const t=db.transaction(STORE,"readwrite");if(key!==undefined)t.objectStore(STORE).put(key,"key");if(file)t.objectStore(STORE).put(file,"file");t.oncomplete=res;t.onerror=()=>rej(t.error);});}\nasync function getState(){const db=await openDB();return new Promise((res,rej)=>{const t=db.transaction(STORE,"readonly"),st=t.objectStore(STORE);let out={};st.get("key").onsuccess=e=>out.key=e.target.result;st.get("file").onsuccess=e=>out.file=e.target.result;t.oncomplete=()=>res(out);t.onerror=()=>rej(t.error);});}\nasync function clearState(){try{const db=await openDB();await new Promise((res,rej)=>{const t=db.transaction(STORE,"readwrite");t.objectStore(STORE).clear();t.oncomplete=res;t.onerror=()=>rej(t.error);});}catch{}}\nfunction saveScroll(){try{sessionStorage.setItem("uploadScroll",String(window.scrollY||0))}catch{}}\nfunction restoreScroll(){try{const y=Number(sessionStorage.getItem("uploadScroll")||0);if(y)requestAnimationFrame(()=>window.scrollTo(0,y))}catch{}}\nwindow.addEventListener("scroll",saveScroll,{passive:true});window.addEventListener("pageshow",restoreScroll);window.addEventListener("focus",restoreScroll);\nasync function upload(file,key){\nif(!key){s.className="status bad";s.textContent="أدخل مفتاح المشرف.";keyEl.focus();return}\nif(!file){s.className="status bad";s.textContent="اختر ملف PDF أولاً.";fileEl.focus();return}\nif(file.size>25*1024*1024){s.className="status bad";s.textContent="الملف أكبر من 25 ميجابايت.";return}\nbtn.disabled=true;s.className="status";const chunkSize=1024*1024,total=Math.ceil(file.size/chunkSize),session=crypto.randomUUID().replaceAll("-","");\ntry{for(let i=0;i<total;i++){const chunk=file.slice(i*chunkSize,Math.min(file.size,(i+1)*chunkSize));s.textContent="جارٍ رفع الجزء "+(i+1)+" من "+total+"…";const r=await fetch("/api/admin/upload-book/chunk?session="+session+"&index="+i+"&total="+total+"&size="+file.size,{method:"PUT",headers:{"content-type":"application/octet-stream","x-admin-key":key},body:chunk});const d=await r.json().catch(()=>({}));if(!r.ok||!d.ok)throw new Error((d.error||"chunk_upload_failed")+" (HTTP "+r.status+")")}\ns.textContent="جاري تجميع النسخة…";const r=await fetch("/api/admin/upload-book/finalize?session="+session+"&total="+total+"&size="+file.size,{method:"POST",headers:{"x-admin-key":key}});const d=await r.json().catch(()=>({}));if(!r.ok||!d.ok)throw new Error((d.error||"finalize_failed")+" (HTTP "+r.status+")");await clearState();s.className="status ok";s.textContent="✅ تم رفع النسخة الكاملة بنجاح — "+d.pages+" صفحة."}catch(e){s.className="status bad";s.textContent="❌ لم يتم الرفع: "+e.message;btn.disabled=false}}\nfileEl.addEventListener("change",async()=>{const file=fileEl.files[0];if(!file)return;try{await saveState(undefined,file);s.className="status";s.textContent="تم حفظ الملف محليًا. اكتب المفتاح ثم اضغط «رفع النسخة الآن».";restoreScroll()}catch(e){s.className="status bad";s.textContent="تعذر حفظ الملف محليًا: "+e.message}})\nkeyEl.addEventListener("input",async()=>{try{await saveState(keyEl.value.trim(),undefined)}catch{}})\nbtn.addEventListener("click",async()=>{const st=await getState().catch(()=>({}));await upload(fileEl.files[0]||st.file,keyEl.value.trim()||st.key||"")});\n(async()=>{try{const st=await getState();if(st.key)keyEl.value=st.key;if(st.file)s.textContent="تم استعادة الملف المحفوظ. اكتب المفتاح أو اضغط «رفع النسخة الآن» للمتابعة.";restoreScroll()}catch{}})();\n</script></body></html>`;
  return new Response(page, {
    status: 200,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff"
    }
  });
}

async function resetDevice(request, env) {
  if (!isAdmin(request, env)) {
    return json({ ok: false, error: "unauthorized" }, 401, env);
  }

  const body = await request.json().catch(() => null);

  if (!body || !safeId(body.licenseId)) {
    return json({ ok: false, error: "invalid_license_id" }, 400, env);
  }

  const result = await env.DB.prepare(
    "UPDATE licenses SET device_hash = NULL, device_bound_at = NULL WHERE id = ?"
  ).bind(body.licenseId).run();

  return json({
    ok: true,
    licenseId: body.licenseId,
    reset: Boolean(result.meta?.changes)
  }, 200, env);
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return preflight(env);

    const url = new URL(request.url);

    try {
      // Health check: confirms that this exact Worker code is live.
      if (url.pathname === "/" && request.method === "GET") {
        return json({
          ok: true,
          service: "smart-encyclopedias-delivery",
          status: "online",
          version: "kv-delivery-v9-approval-diagnostics",
          adminConfigured: Boolean(String(env.LICENSE_ADMIN_KEY || "").trim()),
          book: BOOK_KEY
        }, 200, env);
      }

      if (url.pathname === "/admin/upload-book" && request.method === "GET") {
        return uploadPage();
      }

      if (url.pathname === "/api/app-license/status" && request.method === "GET") {
        return appLicenseStatus(request, env);
      }

      if (url.pathname === "/api/app-license/activate" && request.method === "POST") {
        return activateAppLicense(request, env);
      }

      if (url.pathname === "/api/app-license/admin/create" && request.method === "POST") {
        return createAppLicense(request, env);
      }

      if (url.pathname === "/api/app-license/admin/list" && request.method === "GET") {
        return listAppLicenses(request, env);
      }

      if (url.pathname === "/api/app-license/admin/status" && request.method === "POST") {
        return changeAppLicenseStatus(request, env);
      }

      if (url.pathname === "/api/license/create" && request.method === "POST") {
        return createLicense(request, env);
      }

      if (url.pathname === "/api/license/reset-device" && request.method === "POST") {
        return resetDevice(request, env);
      }

      if (url.pathname === "/api/admin/upload-book/chunk" && request.method === "PUT") {
        return uploadBookChunk(request, env, url);
      }

      if (url.pathname === "/api/admin/upload-book/finalize" && request.method === "POST") {
        return finalizeUpload(request, env, url);
      }

      if (url.pathname === "/api/admin/upload-book" && request.method === "PUT") {
        return uploadBook(request, env);
      }

      if (url.pathname === "/api/delivery/info" && request.method === "GET") {
        return info(request, env, url.searchParams.get("token"));
      }

      if (url.pathname === "/api/delivery/download" && request.method === "GET") {
        return download(request, env, url.searchParams.get("token"));
      }

      return json({ ok: false, error: "not_found" }, 404, env);
    } catch (e) {
      return json({ ok: false, error: "server_error" }, 500, env);
    }
  }
};

// Deployment trigger: store chunk manifest in D1 instead of KV canonical key.
