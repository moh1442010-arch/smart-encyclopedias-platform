package com.mohammedmustafa.smartcompany;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Exact fixed-scale monetary value for accounting calculations.
 *
 * IMPORTANT: This utility is the foundation for a staged migration; existing
 * database REAL/double fields must not be assumed migrated until every read,
 * write, journal posting and report is converted and a data migration is tested.
 * The scale is explicit because currencies do not all use two decimal places.
 */
public final class Money implements Comparable<Money> {
    private final long minorUnits;
    private final int scale;

    private Money(long minorUnits, int scale) {
        if (scale < 0 || scale > 6) {
            throw new IllegalArgumentException("Currency scale must be between 0 and 6");
        }
        this.minorUnits = minorUnits;
        this.scale = scale;
    }

    public static Money ofMinor(long minorUnits, int scale) {
        return new Money(minorUnits, scale);
    }

    /**
     * Converts a major-unit amount without silently rounding away precision.
     * Use only after the currency scale is known from company settings.
     */
    public static Money fromMajor(BigDecimal amount, int scale) {
        Objects.requireNonNull(amount, "amount");
        if (scale < 0 || scale > 6) {
            throw new IllegalArgumentException("Currency scale must be between 0 and 6");
        }
        long minor = amount.setScale(scale, RoundingMode.UNNECESSARY)
                .movePointRight(scale).longValueExact();
        return new Money(minor, scale);
    }

    public static Money parse(String amount, int scale) {
        if (amount == null || amount.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }
        try {
            return fromMajor(new BigDecimal(amount.trim()), scale);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid monetary amount", ex);
        }
    }

    public long minorUnits() {
        return minorUnits;
    }

    public int scale() {
        return scale;
    }

    public BigDecimal toMajorDecimal() {
        return BigDecimal.valueOf(minorUnits, scale);
    }

    public Money plus(Money other) {
        requireSameScale(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), scale);
    }

    public Money minus(Money other) {
        requireSameScale(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), scale);
    }

    public Money negate() {
        return new Money(Math.negateExact(minorUnits), scale);
    }

    @Override
    public int compareTo(Money other) {
        requireSameScale(other);
        return Long.compare(minorUnits, other.minorUnits);
    }

    private void requireSameScale(Money other) {
        Objects.requireNonNull(other, "other");
        if (scale != other.scale) {
            throw new IllegalArgumentException("Cannot combine money with different currency scales");
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Money)) return false;
        Money money = (Money) other;
        return minorUnits == money.minorUnits && scale == money.scale;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minorUnits, scale);
    }

    @Override
    public String toString() {
        return toMajorDecimal().toPlainString();
    }
}
