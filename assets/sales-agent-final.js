/* Final sales-agent override: intercepts the visible agent before the legacy handler. */
(function(){
  'use strict';
  if(window.__FINAL_SALES_AGENT__) return;
  window.__FINAL_SALES_AGENT__ = true;

  function start(){
    var form=document.getElementById('agentForm'), input=document.getElementById('agentInput'), log=document.getElementById('agentLog'), status=document.getElementById('agentStatus');
    if(!form||!input||!log) return;
    var C=window.STORE_CONFIG||{};
    var S={stage:'discover',intent:'unknown',audience:'',goal:'',objection:'',qty:null,currency:'',preview:false,history:[]};
    var P={sdg:Number(C.reservationOffer||120000),sdgBase:Number(C.price||150000),usd:Number(C.foreignCurrencyDiscountedPrice||16),usdBase:Number(C.foreignCurrencyPrice||19)};
    var norm=function(s){return String(s||'').trim().toLowerCase().replace(/[أإآ]/g,'ا').replace(/ة/g,'ه').replace(/[٠-٩]/g,function(d){return String('٠١٢٣٤٥٦٧٨٩'.indexOf(d));});};
    var add=function(t,k){var d=document.createElement('div');d.className='agent-item '+(k||'');d.textContent=t;log.appendChild(d);log.scrollTop=log.scrollHeight;};
    var stat=function(t){if(status)status.textContent=t;};
    var act=function(a){if(!a||!a.type)return;if(a.type==='open_preview'){S.preview=true;var e=document.getElementById('preview');if(e)e.scrollIntoView({behavior:'smooth',block:'start'});else location.href=C.previewUrl||'preview.html';}else if(a.type==='open_preview_page'){var p=Number(a.page);if(p>=1&&p<=20)location.href=(C.previewUrl||'preview.html')+'#'+p;}else if(a.type==='focus_offer'){document.querySelector('.offer')?.scrollIntoView({behavior:'smooth',block:'center'});}else if(a.type==='open_checkout'){location.href=C.purchaseUrl||'checkout.html';}else if(a.type==='open_whatsapp'){location.href=C.whatsappUrl||'https://wa.me/249121851285';}};
    function qty(q){var m=q.match(/(?:^|\s)(\d{1,4})\s*(?:نسخه|نسخ|كتاب|كتب|copy|copies)/);if(m)return Math.max(1,Math.min(2000,Number(m[1])));if(/^(?:واحد|واحده|نسخه واحده|نسخه واحد)$/.test(q)||/\bواحده?\b/.test(q))return 1;return null;}
    function cur(q){if(/دولار|usd|\$/.test(q))return'USD';if(/جنيه|سوداني|sdg/.test(q))return'SDG';return'';}
    function total(n,c){var offer=Math.min(n,200),reg=Math.max(0,n-200);return c==='USD'?{t:offer*P.usd+reg*P.usdBase,s:offer*(P.usdBase-P.usd)}:{t:offer*P.sdg+reg*P.sdgBase,s:offer*(P.sdgBase-P.sdg)};}
    function reply(text,actions){add(text,'done');S.history.push({role:'assistant',text:text});(actions||[]).forEach(act);stat('جاهز للخطوة التالية.');}
    function local(text){var q=norm(text), n=qty(q), c=cur(q);if(!q)return true;
      if(S.intent==='purchase'){
        if(!S.qty&&n){S.qty=n;S.stage='currency';}
        if(!S.currency&&c){S.currency=c;S.stage='ready';}
        if(!S.qty){reply('ممتاز 🌟 لنكمل طلبك. كم نسخة تريد؟ نسخة واحدة أم أكثر؟');return true;}
        if(!S.currency){reply('تمام 👍 بقيت خطوة واحدة: تفضل الدفع بالجنيه السوداني أم بالدولار؟');return true;}
        var z=total(S.qty,S.currency), cn=S.currency==='USD'?'دولار':'جنيه سوداني', unit=S.currency==='USD'?P.usd:P.sdg;
        S.stage='ready';reply('ممتاز 🌟\n\nالطلب: '+S.qty+' نسخة\nسعر العرض: '+unit.toLocaleString('ar-EG')+' '+cn+' للنسخة\nالإجمالي: '+z.t.toLocaleString('ar-EG')+' '+cn+'\nالتوفير: '+z.s.toLocaleString('ar-EG')+' '+cn+'\n\nإذا كنت جاهزاً، أفتح لك صفحة الشراء الآن.',[{type:'open_checkout'}]);return true;
      }
      if(/السلام عليكم|^سلام$|مرحبا|اهلا|هاي|hello|hi/.test(q)){reply('أهلاً وسهلاً بك 🌷 خليني أساعدك بطريقة عملية: ما الذي تريد تحقيقه من الذكاء الاصطناعي؟ هل تبدأ من الصفر، تطور مهارتك، أم تبحث عن مرجع عربي شامل؟');return true;}
      if(/اريد شراء|عايز اشتري|عايز شراء|جاهز للشراء|اشتري|ساشتري|أريد النسخه|عايز النسخه|عايز نسخه/.test(q)){S.intent='purchase';S.stage='purchase';if(n)S.qty=n;if(c)S.currency=c;return local('أريد الشراء');}
      if(/مبتدئ|من الصفر|ما بعرف|لا اعرف|جديد في الذكاء|بدايه/.test(q)){S.audience='beginner';S.goal='learn';reply('ممتاز 👍 إذا كنت تبدأ من الصفر، أهم شيء ألا تتوه بين مصادر متفرقة. الموسوعة تعطيك مساراً منظماً من الأساسيات إلى الموضوعات والتطبيقات الحديثة، ومع النسخة الكاملة يوجد وكيل ذكي للمساعدة. والأفضل أن ترى المستوى بنفسك أولاً: أفتح لك المعاينة المجانية؟',[{type:'open_preview'}]);return true;}
      if(/طالب|طالبه|جامعه|جامعي|دراسه/.test(q)){S.audience='student';S.goal='study';reply('ممتاز 🎓 إذا كنت طالباً، فالقيمة الأساسية هي وجود مرجع عربي منظم ترجع إليه أثناء الدراسة بدلاً من جمع المعلومات من مصادر متفرقة. شاهد المعاينة أولاً، ثم نقرر معاً هل تستحق الشراء بالنسبة لك.',[{type:'open_preview'}]);return true;}
      if(/معلم|مدرس|استاذ|تدريس|مدرب/.test(q)){S.audience='teacher';S.goal='teaching';reply('ممتاز 👨‍🏫 يمكن أن تكون الموسوعة مرجعاً منظماً يساعدك في بناء فهمك وشرح الموضوعات للمتعلمين. لا أريد أن أفترض أنها مناسبة لكل أسلوب تدريس؛ شاهد المعاينة أولاً ثم احكم بنفسك.',[{type:'open_preview'}]);return true;}
      if(/عندي chatgpt|عندي شات|chatgpt|شات جي بي تي/.test(q)){S.objection='existing_ai';reply('مفهوم 👍 وجود ChatGPT لا يلغي الحاجة إلى مرجع منظم إذا كان هدفك التعلم. الأداة تجيبك عند السؤال، بينما الموسوعة تجمع مساراً معرفياً عربياً منظماً، ومع النسخة الكاملة يوجد وكيل مساعد. لا أطلب منك أن تصدقني؛ شاهد المعاينة واحكم بنفسك.',[{type:'open_preview'}]);return true;}
      if(/يوتيوب|مجانا|مجاني|موجوده في الانترنت|المعلومات موجوده/.test(q)){S.objection='free';reply('صحيح، توجد معلومات كثيرة مجانية على الإنترنت 👍 وأنا لا أدعي عكس ذلك. القيمة هنا في التنظيم والمرجع العربي الواحد والمساعدة المرفقة بالنسخة الكاملة. لذلك المعاينة هي أفضل طريقة لتعرف هل هذا التنظيم يستحق الدفع بالنسبة لك.',[{type:'open_preview'}]);return true;}
      if(/غالي|غاليه|مكلف|ما عندي قروش|السعر كبير|ما عندي المبلغ/.test(q)){S.objection='price';reply('أتفهمك تماماً 🤝 ولا أريدك أن تشتري شيئاً غير مناسب لك. خلينا نقلل المخاطرة: شاهد 20 صفحة حقيقية أولاً، وبعدها احكم على جودة المحتوى والتنظيم والوكيل المرفق. إذا اقتنعت، أكمل معك الشراء.',[{type:'open_preview'}]);return true;}
      if(/تغنيني|يغنيني|بديل.*كورس|بديل.*دوره|هل.*الموسوعه.*الكورس|الموسوعه.*بدل.*الكورس/.test(q)){reply('يمكن أن تغطي الموسوعة جزءاً كبيراً من التعلم الذاتي، لكنها ليست بديلاً مضموناً عن كل كورس عملي أو كل مدرب. إذا كان هدفك مرجعاً عربياً منظماً ومساراً للتعلم الذاتي، فهذه نقطة قوتها. شاهد المعاينة أولاً، وبعدها أساعدك في اتخاذ القرار.',[{type:'open_preview'}]);return true;}
      if(/شنو.*الوكيل|ما.*الوكيل|ماذا.*يفعل.*الوكيل|فائده.*الوكيل/.test(q)){reply('الوكيل الذكي مرفق مع النسخة الكاملة 🤝 وهو مساعد تعليمي يشرح المفاهيم، يبسط المصطلحات ويجيب عن الأسئلة. ليس بديلاً سحرياً عن المعلم؛ قيمته أن يكون مساعداً مع المرجع بعد الشراء.');return true;}
      if(/تستحق|ليه اشتري|لماذا اشتري|شنو الفائده|فائدتها|ماذا استفيد/.test(q)){S.intent='interest';reply('إذا كان هدفك التعلم بالعربية، فأنت لا تشتري صفحات فقط؛ أنت تشتري مرجعاً منظماً ترجع إليه، مع وكيل ذكي مرفق بالنسخة الكاملة. لكن الحكم الأفضل يكون من المحتوى نفسه. أفتح لك 20 صفحة حقيقية الآن؟',[{type:'open_preview'}]);return true;}
      if(/تجربه|اجرب|اشوف الاول|اشوف قبل|معاينه|20 صفحه|قبل الشراء/.test(q)){S.stage='preview';reply('بكل سرور 👍 شاهد المعاينة الحقيقية أولاً. وبعد أن تراها، ارجع لي وسأساعدك في تحديد الخطوة التالية.',[{type:'open_preview'}]);return true;}
      if(/السعر|بكم|الثمن|تكلفه|الدفع/.test(q)){reply('النسخة الكاملة 250 صفحة. السعر الأساسي '+P.sdgBase.toLocaleString('ar-EG')+' جنيه سوداني، والعرض لأول 200 نسخة '+P.sdg.toLocaleString('ar-EG')+' جنيه للنسخة. وبالدولار 19، أو 16 دولاراً ضمن العرض. إذا أخبرتني بعدد النسخ أحسب لك الإجمالي والتوفير مباشرة.',[{type:'focus_offer'}]);return true;}
      return false;
    }
    async function cloud(text){var api=String(C.agentApiUrl||'').trim();if(!api)throw Error('no api');var context='سياق المبيعات الحالي: '+JSON.stringify({stage:S.stage,intent:S.intent,audience:S.audience,goal:S.goal,objection:S.objection,quantity:S.qty,currency:S.currency,preview:S.preview,history:S.history.slice(-6)});var r=await fetch(api,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({message:context+'\nرسالة الزائر الحالية: '+text,context:{sales_state:S}})});var d=await r.json().catch(function(){return{};});if(!r.ok)throw Error(d.message||'agent unavailable');return d;}
    document.addEventListener('submit',function(e){if(e.target!==form)return;e.preventDefault();e.stopImmediatePropagation();var text=input.value.trim();if(!text)return;input.value='';add('طلب المستخدم: '+text,'action');S.history.push({role:'user',text:text});if(local(text))return;stat('أفكر كموظف مبيعات...');cloud(text).then(function(d){var r=d.reply||'كيف أستطيع مساعدتك في اتخاذ القرار؟';reply(r,Array.isArray(d.actions)?d.actions:[]);}).catch(function(){reply('أقدر أساعدك 👍 إذا كان هدفك التعلم، ابدأ بالمعاينة المجانية. وإذا كنت جاهزاً للشراء، أخبرني بعدد النسخ والعملة.');});},true);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
})();
