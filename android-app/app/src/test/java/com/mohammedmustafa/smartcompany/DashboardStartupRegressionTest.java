package com.mohammedmustafa.smartcompany;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 28)
public class DashboardStartupRegressionTest {

    @Test
    public void dashboardRendersAfterSuccessfulPinAuthenticationPath() {
        Context app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("smartcompany2.db");
        app.getSharedPreferences("startup_diagnostics", Context.MODE_PRIVATE).edit().clear().commit();
        app.getSharedPreferences("commercial_license", Context.MODE_PRIVATE).edit().clear().commit();

        MainActivity activity = Robolectric.buildActivity(TestMainActivity.class).setup().get();
        try {
            activity.authenticated = true;
            activity.dashboard();

            View root = activity.findViewById(android.R.id.content);
            assertNotNull("Activity content root must remain attached", root);
            assertTrue("Dashboard heading must render", containsText(root, "مركز إدارة النشاط"));
            assertTrue("Factory WIP must be reachable", containsText(root, "حساب التشغيل والتصنيع المرحلي"));
            assertTrue("Reversal entry must be reachable", containsText(root, "القيد العكسي"));
            assertNotNull("Database helper must remain available", activity.db);
            assertFalse("Successful dashboard must not leave a startup failure",
                app.getSharedPreferences("startup_diagnostics", Context.MODE_PRIVATE).contains("last_failure_class"));
        } finally {
            if (activity.db != null) activity.db.close();
            activity.finish();
            app.deleteDatabase("smartcompany2.db");
        }
    }

    @Test
    public void enteringCorrectPinThroughLoginButtonKeepsActivityOnDashboard() {
        Context app = RuntimeEnvironment.getApplication();
        app.deleteDatabase("smartcompany2.db");
        app.getSharedPreferences("startup_diagnostics", Context.MODE_PRIVATE).edit().clear().commit();
        app.getSharedPreferences("commercial_license", Context.MODE_PRIVATE).edit().clear().commit();
        MainActivity activity = Robolectric.buildActivity(TestMainActivity.class).setup().get();
        try {
            Context context = activity;
            assertTrue("Test PIN should be stored", activity.savePin("123456",
                context.getSharedPreferences("security", Context.MODE_PRIVATE)));

            activity.login();
            View rootBefore = activity.findViewById(android.R.id.content);
            EditText pin = firstEditText(rootBefore);
            Button signIn = buttonWithText(rootBefore, "دخول");
            assertNotNull("PIN input should be present", pin);
            assertNotNull("Sign-in button should be present", signIn);
            pin.setText("123456");
            signIn.performClick();

            View rootAfter = activity.findViewById(android.R.id.content);
            assertNotNull("Activity content must remain attached after sign-in", rootAfter);
            assertTrue("Correct PIN must navigate to dashboard", containsText(rootAfter, "مركز إدارة النشاط"));
            assertTrue("Dashboard must expose bakery daily book", containsText(rootAfter, "🥖 دفتر المخبز اليومي — القطعيات والتوزيع والتسوية"));
            assertFalse("Successful login must not record a dashboard failure",
                app.getSharedPreferences("startup_diagnostics", Context.MODE_PRIVATE).contains("last_failure_class"));
        } finally {
            if (activity.db != null) activity.db.close();
            activity.finish();
            app.deleteDatabase("smartcompany2.db");
            app.getSharedPreferences("security", Context.MODE_PRIVATE).edit().clear().commit();
        }
    }

    /** Avoid external licensing-network timing in UI regression tests. */
    public static class TestMainActivity extends MainActivity {
        @Override void checkCommercialAccess() { }
    }

    private EditText firstEditText(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText found = firstEditText(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button buttonWithText(View view, String expected) {
        if (view instanceof Button) {
            CharSequence text = ((Button) view).getText();
            if (text != null && expected.contentEquals(text)) return (Button) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = buttonWithText(group.getChildAt(i), expected);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean containsText(View view, String expected) {
        if (view instanceof TextView) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && expected.contentEquals(text)) return true;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (containsText(group.getChildAt(i), expected)) return true;
            }
        }
        return false;
    }
}
