package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class BakeryMathTest {
    @Test public void dynamicCutRowsDoNotAssumeAFixedNumberOfCuts() {
        assertEquals(7000L, BakeryMath.sum(700L, 700L, 700L, 700L, 700L, 700L, 700L));
        assertEquals(1750000L, BakeryMath.valueForLoaves(
                BakeryMath.sum(700L, 700L, 700L, 700L, 700L, 700L, 700L), 4L));
        assertEquals(1400L, BakeryMath.sum(700L, 700L));
        assertEquals(466667L, BakeryMath.valueForLoaves(1400L, 3L));
        assertEquals(350000L, BakeryMath.valueForLoaves(1400L, 4L));
        assertEquals(280000L, BakeryMath.valueForLoaves(1400L, 5L));
    }
    @Test public void correctedCutValueUsesLoavesPerOneThousand() {
        assertEquals(3000000L, BakeryMath.valueForLoaves(12000L, 4L));
    }
    @Test public void sumsCutRowsAndValuesDailyProductionFromLoavesPerThousand() {
        long totalLoaves = Math.addExact(700L, 700L);
        assertEquals(1400L, totalLoaves);
        assertEquals(2000L, BakeryMath.valueForLoaves(totalLoaves, 700L));
    }
    @Test public void productionMultipliesWorkUnitsByLoaves() {
        assertEquals(2000L, BakeryMath.productionLoaves(4,500));
    }
    @Test public void valuesLoavesUsingConfiguredPriceBasis() {
        assertEquals(250000L, BakeryMath.valueForLoaves(1000,4));
    }
    @Test public void roundsMoneyToWholeSudanesePounds() {
        assertEquals(333L, BakeryMath.valueForLoaves(1,3));
    }
    @Test public void workerWagesPerSackMultiplyBySacksWorked() {
        assertEquals(20000L, BakeryMath.sum(Math.multiplyExact(2000L,5L), Math.multiplyExact(2000L,5L)));
    }
    @Test public void reconciliationAddsAllRecordedCategoriesExactlyOnce() {
        assertEquals(150000L, BakeryMath.accountedTotal(50000,20000,10000,0,30000,25000,15000));
    }
    @Test public void balancedLedgerHasZeroDifference() {
        assertEquals(0L, BakeryMath.difference(150000,150000));
    }
    @Test public void reconciliationRequiresZeroDifferenceConfiguredPriceAndValidRows() {
        assertTrue(BakeryMath.isReconciliationValid(0L, 0, true));
        assertFalse(BakeryMath.isReconciliationValid(0L, 1, true));
        assertFalse(BakeryMath.isReconciliationValid(0L, 0, false));
        assertFalse(BakeryMath.isReconciliationValid(1L, 0, true));
        assertFalse(BakeryMath.isReconciliationValid(-1L, 0, true));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeInvalidRowCount() {
        BakeryMath.isReconciliationValid(0L, -1, true);
    }
    @Test public void reconciliationIncludesExpensesAndSpoilageInRecordedCategories() {
        assertEquals(150000L, BakeryMath.accountedTotal(50000L,20000L,10000L,0L,30000L,25000L,15000L));
        assertEquals(0L, BakeryMath.difference(150000L, BakeryMath.accountedTotal(50000L,20000L,10000L,0L,30000L,25000L,15000L)));
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
    @Test public void flourStockDeductsDailyUsageOnce() {
        assertEquals(3L, BakeryMath.stockAfterUsage(10,0,7));
        assertEquals(3L, BakeryMath.stockAfterUsage(3,7,7));
    }
    @Test public void editingDailyUsageAdjustsOnlyTheDifference() {
        assertEquals(4L, BakeryMath.stockAfterUsage(3,7,6));
        assertEquals(2L, BakeryMath.stockAfterUsage(3,7,8));
    }
    @Test public void reversingDailyWorkRestoresTheOpeningFlourBalance() {
        assertEquals(10L, BakeryMath.stockAfterUsage(3,7,0));
    }
    @Test public void savingOpeningStockAfterDailyUsageKeepsClosingBalanceCorrect() {
        assertEquals(3L, BakeryMath.stockAfterUsage(10,0,7));
        assertEquals(3L, BakeryMath.stockAfterUsage(3,7,7));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeStockMovementInputs() {
        BakeryMath.stockAfterUsage(10,-1,7);
    }
    @Test public void netMarginSubtractsSpoilageWagesExpensesOperatingCostAndPriceDifference() {
        assertEquals(37000L, BakeryMath.netMargin(200000,10000,30000,20000,100000,3000));
        assertEquals(40000L, BakeryMath.netMargin(200000,10000,30000,20000,100000));
    }
    @Test public void operatingCostUsesEnteredPerWorkUnitRatesTimesDailyWork() {
        assertEquals(180000L, BakeryMath.operatingCostPerWorkUnit(10, 6000, 5000, 7000));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativePerWorkUnitCost() {
        BakeryMath.operatingCostPerWorkUnit(10, 500, -1);
    }
    @Test(expected=IllegalArgumentException.class) public void netMarginRejectsNegativeInputs() {
        BakeryMath.netMargin(100000,-1,0,0,0);
    }
    @Test public void tenFlourBagsAndSevenBagsOfWorkLeavesThree() {
        assertEquals(3L, BakeryMath.stockAfterUsage(10L,0L,7L));
        assertEquals(3L, BakeryMath.stockAfterUsage(3L,7L,7L));
    }
    @Test public void bakeryNetMarginUsesProductionLessSpoilageWagesExpensesAndOperatingCost() {
        assertEquals(40000L, BakeryMath.netMargin(200000L,10000L,30000L,20000L,100000L));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeCostQuantity() {
        BakeryMath.operatingCost(-1,500,1,40000,1,2500,1,1800,1,1000,12000);
    }
    @Test(expected=ArithmeticException.class) public void rejectsCostOverflow() {
        BakeryMath.operatingCost(Long.MAX_VALUE,2,0,0,0,0,0,0,0,0,0);
    }

    @Test(expected=ArithmeticException.class) public void rejectsOverflowWhenSummingMoneyCategories() {
        BakeryMath.sum(Long.MAX_VALUE, 1L);
    }
    @Test(expected=ArithmeticException.class) public void rejectsOverflowWhenCalculatingReconciliationDifference() {
        BakeryMath.difference(Long.MAX_VALUE, -1L);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeLoafCountForValuation() {
        BakeryMath.valueForLoaves(-1L, 4L);
    }
    @Test(expected=ArithmeticException.class) public void rejectsOverflowWhenCalculatingDailyProduction() {
        BakeryMath.productionLoaves(Long.MAX_VALUE, 2L);
    }
    @Test public void reconciliationKeepsPriceBasisAndInvalidRowsAsHardStops() {
        assertFalse(BakeryMath.isReconciliationValid(0L, 0, false));
        assertFalse(BakeryMath.isReconciliationValid(0L, 2, true));
        assertTrue(BakeryMath.isReconciliationValid(0L, 0, true));
    }

    @Test public void cutLoavesSumAndAccountantChosenRateUseCorrectProductionValue() {
        long totalLoaves = BakeryMath.sum(700L, 700L);
        assertEquals(1400L, totalLoaves);
        assertEquals(466667L, BakeryMath.valueForLoaves(totalLoaves, 3L));
        assertEquals(350000L, BakeryMath.valueForLoaves(totalLoaves, 4L));
        assertEquals(280000L, BakeryMath.valueForLoaves(totalLoaves, 5L));
    }

    @Test public void approvedBakeryReconciliationExampleBalancesDistributionPlusPriceDifferenceWagesExpensesAndSpoilage() {
        long productionValue = BakeryMath.valueForLoaves(12000L, 4L); // 12,000 / 4 x 1,000
        assertEquals(3000000L, productionValue);
        long distributionCollections = BakeryMath.sum(2500000L, 200000L, 100000L);
        long secondSide = BakeryMath.accountedTotal(
                2500000L, 200000L, 100000L, 50000L, 80000L, 40000L, 30000L);
        assertEquals(3000000L, secondSide);
        assertEquals(0L, BakeryMath.difference(productionValue, secondSide));
        assertEquals(0L, BakeryMath.difference(productionValue,
                BakeryMath.accountedTotal(2500000L, 200000L, 100000L, 50000L, 80000L, 40000L, 30000L)));
    }
}
