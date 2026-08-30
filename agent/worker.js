const SYSTEM_PROMPT = `أنت الوكيل الذكي لمنصة الموسوعات الذكية.
أجب بالعربية الواضحة وباختصار مفيد.
مهمتك مساعدة الزائر في فهم الموسوعة والمعاينة والشراء.
بيانات المنتج المعتمدة: السعر الأساسي 150000 جنيه سوداني، عرض أول 200 نسخة 112500 جنيه، سعر الطلاب 105000 جنيه وفق إثبات صفة الطالب، المعاينة 20 صفحة، النسخة الكاملة 250 صفحة، الدفع عبر بنكك إلى الحساب 1882224، وواتساب أعمال +249121851285.
لا تخترع أسعارًا أو أرقام حساب أو سياسات غير موجودة.
لا تطلب من المستخدم إرسال رقم بطاقة أو كلمة مرور أو مفتاح API.
إذا طلب المستخدم تنفيذ مهمة داخل الموقع، أعد إجراءً من القائمة المسموحة فقط.
الإجراءات المسموحة: open_preview, open_preview_page, open_whatsapp, focus_offer.
أعد JSON فقط بالشكل: {"reply":"...","actions":[{"type":"...","page":1}]}`;

const ALLOWED_ACTIONS = new Set([
  "open_preview",
  "open_preview_page",
  "open_whatsapp",
  "focus_offer",
]);

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST, OPTIONS",
      "access-control-allow-headers": "content-type",
    },
  });
}

function corsPreflight() {
  return new Response(null, {
    status: 204,
    headers: {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "POST, OPTIONS",
      "access-control-allow-headers": "content-type",
    },
  });
}

function sanitizeActions(actions) {
  if (!Array.isArray(actions)) return [];
  return actions
    .filter((a) => a && ALLOWED_ACTIONS.has(a.type))
    .slice(0, 3)
    .map((a) => {
      const out = { type: a.type };
      if (a.type === "open_preview_page") {
        const page = Number(a.page);
        if (Number.isInteger(page) && page >= 1 && page <= 20) out.page = page;
        else return null;
      }
      return out;
    })
    .filter(Boolean);
}

async function callGemini(message, env) {
  if (!env.GEMINI_API_KEY) throw new Error("GEMINI_API_KEY is not configured");

  const model = env.GEMINI_MODEL || "gemini-2.5-flash";
  const url = `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(model)}:generateContent?key=${encodeURIComponent(env.GEMINI_API_KEY)}`;

  const response = await fetch(url, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      systemInstruction: { parts: [{ text: SYSTEM_PROMPT }] },
      contents: [{ role: "user", parts: [{ text: message }] }],
      generationConfig: {
        temperature: 0.2,
        responseMimeType: "application/json",
      },
    }),
  });

  if (!response.ok) throw new Error(`Gemini request failed: ${response.status}`);
  const data = await response.json();
  const text = data?.candidates?.[0]?.content?.parts?.map((p) => p.text || "").join("") || "";
  if (!text) throw new Error("Empty model response");
  return JSON.parse(text);
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return corsPreflight();
    const url = new URL(request.url);
    if (url.pathname !== "/api/agent") return json({ error: "Not found" }, 404);
    if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

    try {
      const body = await request.json();
      const message = typeof body?.message === "string" ? body.message.trim() : "";
      if (!message || message.length > 2000) return json({ error: "Invalid message" }, 400);

      const result = await callGemini(message, env);
      const reply = typeof result?.reply === "string" ? result.reply.slice(0, 5000) : "لم أتمكن من إعداد إجابة الآن.";
      const actions = sanitizeActions(result?.actions);
      return json({ reply, actions });
    } catch (error) {
      return json({
        error: "agent_unavailable",
        message: "الوكيل السحابي غير متاح الآن. استخدم المساعد المحلي مؤقتًا.",
      }, 503);
    }
  },
};
