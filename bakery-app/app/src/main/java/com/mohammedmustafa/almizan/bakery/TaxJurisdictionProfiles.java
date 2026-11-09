package com.mohammedmustafa.almizan.bakery;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Versioned country tax baselines. These are operational starting profiles, not legal advice.
 * Unknown jurisdictions remain CUSTOM_REVIEW_REQUIRED; absence of a VAT rule never means no tax.
 */
final class TaxJurisdictionProfiles {
    private TaxJurisdictionProfiles() {}

    static void ensure(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tax_jurisdiction_profiles ("+
            "country_code TEXT PRIMARY KEY NOT NULL REFERENCES countries(code),"+
            "indirect_tax_type TEXT NOT NULL, profile_status TEXT NOT NULL CHECK(profile_status IN ('VERIFIED_BASELINE','NO_VAT_CONFIRMED','LOCAL_RULES_REQUIRED','REVIEW_REQUIRED')),"+
            "registration_threshold TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '', source_url TEXT NOT NULL DEFAULT '',"+
            "verified_on TEXT NOT NULL, profile_version INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS currency_country_map (currency_code TEXT NOT NULL REFERENCES currencies(code), country_code TEXT NOT NULL REFERENCES countries(code), is_primary INTEGER NOT NULL DEFAULT 1 CHECK(is_primary IN (0,1)), PRIMARY KEY(currency_code,country_code))");
        seed(db,"SD","السودان","Sudan","SD_VAT_17","VAT","VERIFIED_BASELINE","تسجيل المكلفين وفق قانون الضريبة؛ يلزم التحقق من حد التسجيل والنشاط والإعفاءات مع ديوان الضرائب.","17% VAT; registration threshold and exemptions require authority confirmation.","https://tax.gov.sd/en/value-added-tax-vat/","2026-11-08",17,100,"2026-01-01");
        seed(db,"AE","الإمارات العربية المتحدة","United Arab Emirates","AE_VAT_5","VAT","VERIFIED_BASELINE","حد التسجيل الإلزامي 375,000 درهم؛ الاختياري 187,500 درهم؛ توجد توريدات صفرية ومعفاة.","Mandatory AED 375,000; voluntary AED 187,500. Verify supply classification.","https://www.tax.gov.ae/en/services/vat.registration.aspx","2026-11-08",5,100,"2018-01-01");
        seed(db,"SA","المملكة العربية السعودية","Saudi Arabia","SA_VAT_15","VAT","VERIFIED_BASELINE","النسبة الأساسية 15%؛ تطبق نسب الصفر والإعفاءات وفق نوع التوريد؛ التسجيل والفوترة الإلكترونية لهما قواعد مستقلة.","Standard 15%; supply classifications and e-invoicing rules apply.","https://www.zatca.gov.sa/ar/RulesRegulations/Taxes/Pages/VATLaw.aspx","2026-11-08",15,100,"2020-07-01");
        seed(db,"EG","مصر","Egypt","EG_VAT_14","VAT","VERIFIED_BASELINE","حد التسجيل الإلزامي المعلن 500,000 جنيه مصري؛ توجد قائمة إعفاءات وسلع جدول.","Mandatory registration threshold EGP 500,000; exemptions and schedule goods apply.","https://www.eta.gov.eg/ar/node/1379","2026-11-08",14,100,"2016-09-08");
        seed(db,"BH","البحرين","Bahrain","BH_VAT_10","VAT","VERIFIED_BASELINE","النسبة الأساسية 10%؛ حد التسجيل الإلزامي 37,500 دينار بحريني؛ توجد توريدات صفرية ومعفاة.","Standard 10%; mandatory threshold BHD 37,500.","https://www.bahrain.bh/wps/portal/en/BNP/HomeNationalPortal/ContentDetailsPage/","2026-11-08",10,100,"2022-01-01");
        seed(db,"OM","سلطنة عمان","Oman","OM_VAT_5","VAT","VERIFIED_BASELINE","النسبة الأساسية 5%؛ حد التسجيل الإلزامي 38,500 ريال عماني والاختياري 19,250.","Standard 5%; mandatory OMR 38,500; voluntary OMR 19,250.","https://tms.taxoman.gov.om/portal/registration","2026-11-08",5,100,"2021-04-16");
        seed(db,"KE","كينيا","Kenya","KE_VAT_16","VAT","VERIFIED_BASELINE","النسبة العامة 16%؛ توجد توريدات صفرية ومعفاة؛ ضريبة المدخلات والمخرجات تخضع لشروط الخصم.","General 16%; zero-rated and exempt supplies differ.","https://www.kra.go.ke/individual/filing-paying/types-of-taxes/value-added-tax","2026-11-08",16,100,"2013-09-02");
        seed(db,"ZA","جنوب أفريقيا","South Africa","ZA_VAT_15","VAT","VERIFIED_BASELINE","النسبة الأساسية 15%؛ حد التسجيل الإلزامي R2.3 million وفق دليل SARS المنشور في 2026؛ توجد نسب صفرية وإعفاءات.","Standard 15%; compulsory threshold R2.3m per SARS 2026 guidance.","https://www.sars.gov.za/tax-rates/other-taxes/","2026-11-08",15,100,"2018-04-01");
        seed(db,"QA","قطر","Qatar","QA_NO_VAT","NO_VAT","NO_VAT_CONFIRMED","الهيئة العامة للضرائب تؤكد أن ضريبة القيمة المضافة غير مطبقة حالياً؛ تبقى ضرائب أخرى مثل الانتقائية وضريبة الدخل بحسب الحالة.","Qatar GTA states VAT is not applied; other taxes may still apply.","https://www.gta.gov.qa/en/investors-guide","2026-11-08",0,1,"2026-11-08");
        seed(db,"KW","الكويت","Kuwait","KW_NO_VAT","NO_VAT","NO_VAT_CONFIRMED","لا توجد ضريبة قيمة مضافة مطبقة حالياً؛ قد تنطبق ضريبة دخل الشركات والزكاة بحسب الكيان والنشاط.","No VAT currently; corporate income tax and zakat may apply by entity/activity.","https://www.gov.uk/government/publications/exporting-to-kuwait/exporting-to-kuwait","2026-11-08",0,1,"2026-11-08");
        seed(db,"US","الولايات المتحدة","United States","US_LOCAL_SALES_TAX","SALES_TAX_LOCAL","LOCAL_RULES_REQUIRED","لا توجد ضريبة مبيعات اتحادية عامة؛ تختلف ضريبة المبيعات والاستعمال حسب الولاية والمقاطعة والمدينة وموقع الترابط الضريبي.","No federal VAT/sales tax; state and local nexus/rates must be configured by address.","https://taxsummaries.pwc.com/united-states/corporate/other-taxes","2026-11-08",0,1,"2026-11-08");
        seedReview(db,"LY","ليبيا","Libya","LYD");
        seedReview(db,"LB","لبنان","Lebanon","LBP");
        seedReview(db,"SY","سوريا","Syria","SYP");
        seedReview(db,"PS","فلسطين","Palestine","ILS");
        seedReview(db,"JO","الأردن","Jordan","JOD");
        seedReview(db,"IQ","العراق","Iraq","IQD");
        seedReview(db,"YE","اليمن","Yemen","YER");
        seedReview(db,"MA","المغرب","Morocco","MAD");
        seedReview(db,"DZ","الجزائر","Algeria","DZD");
        seedReview(db,"TN","تونس","Tunisia","TND");
        seedReview(db,"MR","موريتانيا","Mauritania","MRU");
        seedReview(db,"SO","الصومال","Somalia","SOS");
        seedReview(db,"DJ","جيبوتي","Djibouti","DJF");
        seedReview(db,"KM","جزر القمر","Comoros","KMF");
        seedReview(db,"ET","إثيوبيا","Ethiopia","ETB");
        seedReview(db,"ER","إريتريا","Eritrea","ERN");
        seedReview(db,"SS","جنوب السودان","South Sudan","SSP");
        seedReview(db,"GB","المملكة المتحدة","United Kingdom","GBP");
        seedReview(db,"CH","سويسرا","Switzerland","CHF");
        seedReview(db,"CA","كندا","Canada","CAD");
        seedReview(db,"AU","أستراليا","Australia","AUD");
        seedReview(db,"NZ","نيوزيلندا","New Zealand","NZD");
        seedReview(db,"JP","اليابان","Japan","JPY");
        seedReview(db,"CN","الصين","China","CNY");
        seedReview(db,"IN","الهند","India","INR");
        seedReview(db,"PK","باكستان","Pakistan","PKR");
        seedReview(db,"TR","تركيا","Türkiye","TRY");
        seedReview(db,"NG","نيجيريا","Nigeria","NGN");
        seedReview(db,"GH","غانا","Ghana","GHS");
        seedReview(db,"UG","أوغندا","Uganda","UGX");
        seedReview(db,"TZ","تنزانيا","Tanzania","TZS");
        seedReview(db,"RW","رواندا","Rwanda","RWF");
        seedReview(db,"BR","البرازيل","Brazil","BRL");
        seedReview(db,"MX","المكسيك","Mexico","MXN");
        seedReview(db,"SG","سنغافورة","Singapore","SGD");
        seedReview(db,"HK","هونغ كونغ","Hong Kong","HKD");
        seedReview(db,"KR","كوريا الجنوبية","South Korea","KRW");
        seedReview(db,"TH","تايلاند","Thailand","THB");
        // Reference-only rates for Arab jurisdictions not yet approved for automatic posting.
        // These records remain inactive until local product exemptions, registration and invoicing rules are verified.
        seedReference(db,"DZ","VAT","VAT_STANDARD","النسبة العامة المرجعية لضريبة القيمة المضافة",19,100,"2026-01-01","https://www.imf.org/en/Publications/Books/Issues/2026/04/16/Taxing-in-the-MENA-Region");
        seedReference(db,"JO","GENERAL_SALES_TAX","GST_STANDARD","ضريبة المبيعات العامة المرجعية — بعض الأغذية والخبز معفاة",16,100,"2026-01-01","https://taxsummaries.pwc.com/jordan/corporate/other-taxes");
        seedReference(db,"LB","VAT","VAT_STANDARD","نسبة مرجعية 11%؛ توجد صفحة حكومية منشورة بنسبة 10% لذلك يلزم حسم آخر تعديل تشريعي قبل التفعيل",11,100,"2026-01-01","https://taxsummaries.pwc.com/lebanon/corporate/other-taxes");
        seedReference(db,"MA","VAT","VAT_STANDARD","النسبة العامة المرجعية 20% مع معدلات مخفضة وإعفاءات حسب السلعة والخدمة",20,100,"2026-01-01","https://taxsummaries.pwc.com/morocco/corporate/significant-developments");
        seedReference(db,"TN","VAT","VAT_STANDARD","النسبة العامة المرجعية 19% مع معدلات مخفضة",19,100,"2026-01-01","https://www.imf.org/en/Publications/Books/Issues/2026/04/16/Taxing-in-the-MENA-Region");
        seedReference(db,"MR","VAT","VAT_STANDARD","النسبة العامة المرجعية 16%",16,100,"2026-01-01","https://taxsummaries.pwc.com/quick-charts/value-added-tax-vat-rates");
        seedReference(db,"DJ","VAT","VAT_STANDARD","النسبة العامة المرجعية 10%",10,100,"2026-01-01","https://www.imf.org/en/Publications/Books/Issues/2026/04/16/Taxing-in-the-MENA-Region");
        seedReference(db,"KM","CONSUMPTION_TAX","CT_STANDARD","ضريبة استهلاك مرجعية 10% وليست VAT؛ يلزم التحقق من نطاق السلع",10,100,"2026-01-01","https://www.ehsebly.app/tools/vat-calculator?lang=en");
        markLocalTaxOnly(db,"IQ","SELECTIVE_SALES_TAX","العراق: ضرائب مبيعات انتقائية على قطاعات وسلع محددة؛ لا توجد نسبة VAT عامة موحدة.");
        markLocalTaxOnly(db,"LY","SELECTIVE_SALES_TAX","ليبيا: لا تعتمد نسبة VAT عامة في هذا الملف؛ توجد رسوم وضرائب قطاعية ويجب التحقق من التشريعات السارية.");
        markLocalTaxOnly(db,"YE","SELECTIVE_SALES_TAX","اليمن: ضرائب مبيعات انتقائية ورسوم قطاعية؛ لا تعامل كضريبة VAT عامة.");
        markLocalTaxOnly(db,"SY","CONSUMPTION_TAX","سوريا: توجد رسوم وضرائب استهلاك؛ يلزم توثيق النظام الساري ومعدلاته حسب السلعة.");
        markLocalTaxOnly(db,"SO","LOCAL_SALES_TAX","الصومال: تختلف الضرائب والرسوم بحسب المستوى الاتحادي والولاية؛ لا توجد نسبة VAT موحدة معتمدة هنا.");
        markLocalTaxOnly(db,"PS","VAT","فلسطين: توجد ضريبة قيمة مضافة؛ يجب تأكيد النسبة والمعاملة حسب المنطقة والتوريد قبل التفعيل.");
        db.execSQL("UPDATE tax_jurisdiction_profiles SET registration_threshold='AED 375000 mandatory; AED 187500 voluntary' WHERE country_code='AE'");
        db.execSQL("UPDATE tax_jurisdiction_profiles SET registration_threshold='SAR 375000 mandatory; SAR 187500 voluntary' WHERE country_code='SA'");
        db.execSQL("UPDATE tax_jurisdiction_profiles SET registration_threshold='EGP 500000 mandatory' WHERE country_code='EG'");
        db.execSQL("UPDATE tax_jurisdiction_profiles SET registration_threshold='BHD 37500 mandatory' WHERE country_code='BH'");
        db.execSQL("UPDATE tax_jurisdiction_profiles SET registration_threshold='OMR 38500 mandatory; OMR 19250 voluntary' WHERE country_code='OM'");
        db.execSQL("UPDATE tax_jurisdiction_profiles SET registration_threshold='ZAR 2300000 mandatory; ZAR 120000 voluntary' WHERE country_code='ZA'");
        // Currency-to-country relationships are stored in the encrypted database, not hard-coded in the login UI.
        mapCurrency(db,"DZD","DZ",true); mapCurrency(db,"BHD","BH",true);
        mapCurrency(db,"KMF","KM",true); mapCurrency(db,"DJF","DJ",true);
        mapCurrency(db,"EGP","EG",true); mapCurrency(db,"IQD","IQ",true);
        mapCurrency(db,"JOD","JO",true); mapCurrency(db,"KWD","KW",true);
        mapCurrency(db,"LBP","LB",true); mapCurrency(db,"USD","LB",false);
        mapCurrency(db,"LYD","LY",true); mapCurrency(db,"MRU","MR",true);
        mapCurrency(db,"MAD","MA",true); mapCurrency(db,"OMR","OM",true);
        mapCurrency(db,"ILS","PS",true); mapCurrency(db,"JOD","PS",false); mapCurrency(db,"USD","PS",false);
        mapCurrency(db,"QAR","QA",true); mapCurrency(db,"SAR","SA",true);
        mapCurrency(db,"SOS","SO",true); mapCurrency(db,"SDG","SD",true);
        mapCurrency(db,"SYP","SY",true); mapCurrency(db,"TND","TN",true);
        mapCurrency(db,"AED","AE",true); mapCurrency(db,"YER","YE",true);
        mapCurrency(db,"USD","US",true);
        db.execSQL("UPDATE company_settings SET tax_profile='SD_VAT_17' WHERE country_code='SD' AND tax_profile='SD_DEFAULT'");
        db.execSQL("INSERT OR IGNORE INTO tax_rules(country_code,tax_code,name,rate_num,rate_den,inclusive,exempt_category,effective_from,active) VALUES"+
            "('SD','VAT_STANDARD','ضريبة القيمة المضافة العامة',17,100,0,'حسب القانون واللوائح','2026-01-01',1),"+
            "('AE','VAT_STANDARD','Standard VAT',5,100,0,'Zero-rated and exempt supplies','2018-01-01',1),"+
            "('SA','VAT_STANDARD','ضريبة القيمة المضافة الأساسية',15,100,0,'التوريدات الصفرية والمعفاة حسب النظام','2020-07-01',1),"+
            "('EG','VAT_STANDARD','ضريبة القيمة المضافة العامة',14,100,0,'الإعفاءات وسلع الجدول حسب القانون','2016-09-08',1),"+
            "('BH','VAT_STANDARD','Standard VAT',10,100,0,'Zero-rated and exempt supplies','2022-01-01',1),"+
            "('OM','VAT_STANDARD','Standard VAT',5,100,0,'Zero-rated and exempt supplies','2021-04-16',1),"+
            "('KE','VAT_STANDARD','General VAT',16,100,0,'Zero-rated and exempt supplies','2013-09-02',1),"+
            "('ZA','VAT_STANDARD','Standard VAT',15,100,0,'Zero-rated and exempt supplies','2018-04-01',1)");
    }

    private static void seedReference(SQLiteDatabase db,String country,String type,String taxCode,String name,int rateNum,int rateDen,String effective,String source) {
        db.execSQL("UPDATE tax_jurisdiction_profiles SET indirect_tax_type=?,profile_status='REVIEW_REQUIRED',notes=?,source_url=?,verified_on='2026-11-09',profile_version=2 WHERE country_code=?",
            new Object[]{type,name,source,country});
        db.execSQL("INSERT OR IGNORE INTO tax_rules(country_code,tax_code,name,rate_num,rate_den,inclusive,exempt_category,effective_from,active) VALUES(?,?,?,?,?,0,?,?,0)",
            new Object[]{country,taxCode,name,rateNum,rateDen,"راجع الاستثناءات والتصنيفات المحلية قبل التفعيل",effective});
    }

    private static void markLocalTaxOnly(SQLiteDatabase db,String country,String type,String notes) {
        db.execSQL("UPDATE tax_jurisdiction_profiles SET indirect_tax_type=?,profile_status='REVIEW_REQUIRED',notes=?,verified_on='2026-11-09',profile_version=2 WHERE country_code=?",
            new Object[]{type,notes,country});
    }

    private static void seedReview(SQLiteDatabase db,String code,String ar,String en,String currency) {
        db.execSQL("INSERT OR IGNORE INTO countries(code,name_ar,name_en,tax_profile,active) VALUES(?,?,?,'CUSTOM_REVIEW_REQUIRED',1)",new Object[]{code,ar,en});
        db.execSQL("INSERT OR IGNORE INTO tax_jurisdiction_profiles(country_code,indirect_tax_type,profile_status,registration_threshold,notes,source_url,verified_on,profile_version) VALUES(?,'UNVERIFIED','REVIEW_REQUIRED','','No tax rate has been preloaded. Confirm current national and subnational rules before enabling tax calculation.','','2026-11-08',1)",new Object[]{code});
    }

    private static void seed(SQLiteDatabase db,String code,String ar,String en,String profile,String type,String status,String notesAr,String notesEn,String source,String checked,int num,int den,String effective) {
        db.execSQL("INSERT OR IGNORE INTO countries(code,name_ar,name_en,tax_profile,active) VALUES(?,?,?,?,1)",new Object[]{code,ar,en,profile});
        db.execSQL("UPDATE countries SET tax_profile=?,active=1 WHERE code=?",new Object[]{profile,code});
        db.execSQL("INSERT OR REPLACE INTO tax_jurisdiction_profiles(country_code,indirect_tax_type,profile_status,registration_threshold,notes,source_url,verified_on,profile_version) VALUES(?,?,?,?,?,?,?,1)",
            new Object[]{code,type,status,"",notesAr+" "+notesEn,source,checked});
    }

    private static void mapCurrency(SQLiteDatabase db,String currency,String country,boolean primary) {
        db.execSQL("INSERT OR IGNORE INTO currency_country_map(currency_code,country_code,is_primary) VALUES(?,?,?)",
            new Object[]{currency,country,primary?1:0});
    }

    // Pure catalog helper retained for deterministic local unit tests; the app UI reads the persisted table.
    @Deprecated
    static List<String> countriesForCurrency(String currency) {
        switch(currency) {
            case "SDG": return Collections.singletonList("SD");
            case "SAR": return Collections.singletonList("SA");
            case "AED": return Collections.singletonList("AE");
            case "EGP": return Collections.singletonList("EG");
            case "QAR": return Collections.singletonList("QA");
            case "KWD": return Collections.singletonList("KW");
            case "USD": return Arrays.asList("US","LB","PS");
            default: return Collections.emptyList();
        }
    }

    static List<String> countriesForCurrency(SQLiteDatabase db,String currency) {
        java.util.ArrayList<String> countries=new java.util.ArrayList<>();
        android.database.Cursor c=db.rawQuery(
            "SELECT country_code FROM currency_country_map WHERE currency_code=? ORDER BY is_primary DESC,country_code",
            new String[]{currency});
        try { while(c.moveToNext()) countries.add(c.getString(0)); }
        finally { c.close(); }
        return countries;
    }
}
