package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import static org.junit.Assert.*;

public class EnterpriseMathTest {
    @Test public void convertsCurrenciesUsingRationalRatesAndMinorUnits() {
        assertEquals(18518L, EnterpriseMath.convertMinorUnits(12345L, 3L, 2L, 2, 2));
        assertEquals(150L, EnterpriseMath.convertMinorUnits(100L, 3L, 2L, 0, 0));
        assertEquals(12345L, EnterpriseMath.convertMinorUnits(12345L, 1L, 1L, 2, 2));
    }

    @Test public void wholePoundSdgConvertsWithoutInventingTwoDecimalPlaces() {
        assertEquals(55000L, EnterpriseMath.convertMinorUnits(55000L, 1L, 1L, 0, 0));
        assertEquals(5500000L, EnterpriseMath.convertMinorUnits(55000L, 1L, 1L, 0, 2));
    }

    @Test public void roundsHalfUpWhenConverting() {
        assertEquals(2L, EnterpriseMath.convertMinorUnits(1L, 3L, 2L, 0, 0));
        assertEquals(1L, EnterpriseMath.convertMinorUnits(1L, 1L, 2L, 0, 0));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidExchangeRate() {
        EnterpriseMath.convertMinorUnits(100L, 0L, 1L, 2, 2);
    }

    @Test public void calculatesPayrollNetExactly() {
        assertEquals(112500L, EnterpriseMath.payrollNet(100000L, 25000L, 12500L));
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsPayrollDeductionsAboveGross() {
        EnterpriseMath.payrollNet(10000L, 0L, 10001L);
    }

    @Test(expected=ArithmeticException.class)
    public void detectsPayrollOverflow() {
        EnterpriseMath.payrollNet(Long.MAX_VALUE, 1L, 0L);
    }

    @Test public void straightLineDepreciationNeverFallsBelowResidualValue() {
        assertEquals(2500L, EnterpriseMath.depreciationForPeriod(100000L, 10000L, 36, 1));
        assertEquals(90000L, EnterpriseMath.cumulativeStraightLineDepreciation(100000L, 10000L, 36, 36));
        assertEquals(90000L, EnterpriseMath.cumulativeStraightLineDepreciation(100000L, 10000L, 36, 48));
        assertEquals(10000L, EnterpriseMath.remainingBookValue(100000L, 10000L, 36, 48));
        assertEquals(0L, EnterpriseMath.depreciationForPeriod(100000L, 10000L, 36, 37));
    }

    @Test public void monthlyDepreciationRoundsWithoutLosingTotalCost() {
        long sum = 0L;
        for (int month = 1; month <= 7; month++) {
            sum = Math.addExact(sum, EnterpriseMath.depreciationForPeriod(100L, 0L, 7, month));
        }
        assertEquals(100L, sum);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsAssetResidualAboveCost() {
        EnterpriseMath.cumulativeStraightLineDepreciation(100L, 101L, 12, 1);
    }
}
