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
}
