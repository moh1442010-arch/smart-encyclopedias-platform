(function(){
"use strict";
function init(){
 if(window.__SMART_AGENT_V2)return;
 window.__SMART_AGENT_V2=true;
 const C=window.STORE_CONFIG||{};
 const api=String(C.agentApiUrl||"https://super-rice-31e6.moh1442010.workers.dev/api/agent").trim();
 const form=document.getElementById("agentForm"),input=document.getElementById("agentInput"),log=document.getElementById("agentLog"),status=document.getElementById("agentStatus");
 if(!form||!input||!log)return;
 const add=(text,type)=>{const d=document.createElement("div");d.className="agent-item "+(type||"");d.textContent=text;log.appendChild(d);log.scrollTop=log.scrollHeight};
 const setStatus=t=>{if(status)status.textContent=t};
 const action=a=>{if(!a||!a.type)return;switch(a.type){case"open_preview":document.getElementById("preview")?.scrollIntoView({behavior:"smooth"});break;case"open_preview_page":{const p=Number(a.page);if(p>=1&&p<=20)window.open((C.previewUrl||"preview.html")+"#"+p,"_blank","noopener,noreferrer");break}case"open_whatsapp":window.open(C.whatsappUrl||"https://wa.me/249121851285","_blank","noopener,noreferrer");break;case"focus_offer":document.querySelector(".offer")?.scrollIntoView({behavior:"smooth",block:"center"});break;case"open_checkout":window.location.href=C.purchaseUrl||"checkout.html";break}};
 const localSales=(raw)=>{const q=String(raw||"").trim().toLowerCase();
  if(!q)return null;
  if(/(تغنيني|يغنيني|بديل.*كورس|بديل.*دورة|بدون.*كورس|هل.*الموسوعة.*الكورس|الموسوعة.*بدل.*الكورس)/.test(q))return {reply:"نعم، الموسوعة مصممة لتغطي رحلة التعلم بصورة متدرجة من الأساسيات إلى الموضوعات المتقدمة والتطبيقات، ويمكن أن تغطي جزءاً كبيراً من التعلم الذاتي. لكنها لا تدّعي أنها بديل عن كل كورس عملي أو مدرب؛ قيمة الموسوعة أنها مرجع شامل ترجع إليه في أي وقت، ومع النسخة الكاملة تحصل أيضاً على وكيل ذكي للمساعدة في الشرح والإجابة عن الأسئلة. الأفضل أن تختبر الجودة بنفسك من خلال 20 صفحة حقيقية قبل القرار. هل أفتح لك المعاينة الآن؟",actions:[{type:"open_preview"}]};
  if(/(غالي|غالية|السعر.*غالي|مبلغ.*كبير|ما.*عندي.*قروش|مكلف|تكلفة)/.test(q))return {reply:"أتفهمك تماماً 👍 لذلك لا أطلب منك الشراء قبل أن ترى القيمة بنفسك. النسخة الكاملة 250 صفحة، ومعها الوكيل الذكي، والعرض الحالي لأول 200 نسخة يخفض السعر من 150,000 إلى 120,000 جنيه سوداني، أو من 19 إلى 16 دولاراً. ويمكنك أولاً مشاهدة 20 صفحة حقيقية من المعاينة. إذا وجدت أن المحتوى يستحق، أوجهك مباشرة للشراء.",actions:[{type:"focus_offer"},{type:"open_preview"}]};
  if(/(السر|سر.*الموسوعة|ما.*السر|شي.*سري)/.test(q))return {reply:"لا يوجد سر سحري أو معلومة مخفية أبيعها لك. القيمة في تنظيم المعرفة العربية من الأساسيات إلى التطبيقات، وتجميع موضوعات الذكاء الاصطناعي الحديثة في مرجع واحد، مع وكيل ذكي يساعدك على الفهم. وأفضل دليل هو أن ترى 20 صفحة حقيقية بنفسك قبل أن تدفع.",actions:[{type:"open_preview"}]};
  if(/(تجربة|اجرب|أجرب|عايز.*اشوف|أريد.*اشوف|قبل.*الشراء|أشوف.*الأول)/.test(q))return {reply:"بالتأكيد 👍 عندك معاينة حقيقية من 20 صفحة قبل الشراء. شاهد المحتوى بنفسك، وإذا اقتنعت بالجودة أفتح لك صفحة الشراء مباشرة.",actions:[{type:"open_preview"}]};
  if(/(أريد شراء|اريد شراء|عايز اشتري|عايز شراء|جاهز.*شراء|سأشتري|اشتري الآن|اشترى)/.test(q))return {reply:"ممتاز 🌟 لنطوّل عليك. النسخة الكاملة 250 صفحة ومعها الوكيل الذكي، والعرض الحالي لأول 200 نسخة بسعر 120,000 جنيه سوداني أو 16 دولاراً. أفتح لك صفحة الشراء الآن لإكمال الطلب.",actions:[{type:"open_checkout"}]};
  if(/(وكيل.*ذكي|الوكيل.*بيعمل|ماذا.*يفعل.*الوكيل|فائدة.*الوكيل)/.test(q))return {reply:"الوكيل الذكي جزء من النسخة الكاملة، ودوره أن يكون مساعدك بعد الشراء: يشرح المفاهيم، يبسط المصطلحات، يجيب عن الأسئلة، ويساعدك على معرفة من أين تبدأ. الفكرة ليست مجرد كتاب، بل مرجع + مساعد تعليمي.",actions:[]};
  if(/(سعر|كم.*الموسوعة|بكم|الثمن|الدفع)/.test(q))return {reply:"السعر الأساسي 150,000 جنيه سوداني، والعرض الحالي لأول 200 نسخة 120,000 جنيه. وبالدولار 19 دولاراً، أو 16 دولاراً ضمن العرض. النسخة الكاملة 250 صفحة ومعها الوكيل الذكي، ويمكنك معاينة 20 صفحة حقيقية قبل الشراء.",actions:[{type:"focus_offer"}]};
  return null;
 };
 form.addEventListener("submit",async e=>{e.preventDefault();e.stopImmediatePropagation();const text=input.value.trim();if(!text)return;input.value="";add("طلب المستخدم: "+text,"action");setStatus("جارٍ تجهيز أفضل إجابة...");
  const local=localSales(text);
  if(local){add(local.reply,"done");(local.actions||[]).forEach(action);setStatus("تمت الإجابة. يمكنك إرسال سؤال جديد.");return;}
  setStatus("جارٍ الاتصال بالوكيل...");
  try{const r=await fetch(api,{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({message:text,context:{page:location.pathname}})});const data=await r.json().catch(()=>({}));if(!r.ok)throw Error(data.message||"agent unavailable");add(data.reply||"تمت معالجة الطلب.","done");(Array.isArray(data.actions)?data.actions:[]).forEach(action);setStatus("تمت الإجابة. يمكنك إرسال سؤال جديد")}
  catch(err){const localFallback=window.PLATFORM_ASSISTANT?.answer?.(text);add(localFallback||"تعذر الاتصال بالوكيل السحابي الآن. يمكنك تجربة المعاينة أو التواصل عبر واتساب.","done");setStatus("الوضع المحلي يعمل.")}
 },true);
}
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",init,{once:true});else init();
})();
