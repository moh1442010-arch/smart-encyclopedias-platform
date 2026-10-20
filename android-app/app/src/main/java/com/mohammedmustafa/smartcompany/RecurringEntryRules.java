package com.mohammedmustafa.smartcompany;

import java.util.Calendar;
import java.util.Locale;

/** Pure recurrence rules shared by the Android persistence/UI layer. */
public final class RecurringEntryRules {
    private RecurringEntryRules() {}

    public static String periodKey(int year, int monthOneBased) {
        if (monthOneBased < 1 || monthOneBased > 12) throw new IllegalArgumentException("month must be 1..12");
        if (year < 1 || year > 9999) throw new IllegalArgumentException("year out of range");
        return String.format(Locale.US, "%04d-%02d", year, monthOneBased);
    }

    public static String occurrenceKey(long scheduleId, String period) {
        if (scheduleId <= 0) throw new IllegalArgumentException("schedule id must be positive");
        validatePeriod(period);
        return scheduleId + ":" + period;
    }

    public static String dueDate(String period, int dueDay) {
        validatePeriod(period);
        if (dueDay < 1 || dueDay > 31) throw new IllegalArgumentException("due day must be 1..31");
        int year = Integer.parseInt(period.substring(0, 4));
        int month = Integer.parseInt(period.substring(5, 7));
        Calendar calendar = Calendar.getInstance(Locale.US);
        calendar.clear();
        calendar.set(year, month - 1, 1);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        return String.format(Locale.US, "%s-%02d", period, Math.min(dueDay, lastDay));
    }

    public static boolean mayAutoPost(boolean fixedAmount, boolean ownerEnabledAutoPost, boolean occurrenceConfirmed) {
        return fixedAmount && ownerEnabledAutoPost && occurrenceConfirmed;
    }

    public static boolean mayPostVariableAmount(Double confirmedAmount) {
        return confirmedAmount != null && Double.isFinite(confirmedAmount) && confirmedAmount > 0;
    }

    private static void validatePeriod(String period) {
        if (period == null || !period.matches("\\d{4}-(0[1-9]|1[0-2])"))
            throw new IllegalArgumentException("period must use YYYY-MM format");
    }
}