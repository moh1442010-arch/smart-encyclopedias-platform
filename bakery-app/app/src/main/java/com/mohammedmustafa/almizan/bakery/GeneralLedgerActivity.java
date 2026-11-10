package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Read-only general-ledger screen. All values are read from posted GL journal lines.
 * It intentionally exposes no manual balance editing or posting operation.
 */
public final class GeneralLedgerActivity extends Activity {
    private SQLiteDatabase db;
    private LinearLayout body;
    private static final int NAVY=Color.rgb(11,31,58), GOLD=Color.rgb(212,175,55);

    @Override protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(AppLanguage.wrap(base));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AppUpdatePolicy.enforceCached(this);
        if(!LicenseAccessGate.isAccessAllowed(this)) {
            finish();
            startActivity(new Intent(this,MainActivity.class));
            return;
        }
        try {
            db=SecureDatabase.open(this,"almizan_bakery_250.db");
            EnterpriseSchema.ensure(db);
            render();
        } catch(RuntimeException ex) {
            showFailure(ex);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        AppUpdatePolicy.check(this);
        if(db!=null&&!LicenseAccessGate.isAccessAllowed(this)) {
            db.close();db=null;finish();
            startActivity(new Intent(this,MainActivity.class));
        }
    }

    @Override protected void onDestroy() {
        if(db!=null&&db.isOpen())db.close();
        super.onDestroy();
    }

    private void render() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(14,12,14,12);
        root.setBackgroundColor(Color.rgb(246,248,251));
        root.setLayoutDirection(AppLanguage.isEnglish(this)?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);

        TextView title=label("الأستاذ العام وميزان المراجعة",21,true);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        Button back=new Button(this);
        back.setText(AppLanguage.tr(this,"العودة إلى الرئيسية"));
        back.setOnClickListener(v->finish());
        root.addView(back);

        ScrollView scroll=new ScrollView(this);
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));
        setContentView(root);

        addSection("ملخص ميزان المراجعة");
        long debit=scalar("SELECT COALESCE(SUM(l.base_debit_minor),0) FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.status='POSTED'");
        long credit=scalar("SELECT COALESCE(SUM(l.base_credit_minor),0) FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id WHERE e.status='POSTED'");
        long entries=scalar("SELECT COUNT(*) FROM gl_journal_entries WHERE status='POSTED'");
        String currency=stringScalar("SELECT base_currency FROM company_settings WHERE id=1","SDG");
        addMetric("عدد القيود المرحلة",String.valueOf(entries));
        addMetric("إجمالي المدين",money(debit,currency));
        addMetric("إجمالي الدائن",money(credit,currency));
        addMetric("الفرق",money(debit-credit,currency));
        TextView balance=label(debit==credit?"ميزان المراجعة متوازن إجمالاً":"يوجد فرق في ميزان المراجعة؛ يلزم التحقيق قبل اعتماد التقارير",15,true);
        balance.setTextColor(debit==credit?Color.rgb(25,115,65):Color.rgb(170,45,35));
        body.addView(balance);

        addSection("أرصدة الحسابات");
        Cursor accounts=db.rawQuery(
            "SELECT a.account_code,a.name_ar,a.name_en,a.normal_side,"+
            "COALESCE(SUM(l.base_debit_minor),0),COALESCE(SUM(l.base_credit_minor),0) "+
            "FROM chart_of_accounts a LEFT JOIN gl_journal_lines l ON l.account_id=a.id "+
            "LEFT JOIN gl_journal_entries e ON e.id=l.entry_id AND e.status='POSTED' "+
            "WHERE a.active=1 GROUP BY a.id,a.account_code,a.name_ar,a.name_en,a.normal_side "+
            "ORDER BY a.account_code",null);
        try {
            while(accounts.moveToNext()) {
                String code=accounts.getString(0);
                String name=AppLanguage.isEnglish(this)?accounts.getString(2):accounts.getString(1);
                String side=accounts.getString(3);
                long dr=accounts.getLong(4),cr=accounts.getLong(5);
                long net=dr-cr;
                String normal="DEBIT".equals(side)?"مدين":"دائن";
                String amount=money(Math.abs(net),currency);
                String nature=net==0?"صفر":((net>0)==("DEBIT".equals(side))?"رصيد "+normal:"رصيد عكسي "+normal);
                TextView row=label(code+" — "+name+"\n"+nature+" | "+amount,14,false);
                row.setPadding(10,10,10,10);
                row.setBackgroundColor(Color.WHITE);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=6;
                body.addView(row,lp);
            }
        } finally {accounts.close();}

        addSection("آخر القيود المرحلة");
        Cursor journals=db.rawQuery(
            "SELECT e.entry_ref,e.transaction_date,e.description,e.source_type,e.source_id,"+
            "COALESCE(SUM(l.base_debit_minor),0),COALESCE(SUM(l.base_credit_minor),0) "+
            "FROM gl_journal_entries e JOIN gl_journal_lines l ON l.entry_id=e.id "+
            "WHERE e.status='POSTED' GROUP BY e.id ORDER BY e.id DESC LIMIT 50",null);
        try {
            if(!journals.moveToFirst())body.addView(label("لا توجد قيود مرحلة حتى الآن. تُرحّل العمليات من شاشاتها بعد استيفاء شروط الترحيل.",14,false));
            else do {
                String line=journals.getString(0)+" | "+journals.getString(1)+"\n"+journals.getString(2)+
                    "\n"+journals.getString(3)+":"+journals.getString(4)+
                    "\nمدين "+money(journals.getLong(5),currency)+" — دائن "+money(journals.getLong(6),currency);
                body.addView(label(line,13,false));
            } while(journals.moveToNext());
        } finally {journals.close();}

        addSection("حالة ترحيل المستندات");
        Cursor outbox=db.rawQuery("SELECT status,COUNT(*) FROM accounting_outbox GROUP BY status ORDER BY status",null);
        try {
            if(!outbox.moveToFirst())body.addView(label("لا توجد أحداث ترحيل في قائمة الانتظار.",13,false));
            else do body.addView(label(outbox.getString(0)+" : "+outbox.getLong(1),13,false));while(outbox.moveToNext());
        } finally {outbox.close();}
        body.addView(label("هذه شاشة عرض فقط. لا تسمح بتعديل أرصدة الأستاذ يدوياً. التصحيح يتم من المستند الأصلي أو بقيد عكسي موثق.",13,false));
    }

    private long scalar(String sql) {
        Cursor c=db.rawQuery(sql,null);try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}
    }
    private String stringScalar(String sql,String fallback) {
        Cursor c=db.rawQuery(sql,null);try{return c.moveToFirst()&&!c.isNull(0)?c.getString(0):fallback;}finally{c.close();}
    }
    private String money(long value,String currency) {
        NumberFormat f=NumberFormat.getIntegerInstance(AppLanguage.isEnglish(this)?Locale.US:new Locale("ar"));
        return f.format(value)+" "+currency;
    }
    private void addSection(String value) {
        TextView t=label(value,17,true);t.setTextColor(NAVY);
        body.addView(t);
    }
    private void addMetric(String name,String value) {
        TextView t=label(name+"\n"+value,15,true);
        t.setBackgroundColor(Color.WHITE);t.setPadding(12,10,12,10);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=5;
        body.addView(t,lp);
    }
    private TextView label(String value,int size,boolean bold) {
        TextView t=new TextView(this);t.setText(AppLanguage.tr(this,value));t.setTextSize(size);
        t.setTextColor(NAVY);t.setPadding(5,8,5,8);
        if(bold)t.setTypeface(null,android.graphics.Typeface.BOLD);
        return t;
    }
    private void showFailure(RuntimeException ex) {
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(18,18,18,18);
        TextView message=label("تعذر فتح دفتر الأستاذ: "+String.valueOf(ex.getMessage()),15,true);
        root.addView(message);Button back=new Button(this);back.setText("العودة");back.setOnClickListener(v->finish());root.addView(back);
        setContentView(root);
        android.util.Log.e("AlMizanGL","General ledger screen failed",ex);
    }
}
