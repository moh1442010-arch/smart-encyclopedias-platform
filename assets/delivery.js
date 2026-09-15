(function(){
  'use strict';
  const API = 'https://smart-encyclopedias-delivery.moh1442010.workers.dev';
  const token = new URLSearchParams(location.search).get('token');
  const status = document.getElementById('status');
  const details = document.getElementById('details');
  const download = document.getElementById('download');
  if(!token){ status.className='status bad'; status.textContent='رابط التسليم غير مكتمل.'; return; }
  fetch(API+'/api/delivery/info?token='+encodeURIComponent(token), {cache:'no-store'})
    .then(r=>r.json().then(d=>({ok:r.ok,data:d})))
    .then(x=>{
      if(!x.ok || !x.data.ok || !x.data.active) throw new Error('inactive');
      const d=x.data;
      status.className='status ok';
      status.innerHTML='مرحباً <b>'+escapeHtml(d.buyer)+'</b>.<br>رقم الترخيص: <span class="license">'+escapeHtml(d.licenseId)+'</span><br>المتبقي من التنزيلات: <b>'+d.downloadsRemaining+'</b> — صالح حتى: <b>'+new Date(d.expiresAt).toLocaleString('ar-EG')+'</b>';
      download.href=API+'/api/delivery/download?token='+encodeURIComponent(token);
      details.hidden=false;
    })
    .catch(()=>{status.className='status bad';status.textContent='تعذر التحقق من رابط التسليم. قد يكون منتهياً أو تم استهلاك الحد المسموح.';});
  function escapeHtml(s){return String(s).replace(/[&<>'\"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','\"':'&quot;'}[c]));}
})();
