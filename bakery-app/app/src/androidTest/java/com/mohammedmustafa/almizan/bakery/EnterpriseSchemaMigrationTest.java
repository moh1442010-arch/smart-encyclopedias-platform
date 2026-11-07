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
            Cursor currency=db.rawQuery("SELECT base_currency,country_code FROM company_settings WHERE id=1",null);
            try { assertTrue(currency.moveToFirst());assertEquals("SDG",currency.getString(0));assertEquals("SD",currency.getString(1)); }
            finally { currency.close(); }
            Cursor currencies=db.rawQuery("SELECT COUNT(*) FROM currencies WHERE code IN ('USD','EUR','SAR','AED')",null);
            try { assertTrue(currencies.moveToFirst());assertEquals(4,currencies.getInt(0)); } finally { currencies.close(); }
            Cursor supplierColumns=db.rawQuery("PRAGMA table_info(suppliers)",null);
            boolean defaultItem=false,defaultUnit=false;
            try { while(supplierColumns.moveToNext()){if("default_item".equals(supplierColumns.getString(1)))defaultItem=true;if("default_unit".equals(supplierColumns.getString(1)))defaultUnit=true;} }
            finally { supplierColumns.close(); }
            assertTrue("Supplier default item field missing",defaultItem);assertTrue("Supplier default unit field missing",defaultUnit);
            long customerId=PartyDirectory.saveCustomer(db,"عميل اختبار","249900000001",125000,"USD");
            assertTrue(customerId>0);
            long supplierId=PartyDirectory.saveSupplier(db,"مورد اختبار","249900000002","دقيق","جوال",50000,"EUR");
            assertTrue(supplierId>0);
            PartyDirectory.saveBankAccount(db,"Test EUR","Test Bank","EUR","GB82WEST12345698765432","WESTGB22","");
            Cursor bank=db.rawQuery("SELECT currency_code,iban FROM company_bank_accounts WHERE account_name='Test EUR'",null);
            try { assertTrue(bank.moveToFirst());assertEquals("EUR",bank.getString(0));assertEquals("GB82WEST12345698765432",bank.getString(1)); } finally { bank.close(); }
            JSONObject partySnapshot=PartyDirectoryBackup.export(db);
            PartyDirectoryBackup.validate(partySnapshot);
            db.delete("customers",null,null);db.delete("suppliers",null,null);db.delete("company_bank_accounts",null,null);
            PartyDirectoryBackup.restore(db,partySnapshot);
            Cursor restored=db.rawQuery("SELECT COUNT(*) FROM customers WHERE id=? AND name='عميل اختبار'",new String[]{String.valueOf(customerId)});
            try { assertTrue(restored.moveToFirst());assertEquals(1,restored.getInt(0)); } finally { restored.close(); }
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
}
