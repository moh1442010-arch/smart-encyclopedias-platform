      document.addEventListener("DOMContentLoaded", () => {
  const CONFIG = window.STORE_CONFIG || {};

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
  // بيانات العرض
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
  // المساعد
  // =========================

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
        "مرحبًا بك 👋\nأنا مساعد منصة الموسوعات الذكية.\n\nيمكنك سؤالي عن المحتوى أو السعر أو الصفحات أو طريقة الشراء.",
        "botmsg"
      );
    }

    setTimeout(() => {
      if (chatInput) chatInput.focus();
    }, 150);
  }

  function closeChat() {
    if (!overlay) return;

    overlay.classList.remove("show");
    overlay.setAttribute("aria-hidden", "true");
  }

  // جميع أزرار "اسأل المساعد"
  document.querySelectorAll("[data-open-chat]").forEach((button) => {
    button.addEventListener("click", openChat);
  });

  // زر الإغلاق
  document.querySelectorAll("[data-close-chat]").forEach((button) => {
    button.addEventListener("click", closeChat);
  });

  // الضغط خارج نافذة المحادثة
  if (overlay) {
    overlay.addEventListener("click", (event) => {
      if (event.target === overlay) {
        closeChat();
      }
    });
  }

  // زر Escape
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeChat();
    }
  });

  // =========================
  // الردود
  // =========================

  function respondToUser(text) {
    const q = text.toLowerCase();
    let reply;

    if (
      q.includes("سعر") ||
      q.includes("بكم") ||
      q.includes("تكلف") ||
      q.includes("كم")
    ) {
      reply =
        `سعر النسخة الكاملة هو ${price} ${currency}.\n` +
        `النسخة الكاملة تحتوي على ${totalPages} صفحة.`;
    }

    else if (
      q.includes("دفع") ||
      q.includes("شراء") ||
      q.includes("بنك") ||
      q.includes("تحويل") ||
      q.includes("بنكك")
    ) {
      reply =
        `طريقة الدفع داخل السودان: بنكك.\n` +
        `رقم الحساب: ${accountNumber}.\n\n` +
        `بعد التحويل أرسل إشعار الدفع عبر واتساب:\n${whatsapp}`;
    }

    else if (
      q.includes("صفحة") ||
      q.includes("صفحات") ||
      q.includes("عدد")
    ) {
      reply =
        `الموسوعة الكاملة تحتوي على ${totalPages} صفحة.\n` +
        `ومتاح ${previewPages} صفحة للمعاينة المجانية.`;
    }

    else if (
      q.includes("محتوى") ||
      q.includes("موضوع") ||
      q.includes("تحتوي") ||
      q.includes("ماذا سأحصل")
    ) {
      reply =
        "الموسوعة تتناول الذكاء الاصطناعي من الأساسيات إلى الموضوعات المتقدمة، " +
        "ومنها LLMs وRAG وهندسة الأوامر والوكلاء والذكاء متعدد الوسائط.";
    }

    else if (
      q.includes("واتساب") ||
      q.includes("واتس")
    ) {
      reply =
        `يمكنك التواصل عبر واتساب على:\n${whatsapp}\n\n` +
        "وبعد التحويل أرسل إشعار الدفع لتأكيد الطلب.";
    }

    else {
      reply =
        "أهلًا بك 👋\n\n" +
        "يمكنك سؤالي عن:\n" +
        "• محتوى الموسوعة\n" +
        "• عدد الصفحات\n" +
        "• السعر\n" +
        "• طريقة الدفع\n" +
        "• طريقة الشراء";
    }

    addMessage(reply, "botmsg");
  }

  // إرسال السؤال
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
  // إنشاء صفحات المعاينة
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
  // تكبير صفحة المعاينة
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
