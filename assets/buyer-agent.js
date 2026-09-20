(function(){
'use strict';
const API='https://super-rice-31e6.moh1442010.workers.dev/api/buyer-agent';
const DELIVERY='https://smart-encyclopedias-delivery.moh1442010.workers.dev';
const token=new URLSearchParams(location.hash.replace(/^#/,'?')).get('token');
const DEVICE_KEY='smart_encyclopedia_device_id_v1';
const status=document.getElementById('status'),log=document.getElementById('log'),form=document.getElementById('form'),input=document.getElementById('input');
function device(){try{let x=localStorage.getItem(DEVICE_KEY);if(!x){x=crypto.randomUUID()+'-'+crypto.randomUUID();localStorage.setItem(DEVICE_KEY,x)}return x}catch(e){return ''}}
const deviceId=device(),history=[];
function add(t,k){const d=document.createElement('div');d.className='item '+k;d.textContent=t;log.appendChild(d);log.scrollTop=log.scrollHeight}
async function verify(){
 if(!token||!deviceId){status.textContent='رابط الوكيل غير مكتمل.';return false}
 const r=await fetch(DELIVERY+'/api/delivery/info?token='+encodeURIComponent(token),{headers:{'x-device-id':deviceId},cache:'no-store'});
 const d=await r.json().catch(()=>({}));
 if(!r.ok||!d.ok||!d.active){status.textContent='تعذر فتح الوكيل: الترخيص غير فعّال أو مرتبط بجهاز آخر.';return false}
 status.textContent='تم التحقق من النسخة المرخّصة. يمكنك البدء.';
 log.hidden=false;form.hidden=false;add('أهلاً بك 🌷 أنا وكيلك المرفق بالنسخة المرخّصة. سأساعدك في الشرح والفهم والإجابة التعليمية.','bot');return true
}
async function ask(text){
 add(text,'user');history.push({role:'user',text});status.textContent='أفكر…';
 try{
  const r=await fetch(API,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({message:text,token,deviceId,history})});
  const d=await r.json().catch(()=>({}));
  if(!r.ok) throw new Error(d.error||'failed');
  const reply=d.reply||'لم أتمكن من الإجابة الآن.';add(reply,'bot');history.push({role:'assistant',text:reply});status.textContent='جاهز لسؤالك التالي.';
 }catch(e){status.textContent='تعذر الاتصال بالوكيل حالياً. تحقق من الاتصال ثم أعد المحاولة.'}
}
form.addEventListener('submit',e=>{e.preventDefault();const t=input.value.trim();if(!t)return;input.value='';ask(t)});
verify();
})();