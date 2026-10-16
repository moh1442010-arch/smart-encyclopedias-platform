package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class JournalInvariantBridgeTest {
    @Test public void validatesPersistedSaleLinesAtCurrencyScale() {
        AccountingCore.JournalEntry entry = JournalInvariantBridge.validate("SALE-1", Arrays.asList(
                new JournalInvariantBridge.LegacyLine("1010", 100.0, 0),
                new JournalInvariantBridge.LegacyLine("1100", 50.0, 0),
                new JournalInvariantBridge.LegacyLine("4000", 0, 150.0),
                new JournalInvariantBridge.LegacyLine("5100", 60.0, 0),
                new JournalInvariantBridge.LegacyLine("1200", 0, 60.0),
                new JournalInvariantBridge.LegacyLine("1010", 0, 0)), 2);
        assertEquals(21000L, entry.totalDebitMinor());
        assertEquals(21000L, entry.totalCreditMinor());
    }

    @Test public void rejectsUnbalancedPersistedLines() {
        try {
            JournalInvariantBridge.validate("BAD-1", Arrays.asList(
                    new JournalInvariantBridge.LegacyLine("1010", 10.0, 0),
                    new JournalInvariantBridge.LegacyLine("4000", 0, 9.99)), 2);
            fail("Unbalanced entry must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("unbalanced"));
        }
    }

    @Test public void rejectsAmountsThatWouldRequireMoreThanHalfMinorUnitAdjustment() {
        try {
            JournalInvariantBridge.validate("ROUND-1", Arrays.asList(
                    new JournalInvariantBridge.LegacyLine("1010", 1.006, 0),
                    new JournalInvariantBridge.LegacyLine("4000", 0, 1.01)), 2);
            fail("Unexpected precision must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("precision"));
        }
    }

    @Test public void rejectsInvalidSideAndNegativeAmounts() {
        try {
            JournalInvariantBridge.validate("BAD-LINE", Arrays.asList(
                    new JournalInvariantBridge.LegacyLine("1010", 1, 1),
                    new JournalInvariantBridge.LegacyLine("4000", 0, 2)), 2);
            fail("A line cannot be both debit and credit");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("not both"));
        }
        try {
            JournalInvariantBridge.validate("NEGATIVE", Arrays.asList(
                    new JournalInvariantBridge.LegacyLine("1010", -1, 0),
                    new JournalInvariantBridge.LegacyLine("4000", 0, -1)), 2);
            fail("Negative amounts must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("non-negative"));
        }
    }
}
