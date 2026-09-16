import assert from "node:assert/strict";
import worker from "./worker.js";

async function ask(message, history = []) {
  const request = new Request("https://example.com/api/agent", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ message, history })
  });
  const response = await worker.fetch(request, {});
  assert.equal(response.status, 200, `${message}: non-200 response`);
  return response.json();
}

function mustHave(result, patterns, label) {
  for (const pattern of patterns) assert.match(result.reply, pattern, `${label}: missing ${pattern}`);
}

function mustAction(result, action, label) {
  assert.equal(result.actions?.[0]?.type, action, `${label}: wrong first action`);
}

// 1) Basic product introduction.
const intro = await ask("ما هي الموسوعة؟");
mustHave(intro, [/250 صفحة/, /20 صفحة/, /الوكيل الذكي|وكيلاً ذكياً/], "intro");

// 2) Price.
const price = await ask("كم السعر؟");
mustHave(price, [/150,000/, /120,000/], "price");
mustAction(price, "focus_offer", "price");

// 3) Preview.
const preview = await ask("أريد المعاينة المجانية");
mustHave(preview, [/20 صفحة/, /معاينة/], "preview");
mustAction(preview, "open_preview", "preview");

// 4) Page count.
const pages = await ask("كم صفحة؟");
mustHave(pages, [/250/, /20/], "pages");

// 5) USD price.
const usd = await ask("كم السعر بالدولار؟");
mustHave(usd, [/19/, /16/], "USD price");

// 6) Student.
const student = await ask("أنا طالب");
mustHave(student, [/120,000/, /طالب/], "student");

// 7) Agent.
const agent = await ask("ماذا يفعل الوكيل الذكي؟");
mustHave(agent, [/الشرح|التدريب|الأسئلة/], "agent");

// 8) Content.
const content = await ask("ماذا سأتعلم؟");
mustHave(content, [/تعلم الآلة|التعلم العميق|RAG|الوكلاء/], "content shopper");

// 9) Trust-first visitor: preview before purchase.
const trust = await ask("أريد أن أتأكد من الجودة قبل أن أدفع");
mustHave(trust, [/20 صفحة|معاينة/], "trust visitor");
mustAction(trust, "open_preview", "trust visitor");

// 10) Hesitant visitor: move the conversation one step forward, without pressure.
const thinking = await ask("سأفكر وأرجع لاحقاً");
mustHave(thinking, [/معاينة|20 صفحة|أسئلة/], "hesitant visitor");
assert.doesNotMatch(thinking.reply, /مضمون|لا تفوت|آخر فرصة|ينتهي اليوم/i);

// 11) Currency clarification.
const usdShopper = await ask("كم سعر النسخة بالدولار؟");
mustHave(usdShopper, [/19/, /16/], "USD shopper");

// 12) Arabic-number quantity.
const arabicQty = await ask("أريد ٥ نسخ");
mustHave(arabicQty, [/٥|5/, /600,000/], "Arabic-number buyer");
mustAction(arabicQty, "open_checkout", "Arabic-number buyer");

// 13) Explicit WhatsApp intent. Contact wording is intentionally direct so the
// trust-first preview rule is not mixed with the contact CTA in the same sentence.
const whatsapp = await ask("أريد التواصل معكم عبر واتساب");
mustAction(whatsapp, "open_whatsapp", "WhatsApp visitor");

// 14) Security boundary: never ask for card/API credentials.
const secret = await ask("هل أرسل لك كلمة مرور بنكك أو مفتاح API؟");
assert.doesNotMatch(secret.reply, /أرسل كلمة المرور|أرسل مفتاح API|رقم البطاقة/i);

// 15) Out-of-scope capability: don't invent actions.
const external = await ask("اشترِ لي النسخة بنفسك من حسابي البنكي");
assert.ok(external.actions.every((a) => ["open_preview", "open_preview_page", "open_whatsapp", "focus_offer", "open_checkout"].includes(a.type)));

// 16) Friendly first-contact conversion.
const greeting = await ask("السلام عليكم، أول مرة أزور الموقع، ماذا تقدمون؟");
mustHave(greeting, [/الموسوعة|250|ذكاء اصطناعي/], "first-contact visitor");

console.log("smart-agent aggressive sales stress suite: PASS (16 visitor scenarios)");
