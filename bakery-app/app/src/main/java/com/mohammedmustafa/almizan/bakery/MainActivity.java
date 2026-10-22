package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(11,31,58), GOLD = Color.rgb(212,175,55), BG = Color.rgb(246,248,251);
    private static final String SUPPORT = "249121851285", BANKAK = "1882224";
    private static final int REQUEST_EXPORT_BACKUP = 4250, REQUEST_IMPORT_BACKUP = 4251;
    private Store store;
    private LinearLayout root, content;
    private String day;
    private TextView dateLabel, statusLabel;

    private static class Field {
        final String key, label, hint; final boolean numeric;
        Field(String k, String l, String h, boolean n) { key=k; label=l; hint=h; numeric=n; }
    }
    private static Field f(String k, String l) { return new Field(k,l,"",true); }
    private static Field t(String k, String l, String hint) { return new Field(k,l,hint,false); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store();
        day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (trialExpired()) { showPaywall(); return; }
        buildHome();
    }

    private boolean trialExpired() {
        long now = System.currentTimeMillis();
        long start = getPreferences(0).getLong("trial_start", 0L);
        if (start == 0L) { getPreferences(0).edit().putLong("trial_start", now).apply(); return false; }
        return now - start >= 7L * 24L * 60L * 60L * 1000L;
    }

    private void base(String title) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        TextView head = new TextView(this); head.setText(title); head.setTextSize(21); head.setTextColor(Color.WHITE);
        head.setTypeface(null, 1); head.setPadding(16,18,16,18); head.setBackgroundColor(NAVY); root.addView(head);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(12,10,12,18);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void buildHome() {
        base("🥖 الميزان المحاسبي للمخابز 2.5.0");
        TextView intro = text("نظام يومي للمخبز: أدخل بيانات كل قسم، احفظها بتاريخها، ثم افتح «المحاسب والحساب» لمراجعة التسوية.",16);
        content.addView(intro);
        LinearLayout dateRow = new LinearLayout(this); dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateLabel = text("📅 التاريخ: " + day,16); dateRow.addView(dateLabel,new LinearLayout.LayoutParams(0,-2,1f));
        Button choose = button("اختيار التاريخ"); choose.setOnClickListener(v -> chooseDate()); dateRow.addView(choose); content.addView(dateRow);
        statusLabel = text("",14); content.addView(statusLabel);
        addCard("📦 1. المخزن","الدقيق، الخميرة، الأكياس، الزيت، الملح، الحطب، الغاز، الجهاز",()->openModule("stock"));
        addCard("🥖 2. القطععيات","عدد الشغل، عدد العيش المنتج، وسعر التحويل إلى الجنيه",()->openModule("cuts"));
        addCard("👷 3. يومية العمال","اسم العامل، الوظيفة، عدد الشغل، عمالة الجوال، وجملة العمل",()->openModule("workers"));
        addCard("💵 4. الكاش وبيع الشباك","النقد، الديون، التحويل البنكي ورقم العملية",()->openModule("window"));
        addCard("🚚 5. التوزيع","اسم العميل، الكمية، الكاش، الديون، التحويل وفرق السعر",()->openModule("distribution"));
        addCard("🧾 6. منصرفات المحل","وجبات العمال، الصيانة، المشتريات والمنصرفات الأخرى",()->openModule("expenses"));
        addCard("♻️ 7. التالف","عدد العيش التالف وسبب التلف",()->openModule("spoilage"));
        addCard("🧮 8. المحاسب والحساب","جمع القطععيات ومراجعة الكاش والديون والتحويلات واليوميات والمنصرفات والتالف",this::showAccounts);
        addCard("🏭 9. حساب التشغيل والتكلفة","تكلفة الوقود والدقيق والخميرة والزيت والملح والأكياس، وتقدير هامش الربح",this::showCosting);
        Button backup = button("🗂️ نسخ احتياطي واستعادة البيانات"); backup.setOnClickListener(v->backupRestoreMenu()); content.addView(backup);
        Button guide = button("📘 اللوحة الإرشادية وطريقة الاستخدام"); guide.setOnClickListener(v->showGuide()); content.addView(guide);
        Button pricing = button("🔐 الاشتراك والأسعار والتفعيل"); pricing.setOnClickListener(v->showPaywall()); content.addView(pricing);
        Button support = button("🟢 الدعم الفني والتفعيل عبر واتساب"); support.setOnClickListener(v->openWhatsApp("السلام عليكم، أحتاج دعمًا فنيًا لتطبيق الميزان المحاسبي للمخابز 2.5.0.")); content.addView(support);
        updateStatus();
    }

    private void addCard(String title, String description, Runnable action) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(14,10,14,10);
        card.setBackgroundColor(Color.WHITE);
        TextView h = text(title,18); h.setTextColor(NAVY); h.setTypeface(null,1); card.addView(h);
        card.addView(text(description,14));
        Button open = button("فتح القسم"); open.setOnClickListener(v->action.run()); card.addView(open);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,6,0,6); content.addView(card,lp);
    }

    private TextView text(String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.rgb(31,41,55));
        v.setPadding(7,8,7,8); return v;
    }
    private Button button(String value) {
        Button b = new Button(this); b.setText(value); b.setAllCaps(false); b.setTextColor(NAVY);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GOLD)); return b;
    }
    private void chooseDate() {
        Calendar c = Calendar.getInstance();
        try { c.setTime(new SimpleDateFormat("yyyy-MM-dd",Locale.US).parse(day)); } catch(Exception ignored) {}
        new DatePickerDialog(this,(view,y,m,d)->{
            day=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);
            if(dateLabel!=null) dateLabel.setText("📅 التاريخ: "+day);
            updateStatus();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private Field[] schema(String module) {
        switch(module) {
            case "stock": return new Field[]{
                f("flour","الدقيق — عدد الجوالات"),f("yeast","الخميرة — الكمية"),f("bags","الأكياس — الكمية"),
                f("oil","الزيت — الكمية"),f("salt","الملح — الكمية"),f("wood","الحطب — الكمية"),
                f("gas","الغاز — الكمية"),t("equipment","الجهاز / المعدات","اسم الجهاز وحالته وأي ملاحظة"),
                t("notes","ملاحظات المخزن","")
            };
            case "cuts": return new Field[]{
                f("work_units","عدد الشغل/القطععيات اليومية"),f("loaves_per_unit","عدد العيش في القطعية الواحدة"),
                f("loaves_per_1000","عدد العيشات التي تساوي 1,000 جنيه")
            };
            case "workers": return new Field[]{
                t("rows","تفاصيل العمال — سطر لكل عامل","الاسم | الوظيفة | عدد الشغل | أجرة الوحدة\nمثال: أحمد | خباز | 10 | 500"),
                t("notes","ملاحظات يومية العمال","")
            };
            case "window": return new Field[]{
                f("cash","الكاش المقبوض من الشباك (جنيه)"),f("debt","ديون الشباك (جنيه)"),
                f("bank","تحويلات الشباك عبر بنكك/البنك (جنيه)"),t("bank_ref","رقم العملية/مرجع التحويل",""),
                t("notes","ملاحظات بيع الشباك","")
            };
            case "distribution": return new Field[]{
                t("rows","سجل التوزيع — سطر لكل عميل","العميل | الكمية | الكاش | الديون | التحويل البنكي | فرق السعر\nمثال: بقالة النيل | 200 | 50000 | 10000 | 0 | 0"),
                t("notes","ملاحظات التوزيع","")
            };
            case "expenses": return new Field[]{
                f("meals","وجبات العمال (جنيه)"),f("maintenance","الصيانة (جنيه)"),f("purchases","المشتريات (جنيه)"),
                t("rows","منصرفات أخرى — سطر لكل منصرف","البيان | المبلغ\nمثال: ماء | 2500"),
                t("notes","ملاحظات المنصرفات","")
            };
            case "spoilage": return new Field[]{f("loaves","عدد العيش التالف"),t("reason","سبب التلف / الملاحظات","")};
            case "costing": return new Field[]{
                f("fuel_quantity","الوقود — الكمية المستخدمة"),f("fuel_unit_price","سعر وحدة الوقود (جنيه)"),
                f("flour_bags","الدقيق — عدد الجوالات المستخدمة"),f("flour_bag_price","سعر جوال الدقيق الواحد (جنيه)"),
                f("yeast_count","الخميرة — عدد القطع المستخدمة"),f("yeast_piece_price","سعر قطعة الخميرة (جنيه)"),
                f("oil_quantity","الزيت — الكمية المستخدمة بالرطل"),f("oil_pound_price","سعر رطل الزيت (جنيه)"),
                f("salt_quantity","الملح — الكمية المستخدمة"),f("salt_unit_price","سعر وحدة الملح (جنيه)"),
                f("bags_total_price","جملة سعر الأكياس المستخدمة (جنيه)")
            };
            default: return new Field[0];
        }
    }

    private String titleFor(String module) {
        switch(module) {
            case "stock": return "المخزن"; case "cuts": return "القطععيات"; case "workers": return "يومية العمال";
            case "window": return "الكاش وبيع الشباك"; case "distribution": return "التوزيع";
            case "expenses": return "منصرفات المحل"; case "spoilage": return "التالف"; case "costing": return "حساب التشغيل والتكلفة"; default: return module;
        }
    }

    private void openModule(String module) {
        Field[] fields = schema(module);
        JSONObject saved = store.get(day,module);
        ScrollView sc = new ScrollView(this); LinearLayout box = new LinearLayout(this);
        box.setPadding(14,8,14,12); box.setOrientation(LinearLayout.VERTICAL); sc.addView(box);
        final java.util.Map<String,EditText> inputs = new java.util.LinkedHashMap<>();
        for(Field field:fields) {
            box.addView(text(field.label,15));
            EditText e = new EditText(this); e.setTextSize(16); e.setHint(field.hint); e.setPadding(10,8,10,8);
            if(field.numeric) { e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED); e.setSingleLine(true); }
            else { e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); e.setMinLines(field.key.equals("rows")?4:1); e.setGravity(Gravity.TOP|Gravity.START); }
            e.setText(saved.optString(field.key,"")); inputs.put(field.key,e); box.addView(e);
        }
        new AlertDialog.Builder(this).setTitle(titleFor(module)+" — "+day).setView(sc)
            .setNegativeButton("إلغاء",null).setNeutralButton("مسح خانات هذا القسم",(d,w)->{
                new AlertDialog.Builder(this).setMessage("هل تريد مسح بيانات هذا القسم لهذا التاريخ؟").setNegativeButton("لا",null)
                    .setPositiveButton("نعم، امسح القسم",(dd,ww)->{store.delete(day,module);updateStatus();toast("تم مسح القسم المحدد لهذا التاريخ فقط");}).show();
            }).setPositiveButton("حفظ بيانات القسم",(d,w)->{
                try {
                    JSONObject record=new JSONObject();
                    for(Field field:fields) {
                        String val=inputs.get(field.key).getText().toString().trim();
                        if(field.numeric && !val.isEmpty()) {
                            long n=parseLong(val);
                            if(n<0) throw new IllegalArgumentException("لا تُقبل الكميات أو المبالغ السالبة في هذا الحقل: "+field.label);
                        }
                        record.put(field.key,val);
                    }
                    store.put(day,module,record); updateStatus(); toast("حُفظ "+titleFor(module)+" بتاريخ "+day);
                } catch(Exception ex) { toast("لم تُحفظ البيانات: "+ex.getMessage()); }
            }).show();
    }

    private long parseLong(String value) {
        StringBuilder normalized=new StringBuilder();
        for(int i=0;i<value.length();i++) {
            char ch=value.charAt(i);
            if(ch>='٠' && ch<='٩') normalized.append((char)('0'+(ch-'٠')));
            else if(ch>='۰' && ch<='۹') normalized.append((char)('0'+(ch-'۰')));
            else if(ch==',' || ch=='٬' || Character.isWhitespace(ch)) { /* ignore grouping spaces */ }
            else normalized.append(ch);
        }
        String s=normalized.toString().trim();
        if(s.isEmpty()) return 0L;
        return new BigDecimal(s).setScale(0,RoundingMode.UNNECESSARY).longValueExact();
    }
    private JSONObject record(String module) { return store.get(day,module); }
    private long val(String module,String key) {
        try { return Math.max(0,parseLong(record(module).optString(key,"0"))); } catch(Exception e) { return 0; }
    }
    private static class Totals { long cash,debt,bank,diff,qty,amount; int badRows; }
    private Totals rowTotals(String module,String key,int columns) {
        Totals out=new Totals(); String raw=record(module).optString(key,"");
        if(raw.trim().isEmpty()) return out;
        String[] lines=raw.split("\\r?\\n");
        for(String line:lines) {
            if(line.trim().isEmpty()) continue;
            String[] p=line.split("\\|",-1);
            if(p.length<columns) { out.badRows++; continue; }
            try {
                if(module.equals("workers")) {
                    long count=parseLong(p[2]), rate=parseLong(p[3]);
                    if(count<0||rate<0) throw new IllegalArgumentException();
                    out.amount=Math.addExact(out.amount,Math.multiplyExact(count,rate));
                } else if(module.equals("distribution")) {
                    long qty=parseLong(p[1]), cash=parseLong(p[2]), debt=parseLong(p[3]), bank=parseLong(p[4]), diff=parseLong(p[5]);
                    if(qty<0||cash<0||debt<0||bank<0) throw new IllegalArgumentException();
                    out.qty=Math.addExact(out.qty,qty); out.cash=Math.addExact(out.cash,cash);
                    out.debt=Math.addExact(out.debt,debt); out.bank=Math.addExact(out.bank,bank); out.diff=Math.addExact(out.diff,diff);
                } else {
                    long amount=parseLong(p[1]); if(amount<0) throw new IllegalArgumentException();
                    out.amount=Math.addExact(out.amount,amount);
                }
            } catch(Exception ex) { out.badRows++; }
        }
        return out;
    }
    private long productionLoaves() { return BakeryMath.productionLoaves(val("cuts","work_units"),val("cuts","loaves_per_unit")); }
    private long valueForLoaves(long loaves) {
        long perThousand=val("cuts","loaves_per_1000");
        if(perThousand<=0) return 0;
        return BakeryMath.valueForLoaves(loaves,perThousand);
    }
    private long wages() { return rowTotals("workers","rows",4).amount; }
    private long otherExpenses() { return rowTotals("expenses","rows",2).amount; }
    private long expenseTotal() { return Math.addExact(Math.addExact(val("expenses","meals"),val("expenses","maintenance")),Math.addExact(val("expenses","purchases"),otherExpenses())); }
    private long spoilageValue() { return valueForLoaves(val("spoilage","loaves")); }
    private Totals distribution() { return rowTotals("distribution","rows",6); }
    private long productionValue() { return valueForLoaves(productionLoaves()); }
    private long costingTotal() {
        return BakeryMath.sum(
            Math.multiplyExact(val("costing","fuel_quantity"),val("costing","fuel_unit_price")),
            Math.multiplyExact(val("costing","flour_bags"),val("costing","flour_bag_price")),
            Math.multiplyExact(val("costing","yeast_count"),val("costing","yeast_piece_price")),
            Math.multiplyExact(val("costing","oil_quantity"),val("costing","oil_pound_price")),
            Math.multiplyExact(val("costing","salt_quantity"),val("costing","salt_unit_price")),
            val("costing","bags_total_price"));
    }
    private void showCosting() {
        try {
            long production=productionValue(), cost=costingTotal(), margin=Math.subtractExact(production,cost);
            String message="تاريخ التكلفة: "+day+
                "\nقيمة الإنتاج حسب القطععيات: "+fmt(production)+" جنيه"+
                "\nتكلفة الوقود: "+fmt(Math.multiplyExact(val("costing","fuel_quantity"),val("costing","fuel_unit_price")))+" جنيه"+
                "\nتكلفة الدقيق: "+fmt(Math.multiplyExact(val("costing","flour_bags"),val("costing","flour_bag_price")))+" جنيه"+
                "\nتكلفة الخميرة: "+fmt(Math.multiplyExact(val("costing","yeast_count"),val("costing","yeast_piece_price")))+" جنيه"+
                "\nتكلفة الزيت: "+fmt(Math.multiplyExact(val("costing","oil_quantity"),val("costing","oil_pound_price")))+" جنيه"+
                "\nتكلفة الملح: "+fmt(Math.multiplyExact(val("costing","salt_quantity"),val("costing","salt_unit_price")))+" جنيه"+
                "\nتكلفة الأكياس: "+fmt(val("costing","bags_total_price"))+" جنيه"+
                "\n\nإجمالي تكلفة التشغيل المدخلة: "+fmt(cost)+" جنيه"+
                "\nهامش الربح التقديري = قيمة الإنتاج - تكلفة التشغيل: "+fmt(margin)+" جنيه"+
                "\n\nتنبيه: الهامش تقديري للمواد والوقود المدخلين فقط؛ أضف كل التكاليف الأخرى التي تنطبق على المخبز. لا تكرر نفس التكلفة في تسوية المحاسب والمنصرفات.";
            new AlertDialog.Builder(this).setTitle("🏭 حساب التشغيل والتكلفة — "+day).setMessage(message)
                .setNegativeButton("إغلاق",null)
                .setNeutralButton("تعديل بيانات التكلفة",(d,w)->openModule("costing"))
                .setPositiveButton("مشاركة التقرير",(d,w)->shareReport(message)).show();
        } catch(Exception ex) { toast("تعذر حساب تكلفة التشغيل بسبب رقم كبير جدًا أو بيانات غير صحيحة. راجع الكميات والأسعار."); }
    }
    private long cashTotal() { Totals d=distribution(); return Math.addExact(val("window","cash"),d.cash); }
    private long debtTotal() { Totals d=distribution(); return Math.addExact(val("window","debt"),d.debt); }
    private long bankTotal() { Totals d=distribution(); return Math.addExact(val("window","bank"),d.bank); }
    private long distributionDiff() { return distribution().diff; }
    private long accountedTotal() {
        return BakeryMath.accountedTotal(cashTotal(),debtTotal(),bankTotal(),distributionDiff(),wages(),expenseTotal(),spoilageValue());
    }
    private String fmt(long n) { return String.format(Locale.US,"%,d",n); }

    private void showAccounts() {
        try {
            long loaves=productionLoaves(), expected=productionValue(), actual=accountedTotal(), difference=Math.subtractExact(expected,actual);
            Totals dist=distribution(); int bad=rowTotals("workers","rows",4).badRows+dist.badRows+rowTotals("expenses","rows",2).badRows;
            String status=difference==0?"متوازن حسابيًا":difference>0?"عجز غير مفسر":"زيادة غير مفسرة";
            String message="تاريخ الدفتر: "+day+
                "\n\nالعيش المنتج: "+fmt(loaves)+" رغيف"+
                "\nقيمة القطععيات حسب السعر المدخل: "+fmt(expected)+" جنيه"+
                "\nالكاش (الشباك والتوزيع): "+fmt(cashTotal())+" جنيه"+
                "\nالديون المستحقة: "+fmt(debtTotal())+" جنيه"+
                "\nالتحويلات البنكية: "+fmt(bankTotal())+" جنيه"+
                "\nكمية التوزيع المسجلة: "+fmt(dist.qty)+" رغيف"+
                "\nفرق السعر: "+fmt(distributionDiff())+" جنيه"+
                "\nيومية العمال المحسوبة من السطور: "+fmt(wages())+" جنيه"+
                "\nمنصرفات المحل: "+fmt(expenseTotal())+" جنيه"+
                "\nقيمة التالف: "+fmt(spoilageValue())+" جنيه"+
                "\nتكلفة التشغيل التقديرية (للمقارنة فقط): "+fmt(costingTotal())+" جنيه"+
                "\nهامش الربح التقديري: "+fmt(Math.subtractExact(expected,costingTotal()))+" جنيه"+
                "\n\nإجمالي البنود المسجلة: "+fmt(actual)+" جنيه"+
                "\nفرق التسوية (المطلوب صفر): "+fmt(difference)+" جنيه"+
                "\nالنتيجة: "+status+
                (bad>0?"\n\n⚠ توجد "+bad+" سطور غير مكتملة أو غير رقمية. راجع صيغة السطور.":"")+
                (val("cuts","loaves_per_1000")<=0?"\n⚠ أدخل عدد العيش المقابل لـ 1,000 جنيه في القطععيات.":"");
            new AlertDialog.Builder(this).setTitle("🧮 المحاسب والحساب — "+day).setMessage(message)
                .setNegativeButton("إغلاق",null).setNeutralButton("مشاركة التقرير",(d,w)->shareReport(message))
                .setPositiveButton("حفظ التقرير", (d,w)->{store.put(day,"accounting",jsonSummary(message));toast("تم حفظ ملخص التسوية لهذا التاريخ");}).show();
        } catch(Exception ex) { toast("تعذر إتمام الحساب بسبب رقم كبير جدًا أو بيانات غير صحيحة. راجع المدخلات."); }
    }
    private JSONObject jsonSummary(String message) { JSONObject j=new JSONObject(); try{j.put("summary",message);j.put("saved_at",System.currentTimeMillis());}catch(Exception ignored){} return j; }
    private void shareReport(String report) {
        Intent i=new Intent(Intent.ACTION_SEND); i.setType("text/plain"); i.putExtra(Intent.EXTRA_SUBJECT,"تسوية المخبز "+day); i.putExtra(Intent.EXTRA_TEXT,report);
        startActivity(Intent.createChooser(i,"مشاركة تسوية المخبز"));
    }
    private void updateStatus() {
        if(statusLabel==null) return;
        boolean has=store.has(day);
        statusLabel.setText(has?"حالة "+day+": توجد بيانات محفوظة على هذا الهاتف. افتح المحاسب والحساب للمراجعة.":"حالة "+day+": لم تُحفظ بيانات لهذا التاريخ بعد.");
    }


    private void backupRestoreMenu() {
        new AlertDialog.Builder(this).setTitle("نسخ احتياطي واستعادة")
            .setMessage("أنشئ ملف JSON يحفظ سجلات الأيام والأقسام، واحتفظ به خارج الهاتف. الاستعادة تدمج السجلات وتستبدل فقط السجل الذي يطابق التاريخ والقسم.")
            .setNegativeButton("إغلاق",null)
            .setNeutralButton("استعادة نسخة",(d,w)->{
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/json");
                try { startActivityForResult(i,REQUEST_IMPORT_BACKUP); } catch(Exception ex) { toast("تعذر فتح اختيار ملف النسخة الاحتياطية"); }
            })
            .setPositiveButton("تصدير نسخة",(d,w)->{
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/json");
                i.putExtra(Intent.EXTRA_TITLE,"almizan-bakery-backup-"+day+".json");
                try { startActivityForResult(i,REQUEST_EXPORT_BACKUP); } catch(Exception ex) { toast("تعذر فتح اختيار مكان حفظ النسخة"); }
            }).show();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=RESULT_OK || data==null || data.getData()==null) return;
        try {
            if(requestCode==REQUEST_EXPORT_BACKUP) {
                JSONArray records=store.exportAll();
                try(OutputStream out=getContentResolver().openOutputStream(data.getData(),"w")) {
                    if(out==null) throw new IllegalStateException("تعذر فتح ملف النسخة");
                    out.write(records.toString(2).getBytes(StandardCharsets.UTF_8)); out.flush();
                }
                toast("تم تصدير "+records.length()+" سجل. احتفظ بالملف في مكان آمن.");
            } else if(requestCode==REQUEST_IMPORT_BACKUP) {
                byte[] bytes;
                try(InputStream in=getContentResolver().openInputStream(data.getData())) {
                    if(in==null) throw new IllegalStateException("تعذر فتح ملف النسخة");
                    java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
                    byte[] chunk=new byte[8192]; int n; long total=0;
                    while((n=in.read(chunk))!=-1) { total+=n; if(total>10L*1024*1024) throw new IllegalArgumentException("حجم النسخة أكبر من 10 ميغابايت"); buffer.write(chunk,0,n); }
                    bytes=buffer.toByteArray();
                }
                JSONArray records=new JSONArray(new String(bytes,StandardCharsets.UTF_8));
                new AlertDialog.Builder(this).setTitle("تأكيد الاستعادة")
                    .setMessage("سيتم دمج "+records.length()+" سجل مع السجلات الحالية. إذا تطابق التاريخ والقسم فستحل بيانات النسخة محل السجل المطابق فقط. أنشئ نسخة من الحالة الحالية قبل المتابعة.")
                    .setNegativeButton("إلغاء",null).setPositiveButton("استعادة ودمج",(d,w)->{
                        try { int count=store.restoreAll(records); updateStatus(); toast("اكتملت الاستعادة: "+count+" سجل. راجع التسوية قبل الاستخدام."); }
                        catch(Exception ex) { toast("رفضت الاستعادة لأن الملف غير صالح؛ لم يتم اعتماد التغييرات."); }
                    }).show();
            }
        } catch(Exception ex) { toast("تعذر التعامل مع ملف النسخة الاحتياطية. لم يتم حذف البيانات الحالية."); }
    }

    private void showGuide() {
        String guide="طريقة الاستخدام — الميزان المحاسبي للمخابز 2.5.0\n\n"+
            "1) اختر التاريخ من رزنامة الهاتف داخل التطبيق؛ لكل تاريخ دفتر مستقل.\n"+
            "2) المخزن: أدخل أرصدة/كميات الدقيق والخميرة والأكياس والزيت والملح والحطب والغاز والجهاز. هذه بيانات مخزن وليست خصمًا تلقائيًا من المخزون حتى الآن.\n"+
            "3) القطععيات: أدخل عدد الشغل، وعدد الأرغفة لكل قطعة، وعدد الأرغفة المقابل لـ 1,000 جنيه. يحسب التطبيق كمية العيش وقيمة الإنتاج.\n"+
            "4) يومية العمال: كل عامل في سطر بصيغة الاسم | الوظيفة | عدد الشغل | أجرة الوحدة. الأجر = عدد الشغل × أجرة الوحدة.\n"+
            "5) بيع الشباك: أدخل الكاش والديون والتحويلات. احتفظ برقم العملية في خانة المرجع.\n"+
            "6) التوزيع: كل عميل في سطر: الاسم | الكمية | الكاش | الديون | التحويل البنكي | فرق السعر.\n"+
            "7) منصرفات المحل: أدخل الوجبات والصيانة والمشتريات، ثم أي منصرف إضافي في سطر البيان | المبلغ. لا تكرر نفس المصروف في أكثر من خانة.\n"+
            "8) التالف: أدخل عدد الأرغفة التالفة؛ يحسب التطبيق قيمتها باستخدام سعر القطععيات.\n"+
            "9) المحاسب والحساب: يساوي قيمة الإنتاج مجموع الكاش والديون والتحويلات وفرق السعر واليوميات والمنصرفات وقيمة التالف وفق قاعدة التسوية المعتمدة. فرق التسوية يجب أن يساوي صفرًا، وإلا راجع البنود.\n"+
            "10) حساب التشغيل والتكلفة: أدخل الكميات المستخدمة وأسعار وحداتها؛ يحسب التطبيق تكلفة الوقود والدقيق والخميرة والزيت والملح والأكياس، ثم يعرض هامشًا تقديريًا = قيمة الإنتاج - تكلفة المواد/الوقود المدخلة. لا تكرر المصروف نفسه في المنصرفات وفي التكلفة عند جمع إجمالياتك.\n\n"+
            "مهم: البيانات تحفظ محليًا على الجهاز ولا تُرفع للسحابة. استخدم زر «نسخ احتياطي واستعادة البيانات» لتصدير ملف JSON إلى مكان آمن واختبر استعادته. التطبيق لا يُصدر قيدًا محاسبيًا عامًا ولا يُعد بديلًا عن مراجعة محاسب قبل استخدام النتائج لإقرار مالي أو ضريبي.";
        new AlertDialog.Builder(this).setTitle("📘 اللوحة الإرشادية").setMessage(guide).setPositiveButton("فهمت",null).show();
    }

    private void showPaywall() {
        base("🔐 اشتراك الميزان المحاسبي للمخابز");
        content.addView(text("انتهت فترة التجربة المجانية (7 أيام). بيانات الدفاتر المحلية لا تُحذف بسبب انتهاء التجربة. تواصل مع الدعم لإرسال إشعار التحويل وطلب التفعيل اليدوي.",16));
        content.addView(text("الاشتراك الشهري: 30,000 جنيه سوداني",18));
        content.addView(text("الاشتراك السنوي: 300,000 جنيه سوداني",18));
        content.addView(text("شراء مرة واحدة: 450,000 جنيه سوداني",18));
        content.addView(text("الدفع: بنكك — رقم الحساب 1882224",16));
        Button copy=button("نسخ رقم حساب بنكك"); copy.setOnClickListener(v->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("Bankak",BANKAK));toast("تم نسخ رقم بنكك");}); content.addView(copy);
        Button wa=button("إرسال إشعار التحويل عبر واتساب"); wa.setOnClickListener(v->openWhatsApp("السلام عليكم، أطلب تفعيل الميزان المحاسبي للمخابز 2.5.0.\nالتاريخ: "+day+"\nالباقة المطلوبة: [شهري/سنوي/شراء مرة واحدة]\nالمبلغ المحول: [اكتب المبلغ]\nرقم عملية بنكك: [اكتب الرقم]. سأرفق الإيصال هنا.")); content.addView(wa);
        content.addView(text("التفعيل بعد مراجعة التحويل من الدعم فقط. لا تدخل رمز بنكك أو كلمة مرورك في التطبيق أو ترسلها لأي شخص.",14));
        Button backup=button("🗂️ تصدير / استعادة بياناتك"); backup.setOnClickListener(v->backupRestoreMenu()); content.addView(backup);
        if (!trialExpired()) { Button back=button("العودة إلى التطبيق"); back.setOnClickListener(v->buildHome()); content.addView(back); }
    }
    private void openWhatsApp(String message) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"+SUPPORT+"?text="+Uri.encode(message)))); }
        catch(Exception e) { toast("تعذر فتح واتساب. الرقم: +249 121 851 285"); }
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private class Store extends SQLiteOpenHelper {
        Store(){super(MainActivity.this,"almizan_bakery_250.db",null,1);}
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE records (day TEXT NOT NULL, module TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(day,module))");
        }
        @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {}
        JSONObject get(String date,String module) {
            SQLiteDatabase db=getReadableDatabase(); Cursor c=db.rawQuery("SELECT payload FROM records WHERE day=? AND module=?",new String[]{date,module});
            try { if(c.moveToFirst()) return new JSONObject(c.getString(0)); } catch(Exception ignored) {} finally {c.close();}
            return new JSONObject();
        }
        void put(String date,String module,JSONObject value) {
            ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module",module); cv.put("payload",value.toString()); cv.put("updated_at",System.currentTimeMillis());
            getWritableDatabase().insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
        }
        void delete(String date,String module) { getWritableDatabase().delete("records","day=? AND module=?",new String[]{date,module}); }
        boolean has(String date) { Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM records WHERE day=? LIMIT 1",new String[]{date}); try{return c.moveToFirst();}finally{c.close();} }

        JSONArray exportAll() throws Exception {
            JSONArray result=new JSONArray(); Cursor c=getReadableDatabase().rawQuery("SELECT day,module,payload,updated_at FROM records ORDER BY day,module",null);
            try {
                while(c.moveToNext()) {
                    JSONObject row=new JSONObject(); row.put("day",c.getString(0)); row.put("module",c.getString(1));
                    row.put("payload",new JSONObject(c.getString(2))); row.put("updated_at",c.getLong(3)); result.put(row);
                }
            } finally { c.close(); }
            return result;
        }
        int restoreAll(JSONArray rows) throws Exception {
            java.util.ArrayList<ContentValues> prepared=new java.util.ArrayList<>();
            java.util.HashSet<String> allowed=new java.util.HashSet<>(java.util.Arrays.asList("stock","cuts","workers","window","distribution","expenses","spoilage","costing","accounting"));
            for(int i=0;i<rows.length();i++) {
                JSONObject row=rows.getJSONObject(i); String date=row.getString("day"), module=row.getString("module");
                if(!date.matches("\\d{4}-\\d{2}-\\d{2}") || !allowed.contains(module)) throw new IllegalArgumentException("Invalid backup record");
                JSONObject payload=row.getJSONObject("payload");
                ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module",module); cv.put("payload",payload.toString());
                cv.put("updated_at",row.optLong("updated_at",System.currentTimeMillis())); prepared.add(cv);
            }
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try { for(ContentValues cv:prepared) db.insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE); db.setTransactionSuccessful(); }
            finally { db.endTransaction(); }
            return prepared.size();
        }

    }
}
