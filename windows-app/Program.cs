using Microsoft.Data.Sqlite;
using System.Diagnostics;
using System.Drawing;

namespace SmartCompany;

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
        Text = "شركة محمد مصطفي الذكية - الاشتراك"; Width = 620; Height = 300;
        StartPosition = FormStartPosition.CenterScreen; RightToLeft = RightToLeft.Yes; RightToLeftLayout = true;
        var title = new Label { Text = "انتهت التجربة المجانية لمدة 7 أيام", Dock = DockStyle.Top, Height = 60, Font = new Font("Segoe UI", 18, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        var code = new TextBox { PlaceholderText = "رمز التفعيل", Dock = DockStyle.Top, Height = 38 };
        var activate = new Button { Text = "تفعيل الاشتراك", Dock = DockStyle.Top, Height = 45 };
        var support = new Button { Text = "التواصل عبر واتساب", Dock = DockStyle.Top, Height = 45 };
        activate.Click += (_, _) => { if (gate.Activate(code.Text)) { MessageBox.Show("تم تفعيل الاشتراك."); Close(); } else MessageBox.Show("رمز التفعيل غير صحيح."); };
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
        Text = "شركة محمد مصطفي الذكية"; Width = 1050; Height = 720; StartPosition = FormStartPosition.CenterScreen;
        RightToLeft = RightToLeft.Yes; RightToLeftLayout = true;
        var head = new Label { Text = "شركة محمد مصطفي الذكية", Dock = DockStyle.Top, Height = 75, BackColor = Color.FromArgb(18,55,42), ForeColor = Color.White, Font = new Font("Segoe UI", 23, FontStyle.Bold), TextAlign = ContentAlignment.MiddleCenter };
        Controls.Add(menu); Controls.Add(summary); Controls.Add(head);
        Add("الأصناف والمخزون", Products); Add("المبيعات", () => Transaction(false)); Add("المشتريات", () => Transaction(true));
        Add("العملاء", () => Simple("customers","العملاء")); Add("الموردون", () => Simple("suppliers","الموردون")); Add("الموظفون", () => Simple("employees","الموظفون"));
        Add("التقارير المالية", Reports); Add("الدعم عبر واتساب", () => Process.Start(new ProcessStartInfo("https://wa.me/249121851285") { UseShellExecute = true }));
        RefreshSummary();
    }

    void Add(string text, Action action)
    {
        var b = new Button { Text = text, Width = 520, Height = 52, Font = new Font("Segoe UI", 13), Margin = new Padding(4) };
        b.Click += (_, _) => action(); menu.Controls.Add(b);
    }
    SqliteConnection C() { var c = new SqliteConnection($"Data Source={dbPath}"); c.Open(); return c; }
    void InitDb()
    {
        using var c = C(); using var x = c.CreateCommand();
        x.CommandText = "CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,price REAL); CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL); CREATE TABLE IF NOT EXISTS purchases(id INTEGER PRIMARY KEY AUTOINCREMENT,product TEXT,qty REAL,price REAL,total REAL); CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT); CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT); CREATE TABLE IF NOT EXISTS employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT);";
        x.ExecuteNonQuery();
    }
    void Products()
    {
        using var f = new Form { Text="الأصناف والمخزون", Width=700, Height=600, RightToLeft=RightToLeft.Yes, RightToLeftLayout=true };
        var n=new TextBox{PlaceholderText="اسم الصنف",Dock=DockStyle.Top,Height=40}; var q=new TextBox{PlaceholderText="الكمية",Dock=DockStyle.Top,Height=40}; var p=new TextBox{PlaceholderText="السعر",Dock=DockStyle.Top,Height=40};
        var add=new Button{Text="إضافة الصنف",Dock=DockStyle.Top,Height=45}; var list=new ListBox{Dock=DockStyle.Fill};
        add.Click+=(_,_)=>{if(!double.TryParse(q.Text,out var qty)||!double.TryParse(p.Text,out var price))return;using var c=C();using var x=c.CreateCommand();x.CommandText="INSERT INTO products(name,qty,price) VALUES($n,$q,$p)";x.Parameters.AddWithValue("$n",n.Text);x.Parameters.AddWithValue("$q",qty);x.Parameters.AddWithValue("$p",price);x.ExecuteNonQuery();list.Items.Add(n.Text+" | المخزون "+qty+" | السعر "+price);n.Clear();q.Clear();p.Clear();RefreshSummary();};
        f.Controls.Add(list);f.Controls.Add(add);f.Controls.Add(p);f.Controls.Add(q);f.Controls.Add(n);f.ShowDialog();
    }
    void Transaction(bool purchase)
    {
        using var f=new Form{Text=purchase?"المشتريات":"المبيعات",Width=650,Height=430,RightToLeft=RightToLeft.Yes,RightToLeftLayout=true};
        var n=new TextBox{PlaceholderText="اسم الصنف",Dock=DockStyle.Top,Height=40};var q=new TextBox{PlaceholderText="الكمية",Dock=DockStyle.Top,Height=40};var p=new TextBox{PlaceholderText="السعر",Dock=DockStyle.Top,Height=40};var save=new Button{Text="حفظ",Dock=DockStyle.Top,Height=45};
        save.Click+=(_,_)=>{if(!double.TryParse(q.Text,out var qty)||!double.TryParse(p.Text,out var price))return;using var c=C();using var check=c.CreateCommand();check.CommandText="SELECT qty FROM products WHERE name=$n";check.Parameters.AddWithValue("$n",n.Text);var o=check.ExecuteScalar();if(o is null){MessageBox.Show("الصنف غير موجود.");return;}var next=Convert.ToDouble(o)+(purchase?qty:-qty);if(next<0){MessageBox.Show("الرصيد غير كافٍ.");return;}using var u=c.CreateCommand();u.CommandText="UPDATE products SET qty=$q WHERE name=$n";u.Parameters.AddWithValue("$q",next);u.Parameters.AddWithValue("$n",n.Text);u.ExecuteNonQuery();using var ins=c.CreateCommand();ins.CommandText=$"INSERT INTO {(purchase?"purchases":"sales")}(product,qty,price,total) VALUES($n,$q,$p,$t)";ins.Parameters.AddWithValue("$n",n.Text);ins.Parameters.AddWithValue("$q",qty);ins.Parameters.AddWithValue("$p",price);ins.Parameters.AddWithValue("$t",qty*price);ins.ExecuteNonQuery();MessageBox.Show("تم الحفظ وتحديث المخزون.");RefreshSummary();};
        f.Controls.Add(save);f.Controls.Add(p);f.Controls.Add(q);f.Controls.Add(n);f.ShowDialog();
    }
    void Simple(string table,string title)
    {
        using var f=new Form{Text=title,Width=650,Height=500,RightToLeft=RightToLeft.Yes,RightToLeftLayout=true};var n=new TextBox{PlaceholderText="الاسم",Dock=DockStyle.Top,Height=40};var add=new Button{Text="إضافة",Dock=DockStyle.Top,Height=45};var list=new ListBox{Dock=DockStyle.Fill};
        add.Click+=(_,_)=>{using var c=C();using var x=c.CreateCommand();x.CommandText=$"INSERT INTO {table}(name) VALUES($n)";x.Parameters.AddWithValue("$n",n.Text);x.ExecuteNonQuery();list.Items.Add(n.Text);n.Clear();};f.Controls.Add(list);f.Controls.Add(add);f.Controls.Add(n);f.ShowDialog();
    }
    void Reports()
    {
        using var c=C();using var s=c.CreateCommand();s.CommandText="SELECT COALESCE(SUM(total),0) FROM sales";var sales=Convert.ToDouble(s.ExecuteScalar());using var p=c.CreateCommand();p.CommandText="SELECT COALESCE(SUM(total),0) FROM purchases";var purchases=Convert.ToDouble(p.ExecuteScalar());MessageBox.Show($"المبيعات: {sales:N2}\nالمشتريات: {purchases:N2}\nصافي الحركة: {(sales-purchases):N2}\n\nتوقيع المحاسب: ____________________","التقرير المالي");
    }
    void RefreshSummary()
    {
        using var c=C();using var s=c.CreateCommand();s.CommandText="SELECT COALESCE(SUM(total),0) FROM sales";var sales=Convert.ToDouble(s.ExecuteScalar());using var p=c.CreateCommand();p.CommandText="SELECT COALESCE(SUM(total),0) FROM purchases";var purchases=Convert.ToDouble(p.ExecuteScalar());summary.Text=$"المبيعات: {sales:N2}   |   المشتريات: {purchases:N2}   |   الصافي: {(sales-purchases):N2}";
    }
}
