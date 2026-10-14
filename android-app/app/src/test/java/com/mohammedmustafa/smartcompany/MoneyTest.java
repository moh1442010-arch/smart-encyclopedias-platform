package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class MoneyTest {
    @Test public void convertsDecimalCurrencyToIntegerMinorUnits() {
        assertEquals(12345L, Money.toMinorUnits("123.45"));
        assertEquals(-125L, Money.toMinorUnits("-1.25"));
        assertEquals(100L, Money.toMinorUnits("1"));
    }

    @Test public void roundsHalfUpAtCurrencyBoundary() {
        assertEquals(101L, Money.toMinorUnits("1.005"));
        assertEquals(100L, Money.toMinorUnits("1.004"));
    }

    @Test public void invoiceArithmeticAvoidsBinaryFloatingPointDrift() {
        assertEquals(100L, Money.multiplyQuantityAndPrice("3", "0.3333333333"));
        assertEquals(1000L, Money.netInvoiceMinorUnits("3", "3.50", "0.50"));
    }

    @Test public void rejectsNonFiniteAndOutOfRangeAmounts() {
        try { Money.toMinorUnits(Double.NaN); fail("NaN must be rejected"); }
        catch (IllegalArgumentException expected) { }
        try { Money.toMinorUnits("999999999999999999999999"); fail("Overflow must be rejected"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void minorUnitsRoundTripExactly() {
        assertEquals(new BigDecimal("123.45"), Money.fromMinorUnits(12345L));
        assertEquals(new BigDecimal("-0.01"), Money.fromMinorUnits(-1L));
    }
}
