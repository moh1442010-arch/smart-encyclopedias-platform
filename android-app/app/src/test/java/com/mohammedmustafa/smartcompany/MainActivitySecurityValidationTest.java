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
    public void perpetualLicenseAllowsOnlyTheConfiguredOfflineGracePeriod() {
        long now = System.currentTimeMillis();
        MainActivity subject = new MainActivity();
        assertTrue(subject.offlineLicenseAllowed("licensed", BuildConfig.EDITION,
                "perpetual", 0L, now - 60_000L, now));
        assertFalse(subject.offlineLicenseAllowed("licensed", BuildConfig.EDITION,
                "perpetual", 0L, now - MainActivity.OFFLINE_GRACE_MS - 1L, now));
        assertFalse(subject.offlineLicenseAllowed("licensed", "another-edition",
                "perpetual", 0L, now - 60_000L, now));
        assertFalse(subject.offlineLicenseAllowed("licensed", BuildConfig.EDITION,
                "monthly", 0L, now - 60_000L, now));
    }

    @Test
    public void offlineTrialAndTimeLimitedLicensesMustStillBeUnexpired() {
        long now = System.currentTimeMillis();
        MainActivity subject = new MainActivity();
        assertTrue(subject.offlineLicenseAllowed("trial", "", "", now + 60_000L,
                now - 60_000L, now));
        assertFalse(subject.offlineLicenseAllowed("trial", "", "", now - 1L,
                now - 60_000L, now));
        assertTrue(subject.offlineLicenseAllowed("licensed", BuildConfig.EDITION,
                "monthly", now + 60_000L, now - 60_000L, now));
        assertFalse(subject.offlineLicenseAllowed("licensed", BuildConfig.EDITION,
                "monthly", now - 1L, now - 60_000L, now));
        assertFalse(subject.offlineLicenseAllowed("licensed", BuildConfig.EDITION,
                "monthly", now + 60_000L, 0L, now));
    }

    @Test
    public void editionNavigationGatesMatchTheInstalledFlavor() {
        assertTrue(activity.editionAllows("الفواتير والمبيعات"));
        assertTrue(activity.editionAllows("الأصناف والمخزون"));
        if ("basic".equals(BuildConfig.EDITION)) {
            assertFalse(activity.editionAllows("الخزينة والشيكات"));
            assertFalse(activity.editionAllows("الحسابات البنكية"));
            assertFalse(activity.editionAllows("المحاسبة والتقارير"));
            assertFalse(activity.editionAllows("الموظفون والرواتب"));
        } else if ("pro".equals(BuildConfig.EDITION)) {
            assertTrue(activity.editionAllows("المحاسبة والتقارير"));
            assertFalse(activity.editionAllows("الموظفون والرواتب"));
        } else {
            assertTrue(activity.editionAllows("المحاسبة والتقارير"));
            assertTrue(activity.editionAllows("الموظفون والرواتب"));
        }
    }

    @Test
    public void partialReturnQuantitiesMustStayWithinOriginalInvoiceLimit() {
        assertTrue(activity.canReturnQuantity(5.0, 0.0, 2.0));
        assertTrue(activity.canReturnQuantity(5.0, 2.0, 3.0));
        assertFalse(activity.canReturnQuantity(5.0, 4.0, 2.0));
        assertFalse(activity.canReturnQuantity(5.0, 0.0, 0.0));
        assertFalse(activity.canReturnQuantity(5.0, 0.0, Double.NaN));
    }
}
