/* Sales Agent Pro v3 — qualification, objection handling, guided selling, checkout routing and follow-up handoff. */
(function(){
  'use strict';
  if(window.__FINAL_SALES_AGENT_PRO__) return;
  window.__FINAL_SALES_AGENT_PRO__=true;

  function start(){
    var form=document.getElementById('agentForm'), input=document.getElementById('agentInput'), log=document.getElementById('agentLog'), status=document.getElementById('agentStatus');
    if(!form||!input||!log) return;
    var C=window.STORE_CONFIG||{};
    var P={
      sdg:Number(C.reservationOffer||120000), sdgBase:Number(C.price||150000),
      usd:Number(C.foreignCurrencyDiscountedPrice||16), usdBase:Number(C.foreignCurrencyPrice||19),
      limit:Number(C.reservationLimit||200), pages:Number(C.pages||250), preview:Number(C.previewPages||20)
    };
    var S={stage:'discover',intent:'unknown',audience:'',goal:'',objection:'',qty:null,currency:'',preview:false,score:0,history:[],lastAction:'',contactAsked:false};

    function norm(s){return String(s||'').trim().toLowerCase().replace(/[أإآ]/g,'ا').replace(/ة/g,'ه').replace(/[٠-٩]/g,function(d){return String('٠١٢٣٤٥٦٧٨٩'.indexOf(d));});}
    function add(t,k){var d=document.createElement('div');d.className='agent-item '+(k||'');d.textContent=t;log.appendChild(d);log.scrollTop=log.scrollHeight;}
    function stat(t){if(status)status.textContent=t;}
    function act(a){
      if(!a||!a.type)return;
      S.lastAction=a.type;
      if(a.type==='open_preview'){
        S.preview=true; S.stage='preview'; S.score=Math.max(S.score,35);
        var e=document.getElementById('preview'); if(e)e.scrollIntoView({behavior:'smooth',block:'start'}); else location.href=C.previewUrl||'preview.html';
      }else if(a.type==='open_preview_page'){
        var p=Number(a.page); if(p>=1&&p<=P.preview)location.href=(C.previewUrl||'preview.html')+'#'+p;
      }else if(a.type==='focus_offer'){
        var o=document.querySelector('.offer'); if(o)o.scrollIntoView({behavior:'smooth',block:'center'});
      }else if(a.type==='open_checkout'){
        S.stage='checkout'; S.score=100; location.href=C.purchaseUrl||'checkout.html';
      }else if(a.type==='open_whatsapp'){
        location.href=C.whatsappUrl||'https://wa.me/249121851285';
      }
    }
    function qty(q){
      var m=q.match(/(?:^|\s)(\d{1,4})\s*(?:نسخه|نسخ|كتاب|كتب|copy|copies)/); if(m)return Math.max(1,Math.min(2000,Number(m[1])));
      if(/^(?:واحد|واحده|نسخه واحده|نسخه واحد)$/.test(q)||/\bواحده?\b/.test(q))return 1;
      return null;
    }
    function cur(q){if(/دولار|usd|\$/.test(q))return'USD';if(/جنيه|سوداني|sdg|ج\.س/.test(q))return'SDG';return'';}
    function total(n,c){var offer=Math.min(n,P.limit),reg=Math.max(0,n-P.limit);return c==='USD'?{t:offer*P.usd+reg*P.usdBase,s:offer*(P.usdBase-P.usd)}:{t:offer*P.sdg+reg*P.sdgBase,s:offer*(P.sdgBase-P.sdg)};}
    function money(n){return Number(n||0).toLocaleString('ar-EG');}
    function reply(text,actions){add(text,'done');S.history.push({role:'assistant',text:text});(actions||[]).forEach(act);stat('جاهز للخطوة التالية.');}
    function score(n){S.score=Math.min(100,S.score+n);}
    function whatsapp(){
      var c=S.currency==='USD'?'الدولار':S.currency==='SDG'?'الجنيه السوداني':'العملة غير محددة';
      var q=S.qty||'غير محدد';
      var msg='السلام عليكم، أرغب في شراء الموسوعة الشاملة في الذكاء الاصطناعي. عدد النسخ: '+q+'، العملة: '+c+'. أرجو إرسال خطوات الدفع والتسليم.';
      return (C.whatsappUrl||'https://wa.me/249121851285')+'?text='+encodeURIComponent(msg);
    }
    function buyActions(){return[{type:'open_checkout'}];}

    function local(text){
      var q=norm(text), n=qty(q), c=cur(q); if(!q)return true;
      if(S.intent==='purchase'){
        if(!S.qty&&n){S.qty=n;score(30);S.stage='currency';}
        if(!S.currency&&c){S.currency=c;score(25);S.stage='ready';}
        if(!S.qty){reply('ممتاز. لنكمل الطلب بسرعة. كم نسخة تريد؟ نسخة واحدة أم أكثر؟');return true;}
        if(!S.currency){reply('تمام. بقيت خطوة واحدة. تفضل الدفع بالجنيه السوداني أم بالدولار؟');return true;}
        var z=total(S.qty,S.currency),cn=S.currency==='USD'?'دولار':'جنيه سوداني',unit=S.currency==='USD'?P.usd:P.sdg;
        S.stage='ready';score(45);
        reply('تم تجهيز طلبك.\n\nعدد النسخ: '+S.qty+'\nسعر العرض: '+money(unit)+' '+cn+' للنسخة\nالإجمالي: '+money(z.t)+' '+cn+'\nالتوفير: '+money(z.s)+' '+cn+'\n\nإذا كنت جاهزاً، أفتح لك صفحة الدفع الآن.',buyActions());return true;
      }
      if(/السلام عليكم|^سلام$|مرحبا|اهلا|هاي|hello|hi/.test(q)){
        reply('أهلاً بك. أنا وكيل المبيعات في المنصة. قبل أن أرشح لك الشراء، أريد أن أفهم هدفك. هل تريد التعلم من الصفر، تطوير مهارتك، استخدام الموسوعة للدراسة أو التدريس، أم تريدها كمرجع عربي شامل؟');return true;
      }
      if(/اريد شراء|عايز اشتري|عايز شراء|جاهز للشراء|اشتري|ساشتري|أريد النسخه|عايز النسخه|عايز نسخه/.test(q)){
        S.intent='purchase';S.stage='purchase';score(45);if(n)S.qty=n;if(c)S.currency=c;return local('أريد الشراء');
      }
      if(/طالب|طالبه|جامعه|جامعي|دراسه/.test(q)){
        S.audience='student';S.goal='study';score(15);
        reply('بما أنك طالب، سأتعامل معك على أساس أنك تحتاج مرجعاً يمكنك الرجوع إليه أثناء الدراسة. لا أريد أن أدفعك للشراء قبل أن ترى المستوى. أفتح لك المعاينة الحقيقية ذات الـ20 صفحة؟',[{type:'open_preview'}]);return true;
      }
      if(/معلم|مدرس|استاذ|تدريس|مدرب/.test(q)){
        S.audience='teacher';S.goal='teaching';score(15);
        reply('إذا كان هدفك التدريس أو التدريب، فالمهم أن ترى مدى صلاحية المحتوى لاحتياجك. افتح المعاينة أولاً، وبعدها يمكنني مساعدتك في تقييم النسخة الكاملة.',[{type:'open_preview'}]);return true;
      }
      if(/مبتدئ|من الصفر|ما بعرف|لا اعرف|جديد في الذكاء|بدايه/.test(q)){
        S.audience='beginner';S.goal='learn';score(15);
        reply('إذا كنت تبدأ من الصفر، فالميزة الأساسية التي أريدك أن تختبرها هي ترتيب المعرفة من الأساسيات إلى الموضوعات الحديثة. لا تعتمد على كلامي. شاهد 20 صفحة حقيقية ثم قرر.',[{type:'open_preview'}]);return true;
      }
      if(/شركة|شركه|مؤسسه|مؤسسة|فريق|موظف|جماعي|جملة|جمله|عشرة|10|عشرون|20/.test(q)){
        S.audience='organization';S.goal='bulk';score(25);
        if(n){S.qty=n;S.intent='purchase';return local(text);}
        reply('إذا كان الطلب لمؤسسة أو مجموعة، أخبرني بعدد النسخ. سأحسب لك الإجمالي والتوفير مباشرة.');return true;
      }
      if(/عندي chatgpt|عندي شات|chatgpt|شات جي بي تي/.test(q)){
        S.objection='existing_ai';score(8);
        reply('وجود ChatGPT لا يلغي قيمة المرجع المنظم إذا كان هدفك التعلم. ChatGPT أداة تسألها، بينما الموسوعة تجمع مساراً معرفياً عربياً منظماً، ومع النسخة الكاملة يوجد وكيل مساعد. الأفضل أن تختبر المحتوى بنفسك من خلال المعاينة. هل أفتحها لك؟',[{type:'open_preview'}]);return true;
      }
      if(/يوتيوب|مجانا|مجاني|موجوده في الانترنت|المعلومات موجوده|الانترنت/.test(q)){
        S.objection='free';score(5);
        reply('صحيح. توجد معلومات مجانية كثيرة. أنا لا أطلب منك الدفع مقابل المعلومة المجردة. قيمة المنتج في جمع المحتوى وتنظيمه في مرجع عربي واحد، مع الوكيل المرفق بالنسخة الكاملة. شاهد المعاينة أولاً واحكم بنفسك.',[{type:'open_preview'}]);return true;
      }
      if(/غالي|غاليه|مكلف|ما عندي قروش|السعر كبير|ما عندي المبلغ|مرتفع/.test(q)){
        S.objection='price';score(10);
        reply('أتفهم اعتراضك. بدل أن أجادلك في السعر، خلينا نقلل المخاطرة. شاهد 20 صفحة حقيقية أولاً. إذا رأيت أن المحتوى والتنظيم والوكيل يستحقون السعر، أكمل معك الشراء. وإذا لم تقتنع، لا تدفع.',[{type:'open_preview'}]);return true;
      }
      if(/ثقه|اثق|موثوق|حقيقي|دليل|اثبات|ضمان/.test(q)){
        S.objection='trust';score(10);
        reply('أفضل دليل لك هو المنتج نفسه. الموقع يتيح لك 20 صفحة حقيقية قبل الدفع. يمكنك فحص المحتوى أولاً، ثم اتخاذ القرار بناءً على ما رأيته.');return true;
      }
      if(/تغنيني|يغنيني|بديل.*كورس|بديل.*دوره|هل.*الموسوعه.*الكورس|الموسوعه.*بدل.*الكورس/.test(q)){
        S.objection='course';score(12);
        reply('يمكن أن تغطي الموسوعة جانباً كبيراً من التعلم الذاتي، لكنها ليست بديلاً مضموناً عن كل كورس عملي أو كل مدرب. إذا كنت تريد مرجعاً عربياً منظماً للتعلم الذاتي، فهذه نقطة قوتها. شاهد المعاينة أولاً ثم قرر.',[{type:'open_preview'}]);return true;
      }
      if(/شنو.*الوكيل|ما.*الوكيل|ماذا.*يفعل.*الوكيل|فائده.*الوكيل|الوكيل.*بيعمل/.test(q)){
        reply('الوكيل الذكي مرفق مع النسخة الكاملة. دوره أن يساعدك في فهم موضوعات الموسوعة، شرح المصطلحات، الإجابة عن الأسئلة، وتوجيهك داخل المحتوى. وهو جزء من تجربة المنتج وليس بديلاً عن المعلم.');return true;
      }
      if(/تستحق|ليه اشتري|لماذا اشتري|شنو الفائده|فائدتها|ماذا استفيد|شنو بشتري/.test(q)){
        S.intent='interest';score(15);
        reply('إذا كان هدفك التعلم بالعربية، فأنت تحصل على مرجع منظم من 250 صفحة ومعه وكيل ذكي في النسخة الكاملة. لكنني لا أريد أن أقول لك إنها تستحق السعر دون أن تراها. أفتح لك المعاينة الآن؟',[{type:'open_preview'}]);return true;
      }
      if(/تجربه|اجرب|اشوف الاول|اشوف قبل|معاينه|20 صفحه|قبل الشراء/.test(q)){
        S.stage='preview';S.preview=true;score(25);
        reply('بكل سرور. شاهد المعاينة الحقيقية أولاً. بعد أن تراها، عد إلي وسأساعدك في تقييمها أو إكمال الشراء.',[{type:'open_preview'}]);return true;
      }
      if(/السعر|بكم|الثمن|تكلفه|الدفع/.test(q)){
        reply('النسخة الكاملة '+P.pages+' صفحة. السعر الأساسي '+money(P.sdgBase)+' جنيه سوداني. عرض أول '+P.limit+' نسخة '+money(P.sdg)+' جنيه للنسخة. وبالدولار '+P.usdBase+' دولار، أو '+P.usd+' دولار ضمن العرض. إذا أخبرتني بعدد النسخ والعملة أحسب لك الإجمالي والتوفير مباشرة.',[{type:'focus_offer'}]);return true;
      }
      if(/كيف ادفع|طريقة الدفع|بنكك|حساب|تحويل|ادفع/.test(q)){
        S.stage='checkout';score(30);
        reply('يمكنك متابعة تفاصيل الدفع من صفحة الشراء. إذا أردت، أفتحها لك الآن، وبعد الدفع ستجد خطوات إرسال إثبات التحويل والتسليم.',[{type:'open_checkout'}]);return true;
      }
      if(/واتساب|تواصل|رقم|موظف|بشر|انسان|انسان حقيقي/.test(q)){
        reply('يمكنك الانتقال مباشرة إلى واتساب والتواصل مع صاحب المنصة. إذا أردت أن أرسل لك المحادثة مع ملخص طلبك، أفتح واتساب الآن.',[{type:'open_whatsapp'}]);return true;
      }
      if(/نعم|ايوه|اوك|اوكي|تمام|موافق/.test(q)&&S.preview){
        S.intent='purchase';score(20);return local('أريد الشراء');
      }
      return false;
    }

    async function cloud(text){
      var api=String(C.agentApiUrl||'').trim(); if(!api)throw Error('no api');
      var context='أنت موظف مبيعات ومسوق محترف للموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية. مهمتك مساعدة الزائر على اتخاذ قرار شراء واعٍ، وليس الضغط عليه. لا تخترع سعراً أو ميزة أو شهادة أو ضماناً. البيانات الموثوقة: '+JSON.stringify({pages:P.pages,previewPages:P.preview,sdgBase:P.sdgBase,sdgOffer:P.sdg,usdBase:P.usdBase,usdOffer:P.usdOffer,limit:P.limit,agentIncluded:true})+'\nحالة المبيعات: '+JSON.stringify(S)+'\nسجل الحوار: '+S.history.slice(-8).map(function(x){return x.role+': '+x.text}).join('\n')+'\nرسالة الزائر: '+text+'\nقواعد: أجب مباشرة. اسأل سؤالاً واحداً فقط إذا كان ضرورياً. لا تكرر قائمة المزايا. عالج الاعتراض ثم اقترح خطوة واحدة. إذا ظهرت نية شراء، اجمع العدد والعملة. إذا كان متردداً، وجّه للمعاينة. إذا طلب الدفع، افتح صفحة الشراء. إذا طلب إنساناً، وجّه لواتساب. كن صادقاً بشأن كونك ذكاءً اصطناعياً.';
      var r=await fetch(api,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({message:context,context:{sales_state:S}})});
      var d=await r.json().catch(function(){return{};}); if(!r.ok)throw Error(d.message||'agent unavailable'); return d;
    }

    document.addEventListener('submit',function(e){
      if(e.target!==form)return; e.preventDefault(); e.stopImmediatePropagation();
      var text=input.value.trim(); if(!text)return; input.value=''; add('سؤال الزائر: '+text,'action'); S.history.push({role:'user',text:text});
      if(local(text))return;
      stat('أحلل احتياجك وأجهز الخطوة المناسبة...');
      cloud(text).then(function(d){var r=d.reply||'كيف أستطيع مساعدتك في اتخاذ القرار؟';reply(r,Array.isArray(d.actions)?d.actions:[]);}).catch(function(){
        reply('أستطيع مساعدتك في الشراء أو المعاينة. إذا كنت متردداً، ابدأ بالـ20 صفحة المجانية. وإذا كنت جاهزاً، أخبرني بعدد النسخ والعملة.');
      });
    },true);

    /* Proactive selling without blocking the visitor. */
    var welcomed=false;
    setTimeout(function(){
      if(welcomed||S.history.length)return;
      var offer=document.querySelector('.offer');
      if(!offer)return;
      add('إذا كنت تتصفح العرض الآن، يمكنني مساعدتك في اختيار الخطوة المناسبة: المعاينة، معرفة السعر، أو الشراء.','done');
      stat('أنا هنا للمساعدة في القرار.');
    },45000);
    window.addEventListener('beforeunload',function(){
      if(S.score>=45&&S.stage!=='checkout'){
        try{sessionStorage.setItem('sales_agent_state',JSON.stringify(S));}catch(e){}
      }
    });
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',start,{once:true});else start();
})();
