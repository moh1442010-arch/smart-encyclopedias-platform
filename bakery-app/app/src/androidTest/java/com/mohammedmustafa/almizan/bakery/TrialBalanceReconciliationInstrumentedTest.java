package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TrialBalanceReconciliationInstrumentedTest {
    private SQLiteDatabase openLedger(Context context, String name) {
        SQLiteDatabase db = SecureDatabase.open(context, name);
        EnterpriseSchema.ensure(db);
        db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('TB-2026-11','2026-11-01','2026-11-30','OPEN')");
        return db;
    }

    private GeneralLedger.Entry entry(String ref, String sourceType, String sourceId,
                                      String date, long cashDebit, long cashCredit,
                                      long salesDebit, long salesCredit) {
        return new GeneralLedger.Entry(ref, "Trial balance fixture " + ref, date,
            sourceType, sourceId, "tb:" + ref, "TEST",
            Arrays.asList(
                new GeneralLedger.Line("1000", "MAIN", "SDG", cashDebit, cashCredit, 1, 1),
                new GeneralLedger.Line("4000", "MAIN", "SDG", salesDebit, salesCredit, 1, 1)
            ));
    }

    private GeneralLedgerReports.Line line(GeneralLedgerReports.TrialBalance report, String code) {
        for (GeneralLedgerReports.Line item : report.lines) {
            if (code.equals(item.code)) return item;
        }
        fail("Missing trial balance account " + code);
        return null;
    }

    @Test public void trialBalanceUsesPostedLedgerOnlyAndNetsReversalAsOfDate() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "trial_balance_" + System.currentTimeMillis() + ".db";
        SQLiteDatabase db = openLedger(context, name);
        try {
            // The opening transaction predates the requested period start. A trial balance
            // as of the end date must still include it, then include its posted reversal.
            GeneralLedger.post(db, entry("TB-SALE", "TB_SALE", "SALE-1",
                "2026-11-08", 1000, 0, 0, 1000));
            GeneralLedger.post(db, entry("TB-REVERSAL", "REVERSAL", "TB-SALE",
                "2026-11-09", 0, 1000, 1000, 0));

            // Draft journal rows must never affect posted-ledger totals.
            db.execSQL("INSERT INTO gl_journal_entries(entry_ref,description,transaction_date,period_id,source_type,source_id,status,created_at) " +
                "VALUES('TB-DRAFT','Draft fixture','2026-11-10',(SELECT id FROM financial_periods WHERE period_code='TB-2026-11'),'TB_DRAFT','DRAFT-1','DRAFT',1)");
            db.execSQL("INSERT INTO gl_journal_lines(entry_id,account_id,branch_id,base_currency_code,base_debit_minor,base_credit_minor,transaction_currency_code,transaction_debit_minor,transaction_credit_minor,fx_rate_num,fx_rate_den) " +
                "VALUES((SELECT id FROM gl_journal_entries WHERE entry_ref='TB-DRAFT'),(SELECT id FROM chart_of_accounts WHERE account_code='1000'),(SELECT id FROM branches WHERE branch_code='MAIN'),'SDG',500,0,'SDG',500,0,1,1)");
            db.execSQL("INSERT INTO gl_journal_lines(entry_id,account_id,branch_id,base_currency_code,base_debit_minor,base_credit_minor,transaction_currency_code,transaction_debit_minor,transaction_credit_minor,fx_rate_num,fx_rate_den) " +
                "VALUES((SELECT id FROM gl_journal_entries WHERE entry_ref='TB-DRAFT'),(SELECT id FROM chart_of_accounts WHERE account_code='4000'),(SELECT id FROM branches WHERE branch_code='MAIN'),'SDG',0,500,'SDG',0,500,1,1)");

            GeneralLedgerReports.TrialBalance report =
                GeneralLedgerReports.trialBalance(db, "2026-11-10", "2026-11-30", "MAIN");

            assertTrue("posted debit and credit totals must agree", report.isBalanced());
            assertEquals("draft rows must be excluded", 2000L, report.debitMinor);
            assertEquals(2000L, report.creditMinor);
            assertEquals("reversal must net cash back to zero", 0L, line(report, "1000").balanceMinor);
            assertEquals("reversal must net sales back to zero", 0L, line(report, "4000").balanceMinor);
        } finally {
            if (db.isOpen()) db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name + "-wal");
            context.deleteDatabase(name + "-shm");
        }
    }
}
