package com.mohammedmustafa.smartcompany;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Platform-neutral accounting invariants using integer minor units.
 *
 * This validator is a foundation for Android/Windows parity. It is not yet
 * connected to the app's posting pipeline or legacy REAL-valued database.
 */
public final class AccountingCore {
    private AccountingCore() {}

    public static final class JournalLine {
        private final String accountCode;
        private final long debitMinor;
        private final long creditMinor;

        public JournalLine(String accountCode, long debitMinor, long creditMinor) {
            if (accountCode == null || accountCode.trim().isEmpty()) {
                throw new IllegalArgumentException("Account code is required");
            }
            if (debitMinor < 0 || creditMinor < 0) {
                throw new IllegalArgumentException("Debit and credit cannot be negative");
            }
            if ((debitMinor == 0) == (creditMinor == 0)) {
                throw new IllegalArgumentException("Each line must contain debit or credit, not both");
            }
            this.accountCode = accountCode.trim();
            this.debitMinor = debitMinor;
            this.creditMinor = creditMinor;
        }

        public String accountCode() { return accountCode; }
        public long debitMinor() { return debitMinor; }
        public long creditMinor() { return creditMinor; }
    }

    public static final class JournalEntry {
        private final String reference;
        private final List<JournalLine> lines;

        public JournalEntry(String reference, List<JournalLine> lines) {
            if (reference == null || reference.trim().isEmpty()) {
                throw new IllegalArgumentException("Journal reference is required");
            }
            if (lines == null || lines.size() < 2) {
                throw new IllegalArgumentException("A journal requires at least two lines");
            }
            List<JournalLine> copy = new ArrayList<>(lines.size());
            for (JournalLine line : lines) copy.add(Objects.requireNonNull(line, "line"));
            long debits = 0;
            long credits = 0;
            for (JournalLine line : copy) {
                debits = Math.addExact(debits, line.debitMinor());
                credits = Math.addExact(credits, line.creditMinor());
            }
            if (debits != credits) {
                throw new IllegalArgumentException("Journal is unbalanced: debit and credit totals differ");
            }
            this.reference = reference.trim();
            this.lines = Collections.unmodifiableList(copy);
        }

        public String reference() { return reference; }
        public List<JournalLine> lines() { return lines; }

        public long totalDebitMinor() {
            long total = 0;
            for (JournalLine line : lines) total = Math.addExact(total, line.debitMinor());
            return total;
        }

        public long totalCreditMinor() {
            long total = 0;
            for (JournalLine line : lines) total = Math.addExact(total, line.creditMinor());
            return total;
        }
    }
}
