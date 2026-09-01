import assert from "node:assert/strict";
import fs from "node:fs/promises";

const source = await fs.readFile(new URL("./worker.js", import.meta.url), "utf8");
const transformed = source.replace("export default {", "globalThis.__smartAgentWorker = {");
assert.notEqual(transformed, source, "worker export marker not found");
new Function("globalThis", "Response", "Request", "crypto", "fetch", transformed)(globalThis, Response, Request, globalThis.crypto, fetch);
const worker = globalThis.__smartAgentWorker;
assert.ok(worker?.fetch, "worker fetch handler missing");

async function ask(message) {
  const response = await worker.fetch(
    new Request("https://test.local/api/agent", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ message }),
    }),
    { GEMINI_MODEL: "gemini-2.5-flash" },
  );
  assert.equal(response.status, 200);
  return response.json();
}

const price = await ask("كم السعر بالدولار والجنيه؟");
assert.match(price.reply, /150,000/);
assert.match(price.reply, /120,000/);
assert.match(price.reply, /19/);
assert.match(price.reply, /16/);
assert.deepEqual(price.actions[0], { type: "focus_offer" });

const preview = await ask("افتح صفحة 5 من المعاينة");
assert.equal(preview.actions[0]?.type, "open_preview_page");
assert.equal(preview.actions[0]?.page, 5);

const purchase = await ask("أريد شراء النسخة");
assert.deepEqual(purchase.actions[0], { type: "open_checkout" });

const agent = await ask("هل يوجد وكيل ذكي مع النسخة؟");
assert.match(agent.reply, /وكيلًا ذكيًا/);

console.log("smart-agent fallback smoke tests: PASS");
