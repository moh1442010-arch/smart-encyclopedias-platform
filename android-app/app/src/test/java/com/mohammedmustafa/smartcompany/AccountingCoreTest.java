package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class AccountingCoreTest {
    @Test public void acceptsBalancedEntryUsingExactMinorUnits() {
        AccountingCore.JournalEntry entry = new AccountingCore.JournalEntry("SALE-001", Arrays.asList(
                new AccountingCore.JournalLine("1010", 1250, 0),
                new AccountingCore.JournalLine("4000", 0, 1250)));
        assertEquals(1250L, entry.totalDebitMinor());
        assertEquals(entry.totalDebitMinor(), entry.totalCreditMinor());
    }

    @Test public void rejectsUnbalancedEntry() {
        try {
            new AccountingCore.JournalEntry("SALE-002", Arrays.asList(
                    new AccountingCore.JournalLine("1010", 1251, 0),
                    new AccountingCore.JournalLine("4000", 0, 1250)));
            fail("Unbalanced journals must not be accepted");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unbalanced"));
        }
    }

    @Test public void rejectsInvalidLineSidesAndNegativeAmounts() {
        assertInvalidLine("", 1, 0);
        assertInvalidLine("1010", -1, 0);
        assertInvalidLine("1010", 1, 1);
        assertInvalidLine("1010", 0, 0);
    }

    @Test public void detectsTotalOverflow() {
        try {
            new AccountingCore.JournalEntry("OVERFLOW", Arrays.asList(
                    new AccountingCore.JournalLine("1010", Long.MAX_VALUE, 0),
                    new AccountingCore.JournalLine("1020", 1, 0)));
            fail("Overflow must not wrap");
        } catch (ArithmeticException expected) {
            // Exact arithmetic is required for ledger invariants.
        }
    }

    private void assertInvalidLine(String account, long debit, long credit) {
        try {
            new AccountingCore.JournalLine(account, debit, credit);
            fail("Invalid journal line accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
