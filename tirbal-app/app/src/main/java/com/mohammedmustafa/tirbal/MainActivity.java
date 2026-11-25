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
        // Review APKs must expose the complete local accounting app without a network/license gate.
        // Release APKs keep the remote licensing flow below.
        if (BuildConfig.REVIEW_MODE) {
            openReviewSession();
            return;
        }
        // Commercial startup is controlled by the licensing service. If config cannot be fetched,
        // the signed bootstrap check may still validate a cached or online license.
        LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
            if(config==null) {
                // Never fall back to an unrestricted review session when the licensing service is unreachable.
                continueLicenseCheck();
                return;
            }
            if(isMandatoryUpdate(config)) {
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
            retry.setOnClickListener(v -> {
                if (BuildConfig.REVIEW_MODE) {
                    if (openDatabaseSafely()) showGate();
                    return;
                }
                LicenseManager.check(this, check -> runOnUiThread(() -> {
                    currentDeviceId = check.deviceId;
                    if (check.unlocked) {
                        if (openDatabaseSafely()) {
                            licenseReady = true;
                            showGate();
                            startLicenseMonitor();
                        }
                    } else showSubscription(check);
                }));
            });
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

    private void showUserGuide() {
        base("الدليل الإرشادي");
        content.addView(text("دليل استخدام حساب التربال", 21, DARK, true));
        content.addView(text("ابدأ بإعداد المشروع، ثم سجّل التكاليف والمبيعات، وبعد الحصاد راجع التسوية والتقارير. احفظ البيانات بعد كل عملية.", 15, Color.DKGRAY, false));

        addGuideSection("١. إعداد المشروع", new String[]{
                "افتح بيانات المشروع من الصفحة الرئيسية.",
                "أدخل اسم المشروع واسم التاجر ونوع المحصول والمساحة إن وجدت.",
                "راجع البيانات قبل الحفظ. تظهر بيانات المشروع في لوحة المتابعة."
        });
        addGuideSection("٢. دفتر التكلفة", new String[]{
                "أضف صفاً مستقلاً لكل تربال.",
                "أدخل مبالغ الميز والأسمدة والمبيدات والتقاوي والتحضير والديون والسلفيات في أعمدتها.",
                "راجع إجمالي كل صف والإجمالي العام قبل الحفظ.",
                "تُعامل البنود المشتركة وفق اتفاق الشراكة، بينما تُخصم البنود المحمّلة على نصيب التربال وفق إعداد التسوية."
        });
        addGuideSection("٣. المدخلات الزراعية", new String[]{
                "أضف صنف المدخل الزراعي والكمية وسعر الوحدة.",
                "أضف صفوفاً أخرى عند الحاجة.",
                "راجع الإجمالي ثم احفظ الدفعة. لا تعتمد رقماً تقديرياً بدلاً من الفاتورة الفعلية."
        });
        addGuideSection("٤. السوق والمبيعات", new String[]{
                "سجّل كل فاتورة باسم التربال وتاريخها ورقمها.",
                "أدخل قيمة البيع وتكلفة الرحلة ومصروفات السوق.",
                "راجع صافي الفاتورة. يمكنك تسجيل أكثر من فاتورة للتربال الواحد."
        });
        addGuideSection("٥. التسوية", new String[]{
                "افتح تسوية نصيب التربال بعد تسجيل الفواتير والتكاليف.",
                "يُحسب صافي المبيعات بعد مصروفات الرحلة والسوق.",
                "تُخصم التكاليف المشتركة قبل قسمة المتبقي حسب نسبة الشراكة المسجلة.",
                "تُخصم الميز والسلفيات والخدمات من نصيب التربال عندما تنص طريقة التسوية على ذلك.",
                "راجع تفاصيل كل تربال ولا تعتمد التسوية قبل مطابقة المستندات."
        });
        addGuideSection("٦. التقارير والدفاتر", new String[]{
                "راجع الأرباح والخسائر لمقارنة المبيعات بالتكاليف.",
                "راجع دفتر اليومية والأستاذ لمتابعة القيود.",
                "استخدم ميزان المراجعة للتحقق من تساوي إجمالي المدين والدائن."
        });
        addGuideSection("٧. اتفاق الشراكة", new String[]{
                "افتح اتفاق الشراكة وحدد مسؤولية كل طرف عن البنود.",
                "حدد من يدفع ومن يتحمل التكلفة وكيف تخصم عند التسوية.",
                "لا تغيّر قواعد الموسم بعد بدء التسجيل إلا بعد توثيق الاتفاق ومراجعة أثره على الحسابات."
        });
        addGuideSection("٨. الأسعار والترخيص والتحديث", new String[]{
                "افتح الأسعار والاشتراك والتحديثات لقراءة الأسعار ومدة التجربة المنشورة من نواة الموقع.",
                "فترة التجربة الافتراضية سبعة أيام، وبعد انتهائها يلزم اعتماد اشتراك صالح.",
                "قدّم طلب الاشتراك وأرفق مرجع التحويل، وانتظر اعتماد الإدارة.",
                "نزّل التحديث من رابط موثوق. يجب توقيع كل إصدار جديد بمفتاح الإصدار نفسه حتى يقبل الهاتف التحديث فوق النسخة المثبتة."
        });
        addGuideSection("١٠. الوكلاء والرواكيب", new String[]{
                "أضف الوكيل الذي يتبع للتاجر، ثم أضف الرواكيب التي يشرف عليها.",
                "سجّل اسم الراكوبة واسم كبير المزارعين المسؤول عنها.",
                "افتح الراكوبة من قائمة الوكيل لإدارة سكانها، وسكان الراكوبة هم التربالة المسجلون فيها.",
                "استخدم زر القيد اليومي بجوار اسم التربال لتسجيل عمله أو ميزه أو مدخلاته الزراعية بتاريخها ومبلغها.",
                "تُحفظ القيود على قاعدة البيانات المحلية وتدخل في دفاتر التكلفة والتسوية وفق تصنيف البند."
        });
        addGuideSection("١١. حماية البيانات", new String[]{
                "احتفظ بالهاتف آمناً ولا تشارك كلمة المرور.",
                "لا تحذف التطبيق ولا تمسح بياناته عند تعذر التحديث.",
                "قبل الانتقال إلى هاتف آخر أو تثبيت نسخة بتوقيع مختلف، اطلب نسخة احتياطية قابلة للاستعادة واختبرها أولاً.",
                "طابق الأرصدة مع الفواتير والإيصالات والسجلات الأصلية بصورة دورية."
        });
    }

    private void addGuideSection(String heading, String[] points) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(12, 10, 12, 10);
        card.setBackground(rounded(Color.WHITE, 14, Color.rgb(225,232,222)));
        card.addView(text(heading, 17, GREEN, true));
        for (String point : points) {
            TextView line = text("• " + point, 14, DARK, false);
            line.setPadding(4, 5, 4, 5);
            card.addView(line);
        }
        content.addView(card, margin(-1, -2, 0, 0, 0, 10));
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
                info.append("\\nمدة التجربة: ").append(config.optInt("trialDays",7)).append(" يوماً");
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

    private static final String SUPPORT_WHATSAPP = "+249121851285";

    private void showSubscription(LicenseManager.Result result) {
        base("اشتراك حساب التربال");
        content.addView(text("فترة تجريبية مجانية، ثم يلزم اشتراك لمواصلة العمل.",16,DARK,true));
        content.addView(text(result.message,14,Color.DKGRAY,false));
        content.addView(text("الأسعار بالجنيه السوداني",17,GREEN,true));

        TextView pricing=text("الأسعار المرجعية: شهري 70,000 • سنوي 650,000 • دائم 1,300,000 ج.س",14,DARK,true);
        content.addView(pricing);
        Spinner plan=spinner(new String[]{"شهري — 70,000 جنيه","سنوي — 650,000 جنيه","دائم — 1,300,000 جنيه"});
        content.addView(plan);

        TextView payment=text("يجري الاتصال بنواة الترخيص لتحميل بيانات التحويل المعتمدة.",14,Color.DKGRAY,false);
        content.addView(payment);
        Button whatsapp=button("التواصل مع الدعم عبر واتساب");
        content.addView(whatsapp);
        whatsapp.setOnClickListener(v->openSupportWhatsApp());

        EditText customer=field("اسم صاحب الاشتراك",false);
        EditText contact=field("رقم هاتف العميل",false);
        EditText receipt=field("رقم مرجع التحويل",false);
        content.addView(customer);content.addView(contact);content.addView(receipt);

        Button submit=button("إرسال طلب الاشتراك للمراجعة");
        submit.setEnabled(false);
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
                        .setPositiveButton("مشاركة تفاصيل الطلب",(d,w)->{
                            Intent share=new Intent(Intent.ACTION_SEND);share.setType("text/plain");
                            share.putExtra(Intent.EXTRA_TEXT,message);
                            startActivity(Intent.createChooser(share,"إرسال تفاصيل طلب الاشتراك"));
                        }).setNegativeButton("حسناً",null).show();
            }));
        });

        Button reload=button("إعادة تحميل الأسعار وبيانات الدفع");
        content.addView(reload);
        reload.setOnClickListener(v->loadSubscriptionConfig(pricing,plan,payment,whatsapp,submit));
        Button refresh=button("تحقق من التفعيل / استعادة الاشتراك");
        content.addView(refresh);
        refresh.setOnClickListener(v->{
            refresh.setEnabled(false);
            LicenseManager.check(this,check->runOnUiThread(()->{
                refresh.setEnabled(true);
                currentDeviceId=check.deviceId;
                if(check.unlocked){if(!openDatabaseSafely())return;licenseReady=true;showGate();startLicenseMonitor();}
                else showSubscription(check);
            }));
        });
        loadSubscriptionConfig(pricing,plan,payment,whatsapp,submit);
    }

    private void loadSubscriptionConfig(TextView pricing,Spinner plan,TextView payment,Button whatsapp,Button submit) {
        pricing.setText("جارٍ تحديث الأسعار من نواة الموقع...");
        payment.setText("جارٍ تحميل بيانات التحويل...");
        submit.setEnabled(false);
        LicenseManager.loadConfig((config,error)->runOnUiThread(()->{
            if(config==null) {
                pricing.setText("تعذر تحديث الأسعار. المعروض أسعار مرجعية غير مؤكدة من الخادم: شهري 70,000 • سنوي 650,000 • دائم 1,300,000 ج.س");
                payment.setText((error==null?"تعذر الاتصال بنواة الترخيص.":error)+" يمكنك التواصل مع الدعم عبر واتساب أو إعادة المحاولة.");
                whatsapp.setEnabled(true);
                submit.setEnabled(false);
                return;
            }
            JSONObject p=config.optJSONObject("payment");
            if(p==null) payment.setText("بيانات التحويل غير متاحة حالياً. تواصل مع الدعم قبل التحويل.");
            else {
                String details="بنك التحويل: "+p.optString("bankName","غير محدد")+
                        "\nاسم الحساب: "+p.optString("accountName","غير محدد")+
                        "\nرقم الحساب: "+p.optString("accountNumber","لم يُضبط بعد")+
                        "\nواتساب الدعم: "+p.optString("whatsapp",SUPPORT_WHATSAPP);
                payment.setText(details);
                String number=p.optString("whatsapp",SUPPORT_WHATSAPP).replaceAll("[^0-9]","");
                whatsapp.setEnabled(!number.isEmpty());
                whatsapp.setOnClickListener(v->openWhatsApp(number));
            }
            JSONObject plans=config.optJSONObject("plans");
            boolean ready=false;
            if(plans!=null) {
                JSONObject monthly=plans.optJSONObject("monthly"),annual=plans.optJSONObject("annual"),lifetime=plans.optJSONObject("lifetime");
                if(monthly!=null&&annual!=null&&lifetime!=null) {
                    long m=monthly.optLong("amount"),a=annual.optLong("amount"),l=lifetime.optLong("amount");
                    pricing.setText("شهري "+money.format(m)+" ج.س  •  سنوي "+money.format(a)+" ج.س  •  دائم "+money.format(l)+" ج.س");
                    plan.setAdapter(new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item,
                            new String[]{"شهري — "+money.format(m)+" جنيه","سنوي — "+money.format(a)+" جنيه","دائم — "+money.format(l)+" جنيه"}));
                    ready=m>0&&a>0&&l>0;
                }
            }
            if(!ready) {
                pricing.setText("تعذر تحميل خطط الاشتراك المعتمدة. أعد المحاولة قبل إرسال الطلب.");
                submit.setEnabled(false);
            } else submit.setEnabled(true);
        }));
    }

    private void openSupportWhatsApp() {
        openWhatsApp(SUPPORT_WHATSAPP.replaceAll("[^0-9]",""));
    }

    private void openWhatsApp(String digits) {
        if(digits==null||digits.isEmpty()){toast("رقم الدعم غير متاح حالياً.");return;}
        String message="مرحباً، أحتاج مساعدة بخصوص ترخيص تطبيق حساب التربال.";
        Intent intent=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse("https://wa.me/"+digits+"?text="+android.net.Uri.encode(message)));
        try{startActivity(intent);}catch(Exception ex){toast("تعذر فتح واتساب أو المتصفح.");}
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
        if (!"حساب التربال".equals(title) && !"اشتراك حساب التربال".equals(title)) {
            Button home = button("⌂  العودة إلى الرئيسية");
            home.setTextSize(14);
            root.addView(home, new LinearLayout.LayoutParams(-1, dp(48)));
            home.setOnClickListener(v -> {
                if (db != null && db.hasProject()) showHome();
                else showGate();
            });
        }
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
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER);
        hero.setPadding(18, 22, 18, 22);
        GradientDrawable heroBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(7,75,39), Color.rgb(24,135,62), Color.rgb(5,83,54)});
        heroBg.setCornerRadius(dp(22));
        hero.setBackground(heroBg);
        TextView brand = text("🌱  حساب التربال", 28, Color.WHITE, true);
        brand.setGravity(Gravity.CENTER);
        hero.addView(brand);
        TextView slogan = text("برنامج المحاسبة الزراعية الذكي", 15, Color.rgb(255,225,145), true);
        slogan.setGravity(Gravity.CENTER);
        hero.addView(slogan);
        TextView project = text(db.projectName() + "  •  " + "الجنيه السوداني", 14, Color.WHITE, false);
        project.setGravity(Gravity.CENTER);
        hero.addView(project);
        content.addView(hero, margin(-1,-2,0,0,0,12));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.VERTICAL);
        stats.setPadding(14, 12, 14, 12);
        stats.setBackground(rounded(Color.WHITE, 16, Color.rgb(220,230,218)));
        long costs = db.total("costs", "total");
        long sales = db.total("sales", "gross");
        long net = db.total("sales", "net");
        stats.addView(text("إجمالي التكلفة  " + money.format(costs) + " ج.س", 16, DARK, true));
        stats.addView(text("إجمالي المبيعات  " + money.format(sales) + " ج.س", 16, GREEN, true));
        stats.addView(text("صافي نتائج المبيعات  " + money.format(net) + " ج.س", 16, net >= 0 ? GREEN : Color.RED, true));
        content.addView(stats, margin(-1,-2,0,0,0,14));
        content.addView(text("إدارة المزرعة والحسابات", 18, DARK, true));

        addTile("📘 الدليل الإرشادي", "تعلم استخدام التطبيق خطوة بخطوة", v -> showUserGuide());
        addTile("🧑‍🌾 الوكلاء والرواكيب", "الوكلاء وكبار المزارعين وسكان الرواكيب", v -> showAgents());
        addTile("🧮 المحاسب والدفاتر", "دفتر اليومية والأستاذ وميزان المراجعة", v -> showJournal());
        addTile("🧾 الفواتير والمبيعات", "فواتير السوق والترحيل وصافي البيع", v -> showSales());
        addTile("🌱 الدليل الزراعي والمواسم", "الطماطم والبطيخ والبامية ومحاصيل ومواعيد الزراعة حسب الإقليم", v -> showCropHub());
        addTile("🌾 تكلفة الزراعة", "الميز والأسمدة والمبيدات والتقاوي والخدمات", v -> showCosts());
        addTile("⚖️ التسوية والميزان", "تسوية الحقوق والالتزامات وحساب الأنصبة", v -> showSettlement());
        addTile("📊 التقارير", "تقارير الموسم والأرباح والخسائر", v -> showReports());
        addTile("🔐 الترخيص والاشتراك", "الأسعار وطلب الاشتراك وحالة الترخيص", v -> showSubscriptionInfo());
        addTile("🌐 نواة الموقع والخلية", "الأسعار والتحديثات والإعلانات المركزية", v -> showUpdateCenter());
        addTile("⚙️ إعدادات التطبيق", "بيانات المشروع وكلمة المرور واتفاق الشراكة", v -> showSettingsHub());
        addTile("🎧 الدعم الفني", "دليل المساعدة وبيانات التواصل", v -> showSupportHub());

        content.addView(text("أدوات العمل اليومية", 18, DARK, true));
        addTile("📁 بيانات المشروع", "اسم المشروع والمحصول والتاجر والمساحة", v -> showProjectForm(true));
        addTile("🛒 السوق والمبيعات", "الفواتير والتاريخ والتكلفة ومنصرفات السوق والصافي", v -> showSales());
        addTile("📅 الرزنامة الزراعية", "مواعيد الزراعة والري والتسميد والرش والحصاد", v -> showCalendar());
        addTile("🌱 مدخلات الزراعة", "إدخال سريع متعدد البنود والكميات والأسعار", v -> showAgricultureInputs());
        addTile("👨‍🌾 العمال والتربال", "تسجيل أسماء التربالة وربطها ببنود التكلفة", v -> showCosts());
        addTile("💳 الديون والسلفيات", "متابعة السلفيات والديون وتصنيفها المحاسبي", v -> showCosts());
        addTile("🤝 اتفاق الشراكة والتسوية", "مسؤوليات التاجر والتربال وقواعد الخصم", v -> showPartnership());
    }

    private void addTile(String title, String subtitle, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(14, 12, 14, 12);
        card.setElevation(dp(3));
        int[][] palette = {
                {Color.rgb(30,75,180), Color.rgb(16,115,205)},
                {Color.rgb(15,115,63), Color.rgb(35,155,78)},
                {Color.rgb(108,42,160), Color.rgb(151,62,190)},
                {Color.rgb(190,105,12), Color.rgb(230,150,20)},
                {Color.rgb(0,120,125), Color.rgb(0,160,145)}
        };
        int colorIndex = Math.abs(title.hashCode()) % palette.length;
        GradientDrawable background = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                palette[colorIndex]);
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), Color.argb(100,255,255,255));
        card.setBackground(background);

        int split = title.indexOf(' ');
        String icon = split > 0 ? title.substring(0, split) : "◈";
        String label = split > 0 ? title.substring(split + 1).trim() : title;
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        TextView iconView = text(icon, 30, Color.WHITE, true);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(rounded(Color.argb(45,255,255,255), 14, Color.argb(90,255,255,255)));
        line.addView(iconView, new LinearLayout.LayoutParams(dp(58), dp(58)));
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(12, 0, 0, 0);
        labels.addView(text(label, 17, Color.WHITE, true));
        labels.addView(text(subtitle, 13, Color.WHITE, false));
        line.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text("‹", 30, Color.WHITE, true);
        arrow.setGravity(Gravity.CENTER);
        line.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(48)));
        card.addView(line);
        content.addView(card, margin(-1,-2,0,0,0,10));
        card.setOnClickListener(click);
    }

    private void showCropHub() {
        base("الدليل الزراعي ومواسم المحاصيل");
        content.addView(text("دليل محلي يعمل دون إنترنت. اختر الإقليم لعرض نوافذ الزراعة والحصاد والمحاصيل المناسبة وفق المراجع المنشورة. المواعيد تقريبية وتتغير مع الأمطار والصنف ومصدر الري.",14,DARK,false));
        String[] regionNames=new String[CropGuideCatalog.REGIONS.length];
        for(int i=0;i<CropGuideCatalog.REGIONS.length;i++)regionNames[i]=CropGuideCatalog.REGIONS[i][1];
        Spinner region=spinner(regionNames);
        content.addView(text("الإقليم الزراعي",15,GREEN,true));
        content.addView(region);
        TextView regionInfo=text("",13,Color.DKGRAY,false);
        regionInfo.setBackground(rounded(Color.WHITE,12,Color.rgb(220,230,218)));
        regionInfo.setPadding(12,10,12,10);
        content.addView(regionInfo);
        LinearLayout cropList=new LinearLayout(this);
        cropList.setOrientation(LinearLayout.VERTICAL);
        content.addView(cropList);
        Runnable render=()->{
            int selected=Math.max(0,Math.min(region.getSelectedItemPosition(),CropGuideCatalog.REGIONS.length-1));
            String[] selectedRegion=CropGuideCatalog.REGIONS[selected];
            regionInfo.setText("التربة: "+selectedRegion[2]+"\nالمناخ والموسم: "+selectedRegion[3]);
            cropList.removeAllViews();
            String[][] crops=CropGuideCatalog.cropsForRegion(selectedRegion[0]);
            for(String[] crop:crops){
                LinearLayout card=new LinearLayout(this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(14,10,14,10);
                card.setBackground(rounded(Color.WHITE,14,Color.rgb(220,230,218)));
                card.addView(text("🌱  "+crop[1],18,GREEN,true));
                card.addView(text("الزراعة: "+crop[2],14,DARK,true));
                card.addView(text("الحصاد: "+crop[3]+"  •  دورة المحصول: "+crop[4],13,Color.DKGRAY,false));
                Button details=button("تفاصيل الإرشاد الزراعي");
                card.addView(details);
                details.setOnClickListener(v->{
                    String message="نافذة الزراعة: "+crop[2]+"\nنافذة الحصاد: "+crop[3]+"\nمدة الدورة: "+crop[4]
                        +"\n\nإرشادات الحقل\n"+crop[5]+"\n\nالمخاطر والآفات التي تستحق المتابعة\n"+crop[6]
                        +"\n\nالمصدر\n"+crop[7];
                    new AlertDialog.Builder(this).setTitle(crop[1]).setMessage(message).setPositiveButton("حسناً",null).show();
                });
                cropList.addView(card,margin(-1,-2,0,0,0,10));
            }
        };
        region.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
            public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){render.run();}
        });
        render.run();
        content.addView(text("إدارة التربة والآفات",18,GREEN,true));
        Button soil=button("دليل أنواع التربة وتحسينها");
        content.addView(soil);
        soil.setOnClickListener(v->showSoilGuide());
        Button pests=button("دليل الآفات والمكافحة والمبيدات");
        content.addView(pests);
        pests.setOnClickListener(v->showPestGuide());
        Button calendar=button("فتح رزنامة أعمال المزرعة");
        content.addView(calendar);
        calendar.setOnClickListener(v->showCalendar());
        addBack();
    }

    private void showSoilGuide() {
        new AlertDialog.Builder(this).setTitle("دليل التربة الزراعية")
            .setMessage("التربة الطينية: تحتفظ بالماء والعناصر، لكنها تحتاج إدارة صرف وتجنب التغريق والانضغاط.\n\nالتربة الرملية: صرفها سريع واحتفاظها بالماء والعناصر أقل؛ قسّم الري إلى دفعات مناسبة وأضف المادة العضوية وفق التحليل.\n\nالتربة الطميية/اللومية: غالباً متوازنة في الصرف والاحتفاظ بالماء، لكن خصوبتها تختلف من حقل لآخر.\n\nالتربة الملحية أو الصودية: لا تعالجها بالتخمين؛ افحص ملوحة التربة ومياه الري، وحدد الصرف والمعالجة مع اختصاصي.\n\nقبل اختيار السماد أو برنامج الري: حلّل التربة والماء، وسجّل القوام والـ pH والملوحة والمادة العضوية. لا توجد جرعة سماد واحدة تصلح لكل الحقول.")
            .setPositiveButton("حسناً",null).show();
    }

    private void showPestGuide() {
        new AlertDialog.Builder(this).setTitle("الآفات والمكافحة الآمنة")
            .setMessage("ابدأ بالمكافحة المتكاملة: فحص دوري، إزالة بقايا النباتات المصابة، دورة زراعية، أصناف سليمة، نظافة الحقل، ومراقبة الحشرات النافعة.\n\nمجموعات المبيدات بحسب المشكلة: مبيدات حشرية للحشرات، مبيدات فطرية للأمراض الفطرية، مبيدات حشائش للأعشاب، ومعاملات بذور عند وجود توصية معتمدة. اختيار المجموعة وحده لا يكفي لتحديد المنتج.\n\nلا تستخدم اسماً تجارياً أو مادة فعالة أو جرعة قبل التأكد من تسجيل المنتج للمحصول والآفة في السودان وقراءة ملصق العبوة. التزم بمعدات الوقاية، وفترة ما قبل الحصاد، وفترة إعادة الدخول، وعدم خلط المنتجات دون توصية مكتوبة. أبعد المبيدات عن الأطفال والمياه والأعلاف، وتخلص من العبوات حسب تعليمات الجهة المختصة.\n\nإذا لم تتأكد من نوع الآفة، صوّر العَرَض واعرضه على مهندس وقاية نبات قبل الرش.")
            .setPositiveButton("حسناً",null).show();
    }

    private void showSubscriptionInfo() {
        base("الترخيص والاشتراك");
        content.addView(text("تُحمّل تفاصيل الأسعار ومدة التجربة وبيانات التحويل من نواة الموقع عند توفر الاتصال.",15,DARK,false));
        Button refresh=button("عرض حالة الاشتراك والأسعار");
        content.addView(refresh);
        refresh.setOnClickListener(v->LicenseManager.loadConfig((cfg,error)->runOnUiThread(()->{
            if(cfg==null){toast(error==null?"تعذر الاتصال بنواة الموقع":error);return;}
            JSONObject plans=cfg.optJSONObject("plans");
            String message="مدة التجربة: "+cfg.optInt("trialDays",7)+" أيام";
            if(plans!=null) {
                JSONObject m=plans.optJSONObject("monthly"),a=plans.optJSONObject("annual"),l=plans.optJSONObject("lifetime");
                if(m!=null)message+="\\nالشهري: "+m.optLong("amount")+" ج.س";
                if(a!=null)message+="\\nالسنوي: "+a.optLong("amount")+" ج.س";
                if(l!=null)message+="\\nالدائم: "+l.optLong("amount")+" ج.س";
            }
            new AlertDialog.Builder(this).setTitle("تفاصيل الاشتراك").setMessage(message).setPositiveButton("حسناً",null).show();
        })));
        addBack();
    }

    private void showSettingsHub() {
        base("إعدادات التطبيق");
        addTile("📁 بيانات المشروع","تعديل بيانات المزرعة والمحصول",v->showProjectForm(true));
        addTile("🤝 اتفاق الشراكة","تحديد المسؤوليات وطريقة الخصم",v->showPartnership());
        addTile("🔐 كلمة المرور","تغيير كلمة مرور التطبيق",v->changePassword());
        addBack();
    }

    private void showSupportHub() {
        base("الدعم الفني");
        content.addView(text("يمكنك الرجوع إلى الدليل الإرشادي لمعرفة خطوات إدخال البيانات ومراجعة الحسابات.",15,DARK,false));
        Button guide=button("فتح الدليل الإرشادي");
        content.addView(guide);
        guide.setOnClickListener(v->showUserGuide());
        addBack();
    }

    private void showAgents() {
        base("الوكلاء والرواكيب");
        content.addView(text("سجّل كل وكيل يتبع للتاجر، ثم أضف الرواكيب التي يشرف عليها. افتح اسم الراكوبة لعرض سكانها وإدخال القيد اليومي لهم.",14,DARK,false));
        Button add=button("＋ إضافة وكيل");
        content.addView(add);
        add.setOnClickListener(v->agentDialog());
        try(Cursor agents=db.agents()) {
            if(!agents.moveToFirst()) content.addView(text("لا توجد وكلاء مسجلون بعد. ابدأ بإضافة وكيل.",15,Color.DKGRAY,false));
            else do {
                long id=agents.getLong(0);
                String name=agents.getString(1),phone=agents.getString(2);
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
                card.setPadding(14,12,14,12);card.setBackground(rounded(Color.WHITE,16,Color.rgb(220,230,218)));
                card.addView(text("🧑‍🌾 "+name,18,GREEN,true));
                card.addView(text("الهاتف: "+(phone==null||phone.isEmpty()?"غير مسجل":phone),13,Color.DKGRAY,false));
                Button open=button("فتح الوكيل وإدارة الرواكيب");
                card.addView(open);open.setOnClickListener(v->showAgentDetail(id,name));
                content.addView(card,margin(-1,-2,0,0,0,10));
            } while(agents.moveToNext());
        }
        addBack();
    }

    private void agentDialog() {
        LinearLayout f=form();
        EditText name=field("اسم الوكيل",false),phone=field("رقم الهاتف (اختياري)",false),notes=field("ملاحظات الوكيل (اختياري)",false);
        f.addView(name);f.addView(phone);f.addView(notes);
        new AlertDialog.Builder(this).setTitle("إضافة وكيل جديد").setView(f)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ الوكيل",(d,w)->{
                if(empty(name)){toast("أدخل اسم الوكيل");return;}
                try{db.saveAgent(val(name),val(phone),val(notes));showAgents();toast("تم حفظ الوكيل");}
                catch(Exception e){toast("تعذر حفظ الوكيل. تحقق من البيانات");}
            }).show();
    }

    private void showAgentDetail(long agentId,String agentName) {
        base("الوكيل: "+agentName);
        content.addView(text("الرواكيب التابعة للوكيل",17,DARK,true));
        Button add=button("＋ إضافة راكوبة");
        content.addView(add);add.setOnClickListener(v->raqubaDialog(agentId,agentName));
        try(Cursor rows=db.raqubasForAgent(agentId)) {
            if(!rows.moveToFirst())content.addView(text("لم تُضف رواكيب لهذا الوكيل بعد.",14,Color.DKGRAY,false));
            else do {
                long id=rows.getLong(0);String name=rows.getString(1),senior=rows.getString(2);
                Button raquba=button("🏡 "+name+"  |  كبير المزارعين: "+senior);
                raquba.setTextSize(14);content.addView(raquba,margin(-1,-2,0,6,0,6));
                raquba.setOnClickListener(v->showRaqubaPanel(agentId,agentName,id,name,senior));
            }while(rows.moveToNext());
        }
        Button back=button("العودة إلى قائمة الوكلاء");content.addView(back);back.setOnClickListener(v->showAgents());
        addBack();
    }

    private void raqubaDialog(long agentId,String agentName) {
        LinearLayout f=form();
        EditText name=field("اسم الراكوبة",false),senior=field("اسم كبير المزارعين",false),notes=field("ملاحظات (اختياري)",false);
        f.addView(name);f.addView(senior);f.addView(notes);
        new AlertDialog.Builder(this).setTitle("إضافة راكوبة للوكيل "+agentName).setView(f)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ الراكوبة",(d,w)->{
                if(empty(name)||empty(senior)){toast("أدخل اسم الراكوبة واسم كبير المزارعين");return;}
                try{db.saveRaquba(agentId,val(name),val(senior),val(notes));showAgentDetail(agentId,agentName);toast("تم حفظ الراكوبة");}
                catch(Exception e){toast("تعذر حفظ الراكوبة");}
            }).show();
    }

    private void showRaqubaPanel(long agentId,String agentName,long raqubaId,String raqubaName,String senior) {
        android.app.Dialog panel=new android.app.Dialog(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16,18,16,18);box.setBackgroundColor(BG);
        TextView heading=text("🏡 "+raqubaName,21,DARK,true);box.addView(heading);
        box.addView(text("الوكيل: "+agentName,14,Color.DKGRAY,false));
        box.addView(text("كبير المزارعين: "+senior,15,GREEN,true));
        box.addView(text("سكان الراكوبة — التربالة",16,DARK,true));
        LinearLayout members=new LinearLayout(this);members.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this);scroll.addView(members);
        box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Runnable reload=()->{
            members.removeAllViews();
            try(Cursor c=db.workersForRaquba(raqubaId)){
                if(!c.moveToFirst())members.addView(text("لم تتم إضافة تربالة لهذه الراكوبة.",14,Color.DKGRAY,false));
                else do {
                    String worker=c.getString(0);
                    LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);line.setGravity(Gravity.CENTER_VERTICAL);
                    line.addView(text("👨‍🌾 "+worker,15,DARK,true),new LinearLayout.LayoutParams(0,-2,1));
                    Button daily=button("قيد يومي");daily.setTextSize(12);line.addView(daily);
                    daily.setOnClickListener(v->dailyAgentEntry(raqubaId,worker));
                    members.addView(line,margin(-1,-2,0,0,0,6));
                }while(c.moveToNext());
            }
        };
        reload.run();
        Button addWorker=button("＋ إضافة تربال إلى الراكوبة");
        box.addView(addWorker);addWorker.setOnClickListener(v->{
            EditText worker=field("اسم التربال / المزارع",false);
            new AlertDialog.Builder(this).setTitle("إضافة ساكن للراكوبة").setView(worker)
                .setNegativeButton("إلغاء",null).setPositiveButton("إضافة",(d,w)->{
                    if(empty(worker)){toast("أدخل اسم التربال");return;}
                    try{db.addRaqubaWorker(raqubaId,val(worker));reload.run();toast("تمت إضافة التربال");}
                    catch(Exception e){toast("الاسم مسجل في هذه الراكوبة بالفعل أو تعذر الحفظ");}
                }).show();
        });
        Button close=button("إغلاق الراكوبة");box.addView(close);close.setOnClickListener(v->panel.dismiss());
        panel.setContentView(box);
        android.view.Window window=panel.getWindow();
        if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);window.setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.90),-1);window.setGravity(Gravity.END);}
        panel.show();
        if(panel.getWindow()!=null)panel.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.90),-1);
    }

    private long findAgentIdForRaquba(long raqubaId) {
        try(Cursor c=db.getReadableDatabase().rawQuery("SELECT agent_id FROM raqubas WHERE id=? AND project_id=1",new String[]{String.valueOf(raqubaId)})){
            if(c.moveToFirst())return c.getLong(0);
        }
        throw new IllegalStateException("الراكوبة غير موجودة");
    }

    private void dailyAgentEntry(long raqubaId,String worker) {
        LinearLayout f=form();
        EditText date=field("تاريخ القيد YYYY-MM-DD",false);date.setText(today());
        Spinner category=spinner(QUICK_CATEGORIES);
        EditText item=field("البيان / العمل الزراعي",false);
        EditText quantity=field("الكمية (اكتب 1 للمبلغ المباشر)",false);quantity.setText("1");quantity.setInputType(8194);
        EditText price=field("سعر الوحدة أو المبلغ بالجنيه",false);price.setInputType(2);
        EditText notes=field("ملاحظات الوكيل",false);
        f.addView(text("التربال: "+worker,15,GREEN,true));f.addView(date);f.addView(category);f.addView(item);f.addView(quantity);f.addView(price);f.addView(notes);
        new AlertDialog.Builder(this).setTitle("القيد اليومي — "+worker).setView(f)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ القيد",(d,w)->{
                try{
                    if(empty(date)||empty(item)||empty(quantity)||empty(price))throw new IllegalArgumentException();
                    double q=Double.parseDouble(val(quantity));long p=Long.parseLong(val(price));
                    if(q<=0||p<0||!Double.isFinite(q))throw new IllegalArgumentException();
                    String cat=category.getSelectedItem().toString();String[] defaults=db.defaultResponsibility(cat);
                    db.addAgentCost(findAgentIdForRaquba(raqubaId),raqubaId,val(date),worker,cat,val(item),q,p,"التاجر",defaults[0],defaults[2],defaults[3],"","قيد يومي بواسطة الوكيل؛ "+val(notes));
                    toast("تم حفظ القيد اليومي للتربال "+worker);
                }catch(Exception e){toast("لم يُحفظ القيد. تحقق من التاريخ والكمية والمبلغ");}
            }).show();
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

    private static final String[] QUICK_CATEGORIES = {"الميز","الأسمدة","المبيدات","التقاوي","تحضير الأرض","الديون","السلفيات"};
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
        content.addView(text("أدخل اسم كل تربال مرة واحدة، ثم أدخل المبالغ تحت البنود. يمكنك إضافة عدد كبير من الصفوف وحذف أي صف قبل الحفظ.",14,DARK,false));
        content.addView(text("الأسمدة والمبيدات والتقاوي والتحضير والديون تدخل في التكلفة المشتركة. الميز والسلفيات تخصم من نصيب التربال بعد المناصفة.",13,Color.DKGRAY,false));
        LinearLayout table=new LinearLayout(this);table.setOrientation(LinearLayout.VERTICAL);
        content.addView(table);
        List<CostRow> rows=new ArrayList<>();
        addQuickCostHeader(table);
        TextView summary=text("",15,DARK,true);
        summary.setPadding(10,12,10,12);
        summary.setBackground(rounded(Color.WHITE,12,Color.rgb(218,228,216)));
        content.addView(summary,margin(-1,-2,0,10,0,10));
        Runnable addRow=()->addQuickCostRow(table,rows,summary);
        Button add=button("＋ إضافة تربال جديد");
        content.addView(add);add.setOnClickListener(v->addRow.run());
        addRow.run();
        EditText date=field("تاريخ دفتر التكلفة YYYY-MM-DD",false);date.setText(today());content.addView(date);
        Button save=button("حفظ جميع صفوف التكلفة");
        content.addView(save);
        save.setOnClickListener(v->{
            if(empty(date)){toast("أدخل تاريخ التكلفة");return;}
            int pending=0;
            try {
                for(CostRow row:rows) {
                    String worker=val(row.worker);
                    boolean hasAmount=false;
                    for(EditText amountField:row.amounts) {
                        String raw=val(amountField);
                        if(!raw.isEmpty()) {
                            long amount=Long.parseLong(raw);
                            if(amount<0)throw new NumberFormatException();
                            if(amount>0)hasAmount=true;
                        }
                    }
                    if(worker.isEmpty()&&hasAmount)throw new IllegalArgumentException("اسم التربال مطلوب لكل صف يحتوي مبالغ");
                    if(!worker.isEmpty()) {
                        for(EditText amountField:row.amounts)if(!val(amountField).isEmpty()&&Long.parseLong(val(amountField))>0)pending++;
                    }
                }
                if(pending==0){toast("أدخل اسم تربال ومبلغاً واحداً على الأقل");return;}
                net.zetetic.database.sqlcipher.SQLiteDatabase writable=db.getWritableDatabase();
                writable.beginTransaction();
                try {
                    for(CostRow row:rows) {
                        String worker=val(row.worker);
                        if(worker.isEmpty())continue;
                        for(int i=0;i<QUICK_CATEGORIES.length;i++) {
                            String raw=val(row.amounts[i]);
                            if(raw.isEmpty())continue;
                            long amount=Long.parseLong(raw);
                            if(amount==0)continue;
                            String category=QUICK_CATEGORIES[i];
                            String[] defaults=db.defaultResponsibility(category);
                            db.addCost(val(date),worker,category,category,1,amount,"التاجر",defaults[0],defaults[2],defaults[3],"","إدخال سريع من دفتر التكلفة");
                        }
                    }
                    writable.setTransactionSuccessful();
                } finally { writable.endTransaction(); }
                toast("تم حفظ "+pending+" بند تكلفة دون تجزئة العملية");
                showCosts();
            }catch(Exception ex){toast("لم تحفظ البيانات. راجع أسماء الترابلة والمبالغ ثم حاول مجدداً.");}
        });
        content.addView(text("آخر السجلات المحفوظة",17,GREEN,true));
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
        HorizontalScrollView hs=new HorizontalScrollView(this);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        String[] labels={"اسم التربال","الميز","السماد","المبيد","التقاوي","التحضير","الديون","السلفيات","الجملة","حذف"};
        int[] widths={132,82,82,82,82,82,82,82,105,64};
        for(int i=0;i<labels.length;i++){
            TextView t=text(labels[i],12,Color.WHITE,true);t.setGravity(Gravity.CENTER);t.setBackgroundColor(GREEN);
            row.addView(t,new LinearLayout.LayoutParams(dp(widths[i]),dp(46)));
        }
        hs.addView(row);table.addView(hs);
    }

    private void addQuickCostRow(LinearLayout table,List<CostRow> rows,TextView summary) {
        CostRow item=new CostRow();item.worker=workerInput("اسم التربال",132);
        HorizontalScrollView hs=new HorizontalScrollView(this);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(item.worker);
        for(int i=0;i<item.amounts.length;i++){
            item.amounts[i]=cellInput("0",true,82);
            item.amounts[i].setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_NEXT);
            row.addView(item.amounts[i]);
        }
        item.total=cellLabel("0",105);row.addView(item.total);
        Button remove=button("حذف");remove.setTextSize(11);remove.setPadding(0,0,0,0);
        row.addView(remove,new LinearLayout.LayoutParams(dp(64),dp(48)));
        hs.addView(row);table.addView(hs);
        TextWatcher watcher=new TextWatcher(){
            public void beforeTextChanged(CharSequence x,int st,int count,int after){}
            public void onTextChanged(CharSequence x,int st,int before,int count){
                long total=0;
                for(EditText e:item.amounts){try{String value=val(e);if(!value.isEmpty())total=Math.addExact(total,Long.parseLong(value));}catch(Exception ignored){}}
                item.total.setText(money.format(total));
                updateQuickCostSummary(rows,summary);
            }
            public void afterTextChanged(Editable e){}
        };
        for(EditText e:item.amounts)e.addTextChangedListener(watcher);
        item.worker.addTextChangedListener(watcher);
        remove.setOnClickListener(v->{rows.remove(item);table.removeView(hs);updateQuickCostSummary(rows,summary);});
        rows.add(item);
        updateQuickCostSummary(rows,summary);
    }

    private void updateQuickCostSummary(List<CostRow> rows,TextView summary) {
        long shared=0,deductions=0,total=0;
        for(CostRow row:rows)for(int i=0;i<row.amounts.length;i++){
            try{
                String raw=val(row.amounts[i]);if(raw.isEmpty())continue;
                long amount=Long.parseLong(raw);
                total=Math.addExact(total,amount);
                if(i==0||i==6)deductions=Math.addExact(deductions,amount);
                else shared=Math.addExact(shared,amount);
            }catch(Exception ignored){}
        }
        summary.setText("إجمالي دفتر التكلفة: "+money.format(total)+" ج.س\n"
            +"التكاليف المشتركة قبل المناصفة: "+money.format(shared)+" ج.س\n"
            +"الميز والسلفيات المخصومة من نصيب التربال: "+money.format(deductions)+" ج.س");
    }

    private AutoCompleteTextView workerInput(String hint,int width) {
        AutoCompleteTextView input=new AutoCompleteTextView(this);
        input.setSingleLine(true);input.setTextSize(14);input.setHint(hint);
        input.setGravity(Gravity.CENTER);input.setPadding(6,4,6,4);
        input.setBackground(rounded(Color.WHITE,6,Color.rgb(210,220,210)));
        input.setThreshold(1);
        ArrayList<String> names=new ArrayList<>();
        try(Cursor c=db.workerNames()){while(c.moveToNext())names.add(c.getString(0));}
        input.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,names));
        input.setLayoutParams(new LinearLayout.LayoutParams(width<0?-1:dp(width),dp(48)));
        return input;
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
        TextView summary=text("إجمالي المدخلات الزراعية: 0 ج.س",16,GREEN,true);
        content.addView(summary);
        Button add=button("＋ إضافة صنف زراعي");content.addView(add);add.setOnClickListener(v->addAgricultureRow(rowsBox,rows,summary));
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
                    if(q<=0||p<0||!Double.isFinite(q))throw new IllegalArgumentException();
                    count++;
                }
                if(count==0){toast("أدخل صنفاً واحداً على الأقل");return;}
                net.zetetic.database.sqlcipher.SQLiteDatabase writable=db.getWritableDatabase();
                writable.beginTransaction();
                try {
                    for(AgricultureRow r:rows){
                        if(empty(r.worker)&&empty(r.item)&&empty(r.quantity)&&empty(r.price))continue;
                        double q=Double.parseDouble(val(r.quantity));long p=Long.parseLong(val(r.price));
                        String category=r.category.getSelectedItem().toString();
                        String[] defaults=db.defaultResponsibility(category);
                        db.addCost(val(date),val(r.worker),category,val(r.item),q,p,"التاجر",defaults[0],defaults[2],defaults[3],"","مدخل زراعي تفصيلي");
                    }
                    writable.setTransactionSuccessful();
                } finally { writable.endTransaction(); }
                toast("تم حفظ "+count+" مدخل زراعي في عملية واحدة");
                showAgricultureInputs();
            }catch(Exception ex){toast("لم تحفظ المدخلات. أكمل الحقول وتحقق من الكميات والأسعار ثم أعد المحاولة.");}
        });
        addBack();
    }

    private void updateAgricultureSummary(List<AgricultureRow> rows,TextView summary) {
        long total=0;
        for(AgricultureRow row:rows)try{
            double quantity=empty(row.quantity)?0:Double.parseDouble(val(row.quantity));
            long price=empty(row.price)?0:Long.parseLong(val(row.price));
            if(quantity>0&&price>=0)total=Math.addExact(total,Math.round(quantity*price));
        }catch(Exception ignored){}
        summary.setText("إجمالي المدخلات الزراعية: "+money.format(total)+" ج.س");
    }

    private void addAgricultureRow(LinearLayout parent,List<AgricultureRow> rows,TextView summary) {
        AgricultureRow r=new AgricultureRow();LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(10,8,10,8);card.setBackground(rounded(Color.WHITE,12,Color.rgb(218,228,216)));
        r.category=spinner(new String[]{"التقاوي","الأسمدة","المبيدات","تحضير الأرض","الري وخدمة الحقل","الوقود والترحيل","أجور العمال","أخرى"});
        r.worker=workerInput("اسم التربال",-1);r.item=field("اسم الصنف / البيان",false);r.quantity=field("الكمية",false);r.quantity.setInputType(8194);
        r.price=field("سعر الوحدة بالجنيه",false);r.price.setInputType(2);r.total=text("الإجمالي: 0 ج.س",15,GREEN,true);
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int count,int after){}public void onTextChanged(CharSequence x,int st,int before,int count){
            try{double q=empty(r.quantity)?0:Double.parseDouble(val(r.quantity));long p=empty(r.price)?0:Long.parseLong(val(r.price));r.total.setText("الإجمالي: "+money.format(Math.round(q*p))+" ج.س");}catch(Exception ex){r.total.setText("تحقق من الكمية والسعر");} updateAgricultureSummary(rows,summary);
        }public void afterTextChanged(Editable e){}};
        r.quantity.addTextChangedListener(watcher);r.price.addTextChangedListener(watcher);
        Button remove=button("حذف هذا الصنف");remove.setTextSize(12);
        remove.setOnClickListener(v->{rows.remove(r);parent.removeView(card);updateAgricultureSummary(rows,summary);});
        card.addView(text("بند زراعي جديد",14,DARK,true));card.addView(r.category);card.addView(r.worker);card.addView(r.item);card.addView(r.quantity);card.addView(r.price);card.addView(r.total);card.addView(remove);
        parent.addView(card,margin(-1,-2,0,0,0,10));rows.add(r);updateAgricultureSummary(rows,summary);
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
        EditText worker=workerInput("اسم التربال",-1);
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
        long totalNet=0,totalShared=0,totalWorkerDeductions=0,totalWorkerHalfShare=0,totalWorkerShare=0;
        tableHeader(new String[]{"التربال","صافي الفواتير","التكلفة المشتركة","نصيب التربال قبل الخصم","خصومات التربال","الصافي المستحق"});
        try(Cursor names=db.workerNames()){
            while(names.moveToNext()){
                String worker=names.getString(0);
                long net=db.netSalesForWorker(worker);
                long shared=db.sumCostsForWorker(worker,SHARED_CATEGORIES);
                long deductions=db.sumCostsForWorker(worker,WORKER_DEDUCTIONS);
                SettlementMath.Share share=SettlementMath.calculateWorkerShare(net,shared,deductions);
                long half=share.halfShare;
                long due=share.netDue;
                tableRow(new String[]{worker,money.format(net),money.format(shared),money.format(half),money.format(deductions),money.format(due)});
                totalNet=Math.addExact(totalNet,net);totalShared=Math.addExact(totalShared,shared);totalWorkerDeductions=Math.addExact(totalWorkerDeductions,deductions);totalWorkerHalfShare=Math.addExact(totalWorkerHalfShare,half);totalWorkerShare=Math.addExact(totalWorkerShare,due);
            }
        }
        content.addView(text("ملخص المشروع",18,GREEN,true));
        reportLine("صافي جميع الفواتير بعد تكلفة الرحلات",totalNet);
        reportLine("إجمالي التحضير والتقاوي والسماد والمبيد",totalShared);
        reportLine("مجموع أنصبة الترابلة قبل الخصومات",totalWorkerHalfShare);
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

    private void addBack() { /* Navigation button is injected once by base() on every working screen. */ }

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
