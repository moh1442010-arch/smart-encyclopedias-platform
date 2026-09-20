/* Sales Agent v4 — hardened Arabic sales funnel, quantity parsing, objections, checkout and WhatsApp handoff. */
(function(){
  'use strict';
  if(window.__SALES_AGENT_V4__) return;
  window.__SALES_AGENT_V4__=true;

  function boot(){
    var form=document.getElementById('agentForm'),input=document.getElementById('agentInput'),log=document.getElementById('agentLog'),status=document.getElementById('agentStatus');
    if(!form||!input||!log)return;
    var C=window.STORE_CONFIG||{};
    var P={sdg:Number(C.reservationOffer||120000),sdgBase:Number(C.price||150000),usd:Number(C.foreignCurrencyDiscountedPrice||16),usdBase:Number(C.foreignCurrencyPrice||19),limit:Number(C.reservationLimit||200),pages:Number(C.pages||260),preview:Number(C.previewPages||20)};
    var S={intent:'unknown',audience:'',goal:'',qty:null,currency:'',stage:'discover',score:0,preview:false};
    function norm(s){return String(s||'').trim().toLowerCase().replace(/[أإآ]/g,'ا').replace(/ة/g,'ه').replace(/[٠-٩]/g,function(d){return String('٠١٢٣٤٥٦٧٨٩'.indexOf(d));});}
    function add(t,kind){var d=document.createElement('div');d.className='agent-item '+(kind||'done');d.textContent=t;log.appendChild(d);log.scrollTop=log.scrollHeight;}
    function say(t){add(t,'done');if(status)status.textContent='جاهز للخطوة التالية.';}
    function money(n){return Number(n||0).toLocaleString('ar-EG');}
    function score(n){S.score=Math.min(100,S.score+n);}
    function total(q,c){var a=Math.min(q,P.limit),b=Math.max(0,q-P.limit);return c==='USD'?{total:a*P.usd+b*P.usdBase,saving:a*(P.usdBase-P.usd)}:{total:a*P.sdg+b*P.sdgBase,saving:a*(P.sdgBase-P.sdg)};}
    var ones=['','واحد','اثنان','ثلاثة','اربعة','خمسة','ستة','سبعة','ثمانية','تسعة','عشرة','احد عشر','اثنا عشر','ثلاثة عشر','اربعة عشر','خمسة عشر','ستة عشر','سبعة عشر','ثمانية عشر','تسعة عشر'];
    var tens=['','','عشرون','ثلاثون','اربعون','خمسون','ستون','سبعون','ثمانون','تسعون'];
    var hundreds=['','مائة','مائتان','ثلاثمائة','اربعمائة','خمسمائة','ستمائة','سبعمائة','ثمانمائة','تسعمائة'];
    function wordsNumber(s){var q=norm(s).replace(/نسخه|نسخ|كتاب|كتب|copy|copies/g,' ').trim();var map={'واحد':1,'واحده':1,'اثنان':2,'اثنين':2,'اثنتان':2,'اثنتين':2,'ثلاثه':3,'ثلاث':3,'اربعه':4,'اربع':4,'خمسه':5,'خمس':5,'سته':6,'ست':6,'سبعه':7,'سبع':7,'ثمانيه':8,'ثمان':8,'تسعه':9,'تسع':9,'عشره':10,'عشر':10,'عشرون':20,'ثلاثون':30,'اربعون':40,'خمسون':50,'ستون':60,'سبعون':70,'ثمانون':80,'تسعون':90,'مائه':100,'مئه':100,'مائتان':200,'مائتين':200};if(map[q]!=null)return map[q];var m=q.match(/^(.+)\s+و(.+)$/);if(m){var a=wordsNumber(m[1]),b=wordsNumber(m[2]);if(a!=null&&b!=null&&b<100)return a+b;}for(var h=1;h<=9;h++){var rest=wordsNumber(q.replace(new RegExp('^'+hundreds[h]+'\\s*'),'').trim());if(q.indexOf(hundreds[h])===0&&rest!=null)return h*100+rest;}for(var t=2;t<=9;t++){if(q===tens[t])return t*10;for(var o=1;o<=9;o++)if(q===ones[o]+' و'+tens[t])return t*10+o;}for(var i=1;i<20;i++)if(q===ones[i])return i;return null;}
    function qty(q){var m=q.match(/(?:^|\s)(\d{1,4})(?:\s|$|نسخه|نسخ|كتاب|كتب|copy|copies)/);if(m)return Math.max(1,Math.min(2000,Number(m[1])));if(/\b(?:نسخه\s*)?(واحد|واحده)\b/.test(q))return 1;var w=wordsNumber(q);return w!=null?Math.max(1,Math.min(2000,w)):null;}
    function currency(q){if(/دولار|usd|\$|امريكي/.test(q))return'USD';if(/جنيه|سوداني|sdg|ج\.س/.test(q))return'SDG';return'';}
    function openPreview(){S.preview=true;S.stage='preview';score(20);var e=document.getElementById('preview');if(e)e.scrollIntoView({behavior:'smooth',block:'start'});else location.href=C.previewUrl||'preview.html';}
    function checkout(){S.stage='checkout';S.score=100;location.href=C.purchaseUrl||'checkout.html';}
    function whatsapp(){var c=S.currency==='USD'?'الدولار الأمريكي':S.currency==='SDG'?'الجنيه السوداني':'غير محددة';var q=S.qty||'غير محدد';var msg='السلام عليكم، أرغب في شراء الموسوعة الشاملة في الذكاء الاصطناعي. عدد النسخ: '+q+'، العملة: '+c+'. أرجو إكمال إجراءات الدفع والتسليم.';location.href=(C.whatsappUrl||'https://wa.me/249121851285')+'?text='+encodeURIComponent(msg);}
    function purchaseFlow(q,n,c){S.intent='purchase';S.stage='purchase';if(n)S.qty=n;if(c)S.currency=c;score(15);if(!S.qty){say('ممتاز. لنكمل الطلب بسرعة. كم نسخة تريد؟ يمكنك كتابة رقم مثل 1 أو 10، أو كتابة «عشر نسخ».');return true;}if(!S.currency){say('تمام. بقيت خطوة واحدة قبل الحساب. هل ستدفع بالجنيه السوداني أم بالدولار؟');return true;}var z=total(S.qty,S.currency),cn=S.currency==='USD'?'دولار أمريكي':'جنيه سوداني',unit=S.currency==='USD'?P.usd:P.sdg;S.stage='ready';score(35);say('تم تجهيز طلبك:\n\nعدد النسخ: '+S.qty+'\nسعر العرض: '+money(unit)+' '+cn+' للنسخة ضمن أول '+P.limit+' نسخة\nالإجمالي: '+money(z.total)+' '+cn+'\nالتوفير: '+money(z.saving)+' '+cn+'\n\nإذا كنت جاهزاً، أفتح لك صفحة تسجيل الطلب والدفع الآن.');setTimeout(checkout,350);return true;}
    function handle(text){var q=norm(text),n=qty(q),c=currency(q);if(!q)return true;
      if(/السلام عليكم|^سلام$|مرحبا|اهلا|هاي|hello|hi/.test(q)){say('أهلاً بك. أنا وكيل المبيعات الذكي في المنصة. سأساعدك في معرفة ما إذا كانت الموسوعة مناسبة لك، ثم أترك لك القرار. هل هدفك التعلم من الصفر، الدراسة، التدريس، تطوير المهارة، أم الحصول على مرجع عربي شامل؟');return true;}
      if(/اريد شراء|عايز اشتري|عايز شراء|جاهز للشراء|اشتري|ساشتري|أريد النسخه|عايز النسخه|عايز نسخه|سجل لي الطلب/.test(q))return purchaseFlow(q,n,c);
      if(S.intent==='purchase'){
        if(/واتساب|موظف|شخص|مباشر|مساعده بشريه/.test(q)){say('بكل سرور. أحوّلك مباشرة إلى واتساب مع رسالة جاهزة تلخص طلبك.');setTimeout(whatsapp,250);return true;}
        if(/دفع|ادفع|كيف ادفع|طريقة الدفع|بنكك|حساب|ايبان|iban/.test(q)){say('يمكنك إكمال تسجيل الطلب ثم متابعة الدفع عبر بنكك — بنك الخرطوم للجنيه السوداني، أو عبر بيانات IBAN للدولار. أفتح لك صفحة الشراء الآن.');setTimeout(checkout,250);return true;}
        return purchaseFlow(q,n,c);
      }
      if(/طالب|طالبه|جامعه|جامعي|دراسه/.test(q)){S.audience='student';S.goal='study';score(10);say('ممتاز. للطالب أهم شيء أن ترى جودة المحتوى قبل الدفع. أفتح لك الآن المعاينة الحقيقية ذات الـ'+P.preview+' صفحة، وبعدها أساعدك في تقييمها.');setTimeout(openPreview,250);return true;}
      if(/معلم|مدرس|استاذ|تدريس|مدرب/.test(q)){S.audience='teacher';S.goal='teaching';score(10);say('إذا كان هدفك التدريس أو التدريب، افحص المحتوى أولاً. أفتح لك المعاينة، ثم نحدد هل النسخة الكاملة مناسبة لك.');setTimeout(openPreview,250);return true;}
      if(/مبتدئ|من الصفر|ما بعرف|لا اعرف|جديد في الذكاء|بدايه/.test(q)){S.audience='beginner';S.goal='learn';score(10);say('هذا بالضبط أحد استخدامات الموسوعة: بناء المعرفة من الأساسيات إلى الموضوعات الحديثة. شاهد المعاينة أولاً ثم قرر بنفسك.');setTimeout(openPreview,250);return true;}
      if(/شركه|شركة|مؤسسه|مؤسسة|فريق|موظفين|جماعي|جمله|جملة/.test(q)){S.audience='organization';S.goal='bulk';score(15);if(n){S.qty=n;return purchaseFlow(q,n,c);}say('للطلب المؤسسي أحتاج فقط إلى عدد النسخ. اكتب مثلاً: 10 نسخ، أو «عشر نسخ». وسأحسب لك الإجمالي مباشرة.');return true;}
      if(/chatgpt|شات جي بي تي|عندي شات/.test(q)){say('وجود ChatGPT لا يلغي قيمة مرجع منظم. ChatGPT أداة للحوار، بينما الموسوعة تقدم مساراً معرفياً عربياً منظماً، ومع النسخة الكاملة يوجد وكيل مساعد. لا تعتمد على كلامي؛ شاهد المعاينة أولاً.');setTimeout(openPreview,250);return true;}
      if(/يوتيوب|مجانا|مجاني|الانترنت|المعلومات موجوده/.test(q)){say('صحيح، توجد معلومات مجانية كثيرة. قيمة المنتج هنا في تنظيم المعرفة في مرجع عربي واحد مع وكيل مساعد في النسخة الكاملة. شاهد المعاينة واحكم بنفسك.');setTimeout(openPreview,250);return true;}
      if(/غالي|غاليه|مكلف|ما عندي قروش|السعر كبير|ما عندي المبلغ|مرتفع/.test(q)){say('أتفهم اعتراضك. لن أجادلك في السعر. الأفضل أن تقلل المخاطرة: شاهد 20 صفحة حقيقية أولاً، وإذا اقتنعت بالقيمة أكمل الشراء، وإذا لم تقتنع فلا تدفع.');setTimeout(openPreview,250);return true;}
      if(/ثقه|اثق|موثوق|حقيقي|دليل|اثبات|ضمان/.test(q)){say('أفضل دليل هو المنتج نفسه. لديك معاينة حقيقية قبل الدفع. افحص المحتوى والتنظيم بنفسك ثم قرر.');setTimeout(openPreview,250);return true;}
      if(/تغنيني|يغنيني|بديل.*كورس|بديل.*دوره|الموسوعه.*الكورس|الموسوعه.*الدوره/.test(q)){say('الموسوعة يمكن أن تكون أساساً قوياً للتعلم الذاتي، لكنها ليست بديلاً مضموناً عن كل كورس عملي أو كل مدرب. شاهد المعاينة أولاً لتعرف إن كانت مناسبة لهدفك.');setTimeout(openPreview,250);return true;}
      if(/شنو.*الوكيل|ما.*الوكيل|ماذا.*يفعل.*الوكيل|فائده.*الوكيل|الوكيل.*بيعمل/.test(q)){say('الوكيل الذكي مرفق مع النسخة الكاملة. يساعدك في فهم موضوعات الموسوعة، شرح المصطلحات، الإجابة عن الأسئلة، وتوجيهك داخل المحتوى. وهو مساعد للتعلم وليس بديلاً عن المعلم.');return true;}
      if(/ليه اشتري|لماذا اشتري|شنو الفائده|ماذا استفيد|شنو بشتري|تستحق/.test(q)){say('إذا كان هدفك التعلم بالعربية، تحصل على مرجع منظم من '+P.pages+' صفحة ومعه وكيل ذكي في النسخة الكاملة. لا أطلب منك أن تصدقني؛ شاهد المعاينة أولاً ثم اتخذ القرار.');setTimeout(openPreview,250);return true;}
      if(/معاينه|20 صفحه|قبل الشراء|اشوف قبل|اجرب|تجربه/.test(q)){say('بكل سرور. هذه أفضل خطوة قبل الشراء: شاهد المعاينة الحقيقية ثم عد إلي إذا أردت التقييم أو إكمال الطلب.');setTimeout(openPreview,250);return true;}
      if(/السعر|بكم|الثمن|تكلفه/.test(q)){say('السعر الأساسي '+money(P.sdgBase)+' جنيه سوداني، وعرض أول '+P.limit+' نسخة هو '+money(P.sdg)+' جنيه. وبالدولار السعر الأساسي $'+P.usdBase+' والعرض $'+P.usd+'. إذا أخبرتني بعدد النسخ والعملة أحسب لك الإجمالي.');return true;}
      if(/الدولار|جنيه|سوداني|عمله|عملة/.test(q)){say('الدفع متاح بالجنيه السوداني أو بالدولار. أخبرني بعدد النسخ وسأحسب لك الإجمالي والتوفير.');return true;}
      if(/دفع|كيف ادفع|طريقة الدفع|بنكك|حساب|iban|ايبان/.test(q)){say('الدفع المحلي عبر بنكك — بنك الخرطوم، والحساب 1882224. وللدولار توجد بيانات IBAN في صفحة الشراء. أفتحها لك الآن؟');setTimeout(checkout,250);return true;}
      if(/واتساب|اتصل|تواصل|موظف|شخص حقيقي/.test(q)){say('يمكنك التواصل مباشرة مع موظف المبيعات عبر واتساب. سأفتح لك المحادثة الآن.');setTimeout(whatsapp,250);return true;}
      return false;
    }
    function fallback(text){var api=C.agentApiUrl;if(!api)return;fetch(api,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({message:text,system:'أنت موظف مبيعات محترف للموسوعة الشاملة في الذكاء الاصطناعي. ساعد الزائر بلطف، لا تخترع سعراً أو ميزة، اسأل سؤالاً واحداً عند الحاجة، وجهه للمعاينة ثم للشراء أو واتساب عند الجاهزية.',context:{pages:P.pages,preview:P.preview,sdg:P.sdg,sdgBase:P.sdgBase,usd:P.usd,usdBase:P.usdBase}})}).then(function(r){return r.json()}).then(function(d){var t=d&&((d.reply)||(d.message)||(d.text));if(t)say(t);else say('أستطيع مساعدتك في السعر، المعاينة، الشراء أو الدفع. ما الذي تريد معرفته؟');}).catch(function(){say('أستطيع مساعدتك في السعر، المعاينة، الشراء أو الدفع. ما الذي تريد معرفته؟');});}
    function save(){try{sessionStorage.setItem('salesAgentV4',JSON.stringify(S));}catch(e){}}
    try{var old=sessionStorage.getItem('salesAgentV4');if(old)S=Object.assign(S,JSON.parse(old));}catch(e){}
    form.addEventListener('submit',function(e){e.preventDefault();var text=input.value.trim();if(!text)return;add(text,'user');input.value='';if(!handle(text))fallback(text);save();},{capture:true});
    if(!log.dataset.v4greeted){log.dataset.v4greeted='1';setTimeout(function(){say('مرحباً بك. أنا وكيل المبيعات الذكي. دوري أن أساعدك في معرفة هل الموسوعة مناسبة لك قبل أن تدفع. أخبرني: ماذا تريد أن تتعلم أو تحقق من الموسوعة؟');},450);}
    setTimeout(function(){if(!S.score&&!log.dataset.v4proactive){log.dataset.v4proactive='1';say('إذا كنت متردداً، ابدأ بالمعاينة المجانية ذات الـ'+P.preview+' صفحة؛ لا تحتاج إلى الدفع لمشاهدتها.');}},45000);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot);else boot();
})();