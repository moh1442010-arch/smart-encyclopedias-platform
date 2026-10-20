using Microsoft.Data.Sqlite;

namespace SmartCompany;

/// <summary>
/// Shared accounting journal rules used by the Windows UI and automated regression tests.
/// </summary>
public static class JournalAccounting
{
    public static long LastId(SqliteConnection c, SqliteTransaction tx)
    {
        using var q = c.CreateCommand();
        q.Transaction = tx;
        q.CommandText = "SELECT last_insert_rowid()";
        return Convert.ToInt64(q.ExecuteScalar());
    }

    public static void PostEntry(SqliteConnection c, SqliteTransaction tx, string sourceType, long sourceId, string memo,
        params (string Account, double Debit, double Credit)[] lines)
    {
        if (string.IsNullOrWhiteSpace(sourceType) || string.IsNullOrWhiteSpace(memo))
            throw new InvalidOperationException("Journal source and memo are required.");
        var debit = lines.Sum(x => x.Debit);
        var credit = lines.Sum(x => x.Credit);
        if (lines.Length < 2 || lines.Any(x => !double.IsFinite(x.Debit) || !double.IsFinite(x.Credit) ||
            x.Debit < 0 || x.Credit < 0 || (x.Debit > 0 && x.Credit > 0)) ||
            Math.Abs(debit - credit) > 0.005 || debit <= 0)
            throw new InvalidOperationException("Unbalanced or invalid journal entry.");

        using var header = c.CreateCommand();
        header.Transaction = tx;
        header.CommandText = "INSERT INTO gl_entries(source_type,source_id,memo) VALUES($s,$id,$m)";
        header.Parameters.AddWithValue("$s", sourceType);
        header.Parameters.AddWithValue("$id", sourceId);
        header.Parameters.AddWithValue("$m", memo.Trim());
        header.ExecuteNonQuery();
        var entryId = LastId(c, tx);

        foreach (var line in lines)
        {
            using var account = c.CreateCommand();
            account.Transaction = tx;
            account.CommandText = "SELECT id FROM gl_accounts WHERE name=$n";
            account.Parameters.AddWithValue("$n", line.Account);
            var accountId = Convert.ToInt64(account.ExecuteScalar() ?? throw new InvalidOperationException("Ledger account missing."));
            using var detail = c.CreateCommand();
            detail.Transaction = tx;
            detail.CommandText = "INSERT INTO gl_lines(entry_id,account_id,debit,credit) VALUES($e,$a,$d,$c)";
            detail.Parameters.AddWithValue("$e", entryId);
            detail.Parameters.AddWithValue("$a", accountId);
            detail.Parameters.AddWithValue("$d", line.Debit);
            detail.Parameters.AddWithValue("$c", line.Credit);
            detail.ExecuteNonQuery();
        }

        using var audit = c.CreateCommand();
        audit.Transaction = tx;
        audit.CommandText = @"INSERT INTO audit_events(event_type,entity_type,entity_id,actor_label,details)
VALUES('JOURNAL_POSTED','journal_entry',$entry,'local-session-unattributed',$details)";
        audit.Parameters.AddWithValue("$entry", entryId.ToString(System.Globalization.CultureInfo.InvariantCulture));
        audit.Parameters.AddWithValue("$details", $"source_type={sourceType};source_id={sourceId};debit_total={debit:R};credit_total={credit:R}");
        audit.ExecuteNonQuery();
    }

    public static long ReverseManualJournal(SqliteConnection c, SqliteTransaction tx, long originalEntryId, string reason)
    {
        if (string.IsNullOrWhiteSpace(reason))
            throw new InvalidOperationException("A reversal reason is required.");

        using (var already = c.CreateCommand())
        {
            already.Transaction = tx;
            already.CommandText = "SELECT COUNT(*) FROM gl_reversals WHERE original_entry_id=$id";
            already.Parameters.AddWithValue("$id", originalEntryId);
            if (Convert.ToInt32(already.ExecuteScalar()) != 0)
                throw new InvalidOperationException("This journal entry has already been reversed.");
        }

        string memo;
        using (var header = c.CreateCommand())
        {
            header.Transaction = tx;
            header.CommandText = @"SELECT e.memo FROM gl_entries e
JOIN manual_journals m ON m.id=e.source_id
WHERE e.id=$id AND e.source_type='MANUAL_JOURNAL'";
            header.Parameters.AddWithValue("$id", originalEntryId);
            var value = header.ExecuteScalar();
            if (value is null || value is DBNull)
                throw new InvalidOperationException("Only an existing manual journal can be reversed here.");
            memo = Convert.ToString(value)!;
        }

        var reversedLines = new List<(string Account, double Debit, double Credit)>();
        using (var q = c.CreateCommand())
        {
            q.Transaction = tx;
            q.CommandText = @"SELECT a.name,l.debit,l.credit
FROM gl_lines l JOIN gl_accounts a ON a.id=l.account_id
WHERE l.entry_id=$id ORDER BY l.id";
            q.Parameters.AddWithValue("$id", originalEntryId);
            using var r = q.ExecuteReader();
            while (r.Read()) reversedLines.Add((r.GetString(0), r.GetDouble(2), r.GetDouble(1)));
        }

        if (reversedLines.Count < 2 ||
            reversedLines.Any(x => !double.IsFinite(x.Debit) || !double.IsFinite(x.Credit)) ||
            Math.Abs(reversedLines.Sum(x => x.Debit) - reversedLines.Sum(x => x.Credit)) > 0.005 ||
            reversedLines.Sum(x => x.Debit) <= 0)
            throw new InvalidOperationException("The original journal is unbalanced or invalid.");

        PostEntry(c, tx, "REVERSAL", originalEntryId,
            "Reverse journal #" + originalEntryId + ": " + reason.Trim() + " | Original: " + memo,
            reversedLines.ToArray());
        long reversalEntryId;
        using (var findReversal = c.CreateCommand())
        {
            findReversal.Transaction = tx;
            findReversal.CommandText = "SELECT id FROM gl_entries WHERE source_type='REVERSAL' AND source_id=$id";
            findReversal.Parameters.AddWithValue("$id", originalEntryId);
            var value = findReversal.ExecuteScalar();
            if (value is null || value is DBNull)
                throw new InvalidOperationException("The reversal journal entry was not created.");
            reversalEntryId = Convert.ToInt64(value);
        }

        using (var audit = c.CreateCommand())
        {
            audit.Transaction = tx;
            audit.CommandText = "INSERT INTO gl_reversals(original_entry_id,reversal_entry_id,reason) VALUES($o,$r,$reason)";
            audit.Parameters.AddWithValue("$o", originalEntryId);
            audit.Parameters.AddWithValue("$r", reversalEntryId);
            audit.Parameters.AddWithValue("$reason", reason.Trim());
            audit.ExecuteNonQuery();
        }
        return reversalEntryId;
    }
}
