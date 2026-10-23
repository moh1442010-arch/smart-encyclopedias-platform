package com.mohammedmustafa.smartcompany;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Exact integer/minor-unit calculations for the bakery daily ledger. */
public final class BakeryLedgerMath {
    private BakeryLedgerMath() {}

    public static long productionLoaves(long cuts, long loavesPerCut) {
        if (cuts < 0 || loavesPerCut < 0) throw new IllegalArgumentException("Negative production quantity");
        return Math.multiplyExact(cuts, loavesPerCut);
    }

    /** Value in SDG where loavesPerThousand loaves are priced at 1,000 SDG. */
    public static long valueForLoaves(long loaves, long loavesPerThousand) {
        if (loaves < 0) throw new IllegalArgumentException("Negative loaf count");
        if (loavesPerThousand <= 0) throw new IllegalArgumentException("Loaves per 1,000 SDG must be positive");
        return BigDecimal.valueOf(loaves).multiply(BigDecimal.valueOf(1000))
                .divide(BigDecimal.valueOf(loavesPerThousand), 0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public static long sum(long... values) {
        long result = 0;
        for (long value : values) result = Math.addExact(result, value);
        return result;
    }

    public static long reconciliationDifference(long productionValue, long categorizedValue) {
        return Math.subtractExact(productionValue, categorizedValue);
    }

    /** Remaining flour sacks for the day; never mutates the opening balance. */
    public static long remainingFlourBags(long openingBags, long usedBags) {
        if (openingBags < 0 || usedBags < 0) throw new IllegalArgumentException("Negative flour quantity");
        if (usedBags > openingBags) throw new IllegalArgumentException("Flour usage exceeds available stock");
        return Math.subtractExact(openingBags, usedBags);
    }

    /** Gross revenue less spoilage, wages, operating expenses and direct flour cost. */
    public static long profitMargin(long salesRevenue, long spoilageValue, long wages,
                                    long workExpenses, long shopExpenses, long flourCost) {
        if (salesRevenue < 0 || spoilageValue < 0 || wages < 0 || workExpenses < 0 ||
                shopExpenses < 0 || flourCost < 0) {
            throw new IllegalArgumentException("Negative cost or revenue");
        }
        return Math.subtractExact(salesRevenue,
                sum(spoilageValue, wages, workExpenses, shopExpenses, flourCost));
    }

    public static long flourCost(long bagsUsed, long costPerBag) {
        if (bagsUsed < 0 || costPerBag < 0) throw new IllegalArgumentException("Negative flour cost input");
        return Math.multiplyExact(bagsUsed, costPerBag);
    }
}