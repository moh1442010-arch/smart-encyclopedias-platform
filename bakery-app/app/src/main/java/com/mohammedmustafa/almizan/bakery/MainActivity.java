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
import android.widget.ImageView;
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
    private static final int REQUEST_EXPORT_BACKUP = 4250, REQUEST_IMPORT_BACKUP = 4251;
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
        send.setOnClickListener(v -> openWhatsApp("تقرير تعطل الميزان المحاسبي للمخابز 2.5.8\n" + report));
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
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_wheat_logo);
        logo.setContentDescription("شعار سنابل القمح والخبز");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        head.addView(logo, new LinearLayout.LayoutParams(58,60));
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
        base("🥖 الميزان المحاسبي للمخابز 2.5.8");
        TextView intro = text("نظام يومي للمخبز: أدخل بيانات كل قسم، احفظها بتاريخها، ثم افتح «المحاسب والحساب» لمراجعة التسوية.",16);
        content.addView(intro);
        LinearLayout dateRow = new LinearLayout(this); dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateLabel = text("📅 التاريخ: " + day,16); dateRow.addView(dateLabel,new LinearLayout.LayoutParams(0,-2,1f));
        Button choose = button("اختيار التاريخ"); choose.setOnClickListener(v -> chooseDate()); dateRow.addView(choose); content.addView(dateRow);
        statusLabel = text("",14); content.addView(statusLabel);
        addDashboardSummary();
        // The nine home icons follow the bakery owner's approved daily workflow order.
        addCard("📦 1. المخزن ومحتوياته","الدقيق والخميرة والزيت والأكياس والحطب والغاز والجاز والملح والمعدات. أدخل الأرصدة والأسعار؛ رصيد الدقيق يُحدّث تلقائيًا مع شغل اليوم.",()->openModule("stock"));
        addCard("🥖 2. القطعيات وشغل اليوم","سجّل كل شغل أو كيس وعدد الأرغفة المنتجة، ثم راجع إجمالي الإنتاج وقيمته تلقائيًا حسب سعر القطعيات.",()->openModule("cuts"));
        addCard("🧾 3. منصرفات المحل","وجبات العمال والصيانة والمشتريات وكل ما يُصرف داخل المحل. سجّل كل مصروف مرة واحدة فقط.",()->openModule("expenses"));
        addCard("👷 4. قيد يومية العمال","اسم العامل ووظيفته وعدد الأشغال ومستحق الجوال أو الوحدة؛ يحسب التطبيق إجمالي الاستحقاقات تلقائيًا.",()->openModule("workers"));
        addCard("💵 5. شباك البيع","سجّل النقد والديون والتحويلات عبر بنكك، مع مرجع العملية وملاحظات البيع.",()->openModule("window"));
        addCard("🚚 6. التوزيع","اسم العميل والكمية والنقد والديون والتحويل وفرق السعر؛ تُجمع تفاصيل العملاء تلقائيًا في التسوية.",()->openModule("distribution"));
        addCard("♻️ 7. التالف والمنصرفات","سجّل العيش التالف وسبب التلف هنا، وراجع تفاصيل المنصرفات من الأيقونة 3 لتجنب تكرار المصروف.",()->openModule("spoilage"));
        addCard("🧮 8. المحاسب والحساب","راجع الإنتاج والكاش والديون وبنكك والتوزيع والعمال والمنصرفات والتالف وفرق التسوية؛ يجب تفسير أي فرق قبل اعتماد اليوم.",this::showAccounts);
        addCard("🏭 9. حساب التشغيل وهامش الربح","راجع تكلفة الدقيق والوقود والخميرة والزيت والملح والأكياس، ثم هامش الربح التقديري بعد التالف والعمال والمنصرفات.",this::showCosting);
        Button backup = button("🗂️ نسخ احتياطي واستعادة البيانات"); backup.setOnClickListener(v->backupRestoreMenu()); content.addView(backup);
        Button guide = button("📘 اللوحة الإرشادية وطريقة الاستخدام"); guide.setOnClickListener(v->showGuide()); content.addView(guide);
        Button pricing = button("🔐 الاشتراك والأسعار والتفعيل"); pricing.setOnClickListener(v->showPaywall()); content.addView(pricing);
        Button support = button("🟢 الدعم الفني والتفعيل عبر واتساب"); support.setOnClickListener(v->openWhatsApp("السلام عليكم، أحتاج دعمًا فنيًا لتطبيق الميزان المحاسبي للمخابز 2.5.8.")); content.addView(support);
        updateStatus();
    }

    private void addDashboardSummary() {
        content.addView(text("📊 ملخص المخبز لهذا اليوم",18));
        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        addMetricTile(first, "flour", "📦 رصيد الدقيق", fmt(stockFlourValue()) + " جوال", "المتاح بالمخزن");
        addMetricTile(first, "work", "🥖 شغل اليوم", fmt(val("cuts","work_units")) + " جوال", "خصم آلي من الرصيد");
        content.addView(first);
        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);
        long production = 0L, margin = 0L;
        try {
            production = productionValue();
            margin = BakeryMath.netMargin(production, spoilageValue(), wages(), expenseTotal(), costingTotal());
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
                f("flour","رصيد الدقيق قبل خصم شغل اليوم — الكمية بالجوال"),f("flour_unit_price","سعر جوال الدقيق الواحد (جنيه)"),
                f("fuel_other","رصيد الوقود الآخر — الكمية"),f("fuel_other_unit_price","سعر وحدة الوقود الآخر (جنيه)"),
                f("yeast","الخميرة — الكمية"),f("yeast_unit_price","سعر وحدة الخميرة الواحدة (جنيه)"),
                f("bags","الأكياس — الكمية"),f("bags_unit_price","سعر الكيس الواحد (جنيه)"),
                f("oil","الزيت — الكمية"),f("oil_unit_price","سعر وحدة الزيت (جنيه)"),
                f("salt","الملح — الكمية"),f("salt_unit_price","سعر وحدة الملح (جنيه)"),
                f("wood","الحطب — الكمية"),f("wood_unit_price","سعر وحدة الحطب (جنيه)"),
                f("gas","الغاز — الكمية"),f("gas_unit_price","سعر وحدة الغاز (جنيه)"),
                f("kerosene","الجاز — الكمية"),f("kerosene_unit_price","سعر وحدة الجاز (جنيه)"),
                f("equipment_quantity","المعدات/الجهاز — عدد الوحدات"),f("equipment_unit_price","سعر وحدة المعدات (جنيه)"),
                t("other_items","أصناف أخرى — سطر لكل صنف","الصنف | الكمية | سعر الوحدة\nمثال: صابون | 5 | 1000"),
                t("equipment","اسم الجهاز / المعدات وحالتها",""),
                t("notes","ملاحظات المخزن","")
            };
            case "cuts": return new Field[]{
                f("work_units","عدد جوالات الدقيق المستخدمة في شغل اليوم — مثال: 6 جوالات"),
                t("work_rows","تفاصيل القطعيات — أدخل كل شغل ثم اضغط حفظ. المثال في التلميح ليس بيانات محفوظة.","مثال فقط — اكتب داخل الحقل: 1 | 500\n2 | 510\n3 | 500\n4 | 500\n5 | 500\n6 | 500"),
                f("loaves_per_unit","عدد الأرغفة الناتجة من جوال دقيق واحد (للتقدير عند عدم إدخال التفاصيل)"),
                f("loaves_per_1000","عدد العيشات التي تساوي 1,000 جنيه — مثال: 4")
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
                f("fuel_quantity","وقود آخر غير مفصل — الكمية المستخدمة"),f("fuel_unit_price","سعر وحدة الوقود الآخر (جنيه)"),
                f("wood_quantity","الحطب — الكمية المستخدمة"),f("wood_unit_price","سعر وحدة الحطب (جنيه)"),
                f("gas_quantity","الغاز — الكمية المستخدمة"),f("gas_unit_price","سعر وحدة الغاز (جنيه)"),
                f("kerosene_quantity","الجاز — الكمية المستخدمة"),f("kerosene_unit_price","سعر وحدة الجاز (جنيه)"),
                f("flour_bags","الدقيق — الجوالات المستخدمة (تُزامن تلقائيًا مع شغل اليوم)"),f("flour_bag_price","سعر جوال الدقيق الواحد (جنيه)"),
                f("yeast_count","الخميرة — الكمية المستهلكة"),f("yeast_piece_price","سعر وحدة الخميرة (جنيه)"),
                f("bags_quantity","الأكياس — عدد الأكياس المستهلكة"),f("bags_unit_price","سعر الكيس الواحد (جنيه)"),
                f("oil_quantity","الزيت — الكمية المستهلكة"),f("oil_pound_price","سعر وحدة الزيت (جنيه)"),
                f("salt_quantity","الملح — الكمية المستهلكة"),f("salt_unit_price","سعر وحدة الملح (جنيه)"),
                f("bags_total_price","إجمالي قديم للأكياس (للتوافق مع السجلات السابقة فقط)"),
                f("equipment_operating_cost","مصروف تشغيل المعدات لهذا اليوم (جنيه)")
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

    private void openModule(String module) {
        Field[] fields = schema(module);
        JSONObject saved = store.get(day,module);
        if (module.equals("stock")) {
            JSONObject currentStock = store.get(day,"stock");
            if (currentStock.length() == 0) {
                JSONObject previous = store.getLatest(day,"stock");
                if (previous.length() > 0) saved = previous;
            }
            JSONObject appliedCosting=store.get(day,"costing");
            JSONObject appliedItems=appliedCosting.optJSONObject("inventory_applied");
            if(appliedItems!=null) {
                String[][] itemKeys={{"yeast","yeast"},{"bags","bags"},{"oil","oil"},{"salt","salt"},{"wood","wood"},{"gas","gas"},{"kerosene","kerosene"},{"fuel_other","fuel_other"}};
                for(String[] pair:itemKeys) { long used=appliedItems.optLong(pair[1],0L); if(used>0) try { saved.put(pair[0],String.valueOf(Math.addExact(parseLong(saved.optString(pair[0],"0")),used))); } catch(Exception ex) { toast("تعذر تجهيز رصيد "+pair[0]+" للتحرير؛ راجع الاستهلاك قبل الحفظ."); } }
            }
            long appliedUsage=store.usageForDate(day);
            if(appliedUsage>0) try { saved.put("flour",String.valueOf(Math.addExact(parseLong(saved.optString("flour","0")),appliedUsage))); } catch(Exception ex) { toast("تعذر تجهيز رصيد الدقيق للتحرير. لا تحفظ التغييرات حتى مراجعة البيانات."); }
        }
        if (module.equals("costing")) {
            // Pull inventory unit prices into costing as defaults; the baker may adjust actual daily usage.
            JSONObject prices = store.getLatest(day, "stock");
            String[][] priceMap = {
                {"flour_bag_price","flour_unit_price"},
                {"yeast_piece_price","yeast_unit_price"},
                {"oil_pound_price","oil_unit_price"},
                {"salt_unit_price","salt_unit_price"},
                {"wood_unit_price","wood_unit_price"},
                {"gas_unit_price","gas_unit_price"},
                {"kerosene_unit_price","kerosene_unit_price"},
                {"bags_unit_price","bags_unit_price"},
                {"fuel_unit_price","fuel_other_unit_price"}
            };
            for (String[] pair : priceMap) {
                if (saved.optString(pair[0], "").trim().isEmpty()) {
                    String price = prices.optString(pair[1], "").trim();
                    if (!price.isEmpty()) try { saved.put(pair[0], price); } catch(Exception ignored) {}
                }
            }
            if (val("cuts","work_units") > 0) {
                try { saved.put("flour_bags", String.valueOf(val("cuts","work_units"))); } catch(Exception ignored) {}
            }
        }
        ScrollView sc = new ScrollView(this); LinearLayout box = new LinearLayout(this);
        box.setPadding(14,8,14,12); box.setOrientation(LinearLayout.VERTICAL); sc.addView(box);
        final java.util.Map<String,EditText> inputs = new java.util.LinkedHashMap<>();
        for(Field field:fields) {
            box.addView(text(field.label,15));
            EditText e = new EditText(this); e.setTextSize(16); e.setHint(field.hint); e.setPadding(10,8,10,8);
            if(field.numeric) { e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED); e.setSingleLine(true); }
            else { e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); e.setMinLines(field.key.equals("rows")?4:1); e.setGravity(Gravity.TOP|Gravity.START); }
            if (module.equals("costing") && field.key.equals("flour_bags")) { e.setEnabled(false); e.setAlpha(0.75f); }
            e.setText(saved.optString(field.key,"")); inputs.put(field.key,e); box.addView(e);
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
                        if (module.equals("stock") && field.key.equals("other_items")) validateStockRows(val);
                        if (module.equals("cuts") && field.key.equals("work_rows")) validateWorkRows(val);
                        record.put(field.key,val);
                    }
                    if (module.equals("cuts")) {
                        long newWork = parseLong(record.optString("work_units","0"));
                        store.saveCutsWithInventory(day, record, newWork);
                        updateStatus();
                        toast("حُفظ شغل اليوم. تم تحديث رصيد الدقيق والتكلفة تلقائيًا.");
                    } else if (module.equals("stock")) {
                        store.saveStockWithInventory(day, record);
                        updateStatus();
                        toast("حُفظ رصيد المخزن. الرصيد الظاهر في الملخص هو المتبقي بعد خصم شغل اليوم.");
                    } else {
                        if (module.equals("costing")) store.saveCostingWithInventory(day,record); else store.put(day,module,record); updateStatus(); toast("حُفظ "+titleFor(module)+" بتاريخ "+day+"؛ تم تحديث استهلاك المواد في المخزن إن كانت الكميات محددة.");
                    }
                } catch(Exception ex) { toast("لم تُحفظ البيانات: "+ex.getMessage()); }
            }).show();
    }

    private void validateStockRows(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        String[] lines = raw.split("\\r?\\n");
        for (int i=0;i<lines.length;i++) {
            String line=lines[i].trim();
            if (line.isEmpty()) continue;
            String[] p=line.split("\\|",-1);
            if (p.length != 3 || p[0].trim().isEmpty())
                throw new IllegalArgumentException("صنف المخزن، السطر "+(i+1)+": اكتب الصنف | الكمية | سعر الوحدة.");
            long quantity=parseLong(p[1]), price=parseLong(p[2]);
            if (quantity < 0 || price < 0)
                throw new IllegalArgumentException("كمية وسعر الصنف لا يمكن أن يكونا سالبين.");
        }
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
    private long stockFlourValue() {
        JSONObject stock = store.get(day, "stock");
        if (stock.length() == 0) stock = store.getLatest(day, "stock");
        try { return Math.max(0L, parseLong(stock.optString("flour", "0"))); }
        catch (Exception ignored) { return 0L; }
    }

    private boolean hasProductionInput() {
        String rows=store.get(day,"cuts").optString("work_rows","").trim();
        return !rows.isEmpty() || (val("cuts","work_units")>0 && val("cuts","loaves_per_unit")>0);
    }
    private long productionLoaves() {
        String rows=store.get(day,"cuts").optString("work_rows","").trim();
        if (!rows.isEmpty()) return BakeryMath.sumWorkRows(rows);
        return BakeryMath.productionLoaves(val("cuts","work_units"),val("cuts","loaves_per_unit"));
    }
    private void validateWorkRows(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        BakeryMath.sumWorkRows(raw);
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
    private Totals distribution() { return rowTotals("distribution","rows",6); }
    private long productionValue() { return valueForLoaves(productionLoaves()); }
    private long costingTotal() {
        long bagCost = val("costing","bags_quantity") > 0
            ? Math.multiplyExact(val("costing","bags_quantity"),val("costing","bags_unit_price"))
            : val("costing","bags_total_price");
        long materials = BakeryMath.operatingCost(
            val("costing","fuel_quantity"),val("costing","fuel_unit_price"),
            val("cuts","work_units"),val("costing","flour_bag_price"),
            val("costing","yeast_count"),val("costing","yeast_piece_price"),
            val("costing","oil_quantity"),val("costing","oil_pound_price"),
            val("costing","salt_quantity"),val("costing","salt_unit_price"),
            bagCost);
        long wood = Math.multiplyExact(val("costing","wood_quantity"),val("costing","wood_unit_price"));
        long gas = Math.multiplyExact(val("costing","gas_quantity"),val("costing","gas_unit_price"));
        long kerosene = Math.multiplyExact(val("costing","kerosene_quantity"),val("costing","kerosene_unit_price"));
        return Math.addExact(Math.addExact(Math.addExact(materials,wood),gas),
            Math.addExact(kerosene,val("costing","equipment_operating_cost")));
    }

    private void showCosting() {
        try {
            long production=productionValue(), cost=costingTotal();
            long spoilage=spoilageValue(), labor=wages(), expenses=expenseTotal();
            long beforeCost=Math.subtractExact(Math.subtractExact(Math.subtractExact(production,spoilage),labor),expenses);
            long margin=BakeryMath.netMargin(production,spoilage,labor,expenses,cost);
            String message="تاريخ التكلفة: "+day+
                "\nقيمة القطعيات/الإنتاج: "+fmt(production)+" جنيه"+
                "\n(-) قيمة التالف: "+fmt(spoilage)+" جنيه"+
                "\n(-) يومية العمال: "+fmt(labor)+" جنيه"+
                "\n(-) منصرفات المحل: "+fmt(expenses)+" جنيه"+
                "\nالصافي قبل تكلفة التشغيل: "+fmt(beforeCost)+" جنيه"+
                "\n(-) تكلفة المواد والوقود الحطب/الغاز/الجاز وتشغيل المعدات: "+fmt(cost)+" جنيه"+
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
        // Revenue reconciliation must compare sales channels only; expenses and payroll belong in the profit calculation.
        return BakeryMath.salesRecordedTotal(cashTotal(),debtTotal(),bankTotal(),distributionDiff());
    }
    private String fmt(long n) { return String.format(Locale.US,"%,d",n); }

    private void showAccounts() {
        try {
            boolean productionEntered=hasProductionInput();
            long loaves=productionEntered?productionLoaves():0L, expected=productionEntered?productionValue():0L, actual=accountedTotal(), difference=productionEntered?Math.subtractExact(expected,actual):0L;
            Totals dist=distribution();
            Totals workerRows=rowTotals("workers","rows",4);
            Totals expenseRows=rowTotals("expenses","rows",2);
            int bad=workerRows.badRows+dist.badRows+expenseRows.badRows;
            String status=!productionEntered?"بيانات الإنتاج غير مكتملة — لا تعتمد التسوية":bad>0?"بيانات السطور غير مكتملة — لا تعتمد التسوية":difference==0?"متوازن حسابيًا":difference>0?"عجز غير مفسر":"زيادة غير مفسرة";
            String message="تاريخ الدفتر: "+day+
                "\n\nالعيش المنتج: "+(productionEntered?fmt(loaves)+" رغيف":"غير محسوب — أدخل سطور القطعيات واحفظها")+
                "\nقيمة القطعيات حسب السعر المدخل: "+(productionEntered?fmt(expected)+" جنيه":"غير محسوبة حتى تسجيل الإنتاج")+
                "\nالكاش (الشباك والتوزيع): "+fmt(cashTotal())+" جنيه"+
                "\nالديون المستحقة: "+fmt(debtTotal())+" جنيه"+
                "\nالتحويلات البنكية: "+fmt(bankTotal())+" جنيه"+
                "\nكمية التوزيع المسجلة: "+fmt(dist.qty)+" رغيف"+
                "\nفرق السعر: "+fmt(distributionDiff())+" جنيه"+
                "\nيومية العمال المحسوبة من السطور: "+fmt(wages())+" جنيه"+
                "\nمنصرفات المحل: "+fmt(expenseTotal())+" جنيه"+
                "\nقيمة التالف: "+fmt(spoilageValue())+" جنيه"+
                "\nتكلفة التشغيل التقديرية (للمقارنة فقط): "+fmt(costingTotal())+" جنيه"+
                "\nهامش الربح التقديري: "+(productionEntered?fmt(BakeryMath.netMargin(expected,spoilageValue(),wages(),expenseTotal(),costingTotal()))+" جنيه":"غير محسوب حتى تسجيل الإنتاج")+
                "\n\nإجمالي المبيعات المسجلة (الكاش + الديون + بنكك + فرق السعر): "+fmt(actual)+" جنيه"+
                "\nفرق تسوية المبيعات (المطلوب صفر): "+(!productionEntered?"غير محسوب حتى تسجيل الإنتاج":bad>0?"غير معتمد حتى تصحيح السطور":fmt(difference)+" جنيه")+
                "\nالنتيجة: "+status+
                (!productionEntered?"\n\n⚠ لم تُحفظ بيانات إنتاج لهذا اليوم. افتح القطعيات، اكتب السطور الستة فعليًا (1 | 500 إلى 6 | 500) ثم اضغط حفظ بيانات القسم؛ النص الرمادي في التلميح مثال فقط ولا يُحتسب.":"")+ 
                (bad>0?"\n\n⚠ توجد "+bad+" سطور غير مكتملة أو غير رقمية: يومية العمال "+workerRows.badRows+"، التوزيع "+dist.badRows+"، المنصرفات "+expenseRows.badRows+". صحح السطور ثم أعد التسوية.":"")+
                (val("cuts","loaves_per_1000")<=0?"\n⚠ أدخل عدد العيش المقابل لـ 1,000 جنيه في القطعيات.":"");
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
        updateDashboardMetrics();
    }

    private void updateDashboardMetrics() {
        if (flourMetric == null) return;
        flourMetric.setText(fmt(val("stock","flour")) + " جوال");
        if (workMetric != null) workMetric.setText(fmt(val("cuts","work_units")) + " جوال");
        try {
            long production = productionValue();
            long margin = BakeryMath.netMargin(production, spoilageValue(), wages(), expenseTotal(), costingTotal());
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
        String guide="طريقة الاستخدام — الميزان المحاسبي للمخابز 2.5.8\n\n"+
            "1) اختر التاريخ من رزنامة الهاتف داخل التطبيق؛ لكل تاريخ دفتر مستقل.\n"+
            "2) المخزن: أدخل كمية كل صنف وسعر الوحدة الواحدة، بما في ذلك الدقيق والخميرة والأكياس والزيت والملح والحطب والغاز والجاز والمعدات. للأصناف الإضافية استخدم سطرًا لكل صنف بالصيغة: الصنف | الكمية | سعر الوحدة. أدخل رصيد الدقيق الافتتاحي قبل شغل اليوم؛ يُخصم عدد الجوالات المستخدمة تلقائيًا دون خصم مزدوج.\n"+
            "3) القطعيات: أدخل عدد جوالات الدقيق المستخدمة، ثم سطرًا لكل شغل بصيغة رقم الشغل | عدد العيش، مثل 1 | 500 و2 | 510. يجمع التطبيق التفاصيل ويعرض قيمة القطعيات = مجموع العيش × 1,000 ÷ عدد العيشات التي تساوي 1,000 جنيه. إذا تركت التفاصيل فارغة يستخدم تقدير عدد الجوالات × الأرغفة لكل جوال.\n"+
            "4) يومية العمال: كل عامل في سطر بصيغة الاسم | الوظيفة | عدد الشغل | أجرة الوحدة. الأجر = عدد الشغل × أجرة الوحدة.\n"+
            "5) بيع الشباك: أدخل الكاش والديون والتحويلات. احتفظ برقم العملية في خانة المرجع.\n"+
            "6) التوزيع: كل عميل في سطر: الاسم | الكمية | الكاش | الديون | التحويل البنكي | فرق السعر.\n"+
            "7) منصرفات المحل: أدخل الوجبات والصيانة والمشتريات، ثم أي منصرف إضافي في سطر البيان | المبلغ. لا تكرر نفس المصروف في أكثر من خانة.\n"+
            "8) التالف: أدخل عدد الأرغفة التالفة؛ يحسب التطبيق قيمتها باستخدام سعر القطعيات.\n"+
            "9) المحاسب والحساب: تسوية المبيعات تقارن قيمة القطعيات بمجموع الكاش والديون وبنكك وفرق السعر فقط. لا تخلط يومية العمال والمنصرفات والتالف مع إيراد المبيعات؛ تُخصم هذه البنود لاحقًا في حساب هامش الربح. لا تسجل المبيعات نفسها في الشباك والتوزيع معًا؛ ذلك يكرر الإيراد. يجب أن يساوي فرق تسوية المبيعات صفرًا بعد منع التكرار.\n"+
            "10) حساب التشغيل والتكلفة: يحسب التطبيق تكلفة الدقيق من عدد جوالات شغل اليوم × سعر الجوال، ويحسب الخميرة والأكياس والزيت والملح والوقود والحطب والغاز والجاز من الكمية المستهلكة × سعر الوحدة، ويضيف مصروف تشغيل المعدات. عند حفظ استهلاك المواد تُخصم الكميات المحددة من أرصدة المخزن، ويُمنع الحفظ إذا كان الرصيد غير كافٍ. أدخل الكمية المستهلكة فقط لا قيمة شراء المخزون كله، ثم يخصم التطبيق التالف ويومية العمال والمنصرفات وتكلفة التشغيل من قيمة القطعيات لعرض هامش الربح. لا تكرر المصروف نفسه.\n\n"+
            "مهم: البيانات تحفظ محليًا على الجهاز ولا تُرفع للسحابة. استخدم زر «نسخ احتياطي واستعادة البيانات» لتصدير ملف JSON إلى مكان آمن واختبر استعادته. التطبيق لا يُصدر قيدًا محاسبيًا عامًا ولا يُعد بديلًا عن مراجعة محاسب قبل استخدام النتائج لإقرار مالي أو ضريبي.";
        new AlertDialog.Builder(this).setTitle("📘 اللوحة الإرشادية").setMessage(guide).setPositiveButton("فهمت",null).show();
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
        base("🔐 اشتراك الميزان المحاسبي للمخابز");
        boolean licensed = hasActiveLicense();
        boolean expired = trialExpired();
        String trialMessage = licensed
            ? "اشتراكك مفعّل على هذا الجهاز. يمكنك متابعة الدفاتر أو طلب تجديد الباقة عند الحاجة."
            : expired
                ? "انتهت فترة التجربة المجانية (7 أيام)، وتم إيقاف الوصول إلى الدفاتر حتى التفعيل. بياناتك المحلية محفوظة ولن تُحذف. اختر الباقة وحوّل المبلغ عبر بنكك ثم أرسل الإيصال عبر واتساب لطلب التفعيل."
                : "أهلًا بك في الميزان المحاسبي للمخابز. بقي نحو " + trialDaysRemaining() + " يومًا من التجربة المجانية. يمكنك اختيار باقة مسبقًا؛ ستظل بياناتك محفوظة على الهاتف.";
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
        Button activate=button("🔑 أدخل رمز التفعيل بعد موافقة الدعم");
        activate.setOnClickListener(v->showActivationDialog());
        content.addView(activate);
        content.addView(text("الدفع عبر بنكك — رقم الحساب: 1882224",16));
        Button copy=button("نسخ رقم حساب بنكك");
        copy.setOnClickListener(v->{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(android.content.ClipData.newPlainText("Bankak",BANKAK));toast("تم نسخ رقم بنكك");});
        content.addView(copy);
        content.addView(text("بعد التحويل، أرفق صورة الإيصال داخل محادثة واتساب. لا ترسل الرقم السري أو رمز التحقق الخاص ببنكك. التفعيل يتم بعد مراجعة التحويل.",14));
        Button backup=button("🗂️ تصدير / استعادة بياناتك");
        backup.setOnClickListener(v->backupRestoreMenu());
        content.addView(backup);
        if (!trialExpired() || hasActiveLicense()) { Button back=button("العودة إلى التطبيق"); back.setOnClickListener(v->buildHome()); content.addView(back); }
    }

    private void requestActivation(String plan,String amount) {
        EditText devices=new EditText(this);
        devices.setSingleLine(true);
        devices.setInputType(InputType.TYPE_CLASS_NUMBER);
        devices.setText("1");
        devices.setHint("عدد الأجهزة المطلوب ترخيصها");
        LinearLayout box=new LinearLayout(this); box.setPadding(24,8,24,4); box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text("الباقة: "+plan+"\nالمبلغ المطلوب: "+amount+"\nحساب بنكك: "+BANKAK+"\n\nحوّل المبلغ أولًا، ثم انسخ رقم الحساب أو أرسل طلب واتساب وأرفق إيصال التحويل. سيُراجع الدعم التحويل قبل إصدار الترخيص.",15));
        box.addView(text("عدد الأجهزة المطلوب ترخيصها (1 إلى 20):",14)); box.addView(devices);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("تأكيد طلب الاشتراك").setView(box)
            .setNegativeButton("إلغاء",null).setPositiveButton("نسخ رقم الحساب",null)
            .setNeutralButton("إرسال الطلب عبر واتساب",null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Bankak",BANKAK));
                toast("تم نسخ رقم بنكك: "+BANKAK);
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String count=devices.getText().toString().trim();
                int n; try { n=Integer.parseInt(normalizePin(count)); } catch(Exception ex) { devices.setError("أدخل عدد الأجهزة"); return; }
                if(n<1 || n>20) { devices.setError("العدد من 1 إلى 20"); return; }
                String message="السلام عليكم، أطلب ترخيص تطبيق الميزان المحاسبي للمخابز 2.5.8.\n"+
                    "الباقة المختارة: "+plan+"\nالمبلغ: "+amount+"\nرقم حساب بنكك: "+BANKAK+"\n"+
                    "تاريخ الطلب: "+day+"\nمعرّف الجهاز الأول: "+deviceId()+"\nعدد الأجهزة المطلوب ترخيصها: "+n+"\n"+
                    "رقم عملية بنكك: [أدخل رقم العملية]\nسأرفق صورة الإيصال. أرجو مراجعة التحويل ثم إصدار ترخيص موقّع لكل جهاز بعد الموافقة. إذا كانت الأجهزة أكثر من واحد سأرسل معرّف كل جهاز.";
                dialog.dismiss(); openWhatsApp(message);
            });
        });
        dialog.show();
    }

    private void openWhatsApp(String message) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/"+SUPPORT+"?text="+Uri.encode(message)))); }
        catch(Exception e) { toast("تعذر فتح واتساب. الرقم: +249 121 851 285"); }
    }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }

    private class Store extends SQLiteOpenHelper {
        Store(){super(MainActivity.this,"almizan_bakery_250.db",null,3);}
        @Override public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE records (day TEXT NOT NULL, module TEXT NOT NULL, payload TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(day,module))");
            db.execSQL("CREATE TABLE inventory_usage (day TEXT PRIMARY KEY NOT NULL, flour_bags INTEGER NOT NULL DEFAULT 0)");
        }
        @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
            if (oldVersion < 2) db.execSQL("CREATE TABLE IF NOT EXISTS inventory_usage (day TEXT PRIMARY KEY NOT NULL, flour_bags INTEGER NOT NULL DEFAULT 0)");
            // Version 3 retains existing records; daily material consumption is stored in costing JSON.
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

        void saveCutsWithInventory(String date,JSONObject cuts,long newWork) throws Exception {
            if (newWork < 0) throw new IllegalArgumentException("عدد جوالات الدقيق لا يمكن أن يكون سالبًا");
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                long previouslyApplied=0L;
                Cursor usage=db.rawQuery("SELECT flour_bags FROM inventory_usage WHERE day=?",new String[]{date});
                try { if(usage.moveToFirst()) previouslyApplied=usage.getLong(0); } finally { usage.close(); }
                if(newWork!=previouslyApplied && hasFutureStockAfter(date))
                    throw new IllegalStateException("لا يمكن تغيير استخدام الدقيق ليوم سابق بعد تسجيل أرصدة مخزن لأيام لاحقة؛ صحح التسلسل الزمني أولًا.");
                JSONObject stock=getLatest(date,"stock");
                if (stock.length()==0 && (newWork>0 || previouslyApplied>0)) throw new IllegalStateException("أدخل رصيد الدقيق في قسم المخزن أولًا، ثم احفظ شغل اليوم.");
                if (stock.length()>0) {
                    String raw=stock.optString("flour","");
                    if (raw.trim().isEmpty() && (newWork>0 || previouslyApplied>0)) throw new IllegalStateException("رصيد الدقيق غير محدد. أدخل عدد الجوالات في المخزن أولًا.");
                    long current=raw.trim().isEmpty()?0L:parseLong(raw);
                    long adjusted=BakeryMath.stockAfterUsage(current,previouslyApplied,newWork);
                    if (adjusted<0) throw new IllegalStateException("رصيد الدقيق لا يكفي لشغل اليوم. الرصيد الحالي: "+current+" جوال.");
                    stock.put("flour",String.valueOf(adjusted));
                    ContentValues scv=new ContentValues(); scv.put("day",date); scv.put("module","stock"); scv.put("payload",stock.toString()); scv.put("updated_at",System.currentTimeMillis());
                    db.insertWithOnConflict("records",null,scv,SQLiteDatabase.CONFLICT_REPLACE);
                }
                ContentValues uv=new ContentValues(); uv.put("day",date); uv.put("flour_bags",newWork);
                db.insertWithOnConflict("inventory_usage",null,uv,SQLiteDatabase.CONFLICT_REPLACE);
                JSONObject cost=get(date,"costing"); cost.put("flour_bags",String.valueOf(newWork));
                ContentValues ccv=new ContentValues(); ccv.put("day",date); ccv.put("module","costing"); ccv.put("payload",cost.toString()); ccv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,ccv,SQLiteDatabase.CONFLICT_REPLACE);
                ContentValues cutcv=new ContentValues(); cutcv.put("day",date); cutcv.put("module","cuts"); cutcv.put("payload",cuts.toString()); cutcv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,cutcv,SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        void saveCostingWithInventory(String date,JSONObject costing) throws Exception {
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                if(hasFutureStockAfter(date)) throw new IllegalStateException("لا يمكن تعديل استهلاك مواد يوم سابق بعد تسجيل مخزون لأيام لاحقة؛ راجع التسلسل الزمني أولًا.");
                JSONObject stock=get(date,"stock");
                if(stock.length()==0) throw new IllegalStateException("احفظ رصيد المخزن لهذا التاريخ أولًا قبل تسجيل استهلاك المواد.");
                JSONObject old=get(date,"costing"); JSONObject oldApplied=old.optJSONObject("inventory_applied");
                if(oldApplied==null) oldApplied=new JSONObject();
                JSONObject requested=new JSONObject();
                String[][] items={{"yeast_count","yeast"},{"bags_quantity","bags"},{"oil_quantity","oil"},{"salt_quantity","salt"},{"wood_quantity","wood"},{"gas_quantity","gas"},{"kerosene_quantity","kerosene"},{"fuel_quantity","fuel_other"}};
                for(String[] item:items) {
                    long prior=oldApplied.optLong(item[1],0L), next=parseLong(costing.optString(item[0],"0"));
                    if(next<0) throw new IllegalArgumentException("كمية الاستهلاك لا يمكن أن تكون سالبة: "+item[1]);
                    String raw=stock.optString(item[1],"");
                    if(raw.trim().isEmpty() && (prior>0 || next>0)) throw new IllegalStateException("حدد رصيد الصنف «"+item[1]+"» في المخزن أولًا.");
                    long current=raw.trim().isEmpty()?0L:parseLong(raw);
                    long adjusted=Math.subtractExact(Math.addExact(current,prior),next);
                    if(adjusted<0) throw new IllegalStateException("رصيد الصنف "+item[1]+" لا يكفي. المتاح بعد إعادة الاستهلاك السابق: "+Math.addExact(current,prior)+". المطلوب: "+next+".");
                    stock.put(item[1],String.valueOf(adjusted)); requested.put(item[1],next);
                }
                ContentValues scv=new ContentValues(); scv.put("day",date); scv.put("module","stock"); scv.put("payload",stock.toString()); scv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,scv,SQLiteDatabase.CONFLICT_REPLACE);
                costing.put("inventory_applied",requested);
                ContentValues ccv=new ContentValues(); ccv.put("day",date); ccv.put("module","costing"); ccv.put("payload",costing.toString()); ccv.put("updated_at",System.currentTimeMillis());
                db.insertWithOnConflict("records",null,ccv,SQLiteDatabase.CONFLICT_REPLACE);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
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
                String raw=stockInput.optString("flour","");
                if(raw.trim().isEmpty() && usage>0) throw new IllegalStateException("أدخل رصيد الدقيق الافتتاحي قبل حفظ المخزن.");
                long opening=raw.trim().isEmpty()?0L:parseLong(raw);
                long closing=Math.subtractExact(opening,usage);
                if(closing<0) throw new IllegalStateException("رصيد الدقيق الافتتاحي أقل من شغل اليوم.");
                stockInput.put("flour",String.valueOf(closing));
                JSONObject costing=get(date,"costing"); JSONObject applied=costing.optJSONObject("inventory_applied");
                if(applied!=null) {
                    String[][] itemKeys={{"yeast","yeast"},{"bags","bags"},{"oil","oil"},{"salt","salt"},{"wood","wood"},{"gas","gas"},{"kerosene","kerosene"},{"fuel_other","fuel_other"}};
                    for(String[] pair:itemKeys) {
                        long consumed=applied.optLong(pair[1],0L); String rawQty=stockInput.optString(pair[0],"");
                        if(rawQty.trim().isEmpty() && consumed>0) throw new IllegalStateException("حدد رصيد "+pair[0]+" قبل حفظ المخزن.");
                        long openingQty=rawQty.trim().isEmpty()?0L:parseLong(rawQty), remaining=Math.subtractExact(openingQty,consumed);
                        if(remaining<0) throw new IllegalStateException("رصيد "+pair[0]+" أقل من الاستهلاك اليومي.");
                        stockInput.put(pair[0],String.valueOf(remaining));
                    }
                }
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
                Cursor c=db.rawQuery("SELECT payload FROM records WHERE day=? AND module='stock'",new String[]{date});
                try {
                    if(c.moveToFirst() && usage>0) {
                        JSONObject stock=new JSONObject(c.getString(0));
                        long closing=parseLong(stock.optString("flour","0"));
                        stock.put("flour",String.valueOf(Math.addExact(closing,usage)));
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
            Cursor u=getReadableDatabase().rawQuery("SELECT day,flour_bags FROM inventory_usage ORDER BY day",null);
            try {
                while(u.moveToNext()) {
                    JSONObject row=new JSONObject(); row.put("day",u.getString(0)); row.put("module","inventory_usage");
                    JSONObject payload=new JSONObject(); payload.put("flour_bags",u.getLong(1)); row.put("payload",payload);
                    row.put("updated_at",System.currentTimeMillis()); result.put(row);
                }
            } finally { u.close(); }
            return result;
        }
        int restoreAll(JSONArray rows) throws Exception {
            java.util.ArrayList<ContentValues> prepared=new java.util.ArrayList<>();
            java.util.HashSet<String> allowed=new java.util.HashSet<>(java.util.Arrays.asList("stock","cuts","workers","window","distribution","expenses","spoilage","costing","accounting","inventory_usage"));
            for(int i=0;i<rows.length();i++) {
                JSONObject row=rows.getJSONObject(i); String date=row.getString("day"), module=row.getString("module");
                if(!date.matches("\\d{4}-\\d{2}-\\d{2}") || !allowed.contains(module)) throw new IllegalArgumentException("Invalid backup record");
                JSONObject payload=row.getJSONObject("payload");
                ContentValues cv=new ContentValues(); cv.put("day",date); cv.put("module",module); cv.put("payload",payload.toString());
                cv.put("updated_at",row.optLong("updated_at",System.currentTimeMillis())); prepared.add(cv);
            }
            SQLiteDatabase db=getWritableDatabase(); db.beginTransaction();
            try {
                for(ContentValues cv:prepared) {
                    if ("inventory_usage".equals(cv.getAsString("module"))) {
                        JSONObject payload=new JSONObject(cv.getAsString("payload"));
                        ContentValues uv=new ContentValues(); uv.put("day",cv.getAsString("day")); uv.put("flour_bags",payload.getLong("flour_bags"));
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
