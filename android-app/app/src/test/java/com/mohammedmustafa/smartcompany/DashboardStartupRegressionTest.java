package com.mohammedmustafa.smartcompany;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
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

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        try {
            // Exercise the exact destination called by the successful PIN handler.
            activity.authenticated = true;
            activity.dashboard();

            View root = activity.findViewById(android.R.id.content);
            assertNotNull("Activity content root must remain attached", root);
            assertTrue(
                "Dashboard must render its heading; falling back to login means startup still failed",
                containsText(root, "مركز إدارة النشاط")
            );
            assertNotNull("Database helper must remain available after login", activity.db);
            assertFalse(
                "Successful dashboard render must not leave a recorded startup failure",
                app.getSharedPreferences("startup_diagnostics", Context.MODE_PRIVATE)
                    .contains("last_failure_class")
            );
        } finally {
            Robolectric.buildActivity(MainActivity.class).get().finish();
            app.deleteDatabase("smartcompany2.db");
        }
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
