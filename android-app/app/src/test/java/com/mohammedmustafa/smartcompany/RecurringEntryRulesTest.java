package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import static org.junit.Assert.*;

public class RecurringEntryRulesTest {
    @Test public void periodAndOccurrenceKeysAreDeterministic() {
        assertEquals("2027-02", RecurringEntryRules.periodKey(2027, 2));
        assertEquals("42:2027-02", RecurringEntryRules.occurrenceKey(42, "2027-02"));
    }
    @Test public void dueDayClampsToLastDayOfShortMonthAndLeapFebruary() {
        assertEquals("2027-02-28", RecurringEntryRules.dueDate("2027-02", 31));
        assertEquals("2028-02-29", RecurringEntryRules.dueDate("2028-02", 31));
        assertEquals("2027-04-30", RecurringEntryRules.dueDate("2027-04", 31));
    }
    @Test public void autoPostingRequiresAllThreeSafeguards() {
        assertTrue(RecurringEntryRules.mayAutoPost(true, true, true));
        assertFalse(RecurringEntryRules.mayAutoPost(false, true, true));
        assertFalse(RecurringEntryRules.mayAutoPost(true, false, true));
        assertFalse(RecurringEntryRules.mayAutoPost(true, true, false));
    }
    @Test public void variableBillsRequireConfirmedPositiveFiniteAmount() {
        assertTrue(RecurringEntryRules.mayPostVariableAmount(125.0));
        assertFalse(RecurringEntryRules.mayPostVariableAmount(null));
        assertFalse(RecurringEntryRules.mayPostVariableAmount(0.0));
        assertFalse(RecurringEntryRules.mayPostVariableAmount(Double.NaN));
        assertFalse(RecurringEntryRules.mayPostVariableAmount(Double.POSITIVE_INFINITY));
    }
}