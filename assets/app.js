'use strict';

const C = window.STORE_CONFIG || {};

/* =========================
   إعدادات الدفع والعرض
   ========================= */

function paymentInit() {
  const accountNumber = document.querySelector('#accountNumber');
  const whatsappNumber = document.querySelector('#whatsappNumber');
  const whatsappLink = document.querySelector('#whatsappLink');

  if (accountNumber) {
    accountNumber.textContent = C.accountNumber || '';
  }

  if (whatsappNumber) {
    whatsappNumber.textContent = C.whatsapp || '';
  }

  if (whatsappLink) {
    whatsappLink.href =
      C.whatsappUrl ||
      C.purchaseUrl ||
      '#';
  }
}


/* =========================
   صفحات المعاينة
   ========================= */

const previews = [
  ['020', 'ما هو الذكاء الاصطناعي؟'],
  ['021', 'كيف يفكر الذكاء الاصطناعي؟'],
  ['029', 'التعلم الآلي Machine Learning'],
  ['037', 'كيف يفكر التعلم الآلي؟'],
  ['043', 'تطبيق عملي للتعلم الآلي'],
  ['049', 'الإفراط في التعلم Overfitting'],
  ['050', 'نقص التعلم Underfitting'],
  ['055', 'تقسيم البيانات Data Splitting'],
  ['066', 'الشبكات العصبية الاصطناعية'],
  ['072', 'نماذج اللغة الكبيرة LLMs'],
  ['073', 'الضبط الدقيق Fine-Tuning'],
  ['074', 'التوليد المعزز بالاسترجاع RAG'],
  ['076', 'هندسة الأوامر Prompt Engineering'],
  ['077', 'التحقق من مخرجات الذكاء الاصطناعي'],
  ['080', 'التعلم العميق Deep Learning'],
  ['118', 'أدوات الذكاء الاصطناعي'],
  ['119', 'الذكاء الاصطناعي الوكيلي Agentic AI'],
  ['120', 'العمل مع الوكلاء والأدوات'],
  ['200', 'الذكاء الاصطناعي متعدد الوسائط'],
  ['204', 'توليد النصوص بالذكاء الاصطناعي']
];


function buildPreviewGrid() {
  const grid = document.querySelector('#previewGrid');

  if (!grid) {
    return;
  }

  grid.innerHTML = '';

  previews.forEach((page, index) => {
    const number = page[0];
    const title = page[1];

    const article = document.createElement('article');

    article.className = 'preview-card';

    article.innerHTML = `
      <button
        class="page"
        data-img="assets/preview/p-${number}.jpg"
        data-title="${title}"
        type="button"
        aria-label="فتح صفحة المعاينة ${index + 1}"
      >
        <img
          loading="lazy"
          src="assets/preview/p-${number}.jpg"
          alt="صفحة معاينة: ${title}"
        >

        <span>صفحة ${index + 1}</span>
      </button>

      <h3>${title}</h3>
      <small>معاينة مجانية</small>
    `;

    grid.appendChild(article);
  });
}


/* =========================
   بيانات السعر والعرض
   ========================= */

function initOffer() {
  const price = document.querySelector('#priceValue');

  if (price) {
    price.textContent = C.price
      ? `${C.price} ${C.currency || ''}`
      : '—';
  }

  const offerTitle = document.querySelector('#offerTitle');

  if (offerTitle) {
    offerTitle.textContent =
      C.offerTitle ||
      'احصل على النسخة الكاملة';
  }

  const offerText = document.querySelector('#offerText');

  if (offerText) {
    offerText.textContent =
      C.offerText || '';
  }

  const oldPrice = document.querySelector('#oldPrice');

  if (oldPrice) {
    if (C.oldPrice) {
      oldPrice.textContent =
        `بدلًا من ${C.oldPrice} ${C.currency || ''}`;
    } else {
      oldPrice.textContent = '';
    }
  }
}


/* =========================
   المساعد
   ========================= */

const overlay =
  document.querySelector('#chatOverlay');

const messages =
  document.querySelector('#messages');


function openChat() {
  if (!overlay || !messages) {
    return;
  }

  overlay.classList.add('show');

  overlay.setAttribute(
    'aria-hidden',
    'false'
  );

  if (!messages.children.length) {
    addBot(
      'مرحبًا بك في منصة الموسوعات الذكية. ' +
      'يمكنك سؤالي عن محتوى الموسوعة أو السعر أو طريقة الشراء.'
    );
  }

  const input =
    document.querySelector('#chatInput');

  if (input) {
    input.focus();
  }
}


function closeChat() {
  if (!overlay) {
    return;
  }

  overlay.classList.remove('show');

  overlay.setAttribute(
    'aria-hidden',
    'true'
  );
}


/* فتح المساعد */

document
  .querySelectorAll('[data-open-chat]')
  .forEach(button => {
    button.addEventListener(
      'click',
      openChat
    );
  });


/* إغلاق المساعد */

document
  .querySelectorAll('[data-close-chat]')
  .forEach(button => {
    button.addEventListener(
      'click',
      closeChat
    );
  });


/* إغلاق عند الضغط خارج نافذة المحادثة */

if (overlay) {
  overlay.addEventListener(
    'click',
    event => {
      if (event.target === overlay) {
        closeChat();
      }
    }
  );
}


/* =========================
   رسائل المساعد
   ========================= */

function addBot(text) {
  if (!messages) {
    return;
  }

  const div =
    document.createElement('div');

  div.className = 'msg botmsg';

  div.textContent = text;

  messages.appendChild(div);

  messages.scrollTop =
    messages.scrollHeight;
}


function addUser(text) {
  if (!messages) {
    return;
  }

  const div =
    document.createElement('div');

  div.className = 'msg usermsg';

  div.textContent = text;

  messages.appendChild(div);

  messages.scrollTop =
    messages.scrollHeight;
}


/* =========================
   معرفة المساعد
   ========================= */

function answer(question) {
  const q =
    question
      .toLowerCase()
      .trim();


  /* السعر */

  if (
    q.includes('سعر') ||
    q.includes('السعر') ||
    q.includes('كم') ||
    q.includes('التكلفة') ||
    q.includes('بكم')
  ) {
    return `
السعر الحالي للموسوعة:

${C.price || '150'} ${C.currency || 'جنيه سوداني'}.

${C.offerText || ''}
    `.trim();
  }


  /* الشراء */

  if (
    q.includes('شراء') ||
    q.includes('اشتر') ||
    q.includes('احصل') ||
    q.includes('النسخة الكاملة')
  ) {
    return `
يمكنك الحصول على النسخة الكاملة من الموسوعة.

عدد صفحات النسخة الكاملة:
${C.pages || '250'} صفحة.

عدد صفحات المعاينة:
${C.previewPages || '20'} صفحة.

طريقة الدفع داخل السودان:
بنكك.

رقم الحساب:
${C.accountNumber || '1882224'}.

بعد الدفع أرسل إشعار الدفع عبر واتساب:
${C.whatsapp || '+249914488188'}.
    `.trim();
  }


  /* الدفع */

  if (
    q.includes('دفع') ||
    q.includes('بنكك') ||
    q.includes('حساب') ||
    q.includes('تحويل')
  ) {
    return `
طريقة الدفع الحالية داخل السودان:

بنكك.

رقم الحساب:
${C.accountNumber || '1882224'}.

بعد التحويل أرسل إشعار الدفع عبر واتساب:

${C.whatsapp || '+249914488188'}.
    `.trim();
  }


  /* المعاينة والصفحات */

  if (
    q.includes('صفحة') ||
    q.includes('250') ||
    q.includes('20') ||
    q.includes('معاينة')
  ) {
    return `
المعاينة المجانية تحتوي على:
${C.previewPages || '20'} صفحة.

النسخة الكاملة تحتوي على:
${C.pages || '250'} صفحة.

يمكنك تصفح صفحات المعاينة قبل اتخاذ قرار الشراء.
    `.trim();
  }


  /* محتوى الموسوعة */

  if (
    q.includes('محتوى') ||
    q.includes('تحتوي') ||
    q.includes('موضوع') ||
    q.includes('ماذا أتعلم') ||
    q.includes('ماذا اتعلم')
  ) {
    return `
تتناول الموسوعة موضوعات في الذكاء الاصطناعي، ومنها:

• أساسيات الذكاء الاصطناعي
• التعلم الآلي
• التعلم العميق
• الشبكات العصبية الاصطناعية
• نماذج اللغة الكبيرة LLMs
• الذكاء الاصطناعي التوليدي
• هندسة الأوامر Prompt Engineering
• التوليد المعزز بالاسترجاع RAG
• الوكلاء الأذكياء
• أدوات الذكاء الاصطناعي
• الذكاء الاصطناعي متعدد الوسائط

وغيرها من الموضوعات.
    `.trim();
  }


  /* الذكاء الاصطناعي */

  if (
    q.includes('ذكاء اصطناعي') ||
    q.includes('ai') ||
    q.includes('artificial intelligence')
  ) {
    return `
الموسوعة الشاملة في الذكاء الاصطناعي مرجع عربي منظم يشرح مفاهيم وتقنيات الذكاء الاصطناعي بصورة متدرجة، من الأساسيات إلى الموضوعات الحديثة.
    `.trim();
  }


  /* واتساب */

  if (
    q.includes('واتساب') ||
    q.includes('واتس')
  ) {
    return `
يمكنك إرسال إشعار الدفع عبر واتساب على الرقم:

${C.whatsapp || '+249914488188'}.
    `.trim();
  }


  /* الإجابة الافتراضية */

  return `
مرحبًا بك في منصة الموسوعات الذكية.

يمكنني مساعدتك في معرفة:

• محتوى الموسوعة
• عدد الصفحات
• السعر
• طريقة الدفع
• بيانات بنكك
• طريقة الحصول على النسخة الكاملة
• الفرق بين المعاينة والنسخة الكاملة

يمكنك أن تسأل مثلًا:

كم سعر الموسوعة؟

كيف أدفع؟

ماذا تحتوي الموسوعة؟
  `.trim();
}


/* =========================
   إرسال الرسائل
   ========================= */

function sendMessage(text) {
  if (!text || !text.trim()) {
    return;
  }

  addUser(text);

  setTimeout(() => {
    addBot(answer(text));
  }, 250);
}


/* نموذج المحادثة */

const chatForm =
  document.querySelector('#chatForm');

if (chatForm) {
  chatForm.addEventListener(
    'submit',
    event => {
      event.preventDefault();

      const input =
        document.querySelector('#chatInput');

      if (!input) {
        return;
      }

      const text =
        input.value.trim();

      if (!text) {
        return;
      }

      input.value = '';

      sendMessage(text);
    }
  );
}


/* الأسئلة السريعة */

document
  .querySelectorAll('[data-q]')
  .forEach(button => {
    button.addEventListener(
      'click',
      () => {
        const question =
          button.dataset.q;

        sendMessage(question);
      }
    );
  });


/* =========================
   معاينة الصفحة بالحجم الكبير
   ========================= */

function initLightbox() {
  document
    .querySelectorAll('.page')
    .forEach(button => {

      button.addEventListener(
        'click',
        () => {

          const image =
            button.dataset.img;

          const title =
            button.dataset.title ||
            'معاينة';


          const box =
            document.createElement('div');

          box.className =
            'lightbox';


          const closeButton =
            document.createElement('button');

          closeButton.type =
            'button';

          closeButton.setAttribute(
            'aria-label',
            'إغلاق'
          );

          closeButton.textContent =
            '×';


          const img =
            document.createElement('img');

          img.src = image;

          img.alt = title;


          const caption =
            document.createElement('div');

          caption.textContent =
            title;


          box.appendChild(closeButton);
          box.appendChild(img);
          box.appendChild(caption);

          document.body.appendChild(box);


          box.addEventListener(
            'click',
            event => {

              if (
                event.target === box ||
                event.target === closeButton
              ) {
                box.remove();
              }

            }
          );

        }
      );

    });
}


/* =========================
   تشغيل الموقع
   ========================= */

paymentInit();

initOffer();

buildPreviewGrid();

initLightbox();
