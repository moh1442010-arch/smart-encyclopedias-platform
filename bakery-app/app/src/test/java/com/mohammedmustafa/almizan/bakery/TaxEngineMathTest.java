package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class TaxEngineMathTest {
    @Test public void roundsStandardVatWithoutFloatingPointDrift() {
        assertEquals(1700L,TaxEngine.roundedRatio(10000L,17L,100L));
        assertEquals(750L,TaxEngine.roundedRatio(10000L,15L,200L));
    }

    @Test public void splitsTaxInclusiveAmountExactly() {
        long gross=10500L;
        long net=TaxEngine.roundedRatio(gross,100L,105L);
        assertEquals(10000L,net);
        assertEquals(500L,gross-net);
    }

    @Test public void handlesVeryLargeAmountsWithoutIntermediateOverflow() {
        assertEquals(1700000000000L,TaxEngine.roundedRatio(10000000000000L,17L,100L));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsNegativeAmounts() {
        TaxEngine.roundedRatio(-1L,17L,100L);
    }

    @Test public void noVatRequiresAnExplicitlyConfirmedJurisdiction() {
        assertTrue(TaxEngine.isConfirmedNoVat("NO_VAT_CONFIRMED","NO_VAT"));
        assertFalse(TaxEngine.isConfirmedNoVat("REVIEW_REQUIRED","NO_VAT"));
        assertFalse(TaxEngine.isConfirmedNoVat("LOCAL_RULES_REQUIRED","NO_VAT"));
        assertFalse(TaxEngine.isConfirmedNoVat("NO_VAT_CONFIRMED","VAT"));
    }

    @Test public void inclusiveRateDenominatorUsesLongArithmetic() {
        long denominator=(long)Integer.MAX_VALUE+(long)Integer.MAX_VALUE;
        assertEquals(4294967294L,denominator);
        assertEquals((long)Integer.MAX_VALUE,TaxEngine.roundedRatio(4294967294L,Integer.MAX_VALUE,denominator));
    }
}
