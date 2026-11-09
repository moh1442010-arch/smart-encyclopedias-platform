package com.mohammedmustafa.almizan.bakery;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FactoryActivity extends Activity {
    private static final int NAVY=Color.rgb(11,31,58), GOLD=Color.rgb(212,175,55);
    private DB db; private LinearLayout body; private String screen="home";
    private final android.os.Handler entitlementHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable entitlementWatchdog = new Runnable() {
        @Override public void run() {
            if (!LicenseAccessGate.isAccessAllowed(FactoryActivity.this)) enforceEntitlement();
            else entitlementHandler.postDelayed(this,30000L);
        }
    };
    private boolean enforceEntitlement() {
        if (LicenseAccessGate.isAccessAllowed(this)) return true;
        entitlementHandler.removeCallbacks(entitlementWatchdog);
        if (db != null) { db.close(); db=null; }
        Toast.makeText(this,AppLanguage.tr(this,"انتهت التجربة أو لا يوجد ترخيص ساري. بياناتك محفوظة؛ يرجى التفعيل للمتابعة."),Toast.LENGTH_LONG).show();
        finish();
        startActivity(new Intent(this,MainActivity.class));
        return false;
    }
    private static final String[][] DEFAULT_MATERIALS={{"دقيق","كجم"},{"سكر","كجم"},{"زيت","لتر"},{"خميرة","كجم"},{"سمن","كجم"},{"بيض","طبق"},{"لبن","لتر"},{"زبدة","كجم"},{"قشطة","كجم"},{"كريمة","لتر"},{"جبن","كجم"},{"شوكولاتة","كجم"},{"كاكاو","كجم"},{"فانيليا","جرام"},{"بيكنج باودر","جرام"},{"ملح","كجم"},{"تمر","كجم"},{"سمسم","كجم"},{"جوز هند","كجم"},{"عسل","كجم"},{"شيرة / قطر","لتر"},{"مكسرات","كجم"},{"هيل","جرام"},{"قرفة","جرام"},{"مواد تغليف","قطعة"},{"علب حلويات","قطعة"},{"أكياس","قطعة"},{"غاز","أسطوانة"},{"حطب","ربطة"},{"وقود","لتر"}};
    private static final String[][] DEFAULT_PRODUCTS={{"باسطة","باسطة"},{"فطائر","فطائر"},{"خبائز","خبائز"},{"حلويات","حلويات"},{"بلح شام","بلح شام"},{"كيك","مخبوزات"},{"بسكويت","مخبوزات"},{"معمول","حلويات"},{"كنافة","حلويات"},{"دونات","مخبوزات"},{"سمبوسة","فطائر"},{"خبز","خبائز"},{"صامولي","خبائز"},{"بيتزا","فطائر"}};
    private String today(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    private String businessName(){android.content.SharedPreferences factory=getSharedPreferences("factory_profile",MODE_PRIVATE);String n=factory.getString("business_name",null);if(n==null){String legacy=getSharedPreferences("bakery_profile",MODE_PRIVATE).getString("business_name","");n=legacy==null?"":legacy.trim();if(!n.isEmpty())factory.edit().putString("business_name",n).apply();}return n==null||n.trim().isEmpty()?"المصنع":n.trim();}
    private void editFactoryName(){EditText name=cell("اسم المصنع",false);name.setText(businessName().equals("المصنع")?"":businessName());new AlertDialog.Builder(this).setTitle("بيانات المصنع").setMessage("اسم مستقل للمصنع، لا يغيّر اسم المخبز ولا يربط دفاترهما.").setView(name).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String value=name.getText().toString().trim();if(value.length()<2){toast("أدخل اسم المصنع بصورة صحيحة");return;}getSharedPreferences("factory_profile",MODE_PRIVATE).edit().putString("business_name",value).apply();toast("تم حفظ اسم المصنع بصورة مستقلة");home();}).show();}
    private void printVisibleScreen(String title){
        StringBuilder out=new StringBuilder("اسم المصنع: ").append(businessName()).append("\nاسم الوحدة: مصنع الخبائز والحلويات والباسطة\nالشاشة: ").append(title).append("\nالتاريخ: ").append(today()).append("\n\n");
        appendPrintable(body,out);
        if(out.length()<100){toast("لا توجد بيانات ظاهرة للطباعة.");return;}
        try{PrinterService.print(this,businessName(),title+" - "+businessName(),out.toString());}
        catch(Exception e){toast("تعذر فتح الطباعة: "+e.getMessage());}
    }
    private void appendPrintable(View view,StringBuilder out){
        if(view instanceof EditText){String value=((EditText)view).getText().toString().trim();if(!value.isEmpty())out.append(value).append("\n");return;}
        if(view instanceof TextView){String value=((TextView)view).getText().toString().trim();if(!value.isEmpty())out.append(value).append("\n");}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)appendPrintable(group.getChildAt(i),out);}
    }
    private void printFactoryDaily(){
        StringBuilder out=new StringBuilder("التاريخ: ").append(today()).append("\n");
        Cursor c=null;
        try{
            out.append("المواد الخام\n");
            c=db.getReadableDatabase().rawQuery("SELECT name,qty,unit,value FROM materials ORDER BY name",null);
            while(c.moveToNext())out.append(c.getString(0)).append(" | ").append(c.getLong(1)/1000.0).append(" ").append(c.getString(2)).append(" | القيمة ").append(c.getLong(3)).append("\n");
            c.close();c=null;
            out.append("\nالمنتجات الجاهزة\n");
            c=db.getReadableDatabase().rawQuery("SELECT p.name,f.qty,f.value FROM finished f JOIN products p ON p.id=f.product_id ORDER BY p.name",null);
            while(c.moveToNext())out.append(c.getString(0)).append(" | الكمية ").append(c.getLong(1)/1000.0).append(" | القيمة ").append(c.getLong(2)).append("\n");
            PrinterService.print(this,businessName(),"تقرير المصنع اليومي",out.toString());
        }catch(Exception e){toast("تعذر تجهيز التقرير للطباعة: "+e.getMessage());}
        finally{if(c!=null)c.close();}
    }
    @Override protected void attachBaseContext(Context newBase){super.attachBaseContext(AppLanguage.wrap(newBase));}
    @Override public void onCreate(Bundle b){super.onCreate(b);AppUpdatePolicy.enforceCached(this);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);if(!enforceEntitlement())return;db=new DB(this);db.seedMaterials(DEFAULT_MATERIALS);db.seedProducts(DEFAULT_PRODUCTS);home();}
    @Override protected void onResume(){super.onResume();AppUpdatePolicy.check(this);if(db!=null&&!enforceEntitlement())return;entitlementHandler.removeCallbacks(entitlementWatchdog);entitlementHandler.post(entitlementWatchdog);}
    @Override protected void onPause(){entitlementHandler.removeCallbacks(entitlementWatchdog);super.onPause();}
    @Override protected void onDestroy(){entitlementHandler.removeCallbacks(entitlementWatchdog);if(db!=null){db.close();db=null;}super.onDestroy();}
    @Override public void onBackPressed(){if(!enforceEntitlement())return;if("home".equals(screen))super.onBackPressed();else home();}
    private TextView label(String s,int size){TextView t=new TextView(this);t.setText(AppLanguage.tr(this,s));t.setTextSize(size);t.setTextColor(Color.rgb(30,40,50));t.setPadding(4,10,4,10);return t;}
    private Button btn(String s,Runnable r){Button b=new Button(this);b.setText(AppLanguage.tr(this,s));b.setOnClickListener(v->r.run());return b;}
    private void frame(String title){
        screen="الرئيسية".equals(title)?"home":title; LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setLayoutDirection(AppLanguage.isEnglish(this)?View.LAYOUT_DIRECTION_LTR:View.LAYOUT_DIRECTION_RTL);root.setPadding(14,12,14,16);root.setBackgroundColor(Color.rgb(246,248,251));
        TextView head=label(businessName()+"\n🏭 مصنع الخبائز والحلويات والباسطة — "+title,21);head.setTextColor(NAVY);head.setBackgroundColor(Color.WHITE);head.setGravity(Gravity.CENTER);root.addView(head);
        root.addView(btn("⌂ العودة إلى مصنع الخبائز والحلويات والباسطة",()->home()));
        root.addView(btn("🌐 اللغة / Language",()->AppLanguage.showPicker(this)));
        root.addView(btn("↩ العودة إلى قائمة المخبز الرئيسية",()->finish()));
        root.addView(btn("⌂ الرئيسية",()->{Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);finish();}));
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);body=new LinearLayout(this);body.setOrientation(1);body.setPadding(4,8,4,16);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));root.addView(btn("🖨️ طباعة الشاشة الحالية",()->printVisibleScreen(title)));setContentView(root);
    }
    private void home(){
        frame("الرئيسية");
        body.addView(label("المنشأة: "+businessName()+"\nنظام مستقل عن دفاتر المخبز. كل المخزون والحركات والتكاليف هنا محفوظة في قاعدة بيانات منفصلة.",15));
        body.addView(btn("تعديل اسم المصنع",()->editFactoryName()));
        body.addView(btn("🖨️ إعداد الطابعة / طباعة تقرير المصنع",()->{new AlertDialog.Builder(this).setTitle("الطباعة").setMessage("ستظهر طابعات Android المتاحة. يمكن استخدام طابعة Bluetooth أو Wi-Fi أو USB إذا كان نظام الجهاز يوفر خدمتها.").setNegativeButton("إغلاق",null).setPositiveButton("طباعة تقرير المصنع",(d,w)->printFactoryDaily()).show();}));
        body.addView(btn("سجل عملاء وموردي المصنع",()->factoryParties()));
        body.addView(btn("1. مخزن المواد الخام ومستلزمات المصنع",()->warehouse()));
        body.addView(btn("2. الإنتاج: اسم المنتج والكمية وسعر الوحدة",()->products()));
        body.addView(btn("3. الوصفات ومكونات الإنتاج",()->recipes()));
        body.addView(btn("4. الإنتاج والتشغيل اليومي",()->production()));
        body.addView(btn("5. نقطة البيع",()->pointOfSale()));
        body.addView(btn("5.1 مبيعات التوزيع",()->distributionSales()));
        body.addView(btn("6. المصروفات والتالف",()->expenses()));
        body.addView(btn("7. حساب التشغيل والتكلفة",()->costing()));
        body.addView(btn("8. الحسابات والأرصدة والتقارير",()->accounts()));
        body.addView(btn("10. التقارير المالية والتحليل",()->financialReports()));
        body.addView(btn("9. مرتجعات المبيعات والأعمال تحت التشغيل وعكس القيود",()->startActivity(new Intent(this,FactoryLedgerActivity.class))));
    }
    private EditText field(String hint,boolean decimal){EditText e=cell(hint,decimal);body.addView(e,new LinearLayout.LayoutParams(-1,(int)(58*getResources().getDisplayMetrics().density+0.5f)));return e;}
    private EditText cell(String hint,boolean decimal){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(AppLanguage.tr(this,hint));e.setTextSize(17);e.setPadding(14,8,14,8);e.setMinHeight((int)(56*getResources().getDisplayMetrics().density+0.5f));e.setFocusable(true);e.setFocusableInTouchMode(true);e.setClickable(true);boolean numeric=decimal||hint.contains("سعر")||hint.contains("تكلفة")||hint.contains("قيمة")||hint.contains("مبلغ")||hint.contains("سداد")||hint.contains("أجور")||hint.contains("مصروف")||hint.contains("الرصيد");e.setInputType(numeric?(android.text.InputType.TYPE_CLASS_NUMBER|(decimal?android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL:0)):android.text.InputType.TYPE_CLASS_TEXT);e.setBackgroundColor(Color.WHITE);return e;}
    private void horizontalFields(View... views){LinearLayout column=new LinearLayout(this);column.setOrientation(1);for(View v:views){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,(int)(58*getResources().getDisplayMetrics().density+0.5f));p.setMargins(2,3,2,3);column.addView(v,p);}body.addView(column,new LinearLayout.LayoutParams(-1,-2));}
    private long num(EditText e){try{java.math.BigDecimal x=new java.math.BigDecimal(e.getText().toString().trim());return x.movePointRight(3).longValueExact();}catch(Exception ex){return 0;}}
    private long money(EditText e){try{return new java.math.BigDecimal(e.getText().toString().trim()).longValueExact();}catch(Exception ex){return 0;}}
    private void warehouse(){
        frame("مخزن الخامات");
        body.addView(label("ملخص أرصدة المخزن للعرض والمراجعة فقط. لإدخال البيانات استخدم حقول الوارد وإضافة الخامة أسفل الجدول. كل خانة إدخال مستقلة وقابلة للكتابة.",14));
        tableHeader("الصنف","الكمية المتاحة","سعر الوحدة","استهلاك اليوم");
        Cursor rows=null;
        try{
            rows=db.getReadableDatabase().rawQuery("SELECT m.name,m.qty,m.unit,CASE WHEN m.qty=0 THEN 0 ELSE m.value*1000/m.qty END,COALESCE((SELECT SUM(u.qty) FROM daily_usage u WHERE u.material_id=m.id AND u.date=?),0) FROM materials m ORDER BY CASE m.name WHEN 'دقيق' THEN 1 WHEN 'سكر' THEN 2 WHEN 'زيت' THEN 3 WHEN 'خميرة' THEN 4 WHEN 'سمن' THEN 5 WHEN 'بيض' THEN 6 WHEN 'لبن' THEN 7 WHEN 'مواد تغليف' THEN 8 ELSE 9 END,m.name",new String[]{today()});
            while(rows.moveToNext()) tableRow(rows.getString(0),String.format(Locale.US,"%.3f %s",rows.getLong(1)/1000.0,rows.getString(2)),String.format(Locale.US,"%.2f",rows.getLong(3)/1000.0),String.format(Locale.US,"%.3f",rows.getLong(4)/1000.0));
        }catch(Exception ex){body.addView(label("تعذر عرض المخزن: "+ex.getMessage(),13));}finally{if(rows!=null)rows.close();}
        body.addView(label("إضافة وارد مخزني — اكتب الكمية وسعر الوحدة في الحقول أدناه",18));
        EditText supplier=cell("المورد المختار",false);supplier.setFocusable(false);final long[] supplierId={0};
        horizontalFields(supplier);
        body.addView(btn("بحث واختيار المورد",()->showFactoryPartyPicker("supplier",(id,name)->{supplierId[0]=id;supplier.setText(name);})));
        EditText n=cell("اسم الصنف",false),u=cell("وحدة القياس",false),q=cell("الكمية",true),unitPrice=cell("سعر الوحدة SDG",false),paidCash=cell("سداد نقدي",false),paidBank=cell("سداد بنكك",false),bankRef=cell("رقم العملية",false);
        horizontalFields(n,u,q,unitPrice,paidCash,paidBank,bankRef);
        body.addView(btn("حفظ فاتورة المورد وتحديث المخزون",()->{String name=n.getText().toString().trim(),unit=u.getText().toString().trim(),ref=bankRef.getText().toString().trim();long qty=num(q),price=money(unitPrice),cashPaid=money(paidCash),bankPaid=money(paidBank);if(supplierId[0]<=0||name.isEmpty()||unit.isEmpty()||qty<=0||price<0||cashPaid<0||bankPaid<0){toast("اختر مورداً مسجلاً وأدخل الصنف والكمية والسعر والسداد");return;}try{long total=java.math.BigDecimal.valueOf(qty).multiply(java.math.BigDecimal.valueOf(price)).divide(java.math.BigDecimal.valueOf(1000),0,java.math.RoundingMode.HALF_UP).longValueExact();if(cashPaid>total||bankPaid>total-cashPaid||(bankPaid>0&&ref.isEmpty()))throw new IllegalArgumentException("راجع السداد ورقم العملية؛ لا يمكن أن يتجاوز المدفوع قيمة الفاتورة.");db.receive(name,unit,qty,total,supplierId[0],supplier.getText().toString(),cashPaid,bankPaid,ref);
                String invoice="التاريخ: "+today()+"\nالمورد: "+supplier.getText()+"\nالصنف: "+name+"\nالوحدة: "+unit+"\nالكمية: "+(qty/1000.0)+"\nقيمة الفاتورة: "+total+" جنيه\nسداد نقدي: "+cashPaid+" جنيه\nسداد بنكك: "+bankPaid+" جنيه\nرقم العملية: "+ref+"\nالمديونية: "+(total-cashPaid-bankPaid)+" جنيه";
                new AlertDialog.Builder(this).setTitle("تم حفظ فاتورة المورد").setMessage("تم تحديث المخزون. هل تريد طباعة الفاتورة؟").setNegativeButton("لاحقاً",(d,w)->warehouse()).setPositiveButton("🖨️ طباعة الفاتورة",(d,w)->{try{PrinterService.print(this,businessName(),"فاتورة مشتريات",invoice);}catch(Exception ex){toast("تعذر الطباعة: "+ex.getMessage());}warehouse();}).show();}catch(Exception ex){toast("تعذر تسجيل فاتورة المورد: "+ex.getMessage());}}));
        body.addView(label("سداد مديونية مورد",16));
        EditText paySupplier=cell("المورد المختار للسداد",false);paySupplier.setFocusable(false);final long[] paySupplierId={0};
        horizontalFields(paySupplier);
        body.addView(btn("بحث واختيار المورد",()->showFactoryPartyPicker("supplier",(id,name)->{paySupplierId[0]=id;paySupplier.setText(name);})));
        EditText supplierCash=cell("سداد نقدي",false),supplierBank=cell("سداد بنكك",false),supplierRef=cell("رقم العملية",false);
        horizontalFields(supplierCash,supplierBank,supplierRef);
        body.addView(btn("حفظ سداد المورد",()->{long cash=money(supplierCash),bank=money(supplierBank);String ref=supplierRef.getText().toString().trim();if(paySupplierId[0]<=0||cash<0||bank<0||cash+bank<=0||(bank>0&&ref.isEmpty())){toast("اختر المورد وأدخل مبلغ السداد ورقم العملية للتحويل");return;}try{db.paySupplier(paySupplierId[0],paySupplier.getText().toString(),cash,bank,ref);toast("تم حفظ سداد المورد");warehouse();}catch(Exception ex){toast("تعذر سداد المورد: "+ex.getMessage());}}));
        body.addView(label("أرصدة الموردين المدينة",15));
        Cursor supplierBalances=null;try{supplierBalances=db.getReadableDatabase().rawQuery("SELECT supplier_name||' | المتبقي: '||(SUM(debt)-COALESCE((SELECT SUM(p.cash+p.bank) FROM supplier_payment_records p WHERE p.supplier_master_id=supplier_purchase_records.supplier_master_id),0)) FROM supplier_purchase_records GROUP BY supplier_master_id,supplier_name HAVING SUM(debt)>COALESCE((SELECT SUM(p.cash+p.bank) FROM supplier_payment_records p WHERE p.supplier_master_id=supplier_purchase_records.supplier_master_id),0) ORDER BY supplier_name",null);while(supplierBalances.moveToNext())body.addView(label(supplierBalances.getString(0),13));}finally{if(supplierBalances!=null)supplierBalances.close();}
        body.addView(label("تسجيل استهلاك خامة خلال اليوم",16));
        EditText useName=field("اسم الصنف",false),useQty=field("كمية الاستهلاك اليومي",true),useNote=field("بيان الاستهلاك",false);
        body.addView(btn("تسجيل الاستهلاك وخصمه من رصيد المخزن",()->{String name=useName.getText().toString().trim();long qty=num(useQty);if(name.isEmpty()||qty<=0){toast("حدد الصنف وكمية الاستهلاك");return;}try{db.consumeRaw(name,qty,useNote.getText().toString());toast("تم تسجيل الاستهلاك وخصمه من المخزون");warehouse();}catch(Exception ex){toast("تعذر تسجيل الاستهلاك: "+ex.getMessage());}}));
        body.addView(label("إضافة صنف جديد ورصيد افتتاحي — وحدات الوزن والقياس قابلة للكتابة",17));
        EditText newName=field("اسم الخامة الجديدة",false),newUnit=field("الوحدة: كجم، جرام، لتر، مل، قطعة، طبق، كيس، جوال، صندوق أو وحدة أخرى",false),openingQty=field("الرصيد الافتتاحي — اكتب الكمية",true),openingPrice=field("تكلفة الوحدة بالجنيه السوداني",false);
        body.addView(btn("إضافة خامة مع الرصيد الافتتاحي والتكلفة",()->{String name=newName.getText().toString().trim(),unit=newUnit.getText().toString().trim();long qty=num(openingQty),price=money(openingPrice);if(name.isEmpty()||unit.isEmpty()||qty<0||price<0||(qty>0&&price<=0)){toast("أدخل اسم الخامة ووحدتها والكمية والتكلفة بصورة صحيحة");return;}try{db.createMaterial(name,unit,qty,price);toast("تمت إضافة الخامة ورصيدها الافتتاحي");warehouse();}catch(Exception ex){toast("تعذر إضافة الخامة: "+ex.getMessage());}}));
        EditText oldName=field("اسم الخامة الحالي",false),rename=field("الاسم الجديد",false),renameUnit=field("الوحدة الجديدة",false);
        body.addView(btn("تعديل بيانات الخامة",()->{try{db.renameMaterial(oldName.getText().toString().trim(),rename.getText().toString().trim(),renameUnit.getText().toString().trim());toast("تم تعديل بيانات الخامة");warehouse();}catch(Exception ex){toast("تعذر تعديل الخامة: "+ex.getMessage());}}));
    }
    private void tableHeader(String a,String b,String c,String d){tableRow(a,b,c,d);}
    private void tableRow(String a,String b,String c,String d){
        LinearLayout row=new LinearLayout(this);row.setOrientation(0);row.setBackgroundColor(Color.WHITE);
        String[] values={a,b,c,d};for(String value:values){TextView cell=label(value,12);cell.setGravity(Gravity.CENTER);cell.setPadding(2,9,2,9);row.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}
        body.addView(row);View line=new View(this);line.setBackgroundColor(GOLD);body.addView(line,new LinearLayout.LayoutParams(-1,1));
    }
    private void products(){
        frame("الإنتاج والمنتجات وأسعار الوحدة");
        body.addView(label("المنتجات الأساسية: باسطة، فطائر، خبائز، حلويات، بلح شام. يمكنك إضافة منتجات أخرى. الكمية المعروضة هي الرصيد الفعلي الناتج من الإنتاج.",14));
        tableHeader("اسم المنتج","الكمية المتاحة","سعر الوحدة","التصنيف");
        Cursor c=null;try{c=db.getReadableDatabase().rawQuery("SELECT p.name,COALESCE(f.qty,0),p.unit_price,p.category,p.unit FROM products p LEFT JOIN finished f ON f.product_id=p.id ORDER BY p.id",null);while(c.moveToNext())tableRow(c.getString(0),String.format(Locale.US,"%.3f %s",c.getLong(1)/1000.0,c.getString(4)),""+c.getLong(2),c.getString(3));}finally{if(c!=null)c.close();}
        EditText name=field("اسم المنتج الجديد",false),category=field("التصنيف: حلويات، خبائز، باسطة، فطائر...",false),productUnit=field("وحدة بيع المنتج: قطعة، كجم، طبق، صينية، علبة، صندوق...",false),price=field("سعر بيع الوحدة بالجنيه",false);
        body.addView(btn("حفظ المنتج وسعر الوحدة",()->{String n=name.getText().toString().trim(),cat=category.getText().toString().trim(),unit=productUnit.getText().toString().trim();long p=money(price);if(n.isEmpty()||unit.isEmpty()||p<0){toast("أدخل اسم المنتج ووحدة البيع وسعر الوحدة");return;}try{db.saveProduct(n,cat.isEmpty()?"منتج إضافي":cat,p,unit);toast("تم حفظ المنتج ووحدة البيع وسعره");products();}catch(Exception ex){toast("تعذر حفظ المنتج: "+ex.getMessage());}}));
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
        EditText product=field("اسم المنتج",false),salePrice=field("سعر بيع الوحدة بالجنيه",false),batchRef=field("مرجع الدفعة (رقم فريد)",false),batches=field("عدد الدفعات",false),output=field("الكمية السليمة المنتجة",true),labor=field("أجور العمال لهذه الدفعة بالجنيه",false),over=field("تكاليف تشغيل مباشرة أخرى بالجنيه",false);
        body.addView(btn("ترحيل دفعة الإنتاج نهائياً",()->{String pn=product.getText().toString().trim(),br=batchRef.getText().toString().trim();long pid=db.id("products",pn),b=money(batches),out=num(output),w=money(labor),oh=money(over),sp=money(salePrice);if(pid==0||br.isEmpty()||b<=0||out<=0||w<0||oh<0||sp<=0){toast("أدخل المنتج وسعر الوحدة ومرجع الدفعة والكميات والتكاليف");return;}try{db.saveProduct(pn,"منتج المصنع",sp);db.finalizeProduction(pid,br,b,out,w,oh);toast("تم ترحيل الإنتاج وخصم المواد وإضافة المنتج للمخزون");production();}catch(Exception ex){toast("لم يرحل الإنتاج: "+ex.getMessage());}}));
        list("SELECT p.name||' | الكمية المنتجة: '||printf('%.3f',f.qty/1000.0)||' | تكلفة الوحدة بالجنيه: '||printf('%.2f',f.unit_cost/1000.0) FROM finished f JOIN products p ON p.id=f.product_id");
    }
    // All legacy sales entry points are redirected to this single accounting screen.
    private void pointOfSale(){sales("نقطة البيع");}
    private void distributionSales(){sales("التوزيع");}
    private void sales(){pointOfSale();}
    private void cashWindow(){pointOfSale();}
    private void sales(String channel){
        frame(channel);
        body.addView(label("شاشة مبيعات "+channel+" لمنتجات المصنع. تسجل الكمية وسعر الوحدة، ثم توزع قيمة الفاتورة إلى نقد وبنكك وآجل. تظهر مبيعات هذا المسار منفصلة في التقارير ويجمعها المركز المحاسبي دون خصم.",14));
        EditText customer=field("العميل المختار من السجل",false);customer.setFocusable(false);final long[] customerId={0};
        body.addView(btn("بحث واختيار العميل",()->showFactoryPartyPicker("customer",(id,name)->{customerId[0]=id;customer.setText(name);})));
        EditText product=field("اسم المنتج",false),qty=field("الكمية المباعة",true),unitPrice=field("سعر بيع الوحدة بالجنيه",false),cash=field("المبلغ النقدي المستلم",false),bank=field("المبلغ المحول عبر بنكك",false),ref=field("رقم عملية بنكك عند التحويل",false);
        body.addView(btn("عرض سعر الوحدة المسجل",()->{long pid=db.id("products",product.getText().toString().trim());if(pid==0){toast("المنتج غير مسجل");return;}long p=db.productPrice(pid);unitPrice.setText(""+p);}));
        body.addView(btn("تسجيل البيع وترحيل الحسابات",()->{
            long pid=db.id("products",product.getText().toString().trim()),q=num(qty),p=money(unitPrice),c=money(cash),bk=money(bank);String cust=customer.getText().toString().trim(),br=ref.getText().toString().trim();
            if(pid==0||customerId[0]<=0||q<=0||p<=0||cust.isEmpty()||c<0||bk<0){toast("اختر عميلاً مسجلاً وأدخل المنتج والكمية والسعر والتحصيل بصورة صحيحة");return;}
            try{
                long total=FactoryMath.saleTotal(q,p);
                if(total<=0||c>total||bk>total-c||(bk>0&&br.isEmpty())){toast("راجع الإجمالي والتحصيل ورقم بنكك");return;}
                db.sell(pid,customerId[0],q,total,c,bk,cust,br,channel);
                String invoice="رقم المستند: POS-"+System.currentTimeMillis()+"\nالتاريخ: "+today()+"\nالعميل: "+cust+"\nالمنتج: "+product.getText()+"\nالكمية: "+(q/1000.0)+"\nسعر الوحدة: "+p+" جنيه\nالإجمالي: "+total+" جنيه\nنقدي: "+c+" جنيه\nبنكك: "+bk+" جنيه\nرقم العملية: "+br+"\nالآجل: "+(total-c-bk)+" جنيه";
                new AlertDialog.Builder(this).setTitle("تم حفظ فاتورة البيع").setMessage("الإجمالي "+total+" جنيه. هل تريد طباعة الفاتورة الآن؟").setNegativeButton("لاحقاً",(d,w)->pointOfSale()).setPositiveButton("🖨️ طباعة الفاتورة",(d,w)->{try{PrinterService.print(this,businessName(),"فاتورة بيع",invoice);}catch(Exception ex){toast("تعذر الطباعة: "+ex.getMessage());}pointOfSale();}).show();
            }catch(Exception ex){toast("لم يتم البيع: "+ex.getMessage());}
        }));
        body.addView(label("تحصيل مديونية سابقة",15));
        EditText debtor=field("العميل المختار",false);debtor.setFocusable(false);final long[] debtorId={0};body.addView(btn("بحث واختيار العميل للتحصيل",()->showFactoryPartyPicker("customer",(id,name)->{debtorId[0]=id;debtor.setText(name);})));
        EditText colCash=field("مبلغ نقدي",false),colBank=field("مبلغ بنكك",false),colRef=field("رقم عملية بنكك",false);
        body.addView(btn("تسجيل التحصيل",()->{String dn=debtor.getText().toString().trim(),br=colRef.getText().toString().trim();long cc=money(colCash),cb=money(colBank);if(debtorId[0]<=0||dn.isEmpty()||cc<0||cb<0||cc+cb<=0||(cb>0&&br.isEmpty())){toast("اختر عميلاً مسجلاً وأدخل بيانات التحصيل بصورة صحيحة");return;}try{db.collect(debtorId[0],dn,cc,cb,br);toast("تم التحصيل وتخفيض مديونية العميل");pointOfSale();}catch(Exception ex){toast("تعذر التحصيل: "+ex.getMessage());}}));
        tableHeader("المنتج والعميل","الكمية","الإجمالي","الآجل");
        Cursor c=null;try{c=db.getReadableDatabase().rawQuery("SELECT p.name||' / '||s.customer,s.qty,s.total,s.debt FROM sales s JOIN products p ON p.id=s.product_id ORDER BY s.id DESC LIMIT 100",null);while(c.moveToNext())tableRow(c.getString(0),String.format(Locale.US,"%.3f",c.getLong(1)/1000.0),""+c.getLong(2),""+c.getLong(3));}finally{if(c!=null)c.close();}
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
    private void financialReports(){
        frame("التقارير المالية والتحليل");
        body.addView(label("تقارير المصنع من ملخص محاسبي موحد. المبيعات تشمل النقد وبنكك والآجل في المخبز والتوزيع. التحصيل النقدي منفصل عن الإيراد.",14));
        body.addView(label("أولاً — قائمة الدخل التشغيلية الموحدة",18));
        FactoryAccountingSummary s=accountingSummary();
        summaryLine("إجمالي مبيعات نقطة البيع",s.counterCash+s.counterBankak+s.counterReceivables);
        summaryLine("إجمالي مبيعات التوزيع",s.distributionCash+s.distributionBankak+s.distributionReceivables);
        summaryLine("إجمالي المبيعات قبل المرتجعات",s.grossSales);
        summaryLine("مرتجعات المبيعات",s.salesReturns);
        summaryLine("صافي المبيعات",s.netSales);
        summaryLine("تكلفة البضاعة المباعة بعد عكس تكلفة المرتجعات",s.costOfGoodsSold);
        summaryLine("المصروفات التشغيلية المسجلة",s.otherExpenses);
        summaryLine("تكلفة التالف",s.spoilage);
        summaryLine("استهلاك الخامات اليدوي غير المرتبط بأوامر الإنتاج",s.manualRawConsumption);
        summaryLine("إجمالي تكلفة التشغيل المحتسبة",s.totalOperatingCost);
        summaryLine("صافي الربح أو الخسارة التشغيلية",s.operatingProfit);
        body.addView(label("ثانياً — التحصيلات والذمم",18));
        summaryLine("النقد وبنكك عند تسجيل البيع",s.cashAndBankakAtSale);
        summaryLine("الآجل المسجل عند البيع",s.receivablesAtSale);
        list("SELECT 'تحصيلات ديون سابقة نقداً وبنكك: '||COALESCE(SUM(cash+bank),0)||' جنيه' FROM collections");
        list("SELECT 'الديون المتبقية للعملاء: '||(COALESCE((SELECT SUM(debt) FROM sales),0)-COALESCE((SELECT SUM(cash+bank) FROM collections),0)-COALESCE((SELECT SUM(credit_adjustment) FROM sales_returns),0))||' جنيه'");
        body.addView(label("ثالثاً — الأرصدة والمخزون",18));
        list("SELECT 'رصيد النقدية: '||(COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='CASH' AND kind IN ('SALE','COLLECTION')),0)-COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='CASH' AND kind IN ('EXPENSE','SALE_RETURN','PURCHASE')),0))||' جنيه'");
        list("SELECT 'رصيد بنكك: '||(COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='BANKAK' AND kind IN ('SALE','COLLECTION')),0)-COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='BANKAK' AND kind IN ('EXPENSE','SALE_RETURN','PURCHASE')),0))||' جنيه'");
        list("SELECT 'قيمة مخزون المواد الخام: '||COALESCE(SUM(value),0)||' جنيه' FROM materials");
        list("SELECT 'قيمة مخزون الإنتاج التام: '||COALESCE(SUM(value),0)||' جنيه' FROM finished");
        list("SELECT 'قيمة الأعمال المتبقية تحت التشغيل: '||COALESCE(SUM(material_cost+labor+overhead-total_cost),0)||' جنيه' FROM work_orders WHERE status IN ('IN_PROGRESS','PARTIAL')");
        body.addView(label("رابعاً — تفاصيل الإنتاج",18));
        list("SELECT 'عدد أوامر التشغيل المكتملة: '||COUNT(*) FROM work_orders WHERE status='COMPLETED'");
        list("SELECT 'عدد أوامر التشغيل الجارية: '||COUNT(*) FROM work_orders WHERE status='IN_PROGRESS'");
        body.addView(label("منع الازدواج: تكلفة المواد وأجور التشغيل المحملة على الإنتاج تدخل في تكلفة المنتج التام، ثم تظهر ضمن تكلفة البضاعة المباعة عند البيع. لذلك لا نضيف صرف الإنتاج المسجل في daily_usage مرة ثانية. أضفنا الاستهلاك اليدوي فقط كبند مستقل. قيمة المشتريات الواردة للمخزن أصل مخزون وليست مصروفاً فورياً. المشتريات الرأسمالية لا تُعامل كمصروف تشغيل بمجرد شرائها.",13));
        body.addView(label("هذه تقارير تشغيلية وليست حتى الآن ميزان مراجعة شاملاً أو قوائم مالية معيارية مكتملة. يجب استكمال ربط كل مصادر المخبز والتوزيع بالمركز المحاسبي قبل اعتماد تقارير المنشأة الموحدة.",13));
    }
    private void costing(){
        frame("حساب التشغيل والتكلفة");
        FactoryAccountingSummary summary=accountingSummary();
        summaryLine("إجمالي المبيعات بعد المرتجعات",summary.netSales);
        summaryLine("إجمالي تكلفة التشغيل",summary.totalOperatingCost);
        summaryLine("صافي الربح أو الخسارة التشغيلية",summary.operatingProfit);
        list("SELECT p.name||' | رصيد المنتج: '||printf('%.3f',f.qty/1000.0)||' | تكلفة الوحدة بالجنيه: '||printf('%.2f',f.unit_cost/1000.0) FROM finished f JOIN products p ON p.id=f.product_id");
        list("SELECT m.name||' | رصيد الخام: '||printf('%.3f',m.qty/1000.0)||' '||m.unit||' | قيمة المخزون: '||m.value FROM materials m ORDER BY m.name");
        list("SELECT 'إجمالي المصروفات: '||COALESCE(SUM(amount),0)||' جنيه' FROM expenses");
        list("SELECT 'استهلاك الخامات اليوم: '||COALESCE(SUM(cost),0)||' جنيه' FROM daily_usage WHERE date=date('now')");
    }
    private void accounts(){
        frame("الحسابات والأرصدة");
        FactoryAccountingSummary s=accountingSummary();
        summaryLine("إجمالي المبيعات بعد المرتجعات",s.netSales);
        summaryLine("نقد البيع",s.counterCash+s.distributionCash);
        summaryLine("بنكك البيع",s.counterBankak+s.distributionBankak);
        summaryLine("الآجل عند البيع",s.receivablesAtSale);
        list("SELECT 'تحصيلات الديون السابقة: '||COALESCE(SUM(cash+bank),0)||' جنيه' FROM collections");
        list("SELECT 'الديون المتبقية للعملاء: '||(COALESCE((SELECT SUM(debt) FROM sales),0)-COALESCE((SELECT SUM(cash+bank) FROM collections),0)-COALESCE((SELECT SUM(credit_adjustment) FROM sales_returns),0))||' جنيه'");
        list("SELECT 'صافي رصيد الصندوق النقدي: '||(COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='CASH' AND kind IN ('SALE','COLLECTION')),0)-COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='CASH' AND kind IN ('EXPENSE','SALE_RETURN','PURCHASE')),0))||' جنيه'");
        list("SELECT 'صافي رصيد بنكك: '||(COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='BANKAK' AND kind IN ('SALE','COLLECTION')),0)-COALESCE((SELECT SUM(amount) FROM cashbook WHERE account='BANKAK' AND kind IN ('EXPENSE','SALE_RETURN','PURCHASE')),0))||' جنيه'");
        summaryLine("تكلفة التشغيل المحتسبة",s.totalOperatingCost);
        summaryLine("صافي الربح أو الخسارة التشغيلية",s.operatingProfit);
        list("SELECT s.customer||' | دين متبقٍ: '||(SUM(s.debt)-COALESCE((SELECT SUM(c.cash+c.bank) FROM collections c WHERE c.customer=s.customer),0)-COALESCE((SELECT SUM(r.credit_adjustment) FROM sales_returns r JOIN sales rs ON rs.id=r.sale_id WHERE rs.customer=s.customer),0)) FROM sales s GROUP BY s.customer HAVING SUM(s.debt)>COALESCE((SELECT SUM(c.cash+c.bank) FROM collections c WHERE c.customer=s.customer),0)+COALESCE((SELECT SUM(r.credit_adjustment) FROM sales_returns r JOIN sales rs ON rs.id=r.sale_id WHERE rs.customer=s.customer),0) ORDER BY s.customer");
    }
    private void summaryLine(String title,long value){TextView row=label(title+": "+String.format(Locale.US,"%,d",value)+" جنيه",14);row.setBackgroundColor(Color.WHITE);body.addView(row);View line=new View(this);line.setBackgroundColor(GOLD);body.addView(line,new LinearLayout.LayoutParams(-1,1));}
    private FactoryAccountingSummary accountingSummary(){
        Cursor c=null;
        try{
            c=db.getReadableDatabase().rawQuery("SELECT "+
                "COALESCE(SUM(CASE WHEN LOWER(COALESCE(channel,'')) LIKE '%distribution%' OR COALESCE(channel,'') LIKE '%توزيع%' THEN 0 ELSE cash END),0),"+
                "COALESCE(SUM(CASE WHEN LOWER(COALESCE(channel,'')) LIKE '%distribution%' OR COALESCE(channel,'') LIKE '%توزيع%' THEN 0 ELSE bank END),0),"+
                "COALESCE(SUM(CASE WHEN LOWER(COALESCE(channel,'')) LIKE '%distribution%' OR COALESCE(channel,'') LIKE '%توزيع%' THEN 0 ELSE debt END),0),"+
                "COALESCE(SUM(CASE WHEN LOWER(COALESCE(channel,'')) LIKE '%distribution%' OR COALESCE(channel,'') LIKE '%توزيع%' THEN cash ELSE 0 END),0),"+
                "COALESCE(SUM(CASE WHEN LOWER(COALESCE(channel,'')) LIKE '%distribution%' OR COALESCE(channel,'') LIKE '%توزيع%' THEN bank ELSE 0 END),0),"+
                "COALESCE(SUM(CASE WHEN LOWER(COALESCE(channel,'')) LIKE '%distribution%' OR COALESCE(channel,'') LIKE '%توزيع%' THEN debt ELSE 0 END),0) FROM sales",null);
            c.moveToFirst();long cc=c.getLong(0),cb=c.getLong(1),cd=c.getLong(2),dc=c.getLong(3),dbank=c.getLong(4),dd=c.getLong(5);c.close();c=null;
            long returns=queryTotal("SELECT COALESCE(SUM(refund_value),0) FROM sales_returns");
            long cogs=queryTotal("SELECT COALESCE(SUM(cogs),0) FROM sales");
            long restored=queryTotal("SELECT COALESCE(SUM(restored_cogs),0) FROM sales_returns");
            if(restored>cogs)throw new IllegalStateException("تكلفة المرتجعات أكبر من تكلفة المبيعات؛ راجع مستندات المرتجع");
            return FactoryAccountingSummary.of(cc,cb,cd,dc,dbank,dd,returns,cogs-restored,
                queryTotal("SELECT COALESCE(SUM(amount),0) FROM expenses"),
                queryTotal("SELECT COALESCE(SUM(cost),0) FROM waste"),
                queryTotal("SELECT COALESCE(SUM(cost),0) FROM daily_usage WHERE kind='MANUAL'"));
        }finally{if(c!=null)c.close();}
    }
    private long queryTotal(String sql){Cursor c=null;try{c=db.getReadableDatabase().rawQuery(sql,null);return c.moveToFirst()?c.getLong(0):0;}finally{if(c!=null)c.close();}}
    private void list(String sql){Cursor c=null;try{c=db.getReadableDatabase().rawQuery(sql,null);if(!c.moveToFirst()){body.addView(label("لا توجد سجلات بعد.",13));return;}do{TextView row=label("│ "+c.getString(0),14);row.setBackgroundColor(Color.WHITE);body.addView(row);View line=new View(this);line.setBackgroundColor(GOLD);body.addView(line,new LinearLayout.LayoutParams(-1,1));}while(c.moveToNext());}catch(Exception e){body.addView(label("تعذر قراءة السجلات: "+e.getMessage(),13));}finally{if(c!=null)c.close();}}
    // Factory party masters and their identifiers are isolated inside almizan_factory_1.db.
    private interface FactoryPartySelected { void onSelect(long id,String name); }
    private void addFactoryPartyDialog(String type,FactoryPartySelected selected){
        String title="customer".equals(type)?"إضافة عميل للمصنع":"إضافة مورد للمصنع";
        LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(18,8,18,8);
        EditText name=new EditText(this);name.setSingleLine(true);name.setHint("اسم "+("customer".equals(type)?"العميل":"المورد"));
        EditText phone=new EditText(this);phone.setSingleLine(true);phone.setHint("رقم الهاتف");phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        EditText iban=new EditText(this);iban.setSingleLine(true);iban.setHint("رقم الحساب أو IBAN (اختياري)");
        form.addView(name);form.addView(phone);form.addView(iban);
        new AlertDialog.Builder(this).setTitle(title).setView(form).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{
            try{long id=db.addFactoryParty(type,name.getText().toString(),phone.getText().toString(),iban.getText().toString());String n=name.getText().toString().trim();toast("تم حفظ الطرف في سجل المصنع المستقل");if(selected!=null)selected.onSelect(id,n);else factoryParties();}
            catch(Exception ex){toast("تعذر حفظ الطرف: "+ex.getMessage());}
        }).show();
    }
    private void showFactoryPartyPicker(String type,FactoryPartySelected selected){
        String title="customer".equals(type)?"اختيار عميل المصنع":"اختيار مورد المصنع";
        Dialog dialog=new Dialog(this);LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(16,12,16,12);
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("ابحث بالاسم أو رقم الهاتف");
        ListView list=new ListView(this);ArrayList<String> labels=new ArrayList<>(),names=new ArrayList<>();ArrayList<Long> ids=new ArrayList<>();
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels);list.setAdapter(adapter);
        Runnable refresh=()->{Cursor cur=null;ids.clear();labels.clear();names.clear();try{cur=db.searchFactoryParties(type,search.getText().toString());while(cur.moveToNext()){ids.add(cur.getLong(0));String name=cur.getString(1),phone=cur.getString(2);names.add(name);labels.add(name+(phone==null||phone.isEmpty()?"":" — "+phone));}}finally{if(cur!=null)cur.close();}adapter.notifyDataSetChanged();};
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){refresh.run();}public void afterTextChanged(android.text.Editable e){}});
        list.setOnItemClickListener((parent,view,position,id)->{if(position>=0&&position<ids.size()){long partyId=ids.get(position);String value=names.get(position);dialog.dismiss();if(selected!=null)selected.onSelect(partyId,value);}});
        layout.addView(search,new LinearLayout.LayoutParams(-1,(int)(56*getResources().getDisplayMetrics().density)));
        layout.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        layout.addView(btn("إضافة "+("customer".equals(type)?"عميل":"مورد")+" جديد",()->addFactoryPartyDialog(type,(id,name)->{dialog.dismiss();if(selected!=null)selected.onSelect(id,name);})));
        layout.addView(btn("إغلاق",dialog::dismiss));
        dialog.setTitle(title);dialog.setContentView(layout);dialog.show();dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels*0.92),(int)(getResources().getDisplayMetrics().heightPixels*0.78));refresh.run();
    }
    private void factoryParties(){
        frame("عملاء وموردو المصنع");
        body.addView(label("هذا السجل مستقل بالكامل عن سجل المخبز. تُحفظ بيانات العملاء والموردين ومعرفاتهم داخل قاعدة المصنع فقط.",15));
        body.addView(btn("إضافة عميل جديد",()->addFactoryPartyDialog("customer",null)));
        body.addView(btn("بحث واختيار عميل",()->showFactoryPartyPicker("customer",(id,name)->toast("العميل المحدد: "+name))));
        body.addView(btn("إضافة مورد جديد",()->addFactoryPartyDialog("supplier",null)));
        body.addView(btn("بحث واختيار مورد",()->showFactoryPartyPicker("supplier",(id,name)->toast("المورد المحدد: "+name))));
        body.addView(label("العملاء المسجلون في المصنع",17));listFactoryParties("customer");
        body.addView(label("الموردون المسجلون في المصنع",17));listFactoryParties("supplier");
    }
    private void listFactoryParties(String type){
        Cursor cur=null;try{cur=db.searchFactoryParties(type,"");while(cur.moveToNext()){String phone=cur.getString(2),iban=cur.getString(3);body.addView(label(cur.getString(1)+(phone==null||phone.isEmpty()?"":" | "+phone)+(iban==null||iban.isEmpty()?"":" | "+iban),14));}}catch(Exception ex){body.addView(label("تعذر عرض سجل الأطراف: "+ex.getMessage(),13));}finally{if(cur!=null)cur.close();}
    }
    private void toast(String s){Toast.makeText(this,AppLanguage.tr(this,s),Toast.LENGTH_LONG).show();}

    private static class DB extends SQLiteOpenHelper {
        DB(Context c){super(c,"almizan_factory_1.db",SecureDatabase.passphrase(c,"almizan_factory_1.db"),null,12,0,null,null,false);}
        public void onCreate(SQLiteDatabase d){
            d.execSQL("CREATE TABLE materials(id INTEGER PRIMARY KEY,name TEXT UNIQUE,unit TEXT,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE products(id INTEGER PRIMARY KEY,name TEXT UNIQUE,category TEXT NOT NULL DEFAULT 'منتج إضافي',unit_price INTEGER NOT NULL DEFAULT 0,unit TEXT NOT NULL DEFAULT 'قطعة')");
            d.execSQL("CREATE TABLE recipe(id INTEGER PRIMARY KEY,product_id INTEGER,material_id INTEGER,qty INTEGER CHECK(qty>0),UNIQUE(product_id,material_id))");
            d.execSQL("CREATE TABLE batches(id INTEGER PRIMARY KEY,batch_ref TEXT UNIQUE,product_id INTEGER,batches INTEGER,output INTEGER,labor INTEGER,overhead INTEGER,cost INTEGER,unit_cost INTEGER,status TEXT,date TEXT)");
            d.execSQL("CREATE TABLE finished(product_id INTEGER PRIMARY KEY,qty INTEGER NOT NULL DEFAULT 0,value INTEGER NOT NULL DEFAULT 0,unit_cost INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE sales(id INTEGER PRIMARY KEY,product_id INTEGER,customer TEXT,customer_master_id INTEGER NOT NULL DEFAULT 0,channel TEXT,qty INTEGER,total INTEGER,cash INTEGER,bank INTEGER,bank_ref TEXT,debt INTEGER,cogs INTEGER,date TEXT)");
            d.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY,date TEXT,category TEXT,amount INTEGER,note TEXT)");
            d.execSQL("CREATE TABLE waste(id INTEGER PRIMARY KEY,product_id INTEGER,qty INTEGER,cost INTEGER,reason TEXT,date TEXT)");
            d.execSQL("CREATE TABLE movements(id INTEGER PRIMARY KEY,kind TEXT,item_id INTEGER,qty INTEGER,value INTEGER,ref TEXT,date TEXT,supplier_master_id INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE collections(id INTEGER PRIMARY KEY,customer TEXT,customer_master_id INTEGER NOT NULL DEFAULT 0,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT,date TEXT)");
            d.execSQL("CREATE TABLE supplier_purchase_records(id INTEGER PRIMARY KEY,supplier_master_id INTEGER NOT NULL,supplier_name TEXT NOT NULL,item_name TEXT NOT NULL,quantity_milli INTEGER NOT NULL,invoice_total INTEGER NOT NULL,paid_cash INTEGER NOT NULL,paid_bank INTEGER NOT NULL,bank_ref TEXT NOT NULL DEFAULT '',debt INTEGER NOT NULL,currency_code TEXT NOT NULL DEFAULT 'SDG',date TEXT NOT NULL)");
            d.execSQL("CREATE TABLE supplier_payment_records(id INTEGER PRIMARY KEY,supplier_master_id INTEGER NOT NULL,supplier_name TEXT NOT NULL,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT NOT NULL DEFAULT '',date TEXT NOT NULL)");
            d.execSQL("CREATE TABLE cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER NOT NULL,ref TEXT,date TEXT)");
            d.execSQL("CREATE TABLE audit(id INTEGER PRIMARY KEY,action TEXT,detail TEXT,date TEXT)");
            d.execSQL("CREATE TABLE factory_customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            d.execSQL("CREATE TABLE factory_suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            d.execSQL("CREATE INDEX idx_factory_customers_phone ON factory_customers(phone)");
            d.execSQL("CREATE INDEX idx_factory_suppliers_phone ON factory_suppliers(phone)");
            d.execSQL("CREATE UNIQUE INDEX idx_factory_customers_phone_unique ON factory_customers(phone) WHERE phone<>''");
            d.execSQL("CREATE UNIQUE INDEX idx_factory_suppliers_phone_unique ON factory_suppliers(phone) WHERE phone<>''");
            d.execSQL("CREATE TABLE daily_usage(id INTEGER PRIMARY KEY,date TEXT,material_id INTEGER,qty INTEGER,cost INTEGER,note TEXT,kind TEXT NOT NULL DEFAULT 'MANUAL')");
            d.execSQL("CREATE TABLE sales_returns(id INTEGER PRIMARY KEY,sale_id INTEGER NOT NULL,product_id INTEGER NOT NULL,qty INTEGER NOT NULL,refund_value INTEGER NOT NULL,refund_cash INTEGER NOT NULL,refund_bank INTEGER NOT NULL,credit_adjustment INTEGER NOT NULL,restored_cogs INTEGER NOT NULL,bank_ref TEXT,reason TEXT,date TEXT)");
            d.execSQL("CREATE TABLE work_orders(id INTEGER PRIMARY KEY,work_ref TEXT UNIQUE,product_id INTEGER NOT NULL,batches INTEGER NOT NULL,planned_qty INTEGER NOT NULL,actual_qty INTEGER NOT NULL DEFAULT 0,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL DEFAULT 0,overhead INTEGER NOT NULL DEFAULT 0,total_cost INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,date TEXT)");
            d.execSQL("CREATE TABLE work_order_materials(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,material_id INTEGER NOT NULL,qty INTEGER NOT NULL,cost INTEGER NOT NULL)");
            d.execSQL("CREATE TABLE journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");
            d.execSQL("CREATE TABLE journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");
            d.execSQL("CREATE TABLE work_order_stages(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,stage_no INTEGER NOT NULL,output_qty INTEGER NOT NULL,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL,overhead INTEGER NOT NULL,total_cost INTEGER NOT NULL,date TEXT,UNIQUE(work_order_id,stage_no))");
            FactoryGlOutbox.ensureSchema(d);
            d.setVersion(12);
        }
        public void onUpgrade(SQLiteDatabase d,int old,int now){if(old<2){d.execSQL("ALTER TABLE batches ADD COLUMN batch_ref TEXT");d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_factory_batch_ref ON batches(batch_ref)");d.execSQL("CREATE TABLE IF NOT EXISTS collections(id INTEGER PRIMARY KEY,customer TEXT,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS cashbook(id INTEGER PRIMARY KEY,account TEXT,kind TEXT,amount INTEGER NOT NULL,ref TEXT,date TEXT)");}if(old<3)d.execSQL("ALTER TABLE sales ADD COLUMN channel TEXT NOT NULL DEFAULT 'التوزيع'");if(old<4){d.execSQL("ALTER TABLE products ADD COLUMN category TEXT NOT NULL DEFAULT 'منتج إضافي'");d.execSQL("ALTER TABLE products ADD COLUMN unit_price INTEGER NOT NULL DEFAULT 0");d.execSQL("CREATE TABLE IF NOT EXISTS daily_usage(id INTEGER PRIMARY KEY,date TEXT,material_id INTEGER,qty INTEGER,cost INTEGER,note TEXT,kind TEXT NOT NULL DEFAULT 'MANUAL')");}if(old<5){d.execSQL("CREATE TABLE IF NOT EXISTS sales_returns(id INTEGER PRIMARY KEY,sale_id INTEGER NOT NULL,product_id INTEGER NOT NULL,qty INTEGER NOT NULL,refund_value INTEGER NOT NULL,refund_cash INTEGER NOT NULL,refund_bank INTEGER NOT NULL,credit_adjustment INTEGER NOT NULL,restored_cogs INTEGER NOT NULL,bank_ref TEXT,reason TEXT,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS work_orders(id INTEGER PRIMARY KEY,work_ref TEXT UNIQUE,product_id INTEGER NOT NULL,batches INTEGER NOT NULL,planned_qty INTEGER NOT NULL,actual_qty INTEGER NOT NULL DEFAULT 0,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL DEFAULT 0,overhead INTEGER NOT NULL DEFAULT 0,total_cost INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS work_order_materials(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,material_id INTEGER NOT NULL,qty INTEGER NOT NULL,cost INTEGER NOT NULL)");d.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY,entry_ref TEXT UNIQUE,description TEXT,status TEXT NOT NULL,reversal_of INTEGER,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY,entry_id INTEGER NOT NULL,account TEXT NOT NULL,debit INTEGER NOT NULL DEFAULT 0,credit INTEGER NOT NULL DEFAULT 0)");}if(old<6)d.execSQL("CREATE TABLE IF NOT EXISTS work_order_stages(id INTEGER PRIMARY KEY,work_order_id INTEGER NOT NULL,stage_no INTEGER NOT NULL,output_qty INTEGER NOT NULL,material_cost INTEGER NOT NULL,labor INTEGER NOT NULL,overhead INTEGER NOT NULL,total_cost INTEGER NOT NULL,date TEXT,UNIQUE(work_order_id,stage_no))");if(old<7){try{d.execSQL("ALTER TABLE sales ADD COLUMN customer_master_id INTEGER NOT NULL DEFAULT 0");}catch(Exception ignored){}try{d.execSQL("ALTER TABLE collections ADD COLUMN customer_master_id INTEGER NOT NULL DEFAULT 0");}catch(Exception ignored){}}if(old<8){try{d.execSQL("ALTER TABLE movements ADD COLUMN supplier_master_id INTEGER NOT NULL DEFAULT 0");}catch(Exception ignored){}d.execSQL("CREATE TABLE IF NOT EXISTS supplier_purchase_records(id INTEGER PRIMARY KEY,supplier_master_id INTEGER NOT NULL,supplier_name TEXT NOT NULL,item_name TEXT NOT NULL,quantity_milli INTEGER NOT NULL,invoice_total INTEGER NOT NULL,paid_cash INTEGER NOT NULL,paid_bank INTEGER NOT NULL,bank_ref TEXT NOT NULL DEFAULT '',debt INTEGER NOT NULL,currency_code TEXT NOT NULL DEFAULT 'SDG',date TEXT NOT NULL)");}if(old<9)d.execSQL("CREATE TABLE IF NOT EXISTS supplier_payment_records(id INTEGER PRIMARY KEY,supplier_master_id INTEGER NOT NULL,supplier_name TEXT NOT NULL,cash INTEGER NOT NULL,bank INTEGER NOT NULL,bank_ref TEXT NOT NULL DEFAULT '',date TEXT NOT NULL)");if(old<10)d.execSQL("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT 'قطعة'");if(old<11){
            d.execSQL("CREATE TABLE IF NOT EXISTS factory_customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            d.execSQL("CREATE TABLE IF NOT EXISTS factory_suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL UNIQUE,phone TEXT NOT NULL DEFAULT '',iban TEXT NOT NULL DEFAULT '',created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            d.execSQL("CREATE INDEX IF NOT EXISTS idx_factory_customers_phone ON factory_customers(phone)");
            d.execSQL("CREATE INDEX IF NOT EXISTS idx_factory_suppliers_phone ON factory_suppliers(phone)");
            d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_factory_customers_phone_unique ON factory_customers(phone) WHERE phone<>''");
            d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_factory_suppliers_phone_unique ON factory_suppliers(phone) WHERE phone<>''");
            d.execSQL("INSERT OR IGNORE INTO factory_customers(name) SELECT DISTINCT TRIM(customer) FROM sales WHERE customer IS NOT NULL AND TRIM(customer)<>''");
            d.execSQL("INSERT OR IGNORE INTO factory_customers(name) SELECT DISTINCT TRIM(customer) FROM collections WHERE customer IS NOT NULL AND TRIM(customer)<>''");
            d.execSQL("UPDATE sales SET customer_master_id=COALESCE((SELECT id FROM factory_customers p WHERE p.name=TRIM(sales.customer)),0)");
            d.execSQL("UPDATE collections SET customer_master_id=COALESCE((SELECT id FROM factory_customers p WHERE p.name=TRIM(collections.customer)),0)");
            d.execSQL("INSERT OR IGNORE INTO factory_suppliers(name) SELECT DISTINCT TRIM(supplier_name) FROM supplier_purchase_records WHERE supplier_name IS NOT NULL AND TRIM(supplier_name)<>''");
            d.execSQL("INSERT OR IGNORE INTO factory_suppliers(name) SELECT DISTINCT TRIM(supplier_name) FROM supplier_payment_records WHERE supplier_name IS NOT NULL AND TRIM(supplier_name)<>''");
            d.execSQL("UPDATE supplier_purchase_records SET supplier_master_id=COALESCE((SELECT id FROM factory_suppliers p WHERE p.name=TRIM(supplier_purchase_records.supplier_name)),0)");
            d.execSQL("UPDATE supplier_payment_records SET supplier_master_id=COALESCE((SELECT id FROM factory_suppliers p WHERE p.name=TRIM(supplier_payment_records.supplier_name)),0)");
            d.execSQL("UPDATE movements SET supplier_master_id=0");
            d.execSQL("UPDATE movements SET ref=COALESCE((SELECT sp.supplier_name FROM supplier_purchase_records sp WHERE substr(movements.ref,1,length(sp.supplier_name)+1)=sp.supplier_name||':' ORDER BY length(sp.supplier_name) DESC LIMIT 1),ref) WHERE kind='RAW_IN' AND instr(ref,':')>0");
            d.execSQL("UPDATE movements SET supplier_master_id=COALESCE((SELECT id FROM factory_suppliers p WHERE p.name=movements.ref),0) WHERE kind='RAW_IN'");
        }
        d.setVersion(11);}
        if(old<12)FactoryGlOutbox.ensureSchema(d);
        d.setVersion(12);}
        void seedProducts(String[][] items){SQLiteDatabase d=w();d.beginTransaction();try{for(String[] item:items)d.execSQL("INSERT OR IGNORE INTO products(name,category,unit_price) VALUES(?,?,0)",new Object[]{item[0],item[1]});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void saveProduct(String name,String category,long price){if(name==null||name.trim().isEmpty()||price<0)throw new IllegalArgumentException("بيانات المنتج غير صحيحة");Cursor c=getReadableDatabase().rawQuery("SELECT unit FROM products WHERE name=?",new String[]{name.trim()});String existing=c.moveToFirst()?c.getString(0):"قطعة";c.close();saveProduct(name,category,price,existing);}
        void saveProduct(String name,String category,long price,String unit){if(name==null||name.trim().isEmpty()||unit==null||unit.trim().isEmpty()||price<0)throw new IllegalArgumentException("بيانات المنتج غير صحيحة");SQLiteDatabase d=w();d.beginTransaction();try{Cursor c=d.rawQuery("SELECT id FROM products WHERE name=?",new String[]{name.trim()});long id=c.moveToFirst()?c.getLong(0):0;c.close();ContentValues v=new ContentValues();v.put("category",category);v.put("unit_price",price);v.put("unit",unit.trim());if(id==0){v.put("name",name.trim());d.insertOrThrow("products",null,v);}else d.update("products",v,"id=?",new String[]{""+id});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('PRODUCT_SAVED',?,date('now'))",new Object[]{name+":"+price+":"+unit});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        long productPrice(long pid){Cursor c=getReadableDatabase().rawQuery("SELECT unit_price FROM products WHERE id=?",new String[]{""+pid});try{return c.moveToFirst()?c.getLong(0):0;}finally{c.close();}}
        void consumeRaw(String name,long qty,String note){if(qty<=0)throw new IllegalArgumentException("كمية الاستهلاك يجب أن تكون موجبة");SQLiteDatabase d=w();d.beginTransaction();try{Cursor c=d.rawQuery("SELECT id,qty,value FROM materials WHERE name=?",new String[]{name});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("الخامة غير مسجلة");}long id=c.getLong(0),stock=c.getLong(1),value=c.getLong(2);c.close();if(qty>stock)throw new IllegalArgumentException("الاستهلاك أكبر من الرصيد المتاح");long cost=FactoryMath.costForStockIssue(value,qty,stock);ContentValues v=new ContentValues();v.put("qty",stock-qty);v.put("value",value-cost);d.update("materials",v,"id=?",new String[]{""+id});String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.execSQL("INSERT INTO daily_usage(date,material_id,qty,cost,note,kind) VALUES(?,?,?,?,?,'MANUAL')",new Object[]{date,id,qty,cost,note});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('DAILY_USE',?,?,?,? ,?)",new Object[]{id,qty,cost,note,date});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('RAW_DAILY_USE',?,date('now'))",new Object[]{name+":"+qty+":"+cost});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        long addFactoryParty(String type,String name,String phone,String iban){
             if(name==null||name.trim().isEmpty())throw new IllegalArgumentException("اسم العميل أو المورد مطلوب");
             String table="customer".equals(type)?"factory_customers":"supplier".equals(type)?"factory_suppliers":null;
             if(table==null)throw new IllegalArgumentException("نوع الطرف غير صحيح");
             String cleanName=name.trim(),cleanPhone=phone==null?"":phone.trim(),cleanIban=iban==null?"":iban.trim();
             SQLiteDatabase d=w();d.beginTransaction();try{
                 if(!cleanPhone.isEmpty()){Cursor p=d.rawQuery("SELECT COUNT(*) FROM "+table+" WHERE phone=?",new String[]{cleanPhone});p.moveToFirst();boolean exists=p.getLong(0)>0;p.close();if(exists)throw new IllegalArgumentException("رقم الهاتف مسجل لطرف آخر في المصنع");}
                 ContentValues v=new ContentValues();v.put("name",cleanName);v.put("phone",cleanPhone);v.put("iban",cleanIban);
                 long id=d.insertOrThrow(table,null,v);d.execSQL("INSERT INTO audit(action,detail,date) VALUES('FACTORY_PARTY_CREATED',?,date('now'))",new Object[]{type+":"+cleanName});
                 d.setTransactionSuccessful();return id;
             }finally{d.endTransaction();}
         }
         Cursor searchFactoryParties(String type,String query){
             String table="customer".equals(type)?"factory_customers":"supplier".equals(type)?"factory_suppliers":null;
             if(table==null)throw new IllegalArgumentException("نوع الطرف غير صحيح");
             String q=query==null?"":query.trim();
             return getReadableDatabase().rawQuery("SELECT id,name,phone,iban FROM "+table+" WHERE name LIKE ? OR phone LIKE ? ORDER BY name",new String[]{"%"+q+"%","%"+q+"%"});
         }
        SQLiteDatabase w(){return getWritableDatabase();}
        void seedMaterials(String[][] items){SQLiteDatabase d=w();d.beginTransaction();try{for(String[] item:items)d.execSQL("INSERT OR IGNORE INTO materials(name,unit,qty,value) VALUES(?,?,0,0)",new Object[]{item[0],item[1]});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void createMaterial(String name,String unit,long qty,long unitPrice){if(name==null||name.trim().isEmpty()||unit==null||unit.trim().isEmpty()||qty<0||unitPrice<0||(qty>0&&unitPrice<=0))throw new IllegalArgumentException("بيانات الخامة غير صحيحة");SQLiteDatabase d=w();d.beginTransaction();try{Cursor exists=d.rawQuery("SELECT COUNT(*) FROM materials WHERE name=?",new String[]{name.trim()});exists.moveToFirst();boolean duplicate=exists.getLong(0)>0;exists.close();if(duplicate)throw new IllegalArgumentException("الخامة موجودة بالفعل؛ استخدم فاتورة وارد لإضافة كمية جديدة");long value=java.math.BigDecimal.valueOf(qty).multiply(java.math.BigDecimal.valueOf(unitPrice)).divide(java.math.BigDecimal.valueOf(1000),0,java.math.RoundingMode.HALF_UP).longValueExact();ContentValues v=new ContentValues();v.put("name",name.trim());v.put("unit",unit.trim());v.put("qty",qty);v.put("value",value);long id=d.insertOrThrow("materials",null,v);if(qty>0)d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('RAW_OPENING',?,?,?,'opening-stock',date('now'))",new Object[]{id,qty,value});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('MATERIAL_CREATED',?,date('now'))",new Object[]{name+":"+unit+":"+qty+":"+unitPrice});d.setTransactionSuccessful();}finally{d.endTransaction();}}
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
        void receive(String name,String unit,long qty,long cost,long supplierId,String supplierName,long paidCash,long paidBank,String bankRef){
            if(qty<=0||cost<0||supplierId<=0||supplierName==null||supplierName.trim().isEmpty()||paidCash<0||paidBank<0||paidCash>cost||paidBank>cost-paidCash||(paidBank>0&&(bankRef==null||bankRef.trim().isEmpty())))throw new IllegalArgumentException("بيانات فاتورة المورد أو الوارد غير صحيحة");
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor c=d.rawQuery("SELECT id,qty,value,unit FROM materials WHERE name=?",new String[]{name});
                long id=0,oldq=0,oldv=0;String oldunit=null;if(c.moveToFirst()){id=c.getLong(0);oldq=c.getLong(1);oldv=c.getLong(2);oldunit=c.getString(3);}c.close();
                if(oldunit!=null&&!oldunit.equals(unit))throw new IllegalArgumentException("وحدة المادة لا تطابق الرصيد السابق");
                long nq=FactoryMath.add(oldq,qty),nv=FactoryMath.add(oldv,cost);
                if(id==0){ContentValues v=new ContentValues();v.put("name",name);v.put("unit",unit);v.put("qty",nq);v.put("value",nv);d.insertOrThrow("materials",null,v);}else{ContentValues v=new ContentValues();v.put("qty",nq);v.put("value",nv);d.update("materials",v,"id=?",new String[]{""+id});}
                long materialId=id==0?this.id("materials",name):id;
                String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date,supplier_master_id) VALUES('RAW_IN',?,?,?,?,?,?)",new Object[]{materialId,qty,cost,supplierName+":"+supplierId,date,supplierId});d.execSQL("INSERT INTO supplier_purchase_records(supplier_master_id,supplier_name,item_name,quantity_milli,invoice_total,paid_cash,paid_bank,bank_ref,debt,currency_code,date) VALUES(?,?,?,?,?,?,?,?,?,'SDG',?)",new Object[]{supplierId,supplierName,name,qty,cost,paidCash,paidBank,bankRef,cost-paidCash-paidBank,date});if(paidBank>0)ensureUniqueBankakRef(d,bankRef);if(paidCash>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','PURCHASE',?,?,?)",new Object[]{paidCash,supplierName,date});if(paidBank>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','PURCHASE',?,?,?)",new Object[]{paidBank,bankRef,date});
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
                for(long[] x:lines){ContentValues v=new ContentValues();v.put("qty",x[1]);v.put("value",x[2]);d.update("materials",v,"id=?",new String[]{""+x[0]});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('RAW_OUT',?,?,?,'production',date('now'))",new Object[]{x[0],x[3],x[4]});d.execSQL("INSERT INTO daily_usage(date,material_id,qty,cost,note,kind) VALUES(date('now'),?,?,?,'صرف خامات لوصفة الإنتاج','PRODUCTION')",new Object[]{x[0],x[3],x[4]});}
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
        void sell(long pid,long customerMasterId,long qty,long total,long cash,long bank,String customer,String ref,String channel){
            SQLiteDatabase d=w();d.beginTransaction();try{
            if(customer==null||customer.trim().isEmpty()||qty<=0||total<=0||cash<0||bank<0||cash>total||bank>total-cash)throw new IllegalArgumentException("بيانات البيع أو التحصيل غير صحيحة");
            FactoryMath.validateBankakReference(bank,ref);
            if(bank>0)ensureUniqueBankakRef(d,ref);
            Cursor c=d.rawQuery("SELECT qty,value,unit_cost FROM finished WHERE product_id=?",new String[]{""+pid});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("لا يوجد مخزون منتج");}long stock=c.getLong(0),value=c.getLong(1),unit=c.getLong(2);c.close();if(stock<qty)throw new IllegalArgumentException("الكمية المتاحة أقل من المطلوب");if(qty<=0||total<=0||cash<0||bank<0||cash>total||bank>total-cash)throw new IllegalArgumentException("قيمة البيع أو التحصيل غير صحيحة");long cogs=FactoryMath.costForStockIssue(value,qty,stock);ContentValues f=new ContentValues();f.put("qty",stock-qty);f.put("value",value-cogs);f.put("unit_cost",stock==qty?0:java.math.BigDecimal.valueOf(value-cogs).multiply(java.math.BigDecimal.valueOf(1000000)).divide(java.math.BigDecimal.valueOf(stock-qty),0,java.math.RoundingMode.HALF_UP).longValueExact());d.update("finished",f,"product_id=?",new String[]{""+pid});d.execSQL("INSERT INTO sales(product_id,customer,customer_master_id,channel,qty,total,cash,bank,bank_ref,debt,cogs,date) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{pid,customer,customerMasterId,channel,qty,total,cash,bank,ref,total-cash-bank,cogs,new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date())});
            String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());if(cash>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','SALE',?,?,?)",new Object[]{cash,"sale",date});if(bank>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','SALE',?,?,?)",new Object[]{bank,ref,date});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('SALE',?,?,?,'sale',date('now'))",new Object[]{pid,qty,cogs});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('SALE_POSTED',?,date('now'))",new Object[]{customer+":"+pid+":"+qty+":"+total});d.setTransactionSuccessful();}finally{d.endTransaction();}
        }
        void collect(long customerMasterId,String customer,long cash,long bank,String ref){if(customer==null||customer.trim().isEmpty()||cash<0||bank<0||FactoryMath.add(cash,bank)<=0||(bank>0&&(ref==null||ref.trim().isEmpty())))throw new IllegalArgumentException("بيانات التحصيل غير صحيحة");SQLiteDatabase d=w();d.beginTransaction();try{FactoryMath.validateBankakReference(bank,ref);if(bank>0)ensureUniqueBankakRef(d,ref);Cursor c=d.rawQuery("SELECT COALESCE(SUM(debt),0) FROM sales WHERE customer=?",new String[]{customer});c.moveToFirst();long debt=c.getLong(0);c.close();Cursor p=d.rawQuery("SELECT COALESCE(SUM(cash+bank),0) FROM collections WHERE customer=?",new String[]{customer});p.moveToFirst();long previous=p.getLong(0);p.close();Cursor cr=d.rawQuery("SELECT COALESCE(SUM(r.credit_adjustment),0) FROM sales_returns r JOIN sales s ON s.id=r.sale_id WHERE s.customer=?",new String[]{customer});cr.moveToFirst();long credits=cr.getLong(0);cr.close();long remaining=Math.max(0,debt-previous-credits);if(FactoryMath.add(cash,bank)>remaining)throw new IllegalArgumentException("التحصيل أكبر من المديونية المتبقية: "+remaining);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.execSQL("INSERT INTO collections(customer,customer_master_id,cash,bank,bank_ref,date) VALUES(?,?,?,?,?,?)",new Object[]{customer,customerMasterId,cash,bank,ref,date});if(cash>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','COLLECTION',?,?,?)",new Object[]{cash,customer,date});if(bank>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','COLLECTION',?,?,?)",new Object[]{bank,ref,date});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('CUSTOMER_COLLECTION',?,date('now'))",new Object[]{customer+":"+cash+":"+bank});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void paySupplier(long supplierId,String supplierName,long cash,long bank,String ref){
            if(supplierId<=0||supplierName==null||supplierName.trim().isEmpty()||cash<0||bank<0||FactoryMath.add(cash,bank)<=0||(bank>0&&(ref==null||ref.trim().isEmpty())))throw new IllegalArgumentException("بيانات سداد المورد غير صحيحة");
            SQLiteDatabase d=w();d.beginTransaction();try{
                Cursor a=d.rawQuery("SELECT COALESCE(SUM(debt),0) FROM supplier_purchase_records WHERE supplier_master_id=?",new String[]{""+supplierId});a.moveToFirst();long invoiceDebt=a.getLong(0);a.close();
                Cursor b=d.rawQuery("SELECT COALESCE(SUM(cash+bank),0) FROM supplier_payment_records WHERE supplier_master_id=?",new String[]{""+supplierId});b.moveToFirst();long previous=b.getLong(0);b.close();
                long remaining=Math.max(0,invoiceDebt-previous);long payment=FactoryMath.add(cash,bank);
                if(payment>remaining)throw new IllegalArgumentException("السداد أكبر من مديونية المورد المتبقية: "+remaining);
                if(bank>0)ensureUniqueBankakRef(d,ref);
                String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
                d.execSQL("INSERT INTO supplier_payment_records(supplier_master_id,supplier_name,cash,bank,bank_ref,date) VALUES(?,?,?,?,?,?)",new Object[]{supplierId,supplierName,cash,bank,ref,date});
                if(cash>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('CASH','PURCHASE',?,?,?)",new Object[]{cash,supplierName,date});
                if(bank>0)d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES('BANKAK','PURCHASE',?,?,?)",new Object[]{bank,ref,date});
                d.execSQL("INSERT INTO audit(action,detail,date) VALUES('SUPPLIER_PAYMENT',?,date('now'))",new Object[]{supplierName+":"+cash+":"+bank});
                d.setTransactionSuccessful();
            }finally{d.endTransaction();}
        }
        void addExpense(String category,long amount,String note,boolean bank,String ref){if(category==null||category.trim().isEmpty()||amount<=0||(bank&&(ref==null||ref.trim().isEmpty())))throw new IllegalArgumentException("بيانات المصروف غير صحيحة");SQLiteDatabase d=w();d.beginTransaction();try{FactoryMath.validateBankakReference(bank?amount:0,ref);if(bank)ensureUniqueBankakRef(d,ref);String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.execSQL("INSERT INTO expenses(date,category,amount,note) VALUES(?,?,?,?)",new Object[]{date,category,amount,note});d.execSQL("INSERT INTO cashbook(account,kind,amount,ref,date) VALUES(?,'EXPENSE',?,?,?)",new Object[]{bank?"BANKAK":"CASH",amount,bank?ref:category,date});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('EXPENSE',?,date('now'))",new Object[]{category+":"+amount});d.setTransactionSuccessful();}finally{d.endTransaction();}}
        void waste(long pid,long qty,String reason){
            if(qty<=0)throw new IllegalArgumentException("كمية التالف يجب أن تكون موجبة");
            SQLiteDatabase d=w();d.beginTransaction();try{Cursor c=d.rawQuery("SELECT qty,value FROM finished WHERE product_id=?",new String[]{""+pid});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("لا يوجد رصيد");}long stock=c.getLong(0),value=c.getLong(1);c.close();if(qty>stock)throw new IllegalArgumentException("التالف أكبر من الرصيد");if(qty<=0)throw new IllegalArgumentException("كمية التالف يجب أن تكون موجبة");long cost=FactoryMath.costForStockIssue(value,qty,stock);ContentValues f=new ContentValues();f.put("qty",stock-qty);f.put("value",value-cost);f.put("unit_cost",stock==qty?0:java.math.BigDecimal.valueOf(value-cost).multiply(java.math.BigDecimal.valueOf(1000000)).divide(java.math.BigDecimal.valueOf(stock-qty),0,java.math.RoundingMode.HALF_UP).longValueExact());d.update("finished",f,"product_id=?",new String[]{""+pid});d.execSQL("INSERT INTO waste(product_id,qty,cost,reason,date) VALUES(?,?,?,?,date('now'))",new Object[]{pid,qty,cost,reason});d.execSQL("INSERT INTO movements(kind,item_id,qty,value,ref,date) VALUES('WASTE',?,?,?,'waste',date('now'))",new Object[]{pid,qty,cost});d.execSQL("INSERT INTO audit(action,detail,date) VALUES('WASTE_POSTED',?,date('now'))",new Object[]{pid+":"+qty+":"+cost+":"+reason});d.setTransactionSuccessful();}finally{d.endTransaction();}
        }
    }
}