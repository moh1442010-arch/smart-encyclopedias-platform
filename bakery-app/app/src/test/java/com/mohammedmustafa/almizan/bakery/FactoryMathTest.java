package com.mohammedmustafa.almizan.bakery;
import org.junit.Test;
import static org.junit.Assert.*;
public class FactoryMathTest {
 @Test public void allocatesMaterialCostProportionally(){assertEquals(2500L,FactoryMath.prorate(10000,250,1000));}
 @Test public void calculatesMilliUnitCost(){assertEquals(2500000L,FactoryMath.unitCostMilli(10000,4000));}
 @Test public void calculatesCustomerDebtAfterCashAndBankak(){assertEquals(3000L,FactoryMath.debt(10000,4000,3000));}
 @Test public void leavesCorrectMaterialBalance(){assertEquals(2500L,FactoryMath.remainingStock(5000,2500));}
 @Test(expected=IllegalArgumentException.class) public void rejectsSaleOverAvailableStock(){FactoryMath.remainingStock(10,11);}
 @Test(expected=IllegalArgumentException.class) public void rejectsOverpayment(){FactoryMath.debt(100,80,30);}
 @Test(expected=ArithmeticException.class) public void rejectsMonetaryOverflow(){FactoryMath.add(Long.MAX_VALUE,1);}
 @Test public void batchMaterialConsumptionMultipliesRecipeByBatchCount(){assertEquals(6000L,FactoryMath.multiply(1500,4));}
 @Test public void rejectsNegativeMonetaryValues(){try{FactoryMath.add(-1,2);fail("negative value accepted");}catch(IllegalArgumentException expected){}}
 @Test public void allocatesWholeBatchCostToOneUnitCorrectly(){assertEquals(1250000L,FactoryMath.unitCostMilli(5000,4000));}
}