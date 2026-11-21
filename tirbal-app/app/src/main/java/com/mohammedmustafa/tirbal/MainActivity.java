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
import java.util.ArrayList;
import java.util.List;
import android.text.TextWatcher;
import android.text.Editable;

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
        // Review-first startup: the app remains usable while the licensing service is being finalized.
        // The website nucleus can turn enforcement on later through /api/config without rebuilding the APK.
        LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
            if(config!=null && isMandatoryUpdate(config)) {
                showMandatoryUpdate(config);
                return;
            }
            if(config!=null && BuildConfig.REMOTE_LICENSE_CONTROL
                    && config.optBoolean("licenseEnforcement",false)) {
                continueLicenseCheck();
            } else {
                openReviewSession();
            }
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
            AccountingBook.backfillLegacy(db.getWritableDatabase());
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


    private void openReviewSession() {
        currentDeviceId = "";
        licenseReady = false;
        licenseHandler.removeCallbacks(licenseMonitor);
        if (!openDatabaseSafely()) return;
        showGate();
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
        return config.optString("downloadUrl","").startsWith("https://") && compareVersions(BuildConfig.VERSION_NAME,minimum)<0;
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
                info.append("\\nحالة الترخيص المركزي: ")
                    .append(config.optBoolean("licenseEnforcement",false) ? "مفعّل" : "متوقف أثناء المراجعة");
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
        content.addView(text("فترة تجريبية مجانية، ثم يلزم اشتراك لمواصلة العمل.",16,DARK,true));
        content.addView(text(result.message,14,Color.DKGRAY,false));
        content.addView(text("الأسعار بالجنيه السوداني",17,GREEN,true));
        TextView pricing=text("جارٍ تحميل الأسعار الحالية...",15,DARK,true);
        content.addView(pricing);
        Spinner plan=spinner(new String[]{"جارٍ تحميل الخطة الشهرية...","جارٍ تحميل الخطة السنوية...","جارٍ تحميل الخطة الدائمة..."});
        TextView payment=text("جارٍ تحميل بيانات التحويل...",14,Color.DKGRAY,false);
        content.addView(payment);
        Button whatsapp=button("التواصل مع الدعم عبر واتساب");
        whatsapp.setEnabled(false);
        content.addView(whatsapp);
        LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
            if(config==null){payment.setText(error);return;}
            JSONObject p=config.optJSONObject("payment");
            if(p==null){payment.setText("بيانات التحويل غير متاحة حالياً.");return;}
            String whatsappNumber=p.optString("whatsapp","");
            String details="بنك التحويل: "+p.optString("bankName","غير محدد")+
                    "\nاسم الحساب: "+p.optString("accountName","غير محدد")+
                    "\nرقم الحساب: "+p.optString("accountNumber","لم يُضبط بعد")+
                    "\nواتساب الدعم: "+(whatsappNumber.isEmpty()?"لم يُضبط بعد":whatsappNumber);
            payment.setText(details);
            String digits=whatsappNumber.replaceAll("[^0-9]","");
            if(!digits.isEmpty()){
                whatsapp.setEnabled(true);
                whatsapp.setOnClickListener(v->{
                    String message="مرحباً، أحتاج مساعدة بخصوص ترخيص تطبيق حساب التربال.";
                    Intent intent=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://wa.me/"+digits+"?text="+android.net.Uri.encode(message)));
                    try{startActivity(intent);}catch(Exception ex){toast("تعذر فتح واتساب أو المتصفح.");}
                });
            }
            JSONObject plans=config.optJSONObject("plans");
            if(plans!=null) {
                JSONObject monthly=plans.optJSONObject("monthly"),annual=plans.optJSONObject("annual"),lifetime=plans.optJSONObject("lifetime");
                if(monthly!=null&&annual!=null&&lifetime!=null) {
                    long m=monthly.optLong("amount"),a=annual.optLong("amount"),l=lifetime.optLong("amount");
                    pricing.setText("شهري "+money.format(m)+" ج.س  •  سنوي "+money.format(a)+" ج.س  •  دائم "+money.format(l)+" ج.س");
                    plan.setAdapter(new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,
                            new String[]{"شهري — "+money.format(m)+" جنيه","سنوي — "+money.format(a)+" جنيه","دائم — "+money.format(l)+" جنيه"}));
                }
            }
        }));
        EditText customer=field("اسم صاحب الاشتراك",false);
        EditText contact=field("رقم هاتف العميل",false);
        EditText receipt=field("رقم مرجع التحويل",false);

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
        content.addView(text("نظام الحسابات الزراعية السوداني", 18, DARK, true));
        boolean firstSetup = !PasswordStore.configured(this) && !db.hasProject();
        if (firstSetup) {
            content.addView(text("التسجيل لأول مرة", 15, Color.DKGRAY, false));
            EditText trader = field("اسم التاجر", false);
            EditText crop = field("نوع المحصول", false);
            EditText pass = field("ادخل كلمة السر", true);
            EditText confirm = field("اعد كلمة السر", true);
            content.addView(trader); content.addView(crop); content.addView(pass); content.addView(confirm);
            Button create = button("إنشاء الحساب");
            content.addView(create);
            create.setOnClickListener(v -> {
                String password = pass.getText().toString();
                if (empty(trader) || empty(crop)) { toast("أدخل اسم التاجر ونوع المحصول"); return; }
                if (password.length() < 8) { toast("كلمة السر يجب ألا تقل عن 8 أحرف"); return; }
                if (!password.equals(confirm.getText().toString())) { toast("كلمتا السر غير متطابقتين"); return; }
                try {
                    PasswordStore.set(this, password);
                    String traderName = val(trader);
                    db.saveProject(traderName, val(crop), traderName, traderName, "غير محدد", 0, today(), "");
                    toast("تم إنشاء الحساب وحفظ بيانات المشروع");
                    showHome();
                } catch (Exception ex) { toast("تعذر إنشاء الحساب. أعد المحاولة"); }
            });
            return;
        }
        content.addView(text("بياناتك محفوظة على هذا الهاتف. أدخل كلمة المرور للدخول.", 14, Color.DKGRAY, false));
        EditText pass = field("كلمة المرور", true);
        content.addView(pass);
        Button enter = button("دخول");
        content.addView(enter);
        enter.setOnClickListener(v -> {
            if (PasswordStore.verify(this, pass.getText().toString())) afterLogin();
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
        addTile("🌱 مدخلات الزراعة", "إدخال سريع متعدد البنود والكميات والأسعار", v -> showAgricultureInputs());
        addTile("👨‍🌾 العمال والتربال", "تسجيل اسم التربال وربط اسمه ببنود التكلفة", v -> showCosts());
        addTile("🤝 تسوية نصيب التربال", "حساب صافي الفواتير والمناصفة والخصومات لكل تربال", v -> showSettlement());
        addTile("📊 الأرباح والخسائر", "ملخص الموسم ونتائج المبيعات والتكاليف", v -> showReports());
        addTile("📚 دفتر اليومية والأستاذ", "قيود محاسبية مزدوجة وسجل العمليات المرحلة", v -> showJournal());
        addTile("⚖️ ميزان المراجعة", "أرصدة الحسابات ومطابقة إجمالي المدين والدائن", v -> showTrialBalance());
        addTile("💳 الديون والسلفيات", "السلفيات تسجل كبند تكلفة قابل للتصنيف", v -> showCosts());
        addTile("🤝 اتفاق الشراكة والتسوية", "مسؤوليات التاجر والتربال والخصومات بعد الحصاد", v -> showPartnership());
        addTile("🔐 تغيير كلمة المرور", "تحديث كلمة مرور التطبيق", v -> changePassword());
        addTile("🌐 مركز التحديثات والإعدادات", "فحص التحديثات والإعلانات والأسعار الحالية", v -> showUpdateCenter());
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
        EditText trader = field("اسم التاجر", false);
        EditText crop = field("نوع المحصول", false);
        content.addView(trader); content.addView(crop);
        Button save = button("حفظ البيانات");
        content.addView(save);
        save.setOnClickListener(v -> {
            if (empty(trader) || empty(crop)) { toast("أدخل اسم التاجر ونوع المحصول"); return; }
            String traderName = val(trader);
            db.saveProject(traderName, val(crop), traderName, traderName, "غير محدد", 0, today(), "");
            toast("تم حفظ بيانات المشروع");
            showHome();
        });
        addBack();
    }

    private static final String[] QUICK_CATEGORIES = {"الميز","الأسمدة","المبيدات","التقاوي","تحضير الأرض","الديون"};
    private static final String[] SHARED_CATEGORIES = {"الأسمدة","المبيدات","التقاوي","تحضير الأرض","الديون"};
    private static final String[] WORKER_DEDUCTIONS = {"الميز","السلفيات","الخدمة داخل الزراعة","الري وخدمة الحقل","أجور العمال","المنصرفات"};

    private class CostRow {
        EditText worker;
        EditText[] amounts = new EditText[QUICK_CATEGORIES.length];
        TextView total;
    }

    private EditText cellInput(String hint, boolean numeric, int width) {
        EditText e=new EditText(this); e.setSingleLine(true); e.setTextSize(13); e.setHint(hint);
        e.setGravity(Gravity.CENTER); e.setPadding(6,4,6,4); e.setBackground(rounded(Color.WHITE,6,Color.rgb(210,220,210)));
        if(numeric)e.setInputType(2);
        e.setLayoutParams(new LinearLayout.LayoutParams(dp(width),dp(48)));
        return e;
    }

    private TextView cellLabel(String value,int width) {
        TextView t=text(value,12,DARK,true);t.setGravity(Gravity.CENTER);t.setBackground(rounded(Color.rgb(239,244,238),6,Color.rgb(210,220,210)));
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(width),dp(48)));return t;
    }

    private void showCosts() {
        base("دفتر تكلفة التربال");
        content.addView(text("أدخل كل تربال في صف واحد. اكتب قيمة كل بند مباشرة، وسيحسب التطبيق جملة الصف تلقائياً.",14,DARK,false));
        content.addView(text("الميز والسلفيات والخدمات المسجلة على التربال تخصم من نصيبه. التحضير والتقاوي والسماد والمبيد والديون المشتركة تدخل في التكلفة المشتركة.",13,Color.DKGRAY,false));
        LinearLayout table=new LinearLayout(this);table.setOrientation(LinearLayout.VERTICAL);
        content.addView(table);
        List<CostRow> rows=new ArrayList<>();
        Runnable addRow=()->addQuickCostRow(table,rows);
        addQuickCostHeader(table);
        Button add=button("＋ إضافة تربال");
        content.addView(add);add.setOnClickListener(v->addRow.run());
        addRow.run();
        EditText date=field("تاريخ دفتر التكلفة YYYY-MM-DD",false);date.setText(today());content.addView(date);
        Button save=button("حفظ جميع صفوف التكلفة");
        content.addView(save);
        save.setOnClickListener(v->{
            if(empty(date)){toast("أدخل تاريخ التكلفة");return;}
            int saved=0;
            try {
                for(CostRow row:rows) {
                    String worker=val(row.worker);
                    if(worker.isEmpty())continue;
                    for(int i=0;i<QUICK_CATEGORIES.length;i++) {
                        String raw=val(row.amounts[i]);
                        if(raw.isEmpty())continue;
                        long amount=Long.parseLong(raw);
                        if(amount<0)throw new NumberFormatException();
                        if(amount==0)continue;
                        String category=QUICK_CATEGORIES[i];
                        String[] defaults=db.defaultResponsibility(category);
                        db.addCost(val(date),worker,category,category,1,amount,"التاجر",defaults[0],defaults[2],defaults[3],"","إدخال سريع من دفتر التكلفة");
                        saved++;
                    }
                }
                if(saved==0){toast("أدخل اسم تربال ومبلغاً واحداً على الأقل");return;}
                toast("تم حفظ "+saved+" بند تكلفة");
                showCosts();
            }catch(Exception ex){toast("تعذر الحفظ. راجع المبالغ وتأكد أنها أرقام صحيحة.");}
        });
        content.addView(text("السجلات المحفوظة",17,GREEN,true));
        tableHeader(new String[]{"التاريخ","التربال","البند","المبلغ"});
        try(Cursor c=db.recent("costs","entry_date")) {
            while(c.moveToNext())tableRow(new String[]{c.getString(c.getColumnIndexOrThrow("entry_date")),c.getString(c.getColumnIndexOrThrow("worker")),c.getString(c.getColumnIndexOrThrow("category")),money.format(c.getLong(c.getColumnIndexOrThrow("total")))});
        }
        reportLine("إجمالي التكلفة المسجلة",db.total("costs","total"));
        Button detailed=button("إدخال تفصيلي للمدخلات الزراعية");content.addView(detailed);detailed.setOnClickListener(v->showAgricultureInputs());
        Button settlement=button("حساب التسوية لكل تربال");content.addView(settlement);settlement.setOnClickListener(v->showSettlement());
        addBack();
    }

    private void addQuickCostHeader(LinearLayout table) {
        HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels={"اسم التربال","الميز","السماد","المبيد","التقاوي","التحضير","الديون","الجملة"};
        int[] widths={142,92,92,92,92,92,92,105};
        for(int i=0;i<labels.length;i++){
            TextView t=text(labels[i],12,Color.WHITE,true);t.setGravity(Gravity.CENTER);t.setBackgroundColor(GREEN);
            row.addView(t,new LinearLayout.LayoutParams(dp(widths[i]),dp(46)));
        }
        hs.addView(row);table.addView(hs);
    }

    private void addQuickCostRow(LinearLayout table,List<CostRow> rows) {
        CostRow item=new CostRow();item.worker=cellInput("اسم التربال",false,142);
        HorizontalScrollView hs=new HorizontalScrollView(this);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(item.worker);
        for(int i=0;i<item.amounts.length;i++){
            item.amounts[i]=cellInput("0",true,92);row.addView(item.amounts[i]);
        }
        item.total=cellLabel("0",105);row.addView(item.total);hs.addView(row);table.addView(hs);
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int count,int after){}public void onTextChanged(CharSequence x,int st,int before,int count){
            long total=0;for(EditText e:item.amounts){try{String v=val(e);if(!v.isEmpty())total+=Long.parseLong(v);}catch(Exception ignored){}}
            item.total.setText(money.format(total));
        }public void afterTextChanged(Editable e){}};
        for(EditText e:item.amounts)e.addTextChangedListener(watcher);
        rows.add(item);
    }

    private class AgricultureRow {
        Spinner category;
        EditText worker,item,quantity,price;
        TextView total;
    }

    private void showAgricultureInputs() {
        base("مدخلات الزراعة");
        content.addView(text("أضف كل صنف كسطر مستقل. يمكنك إضافة أسمدة وتقاوي ومبيدات وتحضير وأي بند آخر، مع حساب الكمية × سعر الوحدة تلقائياً.",14,DARK,false));
        EditText date=field("تاريخ الإدخال YYYY-MM-DD",false);date.setText(today());content.addView(date);
        LinearLayout rowsBox=new LinearLayout(this);rowsBox.setOrientation(LinearLayout.VERTICAL);content.addView(rowsBox);
        List<AgricultureRow> rows=new ArrayList<>();
        Button add=button("＋ إضافة صنف زراعي");content.addView(add);add.setOnClickListener(v->addAgricultureRow(rowsBox,rows));
        add.performClick();
        Button save=button("حفظ جميع المدخلات");content.addView(save);
        save.setOnClickListener(v->{
            if(empty(date)){toast("أدخل التاريخ");return;}
            int count=0;
            try{
                for(AgricultureRow r:rows){
                    if(empty(r.worker)&&empty(r.item)&&empty(r.quantity)&&empty(r.price))continue;
                    if(empty(r.worker)||empty(r.item)||empty(r.quantity)||empty(r.price))throw new IllegalArgumentException();
                    double q=Double.parseDouble(val(r.quantity));long p=Long.parseLong(val(r.price));
                    if(q<=0||p<0)throw new IllegalArgumentException();
                    String category=r.category.getSelectedItem().toString();
                    String[] defaults=db.defaultResponsibility(category);
                    db.addCost(val(date),val(r.worker),category,val(r.item),q,p,"التاجر",defaults[0],defaults[2],defaults[3],"","مدخل زراعي تفصيلي");
                    count++;
                }
                if(count==0){toast("أدخل صنفاً واحداً على الأقل");return;}
                toast("تم حفظ "+count+" مدخل زراعي");
                showAgricultureInputs();
            }catch(Exception ex){toast("أكمل اسم الصنف والكمية والسعر وتأكد من صحة الأرقام");}
        });
        addBack();
    }

    private void addAgricultureRow(LinearLayout parent,List<AgricultureRow> rows) {
        AgricultureRow r=new AgricultureRow();LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(10,8,10,8);card.setBackground(rounded(Color.WHITE,12,Color.rgb(218,228,216)));
        r.category=spinner(new String[]{"التقاوي","الأسمدة","المبيدات","تحضير الأرض","الري وخدمة الحقل","الوقود والترحيل","أجور العمال","أخرى"});
        r.worker=field("اسم التربال",false);r.item=field("اسم الصنف / البيان",false);r.quantity=field("الكمية",false);r.quantity.setInputType(8194);
        r.price=field("سعر الوحدة بالجنيه",false);r.price.setInputType(2);r.total=text("الإجمالي: 0 ج.س",15,GREEN,true);
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int count,int after){}public void onTextChanged(CharSequence x,int st,int before,int count){
            try{double q=empty(r.quantity)?0:Double.parseDouble(val(r.quantity));long p=empty(r.price)?0:Long.parseLong(val(r.price));r.total.setText("الإجمالي: "+money.format(Math.round(q*p))+" ج.س");}catch(Exception ex){r.total.setText("تحقق من الكمية والسعر");}
        }public void afterTextChanged(Editable e){}};
        r.quantity.addTextChangedListener(watcher);r.price.addTextChangedListener(watcher);
        card.addView(text("بند زراعي جديد",14,DARK,true));card.addView(r.category);card.addView(r.worker);card.addView(r.item);card.addView(r.quantity);card.addView(r.price);card.addView(r.total);
        parent.addView(card,margin(-1,-2,0,0,0,10));rows.add(r);
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
        base("السوق وفواتير التربال");
        content.addView(text("سجل كل فاتورة مستقلة. أدخل اسم التربال وتاريخ الفاتورة ورقمها وتكلفة الرحلة وقيمة البيع. الصافي يحسب تلقائياً.",14,DARK,false));
        Button add=button("＋ إضافة فاتورة جديدة");content.addView(add);add.setOnClickListener(v->saleDialog());
        tableHeader(new String[]{"اسم التربال","التاريخ","رقم الفاتورة","قيمة البيع","تكلفة الرحلة","الصافي"});
        try(Cursor c=db.recent("sales","sale_date")){
            while(c.moveToNext()){
                String invoice=c.getString(c.getColumnIndexOrThrow("invoice")),date=c.getString(c.getColumnIndexOrThrow("sale_date"));
                long gross=c.getLong(c.getColumnIndexOrThrow("gross")),trip=c.getLong(c.getColumnIndexOrThrow("market_expenses")),net=c.getLong(c.getColumnIndexOrThrow("net"));
                String worker=c.getString(c.getColumnIndexOrThrow("trader"));
                tableRow(new String[]{worker,date,invoice,money.format(gross),money.format(trip),money.format(net)});
                Button print=button("طباعة الفاتورة "+invoice);content.addView(print);
                print.setOnClickListener(v->InvoicePrinter.print(this,invoice,date,gross,0,trip,net,worker,db.projectName(),money));
            }
        }
        reportLine("إجمالي صافي الفواتير",db.totalSalesNet());
        content.addView(text("يمكن تسجيل عدد غير محدود من الفواتير للتربال نفسه. تجمع الفواتير حسب الاسم في شاشة التسوية.",13,Color.DKGRAY,false));
        Button settlement=button("عرض تسوية التربال");content.addView(settlement);settlement.setOnClickListener(v->showSettlement());
        addBack();
    }

    private void saleDialog() {
        LinearLayout f=form();
        EditText worker=field("اسم التربال",false);
        EditText date=field("تاريخ الفاتورة YYYY-MM-DD",false);date.setText(today());
        EditText invoice=field("رقم الفاتورة",false);
        EditText gross=field("قيمة البيع بالجنيه السوداني",false);gross.setInputType(2);
        EditText trip=field("تكلفة الرحلة / الترحيل",false);trip.setInputType(2);
        EditText notes=field("ملاحظات",false);
        f.addView(worker);f.addView(date);f.addView(invoice);f.addView(gross);f.addView(trip);f.addView(notes);
        TextView net=text("صافي الفاتورة: 0 ج.س",17,GREEN,true);f.addView(net);
        TextWatcher calc=new TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int count,int after){}public void onTextChanged(CharSequence x,int st,int before,int count){
            try{long g=empty(gross)?0:Long.parseLong(val(gross)),t=empty(trip)?0:Long.parseLong(val(trip));net.setText("صافي الفاتورة: "+money.format(g-t)+" ج.س");}catch(Exception ex){net.setText("تحقق من المبالغ");}
        }public void afterTextChanged(Editable e){}};
        gross.addTextChangedListener(calc);trip.addTextChangedListener(calc);
        new AlertDialog.Builder(this).setTitle("فاتورة سوق جديدة").setView(f).setNegativeButton("إلغاء",null).setPositiveButton("حفظ الفاتورة",(d,w)->{
            try{
                if(empty(worker)||empty(date)||empty(invoice)||empty(gross))throw new IllegalArgumentException();
                long g=Long.parseLong(val(gross)),t=empty(trip)?0:Long.parseLong(val(trip));
                if(g<0||t<0||t>g)throw new IllegalArgumentException();
                db.addSale(val(invoice),val(date),g,0,t,val(worker),val(notes));showSales();
            }catch(Exception ex){toast("أكمل البيانات وتأكد أن تكلفة الرحلة لا تتجاوز قيمة البيع");}
        }).show();
    }

    private void showSettlement() {
        base("تسوية نصيب التربال");
        content.addView(text("طريقة الحساب: صافي الفواتير − كامل التكلفة المشتركة المسجلة، ثم تقسيم المتبقي مناصفة. تخصم الميز والسلفيات والخدمات المحددة على التربال من نصيبه فقط.",14,DARK,false));
        long totalNet=0,totalShared=0,totalWorkerDeductions=0,totalWorkerShare=0;
        tableHeader(new String[]{"التربال","صافي الفواتير","التكلفة المشتركة","نصيب التربال قبل الخصم","خصومات التربال","الصافي المستحق"});
        try(Cursor names=db.workerNames()){
            while(names.moveToNext()){
                String worker=names.getString(0);
                long net=db.netSalesForWorker(worker);
                long shared=db.sumCostsForWorker(worker,SHARED_CATEGORIES);
                long deductions=db.sumCostsForWorker(worker,WORKER_DEDUCTIONS);
                long distributable=net-shared;
                long half=distributable/2;
                long due=half-deductions;
                tableRow(new String[]{worker,money.format(net),money.format(shared),money.format(half),money.format(deductions),money.format(due)});
                totalNet+=net;totalShared+=shared;totalWorkerDeductions+=deductions;totalWorkerShare+=due;
            }
        }
        content.addView(text("ملخص المشروع",18,GREEN,true));
        reportLine("صافي جميع الفواتير بعد تكلفة الرحلات",totalNet);
        reportLine("إجمالي التحضير والتقاوي والسماد والمبيد",totalShared);
        reportLine("نصيب التربال قبل الميز والسلفيات",(totalNet-totalShared)/2);
        reportLine("إجمالي خصومات الميز والسلفيات والخدمات",totalWorkerDeductions);
        content.addView(text("إجمالي صافي مستحقات الترابلة: "+money.format(totalWorkerShare)+" ج.س",21,GREEN,true));
        content.addView(text("مثال المعادلة: 50,000,000 − 5,000,000 = 45,000,000. نصيب كل طرف 22,500,000. إذا كانت خصومات التربال 2,000,000 يصبح صافي نصيبه 20,500,000.",13,Color.DKGRAY,false));
        addBack();
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

    private void showJournal() {
        base("دفتر اليومية والأستاذ");
        content.addView(text("تُرحّل تكاليف المشروع وفواتير البيع تلقائياً إلى قيود مزدوجة. ويمكن إضافة قيد يدوي متوازن.",14,DARK,false));
        Button add=button("＋ إضافة قيد يومية");
        content.addView(add);
        add.setOnClickListener(v->manualJournalDialog());
        tableHeader(new String[]{"التاريخ","المرجع","البيان","مدين","دائن"});
        try(Cursor c=AccountingBook.recentEntries(db.getReadableDatabase())) {
            while(c.moveToNext()) {
                tableRow(new String[]{c.getString(1),c.getString(2),c.getString(3),money.format(c.getLong(4)),money.format(c.getLong(5))});
            }
        }
        content.addView(text("يعرض الدفتر آخر 100 قيد مرحّل. القيود الآلية مرتبطة بسجل التكلفة أو فاتورة البيع لمنع الترحيل المكرر.",13,Color.DKGRAY,false));
        addBack();
    }

    private void manualJournalDialog() {
        LinearLayout f=form();
        EditText date=field("التاريخ YYYY-MM-DD",false); date.setText(today());
        EditText description=field("شرح القيد",false);
        Spinner debit=spinner(AccountingBook.ACCOUNT_NAMES);
        Spinner credit=spinner(AccountingBook.ACCOUNT_NAMES);
        EditText amount=field("المبلغ بالجنيه السوداني",false); amount.setInputType(2);
        f.addView(date);f.addView(description);
        f.addView(text("الحساب المدين",14,DARK,true));f.addView(debit);
        f.addView(text("الحساب الدائن",14,DARK,true));f.addView(credit);f.addView(amount);
        new AlertDialog.Builder(this).setTitle("قيد يومية مزدوج").setView(f)
            .setNegativeButton("إلغاء",null).setPositiveButton("ترحيل القيد",(dialog,which)->{
                try {
                    if(empty(date)||empty(description)||empty(amount)){toast("أكمل التاريخ والبيان والمبلغ");return;}
                    long value=Long.parseLong(val(amount));
                    if(value<=0||debit.getSelectedItemPosition()==credit.getSelectedItemPosition())throw new IllegalArgumentException();
                    String debitCode=AccountingBook.ACCOUNT_CODES[debit.getSelectedItemPosition()];
                    String creditCode=AccountingBook.ACCOUNT_CODES[credit.getSelectedItemPosition()];
                    AccountingBook.createManual(db.getWritableDatabase(),val(date),val(description),debitCode,creditCode,value);
                    toast("تم ترحيل القيد وهو متوازن");
                    showJournal();
                } catch(Exception ex){toast("تعذر ترحيل القيد. تحقق من المبلغ والحسابات");}
            }).show();
    }

    private void showTrialBalance() {
        base("ميزان المراجعة");
        content.addView(text("الأرصدة مستخرجة من القيود المرحلة في دفتر اليومية. يجب أن يتساوى إجمالي المدين مع إجمالي الدائن.",14,DARK,false));
        tableHeader(new String[]{"رمز الحساب","اسم الحساب","مدين","دائن","الرصيد"});
        long totalDebit=0,totalCredit=0;
        try(Cursor c=AccountingBook.trialBalance(db.getReadableDatabase())) {
            while(c.moveToNext()) {
                long debit=c.getLong(3),credit=c.getLong(4),balance=c.getLong(5);
                totalDebit+=debit;totalCredit+=credit;
                tableRow(new String[]{c.getString(0),c.getString(1),money.format(debit),money.format(credit),money.format(balance)});
            }
        }
        content.addView(text("إجمالي المدين: "+money.format(totalDebit)+" ج.س",18,DARK,true));
        content.addView(text("إجمالي الدائن: "+money.format(totalCredit)+" ج.س",18,DARK,true));
        boolean balanced=totalDebit==totalCredit;
        content.addView(text(balanced?"الميزان متوازن حسابياً":"يوجد فرق في ميزان المراجعة",17,balanced?GREEN:Color.RED,true));
        Button journal=button("فتح دفتر اليومية");
        content.addView(journal);journal.setOnClickListener(v->showJournal());
        addBack();
    }

    private void showReports() {
        base("ملخص حسابات الموسم");
        long sales=db.totalSalesNet();
        long shared=0,workerOnly=0;
        try(Cursor names=db.workerNames()){
            while(names.moveToNext()){
                String worker=names.getString(0);
                shared+=db.sumCostsForWorker(worker,SHARED_CATEGORIES);
                workerOnly+=db.sumCostsForWorker(worker,WORKER_DEDUCTIONS);
            }
        }
        long remainder=sales-shared;
        reportLine("صافي الفواتير بعد تكاليف الرحلات",sales);
        reportLine("التكاليف المشتركة قبل المناصفة",shared);
        reportLine("المتبقي القابل للتوزيع",remainder);
        reportLine("نصيب كل طرف قبل خصومات التربال",remainder/2);
        reportLine("خصومات الميز والسلفيات والخدمات",workerOnly);
        reportLine("صافي مستحق التربال",remainder/2-workerOnly);
        reportLine("إجمالي مصروفات دفتر التكلفة",db.total("costs","total"));
        Button settlement=button("فتح كشف التسوية التفصيلي");content.addView(settlement);settlement.setOnClickListener(v->showSettlement());
        Button journal=button("فتح دفتر اليومية");content.addView(journal);journal.setOnClickListener(v->showJournal());
        Button trial=button("عرض ميزان المراجعة");content.addView(trial);trial.setOnClickListener(v->showTrialBalance());
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
