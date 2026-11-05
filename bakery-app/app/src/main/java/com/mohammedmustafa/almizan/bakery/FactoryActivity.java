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
    private static final String[][] DEFAULT_MATERIALS={{"دقيق","كجم"},{"سكر","كجم"},{"زيت","لتر"},{"سمن","كجم"},{"خميرة","كجم"},{"حليب","لتر"},{"بيض","طبق"},{"شوكولاتة","كجم"},{"مكسرات","كجم"},{"فانيليا","كجم"},{"ملح","كجم"},{"أكياس وعبوات","قطعة"},{"غاز","أسطوانة"},{"وقود","لتر"}};
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    @Override public void onCreate(Bundle b){super.onCreate(b); db=new DB(this); db.seedMaterials(DEFAULT_MATERIALS); home();}
    @Override public void onBackPressed(){if("home".equals(screen))super.onBackPressed();else home();}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(30,40,50));t.setPadding(4,10,4,10);return t;}
    private Button btn(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
    private void frame(String title){
        screen="الرئيسية".equals(title)?"home":title; LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(14,12,14,16);root.setBackgroundColor(Color.rgb(246,248,251));
        TextView head=label("🏭 مصنع الخبائز والحلويات والباسطة\n"+title,21);head.setTextColor(NAVY);head.setBackgroundColor(Color.WHITE);root.addView(head);
        root.addView(btn("⌂ العودة إلى مصنع الخبائز والحلويات والباسطة",()->home()));
        root.addView(btn("↩ العودة إلى قائمة المخبز الرئيسية",()->finish()));
        ScrollView sc=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(2,8,2,8);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private void home(){
        frame("الرئيسية");
        body.addView(label("نظام مستقل عن دفاتر المخبز. كل المخزون والحركات والتكاليف هنا محفوظة في قاعدة بيانات منفصلة.",15));
        body.addView(btn("1. مخزن المواد الخام ومستلزمات المصنع",()->warehouse()));
        body.addView(btn("2. المنتجات والوصفات",()->recipes()));
        body.addView(btn("3. الإنتاج والتشغيل اليومي",()->production()));
        body.addView(btn("4. نقطة البيع والشباك والمبيعات والتحصيل",()->pointOfSale()));
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
        body.addView(btn("حفظ وارد مخزني",()->{String name=n.getText().toString().trim(),unit=u.getText().toString().trim();long qty=num(q),cost=money(p);if(name.isEmpty()||unit.isEmpty()||qty<=0||cost<0){toast("أكمل الاسم والوحدة والكمية والتكلفة بصورة صحيحة");return;}try{db.receive(name,unit,qty,cost);toast("تم تسجيل الوارد");warehouse();}catch(Exception ex){toast("تعذر تسجيل الوارد: "+ex.getMessage());}}));
        body.addView(label("تعديل اسم المادة أو وحدة القياس. لا يسمح بتغيير الوحدة مع وجود رصيد حتى لا تختلط وحدات القياس.",14));
        EditText oldName=field("اسم المادة الحالي",false),newName=field("الاسم الجديد",false),newUnit=field("الوحدة الجديدة",false);
        body.addView(btn("تعديل بيانات المادة",()->{String old=oldName.getText().toString().trim(),nn=newName.getText().toString().trim(),nu=newUnit.getText().toString().trim();if(old.isEmpty()||nn.isEmpty()||nu.isEmpty()){toast("أدخل الاسم الحالي والاسم والوحدة الجديدة");return;}try{db.renameMaterial(old,nn,nu);toast("تم تعديل بيانات المادة");warehouse();}catch(Exception ex){toast("تعذر تعديل المادة: "+ex.getMessage());}}));
        list("SELECT name||' — '||unit||' | الرصيد: '||printf('%.3f',qty/1000.0)||' | قيمة الرصيد: '||value FROM materials ORDER BY name");
    }
    private void recipes(){
        frame("المنتجات والوصفات");
        body.addView(label("أنشئ المنتج أولاً ثم أضف مكوناته. كمية المكون هي الكمية اللازمة لإنتاج دفعة واحدة.",14));
        EditText n=field("اسم المنتج: خبائز، مخبازات، فطائر، باستا، حلويات...",false);
        body.addView(btn("إضافة منتج",()->{String x=n.getText().toString().trim();if(x.isEmpty()){toast("أدخل اسم المنتج");return;}db.exec("INSERT OR IGNORE INTO products(name) VALUES(?)",new String[]{x});toast("تم حفظ المنتج");recipes();}));
        EditText product=field("اسم المنتج المطابق",false),material=field("اسم المادة المطابق",false),qty=field("كمية المادة لكل دفعة",true);
        body.addView(btn("إضافة مكون للوصفة",()->{String pn=product.getText().toString().trim(),mn=material.getText().toString().trim();long q=num(qty);if(q<=0){toast("الكمية يجب أن تكون أكبر من صفر");return;}long pid=db.id("products",pn),mid=db.id("materials",mn);if(pid==0||mid==0){toast("تأكد من تسجيل المنتج والمادة في النظام");return;}try{db.exec("INSERT INTO recipe(product_id,material_id,qty) VALUES(?,?,?)",new String[]{""+pid,""+mid,""+q});toast("تم ربط المكون بالوصفة");recipes();}catch(Exception ex){toast("هذا المكون مسجل بالفعل أو تعذر حفظه: "+ex.getMessage());}}));
        list("SELECT name FROM products ORDER BY name");
    }
    private void production(){
        frame("الإنتاج والتشغيل اليومي");
        body.addView(label("ترحيل الإنتاج النهائي ينفذ داخل معاملة واحدة، ويمنع خصم المواد أكثر من مرة عند إعادة المحاولة.",14));
        EditText product=field("اسم المنتج",false),batchRef=field("مرجع الدفعة (رقم فريد)",false),batches=field("عدد الدفعات",false),output=field("الكمية السليمة المنتجة",true),labor=field("أجور العمال لهذه الدفعة بالجنيه",false),over=field("تكاليف تشغيل مباشرة أخرى بالجنيه",false);
        body.addView(btn("ترحيل دفعة الإنتاج نهائياً",()->{String pn=product.getText().toString().trim(),br=batchRef.getText().toString().trim();long pid=db.id("products",pn),b=money(batches),out=num(output),w=money(labor),oh=money(over);if(pid==0||br.isEmpty()||b<=0||out<=0||w<0||oh<0){toast("أدخل المنتج ومرجع دفعة فريداً والكميات والتكاليف");return;}try{db.finalizeProduction(pid,br,b,out,w,oh);toast("تم ترحيل الإنتاج وخصم المواد وإضافة المنتج للمخزون");production();}catch(Exception ex){toast("لم يرحل الإنتاج: "+ex.getMessage());}}));
        list("SELECT p.name||' | الكمية المنتجة: '||printf('%.3f',f.qty/1000.0)||' | تكلفة الوحدة بالجنيه: '||printf('%.2f',f.unit_cost/1000.0) FROM finished f JOIN products p ON p.id=f.product_id");
    }
    // Backward-compatible aliases: all former sales entry points open one unified POS ledger.
    private void pointOfSale(){sales("نقطة البيع");}
    private void sales(){pointOfSale();}
    private void cashWindow(){pointOfSale();}
    private void sales(String channel){
        final String unifiedChannel="نقطة البيع";
        frame("نقطة البيع والشباك والمبيعات والتحصيل");
        body.addView(label("شاشة موحدة لكل عمليات البيع سواء كانت من نقطة البيع أو الشباك أو التوزيع. سجل واحد للمبيعات والتحصيل حتى لا تتكرر الأرصدة أو تختلف التقارير.",14));
        EditText customer=field("اسم العميل أو الشباك",false),product=field("اسم المنتج",false),qty=field("الكمية المباعة",true),price=field("سعر البيع الإجمالي بالجنيه",false),cash=field("المبلغ النقدي",false),bank=field("تحويل بنكك",false),ref=field("رقم عملية بنكك (إلزامي عند التحويل)",false);
        body.addView(btn("تسجيل البيع والتحصيل",()->{long pid=db.id("products",product.getText().toString().trim()),q=num(qty),v=money(price),c=money(cash),bk=money(bank);String cust=customer.getText().toString().trim();if(pid==0||q<=0||v<=0||c<0||bk<0||c>v-bk||cust.isEmpty()||(bk>0&&ref.getText().toString().trim().isEmpty())){toast("بيانات البيع غير صحيحة، ورقم بنكك مطلوب عند التحويل");return;}try{db.sell(pid,q,v,c,bk,cust,ref.getText().toString().trim(),unifiedChannel);toast("تم تسجيل البيع والرصيد المستحق");pointOfSale();}catch(Exception ex){toast("تعذر البيع: "+ex.getMessage());}}));
        body.addView(label("تحصيل مديونية عميل سابقة",15));
        EditText debtor=field("اسم العميل",false),colCash=field("مبلغ نقدي مستلم",false),colBank=field("مبلغ مستلم عبر بنكك",false),colRef=field("رقم عملية بنكك عند التحويل",false);
        body.addView(btn("تسجيل تحصيل من رصيد العميل",()->{String dn=debtor.getText().toString().trim(),br=colRef.getText().toString().trim();long cc=money(colCash),cb=money(colBank);if(dn.isEmpty()||cc<0||cb<0||cc+cb<=0||(cb>0&&br.isEmpty())){toast("أدخل العميل ومبلغ التحصيل ورقم بنكك عند التحويل");return;}try{db.collect(dn,cc,cb,br);toast("تم تسجيل التحصيل وتخفيض مديونية العميل");pointOfSale();}catch(Exception ex){toast("تعذر التحصيل: "+ex.getMessage());}}));
        list("SELECT channel||' | '||customer||' | إجمالي: '||total||' | نقد: '||cash||' | بنكك: '||bank||' | دين عند البيع: '||debt FROM sales ORDER BY id DESC");
        list("SELECT customer||' | تحصيل نقدي: '||cash||' | تحصيل بنكك: '||bank FROM collections ORDER BY id DESC");
    }
    private void expenses(){
        frame("المصروفات والتالف");
        EditText cat=field("بند المصروف: إيجار، كهرباء، وقود، صيانة، أجور...",false),amount=field("قيمة المصروف بالجنيه",false),note=field("البيان",false);
        EditText payMode=field("طريقة السداد: نقد أو بنكك",false),payRef=field("رقم عملية بنكك عند التحويل",false);
        body.addView(btn("حفظ المصروف",()->{String c=cat.getText().toString().trim(),mode=payMode.getText().toString().trim(),pr=payRef.getText().toString().trim();long a=money(amount);if(c.isEmpty()||a<=0||(!mode.equals("نقد")&&!mode.equals("بنكك"))||(mode.equals("بنكك")&&pr.isEmpty())){toast("أدخل بنداً وقيمة وطريقة سداد صحيحة");return;}try{db.addExpense(c,a,note.getText().toString(),mode.equals("بنكك"),pr);toast("تم حفظ المصروف");expenses();}catch(Exception ex){toast("تعذر حفظ المصروف: "+ex.getMessage());}}));
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
        list("SELECT 'صافي رصيد الصندوق النقدي: '||(COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='CASH' AND kind IN ('SALE','COLLECTION')),0)-COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='CASH' AND kind='EXPENSE'),0))||' جنيه'");
        list("SELECT 'صافي رصيد بنكك: '||(COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='BANKAK' AND kind IN ('SALE','COLLECTION')),0)-COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='BANKAK' AND kind='EXPENSE'),0))||' جنيه'");
        list("SELECT 'النقد المحصل من المبيعات: '||COALESCE(SUM(cash),0)||' جنيه' FROM sales");
        list("SELECT 'تحويلات بنكك: '||COALESCE(SUM(bank),0)||' جنيه' FROM sales");
        list("SELECT 'ديون العملاء المتبقية: '||(COALESCE((SELECT SUM(debt) FROM sales),0)-COALESCE((SELECT SUM(cash+bank) FROM collections),0))||' جنيه'");
        list("SELECT 'تحصيلات العملاء: '||COALESCE(SUM(cash+bank),0)||' جنيه' FROM collections");
        list("SELECT 'المصروفات: '||COALESCE(SUM(amount),0)||' جنيه' FROM expenses");
        list("SELECT 'تكلفة البضاعة المباعة: '||COALESCE(SUM(cogs),0)||' جنيه' FROM sales");
        list("SELECT 'خسائر التالف: '||COALESCE(SUM(cost),0)||' جنيه' FROM waste");
        list("SELECT 'صافي نتيجة التشغيل: '||(COALESCE((SELECT SUM(total-cogs) FROM sales),0)-COALESCE((SELECT SUM(cost) FROM waste),0)-COALESCE((SELECT SUM(amount) FROM expenses),0))||' جنيه'");
        list("SELECT s.customer||' | دين متبقٍ: '||(SUM(s.debt)-COALESCE((SELECT SUM(c.cash+c.bank) FROM collections c WHERE c.customer=s.customer),0)) FROM sales s GROUP BY s.customer HAVING SUM(s.debt)>COALESCE((SELECT SUM(c.cash+c.bank) FROM collections c WHERE c.customer=s.customer),0) ORDER BY s.customer");
    }
    private void list(String sql){Cursor c=null;try{c=db.getReadableDatabase().rawQuery(sql,null);if(!c.moveToFirst()){body.addView(label("لا توجد سجلات بعد.",13));return;}do{TextView row=label("│ "+c.getString(0),14);row.setBackgroundColor(Color.WHITE);body.addView(row);View line=new View(this);line.setBackgroundColor(GOLD);body.addView(line,new LinearLayout.LayoutParams(-1,1));}while(c.moveToNext());}catch(Exception e){body.addView(label("تعذر قراءة السجلات: "+e.getMessage(),13));}finally{if(c!=null)c.close();}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}

    private static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"almizan_factory_1.db",null,3);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY,name TEXT UNIQUE)");
            d.execSQL("CREATE TABLE recipe(id INTEGER PRIMARY KEY,product_id INTEGER,material_id INTEGER,qty INTEGER CHECK(qty>0),UNIQUE(product_id,material_id))");
            d.execSQL("CREATE TABLE batches(id INTEGER PRIMARY KEY,batch_ref TEXT UNIQUE,product_id INTEGER,batches INTEGER,output INTEGER,labor INTEGER,overhead INTEGER,cost INTEGER,unit_cost INTEGER,status TEXT,date TEXT)");
            d.execSQL("CREATE TABLE finished(product_id INTEGER PRIMARY KEY,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0,unit_cost INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY,product_id INTEGER,customer TEXT,channel TEXT,qty INTEGER,total INTEGER,cash INTEGER,bank INTEGER,bank_ref TEXT,debt INTEGER,cogs INTEGER,date TEXT)");
            d.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY,date TEXT,category TEXT,amount INTEGER,note TEXT)");
            d.execSQL("CREATE TABLE waste(id INTEGER PRIMARY KEY,product_id INTEGER,qty INTEGER,cost INTEGER,reason TEXT,date TEXT)");
            d.execSQL("CREATE TABLE movements(id INTEGER PRIMARY KEY,kind TEXT,item_id INTEGER,qty INTEGER,value INTEGER,ref TEXT,date TEXT)");
            d.execSQL("CREATE TABLE collections(id INTEGER PRIMARY KEY,customer TEXT,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT,date TEXT)");
            d.execSQL("CREATE TABLE cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER NOT NULL,ref TEXT,date TEXT)");
            d.execSQL("CREATE TABLE audit(id INTEGER PRIMARY KEY,action TEXT,detail TEXT,date TEXT)");
        }
        public void onUpgrade(SQLiteDatabase d,int old,int now){if(old<2){d.execSQL("ALTER TABLE batches ADD COLUMN batch_ref TEXT");d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_factory_batch_ref ON batches(batch_ref)");d.execSQL("CREATE TABLE IF NOT EXISTS collections(id INTEGER PRIMARY KEY,customer TEXT,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER NOT NULL,ref TEXT,date TEXT)");}if(old<3)d.execSQL("ALTER TABLE sales ADD COLUMN channel TEXT NOT NULL DEFAULT 'التوزيع'");}
        SQLiteDatabase w(){return getWritableDatabase();}
        void seedMaterials(String[][] items){SQLiteDatabase d=w();d.beginTransaction();try{for(String[] item:items)d.execSQL("INSERT OR IGNORE INTO materials(name,unit,qty,value) VALUES(?,?,0,0)",new Object[]{item[0],item[1]});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void exec(String sql,String[] args){w().execSQL(sql,args);}
        long id(String table,String name){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM "+table+" WHERE name=?",new String[]{name});try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}}
        void renameMaterial(String oldName,String newName,String newUnit){
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor c=d.rawQuery("SELECT qty,unit FROM materials WHERE name=?",new String[]{oldName});
                if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("المادة غير موجودة");}
                long qty=c.getLong(0);String oldUnit=c.getString(1);c.close();
                if(qty>0&&!oldUnit.equals(newUnit))throw new IllegalArgumentException("لا يمكن تغيير الوحدة مع وجود رصيد؛ صفّر الرصيد أولاً");
                ContentValues v=new ContentValues();v.put("name",newName);v.put("unit",newUnit);
                if(d.update("materials",v,"name=?",new String[]{oldName})!=1)throw new IllegalArgumentException("لم يتم تعديل المادة");
                d.execSQL("INSERT INTO audit(action,detail,date) VALUES('MATERIAL_EDIT',?,date('now'))",new Object[]{oldName+" -> "+newName+" ["+newUnit+"]"});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
        }
        void receive(String name,String unit,long qty,long cost){
            if(qty<=0||cost<0)throw new IllegalArgumentException("كمية أو تكلفة الوارد غير صحيحة");
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor c=d.rawQuery("SELECT id,qty,value,unit FROM materials WHERE name=?",new String[]{name});
                long id=0,oldq=0,oldv=0;String oldunit=null;if(c.moveToFirst()){id=c.getLong(0);oldq=c.getLong(1);oldv=c.getLong(2);oldunit=c.getString(3);}c.close();
                if(oldunit!=null&&!oldunit.equals(unit))throw new IllegalArgumentException("وحدة المادة لا تطابق الرصيد السابق");
                long nq=FactoryMath.add(oldq,qty),nv=FactoryMath.add(oldv,cost);
                if(id==0){ContentValues v=new ContentValues();v.put("name",name);v.put("unit",unit);v.put("qty",nq);v.put("value",nv);d.insertOrThrow("materials",null,v);}else{ContentValues v=new ContentValues();v.put("qty",nq);v.put("value",nv);d.update("materials",v,"id=?",new String[]{""+id});}
                long materialId=id==0?this.id("materials",name):id;
                d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('RAW_IN',?,?,?,'purchase',?)",new Object[]{materialId,qty,cost,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});
                d.execSQL("INSERT INTO audit(action,detail,date) VALUES('RAW_RECEIVED',?,date('now'))",new Object[]{name+":"+qty+":"+cost});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
        }
        void finalizeProduction(long pid,String batchRef,long batches,long output,long labor,long overhead){
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor duplicate=d.rawQuery("SELECT COUNT(*) FROM batches WHERE batch_ref=?",new String[]{batchRef});duplicate.moveToFirst();boolean already=duplicate.getInt(0)>0;duplicate.close();if(already)throw new IllegalArgumentException("مرجع الدفعة مستخدم من قبل؛ منعنا ترحيلها مرة أخرى");
                Cursor exists=d.rawQuery("SELECT COUNT(*) FROM recipe WHERE product_id=?",new String[]{""+pid});exists.moveToFirst();if(exists.getInt(0)==0){exists.close();throw new IllegalArgumentException("الوصفة غير مسجلة");}exists.close();
                Cursor r=d.rawQuery("SELECT m.id,m.qty,m.value,m.qty,rc.qty FROM recipe rc JOIN materials m ON m.id=rc.material_id WHERE rc.product_id=?",new String[]{""+pid});
                long materialsCost=0;ArrayList<long[]> lines=new ArrayList<>();
                while(r.moveToNext()){long mid=r.getLong(0),stock=r.getLong(1),value=r.getLong(2),per=r.getLong(4),need=FactoryMath.multiply(per,batches);if(stock<need)throw new IllegalArgumentException("رصيد خام غير كافٍ للمادة رقم "+mid);long cost=FactoryMath.costForStockIssue(value,need,stock);materialsCost=FactoryMath.add(materialsCost,cost);lines.add(new long[]{mid,stock-need,value-cost,need,cost});}r.close();
                long total=FactoryMath.add(FactoryMath.add(materialsCost,labor),overhead),unit=java.math.BigDecimal.valueOf(total).multiply(java.math.BigDecimal.valueOf(1000000)).divide(java.math.BigDecimal.valueOf(output),0,java.math.RoundingMode.HALF_UP).longValueExact();
                for(long[] x:lines){ContentValues v=new ContentValues();v.put("qty",x[1]);v.put("value",x[2]);d.update("materials",v,"id=?",new String[]{""+x[0]});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('RAW_OUT',?,?,?,'production',date('now'))",new Object[]{x[0],x[3],x[4]});}
                Cursor f=d.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});long oldq=0,oldv=0;if(f.moveToFirst()){oldq=f.getLong(0);oldv=f.getLong(1);}f.close();long newq=FactoryMath.add(oldq,output),newv=FactoryMath.add(oldv,total),avg=FactoryMath.weightedAverageUnitCost(oldq,oldv,output,total);
                ContentValues fv=new ContentValues();fv.put("product_id",pid);fv.put("qty",newq);fv.put("value",newv);fv.put("unit_cost",avg);d.insertWithOnConflict("finished",null,fv,SQLiteDatabase.CONFLICT_REPLACE);
                d.execSQL("INSERT INTO batches(batch_ref,product_id,batches,output,labor,overhead,cost,unit_cost,status,date) VALUES(?,?,?,?,?,?,?,?,'FINALIZED',?)",new Object[]{batchRef,pid,batches,output,labor,overhead,total,unit,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});
                d.execSQL("INSERT INTO audit(action,detail,date) VALUES('PRODUCTION_FINALIZED',?,date('now'))",new Object[]{"product="+pid+",cost="+total});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
        }
        void ensureUniqueBankakRef(SQLiteDatabase d,String ref){
            if(ref==null||ref.trim().isEmpty())throw new IllegalArgumentException("رقم عملية بنكك مطلوب");
            Cursor c=d.rawQuery("SELECT COUNT(*) FROM cashbook WHERE account='BANKAK' AND ref=?",new String[]{ref.trim()});
            try{c.moveToFirst();if(c.getLong(0)>0)throw new IllegalArgumentException("رقم عملية بنكك مسجل مسبقاً؛ راجع دفتر الحركات");}finally{c.close();}
        }
        void sell(long pid,long qty,long total,long cash,long bank,String customer,String ref,String channel){
            SQLiteDatabase d=w();d.beginTransaction();try{
            if(customer==null||customer.trim().isEmpty()||qty<=0||total<=0||cash<0||bank<0||cash>total||bank>total-cash)throw new IllegalArgumentException("بيانات البيع أو التحصيل غير صحيحة");
            FactoryMath.validateBankakReference(bank,ref);
            if(bank>0)ensureUniqueBankakRef(d,ref);
            Cursor c=d.rawQuery("SELECT qty,value,unit_cost FROM finished WHERE product_id=?",new String[]{""+pid});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("لا يوجد مخزون منتج");}long stock=c.getLong(0),value=c.getLong(1),unit=c.getLong(2);c.close();if(stock<qty)throw new IllegalArgumentException("الكمية المتاحة أقل من المطلوب");if(qty<=0||total<=0||cash<0||bank<0||cash>total||bank>total-cash)throw new IllegalArgumentException("قيمة البيع أو التحصيل غير صحيحة");long cogs=FactoryMath.costForStockIssue(value,qty,stock);ContentValues f=new ContentValues();f.put("qty",stock-qty);f.put("value",value-cogs);f.put("unit_cost",stock==qty?0:java.math.BigDecimal.valueOf(value-cogs).multiply(java.math.BigDecimal.valueOf(1000000)).divide(java.math.BigDecimal.valueOf(stock-qty),0,java.math.RoundingMode.HALF_UP).longValueExact());d.update("finished",f,"product_id=?",new String[]{""+pid});d.execSQL("INSERT INTO sales(product_id,customer,channel,qty,total,cash,bank,bank_ref,debt,cogs,date) VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{pid,customer,channel,qty,total,cash,bank,ref,total-cash-bank,cogs,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});
            String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());if(cash>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','SALE',?,?,?)",new Object[]{cash,"sale",date});if(bank>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','SALE',?,?,?)",new Object[]{bank,ref,date});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('SALE',?,?,?,'sale',date('now'))",new Object[]{pid,qty,cogs});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('SALE_POSTED',?,date('now'))",new Object[]{customer+":"+pid+":"+qty+":"+total});d.setTransactionSuccessful();}finally{d.endTransaction();}
        }
        void collect(String customer,long cash,long bank,String ref){if(customer==null||customer.trim().isEmpty()||cash<0||bank<0||FactoryMath.add(cash,bank)<=0||(bank>0&&(ref==null||ref.trim().isEmpty())))throw new IllegalArgumentException("بيانات التحصيل غير صحيحة");SQLiteDatabase d=w();d.beginTransaction();try{FactoryMath.validateBankakReference(bank,ref);if(bank>0)ensureUniqueBankakRef(d,ref);Cursor c=d.rawQuery("SELECT COALESCE(SUM(debt),0) FROM sales WHERE customer=?",new String[]{customer});c.moveToFirst();long debt=c.getLong(0);c.close();Cursor p=d.rawQuery("SELECT COALESCE(SUM(cash+bank),0) FROM collections WHERE customer=?",new String[]{customer});p.moveToFirst();long previous=p.getLong(0);p.close();long remaining=Math.max(0,debt-previous);if(FactoryMath.add(cash,bank)>remaining)throw new IllegalArgumentException("التحصيل أكبر من المديونية المتبقية: "+remaining);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.execSQL("INSERT INTO collections(customer,cash,bank,bank_ref,date) VALUES(?,?,?,?,?)",new Object[]{customer,cash,bank,ref,date});if(cash>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','COLLECTION',?,?,?)",new Object[]{cash,customer,date});if(bank>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','COLLECTION',?,?,?)",new Object[]{bank,ref,date});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('CUSTOMER_COLLECTION',?,date('now'))",new Object[]{customer+":"+cash+":"+bank});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void addExpense(String category,long amount,String note,boolean bank,String ref){if(category==null||category.trim().isEmpty()||amount<=0||(bank&&(ref==null||ref.trim().isEmpty())))throw new IllegalArgumentException("بيانات المصروف غير صحيحة");SQLiteDatabase d=w();d.beginTransaction();try{FactoryMath.validateBankakReference(bank?amount:0,ref);if(bank)ensureUniqueBankakRef(d,ref);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.execSQL("INSERT INTO expenses(date,category,amount,note) VALUES(?,?,?,?)",new Object[]{date,category,amount,note});d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES(?,'EXPENSE',?,?,?)",new Object[]{bank?"BANKAK":"CASH",amount,bank?ref:category,date});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('EXPENSE',?,date('now'))",new Object[]{category+":"+amount});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void waste(long pid,long qty,String reason){
            if(qty<=0)throw new IllegalArgumentException("كمية التالف يجب أن تكون موجبة");
            SQLiteDatabase d=w();d.beginTransaction();try{Cursor c=d.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("لا يوجد رصيد");}long stock=c.getLong(0),value=c.getLong(1);c.close();if(qty>stock)throw new IllegalArgumentException("التالف أكبر من الرصيد");if(qty<=0)throw new IllegalArgumentException("كمية التالف يجب أن تكون موجبة");long cost=FactoryMath.costForStockIssue(value,qty,stock);ContentValues f=new ContentValues();f.put("qty",stock-qty);f.put("value",value-cost);f.put("unit_cost",stock==qty?0:java.math.BigDecimal.valueOf(value-cost).multiply(java.math.BigDecimal.valueOf(1000000)).divide(java.math.BigDecimal.valueOf(stock-qty),0,java.math.RoundingMode.HALF_UP).longValueExact());d.update("finished",f,"product_id=?",new String[]{""+pid});d.execSQL("INSERT INTO waste(product_id,qty,cost,reason,date) VALUES(?,?,?,?,date('now'))",new Object[]{pid,qty,cost,reason});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('WASTE',?,?,?,'waste',date('now'))",new Object[]{pid,qty,cost});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('WASTE_POSTED',?,date('now'))",new Object[]{pid+":"+qty+":"+cost+":"+reason});d.setTransactionSuccessful();}finally{d.endTransaction();}
        }
    }
}