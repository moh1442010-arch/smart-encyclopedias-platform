import test from "node:test";
import assert from "node:assert/strict";
import worker from "../src/index.js";

test("new order rejects a client app version that differs from the deployed release", async () => {
  const response = await worker.fetch(new Request("https://bakery-test.local/api/orders", {
    method: "POST",
    headers: { "content-type": "application/json", origin: "https://moh1442010-arch.github.io" },
    body: JSON.stringify({
      customerName: "Test Bakery",
      contact: "+249123456789",
      deviceId: "DEVICE-12345678",
      plan: "monthly",
      receiptReference: "BANKAK-1234",
      appVersion: "2.6.15"
    })
  }), { DB: {}, APP_VERSION: "2.6.17", STORE_ORIGIN: "https://moh1442010-arch.github.io" });
  assert.equal(response.status, 400);
  assert.equal((await response.json()).error, "APP_VERSION_UNSUPPORTED");
});

test("public order status never returns customer details or activation token", async () => {
  const db = {
    prepare() {
      return {
        bind() {
          return {
            first: async () => ({
              id: "12345678-1234-1234-1234-123456789012",
              created_at: "2026-11-03T00:00:00.000Z",
              plan: "monthly",
              amount_sdg: 55000,
              status: "approved",
              app_version: "2.6.17",
              customer_name: "Private Customer",
              activation_token: "PRIVATE-TOKEN-MUST-NOT-LEAK"
            })
          };
        }
      };
    }
  };
  const response = await worker.fetch(new Request(
    "https://bakery-test.local/api/orders/12345678-1234-1234-1234-123456789012"
  ), { DB: db });
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.status, "approved");
  assert.equal("activationToken" in body, false);
  assert.equal("customer_name" in body, false);
  assert.equal(JSON.stringify(body).includes("PRIVATE-TOKEN-MUST-NOT-LEAK"), false);
});

test("activation token recovery requires an authenticated admin session", async () => {
  const response = await worker.fetch(new Request(
    "https://bakery-test.local/admin/orders/12345678-1234-1234-1234-123456789012/token",
    {
      method: "POST",
      headers: {
        origin: "https://bakery-test.local",
        "content-type": "application/json"
      },
      body: JSON.stringify({ note: "Recovery test" })
    }
  ), { DB: {}, ADMIN_PASSWORD: "A".repeat(40) });
  assert.equal(response.status, 401);
  assert.equal((await response.json()).error, "UNAUTHORIZED");
});
