/* Sales agent v2 — local conversion flow with cloud fallback. */
(function(){
  'use strict';
  if(window.__FINAL_SALES_AGENT_V2__) return;
  window.__FINAL_SALES_AGENT_V2__=true;
  function start(){
    var form=document.getElementById('agentForm'),input=document.getElementById('agentInput'),log=document.getElementById('agentLog'),status=document.getElementById('agentStatus');
    if(!form||!input||!log)return;
    var C=window.STORE_CONFIG||{};
    var S={intent:'discover',audience:'',goal:'',objection:'',qty:null,currency:'',preview:false,history:[]};
    var P={sdg:Number(C.reservationOffer||120000),sdgBase:Number(C.price||150000),usd:Number(C.foreignCurrencyDiscountedPrice||16),usdBase:Number(C.foreignCurrencyPrice||19)};
    function norm(s){return String(s||'').trim().toLowerCase().replace(/[أإآ]/g,'ا').replace(/ة/g,'ه').replace(/[٠-٩]/g,function(d){return String('٠١٢٣٤٥٦٧٨٩'.indexOf(d));});}
    function add(t,k){var d=document.createElement('div');d.className='agent-item '+(k||'');d.style.whiteSpace='pre-line';d.textContent=t;log.appendChild(d);log.scrollTop=log.scrollHeight;}
    function stat(t){if(status)status.textContent=t;}
    function act(a){
      if(!a||!a.type)return;
      if(a.type==='open_preview'){
        S.preview=true;
        var e=document.getElementById('preview');
        if(e)e.scrollIntoView({behavior:'smooth',block:'start'});else location.href=C.previewUrl||'preview.html';
      }else if(a.type==='open_preview_page'){
        var p=Number(a.page);if(p>=1&&p<=20)location.href=(C.previewUrl||'preview.html')+'#'+p;
      }else if(a.type==='focus_offer'){
        var o=document.querySelector('.offer');if(o)o.scrollIntoView({behavior:'smooth',block:'center'});
      }else if(a.type==='open_checkout'){location.href=C.purchaseUrl||'checkout.html';
      }else if(a.type==='open_whatsapp'){location.href=C.whatsappUrl||'https://wa.me/249121851285';}
    }
    function qty(q){var m=q.match(/(?:^|\s)(\d{1,4})\s*(?:نسخه|نسخ|كتاب|كتب|copy|copies)/);if(m)return Math.max(1,Math.min(2000,Number(m[1])));if(/(?:^|\s)(?:واحد|واحده|نسخه واحده|نسخه واحد)(?:\s|$)/.test(q))return 1;return null;}
    function cur(q){if(/دولار|usd|\$/.test(q))return'USD';if(/جنيه|سوداني|sdg/.test(q))return'SDG';return'';}
    function total(n,c){var offer=Math.min(n,200),reg=Math.max(0,n-200);return c==='USD'?{t:offer*P.usd+reg*P.usdBase,s:offer*(P.usdBase-P.usd)}:{t:offer*P.sdg+reg*P.sdgBase,s:offer*(P.sdgBase-P.sdg)};}
    function reply(t,actions){add(t,'done');S.history.push({role:'assistant',text:t});(actions||[]).forEach(act);stat('يمكنك مواصلة الحوار أو الانتقال للخطوة التالية.');}
    function purchase(q,n,c){
      S.intent='purchase';if(n)S.qty=n;if(c)S.currency=c;
      if(!S.qty){reply('ممتاز. لنكمل الشراء خطوة بخطوة. كم نسخة تريد؟');return true;}
      if(!S.currency){reply('بقيت خطوة واحدة. تفضل الدفع بالجنيه السوداني أم بالدولار؟');return true;}
      var z=total(S.qty,S.currency),cn=S.currency==='USD'?'دولار':'جنيه سوداني',unit=S.currency==='USD'?P.usd:P.sdg;
      reply('تمام. هذا ملخص طلبك\n\nعدد النسخ: '+S.qty+'\nسعر العرض للنسخة: '+unit.toLocaleString('ar-EG')+' '+cn+'\nالإجمالي: '+z.t.toLocaleString('ar-EG')+' '+cn+'\nالتوفير: '+z.s.toLocaleString('ar-EG')+' '+cn+'\n\nإذا كان هذا مناسباً لك، أفتح لك صفحة الدفع الآن.',[{type:'open_checkout'}]);return true;
    }
    function local(text){
      var q=norm(text),n=qty(q),c=cur(q);if(!q)return true;
      if(S.intent==='purchase'&&(n||c||/اكمل|تمام|موافق|جاهز/.test(q)))return purchase(q,n,c);
      if(/السلام عليكم|^سلام$|مرحبا|اهلا|هاي|hello|hi/.test(q)){reply('أهلاً بك. سأساعدك في اتخاذ القرار، وليس فقط في عرض السعر. ما هدفك من الموسوعة؟ التعلم من الصفر، الدراسة، التدريس، تطوير مهارة، أم شراء نسخة؟');return true;}
      if(/اريد شراء|عايز اشتري|عايز شراء|جاهز للشراء|اشتري|ساشتري|أريد النسخه|عايز النسخه|عايز نسخه|كيف اشتري|طريقة الشراء/.test(q))return purchase(q,n,c);
      if(/مبتدئ|من الصفر|ما بعرف|لا اعرف|جديد في الذكاء|بدايه/.test(q)){S.audience='beginner';S.goal='learn';reply('إذا كنت تبدأ من الصفر، الموسوعة تقدم مساراً منظماً من الأساسيات إلى موضوعات الذكاء الاصطناعي الحديثة. لا تحتاج أن تعتمد على الوصف فقط. شاهد 20 صفحة حقيقية ثم احكم على المحتوى بنفسك.',[{type:'open_preview'}]);return true;}
      if(/طالب|طالبه|جامعه|جامعي|دراسه/.test(q)){S.audience='student';S.goal='study';reply('إذا كنت طالباً، ستستفيد من وجود مرجع عربي منظم بدل جمع الموضوعات من مصادر متفرقة. أنصحك بمشاهدة المعاينة أولاً ثم مقارنة القيمة بالسعر.',[{type:'open_preview'}]);return true;}
      if(/معلم|مدرس|استاذ|تدريس|مدرب/.test(q)){S.audience='teacher';S.goal='teaching';reply('يمكن استخدام الموسوعة كمرجع منظم لفهم موضوعات الذكاء الاصطناعي وتحضير الشرح. شاهد المعاينة أولاً لتتأكد من مستوى المحتوى الذي تحتاجه.',[{type:'open_preview'}]);return true;}
      if(/chatgpt|شات جي بي تي|عندي شات|عندي ذكاء اصطناعي/.test(q)){S.objection='existing_ai';reply('وجود ChatGPT لا يجعل المرجع المنظم غير مفيد. ChatGPT أداة للحوار، بينما الموسوعة تجمع مساراً معرفياً عربياً منظماً، ومع النسخة الكاملة يوجد وكيل مساعد. لا أطلب منك تصديقي؛ شاهد المعاينة وقارن بنفسك.',[{type:'open_preview'}]);return true;}
      if(/يوتيوب|مجانا|مجاني|المعلومات موجوده|موجوده في الانترنت|الانترنت/.test(q)){S.objection='free';reply('صحيح. كثير من معلومات الذكاء الاصطناعي متاحة مجاناً. أنت هنا تدفع مقابل التنظيم في مرجع واحد، والمعاينة والوكيل المرفق بالنسخة الكاملة. لذلك شاهد المحتوى قبل أن تقرر.',[{type:'open_preview'}]);return true;}
      if(/غالي|غاليه|مكلف|ما عندي قروش|السعر كبير|ما عندي المبلغ/.test(q)){S.objection='price';reply('أتفهم اعتراضك على السعر. لا أنصحك بالشراء قبل أن ترى المحتوى. لديك 20 صفحة حقيقية للمعاينة. إذا وجدت أن المحتوى لا يساوي السعر بالنسبة لك، لا تشترِ.',[{type:'open_preview'}]);return true;}
      if(/تغنيني|يغنيني|بديل.*كورس|بديل.*دوره|هل.*الموسوعه.*الكورس|الموسوعه.*بدل.*الكورس/.test(q)){reply('الموسوعة مناسبة للتعلم الذاتي والرجوع إلى مرجع منظم، لكنها لا تضمن أن تغني عن كل كورس عملي أو مدرب. إذا كان هدفك التعلم بالعربية مع مرجع ووكيل مساعد، شاهد المعاينة أولاً ثم قرر.',[{type:'open_preview'}]);return true;}
      if(/شنو.*الوكيل|ما.*الوكيل|ماذا.*يفعل.*الوكيل|فائده.*الوكيل|الوكيل الذكي/.test(q)){reply('الوكيل الذكي يأتي مع النسخة الكاملة. يساعد في شرح مفاهيم الموسوعة، تبسيط المصطلحات، الإجابة عن الأسئلة، وتوجيهك داخل تجربة التعلم. وهو مساعد للمرجع وليس بديلاً عن المعلم.');return true;}
      if(/تستحق|ليه اشتري|لماذا اشتري|شنو الفائده|فائدتها|ماذا استفيد|هل تستحق/.test(q)){S.intent='interest';reply('القيمة الأساسية هي مرجع عربي منظم من الأساسيات إلى موضوعات حديثة، مع وكيل ذكي مرفق بالنسخة الكاملة. أفضل اختبار للقيمة هو المحتوى نفسه. أفتح لك المعاينة الآن؟',[{type:'open_preview'}]);return true;}
      if(/تجربه|اجرب|اشوف الاول|اشوف قبل|معاينه|20 صفحه|قبل الشراء/.test(q)){reply('بكل سرور. افتح 20 صفحة حقيقية الآن. وبعد أن تنتهي، ارجع إلى الوكيل وسأساعدك في مقارنة ما رأيته مع السعر والمزايا.',[{type:'open_preview'}]);return true;}
      if(/السعر|بكم|الثمن|تكلفه|الدفع|كم النسخه/.test(q)){reply('النسخة الكاملة 250 صفحة. السعر الأساسي '+P.sdgBase.toLocaleString('ar-EG')+' جنيه سوداني، وعرض أول 200 نسخة '+P.sdg.toLocaleString('ar-EG')+' جنيه للنسخة. بالدولار 19، وعرض 16 دولاراً. إذا أخبرتني بعدد النسخ أحسب الإجمالي والتوفير لك.',[{type:'focus_offer'}]);return true;}
      if(/صفحه|محتوى|ماذا تحتوي|شنو فيها|المواضيع|الفصول/.test(q)){reply('النسخة الكاملة 250 صفحة، وتغطي أساسيات الذكاء الاصطناعي والتعلم الآلي وLLMs وRAG وهندسة الأوامر والوكلاء والذكاء متعدد الوسائط وغيرها. المعاينة تعرض 20 صفحة حقيقية.',[{type:'open_preview'}]);return true;}
      if(/دفع|بنكك|بنك الخرطوم|حساب|تحويل|واتساب/.test(q)){reply('الدفع المحلي عبر بنكك — بنك الخرطوم، والحساب المعروض في صفحة الشراء. ويتوفر الدفع بالدولار أيضاً. إذا أردت، أفتح لك صفحة الدفع والتفاصيل الآن.',[{type:'open_checkout'}]);return true;}
      return false;
    }
    async function cloud(text){
      var api=String(C.agentApiUrl||'').trim();if(!api)throw Error('no api');
      var context='أنت موظف مبيعات للموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية. هدفك مساعدة الزائر على اتخاذ قرار شراء واعٍ. لا تكذب ولا تعد بما هو غير موجود. البيانات: 250 صفحة، معاينة 20 صفحة، وكيل ذكي مع النسخة الكاملة، السعر الأساسي '+P.sdgBase+' جنيه، عرض أول 200 نسخة '+P.sdg+' جنيه، الدولار '+P.usdBase+' والعرض '+P.usd+'. حالة الحوار '+JSON.stringify(S)+' . أجب عن آخر سؤال فقط، عالج الاعتراض مباشرة، ثم اقترح خطوة واحدة مناسبة.';
      var r=await fetch(api,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({message:context+'\nرسالة الزائر: '+text,context:{sales_state:S}})});
      var d=await r.json().catch(function(){return{};});if(!r.ok)throw Error(d.message||'agent unavailable');return d;
    }
    document.addEventListener('submit',function(e){
      if(e.target!==form)return;
      e.preventDefault();e.stopImmediatePropagation();
      var text=input.value.trim();if(!text)return;input.value='';add('سؤال الزائر: '+text,'action');S.history.push({role:'user',text:text});
      if(local(text))return;
      stat('أراجع احتياجك وأجهز لك إجابة...');
      cloud(text).then(function(d){reply(d.reply||'أخبرني ما الذي تريد معرفته عن الموسوعة أو الشراء.',Array.isArray(d.actions)?d.actions:[]);}).catch(function(){reply('أستطيع مساعدتك في القرار. يمكنك البدء بالمعاينة المجانية، أو أخبرني هل تريد معرفة المحتوى أم السعر أم طريقة الشراء.',[{type:'open_preview'}]);});
    },true);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
})();