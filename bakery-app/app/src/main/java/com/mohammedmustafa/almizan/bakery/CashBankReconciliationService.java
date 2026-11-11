package com.mohammedmustafa.almizan.bakery;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Reconciles factory cashbook movement totals with posted factory-origin GL lines.
 * Current factory schema is single-branch (MAIN) and SDG-only; this service refuses
 * unsupported dimensions instead of silently comparing unlike balances.
 */
final class CashBankReconciliationService {
    private CashBankReconciliationService() {}

    static final class AccountResult {
        final String account;
        final long cashbookMinor;
        final long ledgerMinor;
        final long differenceMinor;
        final int cashbookRows;
        AccountResult(String account,BigInteger cashbook,BigInteger ledger,int rows) {
            this.account=account;
            this.cashbookMinor=cashbook.longValueExact();
            this.ledgerMinor=ledger.longValueExact();
            this.differenceMinor=cashbook.subtract(ledger).longValueExact();
            this.cashbookRows=rows;
        }
        boolean balanced(){return differenceMinor==0L;}
    }

    static final class Report {
        final String branchCode,currencyCode,startDate,endDate;
        final AccountResult cash,bankak;
        Report(String branch,String currency,String start,String end,AccountResult cash,AccountResult bankak) {
            branchCode=branch;currencyCode=currency;startDate=start;endDate=end;
            this.cash=cash;this.bankak=bankak;
        }
        boolean balanced(){return cash.balanced()&&bankak.balanced();}
    }

    static Report reconcileAllHistory(SQLiteDatabase source,SQLiteDatabase ledger,String branch,String currency) {
        if(source==null||!source.isOpen()||ledger==null||!ledger.isOpen())
            throw new IllegalStateException("قاعدتا المصنع والأستاذ العام مطلوبتان للمطابقة");
        String sourceMin=scalar(source,"SELECT MIN(date) FROM cashbook WHERE date IS NOT NULL AND date<>''");
        String sourceMax=scalar(source,"SELECT MAX(date) FROM cashbook WHERE date IS NOT NULL AND date<>''");
        String ledgerMin=scalar(ledger,"SELECT MIN(transaction_date) FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND status='POSTED'");
        String ledgerMax=scalar(ledger,"SELECT MAX(transaction_date) FROM gl_journal_entries WHERE source_type='FACTORY_LEDGER' AND status='POSTED'");
        String start=minDate(sourceMin,ledgerMin),end=maxDate(sourceMax,ledgerMax);
        if(start==null||end==null) {
            String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
            start=today;end=today;
        }
        return reconcile(source,ledger,branch,currency,start,end);
    }

    private static String minDate(String a,String b) {
        if(a==null||a.trim().isEmpty())return b;
        if(b==null||b.trim().isEmpty())return a;
        return a.compareTo(b)<=0?a:b;
    }

    private static String maxDate(String a,String b) {
        if(a==null||a.trim().isEmpty())return b;
        if(b==null||b.trim().isEmpty())return a;
        return a.compareTo(b)>=0?a:b;
    }

    static Report reconcile(SQLiteDatabase source,SQLiteDatabase ledger,String branch,
                            String currency,String startDate,String endDate) {
        if(source==null||!source.isOpen()||ledger==null||!ledger.isOpen())
            throw new IllegalStateException("قاعدتا المصنع والأستاذ العام مطلوبتان للمطابقة");
        if(branch==null||!"MAIN".equals(branch.trim()))
            throw new IllegalArgumentException("دفتر المصنع الحالي يدعم الفرع MAIN فقط");
        if(currency==null||!"SDG".equals(currency.trim().toUpperCase(java.util.Locale.US)))
            throw new IllegalArgumentException("دفتر المصنع الحالي يدعم العملة SDG فقط");
        if(!FinancialPeriodService.validRange(startDate,endDate))
            throw new IllegalArgumentException("نطاق تاريخ المطابقة غير صحيح");

        // Compare transaction-currency SDG amounts; base-currency conversion is not part of this tie-out.\n        requireSingleActive(ledger,"SELECT COUNT(*) FROM branches WHERE branch_code=? AND active=1",branch,
            "الفرع الرئيسي غير موجود أو غير نشط");
        requireSingleActive(ledger,"SELECT COUNT(*) FROM chart_of_accounts WHERE account_code=? AND active=1","1000",
            "حساب الصندوق 1000 غير موجود أو غير نشط");
        requireSingleActive(ledger,"SELECT COUNT(*) FROM chart_of_accounts WHERE account_code=? AND active=1","1010",
            "حساب البنك/بنكك 1010 غير موجود أو غير نشط");

        AccountResult cash=account(source,ledger,"CASH","1000",branch,startDate,endDate);
        AccountResult bank=account(source,ledger,"BANKAK","1010",branch,startDate,endDate);
        return new Report(branch,"SDG",startDate,endDate,cash,bank);
    }

    private static AccountResult account(SQLiteDatabase source,SQLiteDatabase ledger,String sourceAccount,
                                         String glCode,String branch,String start,String end) {
        BigInteger sourceTotal=BigInteger.ZERO;
        int rows=0;
        Cursor c=source.rawQuery("SELECT kind,amount FROM cashbook WHERE account=? AND date>=? AND date<=? ORDER BY id",
            new String[]{sourceAccount,start,end});
        try {
            while(c.moveToNext()) {
                String kind=c.getString(0);long amount=c.getLong(1);
                if(amount<0)throw new IllegalStateException("دفتر النقدية يحتوي مبلغاً سالباً؛ الحساب "+sourceAccount);
                int sign=sign(kind);
                sourceTotal=sourceTotal.add(BigInteger.valueOf(amount).multiply(BigInteger.valueOf(sign)));
                rows++;
            }
        } finally {c.close();}

        BigInteger glTotal=BigInteger.ZERO;
        Cursor g=ledger.rawQuery("SELECT l.transaction_debit_minor,l.transaction_credit_minor "+
            "FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id "+
            "JOIN chart_of_accounts a ON a.id=l.account_id JOIN branches b ON b.id=l.branch_id "+
            "WHERE e.source_type='FACTORY_LEDGER' AND e.status='POSTED' AND a.account_code=? "+
            "AND b.branch_code=? AND l.transaction_currency_code='SDG' AND e.transaction_date>=? AND e.transaction_date<=?",
            new String[]{glCode,branch,start,end});
        try {
            while(g.moveToNext()) {
                glTotal=glTotal.add(BigInteger.valueOf(g.getLong(0)).subtract(BigInteger.valueOf(g.getLong(1))));
            }
        } finally {g.close();}
        return new AccountResult(sourceAccount,sourceTotal,glTotal,rows);
    }

    private static int sign(String kind) {
        if("SALE".equals(kind)||"COLLECTION".equals(kind))return 1;
        if("PURCHASE".equals(kind)||"EXPENSE".equals(kind)||"SALE_RETURN".equals(kind))return -1;
        throw new IllegalStateException("نوع حركة نقدية غير معروف ولا يمكن تجاهله: "+kind);
    }

    private static String scalar(SQLiteDatabase db,String sql) {
        Cursor c=db.rawQuery(sql,null);
        try{return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;}
        finally{c.close();}
    }

    private static void requireSingleActive(SQLiteDatabase db,String sql,String value,String error) {
        Cursor c=db.rawQuery(sql,new String[]{value});
        try{if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException(error);}
        finally{c.close();}
    }
}
