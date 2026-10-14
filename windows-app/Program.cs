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
          new[]{"شركة محمد مصطفى الذكية","Mohammed Mustafa Smart Company"},new[]{"ملخص تشغيلي أولي — ليس قائمة دخل معتمدة","Preliminary operational summary — not an approved income statement"},new[]{"تنبيه: الفرق الحسابي ليس صافي الربح. دفتر الأستاذ يسجل حاليًا عمليات نقدية جديدة محددة فقط؛ الأجل والمرتجعات والرواتب والتاريخ السابق لم تُرحّل بعد.","Notice: the arithmetic difference is not net profit. The ledger currently posts only selected new cash transactions; credit transactions, returns, payroll, and legacy history are not yet posted."},
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
        Text = UiLanguage.T("شركة محمد مصطفى الذكية 2.2.3"); Width = 1050; Height = 720; StartPosition = FormStartPosition.CenterScreen;
        RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes; RightToLeftLayout = !UiLanguage.English;
        var head = new Label { Text = UiLanguage.T("شركة محمد مصطفى الذكية 2.2.3 — نسخة تجريبية"), Dock = DockStyle.Top, Height = 75, BackColor = Color.FromArgb(11,31,58), ForeColor = Color.FromArgb(212,175,55), Font = new Font("Segoe UI", 22, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        Controls.Add(menu); Controls.Add(summary); Controls.Add(head);
        Add("الأصناف والمخزون والباركود", Products); Add("المبيعات", () => Transaction(false)); Add("المشتريات", () => Transaction(true));
        Add("العملاء", () => Simple("customers","العملاء")); Add("الموردون", () => Simple("suppliers","الموردون")); Add("تحصيلات العملاء", () => ReceiptPayment(false)); Add("سداد الموردين", () => ReceiptPayment(true)); Add("الموظفون والرواتب", Employees); Add("المصروفات", CashExpense); Add("النسخ الاحتياطي والاستعادة", BackupRestore);
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
INSERT OR IGNORE INTO gl_accounts(code,name,kind) VALUES ('1000','Cash','ASSET'),('1100','Accounts Receivable','ASSET'),('1200','Inventory','ASSET'),('2000','Accounts Payable','LIABILITY'),('2100','Salaries Payable','LIABILITY'),('3000','Opening Equity','EQUITY'),('4000','Sales Revenue','REVENUE'),('5000','Cost of Goods Sold','EXPENSE'),('6100','Operating Expense','EXPENSE'),('6200','Salaries Expense','EXPENSE'),('1300','Employee Advances','ASSET'),('6300','Payroll Deductions','EXPENSE');";
            ledger.ExecuteNonQuery();
        }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE employees ADD COLUMN job_title TEXT NOT NULL DEFAULT ''"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE employees ADD COLUMN salary REAL NOT NULL DEFAULT 0"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "CREATE TABLE IF NOT EXISTS payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
// Additive registers preserve historical rows and support explicit settlement documents.
using (var payroll = c.CreateCommand()) { payroll.CommandText = "CREATE TABLE IF NOT EXISTS payroll_runs(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,period_month TEXT NOT NULL,base_salary REAL NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(employee_id,period_month))"; payroll.ExecuteNonQuery(); }
        using (var settlements = c.CreateCommand()) { settlements.CommandText = "CREATE TABLE IF NOT EXISTS customer_receipts(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER NOT NULL,amount REAL NOT NULL CHECK(amount>0),note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP); CREATE TABLE IF NOT EXISTS supplier_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,supplier_id INTEGER NOT NULL,amount REAL NOT NULL CHECK(amount>0),note TEXT,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);"; settlements.ExecuteNonQuery(); }
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
                    : "INSERT INTO sales(product,qty,price,total,customer_id,payment_mode) VALUES($n,$q,$p,$t,$party,$mode)";
                insert.Parameters.AddWithValue("$n", productName);
                insert.Parameters.AddWithValue("$q", qty);
                insert.Parameters.AddWithValue("$p", price);
                insert.Parameters.AddWithValue("$t", qty * price);
                insert.Parameters.AddWithValue("$party", partyId.HasValue ? partyId.Value : DBNull.Value);
                insert.Parameters.AddWithValue("$mode", isCredit ? "CREDIT" : "CASH");
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
                ? @"SELECT s.name,COALESCE((SELECT SUM(p.total) FROM purchases p WHERE p.supplier_id=s.id AND p.payment_mode='CREDIT'),0)-COALESCE((SELECT SUM(x.amount) FROM supplier_payments x WHERE x.supplier_id=s.id),0) FROM suppliers s ORDER BY s.name"
                : @"SELECT s.name,COALESCE((SELECT SUM(p.total) FROM sales p WHERE p.customer_id=s.id AND p.payment_mode='CREDIT'),0)-COALESCE((SELECT SUM(x.amount) FROM customer_receipts x WHERE x.customer_id=s.id),0) FROM customers s ORDER BY s.name";
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
                        ? "SELECT COALESCE((SELECT SUM(total) FROM purchases WHERE supplier_id=$id AND payment_mode='CREDIT'),0)-COALESCE((SELECT SUM(amount) FROM supplier_payments WHERE supplier_id=$id),0)"
                        : "SELECT COALESCE((SELECT SUM(total) FROM sales WHERE customer_id=$id AND payment_mode='CREDIT'),0)-COALESCE((SELECT SUM(amount) FROM customer_receipts WHERE customer_id=$id),0)";
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
        f.Controls.Add(list); f.Controls.Add(accrue); f.Controls.Add(saveEvent); f.Controls.Add(note); f.Controls.Add(amount); f.Controls.Add(kind); f.Controls.Add(person); f.Controls.Add(add); f.Controls.Add(salary); f.Controls.Add(job); f.Controls.Add(name);
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
        var balanced = Math.Abs(totalDebit - totalCredit) <= 0.005;
        var report = "دفتر الأستاذ العام — ميزان المراجعة التشخيصي" + Environment.NewLine
            + $"إجمالي المدين: {totalDebit:N2} | إجمالي الدائن: {totalCredit:N2}" + Environment.NewLine
            + $"حالة التوازن الإجمالي: {(balanced ? "متوازن إجمالًا فقط" : "غير متوازن — يتطلب التحقيق")}" + Environment.NewLine
            + $"قيود غير متوازنة: {unbalancedEntries} | أسطر يتيمة: {orphanLines} | أسطر غير صالحة: {invalidLines}" + Environment.NewLine + Environment.NewLine
            + $"عمليات بلا قيد: مبيعات {missingSales} | مشتريات {missingPurchases} | مصروفات {missingExpenses} | مخزون افتتاحي {missingOpeningStock}" + Environment.NewLine
            + $"تحصيلات عملاء بلا قيد: {missingReceipts} | سداد موردين بلا قيد: {missingSupplierPayments}" + Environment.NewLine
            + $"سلف موظفين بلا قيد: {missingPayrollEvents} | استحقاقات رواتب بلا قيد: {missingPayrollAccruals}" + Environment.NewLine + Environment.NewLine
            + "تنبيه: الأعداد قد تشمل سجلات تاريخية سبقت دفتر الأستاذ. الخصومات العامة والمرتجعات والتسويات والإقفال ليست مكتملة؛ لا تعتمد التقرير كقوائم مالية معتمدة." + Environment.NewLine + Environment.NewLine
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
