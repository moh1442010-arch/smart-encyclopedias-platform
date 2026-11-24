package com.mohammedmustafa.tirbal;

import org.junit.Test;
import static org.junit.Assert.*;

public class SettlementMathTest {
    @Test public void calculatesTheAgreedExampleExactly() {
        SettlementMath.Share share = SettlementMath.calculateWorkerShare(50_000_000L, 5_000_000L, 2_000_000L);
        assertEquals(45_000_000L, share.distributable);
        assertEquals(22_500_000L, share.halfShare);
        assertEquals(20_500_000L, share.netDue);
    }

    @Test public void subtractsSeveralInvoicesAsOneNetSalesTotal() {
        long first = SettlementMath.netSale(30_000_000L, 2_000_000L, 500_000L);
        long second = SettlementMath.netSale(25_000_000L, 1_500_000L, 500_000L);
        SettlementMath.Share share = SettlementMath.calculateWorkerShare(first + second, 5_000_000L, 2_000_000L);
        assertEquals(51_000_000L, first + second);
        assertEquals(23_000_000L, share.halfShare);
        assertEquals(21_000_000L, share.netDue);
    }

    @Test public void handlesOddHalvesWithoutCreatingMoney() {
        SettlementMath.Share share = SettlementMath.calculateWorkerShare(101L, 0L, 0L);
        assertEquals(50L, share.halfShare);
    }

    @Test public void permitsNegativeSettlementWhenDeductionsExceedShare() {
        SettlementMath.Share share = SettlementMath.calculateWorkerShare(1_000L, 200L, 600L);
        assertEquals(-200L, share.netDue);
    }

    @Test public void allowsSaleNetToBeNegativeWhenCostsExceedGross() {
        assertEquals(-50L, SettlementMath.netSale(100L, 100L, 50L));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeInvoiceAmount() {
        SettlementMath.netSale(-1L, 0L, 0L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeWorkerDeduction() {
        SettlementMath.calculateWorkerShare(100L, 0L, -1L);
    }

    @Test(expected = ArithmeticException.class)
    public void detectsLongOverflow() {
        SettlementMath.calculateWorkerShare(Long.MAX_VALUE, 0L, 0L);
    }
}
