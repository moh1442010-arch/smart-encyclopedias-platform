package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GeneralLedgerAtomicityInstrumentedTest {
    @Test public void failedLineInsertRollsBackHeaderLinesIdempotencyAndAudit() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="gl_atomicity_"+System.currentTimeMillis()+".db";
        File file=context.getDatabasePath(name);
        File parent=file.getParentFile();
        assertNotNull(parent);
        if(!parent.exists())assertTrue(parent.mkdirs());
        if(file.exists())assertTrue(file.delete());
        SQLiteDatabase db=SecureDatabase.open(context,name);
        String reference="TEST-ATOMIC-"+System.currentTimeMillis();
        String key="TEST-ATOMIC-KEY-"+System.currentTimeMillis();
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('GL-ATOMIC-"+System.currentTimeMillis()+"','2026-11-01','2026-11-30','OPEN')");
            db.execSQL("CREATE TRIGGER fail_gl_line_insert BEFORE INSERT ON gl_journal_lines BEGIN SELECT RAISE(ABORT,'forced_atomicity_test_failure'); END");
            List<GeneralLedger.Line> lines=new ArrayList<>();
            lines.add(new GeneralLedger.Line("1000","MAIN","SDG",1000L,0L,1L,1L));
            lines.add(new GeneralLedger.Line("4000","MAIN","SDG",0L,1000L,1L,1L));
            GeneralLedger.Entry entry=new GeneralLedger.Entry(reference,"Atomicity rollback test","2026-11-11",
                "TEST_ATOMICITY",reference,key,"instrumented-test",lines);
            boolean failed=false;
            try {
                GeneralLedger.post(db,entry);
            } catch(RuntimeException expected) {
                failed=true;
            }
            assertTrue("Injected journal-line failure must reach the caller",failed);
            assertEquals("Journal header must roll back",0,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref=?",new String[]{reference}));
            assertEquals("Journal lines must roll back",0,count(db,
                "SELECT COUNT(*) FROM gl_journal_lines l JOIN gl_journal_entries e ON e.id=l.entry_id WHERE e.entry_ref=?",new String[]{reference}));
            assertEquals("Idempotency key must roll back",0,count(db,
                "SELECT COUNT(*) FROM idempotency_keys WHERE key=? AND operation='GL_POST'",new String[]{key}));
            assertEquals("Audit record must roll back",0,count(db,
                "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_ref=? AND action='JOURNAL_POSTED'",new String[]{reference}));
        } finally {
            db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }

    private static long count(SQLiteDatabase db,String sql,String[] args) {
        Cursor cursor=db.rawQuery(sql,args);
        try {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }
}
