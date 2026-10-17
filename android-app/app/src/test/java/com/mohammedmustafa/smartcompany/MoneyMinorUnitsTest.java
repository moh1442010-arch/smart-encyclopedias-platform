package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import static org.junit.Assert.*;

public class MoneyMinorUnitsTest {
    @Test public void parsesExactTwoDecimalAmountsToIntegerMinorUnits() {
        assertEquals(1234L, MoneyMinorUnits.parse("12.34", 2));
        assertEquals(0L, MoneyMinorUnits.parse("0.00", 2));
        assertEquals(-125L, MoneyMinorUnits.parse("-1.25", 2));
    }

    @Test public void rejectsExcessFractionalPrecisionInsteadOfSilentlyRounding() {
        try {
            MoneyMinorUnits.parse("12.345", 2);
            fail("Excess precision must be rejected at the input boundary");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsInvalidAndOverflowAmounts() {
        try { MoneyMinorUnits.parse("NaN", 2); fail("NaN must be rejected"); }
        catch (IllegalArgumentException expected) { }
        try { MoneyMinorUnits.parse("999999999999999999999999.99", 2); fail("Overflow must be rejected"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void legacyConversionUsesDocumentedHalfUpRounding() {
        assertEquals(101L, MoneyMinorUnits.fromLegacyDouble(1.005d, 2));
        assertEquals(-101L, MoneyMinorUnits.fromLegacyDouble(-1.005d, 2));
        try { MoneyMinorUnits.fromLegacyDouble(Double.POSITIVE_INFINITY, 2); fail("Infinity must be rejected"); }
        catch (IllegalArgumentException expected) { }
    }


    @Test public void computesLineTotalsWithOneExplicitCurrencyRoundingBoundary() {
        assertEquals(1000L, MoneyMinorUnits.lineTotalMinor("3", "3.50", "0.50", 2));
        assertEquals(1000L, MoneyMinorUnits.lineTotalMinor("3.003", "3.33", "0.00", 2));
    }

    @Test public void rejectsInvalidLineAmountsAndExcessPricePrecision() {
        try { MoneyMinorUnits.lineTotalMinor("0", "10.00", "0.00", 2); fail("Zero quantity must be rejected"); }
        catch (IllegalArgumentException expected) { }
        try { MoneyMinorUnits.lineTotalMinor("1", "10.001", "0.00", 2); fail("Price precision must be explicit"); }
        catch (IllegalArgumentException expected) { }
        try { MoneyMinorUnits.lineTotalMinor("1", "10.00", "10.01", 2); fail("Discount cannot exceed gross"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void roundsExtendedCostAtTheCurrencyBoundary() {
        assertEquals(1001L, MoneyMinorUnits.extendedMinor(3, 3.335, 2));
        try { MoneyMinorUnits.extendedMinor(Double.NaN, 1, 2); fail("Non-finite quantity must be rejected"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void formatsMinorUnitsWithoutFloatingPointConversion() {
        assertEquals("1234.56", MoneyMinorUnits.format(123456L, 2));
        assertEquals("-0.05", MoneyMinorUnits.format(-5L, 2));
        assertEquals("123", MoneyMinorUnits.format(123L, 0));
    }
}