package com.mohammedmustafa.almizan.bakery;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact integer arithmetic for bakery production and daily reconciliation. */
public final class BakeryMath {
    private BakeryMath() {}
    // Checked arithmetic implemented locally for Android API 23 compatibility.
    private static long checkedAdd(long a,long b) {
        if((b>0&&a>Long.MAX_VALUE-b)||(b<0&&a<Long.MIN_VALUE-b))throw new ArithmeticException("long overflow");
        return a+b;
    }
    private static long checkedSubtract(long a,long b) {
        if((b<0&&a>Long.MAX_VALUE+b)||(b>0&&a<Long.MIN_VALUE+b))throw new ArithmeticException("long overflow");
        return a-b;
    }
    private static long checkedMultiply(long a,long b) {
        if(a==0||b==0)return 0;
        if((a==Long.MIN_VALUE&&b==-1)||(b==Long.MIN_VALUE&&a==-1))throw new ArithmeticException("long overflow");
        long result=a*b;
        if(result/b!=a)throw new ArithmeticException("long overflow");
        return result;
    }
    public static long productionLoaves(long workUnits, long loavesPerUnit) {
        if (workUnits < 0 || loavesPerUnit < 0) throw new IllegalArgumentException("Negative quantity");
        return checkedMultiply(workUnits, loavesPerUnit);
    }
    public static long valueForLoaves(long loaves, long loavesPerThousand) {
        if (loaves < 0) throw new IllegalArgumentException("Negative loaf count");
        if (loavesPerThousand <= 0) throw new IllegalArgumentException("Loaves per 1,000 SDG must be positive");
        return BigDecimal.valueOf(loaves).multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(loavesPerThousand), 0, RoundingMode.HALF_UP).longValueExact();
    }
    public static long sum(long... values) {
        long result=0; for(long value:values) result=checkedAdd(result,value); return result;
    }
    public static long accountedTotal(long cash, long debt, long bank, long priceDifference, long wages, long expenses, long spoilageValue) {
        return sum(cash, debt, bank, priceDifference, wages, expenses, spoilageValue);
    }
    public static long difference(long expectedProduction, long recordedCategories) {
        return checkedSubtract(expectedProduction, recordedCategories);
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
        return sum(checkedMultiply(fuelQty,fuelPrice),checkedMultiply(flourBags,flourBagPrice),
                checkedMultiply(yeastCount,yeastPrice),checkedMultiply(oilQty,oilPoundPrice),
                checkedMultiply(saltQty,saltPrice),bagsTotalPrice);
    }
    /** Reconciles stock after a change to the daily number of flour bags used. */
    public static long stockAfterUsage(long currentStock, long previouslyAppliedUsage, long newUsage) {
        if (currentStock < 0 || previouslyAppliedUsage < 0 || newUsage < 0)
            throw new IllegalArgumentException("Negative stock or usage");
        return checkedSubtract(currentStock, checkedSubtract(newUsage, previouslyAppliedUsage));
    }

    /** Sum operating rates per work unit and multiply by the day's work count. */
    public static long operatingCostPerWorkUnit(long workUnits, long... unitCosts) {
        if (workUnits < 0) throw new IllegalArgumentException("Negative work units");
        long perWorkUnit = 0L;
        for (long cost : unitCosts) {
            if (cost < 0) throw new IllegalArgumentException("Negative unit operating cost");
            perWorkUnit = checkedAdd(perWorkUnit, cost);
        }
        return checkedMultiply(perWorkUnit, workUnits);
    }

    /** Net daily margin after spoilage, wages, shop expenses, operating costs, and distribution price difference. */
    public static long netMargin(long salesRevenue,long spoilageCost,long wages,long expenses,long operatingCost,long priceDifference) {
        long[] values={salesRevenue,spoilageCost,wages,expenses,operatingCost,priceDifference};
        for(long value:values) if(value<0) throw new IllegalArgumentException("Negative amount");
        return checkedSubtract(checkedSubtract(checkedSubtract(checkedSubtract(checkedSubtract(salesRevenue,spoilageCost),wages),expenses),operatingCost),priceDifference);
    }

    /** Approved bakery margin: total cash, bank transfers and debts less daily consumed operating cost. */
    public static long salesLessOperatingCost(long totalSalesAndReceivables,long dailyOperatingCost) {
        if(totalSalesAndReceivables<0 || dailyOperatingCost<0) throw new IllegalArgumentException("Negative amount");
        return checkedSubtract(totalSalesAndReceivables,dailyOperatingCost);
    }

    /** Compatibility overload for calculations where no separate price difference is recorded. */
    public static long netMargin(long productionValue,long spoilageValue,long wages,long expenses,long operatingCost) {
        return netMargin(productionValue,spoilageValue,wages,expenses,operatingCost,0L);
    }

    /** Estimated margin before any additional costs not included in the supplied cost list. */
    public static long grossMargin(long productionValue,long operatingCost) {
        if(productionValue<0 || operatingCost<0) throw new IllegalArgumentException("Negative amount");
        return checkedSubtract(productionValue,operatingCost);
    }
}
