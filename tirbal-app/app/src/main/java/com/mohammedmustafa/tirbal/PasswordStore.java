package com.mohammedmustafa.tirbal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class PasswordStore {
    private static final String PREF = "tirbal_security";
    private static final String SALT = "salt";
    private static final String HASH = "hash";
    private static final int ITERATIONS = 210000;

    static boolean configured(Context c) { return prefs(c).contains(HASH); }

    static boolean verify(Context c, String password) {
        String salt = prefs(c).getString(SALT, null);
        String expected = prefs(c).getString(HASH, null);
        if (salt == null || expected == null) return false;
        return constantTimeEquals(expected, derive(password, Base64.decode(salt, Base64.NO_WRAP)));
    }

    static void set(Context c, String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        prefs(c).edit().putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(HASH, derive(password, salt)).apply();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String derive(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
        try {
            byte[] result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("تعذر تأمين كلمة المرور", e);
        } finally { spec.clearPassword(); }
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] y = b.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int diff = 0;
        for (int i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
        return diff == 0;
    }
}
