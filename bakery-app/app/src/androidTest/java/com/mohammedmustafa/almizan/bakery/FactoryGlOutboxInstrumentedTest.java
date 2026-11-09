package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FactoryGlOutboxInstrumentedTest {
    private SQLiteDatabase openSource(Context context,String name) {
        SQLiteDatabase db=SecureDatabase.open(context,name);
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
        FactoryGlOutbox.ensureSchema(db);
        return db;
    }

    private long insertBalancedJournal(SQLiteDatabase db,String ref,long amount) {
        db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,date) VALUES(?,?,'POSTED','2026-11-09')",
            new Object[]{ref,"اختبار جسر المصنع"});
        Cursor id=db.rawQuery("SELECT last_insert_rowid()",null);
        long entryId;try{assertTrue(id.moveToFirst());entryId=id.getLong(0);}finally{id.close();}
        db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'الصندوق',?,0)",
            new Object[]{entryId,amount});
        db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'المبيعات',0,?)",
            new Object[]{entryId,amount});
        return entryId;
    }

    private int count(SQLiteDatabase db,String table) {
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null);
        try{assertTrue(c.moveToFirst());return c.getInt(0);}finally{c.close();}
    }

    @Test public void outboxSnapshotCommitsWithSourceJournalAndDuplicateEnqueueIsIdempotent() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_outbox_atomic_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openSource(context,name);
        try {
            db.beginTransaction();
            long entryId=insertBalancedJournal(db,"OUTBOX-ATOMIC-1",15000);
            FactoryGlOutbox.enqueue(db,entryId);
            db.setTransactionSuccessful();
            db.endTransaction();

            assertEquals(1,count(db,"journal_entries"));
            assertEquals(1,count(db,"factory_gl_outbox"));
            assertEquals(2,count(db,"factory_gl_outbox_lines"));

            db.beginTransaction();
            FactoryGlOutbox.enqueue(db,entryId);
            db.setTransactionSuccessful();
            db.endTransaction();
            assertEquals("duplicate enqueue must not create a second event",1,count(db,"factory_gl_outbox"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void sourceJournalWriterCreatesJournalAndOutboxInsideCallerTransaction() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_outbox_writer_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openSource(context,name);
        try {
            db.beginTransaction();
            long id=FactoryGlOutbox.postSourceJournal(db,"WRITER-1","اختبار ترحيل","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{5000,0},new long[]{0,5000});
            db.setTransactionSuccessful();
            db.endTransaction();
            assertTrue(id>0);
            assertEquals(1,count(db,"journal_entries"));
            assertEquals(1,count(db,"factory_gl_outbox"));
            assertEquals(2,count(db,"factory_gl_outbox_lines"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void sourceJournalWriterRejectsUnbalancedAmounts() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_outbox_unbalanced_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openSource(context,name);
        try {
            db.beginTransaction();
            try {
                FactoryGlOutbox.postSourceJournal(db,"WRITER-BAD","قيد غير متوازن","2026-11-09",
                    new String[]{"الصندوق","المبيعات"},new long[]{5000,0},new long[]{0,4999});
                fail("unbalanced factory journal must be rejected");
            } catch(IllegalArgumentException expected) {
                // The source transaction is intentionally rolled back.
            } finally { db.endTransaction(); }
            assertEquals(0,count(db,"journal_entries"));
            assertEquals(0,count(db,"factory_gl_outbox"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    private SQLiteDatabase openDestination(Context context,String name,String periodStatus) {
        SQLiteDatabase db=SecureDatabase.open(context,name);
        EnterpriseSchema.ensure(db);
        db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES(?,?,?,?)",
            new Object[]{"TEST-2026","2026-01-01","2026-12-31",periodStatus});
        return db;
    }

    private void mapCashSale(SQLiteDatabase source,String cashAccount) {
        FactoryGlOutbox.saveMapping(source,"الصندوق",cashAccount,"MAIN","SDG",1,1);
        FactoryGlOutbox.saveMapping(source,"المبيعات","4000","MAIN","SDG",1,1);
    }

    private long destinationEntryCount(SQLiteDatabase destination,String sourceId) {
        Cursor c=destination.rawQuery("SELECT COUNT(*) FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND source_id=?",
            new String[]{sourceId});
        try{assertTrue(c.moveToFirst());return c.getLong(0);}finally{c.close();}
    }

    @Test public void reviewedDefaultMappingsAreExplicitAndNeverOverwriteOperatorChanges() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_default_map_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,name);
        try {
            int first=FactoryGlOutbox.seedReviewedMappings(source,"MAIN","SDG");
            assertEquals(15,first);
            FactoryGlOutbox.saveMapping(source,"الصندوق","1010","MAIN","SDG",1,1);
            int second=FactoryGlOutbox.seedReviewedMappings(source,"MAIN","SDG");
            assertEquals(0,second);
            Cursor cash=source.rawQuery("SELECT target_account_code FROM factory_gl_account_mappings WHERE source_account='الصندوق'",null);
            try{assertTrue(cash.moveToFirst());assertEquals("1010",cash.getString(0));}finally{cash.close();}
        } finally {
            if(source.isOpen())source.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void mappedFactoryJournalPostsToGlAndCrashReplayDoesNotDuplicate() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_delivery_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_delivery_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"DELIVERY-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{18000,0},new long[]{0,18000});
            source.setTransactionSuccessful();source.endTransaction();

            FactoryGlOutbox.ProcessResult first=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,first.posted);
            assertEquals(1,destinationEntryCount(destination,"DELIVERY-1"));

            source.execSQL("UPDATE factory_gl_outbox SET status='PROCESSING',next_attempt_at=0 WHERE source_entry_ref='DELIVERY-1'");
            FactoryGlOutbox.ProcessResult replay=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,replay.posted);
            assertEquals("destination idempotency must prevent a second journal",1,destinationEntryCount(destination,"DELIVERY-1"));
            Cursor state=source.rawQuery("SELECT status,destination_ref FROM factory_gl_outbox WHERE source_entry_ref='DELIVERY-1'",null);
            try{assertTrue(state.moveToFirst());assertEquals("POSTED",state.getString(0));assertEquals("FACTORY-DELIVERY-1",state.getString(1));}finally{state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void changedMappedPayloadWithSameIdempotencyKeyIsRejected() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_changed_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_changed_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"DELIVERY-CHANGED","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{12000,0},new long[]{0,12000});
            source.setTransactionSuccessful();source.endTransaction();
            assertEquals(1,FactoryGlOutbox.processPending(source,destination).posted);
            source.execSQL("UPDATE factory_gl_outbox_lines SET target_account_code='5000' WHERE source_account='المبيعات'");
            source.execSQL("UPDATE factory_gl_outbox SET status='PROCESSING',next_attempt_at=0 WHERE source_entry_ref='DELIVERY-CHANGED'");
            FactoryGlOutbox.ProcessResult changed=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,changed.retried);
            assertEquals(1,destinationEntryCount(destination,"DELIVERY-CHANGED"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='DELIVERY-CHANGED'",null);
            try{assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("مفتاح العملية"));}finally{state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void unknownSourceAccountMappingNeverPostsToGl() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_unmapped_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_unmapped_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            FactoryGlOutbox.saveMapping(source,"المبيعات","4000","MAIN","SDG",1,1);
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"DELIVERY-UNMAPPED","قيد بحساب غير معروف","2026-11-09",
                new String[]{"حساب غير مربوط","المبيعات"},new long[]{4000,0},new long[]{0,4000});
            source.setTransactionSuccessful();source.endTransaction();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.skipped);
            assertEquals(0,destinationEntryCount(destination,"DELIVERY-UNMAPPED"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='DELIVERY-UNMAPPED'",null);
            try{assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("حساب غير مربوط"));}finally{state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void closedFinancialPeriodBlocksPostingAndKeepsEventRetryable() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_closed_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_closed_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"CLOSED");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"DELIVERY-CLOSED","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{6000,0},new long[]{0,6000});
            source.setTransactionSuccessful();source.endTransaction();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.retried);
            assertEquals(0,destinationEntryCount(destination,"DELIVERY-CLOSED"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='DELIVERY-CLOSED'",null);
            try{assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("فترة"));}finally{state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void rollingBackSourceJournalAlsoRollsBackOutboxEvent() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_outbox_rollback_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openSource(context,name);
        try {
            db.beginTransaction();
            long entryId=insertBalancedJournal(db,"OUTBOX-ROLLBACK-1",9000);
            FactoryGlOutbox.enqueue(db,entryId);
            db.endTransaction();

            assertEquals(0,count(db,"journal_entries"));
            assertEquals(0,count(db,"factory_gl_outbox"));
            assertEquals(0,count(db,"factory_gl_outbox_lines"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void changedSourcePayloadCannotReuseExistingOutboxIdentity() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_outbox_immutable_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openSource(context,name);
        try {
            db.beginTransaction();
            long entryId=insertBalancedJournal(db,"OUTBOX-IMMUTABLE-1",10000);
            FactoryGlOutbox.enqueue(db,entryId);
            db.setTransactionSuccessful();
            db.endTransaction();

            db.beginTransaction();
            db.execSQL("UPDATE journal_lines SET debit=11000,credit=0 WHERE entry_id=? AND account='الصندوق'",
                new Object[]{entryId});
            try {
                FactoryGlOutbox.enqueue(db,entryId);
                fail("changed source payload must be rejected");
            } catch(IllegalStateException expected) {
                // The surrounding transaction is intentionally rolled back below.
            } finally {
                db.endTransaction();
            }

            Cursor source=db.rawQuery("SELECT debit FROM journal_lines WHERE entry_id=? AND account='الصندوق'",
                new String[]{String.valueOf(entryId)});
            try{assertTrue(source.moveToFirst());assertEquals(10000,source.getLong(0));}finally{source.close();}
            Cursor outbox=db.rawQuery("SELECT payload_sha256,status FROM factory_gl_outbox WHERE source_entry_id=?",
                new String[]{String.valueOf(entryId)});
            try{assertTrue(outbox.moveToFirst());assertEquals("PENDING",outbox.getString(1));}finally{outbox.close();}
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }
}
