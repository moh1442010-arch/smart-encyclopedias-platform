document.addEventListener("DOMContentLoaded", () => {
  "use strict";
  const CONFIG = window.STORE_CONFIG || {};
  const apiUrl = String(CONFIG.agentApiUrl || "").trim();
  if (!apiUrl) return;

  const form = document.getElementById("agentForm");
  const input = document.getElementById("agentInput");
  const log = document.getElementById("agentLog");
  const status = document.getElementById("agentStatus");
  if (!form || !input || !log) return;

  const addLog = (text, type = "") => {
    const item = document.createElement("div");
    item.className = `agent-item ${type}`;
    item.textContent = text;
    log.appendChild(item);
    log.scrollTop = log.scrollHeight;
  };

  const setStatus = (text) => { if (status) status.textContent = text; };

  const executeAction = (action) => {
    if (!action || !action.type) return;
    switch (action.type) {
      case "open_preview":
        document.getElementById("preview")?.scrollIntoView({ behavior: "smooth", block: "start" });
        break;
      case "open_preview_page": {
        const page = Number(action.page);
        if (Number.isInteger(page) && page >= 1 && page <= 20) {
          document.getElementById("preview")?.scrollIntoView({ behavior: "smooth", block: "start" });
          window.setTimeout(() => window.open(`${CONFIG.previewPdf || "الموسوعة_الشاملة_20_صفحة_للمعاينة.pdf"}#page=${page}`, "_blank", "noopener,noreferrer"), 350);
        }
        break;
      }
      case "open_whatsapp":
        window.open(CONFIG.whatsappUrl || `https://wa.me/${String(CONFIG.whatsapp || "").replace(/\D/g, "")}`, "_blank", "noopener,noreferrer");
        break;
      case "focus_offer":
        document.querySelector(".offer")?.scrollIntoView({ behavior: "smooth", block: "center" });
        break;
      default:
        break;
    }
  };

  async function askAgent(message) {
    const response = await fetch(apiUrl, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ message, context: { page: window.location.pathname } }),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.message || "agent unavailable");
    return data;
  }

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    event.stopImmediatePropagation();
    const text = input.value.trim();
    if (!text) return;
    input.value = "";
    addLog(`طلب المستخدم: ${text}`, "action");
    setStatus("جارٍ الاتصال بالوكيل...");
    try {
      const data = await askAgent(text);
      addLog(data.reply || "تمت معالجة الطلب.", "done");
      (Array.isArray(data.actions) ? data.actions : []).forEach(executeAction);
      setStatus("اكتملت المهمة. أنا جاهز لطلب جديد.");
    } catch (error) {
      addLog("تعذر الاتصال بالوكيل السحابي. سيتم استخدام الوكيل المحلي.", "done");
      setStatus("الوكيل المحلي جاهز.");
    }
  }, true);
});
