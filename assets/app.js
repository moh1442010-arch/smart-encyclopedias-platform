document.addEventListener("DOMContentLoaded", () => {
  "use strict";

  const CONFIG = window.STORE_CONFIG || {};
  const price = CONFIG.price || "150000";
  const currency = CONFIG.currency || "جنيه سوداني";
  const reservationOffer = CONFIG.reservationOffer || "112500";
  const studentPrice = CONFIG.studentPrice || "105000";
  const totalPages = Number(CONFIG.pages) || 250;
  const previewPages = Number(CONFIG.previewPages) || 20;
  const accountNumber = CONFIG.accountNumber || "1882224";
  const whatsapp = CONFIG.whatsapp || "+249121851285";
  const whatsappUrl = CONFIG.whatsappUrl || `https://wa.me/${whatsapp.replace(/\D/g, "")}`;
  const previewPdf = CONFIG.previewPdf || "الموسوعة_الشاملة_20_صفحة_للمعاينة.pdf";

  const setText = (id, value) => {
    const element = document.getElementById(id);
    if (element) element.textContent = value;
  };

  setText("priceValue", `${reservationOffer} ${currency}`);
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
        "مرحبًا بك. أنا شات بوت منصة التعليم الرقمي والموسوعات الذكية، تحت إشراف وتأليف محمد مصطفى بابكر.\n\nأساعدك في فهم الموسوعة والورش والأسعار وطريقة الشراء. كما يمكنني ترشيح نقطة البداية المناسبة لك.\n\nهل ترغب في المعاينة المجانية أم تريد معرفة العرض المتاح؟",
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
    if (/طالب|طلاب|جامع|بطاقه.*طالب|خصم.*طالب/.test(q)) return "student";
    if (/سعر|بكم|بكام|تكلف|التكلفه|القيمه|كم.*جنيه|كم.*السعر/.test(q)) return "price";
    if (/عرض.*200|اول.*200|اسبقيه|خصم.*25|112500/.test(q)) return "reservation";
    if (/عدد.*صفح|كم.*صفح|صفح.*عدد|250.*صفح|مئتين.*خمسين/.test(q)) return "pages";
    if (/معاين|الصفح.*المتاح|صفحه مجاني|مجاني|مجانا|اشوف.*صفح|شوف.*الموسوعه/.test(q)) return "preview";
    if (/دفع|ادفع|تحويل|احول|بنكك|بنك|حساب|رقم الحساب|طريقه الدفع/.test(q)) return "payment";
    if (/اشتري|شراء|اشتراء|اريد.*شراء|كيف.*اشتري|الحصول.*النسخه|اريد.*النسخه/.test(q)) return "buy";
    if (/واتساب|واتس|رقم.*تواصل|تواصل.*معكم|رقم.*الشراء|رقم.*الهاتف/.test(q)) return "whatsapp";
    if (/محتوي|تحتوي|الموسوعه.*عن|ماذا.*في|ماذا.*تتناول|المواضيع|موضوعات|موضوع.*الموسوعه/.test(q)) return "content";
    if (/استفيد|الفائده|فائده|ماذا.*استفيد|ماذا.*ساحصل|لماذا.*اشتري|هل.*تستحق|تستحق.*الشراء/.test(q)) return "benefit";
    if (/مرحبا|اهلا|السلام عليكم|سلام|هاي|hello|hi/.test(q)) return "welcome";
    return "unknown";
  }

  function respondToUser(text) {
    let reply;
    switch (detectIntent(text)) {
      case "student":
        reply = `مرحبًا بك. للطلاب خصم ثابت 30%، والسعر الخاص ${studentPrice} ${currency}.\n\nيشترط إبراز بطاقة طالب أو بطاقة جامعية.\n\nهل تريد معرفة طريقة الحصول على النسخة؟`;
        break;
      case "reservation":
        reply = `عرض أسبقية الحجز متاح لأول 200 نسخة بخصم 25%.\n\nالسعر بعد الخصم: ${reservationOffer} ${currency}.\nالسعر الأساسي: ${price} ${currency}.\n\nهل تريد الانتقال إلى خطوات الشراء؟`;
        break;
      case "price":
        reply = `السعر الأساسي للنسخة الكاملة: ${price} ${currency}.\n\nوالعرض الحالي لأسبقية الحجز لأول 200 نسخة هو ${reservationOffer} ${currency}.\n\nأما الطلاب فلهم سعر ${studentPrice} ${currency} بعد خصم 30% مع إبراز بطاقة طالب أو بطاقة جامعية.\n\nهل تريد معرفة العرض الأنسب لك؟`;
        break;
      case "pages":
        reply = `النسخة الكاملة من موسوعة محمد مصطفى بابكر تحتوي على ${totalPages} صفحة، مع عينة معاينة مجانية من ${previewPages} صفحة.\n\nهل تريد فتح المعاينة الآن؟`;
        break;
      case "preview":
        reply = `يمكنك معاينة ${previewPages} صفحة مجانًا قبل اتخاذ قرار الشراء.\n\nإذا أعجبتك طريقة العرض والمحتوى، يمكنك الاستفادة من عرض أسبقية الحجز لأول 200 نسخة بسعر ${reservationOffer} ${currency}.\n\nهل تريد أن أفتح لك المعاينة؟`;
        break;
      case "content":
        reply = `الموسوعة العربية الشاملة في الذكاء الاصطناعي باللغة العربية، من تأليف محمد مصطفى بابكر، تنتقل من الأساسيات إلى موضوعات مثل نماذج اللغة الكبيرة LLMs، هندسة الأوامر، RAG، الوكلاء والذكاء متعدد الوسائط.\n\nيمكنك البدء من العينة المجانية ثم التوسع في النسخة الكاملة.\n\nما المجال الذي تريد أن تتعلمه أولًا؟`;
        break;
      case "benefit":
        reply = `الفائدة ليست في عدد الصفحات فقط. النسخة الكاملة تقدم مسارًا منظمًا لفهم مفاهيم الذكاء الاصطناعي وتطبيقاتها، مع محتوى بصري وأمثلة تساعدك على الانتقال من المعرفة إلى التطبيق.\n\nيمكنك تجربة 20 صفحة مجانًا قبل الشراء.\n\nهل ترغب في مشاهدة العينة؟`;
        break;
      case "payment":
        reply = `الدفع داخل السودان عبر بنكك.\n\nرقم الحساب: ${accountNumber}\n\nبعد التحويل أرسل صورة إيصال الدفع عبر واتساب أعمال المنصة: ${whatsapp}\n\nهل تريد فتح واتساب لإرسال الإيصال؟`;
        break;
      case "buy":
        reply = `لإتمام الشراء:\n\n1. عرض أسبقية الحجز لأول 200 نسخة: ${reservationOffer} ${currency}.\n2. حوّل المبلغ عبر بنكك إلى الحساب ${accountNumber}.\n3. أرسل صورة إيصال الدفع عبر واتساب أعمال ${whatsapp}.\n4. يتم استكمال إجراءات تسليم النسخة وتفعيل الخدمة.\n\nإذا كنت طالبًا، أخبرني قبل التحويل للحصول على سعر الطلاب ${studentPrice} ${currency} بعد إثبات صفة الطالب.`;
        break;
      case "whatsapp":
        reply = `واتساب أعمال المنصة: ${whatsapp}\n\nيمكنك استخدامه لإرسال إيصال الدفع والتواصل بشأن الشراء والخدمات.\n\nهل تريد فتح واتساب الآن؟`;
        break;
      case "welcome":
        reply = "أهلًا بك. أنا هنا لمساعدتك في اختيار المسار المناسب.\n\nهل تريد المعاينة، معرفة العرض، أم شرح موضوع من موضوعات الذكاء الاصطناعي؟";
        break;
      default:
        reply = `أستطيع مساعدتك في الموسوعة والورش والشراء.\n\nجرّب السؤال عن السعر، عرض أول 200 نسخة، خصم الطلاب، المعاينة، المحتوى، طريقة الدفع أو واتساب.\n\nوإذا أردت شرح مفهوم في الذكاء الاصطناعي، اكتب اسمه وسأبدأ معك.`;
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
    const safePage = Math.min(Math.max(Number(pageNumber) || 1, 1), previewPages);
    const separator = previewPdf.includes("#") ? "&" : "#";
    window.open(`${previewPdf}${separator}page=${safePage}`, "_blank", "noopener,noreferrer");
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
    img.addEventListener("error", () => { img.hidden = true; fallback.hidden = false; });
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
    for (let pageNumber = 1; pageNumber <= previewPages; pageNumber += 1) previewGrid.appendChild(createPreviewCard(pageNumber));
  }

  const agentOverlay = document.getElementById("agentOverlay");
  const agentForm = document.getElementById("agentForm");
  const agentInput = document.getElementById("agentInput");
  const agentLog = document.getElementById("agentLog");
  const agentStatus = document.getElementById("agentStatus");

  function logAgent(text, type = "") {
    if (!agentLog) return;
    const item = document.createElement("div");
    item.className = `agent-item ${type}`;
    item.textContent = text;
    agentLog.appendChild(item);
    agentLog.scrollTop = agentLog.scrollHeight;
  }
  function setAgentStatus(text) { if (agentStatus) agentStatus.textContent = text; }
  function openAgent() {
    if (!agentOverlay) return;
    agentOverlay.classList.add("show");
    agentOverlay.setAttribute("aria-hidden", "false");
    if (agentLog && agentLog.children.length === 0) {
      logAgent("مرحبًا. معك الوكيل المدير لمنصة التعليم الرقمي والموسوعات الذكية، تحت إشراف وتأليف محمد مصطفى بابكر.");
      logAgent("أعطني هدفك، وسأوجهك إلى المعاينة أو الشرح أو الشراء أو التواصل.");
    }
    window.setTimeout(() => agentInput?.focus(), 150);
  }
  function closeAgent() {
    if (!agentOverlay) return;
    agentOverlay.classList.remove("show");
    agentOverlay.setAttribute("aria-hidden", "true");
  }
  document.querySelectorAll("[data-open-agent]").forEach((button) => button.addEventListener("click", openAgent));
  document.querySelectorAll("[data-close-agent]").forEach((button) => button.addEventListener("click", closeAgent));
  agentOverlay?.addEventListener("click", (event) => { if (event.target === agentOverlay) closeAgent(); });
  document.addEventListener("keydown", (event) => { if (event.key === "Escape") { closeChat(); closeAgent(); } });

  function agentIntent(text) {
    const q = normalize(text);
    const pageMatch = q.match(/(?:صفح|صفحه)\s*(?:رقم\s*)?(\d{1,3})/);
    if (/واتساب|واتس|تواصل/.test(q)) return {type:"whatsapp"};
    if (/طالب|طلاب|خصم.*طالب/.test(q)) return {type:"student"};
    if (/شراء|اشتري|دفع|بنكك|حساب|السعر|عرض/.test(q)) return {type:"buy"};
    if (pageMatch) return {type:"page", page:Number(pageMatch[1])};
    if (/معاين|المعاينه|افتح.*معاين|الصفحات/.test(q)) return {type:"preview"};
    if (/بداي|ابدأ|ابدا|من اين|اقترح.*بدا|محتوي|موضوع/.test(q)) return {type:"content"};
    return {type:"unknown"};
  }

  function executeAgent(taskText) {
    const task = agentIntent(taskText);
    setAgentStatus("جارٍ تنفيذ المهمة...");
    logAgent(`طلب المستخدم: ${taskText}`, "action");
    switch (task.type) {
      case "preview":
        document.getElementById("preview")?.scrollIntoView({behavior:"smooth", block:"start"});
        logAgent(`تم فتح قسم المعاينة. لديك ${previewPages} صفحة مجانية.`, "done");
        break;
      case "page":
        if (task.page < 1 || task.page > previewPages) logAgent(`الصفحة ${task.page} خارج نطاق المعاينة. المتاح من 1 إلى ${previewPages}.`, "done");
        else { document.getElementById("preview")?.scrollIntoView({behavior:"smooth", block:"start"}); window.setTimeout(() => openPdfPage(task.page), 450); logAgent(`تم تجهيز فتح الصفحة ${task.page}.`, "done"); }
        break;
      case "content":
        document.getElementById("features")?.scrollIntoView({behavior:"smooth", block:"start"});
        logAgent("ابدأ بالمعاينة، ثم انتقل إلى أساسيات الذكاء الاصطناعي، ثم RAG والوكلاء. ويمكنني شرح أي مفهوم تطلبه.", "done");
        break;
      case "student":
        document.getElementById("offerTitle")?.scrollIntoView({behavior:"smooth", block:"center"});
        logAgent(`سعر الطلاب ${studentPrice} ${currency} بعد خصم 30% مع إبراز بطاقة طالب أو بطاقة جامعية.`, "done");
        break;
      case "buy":
        document.getElementById("offerTitle")?.scrollIntoView({behavior:"smooth", block:"center"});
        logAgent(`العرض الأول ${reservationOffer} ${currency} لأول 200 نسخة. الدفع عبر بنكك، الحساب ${accountNumber}. إرسال الإيصال عبر واتساب ${whatsapp}.`, "done");
        break;
      case "whatsapp":
        logAgent(`جارٍ فتح واتساب أعمال ${whatsapp}...`, "action");
        window.open(whatsappUrl, "_blank", "noopener,noreferrer");
        logAgent("تم فتح قناة التواصل.", "done");
        break;
      default:
        logAgent("جرّب: افتح المعاينة، افتح صفحة 5، ساعدني في الشراء، سعر الطلاب، أو افتح واتساب.", "done");
    }
    window.setTimeout(() => setAgentStatus("اكتملت المهمة. أنا جاهز لطلب جديد."), 400);
  }

  agentForm?.addEventListener("submit", (event) => {
    event.preventDefault();
    const text = agentInput?.value.trim();
    if (!text) return;
    agentInput.value = "";
    executeAgent(text);
  });
  document.querySelectorAll("[data-agent-task]").forEach((button) => {
    button.addEventListener("click", () => {
      const task = button.getAttribute("data-agent-task");
      const presets = {preview:"افتح المعاينة", content:"اقترح لي البداية", buy:"ساعدني في الشراء", whatsapp:"افتح واتساب"};
      executeAgent(presets[task] || task || "");
    });
  });
});
