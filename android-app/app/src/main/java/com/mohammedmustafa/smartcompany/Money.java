package com.mohammedmustafa.smartcompany;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decimal money boundary helpers.
 *
 * Legacy overloads preserve the application's historical two-decimal behavior.
 * Multi-currency callers must pass the fraction digits from CurrencySpec explicitly.
 * These helpers do not themselves migrate legacy SQLite REAL columns.
 */
public final class Money {
    /** Compatibility default only; never use as a universal currency rule. */
    public static final int SCALE = 2;

    private Money() {}

    private static int checkedScale(int scale) {
        if (scale < 0 || scale > 6) {
            throw new IllegalArgumentException("Currency scale must be between 0 and 6");
        }
        return scale;
    }

    public static long toMinorUnits(String majorUnits) {
        return toMinorUnits(majorUnits, SCALE);
    }

    public static long toMinorUnits(String majorUnits, int scale) {
        checkedScale(scale);
        if (majorUnits == null || majorUnits.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            return new BigDecimal(majorUnits.trim())
                    .setScale(scale, RoundingMode.HALF_UP)
                    .movePointRight(scale)
                    .longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid or out-of-range amount", ex);
        }
    }

    public static long toMinorUnits(double majorUnits) {
        return toMinorUnits(majorUnits, SCALE);
    }

    public static long toMinorUnits(double majorUnits, int scale) {
        if (!Double.isFinite(majorUnits)) {
            throw new IllegalArgumentException("Amount must be finite");
        }
        return toMinorUnits(BigDecimal.valueOf(majorUnits).toPlainString(), scale);
    }

    public static BigDecimal fromMinorUnits(long minorUnits) {
        return fromMinorUnits(minorUnits, SCALE);
    }

    public static BigDecimal fromMinorUnits(long minorUnits, int scale) {
        checkedScale(scale);
        return BigDecimal.valueOf(minorUnits, scale);
    }

    public static long multiplyQuantityAndPrice(String quantity, String unitPrice) {
        return multiplyQuantityAndPrice(quantity, unitPrice, SCALE);
    }

    public static long multiplyQuantityAndPrice(String quantity, String unitPrice, int scale) {
        checkedScale(scale);
        if (quantity == null || quantity.trim().isEmpty()
                || unitPrice == null || unitPrice.trim().isEmpty()) {
            throw new IllegalArgumentException("Quantity and unit price are required");
        }
        try {
            BigDecimal q = new BigDecimal(quantity.trim());
            BigDecimal p = new BigDecimal(unitPrice.trim());
            if (q.signum() < 0 || p.signum() < 0) {
                throw new IllegalArgumentException("Quantity and price cannot be negative");
            }
            return q.multiply(p).setScale(scale, RoundingMode.HALF_UP)
                    .movePointRight(scale).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid or out-of-range invoice amount", ex);
        }
    }

    public static long netInvoiceMinorUnits(String quantity, String unitPrice, String discount) {
        return netInvoiceMinorUnits(quantity, unitPrice, discount, SCALE);
    }

    public static long netInvoiceMinorUnits(
            String quantity, String unitPrice, String discount, int scale) {
        long gross = multiplyQuantityAndPrice(quantity, unitPrice, scale);
        long discountMinor = toMinorUnits(discount, scale);
        if (discountMinor < 0 || discountMinor > gross) {
            throw new IllegalArgumentException("Discount must be between zero and gross amount");
        }
        return Math.subtractExact(gross, discountMinor);
    }
}
