package com.mohammedmustafa.smartcompany;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class MainActivitySecurityValidationTest {
    private Context context;
    private ActivityController<MainActivity> controller;
    private MainActivity activity;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("smartcompany2.db");
        controller = Robolectric.buildActivity(MainActivity.class).setup();
        activity = controller.get();
    }

    @After
    public void tearDown() {
        if (activity != null && activity.db != null) activity.db.close();
        if (controller != null) controller.pause().stop().destroy();
        if (context != null) context.deleteDatabase("smartcompany2.db");
    }

    @Test
    public void sensitiveScreensBlockScreenshotsAndRecentAppPreviews() {
        int flags = activity.getWindow().getAttributes().flags;
        assertTrue("Sensitive company data must not be captured in screenshots",
                (flags & android.view.WindowManager.LayoutParams.FLAG_SECURE) != 0);
    }

    @Test
    public void repeatedIncorrectPinsTriggerPersistentTemporaryLockout() {
        SharedPreferences prefs = context.getSharedPreferences("test-pin-lockout", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        for (int i = 0; i < 5; i++) activity.recordPinFailure(prefs);
        assertTrue("Five failed attempts must trigger a temporary lockout", activity.pinLockRemainingMs(prefs) > 0);
        activity.clearPinFailures(prefs);
        assertEquals("Successful authentication reset must clear lockout", 0L, activity.pinLockRemainingMs(prefs));
        prefs.edit().clear().commit();
    }

    @Test
    public void legacyPinVerifierIsUpgradedAfterSuccessfulAuthentication() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences("test-pin-legacy-migration", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        byte[] salt = new byte[16];
        new java.security.SecureRandom().nextBytes(salt);
        String saltText = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP);
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                "2468".toCharArray(), salt, 120000, 256);
        String legacyHash;
        try {
            legacyHash = android.util.Base64.encodeToString(
                    javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                            .generateSecret(spec).getEncoded(),
                    android.util.Base64.NO_WRAP);
        } finally {
            spec.clearPassword();
        }
        prefs.edit().putString("pin_salt", saltText).putString("pin_hash", legacyHash).commit();

        assertTrue("A correct legacy PIN must still authenticate", activity.verifyPin("2468", prefs));
        assertFalse("A wrong PIN must not authenticate", activity.verifyPin("2469", prefs));
        assertNotEquals("Successful legacy login must upgrade the stored verifier",
                legacyHash, prefs.getString("pin_hash", ""));
        assertTrue("The upgraded verifier must match the current PBKDF2-HMAC-SHA256 scheme",
                prefs.getString("pin_hash", "").equals(activity.pinHash("2468",
                        prefs.getString("pin_salt", ""))));
        prefs.edit().clear().commit();
    }

    @Test
    public void backupKeyUsesStrongVersionedKdfAndRetainsLegacyDerivation() throws Exception {
        byte[] salt = new byte[16];
        java.util.Arrays.fill(salt, (byte) 7);
        byte[] current = activity.backupKey("correct horse battery staple", salt).getEncoded();
        byte[] legacy = activity.backupKeyLegacy("correct horse battery staple", salt).getEncoded();
        assertEquals("AES-256 key length must be preserved", 32, current.length);
        assertEquals("Legacy AES-256 backup key length must remain supported", 32, legacy.length);
        assertFalse("The new KDF must not silently reuse the legacy key derivation",
                java.security.MessageDigest.isEqual(current, legacy));
    }

    @Test
    public void restoreGateAcceptsCurrentSchemaAndRejectsFutureSchema() {
        assertTrue("Backups from the current schema must pass the schema-version gate",
                activity.supportedBackupSchemaVersion(MainActivity.DB.SCHEMA_VERSION));
        assertTrue("Previously supported schema versions remain eligible for validation",
                activity.supportedBackupSchemaVersion(8));
        assertFalse("A backup from a future unknown schema must be rejected",
                activity.supportedBackupSchemaVersion(MainActivity.DB.SCHEMA_VERSION + 1));
        assertFalse("Negative schema versions must be rejected",
                activity.supportedBackupSchemaVersion(-1));
    }

    @Test
    public void offlineLicenseGraceRejectsClockRollbackAndExpiredTrial() {
        SharedPreferences prefs = context.getSharedPreferences("test-offline-license-clock", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        long now = 1_800_000_000_000L;
        prefs.edit()
                .putString("mode", "trial")
                .putLong("expires_at", now + 10L * 24 * 60 * 60 * 1000)
                .putLong("last_verified_at", now - 60L * 60 * 1000)
                .putLong("last_seen_wall", now - 60L * 60 * 1000)
                .commit();

        assertTrue("A recently verified unexpired license may use the offline grace period",
                activity.offlineLicenseAllowed(prefs, now));
        assertFalse("A material clock rollback must fail closed while offline",
                activity.offlineLicenseAllowed(prefs, now - 24L * 60 * 60 * 1000));
        assertFalse("An expired trial must not pass the offline check",
                activity.offlineLicenseAllowed(prefs, now + 11L * 24 * 60 * 60 * 1000));
        prefs.edit().putString("mode", "licensed").putLong("expires_at", 0L)
                .putLong("last_verified_at", now - 60L * 60 * 1000)
                .putLong("last_seen_wall", now - 60L * 60 * 1000).commit();
        assertTrue("A perpetual license with no expiry may use the bounded offline grace period",
                activity.offlineLicenseAllowed(prefs, now));
        prefs.edit().clear().commit();
    }

    @Test
    public void chequeDatesRequireRealIsoCalendarDates() {
        assertTrue(activity.validIsoDate("2024-02-29", true));
        assertFalse(activity.validIsoDate("2025-02-29", true));
        assertFalse(activity.validIsoDate("2024-13-01", true));
        assertFalse(activity.validIsoDate("2024-02-30", true));
        assertFalse(activity.validIsoDate("", true));
        assertTrue(activity.validIsoDate("", false));
    }

    @Test
    public void freshInstallDoesNotCreateAUniversalDefaultPin() {
        SharedPreferences prefs = activity.getSharedPreferences("security", Context.MODE_PRIVATE);
        assertFalse("Fresh install must not silently enable a known default PIN", prefs.contains("pin"));
        assertFalse("Fresh install must wait for the user to create a PIN", prefs.contains("pin_hash"));
    }

    @Test
    public void pinIsSaltedAndVerifiedWithoutStoringPlaintext() {
        SharedPreferences prefs = activity.getSharedPreferences("test-security", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();

        assertTrue(activity.savePin("1234", prefs));
        assertTrue(prefs.contains("pin_salt"));
        assertTrue(prefs.contains("pin_hash"));
        assertFalse(prefs.contains("pin"));
        assertTrue(activity.verifyPin("1234", prefs));
        assertFalse(activity.verifyPin("1235", prefs));
    }

    @Test
    public void correctPinCanOpenDashboardWithoutThrowing() {
        SharedPreferences prefs = activity.getSharedPreferences("security", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        assertTrue("Test PIN must be stored before sign-in", activity.savePin("246810", prefs));

        activity.login();
        android.widget.EditText pinField = null;
        android.widget.Button signIn = null;
        for (int i = 0; i < activity.content.getChildCount(); i++) {
            android.view.View child = activity.content.getChildAt(i);
            if (child instanceof android.widget.EditText) {
                pinField = (android.widget.EditText) child;
            } else if (child instanceof android.widget.Button
                    && "دخول".contentEquals(((android.widget.Button) child).getText())) {
                signIn = (android.widget.Button) child;
            }
        }
        assertNotNull("Sign-in PIN field must be visible", pinField);
        assertNotNull("Sign-in button must be visible", signIn);
        pinField.setText("246810");
        signIn.performClick();

        assertTrue("Correct PIN must authenticate and render the dashboard", activity.authenticated);
        assertNotNull("Dashboard content must remain attached", activity.content);
        assertTrue("Dashboard must render at least one view", activity.content.getChildCount() > 0);
        prefs.edit().clear().commit();
    }
}
