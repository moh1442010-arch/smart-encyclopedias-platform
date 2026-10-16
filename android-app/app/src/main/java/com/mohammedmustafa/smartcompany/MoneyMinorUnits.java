package com.mohammedmustafa.smartcompany;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact currency boundary helpers. Not a database migration by themselves. */
public final class MoneyMinorUnits {
    private MoneyMinorUnits() { }

    /** Parse a user-entered decimal amount into integer minor units without silent rounding. */
    public static long parse(String value, int fractionDigits) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        if (fractionDigits < 0 || fractionDigits > 6) {
            throw new IllegalArgumentException("Unsupported currency scale");
        }
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            return amount.setScale(fractionDigits, RoundingMode.UNNECESSARY)
                    .movePointRight(fractionDigits).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid amount or unsupported fractional precision", ex);
        }
    }

    /** Convert a legacy REAL value using an explicit, deterministic migration rounding rule. */
    public static long fromLegacyDouble(double value, int fractionDigits) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Legacy amount must be finite");
        }
        if (fractionDigits < 0 || fractionDigits > 6) {
            throw new IllegalArgumentException("Unsupported currency scale");
        }
        try {
            return BigDecimal.valueOf(value)
                    .setScale(fractionDigits, RoundingMode.HALF_UP)
                    .movePointRight(fractionDigits).longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Legacy amount is outside supported range", ex);
        }
    }

    /** Format integer minor units as a decimal string without binary floating-point arithmetic. */
    public static String format(long minorUnits, int fractionDigits) {
        if (fractionDigits < 0 || fractionDigits > 6) {
            throw new IllegalArgumentException("Unsupported currency scale");
        }
        return BigDecimal.valueOf(minorUnits, fractionDigits).toPlainString();
    }
}