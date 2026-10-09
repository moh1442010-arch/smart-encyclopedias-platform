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
          new[]{"شركة محمد مصطفى الذكية","Mohammed Mustafa Smart Company"},new[]{"ملخص تشغيلي أولي — ليس قائمة دخل معتمدة","Preliminary operational summary — not an approved income statement"},new[]{"تنبيه: الفرق الحسابي أدناه لا يمثل صافي الربح؛ لا توجد بعد قيود أستاذ عام متكاملة.","Notice: the arithmetic difference below is not net profit; integrated general-ledger postings are not implemented."},
          new[]{"الموظفون والرواتب","Employees & Payroll"},new[]{"المصروفات","Expenses"},new[]{"النسخ الاحتياطي والاستعادة","Backup & Restore"},new[]{"تسجيل مصروف","Record expense"},new[]{"تصدير نسخة احتياطية","Export backup"},new[]{"استعادة نسخة احتياطية","Restore backup"},new[]{"تم تصدير النسخة الاحتياطية.","Backup exported."},new[]{"تعذر تصدير النسخة الاحتياطية.","Could not export backup."},new[]{"ستستبدل الاستعادة بيانات الشركة الحالية. أنشئ نسخة احتياطية أولًا. هل تريد المتابعة؟","Restore will replace current company data. Back up first. Continue?"},new[]{"تأكيد الاستعادة","Confirm restore"},new[]{"تمت الاستعادة. راجع الأرصدة والتقارير.","Restore complete. Review balances and reports."},new[]{"فشلت الاستعادة: الملف غير صالح أو غير متوافق.","Restore failed: file is invalid or incompatible."},new[]{"أدخل بيانًا ومبلغًا موجبًا صحيحًا.","Enter a description and a positive amount."},new[]{"إجمالي المصروفات:","Total expenses:"},new[]{"صافي الحركة بعد المصروفات:","Net movement after expenses:"},new[]{"اسم الموظف","Employee name"},new[]{"المسمى الوظيفي","Job title"},new[]{"الراتب الشهري","Monthly salary"},new[]{"إضافة موظف","Add employee"},new[]{"حفظ حركة الراتب","Save payroll event"},new[]{"سلفة","Advance"},new[]{"خصم","Deduction"},new[]{"المبلغ","Amount"},new[]{"البيان","Description"},new[]{"السلف","Advances"},new[]{"الخصومات","Deductions"},new[]{"صافي المستحق","Net payable"},new[]{"أدخل اسمًا وراتبًا صحيحًا.","Enter a valid name and salary."},new[]{"اختر موظفًا وأدخل مبلغًا صحيحًا.","Choose an employee and enter a valid amount."},new[]{"الأصناف والمخزون والباركود","Products, Inventory & Barcode"},new[]{"الأصناف والمخزون","Products & Inventory"},
          new[]{"المبيعات","Sales"},new[]{"المشتريات","Purchases"},new[]{"العملاء","Customers"},new[]{"الموردون","Suppliers"},new[]{"الموظفون","Employees"},new[]{"التقارير المالية","Financial Reports"},
          new[]{"الدعم عبر واتساب","WhatsApp Support"},new[]{"اختيار اللغة / Language","Language: العربية / English"},new[]{"اسم الصنف أو الباركود","Product name or barcode"},new[]{"طباعة التقرير","Print report"},new[]{"إرسال بالبريد الإلكتروني","Send by email"},new[]{"التقرير المالي","Financial Report"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"اسم الصنف","Product name"},
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
        Add("العملاء", () => Simple("customers","العملاء")); Add("الموردون", () => Simple("suppliers","الموردون")); Add("الموظفون والرواتب", Employees); Add("المصروفات", CashExpense); Add("النسخ الاحتياطي والاستعادة", BackupRestore);
        Add("التقارير المالية", Reports); Add("الدعم عبر واتساب", () => Process.Start(new ProcessStartInfo("https://wa.me/249121851285") { UseShellExecute = true }));
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
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE products ADD COLUMN barcode TEXT"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE employees ADD COLUMN job_title TEXT NOT NULL DEFAULT ''"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE employees ADD COLUMN salary REAL NOT NULL DEFAULT 0"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var migration = c.CreateCommand(); migration.CommandText = "CREATE TABLE IF NOT EXISTS payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
        try { using var indexName = c.CreateCommand(); indexName.CommandText = "CREATE UNIQUE INDEX IF NOT EXISTS idx_products_name ON products(name COLLATE NOCASE)"; indexName.ExecuteNonQuery(); } catch (SqliteException) { }
        using var index = c.CreateCommand(); index.CommandText = "CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL AND barcode != ''"; index.ExecuteNonQuery();
    }
    void Products()
    {
        using var f = new Form { Text=UiLanguage.T("الأصناف والمخزون والباركود"), Width=700, Height=600, RightToLeft=UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout=!UiLanguage.English };
        var n=new TextBox{PlaceholderText=UiLanguage.T("اسم الصنف"),Dock=DockStyle.Top,Height=40}; var barcode=new TextBox{PlaceholderText=UiLanguage.T("الباركود (ماسح USB/Bluetooth أو إدخال يدوي)"),Dock=DockStyle.Top,Height=40}; var q=new TextBox{PlaceholderText=UiLanguage.T("الكمية"),Dock=DockStyle.Top,Height=40}; var p=new TextBox{PlaceholderText=UiLanguage.T("السعر"),Dock=DockStyle.Top,Height=40};
        var add=new Button{Text=UiLanguage.T("إضافة الصنف"),Dock=DockStyle.Top,Height=45}; var list=new ListBox{Dock=DockStyle.Fill};
        add.Click+=(_,_)=>{if(string.IsNullOrWhiteSpace(n.Text)||!double.TryParse(q.Text,out var qty)||!double.TryParse(p.Text,out var price)||qty<0||price<0){MessageBox.Show(UiLanguage.T("أدخل اسمًا وكمية وسعرًا صحيحًا."));return;}try{using var c=C();using var x=c.CreateCommand();x.CommandText="INSERT INTO products(name,barcode,qty,price) VALUES($n,$b,$q,$p)";x.Parameters.AddWithValue("$n",n.Text.Trim());x.Parameters.AddWithValue("$b",string.IsNullOrWhiteSpace(barcode.Text)?DBNull.Value:barcode.Text.Trim());x.Parameters.AddWithValue("$q",qty);x.Parameters.AddWithValue("$p",price);x.ExecuteNonQuery();list.Items.Add(n.Text+" | باركود "+barcode.Text+" | المخزون "+qty+" | السعر "+price);n.Clear();barcode.Clear();q.Clear();p.Clear();RefreshSummary();}catch(SqliteException){MessageBox.Show(UiLanguage.T("تعذر الحفظ: الاسم أو الباركود مستخدم مسبقًا."));}};
        f.Controls.Add(list);f.Controls.Add(add);f.Controls.Add(p);f.Controls.Add(q);f.Controls.Add(barcode);f.Controls.Add(n);f.ShowDialog();
    }
    void Transaction(bool purchase)
    {
        using var f = new Form { Text = UiLanguage.T(purchase ? "المشتريات" : "المبيعات"), Width = 650, Height = 430, RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes, RightToLeftLayout = !UiLanguage.English };
        var n = new TextBox { PlaceholderText = UiLanguage.T("اسم الصنف أو الباركود"), Dock = DockStyle.Top, Height = 40 };
        var q = new TextBox { PlaceholderText = UiLanguage.T("الكمية"), Dock = DockStyle.Top, Height = 40 };
        var p = new TextBox { PlaceholderText = UiLanguage.T("السعر"), Dock = DockStyle.Top, Height = 40 };
        var save = new Button { Text = UiLanguage.T("حفظ"), Dock = DockStyle.Top, Height = 45 };
        save.Click += (_, _) =>
        {
            if (!double.TryParse(q.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var qty) ||
                !double.TryParse(p.Text, System.Globalization.NumberStyles.Number, System.Globalization.CultureInfo.CurrentCulture, out var price) ||
                qty <= 0 || price < 0 || string.IsNullOrWhiteSpace(n.Text))
            {
                MessageBox.Show(UiLanguage.T("أدخل صنفًا وكمية موجبة وسعرًا صحيحًا."));
                return;
            }
            try
            {
                using var c = C();
                using var tx = c.BeginTransaction();
                using var check = c.CreateCommand();
                check.Transaction = tx;
                check.CommandText = "SELECT id, qty, name FROM products WHERE name=$n OR barcode=$n LIMIT 1";
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
                reader.Close();
                var next = oldQty + (purchase ? qty : -qty);
                if (next < 0)
                {
                    MessageBox.Show(UiLanguage.T("الرصيد غير كافٍ."));
                    return;
                }
                using var update = c.CreateCommand();
                update.Transaction = tx;
                update.CommandText = "UPDATE products SET qty=$q WHERE id=$id";
                update.Parameters.AddWithValue("$q", next);
                update.Parameters.AddWithValue("$id", productId);
                update.ExecuteNonQuery();
                using var insert = c.CreateCommand();
                insert.Transaction = tx;
                insert.CommandText = $"INSERT INTO {(purchase ? "purchases" : "sales")}(product,qty,price,total) VALUES($n,$q,$p,$t)";
                insert.Parameters.AddWithValue("$n", productName);
                insert.Parameters.AddWithValue("$q", qty);
                insert.Parameters.AddWithValue("$p", price);
                insert.Parameters.AddWithValue("$t", qty * price);
                insert.ExecuteNonQuery();
                tx.Commit();
                MessageBox.Show(UiLanguage.T("تم الحفظ وتحديث المخزون."));
                RefreshSummary();
            }
            catch (Exception)
            {
                MessageBox.Show(UiLanguage.T("تعذر حفظ العملية؛ لم تُعتمد العملية."));
            }
        };
        f.Controls.Add(save); f.Controls.Add(p); f.Controls.Add(q); f.Controls.Add(n); f.ShowDialog();
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
            using var c = C(); using var q = c.CreateCommand();
            q.CommandText = "INSERT INTO expenses(description,amount) VALUES($d,$a)";
            q.Parameters.AddWithValue("$d", description.Text.Trim()); q.Parameters.AddWithValue("$a", value); q.ExecuteNonQuery();
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
                using (var source = new SqliteConnection(new SqliteConnectionStringBuilder { DataSource = dialog.FileName, Mode = SqliteOpenMode.ReadOnly }.ToString()))\n                using (var destination = C()) { source.Open(); source.BackupDatabase(destination); }\n                InitDb(); RefreshSummary(); MessageBox.Show(UiLanguage.T("تمت الاستعادة. راجع الأرصدة والتقارير."));
            }
            catch (Exception) { MessageBox.Show(UiLanguage.T("فشلت الاستعادة: الملف غير صالح أو غير متوافق.")); }
        };
        f.Controls.Add(restore); f.Controls.Add(export); f.ShowDialog();
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
            q.ExecuteNonQuery(); amount.Clear(); note.Clear(); Reload();
        };
        f.Controls.Add(list); f.Controls.Add(saveEvent); f.Controls.Add(note); f.Controls.Add(amount); f.Controls.Add(kind); f.Controls.Add(person); f.Controls.Add(add); f.Controls.Add(salary); f.Controls.Add(job); f.Controls.Add(name);
        Reload(); f.ShowDialog();
    }

    void Simple(string table,string title)
    {
        using var f=new Form{Text=UiLanguage.T(title),Width=650,Height=500,RightToLeft=UiLanguage.English?RightToLeft.No:RightToLeft.Yes,RightToLeftLayout=!UiLanguage.English};var n=new TextBox{PlaceholderText=UiLanguage.T("الاسم"),Dock=DockStyle.Top,Height=40};var add=new Button{Text=UiLanguage.T("إضافة"),Dock=DockStyle.Top,Height=45};var list=new ListBox{Dock=DockStyle.Fill};
        add.Click+=(_,_)=>{using var c=C();using var x=c.CreateCommand();x.CommandText=$"INSERT INTO {table}(name) VALUES($n)";x.Parameters.AddWithValue("$n",n.Text);x.ExecuteNonQuery();list.Items.Add(n.Text);n.Clear();};f.Controls.Add(list);f.Controls.Add(add);f.Controls.Add(n);f.ShowDialog();
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
    void RefreshSummary()
    {
        using var c=C();using var s=c.CreateCommand();s.CommandText="SELECT COALESCE(SUM(total),0) FROM sales";var sales=Convert.ToDouble(s.ExecuteScalar());using var p=c.CreateCommand();p.CommandText="SELECT COALESCE(SUM(total),0) FROM purchases";var purchases=Convert.ToDouble(p.ExecuteScalar());using var ex=c.CreateCommand();ex.CommandText="SELECT COALESCE(SUM(amount),0) FROM expenses";var expenses=Convert.ToDouble(ex.ExecuteScalar());summary.Text=UiLanguage.T($"المبيعات: {sales:N2}   |   المشتريات: {purchases:N2}   |   المصروفات: {expenses:N2}   |   صافي الحركة: {(sales-purchases-expenses):N2}");
    }
}
