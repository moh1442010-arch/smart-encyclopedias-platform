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
                assertNotNull("Injected failure must include a database error",expected.getMessage());
                assertTrue("Failure must come from the injected journal-line trigger: "+expected.getMessage(),
                    expected.getMessage().contains("forced_atomicity_test_failure"));
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

    @Test public void overflowingBaseCurrencyTotalsAreRejectedWithoutPartialWrites() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="gl_overflow_"+System.currentTimeMillis()+".db";
        File file=context.getDatabasePath(name);
        File parent=file.getParentFile();
        assertNotNull(parent);
        if(!parent.exists())assertTrue(parent.mkdirs());
        if(file.exists())assertTrue(file.delete());
        SQLiteDatabase db=SecureDatabase.open(context,name);
        String reference="TEST-OVERFLOW-"+System.currentTimeMillis();
        String key="TEST-OVERFLOW-KEY-"+System.currentTimeMillis();
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('GL-OVERFLOW-"+System.currentTimeMillis()+"','2026-11-01','2026-11-30','OPEN')");
            List<GeneralLedger.Line> lines=new ArrayList<>();
            lines.add(new GeneralLedger.Line("1000","MAIN","SDG",Long.MAX_VALUE,0L,1L,1L));
            lines.add(new GeneralLedger.Line("1000","MAIN","SDG",Long.MAX_VALUE,0L,1L,1L));
            lines.add(new GeneralLedger.Line("4000","MAIN","SDG",0L,Long.MAX_VALUE,1L,1L));
            lines.add(new GeneralLedger.Line("4000","MAIN","SDG",0L,Long.MAX_VALUE,1L,1L));
            GeneralLedger.Entry entry=new GeneralLedger.Entry(reference,"Overflow rollback test","2026-11-11",
                "TEST_OVERFLOW",reference,key,"instrumented-test",lines);
            boolean failed=false;
            try {
                GeneralLedger.post(db,entry);
            } catch(ArithmeticException expected) {
                failed=true;
            }
            assertTrue("Base-currency journal total overflow must be rejected",failed);
            assertEquals("Overflow must not leave a journal header",0,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref=?",new String[]{reference}));
            assertEquals("Overflow must not leave journal lines",0,count(db,
                "SELECT COUNT(*) FROM gl_journal_lines l JOIN gl_journal_entries e ON e.id=l.entry_id WHERE e.entry_ref=?",new String[]{reference}));
            assertEquals("Overflow must not leave an idempotency key",0,count(db,
                "SELECT COUNT(*) FROM idempotency_keys WHERE key=? AND operation='GL_POST'",new String[]{key}));
            assertEquals("Overflow must not leave an audit row",0,count(db,
                "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_ref=? AND action='JOURNAL_POSTED'",new String[]{reference}));
        } finally {
            db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }


    @Test public void sourceReferenceIsUniqueAndAuditableForExactSourceDocument() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="gl_source_identity_"+System.currentTimeMillis()+".db";
        File file=context.getDatabasePath(name);
        File parent=file.getParentFile();
        assertNotNull(parent);
        if(!parent.exists())assertTrue(parent.mkdirs());
        if(file.exists())assertTrue(file.delete());
        SQLiteDatabase db=SecureDatabase.open(context,name);
        String sourceType="FACTORY_PURCHASE";
        String sourceId="PURCHASE-D10-"+System.currentTimeMillis();
        String firstRef="GL-D10-FIRST-"+System.currentTimeMillis();
        String firstKey="GL-D10-KEY-FIRST-"+System.currentTimeMillis();
        String secondRef="GL-D10-SECOND-"+System.currentTimeMillis();
        String secondKey="GL-D10-KEY-SECOND-"+System.currentTimeMillis();
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('GL-D10-"+System.currentTimeMillis()+"','2026-11-01','2026-11-30','OPEN')");
            List<GeneralLedger.Line> lines=new ArrayList<>();
            lines.add(new GeneralLedger.Line("1200","MAIN","SDG",2500L,0L,1L,1L));
            lines.add(new GeneralLedger.Line("2000","MAIN","SDG",0L,2500L,1L,1L));
            GeneralLedger.Entry first=new GeneralLedger.Entry(firstRef,"D10 source identity","2026-11-11",
                sourceType,sourceId,firstKey,"instrumented-test",lines);
            GeneralLedger.Result posted=GeneralLedger.post(db,first);
            assertEquals(firstRef,posted.reference);
            assertEquals("Exact source must resolve to exactly one posted journal",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE source_type=? AND source_id=? AND status='POSTED'",
                new String[]{sourceType,sourceId}));
            assertEquals("Audit row must point to the same immutable GL reference",1,count(db,
                "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_type='GL_JOURNAL' AND entity_ref=? AND action='JOURNAL_POSTED'",
                new String[]{firstRef}));
            assertEquals("Audit row must retain the exact source document identity",1,count(db,
                "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_ref=? AND details LIKE ?",
                new String[]{firstRef,sourceType+":"+sourceId+":%"}));

            GeneralLedger.Entry duplicateSource=new GeneralLedger.Entry(secondRef,"Duplicate D10 source","2026-11-11",
                sourceType,sourceId,secondKey,"instrumented-test",lines);
            boolean rejected=false;
            try {
                GeneralLedger.post(db,duplicateSource);
            } catch(RuntimeException expected) {
                rejected=true;
            }
            assertTrue("A second GL posting for the same source type and source ID must be rejected",rejected);
            assertEquals("Duplicate source must not create a second journal",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE source_type=? AND source_id=?",
                new String[]{sourceType,sourceId}));
            assertEquals("Duplicate source must not leave a second idempotency key",0,count(db,
                "SELECT COUNT(*) FROM idempotency_keys WHERE key=? AND operation='GL_POST'",
                new String[]{secondKey}));
            assertEquals("Duplicate source must not leave a second audit row",0,count(db,
                "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_ref=? AND action='JOURNAL_POSTED'",
                new String[]{secondRef}));
        } finally {
            db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }


    @Test public void reversalKeepsOriginalAndIsIdempotentAndCannotReverseReversal() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="gl_reversal_"+System.currentTimeMillis()+".db";
        File file=context.getDatabasePath(name);
        File parent=file.getParentFile();
        assertNotNull(parent);
        if(!parent.exists())assertTrue(parent.mkdirs());
        if(file.exists())assertTrue(file.delete());
        SQLiteDatabase db=SecureDatabase.open(context,name);
        String suffix=String.valueOf(System.currentTimeMillis());
        String originalRef="GL-REV-ORIGINAL-"+suffix;
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('GL-REV-"+suffix+"','2026-11-01','2026-11-30','OPEN')");
            db.execSQL("INSERT INTO users(username,display_name,pin_salt,pin_hash,active,created_at) VALUES('d11-owner','D11 Owner',X'00',X'00',1,"+System.currentTimeMillis()+")");
            db.execSQL("INSERT INTO user_roles(user_id,role_id,branch_id) SELECT u.id,r.id,b.id FROM users u,roles r,branches b WHERE u.username='d11-owner' AND r.role_code='OWNER' AND b.branch_code='MAIN'");
            List<GeneralLedger.Line> lines=new ArrayList<>();
            lines.add(new GeneralLedger.Line("1000","MAIN","SDG",4200L,0L,1L,1L));
            lines.add(new GeneralLedger.Line("4000","MAIN","SDG",0L,4200L,1L,1L));
            GeneralLedger.Entry original=new GeneralLedger.Entry(originalRef,"D11 original posting","2026-11-11",
                "TEST_REVERSAL","SOURCE-D11-"+suffix,"D11-ORIGINAL-"+suffix,"d11-owner",lines);
            GeneralLedger.post(db,original);

            GeneralLedger.Result reversal=GeneralLedger.reverse(db,originalRef,"2026-11-12","d11-owner");
            assertEquals("REV-"+originalRef,reversal.reference);
            assertFalse("First reversal must be newly posted",reversal.alreadyPosted);
            assertEquals("Original journal must remain posted",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref=? AND status='POSTED'",
                new String[]{originalRef}));
            assertEquals("Reversal must link to the original journal",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries r JOIN gl_journal_entries o ON r.reversal_of=o.id WHERE r.entry_ref=? AND o.entry_ref=? AND r.status='POSTED'",
                new String[]{"REV-"+originalRef,originalRef}));
            assertEquals("Reversal must contain the equal opposite debit and credit",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries r JOIN gl_journal_lines l ON l.entry_id=r.id WHERE r.entry_ref=? AND l.base_debit_minor=0 AND l.base_credit_minor=4200",
                new String[]{"REV-"+originalRef}));
            assertEquals("Original debit must remain unchanged",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.entry_ref=? AND l.base_debit_minor=4200",
                new String[]{originalRef}));

            GeneralLedger.Result retry=GeneralLedger.reverse(db,originalRef,"2026-11-12","d11-owner");
            assertTrue("Retry must reuse the same reversal",retry.alreadyPosted);
            assertEquals("Retry must not create a second reversal",1,count(db,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref=?",
                new String[]{"REV-"+originalRef}));
            assertEquals("Reversal audit record must remain singular",1,count(db,
                "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_ref=? AND action='JOURNAL_POSTED'",
                new String[]{"REV-"+originalRef}));

            boolean rejected=false;
            try {
                GeneralLedger.reverse(db,"REV-"+originalRef,"2026-11-13","d11-owner");
            } catch(IllegalArgumentException expected) {
                rejected=true;
            }
            assertTrue("A reversal journal must not itself be reversed",rejected);
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
