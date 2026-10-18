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
    public void chequeDatesRequireRealIsoCalendarDates() {
        assertTrue(activity.validIsoDate("2024-02-29", true));
        assertFalse(activity.validIsoDate("2025-02-29", true));
        assertFalse(activity.validIsoDate("2024-13-01", true));
        assertFalse(activity.validIsoDate("2024-02-30", true));
        assertFalse(activity.validIsoDate("", true));
        assertTrue(activity.validIsoDate("", false));
    }

    @Test
    public void offlineGraceAllowsCachedPerpetualLicenseButNeverExpiredTrial() {
        long now = 1_800_000_000_000L;
        long verified = now - 60_000L;

        assertTrue("A recently verified perpetual license has no expiry timestamp",
                activity.cachedLicenseAllowsOffline("licensed", 0L, verified, now));
        assertTrue("A recently verified time-limited license remains valid before expiry",
                activity.cachedLicenseAllowsOffline("licensed", now + 60_000L, verified, now));
        assertFalse("An expired paid license must not pass offline checks",
                activity.cachedLicenseAllowsOffline("licensed", now - 1L, verified, now));
        assertTrue("An unexpired trial remains within offline grace",
                activity.cachedLicenseAllowsOffline("trial", now + 60_000L, verified, now));
        assertFalse("An expired trial must remain locked even inside offline grace",
                activity.cachedLicenseAllowsOffline("trial", now - 1L, verified, now));
        assertFalse("Unknown cached modes must fail closed",
                activity.cachedLicenseAllowsOffline("unknown", 0L, verified, now));
        assertFalse("Stale verification must fail closed",
                activity.cachedLicenseAllowsOffline("licensed", 0L, now - 3L * 24L * 60L * 60L * 1000L - 1L, now));
        assertFalse("A clock earlier than the last verification must fail closed",
                activity.cachedLicenseAllowsOffline("licensed", 0L, now + 1L, now));
    }

    @Test
    public void freshInstallDoesNotCreateAUniversalDefaultPin() {
        SharedPreferences prefs = context.getSharedPreferences("security", Context.MODE_PRIVATE);
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
    public void enterpriseControlCenterRequiresFreshLicensedEnterpriseEntitlement() {
        SharedPreferences prefs = context.getSharedPreferences("test-enterprise-entitlement", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        long now = System.currentTimeMillis();

        prefs.edit().putString("mode", "trial").putString("plan", "enterprise")
                .putString("term_type", "annual")
                .putLong("expires_at", now + 60_000L).putLong("last_verified_at", now).commit();
        assertFalse("A trial must not unlock Enterprise-only controls", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putString("mode", "licensed").putString("plan", "pro")
                .putString("term_type", "perpetual")
                .putLong("expires_at", 0L).putLong("last_verified_at", now).commit();
        assertFalse("A non-Enterprise paid license must not unlock Enterprise-only controls", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putString("mode", "licensed").putString("plan", "enterprise")
                .putString("term_type", "")
                .putLong("expires_at", 0L).putLong("last_verified_at", now).commit();
        assertFalse("A missing license term must fail closed", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putString("term_type", "monthly")
                .putLong("expires_at", 0L).commit();
        assertFalse("A monthly license with a missing expiry must not be treated as perpetual", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putString("term_type", "annual")
                .putLong("expires_at", now + 60_000L).commit();
        assertTrue("A recently verified active Enterprise annual license should unlock the control center", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putLong("expires_at", now - 1L).commit();
        assertFalse("An expired Enterprise license must not unlock the control center", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putString("term_type", "perpetual").putLong("expires_at", 0L)
                .putLong("last_verified_at", now).commit();
        assertTrue("A perpetual Enterprise license with no expiry should unlock the control center", activity.hasEnterpriseEntitlement(prefs));

        prefs.edit().putLong("last_verified_at", now - 73L * 60L * 60L * 1000L).commit();
        assertFalse("An Enterprise license beyond offline grace must fail closed", activity.hasEnterpriseEntitlement(prefs));
        prefs.edit().clear().commit();
    }
}
