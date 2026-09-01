/* Final storefront QA guardrails and release links. */
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
    var box=document.querySelector('.payment-box');
    if(box&&!document.getElementById('agentIncludedNotice')){
      var d=document.createElement('div');d.id='agentIncludedNotice';d.innerHTML='<b>وكيل ذكي مع النسخة:</b> مرفق مع كل نسخة كاملة للمساعدة في الشرح والفهم والإجابة عن الأسئلة.';box.appendChild(d);
    }
    var nav=document.querySelector('nav');
    if(nav&&!document.getElementById('updatesNavLink')){
      var a=document.createElement('a');a.id='updatesNavLink';a.href='updates.html';a.textContent='التحديثات';nav.appendChild(a);
    }
    var features=document.getElementById('features');
    if(features&&!document.getElementById('learningPathNotice')){
      var card=document.createElement('div');card.id='learningPathNotice';card.style.cssText='margin-top:18px;padding:16px 18px;border:1px solid #29445e;border-radius:16px;background:#0b2137;color:#d6e2ed;line-height:1.9';
      card.innerHTML='<b>مسار ما بعد الشراء</b><br>الموسوعة الكاملة + الوكيل الذكي + التحديات + الامتحان النهائي + تقدير المستوى + شهادة بعد الاجتياز + مركز التحديثات.';
      features.querySelector('.wrap')?.appendChild(card);
    }
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run);else run();
})();
