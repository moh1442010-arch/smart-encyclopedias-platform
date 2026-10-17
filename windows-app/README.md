# شركة محمد مصطفى الذكية — Windows x64

تطبيق مكتبي Windows مبني باستخدام .NET 8 Windows Forms وSQLite. الواجهة عربية مع خيار اللغة الإنجليزية، والتطبيق يُنشر ذاتيًا مع مكونات التشغيل اللازمة.

## الوحدات الموجودة حاليًا
- الأصناف والمخزون مع إدخال الباركود يدويًا أو عبر ماسح USB/Bluetooth الذي يعمل كلوحة مفاتيح.
- تسجيل المبيعات والمشتريات وتحديث الكمية داخل معاملة قاعدة بيانات.
- العملاء والموردون.
- الموظفون والرواتب الأساسية والسلف والخصومات.
- المصروفات.
- تصدير واستعادة نسخة SQLite مع فحص سلامة الملف قبل الاستعادة.
- ملخص تشغيلي وطباعة التقرير وإرساله بالبريد الإلكتروني.
- دعم العربية والإنجليزية.

## البناء والتنزيل
يُبنى إصدار Windows x64 بواسطة GitHub Actions في الملف `.github/workflows/build-windows-app.yml`. مخرجات البناء تشمل EXE مستقلًا، وZIP، وبصمة SHA-256. افتح تبويب Actions في المستودع، واختر أحدث تشغيل ناجح بعنوان Build Smart Company Windows App، ثم نزّل artifact الذي يبدأ اسمه بـ `smart-company-windows`.

## مكان قاعدة البيانات
`%LOCALAPPDATA%\SmartCompany\smartcompany.db`

## دفتر الأستاذ — مرحلة تشخيصية أولى
أضيفت شاشة «دفتر الأستاذ وميزان المراجعة» لعرض أرصدة الحسابات المسجلة ومقارنة إجمالي المدين والدائن، مع عدّاد للعمليات القديمة التي لا يوجد لها قيد مرتبط في دفتر الأستاذ. أُضيفت كذلك حسابات أساسية للذمم المدينة والدائنة والرواتب ضمن دليل الحسابات.

**هذا ليس اكتمالًا للدورة المحاسبية بعد.** الشاشة تشخيصية؛ لم تُستكمل بعد فواتير الأجل والتحصيل والسداد والمرتجعات والرواتب الفعلية والتسويات والإقفال، ولا يجوز إنشاء قيود تاريخية تلقائيًا قبل تحديد طبيعة كل عملية قديمة لأن السجلات الحالية لا تحفظ دائمًا طريقة السداد أو الطرف المرتبط بها. التغييرات في المخطط إضافية ولا تحذف جداول المستخدمين أو معاملاتهم.

## حدود الإصدار — مهمة
هذه نسخة **ما قبل الإصدار التجاري**. المبيعات والمشتريات والمصروفات والرواتب لا تُرحّل بعد إلى دفتر أستاذ عام متكامل؛ لذلك فإن الفرق الحسابي الظاهر في الملخص ليس صافي ربح محاسبيًا ولا بديلًا عن قائمة دخل أو ميزان مراجعة. كما لم يثبت بعد اختبار جميع السيناريوهات على أجهزة Windows فعلية. لا تستخدمها لإقفال حسابات أو اتخاذ قرارات مالية قبل استكمال دفتر الأستاذ، وترحيل جميع الوحدات، واختبارات محاسبية مستقلة، واختبار النسخ الاحتياطي والاستعادة عمليًا.

## توصيات الاختبار
1. اختبر على قاعدة بيانات تجريبية فقط.
2. اختبر بيع كمية أكبر من المخزون؛ يجب رفضه.
3. اختبر شراءً ثم بيعًا وتحقق من رصيد المخزون.
4. صدّر نسخة احتياطية، ثم اختبر استعادتها على قاعدة تجريبية.
5. طابق إجماليات العمليات مع قيود دفتر الأستاذ بعد إكمال الترحيل المحاسبي.

## الإصلاحات الجارية — فرع تجريبي فقط

في فرع `fix/windows-release-blockers-2026-10` بدأنا معالجة عيوب المراجعة:
- أزيلت أزرار «الباقات والأسعار» و«الدفع والتحويل البنكي» من القائمة الرئيسية حتى لا تعرض هذه النسخة كمنتج تجاري.
- كل اتصال SQLite يفعّل `PRAGMA foreign_keys = ON` ويتحقق من تفعيله.
- استُبدلت ترقيات `ALTER TABLE` التي كانت تبتلع أخطاء SQLite بفحص صريح لوجود العمود؛ الأخطاء الأخرى تتوقف وتظهر بدلًا من المرور بصمت.
- لم تُحوّل مبالغ `REAL/double` إلى وحدات نقدية صحيحة بعد، ولم يُنفّذ بعد نظام الدخول/PIN أو الترخيص الفعلي أو توقيع EXE. لا يُسمح باعتبار هذه البنود مغلقة.

هذه تعديلات أولية لم تُعتمد بعد. يلزم نجاح CI ومراجعة التغييرات واختبارات قاعدة بيانات فعلية قبل الدمج. لا تستخدم قاعدة بيانات إنتاج أو بيانات مالية حقيقية مع هذا الفرع.

## Release gate status — 2026-10-17

The Windows project must be treated as a pre-commercial candidate until a real Windows build and install test passes. A successful CI compilation only proves that the executable can be produced; it does not certify accounting correctness, database encryption, update continuity, licensing, printer behavior, or backup/restore.

Before customer distribution, record test evidence for: fresh install and upgrade; Arabic/English layout; sales and purchase posting; returns and partial payments; inventory valuation and cost of goods sold; customer/supplier balances; payroll and expenses; journal/ledger/trial balance/P&L/balance sheet reconciliation; corrupted-backup rejection and restore rollback; and license expiry, renewal, revocation, and offline policy.

Do not put production customer data into this candidate, and do not publish a Windows download link until the release manifest is updated with the exact tested artifact, version, source commit, SHA-256, and publisher-signing status.
