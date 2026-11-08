package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LicenseAccessGateTest {
    private Context context;
    private SharedPreferences canonical;
    private SharedPreferences legacy;
    private Map<String,?> canonicalBefore;
    private Map<String,?> legacyBefore;

    @Before public void setUp() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        canonical=context.getSharedPreferences("MainActivity_preferences",Context.MODE_PRIVATE);
        legacy=context.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
        canonicalBefore=canonical.getAll();
        legacyBefore=legacy.getAll();
        canonical.edit().clear().commit();
        legacy.edit().clear().commit();
    }

    @After public void restorePreferences() {
        restore(canonical,canonicalBefore);
        restore(legacy,legacyBefore);
    }

    private static void restore(SharedPreferences prefs,Map<String,?> values) {
        SharedPreferences.Editor e=prefs.edit().clear();
        for(Map.Entry<String,?> item:values.entrySet()) {
            Object v=item.getValue(); String k=item.getKey();
            if(v instanceof String)e.putString(k,(String)v);
            else if(v instanceof Long)e.putLong(k,(Long)v);
            else if(v instanceof Integer)e.putInt(k,(Integer)v);
            else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);
            else if(v instanceof Float)e.putFloat(k,(Float)v);
            else if(v instanceof java.util.Set)e.putStringSet(k,(java.util.Set<String>)v);
        }
        assertTrue(e.commit());
    }

    @Test public void expiredSevenDayTrialDeniesApplicationAccess() {
        long now=System.currentTimeMillis();
        canonical.edit().putLong("trial_start",now-8L*24L*60L*60L*1000L)
            .putLong("trial_last_seen",now).remove("license_token")
            .remove("trial_clock_rollback_detected").commit();
        assertFalse("Expired trial must not enter any protected activity",LicenseAccessGate.isAccessAllowed(context));
    }

    @Test public void activeTrialRemainsAvailableBeforeSevenDays() {
        long now=System.currentTimeMillis();
        canonical.edit().putLong("trial_start",now-6L*24L*60L*60L*1000L)
            .putLong("trial_last_seen",now).remove("license_token")
            .remove("trial_clock_rollback_detected").commit();
        assertTrue("Valid trial must remain available before the seven-day limit",LicenseAccessGate.isAccessAllowed(context));
    }

    @Test public void migratesExpiredLegacyGateStateWithoutRestartingTrial() {
        long started=System.currentTimeMillis()-9L*24L*60L*60L*1000L;
        long seen=System.currentTimeMillis();
        legacy.edit().putLong("trial_start",started).putLong("trial_last_seen",seen).commit();
        assertFalse("Migration must not grant a fresh trial",LicenseAccessGate.isAccessAllowed(context));
        assertEquals(started,canonical.getLong("trial_start",0L));
    }

    @Test public void choosesEarliestTrialStartAcrossHistoricalStores() {
        long now=System.currentTimeMillis();
        long earlier=now-9L*24L*60L*60L*1000L;
        long later=now-2L*24L*60L*60L*1000L;
        canonical.edit().putLong("trial_start",later).putLong("trial_last_seen",now).commit();
        legacy.edit().putLong("trial_start",earlier).putLong("trial_last_seen",now).commit();
        assertFalse("The older trial start must win to prevent an extension",LicenseAccessGate.isAccessAllowed(context));
        assertEquals(earlier,canonical.getLong("trial_start",0L));
    }
    @Test public void expiredOrMalformedLicenseTokenCannotStartFreshTrial() {
        canonical.edit().clear().putString("license_token","invalid.expired.token").commit();
        assertFalse("A previously issued invalid or expired token must not create a fresh trial",LicenseAccessGate.isAccessAllowed(context));
        assertEquals("Trial start must remain unset",0L,canonical.getLong("trial_start",0L));
    }


    @Test public void centralizedLicenseVerifierRejectsMissingAndMalformedTokens() {
        assertFalse(LicenseAccessGate.hasValidLicense(context));
        assertFalse(LicenseAccessGate.verifyToken(context,"not-a-signed-token"));
        canonical.edit().putString("license_token","not-a-signed-token").commit();
        assertFalse(LicenseAccessGate.hasValidLicense(context));
    }
}
