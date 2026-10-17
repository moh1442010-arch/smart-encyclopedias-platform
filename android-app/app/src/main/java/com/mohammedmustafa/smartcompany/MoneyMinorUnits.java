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


    /** Calculate a currency line total from a decimal quantity and two-decimal price/discount. */
    public static long lineTotalMinor(String quantity, String unitPrice, String discount, int fractionDigits) {
        if (quantity == null || unitPrice == null || discount == null) {
            throw new IllegalArgumentException("Quantity, price and discount are required");
        }
        if (fractionDigits < 0 || fractionDigits > 6) {
            throw new IllegalArgumentException("Unsupported currency scale");
        }
        try {
            BigDecimal q = new BigDecimal(quantity.trim());
            BigDecimal price = new BigDecimal(unitPrice.trim()).setScale(fractionDigits, RoundingMode.UNNECESSARY);
            BigDecimal disc = new BigDecimal(discount.trim()).setScale(fractionDigits, RoundingMode.UNNECESSARY);
            if (q.signum() <= 0 || price.signum() < 0 || disc.signum() < 0) {
                throw new IllegalArgumentException("Quantity must be positive and monetary values non-negative");
            }
            BigDecimal gross = q.multiply(price);
            if (disc.compareTo(gross) > 0) {
                throw new IllegalArgumentException("Discount exceeds line gross");
            }
            return gross.subtract(disc).setScale(fractionDigits, RoundingMode.HALF_UP)
                    .movePointRight(fractionDigits).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid line values or amount outside supported range", ex);
        }
    }

    /** Round quantity × unit cost once at the currency boundary, without binary floating-point math. */
    public static long extendedMinor(double quantity, double unitCost, int fractionDigits) {
        if (!Double.isFinite(quantity) || !Double.isFinite(unitCost) || quantity < 0 || unitCost < 0) {
            throw new IllegalArgumentException("Quantity and unit cost must be finite and non-negative");
        }
        if (fractionDigits < 0 || fractionDigits > 6) {
            throw new IllegalArgumentException("Unsupported currency scale");
        }
        try {
            return BigDecimal.valueOf(quantity).multiply(BigDecimal.valueOf(unitCost))
                    .setScale(fractionDigits, RoundingMode.HALF_UP)
                    .movePointRight(fractionDigits).longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Extended amount is outside supported range", ex);
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