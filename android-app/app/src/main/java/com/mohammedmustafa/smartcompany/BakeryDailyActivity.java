package com.mohammedmustafa.smartcompany;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Standalone daily bakery ledger. Values are stored per day on this device;
 * this module does not post to the general ledger until that integration is separately verified.
 */
public class BakeryDailyActivity extends Activity {
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private LinearLayout body;
    private TextView summary;
    private String day;
    private static final String PREFS = "bakery_daily_ledger";

    private static final String[] FIELD_KEYS = {
        "cuts", "loaves_per_cut", "loaves_per_thousand",
        "stock_flour", "flour_bags_used", "flour_cost_per_bag", "stock_yeast", "stock_bags", "stock_oil", "stock_salt",
        "stock_diesel", "stock_wood", "stock_gas",
        "window_cash", "window_bank", "window_debt",
        "distribution_cash", "distribution_bank", "distribution_debt", "price_difference",
        "spoiled_loaves", "workers_wages", "work_expenses", "shop_meals", "shop_bread_loaves",
        "shop_tea_sugar", "shop_other_expenses", "worker_details", "distribution_details", "notes"
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        buildScreen();
        loadDay();
        recalculate();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(248, 249, 250));
        TextView title = new TextView(this);
        title.setText("🥖 دفتر المخبز اليومي — المحاسب الميزان");
        title.setTextSize(21); title.setTextColor(Color.WHITE); title.setPadding(16, 18, 16, 18);
        title.setBackgroundColor(Color.rgb(11, 31, 58));
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(12, 10, 12, 18);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        addHeading("تاريخ الدفتر");
        TextView date = label(day);
        body.addView(date);
        addHeading("1) الإنتاج والقطعيات");
        addField("cuts", "عدد القطعيات", "4", false);
        addField("loaves_per_cut", "عدد العيش في القطعية الواحدة", "500", false);
        addField("loaves_per_thousand", "عدد العيشات التي تساوي 1,000 جنيه (يدخلها المحاسب)", "4", false);

        addHeading("2) المخزن — الأرصدة/الكميات");
        addField("stock_flour", "رصيد الدقيق قبل تشغيل اليوم (جوال)", "0", false);
        addField("flour_bags_used", "عدد جوالات الدقيق المستخدمة اليوم (تُخصم آليًا من رصيد اليوم)", "0", false);
        addField("flour_cost_per_bag", "تكلفة جوال الدقيق الواحد بالجنيه", "0", false);
        addField("stock_yeast", "الخميرة (الكمية)", "0", false);
        addField("stock_bags", "الأكياس", "0", false);
        addField("stock_oil", "الزيت (الكمية)", "0", false);
        addField("stock_salt", "الملح (الكمية)", "0", false);
        addField("stock_diesel", "الوقود: جاز (الكمية)", "0", false);
        addField("stock_wood", "الوقود: حطب (الكمية)", "0", false);
        addField("stock_gas", "الوقود: غاز (الكمية)", "0", false);

        addHeading("3) بيع الشباك");
        addField("window_cash", "مبيعات الشباك — النقد المقبوض", "0", false);
        addField("window_bank", "مبيعات الشباك — تحويل بنكي", "0", false);
        addField("window_debt", "مبيعات الشباك — الديون المستحقة", "0", false);

        addHeading("4) التوزيع");
        addField("distribution_details", "تفاصيل التوزيع: اسم العميل | عدد العيش | المبلغ | طريقة الدفع/رقم العملية", "", true);
        addField("distribution_cash", "التوزيع — الكاش المقبوض", "0", false);
        addField("distribution_bank", "التوزيع — التحويلات البنكية", "0", false);
        addField("distribution_debt", "التوزيع — الديون", "0", false);
        addField("price_difference", "فرق السعر (موجب أو سالب)", "0", false);

        addHeading("5) التالف");
        addField("spoiled_loaves", "عدد العيش التالف", "0", false);

        addHeading("6) يومية العمال");
        addField("worker_details", "تفاصيل كل عامل: الاسم | الوظيفة | عدد الشغل | سعر الوحدة | الجملة", "", true);
        addField("workers_wages", "إجمالي يومية العمال بالجنيه", "0", false);

        addHeading("7) منصرفات العمل والمحل");
        addField("work_expenses", "منصرفات الشغل والمحل (الصيانة والجاز/الغاز/الحطب تُسجّل مرة واحدة هنا)", "0", false);
        addField("shop_meals", "فطور العمال (جنيه)", "0", false);
        addField("shop_bread_loaves", "عدد عيش العمال", "0", false);
        addField("shop_tea_sugar", "سكر وشاي (جنيه)", "0", false);
        addField("shop_other_expenses", "منصرفات محل أخرى (جنيه)", "0", false);
        addField("notes", "ملاحظات اليوم", "", true);

        addHeading("8) التسوية الآلية");
        summary = label("");
        summary.setTextSize(16); summary.setPadding(14, 14, 14, 14);
        summary.setBackgroundColor(Color.WHITE);
        body.addView(summary);

        Button save = button("💾 حفظ دفتر هذا اليوم");
        save.setOnClickListener(v -> saveDay());
        body.addView(save);
        Button share = button("📤 مشاركة التقرير");
        share.setOnClickListener(v -> shareReport());
        body.addView(share);
        Button prices = button("💳 الأسعار وطلب التفعيل");
        prices.setOnClickListener(v -> showPrices());
        body.addView(prices);
        Button copy = button("نسخ ملخص التسوية");
        copy.setOnClickListener(v -> {
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                ClipData.newPlainText("Bakery daily reconciliation", reportText()));
            toast("تم نسخ ملخص التسوية");
        });
        body.addView(copy);
        body.addView(label("حساب المخزون والتكلفة في هذا الدفتر تقديري ومحلي: رصيد آخر اليوم = رصيد أول اليوم − جوالات الدقيق المستخدمة. هامش الربح = إيرادات القطعيات والتوزيع − قيمة التالف − يومية العمال − المنصرفات − تكلفة الدقيق المستخدم. لا يُرحّل تلقائيًا إلى دفتر الأستاذ العام في هذا الإصدار."));
    }

    private void addHeading(String text) {
        TextView v = label(text); v.setTextSize(18); v.setTextColor(Color.rgb(11,31,58));
        v.setTypeface(null, 1); v.setPadding(8, 14, 8, 4); body.addView(v);
    }
    private TextView label(String text) {
        TextView v = new TextView(this); v.setText(text); v.setTextSize(14);
        v.setTextColor(Color.rgb(30,41,59)); v.setPadding(8, 8, 8, 8); return v;
    }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false);
        b.setTextColor(Color.rgb(11,31,58)); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(212,175,55)));
        return b;
    }
    private void addField(String key, String title, String initial, boolean multiline) {
        body.addView(label(title));
        EditText input = new EditText(this);
        input.setTextSize(16); input.setPadding(12, 8, 12, 8);
        input.setSingleLine(!multiline);
        if (!multiline) input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                ((key.equals("price_difference")) ? android.text.InputType.TYPE_NUMBER_FLAG_SIGNED : 0) |
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        else { input.setMinLines(2); input.setGravity(Gravity.TOP | Gravity.START); }
        input.setHint(title);
        input.setText(initial);
        fields.put(key, input); body.addView(input);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { recalculate(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private long number(String key) {
        String raw = fields.get(key).getText().toString().trim().replace(",", "");
        if (raw.isEmpty()) return 0;
        try { return new java.math.BigDecimal(raw).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact(); }
        catch (Exception ex) { return 0; }
    }
    private long amount(String key) { return number(key); }
    private long spoiledValue() {
        long loaves = Math.max(0, number("spoiled_loaves"));
        long perThousand = number("loaves_per_thousand");
        if (perThousand <= 0) return 0;
        return BakeryLedgerMath.valueForLoaves(loaves, perThousand);
    }
    private long productionLoaves() {
        return BakeryLedgerMath.productionLoaves(Math.max(0, number("cuts")), Math.max(0, number("loaves_per_cut")));
    }
    private long productionValue() {
        long perThousand = number("loaves_per_thousand");
        if (perThousand <= 0) return 0;
        return BakeryLedgerMath.valueForLoaves(productionLoaves(), perThousand);
    }
    private long categorizedValue() {
        // Reconcile the full production value against all destinations:
        // sales/collections + customer debt + price adjustment + spoilage + operating costs.
        // Expenses are included exactly once; fuel/maintenance belong in work_expenses only.
        return BakeryLedgerMath.sum(
            amount("window_cash"), amount("window_bank"), amount("window_debt"),
            amount("distribution_cash"), amount("distribution_bank"), amount("distribution_debt"),
            amount("price_difference"), spoiledValue(), totalExpenses());
    }
    private long shopExpenses() {
        long breadForWorkers = 0;
        long loavesPerThousand = number("loaves_per_thousand");
        if (loavesPerThousand > 0) {
            breadForWorkers = BakeryLedgerMath.valueForLoaves(
                Math.max(0, number("shop_bread_loaves")), loavesPerThousand);
        }
        return BakeryLedgerMath.sum(
            amount("shop_meals"), breadForWorkers, amount("shop_tea_sugar"), amount("shop_other_expenses"));
    }
    private long totalExpenses() {
        return BakeryLedgerMath.sum(amount("workers_wages"), amount("work_expenses"), shopExpenses());
    }
    private long grossRevenue() {
        return BakeryLedgerMath.sum(amount("window_cash"), amount("window_bank"), amount("window_debt"),
            amount("distribution_cash"), amount("distribution_bank"), amount("distribution_debt"));
    }
    private long flourCostForDay() {
        return BakeryLedgerMath.flourCost(Math.max(0, number("flour_bags_used")), Math.max(0, number("flour_cost_per_bag")));
    }
    private void recalculate() {
        if (summary == null || fields.isEmpty()) return;
        try {
            long loaves = productionLoaves(), expected = productionValue(), categorized = categorizedValue();
            long diff = BakeryLedgerMath.reconciliationDifference(expected, categorized);
            long cashNet = BakeryLedgerMath.sum(amount("window_cash"), amount("distribution_cash"), -totalExpenses());
            long bank = BakeryLedgerMath.sum(amount("window_bank"), amount("distribution_bank"));
            long debt = BakeryLedgerMath.sum(amount("window_debt"), amount("distribution_debt"));
            String status = diff == 0 ? "متوازن حسابيًا" : (diff > 0 ? "عجز غير مفسر" : "زيادة غير مفسرة");
            summary.setText("عدد العيش المنتج: " + loaves +
                "\nقيمة الإنتاج حسب سعر المحاسب: " + fmt(expected) + " جنيه" +
                "\nإجمالي المبيعات/التسوية + التالف: " + fmt(categorized) + " جنيه" +
                "\nفرق التسوية (المطلوب صفر): " + fmt(diff) + " جنيه — " + status +
                "\nالديون المستحقة: " + fmt(debt) + " جنيه" +
                "\nالتحويلات البنكية: " + fmt(bank) + " جنيه" +
                "\nصافي النقد بعد اليوميات والمنصرفات: " + fmt(cashNet) + " جنيه" +
                "\nإجمالي يومية العمال والمنصرفات: " + fmt(totalExpenses()) + " جنيه" +
                "\nقيمة التالف: " + fmt(spoiledValue()) + " جنيه");
            summary.setTextColor(diff == 0 ? Color.rgb(22,101,52) : Color.rgb(153,27,27));
        } catch (Exception ex) {
            summary.setText("تعذر الحساب بسبب قيمة غير صالحة أو تجاوز حدود الأرقام. راجع الخانات وأعد المحاولة.");
            summary.setTextColor(Color.RED);
        }
    }
    private String fmt(long value) { return String.format(Locale.US, "%,d", value); }
    private String reportText() {
        recalculate();
        return "دفتر المخبز اليومي — " + day + "\n" + summary.getText().toString() +
            "\n\nتفاصيل يومية العمال:\n" + value("worker_details") +
            "\n\nتفاصيل التوزيع:\n" + value("distribution_details") +
            "\n\nمنصرفات الشغل والمحل تشمل الصيانة والوقود مرة واحدة فقط.\nملاحظات: " + value("notes");
    }
    private String value(String key) { return fields.containsKey(key) ? fields.get(key).getText().toString() : ""; }
    private void saveDay() {
        try {
            JSONObject record = new JSONObject();
            for (String key : FIELD_KEYS) record.put(key, value(key));
            record.put("saved_at", System.currentTimeMillis());
            record.put("report", reportText());
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("day:" + day, record.toString()).commit();
            toast("تم حفظ دفتر " + day + " على هذا الهاتف");
        } catch (Exception ex) { toast("تعذر حفظ الدفتر؛ لم يتم حذف أي بيانات سابقة"); }
    }
    private void loadDay() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString("day:" + day, null);
        if (raw == null) return;
        try {
            JSONObject record = new JSONObject(raw);
            for (String key : FIELD_KEYS) if (fields.containsKey(key) && record.has(key))
                fields.get(key).setText(record.optString(key, ""));
        } catch (Exception ex) { toast("تعذر قراءة سجل اليوم؛ احتفظ به ولا تمسح بيانات التطبيق"); }
    }
    private void shareReport() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "دفتر المخبز اليومي " + day);
        intent.putExtra(Intent.EXTRA_TEXT, reportText());
        startActivity(Intent.createChooser(intent, "مشاركة تقرير اليوم"));
    }
    private void showPrices() {
        String message = "تجربة مجانية لمدة 7 أيام.\n" +
            "الاشتراك الشهري: 30,000 جنيه سوداني\n" +
            "الاشتراك السنوي: 3,000 جنيه سوداني (كما ورد في الطلب؛ السعر أقل من الشهري ويحتاج تأكيدًا قبل البيع)\n" +
            "شراء مرة واحدة: 450,000 جنيه سوداني\n\n" +
            "الدفع عبر بنكك — رقم الحساب: 1882224\n" +
            "أرسل إشعار التحويل إلى واتساب: +249121851285\n" +
            "لا يتم تفعيل الاشتراك تلقائيًا بمجرد التحويل. أرفق الإيصال وانتظر التأكيد.";
        new AlertDialog.Builder(this).setTitle("الأسعار وطلب التفعيل").setMessage(message)
            .setNegativeButton("إغلاق", null)
            .setNeutralButton("نسخ رقم بنكك", (d,w) -> {
                ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Bankak account", "1882224"));
                toast("تم نسخ رقم بنكك");
            })
            .setPositiveButton("إرسال إشعار عبر واتساب", (d,w) -> {
                String text = "طلب تفعيل محاسب المخبز اليومي.\nتاريخ الدفتر: " + day +
                    "\nيرجى تأكيد الباقة والسعر قبل التحويل.\nرقم بنكك: 1882224";
                Intent wa = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/249121851285?text=" + Uri.encode(text)));
                try { startActivity(wa); } catch (Exception ex) { toast("تعذر فتح واتساب؛ انسخ الرقم وأرسل الإشعار يدويًا"); }
            }).show();
    }
    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }
}