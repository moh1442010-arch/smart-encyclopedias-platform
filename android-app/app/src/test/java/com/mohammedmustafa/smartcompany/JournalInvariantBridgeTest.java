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

    @Test public void validatesCreditPurchaseAndSupplierPaymentJournals() {
        AccountingCore.JournalEntry purchase = JournalInvariantBridge.validate("PURCHASE-1", Arrays.asList(
                new JournalInvariantBridge.LegacyLine("1200", 80.0, 0),
                new JournalInvariantBridge.LegacyLine("2100", 0, 80.0)), 2);
        assertEquals(8000L, purchase.totalDebitMinor());
        assertEquals(purchase.totalDebitMinor(), purchase.totalCreditMinor());

        AccountingCore.JournalEntry supplierPayment = JournalInvariantBridge.validate("SUPPLIER-PAY-1", Arrays.asList(
                new JournalInvariantBridge.LegacyLine("2100", 30.0, 0),
                new JournalInvariantBridge.LegacyLine("1010", 0, 30.0)), 2);
        assertEquals(3000L, supplierPayment.totalDebitMinor());
        assertEquals(supplierPayment.totalDebitMinor(), supplierPayment.totalCreditMinor());
    }

    @Test public void validatesCustomerReceiptAndSalesReturnJournals() {
        AccountingCore.JournalEntry receipt = JournalInvariantBridge.validate("RECEIPT-1", Arrays.asList(
                new JournalInvariantBridge.LegacyLine("1010", 25.0, 0),
                new JournalInvariantBridge.LegacyLine("1100", 0, 25.0)), 2);
        assertEquals(2500L, receipt.totalDebitMinor());
        assertEquals(receipt.totalDebitMinor(), receipt.totalCreditMinor());

        AccountingCore.JournalEntry returnEntry = JournalInvariantBridge.validate("RETURN-1", Arrays.asList(
                new JournalInvariantBridge.LegacyLine("4100", 10.0, 0),
                new JournalInvariantBridge.LegacyLine("1010", 0, 10.0)), 2);
        assertEquals(1000L, returnEntry.totalDebitMinor());
        assertEquals(returnEntry.totalDebitMinor(), returnEntry.totalCreditMinor());
    }

    @Test public void acceptsTinyBinaryFloatingPointNoiseButRejectsCurrencyRounding() {
        AccountingCore.JournalEntry entry = JournalInvariantBridge.validate("FLOAT-NOISE", Arrays.asList(
                new JournalInvariantBridge.LegacyLine("1010", 0.30000000000000004, 0),
                new JournalInvariantBridge.LegacyLine("4000", 0, 0.30)), 2);
        assertEquals(30L, entry.totalDebitMinor());
        try {
            JournalInvariantBridge.validate("ROUND-1", Arrays.asList(
                    new JournalInvariantBridge.LegacyLine("1010", 1.005, 0),
                    new JournalInvariantBridge.LegacyLine("4000", 0, 1.01)), 2);
            fail("Currency rounding must be explicit at the transaction boundary");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("precision"));
        }
    }
}
