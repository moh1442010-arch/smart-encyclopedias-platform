package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class UnifiedAccountingMathTest {
    @Test public void includesCounterAndDistributionCashBankakAndReceivables() {
        UnifiedAccountingMath.Sales sales=new UnifiedAccountingMath.Sales(100,20,30,40,10,50,0);
        assertEquals(250,sales.grossRevenue());
        assertEquals(170,sales.cashAndBankakCollections());
        assertEquals(80,sales.receivablesBeforeCollections());
        assertEquals(150,sales.counterRevenue());
        assertEquals(100,sales.distributionRevenue());
    }

    @Test public void profitSubtractsReturnsAndEveryUniqueOperatingCostOnce() {
        UnifiedAccountingMath.Sales sales=new UnifiedAccountingMath.Sales(1000,200,300,400,100,500,100);
        long costs=UnifiedAccountingMath.operatingCost(Arrays.asList(
            new UnifiedAccountingMath.CostItem("flour:issue:1","RAW_MATERIAL",300,false),
            new UnifiedAccountingMath.CostItem("wages:day:1","LABOR",100,false),
            new UnifiedAccountingMath.CostItem("expense:1","OVERHEAD",50,false),
            new UnifiedAccountingMath.CostItem("spoilage:1","SPOILAGE",25,false),
            new UnifiedAccountingMath.CostItem("asset:oven:1","CAPITAL_ASSET",900,false==true)
        ));
        assertEquals(1375,sales.netRevenue());
        assertEquals(1375,costs);
        assertEquals(0,UnifiedAccountingMath.profit(sales,Arrays.asList(
            new UnifiedAccountingMath.CostItem("flour:issue:1","RAW_MATERIAL",300,false),
            new UnifiedAccountingMath.CostItem("wages:day:1","LABOR",100,false),
            new UnifiedAccountingMath.CostItem("expense:1","OVERHEAD",50,false),
            new UnifiedAccountingMath.CostItem("spoilage:1","SPOILAGE",25,false),
            new UnifiedAccountingMath.CostItem("asset:oven:1","CAPITAL_ASSET",900,true)
        )));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsDuplicateCostSourceToPreventDoubleCounting() {
        UnifiedAccountingMath.operatingCost(Arrays.asList(
            new UnifiedAccountingMath.CostItem("wage:1","LABOR",100,false),
            new UnifiedAccountingMath.CostItem("wage:1","LABOR",100,false)));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsReturnsGreaterThanGrossSales() {
        new UnifiedAccountingMath.Sales(10,0,0,0,0,0,11);
    }

    @Test public void capitalAssetPurchaseIsNotOperatingCost() {
        assertEquals(0,UnifiedAccountingMath.operatingCost(Arrays.asList(
            new UnifiedAccountingMath.CostItem("asset:1","CAPITAL_ASSET",5000,true))));
    }
}
