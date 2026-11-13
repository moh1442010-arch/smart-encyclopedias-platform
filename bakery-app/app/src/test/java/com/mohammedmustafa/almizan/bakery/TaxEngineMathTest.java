package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class TaxEngineMathTest {
    @Test(expected=IllegalArgumentException.class)
    public void rejectsImpossibleTaxEffectiveDatesBeforeDatabaseAccess() {
        TaxEngine.calculate(null,"SD","VAT_STANDARD","2026-02-30",100L,false,false);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsBlankTaxJurisdictionInputsBeforeDatabaseAccess() {
        TaxEngine.calculate(null," ","VAT_STANDARD","2026-11-13",100L,false,false);
    }
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
}
