package com.mohammedmustafa.tirbal;

import android.content.ContentValues;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

/**
 * Double-entry accounting layer for the agricultural project.
 * Every posted entry is balanced and linked to its source record.
 */
final class AccountingBook {
    static final String[] ACCOUNT_CODES = {"1000","1100","1200","2000","2100","2200","3000","4000","5000","5100","5200"};
    static final String[] ACCOUNT_NAMES = {
        "النقدية","العملاء والمدينون","مخزون المحصول","الموردون والدائنون",
        "جاري التاجر","جاري التربال","رأس المال","إيرادات المبيعات",
        "تكلفة المحصول المباع","مصروفات السوق","تكاليف الإنتاج الزراعي"
    };

    private AccountingBook() {}

    static void install(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS accounts (id INTEGER PRIMARY KEY AUTOINCREMENT, code TEXT NOT NULL UNIQUE, name TEXT NOT NULL, account_type TEXT NOT NULL, normal_side TEXT NOT NULL, is_system INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_date TEXT NOT NULL, reference TEXT NOT NULL, description TEXT NOT NULL, source_type TEXT NOT NULL, source_id INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'POSTED', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, UNIQUE(source_type,source_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines (id INTEGER PRIMARY KEY AUTOINCREMENT, entry_id INTEGER NOT NULL, account_id INTEGER NOT NULL, debit INTEGER NOT NULL DEFAULT 0 CHECK(debit>=0), credit INTEGER NOT NULL DEFAULT 0 CHECK(credit>=0), memo TEXT, CHECK(NOT(debit>0 AND credit>0)), FOREIGN KEY(entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE, FOREIGN KEY(account_id) REFERENCES accounts(id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_date ON journal_entries(entry_date,id)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_lines_account ON journal_lines(account_id,entry_id)");
        seed(db);
    }

    private static void seed(SQLiteDatabase db) {
        String[][] rows = {
            {"1000","النقدية","ASSET","DEBIT"},
            {"1100","العملاء والمدينون","ASSET","DEBIT"},
            {"1200","مخزون المحصول","ASSET","DEBIT"},
            {"2000","الموردون والدائنون","LIABILITY","CREDIT"},
            {"2100","جاري التاجر","LIABILITY","CREDIT"},
            {"2200","جاري التربال","LIABILITY","CREDIT"},
            {"3000","رأس المال","EQUITY","CREDIT"},
            {"4000","إيرادات المبيعات","INCOME","CREDIT"},
            {"5000","تكلفة المحصول المباع","EXPENSE","DEBIT"},
            {"5100","مصروفات السوق","EXPENSE","DEBIT"},
            {"5200","تكاليف الإنتاج الزراعي","EXPENSE","DEBIT"}
        };
        for (String[] row : rows) {
            ContentValues v = new ContentValues();
            v.put("code",row[0]); v.put("name",row[1]); v.put("account_type",row[2]); v.put("normal_side",row[3]); v.put("is_system",1);
            db.insertWithOnConflict("accounts",null,v,SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    static void backfillLegacy(SQLiteDatabase db) {
        try (Cursor c=db.rawQuery("SELECT id,entry_date,category,item,total,payer FROM costs ORDER BY id",null)) {
            while(c.moveToNext()) postCost(db,c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getLong(4),c.getString(5));
        }
        try (Cursor c=db.rawQuery("SELECT id,sale_date,invoice,gross,allocated_cost,market_expenses FROM sales ORDER BY id",null)) {
            while(c.moveToNext()) postSale(db,c.getLong(0),c.getString(1),c.getString(2),c.getLong(3),c.getLong(4),c.getLong(5));
        }
    }

    static void postCost(SQLiteDatabase db,long sourceId,String date,String category,String item,long amount,String payer) {
        if(amount<=0)return;
        db.beginTransaction();
        try {
            long entry=entry(db,date,"COST-"+sourceId,"إثبات تكلفة: "+category+" - "+item,"COST",sourceId);
            if(entry>0) {
                line(db,entry,"5200",amount,0,"تكلفة زراعية");
                String credit="التربال".equals(payer)?"2200":("طرف ثالث".equals(payer)?"2000":"2100");
                line(db,entry,credit,0,amount,"مصدر التمويل: "+payer);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    static void postSale(SQLiteDatabase db,long sourceId,String date,String invoice,long gross,long cost,long marketExpenses) {
        if(gross<0||cost<0||marketExpenses<0)return;
        db.beginTransaction();
        try {
            long entry=entry(db,date,"SALE-"+sourceId,"إثبات فاتورة بيع رقم "+invoice,"SALE",sourceId);
            if(entry>0) {
                if(gross>0){line(db,entry,"1100",gross,0,"ذمم العميل");line(db,entry,"4000",0,gross,"إيراد المبيعات");}
                if(cost>0){line(db,entry,"5000",cost,0,"تكلفة المحصول المباع");line(db,entry,"1200",0,cost,"تخفيض مخزون المحصول");}
                if(marketExpenses>0){line(db,entry,"5100",marketExpenses,0,"مصروفات السوق");line(db,entry,"2000",0,marketExpenses,"مستحقات مصروفات السوق");}
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    static void createManual(SQLiteDatabase db,String date,String description,String debitCode,String creditCode,long amount) {
        if(amount<=0||debitCode.equals(creditCode))throw new IllegalArgumentException("قيد غير صالح");
        db.beginTransaction();
        try {
            long id=entry(db,date,"MANUAL-"+System.currentTimeMillis(),description,"MANUAL",System.currentTimeMillis());
            if(id<=0)throw new IllegalStateException("تعذر إنشاء القيد");
            line(db,id,debitCode,amount,0,description);
            line(db,id,creditCode,0,amount,description);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static long entry(SQLiteDatabase db,String date,String reference,String description,String source,long sourceId) {
        try (Cursor c=db.rawQuery("SELECT id FROM journal_entries WHERE source_type=? AND source_id=?",new String[]{source,String.valueOf(sourceId)})) {
            if(c.moveToFirst())return -1;
        }
        ContentValues v=new ContentValues();
        v.put("entry_date",date==null?"":date);v.put("reference",reference);v.put("description",description);
        v.put("source_type",source);v.put("source_id",sourceId);v.put("status","POSTED");
        return db.insertOrThrow("journal_entries",null,v);
    }

    private static void line(SQLiteDatabase db,long entry,String code,long debit,long credit,String memo) {
        long account=accountId(db,code);
        if(account<=0)throw new IllegalStateException("حساب غير موجود: "+code);
        ContentValues v=new ContentValues();
        v.put("entry_id",entry);v.put("account_id",account);v.put("debit",debit);v.put("credit",credit);v.put("memo",memo);
        db.insertOrThrow("journal_lines",null,v);
    }

    static long accountId(SQLiteDatabase db,String code) {
        try(Cursor c=db.rawQuery("SELECT id FROM accounts WHERE code=?",new String[]{code})) {return c.moveToFirst()?c.getLong(0):0;}
    }

    static Cursor trialBalance(SQLiteDatabase db) {
        return db.rawQuery("SELECT a.code,a.name,a.account_type,COALESCE(SUM(l.debit),0) AS debit,COALESCE(SUM(l.credit),0) AS credit,COALESCE(SUM(l.debit-l.credit),0) AS balance FROM accounts a LEFT JOIN journal_lines l ON l.account_id=a.id LEFT JOIN journal_entries e ON e.id=l.entry_id AND e.status='POSTED' GROUP BY a.id,a.code,a.name,a.account_type ORDER BY a.code",null);
    }

    static Cursor recentEntries(SQLiteDatabase db) {
        return db.rawQuery("SELECT e.id,e.entry_date,e.reference,e.description,COALESCE(SUM(l.debit),0) AS debit,COALESCE(SUM(l.credit),0) AS credit FROM journal_entries e JOIN journal_lines l ON l.entry_id=e.id WHERE e.status='POSTED' GROUP BY e.id ORDER BY e.id DESC LIMIT 100",null);
    }
}
