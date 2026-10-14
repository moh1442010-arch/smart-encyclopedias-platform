package com.mohammedmustafa.smartcompany;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Exact decimal boundary helpers for currency values.
 * Store monetary values as integer minor units in new persistence code;
 * legacy REAL columns require a separately tested migration before switching reads/writes.
 */
public final class Money {
    public static final int SCALE = 2;
    private static final BigDecimal MINOR_FACTOR = BigDecimal.TEN.pow(SCALE);

    private Money() {}

    public static long toMinorUnits(String majorUnits) {
        if (majorUnits == null || majorUnits.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            return new BigDecimal(majorUnits.trim())
                    .setScale(SCALE, RoundingMode.HALF_UP)
                    .movePointRight(SCALE)
                    .longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid or out-of-range amount", ex);
        }
    }

    public static long toMinorUnits(double majorUnits) {
        if (!Double.isFinite(majorUnits)) {
            throw new IllegalArgumentException("Amount must be finite");
        }
        return toMinorUnits(BigDecimal.valueOf(majorUnits).toPlainString());
    }

    public static BigDecimal fromMinorUnits(long minorUnits) {
        return BigDecimal.valueOf(minorUnits, SCALE);
    }

    public static long multiplyQuantityAndPrice(String quantity, String unitPrice) {
        try {
            BigDecimal q = new BigDecimal(quantity.trim());
            BigDecimal p = new BigDecimal(unitPrice.trim());
            if (q.signum() < 0 || p.signum() < 0) {
                throw new IllegalArgumentException("Quantity and price cannot be negative");
            }
            return q.multiply(p).setScale(SCALE, RoundingMode.HALF_UP)
                    .movePointRight(SCALE).longValueExact();
        } catch (NumberFormatException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid or out-of-range invoice amount", ex);
        }
    }

    public static long netInvoiceMinorUnits(String quantity, String unitPrice, String discount) {
        long gross = multiplyQuantityAndPrice(quantity, unitPrice);
        long discountMinor = toMinorUnits(discount);
        if (discountMinor < 0 || discountMinor > gross) {
            throw new IllegalArgumentException("Discount must be between zero and gross amount");
        }
        return Math.subtractExact(gross, discountMinor);
    }
}
