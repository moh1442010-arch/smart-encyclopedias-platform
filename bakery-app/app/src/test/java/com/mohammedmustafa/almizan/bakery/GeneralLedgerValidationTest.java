package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeneralLedgerValidationTest {
    @Test public void acceptsBalancedMultiLineJournal() {
        GeneralLedger.validateBalancedTotals(12500L, 12500L, 3);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsSingleLineJournal() {
        GeneralLedger.validateBalancedTotals(100L, 100L, 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroDebitTotal() {
        GeneralLedger.validateBalancedTotals(0L, 100L, 2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroCreditTotal() {
        GeneralLedger.validateBalancedTotals(100L, 0L, 2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnequalBaseCurrencyTotals() {
        GeneralLedger.validateBalancedTotals(100L, 99L, 2);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsNegativeTotals() {
        GeneralLedger.validateBalancedTotals(-100L, -100L, 2);
    }

    @Test public void acceptsDebitOnlyOrCreditOnlyLine() {
        GeneralLedger.validateLine(new GeneralLedger.Line("1000", "MAIN", "SDG", 100L, 0L, 1L, 1L));
        GeneralLedger.validateLine(new GeneralLedger.Line("4000", "MAIN", "SDG", 0L, 100L, 1L, 1L));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsLineWithBothDebitAndCredit() {
        GeneralLedger.validateLine(new GeneralLedger.Line("1000", "MAIN", "SDG", 100L, 100L, 1L, 1L));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsNegativeLineAmount() {
        GeneralLedger.validateLine(new GeneralLedger.Line("1000", "MAIN", "SDG", -1L, 0L, 1L, 1L));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroExchangeRateDenominator() {
        GeneralLedger.validateLine(new GeneralLedger.Line("1000", "MAIN", "SDG", 100L, 0L, 1L, 0L));
    }

    @Test public void acceptsOnlyRealCalendarDates() {
        assertTrue(GeneralLedger.validDate("2026-11-11"));
        assertTrue("Leap day in a leap year must be accepted", GeneralLedger.validDate("2024-02-29"));
        assertTrue("Century leap year divisible by 400 must be accepted", GeneralLedger.validDate("2000-02-29"));
        assertTrue("Dates around the Gregorian cutover remain valid ISO dates", GeneralLedger.validDate("1582-10-10"));
        assertTrue("A non-leap century year must accept its valid February date", GeneralLedger.validDate("1500-02-28"));
        assertFalse("A Julian-only leap day must be rejected by the proleptic Gregorian calendar", GeneralLedger.validDate("1500-02-29"));
        assertFalse("Non-leap century year must be rejected", GeneralLedger.validDate("1900-02-29"));
        assertFalse(GeneralLedger.validDate("2026-02-29"));
        assertFalse(GeneralLedger.validDate("2026-04-31"));
        assertFalse(GeneralLedger.validDate("2026-13-01"));
        assertFalse(GeneralLedger.validDate("2026-00-10"));
        assertFalse(GeneralLedger.validDate("2026-11-1"));
        assertFalse("Year zero is outside the supported accounting date format", GeneralLedger.validDate("0000-01-01"));
        assertFalse(GeneralLedger.validDate(null));
    }
}
