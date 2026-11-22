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
const accounting = read("tirbal-app/app/src/main/java/com/mohammedmustafa/tirbal/AccountingBook.java");
const keyStore = read("tirbal-app/app/src/main/java/com/mohammedmustafa/tirbal/DbKeyStore.java");
const manifest = read("tirbal-app/app/src/main/AndroidManifest.xml");
const config = read("tirbal-licensing/wrangler.toml");

test("Android, Worker and Wrangler use the same product version", () => {
  assert.ok(gradle.includes("versionName '1.8.0'"));
  assert.ok(worker.includes('APP_VERSION = "1.8.0"'));
  assert.ok(config.includes('APP_VERSION = "1.7.0"'));
});

test("trial and prices are consistent across app and Worker", () => {
  assert.ok(worker.includes("trialDays:7"));
  assert.ok(worker.includes("licenseEnforcement:true"));
  assert.match(worker,/trialDays===15[\s\S]*values\.trialDays=7[\s\S]*licenseEnforcement=true/);
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

test("review build can defer licensing and the website nucleus can enforce it later", () => {
  assert.match(gradle, /REMOTE_LICENSE_CONTROL/);
  assert.match(activity, /config\.optBoolean\("licenseEnforcement",false\)/);
  assert.match(activity, /openReviewSession\(\)/);
  assert.match(activity, /if\(config==null\)[\s\S]*continueLicenseCheck\(\)/);
  assert.match(activity, /optInt\("trialDays",7\)/);
  assert.match(worker, /licenseEnforcement:true/);
  assert.match(worker, /licenseEnforcement:settings\.licenseEnforcement/);
  assert.match(worker, /licenseEnforcement:b\.licenseEnforcement===true/);
  assert.match(worker, /id="licenseEnforcement"/);
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


test("agricultural costs and sales are posted to a balanced double-entry ledger", () => {
  assert.match(database, /DB_VERSION = 7/);
  assert.match(database, /AccountingBook\.install\(db\)/);
  assert.match(database, /AccountingBook\.postCost\(/);
  assert.match(database, /AccountingBook\.postSale\(/);
  assert.match(accounting, /UNIQUE\(source_type,source_id\)/);
  assert.match(accounting, /assertBalanced\(db,entry\)/);
  assert.match(accounting, /static Cursor trialBalance\(/);
  assert.match(accounting, /static void backfillLegacy\(/);
  assert.match(accounting, /{"1300","سلفيات على الترابلة","ASSET","DEBIT"}/);
  assert.match(accounting, /"السلفيات".equals\(category\)\?"1300"/);
});


test("quick worker cost sheet, crop input rows, invoices and settlement follow the requested workflow", () => {
  assert.match(activity, /اسم التربال/);
  assert.match(activity, /QUICK_CATEGORIES = \{"الميز","الأسمدة","المبيدات","التقاوي","تحضير الأرض","الديون","السلفيات"\}/);
  assert.match(activity, /showAgricultureInputs\(\)/);
  assert.match(activity, /showSettlement\(\)/);
  assert.match(activity, /تكلفة الرحلة/);
  assert.match(activity, /netSalesForWorker\(worker\)/);
  assert.match(activity, /SHARED_CATEGORIES = \{"الأسمدة","المبيدات","التقاوي","تحضير الأرض","الديون"\}/);
  assert.match(activity, /sumCostsForWorker\(worker,SHARED_CATEGORIES\)/);
  assert.match(activity, /WORKER_DEDUCTIONS = \{"الميز","السلفيات"/);
  assert.match(activity, /sumCostsForWorker\(worker,WORKER_DEDUCTIONS\)/);
  assert.match(database, /DB_VERSION = 7/);
  assert.match(database, /UPDATE partnership_terms SET responsible_party='مشترك', settlement_type='shared_half'/);
  assert.match(activity, /التكاليف المشتركة قبل المناصفة/);
  assert.match(activity, /الميز والسلفيات المخصومة من نصيب التربال/);
  assert.match(activity, /updateQuickCostSummary/);
  assert.match(activity, /workerInput/);
  assert.match(activity, /إضافة تربال جديد/);
  assert.equal(((50000000 - 5000000) / 2) - 2000000, 20500000);
});
