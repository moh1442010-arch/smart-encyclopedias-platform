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
    @Test public void reconciliationAddsAllRecordedCategoriesExactlyOnce() {
        assertEquals(150000L, BakeryMath.accountedTotal(50000,20000,10000,0,30000,25000,15000));
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
    @Test public void calculatesDailyFuelAndIngredientCostAndMargin() {
        long cost = BakeryMath.operatingCost(
                10, 5000, 20, 40000, 3, 2500, 15, 1800, 2, 1000, 12000);
        assertEquals(10*5000L + 20*40000L + 3*2500L + 15*1800L + 2*1000L + 12000L, cost);
        assertEquals(100000L-cost, BakeryMath.grossMargin(100000L,cost));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeCostQuantity() {
        BakeryMath.operatingCost(-1,500,1,40000,1,2500,1,1800,1,1000,12000);
    }
    @Test(expected=ArithmeticException.class) public void rejectsCostOverflow() {
        BakeryMath.operatingCost(Long.MAX_VALUE,2,0,0,0,0,0,0,0,0,0);
    }
}
