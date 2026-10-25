const APP_ID = "com.mohammedmustafa.almizan.bakery";
const EXPECTED_APP_PUBLIC_KEY_DER_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqlGWpIPqSpXdEP67uticpomJ5nKGkHrZkKDgnueolx8cFKyov/PydmJuAnxUaRnggcNte4/PfGzwrXtmoJ6hrpXfYUVjyL3UZ9MdCmPKevrdBftzWYoJ6NG2Dda6ZdlQoDdD/Mads1uJYACKxbipsgWSUr9MzmJdmTSYexDHBS6MWMPyjK+L60Jy3sn6k85bPE+AnPtOW38/no2ucz6h8kANgpd/4XDusR07l6EYOSg8TXuVny6SKUzH6PWf/TC6fXt2erASsPkvc5MehBMI9vXCTpj0PGZikAdyj/Hw6IKyr5HmG8vLgzofB1AfXPgIrJdhGMhdsUn8iD+4ZLBucQIDAQAB";
const PLANS = {
  monthly: { amount: 30000, seconds: 30 * 86400 },
  annual: { amount: 300000, seconds: 365 * 86400 },
  lifetime: { amount: 450000, seconds: 0 }
};
const encoder = new TextEncoder();

function json(data, status = 200, headers = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store", ...headers }
  });
}
function corsHeaders(request, env) {
  const origin = request.headers.get("Origin") || "";
  const allowed = env.STORE_ORIGIN || "";
  if (origin && allowed && origin === allowed) {
    return {
      "access-control-allow-origin": origin,
      "access-control-allow-methods": "GET,POST,OPTIONS",
      "access-control-allow-headers": "content-type,authorization",
      "vary": "Origin"
    };
  }
  return {};
}
function withCors(response, request, env) {
  const h = new Headers(response.headers);
  for (const [k, v] of Object.entries(corsHeaders(request, env))) h.set(k, v);
  return new Response(response.body, { status: response.status, headers: h });
}
function cleanText(v, max = 160) {
  return typeof v === "string" ? v.trim().slice(0, max) : "";
}
function validDevice(v) {
  return /^[A-Za-z0-9-]{8,100}$/.test(v) && !v.includes(";") && !v.includes("=");
}
function base64url(bytes) {
  let s = "";
  for (const b of new Uint8Array(bytes)) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}
function fromBase64(s) {
  const raw = atob(s);
  const out = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) out[i] = raw.charCodeAt(i);
  return out;
}
async function importPrivateKey(env) {
  if (!env.LICENSE_PRIVATE_KEY_PKCS8_B64 || !env.LICENSE_PUBLIC_KEY_DER_B64) {
    throw new Error("LICENSE_SIGNING_NOT_CONFIGURED");
  }
  if (env.LICENSE_PUBLIC_KEY_DER_B64 !== EXPECTED_APP_PUBLIC_KEY_DER_B64) {
    throw new Error("LICENSE_PUBLIC_KEY_MISMATCH");
  }
  const privateKey = await crypto.subtle.importKey(
    "pkcs8",
    fromBase64(env.LICENSE_PRIVATE_KEY_PKCS8_B64),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
  const publicKey = await crypto.subtle.importKey(
    "spki",
    fromBase64(env.LICENSE_PUBLIC_KEY_DER_B64),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    true,
    ["verify"]
  );
  // Refuse to issue unless the configured private key actually matches the app's embedded public key.
  const probe = encoder.encode("Al-Mizan Bakery key-pair check");
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", privateKey, probe);
  const matches = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", publicKey, sig, probe);
  if (!matches) throw new Error("LICENSE_KEY_PAIR_MISMATCH");
  return privateKey;
}
async function issueToken(env, deviceId, plan) {
  const key = await importPrivateKey(env);
  const now = Math.floor(Date.now() / 1000);
  const expires = plan === "lifetime" ? 0 : now + PLANS[plan].seconds;
  const appVersion = cleanText(env.APP_VERSION || "2.5.8", 32);
  // This exact payload format is consumed by MainActivity.verifyLicenseToken().
  const payload = `app=${APP_ID};device=${deviceId};plan=${plan};max_version=${plan === "lifetime" ? appVersion : "any"};expires=${expires}`;
  const bytes = encoder.encode(payload);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, bytes);
  return { token: base64url(bytes) + "." + base64url(signature), expires, appVersion };
}
async function audit(env, orderId, event, actor, detail = "") {
  await env.DB.prepare(
    "INSERT INTO license_audit(order_id,event,actor,created_at,detail) VALUES(?,?,?,?,?)"
  ).bind(orderId, event, actor, new Date().toISOString(), detail.slice(0, 500)).run();
}
async function handle(request, env) {
  const url = new URL(request.url);
  if (request.method === "OPTIONS") return new Response(null, { status: 204 });
  if (!env.DB) return json({ error: "SERVICE_NOT_CONFIGURED" }, 503);

  if (url.pathname === "/health" && request.method === "GET") {
    return json({ ok: true, service: "al-mizan-bakery-licensing", mode: "manual-payment-review" });
  }

  if (url.pathname === "/api/orders" && request.method === "POST") {
    let body;
    try { body = await request.json(); } catch { return json({ error: "INVALID_JSON" }, 400); }
    const customer = cleanText(body.customerName, 100);
    const contact = cleanText(body.contact, 80);
    const deviceId = cleanText(body.deviceId, 100);
    const plan = cleanText(body.plan, 20);
    const receiptRef = cleanText(body.receiptReference, 100);
    if (!customer || !contact || !receiptRef || !validDevice(deviceId) || !PLANS[plan]) {
      return json({ error: "INVALID_FIELDS" }, 400);
    }
    const appVersion = cleanText(env.APP_VERSION || "2.5.8", 32);
    const id = crypto.randomUUID();
    const now = new Date().toISOString();
    await env.DB.prepare(
      "INSERT INTO license_orders(id,created_at,updated_at,customer_name,contact,device_id,plan,amount_sdg,receipt_reference,status,app_version) VALUES(?,?,?,?,?,?,?,?,?,?,?)"
    ).bind(id, now, now, customer, contact, deviceId, plan, PLANS[plan].amount, receiptRef, "pending", appVersion).run();
    await audit(env, id, "order_created", "customer", "Payment requires manual verification; receipt reference recorded.");
    return json({ orderId: id, status: "pending", amountSdg: PLANS[plan].amount, message: "تم تسجيل الطلب. لن يصدر الترخيص قبل مراجعة الدفع." }, 201);
  }

  const statusMatch = url.pathname.match(/^\/api\/orders\/([0-9a-f-]{36})$/i);
  if (statusMatch && request.method === "GET") {
    const row = await env.DB.prepare(
      "SELECT id,created_at,plan,amount_sdg,status,reviewed_at,reviewer_note,activation_token,token_expires,app_version FROM license_orders WHERE id=?"
    ).bind(statusMatch[1]).first();
    if (!row) return json({ error: "ORDER_NOT_FOUND" }, 404);
    // The activation token is only returned after explicit admin approval.
    return json({
      orderId: row.id, createdAt: row.created_at, plan: row.plan, amountSdg: row.amount_sdg,
      status: row.status, reviewedAt: row.reviewed_at, note: row.reviewer_note || "",
      activationToken: row.status === "approved" ? row.activation_token : null,
      expiresAt: row.token_expires, appVersion: row.app_version
    });
  }

  if (url.pathname === "/admin/orders" && request.method === "GET") {
    if (!env.ADMIN_API_KEY || request.headers.get("Authorization") !== `Bearer ${env.ADMIN_API_KEY}`) {
      return json({ error: "UNAUTHORIZED" }, 401);
    }
    const status = url.searchParams.get("status") || "pending";
    if (!["pending", "approved", "rejected"].includes(status)) return json({ error: "INVALID_STATUS" }, 400);
    const rows = await env.DB.prepare(
      "SELECT id,created_at,customer_name,contact,device_id,plan,amount_sdg,receipt_reference,status,app_version FROM license_orders WHERE status=? ORDER BY created_at DESC LIMIT 100"
    ).bind(status).all();
    return json({ orders: rows.results || [] });
  }

  const reviewMatch = url.pathname.match(/^\/admin\/orders\/([0-9a-f-]{36})\/(approve|reject)$/i);
  if (reviewMatch && request.method === "POST") {
    if (!env.ADMIN_API_KEY || request.headers.get("Authorization") !== `Bearer ${env.ADMIN_API_KEY}`) {
      return json({ error: "UNAUTHORIZED" }, 401);
    }
    const orderId = reviewMatch[1], action = reviewMatch[2];
    const order = await env.DB.prepare("SELECT * FROM license_orders WHERE id=?").bind(orderId).first();
    if (!order) return json({ error: "ORDER_NOT_FOUND" }, 404);
    if (order.status !== "pending") return json({ error: "ORDER_ALREADY_REVIEWED" }, 409);
    let body = {};
    try { body = await request.json(); } catch {}
    const note = cleanText(body.note, 500);
    const now = new Date().toISOString();
    if (action === "reject") {
      const result = await env.DB.prepare("UPDATE license_orders SET status='rejected',updated_at=?,reviewed_at=?,reviewer_note=? WHERE id=? AND status='pending'")
        .bind(now, now, note || "لم يتم تأكيد الدفع.", orderId).run();
      if (!result.meta || result.meta.changes !== 1) return json({ error: "ORDER_ALREADY_REVIEWED" }, 409);
      await audit(env, orderId, "order_rejected", "admin", note);
      return json({ orderId, status: "rejected" });
    }
    let issued;
    try {
      issued = await issueToken(env, order.device_id, order.plan);
    } catch (e) {
      const code = String(e && e.message || e);
      const safeCode = ["LICENSE_SIGNING_NOT_CONFIGURED", "LICENSE_PUBLIC_KEY_MISMATCH", "LICENSE_KEY_PAIR_MISMATCH"].includes(code) ? code : "LICENSE_SIGNING_NOT_CONFIGURED";
      return json({ error: safeCode }, 503);
    }
    const result = await env.DB.prepare("UPDATE license_orders SET status='approved',updated_at=?,reviewed_at=?,reviewer_note=?,activation_token=?,token_expires=? WHERE id=? AND status='pending'")
      .bind(now, now, note || "تمت مراجعة الدفع وإصدار الترخيص.", issued.token, issued.expires, orderId).run();
    // Concurrent admin requests must not issue multiple successful approvals for one order.
    if (!result.meta || result.meta.changes !== 1) return json({ error: "ORDER_ALREADY_REVIEWED" }, 409);
    await audit(env, orderId, "license_issued", "admin", `plan=${order.plan};version=${issued.appVersion}`);
    return json({ orderId, status: "approved", activationToken: issued.token, expiresAt: issued.expires, appVersion: issued.appVersion });
  }

  return json({ error: "NOT_FOUND" }, 404);
}
export default {
  async fetch(request, env) {
    try {
      const response = await handle(request, env);
      return withCors(response, request, env);
    } catch (e) {
      // Never log or return secrets, tokens, or private key material.
      return json({ error: "INTERNAL_ERROR" }, 500);
    }
  }
};
