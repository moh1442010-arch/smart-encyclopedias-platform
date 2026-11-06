package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SecureDatabaseMigrationTest {
    @Test public void migratesLegacyBakeryDatabaseAndHidesPlaintext() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="legacy_migration_"+System.currentTimeMillis()+".db";
        File file=context.getDatabasePath(name);
        if(file.exists())assertTrue(file.delete());
        SQLiteDatabase legacy=SQLiteDatabase.openOrCreateDatabase(file,null);
        legacy.execSQL("CREATE TABLE records(day TEXT,module TEXT,payload TEXT,updated_at INTEGER,PRIMARY KEY(day,module))");
        legacy.execSQL("INSERT INTO records(day,module,payload,updated_at) VALUES('2026-11-06','stock','SENSITIVE_LEDGER_VALUE',123)");
        legacy.close();

        byte[] key=SecureDatabase.passphrase(context,name);
        SQLiteDatabase encrypted=SQLiteDatabase.openDatabase(file.getAbsolutePath(),key,null,SQLiteDatabase.OPEN_READWRITE);
        Cursor c=encrypted.rawQuery("SELECT payload FROM records WHERE day='2026-11-06'",null);
        try { assertTrue(c.moveToFirst());assertEquals("SENSITIVE_LEDGER_VALUE",c.getString(0)); }
        finally { c.close();encrypted.close(); }

        byte[] header=new byte[16];
        FileInputStream input=new FileInputStream(file);
        try { assertEquals(16,input.read(header)); } finally { input.close(); }
        assertNotEquals("SQLite format 3\u0000",new String(header,StandardCharsets.US_ASCII));
        assertFalse(new File(file.getAbsolutePath()+".plaintext-migration").exists());
        assertFalse(new File(file.getAbsolutePath()+".cipher-new").exists());
        file.delete();
    }
}
