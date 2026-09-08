(function(){
"use strict";
function init(){
 if(window.__SMART_AGENT_V2)return;
 window.__SMART_AGENT_V2=true;
 const C=window.STORE_CONFIG||{};
 const api=String(C.agentApiUrl||"https://smart-encyclopedias-platform.moh1442010.workers.dev/api/agent").trim();
 const form=document.getElementById("agentForm"),input=document.getElementById("agentInput"),log=document.getElementById("agentLog"),status=document.getElementById("agentStatus");
 if(!form||!input||!log)return;
 const add=(text,type)=>{const d=document.createElement("div");d.className="agent-item "+(type||"");d.textContent=text;log.appendChild(d);log.scrollTop=log.scrollHeight};
 const setStatus=t=>{if(status)status.textContent=t};
 const action=a=>{if(!a||!a.type)return;switch(a.type){case"open_preview":document.getElementById("preview")?.scrollIntoView({behavior:"smooth"});break;case"open_preview_page":{const p=Number(a.page);if(p>=1&&p<=20)window.open((C.previewUrl||"preview.html")+"#"+p,"_blank","noopener,noreferrer");break}case"open_whatsapp":window.open(C.whatsappUrl||"https://wa.me/249121851285","_blank","noopener,noreferrer");break;case"focus_offer":document.querySelector(".offer")?.scrollIntoView({behavior:"smooth",block:"center"});break;case"open_checkout":window.location.href=C.purchaseUrl||"checkout.html";break}};
 form.addEventListener("submit",async e=>{e.preventDefault();e.stopImmediatePropagation();const text=input.value.trim();if(!text)return;input.value="";add("طلب المستخدم: "+text,"action");setStatus("جارٍ الاتصال بالوكيل...");try{const r=await fetch(api,{method:"POST",headers:{"content-type":"application/json"},body:JSON.stringify({message:text,context:{page:location.pathname}})});const data=await r.json().catch(()=>({}));if(!r.ok)throw Error(data.message||"agent unavailable");add(data.reply||"تمت معالجة الطلب.","done");(Array.isArray(data.actions)?data.actions:[]).forEach(action);setStatus("تمت الإجابة. يمكنك إرسال سؤال جديد.")}catch(err){const local=window.PLATFORM_ASSISTANT?.answer?.(text);add(local||"تعذر الاتصال بالوكيل السحابي الآن.","done");setStatus("الوضع المحلي يعمل.")}},true);
}
if(document.readyState==="loading")document.addEventListener("DOMContentLoaded",init,{once:true});else init();
})();
