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
INSERT INTO gl_accounts(code,name,kind) VALUES('1000','Cash','ASSET'),('4000','Sales Revenue','REVENUE'),('6100','Operating Expense','EXPENSE');
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
        RunRecurringEntryRegressionTests(c);
        Console.WriteLine("ALL JOURNAL AND RECURRING ENTRY REGRESSION TESTS PASSED");
    }

    static void RunRecurringEntryRegressionTests(SqliteConnection c)
    {
        RecurringEntryService.EnsureSchema(c);
        long fixedSchedule;
        long fixedOccurrence;
        using (var tx = c.BeginTransaction())
        {
            fixedSchedule = RecurringEntryService.CreateSchedule(c, tx, "Monthly rent", "Operating Expense", "Cash", 500d, false, 31, "2027-01");
            fixedOccurrence = RecurringEntryService.EnsureOccurrence(c, tx, fixedSchedule, "2027-02");
            var duplicateId = RecurringEntryService.EnsureOccurrence(c, tx, fixedSchedule, "2027-02");
            Assert(fixedOccurrence == duplicateId, "same schedule/month resolves to one recurring occurrence");
            using var due = c.CreateCommand();
            due.Transaction = tx;
            due.CommandText = "SELECT due_date,amount,state FROM recurring_occurrences WHERE id=$id";
            due.Parameters.AddWithValue("$id", fixedOccurrence);
            using var r = due.ExecuteReader();
            Assert(r.Read() && r.GetString(0) == "2027-02-28" && Math.Abs(r.GetDouble(1)-500d)<0.000001 && r.GetString(2)=="DRAFT",
                "fixed schedule creates a reviewable draft and clamps day 31 to February's last day");
            tx.Commit();
        }
        long fixedJournal;
        using (var tx = c.BeginTransaction())
        {
            fixedJournal = RecurringEntryService.PostOccurrence(c, tx, fixedOccurrence);
            tx.Commit();
        }
        Assert(ScalarLong(c, "SELECT COUNT(*) FROM gl_entries WHERE id=" + fixedJournal + " AND source_type='RECURRING_ENTRY'") == 1,
            "posting recurring expense creates one linked journal");
        var duplicatePostRejected = false;
        using (var tx = c.BeginTransaction())
        {
            try { RecurringEntryService.PostOccurrence(c, tx, fixedOccurrence); }
            catch (InvalidOperationException) { duplicatePostRejected = true; }
            finally { tx.Rollback(); }
        }
        Assert(duplicatePostRejected && ScalarLong(c, "SELECT COUNT(*) FROM gl_entries WHERE source_type='RECURRING_ENTRY' AND source_id=" + fixedOccurrence) == 1,
            "reposting an already-posted occurrence is rejected without duplicate journal");

        long variableSchedule;
        long variableOccurrence;
        using (var tx = c.BeginTransaction())
        {
            variableSchedule = RecurringEntryService.CreateSchedule(c, tx, "Electricity bill", "Operating Expense", "Cash", null, true, 15, "2027-01");
            variableOccurrence = RecurringEntryService.EnsureOccurrence(c, tx, variableSchedule, "2027-02");
            tx.Commit();
        }
        var missingAmountRejected = false;
        using (var tx = c.BeginTransaction())
        {
            try { RecurringEntryService.PostOccurrence(c, tx, variableOccurrence); }
            catch (InvalidOperationException) { missingAmountRejected = true; }
            finally { tx.Rollback(); }
        }
        Assert(missingAmountRejected && ScalarLong(c, "SELECT COUNT(*) FROM gl_entries WHERE source_type='RECURRING_ENTRY' AND source_id=" + variableOccurrence) == 0,
            "variable utility bill cannot post without a confirmed amount");
        using (var tx = c.BeginTransaction())
        {
            RecurringEntryService.PostOccurrence(c, tx, variableOccurrence, 42.75d);
            tx.Commit();
        }
        using (var q = c.CreateCommand())
        {
            q.CommandText = "SELECT state,amount FROM recurring_occurrences WHERE id=$id";
            q.Parameters.AddWithValue("$id", variableOccurrence);
            using var r = q.ExecuteReader();
            Assert(r.Read() && r.GetString(0)=="POSTED" && Math.Abs(r.GetDouble(1)-42.75d)<0.000001,
                "confirmed variable bill posts its actual amount and marks the occurrence posted");
        }
    }
}
