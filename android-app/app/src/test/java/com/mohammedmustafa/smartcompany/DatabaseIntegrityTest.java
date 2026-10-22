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
    public void enterpriseControlCenterFlagsEmptyAndUnbalancedJournalEntries() {
        MainActivity activity = new MainActivity();

        ContentValues empty = new ContentValues();
        empty.put("created_at", System.currentTimeMillis());
        empty.put("description", "قيد فارغ للاختبار");
        database.insertOrThrow("journal_entries", null, empty);

        ContentValues oneLineEntry = new ContentValues();
        oneLineEntry.put("created_at", System.currentTimeMillis());
        oneLineEntry.put("description", "قيد بسطر واحد للاختبار");
        long oneLineId = database.insertOrThrow("journal_entries", null, oneLineEntry);
        ContentValues loneLine = new ContentValues();
        loneLine.put("journal_entry_id", oneLineId);
        loneLine.put("account_id", accountId("1010"));
        loneLine.put("debit", 10.00);
        loneLine.put("credit", 0.00);
        database.insertOrThrow("journal_lines", null, loneLine);

        ContentValues balancedEntry = new ContentValues();
        balancedEntry.put("created_at", System.currentTimeMillis());
        balancedEntry.put("description", "قيد متوازن للاختبار");
        long balancedId = database.insertOrThrow("journal_entries", null, balancedEntry);
        ContentValues debit = new ContentValues();
        debit.put("journal_entry_id", balancedId);
        debit.put("account_id", accountId("1010"));
        debit.put("debit", 10.00);
        debit.put("credit", 0.00);
        database.insertOrThrow("journal_lines", null, debit);
        ContentValues credit = new ContentValues();
        credit.put("journal_entry_id", balancedId);
        credit.put("account_id", accountId("3000"));
        credit.put("debit", 0.00);
        credit.put("credit", 10.00);
        database.insertOrThrow("journal_lines", null, credit);

        assertEquals("Empty and one-line entries must be flagged; a balanced entry must not be flagged",
                2, activity.countInvalidJournalEntries(database));
    }

    @Test
    public void enterpriseControlCenterFlagsOrphanJournalLines() {
        MainActivity activity = new MainActivity();
        ContentValues orphan = new ContentValues();
        orphan.put("journal_entry_id", 999999L);
        orphan.put("account_id", 999999L);
        orphan.put("debit", 5.00);
        orphan.put("credit", 0.00);
        database.insertOrThrow("journal_lines", null, orphan);

        assertEquals("Journal lines without a valid entry or account must be flagged",
                1, activity.countOrphanJournalLines(database));
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
        assertTrue(tableExists("factory_orders"));
        assertTrue(tableExists("factory_stage_events"));
        assertTrue(tableExists("bread_counter_sales"));
        assertTrue(tableExists("bread_distribution_sales"));
        assertTrue(accountExists("1010"));
        assertTrue(accountExists("1100"));
        assertTrue(accountExists("2100"));
        assertTrue(accountExists("3100"));
        assertTrue(accountExists("4000"));
        assertTrue(accountExists("1210"));
        assertTrue(accountExists("1400"));
        assertTrue(accountExists("2200"));
        assertTrue(accountExists("2300"));
    }


    @Test
    public void distributorDiscountRegisterEnforcesCashDebtAndPriceDifferenceReconciliation() {
        assertTrue(tableExists("bread_distribution_sales"));
        ContentValues row = new ContentValues();
        row.put("transaction_id", 7001L);
        row.put("product_id", 1L);
        row.put("distributor_name", "مطعم تجريبي");
        row.put("qty", 90.0);
        row.put("retail_total", 30000.0);
        row.put("distribution_total", 28000.0);
        row.put("cash_received", 20000.0);
        row.put("debt_amount", 8000.0);
        row.put("price_difference", 2000.0);
        row.put("created_at", System.currentTimeMillis());
        database.insertOrThrow("bread_distribution_sales", null, row);
        Cursor c = database.rawQuery("SELECT cash_received+debt_amount,distribution_total,retail_total-distribution_total,price_difference FROM bread_distribution_sales WHERE transaction_id=7001", null);
        try {
            assertTrue(c.moveToFirst());
            assertEquals(28000.0, c.getDouble(0), 0.001);
            assertEquals(28000.0, c.getDouble(1), 0.001);
            assertEquals(2000.0, c.getDouble(2), 0.001);
            assertEquals(2000.0, c.getDouble(3), 0.001);
        } finally { c.close(); }
        ContentValues invalid = new ContentValues(row);
        invalid.put("transaction_id", 7002L);
        invalid.put("cash_received", 27000.0);
        invalid.put("debt_amount", 2000.0);
        try {
            database.insertOrThrow("bread_distribution_sales", null, invalid);
            fail("Cash plus debt must equal the distribution amount");
        } catch (android.database.SQLException expected) {
            // Database CHECK constraint prevents unreconciled distributor totals.
        }
    }

    @Test
    public void distributorPostingBalancesGrossRevenueDiscountCashDebtAndInventoryCost() {
        MainActivity activity = new MainActivity();
        database.beginTransaction();
        try {
            ContentValues tx = new ContentValues();
            tx.put("type", "SALE"); tx.put("party", "مطعم اختبار");
            tx.put("total", 28000.0); tx.put("paid", 20000.0); tx.put("remaining", 8000.0);
            tx.put("created_at", System.currentTimeMillis());
            long transactionId = database.insertOrThrow("transactions", null, tx);
            long journalId = activity.postBreadDistributionJournal(
                    database, transactionId, "مطعم اختبار", 30000.0, 28000.0,
                    20000.0, 8000.0, 2000.0, 90.0, 100.0);
            assertJournalBalanced(journalId);
            assertAccountTotals("1010", 20000.0, 0.0);
            assertAccountTotals("1100", 8000.0, 0.0);
            assertAccountTotals("4200", 2000.0, 0.0);
            assertAccountTotals("4000", 0.0, 30000.0);
            assertAccountTotals("5100", 9000.0, 0.0);
            assertAccountTotals("1200", 0.0, 9000.0);
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    @Test
    public void returnCanBeRecordedOnlyOnceForOriginalInvoice() {
        assertTrue(columnExists("transactions", "original_transaction_id"));
        assertTrue(accountExists("4100"));
        ContentValues original = new ContentValues();
        original.put("type", "SALE");
        original.put("party", "عميل اختبار");
        original.put("total", 100.0);
        original.put("paid", 100.0);
        original.put("remaining", 0.0);
        original.put("created_at", System.currentTimeMillis());
        original.put("invoice_no", "TEST-ORIGINAL-1");
        long originalId = database.insertOrThrow("transactions", null, original);

        ContentValues returned = new ContentValues();
        returned.put("type", "SALE_RETURN");
        returned.put("party", "عميل اختبار");
        returned.put("total", 100.0);
        returned.put("paid", 100.0);
        returned.put("remaining", 0.0);
        returned.put("created_at", System.currentTimeMillis());
        returned.put("original_transaction_id", originalId);
        database.insertOrThrow("transactions", null, returned);

        try {
            database.insertOrThrow("transactions", null, returned);
            fail("A second return for the same original invoice must be rejected");
        } catch (android.database.SQLException expected) {
            // The partial unique index enforces one full return per original invoice.
        }
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
    public void salesPurchasesAndExpensesPostBalancedAccountingEntries() {
        // Exercise the application's real posting routine, not only hand-built test rows.
        MainActivity activity = new MainActivity();
        long saleId;
        long purchaseId;
        long expenseId;
        database.beginTransaction();
        try {
            activity.postJournal(database, 101L, "SALE", 100.00, 60.00, 40.00, 30.00);
            activity.postJournal(database, 102L, "PURCHASE", 80.00, 20.00, 60.00, 0.00);
            activity.postJournal(database, 103L, "EXPENSE", 10.00, 10.00, 0.00, 0.00);
            saleId = journalIdForTransaction(101L);
            purchaseId = journalIdForTransaction(102L);
            expenseId = journalIdForTransaction(103L);

            assertJournalBalanced(saleId);
            assertJournalBalanced(purchaseId);
            assertJournalBalanced(expenseId);

            assertAccountTotals("1010", 60.00, 30.00);
            assertAccountTotals("1100", 40.00, 0.00);
            assertAccountTotals("1200", 80.00, 30.00);
            assertAccountTotals("2100", 0.00, 60.00);
            assertAccountTotals("4000", 0.00, 100.00);
            assertAccountTotals("5100", 30.00, 0.00);
            assertAccountTotals("6000", 10.00, 0.00);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    @Test
    public void partiallyUnpaidExpenseCreditsCashAndPayableAndBalances() {
        MainActivity activity = new MainActivity();
        database.beginTransaction();
        try {
            activity.postJournal(database, 104L, "EXPENSE", 25.00, 15.00, 10.00, 0.00);
            long journalId = journalIdForTransaction(104L);
            assertJournalBalanced(journalId);
            assertAccountTotals("6000", 25.00, 0.00);
            assertAccountTotals("1010", 0.00, 15.00);
            assertAccountTotals("2100", 0.00, 10.00);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    @Test
    public void journalPostingRejectsMismatchedAmountsBeforeWritingAnything() {
        MainActivity activity = new MainActivity();
        Cursor before = database.rawQuery("SELECT COUNT(*) FROM journal_entries", null);
        int countBefore;
        try { assertTrue(before.moveToFirst()); countBefore = before.getInt(0); }
        finally { before.close(); }

        try {
            activity.postJournal(database, 105L, "EXPENSE", 25.00, 15.00, 9.00, 0.00);
            fail("Mismatched total, paid and remaining amounts must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected: 25 != 15 + 9.
        }

        Cursor after = database.rawQuery("SELECT COUNT(*) FROM journal_entries", null);
        try {
            assertTrue(after.moveToFirst());
            assertEquals(countBefore, after.getInt(0));
        } finally { after.close(); }
    }

    @Test
    public void journalPostingRejectsNegativeAmounts() {
        MainActivity activity = new MainActivity();
        try {
            activity.postJournal(database, 106L, "EXPENSE", 10.00, 15.00, -5.00, 0.00);
            fail("Negative remaining amount must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    @Test
    public void journalPostingRejectsNonFiniteAmountsBeforeWritingAnything() {
        MainActivity activity = new MainActivity();
        int beforeCount = journalEntryCount();

        try {
            activity.postJournal(database, 107L, "EXPENSE", Double.NaN, 0.0, 0.0, 0.0);
            fail("NaN total must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        try {
            activity.postJournal(database, 108L, "EXPENSE", 10.0, Double.POSITIVE_INFINITY, 0.0, 0.0);
            fail("Infinite paid amount must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        try {
            activity.postJournal(database, 109L, "SALE", 10.0, 10.0, 0.0, Double.NaN);
            fail("NaN cost of goods sold must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        assertEquals("Invalid non-finite amounts must not create journal entries",
                beforeCount, journalEntryCount());
    }

    @Test
    public void journalPostingRejectsUnsupportedTransactionTypeBeforeWritingAnything() {
        MainActivity activity = new MainActivity();
        int beforeCount = journalEntryCount();

        try {
            activity.postJournal(database, 110L, "REFUND", 10.0, 10.0, 0.0, 0.0);
            fail("Unsupported journal type must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        assertEquals("Unsupported transaction type must not create journal entries",
                beforeCount, journalEntryCount());
    }

    @Test
    public void postingTheSameTransactionTwiceIsRejectedWithoutDuplicateLedgerRows() {
        MainActivity activity = new MainActivity();
        database.beginTransaction();
        try {
            activity.postJournal(database, 111L, "SALE", 100.00, 100.00, 0.00, 60.00);
            int afterFirstPost = journalEntryCount();

            try {
                activity.postJournal(database, 111L, "SALE", 100.00, 100.00, 0.00, 60.00);
                fail("A transaction must not be posted to the ledger twice");
            } catch (IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("already has a journal entry"));
            }

            assertEquals("Duplicate posting must not create another journal entry",
                    afterFirstPost, journalEntryCount());
            assertJournalBalanced(journalIdForTransaction(111L));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    private int journalEntryCount() {
        Cursor c = database.rawQuery("SELECT COUNT(*) FROM journal_entries", null);
        try {
            assertTrue(c.moveToFirst());
            return c.getInt(0);
        } finally {
            c.close();
        }
    }

    private long journalIdForTransaction(long transactionId) {
        Cursor c = database.rawQuery(
                "SELECT id FROM journal_entries WHERE transaction_id=? ORDER BY id DESC LIMIT 1",
                new String[]{String.valueOf(transactionId)});
        try {
            assertTrue("Journal should be created for transaction " + transactionId, c.moveToFirst());
            return c.getLong(0);
        } finally {
            c.close();
        }
    }

    private void assertJournalBalanced(long journalId) {
        Cursor c = database.rawQuery(
                "SELECT COALESCE(SUM(debit),0),COALESCE(SUM(credit),0) FROM journal_lines WHERE journal_entry_id=?",
                new String[]{String.valueOf(journalId)});
        try {
            assertTrue(c.moveToFirst());
            assertEquals("Journal entry must balance: " + journalId, c.getDouble(0), c.getDouble(1), 0.000001);
        } finally {
            c.close();
        }
    }

    private void assertAccountTotals(String code, double expectedDebit, double expectedCredit) {
        Cursor c = database.rawQuery(
                "SELECT COALESCE(SUM(l.debit),0),COALESCE(SUM(l.credit),0) " +
                "FROM journal_lines l JOIN accounts a ON a.id=l.account_id WHERE a.code=?",
                new String[]{code});
        try {
            assertTrue(c.moveToFirst());
            assertEquals("Debit total for account " + code, expectedDebit, c.getDouble(0), 0.000001);
            assertEquals("Credit total for account " + code, expectedCredit, c.getDouble(1), 0.000001);
        } finally {
            c.close();
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

    @Test
    public void mainAccountsQueriesReturnLedgerBalancesAndEntries() {
        long cash = accountId("1010");
        long sales = accountId("4000");
        ContentValues entry = new ContentValues();
        entry.put("created_at", System.currentTimeMillis());
        entry.put("description", "اختبار ربط الحسابات الرئيسية");
        long journalId = database.insertOrThrow("journal_entries", null, entry);

        ContentValues debit = new ContentValues();
        debit.put("journal_entry_id", journalId);
        debit.put("account_id", cash);
        debit.put("debit", 75.0);
        debit.put("credit", 0.0);
        database.insertOrThrow("journal_lines", null, debit);

        ContentValues credit = new ContentValues();
        credit.put("journal_entry_id", journalId);
        credit.put("account_id", sales);
        credit.put("debit", 0.0);
        credit.put("credit", 75.0);
        database.insertOrThrow("journal_lines", null, credit);

        Cursor accounts = database.rawQuery(
                "SELECT a.id,a.code,a.name,a.type,COALESCE(SUM(l.debit),0),COALESCE(SUM(l.credit),0) " +
                "FROM accounts a LEFT JOIN journal_lines l ON l.account_id=a.id " +
                "GROUP BY a.id,a.code,a.name,a.type ORDER BY a.code", null);
        boolean foundCash = false;
        try {
            while (accounts.moveToNext()) {
                if ("1010".equals(accounts.getString(1))) {
                    foundCash = true;
                    assertEquals(75.0, accounts.getDouble(4), 0.000001);
                    assertEquals(0.0, accounts.getDouble(5), 0.000001);
                }
            }
        } finally {
            accounts.close();
        }
        assertTrue("Main accounts screen must list the cash account", foundCash);

        Cursor ledger = database.rawQuery(
                "SELECT j.id,j.created_at,j.description,j.transaction_id,l.debit,l.credit " +
                "FROM journal_lines l JOIN journal_entries j ON j.id=l.journal_entry_id " +
                "WHERE l.account_id=? ORDER BY j.created_at DESC,j.id DESC,l.id DESC",
                new String[]{String.valueOf(cash)});
        try {
            assertTrue("Account ledger must show the posted journal", ledger.moveToFirst());
            assertEquals(journalId, ledger.getLong(0));
            assertEquals("اختبار ربط الحسابات الرئيسية", ledger.getString(2));
            assertEquals(75.0, ledger.getDouble(4), 0.000001);
        } finally {
            ledger.close();
        }

        Cursor totals = database.rawQuery(
                "SELECT COALESCE(SUM(debit),0),COALESCE(SUM(credit),0) FROM journal_lines", null);
        try {
            assertTrue(totals.moveToFirst());
            assertEquals(totals.getDouble(0), totals.getDouble(1), 0.000001);
        } finally {
            totals.close();
        }
    }

    @Test
    public void grossProfitReportOffsetsSalesReturns() {
        long now = System.currentTimeMillis();
        ContentValues sale = new ContentValues();
        sale.put("type", "SALE");
        sale.put("party", "عميل اختبار");
        sale.put("total", 100.0);
        sale.put("paid", 100.0);
        sale.put("remaining", 0.0);
        sale.put("created_at", now);
        long saleId = database.insertOrThrow("transactions", null, sale);

        ContentValues returned = new ContentValues();
        returned.put("type", "SALE_RETURN");
        returned.put("party", "عميل اختبار");
        returned.put("total", 100.0);
        returned.put("paid", 100.0);
        returned.put("remaining", 0.0);
        returned.put("created_at", now);
        returned.put("original_transaction_id", saleId);
        long returnId = database.insertOrThrow("transactions", null, returned);

        insertInvoiceItem(saleId, 100.0, 60.0);
        insertInvoiceItem(returnId, 100.0, 60.0);

        Cursor c = database.rawQuery(
                "SELECT COALESCE(SUM(CASE WHEN t.type='SALE_RETURN' " +
                "THEN -((i.price-i.cost)*i.qty-i.discount) " +
                "ELSE ((i.price-i.cost)*i.qty-i.discount) END),0) " +
                "FROM invoice_items i JOIN transactions t ON t.id=i.transaction_id " +
                "WHERE t.type IN ('SALE','SALE_RETURN') AND t.created_at BETWEEN ? AND ?",
                new String[]{String.valueOf(now - 1000), String.valueOf(now + 1000)});
        try {
            assertTrue(c.moveToFirst());
            assertEquals("A fully returned sale must not inflate gross profit", 0.0, c.getDouble(0), 0.000001);
        } finally {
            c.close();
        }
    }

    private void insertInvoiceItem(long transactionId, double price, double cost) {
        ContentValues item = new ContentValues();
        item.put("transaction_id", transactionId);
        item.put("product_id", 1L);
        item.put("qty", 1.0);
        item.put("price", price);
        item.put("cost", cost);
        item.put("discount", 0.0);
        database.insertOrThrow("invoice_items", null, item);
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
