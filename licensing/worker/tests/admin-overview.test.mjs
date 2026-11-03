import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

test("admin overview rejects requests without a valid session", async () => {
  const response = await worker.fetch(new Request("https://bakery-test.local/admin/overview"), {
    DB: {},
    ADMIN_PASSWORD: "A".repeat(40)
  });
  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "UNAUTHORIZED");
});

test("admin integrations rejects requests without a valid session", async () => {
  const response = await worker.fetch(new Request("https://bakery-test.local/admin/integrations"), {
    DB: {},
    ADMIN_PASSWORD: "A".repeat(40)
  });
  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "UNAUTHORIZED");
});
