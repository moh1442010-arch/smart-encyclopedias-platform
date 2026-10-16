package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import java.math.BigDecimal;
import static org.junit.Assert.*;

public class MoneyTest {
    @Test public void parsesAndAddsExactlyWithoutBinaryFloatingPoint() {
        Money a = Money.parse("0.10", 2);
        Money b = Money.parse("0.20", 2);
        assertEquals(30L, a.plus(b).minorUnits());
        assertEquals("0.30", a.plus(b).toString());
    }

    @Test public void supportsZeroDecimalCurrencies() {
        Money amount = Money.fromMajor(new BigDecimal("125"), 0);
        assertEquals(125L, amount.minorUnits());
        assertEquals("125", amount.toString());
    }

    @Test public void rejectsSilentRoundingOfInput() {
        try {
            Money.parse("1.005", 2);
            fail("Amounts with unsupported fractional precision must not be rounded silently");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Invalid") ||
                    expected.getCause() instanceof ArithmeticException);
        }
    }

    @Test public void rejectsMixedCurrencyScales() {
        try {
            Money.ofMinor(100, 2).plus(Money.ofMinor(100, 0));
            fail("Different scales must not be combined implicitly");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("different currency scales"));
        }
    }

    @Test public void detectsOverflowInsteadOfWrapping() {
        try {
            Money.ofMinor(Long.MAX_VALUE, 2).plus(Money.ofMinor(1, 2));
            fail("Overflow must be rejected");
        } catch (ArithmeticException expected) {
            // Expected: never permit wrapped monetary totals.
        }
    }

    @Test public void preservesNegativeAmountsAndExactSubtraction() {
        Money balance = Money.parse("10.25", 2).minus(Money.parse("12.50", 2));
        assertEquals("-2.25", balance.toString());
        assertEquals(-225L, balance.minorUnits());
    }
}
