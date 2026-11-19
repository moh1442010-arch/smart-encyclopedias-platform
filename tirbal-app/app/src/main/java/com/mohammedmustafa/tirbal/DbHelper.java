package com.mohammedmustafa.tirbal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "tirbal.db";
    private static final int DB_VERSION = 3;

    public DbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE project (id INTEGER PRIMARY KEY CHECK(id=1), name TEXT NOT NULL, crop TEXT NOT NULL, trader TEXT NOT NULL, owner TEXT NOT NULL, accountant TEXT NOT NULL, area REAL NOT NULL DEFAULT 0, start_date TEXT NOT NULL, region TEXT)");
        db.execSQL("CREATE TABLE costs (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL DEFAULT 1, entry_date TEXT NOT NULL, worker TEXT NOT NULL, category TEXT NOT NULL, item TEXT NOT NULL, quantity REAL NOT NULL, unit_price INTEGER NOT NULL, total INTEGER NOT NULL, payer TEXT NOT NULL DEFAULT 'غير محدد', responsible_party TEXT NOT NULL DEFAULT 'غير محدد', settlement_type TEXT NOT NULL DEFAULT 'owner_bears', deduction_from TEXT NOT NULL DEFAULT 'none', agreement_note TEXT, notes TEXT, FOREIGN KEY(project_id) REFERENCES project(id))");
        db.execSQL("CREATE TABLE partnership_terms (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL DEFAULT 1, category TEXT NOT NULL UNIQUE, responsible_party TEXT NOT NULL, default_payer TEXT NOT NULL, settlement_type TEXT NOT NULL, deduction_from TEXT NOT NULL, note TEXT)");
        seedPartnershipTerms(db);
        db.execSQL("CREATE TABLE sales (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL DEFAULT 1, invoice TEXT NOT NULL, sale_date TEXT NOT NULL, gross INTEGER NOT NULL, allocated_cost INTEGER NOT NULL DEFAULT 0, market_expenses INTEGER NOT NULL DEFAULT 0, net INTEGER NOT NULL, trader TEXT, notes TEXT, FOREIGN KEY(project_id) REFERENCES project(id))");
        db.execSQL("CREATE TABLE calendar_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL DEFAULT 1, task_date TEXT NOT NULL, title TEXT NOT NULL, details TEXT, completed INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(project_id) REFERENCES project(id))");
        db.execSQL("CREATE INDEX idx_costs_date ON costs(entry_date)");
        db.execSQL("CREATE INDEX idx_sales_date ON sales(sale_date)");
        db.execSQL("CREATE INDEX idx_tasks_date ON calendar_tasks(task_date)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE costs ADD COLUMN payer TEXT NOT NULL DEFAULT 'غير محدد'");
            db.execSQL("ALTER TABLE costs ADD COLUMN responsible_party TEXT NOT NULL DEFAULT 'غير محدد'");
            db.execSQL("ALTER TABLE costs ADD COLUMN settlement_type TEXT NOT NULL DEFAULT 'owner_bears'");
            db.execSQL("ALTER TABLE costs ADD COLUMN deduction_from TEXT NOT NULL DEFAULT 'none'");
            db.execSQL("ALTER TABLE costs ADD COLUMN agreement_note TEXT");
            db.execSQL("CREATE TABLE partnership_terms (id INTEGER PRIMARY KEY AUTOINCREMENT, project_id INTEGER NOT NULL DEFAULT 1, category TEXT NOT NULL UNIQUE, responsible_party TEXT NOT NULL, default_payer TEXT NOT NULL, settlement_type TEXT NOT NULL, deduction_from TEXT NOT NULL, note TEXT)");
            seedPartnershipTerms(db);
        }
        if (oldVersion < 3) seedPartnershipTerms(db);
    }

    private void seedPartnershipTerms(SQLiteDatabase db) {
        String[][] rows = {
            {"تحضير الأرض","مشترك","التاجر","shared_half","مناصفة","تكلفة التحضير مناصفة"},
            {"التقاوي","مشترك","التاجر","shared_half","مناصفة","تكلفة التقاوي مناصفة"},
            {"الأسمدة","مشترك","التاجر","shared_half","مناصفة","تكلفة الأسمدة مناصفة"},
            {"المبيدات","مشترك","التاجر","shared_half","مناصفة","تكلفة المبيدات مناصفة"},
            {"الميز","التربال","التاجر","deduct_worker","من نصيب التربال","يدفعها التاجر وتخصم كاملة من نصيب التربال"},
            {"الخدمة داخل الزراعة","التربال","التاجر","deduct_worker","من نصيب التربال","أعمال الزراعة والنظافة والخدمة على التربال"},
            {"الري وخدمة الحقل","التربال","التاجر","deduct_worker","من نصيب التربال","أعمال الري داخل الحقل على التربال"},
            {"أجور العمال","التربال","التاجر","deduct_worker","من نصيب التربال","تخصم من نصيب التربال إذا دفعها التاجر"},
            {"المنصرفات","التربال","التاجر","deduct_worker","من نصيب التربال","المنصرفات المسجلة تخصم من نصيب التربال"},
            {"السلفيات","التربال","التاجر","deduct_worker","من نصيب التربال","السلفيات تخصم من نصيب التربال"},
            {"ضريبة المياه","التاجر","التاجر","trader_only","لا يوجد خصم","على التاجر وحده خارج المناصفة"},
            {"إيجار الأرض","التاجر","التاجر","trader_only","لا يوجد خصم","على التاجر وحده خارج المناصفة"}
        };
        for (String[] row : rows) {
            ContentValues v = new ContentValues();
            v.put("category",row[0]); v.put("responsible_party",row[1]); v.put("default_payer",row[2]);
            v.put("settlement_type",row[3]); v.put("deduction_from",row[4]); v.put("note",row[5]);
            db.insertWithOnConflict("partnership_terms",null,v,SQLiteDatabase.CONFLICT_REPLACE);
        }
    }
    public boolean hasProject() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM project WHERE id=1", null)) { return c.moveToFirst(); }
    }

    public void saveProject(String name, String crop, String trader, String owner, String accountant, double area, String startDate, String region) {
        ContentValues v = new ContentValues();
        v.put("id", 1); v.put("name", name); v.put("crop", crop); v.put("trader", trader);
        v.put("owner", owner); v.put("accountant", accountant); v.put("area", area);
        v.put("start_date", startDate); v.put("region", region);
        getWritableDatabase().insertWithOnConflict("project", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void addCost(String date, String worker, String category, String item, double quantity, long price,
                        String payer, String responsible, String settlementType, String deductionFrom,
                        String agreementNote, String notes) {
        ContentValues v = new ContentValues();
        v.put("project_id", 1); v.put("entry_date", date); v.put("worker", worker);
        v.put("category", category); v.put("item", item); v.put("quantity", quantity);
        v.put("unit_price", price); v.put("total", Math.round(quantity * price));
        v.put("payer", payer); v.put("responsible_party", responsible);
        v.put("settlement_type", settlementType); v.put("deduction_from", deductionFrom);
        v.put("agreement_note", agreementNote); v.put("notes", notes);
        getWritableDatabase().insertOrThrow("costs", null, v);
    }

    public String[] defaultResponsibility(String category) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT responsible_party,default_payer FROM partnership_terms WHERE category=?", new String[]{category})) {
            if (c.moveToFirst()) return new String[]{c.getString(0), c.getString(1), c.getString(2), c.getString(3)};
        }
        return new String[]{"حسب الاتفاق", "التاجر", "agreement_pending", "حسب الاتفاق"};
    }

    public void savePartnershipTerm(String category, String responsible, String payer, String settlement, String deduction, String note) {
        ContentValues v = new ContentValues();
        v.put("category", category); v.put("responsible_party", responsible); v.put("default_payer", payer);
        v.put("settlement_type", settlement); v.put("deduction_from", deduction); v.put("note", note);
        getWritableDatabase().insertWithOnConflict("partnership_terms", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Cursor partnershipTerms() {
        return getReadableDatabase().rawQuery("SELECT * FROM partnership_terms ORDER BY id", null);
    }

    public long totalBySettlement(String settlementType) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM costs WHERE project_id=1 AND settlement_type=?", new String[]{settlementType})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public long totalByDeduction(String deduction) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM costs WHERE project_id=1 AND deduction_from=?", new String[]{deduction})) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public void addSale(String invoice, String date, long gross, long allocatedCost, long marketExpenses, String trader, String notes) {
        ContentValues v = new ContentValues();
        v.put("project_id", 1); v.put("invoice", invoice); v.put("sale_date", date);
        v.put("gross", gross); v.put("allocated_cost", allocatedCost); v.put("market_expenses", marketExpenses);
        v.put("net", gross - allocatedCost - marketExpenses); v.put("trader", trader); v.put("notes", notes);
        getWritableDatabase().insertOrThrow("sales", null, v);
    }

    public void addTask(String date, String title, String details) {
        ContentValues v = new ContentValues();
        v.put("project_id", 1); v.put("task_date", date); v.put("title", title); v.put("details", details);
        getWritableDatabase().insertOrThrow("calendar_tasks", null, v);
    }

    public long total(String table, String column) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COALESCE(SUM(" + column + "),0) FROM " + table + " WHERE project_id=1", null)) {
            return c.moveToFirst() ? c.getLong(0) : 0L;
        }
    }

    public Cursor recent(String table, String orderColumn) {
        return getReadableDatabase().rawQuery("SELECT * FROM " + table + " WHERE project_id=1 ORDER BY " + orderColumn + " DESC, id DESC LIMIT 100", null);
    }

    public String projectName() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT name FROM project WHERE id=1", null)) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }
}
