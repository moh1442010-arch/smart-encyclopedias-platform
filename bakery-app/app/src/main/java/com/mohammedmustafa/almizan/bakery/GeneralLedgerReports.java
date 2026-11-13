package com.mohammedmustafa.almizan.bakery;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only financial reports derived exclusively from posted general-ledger lines.
 * All amounts are integer minor units in the company base currency.
 */
final class GeneralLedgerReports {
    private GeneralLedgerReports() {}

    static final class Line {
        final String code, nameAr, nameEn, type, normalSide;
        final long debitMinor, creditMinor, balanceMinor;
        Line(String code,String nameAr,String nameEn,String type,String normalSide,long debit,long credit) {
            this.code=code;this.nameAr=nameAr;this.nameEn=nameEn;this.type=type;this.normalSide=normalSide;
            this.debitMinor=debit;this.creditMinor=credit;
            this.balanceMinor="DEBIT".equals(normalSide)
                ? exactLong(BigInteger.valueOf(debit).subtract(BigInteger.valueOf(credit)))
                : exactLong(BigInteger.valueOf(credit).subtract(BigInteger.valueOf(debit)));
        }
    }

    static final class TrialBalance {
        final List<Line> lines;
        final long debitMinor, creditMinor;
        TrialBalance(List<Line> lines,long debit,long credit) {
            this.lines=lines;this.debitMinor=debit;this.creditMinor=credit;
        }
        boolean isBalanced(){return debitMinor==creditMinor;}
    }

    static final class IncomeStatement {
        final List<Line> lines;
        final long revenueMinor, expenseMinor, netProfitMinor;
        IncomeStatement(List<Line> lines,long revenue,long expense) {
            this.lines=lines;this.revenueMinor=revenue;this.expenseMinor=expense;
            this.netProfitMinor=exactLong(BigInteger.valueOf(revenue).subtract(BigInteger.valueOf(expense)));
        }
    }

    static final class AccountStatement {
        final String accountCode;
        final long openingDebitMinor, openingCreditMinor, periodDebitMinor, periodCreditMinor;
        AccountStatement(String code,long od,long oc,long pd,long pc) {
            accountCode=code;openingDebitMinor=od;openingCreditMinor=oc;
            periodDebitMinor=pd;periodCreditMinor=pc;
        }
        long closingSignedDebitBalance() {
            return exactLong(BigInteger.valueOf(openingDebitMinor).subtract(BigInteger.valueOf(openingCreditMinor)).add(BigInteger.valueOf(periodDebitMinor)).subtract(BigInteger.valueOf(periodCreditMinor)));
        }
    }

    static TrialBalance trialBalance(SQLiteDatabase db,String from,String to,String branchCode) {
        validateRange(from,to);
        String sql="SELECT a.account_code,a.name_ar,a.name_en,a.account_type,a.normal_side,"+
            "COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.base_debit_minor ELSE 0 END),0),"+
            "COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.base_credit_minor ELSE 0 END),0) "+
            "FROM chart_of_accounts a LEFT JOIN gl_journal_lines l ON l.account_id=a.id "+
            "LEFT JOIN gl_journal_entries e ON e.id=l.entry_id AND e.status='POSTED' AND e.transaction_date<=? "+
            "LEFT JOIN branches b ON b.id=l.branch_id "+
            "WHERE (?='' OR b.branch_code=?) "+
            "GROUP BY a.id,a.account_code,a.name_ar,a.name_en,a.account_type,a.normal_side HAVING a.active=1 OR COUNT(e.id)>0 ORDER BY a.account_code";
        String branch=branchArg(branchCode);
        Cursor c=db.rawQuery(sql,new String[]{to,branch,branch});
        try {
            List<Line> lines=new ArrayList<>();long debit=0,credit=0;
            while(c.moveToNext()) {
                long d=c.getLong(5),cr=c.getLong(6);
                debit=add(debit,d);credit=add(credit,cr);
                lines.add(new Line(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),d,cr));
            }
            return new TrialBalance(lines,debit,credit);
        } finally {c.close();}
    }

    static AccountStatement accountStatement(SQLiteDatabase db,String accountCode,String from,String to,String branchCode) {
        if(blank(accountCode))throw new IllegalArgumentException("رمز الحساب مطلوب");
        validateRange(from,to);
        long openingDebit=sum(db,accountCode,null,from,branchCode,"DEBIT");
        long openingCredit=sum(db,accountCode,null,from,branchCode,"CREDIT");
        long periodDebit=sum(db,accountCode,from,to,branchCode,"DEBIT");
        long periodCredit=sum(db,accountCode,from,to,branchCode,"CREDIT");
        return new AccountStatement(accountCode,openingDebit,openingCredit,periodDebit,periodCredit);
    }

    static TrialBalance incomeStatement(SQLiteDatabase db,String from,String to,String branchCode) {
        return byTypes(db,from,to,branchCode,new String[]{"REVENUE","EXPENSE"});
    }

    static IncomeStatement incomeStatementSummary(SQLiteDatabase db,String from,String to,String branchCode) {
        TrialBalance source=byTypes(db,from,to,branchCode,new String[]{"REVENUE","EXPENSE"});
        long revenue=0L,expense=0L;
        for(Line line:source.lines) {
            long net=exactLong(BigInteger.valueOf(line.debitMinor).subtract(BigInteger.valueOf(line.creditMinor)));
            if("REVENUE".equals(line.type)) {
                long amount="CREDIT".equals(line.normalSide)?-net:net;
                revenue=add(revenue,amount);
            } else if("EXPENSE".equals(line.type)) {
                long amount="DEBIT".equals(line.normalSide)?net:-net;
                expense=add(expense,amount);
            }
        }
        return new IncomeStatement(source.lines,revenue,expense);
    }

    // Account balances only; a complete statement of financial position also requires a posted profit/loss close to equity.
    static TrialBalance balanceSheetAccounts(SQLiteDatabase db,String asOf,String branchCode) {
        validateRange(asOf,asOf);
        return byTypes(db,"0001-01-01",asOf,branchCode,new String[]{"ASSET","LIABILITY","EQUITY"});
    }

    private static TrialBalance byTypes(SQLiteDatabase db,String from,String to,String branchCode,String[] types) {
        validateRange(from,to);
        if(types==null||types.length==0)throw new IllegalArgumentException("أنواع الحسابات مطلوبة");
        StringBuilder placeholders=new StringBuilder();
        for(int i=0;i<types.length;i++){if(i>0)placeholders.append(",");placeholders.append("?");}
        String sql="SELECT a.account_code,a.name_ar,a.name_en,a.account_type,a.normal_side,"+
            "COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.base_debit_minor ELSE 0 END),0),"+
            "COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.base_credit_minor ELSE 0 END),0) "+
            "FROM chart_of_accounts a LEFT JOIN gl_journal_lines l ON l.account_id=a.id "+
            "LEFT JOIN gl_journal_entries e ON e.id=l.entry_id AND e.status='POSTED' AND e.transaction_date>=? AND e.transaction_date<=? "+
            "LEFT JOIN branches b ON b.id=l.branch_id "+
            "WHERE a.account_type IN ("+placeholders+") "+
            "AND (?='' OR b.branch_code=?) "+
            "GROUP BY a.id,a.account_code,a.name_ar,a.name_en,a.account_type,a.normal_side HAVING a.active=1 OR COUNT(e.id)>0 ORDER BY a.account_code";
        String[] queryArgs=new String[types.length+4];
        queryArgs[0]=from;queryArgs[1]=to;
        System.arraycopy(types,0,queryArgs,2,types.length);
        String branch=branchArg(branchCode);
        queryArgs[types.length+2]=branch;queryArgs[types.length+3]=branch;
        Cursor c=db.rawQuery(sql,queryArgs);
        try {
            List<Line> lines=new ArrayList<>();long debit=0,credit=0;
            while(c.moveToNext()) {
                long d=c.getLong(5),cr=c.getLong(6);debit=add(debit,d);credit=add(credit,cr);
                lines.add(new Line(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),d,cr));
            }
            return new TrialBalance(lines,debit,credit);
        } finally {c.close();}
    }

    private static long sum(SQLiteDatabase db,String code,String from,String to,String branchCode,String side) {
        String amount="DEBIT".equals(side)?"l.base_debit_minor":"l.base_credit_minor";
        String dateClause=from==null?"e.transaction_date<?":"e.transaction_date>=? AND e.transaction_date<=?";
        String sql="SELECT COALESCE(SUM("+amount+"),0) FROM gl_journal_lines l "+
            "JOIN gl_journal_entries e ON e.id=l.entry_id JOIN chart_of_accounts a ON a.id=l.account_id "+
            "JOIN branches b ON b.id=l.branch_id WHERE a.account_code=? AND e.status='POSTED' AND "+dateClause+
            " AND (?='' OR b.branch_code=?)";
        String branch=branchArg(branchCode);
        String[] args=from==null?new String[]{code,to,branch,branch}:new String[]{code,from,to,branch,branch};
        Cursor c=db.rawQuery(sql,args);
        try {c.moveToFirst();return c.getLong(0);} finally {c.close();}
    }

    private static String branchArg(String branchCode){return branchCode==null?"":branchCode.trim();}
    private static long add(long a,long b) {return exactLong(BigInteger.valueOf(a).add(BigInteger.valueOf(b)));}
    private static long exactLong(BigInteger value) {
        if(value.compareTo(BigInteger.valueOf(Long.MIN_VALUE))<0 || value.compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0)
            throw new ArithmeticException("المبلغ يتجاوز النطاق الرقمي المسموح");
        return value.longValue();
    }
    private static boolean blank(String s){return s==null||s.trim().isEmpty();}
    private static void validateRange(String from,String to) {
        if(from==null||to==null||!from.matches("\\d{4}-\\d{2}-\\d{2}")||
           !to.matches("\\d{4}-\\d{2}-\\d{2}")||from.compareTo(to)>0)
            throw new IllegalArgumentException("نطاق تاريخ التقرير غير صحيح");
    }
}
