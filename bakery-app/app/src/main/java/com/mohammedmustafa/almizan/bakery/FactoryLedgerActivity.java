package com.mohammedmustafa.almizan.bakery;

import android.app.Activity;
import android.os.Bundle;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FactoryLedgerActivity extends Activity {
    private SQLiteDatabase db;
    private LinearLayout body;
    private static final int GOLD=Color.rgb(212,175,55);
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    @Override public void onCreate(Bundle b){super.onCreate(b);db=openOrCreateDatabase("almizan_factory_1.db",MODE_PRIVATE,null);schema();home();}
    @Override protected void onDestroy(){if(db!=null&&db.isOpen())db.close();super.onDestroy();}
    private void schema(){
        db.execSQL("CREATE TABLE IF NOT EXISTS sales_returns(id INTEGER PRIMARY KEY,sale_id INTEGER NOT NULL,product_id INTEGER NOT NULL,qty INTEGER NOT NULL,refund_value INTEGER NOT NULL,refund_cash INTEGER NOT NULL,refund_bank INTEGER NOT NULL,credit_adjustment INTEGER NOT NULL,restored_cogs INTEGER NOT NULL,bank_ref TEXT,reason TEXT,date TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS work_orders(id INTEGER PRIMARY KEY,work_ref TEXT UNIQUE,product_id INTEGER NOT NULL,batches INTEGER NOT NULL,planned_qty INTEGER NOT NULL,actual_qty INTEGER NOT NULL DEFAULT 0,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL DEFAULT 0,overhead INTEGER NOT NULL DEFAULT 0,total_cost INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,date TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS work_order_materials(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,material_id INTEGER NOT NULL,qty INTEGER NOT NULL,cost INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
    }
    private void frame(String title){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(12,10,12,12);root.setBackgroundColor(Color.rgb(246,248,251));
        TextView h=label("دفتر المصنع — "+title,20);h.setTextColor(Color.rgb(11,31,58));root.addView(h);
        root.addView(button("العودة إلى مصنع الخبائز والحلويات",()->finish()));
        ScrollView scroll=new ScrollView(this);body=new LinearLayout(this);body.setOrientation(1);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(30,40,50));t.setPadding(5,9,5,9);return t;}
    private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
    private EditText field(String hint,boolean decimal){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setInputType(decimal?android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:android.text.InputType.TYPE_CLASS_TEXT);body.addView(e);return e;}
    private long qty(EditText e){try{return new java.math.BigDecimal(e.getText().toString().trim()).movePointRight(3).longValueExact();}catch(Exception x){return 0;}}
    private long amount(EditText e){try{return new java.math.BigDecimal(e.getText().toString().trim()).longValueExact();}catch(Exception x){return 0;}}
    private void say(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void home(){frame("العمليات المتقدمة");body.addView(label("عمليات مستقلة تحفظ في قاعدة بيانات المصنع نفسها، وتعمل دون اتصال بالإنترنت.",14));body.addView(button("مرتجعات المبيعات",()->returns()));body.addView(button("الأعمال تحت التشغيل WIP",()->wip()));body.addView(button("دفتر القيود وعكس القيد",()->journals()));}
    private void rows(String sql){Cursor c=null;try{c=db.rawQuery(sql,null);while(c.moveToNext())body.addView(label(c.getString(0),13));}finally{if(c!=null)c.close();}}
    private void returns(){
        frame("مرتجعات المبيعات");
        body.addView(label("أدخل رقم فاتورة البيع. يعيد النظام المخزون بتكلفة البضاعة الأصلية، ويحسب قيمة الرد التراكمية لمنع أخطاء التقريب. المبلغ المتبقي يخفض مديونية العميل.",14));
        rows("SELECT 'فاتورة '||s.id||' — '||s.customer||' — '||p.name||' — كمية '||printf('%.3f',s.qty/1000.0)||' — قيمة '||s.total FROM sales s JOIN products p ON p.id=s.product_id ORDER BY s.id DESC LIMIT 60");
        EditText sale=field("رقم الفاتورة",false),q=field("كمية المرتجع",true),cash=field("رد نقدي",false),bank=field("رد بنكك",false),ref=field("مرجع بنكك",false),reason=field("سبب المرتجع",false);
        body.addView(button("حفظ المرتجع",()->{long id=amount(sale),n=qty(q),c=amount(cash),b=amount(bank);String r=ref.getText().toString().trim();try{long total=postReturn(id,n,c,b,r,reason.getText().toString());say("تم رد "+total+" جنيه وإعادة الكمية للمخزون");returns();}catch(Exception x){say("تعذر تسجيل المرتجع: "+x.getMessage());}}));
        rows("SELECT 'مرتجع فاتورة '||sale_id||' — كمية '||printf('%.3f',qty/1000.0)||' — قيمة '||refund_value||' — تخفيض آجل '||credit_adjustment FROM sales_returns ORDER BY id DESC");
    }
    private long postReturn(long saleId,long q,long cash,long bank,String bankRef,String reason){
        if(saleId<=0||q<=0||cash<0||bank<0)throw new IllegalArgumentException("بيانات المرتجع غير صحيحة");
        db.beginTransaction();try{
            Cursor s=db.rawQuery("SELECT product_id,qty,total,cogs,customer FROM sales WHERE id=?",new String[]{""+saleId});if(!s.moveToFirst()){s.close();throw new IllegalArgumentException("الفاتورة غير موجودة");}
            long pid=s.getLong(0),sold=s.getLong(1),total=s.getLong(2),cogs=s.getLong(3);String customer=s.getString(4);s.close();
            Cursor p=db.rawQuery("SELECT COALESCE(SUM(qty),0),COALESCE(SUM(refund_value),0),COALESCE(SUM(restored_cogs),0) FROM sales_returns WHERE sale_id=?",new String[]{""+saleId});p.moveToFirst();long prevQty=p.getLong(0),prevValue=p.getLong(1),prevCost=p.getLong(2);p.close();
            FactoryMath.returnableQuantity(sold,prevQty,q);long cumulative=prevQty+q;
            long refund=FactoryMath.proportionalReturnValue(total,cumulative,sold)-prevValue;
            long restore=FactoryMath.proportionalReturnValue(cogs,cumulative,sold)-prevCost;
            if(refund<=0||cash>refund||bank>refund-cash)throw new IllegalArgumentException("المبالغ المعادة تتجاوز قيمة المرتجع");
            FactoryMath.validateBankakReference(bank,bankRef);
            if(bank>0){Cursor x=db.rawQuery("SELECT COUNT(*) FROM cashbook WHERE account='BANKAK' AND ref=?",new String[]{bankRef});x.moveToFirst();boolean used=x.getLong(0)>0;x.close();if(used)throw new IllegalArgumentException("مرجع بنكك مستخدم سابقاً");}
            long credit=refund-cash-bank;
            Cursor due=db.rawQuery("SELECT COALESCE(SUM(debt),0)-COALESCE((SELECT SUM(cash+bank) FROM collections WHERE customer=?),0)-COALESCE((SELECT SUM(r.credit_adjustment) FROM sales_returns r JOIN sales ss ON ss.id=r.sale_id WHERE ss.customer=?),0) FROM sales WHERE customer=?",new String[]{customer,customer,customer});
            due.moveToFirst();long outstanding=Math.max(0,due.getLong(0));due.close();
            if(credit>outstanding)throw new IllegalArgumentException("قيمة تخفيض الآجل أكبر من مديونية العميل المتبقية: "+outstanding);
            Cursor f=db.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});long stock=0,value=0;if(f.moveToFirst()){stock=f.getLong(0);value=f.getLong(1);}f.close();
            ContentValues v=new ContentValues();v.put("product_id",pid);v.put("qty",FactoryMath.add(stock,q));v.put("value",FactoryMath.add(value,restore));v.put("unit_cost",FactoryMath.weightedAverageUnitCost(stock,value,q,restore));db.insertWithOnConflict("finished",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            db.execSQL("INSERT INTO sales_returns(sale_id,product_id,qty,refund_value,refund_cash,refund_bank,credit_adjustment,restored_cogs,bank_ref,reason,date) VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{saleId,pid,q,refund,cash,bank,credit,restore,bankRef,reason,today()});
            if(cash>0)db.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','SALE_RETURN',?,?,?)",new Object[]{cash,"return-"+saleId,today()});
            if(bank>0)db.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','SALE_RETURN',?,?,?)",new Object[]{bank,bankRef,today()});
            db.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('SALE_RETURN',?,?,?,?,?)",new Object[]{pid,q,restore,"sale-"+saleId,today()});
            db.execSQL("INSERT INTO audit(action,detail,date) VALUES('SALE_RETURN',?,date('now'))",new Object[]{saleId+":"+q+":"+refund});
            journal("RETURN-"+saleId+"-"+System.currentTimeMillis(),"مرتجع مبيعات "+saleId,"مرتجعات المبيعات","النقدية/بنكك/العملاء",refund);
            if(restore>0)journal("RETURN-STOCK-"+saleId+"-"+System.currentTimeMillis(),"إعادة تكلفة مخزون المرتجع للفاتورة "+saleId,"مخزون الإنتاج التام","تكلفة البضاعة المباعة",restore);
            db.setTransactionSuccessful();return refund;
        }finally{db.endTransaction();}
    }
    private void wip(){
        frame("الأعمال تحت التشغيل");
        body.addView(label("يحوّل أمر التشغيل الخامات المصروفة إلى أصل تحت التشغيل. عند الإكمال تضاف تكلفة العمالة والتكاليف الصناعية، ثم يحول إجمالي التكلفة إلى مخزون الإنتاج التام.",14));
        EditText ref=field("مرجع أمر التشغيل الفريد",false),product=field("اسم المنتج",false),batches=field("عدد الدفعات",false),planned=field("الكمية المخططة",true);
        body.addView(button("بدء أمر التشغيل وصرف الخامات",()->{try{long cost=startWip(ref.getText().toString().trim(),product.getText().toString().trim(),amount(batches),qty(planned));say("بدأ التشغيل. تكلفة الخامات "+cost+" جنيه");wip();}catch(Exception x){say("تعذر بدء التشغيل: "+x.getMessage());}}));
        EditText finishRef=field("مرجع الأمر الجاري",false),actual=field("الكمية المنتجة فعلياً",true),labor=field("أجور الأمر",false),over=field("تكاليف صناعية غير مباشرة",false);
        body.addView(button("إكمال الأمر وتحويله إلى مخزون تام",()->{try{long cost=finishWip(finishRef.getText().toString().trim(),qty(actual),amount(labor),amount(over));say("اكتمل الأمر. إجمالي التكلفة "+cost+" جنيه");wip();}catch(Exception x){say("تعذر إكمال الأمر: "+x.getMessage());}}));
        EditText cancelRef=field("مرجع أمر التشغيل المراد إلغاؤه",false);
        body.addView(button("إلغاء أمر غير مكتمل وإعادة الخامات للمخزن",()->{try{long cost=cancelWip(cancelRef.getText().toString().trim());say("ألغي الأمر وأعيدت خاماته بقيمة "+cost+" جنيه");wip();}catch(Exception x){say("تعذر إلغاء الأمر: "+x.getMessage());}}));
        rows("SELECT 'أمر '||w.work_ref||' — '||p.name||' — '||w.status||' — تكلفة الخامات '||w.material_cost FROM work_orders w JOIN products p ON p.id=w.product_id ORDER BY w.id DESC");
    }
    private long startWip(String ref,String product,long batches,long planned){
        if(ref.isEmpty()||batches<=0||planned<=0)throw new IllegalArgumentException("أكمل بيانات الأمر");
        db.beginTransaction();try{
            Cursor dup=db.rawQuery("SELECT COUNT(*) FROM work_orders WHERE work_ref=?",new String[]{ref});dup.moveToFirst();boolean exists=dup.getLong(0)>0;dup.close();if(exists)throw new IllegalArgumentException("مرجع الأمر مستخدم");
            Cursor p=db.rawQuery("SELECT id FROM products WHERE name=?",new String[]{product});if(!p.moveToFirst()){p.close();throw new IllegalArgumentException("المنتج غير مسجل");}long pid=p.getLong(0);p.close();
            Cursor r=db.rawQuery("SELECT m.id,m.qty,m.value,rc.qty FROM recipe rc JOIN materials m ON m.id=rc.material_id WHERE rc.product_id=?",new String[]{""+pid});
            if(!r.moveToFirst()){r.close();throw new IllegalArgumentException("الوصفة غير مسجلة");}
            ArrayList<long[]> lines=new ArrayList<>();long total=0;
            do{long mid=r.getLong(0),stock=r.getLong(1),value=r.getLong(2),need=FactoryMath.multiply(r.getLong(3),batches);if(need>stock)throw new IllegalArgumentException("رصيد خام غير كافٍ");long cost=FactoryMath.costForStockIssue(value,need,stock);total=FactoryMath.add(total,cost);lines.add(new long[]{mid,stock-need,value-cost,need,cost});}while(r.moveToNext());r.close();
            db.execSQL("INSERT INTO work_orders(work_ref,product_id,batches,planned_qty,material_cost,status,date) VALUES(?,?,?,?,?,'IN_PROGRESS',?)",new Object[]{ref,pid,batches,planned,total,today()});
            Cursor wi=db.rawQuery("SELECT last_insert_rowid()",null);wi.moveToFirst();long wid=wi.getLong(0);wi.close();
            for(long[] a:lines){ContentValues v=new ContentValues();v.put("qty",a[1]);v.put("value",a[2]);db.update("materials",v,"id=?",new String[]{""+a[0]});db.execSQL("INSERT INTO work_order_materials(work_order_id,material_id,qty,cost) VALUES(?,?,?,?)",new Object[]{wid,a[0],a[3],a[4]});db.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('WIP_RAW_OUT',?,?,?,?,?)",new Object[]{a[0],a[3],a[4],ref,today()});}
            journal("WIP-START-"+wid,"صرف خامات لأمر "+ref,"أعمال تحت التشغيل","مخزون المواد الخام",total);
            db.execSQL("INSERT INTO audit(action,detail,date) VALUES('WIP_STARTED',?,date('now'))",new Object[]{ref+":"+total});
            db.setTransactionSuccessful();return total;
        }finally{db.endTransaction();}
    }
    private long finishWip(String ref,long actual,long labor,long overhead){
        if(ref.isEmpty()||actual<=0||labor<0||overhead<0)throw new IllegalArgumentException("بيانات الإكمال غير صحيحة");
        db.beginTransaction();try{
            Cursor c=db.rawQuery("SELECT id,product_id,planned_qty,material_cost,status FROM work_orders WHERE work_ref=?",new String[]{ref});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("الأمر غير موجود");}
            long id=c.getLong(0),pid=c.getLong(1),planned=c.getLong(2),materials=c.getLong(3);String status=c.getString(4);c.close();
            if(!"IN_PROGRESS".equals(status)||actual!=planned)throw new IllegalArgumentException("أكمل الكمية المخططة بالكامل؛ عدّل الكمية المخططة لتطابق الناتج الفعلي قبل بدء الأمر");
            long total=FactoryMath.workInProgressCost(materials,labor,overhead);
            Cursor f=db.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});long oldQ=0,oldV=0;if(f.moveToFirst()){oldQ=f.getLong(0);oldV=f.getLong(1);}f.close();
            ContentValues v=new ContentValues();v.put("product_id",pid);v.put("qty",FactoryMath.add(oldQ,actual));v.put("value",FactoryMath.add(oldV,total));v.put("unit_cost",FactoryMath.weightedAverageUnitCost(oldQ,oldV,actual,total));db.insertWithOnConflict("finished",null,v,SQLiteDatabase.CONFLICT_REPLACE);
            ContentValues w=new ContentValues();w.put("actual_qty",actual);w.put("labor",labor);w.put("overhead",overhead);w.put("total_cost",total);w.put("status","COMPLETED");db.update("work_orders",w,"id=?",new String[]{""+id});
            long addedCost=FactoryMath.add(labor,overhead);if(addedCost>0)journal("WIP-COST-"+id,"إضافة أجور وتكاليف أمر التشغيل "+ref,"أعمال تحت التشغيل","أجور وتكاليف مستحقة",addedCost);
            journal("WIP-COMPLETE-"+id,"تحويل أمر "+ref+" إلى إنتاج تام","مخزون الإنتاج التام","أعمال تحت التشغيل",total);
            db.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('WIP_FINISHED',?,?,?,?,?)",new Object[]{pid,actual,total,ref,today()});
            db.execSQL("INSERT INTO audit(action,detail,date) VALUES('WIP_COMPLETED',?,date('now'))",new Object[]{ref+":"+actual+":"+total});
            db.setTransactionSuccessful();return total;
        }finally{db.endTransaction();}
    }
    private long cancelWip(String ref){
        if(ref==null||ref.trim().isEmpty())throw new IllegalArgumentException("أدخل مرجع أمر التشغيل");
        db.beginTransaction();try{
            Cursor c=db.rawQuery("SELECT id,material_cost,status FROM work_orders WHERE work_ref=?",new String[]{ref});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("أمر التشغيل غير موجود");}
            long id=c.getLong(0),cost=c.getLong(1);String status=c.getString(2);c.close();
            if(!"IN_PROGRESS".equals(status))throw new IllegalArgumentException("يمكن إلغاء الأمر الجاري فقط");
            Cursor lines=db.rawQuery("SELECT material_id,qty,cost FROM work_order_materials WHERE work_order_id=?",new String[]{""+id});
            while(lines.moveToNext()){
                long mid=lines.getLong(0),q=lines.getLong(1),v=lines.getLong(2);
                Cursor m=db.rawQuery("SELECT qty,value FROM materials WHERE id=?",new String[]{""+mid});if(!m.moveToFirst()){m.close();throw new IllegalStateException("الخامة الأصلية غير موجودة");}
                long nextQty=FactoryMath.add(m.getLong(0),q),nextValue=FactoryMath.add(m.getLong(1),v);m.close();
                ContentValues mv=new ContentValues();mv.put("qty",nextQty);mv.put("value",nextValue);db.update("materials",mv,"id=?",new String[]{""+mid});
                db.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('WIP_RAW_RETURN',?,?,?,?,?)",new Object[]{mid,q,v,ref,today()});
            }
            lines.close();
            ContentValues w=new ContentValues();w.put("status","CANCELLED");db.update("work_orders",w,"id=?",new String[]{""+id});
            if(cost>0)journal("WIP-CANCEL-"+id,"إلغاء أمر التشغيل "+ref,"مخزون المواد الخام","أعمال تحت التشغيل",cost);
            db.execSQL("INSERT INTO audit(action,detail,date) VALUES('WIP_CANCELLED',?,date('now'))",new Object[]{ref+":"+cost});
            db.setTransactionSuccessful();return cost;
        }finally{db.endTransaction();}
    }
    private void journals(){
        frame("دفتر القيود");
        body.addView(label("القيد المتوازن يتكون من طرف مدين وطرف دائن بالقيمة نفسها. يسمح بعكس القيود اليدوية فقط. قيود المرتجعات وWIP مرتبطة بمستندات مخزنية ولا تعكس من هذه الشاشة حتى لا تختلف الأرصدة.",14));
        EditText ref=field("مرجع القيد الفريد",false),desc=field("شرح القيد",false),debit=field("الحساب المدين",false),credit=field("الحساب الدائن",false),value=field("المبلغ",false);
        body.addView(button("ترحيل القيد",()->{try{long a=amount(value);journal(ref.getText().toString().trim(),desc.getText().toString(),debit.getText().toString().trim(),credit.getText().toString().trim(),a);say("تم ترحيل القيد");journals();}catch(Exception x){say("تعذر ترحيل القيد: "+x.getMessage());}}));
        rows("SELECT 'قيد '||id||' — '||entry_ref||' — '||description||' — '||status FROM journal_entries ORDER BY id DESC");
        EditText id=field("رقم القيد المطلوب عكسه",false);
        body.addView(button("إنشاء القيد العكسي",()->{try{long n=reverse(amount(id));say("تم إنشاء القيد العكسي رقم "+n);journals();}catch(Exception x){say("تعذر العكس: "+x.getMessage());}}));
    }
    private void journal(String ref,String desc,String debit,String credit,long value){
        if(ref==null||ref.trim().isEmpty()||debit==null||debit.trim().isEmpty()||credit==null||credit.trim().isEmpty()||value<=0)throw new IllegalArgumentException("بيانات القيد غير صحيحة");
        boolean own=!db.inTransaction();if(own)db.beginTransaction();try{
            db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,date) VALUES(?,?,'POSTED',date('now'))",new Object[]{ref,desc});
            Cursor c=db.rawQuery("SELECT last_insert_rowid()",null);c.moveToFirst();long id=c.getLong(0);c.close();
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,?,?,0)",new Object[]{id,debit,value});
            db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,?,0,?)",new Object[]{id,credit,value});
            db.execSQL("INSERT INTO audit(action,detail,date) VALUES('JOURNAL_POSTED',?,date('now'))",new Object[]{ref+":"+desc+":"+value});
            if(own)db.setTransactionSuccessful();
        }finally{if(own)db.endTransaction();}
    }
    private long reverse(long id){
        db.beginTransaction();try{
            Cursor e=db.rawQuery("SELECT entry_ref,description,status,reversal_of FROM journal_entries WHERE id=?",new String[]{""+id});if(!e.moveToFirst()){e.close();throw new IllegalArgumentException("القيد غير موجود");}String entryRef=e.getString(0),desc=e.getString(1),status=e.getString(2);boolean isReversal=!e.isNull(3);e.close();if(!"POSTED".equals(status))throw new IllegalArgumentException("القيد سبق عكسه أو غير مرحل");
            if(isReversal||entryRef.startsWith("WIP-")||entryRef.startsWith("RETURN-")||entryRef.startsWith("REV-"))throw new IllegalArgumentException("هذا القيد مرتبط بمستند أصلي أو هو قيد عكسي؛ راجع المستند الأصلي ولا تعكسه مرة أخرى");
            String reverseRef="REV-"+id+"-"+System.currentTimeMillis();
            db.execSQL("INSERT INTO journal_entries(entry_ref,description,status,reversal_of,date) VALUES(?,?,'POSTED',?,date('now'))",new Object[]{reverseRef,"عكس "+desc,id});
            Cursor n=db.rawQuery("SELECT last_insert_rowid()",null);n.moveToFirst();long rid=n.getLong(0);n.close();
            Cursor lines=db.rawQuery("SELECT account,debit,credit FROM journal_lines WHERE entry_id=?",new String[]{""+id});while(lines.moveToNext())db.execSQL("INSERT INTO journal_lines(entry_id,account,debit,credit) VALUES(?,?,?,?)",new Object[]{rid,lines.getString(0),lines.getLong(2),lines.getLong(1)});lines.close();
            ContentValues v=new ContentValues();v.put("status","REVERSED");db.update("journal_entries",v,"id=?",new String[]{""+id});
            db.execSQL("INSERT INTO audit(action,detail,date) VALUES('JOURNAL_REVERSED',?,date('now'))",new Object[]{id+":"+rid});
            db.setTransactionSuccessful();return rid;
        }finally{db.endTransaction();}
    }
}
