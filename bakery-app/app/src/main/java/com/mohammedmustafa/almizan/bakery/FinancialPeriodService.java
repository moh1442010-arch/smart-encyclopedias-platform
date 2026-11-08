package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Controls non-overlapping financial periods and immutable close state. */
final class FinancialPeriodService {
    private FinancialPeriodService() {}

    static boolean validRange(String start,String end) {
        return validDate(start)&&validDate(end)&&start.compareTo(end)<=0;
    }

    static long open(SQLiteDatabase db,String code,String start,String end) {
        if(code==null||code.trim().isEmpty()||!validRange(start,end))
            throw new IllegalArgumentException("رمز الفترة أو تاريخ بدايتها ونهايتها غير صحيح");
        if(db.inTransaction())throw new IllegalStateException("افتح الفترة في معاملة مستقلة");
        db.beginTransaction();
        try {
            Cursor overlap=db.rawQuery("SELECT period_code FROM financial_periods WHERE start_date<=? AND end_date>=? LIMIT 1",
                new String[]{end,start});
            try {
                if(overlap.moveToFirst())throw new IllegalStateException("الفترة تتداخل مع فترة مالية موجودة: "+overlap.getString(0));
            } finally {overlap.close();}
            ContentValues v=new ContentValues();
            v.put("period_code",code.trim());v.put("start_date",start);v.put("end_date",end);v.put("status","OPEN");
            long id=db.insertOrThrow("financial_periods",null,v);
            audit(db,"SYSTEM","FINANCIAL_PERIOD_OPENED",code.trim(),start+".."+end);
            db.setTransactionSuccessful();
            return id;
        } finally {db.endTransaction();}
    }

    static void close(SQLiteDatabase db,String code,String actor) {
        if(code==null||code.trim().isEmpty())throw new IllegalArgumentException("رمز الفترة مطلوب");
        if(db.inTransaction())throw new IllegalStateException("أغلق الفترة في معاملة مستقلة");
        db.beginTransaction();
        try {
            Cursor p=db.rawQuery("SELECT id,status FROM financial_periods WHERE period_code=?",new String[]{code.trim()});
            long id;String status;
            try {
                if(!p.moveToFirst())throw new IllegalArgumentException("الفترة المالية غير موجودة");
                id=p.getLong(0);status=p.getString(1);
            } finally {p.close();}
            if(!"OPEN".equals(status))throw new IllegalStateException("الفترة ليست مفتوحة أو سبق إقفالها");
            Cursor totals=db.rawQuery("SELECT COALESCE(SUM(l.base_debit_minor),0),COALESCE(SUM(l.base_credit_minor),0),COUNT(*) " +
                "FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.period_id=? AND e.status='POSTED'",
                new String[]{String.valueOf(id)});
            try {
                totals.moveToFirst();
                long debit=totals.getLong(0),credit=totals.getLong(1),lines=totals.getLong(2);
                if(lines>0)GeneralLedger.validateBalancedTotals(debit,credit,2);
            } finally {totals.close();}
            Cursor invalid=db.rawQuery("SELECT e.entry_ref FROM gl_journal_entries e LEFT JOIN gl_journal_lines l ON l.entry_id=e.id "+
                "WHERE e.period_id=? AND e.status='POSTED' GROUP BY e.id,e.entry_ref "+
                "HAVING COUNT(l.id)<2 OR COALESCE(SUM(l.base_debit_minor),0)<>COALESCE(SUM(l.base_credit_minor),0) LIMIT 1",
                new String[]{String.valueOf(id)});
            try {if(invalid.moveToFirst())throw new IllegalStateException("يوجد قيد غير متوازن أو ناقص: "+invalid.getString(0));}
            finally {invalid.close();}
            Cursor drafts=db.rawQuery("SELECT COUNT(*) FROM gl_journal_entries WHERE period_id=? AND status='DRAFT'",
                new String[]{String.valueOf(id)});
            try {drafts.moveToFirst();if(drafts.getLong(0)>0)throw new IllegalStateException("توجد قيود مسودة لم تُعالج");}
            finally {drafts.close();}
            ContentValues v=new ContentValues();v.put("status","CLOSED");v.put("closed_at",System.currentTimeMillis());
            db.update("financial_periods",v,"id=?",new String[]{String.valueOf(id)});
            audit(db,actor==null?"SYSTEM":actor,"FINANCIAL_PERIOD_CLOSED",code.trim(),"Closed by authorized actor");
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
    }

    private static void audit(SQLiteDatabase db,String actor,String action,String ref,String detail) {
        ContentValues v=new ContentValues();v.put("actor",actor);v.put("action",action);
        v.put("entity_type","FINANCIAL_PERIOD");v.put("entity_ref",ref);v.put("details",detail);
        v.put("created_at",System.currentTimeMillis());db.insertOrThrow("enterprise_audit_log",null,v);
    }
    private static boolean validDate(String d) {
        if(d==null||!d.matches("\\d{4}-\\d{2}-\\d{2}"))return false;
        SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.US);f.setLenient(false);
        ParsePosition p=new ParsePosition(0);java.util.Date parsed=f.parse(d,p);
        return parsed!=null&&p.getIndex()==d.length();
    }
}
