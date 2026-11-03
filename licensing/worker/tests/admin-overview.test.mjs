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

for (const path of ["/admin/integrations", "/admin/reviews", "/admin/analytics"]) {
  test(path + " rejects requests without a valid session", async () => {
    const response = await worker.fetch(new Request("https://bakery-test.local" + path), {
      DB: {},
      ADMIN_PASSWORD: "A".repeat(40)
    });
    assert.equal(response.status, 401);
    assert.equal((await response.json()).error, "UNAUTHORIZED");
  });
}

test("public review intake rejects incomplete content before database writes", async () => {
  const response = await worker.fetch(new Request("https://bakery-test.local/api/reviews", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ name: "A", reviewText: "short" })
  }), { DB: {} });
  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "INVALID_REVIEW_FIELDS");
});

test("aggregate analytics rejects paths outside the public store allowlist", async () => {
  const response = await worker.fetch(new Request("https://bakery-test.local/api/analytics/pageview", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ path: "/admin/panel" })
  }), { DB: {} });
  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "INVALID_PAGE_PATH");
});
