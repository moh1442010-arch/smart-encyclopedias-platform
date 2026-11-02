import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

async function adminConfig(env) {
  const response = await worker.fetch(
    new Request("https://bakery-test.local/diagnostics/admin-config"),
    { DB: {}, APP_VERSION: "2.6.16", ...env }
  );
  assert.equal(response.status, 200);
  return response.json();
}

test("legacy Cloudflare admin secret binding remains supported", async () => {
  const result = await adminConfig({ Admin_BassworD: "L".repeat(40) });
  assert.equal(result.adminPasswordConfigured, true);
  assert.equal(result.appVersion, "2.6.16");
});

test("canonical ADMIN_PASSWORD takes precedence over the legacy alias", async () => {
  const result = await adminConfig({
    ADMIN_PASSWORD: "short",
    Admin_BassworD: "L".repeat(40)
  });
  assert.equal(result.adminPasswordConfigured, false);
});
