package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class FinancialPeriodServiceTest {
    @Test public void acceptsValidPeriod() {
        assertTrue(FinancialPeriodService.validRange("2026-01-01","2026-12-31"));
    }
    @Test public void rejectsReversedPeriod() {
        assertFalse(FinancialPeriodService.validRange("2026-12-31","2026-01-01"));
    }
    @Test public void rejectsInvalidCalendarDate() {
        assertFalse(FinancialPeriodService.validRange("2026-02-30","2026-12-31"));
    }
    @Test public void rejectsWrongDateShape() {
        assertFalse(FinancialPeriodService.validRange("01-01-2026","2026-12-31"));
    }
}
