package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.content.res.Configuration;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppLanguageTest {
    @Test public void languageChoicePersistsAndChangesTextAndLayoutDirection() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.getSharedPreferences("almizan_app_settings",Context.MODE_PRIVATE).edit().clear().commit();
        assertEquals("ar",AppLanguage.getLanguage(context));
        assertEquals("حفظ",AppLanguage.tr(context,"حفظ"));

        AppLanguage.setLanguage(context,"en");
        assertTrue(AppLanguage.isEnglish(context));
        assertEquals("Save",AppLanguage.tr(context,"حفظ"));
        Context english=AppLanguage.wrap(context);
        Configuration configuration=english.getResources().getConfiguration();
        assertEquals("en",configuration.locale.getLanguage());
        assertEquals(android.view.View.LAYOUT_DIRECTION_LTR,configuration.getLayoutDirection());

        AppLanguage.setLanguage(context,"ar");
        assertFalse(AppLanguage.isEnglish(context));
        assertEquals("حفظ",AppLanguage.tr(context,"حفظ"));
        Context arabic=AppLanguage.wrap(context);
        assertEquals("ar",arabic.getResources().getConfiguration().locale.getLanguage());
        assertEquals(android.view.View.LAYOUT_DIRECTION_RTL,arabic.getResources().getConfiguration().getLayoutDirection());
        context.getSharedPreferences("almizan_app_settings",Context.MODE_PRIVATE).edit().clear().commit();
    }
}
