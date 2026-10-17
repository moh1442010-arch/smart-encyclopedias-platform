package com.mohammedmustafa.smartcompany;

import org.junit.Test;
import static org.junit.Assert.*;

public class EditionAccessPolicyTest {
    @Test public void basicKeepsCoreDailyOperationsButNotProModules() {
        assertTrue(EditionAccessPolicy.canAccess(1, "نقطة البيع والمبيعات"));
        assertTrue(EditionAccessPolicy.canAccess(1, "المشتريات"));
        assertTrue(EditionAccessPolicy.canAccess(1, "الأصناف والمخزون"));
        assertTrue(EditionAccessPolicy.canAccess(1, "العملاء والموردون"));
        assertFalse(EditionAccessPolicy.canAccess(1, "المرتجعات"));
        assertFalse(EditionAccessPolicy.canAccess(1, "الحسابات البنكية"));
        assertFalse(EditionAccessPolicy.canAccess(1, "التقارير والتحليل"));
        assertFalse(EditionAccessPolicy.canAccess(1, "الموظفون"));
    }
    @Test public void proAddsAccountingAndBankingButNotBusinessPayroll() {
        assertTrue(EditionAccessPolicy.canAccess(2, "المرتجعات"));
        assertTrue(EditionAccessPolicy.canAccess(2, "الحسابات الرئيسية"));
        assertTrue(EditionAccessPolicy.canAccess(2, "القيود اليدوية"));
        assertTrue(EditionAccessPolicy.canAccess(2, "الحسابات البنكية"));
        assertFalse(EditionAccessPolicy.canAccess(2, "الموظفون والرواتب"));
    }
    @Test public void businessAddsPayrollAndEnterpriseRetainsAllImplementedModules() {
        assertTrue(EditionAccessPolicy.canAccess(3, "الموظفون والرواتب"));
        assertTrue(EditionAccessPolicy.canAccess(4, "الموظفون"));
        assertTrue(EditionAccessPolicy.canAccess(4, "المرتجعات"));
    }
    @Test public void unknownNullSectionsFailClosed() {
        assertFalse(EditionAccessPolicy.canAccess(1, null));
        assertFalse(EditionAccessPolicy.canAccess(0, "نقطة البيع والمبيعات"));
    }
}
