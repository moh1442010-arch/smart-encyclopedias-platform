package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrialPolicyTest {
    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long START = 1_800_000_000_000L;

    @Test public void activeBeforeSevenDays() {
        assertEquals(TrialPolicy.Status.ACTIVE,
                TrialPolicy.evaluate(START, START + 2 * DAY, START + 6 * DAY));
    }

    @Test public void expiresAtExactly168Hours() {
        assertEquals(TrialPolicy.Status.EXPIRED,
                TrialPolicy.evaluate(START, START + 7 * DAY, START + 7 * DAY));
    }

    @Test public void expiresAfterSevenDaysEvenIfLastSeenWasEarlier() {
        assertEquals(TrialPolicy.Status.EXPIRED,
                TrialPolicy.evaluate(START, START + 3 * DAY, START + 8 * DAY));
    }

    @Test public void detectsMaterialClockRollback() {
        assertEquals(TrialPolicy.Status.CLOCK_ROLLBACK,
                TrialPolicy.evaluate(START, START + 4 * DAY, START + 2 * DAY));
    }

    @Test public void toleratesSmallClockAdjustment() {
        assertEquals(TrialPolicy.Status.ACTIVE,
                TrialPolicy.evaluate(START, START + DAY, START + DAY - 60_000L));
    }

    @Test public void rejectsMissingOrInconsistentState() {
        assertEquals(TrialPolicy.Status.INVALID_STATE,
                TrialPolicy.evaluate(0L, START, START));
        assertEquals(TrialPolicy.Status.INVALID_STATE,
                TrialPolicy.evaluate(START + DAY, START, START + 2 * DAY));
    }
}
