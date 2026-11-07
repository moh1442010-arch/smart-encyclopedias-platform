package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Atomic double-entry posting service for the bakery enterprise ledger.
 * Source identifiers and idempotency keys must be stable across retries.
 */
final class GeneralLedger {
    private GeneralLedger() {}

    static final class Line {
        final String accountCode, branchCode, currencyCode;
        final long debitMinor, creditMinor, fxNumerator, fxDenominator;
        Line(String accountCode, String branchCode, String currencyCode,
             long debitMinor, long creditMinor, long fxNumerator, long fxDenominator) {
            this.accountCode=accountCode; this.branchCode=branchCode; this.currencyCode=currencyCode;
            this.debitMinor=debitMinor; this.creditMinor=creditMinor;
            this.fxNumerator=fxNumerator; this.fxDenominator=fxDenominator;
        }
    }

    static final class Entry {
        final String reference, description, date, sourceType, sourceId, idempotencyKey, actor;
        final List<Line> lines;
        Entry(String reference, String description, String date, String sourceType, String sourceId,
              String idempotencyKey, String actor, List<Line> lines) {
            this.reference=reference; this.description=description; this.date=date;
            this.sourceType=sourceType; this.sourceId=sourceId; this.idempotencyKey=idempotencyKey;
            this.actor=actor==null||actor.trim().isEmpty()?"SYSTEM":actor.trim();
            this.lines=lines==null?new ArrayList<Line>():new ArrayList<>(lines);
        }
    }

    static final class Result {
        final String reference;
        final boolean alreadyPosted;
        Result(String reference, boolean alreadyPosted) {
            this.reference=reference; this.alreadyPosted=alreadyPosted;
        }
    }

    static void validateBalancedTotals(long debitMinor, long creditMinor, int lineCount) {
        if (lineCount < 2 || debitMinor <= 0 || creditMinor <= 0 || debitMinor != creditMinor)
            throw new IllegalArgumentException("القيد غير متوازن أو لا يحتوي على سطرين على الأقل");
    }

    static long toBaseMinor(long transactionMinor, long rateNumerator, long rateDenominator,
                            int transactionMinorUnits, int baseMinorUnits) {
        if (transactionMinor <= 0) throw new IllegalArgumentException("يجب أن يكون مبلغ السطر أكبر من صفر");
        return EnterpriseMath.convertMinorUnits(transactionMinor, rateNumerator, rateDenominator,
            transactionMinorUnits, baseMinorUnits);
    }

    static Result postAuthorized(SQLiteDatabase db,Entry entry,String username) {
        if(entry==null||entry.lines==null)throw new IllegalArgumentException("بيانات القيد غير مكتملة");
        for(Line line:entry.lines) {
            if(line==null)throw new IllegalArgumentException("سطر القيد غير صحيح");
            PermissionService.requirePermission(db,username,line.branchCode,"JOURNAL_POST");
        }
        return post(db,entry);
    }

    static Result post(SQLiteDatabase db, Entry entry) {
        validateEntry(entry);
        if (db.inTransaction()) throw new IllegalStateException("يجب ترحيل القيد داخل معاملة مستقلة لمنع اعتماد جزء من القيد");
        boolean ownTransaction=true;
        db.beginTransaction();
        try {
            String prior=findIdempotentResult(db,entry.idempotencyKey);
            if (prior!=null) {
                if (!prior.equals(entry.reference)) throw new IllegalStateException("مفتاح العملية استُخدم لقيد مختلف");
                if (ownTransaction) db.setTransactionSuccessful();
                return new Result(prior,true);
            }
            String baseCurrency=scalar(db,"SELECT base_currency FROM company_settings WHERE id=1",null);
            if (baseCurrency==null || baseCurrency.trim().isEmpty()) throw new IllegalStateException("عملة المنشأة الأساسية غير معرفة");
            int baseMinorUnits=currencyMinorUnits(db,baseCurrency);
            long totalDebit=0L,totalCredit=0L;
            long[] baseDebits=new long[entry.lines.size()],baseCredits=new long[entry.lines.size()];
            long[] accountIds=new long[entry.lines.size()],branchIds=new long[entry.lines.size()];
            for(int i=0;i<entry.lines.size();i++) {
                Line line=entry.lines.get(i);
                validateLine(line);
                int txMinorUnits=currencyMinorUnits(db,line.currencyCode);
                if (baseCurrency.equals(line.currencyCode) && line.fxNumerator!=line.fxDenominator)
                    throw new IllegalArgumentException("سعر صرف العملة الأساسية يجب أن يساوي 1");
                if(line.debitMinor>0) {
                    baseDebits[i]=toBaseMinor(line.debitMinor,line.fxNumerator,line.fxDenominator,txMinorUnits,baseMinorUnits);
                    if(baseDebits[i]<=0) throw new IllegalArgumentException("قيمة السطر بعد التحويل إلى العملة الأساسية تساوي صفرًا");
                    totalDebit=EnterpriseMath.toLongExact(BigInteger.valueOf(totalDebit).add(BigInteger.valueOf(baseDebits[i])));
                } else {
                    baseCredits[i]=toBaseMinor(line.creditMinor,line.fxNumerator,line.fxDenominator,txMinorUnits,baseMinorUnits);
                    if(baseCredits[i]<=0) throw new IllegalArgumentException("قيمة السطر بعد التحويل إلى العملة الأساسية تساوي صفرًا");
                    totalCredit=EnterpriseMath.toLongExact(BigInteger.valueOf(totalCredit).add(BigInteger.valueOf(baseCredits[i])));
                }
                accountIds[i]=requiredId(db,"SELECT id FROM chart_of_accounts WHERE account_code=? AND active=1",line.accountCode,"الحساب غير موجود أو غير نشط: "+line.accountCode);
                branchIds[i]=requiredId(db,"SELECT id FROM branches WHERE branch_code=? AND active=1",line.branchCode,"الفرع غير موجود أو غير نشط: "+line.branchCode);
            }
            validateBalancedTotals(totalDebit,totalCredit,entry.lines.size());
            long periodId=openPeriod(db,entry.date);
            ContentValues head=new ContentValues();
            head.put("entry_ref",entry.reference); head.put("description",entry.description);
            head.put("transaction_date",entry.date); head.put("period_id",periodId);
            head.put("source_type",entry.sourceType); head.put("source_id",entry.sourceId);
            head.put("status","POSTED"); head.put("created_at",System.currentTimeMillis());
            long entryId=db.insertOrThrow("gl_journal_entries",null,head);
            for(int i=0;i<entry.lines.size();i++) {
                Line line=entry.lines.get(i);
                ContentValues values=new ContentValues();
                values.put("entry_id",entryId); values.put("account_id",accountIds[i]); values.put("branch_id",branchIds[i]);
                values.put("base_currency_code",baseCurrency);
                values.put("base_debit_minor",baseDebits[i]); values.put("base_credit_minor",baseCredits[i]);
                values.put("transaction_currency_code",line.currencyCode);
                values.put("transaction_debit_minor",line.debitMinor); values.put("transaction_credit_minor",line.creditMinor);
                values.put("fx_rate_num",line.fxNumerator); values.put("fx_rate_den",line.fxDenominator);
                db.insertOrThrow("gl_journal_lines",null,values);
            }
            ContentValues idem=new ContentValues();
            idem.put("key",entry.idempotencyKey); idem.put("operation","GL_POST");
            idem.put("result_ref",entry.reference); idem.put("created_at",System.currentTimeMillis());
            db.insertOrThrow("idempotency_keys",null,idem);
            ContentValues audit=new ContentValues();
            audit.put("actor",entry.actor); audit.put("action","JOURNAL_POSTED");
            audit.put("entity_type","GL_JOURNAL"); audit.put("entity_ref",entry.reference);
            audit.put("details",entry.sourceType+":"+entry.sourceId+":"+totalDebit);
            audit.put("created_at",System.currentTimeMillis());
            db.insertOrThrow("enterprise_audit_log",null,audit);
            if (ownTransaction) db.setTransactionSuccessful();
            return new Result(entry.reference,false);
        } finally {
            if (ownTransaction) db.endTransaction();
        }
    }

    private static void validateEntry(Entry e) {
        if(e==null || blank(e.reference)||blank(e.description)||!validDate(e.date)||blank(e.sourceType)||
           blank(e.sourceId)||blank(e.idempotencyKey)||e.lines==null||e.lines.size()<2)
            throw new IllegalArgumentException("بيانات القيد أو مرجع المصدر غير مكتملة");
    }
    private static void validateLine(Line l) {
        if(l==null||blank(l.accountCode)||blank(l.branchCode)||blank(l.currencyCode)||
           l.debitMinor<0||l.creditMinor<0||((l.debitMinor>0)==(l.creditMinor>0))||
           l.fxNumerator<=0||l.fxDenominator<=0)
            throw new IllegalArgumentException("سطر القيد غير صحيح");
    }
    private static boolean validDate(String d) {
        if(d==null||!d.matches("\\d{4}-\\d{2}-\\d{2}")) return false;
        SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        format.setLenient(false);
        ParsePosition position=new ParsePosition(0);
        java.util.Date parsed=format.parse(d,position);
        return parsed!=null && position.getIndex()==d.length();
    }
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
    private static String scalar(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);
        try{return c.moveToFirst()?c.getString(0):null;}finally{c.close();}
    }
    private static long requiredId(SQLiteDatabase db,String sql,String value,String error) {
        Cursor c=db.rawQuery(sql,new String[]{value});
        try{if(!c.moveToFirst())throw new IllegalArgumentException(error);return c.getLong(0);}finally{c.close();}
    }
    private static int currencyMinorUnits(SQLiteDatabase db,String code) {
        Cursor c=db.rawQuery("SELECT minor_units FROM currencies WHERE code=? AND active=1",new String[]{code});
        try{if(!c.moveToFirst())throw new IllegalArgumentException("العملة غير معرفة أو غير نشطة: "+code);return c.getInt(0);}finally{c.close();}
    }
    private static long openPeriod(SQLiteDatabase db,String date) {
        Cursor c=db.rawQuery("SELECT id FROM financial_periods WHERE status='OPEN' AND start_date<=? AND end_date>=? ORDER BY start_date DESC",
            new String[]{date,date});
        try {
            if(!c.moveToFirst())throw new IllegalStateException("لا توجد فترة مالية مفتوحة لهذا التاريخ");
            long id=c.getLong(0);
            if(c.moveToNext())throw new IllegalStateException("تداخل الفترات المالية المفتوحة؛ يجب إصلاح الفترات قبل الترحيل");
            return id;
        } finally {c.close();}
    }
    private static String findIdempotentResult(SQLiteDatabase db,String key) {
        Cursor c=db.rawQuery("SELECT result_ref FROM idempotency_keys WHERE key=? AND operation='GL_POST'",new String[]{key});
        try{return c.moveToFirst()?c.getString(0):null;}finally{c.close();}
    }
}
