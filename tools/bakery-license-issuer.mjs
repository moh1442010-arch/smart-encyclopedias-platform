#!/usr/bin/env node
/**
 * Offline issuer for Al-Mizan Bakery signed activation tokens.
 * Never store the corresponding private key in this repository or pass it in chat.
 * The private key must be held outside the repository on the operator's trusted device.
 *
 * Usage:
 *   node tools/bakery-license-issuer.mjs --key /secure/path/bakery-license-private.pem --device DEVICE_ID --plan monthly
 *   node tools/bakery-license-issuer.mjs --key /secure/path/bakery-license-private.pem --device DEVICE_ID --plan annual
 *   node tools/bakery-license-issuer.mjs --key /secure/path/bakery-license-private.pem --device DEVICE_ID --plan lifetime
 *
 * Monthly/annual tokens expire after 30/365 days. Lifetime tokens apply only to the
 * exact app version encoded below, matching the app's current verification policy.
 */
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";

const APP_ID = "com.mohammedmustafa.almizan.bakery";
const CURRENT_APP_VERSION = "2.6.15";
const EXPECTED_PUBLIC_KEY_DER_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3frqcepNGSoat1ZsLo3/Qcgm9/q26bLU5zLRM/9Ff47rstfdrUzQH18PqHLhR3HiSv4jYWAEQEaA008BLnCAyBxtv73s8VQKR39BUNGCea/KiL3xXDmVkujr52kL7QySidKFoV8NaUZLHmNCfHFcOPifTdkEz316HWrcYt9RVC2SDYFXt4IzH5X8p1gKxLJFFMR73p3a28uPAszn8XMqIdXwNQRWNOGCf1NjLL1od1EWVcFvQwEW1nfVesskYLeaPnHxa+U+tx3TVckRJV5Brbao4yT0ozAZLiapFQDyekPs23jHB7rrCC1Zub/oWIm4PSQa0Uc/j9CGUE/V8R9W0wIDAQAB";

function fail(message) {
  console.error(message);
  process.exit(2);
}
function argsOf(argv) {
  const out = {};
  for (let i = 2; i < argv.length; i += 2) {
    const key = argv[i];
    if (!key || !key.startsWith("--") || i + 1 >= argv.length) fail("Invalid arguments. Use --key, --device, and --plan.");
    out[key.slice(2)] = argv[i + 1];
  }
  return out;
}
const args = argsOf(process.argv);
const keyPath = args.key ? path.resolve(args.key) : "";
const device = args.device || "";
const plan = args.plan || "";
if (!keyPath || !device || !plan) {
  fail("Usage: node tools/bakery-license-issuer.mjs --key /secure/private.pem --device DEVICE_ID --plan monthly|annual|lifetime");
}
if (!["monthly", "annual", "lifetime"].includes(plan)) fail("Plan must be monthly, annual, or lifetime.");
if (!/^[A-Za-z0-9-]{8,100}$/.test(device) || device.includes(";") || device.includes("=")) {
  fail("Device ID format is invalid. Copy the device ID shown inside the app exactly.");
}
const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, "..") + path.sep;
if (keyPath.startsWith(repoRoot)) {
  fail("Security stop: keep the private key outside the repository. Move it to a protected location and retry.");
}
if (!fs.existsSync(keyPath) || !fs.statSync(keyPath).isFile()) fail("Private key file not found.");
let privateKey;
try {
  privateKey = crypto.createPrivateKey(fs.readFileSync(keyPath));
} catch {
  fail("Could not load the private key. Use a PEM-encoded RSA private key; do not paste it into source files.");
}
if (privateKey.asymmetricKeyType !== "rsa") fail("The activation key must be an RSA private key.");
let actualPublicKey;
try {
  actualPublicKey = crypto.createPublicKey(privateKey).export({ type: "spki", format: "der" }).toString("base64");
} catch {
  fail("Could not derive the public key from the supplied private key.");
}
if (actualPublicKey !== EXPECTED_PUBLIC_KEY_DER_B64) {
  fail("Key mismatch: this private key does not match the public key embedded in the installed bakery app. No token was issued.");
}
const now = Math.floor(Date.now() / 1000);
const expires = plan === "monthly" ? now + 30 * 86400 : plan === "annual" ? now + 365 * 86400 : 0;
const maxVersion = plan === "lifetime" ? CURRENT_APP_VERSION : "any";
const payload = `app=${APP_ID};device=${device};plan=${plan};max_version=${maxVersion};expires=${expires}`;
const payload64 = Buffer.from(payload, "utf8").toString("base64url");
const signature64 = crypto.sign("RSA-SHA256", Buffer.from(payload, "utf8"), privateKey).toString("base64url");
const token = payload64 + "." + signature64;
console.error("Activation token issued. Send it only to the customer whose device ID matches.");
console.log(token);
