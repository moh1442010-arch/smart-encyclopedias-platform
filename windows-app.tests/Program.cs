using Microsoft.Data.Sqlite;
using SmartCompany;

static class AccountingRegressionTests
{
    static void Assert(bool condition, string message)
    {
        if (!condition) throw new Exception("FAIL: " + message);
        Console.WriteLine("PASS: " + message);
    }

    static long ScalarLong(SqliteConnection c, string sql)
    {
        using var q = c.CreateCommand();
        q.CommandText = sql;
        return Convert.ToInt64(q.ExecuteScalar());
    }

    static void Main()
    {
        using var c = new SqliteConnection("Data Source=:memory:");
        c.Open();
        using (var schema = c.CreateCommand())
        {
            schema.CommandText = @"
CREATE TABLE gl_accounts(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT NOT NULL UNIQUE,name TEXT NOT NULL UNIQUE,kind TEXT NOT NULL);
CREATE TABLE gl_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,source_type TEXT NOT NULL,source_id INTEGER NOT NULL,memo TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,UNIQUE(source_type,source_id));
CREATE TABLE gl_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,entry_id INTEGER NOT NULL REFERENCES gl_entries(id),account_id INTEGER NOT NULL REFERENCES gl_accounts(id),debit REAL NOT NULL DEFAULT 0,credit REAL NOT NULL DEFAULT 0,CHECK(debit>=0 AND credit>=0 AND NOT(debit>0 AND credit>0)));
CREATE TABLE manual_journals(id INTEGER PRIMARY KEY AUTOINCREMENT,memo TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE gl_reversals(id INTEGER PRIMARY KEY AUTOINCREMENT,original_entry_id INTEGER NOT NULL UNIQUE REFERENCES gl_entries(id),reversal_entry_id INTEGER NOT NULL UNIQUE REFERENCES gl_entries(id),reason TEXT NOT NULL,created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP);
INSERT INTO gl_accounts(code,name,kind) VALUES('1000','Cash','ASSET'),('4000','Sales Revenue','REVENUE'),('6100','Depreciation Expense','EXPENSE'),('1500','Fixed Assets','ASSET'),('1590','Accumulated Depreciation','CONTRA_ASSET'),('1400','Work in Process','ASSET'),('1210','Raw Materials','ASSET'),('2200','Salaries Payable','LIABILITY'),('2300','Manufacturing Overhead Payable','LIABILITY'),('6400','Operating Expense','EXPENSE');
CREATE TABLE recurring_expense_runs(id INTEGER PRIMARY KEY AUTOINCREMENT,recurring_id INTEGER NOT NULL,period_key TEXT NOT NULL,expense_id INTEGER NOT NULL,UNIQUE(recurring_id,period_key));
";
            schema.ExecuteNonQuery();
        }

        long manualId;
        using (var tx = c.BeginTransaction())
        {
            using var insert = c.CreateCommand();
            insert.Transaction = tx;
            insert.CommandText = "INSERT INTO manual_journals(memo) VALUES('Test manual journal')";
            insert.ExecuteNonQuery();
            manualId = JournalAccounting.LastId(c, tx);
            JournalAccounting.PostEntry(c, tx, "MANUAL_JOURNAL", manualId, "Test manual journal",
                ("Cash", 125.50, 0d), ("Sales Revenue", 0d, 125.50));
            tx.Commit();
        }

        var originalId = ScalarLong(c, "SELECT id FROM gl_entries WHERE source_type='MANUAL_JOURNAL'");
        long reversalId;
        using (var tx = c.BeginTransaction())
        {
            reversalId = JournalAccounting.ReverseManualJournal(c, tx, originalId, "Regression test reversal");
            tx.Commit();
        }

        Assert(ScalarLong(c, "SELECT COUNT(*) FROM gl_entries WHERE id=" + originalId) == 1,
            "original journal remains present");
        Assert(ScalarLong(c, "SELECT COUNT(*) FROM gl_reversals WHERE original_entry_id=" + originalId + " AND reversal_entry_id=" + reversalId + " AND reason='Regression test reversal'") == 1,
            "reversal audit relationship and reason are recorded");
        Assert(Math.Abs(Convert.ToDouble(ScalarLong(c, "SELECT COUNT(*) FROM gl_lines WHERE entry_id=" + reversalId)) - 2) < 0.001,
            "reversal contains two lines");

        using (var q = c.CreateCommand())
        {
            q.CommandText = @"SELECT a.name,l.debit,l.credit FROM gl_lines l JOIN gl_accounts a ON a.id=l.account_id WHERE l.entry_id=$id ORDER BY l.id";
            q.Parameters.AddWithValue("$id", reversalId);
            using var r = q.ExecuteReader();
            Assert(r.Read() && r.GetString(0) == "Cash" && Math.Abs(r.GetDouble(1)) < 0.001 && Math.Abs(r.GetDouble(2)-125.50) < 0.001,
                "reversal swaps original debit to credit");
            Assert(r.Read() && r.GetString(0) == "Sales Revenue" && Math.Abs(r.GetDouble(1)-125.50) < 0.001 && Math.Abs(r.GetDouble(2)) < 0.001,
                "reversal swaps original credit to debit");
        }

        var beforeDuplicate = ScalarLong(c, "SELECT COUNT(*) FROM gl_entries");
        var duplicateRejected = false;
        using (var tx = c.BeginTransaction())
        {
            try { JournalAccounting.ReverseManualJournal(c, tx, originalId, "Duplicate attempt"); }
            catch (InvalidOperationException) { duplicateRejected = true; }
            finally { tx.Rollback(); }
        }
        Assert(duplicateRejected && ScalarLong(c, "SELECT COUNT(*) FROM gl_entries") == beforeDuplicate,
            "duplicate reversal is rejected without extra entries");

        var beforeBadEntry = ScalarLong(c, "SELECT COUNT(*) FROM gl_entries");
        var unbalancedRejected = false;
        using (var tx = c.BeginTransaction())
        {
            try { JournalAccounting.PostEntry(c, tx, "TEST", 999, "Unbalanced", ("Cash", 10d, 0d), ("Sales Revenue", 0d, 9d)); }
            catch (InvalidOperationException) { unbalancedRejected = true; }
            finally { tx.Rollback(); }
        }
        Assert(unbalancedRejected && ScalarLong(c, "SELECT COUNT(*) FROM gl_entries") == beforeBadEntry,
            "unbalanced journal is rejected before it is posted");

        using (var tx = c.BeginTransaction())
        {
            JournalAccounting.PostEntry(c, tx, "SALE", 77, "Operational sale", ("Cash", 20d, 0d), ("Sales Revenue", 0d, 20d));
            tx.Commit();
        }
        var saleId = ScalarLong(c, "SELECT id FROM gl_entries WHERE source_type='SALE'");
        var operationalRejected = false;
        using (var tx = c.BeginTransaction())
        {
            try { JournalAccounting.ReverseManualJournal(c, tx, saleId, "Should not reverse subledger operation"); }
            catch (InvalidOperationException) { operationalRejected = true; }
            finally { tx.Rollback(); }
        }
        Assert(operationalRejected, "operational entries cannot be reversed through manual-journal workflow");

        Assert(Math.Abs(Convert.ToDouble(ScalarLong(c, "SELECT COUNT(*) FROM gl_reversals"))) == 1,
            "only one reversal audit record exists");
        using (var tx = c.BeginTransaction())
        {
            JournalAccounting.PostEntry(c, tx, "FIXED_ASSET", 1001, "Asset acquisition",
                ("Fixed Assets", 1200d, 0d), ("Cash", 0d, 1200d));
            JournalAccounting.PostEntry(c, tx, "DEPRECIATION", 1002, "Monthly depreciation",
                ("Depreciation Expense", 100d, 0d), ("Accumulated Depreciation", 0d, 100d));
            JournalAccounting.PostEntry(c, tx, "FACTORY_STAGE", 1003, "Factory stage cost",
                ("Work in Process", 500d, 0d), ("Raw Materials", 0d, 300d),
                ("Salaries Payable", 0d, 100d), ("Manufacturing Overhead Payable", 0d, 100d));
            tx.Commit();
        }
        Assert(ScalarLong(c, "SELECT COUNT(*) FROM gl_entries WHERE source_type IN ('FIXED_ASSET','DEPRECIATION','FACTORY_STAGE')") == 3,
            "fixed asset acquisition, depreciation and factory WIP costs post to the ledger");
        Assert(ScalarLong(c, "SELECT COUNT(*) FROM (SELECT e.id FROM gl_entries e JOIN gl_lines l ON l.entry_id=e.id WHERE e.source_type IN ('FIXED_ASSET','DEPRECIATION','FACTORY_STAGE') GROUP BY e.id HAVING ABS(SUM(l.debit)-SUM(l.credit))>0.005)") == 0,
            "new fixed asset, depreciation and factory stage entries balance");
        using (var tx = c.BeginTransaction())
        {
            using var q = c.CreateCommand(); q.Transaction = tx;
            q.CommandText = "INSERT INTO recurring_expense_runs(recurring_id,period_key,expense_id) VALUES(1,'2026-10',2001)";
            q.ExecuteNonQuery(); tx.Commit();
        }
        var duplicatePeriodRejected = false;
        using (var tx = c.BeginTransaction())
        {
            try { using var q = c.CreateCommand(); q.Transaction = tx; q.CommandText = "INSERT INTO recurring_expense_runs(recurring_id,period_key,expense_id) VALUES(1,'2026-10',2002)"; q.ExecuteNonQuery(); }
            catch (SqliteException) { duplicatePeriodRejected = true; }
            finally { tx.Rollback(); }
        }
        Assert(duplicatePeriodRejected, "recurring expenses cannot be generated twice for the same definition and period");
        Console.WriteLine("ALL JOURNAL REGRESSION TESTS PASSED");
    }
}
