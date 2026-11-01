const APP_ID = "com.mohammedmustafa.almizan.bakery";
const EXPECTED_APP_PUBLIC_KEY_DER_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvsK66CYxt4X7R9GcVaJ0FjrheMAkFpzbirdz7/X/lj8UAuqUabNOPN3R5dGuZro90i+raIyngTz+VxRxLcNPRQru4VzWIDLsoBEwi/CrsOaMJFgVLom7jYXFjQ5P25TO5hRxzPN8ZOOG1MTo8j6ipgPOKY5be6Gmsj8R03SbulYnVO8STWC4bSkqwvV9YlEIpZwnyAJVbWHbE4JSKiCC/TMOcfrBgGG0W7qCJeEjnUKPfHAtH85ayZjBQwMIQx+z1CoTFWrqKXywSWN65G7yZbHwsnREovRprxjyVHqmTtLd45VyNQU2zXBKchzkwnpiG1zeD8kfdDeACeaAmxbsiwIDAQAB";
const PLANS = {
  monthly: { amount: 55000, seconds: 30 * 86400 },
  annual: { amount: 550000, seconds: 365 * 86400 },
  lifetime: { amount: 1050000, seconds: 0 }
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
      "access-control-allow-headers": "content-type",
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
function cookieValue(request,name){const raw=request.headers.get("Cookie")||"";const part=raw.split(";").map(v=>v.trim()).find(v=>v.startsWith(name+"="));return part?part.slice(name.length+1):"";}
function sameOriginPost(request,url){return request.method==="POST"&&request.headers.get("Origin")===url.origin;}
function constantTimeEqual(a,b){const x=encoder.encode(String(a)),y=encoder.encode(String(b));let d=x.length^y.length;for(let i=0;i<Math.max(x.length,y.length);i++)d|=(x[i]||0)^(y[i]||0);return d===0;}
async function sessionKey(secret){const material=await crypto.subtle.digest("SHA-256",encoder.encode("Al-Mizan-Admin-Session-v1:"+secret));return crypto.subtle.importKey("raw",material,{name:"HMAC",hash:"SHA-256"},false,["sign","verify"]);}
async function createAdminSession(env){const payload=base64url(encoder.encode(JSON.stringify({exp:Math.floor(Date.now()/1000)+28800,nonce:base64url(crypto.getRandomValues(new Uint8Array(24)))})));const key=await sessionKey(env.ADMIN_PASSWORD);const sig=base64url(await crypto.subtle.sign("HMAC",key,encoder.encode(payload)));return payload+"."+sig;}
async function verifyAdminSession(request,env){if(!env.ADMIN_PASSWORD||env.ADMIN_PASSWORD.length<32)return false;const p=cookieValue(request,"mizan_admin_session").split(".");if(p.length!==2)return false;try{const key=await sessionKey(env.ADMIN_PASSWORD);const sig=p[1].replace(/-/g,"+").replace(/_/g,"/")+"=".repeat((4-p[1].length%4)%4);if(!await crypto.subtle.verify("HMAC",key,fromBase64(sig),encoder.encode(p[0])))return false;const raw=p[0].replace(/-/g,"+").replace(/_/g,"/")+"=".repeat((4-p[0].length%4)%4);const data=JSON.parse(new TextDecoder().decode(fromBase64(raw)));return Number.isInteger(data.exp)&&data.exp>Math.floor(Date.now()/1000)&&typeof data.nonce==="string";}catch{return false;}}
async function loginRateState(request,env){await env.DB.prepare("CREATE TABLE IF NOT EXISTS admin_login_attempts (ip_hash TEXT PRIMARY KEY, window_start INTEGER NOT NULL, attempts INTEGER NOT NULL)").run();const ip=request.headers.get("CF-Connecting-IP")||"unknown";const digest=await crypto.subtle.digest("SHA-256",encoder.encode(ip+"|"+env.ADMIN_PASSWORD));const ipHash=base64url(digest),now=Math.floor(Date.now()/1000);const row=await env.DB.prepare("SELECT window_start,attempts FROM admin_login_attempts WHERE ip_hash=?").bind(ipHash).first();return{ipHash,now,blocked:Boolean(row&&row.window_start>now-900&&row.attempts>=8)};}
async function recordLoginFailure(env,s){await env.DB.prepare("INSERT INTO admin_login_attempts(ip_hash,window_start,attempts) VALUES(?,?,1) ON CONFLICT(ip_hash) DO UPDATE SET attempts=CASE WHEN admin_login_attempts.window_start<=? THEN 1 ELSE admin_login_attempts.attempts+1 END,window_start=CASE WHEN admin_login_attempts.window_start<=? THEN ? ELSE admin_login_attempts.window_start END").bind(s.ipHash,s.now,s.now-900,s.now-900,s.now).run();}
async function clearLoginFailures(env,s){await env.DB.prepare("DELETE FROM admin_login_attempts WHERE ip_hash=?").bind(s.ipHash).run();}
const ADMIN_LOGIN_HTML=`<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>دخول إدارة التراخيص</title><style>body{margin:0;background:#f2f5fa;color:#14243a;font-family:system-ui,Tahoma,Arial;display:grid;min-height:100vh;place-items:center}.box{background:white;width:min(420px,calc(100% - 36px));padding:25px;border-radius:16px;box-shadow:0 8px 32px #10243a18}h2{margin-top:0}label{display:block;margin:12px 0 5px}input,button{font:inherit;width:100%;padding:12px;border-radius:9px;box-sizing:border-box}input{border:1px solid #bbc7d5}button{margin-top:16px;border:0;background:#d4af37;color:#0b1f3a;font-weight:800}p{color:#66758a;font-size:14px}#msg{color:#a32121;min-height:24px}</style><main class="box"><h2>إدارة تراخيص الميزان للمخابز</h2><p>سجّل الدخول للوصول إلى طلبات العملاء وإصدار التراخيص.</p><form id="f"><label for="pw">كلمة مرور الإدارة</label><input id="pw" type="password" minlength="32" maxlength="256" autocomplete="current-password" required><button id="go">تسجيل الدخول</button></form><p id="msg" role="status"></p></main><script>const f=document.getElementById('f'),pw=document.getElementById('pw'),msg=document.getElementById('msg'),go=document.getElementById('go');f.addEventListener('submit',async e=>{e.preventDefault();go.disabled=true;msg.textContent='جارٍ التحقق…';try{const r=await fetch('/admin/login',{method:'POST',credentials:'same-origin',headers:{'content-type':'application/json'},body:JSON.stringify({password:pw.value})});const d=await r.json().catch(()=>({}));if(!r.ok)throw new Error(d.error==='TOO_MANY_ATTEMPTS'?'محاولات كثيرة. انتظر 15 دقيقة ثم حاول مجدداً.':'تعذر تسجيل الدخول. تحقق من كلمة المرور أو إعداد الخدمة.');location.replace('/admin/panel')}catch(err){msg.textContent=err.message;pw.value='';pw.focus();go.disabled=false}});</script></html>`;
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
function signingDiagnostics(env) {
  const privateValue = typeof env.LICENSE_PRIVATE_KEY_PKCS8_B64 === "string" ? env.LICENSE_PRIVATE_KEY_PKCS8_B64.trim() : "";
  const publicValue = typeof env.LICENSE_PUBLIC_KEY_DER_B64 === "string" ? env.LICENSE_PUBLIC_KEY_DER_B64.trim() : "";
  return {
    privateKeyConfigured: privateValue.length > 0,
    publicKeyConfigured: publicValue.length > 0,
    publicKeyMatchesApp: publicValue.replace(/\\s+/g, "") === EXPECTED_APP_PUBLIC_KEY_DER_B64
  };
}
function decodeBase64Value(value) {
  return fromBase64(value.replace(/\\s+/g, ""));
}
async function importPrivateKey(env) {
  const diagnostics = signingDiagnostics(env);
  if (!diagnostics.privateKeyConfigured || !diagnostics.publicKeyConfigured) {
    throw new Error("LICENSE_SIGNING_NOT_CONFIGURED");
  }
  const privateValue = env.LICENSE_PRIVATE_KEY_PKCS8_B64.trim();
  const publicValue = env.LICENSE_PUBLIC_KEY_DER_B64.trim().replace(/\\s+/g, "");
  if (publicValue !== EXPECTED_APP_PUBLIC_KEY_DER_B64) {
    throw new Error("LICENSE_PUBLIC_KEY_MISMATCH");
  }
  let privateKey;
  try {
    privateKey = await crypto.subtle.importKey(
      "pkcs8",
      decodeBase64Value(privateValue),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["sign"]
    );
  } catch {
    throw new Error("LICENSE_PRIVATE_KEY_INVALID");
  }
  let publicKey;
  try {
    publicKey = await crypto.subtle.importKey(
      "spki",
      decodeBase64Value(publicValue),
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      true,
      ["verify"]
    );
  } catch {
    throw new Error("LICENSE_PUBLIC_KEY_INVALID");
  }
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
  const appVersion = cleanText(env.APP_VERSION || "2.6.15", 32);
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

  if (url.pathname === "/admin/login" && request.method === "GET") return new Response(ADMIN_LOGIN_HTML,{headers:{"content-type":"text/html; charset=utf-8","cache-control":"no-store, private","x-content-type-options":"nosniff","referrer-policy":"no-referrer"}});
  if (url.pathname === "/admin/login" && request.method === "POST") {
    if (!sameOriginPost(request,url)) return json({error:"INVALID_ORIGIN"},403);
    if (!env.ADMIN_PASSWORD||env.ADMIN_PASSWORD.length<32) return json({error:"ADMIN_LOGIN_NOT_CONFIGURED"},503);
    let body;try{body=await request.json();}catch{return json({error:"INVALID_JSON"},400);}
    const password=typeof body.password==="string"?body.password:"";if(password.length>256)return json({error:"INVALID_CREDENTIALS"},401);
    const state=await loginRateState(request,env);if(state.blocked)return json({error:"TOO_MANY_ATTEMPTS"},429);
    if(!constantTimeEqual(password,env.ADMIN_PASSWORD)){await recordLoginFailure(env,state);return json({error:"INVALID_CREDENTIALS"},401);}
    await clearLoginFailures(env,state);const token=await createAdminSession(env);
    return json({ok:true},200,{"set-cookie":"mizan_admin_session="+token+"; Path=/admin; HttpOnly; Secure; SameSite=Strict; Max-Age=28800"});
  }
  if (url.pathname === "/admin/logout" && request.method === "POST") {
    if(!sameOriginPost(request,url))return json({error:"INVALID_ORIGIN"},403);
    return json({ok:true},200,{"set-cookie":"mizan_admin_session=; Path=/admin; HttpOnly; Secure; SameSite=Strict; Max-Age=0"});
  }
  if (url.pathname === "/admin/panel" && request.method === "GET") {
    if(!await verifyAdminSession(request,env))return Response.redirect(new URL("/admin/login",url).toString(),302);
    try{
      const panel=await fetch("https://moh1442010-arch.github.io/smart-encyclopedias-platform/store/license-admin.html",{headers:{"accept":"text/html","user-agent":"Al-Mizan-Admin-Panel"}});
      if(!panel.ok)return json({error:"ADMIN_PANEL_UNAVAILABLE"},502);
      return new Response(await panel.text(),{headers:{"content-type":"text/html; charset=utf-8","cache-control":"no-store, private","x-content-type-options":"nosniff","referrer-policy":"no-referrer"}});
    }catch{return json({error:"ADMIN_PANEL_UNAVAILABLE"},502);}
  }

  if (url.pathname === "/diagnostics/admin-config" && request.method === "GET") {
    return json({
      adminPasswordConfigured: Boolean(env.ADMIN_PASSWORD && env.ADMIN_PASSWORD.length >= 32),
      appVersion: cleanText(env.APP_VERSION || "2.6.15", 32)
    });
  }

  if (url.pathname === "/diagnostics/signing-config" && request.method === "GET") {
    return json({
      ...signingDiagnostics(env),
      appVersion: cleanText(env.APP_VERSION || "2.6.15", 32)
    });
  }

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
    const appVersion = cleanText(env.APP_VERSION || "2.6.15", 32);
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

  if (url.pathname === "/admin/signing-check" && request.method === "GET") {
    if (!await verifyAdminSession(request,env)) return json({error:"UNAUTHORIZED"},401);
    try {
      await importPrivateKey(env);
      return json({ ok: true, publicKeyMatchesApp: true, appVersion: cleanText(env.APP_VERSION || "2.6.15", 32) });
    } catch (e) {
      const code = String(e && e.message || e);
      const allowedCodes = ["LICENSE_SIGNING_NOT_CONFIGURED", "LICENSE_PUBLIC_KEY_MISMATCH", "LICENSE_KEY_PAIR_MISMATCH", "LICENSE_PRIVATE_KEY_INVALID", "LICENSE_PUBLIC_KEY_INVALID"];
      const safeCode = allowedCodes.includes(code) ? code : "LICENSE_SIGNING_INTERNAL_ERROR";
      return json({ ok: false, error: safeCode, diagnostic: signingDiagnostics(env) }, 503);
    }
  }

  if (url.pathname === "/admin/orders" && request.method === "GET") {
    if (!await verifyAdminSession(request,env)) {
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
    if(!sameOriginPost(request,url))return json({error:"INVALID_ORIGIN"},403);
    if (!await verifyAdminSession(request,env)) {
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
      const allowedCodes = ["LICENSE_SIGNING_NOT_CONFIGURED", "LICENSE_PUBLIC_KEY_MISMATCH", "LICENSE_KEY_PAIR_MISMATCH", "LICENSE_PRIVATE_KEY_INVALID", "LICENSE_PUBLIC_KEY_INVALID"];
      const safeCode = allowedCodes.includes(code) ? code : "LICENSE_SIGNING_INTERNAL_ERROR";
      return json({ error: safeCode, diagnostic: signingDiagnostics(env) }, 503);
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
