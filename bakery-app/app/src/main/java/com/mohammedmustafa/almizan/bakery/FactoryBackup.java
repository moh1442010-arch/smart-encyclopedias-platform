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
        "expenses","waste","movements","collections","cashbook","audit"
    };
    private static final String[][] COLUMNS = {
        {"id","name","unit","qty","value"},
        {"id","name"},
        {"id","product_id","material_id","qty"},
        {"id","batch_ref","product_id","batches","output","labor","overhead","cost","unit_cost","status","date"},
        {"product_id","qty","value","unit_cost"},
        {"id","product_id","customer","channel","qty","total","cash","bank","bank_ref","debt","cogs","date"},
        {"id","date","category","amount","note"},
        {"id","product_id","qty","cost","reason","date"},
        {"id","kind","item_id","qty","value","ref","date"},
        {"id","customer","cash","bank","bank_ref","date"},
        {"id","account","kind","amount","ref","date"},
        {"id","action","detail","date"}
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
        {"id","action","detail","date"}
    };

    private FactoryBackup() {}

    static JSONObject export(Context context) throws Exception {
        SQLiteDatabase db = open(context);
        try {
            ensureSchema(db);
            JSONObject root = new JSONObject();
            root.put("_type", TYPE);
            root.put("schema_version", 3);
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
        if (root == null || !TYPE.equals(root.optString("_type")) || root.optInt("schema_version") != 3)
            throw new IllegalArgumentException("Unsupported factory backup format");
        JSONObject tables = root.optJSONObject("tables");
        if (tables == null) throw new IllegalArgumentException("Factory backup tables are missing");
        for (int i=0;i<TABLES.length;i++) {
            JSONArray rows = tables.optJSONArray(TABLES[i]);
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
            }
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
                for (int i=0;i<TABLES.length;i++) {
                    JSONArray rows = tables.getJSONArray(TABLES[i]);
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

    private static SQLiteDatabase open(Context context) {
        return context.openOrCreateDatabase(DB_NAME, Context.MODE_PRIVATE, null);
    }

    private static void ensureSchema(SQLiteDatabase d) {
        d.execSQL("CREATE TABLE IF NOT EXISTS materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
        d.execSQL("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY,name TEXT UNIQUE)");
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
        if (d.getVersion() < 3) d.setVersion(3);
    }
}
