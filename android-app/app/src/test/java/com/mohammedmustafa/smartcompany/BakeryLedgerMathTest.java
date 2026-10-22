package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import static org.junit.Assert.*;

public class BakeryLedgerMathTest {
    @Test public void sampleFourCutsAtFiveHundredLoavesAndFourPerThousandIsFiveHundredThousand() {
        long loaves = BakeryLedgerMath.productionLoaves(4, 500);
        assertEquals(2000L, loaves);
        assertEquals(500000L, BakeryLedgerMath.valueForLoaves(loaves, 4));
    }
    @Test public void configurableLoavesPerThousandControlsValue() {
        assertEquals(400000L, BakeryLedgerMath.valueForLoaves(2000, 5));
        assertEquals(1000000L, BakeryLedgerMath.valueForLoaves(2000, 2));
    }
    @Test public void reconciliationIsZeroOnlyWhenValuesMatch() {
        assertEquals(0L, BakeryLedgerMath.reconciliationDifference(500000, 500000));
        assertEquals(2500L, BakeryLedgerMath.reconciliationDifference(500000, 497500));
        assertEquals(-2500L, BakeryLedgerMath.reconciliationDifference(500000, 502500));
    }
    @Test(expected = IllegalArgumentException.class)
    public void rejectsZeroLoavesPerThousand() { BakeryLedgerMath.valueForLoaves(100, 0); }
}