package com.mohammedmustafa.almizan.bakery;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FactoryActivity extends Activity {
    private static final int NAVY=Color.rgb(11,31,58), GOLD=Color.rgb(212,175,55);
    private DB db; private LinearLayout body; private String screen="home";
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    @Override public void onCreate(Bundle b){super.onCreate(b); db=new DB(this); home();}
    @Override public void onBackPressed(){if(!"home".equals(screen))home();else super.onBackPressed();}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(30,40,50));t.setPadding(4,10,4,10);return t;}
    private Button btn(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
    private void frame(String title){
        screen=title; LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(14,12,14,16);root.setBackgroundColor(Color.rgb(246,248,251));
        TextView head=label("🏭 مصنع الخبائز والحلويات والباسطة\n"+title,21);head.setTextColor(NAVY);head.setBackgroundColor(Color.WHITE);root.addView(head);
        root.addView(btn("⌂ العودة إلى مصنع الخبائز والحلويات والباسطة",()->home()));
        ScrollView sc=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(2,8,2,8);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private void home(){
        frame("الرئيسية");
        body.addView(label("نظام مستقل عن دفاتر المخبز. كل المخزون والحركات والتكاليف هنا محفوظة في قاعدة بيانات منفصلة.",15));
        body.addView(btn("1. مخزن المواد الخام ومستلزمات المصنع",()->warehouse()));
        body.addView(btn("2. المنتجات والوصفات",()->recipes()));
        body.addView(btn("3. الإنتاج والتشغيل اليومي",()->production()));
        body.addView(btn("4. التوزيع والمبيعات والتحصيل",()->sales()));
        body.addView(btn("5. المصروفات والتالف",()->expenses()));
        body.addView(btn("6. حساب التشغيل والتكلفة",()->costing()));
        body.addView(btn("7. الحسابات والأرصدة والتقارير",()->accounts()));
    }
    private EditText field(String hint,boolean decimal){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setInputType(decimal?android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:android.text.InputType.TYPE_CLASS_TEXT);body.addView(e);return e;}
    private long num(EditText e){try{java.math.BigDecimal x=new java.math.BigDecimal(e.getText().toString().trim());return x.movePointRight(3).longValueExact();}catch(Exception ex){return 0;}}
    private long money(EditText e){try{return new java.math.BigDecimal(e.getText().toString().trim()).longValueExact();}catch(Exception ex){return 0;}}
    private void warehouse(){
        frame("المخزن");
        body.addView(label("تسجل الكميات بوحدة المادة. الأسعار بالجنيه السوداني. الكمية الداخلية تحفظ بأجزاء من الألف لدعم الكسور.",13));
        EditText n=field("اسم المادة: دقيق، سكر، زيت، سمن، خميرة، بيض، حليب، شوكولاتة، عبوات...",false);
        EditText u=field("وحدة القياس: كجم، لتر، جوال، قطعة...",false);
        EditText q=field("كمية الوارد",true),p=field("إجمالي تكلفة الوارد بالجنيه",false);
        body.addView(btn("حفظ وارد مخزني",()->{String name=n.getText().toString().trim(),unit=u.getText().toString().trim();long qty=num(q),cost=money(p);if(name.isEmpty()||unit.isEmpty()||qty<=0||cost<0){toast("أكمل الاسم والوحدة والكمية والتكلفة بصورة صحيحة");return;}db.receive(name,unit,qty,cost);toast("تم تسجيل الوارد");warehouse();}));
        list("SELECT name||' — '||unit||' | الرصيد: '||printf('%.3f',qty/1000.0)||' | قيمة الرصيد: '||value FROM materials ORDER BY name");
    }
    private void recipes(){
        frame("المنتجات والوصفات");
        body.addView(label("أنشئ المنتج أولاً ثم أضف مكوناته. كمية المكون هي الكمية اللازمة لإنتاج دفعة واحدة.",14));
        EditText n=field("اسم المنتج: خبائز، مخبازات، فطائر، باستا، حلويات...",false);
        body.addView(btn("إضافة منتج",()->{String x=n.getText().toString().trim();if(x.isEmpty()){toast("أدخل اسم المنتج");return;}db.exec("INSERT OR IGNORE INTO products(name) VALUES(?)",new String[]{x});toast("تم حفظ المنتج");recipes();}));
        EditText product=field("اسم المنتج المطابق",false),material=field("اسم المادة المطابق",false),qty=field("كمية المادة لكل دفعة",true);
        body.addView(btn("إضافة مكون للوصفة",()->{String pn=product.getText().toString().trim(),mn=material.getText().toString().trim();long q=num(qty);if(q<=0){toast("الكمية يجب أن تكون أكبر من صفر");return;}long pid=db.id("products",pn),mid=db.id("materials",mn);if(pid==0||mid==0){toast("تأكد من تسجيل المنتج والمادة في النظام");return;}db.exec("INSERT INTO recipe(product_id,material_id,qty) VALUES(?,?,?)",new String[]{""+pid,""+mid,""+q});toast("تم ربط المكون بالوصفة");recipes();}));
        list("SELECT name FROM products ORDER BY name");
    }
    private void production(){
        frame("الإنتاج والتشغيل اليومي");
        body.addView(label("الإنتاج لا يخصم المواد عند حفظ المسودة. الترحيل النهائي ينفذ داخل معاملة واحدة، ويمنع ترحيل الدفعة مرتين.",14));
        EditText product=field("اسم المنتج",false),batches=field("عدد الدفعات",false),output=field("الكمية السليمة المنتجة",true),labor=field("أجور العمال لهذه الدفعة بالجنيه",false),over=field("تكاليف تشغيل مباشرة أخرى بالجنيه",false);
        body.addView(btn("ترحيل دفعة الإنتاج نهائياً",()->{String pn=product.getText().toString().trim();long pid=db.id("products",pn),b=money(batches),out=num(output),w=money(labor),oh=money(over);if(pid==0||b<=0||out<=0||w<0||oh<0){toast("راجع المنتج والكميات والتكاليف");return;}try{db.finalizeProduction(pid,b,out,w,oh);toast("تم ترحيل الإنتاج وخصم المواد وإضافة المنتج للمخزون");production();}catch(Exception ex){toast("لم يرحل الإنتاج: "+ex.getMessage());}}));
        list("SELECT p.name||' | الكمية المنتجة: '||printf('%.3f',f.qty/1000.0)||' | تكلفة الوحدة: '||f.unit_cost FROM finished f JOIN products p ON p.id=f.product_id");
    }
    private void sales(){
        frame("التوزيع والمبيعات والتحصيل");
        body.addView(label("كل عملية بيع تسجل مرة واحدة سواء تمت من الشباك أو بالتوزيع. أدخل قيمة البيع والمتحصل نقداً وبنكك، والباقي يصبح ديناً.",14));
        EditText customer=field("اسم العميل أو الشباك",false),product=field("اسم المنتج",false),qty=field("الكمية المباعة",true),price=field("سعر البيع الإجمالي بالجنيه",false),cash=field("المبلغ النقدي",false),bank=field("تحويل بنكك",false),ref=field("رقم عملية بنكك إن وجد",false);
        body.addView(btn("تسجيل البيع والتحصيل",()->{long pid=db.id("products",product.getText().toString().trim()),q=num(qty),v=money(price),c=money(cash),bk=money(bank);String cust=customer.getText().toString().trim();if(pid==0||q<=0||v<=0||c<0||bk<0||c+bk>v||cust.isEmpty()){toast("بيانات البيع أو التحصيل غير صحيحة");return;}try{db.sell(pid,q,v,c,bk,cust,ref.getText().toString().trim());toast("تم تسجيل البيع والرصيد المستحق");sales();}catch(Exception ex){toast("تعذر البيع: "+ex.getMessage());}}));
        list("SELECT customer||' | إجمالي: '||total||' | نقد: '||cash||' | بنكك: '||bank||' | دين: '||debt FROM sales ORDER BY id DESC");
    }
    private void expenses(){
        frame("المصروفات والتالف");
        EditText cat=field("بند المصروف: إيجار، كهرباء، وقود، صيانة، أجور...",false),amount=field("قيمة المصروف بالجنيه",false),note=field("البيان",false);
        body.addView(btn("حفظ المصروف",()->{String c=cat.getText().toString().trim();long a=money(amount);if(c.isEmpty()||a<=0){toast("أدخل بنداً وقيمة صحيحة");return;}db.exec("INSERT INTO expenses(date,category,amount,note) VALUES(?,?,?,?)",new String[]{today(),c,""+a,note.getText().toString()});toast("تم حفظ المصروف");expenses();}));
        body.addView(label("التالف من المنتج النهائي يخصم من مخزون المنتج ويسجل خسارة مرة واحدة.",14));
        EditText pn=field("اسم المنتج التالف",false),wq=field("كمية التالف",true),reason=field("سبب التلف",false);
        body.addView(btn("تسجيل التالف",()->{long pid=db.id("products",pn.getText().toString().trim()),q=num(wq);if(pid==0||q<=0){toast("حدد المنتج والكمية");return;}try{db.waste(pid,q,reason.getText().toString());toast("تم تسجيل التالف وخصمه من المخزون");expenses();}catch(Exception ex){toast("تعذر تسجيل التالف: "+ex.getMessage());}}));
        list("SELECT category||' | '||amount||' جنيه | '||note FROM expenses ORDER BY id DESC");
    }
    private void costing(){
        frame("حساب التشغيل والتكلفة");
        list("SELECT p.name||' | رصيد المنتج: '||printf('%.3f',f.qty/1000.0)||' | تكلفة الوحدة بالجنيه: '||printf('%.2f',f.unit_cost/1000.0) FROM finished f JOIN products p ON p.id=f.product_id");
        list("SELECT m.name||' | رصيد الخام: '||printf('%.3f',m.qty/1000.0)||' '||m.unit||' | قيمة المخزون: '||m.value FROM materials m ORDER BY m.name");
        list("SELECT 'إجمالي المصروفات: '||COALESCE(SUM(amount),0)||' جنيه' FROM expenses");
    }
    private void accounts(){
        frame("الحسابات والأرصدة");
        list("SELECT 'إجمالي المبيعات: '||COALESCE(SUM(total),0)||' جنيه' FROM sales");
        list("SELECT 'النقد المحصل: '||COALESCE(SUM(cash),0)||' جنيه' FROM sales");
        list("SELECT 'تحويلات بنكك: '||COALESCE(SUM(bank),0)||' جنيه' FROM sales");
        list("SELECT 'ديون العملاء: '||COALESCE(SUM(debt),0)||' جنيه' FROM sales");
        list("SELECT 'المصروفات: '||COALESCE(SUM(amount),0)||' جنيه' FROM expenses");
        list("SELECT 'تكلفة البضاعة المباعة: '||COALESCE(SUM(cogs),0)||' جنيه' FROM sales");
        list("SELECT 'صافي الربح التشغيلي قبل المصروفات العامة: '||(COALESCE((SELECT SUM(total-cogs) FROM sales),0)-COALESCE((SELECT SUM(amount) FROM expenses),0))||' جنيه'");
        list("SELECT customer||' | دين متبقٍ: '||debt FROM sales WHERE debt>0 ORDER BY id DESC");
    }
    private void list(String sql){Cursor c=null;try{c=db.getReadableDatabase().rawQuery(sql,null);while(c.moveToNext())body.addView(label("• "+c.getString(0),14));}catch(Exception e){body.addView(label("لا توجد سجلات بعد.",13));}finally{if(c!=null)c.close();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    private static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"almizan_factory_1.db",null,1);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY,name TEXT UNIQUE)");
            d.execSQL("CREATE TABLE recipe(id INTEGER PRIMARY KEY,product_id INTEGER,material_id INTEGER,qty INTEGER CHECK(qty>0),UNIQUE(product_id,material_id))");
            d.execSQL("CREATE TABLE batches(id INTEGER PRIMARY KEY,product_id INTEGER,batches INTEGER,output INTEGER,labor INTEGER,overhead INTEGER,cost INTEGER,unit_cost INTEGER,status TEXT,date TEXT)");
            d.execSQL("CREATE TABLE finished(product_id INTEGER PRIMARY KEY,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0,unit_cost INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY,product_id INTEGER,customer TEXT,qty INTEGER,total INTEGER,cash INTEGER,bank INTEGER,bank_ref TEXT,debt INTEGER,cogs INTEGER,date TEXT)");
            d.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY,date TEXT,category TEXT,amount INTEGER,note TEXT)");
            d.execSQL("CREATE TABLE waste(id INTEGER PRIMARY KEY,product_id INTEGER,qty INTEGER,cost INTEGER,reason TEXT,date TEXT)");
            d.execSQL("CREATE TABLE movements(id INTEGER PRIMARY KEY,kind TEXT,item_id INTEGER,qty INTEGER,value INTEGER,ref TEXT,date TEXT)");
            d.execSQL("CREATE TABLE audit(id INTEGER PRIMARY KEY,action TEXT,detail TEXT,date TEXT)");
        }
        public void onUpgrade(SQLiteDatabase d,int old,int now){throw new IllegalStateException("Factory database migration required");}
        SQLiteDatabase w(){return getWritableDatabase();}
        void exec(String sql,String[] args){w().execSQL(sql,args);}
        long id(String table,String name){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM "+table+" WHERE name=?",new String[]{name});try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}}
        void receive(String name,String unit,long qty,long cost){
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor c=d.rawQuery("SELECT id,qty,value,unit FROM materials WHERE name=?",new String[]{name});
                long id=0,oldq=0,oldv=0;String oldunit=null;if(c.moveToFirst()){id=c.getLong(0);oldq=c.getLong(1);oldv=c.getLong(2);oldunit=c.getString(3);}c.close();
                if(oldunit!=null&&!oldunit.equals(unit))throw new IllegalArgumentException("وحدة المادة لا تطابق الرصيد السابق");
                long nq=Math.addExact(oldq,qty),nv=Math.addExact(oldv,cost);
                if(id==0){ContentValues v=new ContentValues();v.put("name",name);v.put("unit",unit);v.put("qty",nq);v.put("value",nv);d.insertOrThrow("materials",null,v);}else{ContentValues v=new ContentValues();v.put("qty",nq);v.put("value",nv);d.update("materials",v,"id=?",new String[]{""+id});}
                d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('RAW_IN',?,?,?,'purchase',?)",new Object[]{id==0?this.id("materials",name):id,qty,cost,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
        }
        void finalizeProduction(long pid,long batches,long output,long labor,long overhead){
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor exists=d.rawQuery("SELECT COUNT(*) FROM recipe WHERE product_id=?",new String[]{""+pid});exists.moveToFirst();if(exists.getInt(0)==0){exists.close();throw new IllegalArgumentException("الوصفة غير مسجلة");}exists.close();
                Cursor r=d.rawQuery("SELECT m.id,m.qty,m.value,m.qty,rc.qty FROM recipe rc JOIN materials m ON m.id=rc.material_id WHERE rc.product_id=?",new String[]{""+pid});
                long materialsCost=0;ArrayList<long[]> lines=new ArrayList<>();
                while(r.moveToNext()){long mid=r.getLong(0),stock=r.getLong(1),value=r.getLong(2),per=r.getLong(4),need=Math.multiplyExact(per,batches);if(stock<need)throw new IllegalArgumentException("رصيد خام غير كافٍ للمادة رقم "+mid);long cost=stock==0?0:java.math.BigDecimal.valueOf(value).multiply(java.math.BigDecimal.valueOf(need)).divide(java.math.BigDecimal.valueOf(stock),0,java.math.RoundingMode.HALF_UP).longValueExact();materialsCost=Math.addExact(materialsCost,cost);lines.add(new long[]{mid,stock-need,value-cost,need,cost});}r.close();
                long total=Math.addExact(Math.addExact(materialsCost,labor),overhead),unit=java.math.BigDecimal.valueOf(total).multiply(java.math.BigDecimal.valueOf(1000)).divide(java.math.BigDecimal.valueOf(output),0,java.math.RoundingMode.HALF_UP).longValueExact();
                for(long[] x:lines){ContentValues v=new ContentValues();v.put("qty",x[1]);v.put("value",x[2]);d.update("materials",v,"id=?",new String[]{""+x[0]});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('RAW_OUT',?,?,?,'production',date('now'))",new Object[]{x[0],x[3],x[4]});}
                Cursor f=d.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});long oldq=0,oldv=0;if(f.moveToFirst()){oldq=f.getLong(0);oldv=f.getLong(1);}f.close();long newq=Math.addExact(oldq,output),newv=Math.addExact(oldv,total),avg=java.math.BigDecimal.valueOf(newv).multiply(java.math.BigDecimal.valueOf(1000)).divide(java.math.BigDecimal.valueOf(newq),0,java.math.RoundingMode.HALF_UP).longValueExact();
                ContentValues fv=new ContentValues();fv.put("product_id",pid);fv.put("qty",newq);fv.put("value",newv);fv.put("unit_cost",avg);d.insertWithOnConflict("finished",null,fv,SQLiteDatabase.CONFLICT_REPLACE);
                d.execSQL("INSERT INTO batches(product_id,batches,output,labor,overhead,cost,unit_cost,status,date) VALUES(?,?,?,?,?,?,?,'FINALIZED',?)",new Object[]{pid,batches,output,labor,overhead,total,unit,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});
                d.execSQL("INSERT INTO audit(action,detail,date) VALUES('PRODUCTION_FINALIZED',?,date('now'))",new Object[]{"product="+pid+",cost="+total});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
        }
        void sell(long pid,long qty,long total,long cash,long bank,String customer,String ref){
            SQLiteDatabase d=w();d.beginTransaction();try{Cursor c=d.rawQuery("SELECT qty,value,unit_cost FROM finished WHERE product_id=?",new String[]{""+pid});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("لا يوجد مخزون منتج");}long stock=c.getLong(0),value=c.getLong(1),unit=c.getLong(2);c.close();if(stock<qty)throw new IllegalArgumentException("الكمية المتاحة أقل من المطلوب");long cogs=java.math.BigDecimal.valueOf(value).multiply(java.math.BigDecimal.valueOf(qty)).divide(java.math.BigDecimal.valueOf(stock),0,java.math.RoundingMode.HALF_UP).longValueExact();ContentValues f=new ContentValues();f.put("qty",stock-qty);f.put("value",value-cogs);f.put("unit_cost",stock==qty?0:java.math.BigDecimal.valueOf(value-cogs).multiply(java.math.BigDecimal.valueOf(1000)).divide(java.math.BigDecimal.valueOf(stock-qty),0,java.math.RoundingMode.HALF_UP).longValueExact());d.update("finished",f,"product_id=?",new String[]{""+pid});d.execSQL("INSERT INTO sales(product_id,customer,qty,total,cash,bank,bank_ref,debt,cogs,date) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{pid,customer,qty,total,cash,bank,ref,total-cash-bank,cogs,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('SALE',?,?,?,'sale',date('now'))",new Object[]{pid,qty,cogs});d.setTransactionSuccessful();}finally{d.endTransaction();}
        }
        void waste(long pid,long qty,String reason){
            SQLiteDatabase d=w();d.beginTransaction();try{Cursor c=d.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("لا يوجد رصيد");}long stock=c.getLong(0),value=c.getLong(1);c.close();if(qty>stock)throw new IllegalArgumentException("التالف أكبر من الرصيد");long cost=java.math.BigDecimal.valueOf(value).multiply(java.math.BigDecimal.valueOf(qty)).divide(java.math.BigDecimal.valueOf(stock),0,java.math.RoundingMode.HALF_UP).longValueExact();ContentValues f=new ContentValues();f.put("qty",stock-qty);f.put("value",value-cost);f.put("unit_cost",stock==qty?0:java.math.BigDecimal.valueOf(value-cost).multiply(java.math.BigDecimal.valueOf(1000)).divide(java.math.BigDecimal.valueOf(stock-qty),0,java.math.RoundingMode.HALF_UP).longValueExact());d.update("finished",f,"product_id=?",new String[]{""+pid});d.execSQL("INSERT INTO waste(product_id,qty,cost,reason,date) VALUES(?,?,?,?,date('now'))",new Object[]{pid,qty,cost,reason});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('WASTE',?,?,?,'waste',date('now'))",new Object[]{pid,qty,cost});d.setTransactionSuccessful();}finally{d.endTransaction();}
        }
    }
}