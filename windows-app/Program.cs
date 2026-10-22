using Microsoft.Data.Sqlite;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Printing;
using System.Runtime.InteropServices;

namespace SmartCompany;

// Expert-review candidate refresh 2026-10-22: subscription pricing aligned with Android and public store.
// Paired final review candidate 2.4.4 / 2.3.0: includes review modules, guide/FAQ and control-room diagnostics.
// Rebuild pair after Android compile correction; both artifacts must share the same commit.
// Android 2.4.5 adds an audited one-time reversal screen for eligible manual journals; rebuild both review packages together.

static class UiLanguage
{
    static readonly string path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "SmartCompany", "language.txt");
    public static bool English
    {
        get => File.Exists(path) && File.ReadAllText(path).Trim().Equals("en", StringComparison.OrdinalIgnoreCase);
        set { Directory.CreateDirectory(Path.GetDirectoryName(path)!); File.WriteAllText(path, value ? "en" : "ar"); }
    }
    public static string T(string value)
    {
        if (!English || string.IsNullOrEmpty(value)) return value;
        string[][] pairs = {
          new[]{"المحاسب الميزان","Al-Mizan Accountant"},new[]{"شركة محمد مصطفى الذكية","Mohammed Mustafa Smart Company"},new[]{"ملخص تشغيلي أولي — ليس قائمة دخل معتمدة","Preliminary operational summary — not an approved income statement"},new[]{"تنبيه: الفرق الحسابي ليس صافي الربح. دفتر الأستاذ يرحّل العمليات الجديدة المدعومة، لكن السجلات التاريخية والخصومات العامة وتسوية الرواتب والتسويات والإقفال تحتاج إلى مراجعة قبل اعتماد القوائم.","Notice: arithmetic movement is not net profit. The ledger posts supported new transactions, but legacy history, generic deductions, payroll settlement, adjustments, and closing entries require review before financial statements can be relied on."},
          new[]{"قيد يومية يدوي (سطران)","Manual journal (two lines)"},new[]{"قيد يومية يدوي من سطرين","Manual journal entry with two lines"},new[]{"بيان القيد — مطلوب","Journal description — required"},new[]{"المبلغ — قيمة موجبة","Amount — positive value"},new[]{"حفظ القيد المرحّل","Save posted entry"},new[]{"هذا النموذج ينشئ قيدًا متوازنًا من سطرين. استخدم وحدات المبيعات والمشتريات والمرتجعات لتصحيح العمليات المرتبطة بالمخزون والذمم.","This form creates a balanced two-line journal. Use sales, purchases, and returns modules to correct transactions linked to inventory and receivables/payables."},new[]{"أدخل البيان والمبلغ الموجب واختر حسابين مختلفين.","Enter a description, a positive amount, and two different accounts."},new[]{"تم حفظ القيد اليدوي وترحيله. يمكنك عكسه لاحقًا من شاشة القيد العكسي.","Manual journal posted. You can reverse it later from Reverse Journal Entry."},new[]{"تعذر حفظ القيد؛ لم تُعتمد التغييرات. ","Could not save journal; no changes were committed. "},new[]{"القيد العكسي","Reverse Journal Entry"},new[]{"القيد العكسي — عكس قيد مرحّل","Reverse journal entry — reverse a posted entry"},new[]{"سبب العكس — مطلوب للتدقيق","Reversal reason — required for audit"},new[]{"تحديث القيود المتاحة","Refresh eligible entries"},new[]{"إنشاء القيد العكسي","Create reversing entry"},new[]{"اختر قيدًا واكتب سبب العكس أولًا.","Select an entry and enter a reason first."},new[]{"سيُنشأ قيد عكسي مستقل للقيد رقم ","A separate reversing entry will be created for entry "},new[]{"تأكيد القيد العكسي","Confirm reversing entry"},new[]{". لن يُحذف القيد الأصلي. هل تريد المتابعة؟",". The original entry will not be deleted. Continue?"},new[]{"تم إنشاء قيد عكسي مستقل. بقي القيد الأصلي محفوظًا لأغراض التدقيق.","A separate reversing entry was created. The original entry remains unchanged for audit."},new[]{"تعذر إنشاء القيد العكسي؛ لم تُعتمد التغييرات. ","Could not create reversing entry; no changes were committed. "},new[]{"لا يمكن عكس قيد عكسي.","A reversing entry cannot itself be reversed."},new[]{"تم إنشاء قيد عكسي لهذا القيد مسبقًا.","This entry has already been reversed."},new[]{"القيد الأصلي غير موجود.","Original entry not found."},new[]{"القيد الأصلي غير متوازن أو لا يحتوي أسطرًا كافية؛ أوقف العكس وراجع البيانات.","Original entry is unbalanced or has too few lines; stop and review the data."},new[]{"الموظفون والرواتب","Employees & Payroll"},new[]{"المصروفات","Expenses"},new[]{"النسخ الاحتياطي والاستعادة","Backup & Restore"},new[]{"تسجيل مصروف","Record expense"},new[]{"تصدير نسخة احتياطية","Export backup"},new[]{"استعادة نسخة احتياطية","Restore backup"},new[]{"تم تصدير النسخة الاحتياطية.","Backup exported."},new[]{"تعذر تصدير النسخة الاحتياطية.","Could not export backup."},new[]{"ستستبدل الاستعادة بيانات الشركة الحالية. أنشئ نسخة احتياطية أولًا. هل تريد المتابعة؟","Restore will replace current company data. Back up first. Continue?"},new[]{"تأكيد الاستعادة","Confirm restore"},new[]{"تمت الاستعادة. راجع الأرصدة والتقارير.","Restore complete. Review balances and reports."},new[]{"فشلت الاستعادة: الملف غير صالح أو غير متوافق.","Restore failed: file is invalid or incompatible."},new[]{"أدخل بيانًا ومبلغًا موجبًا صحيحًا.","Enter a description and a positive amount."},new[]{"إجمالي المصروفات:","Total expenses:"},new[]{"صافي الحركة بعد المصروفات:","Net movement after expenses:"},new[]{"اسم الموظف","Employee name"},new[]{"المسمى الوظيفي","Job title"},new[]{"الراتب الشهري","Monthly salary"},new[]{"إضافة موظف","Add employee"},new[]{"حفظ حركة الراتب","Save payroll event"},new[]{"سلفة","Advance"},new[]{"خصم","Deduction"},new[]{"المبلغ","Amount"},new[]{"البيان","Description"},new[]{"السلف","Advances"},new[]{"الخصومات","Deductions"},new[]{"صافي المستحق","Net payable"},new[]{"أدخل اسمًا وراتبًا صحيحًا.","Enter a valid name and salary."},new[]{"اختر موظفًا وأدخل مبلغًا صحيحًا.","Choose an employee and enter a valid amount."},new[]{"الأصناف والمخزون والباركود","Products, Inventory & Barcode"},new[]{"الأصناف والمخزون","Products & Inventory"},
          new[]{"المبيعات","Sales"},new[]{"المشتريات","Purchases"},new[]{"العملاء","Customers"},new[]{"الموردون","Suppliers"},new[]{"الموظفون","Employees"},new[]{"التقارير المالية","Financial Reports"},
          new[]{"الدعم عبر واتساب","WhatsApp Support"},new[]{"الدعم عبر البريد الإلكتروني","Email Support"},new[]{"اختيار اللغة / Language","Language: العربية / English"},new[]{"اسم الصنف أو الباركود","Product name or barcode"},new[]{"طباعة التقرير","Print report"},new[]{"إرسال بالبريد الإلكتروني","Send by email"},new[]{"التقرير المالي","Financial Report"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"اسم الصنف","Product name"},
          new[]{"الباركود (ماسح USB/Bluetooth أو إدخال يدوي)","Barcode (USB/Bluetooth scanner or manual entry)"},new[]{"الكمية","Quantity"},new[]{"السعر","Price"},new[]{"إضافة الصنف","Add product"},
          new[]{"أدخل اسمًا وكمية وسعرًا صحيحًا.","Enter a valid name, quantity and price."},new[]{"تعذر الحفظ: الاسم أو الباركود مستخدم مسبقًا.","Could not save: name or barcode already exists."},
          new[]{"باركود","Barcode"},new[]{"المخزون","Stock"},new[]{"فاتورة بيع","Sales Invoice"},new[]{"حفظ","Save"},new[]{"الصنف غير موجود.","Product not found."},
          new[]{"الرصيد غير كافٍ.","Insufficient stock."},new[]{"تم الحفظ وتحديث المخزون.","Saved and inventory updated."},new[]{"الاسم","Name"},new[]{"إضافة","Add"},
          new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"الصافي:","Net:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},
          new[]{"انتهت التجربة المجانية لمدة 7 أيام","The 7-day free trial has ended"},new[]{"رمز التفعيل","Activation code"},new[]{"تفعيل الاشتراك","Activate subscription"},new[]{"التواصل عبر واتساب","Contact via WhatsApp"},
          new[]{"تم تفعيل الاشتراك.","Subscription activated."},new[]{"رمز التفعيل غير صحيح.","Invalid activation code."},new[]{"التقرير المالي","Financial Report"},new[]{"شركة محمد مصطفى الذكية - الاشتراك","Mohammed Mustafa Smart Company - Subscription"},new[]{"تم تفعيل الاشتراك.","Subscription activated."},new[]{"رمز التفعيل غير صحيح.","Invalid activation code."},new[]{"باركود","Barcode"},new[]{"المخزون","Stock"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"صافي الحركة:","Net movement:"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"الصافي:","Net:"},new[]{"اختيار اللغة / Language","Language: Arabic / English"},new[]{"📘 الدليل الإرشادي والأسئلة الشائعة","📘 User Guide & FAQ"},new[]{"المصروفات الدورية والإيجار والكهرباء","Recurring expenses, rent & electricity"},new[]{"الأصول الثابتة والإهلاك","Fixed assets & depreciation"},new[]{"حساب التشغيل والتصنيع المرحلي","Work-in-process & staged manufacturing"},new[]{"غرفة التحكم والرقابة — Enterprise","Enterprise control room"},new[]{"حفظ تعريف المصروف الدوري","Save recurring expense definition"},new[]{"إنشاء استحقاقات الفترة الحالية مرة واحدة","Generate current-period obligations once"},new[]{"تسجيل الأصل وترحيل الاقتناء","Register asset and post acquisition"},new[]{"احتساب وترحيل إهلاك الشهر الحالي","Calculate and post current-month depreciation"},new[]{"حفظ مرحلة التشغيل","Save manufacturing stage"},new[]{"تحديث مؤشرات الرقابة","Refresh control indicators"}
        };
        string result=value;
        foreach(var pair in pairs) result=result.Replace(pair[0],pair[1]);
        return result;
    }
}

internal static class Program
{
    [STAThread]
    static void Main()
    {
        ApplicationConfiguration.Initialize();
        Application.Run(new MainForm());
    }
}

static class FormCaptureProtection
{
    // Best-effort Windows capture exclusion. This is not a security boundary against
    // external cameras, privileged capture tools, remote-control software, or OS/kernel access.
    const uint WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    [DllImport("user32.dll", SetLastError = true)]
    static extern bool SetWindowDisplayAffinity(IntPtr hWnd, uint dwAffinity);

    public static void Apply(Form form)
    {
        try
        {
            if (form.IsHandleCreated && !form.IsDisposed && form.Visible)
                _ = SetWindowDisplayAffinity(form.Handle, WDA_EXCLUDEFROMCAPTURE);
        }
        catch (Exception)
        {
            // Unsupported Windows versions or API failures must not crash the accounting app.
        }
    }
}

sealed class MainForm : Form
{
    readonly string dbPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "SmartCompany", "smartcompany.db");
    readonly FlowLayoutPanel menu = new() { Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false, AutoScroll = true, Padding = new Padding(30) };
    readonly Label summary = new() { Dock = DockStyle.Top, Height = 55, TextAlign = ContentAlignment.MiddleCenter, Font = new Font("Segoe UI", 13) };

    public MainForm()
    {
        Directory.CreateDirectory(Path.GetDirectoryName(dbPath)!); InitDb();
        Text = UiLanguage.T("المحاسب الميزان 2.3.0"); Width = 1050; Height = 720; StartPosition = FormStartPosition.CenterScreen;
        RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes; RightToLeftLayout = !UiLanguage.English;
        var head = new Label { Text = UiLanguage.T("المحاسب الميزان 2.3.0 — نسخة تجريبية"), Dock = DockStyle.Top, Height = 75, BackColor = Color.FromArgb(11,31,58), ForeColor = Color.FromArgb(212,175,55), Font = new Font("Segoe UI", 22, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        Controls.Add(menu); Controls.Add(summary); Controls.Add(head);
        Add("الأصناف والمخزون والباركود", Products); Add("المبيعات", () => Transaction(false)); Add("المشتريات", () => Transaction(true));
        Add("العملاء", () => Simple("customers","العملاء")); Add("الموردون", () => Simple("suppliers","الموردون")); Add("تحصيلات العملاء", () => ReceiptPayment(false)); Add("سداد الموردين", () => ReceiptPayment(true)); Add("الموظفون والرواتب", Employees); Add("المصروفات", CashExpense); Add("المرتجعات", Returns); Add("النسخ الاحتياطي والاستعادة", BackupRestore);
        Add("دفتر الأستاذ وميزان المراجعة", LedgerReports); Add("قيد يومية يدوي (سطران)", ManualJournalEntry); Add("القيد العكسي", ReverseJournalEntry); Add("التقارير المالية", Reports); Add("الباقات والأسعار", SubscriptionPlans); Add("الدفع والتحويل البنكي", PaymentDetails); Add("الدعم عبر واتساب", () => Process.Start(new ProcessStartInfo("https://wa.me/249121851285") { UseShellExecute = true })); Add("الدعم عبر البريد الإلكتروني", () => Process.Start(new ProcessStartInfo("mailto:moh1442010@gmail.com?subject=دعم%20تطبيق%20شركة%20محمد%20مصطفى%20الذكية") { UseShellExecute = true }));
        Add("المصروفات الدورية والإيجار والكهرباء", RecurringExpenses); Add("الأصول الثابتة والإهلاك", FixedAssets); Add("حساب التشغيل والتصنيع المرحلي", FactoryOperations); Add("غرفة التحكم والرقابة — Enterprise", EnterpriseControlRoom); Add("📘 الدليل الإرشادي والأسئلة الشائعة", UserGuide);
        Add("اختيار اللغة / Language", ToggleLanguage);
        RefreshSummary();
        var captureGuard = new System.Windows.Forms.Timer { Interval = 500 };
        captureGuard.Tick += (_, _) =>
        {
            foreach (Form openForm in Application.OpenForms)
                FormCaptureProtection.Apply(openForm);
        };
        Shown += (_, _) => FormCaptureProtection.Apply(this);
        FormClosed += (_, _) => captureGuard.Stop();
        captureGuard.Start();
    }

    void EnterpriseControlRoom()
    {
        using var c=C();
        long Scalar(string sql){using var q=c.CreateCommand();q.CommandText=sql;return Convert.ToInt64(q.ExecuteScalar());}
        string Value(string sql){using var q=c.CreateCommand();q.CommandText=sql;return Convert.ToString(q.ExecuteScalar())??"0";}
        var invalid=Scalar("SELECT COUNT(*) FROM (SELECT e.id FROM gl_entries e LEFT JOIN gl_lines l ON l.entry_id=e.id GROUP BY e.id HAVING COUNT(l.id)<2 OR ABS(COALESCE(SUM(l.debit),0)-COALESCE(SUM(l.credit),0))>0.005)");
        var orphan=Scalar("SELECT COUNT(*) FROM gl_lines l LEFT JOIN gl_entries e ON e.id=l.entry_id LEFT JOIN gl_accounts a ON a.id=l.account_id WHERE e.id IS NULL OR a.id IS NULL");
        var wipDebit=Value("SELECT printf('%.2f',COALESCE(SUM(l.debit-l.credit),0)) FROM gl_lines l JOIN gl_accounts a ON a.id=l.account_id WHERE a.code='1400'");
        var unpostedStages=Scalar("SELECT COUNT(*) FROM factory_stage_events WHERE journal_entry_id IS NULL AND (material_cost+labor_cost+overhead_cost)>0");
        var assets=Scalar("SELECT COUNT(*) FROM fixed_assets");
        var recurringDue=Scalar("SELECT COUNT(*) FROM recurring_expenses WHERE active=1 AND next_due<=date('now')");
        var unpostedExpenses=Scalar("SELECT COUNT(*) FROM expenses x WHERE NOT EXISTS(SELECT 1 FROM gl_entries e WHERE e.source_type='EXPENSE' AND e.source_id=x.id)");
        var report="غرفة التحكم والرقابة — Enterprise (قراءة فقط)"+Environment.NewLine+
          "القيود غير المتوازنة أو الناقصة: "+invalid+Environment.NewLine+
          "أسطر دفتر أستاذ يتيمة: "+orphan+Environment.NewLine+
          "صافي حركة حساب مخزون تحت التشغيل (1400): "+wipDebit+Environment.NewLine+
          "مراحل تشغيل ذات تكلفة بلا قيد أستاذ: "+unpostedStages+Environment.NewLine+
          "عدد الأصول المسجلة: "+assets+Environment.NewLine+
          "تعريفات مصروف دوري مستحقة حسب التاريخ: "+recurringDue+Environment.NewLine+
          "مصروفات بلا قيد أستاذ مطابق: "+unpostedExpenses+Environment.NewLine+Environment.NewLine+
          "تنبيه: المؤشرات تشخيصية فقط. توازن القيود لا يثبت اكتمال العمليات، وحساب تحت التشغيل لا يثبت انتقال الناتج النهائي للمخزون. راجع دفاتر العملاء والموردين والمخزون والبنوك والرواتب والمرتجعات يدويًا. لا تستخدم هذه الشاشة لإقرار ضريبي أو إقفال نهائي.";
        using var f=new Form{Text=UiLanguage.T("غرفة التحكم والرقابة — Enterprise"),Width=850,Height=600,StartPosition=FormStartPosition.CenterParent,RightToLeft=UiLanguage.English?RightToLeft.No:RightToLeft.Yes,RightToLeftLayout=!UiLanguage.English};
        var view=new TextBox{Multiline=true,ReadOnly=true,ScrollBars=ScrollBars.Vertical,Dock=DockStyle.Fill,Font=new Font("Segoe UI",13),Text=UiLanguage.T(report)};
        var refresh=new Button{Dock=DockStyle.Bottom,Height=44,Text=UiLanguage.T("تحديث مؤشرات الرقابة")};refresh.Click+=(_,_)=>{f.Close();EnterpriseControlRoom();};
        f.Controls.Add(view);f.Controls.Add(refresh);f.ShowDialog(this);
    }

    void UserGuide()
    {
        using var f = new Form { Text = UiLanguage.T("الدليل الإرشادي والأسئلة الشائعة"), Width = 900, Height = 700, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var helpText = """
المحاسب الميزان — الدليل الإرشادي المحلي

ابدأ بأمان
1. استخدم قاعدة تجريبية قبل تسجيل بيانات حقيقية.
2. اضبط اسم المنشأة والعملة والسنة المالية.
3. طابق الأرصدة الافتتاحية مع المستندات وميزان المصدر.
4. اختبر شراءً وبيعًا وتحصيلًا وسدادًا ومرتجعًا.
5. أنشئ نسخة احتياطية واختبر استعادتها على قاعدة تجريبية.

المخزون والمبيعات
• تحقق من الصنف والكمية والسعر والمخزن قبل الحفظ.
• لا تبيع كمية تتجاوز المتاح.
• افحص أثر البيع والشراء والمرتجع على الكمية والتكلفة والذمم.

دفتر الأستاذ والقيد العكسي
• القيد اليدوي يجب أن يتوازن مدينًا ودائنًا.
• القيد العكسي ينشئ قيدًا جديدًا ولا يحذف القيد الأصلي؛ اكتب سببًا واضحًا.
• لا تستخدم عكس القيد اليدوي لتصحيح بيع أو شراء مرتبط بالمخزون؛ صحح العملية من وحدتها الأصلية.
• توازن المدين والدائن وحده لا يثبت اكتمال الترحيل أو صحة القوائم.

النسخ الاحتياطي والاستعادة
• صدّر نسخة قبل التحديث أو الاستعادة.
• جرّب الاستعادة على قاعدة منفصلة أولًا؛ قد تستبدل الاستعادة البيانات الحالية.
• احتفظ بالنسخة في مكان آمن ولا تشاركها مع غير المخولين.

الأصول الثابتة والمصروفات الدورية والمصانع والعقارات
• شاشة المصروفات الدورية تحفظ تعريفًا بمبلغ وتكرار شهري/سنوي وتاريخ استحقاق، وتولد استحقاق الفترة الحالية مرة واحدة وتُرحّله للمصروفات والذمم الدائنة. لا تستورد فواتير مزود الخدمة تلقائيًا ولا تدير عقود العقارات.
• شاشة الأصول تسجل تكلفة الاقتناء والعمر بالأشهر وتاريخ الجاهزية، وترحّل اقتناء الأصل من الصندوق وتولد إهلاكًا خطيًا شهريًا إلى مصروف الإهلاك ومجمعه، مع منع التكرار وعدم تجاوز التكلفة. لا تدعم القيمة المتبقية أو طرق إهلاك متعددة أو الاستبعاد الكامل بعد.
• لا تعتبر المصروف الثابت أصلًا: الإيجار والكهرباء غالبًا مصروفان دوريّان، أما الآلات والمباني فقد تستوفي شروط الرسملة حسب السياسة المحاسبية. يجب أن يراجع المحاسب التصنيف.
• شاشة حساب التشغيل تسجل أوامر التشغيل والمراحل وتكاليف المواد والعمالة والمصاريف الصناعية وكميات المنتج الجيد والتالف، وترحّل تكلفة المرحلة إلى مخزون تحت التشغيل. لا تخصم كمية المواد الخام تلقائيًا من المخزون ولا تنقل المنتج النهائي تلقائيًا إلى مخزون المنتجات الجاهزة.
• إدارة العقار والعقود والوحدات والتحصيلات الجزئية والتأمينات والصيانة ليست وحدة مكتملة في هذا المرشح.
• غرفة التحكم Enterprise تعرض مؤشرات للقيود غير المتوازنة، والأسطر اليتيمة، ومخزون تحت التشغيل، ومراحل التشغيل غير المرحلة، والأصول، والمصروفات الدورية. هي شاشة تشخيصية للقراءة فقط وليست قفل فترة أو اعتماد قوائم مالية.
• اعتبر الأصول والتصنيع والمصروفات الدورية وحدات مراجعة أولية، واختبر قيودها ومطابقتها على بيانات تجريبية قبل الاعتماد.

الأسئلة الشائعة
س: هل توازن المدين والدائن يعني أن الحسابات صحيحة؟
ج: لا. قد تتوازن القيود مع غياب عمليات أو استخدام حساب خاطئ. طابق كل دفتر فرعي مع الأستاذ وسجّل الحركات التي لم تُرحّل.
س: هل يمكن اعتبار الملخص التشغيلي صافي ربح؟
ج: لا. الملخص الحالي ليس قائمة دخل معتمدة؛ لا تستخدمه للإقرار الضريبي أو الإقفال.
س: كيف أختبر الإيجار والكهرباء؟
ج: أنشئ تعريف المصروف، وراجع الاستحقاق المرحلي في الفترة الحالية، ثم طابق حساب المصروف والذمم الدائنة. لا تفترض قراءة فاتورة مزود الخدمة تلقائيًا.
س: هل الإهلاك والتصنيع مكتملان؟
ج: المرشح يسجل إهلاكًا خطيًا ومراحل تكاليف تشغيل، لكنه لا يدعم بعد الاستبعاد الكامل للأصول أو خصم الخام ونقل المنتج النهائي تلقائيًا. سجّل هذه الحدود في تقرير المراجعة.
س: هل غرفة التحكم اعتماد نهائي؟
ج: لا. هي مراجعة تشخيصية للمخاطر. لا تغلق الفترة حتى تُحل الفروقات وتُراجع العمليات غير المرحلة وتنجح مطابقة الأستاذ والدفاتر الفرعية والنسخة الاحتياطية.

العمل دون إنترنت
• الإدخال المحلي قد يعمل دون اتصال، لكن التحقق من الترخيص والتحديثات وبعض الخدمات قد يتطلب الإنترنت. اختبر كل مسار قبل الاعتماد.
• هذه المساعدة محلية ولا ترسل بياناتك تلقائيًا.

حدود الإصدار
• الملخص التشغيلي الحالي ليس قائمة دخل معتمدة، ولا يُستخدم لإقفال حسابات فعلية حتى اكتمال الترحيل المحاسبي واختبارات المصالحة المستقلة.
""";
        var box = new TextBox { Multiline = true, ReadOnly = true, WordWrap = true, ScrollBars = ScrollBars.Vertical, Dock = DockStyle.Fill, Font = new Font("Segoe UI", 12), Text = UiLanguage.T(helpText) };
        f.Controls.Add(box);
        f.ShowDialog(this);
    }

    void SubscriptionPlans()
    {
        using var f = new Form { Text = UiLanguage.T("باقات وأسعار المحاسب الذكي"), Width = 820, Height = 760, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var panel = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false, AutoScroll = true, Padding = new Padding(18) };
        f.Controls.Add(panel);
        void LabelText(string text, int height = 32, int size = 12) => panel.Controls.Add(new Label { Text = UiLanguage.T(text), Width = 740, Height = height, Font = new Font("Segoe UI", size, FontStyle.Regular), TextAlign = ContentAlignment.MiddleCenter });
        LabelText("باقات المحاسب الميزان — الأسعار الموحدة للمراجعة الفنية", 44, 17);
        LabelText("تجربة مجانية 7 أيام. الأسعار أدناه للتجربة والمراجعة؛ لا تحوّل مبلغًا قبل إعلان فتح البيع رسميًا.", 48, 11);
        LabelText("الترخيص الدائم لا يشمل التحديثات المستقبلية.", 32, 11);
        LabelText("السودان — الجنيه السوداني (ج.س) | الدفع عبر بنكك", 34, 14);
        LabelText("Basic: دائم 150,000 | شهري 25,000 | سنوي 250,000 ج.س");
        LabelText("Pro: دائم 300,000 | شهري 45,000 | سنوي 450,000 ج.س");
        LabelText("Business: دائم 600,000 | شهري 80,000 | سنوي 800,000 ج.س");
        LabelText("Enterprise: اشتراك شهري 30,000 | اشتراك سنوي 300,000 | شراء مرة واحدة 450,000 ج.س");
        LabelText("الدفع الدولي — الدولار الأمريكي (USD) | IBAN", 34, 14);
        LabelText("Basic: دائم $249 | شهري $9 | سنوي $90");
        LabelText("Pro: دائم $499 | شهري $19 | سنوي $190");
        LabelText("Business: دائم $999 | شهري $39 | سنوي $390");
        LabelText("Enterprise: دائم $1,999 | شهري $79 | سنوي $790");
        var market = new ComboBox { Width = 720, DropDownStyle = ComboBoxStyle.DropDownList };
        market.Items.AddRange(new object[] { "السودان — جنيه سوداني (بنكك)", "الدفع بالدولار الأمريكي — IBAN" }); market.SelectedIndex = 0;
        var tier = new ComboBox { Width = 720, DropDownStyle = ComboBoxStyle.DropDownList };
        tier.Items.AddRange(new object[] { "Basic", "Pro", "Business", "Enterprise" }); tier.SelectedIndex = 0;
        var period = new ComboBox { Width = 720, DropDownStyle = ComboBoxStyle.DropDownList };
        period.Items.AddRange(new object[] { "ترخيص دائم", "اشتراك شهري", "اشتراك سنوي" }); period.SelectedIndex = 0;
        LabelText("بلد/عملة الدفع");
        panel.Controls.Add(market);
        LabelText("الباقة");
        panel.Controls.Add(tier);
        LabelText("مدة الترخيص/الاشتراك");
        panel.Controls.Add(period);
        var price = new Label { Width = 740, Height = 42, Font = new Font("Segoe UI", 16, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        panel.Controls.Add(price);
        string PriceText()
        {
            int p = tier.SelectedIndex, d = period.SelectedIndex;
            string[][] usd = { new[] { "249", "9", "90" }, new[] { "499", "19", "190" }, new[] { "999", "39", "390" }, new[] { "1,999", "79", "790" } };
            string[][] sdg = { new[] { "150,000", "25,000", "250,000" }, new[] { "300,000", "45,000", "450,000" }, new[] { "600,000", "80,000", "800,000" }, new[] { "450,000", "30,000", "300,000" } };
            if (market.SelectedIndex == 1) return "$" + usd[p][d] + (d == 0 ? " — ترخيص دائم، دون تحديثات مستقبلية" : d == 1 ? " شهريًا" : " سنويًا");
            return sdg[p][d] + " ج.س" + (d == 0 ? " — شراء مرة واحدة، دون تحديثات مستقبلية" : d == 1 ? " شهريًا" : " سنويًا");
        }
        void UpdatePrice(object? sender, EventArgs e) => price.Text = UiLanguage.T("السعر المختار: ") + PriceText();
        market.SelectedIndexChanged += UpdatePrice; tier.SelectedIndexChanged += UpdatePrice; period.SelectedIndexChanged += UpdatePrice; UpdatePrice(this, EventArgs.Empty);
        var company = new TextBox { Width = 720, PlaceholderText = UiLanguage.T("اسم الشركة أو النشاط") }; panel.Controls.Add(company);
        var contact = new TextBox { Width = 720, PlaceholderText = UiLanguage.T("رقم واتساب للتواصل") }; panel.Controls.Add(contact);
        var notes = new TextBox { Width = 720, Height = 55, Multiline = true, PlaceholderText = UiLanguage.T("تفاصيل الخدمة المطلوبة أو عدد المستخدمين والفروع") }; panel.Controls.Add(notes);
        var bankak = new Button { Text = UiLanguage.T("📋 نسخ رقم حساب بنكك: 1882224"), Width = 720, Height = 38 };
        bankak.Click += (_, _) => { try { Clipboard.SetText("1882224"); MessageBox.Show(UiLanguage.T("تم نسخ رقم حساب بنكك.")); } catch { MessageBox.Show(UiLanguage.T("تعذر النسخ.")); } }; panel.Controls.Add(bankak);
        const string iban = "SD8504018822240001";
        var ibanLabel = new Label { Text = "IBAN: " + iban + " — تحقق من البنك أن الحساب يستقبل التحويل بالدولار قبل الإرسال.", Width = 740, Height = 48, TextAlign = ContentAlignment.MiddleCenter };
        panel.Controls.Add(ibanLabel);
        var copyIban = new Button { Text = UiLanguage.T("📋 نسخ رقم IBAN"), Width = 720, Height = 38 };
        copyIban.Click += (_, _) => { try { Clipboard.SetText(iban); MessageBox.Show(UiLanguage.T("تم نسخ رقم IBAN.")); } catch { MessageBox.Show(UiLanguage.T("تعذر النسخ.")); } }; panel.Controls.Add(copyIban);
        var send = new Button { Text = UiLanguage.T("💬 اختيار الباقة وإرسال إشعار عبر واتساب"), Width = 720, Height = 44 };
        send.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(company.Text)) { MessageBox.Show(UiLanguage.T("أدخل اسم الشركة أو النشاط أولًا")); return; }
            var message = "السلام عليكم، أريد طلب باقة من المحاسب الذكي.\nالشركة: " + company.Text.Trim() + "\nالسوق/العملة: " + market.SelectedItem + "\nالباقة: " + tier.SelectedItem + "\nالمدة: " + period.SelectedItem + "\nالسعر المعروض: " + PriceText() + "\nرقم تواصل العميل: " + contact.Text.Trim() + "\nالتفاصيل: " + notes.Text.Trim() + "\nبنكك: 1882224\nIBAN: " + iban + "\nيرجى تأكيد السعر النهائي وبيانات المستفيد والتحويل قبل التفعيل.";
            var url = "https://wa.me/249121851285?text=" + Uri.EscapeDataString(message);
            try { Process.Start(new ProcessStartInfo(url) { UseShellExecute = true }); } catch { MessageBox.Show(UiLanguage.T("تعذر فتح واتساب؛ أرسل الطلب يدويًا.")); }
        }; panel.Controls.Add(send);
        LabelText("إرسال الطلب لا يفعّل الترخيص تلقائيًا؛ يجب تأكيد الدفع يدويًا.", 42, 11);
        f.ShowDialog(this);
    }

    void PaymentDetails()
    {
        using var f = new Form { Text = UiLanguage.T("الدفع والتحويل البنكي"), Width = 650, Height = 390, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        const string iban = "SD8504018822240001";
        var title = new Label { Text = UiLanguage.T("بيانات التحويل البنكي بالدولار الأمريكي (USD)"), Dock = DockStyle.Top, Height = 48, TextAlign = ContentAlignment.MiddleCenter, Font = new Font("Segoe UI", 14, FontStyle.Bold) };
        var number = new TextBox { Text = iban, ReadOnly = true, Dock = DockStyle.Top, Height = 42, TextAlign = HorizontalAlignment.Center, Font = new Font("Segoe UI", 14, FontStyle.Bold) };
        var amount = new TextBox { PlaceholderText = UiLanguage.T("المبلغ المحوّل بالدولار"), Dock = DockStyle.Top, Height = 38 };
        var reference = new TextBox { PlaceholderText = UiLanguage.T("مرجع التحويل البنكي"), Dock = DockStyle.Top, Height = 38 };
        var copy = new Button { Text = UiLanguage.T("📋 نسخ رقم IBAN"), Dock = DockStyle.Top, Height = 44 };
        copy.Click += (_, _) => { try { Clipboard.SetText(iban); MessageBox.Show(UiLanguage.T("تم نسخ رقم IBAN.")); } catch { MessageBox.Show(UiLanguage.T("تعذر النسخ؛ حدد الرقم وانسخه يدويًا.")); } };
        var notice = new Button { Text = UiLanguage.T("💬 إرسال إشعار التحويل عبر واتساب"), Dock = DockStyle.Top, Height = 48 };
        notice.Click += (_, _) => {
            if (string.IsNullOrWhiteSpace(amount.Text) || string.IsNullOrWhiteSpace(reference.Text)) { MessageBox.Show(UiLanguage.T("أدخل المبلغ ومرجع التحويل.")); return; }
            var message = "السلام عليكم، أرسل إشعار تحويل بنكي لشراء ترخيص المحاسب الذكي.\\nالمبلغ: " + amount.Text.Trim() + " USD\\nمرجع التحويل: " + reference.Text.Trim() + "\\nIBAN المستفيد: " + iban + "\\nيرجى تأكيد وصول المبلغ قبل إصدار الترخيص. سأرفق إيصال التحويل في المحادثة.";
            var url = "https://wa.me/249121851285?text=" + Uri.EscapeDataString(message);
            try { Process.Start(new ProcessStartInfo(url) { UseShellExecute = true }); } catch { MessageBox.Show(UiLanguage.T("تعذر فتح واتساب؛ انسخ الإشعار وأرسله للدعم يدويًا.")); }
        };
        var help = new Label { Text = UiLanguage.T("تحقق من اسم المستفيد والعملة والرسوم مع البنك. لا ترسل كلمات مرور أو رموز تحقق. لا يُصدر الترخيص إلا بعد تأكيد وصول المبلغ."), Dock = DockStyle.Top, Height = 60, TextAlign = ContentAlignment.MiddleCenter };
        f.Controls.Add(help); f.Controls.Add(notice); f.Controls.Add(reference); f.Controls.Add(amount); f.Controls.Add(copy); f.Controls.Add(number); f.Controls.Add(title); f.ShowDialog(this);
    }

    void ToggleLanguage()
    {
        UiLanguage.English = !UiLanguage.English;
        Application.Restart();
    }

    void Add(string text, Action action)
    {
        var b = new Button { Text = UiLanguage.T(text), Width = 520, Height = 52, Font = new Font("Segoe UI", 13), Margin = new Padding(4) };
        b.Click += (_, _) => action(); menu.Controls.Add(b);
    }
    SqliteConnection C() { var c = new SqliteConnection($"Data Source={dbPath}"); c.Open(); return c; }
    void InitDb()
    {
        using var c = C(); using var x = c.CreateCommand();
        x.CommandText = @"CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,qty REAL NOT NULL DEFAULT 0,price REAL NOT NULL DEFAULT 0); CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL); CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL); CREATE TABLE IF NOT EXISTS employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,job_title TEXT NOT NULL DEFAULT '',salary REAL NOT NULL DEFAULT 0); CREATE TABLE IF NOT EXISTS payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,description TEXT NOT NULL,amount REAL NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS recurring_expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,amount REAL NOT NULL CHECK(amount>0),frequency TEXT NOT NULL CHECK(frequency IN ('MONTHLY','YEARLY')),next_due TEXT NOT NULL,active INTEGER NOT NULL DEFAULT 1);
CREATE TABLE IF NOT EXISTS recurring_expense_runs(id INTEGER PRIMARY KEY AUTOINCREMENT,recurring_id INTEGER NOT NULL,period_key TEXT NOT NULL,expense_id INTEGER NOT NULL,UNIQUE(recurring_id,period_key));
CREATE TABLE IF NOT EXISTS fixed_assets(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,cost REAL NOT NULL CHECK(cost>0),life_months INTEGER NOT NULL CHECK(life_months>0),ready_date TEXT NOT NULL,acquisition_entry_id INTEGER,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS asset_depreciation(id INTEGER PRIMARY KEY AUTOINCREMENT,asset_id INTEGER NOT NULL,period TEXT NOT NULL,amount REAL NOT NULL CHECK(amount>0),journal_entry_id INTEGER,UNIQUE(asset_id,period));
CREATE TABLE IF NOT EXISTS factory_orders(id INTEGER PRIMARY KEY AUTOINCREMENT,order_name TEXT NOT NULL UNIQUE,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS factory_stage_events(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER NOT NULL,stage_name TEXT NOT NULL,material_cost REAL NOT NULL,labor_cost REAL NOT NULL,overhead_cost REAL NOT NULL,good_qty REAL NOT NULL,scrap_qty REAL NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,journal_entry_id INTEGER);
";
        x.ExecuteNonQuery();
        // Additive schema migrations; never drop or rewrite existing transaction rows.
        try { using var m = c.CreateCommand(); m.CommandText = "ALTER TABLE sales ADD COLUMN customer_id INTEGER"; m.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var m = c.CreateCommand(); m.CommandText = "ALTER TABLE sales ADD COLUMN payment_mode TEXT NOT NULL DEFAULT 'CASH'"; m.ExecuteNonQuery(); } catch (SqliteException) { }
        // Store the cost at sale time so future returns can reverse COGS without guessing historical costs.
        try { using var m = c.CreateCommand(); m.CommandText = "ALTER TABLE sales ADD COLUMN unit_cost REAL NOT NULL DEFAULT 0"; m.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var m = c.CreateCommand(); m.CommandText = "ALTER TABLE purchases ADD COLUMN supplier_id INTEGER"; m.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var m = c.CreateCommand(); m.CommandText = "ALTER TABLE purchases ADD COLUMN payment_mode TEXT NOT NULL DEFAULT 'CASH'"; m.ExecuteNonQuery(); } catch (SqliteException) { }
        // Additive, non-destructive schema changes preserve existing customer data and transactions.
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE products ADD COLUMN barcode TEXT"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE products ADD COLUMN unit_cost REAL NOT NULL DEFAULT 0"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        using (var ledger = c.CreateCommand())
        {
            ledger.CommandText = @"CREATE TABLE IF NOT EXISTS gl_accounts(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL UNIQUE,name TEXT NOT NULL UNIQUE,kind TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS gl_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,source_type TEXT NOT NULL,source_id INTEGER NOT NULL,memo TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(source_type,source_id));
CREATE TABLE IF NOT EXISTS gl_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,entry_id INTEGER NOT NULL REFERENCES gl_entries(id),account_id INTEGER NOT NULL REFERENCES gl_accounts(id),debit REAL NOT NULL DEFAULT 0,credit REAL NOT NULL DEFAULT 0,CHECK(debit>=0 AND credit>=0 AND NOT(debit>0 AND credit>0)));
INSERT OR IGNORE INTO gl_accounts(code,name,kind) VALUES ('1000','Cash','ASSET'),('1100','Accounts Receivable','ASSET'),('1200','Inventory','ASSET'),('2000','Accounts Payable','LIABILITY'),('2100','Salaries Payable','LIABILITY'),('3000','Opening Equity','EQUITY'),('4000','Sales Revenue','REVENUE'),('5000','Cost of Goods Sold','EXPENSE'),('6100','Operating Expense','EXPENSE'),('6200','Salaries Expense','EXPENSE'),('1300','Employee Advances','ASSET'),('6300','Payroll Deductions','EXPENSE'),('4100','Sales Returns','CONTRA_REVENUE'),('4200','Purchase Return Gain','OTHER_INCOME'),('6400','Purchase Return Loss','EXPENSE'),('1500','Fixed Assets','ASSET'),('1590','Accumulated Depreciation','CONTRA_ASSET'),('6500','Depreciation Expense','EXPENSE'),('1400','Work in Process','ASSET'),('1210','Raw Materials','ASSET'),('2200','Salaries Payable','LIABILITY'),('2300','Manufacturing Overhead Payable','LIABILITY');";
            ledger.ExecuteNonQuery();
        }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE employees ADD COLUMN job_title TEXT NOT NULL DEFAULT ''"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE employees ADD COLUMN salary REAL NOT NULL DEFAULT 0"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "CREATE TABLE IF NOT EXISTS payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
// Additive registers preserve historical rows and support explicit settlement documents.
using (var payroll = c.CreateCommand()) { payroll.CommandText = "CREATE TABLE IF NOT EXISTS payroll_runs(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,period_month TEXT NOT NULL,base_salary REAL NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(employee_id,period_month))"; payroll.ExecuteNonQuery(); }
        // Explicit payroll settlements are additive and unique per employee/month; historical data remains untouched.
        using (var payrollSettlement = c.CreateCommand()) { payrollSettlement.CommandText = "CREATE TABLE IF NOT EXISTS payroll_settlements(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,period_month TEXT NOT NULL,base_salary REAL NOT NULL,advance_offset REAL NOT NULL,cash_paid REAL NOT NULL,note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(employee_id,period_month))"; payrollSettlement.ExecuteNonQuery(); }
        using (var settlements = c.CreateCommand()) { settlements.CommandText = "CREATE TABLE IF NOT EXISTS customer_receipts(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,amount REAL NOT NULL CHECK(amount>0),note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS supplier_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,supplier_id INTEGER NOT NULL,amount REAL NOT NULL CHECK(amount>0),note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);"; settlements.ExecuteNonQuery(); }
        // Additive return registers; original sales/purchases are retained for auditability.
        using (var returns = c.CreateCommand()) { returns.CommandText = "CREATE TABLE IF NOT EXISTS sales_returns(id INTEGER PRIMARY KEY AUTOINCREMENT,sale_id INTEGER NOT NULL,product TEXT NOT NULL,qty REAL NOT NULL CHECK(qty>0),unit_price REAL NOT NULL CHECK(unit_price>=0),unit_cost REAL NOT NULL CHECK(unit_cost>=0),total REAL NOT NULL CHECK(total>=0),note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS purchase_returns(id INTEGER PRIMARY KEY AUTOINCREMENT,purchase_id INTEGER NOT NULL,product TEXT NOT NULL,qty REAL NOT NULL CHECK(qty>0),unit_price REAL NOT NULL CHECK(unit_price>=0),total REAL NOT NULL CHECK(total>=0),note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);"; returns.ExecuteNonQuery(); }
        using (var manualJournals = c.CreateCommand())
        {
            manualJournals.CommandText = "CREATE TABLE IF NOT EXISTS manual_journals(id INTEGER PRIMARY KEY AUTOINCREMENT,memo TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)";
            manualJournals.ExecuteNonQuery();
        }
        using (var reversals = c.CreateCommand())
        {
            reversals.CommandText = "CREATE TABLE IF NOT EXISTS gl_reversals(id INTEGER PRIMARY KEY AUTOINCREMENT,original_entry_id INTEGER NOT NULL UNIQUE REFERENCES gl_entries(id),reversal_entry_id INTEGER NOT NULL UNIQUE REFERENCES gl_entries(id),reason TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)";
            reversals.ExecuteNonQuery();
        }
        try { using var indexName = c.CreateCommand(); indexName.CommandText = "CREATE UNIQUE INDEX IF NOT EXISTS idx_products_name ON products(name COLLATE NOCASE)"; indexName.ExecuteNonQuery(); } catch (SqliteException) { }
        using var index = c.CreateCommand(); index.CommandText = "CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL AND barcode != ''"; index.ExecuteNonQuery();
    }
    void Products()
    {
        using var f = new Form { Text=UiLanguage.T("الأصناف والمخزون والباركود"), Width=700, Height=600, RightToLeft=UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout=!UiLanguage.English };
        var n=new TextBox{PlaceholderText=UiLanguage.T("اسم الصنف"),Dock=DockStyle.Top,Height=40}; var barcode=new TextBox{PlaceholderText=UiLanguage.T("الباركود (ماسح USB/Bluetooth أو إدخال يدوي)"),Dock=DockStyle.Top,Height=40}; var q=new TextBox{PlaceholderText=UiLanguage.T("الكمية"),Dock=DockStyle.Top,Height=40}; var p=new TextBox{PlaceholderText=UiLanguage.T("سعر البيع"),Dock=DockStyle.Top,Height=40}; var cost=new TextBox{PlaceholderText=UiLanguage.T("تكلفة الوحدة الافتتاحية"),Dock=DockStyle.Top,Height=40};
        var add=new Button{Text=UiLanguage.T("إضافة الصنف"),Dock=DockStyle.Top,Height=45}; var list=new ListBox{Dock=DockStyle.Fill};
        add.Click+=(_,_)=>{if(string.IsNullOrWhiteSpace(n.Text)||!double.TryParse(q.Text,System.Globalization.NumberStyles.Number,System.Globalization.CultureInfo.CurrentCulture,out var qty)||!double.TryParse(p.Text,System.Globalization.NumberStyles.Number,System.Globalization.CultureInfo.CurrentCulture,out var price)||!double.TryParse(cost.Text,System.Globalization.NumberStyles.Number,System.Globalization.CultureInfo.CurrentCulture,out var unitCost)||qty<0||price<0||unitCost<0||(qty>0&&unitCost<=0)){MessageBox.Show(UiLanguage.T("أدخل اسمًا وكمية وسعرًا صحيحًا، وتكلفة موجبة إذا كان المخزون أكبر من صفر."));return;}try{using var c=C();using var tx=c.BeginTransaction();using var x=c.CreateCommand();x.Transaction=tx;x.CommandText="INSERT INTO products(name,barcode,qty,price,unit_cost) VALUES($n,$b,$q,$p,$cost)";x.Parameters.AddWithValue("$n",n.Text.Trim());x.Parameters.AddWithValue("$b",string.IsNullOrWhiteSpace(barcode.Text)?DBNull.Value:barcode.Text.Trim());x.Parameters.AddWithValue("$q",qty);x.Parameters.AddWithValue("$p",price);x.Parameters.AddWithValue("$cost",unitCost);x.ExecuteNonQuery();var id=LastId(c, tx);if(qty>0&&unitCost>0)PostEntry(c,tx,"OPENING_STOCK",id,"Opening inventory: "+n.Text.Trim(),new (string Account, double Debit, double Credit)[] { ("Inventory",qty*unitCost,0d), ("Opening Equity",0d,qty*unitCost) });tx.Commit();list.Items.Add(n.Text+" | باركود "+barcode.Text+" | المخزون "+qty+" | السعر "+price);n.Clear();barcode.Clear();q.Clear();p.Clear();cost.Clear();RefreshSummary();}catch(SqliteException){MessageBox.Show(UiLanguage.T("تعذر الحفظ: الاسم أو الباركود مستخدم مسبقًا."));}};
        f.Controls.Add(list);f.Controls.Add(add);f.Controls.Add(cost);f.Controls.Add(p);f.Controls.Add(q);f.Controls.Add(barcode);f.Controls.Add(n);f.ShowDialog();
    }
    void Transaction(bool purchase)
    {
        using var f = new Form { Text = UiLanguage.T(purchase ? "المشتريات" : "المبيعات"), Width = 680, Height = 520, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var n = new TextBox { PlaceholderText = UiLanguage.T("اسم الصنف أو الباركود"), Dock = DockStyle.Top, Height = 40 };
        var q = new TextBox { PlaceholderText = UiLanguage.T("الكمية"), Dock = DockStyle.Top, Height = 40 };
        var p = new TextBox { PlaceholderText = UiLanguage.T("السعر"), Dock = DockStyle.Top, Height = 40 };
        var paymentMode = new ComboBox { Dock = DockStyle.Top, Height = 40, DropDownStyle = ComboBoxStyle.DropDownList };
        paymentMode.Items.Add(UiLanguage.English ? "Cash" : "نقدي");
        paymentMode.Items.Add(UiLanguage.English ? "On account (credit)" : "آجل");
        paymentMode.SelectedIndex = 0;
        var party = new ComboBox { Dock = DockStyle.Top, Height = 40, DropDownStyle = ComboBoxStyle.DropDownList, Visible = false };
        var partyIds = new List<long>();
        var partyLabel = new Label { Text = UiLanguage.English ? (purchase ? "Supplier (required for credit)" : "Customer (required for credit)") : (purchase ? "المورد (مطلوب للشراء الآجل)" : "العميل (مطلوب للبيع الآجل)"), Dock = DockStyle.Top, Height = 28, Visible = false };
        var save = new Button { Text = UiLanguage.T("حفظ"), Dock = DockStyle.Top, Height = 45 };

        using (var c = C())
        using (var load = c.CreateCommand())
        {
            load.CommandText = purchase ? "SELECT id,name FROM suppliers ORDER BY name" : "SELECT id,name FROM customers ORDER BY name";
            using var reader = load.ExecuteReader();
            while (reader.Read())
            {
                partyIds.Add(reader.GetInt64(0));
                party.Items.Add(reader.GetString(1));
            }
        }
        paymentMode.SelectedIndexChanged += (_, _) =>
        {
            var credit = paymentMode.SelectedIndex == 1;
            party.Visible = credit; partyLabel.Visible = credit;
        };
        save.Click += (_, _) =>
        {
            if (!double.TryParse(q.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var qty) ||
                !double.TryParse(p.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var price) ||
                qty <= 0 || price < 0 || string.IsNullOrWhiteSpace(n.Text))
            {
                MessageBox.Show(UiLanguage.T("أدخل صنفًا وكمية موجبة وسعرًا صحيحًا."));
                return;
            }
            var isCredit = paymentMode.SelectedIndex == 1;
            if (isCredit && party.SelectedIndex < 0)
            {
                MessageBox.Show(UiLanguage.English ? "Add/select a customer or supplier before saving a credit transaction." : "أضف واختر العميل أو المورد قبل حفظ العملية الآجلة.");
                return;
            }
            try
            {
                using var c = C();
                using var tx = c.BeginTransaction();
                using var check = c.CreateCommand();
                check.Transaction = tx;
                check.CommandText = "SELECT id, qty, name, COALESCE(unit_cost,0) FROM products WHERE name=$n OR barcode=$n LIMIT 1";
                check.Parameters.AddWithValue("$n", n.Text.Trim());
                using var reader = check.ExecuteReader();
                if (!reader.Read())
                {
                    MessageBox.Show(UiLanguage.T("الصنف غير موجود."));
                    return;
                }
                var productId = reader.GetInt64(0);
                var oldQty = reader.GetDouble(1);
                var productName = reader.GetString(2);
                var oldUnitCost = reader.GetDouble(3);
                reader.Close();
                var next = oldQty + (purchase ? qty : -qty);
                if (next < 0)
                {
                    MessageBox.Show(UiLanguage.T("الرصيد غير كافٍ."));
                    return;
                }
                using var update = c.CreateCommand();
                update.Transaction = tx;
                var nextUnitCost = purchase && next > 0 ? ((oldQty * oldUnitCost) + (qty * price)) / next : oldUnitCost;
                update.CommandText = "UPDATE products SET qty=$q, unit_cost=$cost WHERE id=$id";
                update.Parameters.AddWithValue("$q", next);
                update.Parameters.AddWithValue("$cost", nextUnitCost);
                update.Parameters.AddWithValue("$id", productId);
                update.ExecuteNonQuery();

                var partyId = isCredit ? partyIds[party.SelectedIndex] : (long?)null;
                var partyName = isCredit ? party.SelectedItem?.ToString() ?? "" : "";
                using var insert = c.CreateCommand();
                insert.Transaction = tx;
                insert.CommandText = purchase
                    ? "INSERT INTO purchases(product,qty,price,total,supplier_id,payment_mode) VALUES($n,$q,$p,$t,$party,$mode)"
                    : "INSERT INTO sales(product,qty,price,total,customer_id,payment_mode,unit_cost) VALUES($n,$q,$p,$t,$party,$mode,$cost)";
                insert.Parameters.AddWithValue("$n", productName);
                insert.Parameters.AddWithValue("$q", qty);
                insert.Parameters.AddWithValue("$p", price);
                insert.Parameters.AddWithValue("$t", qty * price);
                insert.Parameters.AddWithValue("$party", partyId.HasValue ? partyId.Value : DBNull.Value);
                insert.Parameters.AddWithValue("$mode", isCredit ? "CREDIT" : "CASH");
                if (!purchase) insert.Parameters.AddWithValue("$cost", oldUnitCost);
                insert.ExecuteNonQuery();
                var transactionId = LastId(c, tx);
                var total = qty * price;
                if (purchase)
                {
                    var creditAccount = isCredit ? "Accounts Payable" : "Cash";
                    PostEntry(c, tx, "PURCHASE", transactionId,
                        (isCredit ? "Credit purchase from " : "Cash purchase: ") + productName + (isCredit ? " — " + partyName : ""),
                        new (string Account, double Debit, double Credit)[] { ("Inventory", total, 0d), (creditAccount, 0d, total) });
                }
                else
                {
                    var cogs = qty * oldUnitCost;
                    var debitAccount = isCredit ? "Accounts Receivable" : "Cash";
                    PostEntry(c, tx, "SALE", transactionId,
                        (isCredit ? "Credit sale to " : "Cash sale: ") + productName + (isCredit ? " — " + partyName : ""),
                        new (string Account, double Debit, double Credit)[] { (debitAccount, total, 0d), ("Sales Revenue", 0d, total), ("Cost of Goods Sold", cogs, 0d), ("Inventory", 0d, cogs) });
                }
                tx.Commit();
                MessageBox.Show(UiLanguage.T("تم الحفظ وتحديث المخزون."));
                RefreshSummary();
                f.Close();
            }
            catch (Exception)
            {
                MessageBox.Show(UiLanguage.T("تعذر حفظ العملية؛ لم تُعتمد العملية."));
            }
        };
        f.Controls.Add(save); f.Controls.Add(party); f.Controls.Add(partyLabel); f.Controls.Add(paymentMode); f.Controls.Add(p); f.Controls.Add(q); f.Controls.Add(n);
        f.ShowDialog();
    }


    void Returns()
    {
        using var f = new Form { Text = "المرتجعات", Width = 760, Height = 540, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var kind = new ComboBox { Dock = DockStyle.Top, Height = 38, DropDownStyle = ComboBoxStyle.DropDownList };
        kind.Items.Add("مرتجع مبيعات"); kind.Items.Add("مرتجع مشتريات"); kind.SelectedIndex = 0;
        var original = new ComboBox { Dock = DockStyle.Top, Height = 42, DropDownStyle = ComboBoxStyle.DropDownList };
        var quantity = new TextBox { Dock = DockStyle.Top, Height = 38, PlaceholderText = "كمية المرتجع" };
        var note = new TextBox { Dock = DockStyle.Top, Height = 38, PlaceholderText = "سبب / مرجع المرتجع" };
        var save = new Button { Dock = DockStyle.Top, Height = 44, Text = "حفظ المرتجع وترحيله" };
        var list = new ListBox { Dock = DockStyle.Fill };
        var ids = new List<long>();
        void LoadOriginals()
        {
            original.Items.Clear(); ids.Clear();
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = kind.SelectedIndex == 0
                ? "SELECT s.id,s.product,s.qty,s.price,s.payment_mode,COALESCE(s.unit_cost,0),(SELECT COALESCE(SUM(r.qty),0) FROM sales_returns r WHERE r.sale_id=s.id) FROM sales s ORDER BY s.id DESC"
                : "SELECT p.id,p.product,p.qty,p.price,p.payment_mode,0,(SELECT COALESCE(SUM(r.qty),0) FROM purchase_returns r WHERE r.purchase_id=p.id) FROM purchases p ORDER BY p.id DESC";
            using var reader = q.ExecuteReader();
            while (reader.Read())
            {
                long id = reader.GetInt64(0); string product = reader.GetString(1);
                double available = reader.GetDouble(2) - reader.GetDouble(6);
                if (available <= 0.0000001) continue;
                if (kind.SelectedIndex == 0 && reader.GetDouble(5) <= 0)
                {
                    original.Items.Add("#" + id + " | " + product + " | متاح " + available.ToString("N3") + " | تكلفة تاريخية غير متوفرة");
                    ids.Add(-id);
                }
                else
                {
                    original.Items.Add("#" + id + " | " + product + " | متاح " + available.ToString("N3") + " | " + reader.GetDouble(3).ToString("N2"));
                    ids.Add(id);
                }
            }
            if (original.Items.Count > 0) original.SelectedIndex = 0;
        }
        void ReloadReturns()
        {
            list.Items.Clear();
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = "SELECT 'بيع' AS k,id,product,qty,total,created_at FROM sales_returns UNION ALL SELECT 'شراء',id,product,qty,total,created_at FROM purchase_returns ORDER BY created_at DESC LIMIT 100";
            using var reader = q.ExecuteReader();
            while (reader.Read()) list.Items.Add(reader.GetString(0) + " #" + reader.GetInt64(1) + " | " + reader.GetString(2) + " | كمية " + reader.GetDouble(3).ToString("N3") + " | " + reader.GetDouble(4).ToString("N2") + " | " + reader.GetString(5));
        }
        kind.SelectedIndexChanged += (_, _) => LoadOriginals();
        save.Click += (_, _) =>
        {
            if (original.SelectedIndex < 0 || !double.TryParse(quantity.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var qty) || qty <= 0)
            { MessageBox.Show("اختر الفاتورة وأدخل كمية مرتجع موجبة."); return; }
            if (ids[original.SelectedIndex] < 0)
            { MessageBox.Show("هذه فاتورة تاريخية لا تحتوي تكلفة البيع وقت العملية. يلزم مراجعة التكلفة قبل ترحيل المرتجع."); return; }
            long sourceId = ids[original.SelectedIndex];
            try
            {
                using var c = C(); using var tx = c.BeginTransaction();
                if (kind.SelectedIndex == 0)
                {
                    long productId; long? customerId; string product; string mode; double originalQty, price, cost, alreadyReturned, stock, stockCost;
                    using (var q = c.CreateCommand())
                    {
                        q.Transaction = tx;
                        q.CommandText = "SELECT s.product,s.qty,s.price,s.unit_cost,s.payment_mode,(SELECT COALESCE(SUM(r.qty),0) FROM sales_returns r WHERE r.sale_id=s.id),p.id,p.qty,p.unit_cost,s.customer_id FROM sales s JOIN products p ON p.name=s.product WHERE s.id=$id";
                        q.Parameters.AddWithValue("$id", sourceId);
                        using var reader = q.ExecuteReader();
                        if (!reader.Read()) throw new InvalidOperationException("Original sale or product not found.");
                        product=reader.GetString(0); originalQty=reader.GetDouble(1); price=reader.GetDouble(2); cost=reader.GetDouble(3); mode=reader.GetString(4); alreadyReturned=reader.GetDouble(5); productId=reader.GetInt64(6); stock=reader.GetDouble(7); stockCost=reader.GetDouble(8); customerId=reader.IsDBNull(9)?null:reader.GetInt64(9);
                    }
                    if (qty > originalQty - alreadyReturned + 0.0000001) throw new InvalidOperationException("Return quantity exceeds the unreturned sale quantity.");
                    double total = qty * price; double newStock = stock + qty;
                    double newCost = newStock > 0 ? ((stock * stockCost) + (qty * cost)) / newStock : cost;
                    using (var update = c.CreateCommand())
                    {
                        update.Transaction=tx; update.CommandText="UPDATE products SET qty=$q,unit_cost=$c WHERE id=$id";
                        update.Parameters.AddWithValue("$q",newStock); update.Parameters.AddWithValue("$c",newCost); update.Parameters.AddWithValue("$id",productId); update.ExecuteNonQuery();
                    }
                    using (var insert = c.CreateCommand())
                    {
                        insert.Transaction=tx; insert.CommandText="INSERT INTO sales_returns(sale_id,product,qty,unit_price,unit_cost,total,note) VALUES($s,$p,$q,$u,$c,$t,$n)";
                        insert.Parameters.AddWithValue("$s",sourceId); insert.Parameters.AddWithValue("$p",product); insert.Parameters.AddWithValue("$q",qty); insert.Parameters.AddWithValue("$u",price); insert.Parameters.AddWithValue("$c",cost); insert.Parameters.AddWithValue("$t",total); insert.Parameters.AddWithValue("$n",note.Text.Trim()); insert.ExecuteNonQuery();
                    }
                    long returnId = LastId(c,tx);
                    double arReduction = 0d;
                    if (mode == "CREDIT" && customerId.HasValue)
                    {
                        using var outstanding = c.CreateCommand(); outstanding.Transaction = tx;
                        outstanding.CommandText = @"SELECT COALESCE((SELECT SUM(total) FROM sales WHERE customer_id=$id AND payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(r.total) FROM sales_returns r JOIN sales s ON s.id=r.sale_id WHERE s.customer_id=$id AND s.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(amount) FROM customer_receipts WHERE customer_id=$id),0)";
                        outstanding.Parameters.AddWithValue("$id", customerId.Value);
                        arReduction = Math.Min(total, Math.Max(0d, Convert.ToDouble(outstanding.ExecuteScalar())));
                    }
                    var returnLines = new List<(string Account,double Debit,double Credit)>
                    {
                        ("Sales Returns", total, 0d),
                        ("Inventory", qty*cost, 0d),
                        ("Cost of Goods Sold", 0d, qty*cost)
                    };
                    if (arReduction > 0.005) returnLines.Add(("Accounts Receivable", 0d, arReduction));
                    if (total - arReduction > 0.005) returnLines.Add(("Cash", 0d, total - arReduction));
                    PostEntry(c,tx,"SALE_RETURN",returnId,"Sales return: "+product,returnLines.ToArray());
                }
                else
                {
                    long productId; long? supplierId; string product; string mode; double originalQty, price, alreadyReturned, stock, stockCost;
                    using (var q = c.CreateCommand())
                    {
                        q.Transaction=tx;
                        q.CommandText="SELECT p.product,p.qty,p.price,p.payment_mode,(SELECT COALESCE(SUM(r.qty),0) FROM purchase_returns r WHERE r.purchase_id=p.id),i.id,i.qty,i.unit_cost,p.supplier_id FROM purchases p JOIN products i ON i.name=p.product WHERE p.id=$id";
                        q.Parameters.AddWithValue("$id",sourceId);
                        using var reader=q.ExecuteReader();
                        if(!reader.Read()) throw new InvalidOperationException("Original purchase or product not found.");
                        product=reader.GetString(0); originalQty=reader.GetDouble(1); price=reader.GetDouble(2); mode=reader.GetString(3); alreadyReturned=reader.GetDouble(4); productId=reader.GetInt64(5); stock=reader.GetDouble(6); stockCost=reader.GetDouble(7); supplierId=reader.IsDBNull(8)?null:reader.GetInt64(8);
                    }
                    if(qty>originalQty-alreadyReturned+0.0000001) throw new InvalidOperationException("Return quantity exceeds the unreturned purchase quantity.");
                    if(qty>stock+0.0000001) throw new InvalidOperationException("Cannot return stock that is no longer available.");
                    if (stockCost <= 0.0000001)
                        throw new InvalidOperationException("Historical weighted-average unit cost is missing; reconcile inventory valuation before posting this purchase return.");
                    double total=qty*price;
                    double inventoryCost=qty*stockCost;
                    double newStock=stock-qty;
                    // Perpetual weighted-average costing: remove returned stock at the current carrying cost,
                    // not the supplier's historical invoice price. Keep the remaining average cost unchanged.
                    double newCost=newStock>0.0000001?stockCost:0d;
                    using(var update=c.CreateCommand())
                    {
                        update.Transaction=tx; update.CommandText="UPDATE products SET qty=$q,unit_cost=$c WHERE id=$id";
                        update.Parameters.AddWithValue("$q",newStock); update.Parameters.AddWithValue("$c",newCost); update.Parameters.AddWithValue("$id",productId); update.ExecuteNonQuery();
                    }
                    using(var insert=c.CreateCommand())
                    {
                        insert.Transaction=tx; insert.CommandText="INSERT INTO purchase_returns(purchase_id,product,qty,unit_price,total,note) VALUES($s,$p,$q,$u,$t,$n)";
                        insert.Parameters.AddWithValue("$s",sourceId); insert.Parameters.AddWithValue("$p",product); insert.Parameters.AddWithValue("$q",qty); insert.Parameters.AddWithValue("$u",price); insert.Parameters.AddWithValue("$t",total); insert.Parameters.AddWithValue("$n",note.Text.Trim()); insert.ExecuteNonQuery();
                    }
                    long returnId=LastId(c,tx);
                    double apReduction = 0d;
                    if (mode == "CREDIT" && supplierId.HasValue)
                    {
                        using var outstanding = c.CreateCommand(); outstanding.Transaction = tx;
                        outstanding.CommandText = @"SELECT COALESCE((SELECT SUM(total) FROM purchases WHERE supplier_id=$id AND payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(r.total) FROM purchase_returns r JOIN purchases p ON p.id=r.purchase_id WHERE p.supplier_id=$id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(amount) FROM supplier_payments WHERE supplier_id=$id),0)";
                        outstanding.Parameters.AddWithValue("$id", supplierId.Value);
                        apReduction = Math.Min(total, Math.Max(0d, Convert.ToDouble(outstanding.ExecuteScalar())));
                    }
                    var returnLines = new List<(string Account,double Debit,double Credit)>();
                    if (apReduction > 0.005) returnLines.Add(("Accounts Payable", apReduction, 0d));
                    if (total - apReduction > 0.005) returnLines.Add(("Cash", total - apReduction, 0d));
                    returnLines.Add(("Inventory", 0d, inventoryCost));
                    // The supplier credit/refund follows the original purchase price, while inventory
                    // is relieved at its carrying cost. Post the difference explicitly to avoid
                    // corrupting inventory valuation or forcing a hidden imbalance.
                    if (total > inventoryCost + 0.005)
                        returnLines.Add(("Purchase Return Gain", 0d, total - inventoryCost));
                    else if (inventoryCost > total + 0.005)
                        returnLines.Add(("Purchase Return Loss", inventoryCost - total, 0d));
                    PostEntry(c,tx,"PURCHASE_RETURN",returnId,"Purchase return: "+product,returnLines.ToArray());
                }
                tx.Commit(); quantity.Clear(); note.Clear(); LoadOriginals(); ReloadReturns(); RefreshSummary();
                MessageBox.Show("تم حفظ المرتجع وترحيله. راجع دفتر الأستاذ وميزان المراجعة.");
            }
            catch(Exception ex)
            {
                MessageBox.Show("تعذر حفظ المرتجع؛ أُلغيت العملية بالكامل. "+ex.Message);
            }
        };
        f.Controls.Add(list); f.Controls.Add(save); f.Controls.Add(note); f.Controls.Add(quantity); f.Controls.Add(original); f.Controls.Add(kind);
        LoadOriginals(); ReloadReturns(); f.ShowDialog();
    }

    void RecurringExpenses()
    {
        using var f = new Form { Text = UiLanguage.T("المصروفات الدورية والإيجار والكهرباء"), Width = 900, Height = 700, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var name = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("البيان: إيجار / كهرباء / إنترنت"), Height = 38 };
        var amount = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("قيمة الاستحقاق"), Height = 38 };
        var frequency = new ComboBox { Dock = DockStyle.Top, DropDownStyle = ComboBoxStyle.DropDownList, Height = 36 }; frequency.Items.AddRange(new object[] { "شهري", "سنوي" }); frequency.SelectedIndex = 0;
        var due = new TextBox { Dock = DockStyle.Top, PlaceholderText = "YYYY-MM-DD — تاريخ الاستحقاق", Height = 38 };
        var list = new ListBox { Dock = DockStyle.Fill };
        var save = new Button { Dock = DockStyle.Top, Height = 42, Text = UiLanguage.T("حفظ تعريف المصروف الدوري") };
        var generate = new Button { Dock = DockStyle.Top, Height = 42, Text = UiLanguage.T("إنشاء استحقاقات الفترة الحالية مرة واحدة") };
        f.Controls.Add(list); f.Controls.Add(generate); f.Controls.Add(save); f.Controls.Add(due); f.Controls.Add(frequency); f.Controls.Add(amount); f.Controls.Add(name);
        void Refresh()
        {
            list.Items.Clear(); using var c = C(); using var x = c.CreateCommand(); x.CommandText = "SELECT id,name,amount,frequency,next_due FROM recurring_expenses WHERE active=1 ORDER BY id DESC";
            using var r = x.ExecuteReader(); while (r.Read()) list.Items.Add($"#{r.GetInt64(0)} | {r.GetString(1)} | {r.GetDouble(2):N2} | {r.GetString(3)} | {r.GetString(4)}");
        }
        save.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(name.Text) || !double.TryParse(amount.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var value) || !double.IsFinite(value) || value <= 0 || !DateTime.TryParseExact(due.Text.Trim(), "yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture, System.Globalization.DateTimeStyles.None, out _))
            { MessageBox.Show(UiLanguage.T("أدخل بيانًا ومبلغًا موجبًا وتاريخًا بصيغة YYYY-MM-DD.")); return; }
            using var c = C(); using var x = c.CreateCommand(); x.CommandText = "INSERT INTO recurring_expenses(name,amount,frequency,next_due) VALUES($n,$a,$f,$d)"; x.Parameters.AddWithValue("$n", name.Text.Trim()); x.Parameters.AddWithValue("$a", value); x.Parameters.AddWithValue("$f", frequency.SelectedIndex == 1 ? "YEARLY" : "MONTHLY"); x.Parameters.AddWithValue("$d", due.Text.Trim()); x.ExecuteNonQuery(); Refresh();
        };
        generate.Click += (_, _) =>
        {
            int created = 0; using var c = C(); using var tx = c.BeginTransaction();
            try
            {
                using var q = c.CreateCommand(); q.Transaction = tx; q.CommandText = "SELECT id,name,amount,frequency,next_due FROM recurring_expenses WHERE active=1 ORDER BY id";
                var rows = new List<(long Id,string Name,double Amount,string Frequency,string Due)>(); using (var r=q.ExecuteReader()) while(r.Read()) rows.Add((r.GetInt64(0),r.GetString(1),r.GetDouble(2),r.GetString(3),r.GetString(4)));
                foreach(var item in rows)
                {
                    if(!DateTime.TryParseExact(item.Due,"yyyy-MM-dd",System.Globalization.CultureInfo.InvariantCulture,System.Globalization.DateTimeStyles.None,out var date)) continue;
                    var now=DateTime.Today; if(date.Date>now || (item.Frequency=="YEARLY" && date.Month>now.Month)) continue;
                    string key=item.Frequency=="YEARLY"?now.Year.ToString("0000"):now.ToString("yyyy-MM");
                    using(var already=c.CreateCommand()){already.Transaction=tx;already.CommandText="SELECT 1 FROM recurring_expense_runs WHERE recurring_id=$r AND period_key=$p LIMIT 1";already.Parameters.AddWithValue("$r",item.Id);already.Parameters.AddWithValue("$p",key);if(already.ExecuteScalar()!=null)continue;}
                    using var insert=c.CreateCommand();insert.Transaction=tx;insert.CommandText="INSERT OR IGNORE INTO expenses(description,amount,created_at) VALUES($n,$a,$d)";insert.Parameters.AddWithValue("$n",item.Name+" — "+key);insert.Parameters.AddWithValue("$a",item.Amount);insert.Parameters.AddWithValue("$d",now.ToString("yyyy-MM-dd"));if(insert.ExecuteNonQuery()==0)continue;
                    long id=LastId(c,tx);PostEntry(c,tx,"EXPENSE",id,"Recurring expense: "+item.Name+" / "+key,new (string Account,double Debit,double Credit)[]{("Operating Expense",item.Amount,0d),("Accounts Payable",0d,item.Amount)});
                    using var run=c.CreateCommand();run.Transaction=tx;run.CommandText="INSERT INTO recurring_expense_runs(recurring_id,period_key,expense_id) VALUES($r,$p,$e)";run.Parameters.AddWithValue("$r",item.Id);run.Parameters.AddWithValue("$p",key);run.Parameters.AddWithValue("$e",id);run.ExecuteNonQuery();created++;
                }
                tx.Commit(); MessageBox.Show(UiLanguage.T("عدد الاستحقاقات المنشأة: ")+created); Refresh();
            }
            catch(Exception ex){try{tx.Rollback();}catch{} MessageBox.Show(UiLanguage.T("تعذر إنشاء الاستحقاقات؛ أُلغيت العملية كاملة.")+"\n"+ex.Message);}
        };
        Refresh(); f.ShowDialog(this);
    }

    void FixedAssets()
    {
        using var f = new Form { Text = UiLanguage.T("الأصول الثابتة والإهلاك"), Width = 900, Height = 700, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var name = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("اسم الأصل"), Height = 38 };
        var cost = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("تكلفة الاقتناء"), Height = 38 };
        var life = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("العمر الإنتاجي بالأشهر"), Height = 38 };
        var date = new TextBox { Dock = DockStyle.Top, PlaceholderText = "YYYY-MM-DD — تاريخ الجاهزية للاستخدام", Height = 38 };
        var list = new ListBox { Dock = DockStyle.Fill };
        var add = new Button { Dock = DockStyle.Top, Height = 42, Text = UiLanguage.T("تسجيل الأصل وترحيل الاقتناء") };
        var dep = new Button { Dock = DockStyle.Top, Height = 42, Text = UiLanguage.T("احتساب وترحيل إهلاك الشهر الحالي") };
        f.Controls.Add(list); f.Controls.Add(dep); f.Controls.Add(add); f.Controls.Add(date); f.Controls.Add(life); f.Controls.Add(cost); f.Controls.Add(name);
        void Refresh()
        {
            list.Items.Clear(); using var c=C(); using var x=c.CreateCommand(); x.CommandText="SELECT a.id,a.name,a.cost,a.life_months,COALESCE(SUM(d.amount),0) FROM fixed_assets a LEFT JOIN asset_depreciation d ON d.asset_id=a.id GROUP BY a.id ORDER BY a.id DESC";
            using var r=x.ExecuteReader(); while(r.Read()) list.Items.Add($"#{r.GetInt64(0)} | {r.GetString(1)} | cost {r.GetDouble(2):N2} | accumulated depreciation {r.GetDouble(4):N2} | net {r.GetDouble(2)-r.GetDouble(4):N2}");
        }
        add.Click += (_, _) =>
        {
            if(string.IsNullOrWhiteSpace(name.Text)||!double.TryParse(cost.Text,System.Globalization.NumberStyles.Number,System.Globalization.CultureInfo.CurrentCulture,out var value)||!double.IsFinite(value)||value<=0||!int.TryParse(life.Text,out var months)||months<1||months>1200||!DateTime.TryParseExact(date.Text.Trim(),"yyyy-MM-dd",System.Globalization.CultureInfo.InvariantCulture,System.Globalization.DateTimeStyles.None,out _))
            {MessageBox.Show(UiLanguage.T("تحقق من الاسم والتكلفة والعمر والتاريخ."));return;}
            using var c=C();using var tx=c.BeginTransaction();try{using var x=c.CreateCommand();x.Transaction=tx;x.CommandText="INSERT INTO fixed_assets(name,cost,life_months,ready_date) VALUES($n,$c,$m,$d)";x.Parameters.AddWithValue("$n",name.Text.Trim());x.Parameters.AddWithValue("$c",value);x.Parameters.AddWithValue("$m",months);x.Parameters.AddWithValue("$d",date.Text.Trim());x.ExecuteNonQuery();long id=LastId(c,tx);PostEntry(c,tx,"FIXED_ASSET",id,"Asset acquisition: "+name.Text.Trim(),new (string Account,double Debit,double Credit)[]{("Fixed Assets",value,0d),("Cash",0d,value)});tx.Commit();Refresh();}catch(Exception ex){try{tx.Rollback();}catch{}MessageBox.Show(UiLanguage.T("تعذر تسجيل الأصل؛ لم تُعتمد العملية.")+"\n"+ex.Message);}
        };
        dep.Click += (_, _) =>
        {
            int count=0;using var c=C();using var tx=c.BeginTransaction();try{using var q=c.CreateCommand();q.Transaction=tx;q.CommandText="SELECT id,name,cost,life_months,ready_date FROM fixed_assets ORDER BY id";var rows=new List<(long Id,string Name,double Cost,int Life,string Ready)>();using(var r=q.ExecuteReader())while(r.Read())rows.Add((r.GetInt64(0),r.GetString(1),r.GetDouble(2),r.GetInt32(3),r.GetString(4)));var period=DateTime.Today.ToString("yyyy-MM");foreach(var a in rows){if(!DateTime.TryParseExact(a.Ready,"yyyy-MM-dd",System.Globalization.CultureInfo.InvariantCulture,System.Globalization.DateTimeStyles.None,out var ready)||ready>DateTime.Today)continue;using var accumQ=c.CreateCommand();accumQ.Transaction=tx;accumQ.CommandText="SELECT COALESCE(SUM(amount),0) FROM asset_depreciation WHERE asset_id=$a";accumQ.Parameters.AddWithValue("$a",a.Id);double accumulated=Convert.ToDouble(accumQ.ExecuteScalar());double monthly=Math.Min(a.Cost-accumulated,a.Cost/a.Life);if(monthly<=0.005)continue;using var x=c.CreateCommand();x.Transaction=tx;x.CommandText="INSERT OR IGNORE INTO asset_depreciation(asset_id,period,amount) VALUES($a,$p,$v)";x.Parameters.AddWithValue("$a",a.Id);x.Parameters.AddWithValue("$p",period);x.Parameters.AddWithValue("$v",monthly);if(x.ExecuteNonQuery()==0)continue;long id=LastId(c,tx);PostEntry(c,tx,"DEPRECIATION",id,"Monthly depreciation: "+a.Name+" / "+period,new (string Account,double Debit,double Credit)[]{("Depreciation Expense",monthly,0d),("Accumulated Depreciation",0d,monthly)});using var up=c.CreateCommand();up.Transaction=tx;up.CommandText="UPDATE asset_depreciation SET journal_entry_id=(SELECT id FROM gl_entries WHERE source_type='DEPRECIATION' AND source_id=$id) WHERE id=$id";up.Parameters.AddWithValue("$id",id);up.ExecuteNonQuery();count++;}tx.Commit();MessageBox.Show(UiLanguage.T("عدد قيود الإهلاك المنشأة: ")+count);Refresh();}catch(Exception ex){try{tx.Rollback();}catch{}MessageBox.Show(UiLanguage.T("تعذر ترحيل الإهلاك؛ أُلغيت العملية كاملة.")+"\n"+ex.Message);}
        };
        Refresh(); f.ShowDialog(this);
    }

    void FactoryOperations()
    {
        using var f = new Form { Text = UiLanguage.T("حساب التشغيل والتصنيع المرحلي"), Width = 920, Height = 700, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var order = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("رقم/اسم أمر التشغيل"), Height = 38 };
        var stage = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("اسم المرحلة"), Height = 38 };
        var material = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("تكلفة المواد المصروفة"), Height = 38 };
        var labor = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("العمالة المباشرة"), Height = 38 };
        var overhead = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("المصاريف الصناعية"), Height = 38 };
        var good = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("كمية المنتج الجيد"), Height = 38 };
        var scrap = new TextBox { Dock = DockStyle.Top, PlaceholderText = UiLanguage.T("كمية التالف"), Height = 38 };
        foreach(var t in new[]{material,labor,overhead,good,scrap}) t.Text="0";
        var list = new ListBox { Dock = DockStyle.Fill };
        var save = new Button { Dock = DockStyle.Top, Height = 42, Text = UiLanguage.T("حفظ مرحلة التشغيل") };
        f.Controls.Add(list);f.Controls.Add(save);f.Controls.Add(scrap);f.Controls.Add(good);f.Controls.Add(overhead);f.Controls.Add(labor);f.Controls.Add(material);f.Controls.Add(stage);f.Controls.Add(order);
        void Refresh(){list.Items.Clear();using var c=C();using var x=c.CreateCommand();x.CommandText="SELECT o.order_name,e.stage_name,e.material_cost,e.labor_cost,e.overhead_cost,e.good_qty,e.scrap_qty FROM factory_stage_events e JOIN factory_orders o ON o.id=e.order_id ORDER BY e.id DESC";using var r=x.ExecuteReader();while(r.Read())list.Items.Add($"{r.GetString(0)} | {r.GetString(1)} | cost {(r.GetDouble(2)+r.GetDouble(3)+r.GetDouble(4)):N2} | good {r.GetDouble(5):N2} | scrap {r.GetDouble(6):N2}");}
        save.Click+=(_,_)=>{if(string.IsNullOrWhiteSpace(order.Text)||string.IsNullOrWhiteSpace(stage.Text)||!double.TryParse(material.Text,out var m)||!double.TryParse(labor.Text,out var l)||!double.TryParse(overhead.Text,out var h)||!double.TryParse(good.Text,out var g)||!double.TryParse(scrap.Text,out var s)||new[]{m,l,h,g,s}.Any(v=>!double.IsFinite(v)||v<0)||g+s<=0){MessageBox.Show(UiLanguage.T("أدخل أمرًا ومرحلة وتكاليف وكميات صحيحة."));return;}using var c=C();using var tx=c.BeginTransaction();try{using var x=c.CreateCommand();x.Transaction=tx;x.CommandText="INSERT OR IGNORE INTO factory_orders(order_name) VALUES($n)";x.Parameters.AddWithValue("$n",order.Text.Trim());x.ExecuteNonQuery();using var q=c.CreateCommand();q.Transaction=tx;q.CommandText="SELECT id FROM factory_orders WHERE order_name=$n";q.Parameters.AddWithValue("$n",order.Text.Trim());long id=Convert.ToInt64(q.ExecuteScalar());using var ev=c.CreateCommand();ev.Transaction=tx;ev.CommandText="INSERT INTO factory_stage_events(order_id,stage_name,material_cost,labor_cost,overhead_cost,good_qty,scrap_qty) VALUES($o,$s,$m,$l,$h,$g,$d)";ev.Parameters.AddWithValue("$o",id);ev.Parameters.AddWithValue("$s",stage.Text.Trim());ev.Parameters.AddWithValue("$m",m);ev.Parameters.AddWithValue("$l",l);ev.Parameters.AddWithValue("$h",h);ev.Parameters.AddWithValue("$g",g);ev.Parameters.AddWithValue("$d",s);ev.ExecuteNonQuery();long eventId=LastId(c,tx);double total=m+l+h;if(total>0){var lines=new List<(string Account,double Debit,double Credit)>{("Work in Process",total,0d)};if(m>0)lines.Add(("Raw Materials",0d,m));if(l>0)lines.Add(("Salaries Payable",0d,l));if(h>0)lines.Add(("Manufacturing Overhead Payable",0d,h));PostEntry(c,tx,"FACTORY_STAGE",eventId,"Factory stage: "+order.Text.Trim()+" / "+stage.Text.Trim(),lines.ToArray());using var up=c.CreateCommand();up.Transaction=tx;up.CommandText="UPDATE factory_stage_events SET journal_entry_id=(SELECT id FROM gl_entries WHERE source_type='FACTORY_STAGE' AND source_id=$id) WHERE id=$id";up.Parameters.AddWithValue("$id",eventId);up.ExecuteNonQuery();}tx.Commit();Refresh();}catch(Exception ex){try{tx.Rollback();}catch{}MessageBox.Show(UiLanguage.T("تعذر حفظ المرحلة وترحيل تكلفتها؛ أُلغيت العملية.")+"\n"+ex.Message);}};
        Refresh();f.ShowDialog(this);
    }

    void CashExpense()
    {
        using var f = new Form { Text = UiLanguage.T("المصروفات"), Width = 650, Height = 480, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var description = new TextBox { PlaceholderText = UiLanguage.T("البيان"), Dock = DockStyle.Top, Height = 40 };
        var amount = new TextBox { PlaceholderText = UiLanguage.T("المبلغ"), Dock = DockStyle.Top, Height = 40 };
        var save = new Button { Text = UiLanguage.T("تسجيل مصروف"), Dock = DockStyle.Top, Height = 45 };
        var list = new ListBox { Dock = DockStyle.Fill };
        void Reload()
        {
            list.Items.Clear();
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = "SELECT description,amount,created_at FROM expenses ORDER BY id DESC LIMIT 100";
            using var r = q.ExecuteReader();
            while (r.Read()) list.Items.Add(r.GetString(0) + " | " + r.GetDouble(1).ToString("N2") + " | " + r.GetString(2));
        }
        save.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(description.Text) || !double.TryParse(amount.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var value) || value <= 0)
            { MessageBox.Show(UiLanguage.T("أدخل بيانًا ومبلغًا موجبًا صحيحًا.")); return; }
            using var c = C(); using var tx = c.BeginTransaction(); using var q = c.CreateCommand();
            q.Transaction = tx; q.CommandText = "INSERT INTO expenses(description,amount) VALUES($d,$a)";
            q.Parameters.AddWithValue("$d", description.Text.Trim()); q.Parameters.AddWithValue("$a", value); q.ExecuteNonQuery();
            PostEntry(c, tx, "EXPENSE", LastId(c, tx), "Cash expense: " + description.Text.Trim(), new (string Account, double Debit, double Credit)[] { ("Operating Expense", value, 0d), ("Cash", 0d, value) });
            tx.Commit();
            description.Clear(); amount.Clear(); Reload(); RefreshSummary();
        };
        f.Controls.Add(list); f.Controls.Add(save); f.Controls.Add(amount); f.Controls.Add(description);
        Reload(); f.ShowDialog();
    }

    void BackupRestore()
    {
        using var f = new Form { Text = UiLanguage.T("النسخ الاحتياطي والاستعادة"), Width = 620, Height = 280, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var export = new Button { Text = UiLanguage.T("تصدير نسخة احتياطية"), Dock = DockStyle.Top, Height = 55 };
        var restore = new Button { Text = UiLanguage.T("استعادة نسخة احتياطية"), Dock = DockStyle.Top, Height = 55 };
        export.Click += (_, _) =>
        {
            using var dialog = new SaveFileDialog { Filter = "SQLite database (*.db)|*.db", FileName = "SmartCompany-backup.db" };
            if (dialog.ShowDialog(f) != DialogResult.OK) return;
            try { using var source = C(); using var destination = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = dialog.FileName }.ToString()); destination.Open(); source.BackupDatabase(destination); MessageBox.Show(UiLanguage.T("تم تصدير النسخة الاحتياطية.")); }
            catch (Exception) { MessageBox.Show(UiLanguage.T("تعذر تصدير النسخة الاحتياطية.")); }
        };
        restore.Click += (_, _) =>
        {
            if (MessageBox.Show(UiLanguage.T("ستستبدل الاستعادة بيانات الشركة الحالية. أنشئ نسخة احتياطية أولًا. هل تريد المتابعة؟"), UiLanguage.T("تأكيد الاستعادة"), MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes) return;
            using var dialog = new OpenFileDialog { Filter = "SQLite database (*.db)|*.db|All files (*.*)|*.*" };
            if (dialog.ShowDialog(f) != DialogResult.OK) return;
            try
            {
                using (var source = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = dialog.FileName, Mode = SqliteOpenMode.ReadOnly }.ToString()))
                {
                    source.Open(); using var check = source.CreateCommand(); check.CommandText = "PRAGMA integrity_check";
                    if (!string.Equals(Convert.ToString(check.ExecuteScalar()), "ok", StringComparison.OrdinalIgnoreCase)) throw new InvalidDataException();
                    using var tables = source.CreateCommand(); tables.CommandText = "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('products','sales','purchases')";
                    if (Convert.ToInt32(tables.ExecuteScalar()) != 3) throw new InvalidDataException();
                }
                // Preserve a verified point-in-time copy before replacing live data. The backup stays beside the local database.
                var safetyBackupPath = dbPath + ".pre-restore-" + DateTime.Now.ToString("yyyyMMdd-HHmmss") + ".bak";
                using (var live = C())
                using (var safety = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = safetyBackupPath }.ToString())) { safety.Open(); live.BackupDatabase(safety); }
                using (var source = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = dialog.FileName, Mode = SqliteOpenMode.ReadOnly }.ToString()))
                using (var destination = C()) { source.Open(); source.BackupDatabase(destination); }
                InitDb(); RefreshSummary(); MessageBox.Show(UiLanguage.T("تمت الاستعادة. راجع الأرصدة والتقارير."));
            }
            catch (Exception) { MessageBox.Show(UiLanguage.T("فشلت الاستعادة: الملف غير صالح أو غير متوافق.")); }
        };
        f.Controls.Add(restore); f.Controls.Add(export); f.ShowDialog();
    }

    void ReceiptPayment(bool supplierPayment)
    {
        var table = supplierPayment ? "suppliers" : "customers";
        var title = supplierPayment ? "سداد الموردين" : "تحصيلات العملاء";
        using var f = new Form { Text = UiLanguage.T(title), Width = 680, Height = 500, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var party = new ComboBox { Dock = DockStyle.Top, Height = 40, DropDownStyle = ComboBoxStyle.DropDownList };
        var ids = new List<long>();
        var amount = new TextBox { PlaceholderText = UiLanguage.T("المبلغ"), Dock = DockStyle.Top, Height = 40 };
        var note = new TextBox { PlaceholderText = UiLanguage.T("البيان / مرجع الفاتورة"), Dock = DockStyle.Top, Height = 40 };
        var save = new Button { Text = UiLanguage.T(supplierPayment ? "حفظ سداد المورد" : "حفظ تحصيل العميل"), Dock = DockStyle.Top, Height = 45 };
        var list = new ListBox { Dock = DockStyle.Fill };
        using (var c = C()) using (var q = c.CreateCommand())
        {
            q.CommandText = $"SELECT id,name FROM {table} ORDER BY name";
            using var reader = q.ExecuteReader();
            while(reader.Read()) { ids.Add(reader.GetInt64(0)); party.Items.Add(reader.GetString(1)); }
        }
        void Reload()
        {
            list.Items.Clear();
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = supplierPayment
                ? @"SELECT s.name,
COALESCE((SELECT SUM(p.total) FROM purchases p WHERE p.supplier_id=s.id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(r.total) FROM purchase_returns r JOIN purchases p ON p.id=r.purchase_id WHERE p.supplier_id=s.id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(x.amount) FROM supplier_payments x WHERE x.supplier_id=s.id),0)
FROM suppliers s ORDER BY s.name"
                : @"SELECT s.name,
COALESCE((SELECT SUM(p.total) FROM sales p WHERE p.customer_id=s.id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(r.total) FROM sales_returns r JOIN sales p ON p.id=r.sale_id WHERE p.customer_id=s.id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(x.amount) FROM customer_receipts x WHERE x.customer_id=s.id),0)
FROM customers s ORDER BY s.name";
            using var reader=q.ExecuteReader();
            while(reader.Read()) list.Items.Add(reader.GetString(0)+" | "+UiLanguage.T("الرصيد")+": "+reader.GetDouble(1).ToString("N2"));
        }
        save.Click += (_, _) =>
        {
            if(party.SelectedIndex<0 || !double.TryParse(amount.Text,System.Globalization.NumberStyles.Number,System.Globalization.CultureInfo.CurrentCulture,out var value) || value<=0)
            { MessageBox.Show(UiLanguage.T("اختر العميل أو المورد وأدخل مبلغًا موجبًا صحيحًا.")); return; }
            try
            {
                using var c=C(); using var tx=c.BeginTransaction();
                using (var balance = c.CreateCommand())
                {
                    balance.Transaction = tx;
                    balance.CommandText = supplierPayment
                        ? @"SELECT COALESCE((SELECT SUM(total) FROM purchases WHERE supplier_id=$id AND payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(r.total) FROM purchase_returns r JOIN purchases p ON p.id=r.purchase_id WHERE p.supplier_id=$id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(amount) FROM supplier_payments WHERE supplier_id=$id),0)"
                        : @"SELECT COALESCE((SELECT SUM(total) FROM sales WHERE customer_id=$id AND payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(r.total) FROM sales_returns r JOIN sales p ON p.id=r.sale_id WHERE p.customer_id=$id AND p.payment_mode='CREDIT'),0)
-COALESCE((SELECT SUM(amount) FROM customer_receipts WHERE customer_id=$id),0)";
                    balance.Parameters.AddWithValue("$id",ids[party.SelectedIndex]);
                    var outstanding = Convert.ToDouble(balance.ExecuteScalar());
                    if(value > Math.Max(0d,outstanding) + 0.005d)
                    {
                        tx.Rollback();
                        MessageBox.Show(UiLanguage.T("المبلغ أكبر من الرصيد الآجل المتبقي. راجع الرصيد قبل الحفظ."));
                        return;
                    }
                }
                using var q=c.CreateCommand(); q.Transaction=tx;
                q.CommandText=supplierPayment ? "INSERT INTO supplier_payments(supplier_id,amount,note) VALUES($id,$a,$n)" : "INSERT INTO customer_receipts(customer_id,amount,note) VALUES($id,$a,$n)";
                q.Parameters.AddWithValue("$id",ids[party.SelectedIndex]); q.Parameters.AddWithValue("$a",value); q.Parameters.AddWithValue("$n",note.Text.Trim()); q.ExecuteNonQuery();
                var id=LastId(c,tx);
                if(supplierPayment) PostEntry(c,tx,"SUPPLIER_PAYMENT",id,"Supplier payment: "+party.SelectedItem,new (string Account,double Debit,double Credit)[]{("Accounts Payable",value,0d),("Cash",0d,value)});
                else PostEntry(c,tx,"CUSTOMER_RECEIPT",id,"Customer receipt: "+party.SelectedItem,new (string Account,double Debit,double Credit)[]{("Cash",value,0d),("Accounts Receivable",0d,value)});
                tx.Commit(); amount.Clear(); note.Clear(); Reload(); RefreshSummary();
                MessageBox.Show(UiLanguage.T("تم تسجيل الحركة وترحيلها محاسبيًا."));
            }
            catch(Exception) { MessageBox.Show(UiLanguage.T("تعذر تسجيل الحركة؛ لم تُعتمد العملية.")); }
        };
        f.Controls.Add(list); f.Controls.Add(save); f.Controls.Add(note); f.Controls.Add(amount); f.Controls.Add(party);
        Reload(); f.ShowDialog();
    }

    void Employees()
    {
        using var f = new Form { Text = UiLanguage.T("الموظفون والرواتب"), Width = 820, Height = 650, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var name = new TextBox { PlaceholderText = UiLanguage.T("اسم الموظف"), Dock = DockStyle.Top, Height = 38 };
        var job = new TextBox { PlaceholderText = UiLanguage.T("المسمى الوظيفي"), Dock = DockStyle.Top, Height = 38 };
        var salary = new TextBox { PlaceholderText = UiLanguage.T("الراتب الشهري"), Dock = DockStyle.Top, Height = 38 };
        var add = new Button { Text = UiLanguage.T("إضافة موظف"), Dock = DockStyle.Top, Height = 42 };
        var person = new ComboBox { Dock = DockStyle.Top, DropDownStyle = ComboBoxStyle.DropDownList, Height = 38 };
        var kind = new ComboBox { Dock = DockStyle.Top, DropDownStyle = ComboBoxStyle.DropDownList, Height = 38 };
        kind.Items.Add(UiLanguage.T("سلفة")); kind.Items.Add(UiLanguage.T("خصم")); kind.SelectedIndex = 0;
        var amount = new TextBox { PlaceholderText = UiLanguage.T("المبلغ"), Dock = DockStyle.Top, Height = 38 };
        var note = new TextBox { PlaceholderText = UiLanguage.T("البيان"), Dock = DockStyle.Top, Height = 38 };
        var saveEvent = new Button { Text = UiLanguage.T("حفظ حركة الراتب"), Dock = DockStyle.Top, Height = 42 };
        var accrue = new Button { Text = UiLanguage.T("ترحيل استحقاق رواتب الشهر إلى الأستاذ"), Dock = DockStyle.Top, Height = 42 };
        var settle = new Button { Text = UiLanguage.T("تسوية وصرف راتب الموظف المحدد"), Dock = DockStyle.Top, Height = 42 };
        var list = new ListBox { Dock = DockStyle.Fill };
        var month = DateTime.Now.ToString("yyyy-MM");
        var ids = new List<long>();
        void Reload()
        {
            person.Items.Clear(); ids.Clear(); list.Items.Clear();
            using var c = C();
            using var q = c.CreateCommand();
            q.CommandText = "SELECT e.id,e.name,COALESCE(e.job_title,''),COALESCE(e.salary,0),COALESCE(SUM(CASE WHEN p.kind='ADVANCE' AND p.period_month=$m THEN p.amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN p.kind='DEDUCTION' AND p.period_month=$m THEN p.amount ELSE 0 END),0) FROM employees e LEFT JOIN payroll_events p ON p.employee_id=e.id GROUP BY e.id ORDER BY e.name";
            q.Parameters.AddWithValue("$m", month);
            using var r = q.ExecuteReader();
            while (r.Read())
            {
                ids.Add(r.GetInt64(0));
                person.Items.Add(r.GetString(1) + " — " + r.GetString(2));
                var baseSalary = r.GetDouble(3); var advances = r.GetDouble(4); var deductions = r.GetDouble(5);
                list.Items.Add(r.GetString(1) + " | " + UiLanguage.T("الراتب الشهري") + ": " + baseSalary.ToString("N2") + " | " + UiLanguage.T("السلف") + ": " + advances.ToString("N2") + " | " + UiLanguage.T("الخصومات") + ": " + deductions.ToString("N2") + " | " + UiLanguage.T("صافي المستحق") + ": " + Math.Max(0, baseSalary - advances - deductions).ToString("N2"));
            }
            if (person.Items.Count > 0 && person.SelectedIndex < 0) person.SelectedIndex = 0;
        }
        add.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(name.Text) || !double.TryParse(salary.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var value) || value < 0)
            { MessageBox.Show(UiLanguage.T("أدخل اسمًا وراتبًا صحيحًا.")); return; }
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = "INSERT INTO employees(name,job_title,salary) VALUES($n,$j,$s)";
            q.Parameters.AddWithValue("$n", name.Text.Trim()); q.Parameters.AddWithValue("$j", job.Text.Trim()); q.Parameters.AddWithValue("$s", value);
            q.ExecuteNonQuery(); name.Clear(); job.Clear(); salary.Clear(); Reload();
        };
        saveEvent.Click += (_, _) =>
        {
            if (person.SelectedIndex < 0 || !double.TryParse(amount.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var value) || value <= 0)
            { MessageBox.Show(UiLanguage.T("اختر موظفًا وأدخل مبلغًا صحيحًا.")); return; }
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = "INSERT INTO payroll_events(employee_id,kind,amount,note,period_month) VALUES($id,$k,$a,$n,$m)";
            q.Parameters.AddWithValue("$id", ids[person.SelectedIndex]); q.Parameters.AddWithValue("$k", kind.SelectedIndex == 0 ? "ADVANCE" : "DEDUCTION");
            q.Parameters.AddWithValue("$a", value); q.Parameters.AddWithValue("$n", note.Text.Trim()); q.Parameters.AddWithValue("$m", month);
            using var tx = c.BeginTransaction(); q.Transaction = tx; q.ExecuteNonQuery();
            var eventId = LastId(c, tx);
            if (kind.SelectedIndex == 0) PostEntry(c, tx, "PAYROLL_ADVANCE", eventId, "Employee advance: " + (person.SelectedItem?.ToString() ?? ""), new (string Account, double Debit, double Credit)[] { ("Employee Advances", value, 0d), ("Cash", 0d, value) });
            // Generic deductions remain recorded but unposted until their nature/accounting counterpart is explicitly classified.
            tx.Commit(); amount.Clear(); note.Clear(); Reload(); RefreshSummary();
        };
        accrue.Click += (_, _) =>
        {
            using var c = C(); using var tx = c.BeginTransaction();
            try
            {
                using var employees = c.CreateCommand(); employees.Transaction = tx; employees.CommandText = "SELECT id,name,salary FROM employees ORDER BY id";
                using var reader = employees.ExecuteReader(); var rows = new List<(long Id,string Name,double Salary)>();
                while(reader.Read()) rows.Add((reader.GetInt64(0),reader.GetString(1),reader.GetDouble(2))); reader.Close();
                int posted = 0;
                foreach(var employee in rows)
                {
                    if(employee.Salary <= 0) continue;
                    using var run = c.CreateCommand(); run.Transaction = tx; run.CommandText = "INSERT OR IGNORE INTO payroll_runs(employee_id,period_month,base_salary) VALUES($e,$m,$s)"; run.Parameters.AddWithValue("$e",employee.Id); run.Parameters.AddWithValue("$m",month); run.Parameters.AddWithValue("$s",employee.Salary);
                    if(run.ExecuteNonQuery()==0) continue;
                    var runId = LastId(c, tx);
                    PostEntry(c, tx, "PAYROLL_ACCRUAL", runId, "Monthly salary accrual: " + employee.Name + " / " + month, new (string Account, double Debit, double Credit)[] { ("Salaries Expense", employee.Salary, 0d), ("Salaries Payable", 0d, employee.Salary) }); posted++;
                }
                tx.Commit(); MessageBox.Show(UiLanguage.T("تم ترحيل استحقاق الرواتب الجديدة لهذا الشهر: ") + posted); Reload(); RefreshSummary();
            }
            catch(Exception) { tx.Rollback(); MessageBox.Show(UiLanguage.T("تعذر ترحيل الرواتب؛ أُلغي الترحيل بالكامل.")); }
        };
        settle.Click += (_, _) =>
        {
            if (person.SelectedIndex < 0)
            { MessageBox.Show(UiLanguage.T("اختر الموظف أولًا.")); return; }
            var employeeId = ids[person.SelectedIndex];
            using var c = C();
            using var tx = c.BeginTransaction();
            try
            {
                long runId; double baseSalary; string employeeName;
                using (var q = c.CreateCommand())
                {
                    q.Transaction = tx;
                    q.CommandText = "SELECT r.id,r.base_salary,e.name FROM payroll_runs r JOIN employees e ON e.id=r.employee_id WHERE r.employee_id=$e AND r.period_month=$m";
                    q.Parameters.AddWithValue("$e", employeeId); q.Parameters.AddWithValue("$m", month);
                    using var reader = q.ExecuteReader();
                    if (!reader.Read()) throw new InvalidOperationException("يجب ترحيل استحقاق الراتب لهذا الشهر أولًا.");
                    runId = reader.GetInt64(0); baseSalary = reader.GetDouble(1); employeeName = reader.GetString(2);
                }
                using (var check = c.CreateCommand())
                {
                    check.Transaction = tx;
                    check.CommandText = "SELECT COUNT(*) FROM payroll_settlements WHERE employee_id=$e AND period_month=$m";
                    check.Parameters.AddWithValue("$e", employeeId); check.Parameters.AddWithValue("$m", month);
                    if (Convert.ToInt32(check.ExecuteScalar()) > 0) throw new InvalidOperationException("تمت تسوية راتب هذا الموظف عن الشهر المحدد مسبقًا.");
                }
                double advances, deductions;
                using (var q = c.CreateCommand())
                {
                    q.Transaction = tx;
                    q.CommandText = "SELECT COALESCE(SUM(CASE WHEN kind='ADVANCE' THEN amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN kind='DEDUCTION' THEN amount ELSE 0 END),0) FROM payroll_events WHERE employee_id=$e AND period_month=$m";
                    q.Parameters.AddWithValue("$e", employeeId); q.Parameters.AddWithValue("$m", month);
                    using var reader = q.ExecuteReader(); reader.Read(); advances = reader.GetDouble(0); deductions = reader.GetDouble(1);
                }
                if (deductions > 0.005)
                    throw new InvalidOperationException("توجد خصومات غير مصنفة محاسبيًا. لا يمكن صرف الراتب حتى تحديد حساب الخصم وطريقة ترحيله.");
                if (advances > baseSalary + 0.005)
                    throw new InvalidOperationException("السلف تتجاوز الراتب المستحق. راجع السجل قبل التسوية.");
                var cashPaid = Math.Max(0d, baseSalary - advances);
                using (var insert = c.CreateCommand())
                {
                    insert.Transaction = tx;
                    insert.CommandText = "INSERT INTO payroll_settlements(employee_id,period_month,base_salary,advance_offset,cash_paid,note) VALUES($e,$m,$s,$a,$c,$n)";
                    insert.Parameters.AddWithValue("$e", employeeId); insert.Parameters.AddWithValue("$m", month);
                    insert.Parameters.AddWithValue("$s", baseSalary); insert.Parameters.AddWithValue("$a", advances);
                    insert.Parameters.AddWithValue("$c", cashPaid); insert.Parameters.AddWithValue("$n", "Payroll settlement for " + month);
                    insert.ExecuteNonQuery();
                }
                var settlementId = LastId(c, tx);
                var lines = new List<(string Account, double Debit, double Credit)>
                {
                    ("Salaries Payable", baseSalary, 0d)
                };
                if (advances > 0.005) lines.Add(("Employee Advances", 0d, advances));
                if (cashPaid > 0.005) lines.Add(("Cash", 0d, cashPaid));
                PostEntry(c, tx, "PAYROLL_SETTLEMENT", settlementId, "Salary settlement: " + employeeName + " / " + month, lines.ToArray());
                tx.Commit();
                MessageBox.Show(UiLanguage.T("تمت تسوية الراتب وترحيل الصرف. راجع دفتر الأستاذ وميزان المراجعة."));
                Reload(); RefreshSummary();
            }
            catch (Exception ex)
            {
                tx.Rollback();
                MessageBox.Show(UiLanguage.T("تعذرت تسوية الراتب؛ لم تُعتمد أي تغييرات. ") + ex.Message);
            }
        };
        f.Controls.Add(list); f.Controls.Add(settle); f.Controls.Add(accrue); f.Controls.Add(saveEvent); f.Controls.Add(note); f.Controls.Add(amount); f.Controls.Add(kind); f.Controls.Add(person); f.Controls.Add(add); f.Controls.Add(salary); f.Controls.Add(job); f.Controls.Add(name);
        Reload(); f.ShowDialog();
    }

    void Simple(string table,string title)
    {
        using var f=new Form{Text=UiLanguage.T(title),Width=650,Height=500,RightToLeft=UiLanguage.English?RightToLeft.No:RightToLeft.Yes,RightToLeftLayout=!UiLanguage.English};var n=new TextBox{PlaceholderText=UiLanguage.T("الاسم"),Dock=DockStyle.Top,Height=40};var add=new Button{Text=UiLanguage.T("إضافة"),Dock=DockStyle.Top,Height=45};var list=new ListBox{Dock=DockStyle.Fill};
        add.Click+=(_,_)=>{using var c=C();using var x=c.CreateCommand();x.CommandText=$"INSERT INTO {table}(name) VALUES($n)";x.Parameters.AddWithValue("$n",n.Text);x.ExecuteNonQuery();list.Items.Add(n.Text);n.Clear();};f.Controls.Add(list);f.Controls.Add(add);f.Controls.Add(n);f.ShowDialog();
    }
    void LedgerReports()
    {
        using var c = C();
        var rows = new List<string>();
        double totalDebit = 0, totalCredit = 0;
        using (var q = c.CreateCommand())
        {
            q.CommandText = @"SELECT a.code,a.name,a.kind,
COALESCE(SUM(l.debit),0),COALESCE(SUM(l.credit),0)
FROM gl_accounts a
LEFT JOIN gl_lines l ON l.account_id=a.id
GROUP BY a.id,a.code,a.name,a.kind
ORDER BY a.code";
            using var r = q.ExecuteReader();
            while (r.Read())
            {
                var debit = r.GetDouble(3);
                var credit = r.GetDouble(4);
                totalDebit += debit;
                totalCredit += credit;
                rows.Add($"{r.GetString(0)} | {r.GetString(1)} ({r.GetString(2)}) | مدين: {debit:N2} | دائن: {credit:N2}");
            }
        }
        int missingSales, missingPurchases, missingExpenses, missingOpeningStock;
        int missingReceipts, missingSupplierPayments, missingPayrollEvents, missingPayrollAccruals;
        int missingSalesReturns, missingPurchaseReturns, missingPayrollSettlements, unpostedDeductions;
        int unbalancedEntries, orphanLines, invalidLines;
        using (var q = c.CreateCommand())
        {
            q.CommandText = @"SELECT
(SELECT COUNT(*) FROM sales s LEFT JOIN gl_entries g ON g.source_type='SALE' AND g.source_id=s.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM purchases p LEFT JOIN gl_entries g ON g.source_type='PURCHASE' AND g.source_id=p.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM expenses e LEFT JOIN gl_entries g ON g.source_type='EXPENSE' AND g.source_id=e.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM products p LEFT JOIN gl_entries g ON g.source_type='OPENING_STOCK' AND g.source_id=p.id WHERE p.qty>0 AND g.id IS NULL),
(SELECT COUNT(*) FROM customer_receipts x LEFT JOIN gl_entries g ON g.source_type='CUSTOMER_RECEIPT' AND g.source_id=x.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM supplier_payments x LEFT JOIN gl_entries g ON g.source_type='SUPPLIER_PAYMENT' AND g.source_id=x.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM payroll_events x LEFT JOIN gl_entries g ON g.source_type='PAYROLL_ADVANCE' AND g.source_id=x.id WHERE x.kind='ADVANCE' AND g.id IS NULL),
(SELECT COUNT(*) FROM payroll_runs x LEFT JOIN gl_entries g ON g.source_type='PAYROLL_ACCRUAL' AND g.source_id=x.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM (SELECT e.id FROM gl_entries e LEFT JOIN gl_lines l ON l.entry_id=e.id GROUP BY e.id HAVING ABS(COALESCE(SUM(l.debit),0)-COALESCE(SUM(l.credit),0))>0.005)),
(SELECT COUNT(*) FROM gl_lines l LEFT JOIN gl_entries e ON e.id=l.entry_id WHERE e.id IS NULL),
(SELECT COUNT(*) FROM gl_lines WHERE debit<0 OR credit<0 OR (debit>0 AND credit>0))";
            using var r = q.ExecuteReader();
            r.Read();
            missingSales = r.GetInt32(0); missingPurchases = r.GetInt32(1); missingExpenses = r.GetInt32(2); missingOpeningStock = r.GetInt32(3);
            missingReceipts = r.GetInt32(4); missingSupplierPayments = r.GetInt32(5); missingPayrollEvents = r.GetInt32(6); missingPayrollAccruals = r.GetInt32(7);
            unbalancedEntries = r.GetInt32(8); orphanLines = r.GetInt32(9); invalidLines = r.GetInt32(10);
        }
        using (var q = c.CreateCommand())
        {
            q.CommandText = @"SELECT
(SELECT COUNT(*) FROM sales_returns r LEFT JOIN gl_entries g ON g.source_type='SALE_RETURN' AND g.source_id=r.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM purchase_returns r LEFT JOIN gl_entries g ON g.source_type='PURCHASE_RETURN' AND g.source_id=r.id WHERE g.id IS NULL)";
            using var r = q.ExecuteReader();
            r.Read();
            missingSalesReturns = r.GetInt32(0);
            missingPurchaseReturns = r.GetInt32(1);
        }
        using (var q = c.CreateCommand())
        {
            q.CommandText = @"SELECT
(SELECT COUNT(*) FROM payroll_settlements p LEFT JOIN gl_entries g ON g.source_type='PAYROLL_SETTLEMENT' AND g.source_id=p.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM payroll_events WHERE kind='DEDUCTION')";
            using var r = q.ExecuteReader();
            r.Read();
            missingPayrollSettlements = r.GetInt32(0);
            unpostedDeductions = r.GetInt32(1);
        }
        var balanced = Math.Abs(totalDebit - totalCredit) <= 0.005;
        double inventoryBookValue, inventoryLedgerValue;
        using (var q = c.CreateCommand())
        {
            q.CommandText = "SELECT COALESCE(SUM(qty * COALESCE(unit_cost,0)),0) FROM products";
            inventoryBookValue = Convert.ToDouble(q.ExecuteScalar());
        }
        using (var q = c.CreateCommand())
        {
            q.CommandText = @"SELECT COALESCE(SUM(l.debit-l.credit),0)
FROM gl_lines l JOIN gl_accounts a ON a.id=l.account_id WHERE a.name='Inventory'";
            inventoryLedgerValue = Convert.ToDouble(q.ExecuteScalar());
        }
        var inventoryDifference = inventoryBookValue - inventoryLedgerValue;
        var report = "دفتر الأستاذ العام — ميزان المراجعة التشخيصي" + Environment.NewLine
            + $"إجمالي المدين: {totalDebit:N2} | إجمالي الدائن: {totalCredit:N2}" + Environment.NewLine
            + $"حالة التوازن الإجمالي: {(balanced ? "متوازن إجمالًا فقط" : "غير متوازن — يتطلب التحقيق")}" + Environment.NewLine
            + $"مطابقة قيمة المخزون: قيمة بطاقة المخزون {inventoryBookValue:N2} | رصيد حساب المخزون {inventoryLedgerValue:N2} | الفرق {inventoryDifference:N2}" + Environment.NewLine
            + $"قيود غير متوازنة: {unbalancedEntries} | أسطر يتيمة: {orphanLines} | أسطر غير صالحة: {invalidLines}" + Environment.NewLine + Environment.NewLine
            + $"عمليات بلا قيد: مبيعات {missingSales} | مشتريات {missingPurchases} | مصروفات {missingExpenses} | مخزون افتتاحي {missingOpeningStock}" + Environment.NewLine
            + $"تحصيلات عملاء بلا قيد: {missingReceipts} | سداد موردين بلا قيد: {missingSupplierPayments}" + Environment.NewLine
            + $"مرتجعات مبيعات بلا قيد: {missingSalesReturns} | مرتجعات مشتريات بلا قيد: {missingPurchaseReturns}" + Environment.NewLine
            + $"سلف موظفين بلا قيد: {missingPayrollEvents} | استحقاقات رواتب بلا قيد: {missingPayrollAccruals}" + Environment.NewLine
            + $"تسويات رواتب بلا قيد: {missingPayrollSettlements} | خصومات رواتب غير مصنفة وغير مرحلة: {unpostedDeductions}" + Environment.NewLine + Environment.NewLine
            + "تنبيه: الأعداد قد تشمل سجلات تاريخية سبقت دفتر الأستاذ. الخصومات العامة والمرتجعات والتسويات والإقفال ليست مكتملة؛ لا تعتمد التقرير كقوائم مالية معتمدة." + Environment.NewLine + Environment.NewLine
            + string.Join(Environment.NewLine, rows);
        using var f = new Form { Text = "دفتر الأستاذ وميزان المراجعة", Width = 900, Height = 650, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var preview = new TextBox { Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Both, WordWrap = false, Dock = DockStyle.Fill, Font = new Font("Consolas", 11), Text = report };
        f.Controls.Add(preview);
        f.ShowDialog();
    }

    void ManualJournalEntry()
    {
        using var f = new Form { Text = UiLanguage.T("قيد يومية يدوي من سطرين"), Width = 720, Height = 470, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var memo = new TextBox { Dock = DockStyle.Top, Height = 42, PlaceholderText = UiLanguage.T("بيان القيد — مطلوب") };
        var debitAccount = new ComboBox { Dock = DockStyle.Top, Height = 38, DropDownStyle = ComboBoxStyle.DropDownList };
        var creditAccount = new ComboBox { Dock = DockStyle.Top, Height = 38, DropDownStyle = ComboBoxStyle.DropDownList };
        var amount = new TextBox { Dock = DockStyle.Top, Height = 40, PlaceholderText = UiLanguage.T("المبلغ — قيمة موجبة") };
        var save = new Button { Dock = DockStyle.Top, Height = 48, Text = UiLanguage.T("حفظ القيد المرحّل") };
        var note = new Label { Dock = DockStyle.Top, Height = 64, TextAlign = ContentAlignment.MiddleCenter, Text = UiLanguage.T("هذا النموذج ينشئ قيدًا متوازنًا من سطرين. استخدم وحدات المبيعات والمشتريات والمرتجعات لتصحيح العمليات المرتبطة بالمخزون والذمم.") };
        using (var c = C())
        using (var q = c.CreateCommand())
        {
            q.CommandText = "SELECT name FROM gl_accounts ORDER BY code";
            using var r = q.ExecuteReader();
            while (r.Read()) { debitAccount.Items.Add(r.GetString(0)); creditAccount.Items.Add(r.GetString(0)); }
        }
        if (debitAccount.Items.Count > 0) debitAccount.SelectedIndex = 0;
        if (creditAccount.Items.Count > 1) creditAccount.SelectedIndex = 1;
        save.Click += (_, _) =>
        {
            if (string.IsNullOrWhiteSpace(memo.Text) || debitAccount.SelectedItem is not string debitName || creditAccount.SelectedItem is not string creditName ||
                !double.TryParse(amount.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var value) ||
                !double.IsFinite(value) || value <= 0 || debitName == creditName)
            {
                MessageBox.Show(UiLanguage.T("أدخل البيان والمبلغ الموجب واختر حسابين مختلفين."));
                return;
            }
            using var c = C();
            using var tx = c.BeginTransaction();
            try
            {
                using (var insert = c.CreateCommand())
                {
                    insert.Transaction = tx;
                    insert.CommandText = "INSERT INTO manual_journals(memo) VALUES($m)";
                    insert.Parameters.AddWithValue("$m", memo.Text.Trim());
                    insert.ExecuteNonQuery();
                }
                var journalId = LastId(c, tx);
                PostEntry(c, tx, "MANUAL_JOURNAL", journalId, memo.Text.Trim(),
                    (debitName, value, 0d), (creditName, 0d, value));
                tx.Commit();
                MessageBox.Show(UiLanguage.T("تم حفظ القيد اليدوي وترحيله. يمكنك عكسه لاحقًا من شاشة القيد العكسي."));
                f.DialogResult = DialogResult.OK;
                f.Close();
                RefreshSummary();
            }
            catch (Exception ex)
            {
                try { tx.Rollback(); } catch { }
                MessageBox.Show(UiLanguage.T("تعذر حفظ القيد؛ لم تُعتمد التغييرات. ") + ex.Message);
            }
        };
        f.Controls.Add(save); f.Controls.Add(amount); f.Controls.Add(creditAccount); f.Controls.Add(debitAccount); f.Controls.Add(memo); f.Controls.Add(note);
        f.ShowDialog(this);
    }

    void ReverseJournalEntry()
    {
        using var f = new Form { Text = UiLanguage.T("القيد العكسي — عكس قيد مرحّل"), Width = 900, Height = 600, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var list = new ListBox { Dock = DockStyle.Fill, Font = new Font("Consolas", 10) };
        var ids = new List<long>();
        var reason = new TextBox { Dock = DockStyle.Top, Height = 38, PlaceholderText = UiLanguage.T("سبب العكس — مطلوب للتدقيق") };
        var reload = new Button { Text = UiLanguage.T("تحديث القيود المتاحة"), Dock = DockStyle.Top, Height = 42 };
        var reverse = new Button { Text = UiLanguage.T("إنشاء القيد العكسي"), Dock = DockStyle.Bottom, Height = 48 };

        void LoadEntries()
        {
            ids.Clear();
            list.Items.Clear();
            using var c = C();
            using var q = c.CreateCommand();
            q.CommandText = @"SELECT e.id,e.created_at,e.source_type,e.source_id,e.memo,
COALESCE(SUM(l.debit),0),COALESCE(SUM(l.credit),0),COUNT(l.id)
FROM gl_entries e
LEFT JOIN gl_lines l ON l.entry_id=e.id
LEFT JOIN gl_reversals rv ON rv.original_entry_id=e.id
WHERE e.source_type = 'MANUAL_JOURNAL' AND rv.id IS NULL
GROUP BY e.id,e.created_at,e.source_type,e.source_id,e.memo
HAVING COUNT(l.id)>=2 AND ABS(COALESCE(SUM(l.debit),0)-COALESCE(SUM(l.credit),0))<=0.005
ORDER BY e.id DESC";
            using var r = q.ExecuteReader();
            while (r.Read())
            {
                ids.Add(r.GetInt64(0));
                list.Items.Add($"#{r.GetInt64(0)} | {r.GetString(1)} | {r.GetString(2)}:{r.GetInt64(3)} | مدين {r.GetDouble(5):N2} | دائن {r.GetDouble(6):N2} | {r.GetString(4)}");
            }
        }

        reverse.Click += (_, _) =>
        {
            if (list.SelectedIndex < 0 || string.IsNullOrWhiteSpace(reason.Text))
            {
                MessageBox.Show(UiLanguage.T("اختر قيدًا واكتب سبب العكس أولًا."));
                return;
            }
            var originalId = ids[list.SelectedIndex];
            if (MessageBox.Show(UiLanguage.T("سيُنشأ قيد عكسي مستقل للقيد رقم ") + originalId + UiLanguage.T(". لن يُحذف القيد الأصلي. هل تريد المتابعة؟"),
                UiLanguage.T("تأكيد القيد العكسي"), MessageBoxButtons.YesNo, MessageBoxIcon.Warning) != DialogResult.Yes) return;

            using var c = C();
            using var tx = c.BeginTransaction();
            try
            {
                var reversalId = JournalAccounting.ReverseManualJournal(c, tx, originalId, reason.Text.Trim());
                tx.Commit();
                MessageBox.Show(UiLanguage.T("تم إنشاء قيد عكسي مستقل. بقي القيد الأصلي محفوظًا لأغراض التدقيق.") + " #" + reversalId);
                reason.Clear();
                LoadEntries();
                RefreshSummary();
            }
            catch (Exception ex)
            {
                try { tx.Rollback(); } catch { }
                MessageBox.Show(UiLanguage.T("تعذر إنشاء القيد العكسي؛ لم تُعتمد التغييرات. ") + ex.Message);
            }
        };
        reload.Click += (_, _) => LoadEntries();
        f.Controls.Add(list); f.Controls.Add(reverse); f.Controls.Add(reason); f.Controls.Add(reload);
        LoadEntries();
        f.ShowDialog(this);
    }

    void Reports()
    {
        using var c = C();
        using var s = c.CreateCommand(); s.CommandText = "SELECT COALESCE(SUM(total),0) FROM sales";
        var sales = Convert.ToDouble(s.ExecuteScalar());
        using var p = c.CreateCommand(); p.CommandText = "SELECT COALESCE(SUM(total),0) FROM purchases";
        var purchases = Convert.ToDouble(p.ExecuteScalar());
        using var ex = c.CreateCommand(); ex.CommandText = "SELECT COALESCE(SUM(amount),0) FROM expenses";
        var expenses = Convert.ToDouble(ex.ExecuteScalar());
        var report = UiLanguage.T("ملخص تشغيلي أولي — ليس قائمة دخل معتمدة") + Environment.NewLine + UiLanguage.T("تنبيه: الفرق الحسابي أدناه لا يمثل صافي الربح؛ لا توجد بعد قيود أستاذ عام متكاملة.") + Environment.NewLine + Environment.NewLine +
            UiLanguage.T("المبيعات:") + " " + sales.ToString("N2") + Environment.NewLine +
            UiLanguage.T("المشتريات:") + " " + purchases.ToString("N2") + Environment.NewLine +
            UiLanguage.T("إجمالي المصروفات:") + " " + expenses.ToString("N2") + Environment.NewLine +
            UiLanguage.T("صافي الحركة بعد المصروفات:") + " " + (sales - purchases - expenses).ToString("N2") + Environment.NewLine + Environment.NewLine +
            UiLanguage.T("توقيع المحاسب:") + " ____________________";
        using var f = new Form { Text = UiLanguage.T("التقرير المالي"), Width = 760, Height = 520, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var preview = new TextBox { Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Vertical, Dock = DockStyle.Fill, Font = new Font("Segoe UI", 12), Text = report };
        var actions = new FlowLayoutPanel { Dock = DockStyle.Bottom, Height = 55, FlowDirection = UiLanguage.English ? FlowDirection.LeftToRight : FlowDirection.RightToLeft };
        var print = new Button { Text = UiLanguage.T("طباعة التقرير"), Width = 180, Height = 40 };
        var email = new Button { Text = UiLanguage.T("إرسال بالبريد الإلكتروني"), Width = 220, Height = 40 };
        print.Click += (_, _) =>
        {
            using var document = new PrintDocument();
            document.DocumentName = UiLanguage.T("التقرير المالي");
            document.PrintPage += (_, e) =>
            {
                using var font = new Font("Segoe UI", 11);
                e.Graphics.DrawString(report, font, Brushes.Black, new RectangleF(e.MarginBounds.Left, e.MarginBounds.Top, e.MarginBounds.Width, e.MarginBounds.Height));
                e.HasMorePages = false;
            };
            using var dialog = new PrintDialog { Document = document, UseEXDialog = true };
            if (dialog.ShowDialog(f) == DialogResult.OK) document.Print();
        };
        email.Click += (_, _) =>
        {
            var url = "mailto:?subject=" + Uri.EscapeDataString(UiLanguage.T("التقرير المالي")) + "&body=" + Uri.EscapeDataString(report);
            Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
        };
        actions.Controls.Add(print); actions.Controls.Add(email);
        f.Controls.Add(preview); f.Controls.Add(actions); f.ShowDialog();
    }
    long LastId(SqliteConnection c, SqliteTransaction tx) => JournalAccounting.LastId(c, tx);

    void PostEntry(SqliteConnection c, SqliteTransaction tx, string sourceType, long sourceId, string memo,
        params (string Account, double Debit, double Credit)[] lines)
        => JournalAccounting.PostEntry(c, tx, sourceType, sourceId, memo, lines);

    void RefreshSummary()
    {
        using var c=C();using var s=c.CreateCommand();s.CommandText="SELECT COALESCE(SUM(total),0) FROM sales";var sales=Convert.ToDouble(s.ExecuteScalar());using var p=c.CreateCommand();p.CommandText="SELECT COALESCE(SUM(total),0) FROM purchases";var purchases=Convert.ToDouble(p.ExecuteScalar());using var ex=c.CreateCommand();ex.CommandText="SELECT COALESCE(SUM(amount),0) FROM expenses";var expenses=Convert.ToDouble(ex.ExecuteScalar());summary.Text=UiLanguage.T($"المبيعات: {sales:N2}   |   المشتريات: {purchases:N2}   |   المصروفات: {expenses:N2}   |   صافي الحركة: {(sales-purchases-expenses):N2}");
    }
}
