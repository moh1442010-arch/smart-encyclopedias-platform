document.addEventListener('DOMContentLoaded', function () {
  'use strict';

  var C = window.STORE_CONFIG || {};
  var localPrice = C.price || '130000';
  var localOffer = C.reservationOffer || '117000';
  var localDiscount = C.reservationDiscount || '10%';
  var localStudent = C.studentPrice || '117000';
  var localStudentDiscount = C.studentDiscount || '10%';
  var limit = C.reservationLimit || 100;
  var currency = C.currency || 'جنيه سوداني';
  var usd = C.foreignCurrencyPrice || '19';
  var usdOffer = C.foreignCurrencyDiscountedPrice || '16';
  var iban = C.iban || 'SD8504018822240001';
  var bank = C.bankName || 'بنك الخرطوم';
  var whatsapp = C.whatsappUrl || '#';

  function make(tag, text) {
    var el = document.createElement(tag);
    if (text !== undefined) el.textContent = text;
    return el;
  }

  function normalizeOfferText() {
    var title = document.getElementById('offerTitle');
    var text = document.getElementById('offerText');
    var oldPrice = document.getElementById('oldPrice');
    var priceValue = document.getElementById('priceValue');
    if (title) title.textContent = 'عرض لأول ' + limit + ' نسخة';
    if (text) text.textContent = 'السعر الأساسي ' + localPrice + ' ' + currency + '. عرض لأول ' + limit + ' نسخة بخصم ' + localDiscount + ' بسعر ' + localOffer + ' ' + currency + '. للطلاب خصم ثابت ' + localStudentDiscount + ' بسعر ' + localStudent + ' ' + currency + ' مع إبراز بطاقة طالب أو بطاقة جامعية.';
    if (oldPrice) oldPrice.textContent = 'السعر الأساسي ' + localPrice + ' ' + currency;
    if (priceValue) priceValue.textContent = localOffer + ' ' + currency;

    var offer = document.querySelector('.offer');
    if (offer) {
      var rows = offer.querySelectorAll('.payment-box > div');
      if (rows.length >= 6) {
        rows[0].textContent = 'السعر الأساسي: ' + localPrice + ' ' + currency;
        rows[1].textContent = 'عرض أول ' + limit + ' نسخة: ' + localOffer + ' ' + currency;
        rows[2].textContent = 'سعر الطلاب: ' + localStudent + ' ' + currency + ' بعد خصم ' + localStudentDiscount;
        rows[3].textContent = 'طريقة الدفع: بنكك — بنك الخرطوم';
      }
    }

    document.querySelectorAll('#faq details p').forEach(function (p) {
      if (p.textContent.indexOf('150,000') !== -1 || p.textContent.indexOf('112,500') !== -1 || p.textContent.indexOf('25%') !== -1 || p.textContent.indexOf('30%') !== -1 || p.textContent.indexOf('200 نسخة') !== -1) {
        p.textContent = 'السعر الأساسي ' + localPrice + ' ' + currency + '. عرض أول ' + limit + ' نسخة بسعر ' + localOffer + ' ' + currency + ' بعد خصم ' + localDiscount + '. للطلاب خصم ثابت ' + localStudentDiscount + ' بسعر ' + localStudent + ' ' + currency + '.';
      }
    });
  }

  function addInternationalPayment() {
    if (document.getElementById('internationalPaymentBox')) return;
    var paymentBox = document.querySelector('.payment-box');
    if (!paymentBox) return;

    var box = make('div');
    box.id = 'internationalPaymentBox';
    box.style.cssText = 'margin-top:14px;padding:16px;border:1px solid rgba(31,91,70,.25);border-radius:16px;background:rgba(255,255,255,.7)';

    var title = make('div', 'الدفع الدولي بالدولار');
    title.style.cssText = 'font-weight:800;margin-bottom:8px';
    box.appendChild(title);

    var p = make('div', 'السعر الأساسي: $' + usd + ' USD — سعر العرض لأول ' + limit + ' نسخة: $' + usdOffer + ' USD — خصم الدولار: $3 USD');
    p.style.marginBottom = '8px';
    box.appendChild(p);

    var bankLine = make('div', 'البنك: ' + bank);
    box.appendChild(bankLine);

    var ibanLine = make('div');
    ibanLine.innerHTML = '<b>IBAN:</b> ' + iban;
    ibanLine.style.cssText = 'direction:ltr;text-align:left;font-family:monospace;font-size:16px;margin-top:6px;word-break:break-all';
    box.appendChild(ibanLine);

    var actions = make('div');
    actions.style.cssText = 'display:flex;gap:8px;flex-wrap:wrap;margin-top:10px';

    var copy = make('button', 'نسخ IBAN');
    copy.type = 'button';
    copy.style.cssText = 'padding:9px 12px;border-radius:10px;border:1px solid currentColor;background:transparent;cursor:pointer';
    copy.addEventListener('click', function () {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(iban).then(function () { copy.textContent = 'تم نسخ IBAN'; });
      }
    });
    actions.appendChild(copy);

    var wa = make('a', 'إرسال الإيصال عبر واتساب');
    wa.href = whatsapp;
    wa.target = '_blank';
    wa.rel = 'noopener noreferrer';
    wa.style.cssText = 'padding:9px 12px;border-radius:10px;text-decoration:none;border:1px solid currentColor';
    actions.appendChild(wa);

    box.appendChild(actions);
    paymentBox.parentNode.insertBefore(box, paymentBox.nextSibling);
  }

  function addCheckoutButton() {
    var price = document.querySelector('.price');
    if (!price || document.getElementById('internationalCheckoutButton')) return;
    var a = make('a', 'الدفع الدولي بالدولار');
    a.id = 'internationalCheckoutButton';
    a.href = C.purchaseUrl || 'checkout.html';
    a.style.cssText = 'display:block;margin-top:9px;padding:10px 14px;border-radius:12px;text-align:center;text-decoration:none;border:1px solid currentColor';
    price.appendChild(a);
  }

  function repairPreviewCards() {
    var grid = document.getElementById('previewGrid');
    if (!grid || grid.dataset.qaPreviewFixed === '1') return;
    grid.dataset.qaPreviewFixed = '1';
    var imgs = grid.querySelectorAll('img');
    imgs.forEach(function (img) {
      var match = String(img.alt || '').match(/(\d+)/);
      var page = match ? match[1] : '';
      var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="600" height="800" viewBox="0 0 600 800"><rect width="600" height="800" rx="24" fill="#f5f5f5"/><rect x="35" y="35" width="530" height="730" rx="16" fill="white" stroke="#d5d5d5"/><text x="300" y="360" text-anchor="middle" font-family="Arial" font-size="42" fill="#222">صفحة المعاينة</text><text x="300" y="430" text-anchor="middle" font-family="Arial" font-size="82" font-weight="700" fill="#111">' + page + '</text><text x="300" y="500" text-anchor="middle" font-family="Arial" font-size="24" fill="#666">اضغط لفتح الصفحة من ملف PDF</text></svg>';
      img.src = 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(svg);
      img.alt = 'صفحة المعاينة ' + page;
    });
  }

  function run() {
    normalizeOfferText();
    addInternationalPayment();
    addCheckoutButton();
    repairPreviewCards();
  }

  run();
  window.setTimeout(run, 500);
  window.setTimeout(run, 1500);
});
