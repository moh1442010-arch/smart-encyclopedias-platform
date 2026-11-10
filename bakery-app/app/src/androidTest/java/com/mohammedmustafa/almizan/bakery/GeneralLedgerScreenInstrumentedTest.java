package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GeneralLedgerScreenInstrumentedTest {
    private static long scalar(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try { assertTrue(c.moveToFirst()); return c.getLong(0); }
        finally { c.close(); }
    }

    @Test public void ledgerReadModelShowsPostedBalancesAndExcludesDraftEntries() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="almizan_gl_screen_test.db";
        context.deleteDatabase(name);
        SQLiteDatabase db=null;
        try {
            db=SecureDatabase.open(context,name);
            EnterpriseSchema.ensure(db);
            AccountingDocumentService.ensureSchema(db);
            FinancialPeriodService.open(db,"TEST-2026","2026-01-01","2026-12-31");

            GeneralLedger.Entry posted=new GeneralLedger.Entry(
                "TEST-GL-001","اختبار ربط الأستاذ","2026-11-10","INSTRUMENTED_TEST","GL-001",
                "test:gl:001","TEST",
                Arrays.asList(
                    new GeneralLedger.Line("1000","MAIN","SDG",100,0,1,1),
                    new GeneralLedger.Line("4000","MAIN","SDG",0,100,1,1)
                )
            );
            GeneralLedger.post(db,posted);

            ContentValues draft=new ContentValues();
            draft.put("entry_ref","TEST-GL-DRAFT");draft.put("description","Draft must not appear");
            draft.put("transaction_date","2026-11-10");draft.put("source_type","TEST");
            draft.put("source_id","DRAFT-001");draft.put("status","DRAFT");
            draft.put("created_at",System.currentTimeMillis());
            long draftId=db.insertOrThrow("gl_journal_entries",null,draft);
            long cashId=scalar(db,"SELECT id FROM chart_of_accounts WHERE account_code='1000'",null);
            long revenueId=scalar(db,"SELECT id FROM chart_of_accounts WHERE account_code='4000'",null);
            long branchId=scalar(db,"SELECT id FROM branches WHERE branch_code='MAIN'",null);
            db.execSQL("INSERT INTO gl_journal_lines(entry_id,account_id,branch_id,base_currency_code,base_debit_minor,base_credit_minor,transaction_currency_code,transaction_debit_minor,transaction_credit_minor,fx_rate_num,fx_rate_den) VALUES(?,?,?,'SDG',999,0,'SDG',999,0,1,1)",
                new Object[]{draftId,cashId,branchId});
            db.execSQL("INSERT INTO gl_journal_lines(entry_id,account_id,branch_id,base_currency_code,base_debit_minor,base_credit_minor,transaction_currency_code,transaction_debit_minor,transaction_credit_minor,fx_rate_num,fx_rate_den) VALUES(?,?,?,'SDG',0,999,'SDG',0,999,1,1)",
                new Object[]{draftId,revenueId,branchId});

            String postedOnly="SELECT COALESCE(SUM(l.base_debit_minor),0) FROM chart_of_accounts a "+
                "LEFT JOIN gl_journal_lines l ON l.account_id=a.id AND EXISTS "+
                "(SELECT 1 FROM gl_journal_entries e WHERE e.id=l.entry_id AND e.status='POSTED') "+
                "WHERE a.account_code=?";
            assertEquals(100L,scalar(db,postedOnly,new String[]{"1000"}));
            assertEquals(0L,scalar(db,postedOnly,new String[]{"4000"}));
            assertEquals(100L,scalar(db,
                "SELECT COALESCE(SUM(l.base_credit_minor),0) FROM gl_journal_entries e "+
                "JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.status='POSTED'",null));
            assertEquals(1L,scalar(db,"SELECT COUNT(*) FROM gl_journal_entries WHERE status='POSTED'",null));
            assertEquals(1L,scalar(db,"SELECT COUNT(*) FROM gl_journal_entries WHERE status='DRAFT'",null));
        } finally {
            if(db!=null&&db.isOpen())db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }
}
