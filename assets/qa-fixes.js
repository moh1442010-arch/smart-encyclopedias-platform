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
    document.querySelectorAll('a[href*="wa.me"]').forEach(function(a){
      if(a.href.indexOf('%D8%A7%D9%84%D8%B0%D9%83%D8%A7%D8%A1%20%D8%A7%D9%84%D8%B0%D9%83%D8%A7%D8%A1%20%D8%A7%D9%84%D8%A7%D8%B5%D8%B7%D9%86%D8%A7%D8%B9%D9%8A')!==-1){
        a.href=a.href.replace(/%D8%A7%D9%84%D8%B0%D9%83%D8%A7%D8%A1%20%D8%A7%D9%84%D8%B0%D9%83%D8%A7%D8%A1%20%D8%A7%D9%84%D8%A7%D8%B5%D8%B7%D9%86%D8%A7%D8%B9%D9%8A/g,'%D8%A7%D9%84%D8%B0%D9%83%D8%A7%D8%A1%20%D8%A7%D9%84%D8%A7%D8%B5%D8%B7%D9%86%D8%A7%D8%B9%D9%8A');
      }
    });
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
    if(!document.getElementById('studentPricingNotice')){
      var host=document.querySelector('.checkout-card')||document.querySelector('main')||document.body;
      if(host){
        var n=document.createElement('div');n.id='studentPricingNotice';n.style.cssText='margin:18px 0;padding:14px 16px;border:1px solid #d8b24c;border-radius:14px;background:#fffaf0;line-height:1.9';
        n.innerHTML='<b>سعر الطالب الثابت:</b> بعد إثبات صفة الطالب يكون السعر 120,000 جنيه سوداني أو 16 دولاراً، وهو سعر ثابت لا يرتبط بانتهاء عرض أول 200 نسخة، ولا يُجمع مع خصم آخر.';
        host.appendChild(n);
      }
    }
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run);else run();
})();
