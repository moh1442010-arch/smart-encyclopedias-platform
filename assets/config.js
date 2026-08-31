window.STORE_CONFIG = {
  price: "130000",
  currency: "جنيه سوداني",
  oldPrice: "130000",
  offerTitle: "عرض لأول 100 نسخة",
  offerText: "السعر الأساسي 130,000 جنيه سوداني. عرض لأول 100 نسخة بخصم 10% بسعر 117,000 جنيه. للطلاب خصم ثابت 10% بسعر 117,000 جنيه مع إبراز بطاقة طالب أو بطاقة جامعية.",
  purchaseUrl: "checkout.html",
  accountNumber: "1882224",
  bankName: "بنك الخرطوم",
  iban: "SD8504018822240001",
  whatsapp: "+249121851285",
  whatsappUrl: "https://wa.me/249121851285",
  facebookUrl: "https://www.facebook.com/share/1BkZXmQTW6/",
  tiktokUrl: "https://www.tiktok.com/@mohammedmustafababiker",
  whatsappDisplay: "+249121851285",
  pages: 250,
  previewPages: 20,
  previewPdf: "الموسوعة_الشاملة_20_صفحة_للمعاينة.pdf",
  reservationOffer: "117000",
  studentPrice: "117000",
  studentDiscount: "10%",
  reservationDiscount: "10%",
  reservationLimit: 100,
  paymentMethod: "بنكك — بنك الخرطوم",
  foreignCurrencyEnabled: true,
  foreignCurrency: "USD",
  foreignCurrencyPrice: "19",
  foreignCurrencyDiscountedPrice: "16",
  foreignPaymentMethod: "Bank of Khartoum IBAN",
  foreignPaymentDetails: "SD8504018822240001",
  agentApiUrl: "https://super-rice-31e6.moh1442010.workers.dev/api/agent"
};

(function loadAgentAdapter(){
  function load(){
    if (!window.STORE_CONFIG.agentApiUrl || document.getElementById('agentApiAdapter')) return;
    var script = document.createElement('script');
    script.id = 'agentApiAdapter';
    script.src = 'assets/agent-api.js';
    script.defer = true;
    document.head.appendChild(script);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load);
  else load();
})();

(function loadQAFixes(){
  function load(){
    if (document.getElementById('qaFixes')) return;
    var script = document.createElement('script');
    script.id = 'qaFixes';
    script.src = 'assets/qa-fixes.js';
    script.defer = true;
    document.head.appendChild(script);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', load);
  else load();
})();

(function addPromotionLinks(){
  function render(){
    var footer = document.querySelector('footer .foot');
    if (!footer || document.getElementById('socialPromotionLinks')) return;
    var box = document.createElement('div');
    box.id = 'socialPromotionLinks';
    box.setAttribute('aria-label', 'روابط التواصل والترويج');
    box.style.cssText = 'display:flex;gap:10px;flex-wrap:wrap;align-items:center;justify-content:center;width:100%;margin-top:12px';
    var links = [
      ['Facebook', window.STORE_CONFIG.facebookUrl],
      ['TikTok', window.STORE_CONFIG.tiktokUrl],
      ['WhatsApp', window.STORE_CONFIG.whatsappUrl]
    ];
    links.forEach(function(item){
      var a = document.createElement('a');
      a.href = item[1];
      a.textContent = item[0];
      a.target = '_blank';
      a.rel = 'noopener noreferrer';
      a.style.cssText = 'padding:7px 12px;border:1px solid currentColor;border-radius:999px;text-decoration:none;font-size:14px';
      box.appendChild(a);
    });
    footer.appendChild(box);
  }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', render);
  else render();
})();
