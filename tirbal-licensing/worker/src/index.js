const APP_ID = "com.mohammedmustafa.tirbal";
const APP_VERSION = "1.3.0";
const PUBLIC_KEY_DER_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtXiJgeh0o+EMZfmqPnB5+ewloD9QpftQv9S3SWI2WwG/zr748BOXMI1VXf2xbHzsLpyBHqivm4YGvsmFqurUx2oKF9Bekyy6rjLVQ7DrTTB/jVhIVDoeJPah+rQmrPvZfdqYzKokY0TqztElkWW0bwq+dzuEbmU17J3NfLQM8iXjXIvSqcLLd59syK/bEQn90JmejKarE/u/6zw4Btb5fSxgb7Up6nVczvsaMiJoq7BAuMdEHH8SWV0f75VAZzSv45L4HjhWP9aSyEVl6aQ8ClAjgqbkDFEJj9hY6G86sClLSFgDqwwzDOObLa6rJQN3JuZVJb2Tvyqf5XTfFXLcFwIDAQAB";
const ADMIN_HTML = `<!doctype html><html lang="ar" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow"><title>إدارة تراخيص حساب التربال</title><style>body{font-family:Tahoma,Arial;background:#f3f6f2;color:#183d2b;margin:0;padding:18px}.box{max-width:900px;margin:auto;background:white;padding:18px;border-radius:14px}input,button{font:inherit;padding:10px;margin:5px;border-radius:8px}button{background:#1c683a;color:white;border:0}table{width:100%;border-collapse:collapse}label{display:block;margin:8px 4px}textarea{display:block;width:95%;min-height:65px;font:inherit;padding:8px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr))}td,th{padding:9px;border-bottom:1px solid #ddd;text-align:right}small{color:#666}</style><main class="box"><h2>إدارة تراخيص حساب التربال</h2><section id="login"><input id="pw" type="password" placeholder="كلمة مرور الإدارة"><button id="in">دخول</button></section><section id="panel" hidden><button id="refresh">تحديث الطلبات</button><div id="orders">لا توجد طلبات معروضة</div><hr><h3>إعدادات التطبيق عن بُعد</h3><p>تعديل الأسعار ومدة التجربة والتحديثات دون إصدار APK جديد.</p><div class="grid"><label>السعر الشهري<input id="monthlyPrice" type="number" min="1"></label><label>السعر السنوي<input id="annualPrice" type="number" min="1"></label><label>السعر الدائم<input id="lifetimePrice" type="number" min="1"></label><label>التجربة بالأيام<input id="trialDays" type="number" min="0" max="90"></label><label>أحدث إصدار<input id="latestVersion"></label><label>أقل إصدار مدعوم<input id="minSupportedVersion"></label></div><label>رابط تنزيل التحديث HTTPS<input id="downloadUrl" type="url"></label><label>ملاحظات الإصدار<textarea id="releaseNotes"></textarea></label><label>إعلان يظهر داخل التطبيق<textarea id="announcement"></textarea></label><label><input id="maintenanceMode" type="checkbox"> وضع الصيانة</label><button id="saveSettings">حفظ الإعدادات ونشرها</button><p id="settingsMsg"></p></section><p id="msg"></p></main><script>const $=id=>document.getElementById(id),msg=$("msg");$("in").onclick=async()=>{const r=await fetch("/admin/login",{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({password:$("pw").value})});if(!r.ok){msg.textContent="تعذر تسجيل الدخول";return} $("login").hidden=true;$("panel").hidden=false;load();loadSettings()};async function load(){const r=await fetch("/admin/orders?status=pending");if(!r.ok){msg.textContent="انتهت الجلسة أو تعذر تحميل الطلبات";return}const d=await r.json();$("orders").innerHTML="<table><tr><th>العميل</th><th>الخطة</th><th>المبلغ</th><th>مرجع التحويل</th><th>الإجراء</th></tr>"+(d.orders||[]).map(o=>"<tr><td>"+esc(o.customer_name)+"<br><small>"+esc(o.contact)+"</small></td><td>"+esc(o.plan)+"</td><td>"+o.amount_sdg.toLocaleString("en-US")+" ج.س</td><td>"+esc(o.receipt_reference)+"</td><td><button data-id='"+esc(o.id)+"'>اعتماد بعد التحقق</button></td></tr>").join("")+"</table>";document.querySelectorAll("[data-id]").forEach(b=>b.onclick=async()=>{if(!confirm("هل تحققت من وصول المبلغ فعلياً؟"))return;const r=await fetch("/admin/orders/"+b.dataset.id+"/approve",{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({note:"تم التحقق من التحويل"})});const d=await r.json();msg.textContent=r.ok?"تم تفعيل الترخيص. يلتقط التطبيق التفعيل عند التحقق القادم.":("تعذر التفعيل: "+(d.error||"خطأ"));load()})}async function loadSettings(){const r=await fetch("/admin/settings");if(!r.ok){$("settingsMsg").textContent="تعذر تحميل الإعدادات";return}const s=(await r.json()).settings;for(const k of ["monthlyPrice","annualPrice","lifetimePrice","trialDays","latestVersion","minSupportedVersion","downloadUrl","releaseNotes","announcement"])$(k).value=s[k]??"";$("maintenanceMode").checked=!!s.maintenanceMode}$("saveSettings").onclick=async()=>{const body={monthlyPrice:Number($("monthlyPrice").value),annualPrice:Number($("annualPrice").value),lifetimePrice:Number($("lifetimePrice").value),trialDays:Number($("trialDays").value),latestVersion:$("latestVersion").value,minSupportedVersion:$("minSupportedVersion").value,downloadUrl:$("downloadUrl").value,releaseNotes:$("releaseNotes").value,announcement:$("announcement").value,maintenanceMode:$("maintenanceMode").checked};const r=await fetch("/admin/settings",{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify(body)});const d=await r.json();$("settingsMsg").textContent=r.ok?"تم حفظ الإعدادات. ستصل إلى التطبيق عند فحص الاتصال القادم.":"تعذر الحفظ: "+(d.error||"تحقق من القيم")};function esc(v){return String(v??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]))}$("refresh").onclick=load;</script></html>`;
const PLANS = {
  monthly: { amount: 25000, seconds: 30 * 86400 },
  annual: { amount: 250000, seconds: 365 * 86400 },
  lifetime: { amount: 500000, seconds: 0 }
};
const enc = new TextEncoder();
const b64url = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/\+/g,"-").replace(/\//g,"_").replace(/=+$/g,"");
const fromB64 = value => Uint8Array.from(atob(value), c => c.charCodeAt(0));
const json = (data,status=200,headers={}) => new Response(JSON.stringify(data),{status,headers:{"content-type":"application/json; charset=utf-8","cache-control":"no-store",...headers}});
const clean = (v,n=120) => typeof v==="string" ? v.trim().slice(0,n) : "";
const validDevice = v => /^[A-Za-z0-9_-]{8,100}$/.test(v);
const planExpiry = (plan,now) => plan==="lifetime" ? 0 : now+PLANS[plan].seconds;
const DEFAULT_SETTINGS = {
  monthlyPrice:25000, annualPrice:250000, lifetimePrice:500000, trialDays:15,
  latestVersion:APP_VERSION, minSupportedVersion:"1.0.0", downloadUrl:"",
  releaseNotes:"", announcement:"", maintenanceMode:false
};
async function loadSettings(env) {
  const rows=await env.DB.prepare("SELECT setting_key,setting_value FROM tirbal_settings").all();
  const values={...DEFAULT_SETTINGS};
  for(const row of rows.results||[]) {
    if(Object.prototype.hasOwnProperty.call(values,row.setting_key)) {
      const raw=row.setting_value;
      if(["maintenanceMode"].includes(row.setting_key)) values[row.setting_key]=raw==="true";
      else if(["monthlyPrice","annualPrice","lifetimePrice","trialDays"].includes(row.setting_key)) values[row.setting_key]=Number(raw);
      else values[row.setting_key]=raw;
    }
  }
  return values;
}
const priceFor = (settings,plan) => plan==="monthly"?settings.monthlyPrice:plan==="annual"?settings.annualPrice:settings.lifetimePrice;
async function saveSettings(env,settings) {
  const now=Math.floor(Date.now()/1000);
  const statements=Object.entries(settings).map(([key,value])=>env.DB.prepare("INSERT INTO tirbal_settings(setting_key,setting_value,updated_at) VALUES(?,?,?) ON CONFLICT(setting_key) DO UPDATE SET setting_value=excluded.setting_value,updated_at=excluded.updated_at").bind(key,String(value),now));
  await env.DB.batch(statements);
}
function validVersion(v){return typeof v==="string"&&/^\\d+\\.\\d+\\.\\d+$/.test(v);}
function validUpdateUrl(v){if(!v)return true;try{const u=new URL(v);return u.protocol==="https:";}catch{return false;}}


async function schema(env) {
  await env.DB.batch([
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tirbal_devices (device_id TEXT PRIMARY KEY, first_seen INTEGER NOT NULL, trial_expires INTEGER NOT NULL, plan TEXT, license_expires INTEGER, activation_token TEXT, updated_at INTEGER NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tirbal_orders (id TEXT PRIMARY KEY, created_at INTEGER NOT NULL, customer_name TEXT NOT NULL, contact TEXT NOT NULL, device_id TEXT NOT NULL, plan TEXT NOT NULL, amount_sdg INTEGER NOT NULL, receipt_reference TEXT NOT NULL, status TEXT NOT NULL, reviewed_at INTEGER, reviewer_note TEXT, activation_token TEXT, token_expires INTEGER, app_version TEXT NOT NULL)"),
    env.DB.prepare("CREATE UNIQUE INDEX IF NOT EXISTS idx_tirbal_receipt_active ON tirbal_orders(receipt_reference) WHERE status IN ('pending','approved')"),
    env.DB.prepare("CREATE INDEX IF NOT EXISTS idx_tirbal_orders_status ON tirbal_orders(status,created_at DESC)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tirbal_audit (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id TEXT NOT NULL, event TEXT NOT NULL, actor TEXT NOT NULL, created_at INTEGER NOT NULL, detail TEXT)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tirbal_admin_attempts (ip_hash TEXT PRIMARY KEY, window_start INTEGER NOT NULL, attempts INTEGER NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tirbal_order_attempts (ip_hash TEXT PRIMARY KEY, window_start INTEGER NOT NULL, attempts INTEGER NOT NULL)"),
    env.DB.prepare("CREATE TABLE IF NOT EXISTS tirbal_settings (setting_key TEXT PRIMARY KEY, setting_value TEXT NOT NULL, updated_at INTEGER NOT NULL)")
  ]);
}
function privateDer(value) {
  const raw=String(value||"").trim().replace(/\s+/g,"");
  if(!/^[A-Za-z0-9+/]+={0,2}$/.test(raw)) throw new Error("SIGNING_KEY_INVALID");
  const der=fromB64(raw);
  if(der.length<500 || der[0]!==0x30) throw new Error("SIGNING_KEY_INVALID");
  return der;
}
async function signingKey(env) {
  if(!env.TIRBAL_LICENSE_PRIVATE_KEY_PKCS8_B64) throw new Error("SIGNING_NOT_CONFIGURED");
  return crypto.subtle.importKey("pkcs8",privateDer(env.TIRBAL_LICENSE_PRIVATE_KEY_PKCS8_B64),{name:"RSASSA-PKCS1-v1_5",hash:"SHA-256"},false,["sign"]);
}
async function issue(env,device,plan,expires) {
  const payload=enc.encode("app="+APP_ID+";device="+device+";plan="+plan+";expires="+expires);
  const signature=await crypto.subtle.sign("RSASSA-PKCS1-v1_5",await signingKey(env),payload);
  return b64url(payload)+"."+b64url(signature);
}
async function allowOrder(request,env) {
  const ip=request.headers.get("CF-Connecting-IP")||"unknown";
  const digest=await crypto.subtle.digest("SHA-256",enc.encode(ip+"|TirbalOrderRate-v1"));
  const hash=b64url(digest),now=Math.floor(Date.now()/1000);
  const row=await env.DB.prepare("INSERT INTO tirbal_order_attempts(ip_hash,window_start,attempts) VALUES(?,?,1) ON CONFLICT(ip_hash) DO UPDATE SET attempts=CASE WHEN tirbal_order_attempts.window_start<=? THEN 1 ELSE tirbal_order_attempts.attempts+1 END,window_start=CASE WHEN tirbal_order_attempts.window_start<=? THEN ? ELSE tirbal_order_attempts.window_start END RETURNING attempts").bind(hash,now,now-900,now-900,now).first();
  return Boolean(row&&row.attempts<=8);
}
async function audit(env,id,event,actor,detail="") {
  await env.DB.prepare("INSERT INTO tirbal_audit(order_id,event,actor,created_at,detail) VALUES(?,?,?,?,?)").bind(id,event,actor,Math.floor(Date.now()/1000),detail.slice(0,400)).run();
}
function cookie(request,name) {
  const part=(request.headers.get("Cookie")||"").split(";").map(x=>x.trim()).find(x=>x.startsWith(name+"="));
  return part ? part.slice(name.length+1) : "";
}
function adminPassword(env) { return typeof env.TIRBAL_ADMIN_PASSWORD==="string" ? env.TIRBAL_ADMIN_PASSWORD : ""; }
async function hmacKey(env) {
  const raw=await crypto.subtle.digest("SHA-256",enc.encode("TirbalAdminSession-v1:"+adminPassword(env)));
  return crypto.subtle.importKey("raw",raw,{name:"HMAC",hash:"SHA-256"},false,["sign","verify"]);
}
async function session(env) {
  const p=b64url(enc.encode(JSON.stringify({exp:Math.floor(Date.now()/1000)+28800,nonce:b64url(crypto.getRandomValues(new Uint8Array(16)))})));
  const s=b64url(await crypto.subtle.sign("HMAC",await hmacKey(env),enc.encode(p)));
  return p+"."+s;
}
function constantEqual(a,b) {
  const x=enc.encode(String(a)),y=enc.encode(String(b));let d=x.length^y.length;
  for(let i=0;i<Math.max(x.length,y.length);i++)d|=(x[i]||0)^(y[i]||0);
  return d===0;
}
function sameOriginPost(request,url) { return request.method==="POST" && request.headers.get("Origin")===url.origin; }
async function loginState(request,env) {
  const ip=request.headers.get("CF-Connecting-IP")||"unknown";
  const digest=await crypto.subtle.digest("SHA-256",enc.encode(ip+"|"+adminPassword(env)));
  const hash=b64url(digest),now=Math.floor(Date.now()/1000);
  const row=await env.DB.prepare("SELECT window_start,attempts FROM tirbal_admin_attempts WHERE ip_hash=?").bind(hash).first();
  return {hash,now,blocked:Boolean(row&&row.window_start>now-900&&row.attempts>=8)};
}
async function recordLoginFailure(env,s) {
  await env.DB.prepare("INSERT INTO tirbal_admin_attempts(ip_hash,window_start,attempts) VALUES(?,?,1) ON CONFLICT(ip_hash) DO UPDATE SET attempts=CASE WHEN tirbal_admin_attempts.window_start<=? THEN 1 ELSE tirbal_admin_attempts.attempts+1 END,window_start=CASE WHEN tirbal_admin_attempts.window_start<=? THEN ? ELSE tirbal_admin_attempts.window_start END").bind(s.hash,s.now,s.now-900,s.now-900,s.now).run();
}
async function admin(request,env) {
  if(adminPassword(env).length<32) return false;
  const parts=cookie(request,"tirbal_admin").split(".");
  if(parts.length!==2) return false;
  try {
    const sig=parts[1].replace(/-/g,"+").replace(/_/g,"/")+"=".repeat((4-parts[1].length%4)%4);
    if(!await crypto.subtle.verify("HMAC",await hmacKey(env),fromB64(sig),enc.encode(parts[0]))) return false;
    const p=parts[0].replace(/-/g,"+").replace(/_/g,"/")+"=".repeat((4-parts[0].length%4)%4);
    return JSON.parse(new TextDecoder().decode(fromB64(p))).exp>Math.floor(Date.now()/1000);
  } catch { return false; }
}
async function publicConfig(env,settings) {
  return {
    appId:APP_ID,appVersion:APP_VERSION,latestVersion:settings.latestVersion,minSupportedVersion:settings.minSupportedVersion,
    trialDays:settings.trialDays,currency:"SDG",maintenanceMode:settings.maintenanceMode,
    announcement:settings.announcement,releaseNotes:settings.releaseNotes,downloadUrl:settings.downloadUrl,
    plans:{monthly:{amount:settings.monthlyPrice,days:30},annual:{amount:settings.annualPrice,days:365},lifetime:{amount:settings.lifetimePrice,days:0}},
    payment:{bankName:clean(env.PAYMENT_BANK_NAME,100),accountName:clean(env.PAYMENT_ACCOUNT_NAME,120),accountNumber:clean(env.PAYMENT_ACCOUNT_NUMBER,80),whatsapp:clean(env.PAYMENT_WHATSAPP,30)}
  };
}
async function handle(request,env) {
  const u=new URL(request.url);
  if(!env.DB) return json({error:"SERVICE_NOT_CONFIGURED"},503);
  await schema(env);
  const settings=await loadSettings(env);
  if(u.pathname==="/admin"&&request.method==="GET") return new Response(ADMIN_HTML,{headers:{"content-type":"text/html; charset=utf-8","cache-control":"no-store","x-frame-options":"DENY","x-content-type-options":"nosniff","referrer-policy":"no-referrer"}});
  if(u.pathname==="/health"&&request.method==="GET") return json({ok:true,service:"hisab-al-tirbal-licensing",mode:"manual-payment-review",appVersion:APP_VERSION});
  if(u.pathname==="/api/config"&&request.method==="GET") return json(await publicConfig(env,settings));
  if(u.pathname==="/diagnostics/signing-pair"&&request.method==="GET") {
    try {
      const priv=await signingKey(env);
      const pub=await crypto.subtle.importKey("spki",fromB64(PUBLIC_KEY_DER_B64),{name:"RSASSA-PKCS1-v1_5",hash:"SHA-256"},false,["verify"]);
      const probe=enc.encode("Hisab Al-Tirbal signing pair check");
      const sig=await crypto.subtle.sign("RSASSA-PKCS1-v1_5",priv,probe);
      return json({signingReady:await crypto.subtle.verify("RSASSA-PKCS1-v1_5",pub,sig,probe)});
    } catch { return json({signingReady:false,error:"SIGNING_NOT_READY"},503); }
  }
  if(u.pathname==="/api/bootstrap"&&request.method==="POST") {
    let body;try{body=await request.json();}catch{return json({error:"INVALID_JSON"},400);}
    const device=clean(body.deviceId,100);
    if(!validDevice(device)) return json({error:"INVALID_DEVICE"},400);
    const now=Math.floor(Date.now()/1000);
    let row=await env.DB.prepare("SELECT * FROM tirbal_devices WHERE device_id=?").bind(device).first();
    if(!row) {
      const expires=now+settings.trialDays*86400;
      await env.DB.prepare("INSERT OR IGNORE INTO tirbal_devices(device_id,first_seen,trial_expires,updated_at) VALUES(?,?,?,?)").bind(device,now,expires,now).run();
      row=await env.DB.prepare("SELECT * FROM tirbal_devices WHERE device_id=?").bind(device).first();
    }
    if(row.activation_token && (row.license_expires===0 || row.license_expires>now)) return json({status:"licensed",token:row.activation_token,serverTime:now});
    if(row.trial_expires>now) {
      const token=await issue(env,device,"trial",row.trial_expires);
      return json({status:"trial",token,expires:row.trial_expires,serverTime:now});
    }
    return json({status:"expired",trialExpiredAt:row.trial_expires,serverTime:now});
  }
  if(u.pathname==="/api/license"&&request.method==="GET") {
    const device=clean(u.searchParams.get("deviceId"),100);
    if(!validDevice(device)) return json({error:"INVALID_DEVICE"},400);
    const now=Math.floor(Date.now()/1000);
    const row=await env.DB.prepare("SELECT trial_expires,license_expires,activation_token FROM tirbal_devices WHERE device_id=?").bind(device).first();
    if(!row) return json({error:"DEVICE_NOT_REGISTERED"},404);
    if(row.activation_token && (row.license_expires===0 || row.license_expires>now)) return json({status:"licensed",token:row.activation_token,serverTime:now});
    if(row.trial_expires>now) return json({status:"trial",token:await issue(env,device,"trial",row.trial_expires),expires:row.trial_expires,serverTime:now});
    return json({status:"expired",serverTime:now});
  }
  if(u.pathname==="/api/orders"&&request.method==="POST") {
    let b;try{b=await request.json();}catch{return json({error:"INVALID_JSON"},400);}
    const name=clean(b.customerName,100),contact=clean(b.contact,25),device=clean(b.deviceId,100);
    const plan=clean(b.plan,20),receipt=clean(b.receiptReference,100);
    if(name.length<2||!/^\+?[0-9()\s-]{8,25}$/.test(contact)||!validDevice(device)||!PLANS[plan]||!/^[A-Za-z0-9\-_/]{4,100}$/.test(receipt)) return json({error:"INVALID_FIELDS"},400);
    const exists=await env.DB.prepare("SELECT 1 FROM tirbal_devices WHERE device_id=?").bind(device).first();
    if(!exists) return json({error:"DEVICE_NOT_REGISTERED"},409);
    if(!await allowOrder(request,env)) return json({error:"ORDER_RATE_LIMITED"},429);
    const duplicate=await env.DB.prepare("SELECT id FROM tirbal_orders WHERE receipt_reference=? AND status IN ('pending','approved')").bind(receipt).first();
    if(duplicate) return json({error:"RECEIPT_REFERENCE_ALREADY_USED"},409);
    const id=crypto.randomUUID(),now=Math.floor(Date.now()/1000);
    await env.DB.prepare("INSERT INTO tirbal_orders(id,created_at,customer_name,contact,device_id,plan,amount_sdg,receipt_reference,status,app_version) VALUES(?,?,?,?,?,?,?,?,?,?)").bind(id,now,name,contact,device,plan,priceFor(settings,plan),receipt,"pending",APP_VERSION).run();
    await audit(env,id,"order_created","customer","Payment requires manual verification.");
    return json({orderId:id,status:"pending",amountSdg:PLANS[plan].amount,message:"تم تسجيل طلبك. التفعيل بعد تأكيد الدفع ومراجعة الإدارة."},201);
  }
  if(u.pathname==="/admin/login"&&request.method==="POST") {
    if(!sameOriginPost(request,u))return json({error:"INVALID_ORIGIN"},403);
    let b;try{b=await request.json();}catch{return json({error:"INVALID_JSON"},400);}
    if(adminPassword(env).length<32)return json({error:"ADMIN_NOT_CONFIGURED"},503);
    const state=await loginState(request,env);
    if(state.blocked)return json({error:"TOO_MANY_ATTEMPTS"},429);
    if(!constantEqual(typeof b.password==="string"?b.password:"",adminPassword(env))) {
      await recordLoginFailure(env,state);return json({error:"INVALID_CREDENTIALS"},401);
    }
    await env.DB.prepare("DELETE FROM tirbal_admin_attempts WHERE ip_hash=?").bind(state.hash).run();
    return json({ok:true},200,{"set-cookie":"tirbal_admin="+await session(env)+"; Path=/admin; HttpOnly; Secure; SameSite=Strict; Max-Age=28800"});
  }
  if(u.pathname==="/admin/settings"&&request.method==="GET") {
    if(!await admin(request,env)) return json({error:"UNAUTHORIZED"},401);
    return json({settings});
  }
  if(u.pathname==="/admin/settings"&&request.method==="POST") {
    if(!sameOriginPost(request,u))return json({error:"INVALID_ORIGIN"},403);
    if(!await admin(request,env)) return json({error:"UNAUTHORIZED"},401);
    let b;try{b=await request.json();}catch{return json({error:"INVALID_JSON"},400);}
    const next={
      monthlyPrice:Number(b.monthlyPrice),annualPrice:Number(b.annualPrice),lifetimePrice:Number(b.lifetimePrice),
      trialDays:Number(b.trialDays),latestVersion:clean(b.latestVersion,20),minSupportedVersion:clean(b.minSupportedVersion,20),
      downloadUrl:clean(b.downloadUrl,500),releaseNotes:clean(b.releaseNotes,2000),announcement:clean(b.announcement,500),
      maintenanceMode:b.maintenanceMode===true
    };
    if(![next.monthlyPrice,next.annualPrice,next.lifetimePrice].every(n=>Number.isSafeInteger(n)&&n>=1&&n<=1000000000)||
       !Number.isInteger(next.trialDays)||next.trialDays<0||next.trialDays>90||
       !validVersion(next.latestVersion)||!validVersion(next.minSupportedVersion)||!validUpdateUrl(next.downloadUrl))
      return json({error:"INVALID_SETTINGS"},400);
    await saveSettings(env,next);
    return json({ok:true,settings:next});
  }
  if(u.pathname==="/admin/orders"&&request.method==="GET") {
    if(!await admin(request,env)) return json({error:"UNAUTHORIZED"},401);
    const status=u.searchParams.get("status")||"pending";
    if(!["pending","approved","rejected"].includes(status)) return json({error:"INVALID_STATUS"},400);
    const rows=await env.DB.prepare("SELECT id,created_at,customer_name,contact,device_id,plan,amount_sdg,receipt_reference,status FROM tirbal_orders WHERE status=? ORDER BY created_at DESC LIMIT 100").bind(status).all();
    return json({orders:rows.results||[]});
  }
  const review=u.pathname.match(/^\/admin\/orders\/([0-9a-f-]{36})\/(approve|reject)$/i);
  if(review&&request.method==="POST") {
    if(!sameOriginPost(request,u))return json({error:"INVALID_ORIGIN"},403);
    if(!await admin(request,env)) return json({error:"UNAUTHORIZED"},401);
    let b={};try{b=await request.json();}catch{}
    const order=await env.DB.prepare("SELECT * FROM tirbal_orders WHERE id=?").bind(review[1]).first();
    if(!order) return json({error:"ORDER_NOT_FOUND"},404);
    if(order.status!=="pending") return json({error:"ORDER_ALREADY_REVIEWED"},409);
    const now=Math.floor(Date.now()/1000),note=clean(b.note,400);
    if(review[2]==="reject") {
      await env.DB.prepare("UPDATE tirbal_orders SET status='rejected',reviewed_at=?,reviewer_note=? WHERE id=? AND status='pending'").bind(now,note||"لم يتم تأكيد الدفع",order.id).run();
      await audit(env,order.id,"order_rejected","admin",note);
      return json({status:"rejected"});
    }
    const expires=planExpiry(order.plan,now),token=await issue(env,order.device_id,order.plan,expires);
    const updated=await env.DB.prepare("UPDATE tirbal_orders SET status='approved',reviewed_at=?,reviewer_note=?,activation_token=?,token_expires=? WHERE id=? AND status='pending'").bind(now,note||"تم التحقق من الدفع",token,expires,order.id).run();
    if(!updated.meta||updated.meta.changes!==1) return json({error:"ORDER_ALREADY_REVIEWED"},409);
    await env.DB.prepare("UPDATE tirbal_devices SET plan=?,license_expires=?,activation_token=?,updated_at=? WHERE device_id=?").bind(order.plan,expires,token,now,order.device_id).run();
    await audit(env,order.id,"license_issued","admin","plan="+order.plan);
    return json({status:"approved",activationToken:token,expiresAt:expires});
  }
  return json({error:"NOT_FOUND"},404);
}
export default {async fetch(request,env){try{return await handle(request,env);}catch{return json({error:"INTERNAL_ERROR"},500);}}};
