package com.mohammedmustafa.tirbal;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.database.Cursor;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(28, 104, 58);
    private static final int DARK = Color.rgb(18, 65, 39);
    private static final int GOLD = Color.rgb(190, 145, 43);
    private static final int BG = Color.rgb(246, 248, 244);
    private DbHelper db;
    private LinearLayout root;
    private final NumberFormat money = NumberFormat.getIntegerInstance(new Locale("ar", "SD"));

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
            if(config!=null && isMandatoryUpdate(config)) {
                showMandatoryUpdate(config);
            } else continueLicenseCheck();
        }));
    }

    private boolean licenseReady = false;
    private String currentDeviceId = "";
    private final Handler licenseHandler = new Handler(Looper.getMainLooper());
    private final Runnable licenseMonitor = new Runnable() {
        @Override public void run() {
            if (!licenseReady) return;
            if (!LicenseManager.cachedValid(MainActivity.this)) {
                licenseReady = false;
                if (db != null) db.close();
                showSubscription(new LicenseManager.Result(false, true, currentDeviceId, "انتهت صلاحية الترخيص.", 0));
                return;
            }
            licenseHandler.postDelayed(this, 30000);
        }
    };
    @Override protected void onDestroy() {
        licenseHandler.removeCallbacks(licenseMonitor);
        if (db != null) db.close();
        super.onDestroy();
    }

    private void startLicenseMonitor() {
        licenseHandler.removeCallbacks(licenseMonitor);
        licenseHandler.postDelayed(licenseMonitor, 30000);
    }

    private boolean openDatabaseSafely() {
        try {
            if (db != null) db.close();
            db = new DbHelper(this);
            db.migrateLegacyIfNeeded(this);
            return true;
        } catch (Exception e) {
            if (db != null) db.close();
            db = null;
            base("حماية قاعدة البيانات");
            content.addView(text("تعذر فتح قاعدة البيانات المشفرة أو ترحيل البيانات القديمة. لم يحذف التطبيق النسخة القديمة. أعد المحاولة بعد التأكد من توفر مساحة كافية.",15,DARK,true));
            Button retry = button("إعادة المحاولة");
            content.addView(retry);
            retry.setOnClickListener(v -> LicenseManager.check(this, check -> runOnUiThread(() -> {
                currentDeviceId = check.deviceId;
                if (check.unlocked) {
                    if (openDatabaseSafely()) {
                        licenseReady = true;
                        showGate();
                        startLicenseMonitor();
                    }
                } else showSubscription(check);
            })));
            return false;
        }
    }


    private void continueLicenseCheck() {
        LicenseManager.check(this, result -> runOnUiThread(() -> {
            currentDeviceId = result.deviceId;
            if (result.unlocked) {
                if (!openDatabaseSafely()) return;
                licenseReady = true;
                showGate();
                startLicenseMonitor();
            } else showSubscription(result);
        }));
    }

    private int compareVersions(String a,String b) {
        try {
            String[] x=a.split("\\."),y=b.split("\\.");
            for(int i=0;i<3;i++){int n=i<x.length?Integer.parseInt(x[i]):0;int m=i<y.length?Integer.parseInt(y[i]):0;if(n!=m)return Integer.compare(n,m);}
        } catch(Exception ignored) {}
        return 0;
    }

    private boolean isMandatoryUpdate(JSONObject config) {
        String minimum=config.optString("minSupportedVersion","1.0.0");
        return compareVersions(BuildConfig.VERSION_NAME,minimum)<0;
    }

    private void showMandatoryUpdate(JSONObject config) {
        base("تحديث مطلوب");
        content.addView(text("هذا الإصدار لم يعد مدعوماً. حدّث التطبيق لمواصلة استخدام حساب التربال.",17,DARK,true));
        String notes=config.optString("releaseNotes","");
        if(!notes.isEmpty())content.addView(text(notes,14,Color.DKGRAY,false));
        String url=config.optString("downloadUrl","");
        Button update=button("تنزيل الإصدار الجديد");
        content.addView(update);
        update.setEnabled(url.startsWith("https://"));
        update.setOnClickListener(v->{if(url.startsWith("https://"))startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));});
        Button retry=button("إعادة فحص التحديث");
        content.addView(retry);
        retry.setOnClickListener(v->LicenseManager.loadConfig((cfg,error)->runOnUiThread(()->{
            if(cfg!=null&&isMandatoryUpdate(cfg))showMandatoryUpdate(cfg);else continueLicenseCheck();
        })));
    }

    private void showUpdateCenter() {
        base("مركز التحديثات والإعدادات");
        content.addView(text("يتصل هذا المركز بنواة حساب التربال على الموقع لتحميل الأسعار والإعلانات ومعلومات الإصدارات.",15,DARK,false));
        TextView status=text("جارٍ الاتصال بنواة الموقع...",14,Color.DKGRAY,false);
        content.addView(status);
        Button refresh=button("فحص الآن");
        content.addView(refresh);
        refresh.setOnClickListener(v->{
            refresh.setEnabled(false);status.setText("جارٍ فحص الخادم...");
            LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
                refresh.setEnabled(true);
                if(config==null){status.setText(error);return;}
                String current=BuildConfig.VERSION_NAME,latest=config.optString("latestVersion",current);
                String announcement=config.optString("announcement","");
                String notes=config.optString("releaseNotes","");
                StringBuilder info=new StringBuilder();
                info.append("الإصدار المثبت: ").append(current).append("\\nأحدث إصدار: ").append(latest);
                if(compareVersions(current,latest)<0)info.append("\\nيتوفر تحديث جديد.");
                else info.append("\\nالتطبيق على أحدث إصدار معلن.");
                info.append("\\nمدة التجربة: ").append(config.optInt("trialDays",15)).append(" يوماً");
                JSONObject plans=config.optJSONObject("plans");
                if(plans!=null) {
                    JSONObject monthly=plans.optJSONObject("monthly"),annual=plans.optJSONObject("annual"),lifetime=plans.optJSONObject("lifetime");
                    if(monthly!=null&&annual!=null&&lifetime!=null)info.append("\\nالشهري: ").append(monthly.optLong("amount")).append(" ج.س")
                            .append("\\nالسنوي: ").append(annual.optLong("amount")).append(" ج.س")
                            .append("\\nالدائم: ").append(lifetime.optLong("amount")).append(" ج.س");
                }
                if(!announcement.isEmpty())info.append("\\n\\nإعلان الإدارة\\n").append(announcement);
                if(!notes.isEmpty())info.append("\\n\\nملاحظات الإصدار\\n").append(notes);
                status.setText(info.toString());
                String url=config.optString("downloadUrl","");
                if(compareVersions(current,latest)<0&&url.startsWith("https://")){
                    Button download=button("تنزيل التحديث");
                    content.addView(download);
                    download.setOnClickListener(x->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url))));
                }
                if(config.optBoolean("maintenanceMode",false))toast("الموقع أعلن وضع الصيانة. قد تتأثر خدمات الترخيص.");
            }));
        });
        refresh.performClick();
        addBack();
    }

    private void showSubscription(LicenseManager.Result result) {
        base("اشتراك حساب التربال");
        content.addView(text("فترة تجريبية مجانية لمدة 15 يوماً، ثم يلزم اشتراك لمواصلة العمل.",16,DARK,true));
        content.addView(text(result.message,14,Color.DKGRAY,false));
        content.addView(text("الأسعار بالجنيه السوداني",17,GREEN,true));
        TextView pricing=text("جارٍ تحميل الأسعار الحالية...",15,DARK,true);\n        content.addView(pricing);
        TextView payment=text("جارٍ تحميل بيانات التحويل...",14,Color.DKGRAY,false);
        content.addView(payment);
        LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
            if(config==null){payment.setText(error);return;}
            JSONObject p=config.optJSONObject("payment");
            if(p==null){payment.setText("بيانات التحويل غير متاحة حالياً.");return;}
            String details="بنك التحويل: "+p.optString("bankName","غير محدد")+
                    "\nاسم الحساب: "+p.optString("accountName","غير محدد")+
                    "\nرقم الحساب: "+p.optString("accountNumber","لم يُضبط بعد")+
                    "\nواتساب الدعم: "+p.optString("whatsapp","لم يُضبط بعد");
            payment.setText(details);
        }));
        EditText customer=field("اسم صاحب الاشتراك",false);
        EditText contact=field("رقم هاتف العميل",false);
        EditText receipt=field("رقم مرجع التحويل",false);
        Spinner plan=spinner(new String[]{"شهري — 25,000 جنيه","سنوي — 250,000 جنيه","دائم — 500,000 جنيه"});
        content.addView(customer);content.addView(contact);content.addView(plan);content.addView(receipt);
        Button submit=button("إرسال طلب الاشتراك للمراجعة");
        content.addView(submit);
        submit.setOnClickListener(v->{
            if(empty(customer)||empty(contact)||empty(receipt)){toast("أكمل الاسم ورقم الهاتف ومرجع التحويل");return;}
            String code=new String[]{"monthly","annual","lifetime"}[plan.getSelectedItemPosition()];
            submit.setEnabled(false);
            LicenseManager.submitOrder(val(customer),val(contact),code,val(receipt),currentDeviceId,(order,error)->runOnUiThread(()->{
                submit.setEnabled(true);
                if(order==null){toast(error);return;}
                String id=order.optString("orderId");
                String message="طلب اشتراك حساب التربال\nرقم الطلب: "+id+
                        "\nالاسم: "+val(customer)+"\nالخطة: "+plan.getSelectedItem().toString()+
                        "\nمرجع التحويل: "+val(receipt)+"\nالمبلغ: "+order.optLong("amountSdg")+" جنيه سوداني"+
                        "\nيرجى مراجعة التحويل وتفعيل الترخيص بعد التأكد من وصول المبلغ.";
                new AlertDialog.Builder(this).setTitle("تم تسجيل الطلب")
                        .setMessage("رقم الطلب: "+id+"\nحالة الطلب: قيد مراجعة الدفع. لن يفتح التطبيق حتى اعتماد الترخيص.")
                        .setPositiveButton("مشاركة تفاصيل الطلب", (d,w)->{
                            Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");
                            share.putExtra(Intent.EXTRA_TEXT,message);
                            startActivity(Intent.createChooser(share,"إرسال تفاصيل طلب الاشتراك"));
                        }).setNegativeButton("حسناً",null).show();
            }));
        });
        Button refresh=button("تحقق من التفعيل / استعادة الاشتراك");
        content.addView(refresh);
        refresh.setOnClickListener(v->{
            refresh.setEnabled(false);
            LicenseManager.check(this,check->runOnUiThread(()->{
                refresh.setEnabled(true);
                currentDeviceId = check.deviceId;
                if(check.unlocked){if(!openDatabaseSafely())return;licenseReady=true;showGate();startLicenseMonitor();}
                else showSubscription(check);
            }));
        });
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        TextView header = new TextView(this);
        header.setText("🌾  " + title);
        header.setTextColor(Color.WHITE);
        header.setTextSize(21);
        header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.setPadding(22, 18, 22, 18);
        header.setBackgroundColor(GREEN);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(66)));
        ScrollView scroll = new ScrollView(this);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(16, 16, 16, 24);
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        content = body;
    }
    private LinearLayout content;

    private void showGate() {
        base("حساب التربال");
        TextView intro = text("نظام الحسابات الزراعية السوداني", 18, DARK, true);
        content.addView(intro);
        content.addView(text("بياناتك محفوظة على هذا الهاتف. أنشئ كلمة مرور للدخول.", 14, Color.DKGRAY, false));
        EditText pass = field("كلمة المرور", true);
        EditText confirm = field("تأكيد كلمة المرور", true);
        content.addView(pass); content.addView(confirm);
        Button enter = button(PasswordStore.configured(this) ? "دخول" : "إنشاء كلمة المرور");
        content.addView(enter);
        enter.setOnClickListener(v -> {
            String p = pass.getText().toString();
            if (p.length() < 8) { toast("كلمة المرور يجب ألا تقل عن 8 أحرف أو أرقام"); return; }
            if (!PasswordStore.configured(this)) {
                if (!p.equals(confirm.getText().toString())) { toast("كلمتا المرور غير متطابقتين"); return; }
                PasswordStore.set(this, p);
                toast("تم إنشاء كلمة المرور");
                afterLogin();
            } else if (PasswordStore.verify(this, p)) afterLogin();
            else toast("كلمة المرور غير صحيحة");
        });
    }

    private void afterLogin() {
        if (!db.hasProject()) showProjectForm(false);
        else showHome();
    }

    private void showHome() {
        base("حساب التربال");
        content.addView(text(db.projectName(), 23, DARK, true));
        content.addView(text("لوحة المشروع الزراعي  •  الجنيه السوداني", 14, Color.DKGRAY, false));
        content.addView(text("🤝 شراكة مناصفة 50% للتاجر و50% للتربال", 16, GOLD, true));
        long costs = db.total("costs", "total");
        long sales = db.total("sales", "gross");
        long net = db.total("sales", "net");
        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(14, 14, 14, 14);
        stats.setBackgroundColor(Color.WHITE);
        stats.addView(text("إجمالي التكلفة  " + money.format(costs) + " ج.س", 17, DARK, true));
        stats.addView(text("إجمالي المبيعات  " + money.format(sales) + " ج.س", 17, GREEN, true));
        stats.addView(text("صافي نتائج المبيعات  " + money.format(net) + " ج.س", 17, net >= 0 ? GREEN : Color.RED, true));
        content.addView(stats, margin(-1, -2, 0, 12, 0, 14));
        addTile("📁 بيانات المشروع", "اسم المشروع والمحصول والتاجر والمساحة", v -> showProjectForm(true));
        addTile("🧾 دفتر التكلفة", "التقاوي والأسمدة والمبيدات والميز والعمال والمصروفات والسلفيات", v -> showCosts());
        addTile("🛒 السوق والمبيعات", "الفواتير والتاريخ والتكلفة ومنصرفات السوق والصافي", v -> showSales());
        addTile("📅 الرزنامة الزراعية", "مواعيد الزراعة والري والتسميد والرش والحصاد", v -> showCalendar());
        addTile("🌱 مدخلات الزراعة", "تسجيل كميات وأسعار التقاوي والأسمدة والمبيدات في دفتر التكلفة", v -> showCosts());
        addTile("👨‍🌾 العمال والتربال", "تسجيل اسم التربال وربط اسمه ببنود التكلفة", v -> showCosts());
        addTile("📊 الأرباح والخسائر", "ملخص الموسم ونتائج المبيعات والتكاليف", v -> showReports());
        addTile("💳 الديون والسلفيات", "السلفيات تسجل كبند تكلفة قابل للتصنيف", v -> showCosts());
        addTile("🤝 اتفاق الشراكة والتسوية", "مسؤوليات التاجر والتربال والخصومات بعد الحصاد", v -> showPartnership());
        addTile("🔐 تغيير كلمة المرور", "تحديث كلمة مرور التطبيق", v -> changePassword());\n        addTile("🌐 مركز التحديثات والإعدادات", "فحص التحديثات والإعلانات والأسعار الحالية", v -> showUpdateCenter());
    }

    private void addTile(String title, String subtitle, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16, 13, 16, 13);
        card.setBackground(rounded(Color.WHITE, 18, Color.rgb(225,232,222)));
        TextView t = text(title, 18, GREEN, true);
        TextView s = text(subtitle, 13, Color.DKGRAY, false);
        card.addView(t); card.addView(s);
        content.addView(card, margin(-1, -2, 0, 0, 0, 10));
        card.setOnClickListener(click);
    }

    private void showProjectForm(boolean edit) {
        base("بيانات المشروع الزراعي");
        EditText name = field("اسم المشروع *", false);
        EditText crop = field("نوع المحصول *", false);
        EditText trader = field("اسم التاجر *", false);
        EditText owner = field("اسم صاحب المشروع *", false);
        EditText accountant = field("اسم المحاسب *", false);
        EditText area = field("المساحة بالفدان *", false);
        EditText date = field("تاريخ بداية الموسم YYYY-MM-DD *", false);
        date.setText(today());
        EditText region = field("المنطقة الزراعية", false);
        for (EditText e : new EditText[]{name,crop,trader,owner,accountant,area,date,region}) content.addView(e);
        Button save = button("حفظ بيانات المشروع");
        content.addView(save);
        save.setOnClickListener(v -> {
            if (empty(name)||empty(crop)||empty(trader)||empty(owner)||empty(accountant)||empty(area)||empty(date)) {
                toast("أكمل جميع الحقول الإجبارية"); return;
            }
            try {
                db.saveProject(val(name),val(crop),val(trader),val(owner),val(accountant),Double.parseDouble(val(area)),val(date),val(region));
                toast("تم حفظ بيانات المشروع");
                showHome();
            } catch (Exception ex) { toast("تحقق من قيمة المساحة والتاريخ"); }
        });
        addBack();
    }

    private void showCosts() {
        base("دفتر التكلفة");
        content.addView(text("أضف كل عملية في سطر مستقل. الإجمالي يحسب تلقائياً بالكمية × السعر.",14,DARK,false));
        Button add = button("＋ إضافة بند تكلفة");
        content.addView(add);
        add.setOnClickListener(v -> costDialog());
        tableHeader(new String[]{"التاريخ","اسم التربال","التصنيف","الصنف","الكمية","السعر","الإجمالي","الدافع","المسؤول","التسوية","الخصم من"});
        try (Cursor c = db.recent("costs","entry_date")) {
            while (c.moveToNext()) {
                tableRow(new String[]{c.getString(c.getColumnIndexOrThrow("entry_date")),c.getString(c.getColumnIndexOrThrow("worker")),c.getString(c.getColumnIndexOrThrow("category")),c.getString(c.getColumnIndexOrThrow("item")),c.getString(c.getColumnIndexOrThrow("quantity")),money.format(c.getLong(c.getColumnIndexOrThrow("unit_price"))),money.format(c.getLong(c.getColumnIndexOrThrow("total"))),c.getString(c.getColumnIndexOrThrow("payer")),c.getString(c.getColumnIndexOrThrow("responsible_party")),c.getString(c.getColumnIndexOrThrow("settlement_type")),c.getString(c.getColumnIndexOrThrow("deduction_from"))});
            }
        }
        content.addView(text("إجمالي التكلفة: " + money.format(db.total("costs","total")) + " جنيه سوداني",18,GREEN,true));
        addBack();
    }

    private void costDialog() {
        LinearLayout form = form();
        EditText date = field("التاريخ YYYY-MM-DD",false); date.setText(today());
        EditText worker = field("اسم التربال",false);
        Spinner category = spinner(new String[]{"تحضير الأرض","التقاوي","الأسمدة","المبيدات","الميز","الخدمة داخل الزراعة","الري وخدمة الحقل","أجور العمال","المنصرفات","السلفيات","ضريبة المياه","إيجار الأرض","الوقود والترحيل","أخرى"});
        EditText item = field("الصنف / البيان",false);
        EditText qty = field("الكمية",false); qty.setInputType(8194);
        EditText price = field("السعر بالجنيه السوداني",false); price.setInputType(2);
        Spinner responsible = spinner(new String[]{"التاجر","التربال","مشترك","حسب الاتفاق"});
        Spinner payer = spinner(new String[]{"التاجر","التربال","طرف ثالث"});
        Spinner settlement = spinner(new String[]{"مناصفة بين الطرفين","يخصم كاملاً من نصيب التربال","على التاجر وحده","حسب اتفاق إضافي"});
        Spinner deduction = spinner(new String[]{"مناصفة","من نصيب التربال","لا يوجد خصم","حسب الاتفاق"});
        EditText agreement = field("تفاصيل الاتفاق الخاص بهذا البند",false);
        EditText notes = field("ملاحظات",false);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String[] defaults = db.defaultResponsibility(category.getSelectedItem().toString());
                setSpinnerValue(responsible, defaults[0]);
                setSpinnerValue(payer, defaults[1]);
                String settlementLabel = "shared_half".equals(defaults[2]) ? "مناصفة بين الطرفين" :
                    ("deduct_worker".equals(defaults[2]) ? "يخصم كاملاً من نصيب التربال" :
                    ("trader_only".equals(defaults[2]) ? "على التاجر وحده" : "حسب اتفاق إضافي"));
                setSpinnerValue(settlement, settlementLabel);
                setSpinnerValue(deduction, defaults[3]);
            }
        });
        form.addView(date); form.addView(worker); form.addView(category); form.addView(item); form.addView(qty); form.addView(price);
        form.addView(text("المسؤول حسب الاتفاق",14,DARK,true)); form.addView(responsible);
        form.addView(text("من دفع فعلياً؟",14,DARK,true)); form.addView(payer);
        form.addView(text("طريقة التسوية",14,DARK,true)); form.addView(settlement);
        form.addView(text("الخصم من أي نصيب؟",14,DARK,true)); form.addView(deduction);
        form.addView(agreement); form.addView(notes);
        new AlertDialog.Builder(this).setTitle("إضافة بند تكلفة").setView(form)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
                try {
                    if(empty(date)||empty(item)||empty(qty)||empty(price)) { toast("أكمل التاريخ والصنف والكمية والسعر"); return; }
                    double q=Double.parseDouble(val(qty)); long p=Long.parseLong(val(price));
                    if(q<=0||p<0) throw new NumberFormatException();
                    String settlementCode = new String[]{"shared_half","deduct_worker","trader_only","agreement_pending"}[settlement.getSelectedItemPosition()];
                    db.addCost(val(date),val(worker),category.getSelectedItem().toString(),val(item),q,p,payer.getSelectedItem().toString(),responsible.getSelectedItem().toString(),settlementCode,deduction.getSelectedItem().toString(),val(agreement),val(notes));
                    showCosts();
                } catch(Exception e){toast("تحقق من الكمية والسعر والتاريخ");}
            }).show();
    }

    private void showSales() {
        base("السوق والمبيعات");
        content.addView(text("الصافي = قيمة البيع − التكلفة المخصصة − منصرفات السوق",14,DARK,false));
        Button add=button("＋ تسجيل فاتورة بيع"); content.addView(add); add.setOnClickListener(v->saleDialog());
        tableHeader(new String[]{"الفاتورة","التاريخ","قيمة البيع","التكلفة","منصرفات السوق","الصافي"});
        try(Cursor c=db.recent("sales","sale_date")) {
            while(c.moveToNext()) {
                String invoice=c.getString(c.getColumnIndexOrThrow("invoice"));
                String date=c.getString(c.getColumnIndexOrThrow("sale_date"));
                long gross=c.getLong(c.getColumnIndexOrThrow("gross"));
                long allocated=c.getLong(c.getColumnIndexOrThrow("allocated_cost"));
                long expenses=c.getLong(c.getColumnIndexOrThrow("market_expenses"));
                long net=c.getLong(c.getColumnIndexOrThrow("net"));
                String trader=c.getString(c.getColumnIndexOrThrow("trader"));
                tableRow(new String[]{invoice,date,money.format(gross),money.format(allocated),money.format(expenses),money.format(net)});
                Button print=button("طباعة الفاتورة رقم "+invoice);
                content.addView(print);
                print.setOnClickListener(v->InvoicePrinter.print(this,invoice,date,gross,allocated,expenses,net,trader,db.projectName(),money));
            }
        }
        content.addView(text("إجمالي المبيعات: "+money.format(db.total("sales","gross"))+" جنيه سوداني",18,GREEN,true));
        addBack();
    }

    private void saleDialog() {
        LinearLayout form=form();
        EditText invoice=field("رقم الفاتورة",false);
        EditText date=field("التاريخ YYYY-MM-DD",false); date.setText(today());
        EditText gross=field("قيمة البيع بالجنيه",false); gross.setInputType(2);
        EditText cost=field("التكلفة المخصصة للمباع",false); cost.setInputType(2);
        EditText expenses=field("منصرفات السوق",false); expenses.setInputType(2);
        EditText trader=field("اسم التاجر",false);
        EditText notes=field("ملاحظات",false);
        for(EditText e:new EditText[]{invoice,date,gross,cost,expenses,trader,notes}) form.addView(e);
        new AlertDialog.Builder(this).setTitle("فاتورة بيع").setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
            try {
                if(empty(invoice)||empty(date)||empty(gross)) {toast("أدخل رقم الفاتورة والتاريخ وقيمة البيع");return;}
                long g=Long.parseLong(val(gross)), c=empty(cost)?0:Long.parseLong(val(cost)), x=empty(expenses)?0:Long.parseLong(val(expenses));
                if(g<0||c<0||x<0) throw new NumberFormatException();
                db.addSale(val(invoice),val(date),g,c,x,val(trader),val(notes)); showSales();
            } catch(Exception e){toast("تحقق من المبالغ المدخلة");}
        }).show();
    }

    private void showCalendar() {
        base("الرزنامة الزراعية");
        content.addView(text("سجل مواعيد الموسم حسب منطقتك ونوع المحصول. المواعيد لا تُفرض تلقائياً لاختلاف المواسم.",14,DARK,false));
        Button add=button("＋ إضافة موعد زراعي"); content.addView(add); add.setOnClickListener(v->{
            LinearLayout f=form();
            EditText date=field("تاريخ الموعد YYYY-MM-DD",false); date.setText(today());
            EditText title=field("العملية الزراعية",false);
            Spinner kind=spinner(new String[]{"تجهيز الأرض","الزراعة","الري","التسميد","الرش","إزالة الحشائش","المتابعة","الحصاد","البيع","موعد آخر"});
            EditText details=field("تفاصيل وملاحظات",false);
            f.addView(date); f.addView(kind); f.addView(title); f.addView(details);
            new AlertDialog.Builder(this).setTitle("موعد في الرزنامة").setView(f).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(a,b)->{
                if(empty(date)) {toast("أدخل التاريخ");return;}
                db.addTask(val(date),empty(title)?kind.getSelectedItem().toString():val(title),val(details)); showCalendar();
            }).show();
        });
        tableHeader(new String[]{"التاريخ","العملية","التفاصيل"});
        try(Cursor c=db.recent("calendar_tasks","task_date")) {
            while(c.moveToNext()) tableRow(new String[]{c.getString(c.getColumnIndexOrThrow("task_date")),c.getString(c.getColumnIndexOrThrow("title")),c.getString(c.getColumnIndexOrThrow("details"))});
        }
        addBack();
    }

    private void showPartnership() {
        base("اتفاق الشراكة والتسوية");
        content.addView(text("🤝 اتفاق الموسم: التاجر يمول المشروع حتى الحصاد، والتوزيع الأساسي مناصفة 50% لكل طرف.",16,DARK,true));
        content.addView(text("🌱 تكاليف مشتركة مناصفة",17,GREEN,true));
        content.addView(text("تحضير الأرض • التقاوي • الأسمدة • المبيدات",15,Color.DKGRAY,false));
        content.addView(text("👨‍🌾 التزامات التربال",17,GREEN,true));
        content.addView(text("تنفيذ الزراعة والنظافة والري وكل أعمال الخدمة داخل الحقل. الميز والمنصرفات والسلفيات وأجور الخدمة التي يدفعها التاجر تخصم كاملة من نصيب التربال.",14,Color.DKGRAY,false));
        content.addView(text("🏦 التزامات التاجر وحده",17,GREEN,true));
        content.addView(text("إيجار الأرض وضريبة المياه. لا تدخل هذه البنود في المناصفة ولا تخصم من نصيب التربال.",14,Color.DKGRAY,false));
        content.addView(text("📌 ملخص البنود المسجلة",17,GREEN,true));
        long sharedTotal = db.totalBySettlement("shared_half");
        reportLine("إجمالي البنود المشتركة المسجلة",sharedTotal);
        reportLine("حصة التاجر من البنود المشتركة (50%)",sharedTotal / 2);
        reportLine("حصة التربال من البنود المشتركة (50%)",sharedTotal - (sharedTotal / 2));
        reportLine("إجمالي ما يخصم من نصيب التربال",db.totalByDeduction("من نصيب التربال"));
        reportLine("إجمالي ما يتحمله التاجر وحده",db.totalBySettlement("trader_only"));
        tableHeader(new String[]{"البند","المسؤول","الدافع الافتراضي","قاعدة التسوية"});
        try (Cursor c = db.partnershipTerms()) {
            while (c.moveToNext()) {
                String type=c.getString(c.getColumnIndexOrThrow("settlement_type"));
                String label="حسب الاتفاق";
                if("shared_half".equals(type)) label="مناصفة 50/50";
                else if("deduct_worker".equals(type)) label="خصم من نصيب التربال";
                else if("trader_only".equals(type)) label="على التاجر وحده";
                tableRow(new String[]{c.getString(c.getColumnIndexOrThrow("category")),c.getString(c.getColumnIndexOrThrow("responsible_party")),c.getString(c.getColumnIndexOrThrow("default_payer")),label});
            }
        }
        content.addView(text("لا يتم خصم أي مبلغ من نصيب التربال إلا إذا سُجل ضمن بند واضح في الاتفاق. هذه أرقام البنود المسجلة وليست تسوية حصاد نهائية.",13,Color.DKGRAY,false));
        addBack();
    }
    private void setSpinnerValue(Spinner spinner, String value) {
        android.widget.SpinnerAdapter adapter = spinner.getAdapter();
        if (adapter == null) return;
        for (int i=0; i<adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i).toString())) { spinner.setSelection(i); return; }
        }
        if (value.equals("مشترك") || value.equals("حسب الاتفاق")) spinner.setSelection(adapter.getCount()-1);
    }

    private void showReports() {
        base("الأرباح والخسائر");
        long costs=db.total("costs","total");
        long gross=db.total("sales","gross");
        long allocated=db.total("sales","allocated_cost");
        long expenses=db.total("sales","market_expenses");
        long salesNet=db.total("sales","net");
        long result=gross-costs-expenses;
        reportLine("إجمالي تكلفة المدخلات والمصروفات",costs);
        reportLine("إجمالي قيمة المبيعات",gross);
        reportLine("التكلفة المخصصة للمحصول المباع",allocated);
        reportLine("منصرفات السوق",expenses);
        reportLine("صافي المبيعات بعد تكلفة المباع",salesNet);
        content.addView(text("النتيجة التقديرية للمشروع = المبيعات − إجمالي التكلفة المسجلة − منصرفات السوق",14,Color.DKGRAY,false));
        content.addView(text((result>=0?"فائض مبدئي: ":"عجز مبدئي: ")+money.format(Math.abs(result))+" جنيه سوداني",21,result>=0?GREEN:Color.RED,true));
        content.addView(text("ملاحظة محاسبية: هذا ملخص نقدي أولي. يجب عدم جمع التكلفة المخصصة للمباع مرة ثانية مع إجمالي التكاليف عند إعداد قائمة ربح وخسارة نهائية. ستُستكمل معالجة تكلفة المخزون والمحصول غير المباع في المرحلة التالية.",13,Color.DKGRAY,false));
        addBack();
    }
    private void reportLine(String label,long amount) { content.addView(text(label+" : "+money.format(amount)+" ج.س",17,DARK,true)); }

    private void changePassword() {
        LinearLayout f=form();
        EditText old=field("كلمة المرور الحالية",true), next=field("كلمة المرور الجديدة (8 أحرف على الأقل)",true), confirm=field("تأكيد كلمة المرور الجديدة",true);
        f.addView(old); f.addView(next); f.addView(confirm);
        new AlertDialog.Builder(this).setTitle("تغيير كلمة المرور").setView(f).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
            String n=val(next);
            if(!PasswordStore.verify(this,val(old))) {toast("كلمة المرور الحالية غير صحيحة");return;}
            if(n.length()<8||!n.equals(val(confirm))) {toast("تحقق من كلمة المرور الجديدة والتأكيد");return;}
            PasswordStore.set(this,n); toast("تم تغيير كلمة المرور");
        }).show();
    }

    private void tableHeader(String[] cells) { tableRow(cells,true); }
    private void tableRow(String[] cells) { tableRow(cells,false); }
    private void tableRow(String[] cells,boolean header) {
        HorizontalScrollView hs=new HorizontalScrollView(this);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(String cell:cells) {
            TextView t=text(cell,header?13:12,header?Color.WHITE:Color.BLACK,header);
            t.setGravity(Gravity.CENTER); t.setPadding(12,12,12,12);
            t.setMinWidth(dp(112)); t.setBackgroundColor(header?GREEN:Color.WHITE);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(112),-2);
            lp.setMargins(1,1,1,1); row.addView(t,lp);
        }
        hs.addView(row); content.addView(hs);
    }

    private void addBack() { Button b=button("⌂  العودة إلى الرئيسية"); content.addView(b); b.setOnClickListener(v->showHome()); }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }
    private LinearLayout form() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(8,8,8,8); return l; }
    private Spinner spinner(String[] items) {
        Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,items);
        s.setAdapter(a); return s;
    }
    private EditText field(String hint,boolean password) {
        EditText e=new EditText(this); e.setSingleLine(true); e.setTextSize(15); e.setHint(hint); e.setPadding(12,8,12,8);
        if(password) e.setInputType(129);
        e.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(52)); p.setMargins(0,0,0,8); e.setLayoutParams(p);
        return e;
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.RIGHT); t.setPadding(4,7,4,7);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private Button button(String label) {
        Button b=new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN));
        b.setAllCaps(false); b.setTextSize(15); return b;
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
    private boolean empty(EditText e) { return val(e).trim().isEmpty(); }
    private String val(EditText e) { return e.getText().toString().trim(); }
    private String today() { return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); }
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h); p.setMargins(l,t,r,b); return p;
    }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
}
