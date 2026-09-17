const SYSTEM_PROMPT = `أنت الوكيل الذكي ومسؤول المبيعات الودود لمنصة الموسوعات الذكية.
هدفك تحويل الزائر من فضولي إلى مقتنع ثم مشتري عندما تكون الموسوعة مناسبة له، عبر حوار استشاري صادق: افهم الحاجة، اربط القيمة بالحاجة، عالج الاعتراض، ثم اقترح خطوة واحدة واضحة. لا تكذب ولا تختلق شهادات أو أعداد مبيعات أو ضمانات أو استعجالاً زائفاً.

المعلومات الثابتة: الموسوعة الكاملة 250 صفحة، والمعاينة 20 صفحة حقيقية. السعر الأساسي لغير الطلاب 150000 جنيه سوداني أو 19 دولاراً. عرض أول 200 نسخة لغير الطلاب فقط بسعر 120000 جنيه سوداني أو 16 دولاراً. الطالب بعد إثبات صفته يحصل على 120000 جنيه سوداني أو 16 دولاراً بشكل ثابت في جميع الأوقات، ولا يرتبط هذا السعر بانتهاء عرض أول 200 نسخة، ولا يُجمع مع خصم آخر. النسخة الكاملة تشمل الوكيل الذكي.

قاعدة الدفع والتسليم الإلزامية: أنت لا تتحقق من وصول المال بنفسك، ولا تعتبر رسالة العميل أو صورة التحويل اعتماداً نهائياً، ولا تنشئ ترخيصاً ولا رابط تسليم ولا ترسل النسخة. بعد أن يقول العميل إنه دفع، قل له إن الطلب دخل مرحلة مراجعة الدفع وأن صاحب الموسوعة هو من يعتمد العملية. لا تعده بالتسليم قبل الاعتماد. بعد اعتماد الدفع من صاحب الموسوعة فقط يبدأ إجراء التسليم عبر النظام.

عند ادعاء الدفع: استخدم إجراء open_whatsapp مع حقل message يحتوي بلاغاً واضحاً لصاحب الموسوعة، متضمناً نص رسالة العميل نفسه، وأن البلاغ يحتاج مراجعة واعتماداً يدوياً. لا تقل إن الرسالة أُرسلت تلقائياً؛ الإجراء يفتح قناة واتساب ويجهز الرسالة للإرسال.

الحماية: النسخة المدفوعة مرتبطة بترخيص واحد وبجهاز/ملف متصفح واحد بعد أول تفعيل. إذا طلب العميل استخدام الترخيص على جهاز آخر، لا تمنحه طريقة لتجاوز الربط؛ وجّهه للتواصل مع صاحب الموسوعة لإعادة التفعيل رسمياً.

لا تقل إن الموسوعة تلغي كل الكورسات. لا تدّع قراءة كامل النسخة المدفوعة أو تنفيذ عمليات مصرفية. لا تطلب كلمة مرور أو بطاقة أو مفتاح API. أعد JSON فقط مع reply وactions. الإجراءات: open_preview, open_preview_page, open_whatsapp, focus_offer, open_checkout.`;

const ALLOWED_ACTIONS = new Set(["open_preview", "open_preview_page", "open_whatsapp", "focus_offer", "open_checkout"]);

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "content-type, authorization",
      "cache-control": "no-store"
    }
  });
}

function corsPreflight() {
  return new Response(null, { status: 204, headers: {
    "access-control-allow-origin": "*",
    "access-control-allow-methods": "GET, POST, OPTIONS",
    "access-control-allow-headers": "content-type, authorization"
  }});
}

function safeActionMessage(value) {
  return String(value || "").replace(/[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]/g, "").trim().slice(0, 1800);
}

function sanitizeActions(actions) {
  if (!Array.isArray(actions)) return [];
  return actions.filter(a => a && ALLOWED_ACTIONS.has(a.type)).slice(0, 3).map(a => {
    const out = { type: a.type };
    if (a.type === "open_preview_page") {
      const page = Number(a.page);
      if (!Number.isInteger(page) || page < 1 || page > 20) return null;
      out.page = page;
    }
    if (a.type === "open_whatsapp" && a.message) out.message = safeActionMessage(a.message);
    return out;
  }).filter(Boolean);
}

function normalizeDigits(s) {
  return String(s || "")
    .replace(/[٠-٩]/g, d => String("٠١٢٣٤٥٦٧٨٩".indexOf(d)))
    .replace(/٫/g, ".");
}

function textOf(message) { return normalizeDigits(message).trim().toLowerCase(); }
function has(q, ...terms) { return terms.some(t => q.includes(t)); }

function previewPageFromMessage(message) {
  const q = textOf(message);
  if (!has(q, "صفحة", "صفحه", "page")) return null;
  const m = q.match(/(?:صفحة|صفحه|page)\s*(?:رقم\s*)?(\d{1,2})/i);
  if (!m) return null;
  const page = Number(m[1]);
  return page >= 1 && page <= 20 ? page : null;
}

function quantityFromMessage(message) {
  const q = textOf(message);
  if (has(q, "صفحة", "صفحات", "صفحه", "preview", "معاينة", "تجربة")) return null;
  const patterns = [
    /(?:^|\s)(\d{1,5})\s*(?:نسخ(?:ة)?|نسخه|copy|copies|طبعة|كتاب|كتب)\b/i,
    /(?:أريد|اريد|عايز|عاوز|نحتاج|نريد|سأشتري|ساشتري)\s*(\d{1,5})\b/i,
    /(?:كم|عدد)\s*(?:من\s*)?(\d{1,5})\s*(?:نسخ|نسخة|نسخه)?\b/i,
    /كم\s+(?:سعر|ثمن)\s+(\d{1,5})\s*(?:نسخ|نسخة|نسخه)?\b/i
  ];
  for (const pattern of patterns) {
    const m = q.match(pattern);
    if (m) return Math.max(1, Math.min(10000, Number(m[1])));
  }
  return null;
}

function salesQuantityReply(message) {
  const q = quantityFromMessage(message);
  if (!q) return null;
  const text = textOf(message);
  const usd = has(text, "دولار", "usd", "$", "بالدولار");
  const offer = Math.min(q, 200);
  const regular = Math.max(0, q - 200);
  if (usd) {
    const total = offer * 16 + regular * 19;
    const normal = q * 19;
    const saving = normal - total;
    return { reply: `لعدد ${q} نسخة: ${offer} نسخة بسعر العرض 16 دولاراً${regular ? `، و${regular} نسخة بالسعر الأساسي 19 دولاراً` : ""}. الإجمالي ${total.toLocaleString("en-US")} دولار، والتوفير ${saving.toLocaleString("en-US")} دولار. إذا كنت جاهزاً أفتح لك صفحة الشراء.`, actions: [{ type: "open_checkout" }] };
  }
  const total = offer * 120000 + regular * 150000;
  const normal = q * 150000;
  const saving = normal - total;
  return { reply: `لعدد ${q} نسخة: ${offer} نسخة بسعر العرض 120,000 ج.س${regular ? `، و${regular} نسخة بالسعر الأساسي 150,000 ج.س` : ""}. الإجمالي ${total.toLocaleString("en-US")} جنيه سوداني، والتوفير ${saving.toLocaleString("en-US")} جنيه. إذا كنت جاهزاً أفتح لك صفحة الشراء.`, actions: [{ type: "open_checkout" }] };
}

function paymentReviewAction(message) {
  const clean = safeActionMessage(message);
  return {
    type: "open_whatsapp",
    message: `🔔 بلاغ دفع يحتاج مراجعة صاحب الموسوعة\n\nرسالة الزبون:\n${clean}\n\n⚠️ هذا البلاغ لا يعني اعتماد الدفع. يرجى مراجعة العملية واعتمادها يدوياً قبل إنشاء الترخيص أو تسليم النسخة.`
  };
}

function fallbackAgent(message, history = []) {
  const q = textOf(message);

  if (has(q, "كلمة المرور", "كلمه المرور", "رقم البطاقة", "رقم البطاقه", "مفتاح api", "api key", "password")) {
    return { reply: "لا ترسل لي كلمة مرور أو بيانات بطاقة أو مفتاح API. يمكنني مساعدتك في معلومات المنتج والشراء عبر القنوات المعلنة فقط.", actions: [] };
  }

  if (has(q, "دفعت", "دفعت المبلغ", "حولت", "حولت المبلغ", "تم التحويل", "تم الدفع", "ارسلت التحويل", "أرسلت التحويل", "اثبات الدفع", "إثبات الدفع", "صورة التحويل", "وصل المبلغ")) {
    return { reply: "شكراً لك. دخل طلبك الآن مرحلة مراجعة الدفع. الوكيل لا يعتمد وصول المال بنفسه، ولن يتم إنشاء الترخيص أو إرسال النسخة قبل اعتماد صاحب الموسوعة. سأفتح لك قناة المراجعة في واتساب لتجهيز البلاغ لصاحب الموسوعة.", actions: [paymentReviewAction(message)] };
  }

  const page = previewPageFromMessage(message);
  if (page !== null && has(q, "معاينة", "preview", "افتح", "شاهد", "أرني", "ارني")) {
    return { reply: `بالتأكيد. أفتح لك الصفحة ${page} من المعاينة المجانية ذات الـ20 صفحة.`, actions: [{ type: "open_preview_page", page }] };
  }

  if (has(q, "التجربة المجانية", "تجربة مجانية", "المعاينة المجانية", "المعاينه المجانيه", "أريد المعاينة", "اريد المعاينة", "أريد المعاينه", "اريد المعاينه", "قبل الشراء", "قبل ما أشتري", "قبل ما اشتري", "للتجربة", "للتجربه", "أريد أن أرى", "اريد ان ارى", "أريد اشوف", "اريد اشوف", "أتأكد من الجودة", "اتاكد من الجودة", "أتأكد قبل الدفع", "اتاكد قبل الدفع", "قبل الدفع", "أريد التحقق", "اريد التحقق")) {
    return { reply: "بالتأكيد. أفضل خطوة هي أن تشاهد 20 صفحة حقيقية من المعاينة المجانية أولاً، ثم تقرر بعد أن ترى المحتوى بنفسك.", actions: [{ type: "open_preview" }] };
  }

  if (has(q, "أريد التواصل مباشرة", "اريد التواصل مباشرة", "عايز أتواصل مباشرة", "عايز اتواصل مباشرة", "أريد شخصًا", "اريد شخصا", "تواصل مباشر", "التواصل المباشر", "واتساب", "واتس")) {
    return { reply: "بكل سرور. أفتح لك قناة واتساب للتواصل المباشر والاستفسار أو إتمام الطلب.", actions: [{ type: "open_whatsapp" }] };
  }

  if (has(q, "مؤسسة", "مؤسسه", "موظفينا", "موظفين", "شركة", "شركه")) {
    return { reply: "بكل سرور. للمؤسسات والشركات يمكنني حساب عدد النسخ والتكلفة حسب الكمية، ثم فتح صفحة الشراء عند الجاهزية.", actions: [{ type: "focus_offer" }] };
  }

  if (has(q, "كم صفحة", "كم صفحه", "عدد الصفحات", "صفحة في الموسوعة", "صفحه في الموسوعه")) {
    return { reply: "الموسوعة الكاملة 250 صفحة، ويمكنك معاينة 20 صفحة حقيقية مجاناً قبل الشراء.", actions: [{ type: "open_preview" }] };
  }

  const bulk = salesQuantityReply(message);
  if (bulk) return bulk;

  if (has(q, "كم السعر", "السعر", "بكم", "كم سعر", "السعر بالدولار", "سعر النسخة", "سعر النسخه", "أول 200 بالدولار", "من أول 200 بالدولار")) {
    if (has(q, "طالب", "طالبة", "طلاب")) return { reply: "للطالب بعد إثبات صفته: 120,000 ج.س أو 16 دولاراً، وهذا سعر ثابت في جميع الأوقات ولا يرتبط بانتهاء عرض أول 200 نسخة، ولا يُجمع مع خصم آخر. إذا أردت، أفتح لك المعاينة المجانية أولاً.", actions: [{ type: "open_preview" }] };
    if (has(q, "دولار", "usd", "$")) return { reply: "لغير الطلاب: السعر الأساسي 19 دولاراً، وعرض أول 200 نسخة 16 دولاراً. وبالجنيه السوداني: 150,000 ج.س أساسياً و120,000 ج.س لأول 200 نسخة. الطالب بعد إثبات صفته: 16 دولاراً ثابتاً ولا يجمع مع خصم آخر.", actions: [{ type: "focus_offer" }] };
    return { reply: "لغير الطلاب: السعر الأساسي 150,000 ج.س، وعرض أول 200 نسخة 120,000 ج.س. وبالدولار: 19 دولاراً أساسياً و16 دولاراً لأول 200 نسخة. الطالب بعد إثبات صفته: 120,000 ج.س ثابت ولا يجمع مع خصم آخر.", actions: [{ type: "focus_offer" }] };
  }

  if (has(q, "الكورس", "الدورة", "الدوره") && has(q, "تغني", "بديل", "بديله", "يغني")) {
    return { reply: "الموسوعة ليست وعداً بأنها تلغي كل الكورسات. هي مرجع تعليمي متكامل من الأساسيات إلى التطبيقات، ومعها الوكيل الذكي للمساعدة. وإذا كنت تحتاج تدريباً عملياً مباشراً، يمكن أن تكون مرجعاً مكملاً للكورس.", actions: [{ type: "open_preview" }] };
  }

  if (has(q, "غالي", "مكلف", "معلومات مجانية", "معلومات مجانا", "مجاناً", "مجانا")) {
    return { reply: "أتفهم الاعتراض. القيمة هنا في تقديم محتوى عربي منظم في موسوعة كاملة من 250 صفحة مع وكيل ذكي للمساعدة، مع إمكانية فحص الجودة بنفسك عبر 20 صفحة حقيقية قبل الدفع. إذا أعجبتك المعاينة يصبح القرار أسهل.", actions: [{ type: "open_preview" }] };
  }

  if (has(q, "سأفكر", "سافكر", "أفكر", "افكر", "لاحقا", "لاحقاً", "ارجع لاحقا", "أرجع لاحقا")) {
    return { reply: "خذ وقتك. أفضل خطوة الآن أن تشاهد 20 صفحة من المعاينة، وإذا وجدت المحتوى مناسباً يمكنك العودة للشراء دون قرار متسرع.", actions: [{ type: "open_preview" }] };
  }

  if (has(q, "طالب", "طالبة", "طلاب")) {
    return { reply: "نعم، مناسبة للطالب الذي يريد بناء أساس متدرج في الذكاء الاصطناعي. بعد إثبات صفة الطالب يكون السعر 120,000 ج.س أو 16 دولاراً بشكل ثابت في جميع الأوقات، ولا يرتبط بانتهاء عرض أول 200 نسخة ولا يُجمع مع خصم آخر. ويمكنك البدء بالمعاينة المجانية.", actions: [{ type: "open_preview" }] };
  }

  if (has(q, "ماذا سأتعلم", "ماذا ساتعلم", "ماذا سأجد", "محتوى الموسوعة", "محتوى الموسوعه")) {
    return { reply: "ستجد مساراً منظماً في الذكاء الاصطناعي يشمل تعلم الآلة، التعلم العميق، النماذج اللغوية الكبيرة LLM، وRAG، وهندسة الأوامر Prompt Engineering، والوكلاء الذكيين والموضوعات متعددة الوسائط.", actions: [] };
  }

  if (has(q, "الوكيل", "وكيل ذكي")) {
    return { reply: "الوكيل الذكي المرفق مع النسخة الكاملة يساعد في الشرح والتدريب والإجابة عن الأسئلة المتعلقة بالمحتوى، ويعمل كرفيق تعليمي للمشتري. الوصول الكامل يبدأ بعد اعتماد الدفع وتفعيل الترخيص.", actions: [] };
  }

  if (has(q, "لا أحتاج", "لا احتاج", "لا أريدها", "لا اريدها")) {
    return { reply: "مفهوم. قبل أن تستبعدها، يمكنك إلقاء نظرة على 20 صفحة حقيقية مجاناً، ثم تقرر إن كانت مناسبة لاحتياجك.", actions: [{ type: "open_preview" }] };
  }

  if (has(q, "ما هي الموسوعة", "ما هي الموسوعه", "ما تقدمون", "ماذا تقدمون", "أول مرة", "اول مرة")) {
    return { reply: "هي موسوعة عربية شاملة في الذكاء الاصطناعي من الصفر إلى الاحتراف، في 250 صفحة، مع 20 صفحة معاينة مجانية، والنسخة الكاملة تشمل وكيلاً ذكياً للمساعدة التعليمية.", actions: [] };
  }

  if (has(q, "أريد الشراء", "اريد الشراء", "جاهز أشتري", "جاهز اشتري", "اشترِ الآن", "اشتري الآن", "الدفع", "الشراء الآن", "الشراء")) {
    return { reply: "ممتاز. أفتح لك صفحة الشراء لإتمام الطلب. بعد إرسال الدفع ستظل العملية بانتظار مراجعة واعتماد صاحب الموسوعة قبل إنشاء الترخيص والتسليم.", actions: [{ type: "open_checkout" }] };
  }

  return { reply: "أهلاً بك 🌟 أستطيع أن أوضح لك المحتوى والأسعار والمعاينة والوكيل الذكي، أو أساعدك في إتمام الشراء. يمكنك البدء بمشاهدة 20 صفحة حقيقية مجاناً.", actions: [{ type: "open_preview" }] };
}

async function callGemini(message, env, history = []) {
  if (!env.GEMINI_API_KEY) throw new Error("GEMINI_API_KEY is not configured");
  const model = env.GEMINI_MODEL || "gemini-2.5-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(env.GEMINI_API_KEY)}`;
  const contents = Array.isArray(history) ? history.slice(-12).map(m => ({
    role: (m?.role === "model" || m?.role === "assistant") ? "model" : "user",
    parts: [{ text: String(m?.text || "") }]
  })).filter(m => m.parts[0].text) : [];
  if (!contents.length || contents.at(-1)?.parts?.[0]?.text !== message) contents.push({ role: "user", parts: [{ text: message }] });
  const response = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] }, contents, generationConfig: { temperature: 0.25, responseMimeType: "application/json" } })
  });
  if (!response.ok) throw new Error(`Gemini request failed: ${response.status}`);
  const data = await response.json();
  const text = data?.candidates?.[0]?.content?.parts?.map(p => p.text || "").join("") || "";
  if (!text) throw new Error("Empty model response");
  return JSON.parse(text);
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return corsPreflight();
    const url = new URL(request.url);
    if (url.pathname !== "/api/agent") return json({ ok: true, service: "smart-agent" });
    if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);
    try {
      const body = await request.json();
      const message = String(body?.message || "").trim();
      const history = Array.isArray(body?.history) ? body.history : [];
      if (!message) return json({ reply: "اكتب سؤالك وسأساعدك.", actions: [] }, 400);

      const q = textOf(message);
      const deterministic = fallbackAgent(message, history);
      const isHighPriority =
        previewPageFromMessage(message) !== null ||
        has(q, "التجربة المجانية", "تجربة مجانية", "المعاينة المجانية", "أريد المعاينة", "اريد المعاينة", "قبل الشراء", "قبل الدفع", "أريد التحقق", "اريد التحقق", "أتأكد قبل الدفع", "اتاكد قبل الدفع") ||
        quantityFromMessage(message) !== null ||
        has(q, "دفعت", "دفعت المبلغ", "حولت", "تم التحويل", "تم الدفع", "اثبات الدفع", "إثبات الدفع", "صورة التحويل", "وصل المبلغ") ||
        has(q, "أريد التواصل مباشرة", "اريد التواصل مباشرة", "تواصل مباشر", "التواصل المباشر", "واتساب", "واتس") ||
        has(q, "أريد الشراء", "اريد الشراء", "جاهز أشتري", "جاهز اشتري", "اشترِ الآن", "اشتري الآن", "الشراء الآن");

      let result = deterministic;
      if (!isHighPriority && env.GEMINI_API_KEY) {
        try { result = await callGemini(message, env, history); } catch { result = deterministic; }
      }
      return json({ reply: String(result?.reply || deterministic.reply), actions: sanitizeActions(result?.actions || deterministic.actions) });
    } catch (error) {
      return json({ reply: "حدث عطل مؤقت. يمكنك متابعة المعاينة المجانية أو التواصل عبر واتساب.", actions: [{ type: "open_preview" }], error: env?.DEBUG ? String(error?.message || error) : undefined });
    }
  }
};