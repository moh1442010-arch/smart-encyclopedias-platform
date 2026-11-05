package com.mohammedmustafa.almizan.bakery;
import org.junit.Test;
import static org.junit.Assert.*;
public class FactoryMathTest {
 @Test public void allocatesMaterialCostProportionally(){assertEquals(2500L,FactoryMath.prorate(10000,250,1000));}
 @Test public void calculatesUnitCostInThousandthsOfCurrencyPerWholeUnit(){assertEquals(2500000L,FactoryMath.unitCostMilli(10000,4000));assertEquals(2500.0,FactoryMath.unitCostMilli(10000,4000)/1000.0,0.001);}
 @Test public void valuesMaterialIssueAtWeightedAverageCost(){assertEquals(2500L,FactoryMath.costForStockIssue(10000,250,1000));}
 @Test public void calculatesWeightedAverageAfterReceivingNewStock(){assertEquals(3000000L,FactoryMath.weightedAverageUnitCost(1000,2000,1000,4000));}
 @Test public void keepsZeroUnitCostForEmptyFinishedStock(){assertEquals(0L,FactoryMath.weightedAverageUnitCost(0,0,0,0));}
 @Test(expected=IllegalArgumentException.class) public void rejectsIssueAboveStock(){FactoryMath.costForStockIssue(100,11,10);}
 @Test(expected=IllegalArgumentException.class) public void rejectsNegativeInventoryValuation(){FactoryMath.weightedAverageUnitCost(10,-1,2,3);}

 @Test public void acceptsBankakReferenceWhenAmountIsPresent(){FactoryMath.validateBankakReference(100,"TX-001");}
 @Test public void acceptsNoBankakReferenceForZeroBankak(){FactoryMath.validateBankakReference(0,"");}
 @Test(expected=IllegalArgumentException.class) public void rejectsMissingBankakReference(){FactoryMath.validateBankakReference(100," ");}
 @Test(expected=IllegalArgumentException.class) public void rejectsNegativeBankakAmount(){FactoryMath.validateBankakReference(-1,"TX-001");}
 @Test public void calculatesCustomerDebtAfterCashAndBankak(){assertEquals(3000L,FactoryMath.debt(10000,4000,3000));}
 @Test public void leavesCorrectMaterialBalance(){assertEquals(2500L,FactoryMath.remainingStock(5000,2500));}
 @Test(expected=IllegalArgumentException.class) public void rejectsSaleOverAvailableStock(){FactoryMath.remainingStock(10,11);}
 @Test(expected=IllegalArgumentException.class) public void rejectsOverpayment(){FactoryMath.debt(100,80,30);}
 @Test(expected=ArithmeticException.class) public void rejectsMonetaryOverflow(){FactoryMath.add(Long.MAX_VALUE,1);}
 @Test(expected=ArithmeticException.class) public void rejectsQuantityMultiplicationOverflow(){FactoryMath.multiply(Long.MAX_VALUE,2);}
 @Test public void batchMaterialConsumptionMultipliesRecipeByBatchCount(){assertEquals(6000L,FactoryMath.multiply(1500,4));}
 @Test public void rejectsNegativeMonetaryValues(){try{FactoryMath.add(-1,2);fail("negative value accepted");}catch(IllegalArgumentException expected){}}
 @Test public void allocatesWholeBatchCostToOneUnitCorrectly(){assertEquals(1250000L,FactoryMath.unitCostMilli(5000,4000));}

 @Test public void proratesAllRemainingStockValueExactly(){assertEquals(987654L,FactoryMath.costForStockIssue(987654L,1250L,1250L));}
 @Test public void supportsFractionalRecipeConsumptionAcrossBatches(){assertEquals(500L,FactoryMath.multiply(250L,2L));}
 @Test public void preservesValueWhenInventoryIsPartiallyIssued(){long cost=FactoryMath.costForStockIssue(10001L,333L,1000L);assertEquals(6671L,10001L-cost);}
 @Test public void computesZeroDebtForFullySettledSale(){assertEquals(0L,FactoryMath.debt(15000L,5000L,10000L));}
 @Test public void computesDebtWhenSaleIsPartiallySettled(){assertEquals(7000L,FactoryMath.debt(15000L,5000L,3000L));}
 @Test public void calculatesPartialSaleReturnAtOriginalSellingValue(){assertEquals(3000L,FactoryMath.proportionalReturnValue(12000L,250L,1000L));}
 @Test public void calculatesReturnedCostAtOriginalCogs(){assertEquals(2000L,FactoryMath.proportionalReturnValue(8000L,250L,1000L));}
 @Test public void tracksRemainingReturnableQuantity(){assertEquals(500L,FactoryMath.returnableQuantity(1000L,250L,250L));}
 @Test(expected=IllegalArgumentException.class) public void rejectsReturnsAboveOriginalSaleQuantity(){FactoryMath.returnableQuantity(1000L,700L,301L);}
 @Test public void accumulatesWorkInProgressCost(){assertEquals(17500L,FactoryMath.workInProgressCost(10000L,5000L,2500L));}
 @Test public void calculatesWorkInProgressUnitCost(){assertEquals(3500000L,FactoryMath.workInProgressUnitCost(17500L,5000L));}
 @Test public void acceptsBalancedJournal(){assertTrue(FactoryMath.journalIsBalanced(15000L,15000L));}
 @Test public void rejectsUnbalancedJournal(){assertFalse(FactoryMath.journalIsBalanced(15000L,14999L));}
 @Test(expected=IllegalArgumentException.class) public void rejectsNegativeJournalTotals(){FactoryMath.journalIsBalanced(-1L,0L);}

}
