using Microsoft.Data.Sqlite;

namespace SmartCompany;

/// <summary>
/// Transaction-safe recurring expense engine. It creates one occurrence per schedule/month;
/// callers must pass the caller's transaction so journal and occurrence state commit together.
/// UI defaults to draft/review; variable bills require a confirmed amount.
/// </summary>
public static class RecurringEntryService
{
    public static void EnsureSchema(SqliteConnection c, SqliteTransaction? tx = null)
    {
        using var q = c.CreateCommand();
        q.Transaction = tx;
        q.CommandText = @"
CREATE TABLE IF NOT EXISTS recurring_schedules(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 name TEXT NOT NULL,
 debit_account TEXT NOT NULL,
 credit_account TEXT NOT NULL,
 amount REAL,
 amount_mode TEXT NOT NULL CHECK(amount_mode IN ('FIXED','VARIABLE')),
 due_day INTEGER NOT NULL CHECK(due_day BETWEEN 1 AND 31),
 start_month TEXT NOT NULL,
 end_month TEXT,
 active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)),
 created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK((amount_mode='FIXED' AND amount IS NOT NULL AND amount>0) OR amount_mode='VARIABLE')
);
CREATE TABLE IF NOT EXISTS recurring_occurrences(
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 schedule_id INTEGER NOT NULL REFERENCES recurring_schedules(id),
 period_month TEXT NOT NULL,
 due_date TEXT NOT NULL,
 amount REAL,
 state TEXT NOT NULL DEFAULT 'DRAFT' CHECK(state IN ('DRAFT','POSTED','CANCELLED')),
 journal_entry_id INTEGER UNIQUE,
 created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(schedule_id,period_month),
 CHECK(amount IS NULL OR amount>0)
);";
        q.ExecuteNonQuery();
    }

    public static long CreateSchedule(SqliteConnection c, SqliteTransaction tx, string name,
        string debitAccount, string creditAccount, double? amount, bool variableAmount,
        int dueDay, string startMonth, string? endMonth = null)
    {
        if (string.IsNullOrWhiteSpace(name) || string.IsNullOrWhiteSpace(debitAccount) ||
            string.IsNullOrWhiteSpace(creditAccount) || debitAccount == creditAccount)
            throw new InvalidOperationException("A name and two different accounts are required.");
        if (dueDay is < 1 or > 31) throw new InvalidOperationException("Due day must be between 1 and 31.");
        ValidateMonth(startMonth);
        if (endMonth is not null)
        {
            ValidateMonth(endMonth);
            if (string.CompareOrdinal(endMonth, startMonth) < 0)
                throw new InvalidOperationException("End month cannot precede start month.");
        }
        if (variableAmount ? amount is not null : amount is null || !double.IsFinite(amount.Value) || amount.Value <= 0)
            throw new InvalidOperationException("Fixed schedules need a positive amount; variable schedules must not store an assumed amount.");

        using var q = c.CreateCommand();
        q.Transaction = tx;
        q.CommandText = @"INSERT INTO recurring_schedules(name,debit_account,credit_account,amount,amount_mode,due_day,start_month,end_month)
VALUES($name,$debit,$credit,$amount,$mode,$day,$start,$end); SELECT last_insert_rowid();";
        q.Parameters.AddWithValue("$name", name.Trim());
        q.Parameters.AddWithValue("$debit", debitAccount.Trim());
        q.Parameters.AddWithValue("$credit", creditAccount.Trim());
        q.Parameters.AddWithValue("$amount", amount is null ? DBNull.Value : amount.Value);
        q.Parameters.AddWithValue("$mode", variableAmount ? "VARIABLE" : "FIXED");
        q.Parameters.AddWithValue("$day", dueDay);
        q.Parameters.AddWithValue("$start", startMonth);
        q.Parameters.AddWithValue("$end", endMonth is null ? DBNull.Value : endMonth);
        return Convert.ToInt64(q.ExecuteScalar());
    }

    public static long EnsureOccurrence(SqliteConnection c, SqliteTransaction tx, long scheduleId, string periodMonth)
    {
        ValidateMonth(periodMonth);
        string name; string mode; double? fixedAmount; string debit; string credit; int day; string start; string? end; bool active;
        using (var q = c.CreateCommand())
        {
            q.Transaction = tx;
            q.CommandText = @"SELECT name,amount_mode,amount,debit_account,credit_account,due_day,start_month,end_month,active
FROM recurring_schedules WHERE id=$id";
            q.Parameters.AddWithValue("$id", scheduleId);
            using var r = q.ExecuteReader();
            if (!r.Read()) throw new InvalidOperationException("Recurring schedule not found.");
            name = r.GetString(0); mode = r.GetString(1); fixedAmount = r.IsDBNull(2) ? null : r.GetDouble(2);
            debit = r.GetString(3); credit = r.GetString(4); day = r.GetInt32(5); start = r.GetString(6);
            end = r.IsDBNull(7) ? null : r.GetString(7); active = r.GetInt64(8) == 1;
        }
        if (!active || string.CompareOrdinal(periodMonth, start) < 0 || (end is not null && string.CompareOrdinal(periodMonth, end) > 0))
            throw new InvalidOperationException("Schedule is inactive or outside its effective dates.");
        var date = DueDate(periodMonth, day);
        using (var insert = c.CreateCommand())
        {
            insert.Transaction = tx;
            insert.CommandText = @"INSERT OR IGNORE INTO recurring_occurrences(schedule_id,period_month,due_date,amount,state)
VALUES($id,$period,$date,$amount,'DRAFT')";
            insert.Parameters.AddWithValue("$id", scheduleId);
            insert.Parameters.AddWithValue("$period", periodMonth);
            insert.Parameters.AddWithValue("$date", date);
            insert.Parameters.AddWithValue("$amount", mode == "FIXED" ? fixedAmount!.Value : DBNull.Value);
            insert.ExecuteNonQuery();
        }
        using var find = c.CreateCommand();
        find.Transaction = tx;
        find.CommandText = "SELECT id FROM recurring_occurrences WHERE schedule_id=$id AND period_month=$period";
        find.Parameters.AddWithValue("$id", scheduleId);
        find.Parameters.AddWithValue("$period", periodMonth);
        return Convert.ToInt64(find.ExecuteScalar() ?? throw new InvalidOperationException("Occurrence was not created."));
    }

    public static long PostOccurrence(SqliteConnection c, SqliteTransaction tx, long occurrenceId, double? confirmedAmount = null)
    {
        long scheduleId; string period; string dueDate; string state; double? occurrenceAmount;
        using (var q = c.CreateCommand())
        {
            q.Transaction = tx;
            q.CommandText = @"SELECT schedule_id,period_month,due_date,state,amount FROM recurring_occurrences WHERE id=$id";
            q.Parameters.AddWithValue("$id", occurrenceId);
            using var r = q.ExecuteReader();
            if (!r.Read()) throw new InvalidOperationException("Recurring occurrence not found.");
            scheduleId = r.GetInt64(0); period = r.GetString(1); dueDate = r.GetString(2); state = r.GetString(3);
            occurrenceAmount = r.IsDBNull(4) ? null : r.GetDouble(4);
        }
        if (state != "DRAFT") throw new InvalidOperationException("Only a draft occurrence can be posted.");
        string name; string debit; string credit; string mode; double? configuredAmount;
        using (var q = c.CreateCommand())
        {
            q.Transaction = tx;
            q.CommandText = "SELECT name,debit_account,credit_account,amount_mode,amount FROM recurring_schedules WHERE id=$id";
            q.Parameters.AddWithValue("$id", scheduleId);
            using var r = q.ExecuteReader();
            if (!r.Read()) throw new InvalidOperationException("Recurring schedule not found.");
            name = r.GetString(0); debit = r.GetString(1); credit = r.GetString(2); mode = r.GetString(3);
            configuredAmount = r.IsDBNull(4) ? null : r.GetDouble(4);
        }
        double amount;
        if (mode == "VARIABLE")
        {
            if (confirmedAmount is null || !double.IsFinite(confirmedAmount.Value) || confirmedAmount.Value <= 0)
                throw new InvalidOperationException("A variable bill requires a confirmed positive amount.");
            amount = confirmedAmount.Value;
        }
        else
        {
            amount = configuredAmount ?? throw new InvalidOperationException("Fixed schedule amount is missing.");
            if (confirmedAmount is not null && Math.Abs(confirmedAmount.Value - amount) > 0.000001)
                throw new InvalidOperationException("Confirmed amount does not match the fixed schedule.");
        }

        // PostEntry's unique (source_type, source_id) plus the occurrence's state check makes retries safe.
        JournalAccounting.PostEntry(c, tx, "RECURRING_ENTRY", occurrenceId,
            $"Recurring: {name} | {period} | due {dueDate}", (debit, amount, 0d), (credit, 0d, amount));
        long journalId;
        using (var find = c.CreateCommand())
        {
            find.Transaction = tx;
            find.CommandText = "SELECT id FROM gl_entries WHERE source_type='RECURRING_ENTRY' AND source_id=$id";
            find.Parameters.AddWithValue("$id", occurrenceId);
            journalId = Convert.ToInt64(find.ExecuteScalar() ?? throw new InvalidOperationException("Journal entry was not created."));
        }
        using (var update = c.CreateCommand())
        {
            update.Transaction = tx;
            update.CommandText = "UPDATE recurring_occurrences SET amount=$amount,state='POSTED',journal_entry_id=$journal WHERE id=$id AND state='DRAFT'";
            update.Parameters.AddWithValue("$amount", amount);
            update.Parameters.AddWithValue("$journal", journalId);
            update.Parameters.AddWithValue("$id", occurrenceId);
            if (update.ExecuteNonQuery() != 1) throw new InvalidOperationException("Occurrence state changed; posting was cancelled.");
        }
        return journalId;
    }

    public static string DueDate(string periodMonth, int dueDay)
    {
        ValidateMonth(periodMonth);
        if (dueDay is < 1 or > 31) throw new InvalidOperationException("Due day must be between 1 and 31.");
        var year = int.Parse(periodMonth[..4], System.Globalization.CultureInfo.InvariantCulture);
        var month = int.Parse(periodMonth[5..7], System.Globalization.CultureInfo.InvariantCulture);
        return new DateOnly(year, month, Math.Min(dueDay, DateTime.DaysInMonth(year, month))).ToString("yyyy-MM-dd");
    }

    private static void ValidateMonth(string value)
    {
        if (value is null || value.Length != 7 || value[4] != '-' ||
            !DateOnly.TryParseExact(value + "-01", "yyyy-MM-dd", System.Globalization.CultureInfo.InvariantCulture,
                System.Globalization.DateTimeStyles.None, out _))
            throw new InvalidOperationException("Month must use YYYY-MM format.");
    }
}