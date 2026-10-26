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
import android.graphics.drawable.GradientDrawable;
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
import java.security.SecureRandom;
import java.security.MessageDigest;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import android.util.Base64;

public class MainActivity extends Activity {
    private static final int NAVY = Color.rgb(11,31,58), GOLD = Color.rgb(212,175,55), BG = Color.rgb(246,248,251);
    private static final String SUPPORT = "249121851285", BANKAK = "1882224";
    // Public verification key only. The corresponding private key is stored as a GitHub Actions secret.
    private static final String LICENSE_PUBLIC_KEY_B64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAqlGWpIPqSpXdEP67uticpomJ5nKGkHrZkKDgnueolx8cFKyov/PydmJuAnxUaRnggcNte4/PfGzwrXtmoJ6hrpXfYUVjyL3UZ9MdCmPKevrdBftzWYoJ6NG2Dda6ZdlQoDdD/Mads1uJYACKxbipsgWSUr9MzmJdmTSYexDHBS6MWMPyjK+L60Jy3sn6k85bPE+AnPtOW38/no2ucz6h8kANgpd/4XDusR07l6EYOSg8TXuVny6SKUzH6PWf/TC6fXt2erASsPkvc5MehBMI9vXCTpj0PGZikAdyj/Hw6IKyr5HmG8vLgzofB1AfXPgIrJdhGMhdsUn8iD+4ZLBucQIDAQAB";
    private static final int REQUEST_EXPORT_BACKUP = 4250, REQUEST_IMPORT_BACKUP = 4251, REQUEST_PAYMENT_RECEIPT = 4252, REQUEST_COMPANY_LOGO = 4253;
    private String pendingActivationMessage = "";
    private Store store;
    private LinearLayout root, content;
    private String day;
    private TextView dateLabel, statusLabel, flourMetric, workMetric, productionMetric, marginMetric;

    private static class Field {
        final String key, label, hint; final boolean numeric;
        Field(String k, String l, String h, boolean n) { key=k; label=l; hint=h; numeric=n; }
    }
    private static Field f(String k, String l) { return new Field(k,l,"",true); }
    private static Field t(String k, String l, String hint) { return new Field(k,l,hint,false); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            store = new Store();
            day = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            showPinGate();
        } catch (RuntimeException ex) {
            android.util.Log.e("AlMizanBakery", "Activity startup failed", ex);
            showStartupFailure(ex);
        }
    }

    private void showPinGate() {
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        if (prefs.getString("pin_hash", null) == null || prefs.getString("pin_salt", null) == null) {
            showPinSetup();
        } else {
            showPinLogin();
        }
    }

    private void continueAfterPinAccepted() {
        try {
            if (!hasActiveLicense() && trialExpired()) { showPaywall(); return; }
            buildHome();
        } catch (RuntimeException ex) {
            android.util.Log.e("AlMizanBakery", "Failed to open home after login", ex);
            showStartupFailure(ex);
        }
    }

    private void showStartupFailure(RuntimeException ex) {
        final String report = "تعذر فتح الصفحة الرئيسية بعد تسجيل الدخول.\n" +
            ex.getClass().getName() + ": " + String.valueOf(ex.getMessage()) +
            "\n\n" + android.util.Log.getStackTraceString(ex);
        base("⚠️ تعذر فتح التطبيق");
        content.addView(text("تم تسجيل الخطأ بدل إغلاق التطبيق فجأة. لم تُحذف بيانات الدفاتر. أرسل التقرير للدعم لتحديد السبب بدقة.", 16));
        Button send = button("إرسال تقرير الخطأ عبر واتساب");
        send.setOnClickListener(v -> openWhatsApp("تقرير تعطل المحاسب الميزان للمخابز " + BuildConfig.VERSION_NAME + "\n" + report));
        content.addView(send);
        Button retry = button("إعادة محاولة فتح الصفحة الرئيسية");
        retry.setOnClickListener(v -> continueAfterPinAccepted());
        content.addView(retry);
    }

    private void showPinSetup() {
        LinearLayout box = new LinearLayout(this);
        box.setPadding(24, 12, 24, 4);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView instructions = text("أنشئ كلمة مرور رقمية لا تقل عن 6 أرقام. احفظها جيدًا؛ لا تُرسلها للدعم ولا تضعها في واتساب.", 15);
        box.addView(instructions);
        EditText pin = new EditText(this);
        pin.setHint("كلمة المرور الجديدة — 6 أرقام على الأقل");
        pin.setSingleLine(true);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        box.addView(pin);
        EditText confirm = new EditText(this);
        confirm.setHint("تأكيد كلمة المرور");
        confirm.setSingleLine(true);
        confirm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        box.addView(confirm);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("🔐 إعداد كلمة مرور المخبز")
            .setView(box).setCancelable(false)
            .setPositiveButton("حفظ كلمة المرور", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String p1 = normalizePin(pin.getText().toString());
            String p2 = normalizePin(confirm.getText().toString());
            if (p1.length() < 6 || !p1.matches("[0-9]+")) {
                pin.setError("أدخل 6 أرقام على الأقل");
                return;
            }
            if (!p1.equals(p2)) {
                confirm.setError("كلمتا المرور غير متطابقتين");
                return;
            }
            try {
                byte[] salt = new byte[16];
                new SecureRandom().nextBytes(salt);
                byte[] hash = derivePinHash(p1, salt);
                getPreferences(MODE_PRIVATE).edit()
                    .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                    .apply();
                dialog.dismiss();
                toast("تم إنشاء كلمة المرور. احفظها في مكان آمن.");
                continueAfterPinAccepted();
            } catch (Exception ex) {
                pin.setError("تعذر حفظ كلمة المرور. أعد المحاولة.");
            }
        }));
        dialog.show();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private void showPinLogin() {
        EditText pin = new EditText(this);
        pin.setHint("أدخل كلمة المرور الرقمية");
        pin.setSingleLine(true);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(24, 8, 24, 4);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text("افتح دفاتر المخبز بكلمة المرور التي أنشأتها عند أول تشغيل.", 15));
        box.addView(pin);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("🔐 تسجيل الدخول")
            .setView(box).setCancelable(false)
            .setPositiveButton("دخول", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = normalizePin(pin.getText().toString());
            if (entered.length() < 6 || !entered.matches("[0-9]+")) {
                pin.setError("أدخل كلمة المرور المكوّنة من 6 أرقام على الأقل");
                return;
            }
            try {
                android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
                byte[] salt = Base64.decode(prefs.getString("pin_salt", ""), Base64.NO_WRAP);
                byte[] expected = Base64.decode(prefs.getString("pin_hash", ""), Base64.NO_WRAP);
                byte[] actual = derivePinHash(entered, salt);
                if (!MessageDigest.isEqual(expected, actual)) {
                    pin.setText("");
                    pin.setError("كلمة المرور غير صحيحة");
                    return;
                }
                dialog.dismiss();
                continueAfterPinAccepted();
            } catch (Exception ex) {
                pin.setError("تعذر التحقق. أعد المحاولة.");
            }
        }));
        dialog.show();
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private String normalizePin(String input) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '٠' && c <= '٩') out.append((char)('0' + c - '٠'));
            else if (c >= '۰' && c <= '۹') out.append((char)('0' + c - '۰'));
            else out.append(c);
        }
        return out.toString();
    }

    private byte[] derivePinHash(String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(pin.toCharArray(), salt, 120000, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private String deviceId() {
        android.content.SharedPreferences prefs = getPreferences(0);
        String id = prefs.getString("license_device_id", null);
        if (id == null || id.trim().isEmpty()) {
            id = java.util.UUID.randomUUID().toString();
            prefs.edit().putString("license_device_id", id).apply();
        }
        return id;
    }

    private boolean hasActiveLicense() {
        String token = getPreferences(0).getString("license_token", null);
        return token != null && verifyLicenseToken(token);
    }

    private boolean verifyLicenseToken(String token) {
        try {
            String[] parts = token.trim().split("\\.", -1);
            if (parts.length != 2) return false;
            int flags = Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING;
            byte[] payloadBytes = Base64.decode(parts[0], flags);
            byte[] signatureBytes = Base64.decode(parts[1], flags);
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            java.security.PublicKey key = java.security.KeyFactory.getInstance("RSA").generatePublic(
                new java.security.spec.X509EncodedKeySpec(Base64.decode(LICENSE_PUBLIC_KEY_B64, Base64.NO_WRAP)));
            java.security.Signature verifier = java.security.Signature.getInstance("SHA256withRSA");
            verifier.initVerify(key);
            verifier.update(payloadBytes);
            if (!verifier.verify(signatureBytes)) return false;
            java.util.Map<String,String> fields = new java.util.HashMap<>();
            for (String field : payload.split(";")) {
                int at = field.indexOf('=');
                if (at <= 0) return false;
                fields.put(field.substring(0, at), field.substring(at + 1));
            }
            if (!"com.mohammedmustafa.almizan.bakery".equals(fields.get("app"))) return false;
            if (!deviceId().equals(fields.get("device"))) return false;
            String plan = fields.get("plan");
            if (!"monthly".equals(plan) && !"annual".equals(plan) && !"lifetime".equals(plan)) return false;
            String maxVersion = fields.get("max_version");
            if ("lifetime".equals(plan)) {
                if (!BuildConfig.VERSION_NAME.equals(maxVersion)) return false;
            } else if (!"any".equals(maxVersion)) {
                return false;
            }
            long expires = Long.parseLong(fields.get("expires"));
            if (expires == 0L) return true;
            android.content.SharedPreferences prefs = getPreferences(0);
            long now = System.currentTimeMillis() / 1000L;
            long lastSeen = prefs.getLong("license_last_seen", 0L);
            if (lastSeen > 0L && now < lastSeen - 300L) return false;
            prefs.edit().putLong("license_last_seen", Math.max(lastSeen, now)).apply();
            return now <= expires;
        } catch (Exception ex) {
            android.util.Log.w("AlMizanBakery", "Activation token rejected", ex);
            return false;
        }
    }

    private void showActivationDialog() {
        EditText code = new EditText(this);
        code.setHint("ألصق رمز التفعيل الذي أرسله الدعم");
        code.setMinLines(3);
        code.setGravity(Gravity.TOP | Gravity.START);
        code.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("🔑 تفعيل الاشتراك")
            .setMessage("بعد مراجعة التحويل، يصلك رمز موقّع مرتبط بهذا الجهاز والباقة. أدخله هنا؛ لا ترسل الرمز السري لبنكك.")
            .setView(code).setNegativeButton("إلغاء", null)
            .setPositiveButton("تفعيل", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String token = code.getText().toString().replaceAll("\\s+", "");
            if (!verifyLicenseToken(token)) {
                code.setError("رمز التفعيل غير صحيح أو لا يخص هذا الجهاز أو انتهت صلاحيته.");
                return;
            }
            getPreferences(0).edit().putString("license_token", token).apply();
            dialog.dismiss();
            toast("تم التحقق من رمز التفعيل.");
            buildHome();
        }));
        dialog.show();
    }

    private boolean trialExpired() {
        android.content.SharedPreferences prefs = getPreferences(0);
        long now = System.currentTimeMillis();
        long start = prefs.getLong("trial_start", 0L);
        long lastSeen = prefs.getLong("trial_last_seen", 0L);
        if (start <= 0L) {
            prefs.edit().putLong("trial_start", now).putLong("trial_last_seen", now).apply();
            return false;
        }
        // Prevent a backwards device-clock change from silently extending the offline trial.
        if (lastSeen > 0L && now < lastSeen - 300000L) {
            prefs.edit().putBoolean("trial_clock_rollback_detected", true).apply();
        }
        if (prefs.getBoolean("trial_clock_rollback_detected", false)) return true;
        prefs.edit().putLong("trial_last_seen", Math.max(lastSeen, now)).apply();
        return now - start >= 7L * 24L * 60L * 60L * 1000L;
    }

    private GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * getResources().getDisplayMetrics().density);
        return d;
    }

    private void base(String title) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(BG);
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setPadding(16,14,16,14);
        head.setBackground(rounded(NAVY, 0));
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.ic_bakery);
        logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        logo.setPadding(2,2,2,2);
        logo.setBackground(rounded(NAVY,12));
        head.addView(logo, new LinearLayout.LayoutParams(60,60));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView name = new TextView(this);
        name.setText(title);
        name.setTextSize(19);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null,1);
        TextView subtitle = new TextView(this);
        subtitle.setText("إدارة المخبز • المخزون • التشغيل • الربحية");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(236,226,190));
        titles.addView(name);
        titles.addView(subtitle);
        head.addView(titles,new LinearLayout.LayoutParams(0,-2,1f));
        head.setElevation(5f);
        root.addView(head);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(14,14,14,20);
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);
    }

    private void buildHome() {
        base("🥖 المحاسب الميزان للمخابز " + BuildConfig.VERSION_NAME + "");
        TextView intro = text("منظومة يومية متكاملة للمخبز — من رصيد الدقيق إلى الإنتاج والتحصيل وهامش الربح.",16);
        content.addView(intro);
        addBakeryHero();
        LinearLayout dateRow = new LinearLayout(this); dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateLabel = text("📅 التاريخ: " + day,16); dateRow.addView(dateLabel,new LinearLayout.LayoutParams(0,-2,1f));
        Button choose = button("اختيار التاريخ"); choose.setOnClickListener(v -> chooseDate()); dateRow.addView(choose); content.addView(dateRow);
        statusLabel = text("",14); content.addView(statusLabel);
        addDashboardSummary();
        addCard("📦 1. المخزن","أرصدة الدقيق والخميرة والأكياس والزيت والملح والوقود والمعدات. يظهر رصيد اليوم السابق تلقائيًا.",()->openModule("stock"));
        addCard("🥖 2. شغل اليوم والإنتاج","عدد جوالات الدقيق المستخدمة، عدد الأرغفة لكل جوال، وقيمة الإنتاج بالجنيه. يُخصم الدقيق تلقائيًا من المخزن.",()->openModule("cuts"));
        addCard("👷 3. يومية العمال","اسم العامل، الوظيفة، عدد الشغل، عمالة الجوال، وجملة العمل",()->openModule("workers"));
        addCard("💵 4. الكاش وبيع الشباك","النقد، الديون، التحويل البنكي ورقم العملية",()->openModule("window"));
        addCard("🚚 5. التوزيع","اسم العميل، الكمية، الكاش، الديون، التحويل وفرق السعر",()->openModule("distribution"));
        addCard("🧾 6. منصرفات المحل","وجبات العمال، الصيانة، المشتريات والمنصرفات الأخرى",()->openModule("expenses"));
        addCard("♻️ 7. التالف","عدد العيش التالف وسبب التلف",()->openModule("spoilage"));
        addCard("🧮 8. المحاسب والحساب","جمع القطعيات ومراجعة الكاش والديون والتحويلات واليوميات والمنصرفات والتالف",this::showAccounts);
        addCard("🏭 9. حساب التشغيل والتكلفة","تكلفة الوقود والدقيق والخميرة والزيت والملح والأكياس، وتقدير هامش الربح",this::showCosting);
        Button backup = button("🗂️ نسخ احتياطي واستعادة البيانات"); backup.setOnClickListener(v->backupRestoreMenu()); content.addView(backup);
        Button guide = button("📘 الدليل الإرشادي الشامل"); guide.setOnClickListener(v->showGuide()); content.addView(guide);
        Button faq = button("❓ الأسئلة الشائعة وحلول المشكلات"); faq.setOnClickListener(v->showFaq()); content.addView(faq);
        Button update = button("⬆️ التحقق من التحديثات وتنزيل أحدث إصدار");
        update.setOnClickListener(v -> showUpdateDialog());
        content.addView(update);
        Button pricing = button("🔐 الاشتراك والأسعار والتفعيل"); pricing.setOnClickListener(v->showPaywall()); content.addView(pricing);
        Button support = button("🟢 الدعم الفني والتفعيل عبر واتساب"); support.setOnClickListener(v->openWhatsApp("السلام عليكم، أحتاج دعمًا فنيًا لتطبيق المحاسب الميزان للمخابز " + BuildConfig.VERSION_NAME + ".")); content.addView(support);
        updateStatus();
    }

    private void addBakeryHero() {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(18,16,18,16);
        android.graphics.drawable.GradientDrawable background =
            new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[]{NAVY, Color.rgb(25,58,88)});
        background.setCornerRadius(18 * getResources().getDisplayMetrics().density);
        background.setStroke(Math.max(1,(int)(1.5f*getResources().getDisplayMetrics().density)), GOLD);
        hero.setBackground(background);
        hero.setElevation(4f);

        android.widget.ImageView emblem = new android.widget.ImageView(this);
        emblem.setImageResource(R.drawable.ic_bakery);
        emblem.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams emblemLp = new LinearLayout.LayoutParams(76,76);
        emblemLp.gravity = Gravity.CENTER_HORIZONTAL;
        emblemLp.bottomMargin = 8;
        hero.addView(emblem, emblemLp);

        TextView brand = text("🌾  سنابل القمح والخبز  •  رغيف اليوم بميزان دقيق",19);
        brand.setTextColor(Color.WHITE);
        brand.setTypeface(null,1);
        hero.addView(brand);
        TextView sub = text("المخزن  ←  القطعيات  ←  العمال  ←  التحصيل  ←  التسوية",13);
        sub.setTextColor(Color.rgb(239,229,195));
        hero.addView(sub);

        TextView trial = text(hasActiveLicense()
            ? "✓ الترخيص مفعّل على هذا الجهاز"
            : "🎁 التجربة المجانية: " + trialDaysRemaining() + " يوم متبقٍ — البيانات محفوظة محليًا",12);
        trial.setTextColor(Color.WHITE);
        hero.addView(trial);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button stock = button("📦 المخزن");
        stock.setOnClickListener(v -> openModule("stock"));
        Button work = button("🥖 شغل اليوم");
        work.setOnClickListener(v -> openModule("cuts"));
        LinearLayout.LayoutParams actionLp = new LinearLayout.LayoutParams(0,-2,1f);
        actionLp.setMargins(3,6,3,0);
        actions.addView(stock, actionLp);
        actions.addView(work, actionLp);
        hero.addView(actions);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,6,0,12);
        content.addView(hero,lp);
    }

    private void addDashboardSummary() {
        content.addView(text("📊 ملخص المخبز لهذا اليوم",18));
        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        addMetricTile(first, "flour", "📦 رصيد الدقيق", fmt(stockFlourValue()) + " جوال", "المتبقي بعد خصم شغل اليوم");
        addMetricTile(first, "work", "🥖 شغل اليوم", fmt(val("cuts","work_units")) + " جوال", "خصم آلي من الرصيد");
        content.addView(first);
        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);
        long production = 0L, margin = 0L;
        try {
            production = productionValue();
            margin = BakeryMath.netMargin(salesRevenue(), spoilageCost(), wages(), expenseTotal(), costingTotal(), distributionDiff());
        } catch (Exception ignored) { /* keep the dashboard available if a daily record needs correction */ }
        addMetricTile(second, "production", "💵 قيمة الإنتاج", fmt(production) + " ج", "بحسب سعر القطعيات");
        addMetricTile(second, "margin", "📈 هامش الربح", fmt(margin) + " ج", "تقديري بعد التكاليف");
        content.addView(second);
    }

    private void addMetricTile(LinearLayout row, String key, String heading, String value, String caption) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(10,10,10,10);
        tile.setBackground(rounded(Color.WHITE,16));
        tile.setElevation(2f);
        TextView h = text(heading,13);
        h.setTextColor(NAVY);
        h.setTypeface(null,1);
        TextView v = text(value,18);
        v.setTextColor(NAVY);
        v.setTypeface(null,1);
        TextView note = text(caption,11);
        note.setTextColor(Color.rgb(91,101,116));
        tile.addView(h);
        tile.addView(v);
        tile.addView(note);
        if ("flour".equals(key)) flourMetric = v;
        else if ("work".equals(key)) workMetric = v;
        else if ("production".equals(key)) productionMetric = v;
        else if ("margin".equals(key)) marginMetric = v;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,-2,1f);
        lp.setMargins(4,4,4,8);
        row.addView(tile,lp);
    }

    private void addCard(String title, String description, Runnable action) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(16,14,16,14);
        card.setBackground(rounded(Color.WHITE,18));
        card.setElevation(3f);
        TextView h = text(title,18);
        h.setTextColor(NAVY);
        h.setTypeface(null,1);
        card.addView(h);
        TextView desc = text(description,14);
        desc.setTextColor(Color.rgb(78,89,105));
        card.addView(desc);
        Button open = button("فتح القسم  ←");
        open.setOnClickListener(v->action.run());
        card.addView(open);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.setMargins(0,7,0,7);
        content.addView(card,lp);
    }

    private TextView text(String value, int size) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(Color.rgb(31,41,55));
        v.setPadding(7,8,7,8); return v;
    }
    private Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setAllCaps(false);
        b.setTextColor(NAVY);
        b.setTextSize(15);
        b.setTypeface(null,1);
        b.setMinHeight(48);
        b.setBackground(rounded(GOLD,14));
        b.setElevation(2f);
        return b;
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
                t("items_rows","جدول المخزون — الصنف | الكمية والوحدة | سعر الوحدة بالجنيه",
                    "دقيق | 50 جوال | 60000\nملح | 25 كيلو | 2000\nخميرة | 45 باكو | 6000\nزيت | 108 رطل | 5000\nأكياس | 50 لفة | 2000\nجاز | 0 جالون | 0\nغاز | 0 أسطوانة | 0\nحطب | 0 قنطار | 0\nأضف كل صنف في سطر مستقل: الصنف | الكمية والوحدة | سعر الوحدة"),
                t("equipment","المعدات (اختياري)","اسم المعدة وحالتها"),
                t("notes","ملاحظات المخزن","")
            };
            case "cuts": return new Field[]{
                f("work_units","عدد جوالات الدقيق المستخدمة في شغل اليوم"),
                t("production_rows","القطعيات — رقم الجوال | عدد الأرغفة المنتجة","1 | 700\n2 | 800\n3 | 650\nكل جوال في سطر مستقل"),
                f("loaves_per_1000","عدد الأرغفة التي تساوي 1,000 جنيه")
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
                t("rows","التوزيع — العميل | عدد الأرغفة | نقداً | ديون | بنكك | فرق السعر","أحمد | 100 | 19000 | 0 | 3000 | 0\nبقالة النيل | 200 | 44000 | 0 | 0 | 0\nكل عميل في سطر مستقل. سجّل مبلغ كل عملية في وسيلة التحصيل الفعلية فقط، ولا تكرر المبلغ."),
                t("notes","ملاحظات التوزيع","")
            };
            case "expenses": return new Field[]{
                f("meals","وجبات العمال (جنيه)"),f("maintenance","الصيانة (جنيه)"),f("purchases","المشتريات (جنيه)"),
                t("rows","منصرفات أخرى — سطر لكل منصرف","البيان | المبلغ\nمثال: ماء | 2500"),
                t("notes","ملاحظات المنصرفات","")
            };
            case "spoilage": return new Field[]{f("loaves","عدد العيش التالف"),t("reason","سبب التلف / الملاحظات","")};
            case "costing": return new Field[]{
                f("unit_flour","دقيق — تكلفة الدقيق لكل وحدة شغل (جنيه)"),f("unit_yeast","خميرة — تكلفة الخميرة لكل وحدة شغل (جنيه)"),
                f("unit_oil","زيت — تكلفة الزيت لكل وحدة شغل (جنيه)"),f("unit_bags","أكياس — تكلفة الأكياس لكل وحدة شغل (جنيه)"),
                f("unit_salt","ملح — تكلفة الملح لكل وحدة شغل (جنيه)"),f("unit_wood","حطب — تكلفة الحطب لكل وحدة شغل (جنيه)"),
                f("unit_gas","غاز — تكلفة الغاز لكل وحدة شغل (جنيه)"),f("unit_diesel","جاز — تكلفة الجاز لكل وحدة شغل (جنيه)"),
                f("unit_other1","تكلفة أخرى 1 لكل وحدة شغل (جنيه)"),f("unit_other2","تكلفة أخرى 2 لكل وحدة شغل (جنيه)"),
                f("unit_other3","تكلفة أخرى 3 لكل وحدة شغل (جنيه)")
            };
            default: return new Field[0];
        }
    }

    private String titleFor(String module) {
        switch(module) {
            case "stock": return "المخزن"; case "cuts": return "القطعيات"; case "workers": return "يومية العمال";
            case "window": return "الكاش وبيع الشباك"; case "distribution": return "التوزيع";
            case "expenses": return "منصرفات المحل"; case "spoilage": return "التالف"; case "costing": return "حساب التشغيل والتكلفة"; default: return module;
        }
    }

    private EditText inventoryCell(String value, String hint, boolean numeric) {
        EditText e = new EditText(this);
        e.setText(value);
        e.setHint(hint);
        e.setTextSize(14);
        e.setSingleLine(true);
        e.setPadding(6,8,6,8);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        GradientDrawable border = rounded(Color.WHITE,4);
        border.setStroke(Math.max(1,(int)(1f*getResources().getDisplayMetrics().density)),Color.rgb(150,165,181));
        e.setBackground(border);
        return e;
    }

    private void addInventoryRow(LinearLayout table, java.util.List<EditText[]> rows, String item, String quantity, String consumption, String price) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText name = inventoryCell(item,"الصنف",false);
        EditText qty = inventoryCell(quantity,"الرصيد + الوحدة",false);
        EditText used = inventoryCell(consumption,"استهلاك/جوال",true);
        EditText unitPrice = inventoryCell(price,"سعر الوحدة",true);
        EditText[] cells={name,qty,used,unitPrice};
        for(EditText e:cells) {
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
            lp.setMargins(2,2,2,2);
            row.addView(e,lp);
        }
        table.addView(row,new LinearLayout.LayoutParams(-1,-2));
        rows.add(cells);
    }

    private String normalizeRows(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        // Recover rows saved by older builds with literal backslash-n separators.
        return raw.replace("\\n", "\n").replace("\r\n", "\n").replace("\r", "\n");
    }

    private String[] stockParts(String line) {
        String[] p=line.split("\\|",-1);
        if(p.length>=4) return new String[]{p[0].trim(),p[1].trim(),p[2].trim(),p[3].trim()};
        if(p.length==3) {
            String name=p[0].trim();
            String rate=(name.contains("دقيق") || name.equalsIgnoreCase("flour"))?"1":"0";
            return new String[]{name,p[1].trim(),rate,p[2].trim()};
        }
        return new String[]{line.trim(),"0 وحدة","0","0"};
    }

    private void openStockTable() {
        JSONObject saved=store.get(day,"stock");
        JSONObject current=store.get(day,"stock");
        if(current.length()==0) {
            JSONObject previous=store.getLatest(day,"stock");
            if(previous.length()>0) saved=previous;
        }
        try {
            String raw=saved.optString("items_rows","").trim();
            if(raw.isEmpty()) raw=legacyStockRows(saved);
            long flourQty=stockFlourQuantity(saved);
            raw=setStockFlourQuantity(raw,flourQty);
            if(current.length()>0) {
                JSONObject applied=store.readMaterialUsage(day);
                if(applied.length()==0 && store.usageForDate(day)>0) applied.put("دقيق",store.usageForDate(day));
                raw=store.adjustMaterialRows(raw,applied,true);
            }
            saved.put("items_rows",raw);
        } catch(Exception ex) { toast("تعذر تجهيز المخزون للتحرير؛ لم تُمسح أي بيانات."); return; }

        ScrollView scroll=new ScrollView(this);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(8,8,8,12); scroll.addView(box);
        box.addView(text("أدخل رصيد كل مادة، وكمية استهلاكها مع جوال دقيق واحد، وسعر وحدة الاستهلاك. يُخصم الاستهلاك تلقائيًا عند حفظ شغل اليوم.",14));
        LinearLayout table=new LinearLayout(this); table.setOrientation(LinearLayout.VERTICAL); table.setPadding(2,2,2,2); table.setBackground(rounded(Color.rgb(222,229,237),4));
        LinearLayout header=new LinearLayout(this); header.setOrientation(LinearLayout.HORIZONTAL); header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] headings={"الصنف","الرصيد والوحدة","استهلاك/جوال","سعر الوحدة"};
        for(String heading:headings) {
            TextView h=text(heading,12); h.setTextColor(Color.WHITE); h.setTypeface(null,1); h.setGravity(Gravity.CENTER); h.setBackground(rounded(NAVY,3));
            LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-2,1f); hp.setMargins(2,2,2,2); header.addView(h,hp);
        }
        table.addView(header);
        java.util.List<EditText[]> rows=new java.util.ArrayList<>();
        for(String line:normalizeRows(saved.optString("items_rows","")).split("\\r?\\n")) {
            if(line.trim().isEmpty()) continue;
            String[] p=stockParts(line);
            addInventoryRow(table,rows,p[0],p[1],p[2],p[3]);
        }
        if(rows.isEmpty()) {
            String[][] defaults={{"دقيق","50 جوال","1","60000"},{"ملح","25 كيلو","0","2000"},{"خميرة","45 باكو","0","6000"},{"زيت","108 رطل","0","5000"},{"أكياس","50 لفة","0","2000"},{"جاز","0 جالون","0","0"},{"غاز","0 أسطوانة","0","0"},{"حطب","0 قنطار","0","0"}};
            for(String[] item:defaults) addInventoryRow(table,rows,item[0],item[1],item[2],item[3]);
        }
        box.addView(table);
        Button add=button("＋ إضافة مادة للمخزون"); add.setOnClickListener(v->addInventoryRow(table,rows,"","","","")); box.addView(add);
        box.addView(text("المعدات (اختياري)",14)); EditText equipment=inventoryCell(saved.optString("equipment",""),"اسم المعدة وحالتها",false); box.addView(equipment);
        box.addView(text("ملاحظات المخزن",14)); EditText notes=inventoryCell(saved.optString("notes",""),"ملاحظات",false); box.addView(notes);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("📦 المخزن والتكلفة حسب الاستهلاك — "+day).setView(scroll).setNegativeButton("إلغاء",null).setPositiveButton("حفظ جدول المخزون",null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                StringBuilder textRows=new StringBuilder();
                for(EditText[] cells:rows) {
                    String name=cells[0].getText().toString().trim(), qty=cells[1].getText().toString().trim(),
                        consumption=cells[2].getText().toString().trim(), price=cells[3].getText().toString().trim();
                    if(name.isEmpty()&&qty.isEmpty()&&consumption.isEmpty()&&price.isEmpty()) continue;
                    if(textRows.length()>0) textRows.append("\n");
                    textRows.append(name).append(" | ").append(qty).append(" | ").append(consumption).append(" | ").append(price);
                }
                JSONObject record=new JSONObject(); record.put("items_rows",textRows.toString());
                record.put("equipment",equipment.getText().toString().trim()); record.put("notes",notes.getText().toString().trim());
                validateStockRows(record.optString("items_rows",""));
                record.put("flour",String.valueOf(stockFlourQuantity(record)));
                store.saveStockWithInventory(day,record); updateStatus(); dialog.dismiss();
                toast("حُفظ جدول المواد ومعدلات الاستهلاك. سيُخصم الاستخدام عند حفظ شغل اليوم.");
            } catch(Exception ex) { toast("لم يُحفظ المخزون: "+ex.getMessage()); }
        }));
        dialog.show();
    }

    private void openModule(String module) {
        if (module.equals("stock")) { openStockTable(); return; }
        Field[] fields = schema(module);
        JSONObject saved = store.get(day,module);
        if (module.equals("stock")) {
            JSONObject currentStock = store.get(day,"stock");
            if (currentStock.length() == 0) {
                JSONObject previous = store.getLatest(day,"stock");
                if (previous.length() > 0) saved = previous;
            }
            try {
                if (saved.optString("items_rows","").trim().isEmpty())
                    saved.put("items_rows", legacyStockRows(saved));
                long flourQty = stockFlourQuantity(saved);
                long appliedUsage = currentStock.length() > 0 ? store.usageForDate(day) : 0L;
                if (appliedUsage > 0) flourQty = Math.addExact(flourQty, appliedUsage);
                saved.put("flour", String.valueOf(flourQty));
                saved.put("items_rows", setStockFlourQuantity(saved.optString("items_rows",""), flourQty));
            } catch (Exception ex) {
                toast("تعذر تجهيز سجل المخزن للتحرير. لا تحفظ التغييرات حتى مراجعة البيانات.");
            }
        }
        if (module.equals("costing") && val("cuts","work_units") > 0) {
            try { saved.put("flour_bags", String.valueOf(val("cuts","work_units"))); } catch(Exception ignored) {}
        }
        ScrollView sc = new ScrollView(this); LinearLayout box = new LinearLayout(this);
        box.setPadding(14,8,14,12); box.setOrientation(LinearLayout.VERTICAL); sc.addView(box);
        final java.util.Map<String,EditText> inputs = new java.util.LinkedHashMap<>();
        for(Field field:fields) {
            box.addView(text(field.label,15));
            EditText e = new EditText(this); e.setTextSize(16); e.setHint(field.hint); e.setPadding(12,10,12,10);
            GradientDrawable inputBorder = rounded(Color.WHITE,8);
            inputBorder.setStroke(Math.max(1,(int)(1f*getResources().getDisplayMetrics().density)),Color.rgb(177,188,201));
            e.setBackground(inputBorder);
            LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1,-2);
            inputLp.setMargins(0,2,0,10);
            e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            if(field.numeric) { e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED); e.setSingleLine(true); }
            else { e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); e.setMinLines(field.key.equals("items_rows")?8:field.key.equals("production_rows")?5:field.key.equals("rows")?4:1); e.setGravity(Gravity.TOP|Gravity.START); }
            if (module.equals("costing") && field.key.equals("flour_bags")) { e.setEnabled(false); e.setAlpha(0.75f); }
            e.setText(saved.optString(field.key,"")); inputs.put(field.key,e); box.addView(e,inputLp);
        }
        new AlertDialog.Builder(this).setTitle(titleFor(module)+" — "+day).setView(sc)
            .setNegativeButton("إلغاء",null).setNeutralButton("مسح خانات هذا القسم",(d,w)->{
                new AlertDialog.Builder(this).setMessage("هل تريد مسح بيانات هذا القسم لهذا التاريخ؟").setNegativeButton("لا",null)
                    .setPositiveButton("نعم، امسح القسم",(dd,ww)->{
                        if ((module.equals("stock") || module.equals("cuts")) && store.hasFutureStockAfter(day)) {
                            toast("لا يمكن تعديل أو مسح مخزون/شغل يوم سابق بعد تسجيل أرصدة لأيام لاحقة؛ راجع الأيام اللاحقة يدويًا قبل التصحيح.");
                            return;
                        }
                        if (module.equals("stock") && store.usageForDate(day) > 0) {
                            toast("لا يمكن مسح المخزن بعد خصم شغل اليوم. امسح شغل اليوم أولًا لإرجاع الرصيد بأمان.");
                            return;
                        }
                        try {
                            if (module.equals("cuts")) store.deleteCutsWithInventory(day);
                            else store.delete(day,module);
                            updateStatus();
                            toast("تم مسح القسم المحدد لهذا التاريخ فقط");
                        } catch (Exception ex) {
                            toast("لم يتم المسح؛ تعذر إرجاع المخزون بأمان. بياناتك محفوظة، أرسل التقرير للدعم.");
                            android.util.Log.e("AlMizanBakery", "Safe section deletion failed", ex);
                        }
                    }).show();
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
                    if (module.equals("stock")) {
                        validateStockRows(record.optString("items_rows",""));
                        record.put("flour", String.valueOf(stockFlourQuantity(record)));
                        store.saveStockWithInventory(day, record);
                        updateStatus();
                        toast("حُفظ جدول المخزون. راجع الكميات وأسعار الوحدات.");
                    } else if (module.equals("cuts")) {
                        long newWork = parseLong(record.optString("work_units","0"));
                        store.saveCutsWithInventory(day, record, newWork);
                        updateStatus();
                        toast("حُفظ شغل اليوم. تم تحديث رصيد الدقيق والتكلفة تلقائيًا.");
                    } else {
                        store.put(day,module,record); updateStatus(); toast("حُفظ "+titleFor(module)+" بتاريخ "+day);
                    }
                } catch(Exception ex) { toast("لم تُحفظ البيانات: "+ex.getMessage()); }
            }).show();
    }

    private long parseLong(String value) {
        if (value == null) return 0L;
        StringBuilder normalized=new StringBuilder();
        for(int i=0;i<value.length();i++) {
            char ch=value.charAt(i);
            int digit=Character.digit(ch,10);
            if(digit>=0) normalized.append((char)('0'+digit));
            else if(ch==',' || ch=='٬' || ch=='\u00A0' || ch=='\u202F' || Character.isWhitespace(ch)) {
                // Accept common thousands separators and spaces used by Arabic keyboards.
            } else if(ch=='٫') {
                // Arabic decimal separator: preserve decimal semantics; integer fields reject non-zero fractions below.
                normalized.append('.');
            } else {
                normalized.append(ch);
            }
        }
        String s=normalized.toString().trim();
        if(s.isEmpty()) return 0L;
        try {
            return new BigDecimal(s).setScale(0,RoundingMode.UNNECESSARY).longValueExact();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("القيمة «"+value+"» ليست رقمًا صحيحًا. استخدم الأرقام دون رموز أو فواصل عشرية.",ex);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("القيمة «"+value+"» تحتوي كسرًا عشريًا أو تتجاوز الحد الرقمي المسموح.",ex);
        }
    }
    private JSONObject record(String module) { return store.get(day,module); }
    private long val(String module,String key) {
        try { return Math.max(0,parseLong(record(module).optString(key,"0"))); } catch(Exception e) { return 0; }
    }
    private static class Totals { long cash,debt,bank,diff,qty,amount; int badRows; }
    private Totals rowTotals(String module,String key,int columns) {
        Totals out=new Totals(); String raw=record(module).optString(key,"");
        if(raw.trim().isEmpty()) return out;
        String[] lines=raw.split("\\r?\n");
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
                    long qty=parseLong(p[1]);
                    if(qty<0) throw new IllegalArgumentException();
                    if(p.length>=6) {
                        long cash=parseLong(p[2]), debt=parseLong(p[3]), bank=parseLong(p[4]), diff=parseLong(p[5]);
                        if(cash<0||debt<0||bank<0) throw new IllegalArgumentException();
                        out.cash=Math.addExact(out.cash,cash); out.debt=Math.addExact(out.debt,debt);
                        out.bank=Math.addExact(out.bank,bank); out.diff=Math.addExact(out.diff,diff);
                    } else {
                        long price=parseLong(p[2]), diff=parseLong(p[3]);
                        if(price<0||diff<0) throw new IllegalArgumentException();
                        out.debt=Math.addExact(out.debt,price); out.diff=Math.addExact(out.diff,diff);
                    }
                    out.qty=Math.addExact(out.qty,qty);
                } else {
                    long amount=parseLong(p[1]); if(amount<0) throw new IllegalArgumentException();
                    out.amount=Math.addExact(out.amount,amount);
                }
            } catch(Exception ex) { out.badRows++; }
        }
        return out;
    }
    private String legacyStockRows(JSONObject stock) {
        StringBuilder rows = new StringBuilder();
        String[][] items = {{"دقيق","flour","جوال","1"},{"ملح","salt","كيلو","0"},{"خميرة","yeast","باكو","0"},{"زيت","oil","رطل","0"},{"أكياس","bags","لفة","0"},{"جاز","diesel","جالون","0"},{"غاز","gas","أسطوانة","0"},{"حطب","wood","قنطار","0"}};
        for (String[] item : items) {
            String qty = stock.optString(item[1],"0").trim();
            if (qty.isEmpty()) qty = "0";
            if (rows.length()>0) rows.append("\n");
            rows.append(item[0]).append(" | ").append(qty).append(" ").append(item[2]).append(" | ").append(item[3]).append(" | 0");
        }
        return rows.toString();
    }
    private void validateStockRows(String raw) {
        if (raw.trim().isEmpty()) throw new IllegalArgumentException("أدخل أصناف المخزن أولًا.");
        boolean flourFound=false;
        for (String line : normalizeRows(raw).split("\\r?\\n")) {
            if (line.trim().isEmpty()) continue;
            String[] p=line.split("\\|",-1);
            if (p.length<4 || p[0].trim().isEmpty() || p[1].trim().isEmpty() || p[2].trim().isEmpty() || p[3].trim().isEmpty())
                throw new IllegalArgumentException("كل سطر: الصنف | الرصيد والوحدة | الاستهلاك مع جوال واحد | سعر وحدة الاستهلاك");
            String[] q=p[1].trim().split("\\s+");
            long qty=parseLong(q[0]), used=parseLong(p[2].trim()), price=parseLong(p[3].trim());
            if(qty<0 || used<0 || price<0) throw new IllegalArgumentException("الرصيد والاستهلاك والسعر لا تقبل القيم السالبة");
            if((p[0].trim().contains("دقيق") || p[0].trim().equalsIgnoreCase("flour")) && used!=1)
                throw new IllegalArgumentException("استهلاك الدقيق الافتراضي جوال واحد لكل جوال شغل.");
            if(p[0].trim().contains("دقيق") || p[0].trim().equalsIgnoreCase("flour")) flourFound=true;
        }
        if(!flourFound) throw new IllegalArgumentException("أضف صنف الدقيق إلى المخزن لأن شغل اليوم يخصم من رصيده");
    }
    private long stockFlourQuantity(JSONObject stock) {
        String rows=stock.optString("items_rows","").trim();
        if(!rows.isEmpty()) {
            for(String line:normalizeRows(rows).split("\\r?\\n")) {
                String[] p=line.split("\\|",-1);
                if(p.length>=2 && (p[0].trim().contains("دقيق") || p[0].trim().equalsIgnoreCase("flour"))) {
                    String[] q=p[1].trim().split("\\s+");
                    return parseLong(q[0]);
                }
            }
        }
        return Math.max(0L,parseLong(stock.optString("flour","0")));
    }
    private String setStockFlourQuantity(String raw,long quantity) {
        StringBuilder out=new StringBuilder(); boolean found=false;
        for(String line:raw.split("\\r?\\n")) {
            if(line.trim().isEmpty()) continue;
            String[] p=line.split("\\|",-1);
            if(p.length>=3 && (p[0].trim().contains("دقيق") || p[0].trim().equalsIgnoreCase("flour"))) {
                String old=p[1].trim();
                String[] oldParts=old.split("\\s+",2);
                String unit=oldParts.length>1?oldParts[1]:"جوال";
                line=p[0].trim()+" | "+quantity+" "+unit+" | "+p[2].trim();
                found=true;
            }
            if(out.length()>0) out.append("\n");
            out.append(line);
        }
        if(!found) {
            if(out.length()>0) out.append("\n");
            out.append("دقيق | ").append(quantity).append(" جوال | 0");
        }
        return out.toString();
    }

    private long stockFlourValue() {
        JSONObject stock = store.get(day, "stock");
        if (stock.length() == 0) stock = store.getLatest(day, "stock");
        try { return Math.max(0L, parseLong(stock.optString("flour", "0"))); }
        catch (Exception ignored) { return 0L; }
    }

    private long productionLoaves() {
        String rows=record("cuts").optString("production_rows","").trim();
        if(!rows.isEmpty()) {
            long total=0L;
            for(String line:rows.split("\\r?\\n")) {
                if(line.trim().isEmpty()) continue;
                String[] p=line.split("\\|",-1);
                if(p.length<2) throw new IllegalArgumentException("اكتب رقم الجوال | عدد الأرغفة");
                long number=parseLong(p[0].trim()), loaves=parseLong(p[1].trim());
                if(number<1 || loaves<0) throw new IllegalArgumentException("راجع رقم الجوال وعدد الأرغفة");
                total=Math.addExact(total,loaves);
            }
            return total;
        }
        return BakeryMath.productionLoaves(val("cuts","work_units"),val("cuts","loaves_per_unit"));
    }
    private long valueForLoaves(long loaves) {
        long perThousand=val("cuts","loaves_per_1000");
        if(perThousand<=0) return 0;
        return BakeryMath.valueForLoaves(loaves,perThousand);
    }
    private long wages() { return rowTotals("workers","rows",4).amount; }
    private long otherExpenses() { return rowTotals("expenses","rows",2).amount; }
    private long expenseTotal() { return Math.addExact(Math.addExact(val("expenses","meals"),val("expenses","maintenance")),Math.addExact(val("expenses","purchases"),otherExpenses())); }
    private long spoilageValue() { return valueForLoaves(val("spoilage","loaves")); }
    private Totals distribution() { return rowTotals("distribution","rows",4); }
    private long productionValue() { return valueForLoaves(productionLoaves()); }

    // Profit uses actual recorded sales collections/receivables, not theoretical production value.
    // Stock purchase prices are stored separately from consumption costs to avoid mixing units.
    private long salesRevenue() {
        return Math.addExact(Math.addExact(cashTotal(), debtTotal()), bankTotal());
    }

    // Spoilage is valued at estimated production cost per loaf, never at selling price.
    private long spoilageCost() {
        long produced = productionLoaves();
        if (produced <= 0) return 0L;
        long spoiled = val("spoilage", "loaves");
        if (spoiled < 0 || spoiled > produced)
            throw new IllegalArgumentException("عدد التالف لا يجوز أن يتجاوز الإنتاج");
        return java.math.BigDecimal.valueOf(costingTotal())
            .multiply(java.math.BigDecimal.valueOf(spoiled))
            .divide(java.math.BigDecimal.valueOf(produced), 0, java.math.RoundingMode.HALF_UP)
            .longValueExact();
    }

    private long stockUnitPriceTotal() {
        JSONObject stock=store.get(day,"stock");
        if(stock.length()==0) stock=store.getLatest(day,"stock");
        String rows=stock.optString("items_rows","").trim();
        if(rows.isEmpty()) return 0L;
        long total=0L;
        for(String line:rows.split("\\r?\\n")) {
            if(line.trim().isEmpty()) continue;
            String[] p=line.split("\\|",-1);
            if(p.length<3) throw new IllegalArgumentException("راجع جدول المخزن: الصنف | الكمية والوحدة | سعر الوحدة");
            long price=parseLong(p[2].trim());
            if(price<0) throw new IllegalArgumentException("سعر الوحدة لا يمكن أن يكون سالبًا");
            total=Math.addExact(total,price);
        }
        return total;
    }

    private long costingTotal() {
        long workUnits=val("cuts","work_units");
        JSONObject stock=store.get(day,"stock");
        String raw=stock.optString("items_rows","");
        if(raw.trim().isEmpty()) raw=legacyStockRows(stock);
        long total=0L;
        for(String line:raw.split("\\r?\\n")) {
            if(line.trim().isEmpty()) continue;
            String[] p=line.split("\\|",-1);
            if(p.length==3) p=new String[]{p[0],p[1],(p[0].trim().contains("دقيق")?"1":"0"),p[2]};
            if(p.length<4) continue;
            long consumption=parseLong(p[2].trim());
            long unitPrice=parseLong(p[3].trim());
            total=Math.addExact(total,Math.multiplyExact(Math.multiplyExact(consumption,unitPrice),workUnits));
        }
        return total;
    }

    private void showCosting() {
        try {
            long production=productionValue(), cost=costingTotal();
            long spoilage=spoilageCost(), labor=wages(), expenses=expenseTotal();
            long beforeCost=Math.subtractExact(Math.subtractExact(Math.subtractExact(production,spoilage),labor),expenses);
            long margin=BakeryMath.netMargin(salesRevenue(),spoilageCost(),labor,expenses,cost,distributionDiff());
            String message="تاريخ التكلفة: "+day+
                "\nقيمة القطعيات/الإنتاج: "+fmt(production)+" جنيه"+
                "\n(-) قيمة التالف: "+fmt(spoilage)+" جنيه"+
                "\n(-) يومية العمال: "+fmt(labor)+" جنيه"+
                "\n(-) منصرفات المحل: "+fmt(expenses)+" جنيه"+
                "\nالصافي قبل تكلفة التشغيل: "+fmt(beforeCost)+" جنيه"+
                "\n(-) تكلفة الدقيق والوقود والخميرة والزيت والملح والأكياس: "+fmt(cost)+" جنيه"+
                "\n\nهامش الربح التقديري النهائي: "+fmt(margin)+" جنيه"+
                "\n\nحساب الدقيق مرتبط بعدد جوالات شغل اليوم. راجع أسعار المواد والتالف واليومية والمنصرفات، ولا تسجل المصروف نفسه مرتين.";
            new AlertDialog.Builder(this).setTitle("🏭 حساب التشغيل والتكلفة — "+day).setMessage(message)
                .setNegativeButton("إغلاق",null)
                .setNeutralButton("تعديل بيانات التكلفة",(d,w)->openModule("costing"))
                .setPositiveButton("مشاركة التقرير",(d,w)->shareReport(message)).show();
        } catch(Exception ex) { toast("تعذر حساب التكلفة بسبب رقم كبير جدًا أو بيانات غير صحيحة. راجع الكميات والأسعار."); }
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
            boolean priceConfigured=val("cuts","loaves_per_1000")>0;
            boolean reconciliationValid=BakeryMath.isReconciliationValid(difference,bad,priceConfigured);
            String status=!priceConfigured?"غير مكتمل — أدخل عدد الأرغفة المقابل لـ 1,000 جنيه":bad>0?"غير مكتمل — توجد سطور غير صالحة":difference==0?"متوازن حسابيًا":difference>0?"عجز غير مفسر":"زيادة غير مفسرة";
            String message="تاريخ الدفتر: "+day+
                "\n\nالعيش المنتج: "+fmt(loaves)+" رغيف"+
                "\nقيمة القطعيات حسب السعر المدخل: "+fmt(expected)+" جنيه"+
                "\nالكاش (الشباك والتوزيع): "+fmt(cashTotal())+" جنيه"+
                "\nالديون المستحقة: "+fmt(debtTotal())+" جنيه"+
                "\nالتحويلات البنكية: "+fmt(bankTotal())+" جنيه"+
                "\nكمية التوزيع المسجلة: "+fmt(dist.qty)+" رغيف"+
                "\nفرق السعر: "+fmt(distributionDiff())+" جنيه"+
                "\nيومية العمال المحسوبة من السطور: "+fmt(wages())+" جنيه"+
                "\nمنصرفات المحل: "+fmt(expenseTotal())+" جنيه"+
                "\nقيمة التالف: "+fmt(spoilageValue())+" جنيه"+
                "\nتكلفة التشغيل التقديرية (للمقارنة فقط): "+fmt(costingTotal())+" جنيه"+
                "\nهامش الربح التقديري: "+fmt(BakeryMath.netMargin(salesRevenue(),spoilageCost(),wages(),expenseTotal(),costingTotal(),distributionDiff()))+" جنيه"+
                "\n\nإجمالي البنود المسجلة: "+fmt(actual)+" جنيه"+
                "\nفرق التسوية (المطلوب صفر): "+fmt(difference)+" جنيه"+
                "\nالنتيجة: "+status+
                (bad>0?"\n\n⚠ توجد "+bad+" سطور غير مكتملة أو غير رقمية. راجع صيغة السطور.":"")+
                (val("cuts","loaves_per_1000")<=0?"\n⚠ أدخل عدد العيش المقابل لـ 1,000 جنيه في القطعيات.":"");
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle("🧮 المحاسب والحساب — "+day).setMessage(message)
                .setNegativeButton("إغلاق",null).setNeutralButton("مشاركة التقرير",(d,w)->shareReport(message))
                .setPositiveButton("حفظ التقرير", null).create();
            dialog.setOnShowListener(v -> {
                Button save=dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                save.setEnabled(reconciliationValid);
                save.setText(reconciliationValid?"حفظ التقرير":"أكمل البيانات أولًا");
                save.setOnClickListener(btn -> {
                    if(!reconciliationValid) { toast("لا يمكن اعتماد التسوية قبل اكتمال البيانات وتطابق الطرفين."); return; }
                    store.put(day,"accounting",jsonSummary(message));
                    toast("تم حفظ ملخص التسوية المتوازنة لهذا التاريخ");
                    dialog.dismiss();
                });
            });
            dialog.show();
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
        updateDashboardMetrics();
    }

    private void updateDashboardMetrics() {
        if (flourMetric == null) return;
        flourMetric.setText(fmt(stockFlourValue()) + " جوال");
        if (workMetric != null) workMetric.setText(fmt(val("cuts","work_units")) + " جوال");
        try {
            long production = productionValue();
            long margin = BakeryMath.netMargin(salesRevenue(), spoilageCost(), wages(), expenseTotal(), costingTotal(), distributionDiff());
            if (productionMetric != null) productionMetric.setText(fmt(production) + " ج");
            if (marginMetric != null) marginMetric.setText(fmt(margin) + " ج");
        } catch (Exception ex) {
            if (productionMetric != null) productionMetric.setText("راجع البيانات");
            if (marginMetric != null) marginMetric.setText("راجع البيانات");
        }
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
        if(requestCode==REQUEST_PAYMENT_RECEIPT) {
            if(resultCode==RESULT_OK && data!=null && data.getData()!=null) sharePaymentReceipt(data.getData());
            return;
        }
        if(requestCode==REQUEST_COMPANY_LOGO) {
            if(resultCode==RESULT_OK && data!=null && data.getData()!=null) shareCompanyLogo(data.getData());
            return;
        }
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

    private void showUpdateDialog() {
        new AlertDialog.Builder(this)
            .setTitle("⬆️ تحديث المحاسب الميزان للمخابز")
            .setMessage("الإصدار المثبت: " + BuildConfig.VERSION_NAME +
                "\nافتح صفحة الإصدار لمراجعة أحدث نسخة وتنزيلها. عند التحديث، لا تحذف التطبيق الحالي؛ نزّل ملف APK وثبّته فوق النسخة الموجودة للمحافظة على بياناتك. إذا رفض الهاتف التحديث، لا تحذف النسخة الحالية قبل أخذ نسخة احتياطية.")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("فتح صفحة التحديث", (dialog, which) -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/moh1442010-arch/smart-encyclopedias-platform/releases/tag/al-mizan-bakery-2.6.2-review"));
                    startActivity(intent);
                } catch (Exception ex) {
                    toast("تعذر فتح صفحة التحديث. تحقق من اتصال الإنترنت ثم أعد المحاولة.");
                }
            })
            .show();
    }

    private void showGuide() {
        String guide="الدليل الشامل — المحاسب الميزان للمخابز " + BuildConfig.VERSION_NAME + "\n\n"+
            "أولًا: تسلسل دورة العمل\nابدأ بتاريخ اليوم، راجع المخزن، سجل استهلاك وشغل الدقيق والقطعيات، ثم يومية العمال، مبيعات الشباك، التوزيع، التالف والمنصرفات، وأخيرًا راجع التسوية وحساب التشغيل وهامش الربح. أدخل كل عملية في مكانها مرة واحدة.\n\n"+
            "1) التاريخ: يعتمد التطبيق تاريخ اليوم تلقائيًا، ويمكنك فتح سجل يوم آخر للمراجعة. لا تعدل يومًا سابقًا بعد تسجيل أيام لاحقة إلا بعد مراجعة أثر التعديل على المخزون.\n\n"+
            "2) المخزن: كل مادة لها أربعة حقول: الصنف؛ الرصيد الحالي مع وحدة القياس؛ كمية استهلاك المادة مع جوال دقيق واحد؛ سعر وحدة الاستهلاك بالجنيه. أمثلة الوحدات: الدقيق جوال، الخميرة باكو، الزيت رطل، الأكياس لفة، الملح كيلو، الجاز جالون، الغاز أسطوانة، الحطب قنطار. أدخل معدل الاستهلاك الحقيقي لكل جوال شغل؛ لا تضع سعر شراء الكمية كلها في خانة الاستهلاك. أضف أي مادة أخرى في صف مستقل.\n\n"+
            "مثال المخزون: دقيق | 50 جوال | 1 | 60000. إذا كان استهلاك الخميرة 0.5 باكو لكل جوال دقيق، فلا تدخل نصف باكو في خانة عدد صحيح؛ هذا الإصدار يستخدم كميات صحيحة، لذا سجل الاستهلاك بوحدة أصغر مناسبة أو لا تعتمد قيمة كسرية حتى يدعمها النظام. سعر الوحدة هو سعر الباكو/الرطل/الكيلو/الجالون/الأسطوانة/القنطار بحسب الصنف.\n\n"+
            "3) الخصم الآلي: عند حفظ شغل اليوم، يضرب التطبيق استهلاك كل مادة لكل جوال في عدد جوالات الشغل، ثم يخصم الناتج من رصيدها. مثال: رصيد 45 باكو خميرة، واستهلاك باكو واحد لكل جوال، وشغل 7 جوالات؛ المتبقي 38 باكو. إذا لم يكف الرصيد يرفض التطبيق الحفظ بدل إظهار رصيد سالب. راجع الوحدات ومعدلات الاستهلاك قبل اعتماد الحساب.\n\n"+
            "4) القطعيات والإنتاج: أدخل عدد جوالات الدقيق المستخدمة، ثم سجل كل جوال في سطر مستقل: رقم الجوال | عدد الأرغفة، مثل 1 | 700 و2 | 800 و3 | 650. أدخل عدد الأرغفة التي تساوي 1000 جنيه حسب سعر مخبزك (3 أو 4 أو 5 أو أي رقم صحيح معتمد). قيمة الإنتاج = مجموع الأرغفة ÷ عدد الأرغفة لكل 1000 × 1000. مثال: 4000 ÷ 4 × 1000 = 1,000,000 جنيه.\n\n"+
            "5) تكلفة التشغيل: تُحسب من معدل استهلاك المادة لكل جوال × سعر وحدة المادة × عدد جوالات الشغل. مثال: استهلاك 1 باكو خميرة لكل جوال، وسعر الباكو 6000، وشغل 7 جوالات؛ تكلفة الخميرة 42,000 جنيه. سعر المخزون ليس تكلفة يومية إلا بقدر ما استُهلك بالفعل.\n\n"+
            "6) يومية العمال: سطر لكل عامل بالصيغة: الاسم | الوظيفة | عدد الشغل | أجر الوحدة. مثال: أحمد | خباز | 7 | 2000؛ أجره = 7 × 2000 = 14,000 جنيه. اجمع أجور العمال كلهم، ولا تدخل إجمالي أجرة العامل في خانة سعر الوحدة.\n\n"+
            "7) بيع الشباك: سجل النقد المقبوض، الديون، وتحويلات بنكك/البنك كلًا في خانته. اكتب مرجع التحويل إن وجد. لا تسجل مبيعات التوزيع مرة أخرى ضمن الشباك.\n\n"+
            "8) التوزيع: سطر لكل عميل: اسم العميل | عدد الأرغفة | نقدًا | ديون | بنكك | فرق السعر. افصل وسيلة التحصيل عن فرق السعر. مثال توضيحي: بقالة | 100 | 22000 | 0 | 0 | 3000. تأكد من أن مجموع المبالغ المسجلة يطابق البيع الفعلي المتفق عليه.\n\n"+
            "9) التالف: أدخل عدد الأرغفة التالفة وسبب التلف. تُحوّل الكمية إلى قيمة بحسب معيار سعر الأرغفة، وتدخل تكلفة التالف في حساب الربح وفق سعر التكلفة. لا تخصم قيمة التالف مرة أخرى كمنصرف إذا كانت محسوبة في بند التالف.\n\n"+
            "10) المنصرفات: سجل وجبات العمال والصيانة والمشتريات والمنصرفات الأخرى مع البيان والمبلغ. لا تسجل استهلاك مادة مخزنية كمنصرف نقدي مرة أخرى؛ تكلفة المادة تُحسب من المخزون ومعدل استهلاكها.\n\n"+
            "11) التسوية والمحاسب: راجع الإنتاج، مبيعات الشباك، التوزيع، النقد، بنكك، الديون، فرق السعر، الأجور، التالف والمنصرفات. فرق التسوية غير الصفري إشارة للمراجعة وليس شيئًا ينبغي إخفاؤه. ابحث عن عملية مكررة أو مبلغ ناقص أو وسيلة تحصيل مسجلة في الخانة الخطأ قبل إغلاق اليوم.\n\n"+
            "12) معادلة هامش الربح المعتمدة: المبيعات الفعلية − تكلفة المواد المستهلكة − التالف بسعر التكلفة − أجور العمال − المنصرفات − فرق السعر. احذر التكرار: النقد والديون وبنكك هي طرق تحصيل للمبيعات وليست ثلاث مبيعات منفصلة. لا تخصم المادة نفسها كتكلفة تشغيل وكمنصرف نقدي في الوقت ذاته.\n\n"+
            "13) الحفظ والنسخ الاحتياطي: البيانات تحفظ محليًا ويعمل التطبيق دون إنترنت في الوظائف الأساسية. صدّر نسخة احتياطية إلى مكان آمن، واختبر الاستعادة قبل تحديث التطبيق أو تغيير الهاتف. لا تشارك النسخة الاحتياطية مع غير المخولين لأنها قد تحتوي بيانات العملاء.\n\n"+
            "14) الاشتراك والدعم: التجربة 7 أيام، ثم تظهر شاشة الاشتراك. الباقات المعتمدة: شهر 30,000 جنيه، سنة 300,000 جنيه، شراء مرة واحدة 450,000 جنيه. بعد التحويل أرسل الإيصال عبر واتساب الدعم +249121851285. لا ترسل الرقم السري أو رمز التحقق البنكي. التفعيل لا يتم تلقائيًا بمجرد إرسال الطلب.\n\n"+
            "15) حدود النسخة: هذه نسخة مراجعة. اختبر دورة يوم كاملة، ودقة المخزون والتسوية، والنسخ الاحتياطي والاستعادة على جهاز حقيقي قبل الاعتماد المالي أو الضريبي أو البيع التجاري.";
        ScrollView sc=new ScrollView(this);
        TextView body=text(guide,15); body.setPadding(16,12,16,12); body.setTextIsSelectable(true); sc.addView(body);
        new AlertDialog.Builder(this).setTitle("📘 الدليل الإرشادي الشامل").setView(sc).setPositiveButton("فهمت",null)
            .setNeutralButton("الأسئلة الشائعة",(d,w)->showFaq()).show();
    }

    private void showFaq() {
        String faq="الأسئلة الشائعة — حلول المحاسب والمخبز\n\n"+
            "س: لماذا لم يُحفظ شغل اليوم؟\nج: تأكد من إدخال رصيد المخزن أولًا، وأن الرصيد يكفي استهلاك الدقيق وبقية المواد، وأن كل سطر مخزون يحتوي الصنف والرصيد والوحدة والاستهلاك والسعر.\n\n"+
            "س: لماذا يظهر أن رصيد مادة غير كافٍ؟\nج: راجع رصيد بداية اليوم ووحدة القياس ومعدل استهلاك المادة مع جوال واحد. لا تخلط الجالون بالرطل أو الباكو بالوحدة، ولا تسجل الاستهلاك اليومي في خانة الرصيد.\n\n"+
            "س: كيف أحسب استهلاك الخميرة أو الزيت أو الأكياس؟\nج: اقسم الكمية المستخدمة فعليًا على عدد جوالات الدقيق المشغولة لتحصل على معدل لكل جوال، ثم أدخل المعدل وسعر وحدة القياس الصحيح. إذا كان المعدل كسريًا فلا تقرّبه عشوائيًا؛ استخدم وحدة أصغر أو لا تعتمد النتيجة قبل دعم الكسور.\n\n"+
            "س: هل سعر المخزون يساوي تكلفة شغل اليوم؟\nج: لا. تكلفة اليوم هي معدل استهلاك الوحدة لكل جوال × سعر الوحدة × عدد جوالات الشغل، وليس قيمة كل المخزون الموجود.\n\n"+
            "س: كيف أختار سعر تحويل الأرغفة؟\nج: أدخل عدد الأرغفة التي تساوي 1000 جنيه وفق سياسة مخبزك، مثل 3 أو 4 أو 5. راجع الرقم قبل حفظ القطعيات لأن تغييره يغيّر قيمة الإنتاج والتالف.\n\n"+
            "س: لماذا لا يساوي النقد قيمة الإنتاج؟\nج: لأن جزءًا من المبيعات قد يكون ديونًا أو تحويلات بنكك أو مبيعات توزيع. راجع كل وسيلة تحصيل ومبالغ العملاء، ولا تدخل مبيعات التوزيع مرة ثانية في الشباك.\n\n"+
            "س: كيف أسجل فرق السعر؟\nج: أدخله في خانة فرق السعر الخاصة بعميل التوزيع كما هو متفق عليه، وراجع أثره في التسوية والربح. لا تضف الفرق مرة ثانية إلى قيمة البيع إذا كان المبلغ الفعلي يشمله بالفعل.\n\n"+
            "س: لماذا ظهر هامش الربح سالبًا أو أقل من المتوقع؟\nج: افحص معدل استهلاك المواد وسعر الوحدة، وعدد جوالات الشغل، والأجور، والتالف، والمنصرفات، وفرق السعر. تأكد من عدم تكرار المصروف أو المبيعات، ولا تغيّر المعادلة لإخفاء الفرق.\n\n"+
            "س: كيف أراجع خطأ في يوم سابق؟\nج: افتح التاريخ الصحيح وراجع القيود. إذا كانت هناك أيام لاحقة مسجلة، قد يمنع النظام تعديل المخزون السابق حفاظًا على التسلسل؛ راجع الأرصدة بالتاريخ ولا تحذف بيانات عشوائيًا.\n\n"+
            "س: هل يعمل التطبيق بدون إنترنت؟\nج: نعم، تُحفظ السجلات محليًا وتعمل الوظائف الأساسية دون اتصال. الإنترنت مطلوب للتواصل مع الدعم وتنزيل التحديثات، ولأي خدمة سحابية متاحة.\n\n"+
            "س: ماذا أفعل قبل تحديث التطبيق أو تغيير الهاتف؟\nج: صدّر نسخة احتياطية، واحفظها خارج الهاتف، ثم اختبر استعادتها. لا تحذف التطبيق قبل التأكد من أن النسخة الاحتياطية سليمة.\n\n"+
            "س: انتهت التجربة؛ هل ستُحذف حساباتي؟\nج: شاشة الاشتراك تقفل الوصول للدفاتر بعد انتهاء التجربة، لكن البيانات المحلية لا تُحذف تلقائيًا. اختر الباقة وأرسل طلب التفعيل والإيصال للدعم.\n\n"+
            "س: أين أطلب المساعدة؟\nج: واتساب الدعم الفني +249121851285. أرسل وصف المشكلة وتاريخها وصورة للشاشة عند الحاجة، ولا ترسل كلمة مرورك أو بيانات الدخول البنكية.\n\n"+
            "س: هل النسخة معتمدة للبيع التجاري؟\nج: هذه نسخة مراجعة حتى تكتمل الاختبارات العملية على الهاتف، بما فيها المخزون والتسوية والنسخ الاحتياطي والاستعادة.";
        ScrollView sc=new ScrollView(this);
        TextView body=text(faq,15); body.setPadding(16,12,16,12); body.setTextIsSelectable(true); sc.addView(body);
        new AlertDialog.Builder(this).setTitle("❓ الأسئلة الشائعة").setView(sc).setPositiveButton("إغلاق",null)
            .setNeutralButton("فتح الدليل",(d,w)->showGuide()).show();
    }

    private long trialDaysRemaining() {
        long start = getPreferences(0).getLong("trial_start", 0L);
        if (start == 0L) return 7L;
        long duration = 7L * 24L * 60L * 60L * 1000L;
        long remaining = duration - Math.max(0L, System.currentTimeMillis() - start);
        if (remaining <= 0L) return 0L;
        return (remaining + 24L * 60L * 60L * 1000L - 1L) / (24L * 60L * 60L * 1000L);
    }

    private void showPaywall() {
        base("🔐 اشتراك المحاسب الميزان للمخابز");
        boolean licensed = hasActiveLicense();
        boolean expired = trialExpired();
        String trialMessage = licensed
            ? "اشتراكك مفعّل على هذا الجهاز. يمكنك متابعة الدفاتر أو طلب تجديد الباقة عند الحاجة."
            : expired
                ? "انتهت فترة التجربة المجانية (7 أيام)، وتم إيقاف الوصول إلى الدفاتر حتى التفعيل. بياناتك المحلية محفوظة ولن تُحذف. اختر الباقة وحوّل المبلغ عبر بنكك ثم أرسل الإيصال عبر واتساب لطلب التفعيل."
                : "أهلًا بك في المحاسب الميزان للمخابز. بقي نحو " + trialDaysRemaining() + " يومًا من التجربة المجانية. يمكنك اختيار باقة مسبقًا؛ ستظل بياناتك محفوظة على الهاتف.";
        content.addView(text(trialMessage,16));
        content.addView(text("الاشتراك الشهري — 30,000 جنيه سوداني",18));
        Button monthly=button("اختيار الباقة الشهرية");
        monthly.setOnClickListener(v->requestActivation("شهرية","30,000 جنيه سوداني"));
        content.addView(monthly);
        content.addView(text("الاشتراك السنوي — 300,000 جنيه سوداني",18));
        Button annual=button("اختيار الباقة السنوية");
        annual.setOnClickListener(v->requestActivation("سنوية","300,000 جنيه سوداني"));
        content.addView(annual);
        content.addView(text("شراء مرة واحدة — 450,000 جنيه سوداني",18));
        Button lifetime=button("اختيار باقة الشراء مرة واحدة");
        lifetime.setOnClickListener(v->requestActivation("شراء مرة واحدة","450,000 جنيه سوداني"));
        content.addView(lifetime);
        content.addView(text("تنبيه قبل الدفع: الشراء لمرة واحدة يفعّل هذا الإصدار فقط ولا يشمل التحديثات المستقبلية. الباقات الشهرية والسنوية تشمل التحديثات طوال فترة الاشتراك.",13));
        content.addView(text("معرّف هذا الجهاز لربط الاشتراك به:",14));
        content.addView(text(deviceId(),13));
        Button copyDevice=button("📋 نسخ معرّف الجهاز لإرساله للدعم");
        copyDevice.setOnClickListener(v->{
            android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Bakery device ID",deviceId()));
            toast("تم نسخ معرّف الجهاز. أرسله للدعم فقط؛ لا ترسل كلمة مرورك.");
        });
        content.addView(copyDevice);
        Button activate=button("🔑 أدخل رمز التفعيل بعد موافقة الدعم");
        activate.setOnClickListener(v->showActivationDialog());
        content.addView(activate);
        content.addView(text("الدفع عبر بنكك — رقم الحساب: 1882224",16));
        Button copy=button("نسخ رقم حساب بنكك");
        copy.setOnClickListener(v->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("Bankak",BANKAK));toast("تم نسخ رقم بنكك");});
        content.addView(copy);
        content.addView(text("بعد التحويل، أرفق صورة الإيصال داخل محادثة واتساب. لا ترسل الرقم السري أو رمز التحقق الخاص ببنكك. التفعيل يتم بعد مراجعة التحويل.",14));
        Button logoRequest=button("🌾 إرسال شعار المخبز لتخصيص الفواتير عبر واتساب");
        logoRequest.setOnClickListener(v->chooseCompanyLogo());
        content.addView(logoRequest);
        Button backup=button("🗂️ تصدير / استعادة بياناتك");
        backup.setOnClickListener(v->backupRestoreMenu());
        content.addView(backup);
        if (!trialExpired() || hasActiveLicense()) { Button back=button("العودة إلى التطبيق"); back.setOnClickListener(v->buildHome()); content.addView(back); }
    }

    private void requestActivation(String plan,String amount) {
        final String message="السلام عليكم، أطلب تفعيل المحاسب الميزان للمخابز " + BuildConfig.VERSION_NAME + ".\n"+
            "الباقة المختارة: "+plan+"\n"+
            "المبلغ: "+amount+"\n"+
            "رقم حساب بنكك المحوّل إليه: "+BANKAK+"\n"+
            "تاريخ الطلب: "+day+"\n"+
            "معرّف الجهاز: "+deviceId()+"\n"+
            "رقم عملية بنكك: [اكتب رقم العملية]\n"+
            "أرفق إيصال التحويل في هذه المحادثة. بعد التحقق أرجو إرسال رمز تفعيل مرتبط بهذا الجهاز والباقة.";
        new AlertDialog.Builder(this).setTitle("إرسال طلب التفعيل")
            .setMessage("اختر إرسال الطلب النصي إلى واتساب أو إرفاق صورة/PDF لإيصال التحويل. لا ترسل الرقم السري أو رمز التحقق الخاص ببنكك.")
            .setNegativeButton("إلغاء",null)
            .setNeutralButton("إرسال الطلب النصي",(d,w)->openWhatsApp(message))
            .setPositiveButton("📎 إرفاق الإيصال",(d,w)->choosePaymentReceipt(message))
            .show();
    }

    private void choosePaymentReceipt(String message) {
        pendingActivationMessage = message;
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("*/*");
        picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*","application/pdf"});
        try { startActivityForResult(picker,REQUEST_PAYMENT_RECEIPT); }
        catch(Exception ex) { toast("تعذر فتح اختيار الإيصال؛ يمكنك إرسال الطلب النصي عبر واتساب."); }
    }

    private void sharePaymentReceipt(Uri uri) {
        try {
            String mime = getContentResolver().getType(uri);
            if (mime == null || (!mime.startsWith("image/") && !mime.equals("application/pdf"))) {
                toast("اختر صورة أو ملف PDF صالحًا للإيصال.");
                return;
            }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(mime);
            share.putExtra(Intent.EXTRA_STREAM,uri);
            share.putExtra(Intent.EXTRA_TEXT,pendingActivationMessage);
            share.setClipData(android.content.ClipData.newUri(getContentResolver(), "إيصال التحويل", uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"إرسال طلب التفعيل والإيصال"));
        } catch(Exception ex) {
            toast("تعذر مشاركة الإيصال. يمكنك فتح واتساب وإرسال الطلب النصي ثم إرفاق الإيصال يدويًا.");
        }
    }

    private void chooseCompanyLogo() {
        pendingActivationMessage = "السلام عليكم، أرسل شعار نشاطي لتخصيص فواتير المحاسب الميزان للمخابز " + BuildConfig.VERSION_NAME + ".\n"+
            "معرّف الجهاز: "+deviceId()+"\n"+
            "اسم النشاط: [اكتب اسم المخبز]\n"+
            "يرجى استخدام الشعار في الفواتير بعد مراجعة الطلب.";
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        picker.setType("image/*");
        try { startActivityForResult(picker,REQUEST_COMPANY_LOGO); }
        catch(Exception ex) { toast("تعذر فتح اختيار الشعار؛ يمكنك إرسال الشعار يدويًا عبر واتساب."); }
    }

    private void shareCompanyLogo(Uri uri) {
        try {
            String mime = getContentResolver().getType(uri);
            if (mime == null || !mime.startsWith("image/")) {
                toast("اختر ملف صورة صالحًا للشعار.");
                return;
            }
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType(mime);
            share.putExtra(Intent.EXTRA_STREAM,uri);
            share.putExtra(Intent.EXTRA_TEXT,pendingActivationMessage);
            share.setClipData(android.content.ClipData.newUri(getContentResolver(),"شعار المخبز",uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"إرسال شعار المخبز عبر واتساب"));
        } catch(Exception ex) {
            toast("تعذر مشاركة الشعار. افتح واتساب وأرفق الصورة يدويًا.");
        }
    }

    private void openWhatsApp(String message) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"+SUPPORT+"?text="+Uri.encode(message)))); }
        catch(Exception e) { toast("تعذر فتح واتساب. الرقم: +249 121 851 285"); }
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private class Store extends SQLiteOpenHelper {
        Store(){super(MainActivity.this,"almizan_bakery_250.db",null,4);}
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE records (day TEXT NOT NULL, module TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(day,module))");
            db.execSQL("CREATE TABLE inventory_usage (day TEXT PRIMARY KEY NOT NULL, flour_bags INTEGER NOT NULL DEFAULT 0, material_usage TEXT NOT NULL DEFAULT '{}')");
        }
        @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
            // Schema v3 keeps the existing records table intact and ensures the stock-usage ledger exists.
            // Never drop or recreate user accounting data during an upgrade.
            if (oldVersion < 3) {
                db.execSQL("CREATE TABLE IF NOT EXISTS inventory_usage (day TEXT PRIMARY KEY NOT NULL, flour_bags INTEGER NOT NULL DEFAULT 0)");
            }
            if (oldVersion < 4) {
                try { db.execSQL("ALTER TABLE inventory_usage ADD COLUMN material_usage TEXT NOT NULL DEFAULT '{}'"); } catch (Exception ignored) { }
            }
        }
        JSONObject get(String date,String module) {
            SQLiteDatabase db=getReadableDatabase(); Cursor c=db.rawQuery("SELECT payload FROM records WHERE day=? AND module=?",new String[]{date,module});
            try { if(c.moveToFirst()) return new JSONObject(c.getString(0)); } catch(Exception ignored) {} finally {c.close();}
            return new JSONObject();
        }
        void put(String date,String module,JSONObject value) {
            ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module",module); cv.put("payload",value.toString()); cv.put("updated_at",System.currentTimeMillis());
            getWritableDatabase().insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
        }

        JSONObject getLatest(String date,String module) {
            SQLiteDatabase db=getReadableDatabase();
            Cursor c=db.rawQuery("SELECT payload FROM records WHERE module=? AND day<=? ORDER BY day DESC LIMIT 1",new String[]{module,date});
            try { if(c.moveToFirst()) return new JSONObject(c.getString(0)); }
            catch(Exception ignored) {} finally { c.close(); }
            return new JSONObject();
        }

        private JSONObject materialUsageFor(String raw,long workUnits) throws Exception {
            JSONObject usage=new JSONObject();
            for(String line:raw.split("\\r?\\n")) {
                if(line.trim().isEmpty()) continue;
                String[] p=line.split("\\|",-1);
                if(p.length==3) p=new String[]{p[0],p[1],(p[0].trim().contains("دقيق")?"1":"0"),p[2]};
                if(p.length<4) throw new IllegalArgumentException("راجع أعمدة المخزون الأربعة");
                long amount=Math.multiplyExact(parseLong(p[2].trim()),workUnits);
                if(amount>0) usage.put(p[0].trim(),amount);
            }
            return usage;
        }
        private String adjustMaterialRows(String raw,JSONObject usage,boolean restore) throws Exception {
            StringBuilder out=new StringBuilder();
            for(String line:raw.split("\\r?\\n")) {
                if(line.trim().isEmpty()) continue;
                String[] p=line.split("\\|",-1);
                if(p.length==3) p=new String[]{p[0],p[1],(p[0].trim().contains("دقيق")?"1":"0"),p[2]};
                if(p.length<4) throw new IllegalArgumentException("صيغة صنف المخزن غير مكتملة");
                String name=p[0].trim();
                String[] qp=p[1].trim().split("\\s+",2);
                long qty=parseLong(qp[0]); String unit=qp.length>1?qp[1]:"وحدة";
                long used=usage.optLong(name,0L);
                long next=restore?Math.addExact(qty,used):Math.subtractExact(qty,used);
                if(next<0) throw new IllegalStateException("الرصيد لا يكفي من الصنف: "+name+"؛ المطلوب "+used+" والمتاح "+qty);
                if(out.length()>0) out.append("\n");
                out.append(name).append(" | ").append(next).append(" ").append(unit).append(" | ").append(p[2].trim()).append(" | ").append(p[3].trim());
            }
            return out.toString();
        }
        private JSONObject readMaterialUsage(String date) {
            Cursor c=getReadableDatabase().rawQuery("SELECT material_usage FROM inventory_usage WHERE day=?",new String[]{date});
            try { if(c.moveToFirst()) return new JSONObject(c.getString(0)); } catch(Exception ignored) { } finally { c.close(); }
            return new JSONObject();
        }

        void saveCutsWithInventory(String date,JSONObject cuts,long newWork) throws Exception {
            if(newWork<0) throw new IllegalArgumentException("عدد جوالات الدقيق لا يمكن أن يكون سالبًا");
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                long previouslyApplied=usageForDate(date);
                JSONObject previousUsage=readMaterialUsage(date);
                if(previousUsage.length()==0 && previouslyApplied>0) previousUsage.put("دقيق",previouslyApplied);
                if(newWork!=previouslyApplied && hasFutureStockAfter(date))
                    throw new IllegalStateException("لا يمكن تغيير شغل يوم سابق بعد تسجيل أرصدة لأيام لاحقة.");
                JSONObject stock=getLatest(date,"stock");
                if(stock.length()==0 && (newWork>0 || previouslyApplied>0))
                    throw new IllegalStateException("أدخل أرصدة المواد في المخزن أولًا، ثم احفظ شغل اليوم.");
                JSONObject newUsage=new JSONObject();
                if(stock.length()>0) {
                    String raw=stock.optString("items_rows","");
                    if(raw.trim().isEmpty()) raw=legacyStockRows(stock);
                    raw=adjustMaterialRows(raw,previousUsage,true);
                    newUsage=materialUsageFor(raw,newWork);
                    raw=adjustMaterialRows(raw,newUsage,false);
                    stock.put("items_rows",raw);
                    stock.put("flour",String.valueOf(stockFlourQuantityFromRows(raw)));
                    ContentValues scv=new ContentValues(); scv.put("day",date); scv.put("module","stock"); scv.put("payload",stock.toString()); scv.put("updated_at",System.currentTimeMillis());
                    db.insertWithOnConflict("records",null,scv,SQLiteDatabase.CONFLICT_REPLACE);
                }
                ContentValues uv=new ContentValues(); uv.put("day",date); uv.put("flour_bags",newWork); uv.put("material_usage",newUsage.toString());
                db.insertWithOnConflict("inventory_usage",null,uv,SQLiteDatabase.CONFLICT_REPLACE);
                JSONObject cost=get(date,"costing"); cost.put("flour_bags",String.valueOf(newWork));
                ContentValues ccv=new ContentValues(); ccv.put("day",date); ccv.put("module","costing"); ccv.put("payload",cost.toString()); ccv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,ccv,SQLiteDatabase.CONFLICT_REPLACE);
                ContentValues cutcv=new ContentValues(); cutcv.put("day",date); cutcv.put("module","cuts"); cutcv.put("payload",cuts.toString()); cutcv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,cutcv,SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        private long stockFlourQuantityFromRows(String raw) {
            for(String line:raw.split("\\r?\\n")) {
                String[] p=line.split("\\|",-1);
                if(p.length>=2 && (p[0].trim().contains("دقيق") || p[0].trim().equalsIgnoreCase("flour")))
                    return parseLong(p[1].trim().split("\\s+")[0]);
            }
            return 0L;
        }
        long usageForDate(String date) {
            Cursor c=getReadableDatabase().rawQuery("SELECT flour_bags FROM inventory_usage WHERE day=?",new String[]{date});
            try { return c.moveToFirst()?c.getLong(0):0L; } finally { c.close(); }
        }
        boolean hasFutureStockAfter(String date) {
            Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM records WHERE module='stock' AND day>? LIMIT 1",new String[]{date});
            try { return c.moveToFirst(); } finally { c.close(); }
        }

        void saveStockWithInventory(String date,JSONObject stockInput) throws Exception {
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                if(hasFutureStockAfter(date)) throw new IllegalStateException("لا يمكن تعديل رصيد مخزن سابق بعد تسجيل أرصدة لأيام لاحقة.");
                long usage=usageForDate(date);
                String raw=stockInput.optString("items_rows","");
                if(raw.trim().isEmpty() && usage>0) throw new IllegalStateException("أدخل رصيد المواد الافتتاحي قبل حفظ المخزن.");
                JSONObject applied=readMaterialUsage(date);
                String closingRows=raw.trim().isEmpty()?raw:adjustMaterialRows(raw,applied,false);
                long closing=stockFlourQuantityFromRows(closingRows);
                if(closing<0) throw new IllegalStateException("رصيد الدقيق لا يكفي لشغل اليوم.");
                stockInput.put("flour",String.valueOf(closing));
                if(!closingRows.trim().isEmpty()) stockInput.put("items_rows",closingRows);
                ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module","stock"); cv.put("payload",stockInput.toString()); cv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }

        void deleteCutsWithInventory(String date) {
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                if(hasFutureStockAfter(date)) throw new IllegalStateException("لا يمكن حذف شغل يوم سابق بعد تسجيل أرصدة مخزن لأيام لاحقة.");
                long usage=usageForDate(date);
                JSONObject previousUsage=readMaterialUsage(date);
                if(previousUsage.length()==0 && usage>0) previousUsage.put("دقيق",usage);
                Cursor c=db.rawQuery("SELECT payload FROM records WHERE day=? AND module='stock'",new String[]{date});
                try {
                    if(c.moveToFirst() && (usage>0 || previousUsage.length()>0)) {
                        JSONObject stock=new JSONObject(c.getString(0));
                        String raw=stock.optString("items_rows","");
                        if(!raw.trim().isEmpty()) raw=adjustMaterialRows(raw,previousUsage,true);
                        long restored=stockFlourQuantityFromRows(raw);
                        stock.put("flour",String.valueOf(restored));
                        if(!raw.trim().isEmpty()) stock.put("items_rows",raw);
                        ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module","stock"); cv.put("payload",stock.toString()); cv.put("updated_at",System.currentTimeMillis());
                        db.insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
                    }
                } finally { c.close(); }
                db.delete("records","day=? AND module='cuts'",new String[]{date});
                db.delete("inventory_usage","day=?",new String[]{date});
                JSONObject cost=get(date,"costing"); cost.put("flour_bags","0");
                ContentValues costCv=new ContentValues(); costCv.put("day",date); costCv.put("module","costing"); costCv.put("payload",cost.toString()); costCv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,costCv,SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } catch(Exception ex) {
                throw new IllegalStateException("تعذر إرجاع المخزون بأمان؛ لم تكتمل عملية مسح شغل اليوم.",ex);
            } finally { db.endTransaction(); }
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
            Cursor u=getReadableDatabase().rawQuery("SELECT day,flour_bags,material_usage FROM inventory_usage ORDER BY day",null);
            try {
                while(u.moveToNext()) {
                    JSONObject row=new JSONObject(); row.put("day",u.getString(0)); row.put("module","inventory_usage");
                    JSONObject payload=new JSONObject(); payload.put("flour_bags",u.getLong(1));
                    try { payload.put("material_usage",new JSONObject(u.getString(2))); } catch(Exception ignored) { payload.put("material_usage",new JSONObject()); }
                    row.put("payload",payload); row.put("updated_at",System.currentTimeMillis()); result.put(row);
                }
            } finally { u.close(); }
            return result;
        }
        int restoreAll(JSONArray rows) throws Exception {
            java.util.ArrayList<ContentValues> prepared=new java.util.ArrayList<>();
            java.util.HashSet<String> allowed=new java.util.HashSet<>(java.util.Arrays.asList("stock","cuts","workers","window","distribution","expenses","spoilage","costing","accounting","inventory_usage"));
            for(int i=0;i<rows.length();i++) {
                JSONObject row=rows.getJSONObject(i); String date=row.getString("day"), module=row.getString("module");
                if(!BakeryDataValidation.isValidIsoDate(date) || !allowed.contains(module)) throw new IllegalArgumentException("Invalid backup record date or module");
                JSONObject payload=row.getJSONObject("payload");
                if ("inventory_usage".equals(module)) {
                    if (!payload.has("flour_bags")) throw new IllegalArgumentException("Backup inventory usage is missing flour_bags");
                    BakeryDataValidation.requireNonNegative(payload.getLong("flour_bags"), "inventory_usage.flour_bags");
                }
                ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module",module); cv.put("payload",payload.toString());
                cv.put("updated_at",row.optLong("updated_at",System.currentTimeMillis())); prepared.add(cv);
            }
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                for(ContentValues cv:prepared) {
                    if ("inventory_usage".equals(cv.getAsString("module"))) {
                        JSONObject payload=new JSONObject(cv.getAsString("payload"));
                        ContentValues uv=new ContentValues(); uv.put("day",cv.getAsString("day")); uv.put("flour_bags",payload.getLong("flour_bags"));
                        uv.put("material_usage",payload.optJSONObject("material_usage")!=null?payload.optJSONObject("material_usage").toString():"{}");
                        db.insertWithOnConflict("inventory_usage",null,uv,SQLiteDatabase.CONFLICT_REPLACE);
                    } else {
                        db.insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
                db.setTransactionSuccessful();
            }
            finally { db.endTransaction(); }
            return prepared.size();
        }

    }
}
