document.addEventListener("DOMContentLoaded", () => {
  const CONFIG = window.STORE_CONFIG || {};

  // =========================
  // بيانات الموسوعة
  // =========================

  const price = CONFIG.price || "150";
  const currency = CONFIG.currency || "جنيه سوداني";
  const totalPages = CONFIG.pages || 250;
  const previewPages = CONFIG.previewPages || 20;
  const accountNumber = CONFIG.accountNumber || "1882224";
  const whatsapp = CONFIG.whatsapp || "+249914488188";
  const whatsappUrl =
    CONFIG.whatsappUrl ||
    `https://wa.me/${whatsapp.replace(/\D/g, "")}`;

  // =========================
  // تحديث معلومات الصفحة
  // =========================

  const priceValue = document.getElementById("priceValue");
  const offerTitle = document.getElementById("offerTitle");
  const offerText = document.getElementById("offerText");
  const oldPrice = document.getElementById("oldPrice");
  const account = document.getElementById("accountNumber");
  const whatsappNumber = document.getElementById("whatsappNumber");
  const whatsappLink = document.getElementById("whatsappLink");

  if (priceValue) {
    priceValue.textContent = `${price} ${currency}`;
  }

  if (offerTitle && CONFIG.offerTitle) {
    offerTitle.textContent = CONFIG.offerTitle;
  }

  if (offerText && CONFIG.offerText) {
    offerText.textContent = CONFIG.offerText;
  }

  if (oldPrice && CONFIG.oldPrice) {
    oldPrice.textContent = `${CONFIG.oldPrice} ${currency}`;
  }

  if (account) {
    account.textContent = accountNumber;
  }

  if (whatsappNumber) {
    whatsappNumber.textContent = whatsapp;
  }

  if (whatsappLink) {
    whatsappLink.href = whatsappUrl;
  }

  // =========================
  // عناصر المساعد
  // =========================

  const overlay = document.getElementById("chatOverlay");
  const chatForm = document.getElementById("chatForm");
  const chatInput = document.getElementById("chatInput");
  const messages = document.getElementById("messages");

  // =========================
  // إضافة رسالة
  // =========================

  function addMessage(text, className) {
    if (!messages) return;

    const msg = document.createElement("div");

    msg.className = `msg ${className}`;
    msg.style.whiteSpace = "pre-line";
    msg.textContent = text;

    messages.appendChild(msg);
    messages.scrollTop = messages.scrollHeight;
  }

  // =========================
  // فتح المساعد
  // =========================

  function openChat() {
    if (!overlay) return;

    overlay.classList.add("show");
    overlay.setAttribute("aria-hidden", "false");

    if (messages && messages.children.length === 0) {
      addMessage(
        "مرحبًا بك 👋\n\n" +
        "أنا مساعد منصة الموسوعات الذكية.\n" +
        "يمكنني مساعدتك في معرفة محتوى الموسوعة، السعر، عدد الصفحات، طريقة الدفع والشراء.\n\n" +
        "اكتب سؤالك أو اختر أحد الخيارات.",
        "botmsg"
      );
    }

    setTimeout(() => {
      if (chatInput) chatInput.focus();
    }, 150);
  }

  // =========================
  // إغلاق المساعد
  // =========================

  function closeChat() {
    if (!overlay) return;

    overlay.classList.remove("show");
    overlay.setAttribute("aria-hidden", "true");
  }

  // =========================
  // أزرار فتح المساعد
  // =========================

  document.querySelectorAll("[data-open-chat]").forEach((button) => {
    button.addEventListener("click", openChat);
  });

  document.querySelectorAll("[data-close-chat]").forEach((button) => {
    button.addEventListener("click", closeChat);
  });

  if (overlay) {
    overlay.addEventListener("click", (event) => {
      if (event.target === overlay) {
        closeChat();
      }
    });
  }

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeChat();
    }
  });

  // =========================
  // تنظيف السؤال
  // =========================

  function normalize(text) {
    return text
      .toLowerCase()
      .replace(/[إأآا]/g, "ا")
      .replace(/ة/g, "ه")
      .replace(/ى/g, "ي")
      .replace(/[؟?!.,،]/g, " ")
      .replace(/\s+/g, " ")
      .trim();
  }

  // =========================
  // تحديد نوع السؤال
  // =========================

  function detectIntent(text) {
    const q = normalize(text);

    // السعر
    if (
      /سعر|بكم|بكام|تكلف|التكلفه|القيمه|كم.*جنيه|كم.*السعر/.test(q)
    ) {
      return "price";
    }

    // الصفحات
    if (
      /عدد.*صفح|كم.*صفح|صفح.*عدد|250.*صفح|مئتين.*خمسين/.test(q)
    ) {
      return "pages";
    }

    // المعاينة
    if (
      /معاين|الصفح.*المتاح|صفحه مجاني|مجاني|مجانا|اشوف.*صفح|شوف.*الموسوعه/.test(q)
    ) {
      return "preview";
    }

    // الدفع
    if (
      /دفع|ادفع|تحويل|احول|بنكك|بنك|حساب|رقم الحساب|طريقه الدفع/.test(q)
    ) {
      return "payment";
    }

    // شراء
    if (
      /اشتري|شراء|اشتراء|اريد.*شراء|كيف.*اشتري|كيف.*اشتريها|الحصول.*النسخه|اريد.*النسخه/.test(q)
    ) {
      return "buy";
    }

    // واتساب
    if (
      /واتساب|واتس|رقم.*تواصل|تواصل.*معكم|رقم.*الشراء|رقم.*الهاتف/.test(q)
    ) {
      return "whatsapp";
    }

    // المحتوى
    if (
      /محتوي|تحتوي|الموسوعه.*عن|ماذا.*في|ماذا.*تتناول|المواضيع|موضوعات|موضوع.*الموسوعه/.test(q)
    ) {
      return "content";
    }

    // الفائدة
    if (
      /استفيد|الفائده|فائده|ماذا.*استفيد|ماذا.*ساحصل|لماذا.*اشتري|هل.*تستحق|تستحق.*الشراء/.test(q)
    ) {
      return "benefit";
    }

    // ترحيب
    if (
      /مرحبا|اهلا|السلام عليكم|سلام|هاي|hello|hi/.test(q)
    ) {
      return "welcome";
    }

    return "unknown";
  }

  // =========================
  // الردود الاحترافية
  // =========================

  function respondToUser(text) {
    const intent = function detectIntent(text) {
  const q = normalize(text);

  // السعر
  if (
    q.includes("سعر") ||
    q.includes("بكم") ||
    q.includes("كم السعر") ||
    q.includes("تكلفة") ||
    q.includes("القيمة")
  ) {
    return "price";
  }

  // الصفحات
  if (
    q.includes("عدد الصفحات") ||
    q.includes("كم صفحة") ||
    q.includes("عدد صفحة") ||
    q.includes("250 صفحة")
  ) {
    return "pages";
  }

  // المعاينة
  if (
    q.includes("معاينة") ||
    q.includes("مجاني") ||
    q.includes("مجانية") ||
    q.includes("الصفحات المجانية") ||
    q.includes("اشوف الصفحات")
  ) {
    return "preview";
  }

  // الدفع
  if (
    q.includes("دفع") ||
    q.includes("ادفع") ||
    q.includes("تحويل") ||
    q.includes("بنكك") ||
    q.includes("بنك") ||
    q.includes("حساب") ||
    q.includes("رقم الحساب") ||
    q.includes("طريقة الدفع")
  ) {
    return "payment";
  }

  // الشراء
  if (
    q.includes("شراء") ||
    q.includes("اشتري") ||
    q.includes("اريد شراء") ||
    q.includes("كيف اشتري") ||
    q.includes("الحصول على النسخة") ||
    q.includes("النسخة الكاملة")
  ) {
    return "buy";
  }

  // واتساب
  if (
    q.includes("واتساب") ||
    q.includes("واتس") ||
    q.includes("رقم التواصل") ||
    q.includes("رقم الهاتف")
  ) {
    return "whatsapp";
  }

  // المحتوى
  if (
    q.includes("المحتوى") ||
    q.includes("محتوى الموسوعة") ||
    q.includes("ماذا تحتوي") ||
    q.includes("موضوعات") ||
    q.includes("مواضيع") ||
    q.includes("ماذا تتناول")
  ) {
    return "content";
  }

  // الفائدة
  if (
    q.includes("الفائدة") ||
    q.includes("استفيد") ||
    q.includes("ماذا استفيد") ||
    q.includes("ماذا سأحصل") ||
    q.includes("هل تستحق الشراء") ||
    q.includes("لماذا اشتري")
  ) {
    return "benefit";
  }

  // الترحيب
  if (
    q.includes("مرحبا") ||
    q.includes("اهلا") ||
    q.includes("السلام عليكم") ||
    q === "سلام" ||
    q === "hello" ||
    q === "hi"
  ) {
    return "welcome";
  }

  return "unknown";
    }

    let reply = "";

    switch (intent) {
      case "price":
        reply =
          `💰 سعر النسخة الكاملة: ${price} ${currency}.\n\n` +
          `📚 الموسوعة الكاملة تحتوي على ${totalPages} صفحة.\n\n` +
          `إذا أردت الشراء، أستطيع توضيح طريقة الدفع الآن.`;
        break;

      case "pages":
        reply =
          `📚 النسخة الكاملة تحتوي على ${totalPages} صفحة.\n\n` +
          `👀 ويتوفر حاليًا ${previewPages} صفحة للمعاينة المجانية حتى تتعرف على جودة المحتوى قبل الشراء.`;
        break;

      case "preview":
        reply =
          `👀 نعم، يمكنك معاينة ${previewPages} صفحة مجانًا من الموسوعة.\n\n` +
          `الهدف من المعاينة أن ترى أسلوب المحتوى وجودة المادة قبل اتخاذ قرار الشراء.\n\n` +
          `📚 النسخة الكاملة: ${totalPages} صفحة.`;
        break;

      case "content":
        reply =
          "📚 الموسوعة عبارة عن دليل شامل في الذكاء الاصطناعي، " +
          "وتنتقل من المفاهيم الأساسية إلى موضوعات أكثر تقدمًا.\n\n" +
          "ومن أبرز الموضوعات:\n" +
          "• الذكاء الاصطناعي ومفاهيمه الأساسية\n" +
          "• نماذج اللغة الكبيرة LLMs\n" +
          "• هندسة الأوامر Prompt Engineering\n" +
          "• RAG واسترجاع المعلومات\n" +
          "• وكلاء الذكاء الاصطناعي AI Agents\n" +
          "• الذكاء الاصطناعي متعدد الوسائط\n\n" +
          "والموسوعة الكاملة تحتوي على " +
          `${totalPages} صفحة.`;
        break;

      case "benefit":
        reply =
          "🎯 الفكرة من الموسوعة هي أن تحصل على مرجع عربي منظم " +
          "يساعدك على فهم عالم الذكاء الاصطناعي بدل البحث عن المعلومات المتفرقة في أماكن كثيرة.\n\n" +
          "وهي مناسبة لمن يريد بناء أساس جيد وفهم المصطلحات والتقنيات الحديثة.\n\n" +
          `📚 النسخة الكاملة: ${totalPages} صفحة.\n` +
          `👀 ويمكنك معاينة ${previewPages} صفحة مجانًا قبل الشراء.`;
        break;

      case "payment":
        reply =
          "💳 طريقة الدفع داخل السودان:\n\n" +
          "البنك: بنكك\n" +
          `رقم الحساب: ${accountNumber}\n\n` +
          "بعد إتمام التحويل، أرسل إشعار الدفع عبر واتساب لتأكيد طلبك.\n\n" +
          `📱 واتساب: ${whatsapp}`;
        break;

      case "buy":
        reply =
          "🛒 لشراء النسخة الكاملة:\n\n" +
          `1️⃣ السعر: ${price} ${currency}\n` +
          "2️⃣ التحويل عبر بنكك\n" +
          `3️⃣ رقم الحساب: ${accountNumber}\n` +
          "4️⃣ بعد التحويل أرسل إشعار الدفع عبر واتساب\n\n" +
          `📱 ${whatsapp}\n\n` +
          "وسنساعدك في إكمال الطلب.";
        break;

      case "whatsapp":
        reply =
          "📱 يمكنك التواصل معنا عبر واتساب:\n\n" +
          `${whatsapp}\n\n` +
          "ويمكنك إرسال إشعار التحويل هناك بعد إتمام الدفع.";
        break;

      case "welcome":
        reply =
          "أهلًا وسهلًا بك 👋\n\n" +
          "أنا مساعد منصة الموسوعات الذكية.\n" +
          "اسألني عن السعر، المحتوى، عدد الصفحات، المعاينة أو طريقة الشراء.";
        break;

      default:
        reply =
          "يسعدني مساعدتك 👋\n\n" +
          "يمكنك أن تسألني مثلًا:\n\n" +
          "💰 كم سعر الموسوعة؟\n" +
          "📚 ماذا تحتوي الموسوعة؟\n" +
          "📄 كم عدد الصفحات؟\n" +
          "👀 هل توجد معاينة مجانية؟\n" +
          "💳 كيف أدفع؟\n" +
          "🛒 كيف أشتري النسخة الكاملة؟\n" +
          "📱 ما رقم الواتساب؟";
    }

    addMessage(reply, "botmsg");
  }

  // =========================
  // إرسال السؤال
  // =========================

  if (chatForm) {
    chatForm.addEventListener("submit", (event) => {
      event.preventDefault();

      if (!chatInput) return;

      const text = chatInput.value.trim();

      if (!text) return;

      addMessage(text, "usermsg");

      chatInput.value = "";

      setTimeout(() => {
        respondToUser(text);
      }, 350);
    });
  }

  // =========================
  // الأزرار السريعة
  // =========================

  document.querySelectorAll("[data-q]").forEach((button) => {
    button.addEventListener("click", () => {
      const question = button.getAttribute("data-q");

      if (!question) return;

      openChat();

      addMessage(question, "usermsg");

      setTimeout(() => {
        respondToUser(question);
      }, 300);
    });
  });

  // =========================
  // معاينة الصفحات
  // =========================

  const previewGrid = document.getElementById("previewGrid");

  if (previewGrid) {
    const previewFolder = "assets/preview/";

    for (let i = 1; i <= previewPages; i++) {
      const card = document.createElement("article");
      card.className = "preview-card";

      const page = document.createElement("button");
      page.className = "page";
      page.type = "button";

      const img = document.createElement("img");
      img.src = `${previewFolder}${i}.jpg`;
      img.alt = `صفحة معاينة ${i}`;
      img.loading = "lazy";

      const label = document.createElement("span");
      label.textContent = `صفحة ${i}`;

      page.appendChild(img);
      page.appendChild(label);

      const title = document.createElement("h3");
      title.textContent = `صفحة ${i} من المعاينة`;

      const small = document.createElement("small");
      small.textContent = "معاينة مجانية";

      card.appendChild(page);
      card.appendChild(title);
      card.appendChild(small);

      previewGrid.appendChild(card);

      page.addEventListener("click", () => {
        openLightbox(img.src, `صفحة معاينة رقم ${i}`);
      });
    }
  }

  // =========================
  // تكبير صفحات المعاينة
  // =========================

  function openLightbox(src, title) {
    let box = document.getElementById("lightbox");

    if (!box) {
      box = document.createElement("div");
      box.id = "lightbox";
      box.className = "lightbox";

      box.innerHTML = `
        <button type="button" id="closeLightbox">✕</button>
        <img id="lightbox-img" src="" alt="">
        <div id="lightbox-title"></div>
      `;

      document.body.appendChild(box);

      document
        .getElementById("closeLightbox")
        .addEventListener("click", () => {
          box.remove();
        });

      box.addEventListener("click", (event) => {
        if (event.target === box) {
          box.remove();
        }
      });
    }

    document.getElementById("lightbox-img").src = src;
    document.getElementById("lightbox-title").textContent = title;
  }
});
