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
        } finally { db.close(); }

        JSONObject snapshot=FactoryBackup.export(context);
        FactoryBackup.validate(snapshot);
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
