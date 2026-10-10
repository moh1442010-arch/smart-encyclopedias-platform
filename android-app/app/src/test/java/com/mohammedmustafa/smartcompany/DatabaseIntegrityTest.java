package com.mohammedmustafa.smartcompany;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DatabaseIntegrityTest {
    private Context context;
    private MainActivity.DB helper;
    private SQLiteDatabase database;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("smartcompany2.db");
        helper = new MainActivity.DB(context);
        database = helper.getWritableDatabase();
    }

    @After
    public void tearDown() {
        if (helper != null) helper.close();
        if (context != null) context.deleteDatabase("smartcompany2.db");
    }

    @Test
    public void createsCoreAccountingTablesAndDefaultAccounts() {
        assertTrue(tableExists("transactions"));
        assertTrue(tableExists("invoice_items"));
        assertTrue(tableExists("accounts"));
        assertTrue(tableExists("journal_entries"));
        assertTrue(tableExists("journal_lines"));
        assertTrue(tableExists("party_payments"));
        assertTrue(tableExists("checks"));
        assertTrue(tableExists("payroll_events"));
        assertTrue(accountExists("1010"));
        assertTrue(accountExists("1100"));
        assertTrue(accountExists("2100"));
        assertTrue(accountExists("3100"));
        assertTrue(accountExists("4000"));
    }

    @Test
    public void manualJournalDescriptionColumnIsWritable() {
        ContentValues entry = new ContentValues();
        entry.put("created_at", System.currentTimeMillis());
        entry.put("description", "اختبار آلي");
        long id = database.insertOrThrow("journal_entries", null, entry);

        Cursor c = database.rawQuery(
                "SELECT description FROM journal_entries WHERE id=?",
                new String[]{String.valueOf(id)});
        try {
            assertTrue(c.moveToFirst());
            assertEquals("اختبار آلي", c.getString(0));
        } finally {
            c.close();
        }
    }

    @Test
    public void journalLinesCanBeStoredAsBalancedEntry() {
        long cash = accountId("1010");
        long capital = accountId("3000");
        assertTrue(cash > 0);
        assertTrue(capital > 0);

        database.beginTransaction();
        try {
            ContentValues entry = new ContentValues();
            entry.put("created_at", System.currentTimeMillis());
            entry.put("description", "اختبار توازن القيد");
            long journalId = database.insertOrThrow("journal_entries", null, entry);

            ContentValues debit = new ContentValues();
            debit.put("journal_entry_id", journalId);
            debit.put("account_id", cash);
            debit.put("debit", 125.0);
            debit.put("credit", 0.0);
            database.insertOrThrow("journal_lines", null, debit);

            ContentValues credit = new ContentValues();
            credit.put("journal_entry_id", journalId);
            credit.put("account_id", capital);
            credit.put("debit", 0.0);
            credit.put("credit", 125.0);
            database.insertOrThrow("journal_lines", null, credit);

            Cursor c = database.rawQuery(
                    "SELECT COALESCE(SUM(debit),0), COALESCE(SUM(credit),0) " +
                    "FROM journal_lines WHERE journal_entry_id=?",
                    new String[]{String.valueOf(journalId)});
            try {
                assertTrue(c.moveToFirst());
                assertEquals(c.getDouble(0), c.getDouble(1), 0.000001);
                assertEquals(125.0, c.getDouble(0), 0.000001);
            } finally {
                c.close();
            }
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }


    @Test
    public void upgradeFromVersionEightPreservesRowsAndAddsNewColumns() {
        helper.close();
        context.deleteDatabase("smartcompany2.db");
        java.io.File file = context.getDatabasePath("smartcompany2.db");
        java.io.File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();

        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(file, null);
        old.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,qty REAL NOT NULL DEFAULT 0,sale_price REAL NOT NULL DEFAULT 0,cost REAL NOT NULL DEFAULT 0,reorder REAL NOT NULL DEFAULT 0)");
        old.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT)");
        old.execSQL("INSERT INTO customers(name) VALUES('عميل قديم')");
        old.execSQL("CREATE TABLE suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT)");
        old.execSQL("CREATE TABLE employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT)");
        old.execSQL("CREATE TABLE payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at INTEGER)");
        old.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,party TEXT,total REAL NOT NULL,paid REAL NOT NULL DEFAULT 0,remaining REAL NOT NULL DEFAULT 0,created_at INTEGER)");
        old.execSQL("CREATE TABLE journal_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER,created_at INTEGER)");
        old.execSQL("CREATE TABLE journal_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,journal_entry_id INTEGER,account_id INTEGER,debit REAL NOT NULL DEFAULT 0,credit REAL NOT NULL DEFAULT 0)");
        old.setVersion(8);
        old.close();

        helper = new MainActivity.DB(context);
        database = helper.getWritableDatabase();

        assertTrue(columnExists("customers", "credit_limit"));
        assertTrue(columnExists("transactions", "invoice_no"));
        assertTrue(columnExists("transactions", "customer_id"));
        assertTrue(columnExists("products", "barcode"));
        assertTrue(columnExists("journal_entries", "description"));
        Cursor preserved = database.rawQuery(
                "SELECT name FROM customers WHERE name='عميل قديم'", null);
        try {
            assertTrue("Existing customer row must survive schema migration", preserved.moveToFirst());
        } finally {
            preserved.close();
        }
    }

    private boolean columnExists(String table, String column) {
        Cursor c = database.rawQuery("PRAGMA table_info(" + table + ")", null);
        try {
            while (c.moveToNext()) {
                if (column.equals(c.getString(1))) return true;
            }
            return false;
        } finally {
            c.close();
        }
    }

    private boolean tableExists(String name) {
        Cursor c = database.rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{name});
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    private boolean accountExists(String code) {
        return accountId(code) > 0;
    }

    private long accountId(String code) {
        Cursor c = database.rawQuery(
                "SELECT id FROM accounts WHERE code=?",
                new String[]{code});
        try {
            return c.moveToFirst() ? c.getLong(0) : 0;
        } finally {
            c.close();
        }
    }
}
