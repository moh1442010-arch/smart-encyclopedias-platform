import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const read = p => fs.readFileSync(path.resolve(p), "utf8");
const worker = read("tirbal-licensing/worker/src/index.js");
const gradle = read("tirbal-app/app/build.gradle");
const activity = read("tirbal-app/app/src/main/java/com/mohammedmustafa/tirbal/MainActivity.java");
const license = read("tirbal-app/app/src/main/java/com/mohammedmustafa/tirbal/LicenseManager.java");
const database = read("tirbal-app/app/src/main/java/com/mohammedmustafa/tirbal/DbHelper.java");
const keyStore = read("tirbal-app/app/src/main/java/com/mohammedmustafa/tirbal/DbKeyStore.java");
const manifest = read("tirbal-app/app/src/main/AndroidManifest.xml");
const config = read("tirbal-licensing/wrangler.toml");

test("Android, Worker and Wrangler use the same product version", () => {
  assert.match(gradle, /versionName '1.4.0'/);
  assert.match(worker, /const APP_VERSION = "1.4.0"/);
  assert.match(config, /APP_VERSION = "1.4.0"/);
});

test("trial and prices are consistent across app and Worker", () => {
  assert.ok(worker.includes("trialDays:15"));
  assert.ok(worker.includes("monthly: { amount: 70000, seconds: 30 * 86400 }"));
  assert.ok(worker.includes("annual: { amount: 650000, seconds: 365 * 86400 }"));
  assert.ok(worker.includes("lifetime: { amount: 1300000, seconds: 0 }"));
  assert.match(worker, /values\.monthlyPrice===25000[\s\S]*values\.annualPrice=650000[\s\S]*values\.lifetimePrice=1300000/);
  assert.match(activity, /monthly\.optLong\("amount"\)/);
  assert.match(activity, /annual\.optLong\("amount"\)/);
  assert.match(activity, /lifetime\.optLong\("amount"\)/);
  assert.match(activity, /جارٍ تحميل الخطة الشهرية/);
  assert.match(activity, /https:\/\/wa\.me\//);
  assert.match(activity, /String whatsappNumber=p\.optString\("whatsapp",""\)/);
  assert.match(activity, /String digits=whatsappNumber\.replaceAll\("\[\^0-9\]",""\)/);
});

test("licensing is device-bound and signed, not a local date-only trial", () => {
  assert.match(worker, /enc\.encode\("app="\+APP_ID\+";device="\+device/);
  assert.match(worker, /await crypto\.subtle\.sign\("RSASSA-PKCS1-v1_5"/);
  assert.match(license, /SHA256withRSA/);
  assert.match(license, /Settings\.Secure\.ANDROID_ID/);
  assert.match(license, /SystemClock\.elapsedRealtime/);
  assert.match(license, /boot_count/);
});

test("database opens only after license gate and uses encrypted SQLCipher storage", () => {
  assert.match(activity, /LicenseManager\.check\(this/);
  assert.match(activity, /openDatabaseSafely\(\)/);
  assert.match(database, /net\.zetetic\.database\.sqlcipher\.SQLiteDatabase/);
  assert.match(database, /migrateLegacyIfNeeded/);
  assert.match(keyStore, /AES\/GCM\/NoPadding/);
  assert.match(keyStore, /AndroidKeyStore/);
  assert.match(manifest, /allowBackup="false"/);
});

test("manual review and payment configuration are server-controlled", () => {
  for (const field of ["PAYMENT_BANK_NAME","PAYMENT_ACCOUNT_NAME","PAYMENT_ACCOUNT_NUMBER","PAYMENT_WHATSAPP"]) {
    assert.match(worker, new RegExp(field));
  }
  assert.match(worker, /status:"pending"/);
  assert.match(worker, /sameOriginPost/);
  assert.match(worker, /allowOrder\(request,env\)/);
  assert.doesNotMatch(worker, /-----BEGIN (?:RSA )?PRIVATE KEY-----/);
});

test("multi-worker registry and worker-specific expense screens are present", () => {
  assert.match(gradle, /versionCode 7/);
  assert.match(database, /DB_VERSION = 5/);
  assert.match(database, /CREATE TABLE workers/);
  assert.match(database, /INSERT OR IGNORE INTO workers.*SELECT 1,TRIM\(worker\)/);
  assert.match(database, /public Cursor workers\(\)/);
  assert.match(database, /public Cursor workerCosts\(String worker\)/);
  assert.ok(database.includes('INSERT OR IGNORE INTO workers(project_id,name) SELECT 1,TRIM(worker) FROM costs WHERE worker IS NOT NULL AND length(TRIM(worker))>0 GROUP BY TRIM(worker)'));
  assert.ok(database.includes('return new String[]{"التربال", "التاجر", "deduct_worker", "من نصيب التربال"};'));
  assert.match(activity, /private void showWorkers\(\)/);
  assert.match(activity, /private void addWorkerDialog\(\)/);
  assert.match(activity, /private void showWorkerDetails\(String workerName\)/);
  for (const category of ["الراكوبة","الميز","التقاوي","الأسمدة","المبيدات","المنصرفات","السلفيات"]) {
    assert.ok(activity.includes(category), "Missing worker expense category: " + category);
  }
});

test("harvest settlement deducts shared costs by half and worker/trader obligations separately", () => {
  assert.match(activity, /long workerDeductions = db\.totalByDeduction\("من نصيب التربال"\) \+ directAllocated \+ marketExpenses/);
  assert.match(activity, /long traderNet = grossHarvest \/ 2 - sharedTotal \/ 2 - traderOnly/);
  assert.match(activity, /long workerNet = grossHarvest - grossHarvest \/ 2 - \(sharedTotal - sharedTotal \/ 2\) - workerDeductions/);
  assert.match(database, /"الراكوبة","التربال","التاجر","deduct_worker"/);
  assert.match(database, /"ضريبة المياه","التاجر","التاجر","trader_only"/);
  assert.match(database, /"إيجار الأرض","التاجر","التاجر","trader_only"/);
});
