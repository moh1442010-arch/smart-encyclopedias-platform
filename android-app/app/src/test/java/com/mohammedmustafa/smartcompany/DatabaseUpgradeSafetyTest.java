package com.mohammedmustafa.smartcompany;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DatabaseUpgradeSafetyTest {
    @Test
    public void duplicateLegacyValuesDoNotAbortUniqueIndexMigration() {
        SQLiteDatabase legacy = SQLiteDatabase.create(null);
        legacy.execSQL("CREATE TABLE transactions (original_transaction_id INTEGER, invoice_no TEXT)");
        legacy.execSQL("INSERT INTO transactions(original_transaction_id, invoice_no) VALUES (7, 'INV-1')");
        legacy.execSQL("INSERT INTO transactions(original_transaction_id, invoice_no) VALUES (7, 'INV-1')");

        MainActivity.DB helper = new MainActivity.DB(RuntimeEnvironment.getApplication());
        helper.safeUniqueIndex(legacy,
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_test_return ON transactions(original_transaction_id) WHERE original_transaction_id IS NOT NULL",
            "idx_test_return");
        helper.safeUniqueIndex(legacy,
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_test_invoice ON transactions(invoice_no) WHERE invoice_no IS NOT NULL AND invoice_no != ''",
            "idx_test_invoice");

        Cursor count = legacy.rawQuery("SELECT COUNT(*) FROM transactions", null);
        try {
            assertEquals(2, count.moveToFirst() ? count.getInt(0) : 0);
        } finally {
            count.close();
        }

        Cursor indexes = legacy.rawQuery("SELECT name FROM sqlite_master WHERE type='index' AND name IN ('idx_test_return','idx_test_invoice')", null);
        try {
            assertFalse(indexes.moveToFirst());
        } finally {
            indexes.close();
            legacy.close();
            helper.close();
        }
    }
}
