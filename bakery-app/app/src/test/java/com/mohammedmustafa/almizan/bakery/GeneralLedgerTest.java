package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class GeneralLedgerTest {
    @Test public void acceptsBalancedMultiLineJournal() {
        GeneralLedger.validateBalancedTotals(125000L,125000L,3);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnbalancedJournal() {
        GeneralLedger.validateBalancedTotals(125000L,124999L,2);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsSingleLineJournal() {
        GeneralLedger.validateBalancedTotals(100L,100L,1);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroSidedJournal() {
        GeneralLedger.validateBalancedTotals(0L,0L,2);
    }
    @Test public void convertsForeignCurrencyIntoBaseMinorUnits() {
        assertEquals(18518L,GeneralLedger.toBaseMinor(12345L,3L,2L,2,2));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroAmountJournalLine() {
        GeneralLedger.toBaseMinor(0L,1L,1L,2,2);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidRate() {
        GeneralLedger.toBaseMinor(100L,1L,0L,2,2);
    }
}
