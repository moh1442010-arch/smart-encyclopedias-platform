document.addEventListener("DOMContentLoaded", () => {
  "use strict";

  const CONFIG = window.STORE_CONFIG || {};
  const price = CONFIG.price || "150";
  const currency = CONFIG.currency || "جنيه سوداني";
  const totalPages = Number(CONFIG.pages) || 250;
  const previewPages = Number(CONFIG.previewPages) || 20;
  const accountNumber = CONFIG.accountNumber || "1882224";
  const whatsapp = CONFIG.whatsapp || "+249914488188";
  const whatsappUrl = CONFIG.whatsappUrl || `https://wa.me/${whatsapp.replace(/\D/g, "")}`;
  const previewPdf = CONFIG.previewPdf || "الموسوعة_الشاملة_20_صفحة_للمعاينة.pdf";

  const setText = (id, value) => {
    const element = document.getElementById(id);
    if (element) element.textContent = value;
  };

  setText("priceValue", `${price} ${currency}`);
  if (CONFIG.offerTitle) setText("offerTitle", CONFIG.offerTitle);
  if (CONFIG.offerText) setText("offerText", CONFIG.offerText);
  if (CONFIG.oldPrice) setText("oldPrice", `${CONFIG.oldPrice} ${currency}`);
  setText("accountNumber", accountNumber);
  setText("whatsappNumber", whatsapp);

  const whatsappLink = document.getElementById("whatsappLink");
  if (whatsappLink) whatsappLink.href = whatsappUrl;

  const overlay = document.getElementById("chatOverlay");
  const chatForm = document.getElementById("chatForm");
  const chatInput = document.getElementById("chatInput");
  const messages = document.getElementById("messages");

  function addMessage(text, className) {
    if (!messages) return;
    const msg = document.createElement("div");
    msg.className = `msg ${className}`;
    msg.style.whiteSpace = "pre-line";
    msg.textContent = text;
    messages.appendChild(msg);
    messages.scrollTop = messages.scrollHeight;
  }

  function openChat() {
    if (!overlay) return;
    overlay.classList.add("show");
    overlay.setAttribute("aria-hidden", "false");
    if (messages && messages.children.length === 0) {
      addMessage(
        "مرحبًا بك 👋\n\nأنا مساعد منصة الموسوعات الذكية.\nيمكنني مساعدتك في معرفة المحتوى والسعر وعدد الصفحات وطريقة الدفع والشراء.\n\nاكتب سؤالك أو اختر أحد الخيارات.",
        "botmsg"
      );
    }
    window.setTimeout(() => chatInput?.focus(), 150);
  }

  function closeChat() {
    if (!overlay) return;
    overlay.classList.remove("show");
    overlay.setAttribute("aria-hidden", "true");
  }

  document.querySelectorAll("[data-open-chat]").forEach((button) => button.addEventListener("click", openChat));
  document.querySelectorAll("[data-close-chat]").forEach((button) => button.addEventListener("click", closeChat));
  overlay?.addEventListener("click", (event) => {
    if (event.target === overlay) closeChat();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") closeChat();
  });

  function normalize(text) {
    return String(text)
      .toLowerCase()
      .replace(/[إأآ]/g, "ا")
      .replace(/ة/g, "ه")
      .replace(/ى/g, "ي")
      .replace(/[؟?!.,،]/g, " ")
      .replace(/\s+/g, " ")
      .trim();
  }

  function detectIntent(text) {
    const q = normalize(text);
    if (/سعر|بكم|بكام|تكلف|التكلفه|القيمه|كم.*جنيه|كم.*السعر/.test(q)) return "price";
    if (/عدد.*صفح|كم.*صفح|صفح.*عدد|250.*صفح|مئتين.*خمسين/.test(q)) return "pages";
    if (/معاين|الصفح.*المتاح|صفحه مجاني|مجاني|مجانا|اشوف.*صفح|شوف.*الموسوعه/.test(q)) return "preview";
    if (/دفع|ادفع|تحويل|احول|بنكك|بنك|حساب|رقم الحساب|طريقه الدفع/.test(q)) return "payment";
    if (/اشتري|شراء|اشتراء|اريد.*شراء|كيف.*اشتري|كيف.*اشتريها|الحصول.*النسخه|اريد.*النسخه/.test(q)) return "buy";
    if (/واتساب|واتس|رقم.*تواصل|تواصل.*معكم|رقم.*الشراء|رقم.*الهاتف/.test(q)) return "whatsapp";
    if (/محتوي|تحتوي|الموسوعه.*عن|ماذا.*في|ماذا.*تتناول|المواضيع|موضوعات|موضوع.*الموسوعه/.test(q)) return "content";
    if (/استفيد|الفائده|فائده|ماذا.*استفيد|ماذا.*ساحصل|لماذا.*اشتري|هل.*تستحق|تستحق.*الشراء/.test(q)) return "benefit";
    if (/مرحبا|اهلا|السلام عليكم|سلام|هاي|hello|hi/.test(q)) return "welcome";
    return "unknown";
  }

  function respondToUser(text) {
    let reply;
    switch (detectIntent(text)) {
      case "price":
        reply = `سعر النسخة الكاملة: ${price} ${currency}.\n\nالموسوعة الكاملة تحتوي على ${totalPages} صفحة.\n\nإذا أردت الشراء، أستطيع توضيح طريقة الدفع.`;
        break;
      case "pages":
        reply = `النسخة الكاملة تحتوي على ${totalPages} صفحة.\n\nويتاح حاليًا ${previewPages} صفحة للمعاينة المجانية.`;
        break;
      case "preview":
        reply = `يمكنك معاينة ${previewPages} صفحة مجانًا.\n\nالنسخة الكاملة تحتوي على ${totalPages} صفحة.`;
        break;
      case "content":
        reply = `الموسوعة دليل عربي في الذكاء الاصطناعي ينتقل من المفاهيم الأساسية إلى موضوعات متقدمة.\n\nمن الموضوعات: الذكاء الاصطناعي، نماذج اللغة الكبيرة LLMs، هندسة الأوامر، RAG، وكلاء الذكاء الاصطناعي والذكاء متعدد الوسائط.\n\nالنسخة الكاملة: ${totalPages} صفحة.`;
        break;
      case "benefit":
        reply = `تحصل على مرجع عربي منظم يساعدك على فهم مفاهيم وتقنيات الذكاء الاصطناعي.\n\nيمكنك معاينة ${previewPages} صفحة قبل الشراء.`;
        break;
      case "payment":
        reply = `طريقة الدفع داخل السودان: بنكك.\n\nرقم الحساب: ${accountNumber}\n\nبعد التحويل أرسل إشعار الدفع عبر واتساب.\n\nواتساب: ${whatsapp}`;
        break;
      case "buy":
        reply = `لشراء النسخة الكاملة:\n\n1. السعر: ${price} ${currency}\n2. التحويل عبر بنكك\n3. رقم الحساب: ${accountNumber}\n4. إرسال إشعار التحويل عبر واتساب\n\nواتساب: ${whatsapp}`;
        break;
      case "whatsapp":
        reply = `يمكنك التواصل عبر واتساب: ${whatsapp}`;
        break;
      case "welcome":
        reply = "أهلًا وسهلًا بك 👋\n\nاسألني عن السعر أو المحتوى أو الصفحات أو المعاينة أو طريقة الشراء.";
        break;
      default:
        reply = "يمكنك السؤال مثلًا:\n\nكم سعر الموسوعة؟\nماذا تحتوي الموسوعة؟\nكم عدد الصفحات؟\nهل توجد معاينة مجانية؟\nكيف أدفع؟\nكيف أشتري النسخة الكاملة؟";
    }
    addMessage(reply, "botmsg");
  }

  chatForm?.addEventListener("submit", (event) => {
    event.preventDefault();
    const text = chatInput?.value.trim();
    if (!text) return;
    addMessage(text, "usermsg");
    chatInput.value = "";
    window.setTimeout(() => respondToUser(text), 250);
  });

  document.querySelectorAll("[data-q]").forEach((button) => {
    button.addEventListener("click", () => {
      const question = button.getAttribute("data-q");
      if (!question) return;
      openChat();
      addMessage(question, "usermsg");
      window.setTimeout(() => respondToUser(question), 250);
    });
  });

  const previewGrid = document.getElementById("previewGrid");

  function openPdfPage(pageNumber) {
    const separator = previewPdf.includes("#") ? "&" : "#";
    window.open(`${previewPdf}${separator}page=${pageNumber}`, "_blank", "noopener,noreferrer");
  }

  function createPreviewCard(pageNumber) {
    const card = document.createElement("article");
    card.className = "preview-card";

    const page = document.createElement("button");
    page.type = "button";
    page.className = "page";
    page.setAttribute("aria-label", `فتح صفحة المعاينة ${pageNumber}`);

    const img = document.createElement("img");
    img.src = `assets/assets/preview/${pageNumber}.jpg`;
    img.alt = `صفحة معاينة ${pageNumber}`;
    img.loading = "lazy";

    const fallback = document.createElement("span");
    fallback.className = "preview-fallback";
    fallback.textContent = `صفحة ${pageNumber}`;
    fallback.hidden = true;

    img.addEventListener("error", () => {
      img.hidden = true;
      fallback.hidden = false;
    });

    page.append(img, fallback);
    page.addEventListener("click", () => openPdfPage(pageNumber));

    const title = document.createElement("h3");
    title.textContent = `صفحة ${pageNumber} من المعاينة`;

    const small = document.createElement("small");
    small.textContent = "افتح الصفحة من ملف المعاينة";

    card.append(page, title, small);
    return card;
  }

  if (previewGrid) {
    for (let pageNumber = 1; pageNumber <= previewPages; pageNumber += 1) {
      previewGrid.appendChild(createPreviewCard(pageNumber));
    }
  }
});
