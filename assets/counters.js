/* Live storefront counters + SEO enhancements. */
(function(){
  'use strict';
  if(window.__STORE_COUNTERS__) return;
  window.__STORE_COUNTERS__ = true;

  var NS='moh1442010-arch.github.io';
  var API='https://counterapi.com/api/';
  var WIDGET='https://counterapi.com/c.js?ns='+encodeURIComponent(NS);

  function addSeoSignals(){
    var path=location.pathname.toLowerCase();
    var home=/\/$/.test(path)||/\/index\.html$/.test(path);
    var transactional=/\/(checkout|complaints|payment-review|delivery|admin)\.html$/.test(path);

    if(transactional){
      var meta=document.querySelector('meta[name="robots"]');
      if(!meta){
        meta=document.createElement('meta');
        meta.name='robots';
        document.head.appendChild(meta);
      }
      meta.content='noindex, nofollow, noarchive';
    }

    if(!home) return;

    function addMeta(attr,name,content){
      if(!content || document.querySelector('meta['+attr+'="'+name+'"]')) return;
      var m=document.createElement('meta');
      m.setAttribute(attr,name); m.content=content; document.head.appendChild(m);
    }
    addMeta('property','og:type','website');
    addMeta('property','og:title','الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية | محمد مصطفى بابكر');
    addMeta('property','og:description','موسوعة عربية شاملة في الذكاء الاصطناعي من الصفر إلى الاحتراف: 260 صفحة، معاينة حقيقية من 20 صفحة، ووكيل ذكي مع النسخة الكاملة.');
    addMeta('property','og:url','https://moh1442010-arch.github.io/smart-encyclopedias-platform/');
    addMeta('property','og:locale','ar_AR');
    addMeta('name','twitter:card','summary_large_image');
    addMeta('name','twitter:title','الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية');
    addMeta('name','twitter:description','من الصفر إلى الاحتراف في الذكاء الاصطناعي — 260 صفحة ومعاينة حقيقية ووكيل ذكي.');

    if(!document.getElementById('seoStructuredData')){
      var s=document.createElement('script');
      s.id='seoStructuredData'; s.type='application/ld+json';
      s.textContent=JSON.stringify({
        '@context':'https://schema.org',
        '@graph':[
          {'@type':'Organization','@id':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/#organization','name':'منصة الموسوعات الذكية','url':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/','founder':{'@type':'Person','name':'محمد مصطفى بابكر'}},
          {'@type':'WebSite','@id':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/#website','url':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/','name':'منصة الموسوعات الذكية','inLanguage':'ar','publisher':{'@id':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/#organization'}},
          {'@type':'Product','@id':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/#product','name':'الموسوعة الشاملة في الذكاء الاصطناعي باللغة العربية','description':'موسوعة عربية من 260 صفحة في الذكاء الاصطناعي والتعلم الآلي وLLMs وRAG وهندسة الأوامر والوكلاء والذكاء متعدد الوسائط، مع وكيل ذكي للنسخة الكاملة.','brand':{'@type':'Brand','name':'منصة الموسوعات الذكية'},'author':{'@type':'Person','name':'محمد مصطفى بابكر'},'offers':{'@type':'Offer','url':'https://moh1442010-arch.github.io/smart-encyclopedias-platform/checkout.html','priceCurrency':'SDG','price':'120000','availability':'https://schema.org/InStock'}}
        ]
      });
      document.head.appendChild(s);
    }
  }

  function addStyle(){
    if(document.getElementById('storeCountersStyle')) return;
    var s=document.createElement('style'); s.id='storeCountersStyle';
    s.textContent='.store-counters{display:flex;gap:10px;flex-wrap:wrap;align-items:center;justify-content:center;margin:14px auto;padding:0 12px}.store-counter{min-width:150px;min-height:52px;padding:9px 14px;border-radius:14px;background:#07182f;color:#fff;box-shadow:0 8px 24px #0002;text-align:center;font-size:13px}.store-counter b{display:block;font-size:20px;line-height:1.2;margin-bottom:3px}.store-counter small{opacity:.85}.store-counter.online{border:1px solid #35c978}.store-counter.orders{background:#142033}.store-counters-note{width:100%;text-align:center;font-size:11px;opacity:.65;margin-top:2px}';
    document.head.appendChild(s);
  }

  function addCounters(){
    if(document.getElementById('storeCounters')) return;
    addStyle();
    var host=document.querySelector('.hero')||document.querySelector('main')||document.body;
    var box=document.createElement('section'); box.id='storeCounters'; box.className='store-counters'; box.setAttribute('aria-label','إحصاءات المنصة');
    box.innerHTML='<div class="store-counter"><b class="sc-visitors">—</b><span>إجمالي الزوار الفريدين</span></div><div class="store-counter online"><b class="sc-online">—</b><span>زوار نشطون خلال 30 دقيقة</span></div><div class="store-counter orders"><b class="sc-orders">—</b><span>طلبات شراء مسجلة</span></div><div class="store-counters-note">العدادات إحصاءات تشغيلية؛ طلب الشراء المسجل لا يعني أن الدفع تم تأكيده.</div>';
    if(host&&host.parentNode) host.parentNode.insertBefore(box,host.nextSibling); else document.body.appendChild(box);
  }

  function read(url,cb){fetch(url,{cache:'no-store'}).then(function(r){return r.ok?r.json():null}).then(cb).catch(function(){cb(null)});}
  function loadCounterLibrary(){if(document.getElementById('counterApiLibrary'))return;var s=document.createElement('script');s.id='counterApiLibrary';s.src=WIDGET;s.async=true;document.head.appendChild(s);}
  function loadGabsterOnCheckout(){if(!/checkout\.html$/i.test(location.pathname))return;if(document.querySelector('script[data-gabster-widget]'))return;var s=document.createElement('script');s.setAttribute('data-gabster-widget','');s.setAttribute('data-embed-type','widget');s.src='https://widget.gabster.ai/loader?cbid=6aa587c39ec56a4287e9c161';s.async=true;document.body.appendChild(s);}
  function refresh(){var v=document.querySelector('.sc-visitors'),o=document.querySelector('.sc-online'),p=document.querySelector('.sc-orders');if(!v||!o||!p)return;var base=API+encodeURIComponent(NS)+'/view/any';read(base+'?readOnly=true&unique=true&timeline=total',function(d){if(d&&d.value!=null)v.textContent=Number(d.value).toLocaleString('ar-EG')});read(base+'?readOnly=true&unique=true&timeline=30m',function(d){if(d&&d.value!=null)o.textContent=Number(d.value).toLocaleString('ar-EG')});read(API+encodeURIComponent(NS)+'/order_submitted/any?readOnly=true&timeline=total',function(d){if(d&&d.value!=null)p.textContent=Number(d.value).toLocaleString('ar-EG')});}
  function trackCurrentVisit(){fetch(API+encodeURIComponent(NS)+'/view/'+encodeURIComponent(location.pathname),{cache:'no-store'}).catch(function(){});var src=new URLSearchParams(location.search).get('src');if(src){src=src.replace(/[^a-zA-Z0-9_-]/g,'').slice(0,80);if(src)trackAction('campaign_visit',src);}}
  function trackAction(action,key){if(!action)return;fetch(API+encodeURIComponent(NS)+'/'+encodeURIComponent(action)+'/'+encodeURIComponent(key||'site')+'?trackOnly=true',{cache:'no-store'}).catch(function(){});}
  function bindSalesFunnel(){document.querySelectorAll('[data-track]').forEach(function(el){if(el.dataset.counterTracked)return;el.dataset.counterTracked='1';el.addEventListener('click',function(){trackAction(el.dataset.track,'site');});});document.querySelectorAll('[data-open-agent]').forEach(function(el){if(el.dataset.counterAgent)return;el.dataset.counterAgent='1';el.addEventListener('click',function(){trackAction('agent_open','site');});});document.querySelectorAll('[data-open-chat]').forEach(function(el){if(el.dataset.counterChat)return;el.dataset.counterChat='1';el.addEventListener('click',function(){trackAction('chat_open','site');});});}
  function observeOrderSuccess(){var originalFetch=window.fetch;if(!originalFetch||window.__ORDER_COUNTER_PATCHED__)return;window.__ORDER_COUNTER_PATCHED__=true;window.fetch=function(){var args=arguments;return originalFetch.apply(this,args).then(function(response){try{var input=args[0];var url=typeof input==='string'?input:(input&&input.url)||'';if(url.indexOf('/api/order')!==-1&&response.ok){response.clone().json().then(function(data){if(data&&data.ok){fetch(API+encodeURIComponent(NS)+'/order_submitted/encyclopedia',{cache:'no-store'}).catch(function(){});setTimeout(refresh,700);}}).catch(function(){})}}catch(e){}return response;});};}
  function init(){addSeoSignals();addCounters();trackCurrentVisit();bindSalesFunnel();loadCounterLibrary();observeOrderSuccess();loadGabsterOnCheckout();setTimeout(refresh,900);setInterval(refresh,60000);}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
