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
          new[]{"شركة محمد مصطفي الذكية","Mohammed Mustafa Smart Company"},new[]{"الأصناف والمخزون والباركود","Products, Inventory & Barcode"},new[]{"الأصناف والمخزون","Products & Inventory"},
          new[]{"المبيعات","Sales"},new[]{"المشتريات","Purchases"},new[]{"العملاء","Customers"},new[]{"الموردون","Suppliers"},new[]{"الموظفون","Employees"},new[]{"التقارير المالية","Financial Reports"},
          new[]{"الدعم عبر واتساب","WhatsApp Support"},new[]{"اختيار اللغة / Language","Language: العربية / English"},new[]{"اسم الصنف أو الباركود","Product name or barcode"},new[]{"طباعة التقرير","Print report"},new[]{"إرسال بالبريد الإلكتروني","Send by email"},new[]{"التقرير المالي","Financial Report"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"اسم الصنف","Product name"},
          new[]{"الباركود (ماسح USB/Bluetooth أو إدخال يدوي)","Barcode (USB/Bluetooth scanner or manual entry)"},new[]{"الكمية","Quantity"},new[]{"السعر","Price"},new[]{"إضافة الصنف","Add product"},
          new[]{"أدخل اسمًا وكمية وسعرًا صحيحًا.","Enter a valid name, quantity and price."},new[]{"تعذر الحفظ: الاسم أو الباركود مستخدم مسبقًا.","Could not save: name or barcode already exists."},
          new[]{"باركود","Barcode"},new[]{"المخزون","Stock"},new[]{"فاتورة بيع","Sales Invoice"},new[]{"حفظ","Save"},new[]{"الصنف غير موجود.","Product not found."},
          new[]{"الرصيد غير كافٍ.","Insufficient stock."},new[]{"تم الحفظ وتحديث المخزون.","Saved and inventory updated."},new[]{"الاسم","Name"},new[]{"إضافة","Add"},
          new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"الصافي:","Net:"},new[]{"صافي الحركة:","Net movement:"},new[]{"توقيع المحاسب:","Accountant signature:"},
          new[]{"انتهت التجربة المجانية لمدة 7 أيام","The 7-day free trial has ended"},new[]{"رمز التفعيل","Activation code"},new[]{"تفعيل الاشتراك","Activate subscription"},new[]{"التواصل عبر واتساب","Contact via WhatsApp"},
          new[]{"تم تفعيل الاشتراك.","Subscription activated."},new[]{"رمز التفعيل غير صحيح.","Invalid activation code."},new[]{"التقرير المالي","Financial Report"},new[]{"شركة محمد مصطفي الذكية - الاشتراك","Mohammed Mustafa Smart Company - Subscription"},new[]{"تم تفعيل الاشتراك.","Subscription activated."},new[]{"رمز التفعيل غير صحيح.","Invalid activation code."},new[]{"باركود","Barcode"},new[]{"المخزون","Stock"},new[]{"توقيع المحاسب:","Accountant signature:"},new[]{"صافي الحركة:","Net movement:"},new[]{"المبيعات:","Sales:"},new[]{"المشتريات:","Purchases:"},new[]{"الصافي:","Net:"},new[]{"اختيار اللغة / Language","Language: Arabic / English"}
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
        var license = new LicenseGate();
        if (!license.IsAvailable)
        {
            using var f = new SubscriptionForm(license);
            f.ShowDialog();
            if (!license.IsAvailable) return;
        }
        Application.Run(new MainForm());
    }
}

sealed class LicenseGate
{
    readonly string path = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "SmartCompany", "license.txt");
    public bool IsAvailable
    {
        get
        {
            Directory.CreateDirectory(Path.GetDirectoryName(path)!);
            if (!File.Exists(path)) { File.WriteAllText(path, DateTime.UtcNow.ToString("O")); return true; }
            var s = File.ReadAllText(path).Trim();
            if (s == "ACTIVATED") return true;
            return DateTime.TryParse(s, out var first) && DateTime.UtcNow - first < TimeSpan.FromDays(7);
        }
    }
    public bool Activate(string code)
    {
        if (code.Trim() != "SMART-PAID-2026") return false;
        Directory.CreateDirectory(Path.GetDirectoryName(path)!);
        File.WriteAllText(path, "ACTIVATED");
        return true;
    }
}

sealed class SubscriptionForm : Form
{
    public SubscriptionForm(LicenseGate gate)
    {
        Text = UiLanguage.T("شركة محمد مصطفي الذكية - الاشتراك"); Width = 620; Height = 300;
        StartPosition = FormStartPosition.CenterScreen; RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes; RightToLeftLayout = !UiLanguage.English;
        var title = new Label { Text = UiLanguage.T("انتهت التجربة المجانية لمدة 7 أيام"), Dock = DockStyle.Top, Height = 60, Font = new Font("Segoe UI", 18, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        var code = new TextBox { PlaceholderText = UiLanguage.T("رمز التفعيل"), Dock = DockStyle.Top, Height = 38 };
        var activate = new Button { Text = UiLanguage.T("تفعيل الاشتراك"), Dock = DockStyle.Top, Height = 45 };
        var support = new Button { Text = UiLanguage.T("التواصل عبر واتساب"), Dock = DockStyle.Top, Height = 45 };
        activate.Click += (_, _) => { if (gate.Activate(code.Text)) { MessageBox.Show(UiLanguage.T("تم تفعيل الاشتراك.")); Close(); } else MessageBox.Show(UiLanguage.T("رمز التفعيل غير صحيح.")); };
        support.Click += (_, _) => Process.Start(new ProcessStartInfo("https://wa.me/249121851285") { UseShellExecute = true });
        Controls.Add(support); Controls.Add(activate); Controls.Add(code); Controls.Add(title);
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
        Text = UiLanguage.T("شركة محمد مصطفي الذكية 2.0"); Width = 1050; Height = 720; StartPosition = FormStartPosition.CenterScreen;
        RightToLeft = UiLanguage.English ? RightToLeft.No : RightToLeft.Yes; RightToLeftLayout = !UiLanguage.English;
        var head = new Label { Text = UiLanguage.T("شركة محمد مصطفي الذكية 2.0"), Dock = DockStyle.Top, Height = 75, BackColor = Color.FromArgb(18,55,42), ForeColor = Color.White, Font = new Font("Segoe UI", 23, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        Controls.Add(menu); Controls.Add(summary); Controls.Add(head);
        Add("الأصناف والمخزون والباركود", Products); Add("المبيعات", () => Transaction(false)); Add("المشتريات", () => Transaction(true));
        Add("العملاء", () => Simple("customers","العملاء")); Add("الموردون", () => Simple("suppliers","الموردون")); Add("الموظفون", () => Simple("employees","الموظفون"));
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
        x.CommandText = "CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,price REAL); CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL); CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL); CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT); CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT); CREATE TABLE IF NOT EXISTS employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT);";
        x.ExecuteNonQuery();
        try { using var migration = c.CreateCommand(); migration.CommandText = "ALTER TABLE products ADD COLUMN barcode TEXT"; migration.ExecuteNonQuery(); } catch (SqliteException) { }
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
        var report = UiLanguage.T("التقرير المالي") + Environment.NewLine +
            UiLanguage.T("المبيعات:") + " " + sales.ToString("N2") + Environment.NewLine +
            UiLanguage.T("المشتريات:") + " " + purchases.ToString("N2") + Environment.NewLine +
            UiLanguage.T("صافي الحركة:") + " " + (sales - purchases).ToString("N2") + Environment.NewLine + Environment.NewLine +
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
        using var c=C();using var s=c.CreateCommand();s.CommandText="SELECT COALESCE(SUM(total),0) FROM sales";var sales=Convert.ToDouble(s.ExecuteScalar());using var p=c.CreateCommand();p.CommandText="SELECT COALESCE(SUM(total),0) FROM purchases";var purchases=Convert.ToDouble(p.ExecuteScalar());summary.Text=UiLanguage.T($"المبيعات: {sales:N2}   |   المشتريات: {purchases:N2}   |   الصافي: {(sales-purchases):N2}");
    }
}
