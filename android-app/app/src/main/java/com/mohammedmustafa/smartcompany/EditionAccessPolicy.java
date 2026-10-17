package com.mohammedmustafa.smartcompany;

/** Central feature matrix shared by all Android build variants. */
public final class EditionAccessPolicy {
    private EditionAccessPolicy() {}

    public static boolean canAccess(int level, String section) {
        if (section == null) return false;
        switch (section) {
            case "المرتجعات":
            case "المحاسبة والتقارير":
            case "الحسابات الرئيسية":
            case "القيود اليدوية":
            case "التقارير والتحليل":
            case "الحسابات البنكية":
            case "سجل الشيكات":
                return level >= 2;
            case "الموظفون والرواتب":
            case "الموظفون":
                return level >= 3;
            default:
                return level >= 1;
        }
    }
}
