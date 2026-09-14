/* Live storefront counters: visitors, active guests, and recorded order submissions. */
(function(){
  'use strict';
  if(window.__STORE_COUNTERS__) return;
  window.__STORE_COUNTERS__ = true;

  var NS = 'moh1442010-arch.github.io';
  var API = 'https://counterapi.com/api/';
  var WIDGET = 'https://counterapi.com/c.js?ns=' + encodeURIComponent(NS);

  function addStyle(){
    if(document.getElementById('storeCountersStyle')) return;
    var s=document.createElement('style');
    s.id='storeCountersStyle';
    s.textContent='.store-counters{display:flex;gap:10px;flex-wrap:wrap;align-items:center;justify-content:center;margin:14px auto;padding:0 12px}.store-counter{min-width:150px;min-height:52px;padding:9px 14px;border-radius:14px;background:#07182f;color:#fff;box-shadow:0 8px 24px #0002;text-align:center;font-size:13px}.store-counter b{display:block;font-size:20px;line-height:1.2;margin-bottom:3px}.store-counter small{opacity:.85}.store-counter.online{border:1px solid #35c978}.store-counter.orders{background:#142033}.store-counters-note{width:100%;text-align:center;font-size:11px;opacity:.65;margin-top:2px}';
    document.head.appendChild(s);
  }

  function addCounters(){
    if(document.getElementById('storeCounters')) return;
    addStyle();
    var host=document.querySelector('.hero') || document.querySelector('main') || document.body;
    var box=document.createElement('section');
    box.id='storeCounters';
    box.className='store-counters';
    box.setAttribute('aria-label','إحصاءات المنصة');
    box.innerHTML='<div class="store-counter"><b class="sc-visitors">—</b><span>زائر للموقع</span></div>'+
      '<div class="store-counter online"><b class="sc-online">—</b><span>ضيف الآن</span></div>'+
      '<div class="store-counter orders"><b class="sc-orders">—</b><span>طلبات شراء مسجلة</span></div>'+
      '<div class="store-counters-note">الأعداد تُحدّث تلقائياً وليست بديلاً عن سجل المبيعات المؤكد.</div>';
    if(host && host.parentNode) host.parentNode.insertBefore(box,host.nextSibling); else document.body.appendChild(box);
  }

  function read(url, cb){
    fetch(url,{cache:'no-store'}).then(function(r){return r.ok?r.json():null}).then(cb).catch(function(){cb(null)});
  }

  function loadCounterLibrary(){
    if(document.getElementById('counterApiLibrary')) return;
    var s=document.createElement('script');
    s.id='counterApiLibrary';
    s.src=WIDGET;
    s.async=true;
    document.head.appendChild(s);
  }

  function refresh(){
    var v=document.querySelector('.sc-visitors'),o=document.querySelector('.sc-online'),p=document.querySelector('.sc-orders');
    if(!v||!o||!p) return;
    var base=API+encodeURIComponent(NS)+'/view/any';
    read(base+'?readOnly=true&unique=true&timeline=total',function(d){if(d&&d.value!=null)v.textContent=Number(d.value).toLocaleString('ar-EG')});
    read(base+'?readOnly=true&unique=true&timeline=30m',function(d){if(d&&d.value!=null)o.textContent=Number(d.value).toLocaleString('ar-EG')});
    read(API+encodeURIComponent(NS)+'/order_submitted/any?readOnly=true&timeline=total',function(d){if(d&&d.value!=null)p.textContent=Number(d.value).toLocaleString('ar-EG')});
  }

  function trackCurrentVisit(){
    /* One page-view event per page load. The service anonymizes browser/IP data. */
    fetch(API+encodeURIComponent(NS)+'/view/'+encodeURIComponent(location.pathname),{cache:'no-store'}).catch(function(){});
  }

  function observeOrderSuccess(){
    var originalFetch=window.fetch;
    if(!originalFetch || window.__ORDER_COUNTER_PATCHED__) return;
    window.__ORDER_COUNTER_PATCHED__=true;
    window.fetch=function(){
      var args=arguments;
      return originalFetch.apply(this,args).then(function(response){
        try{
          var input=args[0];
          var url=typeof input==='string'?input:(input&&input.url)||'';
          if(url.indexOf('/api/order')!==-1 && response.ok){
            response.clone().json().then(function(data){
              if(data&&data.ok){
                fetch(API+encodeURIComponent(NS)+'/order_submitted/encyclopedia',{cache:'no-store'}).catch(function(){});
                setTimeout(refresh,700);
              }
            }).catch(function(){});
          }
        }catch(e){}
        return response;
      });
    };
  }

  function init(){
    addCounters();
    trackCurrentVisit();
    loadCounterLibrary();
    observeOrderSuccess();
    setTimeout(refresh,900);
    setInterval(refresh,60000);
  }

  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',init); else init();
})();
