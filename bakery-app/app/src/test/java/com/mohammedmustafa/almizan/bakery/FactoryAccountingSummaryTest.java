package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class FactoryAccountingSummaryTest {
    @Test public void includesAllCounterAndDistributionSettlementTypes() {
        FactoryAccountingSummary s=FactoryAccountingSummary.of(100,20,30,40,10,50,0,0,0,0,0);
        assertEquals(250,s.grossSales);
        assertEquals(170,s.cashAndBankakAtSale);
        assertEquals(80,s.receivablesAtSale);
        assertEquals(150,s.counterCash+s.counterBankak+s.counterReceivables);
        assertEquals(100,s.distributionCash+s.distributionBankak+s.distributionReceivables);
    }

    @Test public void calculatesApprovedOperatingProfitAndDoesNotDoubleCountProductionUsage() {
        FactoryAccountingSummary s=FactoryAccountingSummary.of(1000,200,300,400,100,500,100,700,50,25,30);
        assertEquals(2400,s.netSales);
        assertEquals(805,s.totalOperatingCost);
        assertEquals(1595,s.operatingProfit);
    }

    @Test public void zeroSalesAndCostsProduceZero() {
        FactoryAccountingSummary s=FactoryAccountingSummary.of(0,0,0,0,0,0,0,0,0,0,0);
        assertEquals(0,s.operatingProfit);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsReturnsAboveAllChannelsSales() {
        FactoryAccountingSummary.of(10,0,0,0,0,0,11,0,0,0,0);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsNegativeCostValues() {
        FactoryAccountingSummary.of(10,0,0,0,0,0,0,-1,0,0,0);
    }
}
