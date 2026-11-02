#!/usr/bin/env node
// Validate a local bakery license private key without printing or uploading its contents.
// Usage: node tools/validate-bakery-license-key.mjs /path/to/private-key.pem [/private/path/normalized-key.b64]
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";

const input = process.argv[2];
const output = process.argv[3] || (input ? input + ".pkcs8-base64.txt" : "");
if (!input || !output) {
  console.error("Usage: node tools/validate-bakery-license-key.mjs PRIVATE_KEY_FILE [OUTPUT_B64_FILE]");
  process.exit(2);
}
let privateKey;
try {
  const raw = fs.readFileSync(input);
  const text = raw.toString("utf8").trim();
  if (/^-----BEGIN (?:RSA )?PRIVATE KEY-----/.test(text)) {
    privateKey = crypto.createPrivateKey(text);
  } else {
    const compact = text.replace(/\s+/g, "");
    if (/^[A-Za-z0-9+/]+={0,2}$/.test(compact) && compact.length % 4 === 0) {
      const decoded = Buffer.from(compact, "base64");
      if (decoded.toString("base64") !== compact) throw new Error("invalid Base64");
      if (decoded.subarray(0, 24).toString("ascii").includes("-----BEGIN")) {
        privateKey = crypto.createPrivateKey(decoded.toString("utf8"));
      } else {
        try {
          privateKey = crypto.createPrivateKey({ key: decoded, format: "der", type: "pkcs8" });
        } catch {
          privateKey = crypto.createPrivateKey({ key: decoded, format: "der", type: "pkcs1" });
        }
      }
    } else {
      privateKey = crypto.createPrivateKey({ key: raw, format: "der", type: "pkcs8" });
    }
  }
} catch {
  console.error("NOT MATCH: file is not a supported PEM/Base64/DER RSA private key.");
  process.exit(1);
}
if (privateKey.asymmetricKeyType !== "rsa") {
  console.error("NOT MATCH: bakery license key must be RSA.");
  process.exit(1);
}
const expectedPublicKeyB64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvsK66CYxt4X7R9GcVaJ0FjrheMAkFpzbirdz7/X/lj8UAuqUabNOPN3R5dGuZro90i+raIyngTz+VxRxLcNPRQru4VzWIDLsoBEwi/CrsOaMJFgVLom7jYXFjQ5P25TO5hRxzPN8ZOOG1MTo8j6ipgPOKY5be6Gmsj8R03SbulYnVO8STWC4bSkqwvV9YlEIpZwnyAJVbWHbE4JSKiCC/TMOcfrBgGG0W7qCJeEjnUKPfHAtH85ayZjBQwMIQx+z1CoTFWrqKXywSWN65G7yZbHwsnREovRprxjyVHqmTtLd45VyNQU2zXBKchzkwnpiG1zeD8kfdDeACeaAmxbsiwIDAQAB";
const expectedPublicKey = crypto.createPublicKey({
  key: Buffer.from(expectedPublicKeyB64, "base64"),
  format: "der",
  type: "spki"
});
const probe = Buffer.from("Al-Mizan Bakery key-pair check");
const signature = crypto.sign("RSA-SHA256", probe, privateKey);
if (!crypto.verify("RSA-SHA256", probe, expectedPublicKey, signature)) {
  console.error("NOT MATCH: this private key does not belong to the public key embedded in the bakery app.");
  process.exit(1);
}
const normalized = privateKey.export({ format: "der", type: "pkcs8" }).toString("base64") + "\n";
fs.mkdirSync(path.dirname(path.resolve(output)), { recursive: true });
fs.writeFileSync(output, normalized, { mode: 0o600 });
try { fs.chmodSync(output, 0o600); } catch {}
console.log("MATCH: private key belongs to the bakery app.");
console.log("A normalized PKCS#8 Base64 copy was saved to the requested private file.");
console.log("The key contents were not printed.");
