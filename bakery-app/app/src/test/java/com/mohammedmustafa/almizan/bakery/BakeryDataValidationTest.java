package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class BakeryDataValidationTest {
    @Test public void acceptsRealIsoCalendarDates() {
        assertTrue(BakeryDataValidation.isValidIsoDate("2026-10-24"));
        assertTrue(BakeryDataValidation.isValidIsoDate("2024-02-29"));
    }

    @Test public void rejectsMalformedOrImpossibleDates() {
        assertFalse(BakeryDataValidation.isValidIsoDate(null));
        assertFalse(BakeryDataValidation.isValidIsoDate("2026-1-04"));
        assertFalse(BakeryDataValidation.isValidIsoDate("2026-02-29"));
        assertFalse(BakeryDataValidation.isValidIsoDate("2026-13-01"));
        assertFalse(BakeryDataValidation.isValidIsoDate("2026-10-24x"));
    }

    @Test public void acceptsZeroAndPositiveInventoryUsage() {
        assertEquals(0L, BakeryDataValidation.requireNonNegative(0L, "flour_bags"));
        assertEquals(12L, BakeryDataValidation.requireNonNegative(12L, "flour_bags"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeInventoryUsageInBackup() {
        BakeryDataValidation.requireNonNegative(-1L, "flour_bags");
    }
}
