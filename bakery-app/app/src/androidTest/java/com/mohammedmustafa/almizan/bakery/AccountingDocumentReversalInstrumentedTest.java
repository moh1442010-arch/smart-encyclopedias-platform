package com.mohammedmustafa.almizan.bakery;

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
public class AccountingDocumentReversalInstrumentedTest {
    private long scalar(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try { assertTrue(c.moveToFirst()); return c.getLong(0); }
        finally { c.close(); }
    }

    @Test public void reversalIsPersistentlyLinkedAndRepeatedRequestIsIdempotent() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="accounting_document_reversal_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('REVERSAL-TEST','2026-11-01','2026-11-30','OPEN')");
            AccountingDocumentService.ensureSchema(db);

            GeneralLedger.Entry original=new GeneralLedger.Entry(
                "DOC-REV-ORIGINAL","مستند بيع للاختبار","2026-11-10",
                "ACCOUNTING_DOCUMENT","DOC-REV-1","DOC-REV-ORIGINAL-POST","TEST",
                Arrays.asList(
                    new GeneralLedger.Line("1000","MAIN","SDG",12500,0,1,1),
                    new GeneralLedger.Line("4000","MAIN","SDG",0,12500,1,1)));
            GeneralLedger.post(db,original);

            db.execSQL("INSERT INTO accounting_documents(document_key,document_type,source_module,source_date,currency_code,branch_code,payload_sha256,status,journal_ref,created_at,updated_at) VALUES('DOC-REV-1','SALES_ADJUSTMENT','window','2026-11-10','SDG','MAIN','test-hash','POSTED','DOC-REV-ORIGINAL',1,1)");
            long documentId=scalar(db,"SELECT id FROM accounting_documents WHERE document_key='DOC-REV-1'",null);

            String reversal=AccountingDocumentService.reverseDocument(db,documentId,"2026-11-10","TEST");
            assertEquals("REV-DOC-REV-ORIGINAL",reversal);
            assertEquals("REVERSED",value(db,"SELECT status FROM accounting_documents WHERE id=?",new String[]{String.valueOf(documentId)}));
            assertEquals(reversal,value(db,"SELECT reversal_journal_ref FROM accounting_documents WHERE id=?",new String[]{String.valueOf(documentId)}));
            assertEquals(2,scalar(db,"SELECT COUNT(*) FROM gl_journal_entries WHERE status='POSTED'",null));

            String retry=AccountingDocumentService.reverseDocument(db,documentId,"2026-11-11","TEST-RETRY");
            assertEquals("retry must return the original reversal reference",reversal,retry);
            assertEquals(2,scalar(db,"SELECT COUNT(*) FROM gl_journal_entries WHERE status='POSTED'",null));
            assertEquals(1,scalar(db,"SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref=? AND reversal_of IS NOT NULL",new String[]{reversal}));
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }

    private String value(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try { assertTrue(c.moveToFirst()); return c.getString(0); }
        finally { c.close(); }
    }
}
