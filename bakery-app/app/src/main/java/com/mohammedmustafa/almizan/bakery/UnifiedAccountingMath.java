package com.mohammedmustafa.almizan.bakery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Single approved revenue/cost calculation contract shared by bakery and factory reports.
 * All amounts are integer minor units in one reporting currency after source-rate conversion.
 */
public final class UnifiedAccountingMath {
    private UnifiedAccountingMath() {}

    public static final class Sales {
        public final long counterCash, counterBankak, counterReceivables;
        public final long distributionCash, distributionBankak, distributionReceivables;
        public final long salesReturns;

        public Sales(long counterCash, long counterBankak, long counterReceivables,
                     long distributionCash, long distributionBankak, long distributionReceivables,
                     long salesReturns) {
            long[] values = {counterCash,counterBankak,counterReceivables,
                distributionCash,distributionBankak,distributionReceivables,salesReturns};
            for (long value : values) requireNonNegative(value, "sales amount");
            this.counterCash=counterCash; this.counterBankak=counterBankak;
            this.counterReceivables=counterReceivables; this.distributionCash=distributionCash;
            this.distributionBankak=distributionBankak; this.distributionReceivables=distributionReceivables;
            this.salesReturns=salesReturns;
            if (salesReturns > grossRevenue(values)) throw new IllegalArgumentException("Returns exceed gross sales");
        }

        public long grossRevenue() {
            return add(counterCash,counterBankak,counterReceivables,
                distributionCash,distributionBankak,distributionReceivables);
        }
        public long netRevenue() { return checkedSubtract(grossRevenue(),salesReturns); }
        public long cashAndBankakCollections() {
            return add(counterCash,counterBankak,distributionCash,distributionBankak);
        }
        public long receivablesBeforeCollections() {
            return add(counterReceivables,distributionReceivables);
        }
        public long counterRevenue() {
            return add(counterCash,counterBankak,counterReceivables);
        }
        public long distributionRevenue() {
            return add(distributionCash,distributionBankak,distributionReceivables);
        }
        private static long grossRevenue(long[] values) {
            long result=0;
            for(int i=0;i<6;i++) result=checkedAdd(result,values[i]);
            return result;
        }
    }

    public static final class CostItem {
        public final String sourceKey;
        public final String category;
        public final long amount;
        public final boolean capitalAsset;

        public CostItem(String sourceKey, String category, long amount, boolean capitalAsset) {
            if (sourceKey==null || sourceKey.trim().isEmpty()) throw new IllegalArgumentException("Cost source key is required");
            if (category==null || category.trim().isEmpty()) throw new IllegalArgumentException("Cost category is required");
            requireNonNegative(amount,"cost amount");
            this.sourceKey=sourceKey.trim(); this.category=category.trim();
            this.amount=amount; this.capitalAsset=capitalAsset;
        }
    }

    /** Sum each consumed-cost source once; capital-asset acquisitions are excluded from operating expense. */
    public static long operatingCost(List<CostItem> items) {
        if(items==null) throw new IllegalArgumentException("Cost items are required");
        Set<String> seen=new HashSet<>();
        long total=0;
        for(CostItem item:items) {
            if(item==null) throw new IllegalArgumentException("Null cost item");
            if(!seen.add(item.sourceKey)) throw new IllegalArgumentException("Duplicate cost source: "+item.sourceKey);
            if(!item.capitalAsset) total=checkedAdd(total,item.amount);
        }
        return total;
    }

    /** Profit = counter and distribution revenue (cash + Bankak + receivables) less returns and operating costs. */
    public static long profit(Sales sales, List<CostItem> costs) {
        if(sales==null) throw new IllegalArgumentException("Sales breakdown is required");
        return checkedSubtract(sales.netRevenue(),operatingCost(costs));
    }

    private static long add(long... values) {
        long total=0;
        for(long value:values) total=checkedAdd(total,value);
        return total;
    }
    // Keep checked arithmetic compatible with Android API 23 (avoid Math.*Exact APIs).
    private static long checkedAdd(long a,long b) {
        if ((b>0 && a>Long.MAX_VALUE-b) || (b<0 && a<Long.MIN_VALUE-b))
            throw new ArithmeticException("long overflow");
        return a+b;
    }
    private static long checkedSubtract(long a,long b) {
        if ((b<0 && a>Long.MAX_VALUE+b) || (b>0 && a<Long.MIN_VALUE+b))
            throw new ArithmeticException("long overflow");
        return a-b;
    }
    private static void requireNonNegative(long value,String label) {
        if(value<0) throw new IllegalArgumentException("Negative "+label);
    }
}
