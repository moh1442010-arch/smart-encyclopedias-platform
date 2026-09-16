(function(){
  'use strict';
  const API = 'https://smart-encyclopedias-delivery.moh1442010.workers.dev';
  const token = new URLSearchParams(location.search).get('token');
  const status = document.getElementById('status');
  const details = document.getElementById('details');
  const download = document.getElementById('download');
  const DEVICE_KEY = 'smart_encyclopedia_device_id_v1';

  function getDeviceId(){
    try {
      let id = localStorage.getItem(DEVICE_KEY);
      if(!id){ id = crypto.randomUUID() + '-' + crypto.randomUUID(); localStorage.setItem(DEVICE_KEY, id); }
      return id;
    } catch(e) { return ''; }
  }
  const deviceId = getDeviceId();
  if(!token){ status.className='status bad'; status.textContent='رابط التسليم غير مكتمل.'; return; }
  if(!deviceId){ status.className='status bad'; status.textContent='تعذر تفعيل هذا الجهاز. فعّل التخزين المحلي للمتصفح ثم أعد فتح الرابط.'; return; }

  const deviceHeaders = { 'x-device-id': deviceId };
  fetch(API+'/api/delivery/info?token='+encodeURIComponent(token), {cache:'no-store', headers: deviceHeaders})
    .then(r=>r.json().then(d=>({ok:r.ok,data:d})))
    .then(x=>{
      if(!x.ok || !x.data.ok || !x.data.active) {
        if(x.data && x.data.error === 'license_bound_to_another_device') throw new Error('bound');
        if(x.data && x.data.error === 'payment_not_approved_or_link_expired') throw new Error('approval');
        throw new Error('inactive');
      }
      const d=x.data;
      status.className='status ok';
      status.innerHTML='مرحباً <b>'+escapeHtml(d.buyer)+'</b>.<br>رقم الترخيص: <span class="license">'+escapeHtml(d.licenseId)+'</span><br>هذا الترخيص مرتبط بهذا الجهاز.<br>المتبقي من التنزيلات: <b>'+d.downloadsRemaining+'</b> — صالح حتى: <b>'+new Date(d.expiresAt).toLocaleString('ar-EG')+'</b>';
      download.href=API+'/api/delivery/download?token='+encodeURIComponent(token);
      download.setAttribute('data-device-bound','true');
      details.hidden=false;
    })
    .catch((e)=>{
      status.className='status bad';
      if(e.message==='bound') status.textContent='هذا الترخيص مرتبط بجهاز آخر. لا يمكن استخدامه على جهاز ثانٍ. إذا كان الجهاز قد تغيّر، تواصل مع صاحب الموسوعة لإعادة التفعيل.';
      else if(e.message==='approval') status.textContent='لم يتم اعتماد الدفع بعد. لن يتم تسليم النسخة قبل مراجعة صاحب الموسوعة واعتماد العملية.';
      else status.textContent='تعذر التحقق من رابط التسليم. قد يكون منتهياً أو تم استهلاك الحد المسموح.';
    });

  // The actual download request must carry the same device binding header.
  if(download){
    download.addEventListener('click', function(ev){
      ev.preventDefault();
      fetch(download.href, {headers: deviceHeaders, cache:'no-store'})
        .then(async r=>{
          if(!r.ok){ const d=await r.json().catch(()=>({})); throw new Error(d.error || 'download_failed'); }
          const blob=await r.blob();
          const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download='encyclopedia-'+Date.now()+'.pdf'; document.body.appendChild(a); a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(a.href), 1000);
        })
        .catch(()=>{ status.className='status bad'; status.textContent='تعذر تنزيل النسخة من هذا الجهاز. إذا كان الترخيص مربوطاً بجهاز آخر فلن يعمل هنا.'; });
    });
  }
  function escapeHtml(s){return String(s).replace(/[&<>'\"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','\"':'&quot;'}[c]));}
})();
