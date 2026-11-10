package com.mohammedmustafa.almizan.bakery;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

/**
 * Additive enterprise schema for the bakery database.
 * Existing 2.8.0 records and inventory tables are never dropped or rewritten here.
 * UI workflows and posting services must be added before these tables are presented as complete features.
 */
final class EnterpriseSchema {
    static final int VERSION = 4;
    private EnterpriseSchema() {}

    static void ensure(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON");
        db.execSQL("CREATE TABLE IF NOT EXISTS enterprise_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)");
        int previousSchemaVersion=readSchemaVersion(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS company_settings (id INTEGER PRIMARY KEY CHECK(id=1), country_code TEXT NOT NULL DEFAULT 'SD', base_currency TEXT NOT NULL DEFAULT 'SDG', timezone TEXT NOT NULL DEFAULT 'Africa/Khartoum', language TEXT NOT NULL DEFAULT 'ar', fiscal_year_start TEXT NOT NULL DEFAULT '01-01', tax_profile TEXT NOT NULL DEFAULT 'SD_DEFAULT', business_name TEXT NOT NULL DEFAULT '')");
        db.execSQL("INSERT OR IGNORE INTO company_settings(id) VALUES(1)");
        try { db.execSQL("ALTER TABLE company_settings ADD COLUMN business_name TEXT NOT NULL DEFAULT ''"); } catch (Exception ignored) { }
        db.execSQL("CREATE TABLE IF NOT EXISTS countries (code TEXT PRIMARY KEY NOT NULL, name_ar TEXT NOT NULL, name_en TEXT NOT NULL, tax_profile TEXT NOT NULL DEFAULT 'CUSTOM', active INTEGER NOT NULL DEFAULT 0 CHECK(active IN (0,1)))");
        db.execSQL("INSERT OR IGNORE INTO countries(code,name_ar,name_en,tax_profile,active) VALUES('SD','السودان','Sudan','SD_DEFAULT',1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS currencies (code TEXT PRIMARY KEY NOT NULL CHECK(length(code)=3), name_ar TEXT NOT NULL, name_en TEXT NOT NULL, minor_units INTEGER NOT NULL CHECK(minor_units BETWEEN 0 AND 4), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)))");
        db.execSQL("INSERT OR IGNORE INTO currencies(code,name_ar,name_en,minor_units,active) VALUES('SDG','الجنيه السوداني','Sudanese Pound',0,1)");
        // SDG operational amounts are whole pounds; update metadata only, never rescale stored customer balances.
        db.execSQL("UPDATE currencies SET minor_units=0 WHERE code='SDG'");
        db.execSQL("CREATE TABLE IF NOT EXISTS exchange_rates (id INTEGER PRIMARY KEY AUTOINCREMENT, base_code TEXT NOT NULL REFERENCES currencies(code), quote_code TEXT NOT NULL REFERENCES currencies(code), rate_num INTEGER NOT NULL CHECK(rate_num>0), rate_den INTEGER NOT NULL CHECK(rate_den>0), effective_at TEXT NOT NULL, source TEXT NOT NULL, created_at INTEGER NOT NULL, UNIQUE(base_code,quote_code,effective_at,source), CHECK(base_code<>quote_code))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_exchange_rates_pair_date ON exchange_rates(base_code,quote_code,effective_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS branches (id INTEGER PRIMARY KEY AUTOINCREMENT, branch_code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, address TEXT NOT NULL DEFAULT '', active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), created_at INTEGER NOT NULL)");
        db.execSQL("INSERT OR IGNORE INTO branches(branch_code,name,active,created_at) VALUES('MAIN','الفرع الرئيسي',1,strftime('%s','now')*1000)");
        db.execSQL("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, display_name TEXT NOT NULL, pin_salt BLOB NOT NULL, pin_hash BLOB NOT NULL, active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), created_at INTEGER NOT NULL, last_login INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS roles (id INTEGER PRIMARY KEY AUTOINCREMENT, role_code TEXT NOT NULL UNIQUE, name_ar TEXT NOT NULL, name_en TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS permissions (permission_code TEXT PRIMARY KEY NOT NULL, description TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS role_permissions (role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE, permission_code TEXT NOT NULL REFERENCES permissions(permission_code), PRIMARY KEY(role_id,permission_code))");
        db.execSQL("INSERT OR IGNORE INTO roles(role_code,name_ar,name_en) VALUES('OWNER','مالك المنشأة','Owner'),('MANAGER','مدير','Manager'),('ACCOUNTANT','محاسب','Accountant'),('STOREKEEPER','أمين مخزن','Storekeeper'),('CASHIER','كاشير','Cashier'),('AUDITOR','مراجع','Auditor')");
        db.execSQL("INSERT OR IGNORE INTO permissions(permission_code,description) VALUES('DASHBOARD_READ','Read dashboard'),('SALES_CREATE','Create sales'),('SALES_VOID','Void sales'),('INVENTORY_READ','Read inventory'),('INVENTORY_WRITE','Change inventory'),('PURCHASE_CREATE','Create purchases'),('PURCHASE_APPROVE','Approve purchases'),('PAYROLL_READ','Read payroll'),('PAYROLL_POST','Post payroll'),('ASSET_READ','Read assets'),('ASSET_WRITE','Manage assets'),('JOURNAL_POST','Post journals'),('USERS_MANAGE','Manage users'),('BRANCHES_MANAGE','Manage branches'),('BACKUP_EXPORT','Export backups'),('BACKUP_RESTORE','Restore backups'),('PERIOD_CLOSE','Close financial periods'),('PERIOD_REOPEN','Reopen financial periods')");
        // Explicit least-privilege defaults. Role assignment is branch-scoped and checked by PermissionService.
        seedRolePermissions(db,"OWNER",new String[]{"DASHBOARD_READ","SALES_CREATE","SALES_VOID","INVENTORY_READ","INVENTORY_WRITE","PURCHASE_CREATE","PURCHASE_APPROVE","PAYROLL_READ","PAYROLL_POST","ASSET_READ","ASSET_WRITE","JOURNAL_POST","PERIOD_CLOSE","PERIOD_REOPEN","USERS_MANAGE","BRANCHES_MANAGE","BACKUP_EXPORT","BACKUP_RESTORE"});
        seedRolePermissions(db,"MANAGER",new String[]{"DASHBOARD_READ","SALES_CREATE","SALES_VOID","INVENTORY_READ","INVENTORY_WRITE","PURCHASE_CREATE","PURCHASE_APPROVE","PAYROLL_READ","PAYROLL_POST","ASSET_READ","ASSET_WRITE","JOURNAL_POST","PERIOD_CLOSE","PERIOD_REOPEN","BRANCHES_MANAGE","BACKUP_EXPORT"});
        seedRolePermissions(db,"ACCOUNTANT",new String[]{"DASHBOARD_READ","SALES_CREATE","INVENTORY_READ","PURCHASE_CREATE","PAYROLL_READ","PAYROLL_POST","ASSET_READ","ASSET_WRITE","JOURNAL_POST","PERIOD_CLOSE","BACKUP_EXPORT"});
        seedRolePermissions(db,"STOREKEEPER",new String[]{"DASHBOARD_READ","INVENTORY_READ","INVENTORY_WRITE"});
        seedRolePermissions(db,"CASHIER",new String[]{"DASHBOARD_READ","SALES_CREATE","INVENTORY_READ"});
        seedRolePermissions(db,"AUDITOR",new String[]{"DASHBOARD_READ","INVENTORY_READ","PAYROLL_READ","ASSET_READ","BACKUP_EXPORT"});
        db.execSQL("CREATE TABLE IF NOT EXISTS tax_rules (id INTEGER PRIMARY KEY AUTOINCREMENT, country_code TEXT NOT NULL REFERENCES countries(code), tax_code TEXT NOT NULL, name TEXT NOT NULL, rate_num INTEGER NOT NULL CHECK(rate_num>=0), rate_den INTEGER NOT NULL CHECK(rate_den>0), inclusive INTEGER NOT NULL DEFAULT 0 CHECK(inclusive IN (0,1)), exempt_category TEXT NOT NULL DEFAULT '', effective_from TEXT NOT NULL, effective_to TEXT, active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), UNIQUE(country_code,tax_code,effective_from))");
        db.execSQL("CREATE TABLE IF NOT EXISTS financial_periods (id INTEGER PRIMARY KEY AUTOINCREMENT, period_code TEXT NOT NULL UNIQUE, start_date TEXT NOT NULL, end_date TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('OPEN','CLOSING','CLOSED')), closed_at INTEGER, CHECK(start_date<=end_date))");
        db.execSQL("CREATE TABLE IF NOT EXISTS chart_of_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, account_code TEXT NOT NULL UNIQUE, name_ar TEXT NOT NULL, name_en TEXT NOT NULL, account_type TEXT NOT NULL CHECK(account_type IN ('ASSET','LIABILITY','EQUITY','REVENUE','EXPENSE')), normal_side TEXT NOT NULL CHECK(normal_side IN ('DEBIT','CREDIT')), parent_id INTEGER REFERENCES chart_of_accounts(id), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)))");
        db.execSQL("CREATE TABLE IF NOT EXISTS gl_journal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_ref TEXT NOT NULL UNIQUE, description TEXT NOT NULL, transaction_date TEXT NOT NULL, period_id INTEGER REFERENCES financial_periods(id), source_type TEXT NOT NULL, source_id TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('DRAFT','POSTED','REVERSED')), reversal_of INTEGER REFERENCES gl_journal_entries(id), created_at INTEGER NOT NULL, UNIQUE(source_type,source_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS gl_journal_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_id INTEGER NOT NULL REFERENCES gl_journal_entries(id) ON DELETE CASCADE, account_id INTEGER NOT NULL REFERENCES chart_of_accounts(id), branch_id INTEGER NOT NULL REFERENCES branches(id), base_currency_code TEXT NOT NULL REFERENCES currencies(code), base_debit_minor INTEGER NOT NULL DEFAULT 0 CHECK(base_debit_minor>=0), base_credit_minor INTEGER NOT NULL DEFAULT 0 CHECK(base_credit_minor>=0), transaction_currency_code TEXT NOT NULL REFERENCES currencies(code), transaction_debit_minor INTEGER NOT NULL DEFAULT 0 CHECK(transaction_debit_minor>=0), transaction_credit_minor INTEGER NOT NULL DEFAULT 0 CHECK(transaction_credit_minor>=0), fx_rate_num INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_num>0), fx_rate_den INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_den>0), CHECK((base_debit_minor>0 AND base_credit_minor=0) OR (base_credit_minor>0 AND base_debit_minor=0)), CHECK((transaction_debit_minor>0 AND transaction_credit_minor=0) OR (transaction_credit_minor>0 AND transaction_debit_minor=0)))");
        db.execSQL("CREATE TABLE IF NOT EXISTS idempotency_keys (key TEXT PRIMARY KEY NOT NULL, operation TEXT NOT NULL, result_ref TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS enterprise_audit_log (id INTEGER PRIMARY KEY AUTOINCREMENT, actor TEXT NOT NULL, action TEXT NOT NULL, entity_type TEXT NOT NULL, entity_ref TEXT NOT NULL, details TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_enterprise_audit_entity ON enterprise_audit_log(entity_type,entity_ref,created_at)");
        db.execSQL("INSERT OR IGNORE INTO chart_of_accounts(account_code,name_ar,name_en,account_type,normal_side) VALUES" +
            "('1000','الصندوق','Cash','ASSET','DEBIT')," +
            "('1010','البنك','Bank','ASSET','DEBIT')," +
            "('1100','العملاء','Accounts Receivable','ASSET','DEBIT')," +
            "('1200','مخزون المواد','Inventory','ASSET','DEBIT')," +
            "('1500','الأصول الثابتة','Fixed Assets','ASSET','DEBIT')," +
            "('1590','مجمع الإهلاك','Accumulated Depreciation','ASSET','CREDIT')," +
            "('2000','الموردون','Accounts Payable','LIABILITY','CREDIT')," +
            "('2100','ضرائب مستحقة','Tax Payable','LIABILITY','CREDIT')," +
            "('3000','رأس المال','Owner Equity','EQUITY','CREDIT')," +
            "('3100','الأرباح المحتجزة','Retained Earnings','EQUITY','CREDIT')," +
            "('4000','إيرادات المبيعات','Sales Revenue','REVENUE','CREDIT')," +
            "('4100','إيرادات أخرى','Other Income','REVENUE','CREDIT')," +
            "('5000','تكلفة المواد المباعة','Cost of Goods Sold','EXPENSE','DEBIT')," +
            "('5100','أجور العمال','Wages Expense','EXPENSE','DEBIT')," +
            "('5200','مصروفات تشغيلية','Operating Expenses','EXPENSE','DEBIT')," +
            "('5300','مصروف الإهلاك','Depreciation Expense','EXPENSE','DEBIT')," +
            "('1210','مخزون الإنتاج التام','Finished Goods Inventory','ASSET','DEBIT')," +
            "('1300','أعمال تحت التشغيل','Work in Progress','ASSET','DEBIT')," +
            "('2200','أجور مستحقة','Accrued Wages','LIABILITY','CREDIT')," +
            "('2300','تكاليف صناعية مستحقة','Accrued Manufacturing Overhead','LIABILITY','CREDIT')," +
            "('4050','مرتجعات المبيعات','Sales Returns','REVENUE','DEBIT')," +
            "('5400','خسائر الهالك والتالف','Spoilage and Waste Loss','EXPENSE','DEBIT')");
        db.execSQL("CREATE TABLE IF NOT EXISTS user_roles (user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE, role_id INTEGER NOT NULL REFERENCES roles(id), branch_id INTEGER NOT NULL REFERENCES branches(id), PRIMARY KEY(user_id,role_id,branch_id))");
        db.execSQL("INSERT OR IGNORE INTO currencies(code,name_ar,name_en,minor_units,active) VALUES('USD','الدولار الأمريكي','US Dollar',2,1),('EUR','اليورو','Euro',2,0),('SAR','الريال السعودي','Saudi Riyal',2,1),('AED','الدرهم الإماراتي','UAE Dirham',2,1)");
        // Only Arab-region business currencies are selectable for operational accounting.
        // USD and EUR are reserved for subscription/payment and must never appear in the company ledger currency picker.
        db.execSQL("INSERT OR IGNORE INTO currencies(code,name_ar,name_en,minor_units,active) VALUES" +
            "('EGP','الجنيه المصري','Egyptian Pound',2,1),('LYD','الدينار الليبي','Libyan Dinar',3,1)," +
            "('SDG','الجنيه السوداني','Sudanese Pound',0,1),('SYP','الليرة السورية','Syrian Pound',2,1)," +
            "('LBP','الليرة اللبنانية','Lebanese Pound',2,1),('ILS','الشيكل الإسرائيلي','Israeli New Shekel',2,1),('JOD','الدينار الأردني','Jordanian Dinar',3,1)," +
            "('KWD','الدينار الكويتي','Kuwaiti Dinar',3,1),('BHD','الدينار البحريني','Bahraini Dinar',3,1)," +
            "('OMR','الريال العماني','Omani Rial',3,1),('QAR','الريال القطري','Qatari Riyal',2,1)," +
            "('IQD','الدينار العراقي','Iraqi Dinar',3,1),('YER','الريال اليمني','Yemeni Rial',2,1)," +
            "('MAD','الدرهم المغربي','Moroccan Dirham',2,1),('DZD','الدينار الجزائري','Algerian Dinar',2,1)," +
            "('TND','الدينار التونسي','Tunisian Dinar',3,1),('MRU','الأوقية الموريتانية','Mauritanian Ouguiya',2,1)," +
            "('SOS','الشلن الصومالي','Somali Shilling',2,1),('DJF','الفرنك الجيبوتي','Djiboutian Franc',0,1)," +
            "('KMF','الفرنك القمري','Comorian Franc',0,1),('ETB','البر الإثيوبي','Ethiopian Birr',2,1)," +
            "('ERN','الناكفا الإريتري','Eritrean Nakfa',2,1),('SSP','جنيه جنوب السودان','South Sudanese Pound',2,1)," +
            "('GBP','الجنيه الإسترليني','Pound Sterling',2,1),('CHF','الفرنك السويسري','Swiss Franc',2,1)," +
            "('CAD','الدولار الكندي','Canadian Dollar',2,1),('AUD','الدولار الأسترالي','Australian Dollar',2,1)," +
            "('NZD','الدولار النيوزيلندي','New Zealand Dollar',2,1),('JPY','الين الياباني','Japanese Yen',0,1)," +
            "('CNY','اليوان الصيني','Chinese Yuan',2,1),('INR','الروبية الهندية','Indian Rupee',2,1)," +
            "('PKR','الروبية الباكستانية','Pakistani Rupee',2,1),('TRY','الليرة التركية','Turkish Lira',2,1)," +
            "('ZAR','الراند الجنوب أفريقي','South African Rand',2,1),('NGN','النيرة النيجيرية','Nigerian Naira',2,1)," +
            "('GHS','السيدي الغاني','Ghanaian Cedi',2,1),('KES','الشلن الكيني','Kenyan Shilling',2,1)," +
            "('UGX','الشلن الأوغندي','Ugandan Shilling',0,1),('TZS','الشلن التنزاني','Tanzanian Shilling',2,1)," +
            "('RWF','الفرنك الرواندي','Rwandan Franc',0,1),('BRL','الريال البرازيلي','Brazilian Real',2,1)," +
            "('MXN','البيزو المكسيكي','Mexican Peso',2,1),('SGD','الدولار السنغافوري','Singapore Dollar',2,1)," +
            "('HKD','دولار هونغ كونغ','Hong Kong Dollar',2,1),('KRW','الوون الكوري','South Korean Won',0,1)," +
            "('THB','البات التايلندي','Thai Baht',2,1),('IDR','الروبية الإندونيسية','Indonesian Rupiah',2,1)," +
            "('MYR','الرنجت الماليزي','Malaysian Ringgit',2,1),('PHP','البيزو الفلبيني','Philippine Peso',2,1)," +
            "('RUB','الروبل الروسي','Russian Ruble',2,1),('UAH','الهريفنيا الأوكرانية','Ukrainian Hryvnia',2,1)");
        // Sudanese Pound records in the existing bakery ledger use whole SDG units.
        db.execSQL("UPDATE currencies SET minor_units=0 WHERE code='SDG'");
        // Apply the operational whitelist once as a schema migration. Never overwrite
        // operator-controlled currency activation during subsequent ensure() calls.
        if(previousSchemaVersion < VERSION) {
            db.execSQL("UPDATE currencies SET active=CASE WHEN code IN ('DZD','BHD','KMF','DJF','EGP','IQD','JOD','KWD','LBP','LYD','MRU','MAD','OMR','ILS','QAR','SAR','SOS','SDG','SYP','TND','AED','YER') THEN 1 ELSE 0 END");
        }
        db.execSQL("CREATE TABLE IF NOT EXISTS customers (id INTEGER PRIMARY KEY AUTOINCREMENT, customer_code TEXT NOT NULL UNIQUE, name TEXT NOT NULL COLLATE NOCASE, phone TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', opening_balance_minor INTEGER NOT NULL DEFAULT 0 CHECK(opening_balance_minor>=0), currency_code TEXT NOT NULL DEFAULT 'SDG' REFERENCES currencies(code), credit_limit_minor INTEGER NOT NULL DEFAULT 0 CHECK(credit_limit_minor>=0), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_customers_name_phone ON customers(name,phone)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_customers_search ON customers(name,phone,active)");
        db.execSQL("CREATE TABLE IF NOT EXISTS company_bank_accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, account_name TEXT NOT NULL, bank_name TEXT NOT NULL, currency_code TEXT NOT NULL REFERENCES currencies(code), iban TEXT NOT NULL DEFAULT '', swift_bic TEXT NOT NULL DEFAULT '', account_number TEXT NOT NULL DEFAULT '', is_default INTEGER NOT NULL DEFAULT 0 CHECK(is_default IN (0,1)), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), created_at INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_company_bank_iban ON company_bank_accounts(currency_code,iban) WHERE iban<>''");
        db.execSQL("CREATE TABLE IF NOT EXISTS party_opening_payments (id INTEGER PRIMARY KEY AUTOINCREMENT, party_type TEXT NOT NULL CHECK(party_type IN ('CUSTOMER','SUPPLIER')), party_id INTEGER NOT NULL, payment_date TEXT NOT NULL, amount_minor INTEGER NOT NULL CHECK(amount_minor>0), method TEXT NOT NULL CHECK(method IN ('CASH','BANK','BANKAK')), currency_code TEXT NOT NULL REFERENCES currencies(code), reference TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_party_opening_payments_party ON party_opening_payments(party_type,party_id,payment_date)");
        db.execSQL("CREATE TABLE IF NOT EXISTS party_payments (id INTEGER PRIMARY KEY AUTOINCREMENT, party_type TEXT NOT NULL CHECK(party_type IN ('CUSTOMER','SUPPLIER')), party_id INTEGER NOT NULL, direction TEXT NOT NULL CHECK(direction IN ('RECEIPT','PAYMENT')), payment_date TEXT NOT NULL, amount_minor INTEGER NOT NULL CHECK(amount_minor>0), method TEXT NOT NULL CHECK(method IN ('CASH','BANK','BANKAK')), currency_code TEXT NOT NULL REFERENCES currencies(code), reference TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_party_payments_party ON party_payments(party_type,party_id,payment_date)");
        db.execSQL("CREATE TABLE IF NOT EXISTS supplier_quick_invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, supplier_id INTEGER NOT NULL REFERENCES suppliers(id), item_name TEXT NOT NULL, quantity_milli INTEGER NOT NULL CHECK(quantity_milli>0), invoice_total_minor INTEGER NOT NULL CHECK(invoice_total_minor>=0), paid_cash_minor INTEGER NOT NULL DEFAULT 0 CHECK(paid_cash_minor>=0), paid_bank_minor INTEGER NOT NULL DEFAULT 0 CHECK(paid_bank_minor>=0), bank_reference TEXT NOT NULL DEFAULT '', debt_minor INTEGER NOT NULL CHECK(debt_minor>=0), currency_code TEXT NOT NULL REFERENCES currencies(code), invoice_date TEXT NOT NULL, created_at INTEGER NOT NULL, CHECK(paid_cash_minor+paid_bank_minor<=invoice_total_minor), CHECK(paid_bank_minor=0 OR length(bank_reference)>0))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_supplier_quick_invoice ON supplier_quick_invoices(supplier_id,invoice_date)");
        db.execSQL("CREATE TABLE IF NOT EXISTS suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT, supplier_code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', address TEXT NOT NULL DEFAULT '', opening_balance_minor INTEGER NOT NULL DEFAULT 0, currency_code TEXT NOT NULL DEFAULT 'SDG' REFERENCES currencies(code), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        try { db.execSQL("ALTER TABLE suppliers ADD COLUMN default_item TEXT NOT NULL DEFAULT ''"); } catch (Exception ignored) { }
        try { db.execSQL("ALTER TABLE suppliers ADD COLUMN default_unit TEXT NOT NULL DEFAULT ''"); } catch (Exception ignored) { }
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_suppliers_name_phone ON suppliers(name,phone)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_suppliers_search ON suppliers(name,phone,active)");
        db.execSQL("CREATE TABLE IF NOT EXISTS purchase_invoices (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_no TEXT NOT NULL, supplier_id INTEGER NOT NULL REFERENCES suppliers(id), branch_id INTEGER NOT NULL REFERENCES branches(id), invoice_date TEXT NOT NULL, due_date TEXT, currency_code TEXT NOT NULL REFERENCES currencies(code), fx_rate_num INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_num>0), fx_rate_den INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_den>0), subtotal_minor INTEGER NOT NULL CHECK(subtotal_minor>=0), tax_minor INTEGER NOT NULL DEFAULT 0 CHECK(tax_minor>=0), total_minor INTEGER NOT NULL CHECK(total_minor>=0), paid_minor INTEGER NOT NULL DEFAULT 0 CHECK(paid_minor>=0 AND paid_minor<=total_minor), status TEXT NOT NULL CHECK(status IN ('DRAFT','POSTED','PART_PAID','PAID','VOID')), notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, UNIQUE(supplier_id,invoice_no))");
        db.execSQL("CREATE TABLE IF NOT EXISTS purchase_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id INTEGER NOT NULL REFERENCES purchase_invoices(id) ON DELETE CASCADE, item_name TEXT NOT NULL, unit TEXT NOT NULL, quantity_milli INTEGER NOT NULL CHECK(quantity_milli>0), unit_cost_minor INTEGER NOT NULL CHECK(unit_cost_minor>=0), line_total_minor INTEGER NOT NULL CHECK(line_total_minor>=0), inventory_item_id INTEGER, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS supplier_payments (id INTEGER PRIMARY KEY AUTOINCREMENT, supplier_id INTEGER NOT NULL REFERENCES suppliers(id), invoice_id INTEGER REFERENCES purchase_invoices(id), payment_date TEXT NOT NULL, amount_minor INTEGER NOT NULL CHECK(amount_minor>0), currency_code TEXT NOT NULL REFERENCES currencies(code), method TEXT NOT NULL, reference TEXT NOT NULL DEFAULT '', branch_id INTEGER NOT NULL REFERENCES branches(id), journal_ref TEXT NOT NULL UNIQUE, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_purchase_supplier_date ON purchase_invoices(supplier_id,invoice_date)");
        db.execSQL("CREATE TABLE IF NOT EXISTS employees (id INTEGER PRIMARY KEY AUTOINCREMENT, employee_code TEXT NOT NULL UNIQUE, full_name TEXT NOT NULL, phone TEXT NOT NULL DEFAULT '', job_title TEXT NOT NULL, hire_date TEXT NOT NULL, base_salary_minor INTEGER NOT NULL CHECK(base_salary_minor>=0), currency_code TEXT NOT NULL REFERENCES currencies(code), branch_id INTEGER NOT NULL REFERENCES branches(id), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS payroll_periods (id INTEGER PRIMARY KEY AUTOINCREMENT, period_code TEXT NOT NULL UNIQUE, start_date TEXT NOT NULL, end_date TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('DRAFT','APPROVED','POSTED','CLOSED')), currency_code TEXT NOT NULL REFERENCES currencies(code), branch_id INTEGER NOT NULL REFERENCES branches(id), journal_ref TEXT UNIQUE, created_at INTEGER NOT NULL, CHECK(start_date<=end_date))");
        db.execSQL("CREATE TABLE IF NOT EXISTS payroll_items (id INTEGER PRIMARY KEY AUTOINCREMENT, period_id INTEGER NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE, employee_id INTEGER NOT NULL REFERENCES employees(id), basic_minor INTEGER NOT NULL CHECK(basic_minor>=0), additions_minor INTEGER NOT NULL DEFAULT 0 CHECK(additions_minor>=0), deductions_minor INTEGER NOT NULL DEFAULT 0 CHECK(deductions_minor>=0), net_minor INTEGER NOT NULL CHECK(net_minor>=0), paid_minor INTEGER NOT NULL DEFAULT 0 CHECK(paid_minor>=0 AND paid_minor<=net_minor), UNIQUE(period_id,employee_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS fixed_assets (id INTEGER PRIMARY KEY AUTOINCREMENT, asset_code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, acquisition_date TEXT NOT NULL, cost_minor INTEGER NOT NULL CHECK(cost_minor>0), residual_minor INTEGER NOT NULL DEFAULT 0 CHECK(residual_minor>=0 AND residual_minor<=cost_minor), useful_life_months INTEGER NOT NULL CHECK(useful_life_months>0), depreciation_method TEXT NOT NULL CHECK(depreciation_method IN ('STRAIGHT_LINE')), currency_code TEXT NOT NULL REFERENCES currencies(code), branch_id INTEGER NOT NULL REFERENCES branches(id), status TEXT NOT NULL CHECK(status IN ('ACTIVE','DISPOSED')), accumulated_depreciation_minor INTEGER NOT NULL DEFAULT 0 CHECK(accumulated_depreciation_minor>=0), created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS depreciation_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, asset_id INTEGER NOT NULL REFERENCES fixed_assets(id), period_code TEXT NOT NULL, amount_minor INTEGER NOT NULL CHECK(amount_minor>=0), accumulated_minor INTEGER NOT NULL CHECK(accumulated_minor>=0), journal_ref TEXT NOT NULL UNIQUE, posted_at INTEGER NOT NULL, UNIQUE(asset_id,period_code))");
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_outbox (id INTEGER PRIMARY KEY AUTOINCREMENT, event_uuid TEXT NOT NULL UNIQUE, entity_type TEXT NOT NULL, entity_id TEXT NOT NULL, operation TEXT NOT NULL CHECK(operation IN ('UPSERT','DELETE')), payload TEXT NOT NULL, created_at INTEGER NOT NULL, attempt_count INTEGER NOT NULL DEFAULT 0, last_error TEXT NOT NULL DEFAULT '', synced_at INTEGER)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sync_outbox_pending ON sync_outbox(synced_at,created_at)");
        TaxJurisdictionProfiles.ensure(db);
        db.execSQL("INSERT OR REPLACE INTO enterprise_meta(key,value) VALUES('schema_version','" + VERSION + "')");
    }

    private static int readSchemaVersion(SQLiteDatabase db) {
        Cursor cursor=db.rawQuery("SELECT value FROM enterprise_meta WHERE key='schema_version'",null);
        try {
            if(!cursor.moveToFirst())return 0;
            try{return Integer.parseInt(cursor.getString(0));}
            catch(NumberFormatException ignored){return 0;}
        } finally {cursor.close();}
    }

    private static void seedRolePermissions(SQLiteDatabase db,String roleCode,String[] permissionCodes) {
        for(String permissionCode:permissionCodes) {
            db.execSQL("INSERT OR IGNORE INTO role_permissions(role_id,permission_code) "+
                "SELECT id,? FROM roles WHERE role_code=?",new Object[]{permissionCode,roleCode});
        }
    }
}
