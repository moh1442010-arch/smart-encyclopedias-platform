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
public class FactoryLedgerSyncServiceInstrumentedTest {
    private SQLiteDatabase source(Context context,String name) {
        SQLiteDatabase db=SecureDatabase.open(context,name);
        db.execSQL("CREATE TABLE journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
        db.execSQL("CREATE TABLE journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
        FactoryGlOutbox.ensureSchema(db);
        return db;
    }

    private SQLiteDatabase destination(Context context,String name) {
        SQLiteDatabase db=SecureDatabase.open(context,name);
        EnterpriseSchema.ensure(db);
        db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('SYNC-2026','2026-01-01','2026-12-31','OPEN')");
        return db;
    }

    private void insertSourceJournal(SQLiteDatabase db,String ref,long amount) {
        db.beginTransaction();
        try {
            db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,date) VALUES(?,?,'POSTED','2026-11-10')",
                new Object[]{ref,"اختبار مزامنة دفتر المصنع"});
            Cursor id=db.rawQuery("SELECT last_insert_rowid()",null);
            long entryId;
            try{assertTrue(id.moveToFirst());entryId=id.getLong(0);}finally{id.close();}
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'الصندوق',?,0)",
                new Object[]{entryId,amount});
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'إيرادات المبيعات',0,?)",
                new Object[]{entryId,amount});
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }

    private long scalar(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try{assertTrue(c.moveToFirst());return c.getLong(0);}finally{c.close();}
    }

    @Test public void mainLedgerScreenSyncPostsFactoryHistoryAndReconcilesIt() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_ledger_sync_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_ledger_sync_destination_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=source(context,sourceName);
        SQLiteDatabase destination=destination(context,destinationName);
        try {
            insertSourceJournal(source,"SYNC-SALE-001",24500L);
            FactoryLedgerSyncService.Result first=FactoryLedgerSyncService.synchronize(source,destination);
            assertEquals(15,first.mappingsSeeded);
            assertEquals(1,first.legacyCaptured);
            assertEquals(1,first.posted);
            assertTrue(first.clean());
            assertEquals(1,scalar(destination,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND source_id='SYNC-SALE-001'",null));
            assertEquals(24500L,scalar(destination,
                "SELECT SUM(l.base_debit_minor) FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.source_type='FACTORY_LEDGER' AND e.source_id='SYNC-SALE-001'",null));

            FactoryLedgerSyncService.Result replay=FactoryLedgerSyncService.synchronize(source,destination);
            assertEquals(0,replay.legacyCaptured);
            assertEquals(0,replay.posted);
            assertTrue(replay.clean());
            assertEquals("repeat sync must not duplicate the general-ledger entry",1,scalar(destination,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND source_id='SYNC-SALE-001'",null));
        } finally {
            if(source.isOpen())source.close();
            if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }

    @Test public void foreignBaseCurrencyDoesNotSeedAnUnverifiedOneToOneMapping() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String sourceName="factory_ledger_sync_fx_source_"+System.currentTimeMillis()+".db";
        String destinationName="factory_ledger_sync_fx_destination_"+System.currentTimeMillis()+".db";
        SQLiteDatabase source=source(context,sourceName);
        SQLiteDatabase destination=destination(context,destinationName);
        try {
            destination.execSQL("UPDATE company_settings SET base_currency='SAR' WHERE id=1");
            insertSourceJournal(source,"SYNC-FX-001",10000L);
            FactoryLedgerSyncService.Result result=FactoryLedgerSyncService.synchronize(source,destination);
            assertEquals(0,result.mappingsSeeded);
            assertEquals(0,result.posted);
            assertFalse(result.clean());
            assertEquals(0,scalar(destination,
                "SELECT COUNT(*) FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND source_id='SYNC-FX-001'",null));
        } finally {
            if(source.isOpen())source.close();
            if(destination.isOpen())destination.close();
            context.deleteDatabase(sourceName);context.deleteDatabase(sourceName+"-wal");context.deleteDatabase(sourceName+"-shm");
            context.deleteDatabase(destinationName);context.deleteDatabase(destinationName+"-wal");context.deleteDatabase(destinationName+"-shm");
        }
    }
}
