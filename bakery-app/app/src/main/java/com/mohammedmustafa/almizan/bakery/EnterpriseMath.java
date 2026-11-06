package com.mohammedmustafa.almizan.bakery;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/** Exact financial primitives for future enterprise workflows. Amounts are integer minor units. */
final class EnterpriseMath {
    private EnterpriseMath() {}

    static long convertMinorUnits(long amountMinor, long rateNumerator, long rateDenominator,
                                  int sourceMinorUnits, int targetMinorUnits) {
        if (amountMinor < 0 || rateNumerator <= 0 || rateDenominator <= 0 ||
            sourceMinorUnits < 0 || sourceMinorUnits > 4 ||
            targetMinorUnits < 0 || targetMinorUnits > 4) {
            throw new IllegalArgumentException("Invalid currency conversion inputs");
        }
        BigInteger numerator = BigInteger.valueOf(amountMinor)
            .multiply(BigInteger.valueOf(rateNumerator))
            .multiply(BigInteger.TEN.pow(targetMinorUnits));
        BigInteger denominator = BigInteger.valueOf(rateDenominator)
            .multiply(BigInteger.TEN.pow(sourceMinorUnits));
        BigInteger rounded = new BigDecimal(numerator)
            .divide(new BigDecimal(denominator), 0, RoundingMode.HALF_UP)
            .toBigIntegerExact();
        return rounded.longValueExact();
    }

    static long payrollNet(long basicMinor, long additionsMinor, long deductionsMinor) {
        if (basicMinor < 0 || additionsMinor < 0 || deductionsMinor < 0) {
            throw new IllegalArgumentException("Payroll values cannot be negative");
        }
        long gross = Math.addExact(basicMinor, additionsMinor);
        long net = Math.subtractExact(gross, deductionsMinor);
        if (net < 0) throw new IllegalArgumentException("Deductions exceed gross pay");
        return net;
    }

    static long cumulativeStraightLineDepreciation(long costMinor, long residualMinor,
                                                   int usefulLifeMonths, int elapsedMonths) {
        if (costMinor <= 0 || residualMinor < 0 || residualMinor > costMinor ||
            usefulLifeMonths <= 0 || elapsedMonths < 0) {
            throw new IllegalArgumentException("Invalid asset depreciation inputs");
        }
        long depreciable = costMinor - residualMinor;
        int periods = Math.min(elapsedMonths, usefulLifeMonths);
        BigInteger numerator = BigInteger.valueOf(depreciable).multiply(BigInteger.valueOf(periods));
        BigInteger rounded = new BigDecimal(numerator)
            .divide(BigDecimal.valueOf(usefulLifeMonths), 0, RoundingMode.HALF_UP)
            .toBigIntegerExact();
        return Math.min(depreciable, rounded.longValueExact());
    }

    static long depreciationForPeriod(long costMinor, long residualMinor, int usefulLifeMonths,
                                      int elapsedMonths) {
        if (elapsedMonths <= 0) throw new IllegalArgumentException("Elapsed month must be positive");
        long current = cumulativeStraightLineDepreciation(costMinor, residualMinor, usefulLifeMonths, elapsedMonths);
        long previous = cumulativeStraightLineDepreciation(costMinor, residualMinor, usefulLifeMonths, elapsedMonths - 1);
        return Math.subtractExact(current, previous);
    }

    static long remainingBookValue(long costMinor, long residualMinor, int usefulLifeMonths,
                                   int elapsedMonths) {
        long accumulated = cumulativeStraightLineDepreciation(costMinor, residualMinor, usefulLifeMonths, elapsedMonths);
        return Math.subtractExact(costMinor, accumulated);
    }
}
