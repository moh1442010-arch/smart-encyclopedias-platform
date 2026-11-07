package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class EnterpriseSchemaMigrationTest {
    @Test public void addsEnterpriseTablesWithoutChangingExistingBakeryRecords() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="enterprise_migration_"+System.currentTimeMillis()+".db";
        File file=context.getDatabasePath(name);
        File parent=file.getParentFile();
        assertNotNull(parent);
        if(!parent.exists())assertTrue(parent.mkdirs());
        if(file.exists())assertTrue(file.delete());
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            db.execSQL("CREATE TABLE records(day TEXT NOT NULL,module TEXT NOT NULL,payload TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(day,module))");
            db.execSQL("INSERT INTO records(day,module,payload,updated_at) VALUES('2026-11-06','stock','KEEP_EXISTING_STOCK',123)");
            EnterpriseSchema.ensure(db);
            Cursor old=db.rawQuery("SELECT payload FROM records WHERE day='2026-11-06' AND module='stock'",null);
            try { assertTrue(old.moveToFirst()); assertEquals("KEEP_EXISTING_STOCK",old.getString(0)); }
            finally { old.close(); }
            String[] required={"company_settings","countries","currencies","exchange_rates","branches","users","roles","permissions","suppliers","customers","company_bank_accounts","party_opening_payments","party_payments","supplier_quick_invoices","purchase_invoices","purchase_lines","supplier_payments","employees","payroll_periods","payroll_items","fixed_assets","depreciation_entries","sync_outbox","tax_rules","financial_periods","chart_of_accounts","gl_journal_entries","gl_journal_lines","idempotency_keys"};
            for(String table:required) {
                Cursor exists=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});
                try { assertTrue("Missing table "+table,exists.moveToFirst()&&exists.getInt(0)==1); }
                finally { exists.close(); }
            }
            Cursor profileColumn=db.rawQuery("PRAGMA table_info(company_settings)",null);boolean hasBusinessName=false;
            try { while(profileColumn.moveToNext())if("business_name".equals(profileColumn.getString(1)))hasBusinessName=true; } finally { profileColumn.close(); }
            assertTrue("Company name must be persisted in encrypted company settings",hasBusinessName);
            Cursor currency=db.rawQuery("SELECT base_currency,country_code FROM company_settings WHERE id=1",null);
            try { assertTrue(currency.moveToFirst());assertEquals("SDG",currency.getString(0));assertEquals("SD",currency.getString(1)); }
            finally { currency.close(); }
            Cursor subscriptionOnly=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE code IN ('USD','EUR') AND active=0",null);
            try { assertTrue(subscriptionOnly.moveToFirst());assertEquals("USD and EUR must be inactive for in-app operations",2,subscriptionOnly.getInt(0)); } finally { subscriptionOnly.close(); }
            Cursor currencies=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE code IN ('USD','EUR','SAR','AED')",null);
            try { assertTrue(currencies.moveToFirst());assertEquals(4,currencies.getInt(0)); } finally { currencies.close(); }
            Cursor operationalCurrencies=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE active=1 AND code IN ('USD','EUR')",null);
            try { assertTrue(operationalCurrencies.moveToFirst());assertEquals("USD/EUR must never appear in operational currency pickers",0,operationalCurrencies.getInt(0)); } finally { operationalCurrencies.close(); }
            Cursor regionalCurrencies=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE active=1 AND code IN ('SDG','EGP','LYD','SAR','AED')",null);
            try { assertTrue(regionalCurrencies.moveToFirst());assertEquals("Required regional operating currencies missing",5,regionalCurrencies.getInt(0)); } finally { regionalCurrencies.close(); }
            Cursor sdgPrecision=db.rawQuery("SELECT minor_units FROM currencies WHERE code='SDG'",null);
            try { assertTrue(sdgPrecision.moveToFirst());assertEquals("Legacy bakery ledger stores whole SDG units",0,sdgPrecision.getInt(0)); } finally { sdgPrecision.close(); }
            Cursor supplierColumns=db.rawQuery("PRAGMA table_info(suppliers)",null);
            boolean defaultItem=false,defaultUnit=false;
            try { while(supplierColumns.moveToNext()){if("default_item".equals(supplierColumns.getString(1)))defaultItem=true;if("default_unit".equals(supplierColumns.getString(1)))defaultUnit=true;} }
            finally { supplierColumns.close(); }
            assertTrue("Supplier default item field missing",defaultItem);assertTrue("Supplier default unit field missing",defaultUnit);
            long customerId=PartyDirectory.saveCustomer(db,"عميل اختبار","249900000001",125000,"SAR");
            assertTrue(customerId>0);
            long supplierId=PartyDirectory.saveSupplier(db,"مورد اختبار","249900000002","دقيق","جوال",50000,"AED");
            assertTrue(supplierId>0);
            PartyDirectory.saveBankAccount(db,"Test SAR","Test Bank","SAR","GB82WEST12345698765432","WESTGB22","");
            Cursor bank=db.rawQuery("SELECT currency_code,iban FROM company_bank_accounts WHERE account_name='Test SAR'",null);
            try { assertTrue(bank.moveToFirst());assertEquals("SAR",bank.getString(0));assertEquals("GB82WEST12345698765432",bank.getString(1)); } finally { bank.close(); }
            db.execSQL("INSERT INTO exchange_rates(base_code,quote_code,rate_num,rate_den,effective_at,source,created_at) VALUES('SAR','SDG',750,1,'2026-11-07','TEST',1)");
            JSONObject partySnapshot=PartyDirectoryBackup.export(db);
            PartyDirectoryBackup.validate(partySnapshot);
            db.delete("customers",null,null);db.delete("suppliers",null,null);db.delete("company_bank_accounts",null,null);db.delete("exchange_rates",null,null);
            PartyDirectoryBackup.restore(db,partySnapshot);
            Cursor restored=db.rawQuery("SELECT COUNT(*) FROM customers WHERE id=? AND name='عميل اختبار'",new String[]{String.valueOf(customerId)});
            try { assertTrue(restored.moveToFirst());assertEquals(1,restored.getInt(0)); } finally { restored.close(); }
            Cursor restoredRate=db.rawQuery("SELECT rate_num,rate_den FROM exchange_rates WHERE base_code='SAR' AND quote_code='SDG'",null);
            try { assertTrue(restoredRate.moveToFirst());assertEquals(750,restoredRate.getLong(0));assertEquals(1,restoredRate.getLong(1)); } finally { restoredRate.close(); }
            Cursor fk=db.rawQuery("PRAGMA foreign_keys",null);
            try { assertTrue(fk.moveToFirst());assertEquals(1,fk.getInt(0)); } finally { fk.close(); }
            boolean orphanRejected=false;
            try { db.execSQL("INSERT INTO supplier_payments(supplier_id,payment_date,amount_minor,currency_code,method,branch_id,journal_ref,created_at) VALUES(999,'2026-11-06',100,'SDG','CASH',1,'TEST-ORPHAN',1)"); }
            catch(RuntimeException expected) { orphanRejected=true; }
            assertTrue("Foreign key must reject orphan supplier payment",orphanRejected);
        } finally {
            db.close();
            context.deleteDatabase(name);
            context.deleteDatabase(name+"-wal");
            context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void ledgerReportsIncludeEquityAndExcludeDraftsWithCumulativeTrialBalance() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="ledger_reports_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO chart_of_accounts(id,account_code,name_ar,name_en,account_type,normal_side,active) VALUES"+
                "(101,'1000','النقدية','Cash','ASSET','DEBIT',1),"+
                "(102,'3000','رأس المال','Capital','EQUITY','CREDIT',1),"+
                "(103,'4000','الإيرادات','Revenue','REVENUE','CREDIT',1),"+
                "(104,'5000','المصروفات','Expenses','EXPENSE','DEBIT',1)");
            db.execSQL("INSERT INTO gl_journal_entries(id,entry_ref,description,transaction_date,source_type,source_id,status,created_at) VALUES"+
                "(201,'OPEN-1','Opening capital','2026-10-01','TEST','OPEN-1','POSTED',1),"+
                "(202,'SALE-1','Posted sale','2026-11-05','TEST','SALE-1','POSTED',2),"+
                "(203,'DRAFT-1','Draft expense','2026-11-10','TEST','DRAFT-1','DRAFT',3)");
            db.execSQL("INSERT INTO gl_journal_lines(entry_id,account_id,branch_id,base_currency_code,base_debit_minor,base_credit_minor,transaction_currency_code,transaction_debit_minor,transaction_credit_minor) VALUES"+
                "(201,101,1,'SDG',1000,0,'SDG',1000,0),"+
                "(201,102,1,'SDG',0,1000,'SDG',0,1000),"+
                "(202,101,1,'SDG',500,0,'SDG',500,0),"+
                "(202,103,1,'SDG',0,500,'SDG',0,500),"+
                "(203,104,1,'SDG',700,0,'SDG',700,0),"+
                "(203,101,1,'SDG',0,700,'SDG',0,700)");
            GeneralLedgerReports.TrialBalance trial=GeneralLedgerReports.trialBalance(db,"2026-11-01","2026-11-30",null);
            assertTrue("Trial balance must include opening entries before report start",trial.debitMinor==1500&&trial.creditMinor==1500);
            assertTrue(trial.isBalanced());
            GeneralLedgerReports.TrialBalance position=GeneralLedgerReports.balanceSheetAccounts(db,"2026-11-30",null);
            boolean equity=false;for(GeneralLedgerReports.Line line:position.lines)if("TST-E102".equals(line.code))equity=true;
            assertTrue("Balance sheet account query must include EQUITY",equity);
            GeneralLedgerReports.TrialBalance income=GeneralLedgerReports.incomeStatement(db,"2026-11-01","2026-11-30",null);
            boolean revenue=false,draftExpense=false;
            for(GeneralLedgerReports.Line line:income.lines){
                if("TST-R103".equals(line.code))revenue=line.creditMinor==500;
                if("TST-X104".equals(line.code))draftExpense=line.debitMinor==0;
            }
            assertTrue("Income report must include posted revenue",revenue);
            assertTrue("Income report must exclude draft expense lines",draftExpense);
        } finally {
            db.close();context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }
}
