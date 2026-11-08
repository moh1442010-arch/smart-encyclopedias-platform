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

test("2.8.0 customer order is accepted and recorded as pending without issuing a token", async () => {
  const writes = [];
  const db = {
    prepare(sql) {
      return {
        bind(...values) {
          return {
            first: async () => {
              if (sql.includes("license_order_rate_limits")) return { attempts: 1 };
              if (sql.includes("SELECT id FROM license_orders")) return null;
              return null;
            },
            run: async () => {
              writes.push({ sql, values });
              return { meta: { changes: 1 } };
            }
          };
        }
      };
    }
  };
  const response = await worker.fetch(new Request("https://bakery-test.local/api/orders", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      origin: "https://bakery-test.local",
      "CF-Connecting-IP": "192.0.2.44"
    },
    body: JSON.stringify({
      customerName: "Test Bakery",
      contact: "+249123456789",
      deviceId: "DEVICE-12345678",
      plan: "monthly",
      receiptReference: "BANKAK-20261107-TEST",
      appVersion: "2.8.0"
    })
  }), { DB: db, APP_VERSION: "2.8.0", STORE_ORIGIN: "https://bakery-test.local" });

  assert.equal(response.status, 201);
  const body = await response.json();
  assert.equal(body.status, "pending");
  assert.equal(body.amount, 55000);\n  assert.equal(body.currencyCode, "SDG");
  assert.ok(body.orderId);
  assert.equal("activationToken" in body, false);
  assert.ok(writes.some(x => x.sql.includes("INSERT INTO license_orders")));
  assert.ok(writes.some(x => x.sql.includes("INSERT INTO license_audit") && x.values.includes("order_created")));
});

test("admin login page is served without exposing dashboard data", async () => {
  const response = await worker.fetch(
    new Request("https://bakery-test.local/admin/login"),
    { DB: {}, ADMIN_PASSWORD: "A".repeat(40) }
  );
  assert.equal(response.status, 200);
  const html = await response.text();
  assert.match(html, /تسجيل الدخول/);
  assert.doesNotMatch(html, /activationToken|LICENSE_PRIVATE_KEY_PKCS8_B64/);
});



test("2.8.1 candidate order is accepted as pending without issuing an activation token", async () => {
  const writes = [];
  const db = {
    prepare(sql) {
      return {
        bind(...values) {
          return {
            first: async () => {
              if (sql.includes("license_order_rate_limits")) return { attempts: 1 };
              if (sql.includes("SELECT id FROM license_orders")) return null;
              return null;
            },
            run: async () => {
              writes.push({ sql, values });
              return { meta: { changes: 1 } };
            }
          };
        }
      };
    }
  };
  const response = await worker.fetch(new Request("https://bakery-test.local/api/orders", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      origin: "https://bakery-test.local",
      "CF-Connecting-IP": "192.0.2.45"
    },
    body: JSON.stringify({
      customerName: "Candidate 2.8.1 Bakery",
      contact: "+249123456789",
      deviceId: "DEVICE-281-123456",
      plan: "monthly",
      receiptReference: "TEST-ONLY-281",
      appVersion: "2.8.1"
    })
  }), {
    DB: db,
    APP_VERSION: "2.8.1",
    PREVIOUS_APP_VERSION: "2.8.0",
    STORE_ORIGIN: "https://bakery-test.local"
  });

  assert.equal(response.status, 201);
  const body = await response.json();
  assert.equal(body.status, "pending");
  assert.equal(body.amountSdg, 55000);
  assert.ok(body.orderId);
  assert.equal("activationToken" in body, false);
  assert.ok(writes.some(x => x.sql.includes("INSERT INTO license_orders") && x.values.includes("2.8.1")));
  assert.ok(writes.some(x => x.sql.includes("INSERT INTO license_audit") && x.values.includes("order_created")));
});

test("international license prices accept SAR, AED and USD and reject EUR", async () => {
  async function submit(currencyCode, receiptReference) {
    const writes=[];
    const db={prepare(sql){return{bind(...values){return{
      first:async()=>sql.includes("license_order_rate_limits")?{attempts:1}:null,
      run:async()=>{writes.push({sql,values});return{meta:{changes:1}};}
    };}};}};
    const response=await worker.fetch(new Request("https://bakery-test.local/api/orders",{
      method:"POST",headers:{"content-type":"application/json",origin:"https://bakery-test.local","CF-Connecting-IP":"192.0.2.90"},
      body:JSON.stringify({customerName:"International Bakery",contact:"+249123456789",deviceId:"DEVICE-INT-123456",plan:"annual",currencyCode,receiptReference,appVersion:"2.8.1"})
    }),{DB:db,APP_VERSION:"2.8.1",STORE_ORIGIN:"https://bakery-test.local"});
    return {response,writes};
  }
  for (const [currencyCode,amount] of [["SAR",290],["AED",290],["USD",80]]) {
    const {response,writes}=await submit(currencyCode,"REF-"+currencyCode);
    assert.equal(response.status,201);
    const body=await response.json();
    assert.equal(body.amount,amount);
    assert.equal(body.currencyCode,currencyCode);
    const insert=writes.find(x=>x.sql.includes("INSERT INTO license_orders"));
    assert.ok(insert);
    assert.ok(insert.values.includes(currencyCode));
    assert.ok(insert.values.includes(amount));
  }
  const invalid=await submit("EUR","REF-EUR");
  assert.equal(invalid.response.status,400);
});
