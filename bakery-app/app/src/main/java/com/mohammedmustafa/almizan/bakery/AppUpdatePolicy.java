package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Central remote minimum-version policy. Cached policy remains enforceable offline. */
public final class AppUpdatePolicy {
    private static final String ENDPOINT =
        "https://al-mizan-bakery-licensing.mohammedmustafa.workers.dev/api/app-policy";
    private static final String STORE_URL =
        "https://moh1442010-arch.github.io/smart-encyclopedias-platform/store/bakery.html";
    private static final AtomicBoolean CHECKING = new AtomicBoolean(false);
    private static final WeakHashMap<Activity, Boolean> SHOWN = new WeakHashMap<>();

    private AppUpdatePolicy() {}

    public static void enforceCached(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        int minimum = activity.getSharedPreferences("app_update_policy", 0)
            .getInt("minimum_version_code", 0);
        if (minimum > 0 && BuildConfig.VERSION_CODE < minimum) {
            String url = activity.getSharedPreferences("app_update_policy", 0)
                .getString("update_url", STORE_URL);
            String message = activity.getSharedPreferences("app_update_policy", 0)
                .getString("message", "يتطلب هذا الإصدار تحديث التطبيق لمتابعة الاستخدام.");
            showBlockingDialog(activity, message, url, minimum);
        }
    }

    public static void check(Activity activity) {
        if (activity == null || activity.isFinishing()) return;
        enforceCached(activity);
        if (!CHECKING.compareAndSet(false, true)) return;
        Thread thread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL url = new URL(ENDPOINT + "?versionCode=" + BuildConfig.VERSION_CODE);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setUseCaches(false);
                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) return;
                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                }
                JSONObject policy = new JSONObject(response.toString());
                if (!"com.mohammedmustafa.almizan.bakery".equals(policy.optString("appId"))) return;
                int minimum = policy.optInt("minimumSupportedVersionCode", 0);
                if (minimum < 0) return;
                String updateUrl = policy.optString("updateUrl", STORE_URL);
                Uri parsed = Uri.parse(updateUrl);
                if (!"https".equalsIgnoreCase(parsed.getScheme()) ||
                    !"moh1442010-arch.github.io".equalsIgnoreCase(parsed.getHost())) {
                    updateUrl = STORE_URL;
                }
                String message = policy.optString("message",
                    "يتطلب هذا الإصدار تحديث التطبيق لمتابعة الاستخدام.");
                activity.getSharedPreferences("app_update_policy", 0).edit()
                    .putInt("minimum_version_code", minimum)
                    .putString("update_url", updateUrl)
                    .putString("message", message)
                    .putLong("policy_checked_at", System.currentTimeMillis())
                    .apply();
                if (policy.optBoolean("mandatory", false) && BuildConfig.VERSION_CODE < minimum) {
                    final String finalUrl = updateUrl;
                    activity.runOnUiThread(() -> showBlockingDialog(activity, message, finalUrl, minimum));
                }
            } catch (Exception ignored) {
                // Preserve normal operation during temporary network failure; a previously cached
                // mandatory policy is enforced synchronously by enforceCached().
            } finally {
                if (connection != null) connection.disconnect();
                CHECKING.set(false);
            }
        }, "AlMizan-AppPolicy");
        thread.setDaemon(true);
        thread.start();
    }

    private static void showBlockingDialog(Activity activity, String message, String updateUrl, int minimumVersionCode) {
        if (activity == null || activity.isFinishing()) return;
        synchronized (SHOWN) {
            if (Boolean.TRUE.equals(SHOWN.get(activity))) return;
            SHOWN.put(activity, true);
        }
        activity.runOnUiThread(() -> {
            if (activity.isFinishing()) return;
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("تحديث إلزامي")
                .setMessage(message + "\\n\\nالإصدار المثبت: " + BuildConfig.VERSION_NAME +
                    "\\nالحد الأدنى المطلوب: " + minimumVersionCode + " أو أحدث.")
                .setPositiveButton("تحديث الآن", (d, which) -> {
                    try {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)));
                    } catch (Exception ignored) {
                        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL)));
                    }
                })
                .setCancelable(false)
                .create();
            dialog.setOnKeyListener((d, keyCode, event) -> keyCode == KeyEvent.KEYCODE_BACK);
            dialog.show();
        });
    }
}
