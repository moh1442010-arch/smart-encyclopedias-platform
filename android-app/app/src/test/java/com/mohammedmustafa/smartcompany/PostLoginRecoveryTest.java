package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PostLoginRecoveryTest {
    public static class TestActivity extends MainActivity {
        @Override void checkCommercialAccess() {
            // Keep this UI regression test deterministic and independent of network/licensing.
        }
    }

    @Test
    public void dashboardRendersAfterSuccessfulLoginWithFreshDatabase() {
        TestActivity activity = Robolectric.buildActivity(TestActivity.class).setup().get();
        activity.authenticated = true;
        activity.dashboard();

        assertNotNull("Dashboard content container must be created", activity.content);
        assertTrue("Dashboard should render controls after authentication",
                activity.content.getChildCount() > 0);
    }

    @Test
    public void dashboardSummaryFailureDoesNotPreventDashboardRendering() {
        TestActivity activity = Robolectric.buildActivity(TestActivity.class).setup().get();
        activity.db.close();

        activity.authenticated = true;
        activity.dashboard();

        assertNotNull("Dashboard recovery must keep a content container", activity.content);
        assertTrue("Dashboard should still render when the summary query fails",
                activity.content.getChildCount() > 0);
    }
}
