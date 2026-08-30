window.STORE_CONFIG = {
  price: "150000",
  currency: "جنيه سوداني",
  oldPrice: "",
  offerTitle: "عرض أسبقية الحجز لأول 200 نسخة",
  offerText: "السعر الأساسي 150,000 جنيه سوداني. عرض أسبقية الحجز بخصم 25% لأول 200 نسخة بسعر 112,500 جنيه. للطلاب خصم ثابت 30% بسعر 105,000 جنيه مع إبراز بطاقة طالب أو بطاقة جامعية.",
  purchaseUrl: "https://wa.me/249121851285",
  accountNumber: "1882224",
  whatsapp: "+249121851285",
  whatsappUrl: "https://wa.me/249121851285",
  facebookUrl: "https://www.facebook.com/share/1BkZXmQTW6/",
  tiktokUrl: "https://www.tiktok.com/@mohammedmustafababiker",
  whatsappDisplay: "+249121851285",
  pages: 250,
  previewPages: 20,
  previewPdf: "الموسوعة_الشاملة_20_صفحة_للمعاينة.pdf",
  reservationOffer: "112500",
  studentPrice: "105000",
  studentDiscount: "30%",
  reservationDiscount: "25%",
  reservationLimit: 200,
  paymentMethod: "بنكك",
  agentApiUrl: ""
};

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
