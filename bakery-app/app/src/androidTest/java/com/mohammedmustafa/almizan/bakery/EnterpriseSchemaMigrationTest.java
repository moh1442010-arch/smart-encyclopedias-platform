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
            String[] required={"company_settings","countries","currencies","exchange_rates","branches","users","roles","permissions","suppliers","customers","company_bank_accounts","party_opening_payments","party_payments","supplier_quick_invoices","purchase_invoices","purchase_lines","supplier_payments","employees","payroll_periods","payroll_items","fixed_assets","depreciation_entries","sync_outbox","tax_rules","tax_jurisdiction_profiles","currency_country_map","financial_periods","chart_of_accounts","gl_journal_entries","gl_journal_lines","idempotency_keys"};
            for(String table:required) {
                Cursor exists=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});
                try { assertTrue("Missing table "+table,exists.moveToFirst()&&exists.getInt(0)==1); }
                finally { exists.close(); }
            }
            Cursor sdTax=db.rawQuery("SELECT rate_num,rate_den FROM tax_rules WHERE country_code='SD' AND tax_code='VAT_STANDARD'",null);
            try { assertTrue("Sudan VAT baseline missing",sdTax.moveToFirst());assertEquals(17,sdTax.getInt(0));assertEquals(100,sdTax.getInt(1)); }
            finally { sdTax.close(); }
            Cursor uaeTax=db.rawQuery("SELECT rate_num,rate_den FROM tax_rules WHERE country_code='AE' AND tax_code='VAT_STANDARD'",null);
            try { assertTrue("UAE VAT baseline missing",uaeTax.moveToFirst());assertEquals(5,uaeTax.getInt(0));assertEquals(100,uaeTax.getInt(1)); }
            finally { uaeTax.close(); }
            Cursor noVat=db.rawQuery("SELECT profile_status,indirect_tax_type FROM tax_jurisdiction_profiles WHERE country_code='QA'",null);
            try { assertTrue(noVat.moveToFirst());assertEquals("NO_VAT_CONFIRMED",noVat.getString(0));assertEquals("NO_VAT",noVat.getString(1)); }
            finally { noVat.close(); }
            Cursor unknownTax=db.rawQuery("SELECT profile_status FROM tax_jurisdiction_profiles WHERE country_code='LY'",null);
            try { assertTrue(unknownTax.moveToFirst());assertEquals("REVIEW_REQUIRED",unknownTax.getString(0)); }
            finally { unknownTax.close(); }
            FactoryGlOutbox.validateReviewedChart(db);
            String[] factoryAccounts={"1210","1300","2200","2300","4050","5400"};
            for(String code:factoryAccounts){
                Cursor account=db.rawQuery("SELECT COUNT(*) FROM chart_of_accounts WHERE account_code=? AND active=1",new String[]{code});
                try{assertTrue("Missing factory accounting control account "+code,account.moveToFirst()&&account.getInt(0)==1);}
                finally{account.close();}
            }
            Cursor profileColumn=db.rawQuery("PRAGMA table_info(company_settings)",null);boolean hasBusinessName=false;
            try { while(profileColumn.moveToNext())if("business_name".equals(profileColumn.getString(1)))hasBusinessName=true; } finally { profileColumn.close(); }
            assertTrue("Company name must be persisted in encrypted company settings",hasBusinessName);
            Cursor currency=db.rawQuery("SELECT base_currency,country_code FROM company_settings WHERE id=1",null);
            try { assertTrue(currency.moveToFirst());assertEquals("SDG",currency.getString(0));assertEquals("SD",currency.getString(1)); }
            finally { currency.close(); }
            Cursor subscriptionOnly=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE code IN ('USD','EUR') AND active=0",null);
            try { assertTrue(subscriptionOnly.moveToFirst());assertEquals("USD and EUR must be disabled for operational selections",2,subscriptionOnly.getInt(0)); } finally { subscriptionOnly.close(); }
            Cursor operationalCurrencies=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE active=1 AND code IN ('DZD','BHD','KMF','DJF','EGP','IQD','JOD','KWD','LBP','LYD','MRU','MAD','OMR','ILS','QAR','SAR','SOS','SDG','SYP','TND','AED','YER')",null);
            try { assertTrue(operationalCurrencies.moveToFirst());assertEquals("All 22 Arab-country operating currencies must remain selectable",22,operationalCurrencies.getInt(0)); } finally { operationalCurrencies.close(); }
            Cursor nonRegional=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE active=1 AND code NOT IN ('DZD','BHD','KMF','DJF','EGP','IQD','JOD','KWD','LBP','LYD','MRU','MAD','OMR','ILS','QAR','SAR','SOS','SDG','SYP','TND','AED','YER')",null);
            try { assertTrue(nonRegional.moveToFirst());assertEquals("Non-Arab and subscription currencies must not be operational",0,nonRegional.getInt(0)); } finally { nonRegional.close(); }
            Cursor regionalCurrencies=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE active=1 AND code IN ('SDG','EGP','LYD','SAR','AED')",null);
            try { assertTrue(regionalCurrencies.moveToFirst());assertEquals("Required regional operating currencies missing",5,regionalCurrencies.getInt(0)); } finally { regionalCurrencies.close(); }
            Cursor arabMappings=db.rawQuery("SELECT COUNT(DISTINCT country_code) FROM currency_country_map WHERE country_code IN ('DZ','BH','KM','DJ','EG','IQ','JO','KW','LB','LY','MR','MA','OM','PS','QA','SA','SO','SD','SY','TN','AE','YE')",null);
            try { assertTrue(arabMappings.moveToFirst());assertEquals("All 22 Arab League country currency mappings must be seeded",22,arabMappings.getInt(0)); } finally { arabMappings.close(); }
            Cursor usdMappings=db.rawQuery("SELECT COUNT(*) FROM currency_country_map WHERE currency_code='USD'",null);
            try { assertTrue(usdMappings.moveToFirst());assertEquals("Shared USD must require country selection",3,usdMappings.getInt(0)); } finally { usdMappings.close(); }
            Cursor ils=db.rawQuery("SELECT active FROM currencies WHERE code='ILS'",null);
            try { assertTrue(ils.moveToFirst());assertEquals(1,ils.getInt(0)); } finally { ils.close(); }
            Cursor sdgPrecision=db.rawQuery("SELECT minor_units FROM currencies WHERE code='SDG'",null);
            try { assertTrue(sdgPrecision.moveToFirst());assertEquals("SDG preserves the existing bakery ledger whole-pound precision",0,sdgPrecision.getInt(0)); } finally { sdgPrecision.close(); }
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

    @Test public void ensurePreservesOperatorCurrencyActivationAfterOneTimeWhitelistMigration() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="enterprise_currency_activation_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("UPDATE currencies SET active=0 WHERE code='SDG'");
            EnterpriseSchema.ensure(db);
            Cursor states=db.rawQuery("SELECT code,active FROM currencies WHERE code IN ('SDG','USD') ORDER BY code",null);
            try {
                assertTrue(states.moveToFirst());
                assertEquals("SDG",states.getString(0));
                assertEquals("SDG operator deactivation must survive ensure()",0,states.getInt(1));
                assertTrue(states.moveToNext());
                assertEquals("USD",states.getString(0));
                assertEquals("USD must remain excluded by the one-time operational whitelist",0,states.getInt(1));
            } finally { states.close(); }
            Cursor version=db.rawQuery("SELECT value FROM enterprise_meta WHERE key='schema_version'",null);
            try { assertTrue(version.moveToFirst());assertEquals("4",version.getString(0)); }
            finally { version.close(); }
        } finally {
            db.close();context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void ledgerReportsIncludeEquityAndExcludeDraftsWithCumulativeTrialBalance() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="ledger_reports_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO chart_of_accounts(id,account_code,name_ar,name_en,account_type,normal_side,active) VALUES"+
                "(101,'TST-A101','النقدية','Cash','ASSET','DEBIT',1),"+
                "(102,'TST-E102','رأس المال','Capital','EQUITY','CREDIT',1),"+
                "(103,'TST-R103','الإيرادات','Revenue','REVENUE','CREDIT',1),"+
                "(104,'TST-X104','المصروفات','Expenses','EXPENSE','DEBIT',1)");
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
            GeneralLedgerReports.IncomeStatement summary=GeneralLedgerReports.incomeStatementSummary(db,"2026-11-01","2026-11-30",null);
            assertEquals("Income statement revenue must use credit-normal balances",500L,summary.revenueMinor);
            assertEquals("Draft expense must not affect income statement totals",0L,summary.expenseMinor);
            assertEquals("Net profit must be revenue less expenses",500L,summary.netProfitMinor);
        } finally {
            db.close();context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }
    @Test public void generalLedgerReversalPostsOnceAndKeepsBothSidesInTrialBalance() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="ledger_reversal_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            // Reversal now enforces JOURNAL_POST at the service boundary.
            // Give the test actor an explicit OWNER role in the MAIN branch.
            db.execSQL("INSERT INTO users(username,display_name,pin_salt,pin_hash,active,created_at) VALUES('ledger-owner','Ledger Owner',X'01',X'02',1,1)");
            db.execSQL("INSERT INTO user_roles(user_id,role_id,branch_id) SELECT u.id,r.id,b.id FROM users u,roles r,branches b WHERE u.username='ledger-owner' AND r.role_code='OWNER' AND b.branch_code='MAIN'");
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES('FY26-11','2026-11-01','2026-11-30','OPEN')");
            java.util.List<GeneralLedger.Line> lines=new java.util.ArrayList<>();
            lines.add(new GeneralLedger.Line("1000","MAIN","SDG",12500,0,1,1));
            lines.add(new GeneralLedger.Line("4000","MAIN","SDG",0,12500,1,1));
            GeneralLedger.Entry original=new GeneralLedger.Entry("TEST-ORIGINAL","اختبار بيع نقدي","2026-11-09",
                "TEST_SALE","TEST-ORIGINAL","test-sale:original","ledger-owner",lines);
            GeneralLedger.post(db,original);
            GeneralLedger.Result first=GeneralLedger.reverse(db,"TEST-ORIGINAL","2026-11-09","ledger-owner");
            assertFalse("First reversal must create a new journal",first.alreadyPosted);
            GeneralLedger.Result retry=GeneralLedger.reverse(db,"TEST-ORIGINAL","2026-11-09","ledger-owner");
            assertTrue("Repeated reversal must be idempotent",retry.alreadyPosted);
            assertEquals(first.reference,retry.reference);
            Cursor count=db.rawQuery("SELECT COUNT(*) FROM gl_journal_entries WHERE source_type='REVERSAL' AND source_id='TEST-ORIGINAL'",null);
            try { assertTrue(count.moveToFirst());assertEquals(1,count.getInt(0)); } finally { count.close(); }
            Cursor relation=db.rawQuery("SELECT reversal_of FROM gl_journal_entries WHERE entry_ref=?",new String[]{first.reference});
            try { assertTrue(relation.moveToFirst());assertTrue("Reversal must reference original journal id",relation.getLong(0)>0); } finally { relation.close(); }
            GeneralLedgerReports.TrialBalance trial=GeneralLedgerReports.trialBalance(db,"2026-11-01","2026-11-30",null);
            assertTrue("Original and reversal must both remain in the posted trial balance",trial.isBalanced());
            for(GeneralLedgerReports.Line line:trial.lines) {
                if("1000".equals(line.code)||"4000".equals(line.code))
                    assertEquals("Reversed account must net to zero",0L,line.balanceMinor);
            }
        } finally {
            db.close();context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

    @Test public void permissionMatrixIsBranchScopedAndLeastPrivilege() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="permission_matrix_"+System.currentTimeMillis()+".db";
        SQLiteDatabase db=SecureDatabase.open(context,name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO users(username,display_name,pin_salt,pin_hash,active,created_at) VALUES('owner_test','Owner',X'01',X'02',1,1)");
            db.execSQL("INSERT INTO users(username,display_name,pin_salt,pin_hash,active,created_at) VALUES('auditor_test','Auditor',X'01',X'02',1,1)");
            db.execSQL("INSERT INTO user_roles(user_id,role_id,branch_id) SELECT u.id,r.id,b.id FROM users u,roles r,branches b WHERE u.username='owner_test' AND r.role_code='OWNER' AND b.branch_code='MAIN'");
            db.execSQL("INSERT INTO user_roles(user_id,role_id,branch_id) SELECT u.id,r.id,b.id FROM users u,roles r,branches b WHERE u.username='auditor_test' AND r.role_code='AUDITOR' AND b.branch_code='MAIN'");
            assertTrue("Owner must be allowed to post journals",PermissionService.hasPermission(db,"owner_test","MAIN","JOURNAL_POST"));
            assertTrue("Owner must manage users",PermissionService.hasPermission(db,"owner_test","MAIN","USERS_MANAGE"));
            assertTrue("Auditor must read payroll",PermissionService.hasPermission(db,"auditor_test","MAIN","PAYROLL_READ"));
            assertFalse("Auditor must not post journals",PermissionService.hasPermission(db,"auditor_test","MAIN","JOURNAL_POST"));
            assertFalse("Auditor must not restore backups",PermissionService.hasPermission(db,"auditor_test","MAIN","BACKUP_RESTORE"));
            assertFalse("Role must not grant access to another branch",PermissionService.hasPermission(db,"owner_test","UNKNOWN","JOURNAL_POST"));
            boolean denied=false;
            try { PermissionService.requirePermission(db,"auditor_test","MAIN","JOURNAL_POST"); }
            catch(SecurityException expected) { denied=true; }
            assertTrue("Service layer must throw on unauthorized writes",denied);
            GeneralLedger.Entry protectedEntry=new GeneralLedger.Entry("SEC-1","Unauthorized journal","2026-11-07","TEST","SEC-1","SEC-IDEM-1","auditor_test",
                java.util.Arrays.asList(
                    new GeneralLedger.Line("1000","MAIN","SDG",100,0,1,1),
                    new GeneralLedger.Line("3000","MAIN","SDG",0,100,1,1)));
            boolean postingDenied=false;
            try { GeneralLedger.postAuthorized(db,protectedEntry,"auditor_test"); }
            catch(SecurityException expected) { postingDenied=true; }
            assertTrue("Unauthorized actor must be blocked before journal persistence",postingDenied);
            Cursor noJournal=db.rawQuery("SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref='SEC-1'",null);
            try { assertTrue(noJournal.moveToFirst());assertEquals(0,noJournal.getInt(0)); } finally { noJournal.close(); }
        } finally {
            db.close();context.deleteDatabase(name);context.deleteDatabase(name+"-wal");context.deleteDatabase(name+"-shm");
        }
    }

}
