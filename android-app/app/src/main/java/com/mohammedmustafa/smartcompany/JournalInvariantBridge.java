package com.mohammedmustafa.smartcompany;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Converts persisted legacy journal lines to exact minor units for invariant checking. */
public final class JournalInvariantBridge {
    private JournalInvariantBridge() {}

    public static final class LegacyLine {
        public final String accountCode;
        public final double debit;
        public final double credit;

        public LegacyLine(String accountCode, double debit, double credit) {
            this.accountCode = accountCode;
            this.debit = debit;
            this.credit = credit;
        }
    }

    public static AccountingCore.JournalEntry validate(String reference, List<LegacyLine> legacyLines, int scale) {
        if (scale < 0 || scale > 6) throw new IllegalArgumentException("Currency scale must be between 0 and 6");
        if (legacyLines == null) throw new IllegalArgumentException("Journal lines are required");
        List<AccountingCore.JournalLine> exactLines = new ArrayList<>();
        for (LegacyLine line : legacyLines) {
            if (line == null) throw new IllegalArgumentException("Journal line is required");
            if (!Double.isFinite(line.debit) || !Double.isFinite(line.credit) || line.debit < 0 || line.credit < 0) {
                throw new IllegalArgumentException("Journal line amounts must be finite and non-negative");
            }
            if (line.debit == 0d && line.credit == 0d) continue;
            long debit = toMinorUnits(line.debit, scale);
            long credit = toMinorUnits(line.credit, scale);
            exactLines.add(new AccountingCore.JournalLine(line.accountCode, debit, credit));
        }
        return new AccountingCore.JournalEntry(reference, exactLines);
    }

    private static long toMinorUnits(double value, int scale) {
        BigDecimal raw = BigDecimal.valueOf(value);
        BigDecimal rounded = raw.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal representationTolerance = new BigDecimal("0.000000001");
        if (raw.subtract(rounded).abs().compareTo(representationTolerance) > 0) {
            throw new IllegalArgumentException("Journal amount exceeds configured currency precision");
        }
        return rounded.movePointRight(scale).longValueExact();
    }
}
