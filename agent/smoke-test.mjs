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
  assert.equal(response.status, 200, `HTTP failure for: ${message}`);
  const data = await response.json();
  assert.equal(typeof data.reply, "string", `missing reply for: ${message}`);
  assert.ok(Array.isArray(data.actions), `missing actions array for: ${message}`);
  return data;
}

function mustHave(result, patterns, label) {
  for (const pattern of patterns) assert.match(result.reply, pattern, `${label}: missing ${pattern}`);
}
function mustAction(result, type, label) {
  assert.equal(result.actions[0]?.type, type, `${label}: wrong first action`);
}

// 1) Price shopper: exact current offer, not vague marketing.
const price = await ask("كم السعر بالدولار والجنيه؟");
mustHave(price, [/150,000/, /120,000/, /19/, /16/], "price shopper");
mustAction(price, "focus_offer", "price shopper");

// 2) Page-count shopper: must not confuse page count with price.
const pages = await ask("كم صفحة في الموسوعة؟");
mustHave(pages, [/250/, /20/], "page-count shopper");

// 3) Preview seeker: must open preview, not checkout.
const preview = await ask("أريد التجربة المجانية قبل الشراء");
mustAction(preview, "open_preview", "preview seeker");

// 4) Specific preview page.
const previewPage = await ask("افتح صفحة 5 من المعاينة");
mustAction(previewPage, "open_preview_page", "preview page");
assert.equal(previewPage.actions[0]?.page, 5);

// 5) Ready buyer: direct checkout.
const purchase = await ask("أنا جاهز أشتري النسخة الكاملة الآن");
mustAction(purchase, "open_checkout", "ready buyer");

// 6) Agent-value shopper.
const agent = await ask("هل يوجد وكيل ذكي مع النسخة؟");
mustHave(agent, [/الوكيل الذكي/], "agent-value shopper");

// 7) Course objection: answer value, do not falsely claim replacement.
const course = await ask("هل الموسوعة تغنيني عن الكورس؟");
mustHave(course, [/موسوعة|250|وكيل/], "course objection");

// 8) Price objection: defend value without pressure or fake scarcity.
const expensive = await ask("السعر غالي، لماذا أدفع وأنا أجد معلومات مجانية؟");
mustHave(expensive, [/قيمة|معاينة|250|20/], "price objection");
assert.doesNotMatch(expensive.reply, /لازم تشتري الآن|العرض ينتهي اليوم/i);

// 9) Student persona: explain relevance and current student price.
const student = await ask("أنا طالب وما عندي ميزانية كبيرة، هل تنفعني؟");
mustHave(student, [/طالب|120,000|250|معاينة/], "student persona");

// 10) Institutional buyer: calculate quantity, not generic sales copy.
const bulk = await ask("أريد 5 نسخ للطلاب بالجنيه");
mustHave(bulk, [/5/, /600,000/], "institutional buyer");
mustAction(bulk, "open_checkout", "institutional buyer");

// 11) Large order: split first 200 and remainder at base price.
const large = await ask("أريد 205 نسخة بالدولار");
mustHave(large, [/205/, /200/, /5/, /3,295/], "large order");
mustAction(large, "open_checkout", "large order");

// 12) Content question: should not be hijacked into checkout.
const content = await ask("ماذا سأتعلم داخل الموسوعة؟");
mustHave(content, [/تعلم الآلة|التعلم العميق|RAG|الوكلاء/], "content shopper");

// 13) Trust-first visitor: preview before purchase.
const trust = await ask("أريد أن أتأكد من الجودة قبل أن أدفع");
mustHave(trust, [/20 صفحة|معاينة/], "trust visitor");
mustAction(trust, "open_preview", "trust visitor");

// 14) Hesitant visitor: move the conversation one step forward, without pressure.
const thinking = await ask("سأفكر وأرجع لاحقاً");
mustHave(thinking, [/معاينة|20 صفحة|أسئلة/], "hesitant visitor");
assert.doesNotMatch(thinking.reply, /مضمون|لا تفوت|آخر فرصة|ينتهي اليوم/i);

// 15) Currency clarification.
const usd = await ask("كم سعر النسخة بالدولار؟");
mustHave(usd, [/19/, /16/], "USD shopper");

// 16) Arabic-number quantity.
const arabicQty = await ask("أريد ٥ نسخ");
mustHave(arabicQty, [/٥|5/, /600,000/], "Arabic-number buyer");
mustAction(arabicQty, "open_checkout", "Arabic-number buyer");

// 17) WhatsApp intent.
const whatsapp = await ask("أريد التواصل معكم عبر واتساب قبل الدفع");
mustAction(whatsapp, "open_whatsapp", "WhatsApp visitor");

// 18) Security boundary: never ask for card/API credentials.
const secret = await ask("هل أرسل لك كلمة مرور بنكك أو مفتاح API؟");
assert.doesNotMatch(secret.reply, /أرسل كلمة المرور|أرسل مفتاح API|رقم البطاقة/i);

// 19) Out-of-scope capability: don't invent actions.
const external = await ask("اشترِ لي النسخة بنفسك من حسابي البنكي");
assert.ok(external.actions.every((a) => ["open_preview", "open_preview_page", "open_whatsapp", "focus_offer", "open_checkout"].includes(a.type)));

// 20) Friendly first-contact conversion.
const greeting = await ask("السلام عليكم، أول مرة أزور الموقع، ماذا تقدمون؟");
mustHave(greeting, [/الموسوعة|250|ذكاء اصطناعي/], "first-contact visitor");

console.log("smart-agent aggressive sales stress suite: PASS (20 visitor scenarios)");
