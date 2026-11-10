package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;
import java.lang.reflect.Constructor;
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
            Cursor payloadVersion=db.rawQuery("SELECT payload_version FROM factory_gl_outbox WHERE source_entry_ref='OUTBOX-ATOMIC-1'",null);
            try{assertTrue(payloadVersion.moveToFirst());assertEquals(1,payloadVersion.getInt(0));}finally{payloadVersion.close();}
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

    @Test public void existingFactoryDatabaseV12UpgradesAdditivelyToV13() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="almizan_factory_1.db";
        context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        SQLiteDatabase legacy=SecureDatabase.open(context,name);
        legacy.execSQL("CREATE TABLE materials(id INTEGER PRIMARY KEY,name TEXT,qty INTEGER,value INTEGER)");
        legacy.execSQL("INSERT INTO materials(id,name,qty,value) VALUES(1,'دقيق',12000,9000)");
        legacy.execSQL("CREATE TABLE factory_gl_outbox(id INTEGER PRIMARY KEY AUTOINCREMENT,event_key TEXT NOT NULL UNIQUE,source_entry_id INTEGER NOT NULL UNIQUE,source_entry_ref TEXT NOT NULL UNIQUE,description TEXT NOT NULL,transaction_date TEXT NOT NULL,payload_sha256 TEXT NOT NULL,status TEXT NOT NULL,attempt_count INTEGER NOT NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',destination_ref TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,posted_at INTEGER)");
        legacy.execSQL("INSERT INTO factory_gl_outbox(event_key,source_entry_id,source_entry_ref,description,transaction_date,payload_sha256,status,attempt_count,next_attempt_at,last_error,destination_ref,created_at,updated_at) VALUES('OLD-V12',1,'OLD-V12','old event','2026-11-09','0000000000000000000000000000000000000000000000000000000000000000','PENDING',0,0,'','',1,1)");
        legacy.setVersion(12);
        legacy.close();
        SQLiteOpenHelper helper=null;
        try {
            Class<?> dbClass=Class.forName("com.mohammedmustafa.almizan.bakery.FactoryActivity$DB");
            Constructor<?> constructor=dbClass.getDeclaredConstructor(Context.class);
            constructor.setAccessible(true);
            helper=(SQLiteOpenHelper)constructor.newInstance(context);
            SQLiteDatabase upgraded=helper.getWritableDatabase();
            assertEquals(13,upgraded.getVersion());
            Cursor outboxTable=upgraded.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='factory_gl_outbox'",null);
            try{assertTrue(outboxTable.moveToFirst());assertEquals(1,outboxTable.getInt(0));}finally{outboxTable.close();}
            Cursor version=upgraded.rawQuery("SELECT payload_version FROM factory_gl_outbox WHERE event_key='OLD-V12'",null);
            try{assertTrue(version.moveToFirst());assertEquals(1,version.getInt(0));}finally{version.close();}
            Cursor oldData=upgraded.rawQuery("SELECT name,qty,value FROM materials WHERE id=1",null);
            try{assertTrue(oldData.moveToFirst());assertEquals("دقيق",oldData.getString(0));assertEquals(12000,oldData.getLong(1));assertEquals(9000,oldData.getLong(2));}
            finally{oldData.close();}
        } finally {
            if(helper!=null)helper.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void backfillCapturesValidLegacyJournalsWithoutChangingHistoryAndLeavesInvalidOnesVisible() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="factory_gl_backfill_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=openSource(context,name);
        try {
            db.beginTransaction();
            insertBalancedJournal(db,"LEGACY-VALID",9000);
            long reversedOriginal=insertBalancedJournal(db,"LEGACY-REVERSED",4000);
            db.execSQL("UPDATE journal_entries SET status='REVERSED' WHERE id=?",new Object[]{reversedOriginal});
            db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,reversal_of,date) VALUES('LEGACY-REVERSAL','عكس قيد قديم','POSTED',?,'2026-11-09')",
                new Object[]{reversedOriginal});
            long reversalId;Cursor reversalCursor=db.rawQuery("SELECT last_insert_rowid()",null);
            try{assertTrue(reversalCursor.moveToFirst());reversalId=reversalCursor.getLong(0);}finally{reversalCursor.close();}
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'المبيعات',4000,0)",new Object[]{reversalId});
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'الصندوق',0,4000)",new Object[]{reversalId});
            db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,date) VALUES('LEGACY-BAD','قديم غير متوازن','POSTED','2026-11-09')");
            long invalid;Cursor id=db.rawQuery("SELECT last_insert_rowid()",null);
            try{assertTrue(id.moveToFirst());invalid=id.getLong(0);}finally{id.close();}
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'الصندوق',7000,0)",new Object[]{invalid});
            db.setTransactionSuccessful();db.endTransaction();

            FactoryGlOutbox.BackfillResult result=FactoryGlOutbox.backfillPostedJournals(db,100);
            assertEquals(3,result.captured);assertEquals(1,result.failed);
            assertEquals(3,count(db,"factory_gl_outbox"));
            assertEquals(4,count(db,"journal_entries"));
            assertEquals(6,count(db,"factory_gl_outbox_lines"));
            Cursor capturedReversed=db.rawQuery("SELECT COUNT(*) FROM factory_gl_outbox WHERE source_entry_ref IN ('LEGACY-REVERSED','LEGACY-REVERSAL')",null);
            try{assertTrue(capturedReversed.moveToFirst());assertEquals(2,capturedReversed.getInt(0));}finally{capturedReversed.close();}
            Cursor missing=db.rawQuery("SELECT COUNT(*) FROM journal_entries j LEFT JOIN factory_gl_outbox o ON o.source_entry_id=j.id WHERE j.id=? AND o.id IS NULL",
                new String[]{String.valueOf(invalid)});
            try{assertTrue(missing.moveToFirst());assertEquals(1,missing.getInt(0));}finally{missing.close();}
            FactoryGlOutbox.BackfillResult replay=FactoryGlOutbox.backfillPostedJournals(db,100);
            assertEquals(0,replay.captured);assertEquals(1,replay.failed);
            assertEquals(3,count(db,"factory_gl_outbox"));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
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
            FactoryGlOutbox.ReconciliationReport report=FactoryGlOutbox.reconcile(source,destination);
            assertTrue("posted source/outbox/GL must reconcile",report.clean());
            assertEquals(0,report.missingOutbox);assertEquals(0,report.orphanOutbox);assertEquals(0,report.missingDestination);assertEquals(0,report.amountMismatches);
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
            FactoryGlOutbox.ReconciliationReport report=FactoryGlOutbox.reconcile(source,destination);
            assertEquals(1,report.unacknowledgedDestination);
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void reconciliationDetectsDestinationTransactionAmountMismatch() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_reconcile_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_reconcile_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"RECONCILE-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{15000,0},new long[]{0,15000});
            source.setTransactionSuccessful();source.endTransaction();
            assertEquals(1,FactoryGlOutbox.processPending(source,destination).posted);
            destination.execSQL("UPDATE gl_journal_lines SET transaction_debit_minor=15001 WHERE entry_id IN "+
                "(SELECT id FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND source_id='RECONCILE-1') AND transaction_debit_minor>0");
            FactoryGlOutbox.ReconciliationReport report=FactoryGlOutbox.reconcile(source,destination);
            assertEquals(1,report.amountMismatches);
            assertFalse(report.clean());
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void foreignBaseCurrencyRejectsUnverifiedOneToOneFactoryRate() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_fx_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_fx_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            destination.execSQL("UPDATE company_settings SET base_currency='SAR' WHERE id=1");
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"DELIVERY-FX","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{8000,0},new long[]{0,8000});
            source.setTransactionSuccessful();source.endTransaction();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.retried);
            assertEquals(0,destinationEntryCount(destination,"DELIVERY-FX"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='DELIVERY-FX'",null);
            try{assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("سعر الصرف غير محدد"));}finally{state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void nonSdgImportedMappingCannotPostFactoryAmountsAsAnotherCurrency() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_currency_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_currency_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"1000");
            source.execSQL("UPDATE factory_gl_account_mappings SET currency_code='AED',fx_rate_num=2,fx_rate_den=1 WHERE source_account='الصندوق'");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"CURRENCY-SOURCE-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{18000,0},new long[]{0,18000});
            source.setTransactionSuccessful();source.endTransaction();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.retried);
            assertEquals(0,destinationEntryCount(destination,"CURRENCY-SOURCE-1"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='CURRENCY-SOURCE-1'",null);
            try{assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("عملة مصدر دفتر المصنع ثابتة SDG"));}finally{state.close();}
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
            db.execSQL("UPDATE journal_lines SET debit=0,credit=11000 WHERE entry_id=? AND account='المبيعات'",
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

    @Test public void retryLimitMovesEventToDeadLetterWithCappedBackoff() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_dead_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_dead_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"CLOSED");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"DEAD-LETTER-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{7000,0},new long[]{0,7000});
            source.setTransactionSuccessful();source.endTransaction();
            source.execSQL("UPDATE factory_gl_outbox SET attempt_count=7,next_attempt_at=0 WHERE source_entry_ref='DEAD-LETTER-1'");
            long before=System.currentTimeMillis();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.deadLetter);
            Cursor state=source.rawQuery("SELECT status,attempt_count,next_attempt_at FROM factory_gl_outbox WHERE source_entry_ref='DEAD-LETTER-1'",null);
            try {
                assertTrue(state.moveToFirst());
                assertEquals("DEAD_LETTER",state.getString(0));
                assertEquals(8,state.getInt(1));
                assertTrue(state.getLong(2)>=before);
                assertTrue(state.getLong(2)<=before+3600000L);
            } finally {state.close();}
            assertEquals(0,destinationEntryCount(destination,"DEAD-LETTER-1"));
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void firstResolvedAccountMappingIsFrozenAcrossRetry() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_frozen_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_frozen_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"CLOSED");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"MAPPING-FROZEN-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{11000,0},new long[]{0,11000});
            source.setTransactionSuccessful();source.endTransaction();

            FactoryGlOutbox.ProcessResult first=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,first.retried);
            Cursor snapshot=source.rawQuery("SELECT target_account_code FROM factory_gl_outbox_lines WHERE source_account='الصندوق'",null);
            try {assertTrue(snapshot.moveToFirst());assertEquals("1000",snapshot.getString(0));} finally {snapshot.close();}

            FactoryGlOutbox.saveMapping(source,"الصندوق","1010","MAIN","SDG",1,1);
            destination.execSQL("UPDATE financial_periods SET status='OPEN' WHERE period_code='TEST-2026'");
            FactoryGlOutbox.makeRetriesDueNow(source);
            FactoryGlOutbox.ProcessResult second=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,second.posted);

            Cursor posted=destination.rawQuery("SELECT a.account_code FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id JOIN chart_of_accounts a ON a.id=l.account_id WHERE e.source_type='FACTORY_LEDGER' AND e.source_id='MAPPING-FROZEN-1' ORDER BY l.id",null);
            try {assertTrue(posted.moveToFirst());assertEquals("1000",posted.getString(0));} finally {posted.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void inactiveOrUnknownDestinationAccountCannotPostFactoryJournal() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_inactive_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_inactive_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"9999");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"INACTIVE-ACCOUNT-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{5000,0},new long[]{0,5000});
            source.setTransactionSuccessful();source.endTransaction();

            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.retried);
            assertEquals(0,destinationEntryCount(destination,"INACTIVE-ACCOUNT-1"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='INACTIVE-ACCOUNT-1'",null);
            try {assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("الحساب غير موجود أو غير نشط"));} finally {state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void legacyReversedJournalAndItsReversalBothPostAndNetToZero() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_reverse_pair_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_reverse_pair_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"1000");
            source.beginTransaction();
            long original=insertBalancedJournal(source,"LEGACY-PAIR-ORIGINAL",4200);
            source.execSQL("UPDATE journal_entries SET status='REVERSED' WHERE id=?",new Object[]{original});
            source.execSQL("INSERT INTO journal_entries(entry_ref,description,status,reversal_of,date) VALUES('LEGACY-PAIR-REVERSAL','عكس القيد التاريخي','POSTED',?,'2026-11-09')",
                new Object[]{original});
            Cursor id=source.rawQuery("SELECT last_insert_rowid()",null);
            long reversal;try{assertTrue(id.moveToFirst());reversal=id.getLong(0);}finally{id.close();}
            source.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'المبيعات',4200,0)",new Object[]{reversal});
            source.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'الصندوق',0,4200)",new Object[]{reversal});
            source.setTransactionSuccessful();source.endTransaction();

            FactoryGlOutbox.BackfillResult backfill=FactoryGlOutbox.backfillPostedJournals(source,100);
            assertEquals(2,backfill.captured);
            FactoryGlOutbox.ProcessResult posted=FactoryGlOutbox.processPending(source,destination);
            assertEquals(2,posted.posted);
            assertEquals(2,destinationEntryCount(destination,"LEGACY-PAIR-ORIGINAL")+destinationEntryCount(destination,"LEGACY-PAIR-REVERSAL"));
            Cursor totals=destination.rawQuery("SELECT SUM(l.base_debit_minor),SUM(l.base_credit_minor) FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.source_type='FACTORY_LEDGER' AND e.source_id IN ('LEGACY-PAIR-ORIGINAL','LEGACY-PAIR-REVERSAL')",null);
            try {assertTrue(totals.moveToFirst());assertEquals(8400,totals.getLong(0));assertEquals(8400,totals.getLong(1));} finally {totals.close();}
            FactoryGlOutbox.ReconciliationReport report=FactoryGlOutbox.reconcile(source,destination);
            assertTrue(report.clean());
            assertEquals(2,report.sourceJournals);
            assertEquals(2,report.posted);
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void inactiveBranchCannotReceiveFactoryPosting() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_branch_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_branch_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            FactoryGlOutbox.saveMapping(source,"الصندوق","1000","MISSING-BRANCH","SDG",1,1);
            FactoryGlOutbox.saveMapping(source,"المبيعات","4000","MAIN","SDG",1,1);
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"INACTIVE-BRANCH-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{3000,0},new long[]{0,3000});
            source.setTransactionSuccessful();source.endTransaction();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.retried);
            assertEquals(0,destinationEntryCount(destination,"INACTIVE-BRANCH-1"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='INACTIVE-BRANCH-1'",null);
            try {assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("الفرع غير موجود أو غير نشط"));} finally {state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void inactiveCurrencyCannotReceiveFactoryPosting() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_gl_currency_active_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_gl_currency_active_dest_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=openSource(context,sourceName),destination=openDestination(context,destinationName,"OPEN");
        try {
            mapCashSale(source,"1000");
            destination.execSQL("UPDATE currencies SET active=0 WHERE code='SDG'");
            source.beginTransaction();
            FactoryGlOutbox.postSourceJournal(source,"INACTIVE-CURRENCY-1","بيع نقدي","2026-11-09",
                new String[]{"الصندوق","المبيعات"},new long[]{2500,0},new long[]{0,2500});
            source.setTransactionSuccessful();source.endTransaction();
            FactoryGlOutbox.ProcessResult result=FactoryGlOutbox.processPending(source,destination);
            assertEquals(1,result.retried);
            assertEquals(0,destinationEntryCount(destination,"INACTIVE-CURRENCY-1"));
            Cursor state=source.rawQuery("SELECT status,last_error FROM factory_gl_outbox WHERE source_entry_ref='INACTIVE-CURRENCY-1'",null);
            try {assertTrue(state.moveToFirst());assertEquals("RETRY",state.getString(0));assertTrue(state.getString(1).contains("عملة غير نشطة"));} finally {state.close();}
        } finally {
            if(source.isOpen())source.close();if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }
}
