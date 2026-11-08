package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import android.content.ContentValues;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

public class PartyDirectoryActivity extends Activity {
    private static final int NAVY=0xFF0B1F3A,GOLD=0xFFD4AF37;
    private PartyDirectory.Db helper;
    private SQLiteDatabase db;
    private LinearLayout body;
    private final android.os.Handler entitlementHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable entitlementWatchdog = new Runnable() {
        @Override public void run() {
            if (!LicenseAccessGate.isAccessAllowed(PartyDirectoryActivity.this)) enforceEntitlement();
            else entitlementHandler.postDelayed(this,30000L);
        }
    };
    private boolean enforceEntitlement() {
        if (LicenseAccessGate.isAccessAllowed(this)) return true;
        entitlementHandler.removeCallbacks(entitlementWatchdog);
        if (helper != null) { helper.close(); helper=null; db=null; }
        Toast.makeText(this,"انتهت التجربة أو لا يوجد ترخيص ساري. بياناتك محفوظة؛ يرجى التفعيل للمتابعة.",Toast.LENGTH_LONG).show();
        finish();
        startActivity(new Intent(this,MainActivity.class));
        return false;
    }

    @Override protected void attachBaseContext(android.content.Context newBase){super.attachBaseContext(AppLanguage.wrap(newBase));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        if(!enforceEntitlement())return;
        helper=new PartyDirectory.Db(this);db=helper.getWritableDatabase();
        PartyDirectory.migrateLegacyCustomers(db);
        render();
    }
    @Override protected void onResume(){super.onResume();if(db!=null&&!enforceEntitlement())return;entitlementHandler.removeCallbacks(entitlementWatchdog);entitlementHandler.post(entitlementWatchdog);}
    @Override protected void onPause(){entitlementHandler.removeCallbacks(entitlementWatchdog);super.onPause();}
    @Override protected void onDestroy(){entitlementHandler.removeCallbacks(entitlementWatchdog);if(helper!=null)helper.close();super.onDestroy();}

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(AppLanguage.isEnglish(this)?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);root.setPadding(12,10,12,12);root.setBackgroundColor(0xFFF6F8FB);
        TextView title=new TextView(this);title.setText(AppLanguage.tr(this,getSharedPreferences("bakery_profile",MODE_PRIVATE).getString("business_name","المخبز")+"\\nسجل العملاء والموردين والحسابات البنكية"));title.setTextSize(20);title.setTextColor(NAVY);title.setGravity(Gravity.CENTER);root.addView(title);
        Button home=button("⌂ الرئيسية");home.setOnClickListener(v->{Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);finish();});root.addView(home);
        root.addView(button("🌐 اللغة / Language"));
        root.getChildAt(2).setOnClickListener(v->AppLanguage.showPicker(this));
        Button back=button("↩ رجوع إلى الشاشة السابقة");back.setOnClickListener(v->finish());root.addView(back);
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Button print=button("🖨️ طباعة سجل العملاء والموردين والحسابات");print.setOnClickListener(v->printDirectory());root.addView(print);
        setContentView(root);
        customersSection();suppliersSection();bankAccountsSection();exchangeRatesSection();
    }

    private void customersSection(){
        heading("العملاء الأساسيون");
        body.addView(note("سجّل العميل مرة واحدة. يظهر بعد ذلك في مؤشر البحث داخل شاشات البيع والتوزيع والتحصيل."));
        EditText name=cell("اسم العميل"),phone=cell("رقم الهاتف"),debt=cell("إجمالي المديونية"),cash=cell("سداد نقدي"),bank=cell("سداد بنكك"),bankRef=cell("رقم العملية");
        Spinner currency=currencySpinner();
        addHorizontal(name,phone,debt,cash,bank,bankRef,currency);
        Button save=button("حفظ العميل");save.setOnClickListener(v->{try{
            String currencyCode=currency.getSelectedItem().toString();long opening=amount(debt,currencyCode),cashPaid=amount(cash,currencyCode),bankPaid=amount(bank,currencyCode);
            long id=PartyDirectory.saveCustomer(db,name.getText().toString(),phone.getText().toString(),opening,cashPaid,bankPaid,bankRef.getText().toString(),currencyCode);
            toast("تم حفظ العميل برقم "+id+" — المديونية المتبقية: "+(opening-cashPaid-bankPaid));
            name.setText("");phone.setText("");debt.setText("");cash.setText("");bank.setText("");bankRef.setText("");refreshLists();
        }catch(Exception e){toast("لم يُحفظ العميل: "+e.getMessage());}});body.addView(save);
        Button searchCustomer=button("بحث عن عميل مسجل");
        searchCustomer.setOnClickListener(v->PartyDirectory.showCustomerPicker(this,(id,selectedName)->{name.setText(selectedName);name.setTag(id);}));
        body.addView(searchCustomer);
        body.addView(note("التسجيل أفقي: اسم العميل | الهاتف | إجمالي المديونية | نقدي | بنكك | رقم العملية | العملة. تُحفظ المديونية المتبقية بعد خصم السداد الافتتاحي. وتبقى التحصيلات اللاحقة في شاشة التحصيل."));
        list("SELECT x.name||' | '||x.phone||' | المديونية: '||CASE c.minor_units WHEN 2 THEN printf('%.2f',x.opening_balance_minor/100.0) ELSE CAST(x.opening_balance_minor AS TEXT) END||' '||x.currency_code FROM customers x JOIN currencies c ON c.code=x.currency_code WHERE x.active=1 ORDER BY x.name");
    }

    private void suppliersSection(){
        heading("الموردون الأساسيون");
        body.addView(note("سجّل المورد مرة واحدة. الصنف والوحدة حقول افتراضية تساعد في الفواتير اللاحقة."));
        EditText name=cell("اسم المورد"),phone=cell("رقم الهاتف"),item=cell("الصنف"),unit=cell("الوحدة"),qty=cell("الكمية"),invoice=cell("قيمة الفاتورة"),cash=cell("سداد نقدي"),bank=cell("سداد بنكك"),bankRef=cell("رقم العملية");
        Spinner currency=currencySpinner();
        addHorizontal(name,phone,item,unit,qty,invoice,cash,bank,bankRef,currency);
        Button save=button("حفظ المورد والفاتورة الافتتاحية");save.setOnClickListener(v->{try{
            String currencyCode=currency.getSelectedItem().toString();long quantity=quantity(qty),invoiceTotal=amount(invoice,currencyCode),cashPaid=amount(cash,currencyCode),bankPaid=amount(bank,currencyCode);
            long id=PartyDirectory.saveSupplier(db,name.getText().toString(),phone.getText().toString(),item.getText().toString(),unit.getText().toString(),quantity,invoiceTotal,cashPaid,bankPaid,bankRef.getText().toString(),currencyCode);
            toast("تم حفظ المورد برقم "+id+" — المديونية المتبقية: "+(invoiceTotal-cashPaid-bankPaid));
            name.setText("");phone.setText("");item.setText("");unit.setText("");qty.setText("");invoice.setText("");cash.setText("");bank.setText("");bankRef.setText("");refreshLists();
        }catch(Exception e){toast("لم يُحفظ المورد: "+e.getMessage());}});body.addView(save);
        Button searchSupplier=button("بحث عن مورد مسجل");
        searchSupplier.setOnClickListener(v->PartyDirectory.showSupplierPicker(this,(id,selectedName)->{name.setText(selectedName);name.setTag(id);}));
        body.addView(searchSupplier);
        body.addView(note("التسجيل أفقي: اسم المورد | الهاتف | الصنف | الوحدة | الكمية | قيمة الفاتورة | نقدي | بنكك | رقم العملية | العملة. تحفظ الفاتورة الافتتاحية والمديونية المتبقية في سجل المورد."));
        list("SELECT x.name||' | '||x.phone||' | '||x.default_item||' | المديونية: '||CASE c.minor_units WHEN 2 THEN printf('%.2f',x.opening_balance_minor/100.0) ELSE CAST(x.opening_balance_minor AS TEXT) END||' '||x.currency_code FROM suppliers x JOIN currencies c ON c.code=x.currency_code WHERE x.active=1 ORDER BY x.name");
    }

    private void bankAccountsSection(){
        heading("حسابات استقبال العملات وبيانات IBAN");
        body.addView(note("أضف بيانات الحساب البنكي التي تصدرها الجهة المصرفية. لا نضع أرقام IBAN افتراضية."));
        EditText account=cell("اسم الحساب/صاحب الحساب"),bank=cell("اسم البنك"),iban=cell("IBAN"),swift=cell("SWIFT/BIC"),number=cell("رقم الحساب المحلي");
        Spinner currency=currencySpinner();
        addHorizontal(account,bank,currency,iban,swift,number);
        Button save=button("حفظ الحساب البنكي");save.setOnClickListener(v->{try{
            PartyDirectory.saveBankAccount(db,account.getText().toString(),bank.getText().toString(),currency.getSelectedItem().toString(),iban.getText().toString(),swift.getText().toString(),number.getText().toString());
            toast("تم حفظ حساب الاستقبال.");account.setText("");bank.setText("");iban.setText("");swift.setText("");number.setText("");refreshLists();
        }catch(Exception e){toast("لم يُحفظ الحساب: "+e.getMessage());}});body.addView(save);
        list("SELECT bank_name||' | '||account_name||' | '||currency_code||' | IBAN: '||CASE WHEN iban='' THEN 'غير مدخل' ELSE iban END FROM company_bank_accounts WHERE active=1 ORDER BY currency_code");
    }

    private void insertPartyPayment(String type,long id,String direction,long amount,String method,String currency,String reference){
        ContentValues v=new ContentValues();v.put("party_type",type);v.put("party_id",id);v.put("direction",direction);v.put("payment_date",today());v.put("amount_minor",amount);v.put("method",method);v.put("currency_code",currency);v.put("reference",reference);v.put("created_at",System.currentTimeMillis());db.insertOrThrow("party_payments",null,v);
    }
    private void exchangeRatesSection(){
        heading("أسعار الصرف المؤرخة");
        body.addView(note("أدخل السعر كنسبة دقيقة: وحدة واحدة من العملة الأولى تساوي البسط ÷ المقام من العملة الثانية. مثال توضيحي فقط: 1 SAR = 750/1 SDG. لا تُستخدم الأسعار في الفواتير حتى ربط كل مسار محاسبي بخدمة التحويل."));
        Spinner base=currencySpinner(),quote=currencySpinner();
        EditText numerator=cell("بسط سعر الصرف"),denominator=cell("مقام سعر الصرف"),effective=cell("تاريخ السريان YYYY-MM-DD"),source=cell("مصدر السعر");
        effective.setText(today());denominator.setText("1");
        addHorizontal(base,quote,numerator,denominator,effective,source);
        Button save=button("حفظ سعر الصرف");save.setOnClickListener(v->{try{
            String from=base.getSelectedItem().toString(),to=quote.getSelectedItem().toString();
            long n=amount(numerator),d=amount(denominator);String date=effective.getText().toString().trim();
            if(from.equals(to)||n<=0||d<=0||!date.matches("\\d{4}-\\d{2}-\\d{2}")||source.getText().toString().trim().isEmpty())throw new IllegalArgumentException("حدد عملتين مختلفتين وبسطاً ومقاماً موجبين وتاريخاً ومصدراً.");
            ContentValues values=new ContentValues();values.put("base_code",from);values.put("quote_code",to);values.put("rate_num",n);values.put("rate_den",d);values.put("effective_at",date);values.put("source",source.getText().toString().trim());values.put("created_at",System.currentTimeMillis());
            db.insertOrThrow("exchange_rates",null,values);toast("تم حفظ سعر الصرف المؤرخ.");refreshLists();
        }catch(Exception e){toast("لم يُحفظ سعر الصرف: "+e.getMessage());}});body.addView(save);
        list("SELECT '1 '||base_code||' = '||rate_num||'/'||rate_den||' '||quote_code||' | ساري من: '||effective_at||' | المصدر: '||source FROM exchange_rates ORDER BY effective_at DESC,id DESC");
    }

    private void printDirectory(){
        String business=getSharedPreferences("bakery_profile",MODE_PRIVATE).getString("business_name","المخبز");
        StringBuilder out=new StringBuilder("المنشأة: ").append(business).append("\\nسجل العملاء والموردين والحسابات البنكية\\nالتاريخ: ").append(today()).append("\\n\\n");
        appendPrintable(body,out);
        try{PrinterService.print(this,business,"سجل العملاء والموردين",out.toString());}catch(Exception e){toast("تعذر فتح الطباعة: "+e.getMessage());}
    }
    private void appendPrintable(View view,StringBuilder out){
        if(view instanceof EditText){String value=((EditText)view).getText().toString().trim();if(!value.isEmpty())out.append(value).append("\\n");return;}
        if(view instanceof TextView){String value=((TextView)view).getText().toString().trim();if(!value.isEmpty())out.append(value).append("\\n");}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)appendPrintable(group.getChildAt(i),out);}
    }
    private void refreshLists(){render();}
    private void heading(String s){TextView t=note(s);t.setTextColor(NAVY);t.setTextSize(18);t.setTypeface(null,1);body.addView(t);}
    private TextView note(String s){TextView t=new TextView(this);t.setText(AppLanguage.tr(this,s));t.setTextSize(13);t.setTextColor(0xFF334155);t.setPadding(5,7,5,7);return t;}
    private Button button(String s){Button b=new Button(this);b.setText(AppLanguage.tr(this,s));b.setAllCaps(false);b.setTextColor(NAVY);b.setBackgroundColor(GOLD);return b;}
    private EditText cell(String hint){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(AppLanguage.tr(this,hint));e.setMinWidth(dp(160));e.setTextSize(14);return e;}
    private void addHorizontal(View... views){HorizontalScrollView h=new HorizontalScrollView(this);h.setHorizontalScrollBarEnabled(true);LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setLayoutDirection(AppLanguage.isEnglish(this)?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);for(View v:views){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(170),dp(56));lp.setMargins(2,2,2,2);row.addView(v,lp);}h.addView(row);body.addView(h,new LinearLayout.LayoutParams(-1,-2));}
    private Spinner currencySpinner(){Spinner s=new Spinner(this);ArrayList<String> codes=new ArrayList<>();Cursor cur=db.rawQuery("SELECT code FROM currencies WHERE active=1 ORDER BY CASE code WHEN 'SDG' THEN 0 ELSE 1 END,name_ar",null);try{while(cur.moveToNext())codes.add(cur.getString(0));}finally{cur.close();}ArrayAdapter<String>a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,codes);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setMinimumWidth(dp(130));return s;}
    private void list(String sql){Cursor c=null;try{c=db.rawQuery(sql,null);while(c.moveToNext())body.addView(note("• "+c.getString(0)));}catch(Exception e){body.addView(note("تعذر عرض القائمة: "+e.getMessage()));}finally{if(c!=null)c.close();}}
    private long amount(EditText e,String currency){String s=e.getText().toString().trim();if(s.isEmpty())return 0;int units=0;Cursor c=null;try{c=db.rawQuery("SELECT minor_units FROM currencies WHERE code=?",new String[]{currency});if(c.moveToFirst())units=c.getInt(0);}finally{if(c!=null)c.close();}return new java.math.BigDecimal(s).movePointRight(units).setScale(0,java.math.RoundingMode.UNNECESSARY).longValueExact();}
    private long amount(EditText e){String s=e.getText().toString().trim();if(s.isEmpty())return 0;return new java.math.BigDecimal(s).setScale(0,java.math.RoundingMode.UNNECESSARY).longValueExact();}
    private long quantity(EditText e){String s=e.getText().toString().trim();if(s.isEmpty())return 0;return new java.math.BigDecimal(s).movePointRight(3).setScale(0,java.math.RoundingMode.UNNECESSARY).longValueExact();}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    private void toast(String s){Toast.makeText(this,AppLanguage.tr(this,s),Toast.LENGTH_LONG).show();}

    private static final class TextWatcherAmount {
        static void watch(EditText[] fields,Runnable update){
            android.text.TextWatcher w=new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){update.run();}public void afterTextChanged(android.text.Editable e){}};
            for(EditText e:fields)e.addTextChangedListener(w);
        }
    }
}
