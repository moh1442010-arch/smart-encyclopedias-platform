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
    public void chequeDatesRequireRealIsoCalendarDates() {
        assertTrue(activity.validIsoDate("2024-02-29", true));
        assertFalse(activity.validIsoDate("2025-02-29", true));
        assertFalse(activity.validIsoDate("2024-13-01", true));
        assertFalse(activity.validIsoDate("2024-02-30", true));
        assertFalse(activity.validIsoDate("", true));
        assertTrue(activity.validIsoDate("", false));
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
}
