package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/** JSON backup bridge for the isolated factory database. */
final class FactoryBackup {
    static final String TYPE = "almizan_factory_backup";
    private static final String DB_NAME = "almizan_factory_1.db";
    private static final String[] TABLES = {
        "materials","products","recipe","batches","finished","sales",
        "expenses","waste","movements","collections","cashbook","audit","daily_usage","sales_returns","work_orders","work_order_materials","journal_entries","journal_lines","work_order_stages","supplier_purchase_records","supplier_payment_records","factory_customers","factory_suppliers","factory_gl_account_mappings","factory_gl_outbox","factory_gl_outbox_lines"
    };
    private static final String[][] COLUMNS = {
        {"id","name","unit","qty","value"},
        {"id","name","category","unit_price","unit"},
        {"id","product_id","material_id","qty"},
        {"id","batch_ref","product_id","batches","output","labor","overhead","cost","unit_cost","status","date"},
        {"product_id","qty","value","unit_cost"},
        {"id","product_id","customer","customer_master_id","channel","qty","total","cash","bank","bank_ref","debt","cogs","date"},
        {"id","date","category","amount","note"},
        {"id","product_id","qty","cost","reason","date"},
        {"id","kind","item_id","qty","value","ref","date","supplier_master_id"},
        {"id","customer","customer_master_id","cash","bank","bank_ref","date"},
        {"id","account","kind","amount","ref","date"},
        {"id","action","detail","date"},
        {"id","date","material_id","qty","cost","note","kind"},
        {"id","sale_id","product_id","qty","refund_value","refund_cash","refund_bank","credit_adjustment","restored_cogs","bank_ref","reason","date"},
        {"id","work_ref","product_id","batches","planned_qty","actual_qty","material_cost","labor","overhead","total_cost","status","date"},
        {"id","work_order_id","material_id","qty","cost"},
        {"id","entry_ref","description","status","reversal_of","date"},
        {"id","entry_id","account","debit","credit"},
        {"id","work_order_id","stage_no","output_qty","material_cost","labor","overhead","total_cost","date"},
        {"id","supplier_master_id","supplier_name","item_name","quantity_milli","invoice_total","paid_cash","paid_bank","bank_ref","debt","currency_code","date"},
        {"id","supplier_master_id","supplier_name","cash","bank","bank_ref","date"},
        {"id","name","phone","iban","created_at"},
        {"id","name","phone","iban","created_at"},
        {"source_account","target_account_code","branch_code","currency_code","fx_rate_num","fx_rate_den","active","updated_at"},
        {"id","event_key","source_entry_id","source_entry_ref","description","transaction_date","payload_sha256","payload_version","status","attempt_count","next_attempt_at","last_error","destination_ref","created_at","updated_at","posted_at"},
        {"id","outbox_id","line_no","source_account","debit_minor","credit_minor","target_account_code","branch_code","currency_code","fx_rate_num","fx_rate_den"}
    };
    private static final String[][] REQUIRED = {
        {"id","name","unit","qty","value"},
        {"id","name"},
        {"id","product_id","material_id","qty"},
        {"id","batch_ref","product_id","batches","output","labor","overhead","cost","unit_cost","status","date"},
        {"product_id","qty","value","unit_cost"},
        {"id","product_id","customer","channel","qty","total","cash","bank","debt","cogs","date"},
        {"id","date","category","amount"},
        {"id","product_id","qty","cost","date"},
        {"id","kind","item_id","qty","value","date"},
        {"id","customer","cash","bank","date"},
        {"id","account","kind","amount","date"},
        {"id","action","detail","date"},
        {"id","date","material_id","qty","cost"},
        {"id","sale_id","product_id","qty","refund_value","refund_cash","refund_bank","credit_adjustment","restored_cogs","date"},
        {"id","work_ref","product_id","batches","planned_qty","material_cost","status","date"},
        {"id","work_order_id","material_id","qty","cost"},
        {"id","entry_ref","status","date"},
        {"id","entry_id","account","debit","credit"},
        {"id","work_order_id","stage_no","output_qty","material_cost","labor","overhead","total_cost","date"},
        {"id","supplier_master_id","supplier_name","item_name","quantity_milli","invoice_total","paid_cash","paid_bank","debt","currency_code","date"},
        {"id","supplier_master_id","supplier_name","cash","bank","date"},
        {"id","name"},
        {"id","name"},
        {"source_account","target_account_code","branch_code","currency_code","fx_rate_num","fx_rate_den","active","updated_at"},
        {"id","event_key","source_entry_id","source_entry_ref","description","transaction_date","payload_sha256","status","attempt_count","next_attempt_at","last_error","destination_ref","created_at","updated_at"},
        {"id","outbox_id","line_no","source_account","debit_minor","credit_minor"}
    };

    private FactoryBackup() {}

    // Keep backup validation compatible with the declared Android API 28 minimum.
    private static long safeAdd(long a,long b) {
        if (a<0 || b<0 || Long.MAX_VALUE-a<b) throw new IllegalArgumentException("Invalid or overflowing backup total");
        return a+b;
    }

    static JSONObject export(Context context) throws Exception {
        SQLiteDatabase db = open(context);
        try {
            ensureSchema(db);
            JSONObject root = new JSONObject();
            root.put("_type", TYPE);
            root.put("schema_version", 11);
            JSONObject tables = new JSONObject();
            for (int i=0;i<TABLES.length;i++) {
                JSONArray rows = new JSONArray();
                Cursor c = db.rawQuery("SELECT * FROM " + TABLES[i], null);
                try {
                    while (c.moveToNext()) {
                        JSONObject row = new JSONObject();
                        for (int j=0;j<c.getColumnCount();j++) {
                            int type = c.getType(j);
                            if (type == Cursor.FIELD_TYPE_NULL) row.put(c.getColumnName(j), JSONObject.NULL);
                            else if (type == Cursor.FIELD_TYPE_INTEGER) row.put(c.getColumnName(j), c.getLong(j));
                            else if (type == Cursor.FIELD_TYPE_FLOAT) row.put(c.getColumnName(j), c.getDouble(j));
                            else if (type == Cursor.FIELD_TYPE_BLOB) throw new IllegalStateException("Unsupported BLOB in factory backup");
                            else row.put(c.getColumnName(j), c.getString(j));
                        }
                        rows.put(row);
                    }
                } finally { c.close(); }
                tables.put(TABLES[i], rows);
            }
            JSONObject profile=new JSONObject();
            profile.put("business_name",context.getSharedPreferences("factory_profile",Context.MODE_PRIVATE).getString("business_name",""));
            root.put("profile",profile);
            root.put("tables", tables);
            return root;
        } finally { db.close(); }
    }

    static void validate(JSONObject root) throws Exception {
        if (root == null || !TYPE.equals(root.optString("_type")) || (root.optInt("schema_version") < 3 || root.optInt("schema_version") > 11))
            throw new IllegalArgumentException("Unsupported factory backup format");
        JSONObject tables = root.optJSONObject("tables");
        if (tables == null) throw new IllegalArgumentException("Factory backup tables are missing");
        if(root.optInt("schema_version")>=9){
            JSONObject profile=root.optJSONObject("profile");
            if(profile==null||!profile.has("business_name"))throw new IllegalArgumentException("Factory backup identity is missing");
        }
        for (int i=0;i<TABLES.length;i++) {
            JSONArray rows = tables.optJSONArray(TABLES[i]);
            if (rows == null && root.optInt("schema_version")<5 && Arrays.asList("sales_returns","work_orders","work_order_materials","journal_entries","journal_lines").contains(TABLES[i])) continue;
            if (rows == null && root.optInt("schema_version")<6 && "work_order_stages".equals(TABLES[i])) continue;
            if (rows == null && root.optInt("schema_version")<7 && Arrays.asList("supplier_purchase_records","supplier_payment_records").contains(TABLES[i])) continue;
            if (rows == null && root.optInt("schema_version")<8 && Arrays.asList("factory_customers","factory_suppliers").contains(TABLES[i])) continue;
            if (rows == null && root.optInt("schema_version")<10 && Arrays.asList("factory_gl_account_mappings","factory_gl_outbox","factory_gl_outbox_lines").contains(TABLES[i])) continue;
            if (rows == null && root.optInt("schema_version")==3 && "daily_usage".equals(TABLES[i])) continue;
            if (rows == null) throw new IllegalArgumentException("Missing factory table: " + TABLES[i]);
            for (int n=0;n<rows.length();n++) {
                JSONObject row = rows.getJSONObject(n);
                for (String key : REQUIRED[i]) {
                    if (!row.has(key) || row.isNull(key)) throw new IllegalArgumentException("Missing factory field: " + TABLES[i] + "." + key);
                }
                if(root.optInt("schema_version")>=11&&"factory_gl_outbox".equals(TABLES[i])&&!row.has("payload_version"))
                    throw new IllegalArgumentException("Missing factory outbox payload version");
                for (java.util.Iterator<String> it=row.keys();it.hasNext();) {
                    String key=it.next();
                    if (!Arrays.asList(COLUMNS[i]).contains(key)) throw new IllegalArgumentException("Unknown factory field");
                }
                validateRow(TABLES[i], row);
            }
        }
        validateLedgerRelations(tables,root.optInt("schema_version"));
    }

    private static void validateLedgerRelations(JSONObject tables,int version) throws Exception {
        JSONArray sales=tables.optJSONArray("sales"), returns=tables.optJSONArray("sales_returns");
        if(version>=5&&sales!=null&&returns!=null){
            for(int i=0;i<sales.length();i++){
                JSONObject sale=sales.getJSONObject(i);long returned=0;
                for(int j=0;j<returns.length();j++){JSONObject r=returns.getJSONObject(j);if(r.getLong("sale_id")==sale.getLong("id"))returned=safeAdd(returned,r.getLong("qty"));}
                if(returned>sale.getLong("qty"))throw new IllegalArgumentException("Returned quantity exceeds original sale");
            }
        }
        JSONArray entries=tables.optJSONArray("journal_entries"),lines=tables.optJSONArray("journal_lines");
        if(version>=5&&entries!=null&&lines!=null){
            for(int i=0;i<entries.length();i++){
                long id=entries.getJSONObject(i).getLong("id"),debit=0,credit=0;int count=0;
                for(int j=0;j<lines.length();j++){JSONObject line=lines.getJSONObject(j);if(line.getLong("entry_id")==id){debit=safeAdd(debit,line.getLong("debit"));credit=safeAdd(credit,line.getLong("credit"));count++;}}
                if(count<2||debit!=credit)throw new IllegalArgumentException("Unbalanced or incomplete journal entry in backup");
            }
            for(int j=0;j<lines.length();j++){
                long id=lines.getJSONObject(j).getLong("entry_id");boolean found=false;
                for(int i=0;i<entries.length();i++)if(entries.getJSONObject(i).getLong("id")==id){found=true;break;}
                if(!found)throw new IllegalArgumentException("Orphan journal line in backup");
            }
        }
        if(version>=10){
            JSONArray outbox=tables.optJSONArray("factory_gl_outbox"),outboxLines=tables.optJSONArray("factory_gl_outbox_lines");
            for(int i=0;i<outbox.length();i++){
                JSONObject event=outbox.getJSONObject(i);long sourceId=event.getLong("source_entry_id");
                boolean sourceFound=false;for(int j=0;j<entries.length();j++){JSONObject entry=entries.getJSONObject(j);if(entry.getLong("id")==sourceId&&entry.getString("entry_ref").equals(event.getString("source_entry_ref"))){sourceFound=true;break;}}
                if(!sourceFound)throw new IllegalArgumentException("Outbox event has no matching factory journal");
                long debit=0,credit=0;int count=0;
                for(int j=0;j<outboxLines.length();j++){JSONObject line=outboxLines.getJSONObject(j);if(line.getLong("outbox_id")==event.getLong("id")){debit=safeAdd(debit,line.getLong("debit_minor"));credit=safeAdd(credit,line.getLong("credit_minor"));count++;}}
                if(count<2||debit!=credit)throw new IllegalArgumentException("Unbalanced factory outbox snapshot");
                String status=event.getString("status");
                if(!Arrays.asList("PENDING","PROCESSING","POSTED","RETRY","DEAD_LETTER").contains(status))throw new IllegalArgumentException("Invalid factory outbox status");
                if("POSTED".equals(status)&&(event.optString("destination_ref").trim().isEmpty()||event.isNull("posted_at")))throw new IllegalArgumentException("Posted outbox event is missing destination acknowledgement");
            }
            for(int i=0;i<outboxLines.length();i++){
                JSONObject line=outboxLines.getJSONObject(i);boolean found=false;
                for(int j=0;j<outbox.length();j++)if(outbox.getJSONObject(j).getLong("id")==line.getLong("outbox_id")){found=true;break;}
                if(!found)throw new IllegalArgumentException("Orphan factory outbox line");
            }
        }
    }

    private static void validateRow(String table, JSONObject row) throws Exception {
        String[] numeric;
        if ("materials".equals(table)) numeric=new String[]{"id","qty","value"};
        else if ("products".equals(table)) numeric=new String[]{"id"};
        else if ("daily_usage".equals(table)) numeric=new String[]{"id","material_id","qty","cost"};
        else if ("sales_returns".equals(table)) numeric=new String[]{"id","sale_id","product_id","qty","refund_value","refund_cash","refund_bank","credit_adjustment","restored_cogs"};
        else if ("work_orders".equals(table)) numeric=new String[]{"id","product_id","batches","planned_qty","actual_qty","material_cost","labor","overhead","total_cost"};
        else if ("work_order_materials".equals(table)) numeric=new String[]{"id","work_order_id","material_id","qty","cost"};
        else if ("journal_lines".equals(table)) numeric=new String[]{"id","entry_id","debit","credit"};
        else if ("journal_entries".equals(table)) numeric=new String[]{"id"};
        else if ("work_order_stages".equals(table)) numeric=new String[]{"id","work_order_id","stage_no","output_qty","material_cost","labor","overhead","total_cost"};
        else if ("supplier_purchase_records".equals(table)) numeric=new String[]{"id","supplier_master_id","quantity_milli","invoice_total","paid_cash","paid_bank","debt"};
        else if ("supplier_payment_records".equals(table)) numeric=new String[]{"id","supplier_master_id","cash","bank"};
        else if ("factory_customers".equals(table) || "factory_suppliers".equals(table)) numeric=new String[]{"id"};
        else if ("factory_gl_account_mappings".equals(table)) numeric=new String[]{"fx_rate_num","fx_rate_den","active","updated_at"};
        else if ("factory_gl_outbox".equals(table)) numeric=new String[]{"id","source_entry_id","payload_version","attempt_count","next_attempt_at","created_at","updated_at"};
        else if ("factory_gl_outbox_lines".equals(table)) numeric=new String[]{"id","outbox_id","line_no","debit_minor","credit_minor"};
        else if ("recipe".equals(table)) numeric=new String[]{"id","product_id","material_id","qty"};
        else if ("batches".equals(table)) numeric=new String[]{"id","product_id","batches","output","labor","overhead","cost","unit_cost"};
        else if ("finished".equals(table)) numeric=new String[]{"product_id","qty","value","unit_cost"};
        else if ("sales".equals(table)) numeric=new String[]{"id","product_id","qty","total","cash","bank","debt","cogs"};
        else if ("expenses".equals(table)) numeric=new String[]{"id","amount"};
        else if ("waste".equals(table)) numeric=new String[]{"id","product_id","qty","cost"};
        else if ("movements".equals(table)) numeric=new String[]{"id","item_id","qty","value"};
        else if ("collections".equals(table)) numeric=new String[]{"id","cash","bank"};
        else if ("cashbook".equals(table)) numeric=new String[]{"id","amount"};
        else numeric=new String[]{"id"};
        for (String key : numeric) {
            if ("products".equals(table) && "unit_price".equals(key) && !row.has(key)) continue;
            if ("factory_gl_outbox".equals(table) && "payload_version".equals(key) && !row.has(key)) continue;
            Object value=row.get(key);
            if (!(value instanceof Number)) throw new IllegalArgumentException("Invalid numeric factory field");
            Number number=(Number)value;
            long exact=number.longValue();
            if (number.doubleValue()!=((double)exact) || exact<0) throw new IllegalArgumentException("Negative or fractional factory integer");
        }
        if ("daily_usage".equals(table) && row.getLong("qty")==0) throw new IllegalArgumentException("Daily usage quantity must be positive");
        if ("sales_returns".equals(table) && (row.getLong("qty")==0 || row.getLong("refund_value")==0 || safeAdd(safeAdd(row.getLong("refund_cash"),row.getLong("refund_bank")),row.getLong("credit_adjustment"))!=row.getLong("refund_value"))) throw new IllegalArgumentException("Invalid sales return settlement");
        if ("work_orders".equals(table) && (row.getLong("batches")==0 || row.getLong("planned_qty")==0)) throw new IllegalArgumentException("Invalid work order");
        if ("journal_lines".equals(table) && safeAdd(row.getLong("debit"),row.getLong("credit"))==0) throw new IllegalArgumentException("Empty journal line");
        if ("recipe".equals(table) && row.getLong("qty")==0) throw new IllegalArgumentException("Recipe quantity must be positive");
        if ("batches".equals(table) && (row.getLong("batches")==0 || row.getLong("output")==0)) throw new IllegalArgumentException("Invalid production batch quantity");
        if ("sales".equals(table)) {
            long total=row.getLong("total"), cash=row.getLong("cash"), bank=row.getLong("bank"), debt=row.getLong("debt");
            if (total==0 || row.getLong("qty")==0 || safeAdd(safeAdd(cash,bank),debt)!=total)
                throw new IllegalArgumentException("Invalid sale settlement in backup");
            if (bank>0 && row.optString("bank_ref").trim().isEmpty()) throw new IllegalArgumentException("Missing Bankak reference in backup");
        }
        if ("collections".equals(table)) {
            if (safeAdd(row.getLong("cash"),row.getLong("bank"))==0) throw new IllegalArgumentException("Empty customer collection");
            if (row.getLong("bank")>0 && row.optString("bank_ref").trim().isEmpty()) throw new IllegalArgumentException("Missing collection Bankak reference");
        }
        if ("expenses".equals(table) && row.getLong("amount")==0) throw new IllegalArgumentException("Expense amount must be positive");
        if ("waste".equals(table) && row.getLong("qty")==0) throw new IllegalArgumentException("Waste quantity must be positive");
        if ("cashbook".equals(table) && row.getLong("amount")==0) throw new IllegalArgumentException("Cashbook amount must be positive");
        if ("supplier_purchase_records".equals(table)) { long total=row.getLong("invoice_total"),cash=row.getLong("paid_cash"),bank=row.getLong("paid_bank"),debt=row.getLong("debt"); if(row.getLong("quantity_milli")==0||total==0||safeAdd(safeAdd(cash,bank),debt)!=total||(bank>0&&row.optString("bank_ref").trim().isEmpty()))throw new IllegalArgumentException("Invalid supplier invoice settlement"); }
        if ("supplier_payment_records".equals(table) && safeAdd(row.getLong("cash"),row.getLong("bank"))==0) throw new IllegalArgumentException("Empty supplier payment");
        if ("supplier_payment_records".equals(table) && row.getLong("bank")>0 && row.optString("bank_ref").trim().isEmpty()) throw new IllegalArgumentException("Missing supplier Bankak reference");
        if ("factory_gl_account_mappings".equals(table) && (row.getLong("fx_rate_num")==0 || row.getLong("fx_rate_den")==0 || row.getLong("active")>1 || row.optString("source_account").trim().isEmpty() || row.optString("target_account_code").trim().isEmpty())) throw new IllegalArgumentException("Invalid factory GL account mapping");
        if ("factory_gl_outbox".equals(table)) {
            if(row.has("payload_version")&&row.getInt("payload_version")<1)throw new IllegalArgumentException("Invalid factory outbox payload version");
            String status=row.optString("status");
            if (!Arrays.asList("PENDING","PROCESSING","POSTED","RETRY","DEAD_LETTER").contains(status) || !row.optString("payload_sha256").matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid factory GL outbox event");
            if (!row.isNull("posted_at") && !(row.get("posted_at") instanceof Number)) throw new IllegalArgumentException("Invalid factory GL posted timestamp");
        }
        if ("factory_gl_outbox_lines".equals(table)) {
            long debit=row.getLong("debit_minor"),credit=row.getLong("credit_minor");
            if (((debit>0)==(credit>0)) || debit<0 || credit<0) throw new IllegalArgumentException("Invalid factory GL outbox journal line");
            boolean anyMapping=!row.isNull("target_account_code")||!row.isNull("branch_code")||!row.isNull("currency_code")||!row.isNull("fx_rate_num")||!row.isNull("fx_rate_den");
            boolean fullMapping=!row.isNull("target_account_code")&&!row.isNull("branch_code")&&!row.isNull("currency_code")&&!row.isNull("fx_rate_num")&&!row.isNull("fx_rate_den");
            if (anyMapping&&!fullMapping) throw new IllegalArgumentException("Partial factory GL outbox account mapping");
            if (fullMapping&&(row.getLong("fx_rate_num")==0||row.getLong("fx_rate_den")==0)) throw new IllegalArgumentException("Invalid factory GL outbox exchange rate");
        }
    }

    static void restore(Context context, JSONObject root) throws Exception {
        validate(root);
        SQLiteDatabase db = open(context);
        try {
            ensureSchema(db);
            db.beginTransaction();
            try {
                JSONObject tables = root.getJSONObject("tables");
                // A restore replaces the factory snapshot; it must not merge with newer rows.
                // Delete child tables first so old backups cannot leave stale ledger records behind.
                for (int i=TABLES.length-1;i>=0;i--) db.delete(TABLES[i], null, null);
                for (int i=0;i<TABLES.length;i++) {
                    JSONArray rows = tables.optJSONArray(TABLES[i]);
                    if (rows == null && root.optInt("schema_version")<5 && Arrays.asList("sales_returns","work_orders","work_order_materials","journal_entries","journal_lines").contains(TABLES[i])) continue;
                    if (rows == null && root.optInt("schema_version")<6 && "work_order_stages".equals(TABLES[i])) continue;
                    if (rows == null && root.optInt("schema_version")<7 && Arrays.asList("supplier_purchase_records","supplier_payment_records").contains(TABLES[i])) continue;
                    if (rows == null && root.optInt("schema_version")<8 && Arrays.asList("factory_customers","factory_suppliers").contains(TABLES[i])) continue;
                    if (rows == null && root.optInt("schema_version")<10 && Arrays.asList("factory_gl_account_mappings","factory_gl_outbox","factory_gl_outbox_lines").contains(TABLES[i])) continue;
                    if (rows == null && root.optInt("schema_version")==3 && "daily_usage".equals(TABLES[i])) continue;
                    if (rows == null) throw new IllegalArgumentException("Missing factory table: "+TABLES[i]);
                    for (int n=0;n<rows.length();n++) {
                        JSONObject row = rows.getJSONObject(n);
                        ContentValues values = new ContentValues();
                        for (String key : COLUMNS[i]) {
                            if (!row.has(key)) continue;
                            Object value = row.get(key);
                            if (value == JSONObject.NULL) values.putNull(key);
                            else if (value instanceof Number) values.put(key, ((Number)value).longValue());
                            else if (value instanceof Boolean) values.put(key, ((Boolean)value) ? 1 : 0);
                            else values.put(key, String.valueOf(value));
                        }
                        db.insertWithOnConflict(TABLES[i], null, values, SQLiteDatabase.CONFLICT_REPLACE);
                    }
                }
                ensureFactoryPartyMasters(db);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            if(root.optInt("schema_version")>=9){
                JSONObject profile=root.getJSONObject("profile");
                context.getSharedPreferences("factory_profile",Context.MODE_PRIVATE).edit()
                    .putString("business_name",profile.optString("business_name","")).apply();
            }
        } finally { db.close(); }
    }

    private static void ensureFactoryPartyMasters(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE IF NOT EXISTS factory_customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        d.execSQL("CREATE TABLE IF NOT EXISTS factory_suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
        d.execSQL("INSERT OR IGNORE INTO factory_customers(name) SELECT DISTINCT TRIM(customer) FROM sales WHERE customer IS NOT NULL AND TRIM(customer)<>''");
        d.execSQL("INSERT OR IGNORE INTO factory_customers(name) SELECT DISTINCT TRIM(customer) FROM collections WHERE customer IS NOT NULL AND TRIM(customer)<>''");
        d.execSQL("UPDATE sales SET customer_master_id=COALESCE((SELECT id FROM factory_customers p WHERE p.name=TRIM(sales.customer)),0)");
        d.execSQL("UPDATE collections SET customer_master_id=COALESCE((SELECT id FROM factory_customers p WHERE p.name=TRIM(collections.customer)),0)");
        d.execSQL("INSERT OR IGNORE INTO factory_suppliers(name) SELECT DISTINCT TRIM(supplier_name) FROM supplier_purchase_records WHERE supplier_name IS NOT NULL AND TRIM(supplier_name)<>''");
        d.execSQL("INSERT OR IGNORE INTO factory_suppliers(name) SELECT DISTINCT TRIM(supplier_name) FROM supplier_payment_records WHERE supplier_name IS NOT NULL AND TRIM(supplier_name)<>''");
        d.execSQL("UPDATE supplier_purchase_records SET supplier_master_id=COALESCE((SELECT id FROM factory_suppliers p WHERE p.name=TRIM(supplier_purchase_records.supplier_name)),0)");
        d.execSQL("UPDATE supplier_payment_records SET supplier_master_id=COALESCE((SELECT id FROM factory_suppliers p WHERE p.name=TRIM(supplier_payment_records.supplier_name)),0)");
        d.execSQL("UPDATE movements SET supplier_master_id=0");
        d.execSQL("UPDATE movements SET ref=COALESCE((SELECT sp.supplier_name FROM supplier_purchase_records sp WHERE substr(movements.ref,1,length(sp.supplier_name)+1)=sp.supplier_name||':' ORDER BY length(sp.supplier_name) DESC LIMIT 1),ref) WHERE kind='RAW_IN' AND instr(ref,':')>0");
        d.execSQL("UPDATE movements SET supplier_master_id=COALESCE((SELECT id FROM factory_suppliers p WHERE p.name=movements.ref),0) WHERE kind='RAW_IN'");
    }

    private static void ensureColumn(SQLiteDatabase d,String table,String column,String definition){Cursor c=d.rawQuery("PRAGMA table_info("+table+")",null);boolean found=false;try{while(c.moveToNext())if(column.equals(c.getString(1)))found=true;}finally{c.close();}if(!found)d.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}

    private static SQLiteDatabase open(Context context) {
        return SecureDatabase.open(context, DB_NAME);
    }

    private static void ensureSchema(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE IF NOT EXISTS materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY,name TEXT UNIQUE,category TEXT NOT NULL DEFAULT 'منتج إضافي',unit_price INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT 'قطعة')");
        d.execSQL("CREATE TABLE IF NOT EXISTS recipe(id INTEGER PRIMARY KEY,product_id INTEGER,material_id INTEGER,qty INTEGER CHECK(qty>0),UNIQUE(product_id,material_id))");
        d.execSQL("CREATE TABLE IF NOT EXISTS batches(id INTEGER PRIMARY KEY,batch_ref TEXT UNIQUE,product_id INTEGER,batches INTEGER,output INTEGER,labor INTEGER,overhead INTEGER,cost INTEGER,unit_cost INTEGER,status TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS finished(product_id INTEGER PRIMARY KEY,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0,unit_cost INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY,product_id INTEGER,customer TEXT,customer_master_id INTEGER NOT NULL DEFAULT 0,channel TEXT,qty INTEGER,total INTEGER,cash INTEGER,bank INTEGER,bank_ref TEXT,debt INTEGER,cogs INTEGER,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY,date TEXT,category TEXT,amount INTEGER,note TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS waste(id INTEGER PRIMARY KEY,product_id INTEGER,qty INTEGER,cost INTEGER,reason TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS movements(id INTEGER PRIMARY KEY,kind TEXT,item_id INTEGER,qty INTEGER,value INTEGER,ref TEXT,date TEXT,supplier_master_id INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS collections(id INTEGER PRIMARY KEY,customer TEXT,customer_master_id INTEGER NOT NULL DEFAULT 0,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER NOT NULL,ref TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY,action TEXT,detail TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS daily_usage(id INTEGER PRIMARY KEY,date TEXT,material_id INTEGER,qty INTEGER,cost INTEGER,note TEXT,kind TEXT NOT NULL DEFAULT 'MANUAL')");
        d.execSQL("CREATE TABLE IF NOT EXISTS sales_returns(id INTEGER PRIMARY KEY,sale_id INTEGER NOT NULL,product_id INTEGER NOT NULL,qty INTEGER NOT NULL,refund_value INTEGER NOT NULL,refund_cash INTEGER NOT NULL,refund_bank INTEGER NOT NULL,credit_adjustment INTEGER NOT NULL,restored_cogs INTEGER NOT NULL,bank_ref TEXT,reason TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS work_orders(id INTEGER PRIMARY KEY,work_ref TEXT UNIQUE,product_id INTEGER NOT NULL,batches INTEGER NOT NULL,planned_qty INTEGER NOT NULL,actual_qty INTEGER NOT NULL DEFAULT 0,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL DEFAULT 0,overhead INTEGER NOT NULL DEFAULT 0,total_cost INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS work_order_materials(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,material_id INTEGER NOT NULL,qty INTEGER NOT NULL,cost INTEGER NOT NULL)");
        d.execSQL("CREATE TABLE IF NOT EXISTS work_order_stages(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,stage_no INTEGER NOT NULL,output_qty INTEGER NOT NULL,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL,overhead INTEGER NOT NULL,total_cost INTEGER NOT NULL,date TEXT,UNIQUE(work_order_id,stage_no))");
        d.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
        ensureColumn(d,"daily_usage","kind","TEXT NOT NULL DEFAULT 'MANUAL'");
        ensureColumn(d,"products","category","TEXT NOT NULL DEFAULT 'منتج إضافي'");
        ensureColumn(d,"products","unit_price","INTEGER NOT NULL DEFAULT 0");
         ensureColumn(d,"products","unit","TEXT NOT NULL DEFAULT 'قطعة'");
        d.execSQL("CREATE TABLE IF NOT EXISTS supplier_purchase_records(id INTEGER PRIMARY KEY,supplier_master_id INTEGER NOT NULL,supplier_name TEXT NOT NULL,item_name TEXT NOT NULL,quantity_milli INTEGER NOT NULL,invoice_total INTEGER NOT NULL,paid_cash INTEGER NOT NULL,paid_bank INTEGER NOT NULL,bank_ref TEXT NOT NULL DEFAULT '',debt INTEGER NOT NULL,currency_code TEXT NOT NULL DEFAULT 'SDG',date TEXT NOT NULL)");
        d.execSQL("CREATE TABLE IF NOT EXISTS supplier_payment_records(id INTEGER PRIMARY KEY,supplier_master_id INTEGER NOT NULL,supplier_name TEXT NOT NULL,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT NOT NULL DEFAULT '',date TEXT NOT NULL)");
         d.execSQL("CREATE TABLE IF NOT EXISTS factory_customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
         d.execSQL("CREATE TABLE IF NOT EXISTS factory_suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
         d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_factory_customers_phone_unique ON factory_customers(phone) WHERE phone<>''");
         d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_factory_suppliers_phone_unique ON factory_suppliers(phone) WHERE phone<>''");
        ensureColumn(d,"sales","customer_master_id","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(d,"collections","customer_master_id","INTEGER NOT NULL DEFAULT 0");
        ensureColumn(d,"movements","supplier_master_id","INTEGER NOT NULL DEFAULT 0");
        FactoryGlOutbox.ensureSchema(d);
        if (d.getVersion() < 13) d.setVersion(13);
    }
}
