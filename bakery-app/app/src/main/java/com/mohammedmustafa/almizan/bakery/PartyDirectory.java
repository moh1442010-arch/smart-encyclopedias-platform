package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class PartyDirectory {
    private PartyDirectory() {}
    interface Selected { void onSelected(long id,String name); }
    static final class Party {
        final long id; final String name,phone;
        Party(long id,String name,String phone){this.id=id;this.name=name;this.phone=phone;}
        @Override public String toString(){return phone.isEmpty()?name:name+" — "+phone;}
    }

    static final class Db extends SQLiteOpenHelper {
        Db(Context c){super(c,"almizan_bakery_250.db",SecureDatabase.passphrase(c,"almizan_bakery_250.db"),null,6,0,null,null,false);}
        @Override public void onCreate(SQLiteDatabase db){
            db.execSQL("CREATE TABLE IF NOT EXISTS records (day TEXT NOT NULL,module TEXT NOT NULL,payload TEXT NOT NULL,updated_at INTEGER NOT NULL,PRIMARY KEY(day,module))");
            db.execSQL("CREATE TABLE IF NOT EXISTS inventory_usage (day TEXT PRIMARY KEY NOT NULL,flour_bags INTEGER NOT NULL DEFAULT 0,material_usage TEXT NOT NULL DEFAULT '{}')");
            EnterpriseSchema.ensure(db);
        }
        @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){EnterpriseSchema.ensure(db);}
    }

    static void migrateLegacyCustomers(SQLiteDatabase db) {
        Cursor c=db.rawQuery("SELECT payload FROM records WHERE module='customers' ORDER BY day",null);
        try {
            while(c.moveToNext()) {
                try {
                    JSONObject payload=new JSONObject(c.getString(0));
                    String rows=payload.optString("rows","");
                    for(String line:rows.split("\\r?\\n")) {
                        String[] p=line.split("\\|",-1);
                        String name=p.length>0?p[0].trim():"";
                        if(name.isEmpty())continue;
                        String phone=p.length>2?p[2].trim():"";
                        Cursor exists=db.rawQuery("SELECT 1 FROM customers WHERE name=? AND phone=? LIMIT 1",new String[]{name,phone});
                        boolean found;try{found=exists.moveToFirst();}finally{exists.close();}
                        if(!found) {
                            ContentValues v=new ContentValues();v.put("customer_code","CUS-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.US));
                            v.put("name",name);v.put("phone",phone);v.put("currency_code","SDG");v.put("created_at",System.currentTimeMillis());
                            db.insertWithOnConflict("customers",null,v,SQLiteDatabase.CONFLICT_IGNORE);
                        }
                    }
                } catch(Exception ignored) { }
            }
        } finally {c.close();}
    }

    static long findCustomerIdByName(SQLiteDatabase db,String name) {
        if(name==null||name.trim().isEmpty())return 0;
        Cursor c=db.rawQuery("SELECT id FROM customers WHERE name=? COLLATE NOCASE ORDER BY id LIMIT 1",new String[]{name.trim()});
        try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}
    }

    static long ensureCustomerName(SQLiteDatabase db,String name) {
        if(name==null||name.trim().isEmpty())throw new IllegalArgumentException("اسم العميل مطلوب.");
        Cursor c=db.rawQuery("SELECT id FROM customers WHERE name=? COLLATE NOCASE ORDER BY id LIMIT 1",new String[]{name.trim()});
        try {if(c.moveToFirst())return c.getLong(0);} finally {c.close();}
        ContentValues v=new ContentValues();v.put("customer_code","CUS-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.US));
        v.put("name",name.trim());v.put("phone","");v.put("currency_code","SDG");v.put("created_at",System.currentTimeMillis());
        return db.insertOrThrow("customers",null,v);
    }

    static long saveCustomer(SQLiteDatabase db,String name,String phone,long openingBalance,String currency) {
        requireParty(name,openingBalance,currency);
        Cursor c=db.rawQuery("SELECT id FROM customers WHERE name=? COLLATE NOCASE AND phone=? LIMIT 1",new String[]{name.trim(),safe(phone)});
        try {if(c.moveToFirst())throw new IllegalArgumentException("هذا العميل مسجل بالفعل؛ استخدم البحث لاختياره.");}
        finally {c.close();}
        ContentValues v=new ContentValues();v.put("customer_code","CUS-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.US));
        v.put("name",name.trim());v.put("phone",safe(phone));v.put("opening_balance_minor",openingBalance);v.put("currency_code",currency);v.put("created_at",System.currentTimeMillis());
        return db.insertOrThrow("customers",null,v);
    }

    static long saveSupplier(SQLiteDatabase db,String name,String phone,String item,String unit,long openingBalance,String currency) {
        requireParty(name,openingBalance,currency);
        Cursor c=db.rawQuery("SELECT id FROM suppliers WHERE name=? COLLATE NOCASE AND phone=? LIMIT 1",new String[]{name.trim(),safe(phone)});
        try {if(c.moveToFirst())throw new IllegalArgumentException("هذا المورد مسجل بالفعل؛ استخدم البحث لاختياره.");}
        finally {c.close();}
        ContentValues v=new ContentValues();v.put("supplier_code","SUP-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.US));
        v.put("name",name.trim());v.put("phone",safe(phone));v.put("default_item",safe(item));v.put("default_unit",safe(unit));
        v.put("opening_balance_minor",openingBalance);v.put("currency_code",currency);v.put("created_at",System.currentTimeMillis());
        return db.insertOrThrow("suppliers",null,v);
    }

    static void saveBankAccount(SQLiteDatabase db,String accountName,String bankName,String currency,String iban,String swift,String accountNumber) {
        if(blank(accountName)||blank(bankName)||blank(currency))throw new IllegalArgumentException("أدخل اسم الحساب والبنك والعملة.");
        String normalized=iban==null?"":iban.replaceAll("\\s+","").toUpperCase(Locale.US);
        if(!normalized.isEmpty()&&!normalized.matches("[A-Z]{2}[0-9A-Z]{13,32}"))throw new IllegalArgumentException("صيغة IBAN غير صحيحة. أدخل الرقم كما أصدره البنك.");
        if(!blank(swift)&&!swift.trim().matches("[A-Za-z0-9]{8}([A-Za-z0-9]{3})?"))throw new IllegalArgumentException("صيغة SWIFT/BIC غير صحيحة.");
        ContentValues v=new ContentValues();v.put("account_name",accountName.trim());v.put("bank_name",bankName.trim());v.put("currency_code",currency);
        v.put("iban",normalized);v.put("swift_bic",safe(swift).toUpperCase(Locale.US));v.put("account_number",safe(accountNumber));v.put("created_at",System.currentTimeMillis());
        db.insertOrThrow("company_bank_accounts",null,v);
    }

    static void showCustomerPicker(Activity activity,Selected callback) { showPicker(activity,true,callback); }
    static void showSupplierPicker(Activity activity,Selected callback) { showPicker(activity,false,callback); }
    private static void showPicker(Activity activity,boolean customer,Selected callback) {
        Db helper=new Db(activity);SQLiteDatabase db=helper.getReadableDatabase();
        String table=customer?"customers":"suppliers";
        Cursor c=db.rawQuery("SELECT id,name,phone FROM "+table+" WHERE active=1 ORDER BY name COLLATE NOCASE",null);
        List<Party> all=new ArrayList<>();
        try{while(c.moveToNext())all.add(new Party(c.getLong(0),c.getString(1),c.getString(2)));}finally{c.close();helper.close();}
        if(all.isEmpty()){new AlertDialog.Builder(activity).setMessage(customer?"لا يوجد عملاء مسجلون. أضف العميل أولاً من سجل العملاء والموردين.":"لا يوجد موردون مسجلون. أضف المورد أولاً من سجل العملاء والموردين.").setPositiveButton("حسنًا",null).show();return;}
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(12,8,12,8);
        EditText search=new EditText(activity);search.setSingleLine(true);search.setHint(customer?"ابحث باسم العميل أو الهاتف":"ابحث باسم المورد أو الهاتف");box.addView(search);
        ListView list=new ListView(activity);box.addView(list,new LinearLayout.LayoutParams(-1,0,1f));
        ArrayAdapter<Party> adapter=new ArrayAdapter<>(activity,android.R.layout.simple_list_item_1,new ArrayList<>(all));list.setAdapter(adapter);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(customer?"اختيار عميل":"اختيار مورد").setView(box).setNegativeButton("إلغاء",null).create();
        list.setOnItemClickListener((parent,view,position,id)->{Party p=adapter.getItem(position);if(p!=null)callback.onSelected(p.id,p.name);dialog.dismiss();});
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                String q=s.toString().trim().toLowerCase(Locale.ROOT);adapter.clear();
                for(Party p:all)if(p.name.toLowerCase(Locale.ROOT).contains(q)||p.phone.toLowerCase(Locale.ROOT).contains(q))adapter.add(p);
                adapter.notifyDataSetChanged();
            }
            public void afterTextChanged(Editable e){}
        });
        dialog.show();dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,(int)(activity.getResources().getDisplayMetrics().heightPixels*0.72f));
    }

    private static void requireParty(String name,long opening,String currency){
        if(blank(name))throw new IllegalArgumentException("الاسم مطلوب.");
        if(opening<0)throw new IllegalArgumentException("المديونية الافتتاحية لا تقبل قيمة سالبة.");
        if(blank(currency))throw new IllegalArgumentException("حدد العملة.");
    }
    private static String safe(String value){return value==null?"":value.trim();}
    private static boolean blank(String value){return value==null||value.trim().isEmpty();}
}
