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

## Release gate status — 2026-10-16

The Windows project must be treated as a pre-commercial candidate until a real Windows build and install test passes. A successful CI compilation only proves that the executable can be produced; it does not certify accounting correctness, database encryption, update continuity, licensing, printer behavior, or backup/restore.

Before customer distribution, record test evidence for: fresh install and upgrade; Arabic/English layout; sales and purchase posting; returns and partial payments; inventory valuation and cost of goods sold; customer/supplier balances; payroll and expenses; journal/ledger/trial balance/P&L/balance sheet reconciliation; corrupted-backup rejection and restore rollback; and license expiry, renewal, revocation, and offline policy.

Do not put production customer data into this candidate, and do not publish a Windows download link until the release manifest is updated with the exact tested artifact, version, source commit, SHA-256, and publisher-signing status.


## New candidate 2.2.9 — reverse journal entries and capture mitigation

The 2.2.9 candidate adds a two-line manual journal form and an audited reverse-entry workflow for manual journals only. A reversal creates a new entry with debit and credit sides swapped, records the original/reversal relationship and a required reason, and leaves the original journal entry intact. It blocks duplicate reversals and does not allow reversing a reversal. Operational entries such as sales, purchases, expenses, payroll, receipts, and returns are intentionally excluded from this manual reversal screen; correct them through the relevant operational workflow so inventory and subledgers do not silently diverge. This feature must still be tested before financial use.

The app also attempts to apply Windows `WDA_EXCLUDEFROMCAPTURE` to its visible windows. This is a **best-effort privacy mitigation only**, not a guarantee against screenshots or recording. Results depend on Windows version and capture method; it cannot prevent external cameras, privileged or kernel-level tools, remote-control software, or all recording paths. Do not display passwords, private keys, API tokens, license signing secrets, or real customer data in the app.

**Security blocker:** the current app stores its local SQLite database under `%LOCALAPPDATA%\\SmartCompany\\smartcompany.db`; this implementation does not establish that the database is encrypted at rest. Do not treat the app as resistant to a local administrator or a determined reverse engineer. Before production use, implement and independently test database encryption, protected secret storage, signed updates, and a threat-model review. Never embed private signing keys, service API secrets, or production credentials in the Windows executable or repository.

The CI smoke test currently checks that the GUI process remains alive briefly. It is not an end-to-end GUI test and does not establish accounting correctness, successful installation on a customer device, screen-capture blocking, or production security.
