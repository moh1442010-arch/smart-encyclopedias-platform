package com.mohammedmustafa.almizan.bakery;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class FactoryGlOutboxTest {
    private FactoryGlOutbox.SnapshotLine line(String account,long debit,long credit) {
        return new FactoryGlOutbox.SnapshotLine(0,account,debit,credit,null,null,null,1,1);
    }

    @Test public void eventKeyIsStableAndNamespacedByFactoryLedger() {
        assertEquals("factory-ledger:WIP-STAGE-12",FactoryGlOutbox.eventKey("WIP-STAGE-12"));
        assertEquals(FactoryGlOutbox.eventKey("SALE-44"),FactoryGlOutbox.eventKey(" SALE-44 "));
    }

    @Test(expected=IllegalArgumentException.class)
    public void eventKeyRejectsBlankSourceReference() {
        FactoryGlOutbox.eventKey("  ");
    }

    @Test public void snapshotHashDoesNotDependOnLineOrder() {
        List<FactoryGlOutbox.SnapshotLine> a=new ArrayList<>();
        a.add(line("الصندوق",12000,0));
        a.add(line("إيرادات المبيعات",0,12000));
        List<FactoryGlOutbox.SnapshotLine> b=new ArrayList<>();
        b.add(line("إيرادات المبيعات",0,12000));
        b.add(line("الصندوق",12000,0));
        assertEquals(FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-09",a),
            FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-09",b));
    }

    @Test public void snapshotHashChangesWhenSourceAmountChanges() {
        List<FactoryGlOutbox.SnapshotLine> original=new ArrayList<>();
        original.add(line("الصندوق",12000,0));
        original.add(line("إيرادات المبيعات",0,12000));
        List<FactoryGlOutbox.SnapshotLine> changed=new ArrayList<>();
        changed.add(line("الصندوق",12500,0));
        changed.add(line("إيرادات المبيعات",0,12500));
        assertNotEquals(FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-09",original),
            FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-09",changed));
    }

    @Test public void snapshotHashChangesWhenAccountOrHeaderChanges() {
        List<FactoryGlOutbox.SnapshotLine> lines=new ArrayList<>();
        lines.add(line("الصندوق",12000,0));
        lines.add(line("إيرادات المبيعات",0,12000));
        String baseline=FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-09",lines);
        assertNotEquals(baseline,FactoryGlOutbox.payloadHash("SALE-1","بيع آجل","2026-11-09",lines));
        assertNotEquals(baseline,FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-10",lines));
        List<FactoryGlOutbox.SnapshotLine> changedAccount=new ArrayList<>();
        changedAccount.add(line("بنكك",12000,0));
        changedAccount.add(line("إيرادات المبيعات",0,12000));
        assertNotEquals(baseline,FactoryGlOutbox.payloadHash("SALE-1","بيع نقدي","2026-11-09",changedAccount));
    }
}
