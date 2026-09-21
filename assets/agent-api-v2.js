(function () {
  'use strict';

  function init() {
    if (window.__SMART_AGENT_V4) return;
    window.__SMART_AGENT_V4 = true;

    const C = window.STORE_CONFIG || {};
    const api = String(C.agentApiUrl || '').trim();
    const form = document.getElementById('agentForm');
    const input = document.getElementById('agentInput');
    const log = document.getElementById('agentLog');
    const status = document.getElementById('agentStatus');
    if (!form || !input || !log) return;

    const state = {
      stage: 'discover',
      audience: '',
      goal: '',
      objection: '',
      intent: 'unknown',
      quantity: null,
      currency: '',
      history: [],
      previewShown: false
    };

    const price = {
      sdg: Number(C.reservationOffer || 120000),
      sdgRegular: Number(C.price || 150000),
      usd: Number(C.foreignCurrencyDiscountedPrice || 16),
      usdRegular: Number(C.foreignCurrencyPrice || 19)
    };

    const norm = s => String(s || '').trim().toLowerCase()
      .replace(/[أإآ]/g, 'ا').replace(/ة/g, 'ه');

    function add(text, type) {
      const d = document.createElement('div');
      d.className = 'agent-item ' + (type || '');
      d.textContent = text;
      log.appendChild(d);
      log.scrollTop = log.scrollHeight;
    }

    function setStatus(t) { if (status) status.textContent = t; }

    function action(a) {
      if (!a || !a.type) return;
      if (a.type === 'open_preview') {
        state.previewShown = true;
        const el = document.getElementById('preview');
        if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
        else window.location.href = C.previewUrl || 'preview.html';
      } else if (a.type === 'open_preview_page') {
        const p = Number(a.page);
        if (p >= 1 && p <= 20) window.location.href = (C.previewUrl || 'preview.html') + '#' + p;
      } else if (a.type === 'open_whatsapp') {
        window.location.href = C.whatsappUrl || 'https://wa.me/249121851285';
      } else if (a.type === 'focus_offer') {
        document.querySelector('.offer')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      } else if (a.type === 'open_checkout') {
        window.location.href = C.purchaseUrl || 'checkout.html';
      }
    }

    function digits(s) {
      return String(s || '').replace(/[٠-٩]/g, d => String('٠١٢٣٤٥٦٧٨٩'.indexOf(d)));
    }

    function getQuantity(q) {
      const x = digits(q).trim();
      const m = x.match(/(?:^|\s)(\d{1,4})\s*(?:نسخه|نسخ|copy|copies|طبعه|كتب)?(?:\s|$)/i);
      if (m) return Math.max(1, Math.min(2000, Number(m[1])));
      const words = {'واحد':1,'واحده':1,'اثنان':2,'اثنين':2,'ثلاث':3,'ثلاثه':3,'اربعه':4,'اربع':4,'خمسه':5,'خمس':5,'سته':6,'ست':6,'سبعه':7,'سبع':7,'ثمانيه':8,'ثمان':8,'تسعه':9,'تسع':9,'عشره':10,'عشر':10};
      const w = x.replace(/نسخه|نسخ|كتاب|كتب|copy|copies/g,'').trim();
      return words[w] || null;
    }

    function getCurrency(q) {
      if (/(دولار|usd|\$)/.test(q)) return 'USD';
      if (/(جنيه|سوداني|sdg)/.test(q)) return 'SDG';
      return '';
    }

    function totalFor(qty, cur) {
      const n = Math.max(1, Number(qty || 1));
      if (cur === 'USD') {
        const offer = Math.min(n, 200), regular = Math.max(0, n - 200);
        return { total: offer * price.usd + regular * price.usdRegular, saving: offer * (price.usdRegular - price.usd), offer, regular };
      }
      const offer = Math.min(n, 200), regular = Math.max(0, n - 200);
      return { total: offer * price.sdg + regular * price.sdgRegular, saving: offer * (price.sdgRegular - price.sdg), offer, regular };
    }

    function closeQuestion(text) {
      const q = norm(text);
      if (state.intent === 'purchase' && !state.quantity) {
        const qty = getQuantity(q);
        if (qty) { state.quantity = qty; state.stage = 'currency'; return null; }
        return 'ممتاز 🌟 لنكمل طلبك. كم نسخة تريد: نسخة واحدة أم أكثر؟';
      }
      if (state.intent === 'purchase' && state.quantity && !state.currency) {
        const cur = getCurrency(q);
        if (cur) { state.currency = cur; state.stage = 'ready'; return null; }
        return 'تمام 👍 بقيت خطوة واحدة: تفضل الدفع بالجنيه السوداني أم بالدولار؟';
      }
      return null;
    }

    function salesReply(text) {
      const q = norm(text);
      if (!q) return null;

      const qty = getQuantity(q);
      const cur = getCurrency(q);

      if (/(السلام عليكم|مرحبا|اهلا|هاي|hello|hi)/.test(q) && state.history.length === 0) {
        state.stage = 'discover';
        return { reply: 'أهلاً وسهلاً بك 🌷 أنا هنا لأساعدك في معرفة ما إذا كانت الموسوعة مناسبة لك، وليس فقط لأعطيك قائمة مواصفات. ما الذي تبحث عنه تحديداً: تبدأ من الصفر، تطور مهاراتك، أم تبحث عن مرجع شامل للذكاء الاصطناعي؟', actions: [] };
      }

      if (/(مبتدئ|من الصفر|ما بعرف|لا اعرف|جديد في الذكاء|بدايه)/.test(q)) {
        state.audience = 'beginner'; state.goal = 'learn';
        return { reply: 'ممتاز 👍 إذا كنت تبدأ من الصفر، فالأهم أن يكون لديك مسار مرتب بدل التنقل بين عشرات المصادر. الموسوعة مصممة كتدرج من الأساسيات إلى الموضوعات والتطبيقات الحديثة، والنسخة الكاملة معها وكيل ذكي يساعدك في الشرح. قبل أن تدفع، دعني أريك 20 صفحة حقيقية من المحتوى وتحكم بنفسك.', actions: [{ type: 'open_preview' }] };
      }

      if (/(طالب|طالبه|جامعه|جامعي|دراسه)/.test(q)) {
        state.audience = 'student'; state.goal = 'study';
        if (/(السعر|كم|بكم|الثمن|تكلفه|الدفع)/.test(q)) {
          return { reply: 'ممتاز 🎓 للطالب: السعر الخاص بالطالب هو 120,000 جنيه سوداني أو 16 دولاراً، بعد إثبات صفة الطالب. هذا السعر ثابت للطالب ولا يتجمع مع عرض أول 200 نسخة. ويمكنك أولاً مشاهدة المعاينة المجانية قبل اتخاذ القرار.', actions: [{ type: 'open_preview' }] };
        }
        return { reply: 'ممتاز 🎓 للطالب، القيمة الأساسية هي وجود مرجع عربي منظم يمكن الرجوع إليه أثناء التعلم بدلاً من جمع المعلومات من مصادر متفرقة. وإذا كانت ميزانيتك محدودة، لا أريدك أن تشتري قبل أن ترى المستوى بنفسك؛ ابدأ بالمعاينة المجانية.', actions: [{ type: 'open_preview' }] };
      }

      if (/(معلم|مدرس|استاذ|تدريس|مدرب)/.test(q)) {
        state.audience = 'teacher'; state.goal = 'teaching';
        return { reply: 'ممتاز 👨‍🏫 إذا كنت معلماً أو مدرباً، فالموسوعة يمكن أن تكون مرجعاً منظماً لبناء فهمك للموضوعات وشرحها للمتعلمين. لن أفترض أنها تناسب أسلوب تدريس كل شخص؛ شاهد المعاينة أولاً، وإذا رأيت أن التنظيم والمحتوى يخدمانك ننتقل للشراء.', actions: [{ type: 'open_preview' }] };
      }

      if (/(عندي chatgpt|عندي شات|chatgpt|شات جي بي تي|موجود عندي ذكاء اصطناعي)/.test(q)) {
        state.objection = 'existing_ai';
        return { reply: 'وهذا طبيعي جداً 👍 ChatGPT وأدوات الذكاء الاصطناعي ممتازة في الإجابة والتفاعل، لكن السؤال هنا مختلف: هل تريد أداة تسألها عند الحاجة فقط، أم تريد مرجعاً عربياً منظماً ترجع إليه وتتعلم منه؟ الموسوعة تحاول أن تجمع المعرفة في مسار واحد، ومع النسخة الكاملة يوجد وكيل مساعد. والأفضل ألا تأخذ كلامي كدليل؛ شاهد المعاينة أولاً.', actions: [{ type: 'open_preview' }] };
      }

      if (/(يوتيوب|مجانا|مجاني|مجانيه|موجوده في الانترنت|المعلومات موجوده)/.test(q)) {
        state.objection = 'free_content';
        return { reply: 'صحيح، توجد معلومات كثيرة مجانية على الإنترنت 👍 والقيمة هنا ليست الادعاء بأن المعلومات غير موجودة، بل في تنظيمها في مرجع عربي واحد يمكن الرجوع إليه، مع أدوات ومساعدة مرتبطة بالنسخة الكاملة. لذلك أفضل طريقة لاتخاذ القرار هي تجربة المعاينة المجانية بدلاً من الاعتماد على الوصف فقط.', actions: [{ type: 'open_preview' }] };
      }

      if (/(غالي|غاليه|مكلف|ما عندي قروش|السعر كبير|ما عندي المبلغ)/.test(q)) {
        state.objection = 'price'; state.stage = 'objection';
        return { reply: 'أتفهمك تماماً 🤝 والسعر لا ينبغي أن يكون سبباً لشراء شيء غير مناسب لك. لذلك دعنا نقلل مخاطرة القرار: شاهد أولاً 20 صفحة حقيقية، ثم قيّم هل التنظيم والمحتوى والوكيل المرفق يستحقان المبلغ بالنسبة لك. إذا اقتنعت بعدها، أساعدك في إكمال الشراء.', actions: [{ type: 'open_preview' }] };
      }

      if (/(تغنيني|يغنيني|بديل.*كورس|بديل.*دوره|هل.*الموسوعه.*الكورس|الموسوعه.*بدل.*الكورس)/.test(q)) {
        return { reply: 'يمكن أن تغطي الموسوعة جزءاً كبيراً من التعلم الذاتي، لكنها ليست بديلاً مضموناً عن كل كورس عملي أو كل مدرب. إذا كان هدفك بناء أساس قوي والرجوع إلى مرجع عربي منظم، فهذه إحدى نقاط قوتها. شاهد المعاينة أولاً، وبعدها أساعدك في تحديد هل تناسب احتياجك.', actions: [{ type: 'open_preview' }] };
      }

      if (/(شنو.*الوكيل|ما.*الوكيل|ماذا.*يفعل.*الوكيل|فائده.*الوكيل)/.test(q)) {
        return { reply: 'الوكيل الذكي مرفق مع النسخة الكاملة 🤝 وهو مساعد تعليمي: يشرح المفاهيم، يبسط المصطلحات ويجيب عن الأسئلة. لا أريد أن أقدمه لك كبديل للمعلم أو كشيء سحري؛ قيمته الحقيقية أن يكون مساعداً مع المرجع بعد الشراء.', actions: [] };
      }

      if (/(الموسوعه.*تستحق|تستحق|ليه اشتري|لماذا اشتري|شنو الفائده|فائدتها|ماذا استفيد)/.test(q)) {
        state.intent = 'interest';
        return { reply: 'إذا كان هدفك التعلم بالعربية، فالقيمة ليست في عدد الصفحات وحده؛ بل في أن تجد موضوعات الذكاء الاصطناعي مرتبة في مسار واحد، وتملك مرجعاً ترجع إليه، ومع النسخة الكاملة مساعداً ذكياً للشرح. لكنني أفضل أن أثبت لك ذلك عملياً: هل أفتح لك المعاينة المجانية؟', actions: [{ type: 'open_preview' }] };
      }

      if (/(تجربه|اجرب|اشوف الاول|اشوف قبل|معاينه|20 صفحه|قبل الشراء)/.test(q)) {
        state.stage = 'preview';
        return { reply: 'بكل سرور 👍 شاهد المعاينة الحقيقية أولاً. خذ وقتك في قراءة الصفحات، وإذا وجدت أن المستوى مناسب لك سأساعدك في الخطوة التالية.', actions: [{ type: 'open_preview' }] };
      }

      if (/(السعر|كم|بكم|الثمن|تكلفه|الدفع)/.test(q) && !qty) {
        return { reply: 'النسخة الكاملة 260 صفحة. السعر الأساسي ' + price.sdgRegular.toLocaleString('ar-EG') + ' جنيه سوداني، والعرض الحالي لأول 200 نسخة ' + price.sdg.toLocaleString('ar-EG') + ' جنيه للنسخة. وبالدولار 19 دولاراً، أو 16 دولاراً ضمن العرض. إذا أخبرتني بعدد النسخ أحسب لك الإجمالي مباشرة.', actions: [{ type: 'focus_offer' }] };
      }

      if (/(اريد شراء|عايز اشتري|عايز شراء|جاهز للشراء|اشتري|ساشتري|أريد النسخه|عايز النسخه)/.test(q)) {
        state.intent = 'purchase'; state.stage = 'purchase';
        if (qty) state.quantity = qty;
        if (cur) state.currency = cur;
        const next = closeQuestion(text);
        if (next) return { reply: next, actions: [] };
      }

      if (state.intent === 'purchase') {
        const next = closeQuestion(text);
        if (next) return { reply: next, actions: [] };
        if (state.quantity && state.currency) {
          const t = totalFor(state.quantity, state.currency);
          state.stage = 'ready';
          const currencyText = state.currency === 'USD' ? 'دولار' : 'جنيه سوداني';
          return { reply: 'ممتاز 🌟\n\nالطلب: ' + state.quantity + ' نسخة\nسعر العرض للنسخة: ' + (state.currency === 'USD' ? price.usd : price.sdg).toLocaleString('ar-EG') + ' ' + currencyText + '\nالإجمالي: ' + t.total.toLocaleString('ar-EG') + ' ' + currencyText + (t.saving ? '\nالتوفير مقارنة بالسعر الأساسي: ' + t.saving.toLocaleString('ar-EG') + ' ' + currencyText : '') + '\n\nإذا كنت جاهزاً، أفتح لك صفحة الشراء الآن.', actions: [{ type: 'open_checkout' }] };
        }
      }

      if (qty) {
        state.intent = 'purchase'; state.quantity = qty;
        if (cur) state.currency = cur;
        const next = closeQuestion(text);
        if (next) return { reply: next, actions: [] };
      }

      return null;
    }

    async function cloud(text) {
      if (!api) throw new Error('no agent api');
      const payload = {
        message: text,
        context: {
          page: location.pathname,
          sales_state: state,
          product: {
            title: 'الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية',
            pages: Number(C.pages || 260),
            previewPages: Number(C.previewPages || 20),
            priceSDG: price.sdg,
            priceUSD: price.usd,
            agentIncluded: Boolean(C.agentIncluded)
          },
          conversation: state.history.slice(-8)
        }
      };
      const r = await fetch(api, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(payload) });
      const data = await r.json().catch(() => ({}));
      if (!r.ok) throw new Error(data.message || 'agent unavailable');
      return data;
    }

    async function handle(text) {
      add('طلب المستخدم: ' + text, 'action');
      state.history.push({ role: 'user', text });

      const local = salesReply(text);
      if (local) {
        add(local.reply, 'done');
        state.history.push({ role: 'assistant', text: local.reply });
        (local.actions || []).forEach(action);
        setStatus('جاهز للخطوة التالية.');
        return;
      }

      setStatus('أفكر كموظف مبيعات وأبحث عن أفضل خطوة لك...');
      try {
        const data = await cloud(text);
        const reply = data.reply || 'كيف أستطيع مساعدتك في اختيار الأنسب لك؟';
        add(reply, 'done');
        state.history.push({ role: 'assistant', text: reply });
        (Array.isArray(data.actions) ? data.actions : []).forEach(action);
        setStatus('تمت الإجابة.');
      } catch (e) {
        add('أقدر أساعدك من هنا أيضاً 👍 إذا كان هدفك التعلم، أبدأ معك بالمعاينة المجانية؛ وإذا كنت جاهزاً للشراء أخبرني بعدد النسخ والعملة.', 'done');
        setStatus('الوضع المحلي يعمل.');
      }
    }

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      e.stopImmediatePropagation();
      const text = input.value.trim();
      if (!text) return;
      input.value = '';
      handle(text);
    }, true);

    add('أهلاً بك 🌷 أنا وكيلك الذكي. دعنا نرى أولاً ما الذي تحتاجه، ثم أساعدك في اتخاذ القرار المناسب.', 'done');
    setStatus('جاهز لمساعدتك.');
  }

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init, { once: true });
  else init();
})();