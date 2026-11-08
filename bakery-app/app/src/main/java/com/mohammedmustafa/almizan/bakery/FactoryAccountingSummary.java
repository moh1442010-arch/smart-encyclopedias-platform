package com.mohammedmustafa.almizan.bakery;

import java.util.Arrays;

/**
 * One reporting contract for factory sales and operating costs.
 * Sales include receivables; collections remain a separate cash-flow measure.
 */
public final class FactoryAccountingSummary {
    public final long counterCash, counterBankak, counterReceivables;
    public final long distributionCash, distributionBankak, distributionReceivables;
    public final long grossSales, salesReturns, netSales;
    public final long cashAndBankakAtSale, receivablesAtSale;
    public final long costOfGoodsSold, otherExpenses, spoilage, manualRawConsumption;
    public final long totalOperatingCost, operatingProfit;

    private FactoryAccountingSummary(UnifiedAccountingMath.Sales sales,
            long costOfGoodsSold, long otherExpenses, long spoilage, long manualRawConsumption) {
        this.counterCash=sales.counterCash;
        this.counterBankak=sales.counterBankak;
        this.counterReceivables=sales.counterReceivables;
        this.distributionCash=sales.distributionCash;
        this.distributionBankak=sales.distributionBankak;
        this.distributionReceivables=sales.distributionReceivables;
        this.grossSales=sales.grossRevenue();
        this.salesReturns=sales.salesReturns;
        this.netSales=sales.netRevenue();
        this.cashAndBankakAtSale=sales.cashAndBankakCollections();
        this.receivablesAtSale=sales.receivablesBeforeCollections();
        this.costOfGoodsSold=nonNegative(costOfGoodsSold,"cost of goods sold");
        this.otherExpenses=nonNegative(otherExpenses,"expenses");
        this.spoilage=nonNegative(spoilage,"spoilage");
        this.manualRawConsumption=nonNegative(manualRawConsumption,"manual raw consumption");
        this.totalOperatingCost=UnifiedAccountingMath.operatingCost(Arrays.asList(
            new UnifiedAccountingMath.CostItem("factory:cogs","COST_OF_GOODS_SOLD",this.costOfGoodsSold,false),
            new UnifiedAccountingMath.CostItem("factory:expenses","OPERATING_EXPENSES",this.otherExpenses,false),
            new UnifiedAccountingMath.CostItem("factory:spoilage","SPOILAGE",this.spoilage,false),
            new UnifiedAccountingMath.CostItem("factory:manual-raw-usage","RAW_MATERIAL_CONSUMPTION",this.manualRawConsumption,false)
        ));
        this.operatingProfit=UnifiedAccountingMath.profit(sales,Arrays.asList(
            new UnifiedAccountingMath.CostItem("factory:cogs","COST_OF_GOODS_SOLD",this.costOfGoodsSold,false),
            new UnifiedAccountingMath.CostItem("factory:expenses","OPERATING_EXPENSES",this.otherExpenses,false),
            new UnifiedAccountingMath.CostItem("factory:spoilage","SPOILAGE",this.spoilage,false),
            new UnifiedAccountingMath.CostItem("factory:manual-raw-usage","RAW_MATERIAL_CONSUMPTION",this.manualRawConsumption,false)
        ));
    }

    public static FactoryAccountingSummary of(
            long counterCash,long counterBankak,long counterReceivables,
            long distributionCash,long distributionBankak,long distributionReceivables,
            long salesReturns,long netCostOfGoodsSold,long expenses,long spoilage,long manualRawConsumption) {
        UnifiedAccountingMath.Sales sales=new UnifiedAccountingMath.Sales(
            counterCash,counterBankak,counterReceivables,
            distributionCash,distributionBankak,distributionReceivables,salesReturns);
        return new FactoryAccountingSummary(sales,netCostOfGoodsSold,expenses,spoilage,manualRawConsumption);
    }

    private static long nonNegative(long value,String name) {
        if(value<0) throw new IllegalArgumentException("Negative "+name);
        return value;
    }
}
