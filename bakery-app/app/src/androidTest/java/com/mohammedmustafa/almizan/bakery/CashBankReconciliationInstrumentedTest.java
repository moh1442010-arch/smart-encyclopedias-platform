package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CashBankReconciliationInstrumentedTest {
    private static final String START="2026-11-01",END="2026-11-30";

    private SQLiteDatabase open(Context context,String name) {
        SQLiteDatabase db=SecureDatabase.open(context,name);
        db.execSQL("CREATE TABLE cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER,ref TEXT,date TEXT)");
        db.execSQL("CREATE TABLE company_settings(id INTEGER PRIMARY KEY,base_currency TEXT)");
        db.execSQL("INSERT INTO company_settings VALUES(1,'SDG')");
        db.execSQL("CREATE TABLE branches(id INTEGER PRIMARY KEY,branch_code TEXT,active INTEGER)");
        db.execSQL("INSERT INTO branches VALUES(1,'MAIN',1)");
        db.execSQL("INSERT INTO branches VALUES(2,'OTHER',1)");
        db.execSQL("CREATE TABLE chart_of_accounts(id INTEGER PRIMARY KEY,account_code TEXT,active INTEGER)");
        db.execSQL("INSERT INTO chart_of_accounts VALUES(1,'1000',1)");
        db.execSQL("INSERT INTO chart_of_accounts VALUES(2,'1010',1)");
        db.execSQL("CREATE TABLE gl_journal_entries(id INTEGER PRIMARY KEY,source_type TEXT,status TEXT,transaction_date TEXT)");
        db.execSQL("CREATE TABLE gl_journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER,account_id INTEGER,branch_id INTEGER,transaction_currency_code TEXT,transaction_debit_minor INTEGER,transaction_credit_minor INTEGER)");
        return db;
    }

    private void cash(SQLiteDatabase db,String account,String kind,long amount,String date) {
        db.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES(?,?,?,'TEST',?)",
            new Object[]{account,kind,amount,date});
    }

    private void gl(SQLiteDatabase db,String sourceType,String date,int account,int branch,String currency,long debit,long credit) {
        db.execSQL("INSERT INTO gl_journal_entries(source_type,status,transaction_date) VALUES(?,'POSTED',?)",
            new Object[]{sourceType,date});
        db.execSQL("INSERT INTO gl_journal_lines(entry_id,account_id,branch_id,transaction_currency_code,transaction_debit_minor,transaction_credit_minor) "+
            "VALUES(last_insert_rowid(),?,?,?,?,?)",new Object[]{account,branch,currency,debit,credit});
    }

    @Test public void matchesCashAndBankakIncludingReturnsAndExcludesOtherDimensions() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="cash_bank_match_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=open(context,name);
        try {
            cash(db,"CASH","SALE",100,START);
            cash(db,"CASH","PURCHASE",40,END);
            cash(db,"BANKAK","COLLECTION",50,START);
            cash(db,"BANKAK","SALE_RETURN",10,END);
            gl(db,"FACTORY_LEDGER",START,1,1,"SDG",100,0);
            gl(db,"FACTORY_LEDGER",END,1,1,"SDG",0,40);
            gl(db,"FACTORY_LEDGER",START,2,1,"SDG",50,0);
            gl(db,"FACTORY_LEDGER",END,2,1,"SDG",0,10);
            gl(db,"MANUAL",START,1,1,"SDG",9000,0);
            gl(db,"FACTORY_LEDGER","2026-10-31",1,1,"SDG",7000,0);
            gl(db,"FACTORY_LEDGER",START,1,2,"SDG",6000,0);
            gl(db,"FACTORY_LEDGER",START,1,1,"USD",5000,0);

            CashBankReconciliationService.Report report=CashBankReconciliationService.reconcile(
                db,db,"MAIN","SDG",START,END);
            assertTrue(report.cash.balanced());
            assertTrue(report.bankak.balanced());
            assertEquals(60L,report.cash.cashbookMinor);
            assertEquals(60L,report.cash.ledgerMinor);
            assertEquals(40L,report.bankak.cashbookMinor);
            assertEquals(40L,report.bankak.ledgerMinor);
            assertTrue(report.balanced());
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void reportsPositiveAndNegativeDifferencesWithoutHidingThem() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="cash_bank_difference_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=open(context,name);
        try {
            cash(db,"CASH","SALE",100,START);
            gl(db,"FACTORY_LEDGER",START,1,1,"SDG",70,0);
            gl(db,"FACTORY_LEDGER",START,2,1,"SDG",0,15);
            CashBankReconciliationService.Report report=CashBankReconciliationService.reconcile(
                db,db,"MAIN","SDG",START,END);
            assertEquals(30L,report.cash.differenceMinor);
            assertEquals(-15L,report.bankak.differenceMinor);
            assertFalse(report.balanced());
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void rejectsUnknownCashbookMovementInsteadOfTreatingItAsZero() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="cash_bank_unknown_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=open(context,name);
        try {
            cash(db,"CASH","UNCLASSIFIED",25,START);
            try {
                CashBankReconciliationService.reconcile(db,db,"MAIN","SDG",START,END);
                fail("Unknown movement kinds must block reconciliation");
            } catch(IllegalStateException expected) {
                assertTrue(expected.getMessage().contains("غير معروف"));
            }
        } finally {
            if(db.isOpen())db.close();
            context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }
}
