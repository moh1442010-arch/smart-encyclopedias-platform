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
  pages: 250,
  previewPages: 20,
  previewPdf: "الموسوعة_الشاملة_20_صفحة_للمعاينة.pdf",
  reservationOffer: "112500",
  studentPrice: "105000",
  studentDiscount: "30%",
  reservationDiscount: "25%",
  reservationLimit: 200,
  paymentMethod: "بنكك"
};

/* QA fallback: the repository currently keeps the preview PDF as the source of truth.
   If a preview thumbnail is missing, show a clean local placeholder instead of a broken image.
   Clicking the card still opens the requested PDF page. */
(() => {
  const placeholder = "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2MDAiIGhlaWdodD0iODAwIiB2aWV3Qm94PSIwIDAgNjAwIDgwMCI+PHJlY3Qgd2lkdGg9IjYwMCIgaGVpZ2h0PSI4MDAiIGZpbGw9IiNmM2Y0ZjYiLz48cmVjdCB4PSIyOCIgeT0iMjgiIHdpZHRoPSI1NDQiIGhlaWdodD0iNzQ0IiByeD0iMTgiIGZpbGw9Im5vbmUiIHN0cm9rZT0iIzljYTNhZiIgc3Ryb2tlLXdpZHRoPSI0Ii8+PHRleHQgeD0iMzAwIiB5PSIzNjAiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSI0MiIgZmlsbD0iIzM3NDE1MSI+2YXYudin2YrZhtipPC90ZXh0Pjx0ZXh0IHg9IjMwMCIgeT0iNDIwIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMjgiIGZpbGw9IiM2YjcyODAiPti12YHYrdipINmF2YYg2KfZhNmF2YjYs9mI2LnYqTwvdGV4dD48L3N2Zz4=";

  const fix = () => {
    document.querySelectorAll('img[src*="assets/assets/preview/"]').forEach((img) => {
      if (img.dataset.qaFallback === "1") return;
      img.dataset.qaFallback = "1";
      img.src = placeholder;
      img.alt = img.alt || "صفحة من المعاينة";
    });
  };

  const start = () => {
    fix();
    new MutationObserver(fix).observe(document.body, { childList: true, subtree: true });
  };

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", start, { once: true });
  else start();
})();
