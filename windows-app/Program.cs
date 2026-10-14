using Microsoft.Data.Sqlite;
using System.Diagnostics;
using System.Drawing;
using System.Drawing.Printing;

namespace SmartCompany;

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
          new[]{"شركة محمد مصطفى الذكية","Mohammed Mustafa Smart Company"},new[]{"الباقات والأسعار","Plans & Pricing"},new[]{"ملخص تشغيلي أولي — ليس قائمة دخل معتمدة","Preliminary operational summary — not an approved income statement"},new[]{"تنبيه: الفرق الحسابي ليس صافي الربح. دفتر الأستاذ يرحّل العمليات الجديدة المدعومة، لكن السجلات التاريخية والخصومات العامة وتسوية الرواتب والتسويات والإقفال تحتاج إلى مراجعة قبل اعتماد القوائم.","Notice: arithmetic movement is not net profit. The ledger posts supported new transactions, but legacy history, generic deductions, payroll settlement, adjustments, and closing entries require review before financial statements can be relied on."},
          new[]{"الموظفون والرواتب","Employees & Payroll"},new[]{"المصروفات","Expenses"},new[]{"النسخ الاحتياطي والاستعادة","Backup & Restore"},new[]{"تسجيل مصروف","Record expense"},new[]{"تصدير نسخة احتياطية","Export backup"},new[]{"استعادة نسخة احتياطية","Restore backup"},new[]{"تم تصدير النسخة الاحتياطية.","Backup exported."},new[]{"تعذر تصدير النسخة الاحتياطية.","Could not export backup."},new[]{"ستستبدل الاستعادة بيانات الشركة الحالية. أنشئ نسخة احتياطية أولًا. هل تريد المتابعة؟","Restore will replace current company data. Back up first. Continue?"},new[]{"تأكيد الاستعادة","Confirm restore"},new[]{"تمت الاستعادة. راجع الأرصدة والتقارير.","Restore complete. Review balances and reports."},new[]{"فشلت الاستعادة: الملف غير صالح أو غير متوافق.","Restore failed: file is invalid or incompatible."},new[]{"أدخل بيانًا ومبلغًا موجبًا صحيحًا.","Enter a description and a positive amount."},new[]{"إجمالي المصروفات:","Total expenses:"},new[]{"صافي الحركة بعد المصروفات:","Net movement after expenses:"},new[]{"اسم الموظف","Employee name"},new[]{"المسمى الوظيفي","Job title"},new[]{"الراتب الشهري","Monthly salary"},new[]{"إضافة موظف","Add employee"},new[]{"حفظ حركة الراتب","Save payroll event"},new[]{"سلفة","Advance"},new[]{"خصم","Deduction"},new[]{"المبلغ","Amount"},new[]{"البيان","Description"},new[]{"السلف","Advances"},new[]{"الخصومات","Deductions"},new[]{"صافي المستحق","Net payable"},new[]{"أدخل اسمًا وراتبًا صحيحًا.","Enter a valid name and salary."},new[]{"اختر موظفًا وأدخل مبلغًا صحيحًا.","Choose an employee and enter a valid amount."},new[]{"الأصناف والمخزون والباركود","Products, Inventory & Barcode"},new[]{"الأصناف والمخزون","Products & Inventory"},
          new[]{"المبيعات","Sales"},new[]{"المشتريات","Purchases"},new[]{"العملاء","Customers"},new[]{"الموردون","Suppliers"},new[]{"الموظفون","Employees"},new[]{"التقارير المالية","Financial Reports"},
          new[]{"الدعم عبر واتساب","WhatsApp Support"},new[]{"الدعم عبر البريد الإلكتروني","Email Support"},new[]{"اختيار اللغة / Language","Language: العربية / English"},new[]{"اسم الصنف أو الباركود","Product name or barcode"},new[]{"طباعة التقرير","Print report"},new[]{"إرسال بالبريد الإلكتروني","Send by email"},new[]{"التقرير المالي","Financial Report"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"اسم الصنف","Product name"},
          new[]{"الباركود (ماسح USB/Bluetooth أو إدخال يدوي)","Barcode (USB/Bluetooth scanner or manual entry)"},new[]{"الكمية","Quantity"},new[]{"السعر","Price"},new[]{"إضافة الصنف","Add product"},
          new[]{"أدخل اسمًا وكمية وسعرًا صحيحًا.","Enter a valid name, quantity and price."},new[]{"تعذر الحفظ: الاسم أو الباركود مستخدم مسبقًا.","Could not save: name or barcode already exists."},
          new[]{"باركود","Barcode"},new[]{"المخزون","Stock"},new[]{"فاتورة بيع","Sales Invoice"},new[]{"حفظ","Save"},new[]{"الصنف غير موجود.","Product not found."},
          new[]{"الرصيد غير كافٍ.","Insufficient stock."},new[]{"تم الحفظ وتحديث المخزون.","Saved and inventory updated."},new[]{"الاسم","Name"},new[]{"إضافة","Add"},
          new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"الصافي:","Net:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},
          new[]{"انتهت التجربة المجانية لمدة 7 أيام","The 7-day free trial has ended"},new[]{"رمز التفعيل","Activation code"},new[]{"تفعيل الاشتراك","Activate subscription"},new[]{"التواصل عبر واتساب","Contact via WhatsApp"},
          new[]{"تم تفعيل الاشتراك.","Subscription activated."},new[]{"رمز التفعيل غير صحيح.","Invalid activation code."},new[]{"التقرير المالي","Financial Report"},new[]{"شركة محمد مصطفى الذكية - الاشتراك","Mohammed Mustafa Smart Company - Subscription"},new[]{"تم تفعيل الاشتراك.","Subscription activated."},new[]{"رمز التفعيل غير صحيح.","Invalid activation code."},new[]{"باركود","Barcode"},new[]{"المخزون","Stock"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"صافي الحركة:","Net movement:"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"الصافي:","Net:"},new[]{"اختيار اللغة / Language","Language: Arabic / English"}
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

sealed class MainForm : Form
{
    readonly string dbPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "SmartCompany", "smartcompany.db");
    readonly FlowLayoutPanel menu = new() { Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false, AutoScroll = true, Padding = new Padding(30) };
    readonly Label summary = new() { Dock = DockStyle.Top, Height = 55, TextAlign = ContentAlignment.MiddleCenter, Font = new Font("Segoe UI", 13) };

    public MainForm()
    {
        Directory.CreateDirectory(Path.GetDirectoryName(dbPath)!); InitDb();
        Text = UiLanguage.T("شركة محمد مصطفى الذكية 2.2.4"); Width = 1050; Height = 720; StartPosition = FormStartPosition.CenterScreen;
        RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes; RightToLeftLayout = !UiLanguage.English;
        var head = new Label { Text = UiLanguage.T("شركة محمد مصطفى الذكية 2.2.4 — نسخة تجريبية"), Dock = DockStyle.Top, Height = 75, BackColor = Color.FromArgb(11,31,58), ForeColor = Color.FromArgb(212,175,55), Font = new Font("Segoe UI", 22, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        Controls.Add(menu); Controls.Add(summary); Controls.Add(head);
        Add("الأصناف والمخزون والباركود", Products); Add("المبيعات", () => Transaction(false)); Add("المشتريات", () => Transaction(true));
        Add("العملاء", () => Simple("customers","العملاء")); Add("الموردون", () => Simple("suppliers","الموردون")); Add("تحصيلات العملاء", () => ReceiptPayment(false)); Add("سداد الموردين", () => ReceiptPayment(true)); Add("الموظفون والرواتب", Employees); Add("المصروفات", CashExpense); Add("المرتجعات", Returns); Add("النسخ الاحتياطي والاستعادة", BackupRestore);
        Add("دفتر الأستاذ وميزان المراجعة", LedgerReports); Add("التقارير المالية", Reports); Add("الدعم عبر واتساب", () => Process.Start(new ProcessStartInfo("https://wa.me/249121851285") { UseShellExecute = true })); Add("الدعم عبر البريد الإلكتروني", () => Process.Start(new ProcessStartInfo("mailto:moh1442010@gmail.com?subject=دعم%20تطبيق%20شركة%20محمد%20مصطفى%20الذكية") { UseShellExecute = true }));
        Add("اختيار اللغة / Language", ToggleLanguage);
        RefreshSummary();
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
        x.CommandText = "CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,qty REAL NOT NULL DEFAULT 0,price REAL NOT NULL DEFAULT 0); CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL); CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL); CREATE TABLE IF NOT EXISTS employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,job_title TEXT NOT NULL DEFAULT '',salary REAL NOT NULL DEFAULT 0); CREATE TABLE IF NOT EXISTS payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,description TEXT NOT NULL,amount REAL NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);";
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
INSERT OR IGNORE INTO gl_accounts(code,name,kind) VALUES ('1000','Cash','ASSET'),('1100','Accounts Receivable','ASSET'),('1200','Inventory','ASSET'),('2000','Accounts Payable','LIABILITY'),('2100','Salaries Payable','LIABILITY'),('3000','Opening Equity','EQUITY'),('4000','Sales Revenue','REVENUE'),('5000','Cost of Goods Sold','EXPENSE'),('6100','Operating Expense','EXPENSE'),('6200','Salaries Expense','EXPENSE'),('1300','Employee Advances','ASSET'),('6300','Payroll Deductions','EXPENSE'),('2200','Payroll Deductions Payable','LIABILITY'),('4100','Sales Returns','CONTRA_REVENUE'),('4200','Purchase Return Gain','OTHER_INCOME'),('6400','Purchase Return Loss','EXPENSE');";
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
            using var c = C(); using var tx = c.BeginTransaction();
            try
            {
                using (var settled = c.CreateCommand())
                {
                    settled.Transaction = tx;
                    settled.CommandText = "SELECT COUNT(*) FROM payroll_settlements WHERE employee_id=$e AND period_month=$m";
                    settled.Parameters.AddWithValue("$e", ids[person.SelectedIndex]); settled.Parameters.AddWithValue("$m", month);
                    if (Convert.ToInt32(settled.ExecuteScalar()) > 0)
                        throw new InvalidOperationException("تمت تسوية راتب هذا الشهر؛ لا يمكن إضافة حركة بأثر رجعي.");
                }
                if (kind.SelectedIndex == 1)
                {
                    using var accrued = c.CreateCommand(); accrued.Transaction = tx;
                    accrued.CommandText = "SELECT COUNT(*) FROM payroll_runs WHERE employee_id=$e AND period_month=$m";
                    accrued.Parameters.AddWithValue("$e", ids[person.SelectedIndex]); accrued.Parameters.AddWithValue("$m", month);
                    if (Convert.ToInt32(accrued.ExecuteScalar()) == 0)
                        throw new InvalidOperationException("يجب ترحيل استحقاق الراتب قبل تسجيل الخصم.");
                    using var totals = c.CreateCommand(); totals.Transaction = tx;
                    totals.CommandText = "SELECT r.base_salary,COALESCE((SELECT SUM(amount) FROM payroll_events WHERE employee_id=$e AND period_month=$m AND kind='DEDUCTION'),0),COALESCE((SELECT SUM(amount) FROM payroll_events WHERE employee_id=$e AND period_month=$m AND kind='ADVANCE'),0) FROM payroll_runs r WHERE r.employee_id=$e AND r.period_month=$m";
                    totals.Parameters.AddWithValue("$e", ids[person.SelectedIndex]); totals.Parameters.AddWithValue("$m", month);
                    double salary, priorDeductions, priorAdvances;
                    using (var rr = totals.ExecuteReader())
                    {
                        if (!rr.Read()) throw new InvalidOperationException("لم يُعثر على استحقاق الراتب.");
                        salary = rr.GetDouble(0); priorDeductions = rr.GetDouble(1); priorAdvances = rr.GetDouble(2);
                    }
                    if (value + priorDeductions + priorAdvances > salary + 0.005)
                        throw new InvalidOperationException("لا يمكن تسجيل خصم يتجاوز الراتب المتبقي بعد الخصومات السابقة.");
                }
                using var q = c.CreateCommand(); q.Transaction = tx;
                q.CommandText = "INSERT INTO payroll_events(employee_id,kind,amount,note,period_month) VALUES($id,$k,$a,$n,$m)";
                q.Parameters.AddWithValue("$id", ids[person.SelectedIndex]); q.Parameters.AddWithValue("$k", kind.SelectedIndex == 0 ? "ADVANCE" : "DEDUCTION");
                q.Parameters.AddWithValue("$a", value); q.Parameters.AddWithValue("$n", note.Text.Trim()); q.Parameters.AddWithValue("$m", month);
                q.ExecuteNonQuery();
                var eventId = LastId(c, tx);
                if (kind.SelectedIndex == 0)
                    PostEntry(c, tx, "PAYROLL_ADVANCE", eventId, "Employee advance: " + (person.SelectedItem?.ToString() ?? ""), new (string Account, double Debit, double Credit)[] { ("Employee Advances", value, 0d), ("Cash", 0d, value) });
                else
                    PostEntry(c, tx, "PAYROLL_DEDUCTION", eventId, "Payroll deduction withheld: " + (person.SelectedItem?.ToString() ?? ""), new (string Account, double Debit, double Credit)[] { ("Salaries Payable", value, 0d), ("Payroll Deductions Payable", 0d, value) });
                tx.Commit(); amount.Clear(); note.Clear(); Reload(); RefreshSummary();
            }
            catch (Exception ex)
            {
                tx.Rollback();
                MessageBox.Show(UiLanguage.T("تعذر حفظ حركة الراتب؛ لم تُعتمد أي تغييرات. ") + ex.Message);
            }
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
                if (advances + deductions > baseSalary + 0.005)
                    throw new InvalidOperationException("مجموع السلف والخصومات يتجاوز الراتب المستحق.");
                var payableAfterDeduction = Math.Max(0d, baseSalary - deductions);
                var cashPaid = Math.Max(0d, baseSalary - advances - deductions);
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
                    ("Salaries Payable", payableAfterDeduction, 0d)
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
        int missingReceipts, missingSupplierPayments, missingPayrollEvents, missingPayrollDeductions, missingPayrollAccruals;
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
(SELECT COUNT(*) FROM payroll_events x LEFT JOIN gl_entries g ON g.source_type='PAYROLL_DEDUCTION' AND g.source_id=x.id WHERE x.kind='DEDUCTION' AND g.id IS NULL),
(SELECT COUNT(*) FROM payroll_runs x LEFT JOIN gl_entries g ON g.source_type='PAYROLL_ACCRUAL' AND g.source_id=x.id WHERE g.id IS NULL),
(SELECT COUNT(*) FROM (SELECT e.id FROM gl_entries e LEFT JOIN gl_lines l ON l.entry_id=e.id GROUP BY e.id HAVING ABS(COALESCE(SUM(l.debit),0)-COALESCE(SUM(l.credit),0))>0.005)),
(SELECT COUNT(*) FROM gl_lines l LEFT JOIN gl_entries e ON e.id=l.entry_id WHERE e.id IS NULL),
(SELECT COUNT(*) FROM gl_lines WHERE debit<0 OR credit<0 OR (debit>0 AND credit>0))";
            using var r = q.ExecuteReader();
            r.Read();
            missingSales = r.GetInt32(0); missingPurchases = r.GetInt32(1); missingExpenses = r.GetInt32(2); missingOpeningStock = r.GetInt32(3);
            missingReceipts = r.GetInt32(4); missingSupplierPayments = r.GetInt32(5); missingPayrollEvents = r.GetInt32(6); missingPayrollDeductions = r.GetInt32(7); missingPayrollAccruals = r.GetInt32(8);
            unbalancedEntries = r.GetInt32(9); orphanLines = r.GetInt32(10); invalidLines = r.GetInt32(11);
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
(SELECT COUNT(*) FROM payroll_events x LEFT JOIN gl_entries g ON g.source_type='PAYROLL_DEDUCTION' AND g.source_id=x.id WHERE x.kind='DEDUCTION' AND g.id IS NULL)";
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
            + $"تسويات رواتب بلا قيد: {missingPayrollSettlements} | خصومات رواتب بلا قيد: {unpostedDeductions}" + Environment.NewLine + Environment.NewLine
            + "تنبيه: قد تشمل الأعداد سجلات تاريخية سبقت دفتر الأستاذ. لا تُرحّل السجلات القديمة تلقائيًا؛ راجعها يدويًا. لا تعتمد التقرير كقوائم مالية معتمدة قبل اكتمال اختبارات مستقلة." + Environment.NewLine + Environment.NewLine
            + string.Join(Environment.NewLine, rows);
        using var f = new Form { Text = "دفتر الأستاذ وميزان المراجعة", Width = 900, Height = 650, StartPosition = FormStartPosition.CenterParent, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var preview = new TextBox { Multiline = true, ReadOnly = true, ScrollBars = ScrollBars.Both, WordWrap = false, Dock = DockStyle.Fill, Font = new Font("Consolas", 11), Text = report };
        f.Controls.Add(preview);
        f.ShowDialog();
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
    long LastId(SqliteConnection c, SqliteTransaction tx)
    {
        using var q = c.CreateCommand(); q.Transaction = tx; q.CommandText = "SELECT last_insert_rowid()";
        return Convert.ToInt64(q.ExecuteScalar());
    }

    void PostEntry(SqliteConnection c, SqliteTransaction tx, string sourceType, long sourceId, string memo, params (string Account, double Debit, double Credit)[] lines)
    {
        var debit = lines.Sum(x => x.Debit);
        var credit = lines.Sum(x => x.Credit);
        if (lines.Length < 2 || Math.Abs(debit - credit) > 0.005 || lines.Any(x => x.Debit < 0 || x.Credit < 0 || (x.Debit > 0 && x.Credit > 0)))
            throw new InvalidOperationException("Unbalanced or invalid journal entry.");
        using var header = c.CreateCommand(); header.Transaction = tx;
        header.CommandText = "INSERT INTO gl_entries(source_type,source_id,memo) VALUES($s,$id,$m)";
        header.Parameters.AddWithValue("$s", sourceType); header.Parameters.AddWithValue("$id", sourceId); header.Parameters.AddWithValue("$m", memo);
        header.ExecuteNonQuery(); var entryId = LastId(c, tx);
        foreach (var line in lines)
        {
            using var account = c.CreateCommand(); account.Transaction = tx;
            account.CommandText = "SELECT id FROM gl_accounts WHERE name=$n"; account.Parameters.AddWithValue("$n", line.Account);
            var accountId = Convert.ToInt64(account.ExecuteScalar() ?? throw new InvalidOperationException("Ledger account missing."));
            using var detail = c.CreateCommand(); detail.Transaction = tx;
            detail.CommandText = "INSERT INTO gl_lines(entry_id,account_id,debit,credit) VALUES($e,$a,$d,$c)";
            detail.Parameters.AddWithValue("$e", entryId); detail.Parameters.AddWithValue("$a", accountId);
            detail.Parameters.AddWithValue("$d", line.Debit); detail.Parameters.AddWithValue("$c", line.Credit); detail.ExecuteNonQuery();
        }
    }

    void RefreshSummary()
    {
        using var c=C();using var s=c.CreateCommand();s.CommandText="SELECT COALESCE(SUM(total),0) FROM sales";var sales=Convert.ToDouble(s.ExecuteScalar());using var p=c.CreateCommand();p.CommandText="SELECT COALESCE(SUM(total),0) FROM purchases";var purchases=Convert.ToDouble(p.ExecuteScalar());using var ex=c.CreateCommand();ex.CommandText="SELECT COALESCE(SUM(amount),0) FROM expenses";var expenses=Convert.ToDouble(ex.ExecuteScalar());summary.Text=UiLanguage.T($"المبيعات: {sales:N2}   |   المشتريات: {purchases:N2}   |   المصروفات: {expenses:N2}   |   صافي الحركة: {(sales-purchases-expenses):N2}");
    }
}
