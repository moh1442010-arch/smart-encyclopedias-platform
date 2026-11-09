# ALMIZAN-90-REPAIR

أداة ضبط إغلاق العيوب التسعين لتطبيق الميزان للمخابز. تعمل محلياً في Termux أو Linux أو داخل GitHub Actions.

## ما تفعله
- تقرأ سجل D01–D90 وتقسمه إلى المراحل الثلاث المعتمدة.
- تعرض عدد العيوب المفتوحة وقيد التنفيذ والمغلقة بدليل.
- تمنع إعلان الإغلاق إذا غاب commit أو أمر اختبار قابل لإعادة الإنتاج أو نتيجة PASS أو مرجع دليل.
- لا تعدل كود التطبيق، ولا قواعد البيانات، ولا مفاتيح التوقيع، ولا بيانات العملاء.
- لا تغلق أي بند تلقائياً.

## التشغيل في Termux
من جذر المستودع:

```sh
python tools/almizan-90-repair/almizan90.py report
python tools/almizan-90-repair/almizan90.py gate
```

سيُظهر الأمر gate حالة BLOCKED ما دامت هناك عيوب مفتوحة. هذا متوقع في البداية، وليس فشل بناء APK.

## شروط تسجيل أي إغلاق
داخل `bakery-app/docs/ALMIZAN_90_REPAIR_REGISTER.json`، لا تغيّر status إلى CLOSED إلا بعد نجاح اختبار حقيقي، ثم أضف:
- commit: بصمة commit التي تحتوي الإصلاح.
- test_command: أمر الاختبار القابل لإعادة التنفيذ.
- result: يجب أن تكون PASS.
- evidence_ref: رابط تشغيل CI أو تقرير اختبار موثق.

مثال سجل إغلاق:
```json
{
  "id": "D11",
  "phase": "P1",
  "status": "CLOSED",
  "evidence": {
    "commit": "0123456789abcdef0123456789abcdef01234567",
    "test_command": "./gradlew connectedDebugAndroidTest",
    "result": "PASS",
    "evidence_ref": "https://github.com/OWNER/REPO/actions/runs/123456"
  }
}
```

لا تعتبر اختبارات المصدر أو بناء APK بديلاً عن اختبارات Android instrumentation أو اختبار الترقية على جهاز فعلي. نجاح هذا السجل وحده لا يمنح اعتماداً تجارياً؛ يلزم اجتياز بوابات الأجهزة والتوقيع والنسخ والاستعادة وموافقة مالك المنتج.

## أمر الإغلاق النهائي
`python tools/almizan-90-repair/almizan90.py gate`

لا ينجح إلا عندما تكون D01–D90 مغلقة بأدلة مكتملة.
