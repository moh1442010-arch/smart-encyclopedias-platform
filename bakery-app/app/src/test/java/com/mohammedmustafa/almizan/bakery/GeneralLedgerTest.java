package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class GeneralLedgerTest {
    @Test public void acceptsValidLeapDay() {
        assertTrue(GeneralLedger.validDate("2024-02-29"));
    }

    @Test public void rejectsImpossibleCalendarDate() {
        assertFalse(GeneralLedger.validDate("2026-02-30"));
        assertFalse(GeneralLedger.validDate("2026-13-01"));
        assertFalse(GeneralLedger.validDate("2026-04-31"));
    }

    @Test public void rejectsNonCanonicalJournalDate() {
        assertFalse(GeneralLedger.validDate("2026-2-03"));
        assertFalse(GeneralLedger.validDate("03-02-2026"));
        assertFalse(GeneralLedger.validDate(""));
    }

    @Test public void acceptsBalancedMultiLineJournal() {
        GeneralLedger.validateBalancedTotals(125000L,125000L,3);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsUnbalancedJournal() {
        GeneralLedger.validateBalancedTotals(125000L,124999L,2);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsSingleLineJournal() {
        GeneralLedger.validateBalancedTotals(100L,100L,1);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroSidedJournal() {
        GeneralLedger.validateBalancedTotals(0L,0L,2);
    }
    @Test public void convertsForeignCurrencyIntoBaseMinorUnits() {
        assertEquals(18518L,GeneralLedger.toBaseMinor(12345L,3L,2L,2,2));
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsZeroAmountJournalLine() {
        GeneralLedger.toBaseMinor(0L,1L,1L,2,2);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsInvalidRate() {
        GeneralLedger.toBaseMinor(100L,1L,0L,2,2);
    }
    private GeneralLedger.Entry sampleEntry(long cashDebit,long revenueCredit,String sourceId) {
        List<GeneralLedger.Line> lines=new ArrayList<>();
        lines.add(new GeneralLedger.Line("CASH","MAIN","SDG",cashDebit,0,1,1));
        lines.add(new GeneralLedger.Line("SALES","MAIN","SDG",0,revenueCredit,1,1));
        return new GeneralLedger.Entry("SALE-100","Cash sale","2026-11-08","SALE",sourceId,
            "sale:100","tester",lines);
    }

    @Test public void idempotentPayloadAcceptsSameLinesInDifferentOrder() {
        GeneralLedger.Entry a=sampleEntry(10000,10000,"100");
        List<GeneralLedger.Line> reversed=new ArrayList<>();
        reversed.add(a.lines.get(1));reversed.add(a.lines.get(0));
        GeneralLedger.Entry b=new GeneralLedger.Entry(a.reference,a.description,a.date,a.sourceType,a.sourceId,
            a.idempotencyKey,a.actor,reversed);
        assertTrue(GeneralLedger.samePostingPayload(a,b));
    }

    @Test public void idempotencyRejectsChangedAmount() {
        assertFalse(GeneralLedger.samePostingPayload(sampleEntry(10000,10000,"100"),
            sampleEntry(12000,12000,"100")));
    }

    @Test public void idempotencyRejectsChangedSourceIdentity() {
        assertFalse(GeneralLedger.samePostingPayload(sampleEntry(10000,10000,"100"),
            sampleEntry(10000,10000,"101")));
    }

    @Test public void idempotencyRejectsChangedAccountOrExchangeRate() {
        GeneralLedger.Entry a=sampleEntry(10000,10000,"100");
        List<GeneralLedger.Line> changed=new ArrayList<>();
        changed.add(new GeneralLedger.Line("BANK","MAIN","SDG",10000,0,1,1));
        changed.add(a.lines.get(1));
        GeneralLedger.Entry b=new GeneralLedger.Entry(a.reference,a.description,a.date,a.sourceType,a.sourceId,
            a.idempotencyKey,a.actor,changed);
        assertFalse(GeneralLedger.samePostingPayload(a,b));
    }

    @Test public void rejectsNegativeJournalAmounts() {
        try {
            GeneralLedger.validateLine(new GeneralLedger.Line("CASH","MAIN","SDG",-1,0,1,1));
            fail("negative debit must be rejected");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsDebitAndCreditOnSameLine() {
        try {
            GeneralLedger.validateLine(new GeneralLedger.Line("CASH","MAIN","SDG",1,1,1,1));
            fail("a journal line cannot be both debit and credit");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsEmptyJournalLine() {
        try {
            GeneralLedger.validateLine(new GeneralLedger.Line("CASH","MAIN","SDG",0,0,1,1));
            fail("a journal line must have one positive side");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsOverflowDuringCurrencyConversion() {
        try {
            GeneralLedger.toBaseMinor(Long.MAX_VALUE,Long.MAX_VALUE,1,4,4);
            fail("overflow must not wrap into a valid ledger amount");
        } catch (ArithmeticException expected) { }
    }

    @Test public void rejectsNonPositiveBalancedTotals() {
        try {
            GeneralLedger.validateBalancedTotals(-10,-10,2);
            fail("negative totals must be rejected");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsNullAndIncompleteJournalLines() {
        try {
            GeneralLedger.validateLine(null);
            fail("null journal line must be rejected");
        } catch (IllegalArgumentException expected) { }
        try {
            GeneralLedger.validateLine(new GeneralLedger.Line("", "MAIN", "SDG", 1, 0, 1, 1));
            fail("blank account code must be rejected");
        } catch (IllegalArgumentException expected) { }
        try {
            GeneralLedger.validateLine(new GeneralLedger.Line("CASH", "", "SDG", 1, 0, 1, 1));
            fail("blank branch code must be rejected");
        } catch (IllegalArgumentException expected) { }
        try {
            GeneralLedger.validateLine(new GeneralLedger.Line("CASH", "MAIN", "", 1, 0, 1, 1));
            fail("blank currency code must be rejected");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void rejectsCreditDebitAndExchangeRateBoundaryViolations() {
        GeneralLedger.Line[] invalid = new GeneralLedger.Line[] {
            new GeneralLedger.Line("CASH","MAIN","SDG",0,-1,1,1),
            new GeneralLedger.Line("CASH","MAIN","SDG",1,0,0,1),
            new GeneralLedger.Line("CASH","MAIN","SDG",1,0,1,0),
            new GeneralLedger.Line("CASH","MAIN","SDG",1,0,-1,1),
            new GeneralLedger.Line("CASH","MAIN","SDG",1,0,1,-1)
        };
        for (GeneralLedger.Line line : invalid) {
            try {
                GeneralLedger.validateLine(line);
                fail("invalid amount or FX rate must be rejected");
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void rejectsUnbalancedTotalsAndInsufficientLines() {
        long[][] invalid = new long[][] {{100,99,2},{100,100,0},{100,100,1},{0,100,2},{100,0,2}};
        for (long[] values : invalid) {
            try {
                GeneralLedger.validateBalancedTotals(values[0],values[1],(int)values[2]);
                fail("invalid journal totals must be rejected");
            } catch (IllegalArgumentException expected) { }
        }
    }

}
