package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class BakeryMathTest {
    @Test public void productionMultipliesWorkUnitsByLoaves() {
        assertEquals(2000L, BakeryMath.productionLoaves(4,500));
    }
    @Test public void valuesLoavesUsingConfiguredPriceBasis() {
        assertEquals(250000L, BakeryMath.valueForLoaves(1000,4));
    }
    @Test public void roundsMoneyToWholeSudanesePounds() {
        assertEquals(333L, BakeryMath.valueForLoaves(1,3));
    }
    @Test public void balancedLedgerHasZeroDifference() {
        assertEquals(0L, BakeryMath.difference(150000,150000));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsZeroPriceBasis() {
        BakeryMath.valueForLoaves(100,0);
    }
    @Test(expected=ArithmeticException.class) public void rejectsOverflow() {
        BakeryMath.productionLoaves(Long.MAX_VALUE,2);
    }
}
