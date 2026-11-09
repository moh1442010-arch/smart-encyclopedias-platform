package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FactoryBackupRestoreTest {
    @Test public void restoresFactoryDataAndIdentityWithoutChangingBakeryIdentity() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String database="almizan_factory_1.db";
        context.deleteDatabase(database);
        context.deleteDatabase(database+"-wal");
        context.deleteDatabase(database+"-shm");
        android.content.SharedPreferences factory=context.getSharedPreferences("factory_profile",Context.MODE_PRIVATE);
        android.content.SharedPreferences bakery=context.getSharedPreferences("bakery_profile",Context.MODE_PRIVATE);
        factory.edit().putString("business_name","مصنع الاختبار").commit();
        bakery.edit().putString("business_name","مخبز الاختبار").commit();

        SQLiteDatabase db=SecureDatabase.open(context,database);
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
            db.execSQL("INSERT INTO materials(name,unit,qty,value) VALUES('backup-test-material','kg',17,8500)");
            db.execSQL("CREATE TABLE IF NOT EXISTS factory_customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            db.execSQL("INSERT INTO factory_customers(name,phone,iban) VALUES('Factory Test Customer','249900000010','SD00000000000000000001')");
            db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
            db.beginTransaction();
            try {
                db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,date) VALUES('BACKUP-OUTBOX-1','Backup bridge test','POSTED','2026-11-09')");
                Cursor journalId=db.rawQuery("SELECT last_insert_rowid()",null);
                long id;try{assertTrue(journalId.moveToFirst());id=journalId.getLong(0);}finally{journalId.close();}
                db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'الصندوق',7000,0)",new Object[]{id});
                db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,'المبيعات',0,7000)",new Object[]{id});
                FactoryGlOutbox.saveMapping(db,"الصندوق","1000","MAIN","SDG",1,1);
                FactoryGlOutbox.enqueue(db,id);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        } finally { db.close(); }

        JSONObject snapshot=FactoryBackup.export(context);
        assertEquals(13,snapshot.getInt("schema_version"));
        assertEquals(1,snapshot.getJSONObject("tables").getJSONArray("factory_gl_outbox").getJSONObject(0).getInt("payload_version"));
        assertEquals(1,snapshot.getJSONObject("tables").getJSONArray("factory_gl_outbox").length());
        assertEquals(2,snapshot.getJSONObject("tables").getJSONArray("factory_gl_outbox_lines").length());
        FactoryBackup.validate(snapshot);
        JSONObject legacySnapshot=new JSONObject(snapshot.toString());
        legacySnapshot.put("schema_version",11);
        legacySnapshot.getJSONObject("tables").getJSONArray("factory_gl_outbox").getJSONObject(0).remove("payload_version");
        FactoryBackup.validate(legacySnapshot);
        assertEquals("مصنع الاختبار",snapshot.getJSONObject("profile").getString("business_name"));

        db=SecureDatabase.open(context,database);
        try {
            db.execSQL("UPDATE materials SET qty=99,value=99000 WHERE name='backup-test-material'");
            db.execSQL("DELETE FROM factory_customers");
        } finally { db.close(); }
        factory.edit().putString("business_name","اسم معدل بعد النسخة").commit();

        FactoryBackup.restore(context,snapshot);
        db=SecureDatabase.open(context,database);
        try {
            Cursor material=db.rawQuery("SELECT qty,value FROM materials WHERE name='backup-test-material'",null);
            try { assertTrue(material.moveToFirst());assertEquals(17,material.getLong(0));assertEquals(8500,material.getLong(1)); }
            finally { material.close(); }
            Cursor party=db.rawQuery("SELECT phone,iban FROM factory_customers WHERE name='Factory Test Customer'",null);
            try { assertTrue(party.moveToFirst());assertEquals("249900000010",party.getString(0));assertEquals("SD00000000000000000001",party.getString(1)); }
            finally { party.close(); }
            Cursor outbox=db.rawQuery("SELECT source_entry_ref,status,payload_version FROM factory_gl_outbox",null);
            try { assertTrue(outbox.moveToFirst());assertEquals("BACKUP-OUTBOX-1",outbox.getString(0));assertEquals("PENDING",outbox.getString(1));assertEquals(1,outbox.getInt(2)); }
            finally { outbox.close(); }
            Cursor outboxLines=db.rawQuery("SELECT COUNT(*) FROM factory_gl_outbox_lines",null);
            try { assertTrue(outboxLines.moveToFirst());assertEquals(2,outboxLines.getInt(0)); }
            finally { outboxLines.close(); }
            Cursor mapping=db.rawQuery("SELECT target_account_code,branch_code,currency_code FROM factory_gl_account_mappings WHERE source_account='الصندوق'",null);
            try { assertTrue(mapping.moveToFirst());assertEquals("1000",mapping.getString(0));assertEquals("MAIN",mapping.getString(1));assertEquals("SDG",mapping.getString(2)); }
            finally { mapping.close(); }
        } finally { db.close(); }

        assertEquals("مصنع الاختبار",factory.getString("business_name",""));
        assertEquals("مخبز الاختبار",bakery.getString("business_name",""));
        context.deleteDatabase(database);
        context.deleteDatabase(database+"-wal");
        context.deleteDatabase(database+"-shm");
        factory.edit().clear().commit();
        bakery.edit().clear().commit();
    }
}
