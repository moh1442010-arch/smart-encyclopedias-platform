package com.mohammedmustafa.almizan.bakery;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact integer arithmetic for bakery production and daily reconciliation. */
public final class BakeryMath {
    private BakeryMath() {}
    public static long productionLoaves(long workUnits, long loavesPerUnit) {
        if (workUnits < 0 || loavesPerUnit < 0) throw new IllegalArgumentException("Negative quantity");
        return Math.multiplyExact(workUnits, loavesPerUnit);
    }
    public static long valueForLoaves(long loaves, long loavesPerThousand) {
        if (loaves < 0) throw new IllegalArgumentException("Negative loaf count");
        if (loavesPerThousand <= 0) throw new IllegalArgumentException("Loaves per 1,000 SDG must be positive");
        return BigDecimal.valueOf(loaves).multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(loavesPerThousand), 0, RoundingMode.HALF_UP).longValueExact();
    }
    public static long sum(long... values) {
        long result=0; for(long value:values) result=Math.addExact(result,value); return result;
    }
    public static long accountedTotal(long cash, long debt, long bank, long priceDifference, long wages, long expenses, long spoilageValue) {
        return sum(cash, debt, bank, priceDifference, wages, expenses, spoilageValue);
    }
    public static long difference(long expectedProduction, long recordedCategories) {
        return Math.subtractExact(expectedProduction, recordedCategories);
    }
    /** A daily reconciliation is acceptable only with configured pricing, valid rows, and zero difference. */
    public static boolean isReconciliationValid(long difference, int invalidRows, boolean priceConfigured) {
        if (invalidRows < 0) throw new IllegalArgumentException("Negative invalid-row count");
        return priceConfigured && invalidRows == 0 && difference == 0;
    }
    /** Daily ingredient/fuel cost. Quantities and unit prices must be non-negative. */
    public static long operatingCost(long fuelQty,long fuelPrice,long flourBags,long flourBagPrice,
            long yeastCount,long yeastPrice,long oilQty,long oilPoundPrice,
            long saltQty,long saltPrice,long bagsTotalPrice) {
        long[] values={fuelQty,fuelPrice,flourBags,flourBagPrice,yeastCount,yeastPrice,
                oilQty,oilPoundPrice,saltQty,saltPrice,bagsTotalPrice};
        for(long value:values) if(value<0) throw new IllegalArgumentException("Negative cost input");
        return sum(Math.multiplyExact(fuelQty,fuelPrice),Math.multiplyExact(flourBags,flourBagPrice),
                Math.multiplyExact(yeastCount,yeastPrice),Math.multiplyExact(oilQty,oilPoundPrice),
                Math.multiplyExact(saltQty,saltPrice),bagsTotalPrice);
    }
    /** Reconciles stock after a change to the daily number of flour bags used. */
    public static long stockAfterUsage(long currentStock, long previouslyAppliedUsage, long newUsage) {
        if (currentStock < 0 || previouslyAppliedUsage < 0 || newUsage < 0)
            throw new IllegalArgumentException("Negative stock or usage");
        return Math.subtractExact(currentStock, Math.subtractExact(newUsage, previouslyAppliedUsage));
    }

    /** Sum operating rates per work unit and multiply by the day's work count. */
    public static long operatingCostPerWorkUnit(long workUnits, long... unitCosts) {
        if (workUnits < 0) throw new IllegalArgumentException("Negative work units");
        long perWorkUnit = 0L;
        for (long cost : unitCosts) {
            if (cost < 0) throw new IllegalArgumentException("Negative unit operating cost");
            perWorkUnit = Math.addExact(perWorkUnit, cost);
        }
        return Math.multiplyExact(perWorkUnit, workUnits);
    }

    /** Net daily margin after spoilage, wages, shop expenses, operating costs, and distribution price difference. */
    public static long netMargin(long salesRevenue,long spoilageCost,long wages,long expenses,long operatingCost,long priceDifference) {
        long[] values={salesRevenue,spoilageCost,wages,expenses,operatingCost,priceDifference};
        for(long value:values) if(value<0) throw new IllegalArgumentException("Negative amount");
        return Math.subtractExact(Math.subtractExact(Math.subtractExact(Math.subtractExact(Math.subtractExact(salesRevenue,spoilageCost),wages),expenses),operatingCost),priceDifference);
    }

    /** Compatibility overload for calculations where no separate price difference is recorded. */
    public static long netMargin(long productionValue,long spoilageValue,long wages,long expenses,long operatingCost) {
        return netMargin(productionValue,spoilageValue,wages,expenses,operatingCost,0L);
    }

    /** Estimated margin before any additional costs not included in the supplied cost list. */
    public static long grossMargin(long productionValue,long operatingCost) {
        if(productionValue<0 || operatingCost<0) throw new IllegalArgumentException("Negative amount");
        return Math.subtractExact(productionValue,operatingCost);
    }
}
