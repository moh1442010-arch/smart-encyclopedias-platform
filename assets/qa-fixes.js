/* Final QA guardrails. Never introduce a second price source. */
(function(){
  'use strict';
  function run(){
    var c=window.STORE_CONFIG;if(!c)return;
    var old=document.getElementById('oldPrice'),price=document.getElementById('priceValue'),title=document.getElementById('offerTitle'),text=document.getElementById('offerText'),account=document.getElementById('accountNumber'),wa=document.getElementById('whatsappNumber'),link=document.getElementById('whatsappLink');
    if(old)old.textContent='السعر الأساسي '+Number(c.price).toLocaleString('ar-EG')+' جنيه سوداني';
    if(price)price.textContent=Number(c.reservationOffer).toLocaleString('ar-EG')+' جنيه سوداني';
    if(title)title.textContent=c.offerTitle;
    if(text)text.textContent=c.offerText+' ومع كل نسخة كاملة تحصل على وكيل ذكي للمساعدة في الشرح والفهم.';
    if(account)account.textContent=c.accountNumber;
    if(wa)wa.textContent=c.whatsappDisplay||c.whatsapp;
    if(link)link.href=c.whatsappUrl;
    var box=document.querySelector('.payment-box');if(box&&!document.getElementById('agentIncludedNotice')){var d=document.createElement('div');d.id='agentIncludedNotice';d.innerHTML='<b>وكيل ذكي مع النسخة:</b> مرفق مع كل نسخة كاملة للمساعدة في الشرح والفهم والإجابة عن الأسئلة.';box.appendChild(d)}
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run);else run();
})();
