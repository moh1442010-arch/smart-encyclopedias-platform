(function () {
  "use strict";

  function init() {
    if (window.__SMART_AGENT_V3) return;
    window.__SMART_AGENT_V3 = true;

    const C = window.STORE_CONFIG || {};

    const api = String(
      C.agentApiUrl ||
      "https://super-rice-31e6.moh1442010.workers.dev/api/agent"
    ).trim();

    const form = document.getElementById("agentForm");
    const input = document.getElementById("agentInput");
    const log = document.getElementById("agentLog");
    const status = document.getElementById("agentStatus");

    if (!form || !input || !log) return;

    const session = {
      stage: "greeting",
      intent: "",
      audience: "",
      objection: "",
      quantity: 1,
      currency: "",
      lastQuestion: ""
    };

    function add(text, type) {
      const d = document.createElement("div");
      d.className = "agent-item " + (type || "");
      d.textContent = text;
      log.appendChild(d);
      log.scrollTop = log.scrollHeight;
    }

    function setStatus(text) {
      if (status) status.textContent = text;
    }

    function act(a) {
      if (!a || !a.type) return;

      switch (a.type) {
        case "open_preview":
          if (document.getElementById("preview")) {
            document
              .getElementById("preview")
              .scrollIntoView({ behavior: "smooth", block: "start" });
          } else {
            window.location.href = C.previewUrl || "preview.html";
          }
          break;

        case "open_preview_page": {
          const p = Number(a.page);
          if (p >= 1 && p <= 20) {
            window.location.href =
              (C.previewUrl || "preview.html") + "#" + p;
          }
          break;
        }

        case "open_whatsapp":
          window.location.href =
            C.whatsappUrl || "https://wa.me/249121851285";
          break;

        case "focus_offer":
          document.querySelector(".offer")?.scrollIntoView({
            behavior: "smooth",
            block: "center"
          });
          break;

        case "open_checkout":
          window.location.href = C.purchaseUrl || "checkout.html";
          break;
      }
    }

    function priceText() {
      const sdg = Number(C.reservationOffer || 120000);
      const usd = Number(C.foreignCurrencyDiscountedPrice || 16);

      return {
        sdg: sdg,
        usd: usd,
        sdgText: sdg.toLocaleString("ar-EG") + " جنيه سوداني",
        usdText: usd + " دولار"
      };
    }

    function localSales(raw) {
      const q = String(raw || "").trim().toLowerCase();

      if (!q) return null;

      const P = priceText();

      /* شراء */
      if (
        /(أريد شراء|اريد شراء|عايز اشتري|عايز شراء|جاهز للشراء|جاهز.*اشتري|سأشتري|اشتري الآن|اشترى الآن|عايز النسخة|أريد النسخة)/.test(
          q
        )
      ) {
        session.intent = "purchase";
        session.stage = "purchase_details";

        return {
          reply:
            "ممتاز 🌟 يسعدني مساعدتك في إكمال الشراء. قبل أن أوجهك للدفع، فقط أحتاج معلومتين: هل تريد نسخة واحدة أم أكثر؟ وهل تريد الدفع بالجنيه السوداني أم بالدولار؟",
          actions: []
        };
      }

      /* عدد النسخ */
      if (
        session.stage === "purchase_details" &&
        /(نسخة واحدة|نسخة واحد|واحدة|واحد|1 نسخة|نسخه واحده)/.test(q)
      ) {
        session.quantity = 1;
        session.stage = "currency";

        return {
          reply:
            "تمام 👍 نسخة واحدة. الآن اختر العملة: الجنيه السوداني أم الدولار؟",
          actions: []
        };
      }

      const qtyMatch = q.match(/(\d+)\s*(نسخة|نسخ)/);

      if (session.stage === "purchase_details" && qtyMatch) {
        session.quantity = Math.max(1, Number(qtyMatch[1]));
        session.stage = "currency";

        return {
          reply:
            "تمام 👍 سجلت لك " +
            session.quantity +
            " نسخة. هل تريد الحساب بالجنيه السوداني أم بالدولار؟",
          actions: []
        };
      }

      /* العملة */
      if (
        session.stage === "currency" &&
        /(جنيه|جنيه سوداني|سوداني|sdg)/.test(q)
      ) {
        session.currency = "SDG";

        const total = P.sdg * session.quantity;

        session.stage = "ready_to_pay";

        return {
          reply:
            "تمام 👍\n\n" +
            "عدد النسخ: " +
            session.quantity +
            "\n" +
            "سعر النسخة ضمن العرض: " +
            P.sdgText +
            "\n" +
            "الإجمالي: " +
            total.toLocaleString("ar-EG") +
            " جنيه سوداني.\n\n" +
            "إذا كنت جاهزاً، أفتح لك صفحة الشراء لإكمال الخطوات.",
          actions: [{ type: "open_checkout" }]
        };
      }

      if (
        session.stage === "currency" &&
        /(دولار|دولارات|usd|\$)/.test(q)
      ) {
        session.currency = "USD";

        const total = P.usd * session.quantity;

        session.stage = "ready_to_pay";

        return {
          reply:
            "تمام 👍\n\n" +
            "عدد النسخ: " +
            session.quantity +
            "\n" +
            "سعر النسخة ضمن العرض: " +
            P.usdText +
            "\n" +
            "الإجمالي: " +
            total +
            " دولار.\n\n" +
            "إذا كنت جاهزاً، أفتح لك صفحة الشراء لإكمال الخطوات.",
          actions: [{ type: "open_checkout" }]
        };
      }

      /* المعاينة */
      if (
        /(تجربة|اجرب|أجرب|عايز اشوف|أريد اشوف|عايز أشوف|قبل الشراء|أشوف الأول|المعاينة|المعاينه|20 صفحة)/.test(
          q
        )
      ) {
        session.stage = "preview";

        return {
          reply:
            "بكل تأكيد 👍 يمكنك مشاهدة معاينة حقيقية من 20 صفحة قبل الشراء. شاهد المحتوى بنفسك، وإذا وجدت أن المستوى مناسب لك أساعدك بعدها في إكمال الشراء.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* السعر */
      if (
        /(السعر|كم سعر|كم.*الموسوعة|بكم|الثمن|تكلفة|الدفع|كم.*تكلف)/.test(
          q
        )
      ) {
        session.stage = "price";

        return {
          reply:
            "النسخة الكاملة 250 صفحة ومعها الوكيل الذكي. السعر الأساسي " +
            Number(C.price || 150000).toLocaleString("ar-EG") +
            " جنيه سوداني، والعرض الحالي لأول 200 نسخة " +
            P.sdgText +
            ". وبالدولار السعر ضمن العرض " +
            P.usdText +
            ".\n\n" +
            "ويمكنك قبل الشراء مشاهدة 20 صفحة حقيقية من المعاينة. هل تريد أن أفتحها لك؟",
          actions: [
            { type: "focus_offer" },
            { type: "open_preview" }
          ]
        };
      }

      /* غالي */
      if (
        /(غالي|غالية|السعر.*غالي|مبلغ.*كبير|ما.*عندي.*قروش|مكلف|مكلفة|ما عندي)/.test(
          q
        )
      ) {
        session.objection = "price";
        session.stage = "objection";

        return {
          reply:
            "أتفهمك تماماً 👍 ومن حقك تتأكد من القيمة قبل أن تدفع. لذلك لا أطلب منك اتخاذ القرار الآن. النسخة الكاملة 250 صفحة، ومعها الوكيل الذكي للمساعدة في الشرح والإجابة، والأهم أنك تستطيع أولاً مشاهدة 20 صفحة حقيقية من المحتوى. إذا وجدت أن القيمة مناسبة لك، نكمل الشراء بعدها.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* الكورس */
      if (
        /(تغنيني|يغنيني|بديل.*كورس|بديل.*دورة|بدون.*كورس|هل.*الموسوعة.*الكورس|الموسوعة.*بدل.*الكورس|تغني.*الكورس)/.test(
          q
        )
      ) {
        return {
          reply:
            "يمكن أن تغطي الموسوعة جزءاً كبيراً من رحلة التعلم الذاتي لأنها تبدأ من الأساسيات وتتدرج إلى موضوعات وتطبيقات متقدمة، لكنها لا تدّعي أنها بديل عن كل كورس عملي أو كل مدرب. ميزتها أنها مرجع شامل ترجع إليه في أي وقت، ومع النسخة الكاملة تحصل أيضاً على وكيل ذكي يساعدك في الفهم والإجابة عن الأسئلة.\n\n" +
            "والأفضل أن تحكم بنفسك بعد مشاهدة المعاينة المجانية.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* الوكيل */
      if (
        /(وكيل.*ذكي|الوكيل.*بيعمل|ماذا.*يفعل.*الوكيل|فائدة.*الوكيل|شنو.*الوكيل|الوكيل.*شنو)/.test(
          q
        )
      ) {
        return {
          reply:
            "الوكيل الذكي يأتي مع النسخة الكاملة 🤝 ودوره أن يكون مساعداً لك بعد الشراء: يشرح المفاهيم، يبسط المصطلحات، يجيب عن الأسئلة، ويساعدك في فهم محتوى الموسوعة. الفكرة ليست مجرد كتاب، بل مرجع + مساعد تعليمي.",
          actions: []
        };
      }

      /* السر */
      if (
        /(السر|سر.*الموسوعة|ما.*السر|شي.*سري|معلومة.*سرية)/.test(q)
      ) {
        return {
          reply:
            "لا يوجد سر سحري أو معلومة مخفية أبيعها لك 🙂 القيمة الحقيقية في تنظيم المعرفة العربية في مرجع واحد، من الأساسيات إلى التطبيقات، مع الوكيل الذكي. وأفضل دليل على ذلك هو أن ترى 20 صفحة حقيقية بنفسك قبل الشراء.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* مبتدئ */
      if (
        /(مبتدئ|مبتدئة|من الصفر|ما بعرف|لا أعرف|ما اعرف|جديد في الذكاء|جديد على الذكاء)/.test(
          q
        )
      ) {
        session.audience = "beginner";

        return {
          reply:
            "ممتاز 👍 إذا كنت تبدأ من الصفر، فهذا بالضبط أحد المسارات التي صُممت لها الموسوعة. تبدأ معك بالمفاهيم الأساسية ثم تتدرج بك إلى الموضوعات والتطبيقات الحديثة. أنصحك أولاً بمشاهدة المعاينة المجانية حتى ترى أسلوب الشرح بنفسك.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* طالب */
      if (/(طالب|طالبة|جامعة|جامعي|دراسة|طلاب)/.test(q)) {
        session.audience = "student";

        return {
          reply:
            "ممتاز 🎓 الموسوعة مناسبة كمرجع تعليمي يساعد الطالب على بناء فهم متدرج للذكاء الاصطناعي، ويمكن الرجوع إليها أثناء الدراسة والتعلم الذاتي. وإذا أردت التأكد من مستواها قبل الشراء، يمكنك مشاهدة المعاينة المجانية.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* معلم */
      if (/(معلم|مدرس|أستاذ|تدريس|مدرسة|تعليم)/.test(q)) {
        session.audience = "teacher";

        return {
          reply:
            "ممتاز 👨‍🏫 إذا كنت معلماً أو مدرباً، فالموسوعة يمكن أن تكون مرجعاً منظماً يساعدك في فهم موضوعات الذكاء الاصطناعي وشرحها للمتعلمين. ويمكنك أولاً الاطلاع على المعاينة قبل اتخاذ القرار.",
          actions: [{ type: "open_preview" }]
        };
      }

      /* ترحيب */
      if (
        /(السلام عليكم|سلام|مرحبا|مرحباً|أهلا|أهلاً|هاي|hello|hi)/.test(q)
      ) {
        session.stage = "discover";

        return {
          reply:
            "أهلاً وسهلاً بك 🌷 أنا هنا لأساعدك في معرفة الموسوعة واتخاذ القرار المناسب لك، وليس فقط لإعطائك معلومات عامة.\n\nهل هدفك تعلم الذكاء الاصطناعي من الصفر، أم تريد معرفة محتوى الموسوعة، أم أنك تفكر في شراء النسخة الكاملة؟",
          actions: []
        };
      }

      /* مساعدة عامة */
      if (
        /(ما هي|شنو|ماهو|الموسوعة|عرفني|اعرف|معلومات|محتوى|الفصول|الأبواب)/.test(
          q
        )
      ) {
        return {
          reply:
            "الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية هي مرجع تعليمي متدرج من الصفر إلى الاحتراف، والنسخة الكاملة تتكون من 250 صفحة ومعها وكيل ذكي للمساعدة في الشرح والإجابة عن الأسئلة.\n\nإذا أردت، أستطيع أن أفتح لك المعاينة المجانية لترى المحتوى بنفسك.",
          actions: [{ type: "open_preview" }]
        };
      }

      return null;
    }

    async function send(text) {
      const local = localSales(text);

      if (local) {
        add(local.reply, "done");
        (local.actions || []).forEach(act);
        setStatus("تمت الإجابة. يمكنك إرسال سؤال آخر.");
        return;
      }

      setStatus("جارٍ الاتصال بالوكيل...");

      try {
        const response = await fetch(api, {
          method: "POST",
          headers: {
            "content-type": "application/json"
          },
          body: JSON.stringify({
            message: text,
            context: {
              page: location.pathname,
              session: session,
              product: {
                title: "الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية",
                pages: Number(C.pages || 250),
                previewPages: Number(C.previewPages || 20),
                priceSDG: Number(C.reservationOffer || 120000),
                priceUSD: Number(
                  C.foreignCurrencyDiscountedPrice || 16
                ),
                agentIncluded: Boolean(C.agentIncluded)
              }
            }
          })
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
          throw new Error(data.message || "agent unavailable");
        }

        add(
          data.reply ||
            "تمت معالجة طلبك. كيف يمكنني مساعدتك أكثر؟",
          "done"
        );

        if (Array.isArray(data.actions)) {
          data.actions.forEach(act);
        }

        setStatus("تمت الإجابة. يمكنك إرسال سؤال جديد.");
      } catch (error) {
        const fallback =
          window.PLATFORM_ASSISTANT &&
          typeof window.PLATFORM_ASSISTANT.answer === "function"
            ? window.PLATFORM_ASSISTANT.answer(text)
            : null;

        add(
          fallback ||
            "تعذر الاتصال بالوكيل السحابي الآن. لكن يمكنك مشاهدة المعاينة المجانية أو التواصل معنا عبر واتساب.",
          "done"
        );

        setStatus("الوضع المحلي يعمل.");
      }
    }

    form.addEventListener(
      "submit",
      function (e) {
        e.preventDefault();
        e.stopImmediatePropagation();

        const text = input.value.trim();

        if (!text) return;

        input.value = "";

        add("طلب المستخدم: " + text, "action");

        send(text);
      },
      true
    );

    add(
      "أهلاً بك 🌷 أنا وكيلك الذكي. أخبرني: هل تريد التعرف على الموسوعة، تجربة المعاينة، أم أنك تفكر في الشراء؟",
      "done"
    );

    setStatus("جاهز لمساعدتك.");
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init, { once: true });
  } else {
    init();
  }
})();
