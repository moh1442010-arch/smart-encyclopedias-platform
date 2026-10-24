package com.mohammedmustafa.almizan.bakery;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Validation helpers for untrusted backup data before it reaches SQLite. */
public final class BakeryDataValidation {
    private BakeryDataValidation() {}

    public static boolean isValidIsoDate(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(value, position);
        return parsed != null && position.getIndex() == value.length() && value.equals(format.format(parsed));
    }

    public static long requireNonNegative(long value, String field) {
        if (value < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value;
    }
}
