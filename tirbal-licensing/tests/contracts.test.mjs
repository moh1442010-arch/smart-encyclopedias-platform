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
  assert.match(gradle, /versionName '1\.3\.1'/);
  assert.match(worker, /const APP_VERSION = "1\.3\.1"/);
  assert.match(config, /APP_VERSION = "1\.3\.1"/);
});

test("trial and prices are consistent across app and Worker", () => {
  assert.match(worker, /trialDays:15/);
  assert.match(worker, /monthly: \{ amount: 70000, seconds: 30 \* 86400 \}/);
  assert.match(worker, /annual: \{ amount: 700000, seconds: 365 \* 86400 \}/);
  assert.match(worker, /lifetime: \{ amount: 1300000, seconds: 0 \}/);
  assert.match(activity, /شهري 70,000 ج\.س/);
  assert.match(activity, /سنوي 650,000 ج\.س/);
  assert.match(activity, /دائم 1,300,000 ج\.س/);
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
