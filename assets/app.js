const C = window.STORE_CONFIG || {};

const paymentInit = () => {
  const acct = document.querySelector('#accountNumber');
  if (acct) acct.textContent = C.accountNumber || '';

  const wa = document.querySelector('#whatsappNumber');
  if (wa) wa.textContent = C.whatsapp || '';

  const wl = document.querySelector('#whatsappLink');
  if (wl) wl.href = C.whatsappUrl || C.purchaseUrl || '#';
};

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

const grid = document.querySelector('#previewGrid');

if (grid) {
  previews.forEach((p, i) => {
    const el = document.createElement('article');

    el.className = 'preview-card';

    el.innerHTML = `
      <button
        class="page"
        data-img="assets/preview/p-${p[0]}.jpg"
        data-title="${p[1]}"
        type="button"
      >
        <img
          loading="lazy"
          src="assets/preview/p-${p[0]}.jpg"
          alt="صفحة معاينة: ${p[1]}"
        >
        <span>صفحة ${i + 1}</span>
      </button>

      <h3>${p[1]}</h3>
      <small>معاينة مجانية</small>
    `;

    grid.appendChild(el);
  });
}

paymentInit();

const price = document.querySelector('#priceValue');

if (price) {
  price.textContent = C.price
    ? `${C.price} ${C.currency || ''}`
    : '—';
}

const offerTitle = document.querySelector('#offerTitle');

if (offerTitle) {
  offerTitle.textContent =
    C.offerTitle || 'احصل على النسخة الكاملة';
}

const offerText = document.querySelector('#offerText');

if (offerText) {
  offerText.textContent = C.offerText || '';
}

const oldPrice = document.querySelector('#oldPrice');

if (oldPrice && C.oldPrice) {
  oldPrice.textContent =
    `بدلًا من ${C.oldPrice} ${C.currency || ''}`;
}


/* =========================
   CHAT BOT
   ========================= */

const overlay = document.querySelector('#chatOverlay');
const messages = document.querySelector('#messages');


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
      'مرحبًا! أنا مساعد منصة الموسوعات الذكية. ' +
      'اسألني عن محتوى الموسوعة أو السعر أو طريقة الشراء.'
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


document
  .querySelectorAll('[data-open-chat]')
  .forEach(button => {

    button.addEventListener(
      'click',
      openChat
    );

  });


const closeBtn =
  document.querySelector('[data-close-chat]');

if (closeBtn) {

  closeBtn.addEventListener(
    'click',
    closeChat
  );

}


function addBot(text) {

  if (!messages) {
    return;
  }

  const div =
    document.createElement('div');

  div.className =
    'msg botmsg';

  div.textContent =
    text;

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

  div.className =
    'msg usermsg';

  div.textContent =
    text;

  messages.appendChild(div);

  messages.scrollTop =
    messages.scrollHeight;
}


/* =========================
   BOT KNOWLEDGE
   ========================= */

function answer(question) {

  const q =
    question
      .toLowerCase()
      .trim();


  if (
    q.includes('سعر') ||
    q.includes('كم') ||
    q.includes('السعر') ||
    q.includes('التكلفة')
  ) {

    return `
السعر الحالي للموسوعة هو:

${C.price || '150'} ${C.currency || 'جنيه سوداني'}.

${C.offerText || ''}
    `.trim();

  }


  if (
    q.includes('شراء') ||
    q.includes('اشتر') ||
    q.includes('احصل') ||
    q.includes('النسخة الكاملة')
  ) {

    return `
يمكنك الحصول على النسخة الكاملة من الموسوعة.

النسخة الكاملة تحتوي على ${
      C.pages || '250'
    } صفحة.

المعاينة المجانية تحتوي على ${
      C.previewPages || '20'
    } صفحة.

للدفع داخل السودان:
بنكك

رقم الحساب:
${C.accountNumber || '1882224'}

وبعد الدفع يمكنك إرسال إشعار الدفع عبر واتساب:
${C.whatsapp || '+249914488188'}
    `.trim();

  }


  if (
    q.includes('دفع') ||
    q.includes('بنكك') ||
    q.includes('حساب') ||
    q.includes('تحويل')
  ) {

    return `
طريقة الدفع الحالية داخل السودان:

بنكك

رقم الحساب:
${C.accountNumber || '1882224'}

بعد التحويل أرسل إشعار الدفع عبر واتساب:

${C.whatsapp || '+249914488188'}
    `.trim();

  }


  if (
    q.includes('صفحة') ||
    q.includes('250') ||
    q.includes('20') ||
    q.includes('معاينة')
  ) {

    return `
المعاينة المجانية تحتوي على ${
      C.previewPages || '20'
    } صفحة.

النسخة الكاملة تحتوي على ${
      C.pages || '250'
    } صفحة.

يمكنك تصفح المعاينة قبل اتخاذ قرار الشراء.
    `.trim();

  }


  if (
    q.includes('محتوى') ||
    q.includes('تحتوي') ||
    q.includes('موضوع') ||
    q.includes('ماذا أتعلم') ||
    q.includes('ماذا اتعلم')
  ) {

    return `
الموسوعة تتناول مجموعة واسعة من موضوعات الذكاء الاصطناعي، ومنها:

• أساسيات الذكاء الاصطناعي
• التعلم الآلي
• التعلم العميق
• الشبكات العصبية
• نماذج اللغة الكبيرة LLMs
• الذكاء الاصطناعي التوليدي
• هندسة الأوامر Prompt Engineering
• RAG
• الوكلاء الذكيون
• أدوات الذكاء الاصطناعي
• الذكاء الاصطناعي متعدد الوسائط

وغيرها من الموضوعات.
    `.trim();

  }


  if (
    q.includes('ذكاء اصطناعي') ||
    q.includes('ai') ||
    q.includes('artificial intelligence')
  ) {

    return `
الموسوعة الشاملة في الذكاء الاصطناعي هي مرجع عربي منظم يشرح مفاهيم وتقنيات الذكاء الاصطناعي بصورة متدرجة، من الأساسيات إلى التقنيات الحديثة.
    `.trim();

  }


  if (
    q.includes('واتساب') ||
    q.includes('واتس')
  ) {

    return `
يمكنك إرسال إشعار الدفع عبر واتساب على الرقم:

${C.whatsapp || '+249914488188'}
    `.trim();

  }


  return `
مرحبًا بك في منصة الموسوعات الذكية.

أستطيع مساعدتك في معرفة:

• محتوى الموسوعة
• عدد الصفحات
• السعر
• طريقة الدفع
• بيانات بنكك
• طريقة الحصول على النسخة الكاملة
• الفرق بين المعاينة والنسخة الكاملة

جرّب مثلًا أن تسأل:
"كم سعر الموسوعة؟"
أو:
"كيف أدفع؟"
أو:
"ماذا تحتوي الموسوعة؟"
  `.trim();

}


/* =========================
   SEND MESSAGE
   ========================= */

function sendMessage(text) {

  if (!text || !text.trim()) {
    return;
  }

  addUser(text);

  setTimeout(() => {

    addBot(
      answer(text)
    );

  }, 250);

}


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


/* =========================
   QUICK QUESTIONS
   ========================= */

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
   PREVIEW LIGHTBOX
   ========================= */

document
  .querySelectorAll('.page')
  .forEach(button => {

    button.addEventListener(
      'click',
      () => {

        const image =
          button.dataset.img;

        const title =
          button.dataset.title || 'معاينة';


        const box =
          document.createElement('div');

        box.className =
          'lightbox';


        box.innerHTML = `
          <button
            type="button"
            aria-label="إغلاق"
          >
            ×
          </button>

          <img
            src="${image}"
            alt="${title}"
          >

          <div>
            ${title}
          </div>
        `;


        document.body.appendChild(box);


        box.addEventListener(
          'click',
          event => {

            if (
              event.target === box ||
              event.target.tagName === 'BUTTON'
            ) {

              box.remove();

            }

          }
        );

      }
    );

  });const C = window.STORE_CONFIG || {};

const paymentInit = () => {
  const acct = document.querySelector('#accountNumber');
  if (acct) acct.textContent = C.accountNumber || '';

  const wa = document.querySelector('#whatsappNumber');
  if (wa) wa.textContent = C.whatsapp || '';

  const wl = document.querySelector('#whatsappLink');
  if (wl) wl.href = C.whatsappUrl || C.purchaseUrl || '#';
};

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

const grid = document.querySelector('#previewGrid');

if (grid) {
  previews.forEach((p, i) => {
    const el = document.createElement('article');

    el.className = 'preview-card';

    el.innerHTML = `
      <button
        class="page"
        data-img="assets/preview/p-${p[0]}.jpg"
        data-title="${p[1]}"
        type="button"
      >
        <img
          loading="lazy"
          src="assets/preview/p-${p[0]}.jpg"
          alt="صفحة معاينة: ${p[1]}"
        >
        <span>صفحة ${i + 1}</span>
      </button>

      <h3>${p[1]}</h3>
      <small>معاينة مجانية</small>
    `;

    grid.appendChild(el);
  });
}

paymentInit();

const price = document.querySelector('#priceValue');

if (price) {
  price.textContent = C.price
    ? `${C.price} ${C.currency || ''}`
    : '—';
}

const offerTitle = document.querySelector('#offerTitle');

if (offerTitle) {
  offerTitle.textContent =
    C.offerTitle || 'احصل على النسخة الكاملة';
}

const offerText = document.querySelector('#offerText');

if (offerText) {
  offerText.textContent = C.offerText || '';
}

const oldPrice = document.querySelector('#oldPrice');

if (oldPrice && C.oldPrice) {
  oldPrice.textContent =
    `بدلًا من ${C.oldPrice} ${C.currency || ''}`;
}


/* =========================
   CHAT BOT
   ========================= */

const overlay = document.querySelector('#chatOverlay');
const messages = document.querySelector('#messages');


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
      'مرحبًا! أنا مساعد منصة الموسوعات الذكية. ' +
      'اسألني عن محتوى الموسوعة أو السعر أو طريقة الشراء.'
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


document
  .querySelectorAll('[data-open-chat]')
  .forEach(button => {

    button.addEventListener(
      'click',
      openChat
    );

  });


const closeBtn =
  document.querySelector('[data-close-chat]');

if (closeBtn) {

  closeBtn.addEventListener(
    'click',
    closeChat
  );

}


function addBot(text) {

  if (!messages) {
    return;
  }

  const div =
    document.createElement('div');

  div.className =
    'msg botmsg';

  div.textContent =
    text;

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

  div.className =
    'msg usermsg';

  div.textContent =
    text;

  messages.appendChild(div);

  messages.scrollTop =
    messages.scrollHeight;
}


/* =========================
   BOT KNOWLEDGE
   ========================= */

function answer(question) {

  const q =
    question
      .toLowerCase()
      .trim();


  if (
    q.includes('سعر') ||
    q.includes('كم') ||
    q.includes('السعر') ||
    q.includes('التكلفة')
  ) {

    return `
السعر الحالي للموسوعة هو:

${C.price || '150'} ${C.currency || 'جنيه سوداني'}.

${C.offerText || ''}
    `.trim();

  }


  if (
    q.includes('شراء') ||
    q.includes('اشتر') ||
    q.includes('احصل') ||
    q.includes('النسخة الكاملة')
  ) {

    return `
يمكنك الحصول على النسخة الكاملة من الموسوعة.

النسخة الكاملة تحتوي على ${
      C.pages || '250'
    } صفحة.

المعاينة المجانية تحتوي على ${
      C.previewPages || '20'
    } صفحة.

للدفع داخل السودان:
بنكك

رقم الحساب:
${C.accountNumber || '1882224'}

وبعد الدفع يمكنك إرسال إشعار الدفع عبر واتساب:
${C.whatsapp || '+249914488188'}
    `.trim();

  }


  if (
    q.includes('دفع') ||
    q.includes('بنكك') ||
    q.includes('حساب') ||
    q.includes('تحويل')
  ) {

    return `
طريقة الدفع الحالية داخل السودان:

بنكك

رقم الحساب:
${C.accountNumber || '1882224'}

بعد التحويل أرسل إشعار الدفع عبر واتساب:

${C.whatsapp || '+249914488188'}
    `.trim();

  }


  if (
    q.includes('صفحة') ||
    q.includes('250') ||
    q.includes('20') ||
    q.includes('معاينة')
  ) {

    return `
المعاينة المجانية تحتوي على ${
      C.previewPages || '20'
    } صفحة.

النسخة الكاملة تحتوي على ${
      C.pages || '250'
    } صفحة.

يمكنك تصفح المعاينة قبل اتخاذ قرار الشراء.
    `.trim();

  }


  if (
    q.includes('محتوى') ||
    q.includes('تحتوي') ||
    q.includes('موضوع') ||
    q.includes('ماذا أتعلم') ||
    q.includes('ماذا اتعلم')
  ) {

    return `
الموسوعة تتناول مجموعة واسعة من موضوعات الذكاء الاصطناعي، ومنها:

• أساسيات الذكاء الاصطناعي
• التعلم الآلي
• التعلم العميق
• الشبكات العصبية
• نماذج اللغة الكبيرة LLMs
• الذكاء الاصطناعي التوليدي
• هندسة الأوامر Prompt Engineering
• RAG
• الوكلاء الذكيون
• أدوات الذكاء الاصطناعي
• الذكاء الاصطناعي متعدد الوسائط

وغيرها من الموضوعات.
    `.trim();

  }


  if (
    q.includes('ذكاء اصطناعي') ||
    q.includes('ai') ||
    q.includes('artificial intelligence')
  ) {

    return `
الموسوعة الشاملة في الذكاء الاصطناعي هي مرجع عربي منظم يشرح مفاهيم وتقنيات الذكاء الاصطناعي بصورة متدرجة، من الأساسيات إلى التقنيات الحديثة.
    `.trim();

  }


  if (
    q.includes('واتساب') ||
    q.includes('واتس')
  ) {

    return `
يمكنك إرسال إشعار الدفع عبر واتساب على الرقم:

${C.whatsapp || '+249914488188'}
    `.trim();

  }


  return `
مرحبًا بك في منصة الموسوعات الذكية.

أستطيع مساعدتك في معرفة:

• محتوى الموسوعة
• عدد الصفحات
• السعر
• طريقة الدفع
• بيانات بنكك
• طريقة الحصول على النسخة الكاملة
• الفرق بين المعاينة والنسخة الكاملة

جرّب مثلًا أن تسأل:
"كم سعر الموسوعة؟"
أو:
"كيف أدفع؟"
أو:
"ماذا تحتوي الموسوعة؟"
  `.trim();

}


/* =========================
   SEND MESSAGE
   ========================= */

function sendMessage(text) {

  if (!text || !text.trim()) {
    return;
  }

  addUser(text);

  setTimeout(() => {

    addBot(
      answer(text)
    );

  }, 250);

}


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


/* =========================
   QUICK QUESTIONS
   ========================= */

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
   PREVIEW LIGHTBOX
   ========================= */

document
  .querySelectorAll('.page')
  .forEach(button => {

    button.addEventListener(
      'click',
      () => {

        const image =
          button.dataset.img;

        const title =
          button.dataset.title || 'معاينة';


        const box =
          document.createElement('div');

        box.className =
          'lightbox';


        box.innerHTML = `
          <button
            type="button"
            aria-label="إغلاق"
          >
            ×
          </button>

          <img
            src="${image}"
            alt="${title}"
          >

          <div>
            ${title}
          </div>
        `;


        document.body.appendChild(box);


        box.addEventListener(
          'click',
          event => {

            if (
              event.target === box ||
              event.target.tagName === 'BUTTON'
            ) {

              box.remove();

            }

          }
        );

      }
    );

  });
