package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Persisted Arabic/English locale and a transitional dictionary for programmatic screens. */
final class AppLanguage {
    private static final String PREFS="almizan_app_settings";
    private static final String KEY_LANGUAGE="language";
    private static final Map<String,String> ENGLISH=new HashMap<>();
    static {
        put("🌐 اللغة / Language","🌐 Language / اللغة");
        put("اختيار اللغة","Choose language");
        put("العربية","Arabic");
        put("الرئيسية","Home");
        put("العودة إلى الرئيسية","Back to Home");
        put("العودة إلى مصنع الخبائز والحلويات والباسطة","Back to Factory");
        put("المخبز","Bakery");
        put("المصنع","Factory");
        put("المخزون","Inventory");
        put("المواد الخام","Raw materials");
        put("المنتجات","Products");
        put("الإنتاج","Production");
        put("المبيعات","Sales");
        put("العملاء","Customers");
        put("الموردون","Suppliers");
        put("المصروفات","Expenses");
        put("التالف","Waste");
        put("التقارير","Reports");
        put("الدفاتر","Ledgers");
        put("التحصيل","Collections");
        put("حفظ","Save");
        put("إلغاء","Cancel");
        put("بحث","Search");
        put("إضافة","Add");
        put("تعديل","Edit");
        put("حذف","Delete");
        put("تصدير","Export");
        put("استيراد","Import");
        put("النسخ الاحتياطي","Backup");
        put("استعادة","Restore");
        put("الإعدادات","Settings");
        put("التاريخ","Date");
        put("الكمية","Quantity");
        put("السعر","Price");
        put("القيمة","Value");
        put("الرصيد","Balance");
        put("صافي الربح","Net profit");
        put("التكلفة","Cost");
        put("الإيرادات","Revenue");
        put("الأصول","Assets");
        put("الخصوم","Liabilities");
        put("حقوق الملكية","Equity");
        put("ميزان المراجعة","Trial balance");
        put("قائمة الدخل","Income statement");
        put("الميزانية العمومية","Balance sheet");
        put("التدفق النقدي","Cash flow");
        put("تم الحفظ","Saved");
        put("تعذر الحفظ","Could not save");
        put("الرجوع","Back");
        put("تسجيل الخروج","Sign out");
        put("تفعيل","Activate");
        put("الاشتراك","Subscription");
        put("نسخ رقم IBAN","Copy IBAN");
        put("نسخ رقم حساب بنكك","Copy Bankak account");
        put("الدفع المحلي عبر بنكك — رقم الحساب: 1882224","Local payment via Bankak — account: 1882224");
        put("التحويل المصرفي الدولي — IBAN: SD8504018822240001","International bank transfer — IBAN: SD8504018822240001");
        put("الاشتراك الشهري — 55,000 جنيه سوداني","Monthly subscription — 55,000 SDG");
        put("الاشتراك السنوي — 550,000 جنيه سوداني","Annual subscription — 550,000 SDG");
        put("الترخيص الدائم — 1,050,000 جنيه سوداني","Permanent license — 1,050,000 SDG");
        put("الرجاء الانتظار","Please wait");
        put("لا توجد بيانات","No data");
        put("تأكيد","Confirm");
        put("إغلاق","Close");
    }
    private AppLanguage() {}
    private static void put(String ar,String en){ENGLISH.put(ar,en);}
    static String getLanguage(Context context){
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY_LANGUAGE,"ar");
    }
    static boolean isEnglish(Context context){return "en".equals(getLanguage(context));}
    static void setLanguage(Context context,String language){
        String normalized="en".equalsIgnoreCase(language)?"en":"ar";
        if(!context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY_LANGUAGE,normalized).commit())
            throw new IllegalStateException("تعذر حفظ إعداد اللغة");
    }
    static Context wrap(Context base){
        Locale locale=new Locale(getLanguage(base));
        Locale.setDefault(locale);
        Configuration config=new Configuration(base.getResources().getConfiguration());
        if(Build.VERSION.SDK_INT>=17){config.setLocale(locale);config.setLayoutDirection(locale);}
        else {config.locale=locale;}
        return base.createConfigurationContext(config);
    }
    static String tr(Context context,String value){
        if(value==null||!isEnglish(context))return value;
        String exact=ENGLISH.get(value);
        if(exact!=null)return exact;
        // Translate common static tokens embedded in longer labels without changing numbers.
        String translated=value;
        for(Map.Entry<String,String> entry:ENGLISH.entrySet())
            if(entry.getKey().length()>2)translated=translated.replace(entry.getKey(),entry.getValue());
        return translated;
    }
    static String languageLabel(Context context){return isEnglish(context)?"Language: English":"اللغة: العربية";}
}
