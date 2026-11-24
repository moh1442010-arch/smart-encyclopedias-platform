package com.mohammedmustafa.tirbal;

/** Pure accounting calculations shared by data entry and settlement screens. */
final class SettlementMath {
    private SettlementMath() {}

    static final class Share {
        final long distributable;
        final long halfShare;
        final long workerDeductions;
        final long netDue;

        Share(long distributable, long halfShare, long workerDeductions, long netDue) {
            this.distributable = distributable;
            this.halfShare = halfShare;
            this.workerDeductions = workerDeductions;
            this.netDue = netDue;
        }
    }

    static long netSale(long gross, long allocatedCost, long marketExpenses) {
        if (gross < 0 || allocatedCost < 0 || marketExpenses < 0) {
            throw new IllegalArgumentException("قيمة الفاتورة والتكاليف لا يمكن أن تكون سالبة");
        }
        return Math.subtractExact(Math.subtractExact(gross, allocatedCost), marketExpenses);
    }

    static Share calculateWorkerShare(long netSales, long sharedCosts, long workerDeductions) {
        if (sharedCosts < 0 || workerDeductions < 0) {
            throw new IllegalArgumentException("التكاليف والخصومات لا يمكن أن تكون سالبة");
        }
        long distributable = Math.subtractExact(netSales, sharedCosts);
        long half = distributable / 2L;
        long due = Math.subtractExact(half, workerDeductions);
        return new Share(distributable, half, workerDeductions, due);
    }
}
