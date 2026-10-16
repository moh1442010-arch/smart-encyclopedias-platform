import test from "node:test";
import assert from "node:assert/strict";
import worker from "../worker.mjs";

const RealDate = Date;
let clock = RealDate.parse("2026-10-15T12:00:00.000Z");

class TestDate extends RealDate {
  constructor(...args) { super(...(args.length ? args : [clock])); }
  static now() { return clock; }
}

function makeDb() {
  const trials = new Map();
  const licenses = new Map();
  const db = {
    async batch(statements) {
      const results = [];
      for (const statement of statements) results.push(await statement.run());
      return results;
    },
    prepare(sql) {
      let values = [];
      const stmt = {
        bind(...args) { values = args; return stmt; },
        async run() {
          if (sql.includes("INSERT OR IGNORE INTO app_trials")) {
            const [hash, started, expires] = values;
            if (!trials.has(hash)) trials.set(hash, { device_hash: hash, started_at: started, expires_at: expires });
            return { meta: { changes: 1 } };
          }
          if (sql.includes("INSERT INTO app_licenses")) {
            const [id, codeHash, plan, termType, customerName, customerEmail, orderId, expiresAt, approvedBy] = values;
            if ([...licenses.values()].some(x => x.license_code_hash === codeHash) || licenses.has(id)) throw new Error("duplicate");
            licenses.set(id, { id, license_code_hash: codeHash, plan, term_type: termType, customer_name: customerName, customer_email: customerEmail, order_id: orderId, status: "active", device_hash: null, starts_at: new TestDate().toISOString(), expires_at: expiresAt, approved_by: approvedBy });
            return { meta: { changes: 1 } };
          }
          if (sql.includes("UPDATE app_licenses SET device_hash = NULL")) {
            const [id, hash] = values; const row = licenses.get(id);
            if (row && row.device_hash === hash) {
              row.device_hash = null;
              if (row.status === "active") row.status = "superseded";
              return { meta: { changes: 1 } };
            }
            return { meta: { changes: 0 } };
          }
          if (sql.includes("UPDATE app_licenses SET device_hash")) {
            const [hash, id] = values; const row = licenses.get(id);
            if (row && row.status === "active" && row.device_hash === null) {
              if ([...licenses.values()].some(x => x.id !== id && x.device_hash === hash)) throw new Error("device already bound");
              row.device_hash = hash; return { meta: { changes: 1 } };
            }
            return { meta: { changes: 0 } };
          }
          if (sql.includes("UPDATE app_licenses SET status")) {
            const [status, id] = values; const row = licenses.get(id);
            if (!row) return { meta: { changes: 0 } };
            row.status = status; return { meta: { changes: 1 } };
          }
          return { meta: { changes: 0 } };
        },
        async first() {
          if (sql.includes("FROM app_licenses WHERE device_hash = ?")) return [...licenses.values()].find(x => x.device_hash === values[0]) || null;
          if (sql.includes("FROM app_licenses WHERE license_code_hash = ?")) return [...licenses.values()].find(x => x.license_code_hash === values[0]) || null;
          if (sql.includes("FROM app_licenses WHERE id = ?")) {
            const row = licenses.get(values[0]); return row ? { device_hash: row.device_hash } : null;
          }
          if (sql.includes("FROM app_trials WHERE device_hash = ?")) return trials.get(values[0]) || null;
          if (sql.includes("COUNT(*) AS total")) return { total: licenses.size, active: [...licenses.values()].filter(x => x.status === "active").length };
          return null;
        },
        async all() { return { results: [...licenses.values()] }; }
      };
      return stmt;
    }
  };
  return { db, trials, licenses };
}

async function call(env, path, { method = "GET", device = "test-device-000001", admin, body } = {}) {
  const headers = {};
  if (device) headers["x-app-device-id"] = device;
  if (admin) headers["x-admin-key"] = admin;
  if (body !== undefined) headers["content-type"] = "application/json";
  return worker.fetch(new Request("https://test.local" + path, {
    method, headers, body: body === undefined ? undefined : JSON.stringify(body)
  }), env);
}

test("server trial starts once and expires after exactly seven days", async () => {
  const originalDate = globalThis.Date;
  globalThis.Date = TestDate;
  clock = RealDate.parse("2026-10-15T12:00:00.000Z");
  try {
    const { db, trials } = makeDb(); const env = { DB: db };
    const first = await (await call(env, "/api/app-license/status")).json();
    assert.equal(first.ok, true);
    assert.equal(first.mode, "trial");
    assert.equal(first.expiresAt, new TestDate(clock + 7 * 86400000).toISOString());
    const second = await (await call(env, "/api/app-license/status")).json();
    assert.equal(second.expiresAt, first.expiresAt);
    assert.equal(trials.size, 1);
    clock += 7 * 86400000 + 1;
    const expired = await (await call(env, "/api/app-license/status")).json();
    assert.equal(expired.mode, "expired");
    assert.equal(expired.remainingMs, 0);
  } finally { globalThis.Date = originalDate; }
});

test("missing device ID and invalid activation codes are rejected", async () => {
  const { db } = makeDb(); const env = { DB: db };
  const missing = await call(env, "/api/app-license/status", { device: "" });
  assert.equal(missing.status, 400);
  const invalid = await call(env, "/api/app-license/activate", { method: "POST", body: { activationCode: "bad" } });
  assert.equal(invalid.status, 400);
  assert.equal((await invalid.json()).error, "invalid_activation_code");
});

test("paid activation is device-bound and a revoked license is not granted", async () => {
  const originalDate = globalThis.Date;
  globalThis.Date = TestDate;
  clock = RealDate.parse("2026-10-15T12:00:00.000Z");
  try {
    const { db, licenses } = makeDb(); const env = { DB: db, LICENSE_ADMIN_KEY: "test-admin-secret" };
    const createdResponse = await call(env, "/api/app-license/admin/create", {
      method: "POST", device: "", admin: "test-admin-secret",
      body: { approved: true, plan: "pro", termType: "annual", customerName: "Test Customer", licenseId: "TEST-LICENSE-01" }
    });
    assert.equal(createdResponse.status, 201);
    const created = await createdResponse.json();
    const activated = await call(env, "/api/app-license/activate", { method: "POST", device: "device-0000000001", body: { activationCode: created.activationCode } });
    assert.equal(activated.status, 200);
    assert.equal((await activated.json()).mode, "licensed");
    const otherDevice = await call(env, "/api/app-license/activate", { method: "POST", device: "device-0000000002", body: { activationCode: created.activationCode } });
    assert.equal(otherDevice.status, 403);
    assert.equal((await otherDevice.json()).error, "license_bound_to_another_device");
    licenses.get("TEST-LICENSE-01").status = "revoked";
    const revoked = await call(env, "/api/app-license/status", { device: "device-0000000001" });
    assert.equal((await revoked.json()).mode, "expired");
  } finally { globalThis.Date = originalDate; }
});


test("expired monthly license stays locked instead of receiving a new trial", async () => {
  const originalDate = globalThis.Date;
  globalThis.Date = TestDate;
  clock = RealDate.parse("2026-10-15T12:00:00.000Z");
  try {
    const { db } = makeDb();
    const env = { DB: db, LICENSE_ADMIN_KEY: "test-admin-secret" };
    const createdResponse = await call(env, "/api/app-license/admin/create", {
      method: "POST", device: "", admin: "test-admin-secret",
      body: { approved: true, plan: "basic", termType: "monthly", customerName: "Monthly Test", licenseId: "TEST-MONTH-01" }
    });
    assert.equal(createdResponse.status, 201);
    const created = await createdResponse.json();
    const activated = await call(env, "/api/app-license/activate", {
      method: "POST", device: "monthly-device-0001", body: { activationCode: created.activationCode }
    });
    assert.equal(activated.status, 200);
    clock += 31 * 86400000;
    const status = await (await call(env, "/api/app-license/status", { device: "monthly-device-0001" })).json();
    assert.equal(status.mode, "expired");
    assert.equal(status.licenseStatus, "active");
    assert.equal(status.licenseId, "TEST-MONTH-01");
  } finally { globalThis.Date = originalDate; }
});

test("activating an upgraded edition on the same device supersedes the old license safely", async () => {
  const originalDate = globalThis.Date;
  globalThis.Date = TestDate;
  clock = RealDate.parse("2026-10-16T12:00:00.000Z");
  try {
    const { db, licenses } = makeDb();
    const env = { DB: db, LICENSE_ADMIN_KEY: "test-admin-secret" };
    const firstCreated = await call(env, "/api/app-license/admin/create", {
      method: "POST", device: "", admin: "test-admin-secret",
      body: { approved: true, plan: "basic", termType: "perpetual", customerName: "Upgrade Test", licenseId: "TEST-BASIC-01" }
    });
    const first = await firstCreated.json();
    assert.equal(firstCreated.status, 201);
    const firstActivation = await call(env, "/api/app-license/activate", {
      method: "POST", device: "upgrade-device-0001", body: { activationCode: first.activationCode }
    });
    assert.equal(firstActivation.status, 200);

    const nextCreated = await call(env, "/api/app-license/admin/create", {
      method: "POST", device: "", admin: "test-admin-secret",
      body: { approved: true, plan: "pro", termType: "annual", customerName: "Upgrade Test", licenseId: "TEST-PRO-0001" }
    });
    const next = await nextCreated.json();
    assert.equal(nextCreated.status, 201);
    const mismatch = await call(env, "/api/app-license/activate", {
      method: "POST", device: "upgrade-device-0001", body: { activationCode: next.activationCode, edition: "basic" }
    });
    assert.equal(mismatch.status, 409);
    assert.equal((await mismatch.json()).error, "license_plan_mismatch");
    assert.equal(licenses.get("TEST-BASIC-01").status, "active");
    assert.notEqual(licenses.get("TEST-BASIC-01").device_hash, null);
    const nextActivation = await call(env, "/api/app-license/activate", {
      method: "POST", device: "upgrade-device-0001", body: { activationCode: next.activationCode, edition: "pro" }
    });
    assert.equal(nextActivation.status, 200);
    const status = await (await call(env, "/api/app-license/status", { device: "upgrade-device-0001" })).json();
    assert.equal(status.mode, "licensed");
    assert.equal(status.plan, "pro");
    assert.equal(licenses.get("TEST-BASIC-01").status, "superseded");
    assert.equal(licenses.get("TEST-BASIC-01").device_hash, null);

    const reuseOldCode = await call(env, "/api/app-license/activate", {
      method: "POST", device: "different-device-0001", body: { activationCode: first.activationCode }
    });
    assert.equal(reuseOldCode.status, 403);
    assert.equal((await reuseOldCode.json()).error, "license_invalid_or_expired");
  } finally { globalThis.Date = originalDate; }
});
