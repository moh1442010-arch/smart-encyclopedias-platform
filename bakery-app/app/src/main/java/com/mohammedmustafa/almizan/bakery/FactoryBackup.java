package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/** JSON backup bridge for the isolated factory database. */
final class FactoryBackup {
    static final String TYPE = "almizan_factory_backup";
    private static final String DB_NAME = "almizan_factory_1.db";
    private static final String[] TABLES = {
        "materials","products","recipe","batches","finished","sales",
        "expenses","waste","movements","collections","cashbook","audit","daily_usage","sales_returns","work_orders","work_order_materials","journal_entries","journal_lines"
    };
    private static final String[][] COLUMNS = {
        {"id","name","unit","qty","value"},
        {"id","name","category","unit_price"},
        {"id","product_id","material_id","qty"},
        {"id","batch_ref","product_id","batches","output","labor","overhead","cost","unit_cost","status","date"},
        {"product_id","qty","value","unit_cost"},
        {"id","product_id","customer","channel","qty","total","cash","bank","bank_ref","debt","cogs","date"},
        {"id","date","category","amount","note"},
        {"id","product_id","qty","cost","reason","date"},
        {"id","kind","item_id","qty","value","ref","date"},
        {"id","customer","cash","bank","bank_ref","date"},
        {"id","account","kind","amount","ref","date"},
        {"id","action","detail","date"},
        {"id","date","material_id","qty","cost","note","kind"},
        {"id","sale_id","product_id","qty","refund_value","refund_cash","refund_bank","credit_adjustment","restored_cogs","bank_ref","reason","date"},
        {"id","work_ref","product_id","batches","planned_qty","actual_qty","material_cost","labor","overhead","total_cost","status","date"},
        {"id","work_order_id","material_id","qty","cost"},
        {"id","entry_ref","description","status","reversal_of","date"},
        {"id","entry_id","account","debit","credit"}
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
        {"id","entry_id","account","debit","credit"}
    };

    private FactoryBackup() {}

    static JSONObject export(Context context) throws Exception {
        SQLiteDatabase db = open(context);
        try {
            ensureSchema(db);
            JSONObject root = new JSONObject();
            root.put("_type", TYPE);
            root.put("schema_version", 5);
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
            root.put("tables", tables);
            return root;
        } finally { db.close(); }
    }

    static void validate(JSONObject root) throws Exception {
        if (root == null || !TYPE.equals(root.optString("_type")) || (root.optInt("schema_version") != 3 && root.optInt("schema_version") != 4 && root.optInt("schema_version") != 5))
            throw new IllegalArgumentException("Unsupported factory backup format");
        JSONObject tables = root.optJSONObject("tables");
        if (tables == null) throw new IllegalArgumentException("Factory backup tables are missing");
        for (int i=0;i<TABLES.length;i++) {
            JSONArray rows = tables.optJSONArray(TABLES[i]);
            if (rows == null && root.optInt("schema_version")<5 && Arrays.asList("sales_returns","work_orders","work_order_materials","journal_entries","journal_lines").contains(TABLES[i])) continue;
            if (rows == null && root.optInt("schema_version")==3 && "daily_usage".equals(TABLES[i])) continue;
            if (rows == null) throw new IllegalArgumentException("Missing factory table: " + TABLES[i]);
            for (int n=0;n<rows.length();n++) {
                JSONObject row = rows.getJSONObject(n);
                for (String key : REQUIRED[i]) {
                    if (!row.has(key) || row.isNull(key)) throw new IllegalArgumentException("Missing factory field: " + TABLES[i] + "." + key);
                }
                for (java.util.Iterator<String> it=row.keys();it.hasNext();) {
                    String key=it.next();
                    if (!Arrays.asList(COLUMNS[i]).contains(key)) throw new IllegalArgumentException("Unknown factory field");
                }
                validateRow(TABLES[i], row);
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
        else if ("journal_entries".equals(table)) numeric=new String[]{"id","reversal_of"};
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
            Object value=row.get(key);
            if (!(value instanceof Number)) throw new IllegalArgumentException("Invalid numeric factory field");
            Number number=(Number)value;
            long exact=number.longValue();
            if (number.doubleValue()!=((double)exact) || exact<0) throw new IllegalArgumentException("Negative or fractional factory integer");
        }
        if ("daily_usage".equals(table) && row.getLong("qty")==0) throw new IllegalArgumentException("Daily usage quantity must be positive");
        if ("sales_returns".equals(table) && (row.getLong("qty")==0 || row.getLong("refund_value")==0 || Math.addExact(Math.addExact(row.getLong("refund_cash"),row.getLong("refund_bank")),row.getLong("credit_adjustment"))!=row.getLong("refund_value"))) throw new IllegalArgumentException("Invalid sales return settlement");
        if ("work_orders".equals(table) && (row.getLong("batches")==0 || row.getLong("planned_qty")==0)) throw new IllegalArgumentException("Invalid work order");
        if ("journal_lines".equals(table) && Math.addExact(row.getLong("debit"),row.getLong("credit"))==0) throw new IllegalArgumentException("Empty journal line");
        if ("recipe".equals(table) && row.getLong("qty")==0) throw new IllegalArgumentException("Recipe quantity must be positive");
        if ("batches".equals(table) && (row.getLong("batches")==0 || row.getLong("output")==0)) throw new IllegalArgumentException("Invalid production batch quantity");
        if ("sales".equals(table)) {
            long total=row.getLong("total"), cash=row.getLong("cash"), bank=row.getLong("bank"), debt=row.getLong("debt");
            if (total==0 || row.getLong("qty")==0 || Math.addExact(Math.addExact(cash,bank),debt)!=total)
                throw new IllegalArgumentException("Invalid sale settlement in backup");
            if (bank>0 && row.optString("bank_ref").trim().isEmpty()) throw new IllegalArgumentException("Missing Bankak reference in backup");
        }
        if ("collections".equals(table)) {
            if (Math.addExact(row.getLong("cash"),row.getLong("bank"))==0) throw new IllegalArgumentException("Empty customer collection");
            if (row.getLong("bank")>0 && row.optString("bank_ref").trim().isEmpty()) throw new IllegalArgumentException("Missing collection Bankak reference");
        }
        if ("expenses".equals(table) && row.getLong("amount")==0) throw new IllegalArgumentException("Expense amount must be positive");
        if ("waste".equals(table) && row.getLong("qty")==0) throw new IllegalArgumentException("Waste quantity must be positive");
        if ("cashbook".equals(table) && row.getLong("amount")==0) throw new IllegalArgumentException("Cashbook amount must be positive");
    }

    static void restore(Context context, JSONObject root) throws Exception {
        validate(root);
        SQLiteDatabase db = open(context);
        try {
            ensureSchema(db);
            db.beginTransaction();
            try {
                JSONObject tables = root.getJSONObject("tables");
                for (int i=0;i<TABLES.length;i++) {
                    JSONArray rows = tables.optJSONArray(TABLES[i]);
                    if (rows == null && root.optInt("schema_version")<5 && Arrays.asList("sales_returns","work_orders","work_order_materials","journal_entries","journal_lines").contains(TABLES[i])) continue;
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
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        } finally { db.close(); }
    }

    private static void ensureColumn(SQLiteDatabase d,String table,String column,String definition){Cursor c=d.rawQuery("PRAGMA table_info("+table+")",null);boolean found=false;try{while(c.moveToNext())if(column.equals(c.getString(1)))found=true;}finally{c.close();}if(!found)d.execSQL("ALTER TABLE "+table+" ADD COLUMN "+column+" "+definition);}

    private static SQLiteDatabase open(Context context) {
        return context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
    }

    private static void ensureSchema(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE IF NOT EXISTS materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY,name TEXT UNIQUE,category TEXT NOT NULL DEFAULT 'منتج إضافي',unit_price INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS recipe(id INTEGER PRIMARY KEY,product_id INTEGER,material_id INTEGER,qty INTEGER CHECK(qty>0),UNIQUE(product_id,material_id))");
        d.execSQL("CREATE TABLE IF NOT EXISTS batches(id INTEGER PRIMARY KEY,batch_ref TEXT UNIQUE,product_id INTEGER,batches INTEGER,output INTEGER,labor INTEGER,overhead INTEGER,cost INTEGER,unit_cost INTEGER,status TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS finished(product_id INTEGER PRIMARY KEY,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0,unit_cost INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS sales(id INTEGER PRIMARY KEY,product_id INTEGER,customer TEXT,channel TEXT,qty INTEGER,total INTEGER,cash INTEGER,bank INTEGER,bank_ref TEXT,debt INTEGER,cogs INTEGER,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS expenses(id INTEGER PRIMARY KEY,date TEXT,category TEXT,amount INTEGER,note TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS waste(id INTEGER PRIMARY KEY,product_id INTEGER,qty INTEGER,cost INTEGER,reason TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS movements(id INTEGER PRIMARY KEY,kind TEXT,item_id INTEGER,qty INTEGER,value INTEGER,ref TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS collections(id INTEGER PRIMARY KEY,customer TEXT,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER NOT NULL,ref TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS audit(id INTEGER PRIMARY KEY,action TEXT,detail TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS daily_usage(id INTEGER PRIMARY KEY,date TEXT,material_id INTEGER,qty INTEGER,cost INTEGER,note TEXT,kind TEXT NOT NULL DEFAULT 'MANUAL')");
        d.execSQL("CREATE TABLE IF NOT EXISTS sales_returns(id INTEGER PRIMARY KEY,sale_id INTEGER NOT NULL,product_id INTEGER NOT NULL,qty INTEGER NOT NULL,refund_value INTEGER NOT NULL,refund_cash INTEGER NOT NULL,refund_bank INTEGER NOT NULL,credit_adjustment INTEGER NOT NULL,restored_cogs INTEGER NOT NULL,bank_ref TEXT,reason TEXT,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS work_orders(id INTEGER PRIMARY KEY,work_ref TEXT UNIQUE,product_id INTEGER NOT NULL,batches INTEGER NOT NULL,planned_qty INTEGER NOT NULL,actual_qty INTEGER NOT NULL DEFAULT 0,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL DEFAULT 0,overhead INTEGER NOT NULL DEFAULT 0,total_cost INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS work_order_materials(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,material_id INTEGER NOT NULL,qty INTEGER NOT NULL,cost INTEGER NOT NULL)");
        d.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
        d.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
        ensureColumn(d,"daily_usage","kind","TEXT NOT NULL DEFAULT 'MANUAL'");
        ensureColumn(d,"products","category","TEXT NOT NULL DEFAULT 'منتج إضافي'");
        ensureColumn(d,"products","unit_price","INTEGER NOT NULL DEFAULT 0");
        if (d.getVersion() < 4) d.setVersion(4);
    }
}
