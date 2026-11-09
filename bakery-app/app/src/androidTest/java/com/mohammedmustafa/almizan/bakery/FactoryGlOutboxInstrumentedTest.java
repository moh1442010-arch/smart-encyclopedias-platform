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
