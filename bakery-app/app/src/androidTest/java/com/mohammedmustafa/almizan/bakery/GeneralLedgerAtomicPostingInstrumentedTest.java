package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GeneralLedgerAtomicPostingInstrumentedTest {
    private SQLiteDatabase openLedger(Context context,String name) {
        SQLiteDatabase db=SecureDatabase.open(context,name);
        EnterpriseSchema.ensure(db);
        db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('TEST-2026-11','2026-11-01','2026-11-30','OPEN')");
        return db;
    }
    private GeneralLedger.Entry entry(String suffix) {
        return new GeneralLedger.Entry("GL-ATOMIC-"+suffix,"اختبار ترحيل ذري","2026-11-10",
            "INSTRUMENTED_TEST","SOURCE-"+suffix,"IDEM-"+suffix,"TEST",
            Arrays.asList(new GeneralLedger.Line("1000","MAIN","SDG",1000,0,1,1),
                          new GeneralLedger.Line("4000","MAIN","SDG",0,1000,1,1)));
    }
    private int count(SQLiteDatabase db,String table) {
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null);
        try { assertTrue(c.moveToFirst()); return c.getInt(0); } finally { c.close(); }
    }
    @Test public void injectedAuditFailureRollsBackHeaderLinesAndIdempotency() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="gl_atomic_failure_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openLedger(context,name);
        try {
            db.execSQL("CREATE TRIGGER fail_gl_audit BEFORE INSERT ON enterprise_audit_log BEGIN SELECT RAISE(ABORT,'INJECTED_AUDIT_FAILURE'); END");
            boolean failed=false;
            try { GeneralLedger.post(db,entry("FAIL")); } catch(RuntimeException expected) { failed=true; }
            assertTrue("Injected failure must escape to caller",failed);
            assertEquals("journal header must roll back",0,count(db,"gl_journal_entries"));
            assertEquals("journal lines must roll back",0,count(db,"gl_journal_lines"));
            assertEquals("idempotency key must roll back",0,count(db,"idempotency_keys"));
            assertEquals("audit row must not persist",0,count(db,"enterprise_audit_log"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name); context.deleteDatabase(name+"-wal"); context.deleteDatabase(name+"-shm");
        }
    }
    @Test public void successfulPostingIsBalancedAndRetryIsIdempotent() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="gl_atomic_success_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openLedger(context,name);
        try {
            GeneralLedger.Entry entry=entry("SUCCESS");
            GeneralLedger.Result first=GeneralLedger.post(db,entry);
            assertFalse(first.alreadyPosted);
            assertEquals(1,count(db,"gl_journal_entries")); assertEquals(2,count(db,"gl_journal_lines"));
            assertEquals(1,count(db,"idempotency_keys")); assertEquals(1,count(db,"enterprise_audit_log"));
            GeneralLedger.Result retry=GeneralLedger.post(db,entry);
            assertTrue(retry.alreadyPosted); assertEquals(first.reference,retry.reference);
            assertEquals(1,count(db,"gl_journal_entries")); assertEquals(2,count(db,"gl_journal_lines"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name); context.deleteDatabase(name+"-wal"); context.deleteDatabase(name+"-shm");
        }
    }
}
