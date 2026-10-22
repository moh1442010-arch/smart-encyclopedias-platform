package com.mohammedmustafa.smartcompany;

import android.app.*;import android.os.*;import android.content.*;import android.database.*;import android.database.sqlite.*;import android.graphics.Color;import android.print.*;import android.webkit.*;import android.view.*;import android.widget.*;import java.util.*;import java.io.*;

public class MainActivity extends Activity{
 DB db; LinearLayout content; boolean authenticated=false; boolean needsRelogin=false; EditText pendingBarcodeTarget; android.net.Uri planLogoUri=null,planReceiptUri=null; final int green=Color.rgb(11,31,58);
 boolean english(){return getSharedPreferences("settings",0).getBoolean("english",false);}
 String tr(String value){
  if(value==null||!english())return value;
  String[][] pairs={
   {"انتهت التجربة المجانية. الرجاء التواصل مع الدعم.","The free trial has ended. Please contact support."},{"الترخيص التجاري","Commercial License"},{"تفعيل المحاسب الميزان","Activate Al-Mizan Accountant"},{"تحقق من الترخيص أو أدخل رمز التفعيل الذي حصلت عليه بعد اعتماد الشراء.","Verify your license or enter the activation code issued after purchase approval."},{"يلزم الاتصال بالإنترنت للتحقق من الترخيص أول مرة أو بعد انتهاء مهلة العمل دون اتصال.","Internet access is required for first-time license verification or after the offline grace period."},{"تعذر التحقق من الترخيص. حاول مرة أخرى.","Unable to verify the license. Please try again."},{"رمز التفعيل التجاري","Commercial activation code"},{"تفعيل الترخيص","Activate License"},{"أدخل رمز تفعيل صحيحًا","Enter a valid activation code"},{"تعذر الاتصال بخادم الترخيص.","Could not connect to the license server."},{"تعذر التفعيل: ","Activation failed: "},{"تم تفعيل الترخيص بنجاح","License activated successfully"},{"إعادة التحقق من الترخيص","Recheck License"},{"انتهت التجربة المجانية. أدخل رمز الترخيص التجاري للمتابعة.","The free trial has ended. Enter a commercial license code to continue."},{"لا تحذف بيانات الشركة عند انتهاء التجربة أو الترخيص. يجب أن تبقى بياناتك قابلة للتصدير.","Do not delete company data when the trial or license expires. Your data must remain exportable."},{"💼 الباقات والأسعار","💼 Plans & Pricing"},{"🔄 فحص التحديثات","🔄 Check for updates"},{"أنت تستخدم أحدث إصدار منشور للمراجعة.","You are using the latest published review version."},{"يتوفر إصدار أحدث. هل تريد فتح مركز التحديث والتحقق؟","A newer version is available. Open the update verification center?"},{"تعذر الاتصال بمركز التحديثات. تحقق من الإنترنت ثم أعد المحاولة.","Could not reach the update center. Check your internet connection and retry."},{"تعذر التحقق من بيانات الإصدار؛ لم يتم تنزيل أي ملف.","Could not verify release metadata; no file was downloaded."},{"جارٍ فحص الإصدار المنشور…","Checking the published version…"},{"الإصدار المثبت: ","Installed version: "},{"الإصدار المنشور: ","Published version: "},{"حالة الإصدار: ","Release status: "},{"لن يبدأ التنزيل تلقائيًا. تحقق من بصمة الملف والتوقيع قبل التثبيت.","No automatic download will start. Verify the APK checksum and signature before installing."},{"إلغاء","Cancel"},{"فتح مركز التحديثات","Open update center"},{"الباقات والأسعار","Plans & Pricing"},{"اختر طريقة الشراء التي تناسب نشاطك","Choose the purchase option that fits your business"},{"تجربة مجانية لمدة 7 أيام — للتقييم فقط. بعد انتهائها تُقفل الوظائف المدفوعة، ولا ينبغي حذف بيانات الشركة أو منع تصديرها.","7-day free trial for evaluation only. Paid features lock after expiry; company data must not be deleted or blocked from export."},{"1) الترخيص الدائم","1) Perpetual license"},{"دفعة واحدة لاستخدام النسخة المحددة وقت الشراء. لا يشمل المزايا الجديدة المستقبلية أو الخدمات السحابية إلا إذا نص الترخيص على ذلك. تُعلن سياسة إصلاحات الأمان والتوافق بوضوح.","One-time payment for the version purchased. Future features and cloud services are excluded unless stated in the license. Security and compatibility-fix policy must be clear."},{"2) الاشتراك الشهري","2) Monthly subscription"},{"يشمل مزايا الباقة والتحديثات والدعم المحدد طوال مدة الاشتراك. يمكن إيقاف الوظائف المدفوعة عند الانتهاء، مع الحفاظ على البيانات وإتاحة تصديرها.","Includes the plan features, updates and stated support during the subscription. Paid features may stop at expiry, while data remains available for export."},{"3) الاشتراك السنوي","3) Annual subscription"},{"يشمل مزايا الاشتراك الشهري نفسها لمدة سنة كاملة، مع سعر سنوي مخفض مقارنة بالدفع الشهري عند اعتماد قائمة الأسعار النهائية.","Includes the same plan features as monthly for a full year, with a discount versus monthly billing once final prices are approved."},{"مستويات المنتج","Product tiers"},{"Basic — إدارة المبيعات والمشتريات والمخزون والعملاء والموردين والتقارير الأساسية.","Basic — sales, purchases, inventory, customers, suppliers and basic reports."},{"Pro — مزايا Basic وتقارير ووظائف محاسبية موسعة بعد اختبارها واعتمادها.","Pro — Basic features plus expanded reports and accounting functions once tested and approved."},{"Business — احتياجات أوسع للمستخدمين والصلاحيات والفروع عند اكتمال تنفيذها.","Business — broader user, permission and branch capabilities once implemented."},{"Enterprise — عرض مخصص للشركات الكبيرة بحسب المستخدمين والفروع والتكاملات والدعم.","Enterprise — a tailored offer for larger companies based on users, branches, integrations and support."},{"قائمة الأسعار المعتمدة للمراجعة الفنية فقط","Approved price list for technical review only"},{"السودان — Basic: دائم 150,000 ج.س | شهري 25,000 ج.س | سنوي 250,000 ج.س","Sudan — Basic: perpetual SDG 150,000 | monthly SDG 25,000 | annual SDG 250,000"},{"السودان — Pro: دائم 300,000 ج.س | شهري 45,000 ج.س | سنوي 450,000 ج.س","Sudan — Pro: perpetual SDG 300,000 | monthly SDG 45,000 | annual SDG 450,000"},{"السودان — Business: دائم 600,000 ج.س | شهري 80,000 ج.س | سنوي 800,000 ج.س","Sudan — Business: perpetual SDG 600,000 | monthly SDG 80,000 | annual SDG 800,000"},{"السودان — Enterprise: دائم 1,200,000 ج.س | شهري 150,000 ج.س | سنوي 1,500,000 ج.س","Sudan — Enterprise: perpetual SDG 1,200,000 | monthly SDG 150,000 | annual SDG 1,500,000"},{"الدفع الدولي — الدولار الأمريكي (USD) | IBAN","International payments — US dollars (USD) | IBAN"},{"Basic: دائم $249 | شهري $9 | سنوي $90","Basic: perpetual $249 | monthly $9 | annual $90"},{"Pro: دائم $499 | شهري $19 | سنوي $190","Pro: perpetual $499 | monthly $19 | annual $190"},{"Business: دائم $999 | شهري $39 | سنوي $390","Business: perpetual $999 | monthly $39 | annual $390"},{"Enterprise: دائم $1,999 | شهري $79 | سنوي $790","Enterprise: perpetual $1,999 | monthly $79 | annual $790"},{"مصر والعراق وبقية الدول: الأسعار المحلية لم تعتمد بعد، ولن تُحسب بتحويل آلي للعملة.","Egypt, Iraq and other markets: local prices are not approved yet and will not be converted automatically."},{"مهم: هذه الشاشة تعرض هيكل الباقات والأسعار المقترحة فقط. الشراء والتفعيل والتجديد الآلي غير متاحين بعد؛ لا تدفع مقابل اشتراك من داخل هذا الإصدار.","Important: this screen shows proposed plans and prices only. Purchase, activation and auto-renewal are not available yet; do not pay for a subscription in this build."},{"نسخ رقم حساب بنكك","Copy Bankak account number"},{"تم نسخ رقم حساب بنكك","Bankak account number copied"},{"اختيار شعار الشركة (اختياري)","Choose company logo (optional)"},{"اختيار إيصال التحويل","Choose payment receipt"},{"إرسال طلب الاشتراك والشعار/الإيصال","Send subscription request and logo/receipt"},{"إرسال الطلب عبر تطبيق مناسب","Send request using an app"},{"أدخل اسم الشركة أو النشاط أولًا","Enter company or business name first"},{"تم اختيار شعار الشركة","Company logo selected"},{"تم اختيار إيصال التحويل","Payment receipt selected"},{"تعذر فتح تطبيق الإرسال؛ حاول البريد أو واتساب يدويًا","Could not open sharing app; try email or WhatsApp manually"},{"رقم واتساب للتواصل","WhatsApp contact number"},{"اسم الشركة أو النشاط","Company or business name"},{"نوع الخدمة / الباقة","Service / plan type"},{"مدة الاشتراك","Subscription duration"},{"تفاصيل الخدمة المطلوبة أو عدد المستخدمين والفروع","Requested service details or number of users and branches"},{"الدفع في السودان عبر بنكك","Payment in Sudan via Bankak"},{"رقم الحساب: 1882224","Account number: 1882224"},{"بعد التحويل، أرفق إيصال الدفع. يمكنك أيضًا إرفاق شعار الشركة إذا طلبت تخصيص الفواتير أو إعداد التطبيق باسم نشاطك.","After transferring, attach the payment receipt. You can also attach your company logo if you want customized invoices or setup under your business name."},{"الأسعار المعروضة سابقًا أسعار اختبار وليست نهائية. تأكد من السعر النهائي ومدة الخدمة مع الدعم قبل التحويل. إرسال الطلب لا يعني تفعيل الاشتراك تلقائيًا.","Previously shown prices are test prices, not final. Confirm the final price and term with support before transferring. Sending a request does not activate the subscription automatically."},{"مستويات الخدمة","Service tiers"},{"طلب خدمة — شركة محمد مصطفى الذكية / المحاسب الميزان","Service request — Mohammed Mustafa Smart Company / Al-Mizan Accountant"},{"الباقة: ","Plan: "},{"المدة: ","Duration: "},{"الشركة: ","Company: "},{"رقم التواصل: ","Contact number: "},{"التفاصيل: ","Details: "},{"يرجى مراجعة الطلب والتحويل قبل تفعيل الترخيص.","Please review the request and payment before activating the license."},{"اشتراك شهري","Monthly subscription"},{"اشتراك سنوي","Annual subscription"},{"ترخيص دائم","Perpetual license"},{"Enterprise","Enterprise"},{"Basic","Basic"},{"Pro","Pro"},{"Business","Business"},{"تجربة مجانية لمدة 7 أيام — للتقييم فقط. لا تُحذف بيانات الشركة عند انتهاء الترخيص.","7-day free trial for evaluation only. Company data must not be deleted when the license expires."},{"الاستفسار عن الباقات","Ask about plans"},{"الرئيسية","Home"},{"الاشتراك","Subscription"},{"💳 اشترك الآن — عرض الباقات والأسعار","💳 Subscribe Now — View Plans & Prices"},{"💳 اشترك الآن — أرسل طلب الاشتراك عبر واتساب","💳 Subscribe Now — Send Subscription Request via WhatsApp"},{"الدقة المحاسبية والتوازن المالي","Accounting Accuracy & Balance"},{"التواصل مع الدعم","Contact support"},{"شركة محمد مصطفى الذكية","Mohammed Mustafa Smart Company"},{"المحاسبة والمخزون — الإصدار 2.3.5","Accounting & Inventory — Version 2.3.5"},{"أنشئ رمز دخول خاصًا بك (4 أرقام على الأقل).","Create your PIN (at least 6 digits)."},{"رمز الدخول","PIN"},{"حفظ الرمز والدخول","Save PIN & Sign In"},{"الرمز قصير","PIN is too short"},{"رمز الدخول غير صحيح","Incorrect PIN"},{"دخول","Sign In"},{"لوحة التحكم","Dashboard"},{"مركز إدارة النشاط","Business Management Center"},{"الفواتير والمبيعات","Sales Invoices"},{"فاتورة بيع","Sales Invoice"},{"فاتورة شراء","Purchase Invoice"},{"المشتريات","Purchases"},{"الأصناف والمخزون والباركود","Products, Inventory & Barcode"},{"الأصناف والمخزون","Products & Inventory"},{"العملاء","Customers"},{"الموردون","Suppliers"},{"الصندوق والمصروفات","Cash & Expenses"},{"الحسابات البنكية","Bank Accounts"},{"سجل الشيكات","Cheque Register"},{"الموظفون","Employees"},{"التقارير والتحليل","Reports & Analysis"},{"النسخ الاحتياطي","Backup"},{"الدعم","Support"},{"المبيعات","Sales"},{"المصروفات","Expenses"},{"الباركود (امسح بالماسح أو اكتب الرقم)","Barcode (scan or enter manually)"},{"اسم الصنف","Product name"},{"الرصيد الافتتاحي","Opening stock"},{"سعر البيع","Sale price"},{"تكلفة الوحدة","Unit cost"},{"حد إعادة الطلب","Reorder level"},{"يمكن استخدام ماسح USB/Bluetooth أو كاميرا الجوال لمسح الباركود.","Use a USB/Bluetooth scanner or your phone camera to scan barcodes."},{"مسح بالكاميرا","Scan with camera"},{"وجّه الكاميرا نحو الباركود","Point the camera at the barcode"},{"إضافة الصنف","Add product"},{"أدخل اسم الصنف","Enter a product name"},{"تم حفظ الصنف","Product saved"},{"تعذر الحفظ: تأكد من عدم تكرار الاسم أو الباركود","Could not save: product name or barcode may already exist"},{"باركود","Barcode"},{"رصيد","Stock"},{"بيع","Sale"},{"تكلفة","Cost"},{"إعادة طلب","Reorder"},{"اسم البنك","Bank name"},{"اسم الحساب","Account name"},{"إضافة حساب بنكي","Add bank account"},{"أدخل اسم البنك والحساب","Enter bank and account names"},{"تمت إضافة الحساب","Bank account added"},{"تعذر إضافة الحساب؛ تحقق من البيانات","Could not add bank account; check the details"},{"تسجيل حركة بنكية","Record bank transaction"},{"اسم الحساب البنكي كما أُدخل","Bank account name (exact)"},{"المبلغ","Amount"},{"البيان","Description"},{"إيداع","Deposit"},{"سحب","Withdrawal"},{"حفظ الحركة","Save transaction"},{"أدخل مبلغًا صحيحًا","Enter a valid amount"},{"الحساب البنكي غير موجود","Bank account not found"},{"تم تسجيل الحركة","Transaction recorded"},{"الرصيد","Balance"},{"سجل الشيكات الواردة والصادرة ومواعيد استحقاقها","Track incoming/outgoing cheques and due dates"},{"شيك وارد","Incoming cheque"},{"شيك صادر","Outgoing cheque"},{"رقم الشيك","Cheque number"},{"اسم العميل أو المستفيد","Customer or payee"},{"قيمة الشيك","Cheque amount"},{"اللغة / Language","Language: العربية / English"},{"قيمة الشيك","Cheque amount"},{"الاسم","Name"},{" | المدفوع "," | Paid "},{" | المتبقي "," | Remaining "},{"الإجمالي: 0.00","Total: 0.00"},{" | الإجمالي "," | Total "},{"تاريخ الإصدار YYYY-MM-DD","Issue date YYYY-MM-DD"},{"تاريخ الاستحقاق YYYY-MM-DD","Due date YYYY-MM-DD"},{"تسجيل مصروف","Record expense"},{"تصدير نسخة قاعدة البيانات","Export database backup"},{"إدارة الحسابات وحركات الإيداع والسحب","Manage bank accounts, deposits and withdrawals"},{"قيمة الشيك","Cheque amount"},{"المدفوع","Paid"},{"الخصم","Discount"},{"تاريخ الاستحقاق YYYY-MM-DD","Due date YYYY-MM-DD"},{"قيد الانتظار","Pending"},{"مستحق","Due"},{"تم التحصيل/الصرف","Cleared"},{"مرتجع","Returned"},{"ملغى","Cancelled"},{"حفظ الشيك","Save cheque"},{"رقم الشيك والقيمة وتاريخ الاستحقاق مطلوبة","Cheque number, amount and due date are required"},{"تم حفظ الشيك","Cheque saved"},{"تعذر حفظ الشيك؛ ربما رقم الشيك مسجل مسبقًا","Could not save cheque; number may already exist"},{"تحديث حالة شيك محفوظ","Update saved cheque status"},{"رقم الشيك المراد تحديثه","Cheque number to update"},{"تحديث حالة الشيك","Update cheque status"},{"تم تحديث الحالة إن كان رقم الشيك موجودًا","Status updated if cheque number exists"},{"وارد","Incoming"},{"صادر","Outgoing"},{"شيك","Cheque"},{"الاستحقاق","Due"},{"الاسم","Name"},{"إضافة","Add"},{"تسجيل مصروف","Record expense"},{"تم تسجيل المصروف وترحيله محاسبيًا","Expense recorded and posted"},{"تصدير نسخة قاعدة البيانات","Export database backup"},{"يمكن تصدير النسخة واستعادتها. اختبر الاستعادة أولًا على نسخة تجريبية وتأكد من سلامة الأرصدة قبل اعتمادها.","Backup export and restore are available. Test restore with a sample copy and verify balances before relying on it."},{"تم تصدير النسخة الاحتياطية","Backup exported"},{"تعذر التصدير","Export failed"},{"ميزان المراجعة","Trial Balance"},{"مدين","Debit"},{"دائن","Credit"},{"إجمالي المدين","Total debit"},{"الفرق","Difference"},{"الصندوق","Cash"},{"المخزون","Inventory"},{"رأس المال","Capital"},{"تكلفة البضاعة المباعة","Cost of Goods Sold"},{"حساب الإجمالي","Calculate total"},{"الإجمالي","Total"},{"حفظ وترحيل","Save & Post"},{"الصنف غير موجود","Product not found"},{"الكمية","Quantity"},{"السعر","Price"},{"المدفوع","Paid"},{"الخصم","Discount"},{"الكمية غير صحيحة","Invalid quantity"},{"الرصيد غير كافٍ","Insufficient stock"},{"تم حفظ الفاتورة وترحيلها محاسبيًا","Invoice saved and posted"},{"المورد","Supplier"},{"العميل","Customer"},{"إدارة الحسابات وحركات الإيداع والسحب","Manage accounts, deposits and withdrawals"},{"حفظ الشيك","Save cheque"},{"الدعم عبر واتساب","WhatsApp support"},{"الحسابات الرئيسية","Main Accounts"},{"دليل الحسابات والأرصدة","Chart of accounts and balances"},{"إجمالي المدين","Total debits"},{"إجمالي الدائن","Total credits"},{"الرصيد الطبيعي","Normal balance"},{"دفتر الأستاذ","Account ledger"},{"لا توجد حركات مرحّلة لهذا الحساب.","No posted activity for this account."},{"الحسابات الرئيسية تجمع أرصدة القيود المحاسبية المرحّلة من وحدات البرنامج. أي حركة غير مرحّلة محاسبيًا لن تظهر هنا.","Main accounts summarize posted accounting entries from app modules. Transactions not posted to the ledger will not appear here."},{"متوازن","Balanced"},{"غير متوازن","Out of balance"},{"العودة إلى الحسابات الرئيسية","Back to main accounts"},{"فتح المبيعات","Open sales"},{"فتح المشتريات","Open purchases"},{"فتح المخزون","Open inventory"},{"فتح الصندوق والمصروفات","Open cash and expenses"},{"فتح الحسابات البنكية","Open bank accounts"},{"فتح القيود اليدوية","Open manual journals"},{"فتح التقارير","Open reports"},{"الدعم عبر البريد الإلكتروني","Email support"},{"رقم الدعم: +249 121 851 285","Support WhatsApp: +249 121 851 285"},{"البريد الإلكتروني: moh1442010@gmail.com","Email: moh1442010@gmail.com"},{"راسلنا عبر واتساب أو البريد الإلكتروني. لا ترسل كلمات المرور أو بياناتك المالية السرية.","Contact us by WhatsApp or email. Do not send passwords or sensitive financial data."},{"ملاحظة: لا يوجد نظام ترخيص تجاري متصل بخادم حتى الآن؛ لا تشترِ أو توزّع التطبيق على أنه يتضمن تفعيلًا مدفوعًا.","Note: Server-based commercial licensing is not implemented yet; do not sell or distribute the app as having paid activation."},{"اللغة / Language","Language: العربية / English"},{"الموظفون والرواتب","Employees & Payroll"},{"إضافة موظف واحتساب الراتب والسلف والخصومات الشهرية","Add employee and calculate monthly salary, advances and deductions"},{"استعادة نسخة قاعدة البيانات","Restore database backup"},{"سيتم استبدال بيانات التطبيق الحالية بالنسخة المختارة. هل أنشأت نسخة احتياطية حديثة؟","Current app data will be replaced with the selected backup. Have you made a recent backup?"},{"اختيار ملف","Choose file"},{"إلغاء","Cancel"},{"فشلت الاستعادة: الملف غير صالح أو تعذر قراءته","Restore failed: the file is invalid or could not be read"},{"تمت استعادة النسخة الاحتياطية؛ راجع أرصدتك وتقاريرك","Backup restored; please review balances and reports"},{"اسم الموظف","Employee name"},{"المسمى الوظيفي","Job title"},{"الراتب الشهري","Monthly salary"},{"إضافة موظف","Add employee"},{"تعذر حفظ الموظف","Could not save employee"},{"تسجيل سلفة أو خصم","Record advance or deduction"},{"سلفة","Advance"},{"خصم","Deduction"},{"حفظ الحركة","Save transaction"},{"المبلغ","Amount"},{"البيان","Description"},{"تم تسجيل الحركة","Transaction recorded"},{"صافي المستحق","Net payable"},{"القيود اليدوية","Manual Journal Entries"},{"إنشاء قيود يومية متوازنة","Create balanced journal entries"},{"وصف القيد / المرجع","Journal description / reference"},{"إضافة سطر للقيد","Add journal line"},{"اعتماد القيد اليدوي","Post manual journal"},{"آخر القيود اليدوية","Recent manual journals"},{"أدخل سطرين على الأقل. يجب أن يكون كل سطر مدينًا أو دائنًا فقط، وأن يتساوى إجمالي الطرفين. القيود المحفوظة لا تُحذف من هذه الشاشة.","Enter at least two lines. Each line must be debit or credit only, and totals must balance. Posted entries cannot be deleted here."},{"العربية","Arabic"},{"English","English"},{"تغيير رمز الدخول","Change PIN"},{"أدخل الرمز الحالي ثم اختر رمزًا جديدًا من 6 أرقام على الأقل. لا تشارك الرمز مع أحد.","Enter the current PIN, then choose a new 4-digit PIN. Do not share it."},{"رمز الدخول الحالي","Current PIN"},{"رمز جديد (6 أرقام على الأقل)","New PIN (at least 6 digits)"},{"تأكيد الرمز الجديد","Confirm new PIN"},{"حفظ رمز الدخول الجديد","Save new PIN"},{"رمز الدخول يجب أن يتكون من 6 أرقام على الأقل","PIN must be at least 6 digits"},{"تأكيد الرمز غير مطابق","PIN confirmation does not match"},{"تم تغيير رمز الدخول بنجاح","PIN changed successfully"},{"عملة واحدة لكل ملف شركة. تغيير العملة لا يحوّل الأرصدة القديمة تلقائيًا.","One currency per company profile. Changing currency does not automatically convert existing balances."}
  };
  Arrays.sort(pairs,(a,b)->Integer.compare(b[0].length(),a[0].length()));
  String out=value;
  for(String[] p:pairs)out=out.replace(p[0],p[1]);
  return out;
 }
 void toggleLanguage(){boolean next=!english();getSharedPreferences("settings",0).edit().putBoolean("english",next).apply();if(authenticated)dashboard();else login();}
 Button languageButton(){Button x=b(english()?"العربية":"ENGLISH");x.setOnClickListener(v->toggleLanguage());return x;}
 public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
  final Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
   try{
    String stack=android.util.Log.getStackTraceString(error);
    getSharedPreferences("startup_diagnostics",0).edit()
     .putString("last_failure_class",error.getClass().getName())
     .putString("last_failure_message",error.getMessage()==null?"(no message)":error.getMessage().substring(0,Math.min(240,error.getMessage().length())))
     .putString("last_failure_stack",stack.substring(0,Math.min(6000,stack.length())))
     .putLong("last_failure_at",System.currentTimeMillis()).commit();
   }catch(Throwable ignored){}
   if(previous!=null)previous.uncaughtException(thread,error);
  });
  try{
   db=new DB(this);
   // Initialize and migrate before login so schema failures are visible and diagnosable.
   db.getWritableDatabase();
  }catch(Throwable ex){
   android.util.Log.e("AlMizanStartup","Database initialization failed",ex);
   showStartupFailure(ex);
   return;
  }
  checkCommercialAccess();
 }
 void showStartupFailure(Throwable ex){
  String stack=android.util.Log.getStackTraceString(ex);
  getSharedPreferences("startup_diagnostics",0).edit()
   .putString("last_failure_class",ex.getClass().getName())
   .putString("last_failure_message",ex.getMessage()==null?"(no message)":ex.getMessage().substring(0,Math.min(240,ex.getMessage().length())))
   .putString("last_failure_stack",stack.substring(0,Math.min(6000,stack.length())))
   .putLong("last_failure_at",System.currentTimeMillis()).commit();
  page("تشخيص بدء التشغيل");
  content.addView(t("تعذر تهيئة قاعدة بيانات التطبيق. لم يحاول التطبيق حذف البيانات أو إعادة إنشاء قاعدة البيانات تلقائيًا.",16));
  content.addView(t("نوع الخطأ: "+ex.getClass().getName()+"\\nالتفاصيل: "+(ex.getMessage()==null?"(لا توجد رسالة)":ex.getMessage()),13));
  Button copy=b("نسخ تقرير الخطأ للدعم");
  content.addView(copy);
  copy.setOnClickListener(v->{String report=getSharedPreferences("startup_diagnostics",0).getString("last_failure_stack",stack);((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Al-Mizan startup diagnostic",report));toast("تم نسخ تقرير الخطأ؛ أرسله للدعم دون إضافة كلمات مرور");});
 }

 static final String LICENSE_API="https://smart-encyclopedias-delivery.moh1442010.workers.dev";
 static final long OFFLINE_GRACE_MS=72L*60L*60L*1000L;
 long parseLicenseTime(String value){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));java.util.Date d=f.parse(value);return d==null?0:d.getTime();}catch(Exception e){try{java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.US);f.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));java.util.Date d=f.parse(value);return d==null?0:d.getTime();}catch(Exception ignored){return 0;}}}
 String appDeviceId(){String id=android.provider.Settings.Secure.getString(getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);return id==null?"":id;}
 boolean hasEnterpriseEntitlement(android.content.SharedPreferences prefs){
   if(!"licensed".equals(prefs.getString("mode","")))return false;
   if(!"enterprise".equals(prefs.getString("plan","").trim().toLowerCase(Locale.ROOT)))return false;
   String termType=prefs.getString("term_type","").trim().toLowerCase(Locale.ROOT);
   long expiry=prefs.getLong("expires_at",0),verified=prefs.getLong("last_verified_at",0),now=System.currentTimeMillis();
   if("perpetual".equals(termType)){if(expiry!=0)return false;}
   else if("monthly".equals(termType)||"annual".equals(termType)){if(expiry<=now)return false;}
   else return false;
   return cachedLicenseAllowsOffline("licensed",expiry,verified,now);
  }
  boolean cachedLicenseAllowsOffline(String mode,long expiry,long verified,long now){
  if(verified<=0||now<verified||now-verified>OFFLINE_GRACE_MS)return false;
  if("licensed".equals(mode))return expiry==0||expiry>now;
  if("trial".equals(mode))return expiry>now;
  return false;
 }
 void checkCommercialAccess(){
  new Thread(()->{
   boolean allowed=false; String mode=""; String error="";
   try{
    java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(LICENSE_API+"/api/app-license/status").openConnection();
    c.setRequestMethod("GET");c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("x-app-device-id",appDeviceId());
    int code=c.getResponseCode();java.io.InputStream in=code>=200&&code<300?c.getInputStream():c.getErrorStream();
    java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();
    org.json.JSONObject d=new org.json.JSONObject(out.toString("UTF-8"));
    if(code>=200&&code<300&&d.optBoolean("ok")){
     mode=d.optString("mode","");long expiry=0;try{expiry=parseLicenseTime(d.optString("expiresAt",""));}catch(Exception ignored){}
     allowed=("licensed".equals(mode)||("trial".equals(mode)&&expiry>System.currentTimeMillis()));
     getSharedPreferences("commercial_license",0).edit().putString("mode",mode).putString("plan",d.optString("plan","").toLowerCase(Locale.ROOT)).putString("term_type",d.optString("termType","").toLowerCase(Locale.ROOT)).putLong("expires_at",expiry).putLong("last_verified_at",System.currentTimeMillis()).putString("license_id",d.optString("licenseId","")).apply();
     if(!allowed)error="انتهت التجربة المجانية. أدخل رمز الترخيص التجاري للمتابعة.";
    }else error="تعذر التحقق من الترخيص. حاول مرة أخرى.";
    c.disconnect();
   }catch(Exception ex){
    android.content.SharedPreferences p=getSharedPreferences("commercial_license",0);
    String cachedMode=p.getString("mode","");
    long expiry=p.getLong("expires_at",0),verified=p.getLong("last_verified_at",0);
    allowed=cachedLicenseAllowsOffline(cachedMode,expiry,verified,System.currentTimeMillis());
    error=allowed?"":"يلزم الاتصال بالإنترنت للتحقق من الترخيص أول مرة أو بعد انتهاء مهلة العمل دون اتصال.";
   }
   final boolean ok=allowed;final String message=error;
   runOnUiThread(()->{if(ok)login();else licensePage(message);});
  }).start();
 }
 void licensePage(String message){
  page("الاشتراك");content.addView(languageButton());TextView brandTitle=t(english()?"Al-Mizan Accountant":"⚖ المحاسب الميزان",26);brandTitle.setTextColor(Color.rgb(11,31,58));brandTitle.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(brandTitle);Button subscribe=b("💳 اشترك الآن — عرض الباقات والأسعار");subscribe.setTextSize(18);content.addView(subscribe);subscribe.setOnClickListener(v->plansPage());content.addView(t("تفعيل المحاسب الميزان",24));
  content.addView(t(message==null?"تحقق من الترخيص أو أدخل رمز التفعيل الذي حصلت عليه بعد اعتماد الشراء.":message,15));
  EditText code=e("رمز التفعيل التجاري");code.setSingleLine(true);content.addView(code);
  Button activate=b("تفعيل الترخيص");content.addView(activate);activate.setOnClickListener(v->{
   String activation=code.getText().toString().trim().toUpperCase(Locale.ROOT);
   if(activation.length()<12){toast("أدخل رمز تفعيل صحيحًا");return;}
   activate.setEnabled(false);
   new Thread(()->{
    String resultMessage="تعذر الاتصال بخادم الترخيص.";
    boolean ok=false;
    try{
     java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(LICENSE_API+"/api/app-license/activate").openConnection();
     c.setRequestMethod("POST");c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setDoOutput(true);
     c.setRequestProperty("Content-Type","application/json; charset=utf-8");c.setRequestProperty("x-app-device-id",appDeviceId());
     org.json.JSONObject body=new org.json.JSONObject();body.put("activationCode",activation);
     try(java.io.OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
     int status=c.getResponseCode();java.io.InputStream in=status>=200&&status<300?c.getInputStream():c.getErrorStream();
     java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);in.close();
     org.json.JSONObject d=new org.json.JSONObject(out.toString("UTF-8"));
     if(status>=200&&status<300&&d.optBoolean("ok")){
      long expiry=0;try{expiry=parseLicenseTime(d.optString("expiresAt",""));}catch(Exception ignored){}
      getSharedPreferences("commercial_license",0).edit().putString("mode","licensed").putString("plan",d.optString("plan","").toLowerCase(Locale.ROOT)).putString("term_type",d.optString("termType","").toLowerCase(Locale.ROOT)).putLong("expires_at",expiry).putLong("last_verified_at",System.currentTimeMillis()).putString("license_id",d.optString("licenseId","")).apply();ok=true;
     }else resultMessage="تعذر التفعيل: "+d.optString("error","تحقق من الرمز وحالته.");
     c.disconnect();
    }catch(Exception ex){}
    final boolean activated=ok;final String msg=resultMessage;
    runOnUiThread(()->{activate.setEnabled(true);if(activated){toast("تم تفعيل الترخيص بنجاح");login();}else{toast(msg);}});
   }).start();
  });
  Button retry=b("إعادة التحقق من الترخيص");content.addView(retry);retry.setOnClickListener(v->checkCommercialAccess());
  Button contact=b("التواصل مع الدعم");content.addView(contact);contact.setOnClickListener(v->support());
  content.addView(t("لا تحذف بيانات الشركة عند انتهاء التجربة أو الترخيص. يجب أن تبقى بياناتك قابلة للتصدير.",14));
 }
 TextView priceLine(String s){TextView v=t(s,19);v.setTextColor(Color.rgb(11,31,58));v.setTypeface(null,android.graphics.Typeface.BOLD);v.setBackgroundColor(Color.rgb(255,249,229));v.setPadding(18,18,18,18);v.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(8,5,8,5);v.setLayoutParams(p);return v;}
 TextView t(String s,int z){TextView v=new TextView(this);v.setText(tr(s));v.setTextSize(z);v.setPadding(18,14,18,14);return v;}
 Button b(String s){Button x=new Button(this);x.setText(tr(s));x.setAllCaps(false);return x;}
 EditText e(String h){EditText x=new EditText(this);x.setHint(tr(h));x.setPadding(18,8,18,8);return x;}
 void attachDatePicker(EditText field){
  field.setFocusable(false);field.setClickable(true);field.setLongClickable(true);
  field.setOnClickListener(v->{
   java.util.Calendar cal=java.util.Calendar.getInstance();
   String current=field.getText().toString().trim();
   if(current.matches("\\d{4}-\\d{2}-\\d{2}"))try{
    java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);sdf.setLenient(false);
    java.util.Date d=sdf.parse(current);if(d!=null)cal.setTime(d);
   }catch(Exception ignored){}
   new android.app.DatePickerDialog(this,(picker,year,month,day)->
    field.setText(String.format(Locale.US,"%04d-%02d-%02d",year,month+1,day)),
    cal.get(java.util.Calendar.YEAR),cal.get(java.util.Calendar.MONTH),cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
  });
  field.setOnLongClickListener(v->{field.setText("");return true;});
 }
 void page(String h){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(Color.rgb(248,249,250));LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(8,4,8,4);bar.setBackgroundColor(green);TextView q=t((english()?"Al-Mizan Accountant | ":"المحاسب الميزان | ")+tr(h),18);q.setTextColor(Color.WHITE);bar.addView(q,new LinearLayout.LayoutParams(0,-2,1));Button home=b("⬅ الرجوع للقائمة الرئيسية");home.setTextColor(Color.rgb(11,31,58));home.setTextSize(14);home.setAllCaps(false);home.setBackgroundColor(Color.rgb(212,175,55));home.setPadding(12,8,12,8);home.setOnClickListener(v->dashboard());bar.addView(home);r.addView(bar);ScrollView s=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);s.addView(content);r.addView(s,new LinearLayout.LayoutParams(-1,0,1));Button homeBottom=b("⬅ الرجوع إلى القائمة الرئيسية");homeBottom.setTextSize(18);homeBottom.setTextColor(Color.rgb(11,31,58));homeBottom.setAllCaps(false);homeBottom.setTypeface(null,1);homeBottom.setBackgroundColor(Color.rgb(212,175,55));homeBottom.setMinHeight((int)(58*getResources().getDisplayMetrics().density));homeBottom.setOnClickListener(v->dashboard());LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(12,8,12,10);r.addView(homeBottom,hp);setContentView(r);}
 boolean trial(){android.content.SharedPreferences p=getSharedPreferences("trial",0);long f=p.getLong("first",0);if(f==0){f=System.currentTimeMillis();p.edit().putLong("first",f).apply();}return System.currentTimeMillis()-f<7L*86400000L;}
 void lock(String msg){page("الاشتراك");content.addView(t(msg,20));Button w=b("التواصل مع الدعم");content.addView(w);w.setOnClickListener(v->support());content.addView(t("ملاحظة: لا يوجد نظام ترخيص تجاري متصل بخادم حتى الآن؛ لا تشترِ أو توزّع التطبيق على أنه يتضمن تفعيلًا مدفوعًا.",14));}
 void login(){needsRelogin=false;page("شركة محمد مصطفى الذكية");content.addView(t("المحاسبة والمخزون — الإصدار "+BuildConfig.VERSION_NAME,24));content.addView(languageButton());android.content.SharedPreferences p=getSharedPreferences("security",0);if(!p.contains("pin_hash")&&!p.contains("pin")){content.addView(t("أنشئ رمز دخول خاصًا بك لحماية بيانات شركتك. لا تستخدم رمزًا سهل التخمين.",16));EditText first=e("رمز جديد (6 أرقام على الأقل)");first.setInputType(2|0x10);content.addView(first);EditText confirm=e("أعد إدخال رمز الدخول");confirm.setInputType(2|0x10);content.addView(confirm);Button create=b("حفظ الرمز والدخول");content.addView(create);create.setOnClickListener(v->{String a=first.getText().toString(),z=confirm.getText().toString();if(a.length()<6){toast("رمز الدخول يجب أن يتكون من 6 أرقام على الأقل");return;}if(!a.matches("[0-9]+")){toast("استخدم أرقامًا فقط");return;}if(!a.equals(z)){toast("الرمزان غير متطابقين");return;}if(savePin(a,p)){authenticated=true;dashboard();}else toast("تعذر حفظ رمز الدخول بأمان");});return;}EditText x=e("رمز الدخول");x.setInputType(2|0x10);content.addView(x);Button sign=b("دخول");content.addView(sign);sign.setOnClickListener(v->{long remaining=pinLockRemainingMs(p);if(remaining>0){toast("تم إيقاف المحاولات مؤقتًا. انتظر "+Math.max(1,(remaining+999)/1000)+" ثانية");return;}if(verifyPin(x.getText().toString(),p)){clearPinFailures(p);authenticated=true;dashboard();}else{recordPinFailure(p);long wait=pinLockRemainingMs(p);toast(wait>0?"تجاوزت عدد المحاولات. أعد المحاولة بعد "+Math.max(1,(wait+999)/1000)+" ثانية":"رمز الدخول غير صحيح");}});}
 long pinLockRemainingMs(android.content.SharedPreferences prefs){return Math.max(0L,prefs.getLong("pin_lock_until",0L)-System.currentTimeMillis());}
 void recordPinFailure(android.content.SharedPreferences prefs){int attempts=prefs.getInt("pin_failures",0)+1;android.content.SharedPreferences.Editor e=prefs.edit().putInt("pin_failures",attempts);if(attempts>=5){int exponent=Math.min(5,attempts-5);long delay=Math.min(15L*60L*1000L,30000L*(1L<<exponent));e.putLong("pin_lock_until",System.currentTimeMillis()+delay);}e.commit();}
 void clearPinFailures(android.content.SharedPreferences prefs){prefs.edit().remove("pin_failures").remove("pin_lock_until").commit();}
 boolean savePin(String pin,android.content.SharedPreferences prefs){try{byte[] salt=new byte[16];new java.security.SecureRandom().nextBytes(salt);String saltText=android.util.Base64.encodeToString(salt,android.util.Base64.NO_WRAP);String hash=pinHash(pin,salt);prefs.edit().putString("pin_salt",saltText).putString("pin_hash",hash).remove("pin").commit();return prefs.contains("pin_hash")&&prefs.contains("pin_salt");}catch(Exception ex){toast("تعذر حفظ رمز الدخول بأمان");return false;}}
 boolean verifyPin(String pin,android.content.SharedPreferences prefs){try{if(prefs.contains("pin_hash")){byte[] salt=android.util.Base64.decode(prefs.getString("pin_salt",""),android.util.Base64.NO_WRAP);byte[] expected=android.util.Base64.decode(prefs.getString("pin_hash",""),android.util.Base64.NO_WRAP);byte[] actual=android.util.Base64.decode(pinHash(pin,salt),android.util.Base64.NO_WRAP);return java.security.MessageDigest.isEqual(expected,actual);}String legacy=prefs.getString("pin",null);if(legacy!=null&&legacy.equals(pin)){return savePin(pin,prefs);}return false;}catch(Exception ex){return false;}}
 String pinHash(String pin,byte[] salt)throws Exception{javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(pin.toCharArray(),salt,120000,256);try{return android.util.Base64.encodeToString(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded(),android.util.Base64.NO_WRAP);}finally{spec.clearPassword();}}
 String pinHash(String pin,String salt)throws Exception{return pinHash(pin,android.util.Base64.decode(salt,android.util.Base64.NO_WRAP));}
   void checkForAppUpdate(){
   toast("جارٍ فحص الإصدار المنشور…");
   new Thread(()->{
    java.net.HttpURLConnection connection=null;
    try{
     java.net.URL url=new java.net.URL("https://raw.githubusercontent.com/moh1442010-arch/smart-encyclopedias-platform/main/android-release-manifest.json");
     connection=(java.net.HttpURLConnection)url.openConnection();connection.setRequestMethod("GET");connection.setConnectTimeout(6000);connection.setReadTimeout(6000);connection.setUseCaches(false);connection.setRequestProperty("Cache-Control","no-cache");
     if(connection.getResponseCode()!=200)throw new java.io.IOException("manifest_http_"+connection.getResponseCode());
     java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();try(java.io.InputStream in=connection.getInputStream()){byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){bytes.write(buffer,0,n);if(bytes.size()>65536)throw new java.io.IOException("manifest_too_large");}}
     org.json.JSONObject manifest=new org.json.JSONObject(bytes.toString("UTF-8"));
     String packageId=manifest.optString("packageId","");int publishedCode=manifest.optInt("versionCode",-1);String publishedName=manifest.optString("versionName","");String status=manifest.optString("status","");String center=manifest.optString("updateCenterUrl","");
     if(!"com.mohammedmustafa.smartcompany".equals(packageId)||publishedCode<1||publishedName.isEmpty()||!center.startsWith("https://moh1442010-arch.github.io/smart-encyclopedias-platform/android-updates/"))throw new java.io.IOException("invalid_release_manifest");
     runOnUiThread(()->{
      if(publishedCode>BuildConfig.VERSION_CODE){
       new AlertDialog.Builder(this).setTitle(tr("يتوفر إصدار أحدث. هل تريد فتح مركز التحديث والتحقق؟")).setMessage(tr("الإصدار المثبت: ")+BuildConfig.VERSION_NAME+" ("+BuildConfig.VERSION_CODE+")\n"+tr("الإصدار المنشور: ")+publishedName+" ("+publishedCode+")\n"+tr("حالة الإصدار: ")+status+"\n\n"+tr("لن يبدأ التنزيل تلقائيًا. تحقق من بصمة الملف والتوقيع قبل التثبيت.")).setNegativeButton(tr("إلغاء"),(d,w)->{}).setPositiveButton(tr("فتح مركز التحديثات"),(d,w)->open(center)).show();
      }else if(publishedCode==BuildConfig.VERSION_CODE){toast("أنت تستخدم أحدث إصدار منشور للمراجعة.");}
      else{toast("تعذر التحقق من بيانات الإصدار؛ لم يتم تنزيل أي ملف.");}
     });
    }catch(Exception ex){runOnUiThread(()->toast("تعذر الاتصال بمركز التحديثات. تحقق من الإنترنت ثم أعد المحاولة."));}
    finally{if(connection!=null)connection.disconnect();}
   }).start();
  }
  void dashboard(){
  try {
  android.content.SharedPreferences lic=getSharedPreferences("commercial_license",0);long licenseExpiry=lic.getLong("expires_at",0);if("trial".equals(lic.getString("mode",""))&&licenseExpiry>0&&licenseExpiry<=System.currentTimeMillis()){licensePage("انتهت التجربة المجانية. أدخل رمز الترخيص التجاري للمتابعة.");return;}
  page("لوحة التحكم");content.setPadding(10,10,10,16);content.addView(languageButton());
  TextView brandTitle=t(english()?"Al-Mizan Accountant":"⚖ المحاسب الميزان",28);brandTitle.setTextColor(Color.rgb(11,31,58));brandTitle.setTypeface(null,android.graphics.Typeface.BOLD);brandTitle.setGravity(Gravity.CENTER);content.addView(brandTitle,new LinearLayout.LayoutParams(-1,-2));
  TextView englishBrand=t(english()?"Accounting with Accuracy and Balance":"الدقة المحاسبية والتوازن المالي",16);englishBrand.setTextColor(Color.rgb(100,116,139));englishBrand.setGravity(Gravity.CENTER);content.addView(englishBrand,new LinearLayout.LayoutParams(-1,-2));
  TextView heading=t("مركز إدارة النشاط",24);heading.setTextColor(green);content.addView(heading);
  Button plansButton=b("💼 الباقات والأسعار");plansButton.setTextSize(18);content.addView(plansButton,new LinearLayout.LayoutParams(-1,-2));plansButton.setOnClickListener(v->plansPage());
   Button updateButton=b("🔄 فحص التحديثات");updateButton.setTextSize(18);content.addView(updateButton,new LinearLayout.LayoutParams(-1,-2));updateButton.setOnClickListener(v->checkForAppUpdate());
  Button guideButton=b("📘 الدليل الإرشادي والأسئلة الشائعة");guideButton.setTextSize(18);content.addView(guideButton,new LinearLayout.LayoutParams(-1,-2));guideButton.setOnClickListener(v->openUserGuide());
  TextView summaryBox=t(safeDashboardSummary(),15);summaryBox.setBackgroundColor(Color.rgb(232,242,236));content.addView(summaryBox);
  Button recurringButton=b("🧾 المصروفات الدورية والإيجار والكهرباء");content.addView(recurringButton,new LinearLayout.LayoutParams(-1,-2));recurringButton.setOnClickListener(v->recurringExpenses());
  Button assetsButton=b("🏢 سجل الأصول الثابتة والإهلاك");content.addView(assetsButton,new LinearLayout.LayoutParams(-1,-2));assetsButton.setOnClickListener(v->fixedAssets());
  if(hasEnterpriseEntitlement(getSharedPreferences("commercial_license",0))){Button controlCenter=b("🛡️ مركز الرقابة والإقفال المالي — Enterprise");controlCenter.setTextSize(17);content.addView(controlCenter,new LinearLayout.LayoutParams(-1,-2));controlCenter.setOnClickListener(v->enterpriseControlCenter());}
  String[][] items={
   {"🧾","نقطة البيع والمبيعات","إنشاء فواتير البيع وتحصيل المبالغ"},{"🛒","المشتريات","فواتير الشراء وتحديث تكلفة المخزون"},{"🔄","المرتجعات","عكس فاتورة بيع أو شراء كاملة مرة واحدة"},
   {"📦","الأصناف والمخزون","الأرصدة والأسعار وحركات المخزون"},{"👥","العملاء والموردون","العملاء والموردون والأرصدة"},
   {"💳","الخزينة والشيكات","الصندوق والبنوك والشيكات والتحصيلات"},{"📚","المحاسبة والتقارير","دليل الحسابات والقيود ودفتر الأستاذ والتقارير"},
   {"👔","الموظفون والرواتب","الموظفون والرواتب والسلف والخصومات"},{"🏭","حساب التشغيل والتصنيع المرحلي","أوامر التشغيل وتكاليف المراحل ومخزون تحت التشغيل"},{"🪟","الكاش — بيع الشباك","مبيعات المحل النقدية وخصم الكمية من المخزون"},{"🚚","التوزيع","المطاعم والبقالات: العدد والاسم والمبلغ والنقد والديون وفرق السعر"},{"↩️","القيد العكسي","عكس القيد اليدوي المؤهل مع حفظ القيد الأصلي"},{"⚙️","أدوات النظام","النسخ الاحتياطي وإعدادات الشركة والدعم"}
  };
  LinearLayout row=null;
  for(int i=0;i<items.length;i++){
   if(i%2==0){row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);content.addView(row,new LinearLayout.LayoutParams(-1,-2));}
   final String title=items[i][1];
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);card.setPadding(8,14,8,14);card.setBackgroundColor(Color.WHITE);card.setElevation(3f);
   TextView icon=t(items[i][0],27);icon.setGravity(Gravity.CENTER);icon.setPadding(0,0,0,3);card.addView(icon,new LinearLayout.LayoutParams(-1,-2));
   TextView label=t(title,16);label.setGravity(Gravity.CENTER);label.setTextColor(green);card.addView(label,new LinearLayout.LayoutParams(-1,-2));
   TextView detail=t(items[i][2],12);detail.setGravity(Gravity.CENTER);detail.setTextColor(Color.DKGRAY);card.addView(detail,new LinearLayout.LayoutParams(-1,-2));
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);lp.setMargins(6,6,6,6);row.addView(card,lp);card.setOnClickListener(v->openSection(title));
  }
  } catch (Throwable ex) {
   android.util.Log.e("AlMizanStartup", "Dashboard rendering failed after authentication", ex);
   String failureStack = android.util.Log.getStackTraceString(ex);
   try {
    getSharedPreferences("startup_diagnostics", 0).edit()
     .putString("last_failure_class", ex.getClass().getName())
     .putString("last_failure_message", ex.getMessage() == null ? "(no message)" : ex.getMessage().substring(0, Math.min(240, ex.getMessage().length())))
     .putString("last_failure_stack", failureStack.substring(0, Math.min(3500, failureStack.length())))
     .putLong("last_failure_at", System.currentTimeMillis()).commit();
   } catch (Throwable diagnosticFailure) {
    android.util.Log.e("AlMizanStartup", "Could not persist dashboard failure diagnostics", diagnosticFailure);
   }
   authenticated = false;
   try {
    page("تعذر فتح لوحة التحكم");
    content.addView(t("لم تُحذف قاعدة البيانات أو بيانات الشركة. سجّل تفاصيل الخطأ أدناه لإرسالها للدعم، ولا تحذف التطبيق.", 16));
    android.content.SharedPreferences diag = getSharedPreferences("startup_diagnostics", 0);
    content.addView(t("نوع الخطأ: " + diag.getString("last_failure_class", ex.getClass().getName()), 14));
    content.addView(t("التفاصيل: " + diag.getString("last_failure_message", ex.getMessage() == null ? "(no message)" : ex.getMessage()), 14));
    Button copy = b("نسخ تفاصيل التشخيص");
    content.addView(copy);
    copy.setOnClickListener(v -> {
     String report = "Al-Mizan dashboard failure\\nVersion: " + BuildConfig.VERSION_NAME +
      "\\nClass: " + diag.getString("last_failure_class", "unknown") +
      "\\nMessage: " + diag.getString("last_failure_message", "unknown") +
      "\\nStack: " + diag.getString("last_failure_stack", "");
     ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(android.content.ClipData.newPlainText("Al-Mizan diagnostics", report));
     toast("تم نسخ التشخيص؛ أرسله للدعم");
    });
    Button retry = b("إعادة محاولة فتح لوحة التحكم");
    content.addView(retry);
    retry.setOnClickListener(v -> dashboard());
   } catch (Throwable recoveryFailure) {
    android.util.Log.e("AlMizanStartup", "Dashboard recovery UI failed", recoveryFailure);
    Toast.makeText(this, "تعذر فتح لوحة التحكم. لم تُحذف البيانات؛ أعد فتح التطبيق وأرسل سجل الأعطال للدعم.", Toast.LENGTH_LONG).show();
   }
  }
 }
 String safeDashboardSummary() {
  try { return summary(); }
  catch (Throwable ex) {
   android.util.Log.e("AlMizanStartup", "Dashboard summary query failed", ex);
   try {
    getSharedPreferences("startup_diagnostics", 0).edit()
     .putString("last_failure_class", ex.getClass().getName())
     .putString("last_failure_message", ex.getMessage() == null ? "(no message)" : ex.getMessage().substring(0, Math.min(240, ex.getMessage().length())))
     .putString("last_failure_stack", android.util.Log.getStackTraceString(ex).substring(0, Math.min(3500, android.util.Log.getStackTraceString(ex).length())))
     .putLong("last_failure_at", System.currentTimeMillis()).commit();
   } catch (Throwable ignored) {}
   return "تعذر تحميل ملخص الأرصدة فقط؛ بقية لوحة التحكم متاحة. راجع سجل التشخيص قبل اعتماد البيانات.";
  }
 }
 int countInvalidJournalEntries(SQLiteDatabase sq){
  Cursor c=sq.rawQuery("SELECT COUNT(*) FROM (SELECT e.id FROM journal_entries e LEFT JOIN journal_lines l ON l.journal_entry_id=e.id GROUP BY e.id HAVING COUNT(l.id)<2 OR ABS(COALESCE(SUM(l.debit),0)-COALESCE(SUM(l.credit),0))>0.005)",null);
  try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
 }
 int countOrphanJournalLines(SQLiteDatabase sq){
  Cursor c=sq.rawQuery("SELECT COUNT(*) FROM journal_lines l LEFT JOIN journal_entries e ON e.id=l.journal_entry_id LEFT JOIN accounts a ON a.id=l.account_id WHERE e.id IS NULL OR a.id IS NULL",null);
  try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}
 }
 void openUserGuide(){
   page("الدليل الإرشادي والأسئلة الشائعة");
   content.addView(t("الدليل الكامل والأسئلة الشائعة يعملان محليًا دون إنترنت. راجع حدود الوحدات التجريبية واستخدم بيانات اختبار فقط.",14));
   WebView guide=new WebView(this);
   guide.getSettings().setJavaScriptEnabled(true);
   guide.getSettings().setAllowFileAccess(true);
   guide.setWebViewClient(new WebViewClient());
   guide.loadUrl("file:///android_asset/guide/index.html");
   content.addView(guide,new LinearLayout.LayoutParams(-1,(int)(650*getResources().getDisplayMetrics().density)));
  }
 void enterpriseControlCenter(){
   page("مركز الرقابة والإقفال المالي — Enterprise");
   content.addView(t("مركز رقابي للقراءة فقط: يعرض مؤشرات ومخاطر تحتاج مراجعة، ولا يعدّل القيود أو الأرصدة تلقائيًا.",15));
   content.addView(t("تنبيه: هذه نسخة أولية للمراجعة. مؤشر الذمم يستخدم عمر الحركة (30 يومًا) لعدم وجود تاريخ استحقاق مستقل، ولا يُعد تقرير أعمار ديون معتمدًا.",13));
   SQLiteDatabase sq=db.getReadableDatabase();
   try{
    Cursor j=sq.rawQuery("SELECT COALESCE(SUM(debit),0),COALESCE(SUM(credit),0) FROM journal_lines",null);
    double debit=0,credit=0;if(j.moveToFirst()){debit=j.getDouble(0);credit=j.getDouble(1);}j.close();
    double difference=debit-credit;
    content.addView(t("1) توازن إجمالي دفتر القيود: "+(Math.abs(difference)<=0.005?"متوازن حسابيًا":"يحتاج مراجعة"),18));
    content.addView(t("إجمالي المدين: "+fmt(debit)+" | إجمالي الدائن: "+fmt(credit)+" | الفرق: "+fmt(difference),14));
    int bad=countInvalidJournalEntries(sq);
    content.addView(t("2) قيود ناقصة الأسطر أو غير متوازنة: "+bad+(bad==0?" — لم تظهر قيود ناقصة أو غير متوازنة":" — راجع القيود التي لا تحتوي سطرين على الأقل أو لا يتساوى مدينها ودائنها"),17));
    int orphanLines=countOrphanJournalLines(sq);
    content.addView(t("3) أسطر قيود بلا قيد رئيسي أو حساب صالح: "+orphanLines,17));
    Cursor missing=sq.rawQuery("SELECT COUNT(*) FROM transactions t WHERE t.type IN ('SALE','PURCHASE','EXPENSE','SALE_RETURN','PURCHASE_RETURN') AND NOT EXISTS (SELECT 1 FROM journal_entries j WHERE j.transaction_id=t.id)",null);
    int unposted=missing.moveToFirst()?missing.getInt(0):0;missing.close();
    content.addView(t("4) حركات بيع/شراء/مصروف/مرتجع بلا قيد مرتبط: "+unposted,17));
    long cutoff=System.currentTimeMillis()-30L*24L*60L*60L*1000L;
    Cursor receivables=sq.rawQuery("SELECT COUNT(*),COALESCE(SUM(remaining),0) FROM transactions WHERE type='SALE' AND remaining>0 AND created_at<?",new String[]{String.valueOf(cutoff)});
    int oldCount=0;double oldBalance=0;if(receivables.moveToFirst()){oldCount=receivables.getInt(0);oldBalance=receivables.getDouble(1);}receivables.close();
    content.addView(t("5) ذمم مبيعات متبقية مضى على حركتها أكثر من 30 يومًا: "+oldCount+" | الرصيد: "+fmt(oldBalance),17));
    Cursor stock=sq.rawQuery("SELECT COUNT(*) FROM products WHERE qty<=reorder",null);
    int low=stock.moveToFirst()?stock.getInt(0):0;stock.close();
    content.addView(t("6) أصناف عند حد إعادة الطلب أو دونه: "+low,17));
    content.addView(t("7) النسخ الاحتياطي: لا يُسجّل التطبيق بعد تحققًا آليًا من آخر نسخة؛ أنشئ نسخة مشفرة واختبر استعادتها في بيئة تجريبية.",14));
    Cursor assets=sq.rawQuery("SELECT COUNT(*),COALESCE(SUM(cost),0),COALESCE(SUM((SELECT COALESCE(SUM(amount),0) FROM asset_depreciation d WHERE d.asset_id=a.id)),0) FROM fixed_assets a",null);if(assets.moveToFirst())content.addView(t("8) الأصول الثابتة: "+assets.getInt(0)+" أصلًا | التكلفة "+fmt(assets.getDouble(1))+" | مجمع الإهلاك "+fmt(assets.getDouble(2)),15));assets.close();
    Cursor wip=sq.rawQuery("SELECT COALESCE(SUM(l.debit-l.credit),0) FROM journal_lines l JOIN accounts a ON a.id=l.account_id WHERE a.code='1400'",null);double wipValue=wip.moveToFirst()?wip.getDouble(0):0;wip.close();content.addView(t("9) صافي حساب مخزون تحت التشغيل: "+fmt(wipValue)+" — طابقه مع أوامر التشغيل ومخرجات المصنع.",15));
    Cursor stages=sq.rawQuery("SELECT COUNT(*) FROM factory_stage_events WHERE journal_entry_id IS NULL AND material_cost+labor_cost+overhead_cost>0",null);int missingStages=stages.moveToFirst()?stages.getInt(0):0;stages.close();content.addView(t("10) مراحل تشغيل بتكاليف دون قيد: "+missingStages,15));
    Cursor recurring=sq.rawQuery("SELECT COUNT(*) FROM recurring_expenses WHERE active=1 AND next_due<=?",new String[]{new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date())});int dueRecurring=recurring.moveToFirst()?recurring.getInt(0):0;recurring.close();content.addView(t("11) تعريفات مصروف دوري مستحقة حسب التاريخ: "+dueRecurring,15));
    content.addView(t("قائمة إقفال الفترة: راجع القيود غير المتوازنة، الحركات غير المرحلة، الصندوق والبنوك، المخزون، الذمم، ثم أنشئ نسخة مشفرة قبل اعتماد الإقفال. لا يوجد قفل فترة آلي في هذه النسخة الأولية.",14));
    Button accounts=b("📚 مراجعة القيود وميزان المراجعة");content.addView(accounts);accounts.setOnClickListener(v->mainAccounts());
    Button stockButton=b("📦 مراجعة الأصناف والمخزون");content.addView(stockButton);stockButton.setOnClickListener(v->products());
    Button debtButton=b("👥 مراجعة العملاء والذمم");content.addView(debtButton);debtButton.setOnClickListener(v->party("customers","العملاء"));
    Button backupButton=b("🔐 النسخ الاحتياطي والاستعادة");content.addView(backupButton);backupButton.setOnClickListener(v->backup());
   }catch(Exception ex){
    content.addView(t("تعذر إكمال بعض فحوص الرقابة. لم تُجرَ أي تعديلات على البيانات؛ راجع قاعدة البيانات وسجل الأخطاء قبل اعتماد الإقفال.",15));
   }
  }
  void plansPage(){
   page("الباقات والأسعار");
   content.addView(t("باقات المحاسب الميزان — الأسعار المعتمدة للمراجعة الفنية",21));
   content.addView(t("تجربة مجانية لمدة 7 أيام. هذه الأسعار وفق قائمة الباقات المعتمدة سابقًا. النسخة الحالية مخصصة للمراجعة الفنية فقط؛ لا تحوّل أي مبلغ ولا تعتبر طلب واتساب تفعيلًا للترخيص قبل اعتماد الإصدار وفتح البيع رسميًا.",14));
   content.addView(t("الترخيص الدائم لا يشمل التحديثات المستقبلية. الأسعار المعروضة نهائية حسب قرار التسعير، لكن البيع والتفعيل لا يفتحان إلا بعد اجتياز اختبارات الإصدار واعتماد النشر.",14));
   content.addView(t("🇸🇩 السودان — الجنيه السوداني (SDG) | بنكك",18));
   content.addView(priceLine("Basic: دائم 150,000 ج.س | شهري 25,000 ج.س | سنوي 250,000 ج.س"));
   content.addView(priceLine("Pro: دائم 300,000 ج.س | شهري 45,000 ج.س | سنوي 450,000 ج.س"));
   content.addView(priceLine("Business: دائم 600,000 ج.س | شهري 80,000 ج.س | سنوي 800,000 ج.س"));
   content.addView(priceLine("Enterprise: دائم 1,200,000 ج.س | شهري 150,000 ج.س | سنوي 1,500,000 ج.س"));
    content.addView(t("🌐 الدفع الدولي — الدولار الأمريكي (USD) | IBAN",18));
   content.addView(priceLine("Basic: دائم $249 | شهري $9 | سنوي $90"));
   content.addView(priceLine("Pro: دائم $499 | شهري $19 | سنوي $190"));
   content.addView(priceLine("Business: دائم $999 | شهري $39 | سنوي $390"));
    content.addView(priceLine("Enterprise: دائم $1,999 | شهري $79 | سنوي $790"));
   Spinner market=new Spinner(this);
   String[] markets={"السودان — جنيه سوداني (بنكك)","الدفع بالدولار الأمريكي — IBAN"};
   market.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,markets));
   content.addView(t("عملة وطريقة الدفع",15));content.addView(market);
   Spinner tier=new Spinner(this);String[] tiers={"Basic","Pro","Business","Enterprise"};
   tier.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,tiers));
   content.addView(t("الباقة",15));content.addView(tier);
   Spinner period=new Spinner(this);String[] periods={"اشتراك شهري","اشتراك سنوي","ترخيص دائم"};
   period.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,periods));
   content.addView(t("مدة الاشتراك أو الترخيص",15));content.addView(period);
   TextView selectedPrice=t("السعر المختار: 25,000 ج.س شهريًا",21);selectedPrice.setTextColor(green);content.addView(selectedPrice);
   Runnable updatePrice=()->{
    String m=String.valueOf(market.getSelectedItem()),p=String.valueOf(tier.getSelectedItem()),d=String.valueOf(period.getSelectedItem());
    int pi=p.equals("Basic")?0:p.equals("Pro")?1:p.equals("Business")?2:3;
    int di=d.equals("ترخيص دائم")?0:d.equals("اشتراك شهري")?1:2;
    String price;
    if(m.startsWith("السودان")){
     String[][] prices={{"150,000","25,000","250,000"},{"300,000","45,000","450,000"},{"600,000","80,000","800,000"},{"1,200,000","150,000","1,500,000"}};
     price=prices[pi][di]+" ج.س"+(di==0?" — ترخيص دائم":di==1?" شهريًا":" سنويًا");
    }else{
     String[][] prices={{"249","9","90"},{"499","19","190"},{"999","39","390"},{"1,999","79","790"}};
     price="$"+prices[pi][di]+(di==0?" — ترخيص دائم":di==1?" شهريًا":" سنويًا");
    }
    selectedPrice.setText("السعر المختار: "+price);
   };
   android.widget.AdapterView.OnItemSelectedListener priceListener=new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){updatePrice.run();}public void onNothingSelected(android.widget.AdapterView<?> parent){}};
   market.setOnItemSelectedListener(priceListener);tier.setOnItemSelectedListener(priceListener);period.setOnItemSelectedListener(priceListener);
   EditText company=e("اسم الشركة أو النشاط");company.setInputType(1);content.addView(company);
   EditText contact=e("رقم واتساب للتواصل");contact.setInputType(3);content.addView(contact);
   EditText notes=e("تفاصيل الخدمة المطلوبة أو عدد المستخدمين والفروع");notes.setMinLines(2);notes.setGravity(Gravity.TOP);content.addView(notes);
   content.addView(t("بيانات الدفع",19));
   content.addView(t("بنكك — رقم الحساب: 1882224 (للجنيه السوداني فقط)",18));
   Button copyBankak=b("📋 نسخ رقم حساب بنكك");content.addView(copyBankak);copyBankak.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Bankak account","1882224"));toast("تم نسخ رقم حساب بنكك");});
   content.addView(t("IBAN للدفع بالدولار الأمريكي: SD8504018822240001",14));
   Button copyIban=b("📋 نسخ رقم IBAN");content.addView(copyIban);copyIban.setOnClickListener(v->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("IBAN","SD8504018822240001"));toast("تم نسخ رقم IBAN");});
   content.addView(t("هذه نسخة للمراجعة الفنية وليست إعلان فتح البيع. لا تحوّل أي مبلغ قبل اعتماد الإصدار وتأكيد الطلب رسميًا.",14));
   Button send=b("💬 إرسال طلب/استفسار عبر واتساب");content.addView(send);send.setOnClickListener(v->{
    String m=String.valueOf(market.getSelectedItem()),p=String.valueOf(tier.getSelectedItem()),d=String.valueOf(period.getSelectedItem()),price=selectedPrice.getText().toString();
    if(company.getText().toString().trim().isEmpty()){toast("أدخل اسم الشركة أو النشاط أولًا");return;}
    String message="السلام عليكم، أريد الاستفسار عن باقة المحاسب الميزان.\nالشركة: "+company.getText().toString().trim()+"\nالعملة/طريقة الدفع: "+m+"\nالباقة: "+p+"\nالمدة: "+d+"\nالسعر المعروض: "+price+"\nرقم التواصل: "+contact.getText().toString().trim()+"\nالتفاصيل: "+notes.getText().toString().trim()+"\nبنكك (SDG): 1882224\nIBAN (USD): SD8504018822240001\nالطلب للاستفسار فقط؛ لا يتم التفعيل إلا بعد اعتماد الإصدار وتأكيد الدفع رسميًا.";
    open("https://wa.me/249121851285?text="+android.net.Uri.encode(message));
   });
   Button supportButton=b("التواصل مع الدعم عبر واتساب");content.addView(supportButton);supportButton.setOnClickListener(v->support());
  }
 void groupMenu(String title,String[][] links){
  page(title);content.addView(t("اختر الوظيفة المطلوبة",16));
  for(String[] link:links){Button go=b(link[0]);go.setTextSize(17);content.addView(go,new LinearLayout.LayoutParams(-1,-2));go.setOnClickListener(v->openSection(link[1]));}
 }
 void mainAccounts(){
  page("الحسابات الرئيسية");
  content.addView(t("دليل الحسابات والأرصدة",22));
  content.addView(t("الحسابات الرئيسية تجمع أرصدة القيود المحاسبية المرحّلة من وحدات البرنامج. أي حركة غير مرحّلة محاسبيًا لن تظهر هنا.",14));
  String[][] links={{"فتح المبيعات","الفواتير والمبيعات"},{"فتح المشتريات","المشتريات"},{"فتح المخزون","الأصناف والمخزون"},{"🏭 حساب التشغيل والتصنيع المرحلي","حساب التشغيل والتصنيع المرحلي"},{"فتح الصندوق والمصروفات","الصندوق والمصروفات"},{"فتح الحسابات البنكية","الحسابات البنكية"},{"فتح القيود اليدوية","القيود اليدوية"},{"↩️ القيد العكسي","القيد العكسي"},{"فتح التقارير","التقارير والتحليل"}};
  for(String[] link:links){Button go=b(link[0]);content.addView(go);go.setOnClickListener(v->openSection(link[1]));}
  Cursor totals=db.q("SELECT COALESCE(SUM(debit),0),COALESCE(SUM(credit),0) FROM journal_lines");
  double totalDebit=0,totalCredit=0;if(totals.moveToFirst()){totalDebit=totals.getDouble(0);totalCredit=totals.getDouble(1);}totals.close();
  content.addView(t("إجمالي المدين: "+fmt(totalDebit)+" | إجمالي الدائن: "+fmt(totalCredit),16));
  double difference=totalDebit-totalCredit;
  content.addView(t("حالة القيود: "+(Math.abs(difference)<=0.005?"متوازن":"غير متوازن")+" | الفرق: "+fmt(difference),16));
  content.addView(t("اضغط على أي حساب لفتح دفتر الأستاذ وتفاصيل الحركات المرحّلة.",14));
  Cursor accounts=db.q("SELECT a.id,a.code,a.name,a.type,COALESCE(SUM(l.debit),0),COALESCE(SUM(l.credit),0) FROM accounts a LEFT JOIN journal_lines l ON l.account_id=a.id GROUP BY a.id,a.code,a.name,a.type ORDER BY a.code");
  while(accounts.moveToNext()){
   final long id=accounts.getLong(0);String code=accounts.getString(1),name=accounts.getString(2),type=accounts.getString(3);double dr=accounts.getDouble(4),cr=accounts.getDouble(5);
   double balance=("LIABILITY".equals(type)||"EQUITY".equals(type)||"REVENUE".equals(type))?cr-dr:dr-cr;
   Button item=b(code+" — "+name+"\nمدين: "+fmt(dr)+" | دائن: "+fmt(cr)+"\nالرصيد الطبيعي: "+fmt(balance));content.addView(item);item.setOnClickListener(v->accountLedger(id,code,name));
  }
  accounts.close();Button trial=b("فتح ميزان المراجعة");content.addView(trial);trial.setOnClickListener(v->trialBalance());
 }
 void accountLedger(long accountId,String code,String name){
  page("دفتر الأستاذ — "+code);content.addView(t(name,21));
  Button back=b("العودة إلى الحسابات الرئيسية");content.addView(back);back.setOnClickListener(v->mainAccounts());
  Cursor entries=db.q("SELECT j.id,j.created_at,j.description,j.transaction_id,l.debit,l.credit FROM journal_lines l JOIN journal_entries j ON j.id=l.journal_entry_id WHERE l.account_id=? ORDER BY j.created_at DESC,j.id DESC,l.id DESC",String.valueOf(accountId));
  double debit=0,credit=0;int count=0;
  while(entries.moveToNext()){
   count++;long entryId=entries.getLong(0),created=entries.getLong(1);String description=entries.getString(2);long transactionId=entries.isNull(3)?0:entries.getLong(3);double dr=entries.getDouble(4),cr=entries.getDouble(5);debit+=dr;credit+=cr;
   String date=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date(created));
   content.addView(t(date+" | قيد #"+entryId+(transactionId>0?" | حركة #"+transactionId:"")+"\n"+(description==null||description.trim().isEmpty()?"قيد مرحّل":description)+"\nمدين: "+fmt(dr)+" | دائن: "+fmt(cr),14));
  }
  entries.close();if(count==0)content.addView(t("لا توجد حركات مرحّلة لهذا الحساب.",15));
  content.addView(t("إجمالي المدين: "+fmt(debit)+" | إجمالي الدائن: "+fmt(credit)+" | صافي مدين الحساب: "+fmt(debit-credit),16));
 }
 void changePin(){page("تغيير رمز الدخول");content.addView(t("أدخل الرمز الحالي ثم اختر رمزًا جديدًا من 6 أرقام على الأقل. لا تشارك الرمز مع أحد.",15));EditText oldPin=e("رمز الدخول الحالي"),newPin=e("رمز جديد (6 أرقام على الأقل)"),confirmPin=e("تأكيد الرمز الجديد");for(EditText x:new EditText[]{oldPin,newPin,confirmPin}){x.setInputType(2|0x10);content.addView(x);}Button save=b("حفظ رمز الدخول الجديد");content.addView(save);save.setOnClickListener(v->{android.content.SharedPreferences p=getSharedPreferences("security",0);String old=oldPin.getText().toString(),next=newPin.getText().toString(),confirm=confirmPin.getText().toString();if(!verifyPin(old,p)){toast("رمز الدخول الحالي غير صحيح");return;}if(next.length()<6||!next.matches("[0-9]+")){toast("رمز الدخول يجب أن يتكون من 6 أرقام على الأقل");return;}if(!next.equals(confirm)){toast("تأكيد الرمز غير مطابق");return;}if(savePin(next,p)){toast("تم تغيير رمز الدخول بنجاح");dashboard();}});}
 void companySettings(){
  page("إعدادات الشركة");
  android.content.SharedPreferences p=getSharedPreferences("company_profile",0);
  EditText name=e("اسم الشركة");name.setText(p.getString("name","شركة محمد مصطفى الذكية"));content.addView(name);
  String[] currencyCodes={"SDG","SAR","AED","EGP","QAR","KWD","BHD","OMR","JOD","IQD","MAD","TND","DZD","LYD","YER","LBP","SYP","MRU","SOS","DJF","USD","EUR","TRY","GBP","CNY","JPY","INR","PKR","NGN","GHS","KES","ZAR","CAD","AUD","CHF","BRL","XOF","XAF"};String[] currencyLabels=english()?new String[]{"SDG — Sudanese Pound","SAR — Saudi Riyal","AED — UAE Dirham","EGP — Egyptian Pound","QAR — Qatari Riyal","KWD — Kuwaiti Dinar","BHD — Bahraini Dinar","OMR — Omani Rial","JOD — Jordanian Dinar","IQD — Iraqi Dinar","MAD — Moroccan Dirham","TND — Tunisian Dinar","DZD — Algerian Dinar","LYD — Libyan Dinar","YER — Yemeni Rial","LBP — Lebanese Pound","SYP — Syrian Pound","MRU — Mauritanian Ouguiya","SOS — Somali Shilling","DJF — Djiboutian Franc","USD — US Dollar","EUR — Euro","TRY — Turkish Lira","GBP — Pound Sterling","CNY — Chinese Yuan","JPY — Japanese Yen","INR — Indian Rupee","PKR — Pakistani Rupee","NGN — Nigerian Naira","GHS — Ghanaian Cedi","KES — Kenyan Shilling","ZAR — South African Rand","CAD — Canadian Dollar","AUD — Australian Dollar","CHF — Swiss Franc","BRL — Brazilian Real","XOF — West African CFA Franc","XAF — Central African CFA Franc"}:new String[]{"SDG — الجنيه السوداني","SAR — الريال السعودي","AED — الدرهم الإماراتي","EGP — الجنيه المصري","QAR — الريال القطري","KWD — الدينار الكويتي","BHD — الدينار البحريني","OMR — الريال العماني","JOD — الدينار الأردني","IQD — الدينار العراقي","MAD — الدرهم المغربي","TND — الدينار التونسي","DZD — الدينار الجزائري","LYD — الدينار الليبي","YER — الريال اليمني","LBP — الليرة اللبنانية","SYP — الليرة السورية","MRU — الأوقية الموريتانية","SOS — الشلن الصومالي","DJF — الفرنك الجيبوتي","USD — الدولار الأمريكي","EUR — اليورو","TRY — الليرة التركية","GBP — الجنيه الإسترليني","CNY — اليوان الصيني","JPY — الين الياباني","INR — الروبية الهندية","PKR — الروبية الباكستانية","NGN — النايرا النيجيرية","GHS — السيدي الغاني","KES — الشلن الكيني","ZAR — الراند الجنوب أفريقي","CAD — الدولار الكندي","AUD — الدولار الأسترالي","CHF — الفرنك السويسري","BRL — الريال البرازيلي","XOF — فرنك غرب أفريقيا","XAF — فرنك وسط أفريقيا"};Spinner currency=new Spinner(this);ArrayAdapter<String> currencyAdapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,currencyLabels);currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);currency.setAdapter(currencyAdapter);String savedCurrency=p.getString("currency","SDG");for(int i=0;i<currencyCodes.length;i++)if(currencyCodes[i].equals(savedCurrency)){currency.setSelection(i);break;}content.addView(t("عملة واحدة لكل ملف شركة. تغيير العملة لا يحوّل الأرصدة القديمة تلقائيًا.",14));content.addView(currency);
  EditText decimals=e("عدد المنازل العشرية (0 أو 2)");decimals.setInputType(2);decimals.setText(String.valueOf(p.getInt("decimals",2)));content.addView(decimals);
  EditText tax=e("نسبة الضريبة % (0 إذا لا توجد ضريبة)");tax.setInputType(8194);tax.setText(String.valueOf(p.getString("tax_percent","0")));content.addView(tax);
  EditText prefix=e("بادئة رقم الفاتورة");prefix.setText(p.getString("invoice_prefix","INV"));content.addView(prefix);
  content.addView(t("عدد المنازل يغيّر طريقة العرض فقط ولا يغيّر تخزين المبالغ. نسبة الضريبة محفوظة كإعداد ولا تُضاف تلقائيًا إلى الفواتير. لا يوجد تكامل ضريبي رسمي.",14));
  Button save=b("حفظ إعدادات الشركة");content.addView(save);
  save.setOnClickListener(v->{
   String company=name.getText().toString().trim(),cur=currencyCodes[currency.getSelectedItemPosition()],pre=prefix.getText().toString().trim(),taxText=tax.getText().toString().trim();
   int dec;double rate;
   try{dec=Integer.parseInt(decimals.getText().toString().trim());rate=Double.parseDouble(taxText.isEmpty()?"0":taxText);}catch(Exception ex){toast("تحقق من عدد المنازل ونسبة الضريبة");return;}
   if(company.isEmpty()||cur.isEmpty()||pre.isEmpty()||(dec!=0&&dec!=2)||Double.isNaN(rate)||Double.isInfinite(rate)||rate<0||rate>100){toast("بيانات غير صحيحة: أكمل الاسم والعملة والبادئة، والمنازل 0 أو 2، والضريبة بين 0 و100");return;}
   boolean ok=p.edit().putString("name",company).putString("currency",cur).putInt("decimals",dec).putString("tax_percent",String.valueOf(rate)).putString("invoice_prefix",pre).commit();
   toast(ok?"تم حفظ إعدادات الشركة":"تعذر حفظ الإعدادات");
  });
 }
 void openSection(String n){switch(n){
 case"نقطة البيع والمبيعات":case"الفواتير والمبيعات":invoice(false);break;
 case"المشتريات":invoice(true);break;case"المرتجعات":returns();break;case"الأصناف والمخزون":products();break;
 case"العملاء والموردون":groupMenu(n,new String[][]{{"العملاء","العملاء"},{"الموردون","الموردون"}});break;
 case"العملاء":party("customers","العملاء");break;case"الموردون":party("suppliers","الموردون");break;
 case"الخزينة والشيكات":groupMenu(n,new String[][]{{"الصندوق والمصروفات","الصندوق والمصروفات"},{"الحسابات البنكية","الحسابات البنكية"},{"سجل الشيكات","سجل الشيكات"}});break;
 case"الصندوق والمصروفات":cash();break;case"الحسابات البنكية":banks();break;case"سجل الشيكات":checks();break;
 case"المحاسبة والتقارير":groupMenu(n,new String[][]{{"الحسابات الرئيسية ودفتر الأستاذ","الحسابات الرئيسية"},{"القيود اليدوية","القيود اليدوية"},{"القيد العكسي","القيد العكسي"},{"التقارير والتحليل","التقارير والتحليل"}});break;
 case"القيود اليدوية":manualJournal();break;case"القيد العكسي":reverseManualJournal();break;case"التقارير والتحليل":reports();break;case"الحسابات الرئيسية":mainAccounts();break;
 case"حساب التشغيل والتصنيع المرحلي":factoryOperations();break;case"الكاش — بيع الشباك":breadCounter();break;case"التوزيع":breadDistribution();break;
 case"الموظفون والرواتب":case"الموظفون":employees();break;
 case"أدوات النظام":groupMenu(n,new String[][]{{"النسخ الاحتياطي والاستعادة","النسخ الاحتياطي"},{"إعدادات الشركة","إعدادات الشركة"},{"الدفع والتحويل البنكي","الدفع والتحويل البنكي"},{"تغيير رمز الدخول","تغيير رمز الدخول"},{"الدعم","الدعم"}});break;
 case"النسخ الاحتياطي":backup();break;case"إعدادات الشركة":companySettings();break;case"الدفع والتحويل البنكي":paymentDetails();break;case"تغيير رمز الدخول":changePin();break;case"الدعم":support();break;default:support();}}
  void paymentDetails(){
   page("الدفع والتحويل البنكي");
   String iban="SD8504018822240001";
   content.addView(t("بيانات التحويل البنكي بالدولار الأمريكي (USD)",18));
   TextView number=t(iban,20);number.setTextIsSelectable(true);number.setTypeface(null,android.graphics.Typeface.BOLD);content.addView(number);
   content.addView(t("تحقق من اسم المستفيد والعملة والرسوم مع البنك قبل التحويل. لا ترسل كلمات مرور أو رموز تحقق.",14));
   Button copy=b("📋 نسخ رقم IBAN");content.addView(copy);copy.setOnClickListener(v->{try{android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("IBAN",iban));toast("تم نسخ رقم IBAN");}catch(Exception ex){toast("تعذر النسخ؛ اضغط مطولاً على الرقم لنسخه");}});
   Button notice=b("💬 إرسال إشعار التحويل عبر واتساب");content.addView(notice);notice.setOnClickListener(v->{EditText amount=e("المبلغ المحوّل بالدولار"),reference=e("مرجع التحويل البنكي");amount.setInputType(8194);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.addView(amount);box.addView(reference);new AlertDialog.Builder(this).setTitle("إشعار التحويل").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("متابعة إلى واتساب",(d,w)->{String a=amount.getText().toString().trim(),r=reference.getText().toString().trim();if(a.isEmpty()||r.isEmpty()){toast("أدخل المبلغ ومرجع التحويل");return;}String message="السلام عليكم، أرسل إشعار تحويل بنكي لشراء ترخيص المحاسب الميزان.\\nالمبلغ: "+a+" USD\\nمرجع التحويل: "+r+"\\nIBAN المستفيد: "+iban+"\\nيرجى تأكيد وصول المبلغ قبل إصدار الترخيص. سأرفق إيصال التحويل هنا.";open("https://wa.me/249121851285?text="+android.net.Uri.encode(message));}).show();});
   Button support=b("التواصل مع الدعم");content.addView(support);support.setOnClickListener(v->open("https://wa.me/249121851285"));
  }
  void support(){page("الدعم");content.addView(t("راسلنا عبر واتساب أو البريد الإلكتروني. لا ترسل كلمات المرور أو بياناتك المالية السرية.",16));content.addView(t("رقم الدعم: +249 121 851 285",16));Button wa=b("الدعم عبر واتساب");content.addView(wa);wa.setOnClickListener(v->open("https://wa.me/249121851285"));content.addView(t("البريد الإلكتروني: moh1442010@gmail.com",16));Button mail=b("الدعم عبر البريد الإلكتروني");content.addView(mail);mail.setOnClickListener(v->{try{Intent i=new Intent(Intent.ACTION_SENDTO,android.net.Uri.parse("mailto:moh1442010@gmail.com"));i.putExtra(Intent.EXTRA_SUBJECT,"دعم تطبيق شركة محمد مصطفى الذكية");startActivity(i);}catch(Exception ex){toast("تعذر فتح تطبيق البريد الإلكتروني");}});}
 String summary(){Cursor c=db.q("SELECT COALESCE(SUM(CASE WHEN type='SALE' THEN total WHEN type='SALE_RETURN' THEN -total ELSE 0 END),0),COALESCE(SUM(CASE WHEN type='PURCHASE' THEN total WHEN type='PURCHASE_RETURN' THEN -total ELSE 0 END),0),COALESCE(SUM(CASE WHEN type='EXPENSE' THEN total ELSE 0 END),0) FROM transactions");double s=0,p=0,x=0;if(c.moveToFirst()){s=c.getDouble(0);p=c.getDouble(1);x=c.getDouble(2);}c.close();return tr("المبيعات: ") +fmt(s)+tr(" | المشتريات: ")+fmt(p)+tr(" | المصروفات: ")+fmt(x);}
 void products(){page("الأصناف والمخزون والباركود");EditText n=e("اسم الصنف"),barcode=e("الباركود (امسح بالماسح أو اكتب الرقم)"),q=e("الرصيد الافتتاحي"),p=e("سعر البيع"),c=e("تكلفة الوحدة"),r=e("حد إعادة الطلب");for(EditText x:new EditText[]{n,barcode,q,p,c,r}){if(x==n)x.setInputType(1);else if(x==barcode)x.setInputType(2);else x.setInputType(8194);content.addView(x);}content.addView(t("يمكن استخدام ماسح USB/Bluetooth أو كاميرا الجوال لمسح الباركود.",13));Button scanBarcode=b("مسح بالكاميرا");content.addView(scanBarcode);scanBarcode.setOnClickListener(v->{pendingBarcodeTarget=barcode;startBarcodeScan();});Button add=b("إضافة الصنف");content.addView(add);add.setOnClickListener(v->{String name=n.getText().toString().trim(),code=barcode.getText().toString().trim();if(name.isEmpty()||!validNumber(q,false)||!validNumber(p,false)||!validNumber(c,false)||!validNumber(r,false)){toast("أدخل اسم الصنف وأرقامًا صحيحة للرصيد والأسعار وحد إعادة الطلب");return;}double opening=num(q),salePrice=num(p),unitCost=num(c),reorder=num(r);if(opening<0||salePrice<0||unitCost<0||reorder<0){toast("لا يُسمح بقيم سالبة في الرصيد أو الأسعار أو حد إعادة الطلب");return;}if(opening>0&&unitCost<=0){toast("أدخل تكلفة موجبة للرصيد الافتتاحي حتى يمكن ترحيله محاسبيًا");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{ContentValues pv=new ContentValues();pv.put("name",name);pv.put("barcode",code.isEmpty()?null:code);pv.put("qty",opening);pv.put("sale_price",salePrice);pv.put("cost",unitCost);pv.put("reorder",reorder);sq.insertOrThrow("products",null,pv);if(opening>0){long inventory=accountId(sq,"1200"),openingBalances=accountId(sq,"3100");if(inventory==0||openingBalances==0)throw new IllegalStateException("Missing opening stock ledger account");double value=opening*unitCost;if(Double.isNaN(value)||Double.isInfinite(value))throw new IllegalArgumentException("Opening stock value is not finite");long je=journal(sq);line(sq,je,inventory,value,0);line(sq,je,openingBalances,0,value);assertBalanced(sq,je);}sq.setTransactionSuccessful();toast("تم حفظ الصنف وترحيل رصيده الافتتاحي إن وُجد");products();}catch(Exception ex){toast("تعذر الحفظ: تحقق من عدم تكرار الاسم أو الباركود وصحة الرصيد والتكلفة");}finally{sq.endTransaction();}});Cursor z=db.q("SELECT name,barcode,qty,sale_price,cost,reorder FROM products ORDER BY id DESC");while(z.moveToNext())content.addView(t(z.getString(0)+" | باركود "+(z.getString(1)==null?"—":z.getString(1))+" | رصيد "+z.getDouble(2)+" | بيع "+fmt(z.getDouble(3))+" | تكلفة "+fmt(z.getDouble(4))+(z.getDouble(2)<=z.getDouble(5)?" | ⚠ إعادة طلب":""),15));z.close();}
 void invoice(boolean purchase){
   page(purchase?"فاتورة شراء":"فاتورة بيع");
   EditText customer=e(purchase?"المورد":"العميل"),item=e("اسم الصنف"),qty=e("الكمية"),price=e("السعر"),paid=e("المدفوع"),discount=e("الخصم");
   for(EditText x:new EditText[]{qty,price,paid,discount})x.setInputType(8194);
   for(EditText x:new EditText[]{customer,item,qty,price,paid,discount})content.addView(x);
   Button scanItem=b("مسح بالكاميرا");content.addView(scanItem);scanItem.setOnClickListener(v->{pendingBarcodeTarget=item;startBarcodeScan();});
   TextView total=t("الإجمالي: 0.00",18);content.addView(total);
   Button calc=b("حساب الإجمالي");content.addView(calc);
   calc.setOnClickListener(v->{
    if(!validNumber(qty,true)||!validNumber(price,true)||!validNumber(discount,false)){toast("أدخل كمية وسعرًا وخصمًا رقميًا صحيحًا");return;}
    double q=num(qty),pr=num(price),disc=num(discount);
    if(q<=0||pr<0||disc<0||disc>q*pr){toast("تحقق من الكمية والسعر والخصم");return;}
    total.setText(tr("الإجمالي: ")+fmt(q*pr-disc));
   });
   Button save=b("حفظ وترحيل");content.addView(save);
   save.setOnClickListener(v->{
    String party=customer.getText().toString().trim(),lookup=item.getText().toString().trim();
    if(party.isEmpty()){toast("أدخل اسم العميل أو المورد");return;}
    if(lookup.isEmpty()||!validNumber(qty,true)||!validNumber(price,true)||!validNumber(discount,false)||!validNumber(paid,false)){toast("أكمل بيانات الفاتورة بأرقام صحيحة");return;}
    double q=num(qty),pr=num(price),disc=num(discount),paidValue=num(paid);
    if(q<=0||pr<0||disc<0||disc>q*pr||paidValue<0||paidValue>q*pr-disc||(Double.isNaN(q*pr-disc)||Double.isInfinite(q*pr-disc))){toast("تحقق من الكمية والسعر والخصم والمدفوع");return;}
    Cursor c=db.q("SELECT id,qty,cost FROM products WHERE name=? OR barcode=?",lookup,lookup);
    if(!c.moveToFirst()){c.close();toast("الصنف غير موجود");return;}
    long pid=c.getLong(0);double old=c.getDouble(1),cost=c.getDouble(2);c.close();
    double next=purchase?old+q:old-q;
    if(next<0){toast("الرصيد غير كافٍ");return;}
    double totalValue=q*pr-disc,pay=Math.min(paidValue,totalValue),remaining=totalValue-pay;
    SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();
    try{
     ContentValues pv=new ContentValues();pv.put("qty",next);
     if(purchase)pv.put("cost",((old*cost)+(q*pr-disc))/Math.max(0.0000001,next));
     int changed=sq.update("products",pv,"id=?",new String[]{String.valueOf(pid)});
     if(changed!=1)throw new IllegalStateException("Product update failed");
     ContentValues tv=new ContentValues();String type=purchase?"PURCHASE":"SALE";String partyTable=purchase?"suppliers":"customers";Cursor partyCursor=sq.rawQuery("SELECT id FROM "+partyTable+" WHERE name=? ORDER BY id LIMIT 1",new String[]{party});long partyId;if(partyCursor.moveToFirst()){partyId=partyCursor.getLong(0);partyCursor.close();}else{partyCursor.close();ContentValues partyValues=new ContentValues();partyValues.put("name",party);if(!purchase)partyValues.put("credit_limit",0);partyId=sq.insertOrThrow(partyTable,null,partyValues);}if(!purchase&&remaining>0){Cursor limc=sq.rawQuery("SELECT COALESCE(credit_limit,0) FROM customers WHERE id=?",new String[]{String.valueOf(partyId)});double creditLimit=limc.moveToFirst()?limc.getDouble(0):0;limc.close();if(creditLimit>0){Cursor bal=sq.rawQuery("SELECT COALESCE(SUM(remaining),0) FROM transactions WHERE type='SALE' AND (customer_id=? OR (customer_id IS NULL AND party=?))",new String[]{String.valueOf(partyId),party});double currentDue=bal.moveToFirst()?bal.getDouble(0):0;bal.close();if(currentDue+remaining>creditLimit+0.005)throw new IllegalArgumentException("تجاوزت الفاتورة حد ائتمان العميل. الرصيد الحالي "+fmt(currentDue)+" والحد "+fmt(creditLimit));}}tv.put("type",type);tv.put("party",party);if(purchase)tv.put("supplier_id",partyId);else tv.put("customer_id",partyId);tv.put("total",totalValue);tv.put("paid",pay);tv.put("remaining",remaining);tv.put("created_at",System.currentTimeMillis());
     long tid=sq.insertOrThrow("transactions",null,tv);String invoicePrefix=getSharedPreferences("company_profile",0).getString("invoice_prefix","INV");String invoiceNo=invoicePrefix+"-"+new java.text.SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())+"-"+String.format(Locale.US,"%06d",tid);sq.execSQL("UPDATE transactions SET invoice_no=? WHERE id=?",new Object[]{invoiceNo,tid});
     ContentValues iv=new ContentValues();iv.put("transaction_id",tid);iv.put("product_id",pid);iv.put("qty",q);iv.put("price",pr);iv.put("cost",cost);iv.put("discount",disc);sq.insertOrThrow("invoice_items",null,iv);
     postJournal(sq,tid,type,totalValue,pay,remaining,q*cost);
     sq.setTransactionSuccessful();
    }catch(Exception ex){toast("لم تُحفظ الفاتورة. لم يتم اعتماد أي تغيير؛ راجع البيانات وحاول مجددًا.");return;}
    finally{sq.endTransaction();}
    Cursor saved=db.q("SELECT invoice_no FROM transactions ORDER BY id DESC LIMIT 1");String savedInvoice=saved.moveToFirst()?saved.getString(0):"";saved.close();toast("تم حفظ الفاتورة وترحيلها محاسبيًا — "+savedInvoice);dashboard();
   });
  }


 void breadCounter(){
  page("الكاش — بيع الشباك");
  content.addView(t("سجّل مبيعات المحل النقدية هنا. إجمالي البيع يساوي النقد المستلم، وتُخصم الكمية من المخزون وتُرحّل العملية محاسبيًا.",14));
  EditText item=e("اسم الصنف أو الباركود"),qty=e("عدد العيش المباع"),price=e("سعر الوحدة");item.setInputType(1);qty.setInputType(8194);price.setInputType(8194);content.addView(item);content.addView(qty);content.addView(price);
  TextView total=t("الإجمالي النقدي: —",18);content.addView(total);
  Button calc=b("🧮 حساب الإجمالي");content.addView(calc);calc.setOnClickListener(v->{if(!validNumber(qty,true)||!validNumber(price,true)){toast("أدخل الكمية والسعر");return;}double q=num(qty),p=num(price);if(q<=0||Math.abs(q-Math.rint(q))>0.000001||p<0||!Double.isFinite(q*p)){toast("الكمية عدد صحيح والسعر غير سالب");return;}total.setText("الإجمالي النقدي: "+fmt(q*p));});
  Button save=b("💵 حفظ وترحيل");content.addView(save);save.setOnClickListener(v->{String lookup=item.getText().toString().trim();if(lookup.isEmpty()||!validNumber(qty,true)||!validNumber(price,true)){toast("أكمل بيانات البيع");return;}double q=num(qty),p=num(price),amount=q*p;if(q<=0||Math.abs(q-Math.rint(q))>0.000001||p<0||!Double.isFinite(amount)){toast("تحقق من الكمية والسعر");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{
   Cursor c=sq.rawQuery("SELECT id,qty,cost FROM products WHERE name=? OR barcode=? LIMIT 1",new String[]{lookup,lookup});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("الصنف غير موجود");}long pid=c.getLong(0);double stock=c.getDouble(1),cost=c.getDouble(2);c.close();if(stock+0.000001<q)throw new IllegalArgumentException("المخزون غير كافٍ");
   ContentValues tv=new ContentValues();tv.put("type","SALE");tv.put("party","بيع الشباك");tv.put("total",amount);tv.put("paid",amount);tv.put("remaining",0);tv.put("created_at",System.currentTimeMillis());long tid=sq.insertOrThrow("transactions",null,tv);String invoiceNo="BC-"+new java.text.SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())+"-"+String.format(Locale.US,"%06d",tid);sq.execSQL("UPDATE transactions SET invoice_no=? WHERE id=?",new Object[]{invoiceNo,tid});
   ContentValues iv=new ContentValues();iv.put("transaction_id",tid);iv.put("product_id",pid);iv.put("qty",q);iv.put("price",p);iv.put("cost",cost);iv.put("discount",0);sq.insertOrThrow("invoice_items",null,iv);
   ContentValues row=new ContentValues();row.put("transaction_id",tid);row.put("product_id",pid);row.put("qty",q);row.put("unit_price",p);row.put("total",amount);row.put("created_at",System.currentTimeMillis());sq.insertOrThrow("bread_counter_sales",null,row);
   ContentValues upd=new ContentValues();upd.put("qty",stock-q);if(sq.update("products",upd,"id=?",new String[]{String.valueOf(pid)})!=1)throw new IllegalStateException("فشل تحديث المخزون");postJournal(sq,tid,"SALE",amount,amount,0,q*cost);sq.setTransactionSuccessful();
  }catch(Exception ex){toast("لم تُحفظ العملية: "+(ex.getMessage()==null?"راجع البيانات":ex.getMessage()));return;}finally{sq.endTransaction();}breadCounter();});
  Cursor totals=db.q("SELECT COALESCE(SUM(total),0),COALESCE(SUM(qty),0),COUNT(*) FROM bread_counter_sales");if(totals.moveToFirst())content.addView(t("جملة كاش الشباك: "+fmt(totals.getDouble(0))+" | عدد العيش: "+String.format(Locale.US,"%.0f",totals.getDouble(1))+" | العمليات: "+totals.getInt(2),17));totals.close();
  Cursor recent=db.q("SELECT b.id,t.invoice_no,p.name,b.qty,b.unit_price,b.total FROM bread_counter_sales b JOIN products p ON p.id=b.product_id JOIN transactions t ON t.id=b.transaction_id ORDER BY b.id DESC LIMIT 30");content.addView(t("آخر عمليات الشباك",17));while(recent.moveToNext())content.addView(t("#"+recent.getLong(0)+" | "+recent.getString(1)+" | "+recent.getString(2)+" | العدد "+String.format(Locale.US,"%.0f",recent.getDouble(3))+" | الوحدة "+fmt(recent.getDouble(4))+" | الإجمالي "+fmt(recent.getDouble(5)),14));recent.close();
 }
 long postBreadDistributionJournal(SQLiteDatabase sq,long tid,String distributor,double gross,double net,double paid,double due,double diff,double qty,double cost){
  if(tid<=0||distributor==null||distributor.trim().isEmpty()||!Double.isFinite(gross)||!Double.isFinite(net)||!Double.isFinite(paid)||!Double.isFinite(due)||!Double.isFinite(diff)||!Double.isFinite(qty)||!Double.isFinite(cost)||gross<0||net<0||paid<0||due<0||diff<0||qty<=0||cost<0||Math.abs(paid+due-net)>0.005||Math.abs(gross-net-diff)>0.005)throw new IllegalArgumentException("مبالغ التوزيع غير متطابقة");
  Cursor prior=sq.rawQuery("SELECT 1 FROM journal_entries WHERE transaction_id=? LIMIT 1",new String[]{String.valueOf(tid)});boolean exists=prior.moveToFirst();prior.close();if(exists)throw new IllegalStateException("Sale already posted");
  long ca=accountId(sq,"1010"),ar=accountId(sq,"1100"),sa=accountId(sq,"4000"),disc=accountId(sq,"4200"),cogs=accountId(sq,"5100"),inv=accountId(sq,"1200");if(ca==0||ar==0||sa==0||disc==0||cogs==0||inv==0)throw new IllegalStateException("حساب محاسبي مطلوب غير موجود");
  ContentValues jev=new ContentValues();jev.put("transaction_id",tid);jev.put("created_at",System.currentTimeMillis());jev.put("description","توزيع مخفض — "+distributor.trim());long je=sq.insertOrThrow("journal_entries",null,jev);
  if(paid>0)line(sq,je,ca,paid,0);if(due>0)line(sq,je,ar,due,0);if(diff>0.005)line(sq,je,disc,diff,0);line(sq,je,sa,0,gross);if(qty*cost>0){line(sq,je,cogs,qty*cost,0);line(sq,je,inv,0,qty*cost);}assertBalanced(sq,je);return je;
 }
void breadDistribution(){
  page("التوزيع");content.addView(t("للمطاعم والبقالات التي تشتري كميات كبيرة بسعر مخفض. فرق السعر = قيمة الكمية بسعر المحل − مبلغ التوزيع. النقد + الدين يجب أن يساوي مبلغ التوزيع، والكمية تُخصم من المخزون.",14));content.addView(t("مثال: 90 عيشة بقيمة محل 30,000 ج.س، ومبلغ توزيع 28,000 ج.س؛ فرق السعر 2,000 ج.س.",14));
  EditText name=e("اسم المطعم أو البقالة"),item=e("اسم الصنف أو الباركود"),qty=e("العدد"),gross=e("جملة المبلغ بسعر المحل"),net=e("جملة مبلغ التوزيع"),cash=e("النقد المستلم"),debt=e("الدين المتبقي");name.setInputType(1);item.setInputType(1);for(EditText x:new EditText[]{qty,gross,net,cash,debt})x.setInputType(8194);for(EditText x:new EditText[]{name,item,qty,gross,net,cash,debt})content.addView(x);
  TextView difference=t("فرق السعر: —",18);difference.setTextColor(green);content.addView(difference);Button calc=b("🧮 حساب ومطابقة المبالغ");content.addView(calc);calc.setOnClickListener(v->{if(!validNumber(qty,true)||!validNumber(gross,true)||!validNumber(net,true)||!validNumber(cash,true)||!validNumber(debt,true)){toast("أدخل جميع الأرقام");return;}double q=num(qty),g=num(gross),n=num(net),c=num(cash),d=num(debt);if(q<=0||Math.abs(q-Math.rint(q))>0.000001||g<0||n<0||n>g+0.005||c<0||d<0||Math.abs(c+d-n)>0.005){toast("النقد + الدين يجب أن يساوي مبلغ التوزيع، ومبلغ التوزيع لا يتجاوز سعر المحل");return;}difference.setText("فرق السعر: "+fmt(g-n)+" | الدين: "+fmt(d));});
  Button save=b("🚚 حفظ وترحيل التوزيع");content.addView(save);save.setOnClickListener(v->{String distributor=name.getText().toString().trim(),lookup=item.getText().toString().trim();if(distributor.isEmpty()||lookup.isEmpty()||!validNumber(qty,true)||!validNumber(gross,true)||!validNumber(net,true)||!validNumber(cash,true)||!validNumber(debt,true)){toast("أكمل الاسم والصنف والمبالغ");return;}double q=num(qty),g=num(gross),n=num(net),paid=num(cash),due=num(debt),diff=g-n;if(q<=0||Math.abs(q-Math.rint(q))>0.000001||g<0||n<0||n>g+0.005||paid<0||due<0||Math.abs(paid+due-n)>0.005||!Double.isFinite(g)||!Double.isFinite(n)||!Double.isFinite(paid)||!Double.isFinite(due)||diff< -0.005){toast("راجع المبالغ: النقد + الدين = مبلغ التوزيع، وفرق السعر غير سالب");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{
   Cursor c=sq.rawQuery("SELECT id,qty,cost FROM products WHERE name=? OR barcode=? LIMIT 1",new String[]{lookup,lookup});if(!c.moveToFirst()){c.close();throw new IllegalArgumentException("الصنف غير موجود");}long pid=c.getLong(0);double stock=c.getDouble(1),cost=c.getDouble(2);c.close();if(stock+0.000001<q)throw new IllegalArgumentException("المخزون غير كافٍ");
   Cursor pc=sq.rawQuery("SELECT id FROM customers WHERE name=? ORDER BY id LIMIT 1",new String[]{distributor});long customer;if(pc.moveToFirst()){customer=pc.getLong(0);pc.close();}else{pc.close();ContentValues cv=new ContentValues();cv.put("name",distributor);cv.put("credit_limit",0);customer=sq.insertOrThrow("customers",null,cv);}
   ContentValues tv=new ContentValues();tv.put("type","SALE");tv.put("party",distributor);tv.put("customer_id",customer);tv.put("total",n);tv.put("paid",paid);tv.put("remaining",due);tv.put("created_at",System.currentTimeMillis());long tid=sq.insertOrThrow("transactions",null,tv);String invoiceNo="BD-"+new java.text.SimpleDateFormat("yyyyMMdd",Locale.US).format(new Date())+"-"+String.format(Locale.US,"%06d",tid);sq.execSQL("UPDATE transactions SET invoice_no=? WHERE id=?",new Object[]{invoiceNo,tid});
   ContentValues iv=new ContentValues();iv.put("transaction_id",tid);iv.put("product_id",pid);iv.put("qty",q);iv.put("price",n/q);iv.put("cost",cost);iv.put("discount",0);sq.insertOrThrow("invoice_items",null,iv);
   ContentValues row=new ContentValues();row.put("transaction_id",tid);row.put("product_id",pid);row.put("distributor_name",distributor);row.put("qty",q);row.put("retail_total",g);row.put("distribution_total",n);row.put("cash_received",paid);row.put("debt_amount",due);row.put("price_difference",Math.max(0,diff));row.put("created_at",System.currentTimeMillis());sq.insertOrThrow("bread_distribution_sales",null,row);
   ContentValues upd=new ContentValues();upd.put("qty",stock-q);if(sq.update("products",upd,"id=?",new String[]{String.valueOf(pid)})!=1)throw new IllegalStateException("فشل تحديث المخزون");
   postBreadDistributionJournal(sq,tid,distributor,g,n,paid,due,Math.max(0,diff),q,cost);sq.setTransactionSuccessful();
  }catch(Exception ex){toast("لم يُحفظ التوزيع: "+(ex.getMessage()==null?"راجع البيانات":ex.getMessage()));return;}finally{sq.endTransaction();}breadDistribution();});
  Cursor totals=db.q("SELECT COALESCE(SUM(retail_total),0),COALESCE(SUM(distribution_total),0),COALESCE(SUM(cash_received),0),COALESCE(SUM(debt_amount),0),COALESCE(SUM(price_difference),0),COALESCE(SUM(qty),0),COUNT(*) FROM bread_distribution_sales");if(totals.moveToFirst())content.addView(t("سعر المحل "+fmt(totals.getDouble(0))+" | مبلغ التوزيع "+fmt(totals.getDouble(1))+" | النقد "+fmt(totals.getDouble(2))+" | الديون "+fmt(totals.getDouble(3))+" | فروق السعر "+fmt(totals.getDouble(4))+" | العدد "+String.format(Locale.US,"%.0f",totals.getDouble(5))+" | العمليات "+totals.getInt(6),15));totals.close();
  Cursor recent=db.q("SELECT b.id,t.invoice_no,b.qty,b.distributor_name,b.distribution_total,b.cash_received,b.debt_amount,b.price_difference FROM bread_distribution_sales b JOIN transactions t ON t.id=b.transaction_id ORDER BY b.id DESC LIMIT 100");content.addView(t("سجل التوزيع: الرقم | العدد | الاسم | المبلغ | النقد | الديون | فرق السعر",17));while(recent.moveToNext())content.addView(t("#"+recent.getLong(0)+" | "+recent.getString(1)+" | "+String.format(Locale.US,"%.0f",recent.getDouble(2))+" | "+recent.getString(3)+" | "+fmt(recent.getDouble(4))+" | "+fmt(recent.getDouble(5))+" | "+fmt(recent.getDouble(6))+" | "+fmt(recent.getDouble(7)),14));recent.close();
 }

 void returns(){
  page("المرتجعات");content.addView(t("عكس فاتورة كاملة فقط — لا تُحذف الفاتورة الأصلية، ويُمنع ردّها مرتين. راجع الكمية المتاحة قبل اعتماد مرتجع الشراء.",15));
  EditText invoice=e("رقم الفاتورة الأصلية");invoice.setSingleLine(true);content.addView(invoice);
  Button process=b("معاينة الفاتورة والمرتجع");content.addView(process);
  process.setOnClickListener(v->{
   String no=invoice.getText().toString().trim();if(no.isEmpty()){toast("أدخل رقم الفاتورة");return;}
   Cursor c=db.q("SELECT id,type,party,total,paid,remaining,COALESCE(customer_id,0),COALESCE(supplier_id,0) FROM transactions WHERE invoice_no=?",no);
   if(!c.moveToFirst()){c.close();toast("لم يُعثر على الفاتورة");return;}
   long originalId=c.getLong(0);String originalType=c.getString(1),party=c.getString(2);double total=c.getDouble(3),paid=c.getDouble(4),remaining=c.getDouble(5);long customerId=c.getLong(6),supplierId=c.getLong(7);c.close();
   boolean sale=originalType.equals("SALE"),purchase=originalType.equals("PURCHASE");
   if(!sale&&!purchase){toast("يمكن إرجاع فاتورة بيع أو شراء أصلية فقط");return;}
   Cursor prior=db.q("SELECT id FROM transactions WHERE original_transaction_id=?",String.valueOf(originalId));boolean already=prior.moveToFirst();prior.close();if(already){toast("سبق تسجيل مرتجع لهذه الفاتورة؛ لا يمكن تكراره");return;}
   Cursor items=db.q("SELECT i.product_id,i.qty,i.price,i.cost,i.discount,p.name,p.qty,p.cost FROM invoice_items i JOIN products p ON p.id=i.product_id WHERE i.transaction_id=? ORDER BY i.id",String.valueOf(originalId));
   if(!items.moveToFirst()){items.close();toast("لا توجد تفاصيل أصناف صالحة لهذه الفاتورة");return;}
   ArrayList<long[]> ids=new ArrayList<>();ArrayList<double[]> vals=new ArrayList<>();ArrayList<String> names=new ArrayList<>();boolean invalid=false;double inventoryValue=0,returnCogs=0;int count=0;
   do{long pid=items.getLong(0);double q=items.getDouble(1),price=items.getDouble(2),itemCost=items.getDouble(3),disc=items.getDouble(4),stock=items.getDouble(6),avgCost=items.getDouble(7);if(q<=0||price<0||itemCost<0||disc<0){invalid=true;break;}if(purchase&&stock+0.000001<q){invalid=true;break;}ids.add(new long[]{pid});vals.add(new double[]{q,price,itemCost,disc,stock,avgCost});names.add(items.getString(5));inventoryValue+=q*price-disc;returnCogs+=q*itemCost;count++;}while(items.moveToNext());items.close();
   if(invalid||count==0){toast(purchase?"الكمية المتاحة لا تسمح بإرجاع الشراء أو بيانات الأصناف غير سليمة":"بيانات أصناف الفاتورة غير سليمة");return;}
   final String summaryText=(sale?"مرتجع بيع":"مرتجع شراء")+" — "+no+"\nالطرف: "+party+"\nإجمالي العكس: "+fmt(total)+"\nالمبلغ المدفوع/المردود: "+fmt(paid)+"\nالرصيد المستحق المعكوس: "+fmt(remaining)+"\nعدد الأصناف: "+count+"\nسيتم عكس الفاتورة كاملة.";
   new AlertDialog.Builder(this).setTitle("تأكيد المرتجع").setMessage(summaryText).setNegativeButton("إلغاء",null).setPositiveButton("اعتماد المرتجع", (dlg,which)->{
    SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();
    try{
     Cursor dup=sq.rawQuery("SELECT 1 FROM transactions WHERE original_transaction_id=? LIMIT 1",new String[]{String.valueOf(originalId)});boolean exists=dup.moveToFirst();dup.close();if(exists)throw new IllegalStateException("Return already exists");
     double totalCost=0;
     for(int i=0;i<ids.size();i++){
      long pid=ids.get(i)[0];double[] z=vals.get(i);double q=z[0],price=z[1],itemCost=z[2],disc=z[3],stock=z[4],avg=z[5];double next=purchase?stock-q:stock+q;
      ContentValues pv=new ContentValues();pv.put("qty",next);
      if(purchase){double currentValue=stock*avg,returnedValue=q*price-disc;if(next<0.000001){if(currentValue+0.005<returnedValue)throw new IllegalStateException("Inventory valuation mismatch");pv.put("cost",0);}else{double newValue=currentValue-returnedValue;if(newValue< -0.005)throw new IllegalStateException("Inventory valuation mismatch");pv.put("cost",Math.max(0,newValue/next));}totalCost+=returnedValue;}
      else{pv.put("cost",next>0?((stock*avg)+(q*itemCost))/next:itemCost);totalCost+=q*itemCost;}
      if(sq.update("products",pv,"id=?",new String[]{String.valueOf(pid)})!=1)throw new IllegalStateException("Product update failed");
     }
     ContentValues rt=new ContentValues();rt.put("type",sale?"SALE_RETURN":"PURCHASE_RETURN");rt.put("party",party);rt.put("total",total);rt.put("paid",paid);rt.put("remaining",remaining);rt.put("created_at",System.currentTimeMillis());rt.put("original_transaction_id",originalId);if(sale)rt.put("customer_id",customerId);else rt.put("supplier_id",supplierId);long returnId=sq.insertOrThrow("transactions",null,rt);
     String prefix=getSharedPreferences("company_profile",0).getString("invoice_prefix","INV");String returnNo=prefix+"-RET-"+String.format(Locale.US,"%06d",returnId);sq.execSQL("UPDATE transactions SET invoice_no=? WHERE id=?",new Object[]{returnNo,returnId});
     for(int i=0;i<ids.size();i++){double[] z=vals.get(i);ContentValues iv=new ContentValues();iv.put("transaction_id",returnId);iv.put("product_id",ids.get(i)[0]);iv.put("qty",z[0]);iv.put("price",z[1]);iv.put("cost",z[2]);iv.put("discount",z[3]);sq.insertOrThrow("invoice_items",null,iv);}
     long je=journal(sq),cash=accountId(sq,"1010"),receivable=accountId(sq,"1100"),inventory=accountId(sq,"1200"),salesReturns=accountId(sq,"4100"),payable=accountId(sq,"2100"),cogs=accountId(sq,"5100");
     if(sale){line(sq,je,salesReturns,total,0);if(paid>0)line(sq,je,cash,0,paid);if(remaining>0)line(sq,je,receivable,0,remaining);if(totalCost>0){line(sq,je,inventory,totalCost,0);line(sq,je,cogs,0,totalCost);}}
     else{if(paid>0)line(sq,je,cash,paid,0);if(remaining>0)line(sq,je,payable,remaining,0);line(sq,je,inventory,0,total);}
     assertBalanced(sq,je);sq.execSQL("UPDATE journal_entries SET description=? WHERE id=?",new Object[]{"مرتجع "+(sale?"بيع ":"شراء ")+no,je});sq.setTransactionSuccessful();toast("تم اعتماد المرتجع وترحيل القيد: "+returnNo);dashboard();
    }catch(Exception ex){toast("تعذر اعتماد المرتجع؛ أُلغيت جميع التغييرات. راجع رصيد المخزون وتقييمه.");}
    finally{sq.endTransaction();}
   }).show();
  });
 }

 void banks(){
  page("الحسابات البنكية");
  content.addView(t("إدارة الحسابات وحركات الإيداع والسحب",16));
  EditText bank=e("اسم البنك"),name=e("اسم الحساب"),opening=e("الرصيد الافتتاحي");
  opening.setInputType(8194);content.addView(bank);content.addView(name);content.addView(opening);
  Button add=b("إضافة حساب بنكي");content.addView(add);
  add.setOnClickListener(v->{String bn=bank.getText().toString().trim(),an=name.getText().toString().trim();double ob=num(opening);if(bn.isEmpty()||an.isEmpty()||!validNumber(opening,true)||ob<0){toast("أدخل اسم البنك والحساب ورصيدًا افتتاحيًا صحيحًا");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{ContentValues bv=new ContentValues();bv.put("bank_name",bn);bv.put("account_name",an);bv.put("opening_balance",ob);sq.insertOrThrow("bank_accounts",null,bv);if(ob>0){long je=journal(sq);line(sq,je,accountId(sq,"1020"),ob,0);line(sq,je,accountId(sq,"3000"),0,ob);assertBalanced(sq,je);}sq.setTransactionSuccessful();toast("تمت إضافة الحساب");banks();}catch(Exception ex){toast("تعذر إضافة الحساب؛ تحقق من البيانات");}finally{sq.endTransaction();}});
  content.addView(t("تسجيل حركة بنكية",18));
  EditText account=e("اسم الحساب البنكي كما أُدخل"),amount=e("المبلغ"),note=e("البيان");amount.setInputType(8194);content.addView(account);content.addView(amount);content.addView(note);
  Spinner kind=new Spinner(this);kind.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("إيداع"),tr("سحب")}));content.addView(kind);
  Button move=b("حفظ الحركة");content.addView(move);
  move.setOnClickListener(v->{double a=num(amount);if(!validNumber(amount,true)||a<=0){toast("أدخل مبلغًا صحيحًا");return;}Cursor c=db.q("SELECT id FROM bank_accounts WHERE account_name=?",account.getText().toString().trim());if(!c.moveToFirst()){c.close();toast("الحساب البنكي غير موجود");return;}long id=c.getLong(0);c.close();String k=kind.getSelectedItemPosition()==0?"DEPOSIT":"WITHDRAWAL";SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{ContentValues mv=new ContentValues();mv.put("account_id",id);mv.put("kind",k);mv.put("amount",a);mv.put("note",note.getText().toString().trim());mv.put("created_at",System.currentTimeMillis());sq.insertOrThrow("bank_movements",null,mv);long je=journal(sq);if(k.equals("DEPOSIT")){line(sq,je,accountId(sq,"1020"),a,0);line(sq,je,accountId(sq,"1010"),0,a);}else{line(sq,je,accountId(sq,"1010"),a,0);line(sq,je,accountId(sq,"1020"),0,a);}assertBalanced(sq,je);sq.setTransactionSuccessful();toast("تم تسجيل الحركة وترحيلها محاسبيًا");banks();}catch(Exception ex){toast("تعذر تسجيل الحركة");}finally{sq.endTransaction();}});
  Cursor c=db.q("SELECT b.bank_name,b.account_name,b.opening_balance+COALESCE(SUM(CASE WHEN m.kind='DEPOSIT' THEN m.amount WHEN m.kind='WITHDRAWAL' THEN -m.amount ELSE 0 END),0) FROM bank_accounts b LEFT JOIN bank_movements m ON m.account_id=b.id GROUP BY b.id ORDER BY b.id DESC");
  while(c.moveToNext())content.addView(t(c.getString(0)+" — "+c.getString(1)+" | الرصيد: "+fmt(c.getDouble(2)),15));c.close();
 }
 void checks(){
  page("سجل الشيكات");content.addView(t("تنبيه: حفظ الشيك وتغيير حالته يسجلان بياناته فقط؛ لا يُنشأ قيد محاسبي تلقائي عند التحصيل أو الصرف حتى تتم تسوية معتمدة.",15));content.addView(t("سجل الشيكات الواردة والصادرة ومواعيد استحقاقها",16));
  Spinner direction=new Spinner(this);direction.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("شيك وارد"),tr("شيك صادر")}));content.addView(direction);
  EditText number=e("رقم الشيك"),bank=e("اسم البنك"),party=e("اسم العميل أو المستفيد"),amount=e("قيمة الشيك"),issue=e("تاريخ الإصدار YYYY-MM-DD"),due=e("تاريخ الاستحقاق YYYY-MM-DD");
  amount.setInputType(8194);for(EditText x:new EditText[]{number,bank,party,amount,issue,due})content.addView(x);
  Spinner status=new Spinner(this);status.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("قيد الانتظار"),tr("مستحق"),tr("تم التحصيل/الصرف"),tr("مرتجع"),tr("ملغى")}));content.addView(status);
  Button save=b("حفظ الشيك");content.addView(save);
  save.setOnClickListener(v->{String issueDate=issue.getText().toString().trim(),dueDate=due.getText().toString().trim();if(number.getText().toString().trim().isEmpty()||!validNumber(amount,true)||num(amount)<=0||!validIsoDate(dueDate,true)||!validIsoDate(issueDate,false)){toast("أدخل رقم الشيك وقيمة موجبة وتواريخ صحيحة بصيغة YYYY-MM-DD");return;}if(!issueDate.isEmpty()&&issueDate.compareTo(dueDate)>0){toast("تاريخ إصدار الشيك لا يمكن أن يأتي بعد تاريخ استحقاقه");return;}String[] states={"PENDING","DUE","CLEARED","RETURNED","CANCELLED"};try{db.exec("INSERT INTO checks(direction,check_no,bank_name,party,amount,issue_date,due_date,status,created_at) VALUES(?,?,?,?,?,?,?,?,?)",direction.getSelectedItemPosition()==0?"INCOMING":"OUTGOING",number.getText().toString().trim(),bank.getText().toString().trim(),party.getText().toString().trim(),num(amount),issueDate,dueDate,states[status.getSelectedItemPosition()],System.currentTimeMillis());toast("تم حفظ الشيك");checks();}catch(Exception ex){toast("تعذر حفظ الشيك؛ ربما رقم الشيك مسجل مسبقًا");}});
  content.addView(t("تحديث حالة شيك محفوظ",17));EditText updateNo=e("رقم الشيك المراد تحديثه");content.addView(updateNo);Spinner updateDirection=new Spinner(this);updateDirection.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("شيك وارد"),tr("شيك صادر")}));content.addView(updateDirection);Spinner updateStatus=new Spinner(this);updateStatus.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("قيد الانتظار"),tr("مستحق"),tr("تم التحصيل/الصرف"),tr("مرتجع"),tr("ملغى")}));content.addView(updateStatus);Button update=b("تحديث حالة الشيك");content.addView(update);update.setOnClickListener(v->{String no=updateNo.getText().toString().trim();if(no.isEmpty()){toast("أدخل رقم الشيك أولًا");return;}String[] states={"PENDING","DUE","CLEARED","RETURNED","CANCELLED"};String dir=updateDirection.getSelectedItemPosition()==0?"INCOMING":"OUTGOING";SQLiteDatabase sq=db.getWritableDatabase();ContentValues values=new ContentValues();values.put("status",states[updateStatus.getSelectedItemPosition()]);int changed=sq.update("checks",values,"check_no=? AND direction=?",new String[]{no,dir});if(changed==0){toast("لم يُعثر على شيك بهذا الرقم والاتجاه");return;}toast("تم تحديث حالة الشيك");checks();});
  Cursor c=db.q("SELECT direction,check_no,bank_name,party,amount,due_date,status FROM checks ORDER BY due_date ASC,id DESC");
  while(c.moveToNext()){String d=c.getString(0).equals("INCOMING")?"وارد":"صادر";String st=c.getString(6);String label=st.equals("PENDING")?"قيد الانتظار":st.equals("DUE")?"مستحق":st.equals("CLEARED")?"تم التحصيل/الصرف":st.equals("RETURNED")?"مرتجع":"ملغى";content.addView(t(d+" | شيك "+c.getString(1)+" | "+c.getString(2)+" | "+c.getString(3)+" | "+fmt(c.getDouble(4))+" | الاستحقاق "+c.getString(5)+" | "+label,14));}c.close();
 }
 void employees(){
  page("الموظفون والرواتب");
  content.addView(t("إضافة موظف واحتساب الراتب والسلف والخصومات الشهرية. السلف الجديدة تُرحّل إلى الأستاذ العام؛ الخصومات والرواتب المستحقة تحتاج إلى دورة اعتماد وترحيل مستقلة.",15));
  EditText name=e("اسم الموظف"),job=e("المسمى الوظيفي"),salary=e("الراتب الشهري");salary.setInputType(8194);
  content.addView(name);content.addView(job);content.addView(salary);
  Button add=b("إضافة موظف");content.addView(add);
  add.setOnClickListener(v->{if(name.getText().toString().trim().isEmpty()||!validNumber(salary,true)||num(salary)<0){toast("أدخل اسمًا وراتبًا صحيحًا");return;}try{db.exec("INSERT INTO employees(name,job_title,salary) VALUES(?,?,?)",name.getText().toString().trim(),job.getText().toString().trim(),num(salary));toast("تمت إضافة الموظف");employees();}catch(Exception ex){toast("تعذر حفظ الموظف");}});
  Cursor list=db.q("SELECT id,name,COALESCE(job_title,''),COALESCE(salary,0) FROM employees ORDER BY name");
  ArrayList<Long> ids=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
  while(list.moveToNext()){ids.add(list.getLong(0));labels.add(list.getString(1)+" — "+list.getString(2));}
  list.close();
  if(ids.isEmpty()){content.addView(t("أضف موظفًا أولًا لتسجيل السلف والخصومات.",14));return;}
  content.addView(t("تسجيل سلفة أو خصم",18));
  Spinner person=new Spinner(this);person.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));content.addView(person);
  Spinner kind=new Spinner(this);kind.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{tr("سلفة"),tr("خصم")}));content.addView(kind);
  EditText amount=e("المبلغ"),note=e("البيان");amount.setInputType(8194);content.addView(amount);content.addView(note);
  Button save=b("حفظ الحركة");content.addView(save);
  save.setOnClickListener(v->{double a=num(amount);if(!(!Double.isNaN(a)&&!Double.isInfinite(a))||a<=0){toast("أدخل مبلغًا صحيحًا");return;}String month=new java.text.SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());boolean advance=kind.getSelectedItemPosition()==0;SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{ContentValues ev=new ContentValues();ev.put("employee_id",ids.get(person.getSelectedItemPosition()));ev.put("kind",advance?"ADVANCE":"DEDUCTION");ev.put("amount",a);ev.put("note",note.getText().toString().trim());ev.put("period_month",month);ev.put("created_at",System.currentTimeMillis());ev.put("posted",advance?1:0);long eventId=sq.insertOrThrow("payroll_events",null,ev);if(advance){long advances=accountId(sq,"1300"),cash=accountId(sq,"1010");if(advances==0||cash==0)throw new IllegalStateException("Missing ledger account");long je=journal(sq);line(sq,je,advances,a,0);line(sq,je,cash,0,a);assertBalanced(sq,je);}sq.setTransactionSuccessful();toast(advance?"تم تسجيل السلفة وترحيلها محاسبيًا":"تم تسجيل الخصم في كشف الرواتب فقط؛ يلزم اعتماد قيد الرواتب قبل ترحيله");}catch(Exception ex){toast("تعذر حفظ الحركة؛ لم يتم اعتماد التغيير");}finally{sq.endTransaction();}employees();});
  String month=new java.text.SimpleDateFormat("yyyy-MM",Locale.US).format(new Date());
  Cursor rows=db.q("SELECT e.name,COALESCE(e.job_title,''),COALESCE(e.salary,0),COALESCE(SUM(CASE WHEN p.kind='ADVANCE' THEN p.amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN p.kind='DEDUCTION' THEN p.amount ELSE 0 END),0) FROM employees e LEFT JOIN payroll_events p ON p.employee_id=e.id AND p.period_month=? GROUP BY e.id ORDER BY e.name",month);
  while(rows.moveToNext()){double sal=rows.getDouble(2),adv=rows.getDouble(3),ded=rows.getDouble(4);content.addView(t(rows.getString(0)+" — "+rows.getString(1)+" | الراتب "+fmt(sal)+" | السلف "+fmt(adv)+" | الخصومات "+fmt(ded)+" | صافي المستحق "+fmt(sal-adv-ded),14));}
  rows.close();
 }
 void party(String table,String title){page(title);partyPayments(table,title);content.addView(t("كشف أولي للفواتير والأرصدة المتبقية؛ حد ائتمان العملاء يُطبّق على فواتير البيع الجديدة إذا كان أكبر من صفر. راجع القيود والتقارير قبل اعتمادها نهائيًا.",14));EditText statementName=e("اسم العميل/المورد لعرض الحركات");content.addView(statementName);Button statement=b("عرض كشف الحركات وأعمار المديونية");content.addView(statement);statement.setOnClickListener(v->{String partyName=statementName.getText().toString().trim();if(partyName.isEmpty()){toast("أدخل الاسم أولًا");return;}String txType=title.equals("العملاء")?"SALE":"PURCHASE";String idColumn=title.equals("العملاء")?"customer_id":"supplier_id";String partyTable=title.equals("العملاء")?"customers":"suppliers";Cursor partyCursor=db.q("SELECT id FROM "+partyTable+" WHERE name=? ORDER BY id LIMIT 1",partyName);long partyId=partyCursor.moveToFirst()?partyCursor.getLong(0):-1;partyCursor.close();Cursor rows=db.q("SELECT type,total,paid,remaining,created_at FROM transactions WHERE type=? AND (("+idColumn+" IN (SELECT id FROM "+partyTable+" WHERE name=?)) OR ("+idColumn+" IS NULL AND party=?)) ORDER BY created_at DESC",txType,partyName,partyName);if(!rows.moveToFirst()){rows.close();toast("لا توجد فواتير مطابقة للاسم");return;}StringBuilder out=new StringBuilder("كشف أولي: "+partyName+"\n");do{long age=Math.max(0,(System.currentTimeMillis()-rows.getLong(4))/86400000L);out.append(rows.getString(0)).append(" | الإجمالي ").append(fmt(rows.getDouble(1))).append(" | المدفوع ").append(fmt(rows.getDouble(2))).append(" | المتبقي ").append(fmt(rows.getDouble(3))).append(" | عمر الرصيد ").append(age).append(" يومًا\n");}while(rows.moveToNext());rows.close();new AlertDialog.Builder(this).setTitle("كشف أولي — "+partyName).setMessage(out.toString()).setPositiveButton("حسنًا",null).show();});EditText n=e("الاسم");content.addView(n);EditText credit=e(title.equals("العملاء")?"حد الائتمان (0 = بلا حد)":"حد التعامل (للتوثيق فقط)");credit.setInputType(8194);content.addView(credit);Button add=b("إضافة");content.addView(add);add.setOnClickListener(v->{String nm=n.getText().toString().trim();double lim=num(credit);if(nm.isEmpty()||lim<0||Double.isNaN(lim)||Double.isInfinite(lim)){toast("أدخل اسمًا وحدًا صحيحًا غير سالب");return;}if(table.equals("customers"))db.exec("INSERT INTO customers(name,credit_limit) VALUES(?,?)",nm,lim);else db.exec("INSERT INTO suppliers(name) VALUES(?)",nm);n.setText("");credit.setText("");party(table,title);});Cursor c=db.q(table.equals("customers")?"SELECT name,credit_limit FROM customers ORDER BY id DESC":"SELECT name,0 FROM suppliers ORDER BY id DESC");while(c.moveToNext())content.addView(t("• "+c.getString(0)+(table.equals("customers")?" | حد الائتمان: "+fmt(c.getDouble(1)):""),16));c.close();}
 void partyPayments(String table,String title){
   boolean customer=title.equals("العملاء");String partyType=customer?"CUSTOMER":"SUPPLIER",txType=customer?"SALE":"PURCHASE";
   content.addView(t(customer?"سند قبض من عميل":"سند صرف لمورد",17));
   EditText partyName=e(customer?"اسم العميل":"اسم المورد"),amount=e("مبلغ السند"),note=e("البيان / رقم المرجع");amount.setInputType(8194);
   content.addView(partyName);content.addView(amount);content.addView(note);
   Button save=b(customer?"حفظ سند القبض وترحيل القيد":"حفظ سند الصرف وترحيل القيد");content.addView(save);
   Cursor history=db.q("SELECT p.amount,p.note,p.created_at,p.direction,x.name FROM party_payments p LEFT JOIN "+table+" x ON x.id=p.party_id WHERE p.party_type=? ORDER BY p.created_at DESC LIMIT 10",customer?"CUSTOMER":"SUPPLIER");content.addView(t("آخر 10 سندات قبض/صرف",16));while(history.moveToNext()){String dt=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date(history.getLong(2)));content.addView(t((history.getString(3).equals("RECEIPT")?"قبض":"صرف")+" | "+(history.getString(4)==null?"":history.getString(4))+" | "+fmt(history.getDouble(0))+" | "+dt+" | "+(history.getString(1)==null?"":history.getString(1)),13));}history.close();
   save.setOnClickListener(v->{
    String name=partyName.getText().toString().trim();double value=num(amount);
    if(name.isEmpty()||!validNumber(amount,true)||value<=0){toast("أدخل اسمًا ومبلغًا صحيحًا موجبًا");return;}
    SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();
    try{
     Cursor pc=sq.rawQuery("SELECT id FROM "+table+" WHERE name=? ORDER BY id LIMIT 1",new String[]{name});
     long pid;if(pc.moveToFirst()){pid=pc.getLong(0);pc.close();}else{pc.close();ContentValues pv=new ContentValues();pv.put("name",name);pid=sq.insertOrThrow(table,null,pv);}
     Cursor due=sq.rawQuery("SELECT COALESCE(SUM(remaining),0) FROM transactions WHERE type=? AND ((?='CUSTOMER' AND (customer_id=? OR (customer_id IS NULL AND party=?))) OR (?='SUPPLIER' AND (supplier_id=? OR (supplier_id IS NULL AND party=?))))",new String[]{txType,partyType,String.valueOf(pid),name,partyType,String.valueOf(pid),name});
     double outstanding=due.moveToFirst()?due.getDouble(0):0;due.close();
     if(value>outstanding+0.000001)throw new IllegalArgumentException("المبلغ أكبر من الرصيد المستحق");
     Cursor rows=sq.rawQuery("SELECT id,remaining FROM transactions WHERE type=? AND ((?='CUSTOMER' AND (customer_id=? OR (customer_id IS NULL AND party=?))) OR (?='SUPPLIER' AND (supplier_id=? OR (supplier_id IS NULL AND party=?)))) AND remaining>0 ORDER BY created_at ASC,id ASC",new String[]{txType,partyType,String.valueOf(pid),name,partyType,String.valueOf(pid),name});
     double left=value;
     while(rows.moveToNext()&&left>0.000001){long tid=rows.getLong(0);double rem=rows.getDouble(1),alloc=Math.min(left,rem);sq.execSQL("UPDATE transactions SET paid=paid+?,remaining=MAX(0,remaining-?) WHERE id=?",new Object[]{alloc,alloc,tid});left-=alloc;}
     rows.close();if(left>0.000001)throw new IllegalStateException("تعذر توزيع السند كاملًا");
     long je=journal(sq);long cash=accountId(sq,"1010"),control=accountId(sq,customer?"1100":"2100");
     if(customer){line(sq,je,cash,value,0);line(sq,je,control,0,value);}else{line(sq,je,control,value,0);line(sq,je,cash,0,value);}
     assertBalanced(sq,je);
     ContentValues pv=new ContentValues();pv.put("party_type",partyType);pv.put("party_id",pid);pv.put("direction",customer?"RECEIPT":"PAYMENT");pv.put("amount",value);pv.put("note",note.getText().toString().trim());pv.put("created_at",System.currentTimeMillis());pv.put("journal_entry_id",je);sq.insertOrThrow("party_payments",null,pv);
     sq.setTransactionSuccessful();toast(customer?"تم حفظ سند القبض وتوزيعه وترحيله":"تم حفظ سند الصرف وتوزيعه وترحيله");party(table,title);
    }catch(IllegalArgumentException ex){toast(ex.getMessage()==null?"تحقق من مبلغ السند":ex.getMessage());}
    catch(Exception ex){toast("تعذر حفظ السند؛ أُلغيت العملية كاملة ولم تُعتمد تغييرات جزئية");}
    finally{sq.endTransaction();}
   });
  }
  void recurringExpenses(){
   page("المصروفات الدورية والإيجار والكهرباء");
   content.addView(t("أنشئ استحقاقًا دوريًا ثم أنشئ استحقاق الفترة مرة واحدة. تُرحّل الاستحقاقات إلى المصروفات والذمم؛ راجع الحسابات قبل اعتمادها.",14));
   EditText name=e("البيان: إيجار / كهرباء / إنترنت"),amount=e("قيمة الاستحقاق"),frequency=e("التكرار: شهري أو سنوي"),due=e("تاريخ الاستحقاق YYYY-MM-DD");
   amount.setInputType(8194);content.addView(name);content.addView(amount);content.addView(frequency);content.addView(due);
   Button save=b("حفظ تعريف المصروف الدوري");content.addView(save);
   save.setOnClickListener(view->{String n=name.getText().toString().trim(),f=frequency.getText().toString().trim().toUpperCase(Locale.US),d=due.getText().toString().trim();double x=num(amount);if(n.isEmpty()||x<=0||(Double.isNaN(x)||Double.isInfinite(x))||!(f.equals("شهري")||f.equals("شهريًا")||f.equals("MONTHLY")||f.equals("سنوي")||f.equals("سنويًا")||f.equals("YEARLY"))||!validIsoDate(d,true)){toast("أدخل البيان ومبلغًا موجبًا وتكرارًا شهريًا/سنويًا وتاريخًا صحيحًا");return;}SQLiteDatabase sq=db.getWritableDatabase();ContentValues v=new ContentValues();v.put("name",n);v.put("amount",x);v.put("frequency",(f.startsWith("س")||f.equals("YEARLY"))?"YEARLY":"MONTHLY");v.put("next_due",d);v.put("active",1);try{sq.insertOrThrow("recurring_expenses",null,v);toast("تم حفظ تعريف المصروف الدوري");recurringExpenses();}catch(Exception ex){toast("تعذر الحفظ؛ تحقق من البيانات");}});
   Button generate=b("إنشاء استحقاق الفترة الحالية");content.addView(generate);
   generate.setOnClickListener(v->{Cursor c=db.q("SELECT id,name,amount,frequency,next_due FROM recurring_expenses WHERE active=1 ORDER BY id");int created=0;SQLiteDatabase sq=db.getWritableDatabase();while(c.moveToNext()){long id=c.getLong(0);String n=c.getString(1),f=c.getString(3),d=c.getString(4);String today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date());if(d.compareTo(today)>0)continue;String nowPeriod=new java.text.SimpleDateFormat("yyyy-MM",Locale.US).format(new java.util.Date());String key=f.equals("YEARLY")?nowPeriod.substring(0,4):nowPeriod;sq.beginTransaction();try{ContentValues run=new ContentValues();run.put("recurring_id",id);run.put("period_key",key);run.put("transaction_id",0);long runId=sq.insertOrThrow("recurring_expense_runs",null,run);ContentValues tx=new ContentValues();tx.put("type","EXPENSE");tx.put("party",n+" — "+key);tx.put("total",c.getDouble(2));tx.put("paid",0);tx.put("remaining",c.getDouble(2));tx.put("created_at",System.currentTimeMillis());long tid=sq.insertOrThrow("transactions",null,tx);postJournal(sq,tid,"EXPENSE",c.getDouble(2),0,c.getDouble(2),0);ContentValues up=new ContentValues();up.put("transaction_id",tid);sq.update("recurring_expense_runs",up,"id=?",new String[]{String.valueOf(runId)});sq.setTransactionSuccessful();created++;}catch(Exception ex){/* unique period key prevents duplicate generation */}finally{sq.endTransaction();}}c.close();toast("عدد الاستحقاقات الجديدة: "+created+" — راجع دفتر الأستاذ");recurringExpenses();});
   Cursor c=db.q("SELECT id,name,amount,frequency,next_due,active FROM recurring_expenses ORDER BY id DESC");while(c.moveToNext())content.addView(t(c.getString(1)+" | "+fmt(c.getDouble(2))+" | "+c.getString(3)+" | الاستحقاق "+c.getString(4),14));c.close();
  }
  void fixedAssets(){
   page("الأصول الثابتة والإهلاك");
   content.addView(t("سجل أصل، ثم أنشئ إهلاكًا خطيًا للفترة الحالية. لا يشمل هذا النموذج إعادة التقييم أو طرق الإهلاك المركبة؛ اعتمد العمر والقيمة المتبقية والسياسة مع المحاسب.",14));
   EditText name=e("اسم الأصل"),cost=e("تكلفة الاقتناء"),life=e("العمر الإنتاجي بالأشهر"),date=e("تاريخ الجاهزية YYYY-MM-DD");cost.setInputType(8194);life.setInputType(2);content.addView(name);content.addView(cost);content.addView(life);content.addView(date);
   Button add=b("تسجيل الأصل وترحيل الاقتناء");content.addView(add);
   add.setOnClickListener(view->{String n=name.getText().toString().trim(),d=date.getText().toString().trim();double x=num(cost);int months;try{months=Integer.parseInt(life.getText().toString().trim());}catch(Exception ex){months=0;}if(n.isEmpty()||(Double.isNaN(x)||Double.isInfinite(x))||x<=0||months<1||months>1200||!validIsoDate(d,true)){toast("تحقق من اسم الأصل والتكلفة والعمر والتاريخ");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{long assetAcc=accountId(sq,"1500"),cash=accountId(sq,"1010");if(assetAcc==0||cash==0)throw new IllegalStateException("missing accounts");ContentValues v=new ContentValues();v.put("name",n);v.put("cost",x);v.put("life_months",months);v.put("ready_date",d);v.put("created_at",System.currentTimeMillis());long id=sq.insertOrThrow("fixed_assets",null,v);long je=journal(sq);line(sq,je,assetAcc,x,0);line(sq,je,cash,0,x);assertBalanced(sq,je);ContentValues jv=new ContentValues();jv.put("acquisition_journal_id",je);sq.update("fixed_assets",jv,"id=?",new String[]{String.valueOf(id)});sq.setTransactionSuccessful();toast("تم تسجيل الأصل وترحيل اقتنائه");fixedAssets();}catch(Exception ex){toast("تعذر تسجيل الأصل؛ لم تعتمد العملية");}finally{sq.endTransaction();}});
   Button dep=b("احتساب وترحيل إهلاك الشهر الحالي");content.addView(dep);dep.setOnClickListener(view->{String period=new java.text.SimpleDateFormat("yyyy-MM",Locale.US).format(new java.util.Date()),today=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new java.util.Date());SQLiteDatabase sq=db.getWritableDatabase();Cursor c=db.q("SELECT id,name,cost,life_months,ready_date FROM fixed_assets ORDER BY id");int done=0;while(c.moveToNext()){long id=c.getLong(0);String n=c.getString(1),rd=c.getString(4);if(rd.compareTo(today)>0)continue;Cursor ac=db.q("SELECT COALESCE(SUM(amount),0) FROM asset_depreciation WHERE asset_id=?",String.valueOf(id));double accumulated=ac.moveToFirst()?ac.getDouble(0):0;ac.close();double monthly=Math.min(c.getDouble(2)-accumulated,c.getDouble(2)/c.getInt(3));if(monthly<=0.005)continue;sq.beginTransaction();try{ContentValues v=new ContentValues();v.put("asset_id",id);v.put("period",period);v.put("amount",monthly);long depId=sq.insertOrThrow("asset_depreciation",null,v);long je=journal(sq);line(sq,je,accountId(sq,"6100"),monthly,0);line(sq,je,accountId(sq,"1590"),0,monthly);assertBalanced(sq,je);ContentValues up=new ContentValues();up.put("journal_entry_id",je);sq.update("asset_depreciation",up,"id=?",new String[]{String.valueOf(depId)});sq.setTransactionSuccessful();done++;}catch(Exception ex){/* unique asset-period key prevents duplicate depreciation */}finally{sq.endTransaction();}}c.close();toast("عدد قيود الإهلاك الجديدة: "+done);fixedAssets();});
   Cursor c=db.q("SELECT a.id,a.name,a.cost,a.life_months,a.ready_date,COALESCE(SUM(d.amount),0) FROM fixed_assets a LEFT JOIN asset_depreciation d ON d.asset_id=a.id GROUP BY a.id ORDER BY a.id DESC");while(c.moveToNext())content.addView(t(c.getString(1)+" | تكلفة "+fmt(c.getDouble(2))+" | مجمع الإهلاك "+fmt(c.getDouble(5))+" | صافي "+fmt(c.getDouble(2)-c.getDouble(5)),14));c.close();
  }
  void factoryOperations(){
   page("حساب التشغيل والتصنيع المرحلي");
   content.addView(t("سجل أمر التشغيل ومراحل الإنتاج مع المواد والعمالة والمصاريف والتالف. هذا السجل يثبت تكاليف المراحل؛ لا يُرحّل إلى مخزون المنتج النهائي آليًا في هذه النسخة، ويجب على الخبراء التحقق من مطابقة التكاليف قبل الاعتماد.",14));
   EditText order=e("رقم/اسم أمر التشغيل"),stage=e("اسم المرحلة"),material=e("تكلفة المواد المصروفة"),labor=e("العمالة"),overhead=e("المصاريف الصناعية"),good=e("كمية المنتج الجيد"),scrap=e("كمية التالف");for(EditText x:new EditText[]{material,labor,overhead,good,scrap})x.setInputType(8194);for(EditText x:new EditText[]{order,stage,material,labor,overhead,good,scrap})content.addView(x);
   Button save=b("حفظ مرحلة التشغيل");content.addView(save);save.setOnClickListener(v->{String o=order.getText().toString().trim(),s=stage.getText().toString().trim();double m=num(material),l=num(labor),h=num(overhead),g=num(good),sc=num(scrap);if(o.isEmpty()||s.isEmpty()||m<0||l<0||h<0||g<0||sc<0||(Double.isNaN(m+l+h+g+sc)||Double.isInfinite(m+l+h+g+sc))||g+sc<=0){toast("أدخل أمرًا ومرحلة وتكاليف وكميات صحيحة");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{ContentValues ov=new ContentValues();ov.put("order_name",o);long oid=sq.insertWithOnConflict("factory_orders",null,ov,SQLiteDatabase.CONFLICT_IGNORE);Cursor q=sq.rawQuery("SELECT id FROM factory_orders WHERE order_name=?",new String[]{o});if(!q.moveToFirst())throw new IllegalStateException();oid=q.getLong(0);q.close();ContentValues ev=new ContentValues();ev.put("order_id",oid);ev.put("stage_name",s);ev.put("material_cost",m);ev.put("labor_cost",l);ev.put("overhead_cost",h);ev.put("good_qty",g);ev.put("scrap_qty",sc);ev.put("created_at",System.currentTimeMillis());long eventId=sq.insertOrThrow("factory_stage_events",null,ev);double totalCost=m+l+h;if(totalCost>0){long wip=accountId(sq,"1400"),raw=accountId(sq,"1210"),laborPayable=accountId(sq,"2200"),overheadPayable=accountId(sq,"2300");if(wip<=0||(m>0&&raw<=0)||(l>0&&laborPayable<=0)||(h>0&&overheadPayable<=0))throw new IllegalStateException("Factory cost accounts are not configured");long je=journal(sq);line(sq,je,wip,totalCost,0);if(m>0)line(sq,je,raw,0,m);if(l>0)line(sq,je,laborPayable,0,l);if(h>0)line(sq,je,overheadPayable,0,h);assertBalanced(sq,je);ContentValues jv=new ContentValues();jv.put("journal_entry_id",je);sq.update("factory_stage_events",jv,"id=?",new String[]{String.valueOf(eventId)});}sq.setTransactionSuccessful();toast("تم حفظ مرحلة التشغيل وترحيل تكلفتها إلى مخزون تحت التشغيل؛ راجع كميات المواد والمصادر والتالف");factoryOperations();}catch(Exception ex){toast("تعذر حفظ المرحلة");}finally{sq.endTransaction();}});
   Cursor c=db.q("SELECT o.order_name,e.stage_name,e.material_cost,e.labor_cost,e.overhead_cost,e.good_qty,e.scrap_qty FROM factory_stage_events e JOIN factory_orders o ON o.id=e.order_id ORDER BY e.id DESC");while(c.moveToNext())content.addView(t(c.getString(0)+" | "+c.getString(1)+" | تكاليف "+fmt(c.getDouble(2)+c.getDouble(3)+c.getDouble(4))+" | جيد "+c.getDouble(5)+" | تالف "+c.getDouble(6),14));c.close();
  }

  void cash(){page("الصندوق والمصروفات");EditText desc=e("البيان"),amt=e("المبلغ");amt.setInputType(8194);content.addView(desc);content.addView(amt);Button add=b("تسجيل مصروف");content.addView(add);add.setOnClickListener(v->{double x=num(amt);if(!validNumber(amt,true)||x<=0){toast("أدخل مبلغًا موجبًا وصحيحًا للمصروف");return;}SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{ContentValues tv=new ContentValues();tv.put("type","EXPENSE");tv.put("party",desc.getText().toString());tv.put("total",x);tv.put("paid",x);tv.put("remaining",0);tv.put("created_at",System.currentTimeMillis());long tid=sq.insertOrThrow("transactions",null,tv);postJournal(sq,tid,"EXPENSE",x,x,0,0);sq.setTransactionSuccessful();}catch(Exception ex){toast("تعذر تسجيل المصروف؛ لم تُعتمد العملية");return;}finally{sq.endTransaction();}toast("تم تسجيل المصروف وترحيله محاسبيًا");dashboard();});}

 void reports(){
   page("التقارير والتحليل");
   content.addView(t("اختر الفترة بالتنسيق YYYY-MM-DD. اترك الحقلين فارغين لعرض كل السجل. النتائج أولية وليست قوائم مالية معتمدة لأن بعض حركات البنوك والرواتب والشيكات لم تكتمل ترحيلاتها.",14));
   EditText from=e("من تاريخ — اضغط لاختيار التاريخ، واضغط مطولًا للمسح"),to=e("إلى تاريخ — اضغط لاختيار التاريخ، واضغط مطولًا للمسح");
   attachDatePicker(from);attachDatePicker(to);content.addView(from);content.addView(to);
   Button apply=b("عرض التقرير للفترة");content.addView(apply);
   apply.setOnClickListener(v->{
    try{
     java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);sdf.setLenient(false);
     String a=from.getText().toString().trim(),z=to.getText().toString().trim();
     if((!a.isEmpty()&&!a.matches("\\d{4}-\\d{2}-\\d{2}"))||(!z.isEmpty()&&!z.matches("\\d{4}-\\d{2}-\\d{2}")))throw new IllegalArgumentException();
     long start=0,end=Long.MAX_VALUE;
     if(!a.isEmpty()){java.util.Date d=sdf.parse(a);if(d==null)throw new IllegalArgumentException();start=d.getTime();}
     if(!z.isEmpty()){java.util.Date d=sdf.parse(z);if(d==null)throw new IllegalArgumentException();java.util.Calendar cal=java.util.Calendar.getInstance();cal.setTime(d);cal.add(java.util.Calendar.DAY_OF_MONTH,1);end=cal.getTimeInMillis()-1;}
     if(start>end)throw new IllegalArgumentException();
     content.removeViews(4,content.getChildCount()-4);
     renderReport(start,end,(a.isEmpty()?"البداية":a)+" — "+(z.isEmpty()?"حتى الآن":z));
    }catch(Exception ex){toast("صيغة التاريخ غير صحيحة أو أن البداية بعد النهاية؛ استخدم YYYY-MM-DD");}
   });
   renderReport(0,Long.MAX_VALUE,"كل السجل");
  }
  void renderReport(long start,long end,String period){
   content.addView(t("الفترة: "+period,16));
   String[] args={String.valueOf(start),String.valueOf(end)};
   Cursor c=db.q("SELECT type,COALESCE(SUM(total),0),COALESCE(SUM(paid),0),COALESCE(SUM(remaining),0) FROM transactions WHERE created_at BETWEEN ? AND ? GROUP BY type ORDER BY type",args);
   while(c.moveToNext())content.addView(t(c.getString(0)+" | الإجمالي "+fmt(c.getDouble(1))+" | المدفوع "+fmt(c.getDouble(2))+" | المتبقي "+fmt(c.getDouble(3)),17));c.close();
   Cursor p=db.q("SELECT COALESCE(SUM(CASE WHEN t.type='SALE_RETURN' THEN -((i.price-i.cost)*i.qty-i.discount) ELSE ((i.price-i.cost)*i.qty-i.discount) END),0) FROM invoice_items i JOIN transactions t ON t.id=i.transaction_id WHERE t.type IN ('SALE','SALE_RETURN') AND t.created_at BETWEEN ? AND ?",args);double gp=p.moveToFirst()?p.getDouble(0):0;p.close();
   Cursor ex=db.q("SELECT COALESCE(SUM(total),0) FROM transactions WHERE type='EXPENSE' AND created_at BETWEEN ? AND ?",args);double expense=ex.moveToFirst()?ex.getDouble(0):0;ex.close();
   content.addView(t("مجمل الربح التقديري: "+fmt(gp),20));content.addView(t("تقدير تشغيلي أولي بعد المصروفات: "+fmt(gp-expense),20));
   Button tb=b("ميزان المراجعة");content.addView(tb);tb.setOnClickListener(v->trialBalance());
   Button print=b("طباعة التقرير / PDF");content.addView(print);print.setOnClickListener(v->printText("التقرير المالي",reportText(start,end,period)));
   Button email=b("إرسال التقرير بالبريد الإلكتروني");content.addView(email);email.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,tr("التقرير المالي"));i.putExtra(Intent.EXTRA_TEXT,reportText(start,end,period));startActivity(Intent.createChooser(i,tr("إرسال التقرير بالبريد الإلكتروني")));});
  }
  String reportText(long start,long end,String period){
   StringBuilder out=new StringBuilder(tr("الفترة")).append(": ").append(period).append("\n");
   Cursor c=db.q("SELECT type,COALESCE(SUM(total),0),COALESCE(SUM(paid),0),COALESCE(SUM(remaining),0) FROM transactions WHERE created_at BETWEEN ? AND ? GROUP BY type ORDER BY type",String.valueOf(start),String.valueOf(end));
   while(c.moveToNext())out.append(c.getString(0)).append(" | ").append(tr("الإجمالي")).append(": ").append(fmt(c.getDouble(1))).append(" | ").append(tr("المدفوع")).append(": ").append(fmt(c.getDouble(2))).append(" | ").append(tr("المتبقي")).append(": ").append(fmt(c.getDouble(3))).append("\n");c.close();
   Cursor p=db.q("SELECT COALESCE(SUM(CASE WHEN t.type='SALE_RETURN' THEN -((i.price-i.cost)*i.qty-i.discount) ELSE ((i.price-i.cost)*i.qty-i.discount) END),0) FROM invoice_items i JOIN transactions t ON t.id=i.transaction_id WHERE t.type IN ('SALE','SALE_RETURN') AND t.created_at BETWEEN ? AND ?",String.valueOf(start),String.valueOf(end));double gp=p.moveToFirst()?p.getDouble(0):0;p.close();
   Cursor ex=db.q("SELECT COALESCE(SUM(total),0) FROM transactions WHERE type='EXPENSE' AND created_at BETWEEN ? AND ?",String.valueOf(start),String.valueOf(end));double expense=ex.moveToFirst()?ex.getDouble(0):0;ex.close();
   out.append(tr("مجمل الربح التقديري")).append(": ").append(fmt(gp)).append("\n").append(tr("تقدير تشغيلي أولي")).append(": ").append(fmt(gp-expense)).append("\n").append(tr("تنبيه: النتائج أولية وليست قوائم مالية معتمدة."));
   return out.toString();
  }
  void printText(String title,String body){WebView web=new WebView(this);web.setWebViewClient(new WebViewClient(){public void onPageFinished(WebView view,String url){PrintManager pm=(PrintManager)getSystemService(Context.PRINT_SERVICE);PrintAttributes attrs=new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("smartcompany","Smart Company",300,300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build();pm.print(title,view.createPrintDocumentAdapter(title),attrs);}});String safe=body.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");String html="<html><body dir='"+(english()?"ltr":"rtl")+"' style='font-family:sans-serif;padding:24px'><h2>"+tr(title)+"</h2><pre style='white-space:pre-wrap;font-size:14pt'>"+safe+"</pre><p>"+tr("توقيع المحاسب:")+" ____________________</p></body></html>";web.loadDataWithBaseURL(null,html,"text/html","UTF-8",null);}
 void backup(){page("النسخ الاحتياطي");content.addView(t("تُشفّر النسخة بكلمة مرور تختارها. احفظ كلمة المرور منفصلة؛ لا يمكن استعادة النسخة دونها. استعادة نسخة قديمة تستبدل البيانات الحالية.",15));Button x=b("تصدير نسخة مشفرة");content.addView(x);x.setOnClickListener(v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"AlMizan-encrypted-backup.almbak");startActivityForResult(i,900);});Button restore=b("استعادة نسخة مشفرة");content.addView(restore);restore.setOnClickListener(v->{new AlertDialog.Builder(this).setTitle(tr("استعادة نسخة قاعدة البيانات")).setMessage(tr("سيتم استبدال بيانات التطبيق الحالية بالنسخة المختارة. أنشئ نسخة حديثة أولًا، وتأكد من معرفة كلمة مرور النسخة المشفرة.")).setNegativeButton(tr("إلغاء"),(d,w)->{}).setPositiveButton(tr("اختيار ملف"),(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,901);}).show();});}
  void startBarcodeScan(){try{com.google.zxing.integration.android.IntentIntegrator scanner=new com.google.zxing.integration.android.IntentIntegrator(this);scanner.setPrompt(tr("وجّه الكاميرا نحو الباركود"));scanner.setBeepEnabled(false);scanner.setOrientationLocked(false);scanner.setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.ALL_CODE_TYPES);scanner.initiateScan();}catch(Exception ex){toast("تعذر تشغيل الكاميرا أو ماسح الباركود");}}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);com.google.zxing.integration.android.IntentResult scanResult=com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(request,result,data);if(scanResult!=null){if(scanResult.getContents()!=null&&pendingBarcodeTarget!=null){pendingBarcodeTarget.setText(scanResult.getContents().trim());toast("تمت قراءة الباركود");}pendingBarcodeTarget=null;return;}if(data==null||data.getData()==null||result!=RESULT_OK)return;if(request==902){planLogoUri=data.getData();toast("تم اختيار شعار الشركة");return;}else if(request==903){planReceiptUri=data.getData();toast("تم اختيار إيصال التحويل");return;}if(request==900){promptBackupPassword(data.getData(),true);}else if(request==901){promptBackupPassword(data.getData(),false);}}
 
  void promptBackupPassword(android.net.Uri uri,boolean exporting){
   EditText password=new EditText(this);password.setSingleLine(true);password.setInputType(0x81);
   password.setHint(exporting?"كلمة مرور النسخة (10 أحرف على الأقل)":"كلمة مرور النسخة الاحتياطية");
   new AlertDialog.Builder(this).setTitle(exporting?"تشفير النسخة الاحتياطية":"فك تشفير النسخة الاحتياطية")
    .setMessage(exporting?"اختر كلمة مرور قوية واحفظها في مكان آمن. لن يمكن استعادة النسخة دونها.":"أدخل كلمة المرور التي استُخدمت عند إنشاء النسخة.")
    .setView(password).setNegativeButton("إلغاء",null).setPositiveButton("متابعة",(d,w)->{
     String pass=password.getText().toString();
     if(exporting&&pass.length()<10){toast("استخدم كلمة مرور من 10 أحرف على الأقل");return;}
     if(!exporting&&pass.isEmpty()){toast("أدخل كلمة مرور النسخة");return;}
     if(exporting)writeEncryptedBackup(uri,pass);else restoreEncryptedBackup(uri,pass);
    }).show();
  }
  javax.crypto.SecretKey backupKey(String password,byte[] salt)throws Exception{
   javax.crypto.spec.PBEKeySpec spec=new javax.crypto.spec.PBEKeySpec(password.toCharArray(),salt,210000,256);
   try{return new javax.crypto.spec.SecretKeySpec(javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded(),"AES");}
   finally{spec.clearPassword();}
  }
  void writeEncryptedBackup(android.net.Uri uri,String password){
   try{
    Cursor checkpoint=db.getWritableDatabase().rawQuery("PRAGMA wal_checkpoint(FULL)",null);try{if(!checkpoint.moveToFirst()||checkpoint.getInt(0)!=0)throw new IOException("Database checkpoint is busy");}finally{checkpoint.close();}
    byte[] salt=new byte[16],iv=new byte[12];java.security.SecureRandom random=new java.security.SecureRandom();random.nextBytes(salt);random.nextBytes(iv);
    javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,backupKey(password,salt),new javax.crypto.spec.GCMParameterSpec(128,iv));
    try(InputStream in=new FileInputStream(getDatabasePath("smartcompany2.db"));OutputStream raw=getContentResolver().openOutputStream(uri)){
     if(raw==null)throw new IOException("Output stream unavailable");
     raw.write(new byte[]{'A','L','M','I','Z','A','N','1'});raw.write(salt);raw.write(iv);
     javax.crypto.CipherOutputStream encrypted=new javax.crypto.CipherOutputStream(raw,cipher);
     byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)encrypted.write(buf,0,n);encrypted.close();
    }
    toast("تم تصدير نسخة احتياطية مشفرة. احتفظ بكلمة المرور منفصلة.");
   }catch(Exception ex){toast("تعذر إنشاء النسخة المشفرة");}
  }
  void restoreEncryptedBackup(android.net.Uri uri,String password){
   File temp=new File(getCacheDir(),"restore-check.db");File target=getDatabasePath("smartcompany2.db");File staged=new File(target.getPath()+".restore-new");File previous=new File(target.getPath()+".restore-old");
   try{
    try(InputStream raw=getContentResolver().openInputStream(uri)){
     if(raw==null)throw new IOException("Input stream unavailable");
     byte[] magic=new byte[8],salt=new byte[16],iv=new byte[12];
     if(readFully(raw,magic)!=8||!java.util.Arrays.equals(magic,new byte[]{'A','L','M','I','Z','A','N','1'}))throw new IOException("Unsupported backup format");
     if(readFully(raw,salt)!=16||readFully(raw,iv)!=12)throw new IOException("Truncated backup header");
     javax.crypto.Cipher cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
     cipher.init(javax.crypto.Cipher.DECRYPT_MODE,backupKey(password,salt),new javax.crypto.spec.GCMParameterSpec(128,iv));
     try(FileOutputStream out=new FileOutputStream(temp)){byte[] buf=new byte[8192];int n;while((n=raw.read(buf))!=-1){byte[] plain=cipher.update(buf,0,n);if(plain!=null&&plain.length>0)out.write(plain);}byte[] tail=cipher.doFinal();if(tail!=null&&tail.length>0)out.write(tail);out.getFD().sync();}
    }
    boolean valid=false;SQLiteDatabase check=null;
    try{
     check=SQLiteDatabase.openDatabase(temp.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
     Cursor integrity=check.rawQuery("PRAGMA integrity_check",null);
     try{valid=integrity.moveToFirst()&&"ok".equalsIgnoreCase(integrity.getString(0))&&!integrity.moveToNext();}finally{integrity.close();}
     Cursor tables=check.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('products','transactions','accounts','journal_entries','journal_lines')",null);
     try{valid=valid&&tables.moveToFirst()&&tables.getInt(0)==5;}finally{tables.close();}
     Cursor version=check.rawQuery("PRAGMA user_version",null);
     try{valid=valid&&version.moveToFirst()&&version.getInt(0)>=0&&version.getInt(0)<=11;}finally{version.close();}
    }finally{if(check!=null)check.close();}
    if(!valid)throw new IOException("Invalid or unauthenticated database backup");
    try(InputStream in=new FileInputStream(temp);FileOutputStream out=new FileOutputStream(staged)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);out.getFD().sync();}
    if(db!=null)db.close();new File(target.getPath()+"-wal").delete();new File(target.getPath()+"-shm").delete();
    if(previous.exists()&&!previous.delete())throw new IOException("Cannot clear previous database rollback file");
    if(target.exists()&&!target.renameTo(previous))throw new IOException("Cannot preserve current database before restore");
    if(!staged.renameTo(target)){if(previous.exists())previous.renameTo(target);throw new IOException("Cannot atomically install restored database");}
    db=new DB(this);db.getWritableDatabase();if(previous.exists())previous.delete();temp.delete();
    toast("تمت استعادة النسخة المشفرة؛ راجع الأرصدة والتقارير");if(authenticated)dashboard();else login();
   }catch(Exception ex){
    staged.delete();if(previous.exists()){if(target.exists())target.delete();previous.renameTo(target);}temp.delete();
    if(db!=null)try{db.close();}catch(Exception ignored){}db=new DB(this);try{db.getWritableDatabase();}catch(Exception ignored){}
    toast("فشلت الاستعادة. تحقق من كلمة المرور وسلامة الملف؛ حُفظت البيانات السابقة إن أمكن");
   }
  }
  int readFully(InputStream in,byte[] bytes)throws IOException{int offset=0;while(offset<bytes.length){int n=in.read(bytes,offset,bytes.length-offset);if(n<0)break;offset+=n;}return offset;}

  void manualJournal(){
   page("القيود اليدوية والقيد العكسي");
   Button reverseButton=b("↩️ فتح شاشة القيد العكسي للقيود اليدوية");content.addView(reverseButton);reverseButton.setOnClickListener(v->reverseManualJournal());
   content.addView(t("أدخل سطرين على الأقل. يجب أن يكون كل سطر مدينًا أو دائنًا فقط، وأن يتساوى إجمالي الطرفين. القيود المحفوظة لا تُحذف من هذه الشاشة.",14));
   Cursor ac=db.q("SELECT id,code,name FROM accounts ORDER BY code");
   ArrayList<Long> accountIds=new ArrayList<>();ArrayList<String> accountLabels=new ArrayList<>();
   while(ac.moveToNext()){accountIds.add(ac.getLong(0));accountLabels.add(ac.getString(1)+" — "+ac.getString(2));}ac.close();
   if(accountIds.size()<2){content.addView(t("يلزم وجود حسابين على الأقل في دليل الحسابات.",14));return;}
   EditText memo=e("وصف القيد / المرجع");content.addView(memo);
   LinearLayout linesBox=new LinearLayout(this);linesBox.setOrientation(LinearLayout.VERTICAL);content.addView(linesBox);
   ArrayList<Spinner> accounts=new ArrayList<>();ArrayList<EditText> debits=new ArrayList<>();ArrayList<EditText> credits=new ArrayList<>();
   Runnable addLine=()->{LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);Spinner sp=new Spinner(this);sp.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,accountLabels));EditText dr=e("مدين");dr.setInputType(8194);EditText cr=e("دائن");cr.setInputType(8194);row.addView(t("سطر "+(accounts.size()+1),14));row.addView(sp);LinearLayout amounts=new LinearLayout(this);amounts.setOrientation(LinearLayout.HORIZONTAL);amounts.addView(dr,new LinearLayout.LayoutParams(0,-2,1));amounts.addView(cr,new LinearLayout.LayoutParams(0,-2,1));row.addView(amounts);linesBox.addView(row);accounts.add(sp);debits.add(dr);credits.add(cr);};
   addLine.run();addLine.run();Button more=b("إضافة سطر للقيد");content.addView(more);more.setOnClickListener(v->{if(accounts.size()>=20){toast("الحد الأقصى 20 سطرًا للقيد الواحد");return;}addLine.run();});
   Button save=b("اعتماد القيد اليدوي");content.addView(save);save.setOnClickListener(v->{
    String description=memo.getText().toString().trim();if(description.isEmpty()){toast("أدخل وصفًا أو مرجعًا للقيد");return;}
    double totalDr=0,totalCr=0;ArrayList<double[]> values=new ArrayList<>();
    for(int i=0;i<accounts.size();i++){if(!validNumber(debits.get(i),false)||!validNumber(credits.get(i),false)){toast("أدخل أرقامًا صحيحة في جميع الأسطر");return;}double dr=num(debits.get(i)),cr=num(credits.get(i));if(dr<0||cr<0||Double.isInfinite(dr)||Double.isInfinite(cr)||Double.isNaN(dr)||Double.isNaN(cr)||(dr>0&&cr>0)){toast("كل سطر يجب أن يكون مدينًا أو دائنًا فقط وبقيمة غير سالبة");return;}if(dr==0&&cr==0)continue;totalDr+=dr;totalCr+=cr;values.add(new double[]{accountIds.get(accounts.get(i).getSelectedItemPosition()),dr,cr});}
    if(values.size()<2||Math.abs(totalDr-totalCr)>0.005||totalDr<=0){toast("القيد غير متوازن أو لا يحتوي على سطرين ماليين على الأقل");return;}
    SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();try{long je=journal(sq);ContentValues desc=new ContentValues();desc.put("description",description);sq.update("journal_entries",desc,"id=?",new String[]{String.valueOf(je)});for(double[] ln:values)line(sq,je,(long)ln[0],ln[1],ln[2]);assertBalanced(sq,je);sq.setTransactionSuccessful();toast("تم اعتماد القيد اليدوي المتوازن");trialBalance();}catch(Exception ex){toast("تعذر اعتماد القيد؛ أُلغيت العملية بالكامل");}finally{sq.endTransaction();}
   });
   Cursor recent=db.q("SELECT j.id,j.description,j.created_at,COALESCE(SUM(l.debit),0),COALESCE(SUM(l.credit),0) FROM journal_entries j JOIN journal_lines l ON l.journal_entry_id=j.id WHERE j.transaction_id IS NULL AND j.description IS NOT NULL GROUP BY j.id ORDER BY j.created_at DESC LIMIT 10");content.addView(t("آخر القيود اليدوية",17));while(recent.moveToNext()){content.addView(t("#"+recent.getLong(0)+" | "+recent.getString(1)+" | "+new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date(recent.getLong(2)))+" | مدين "+fmt(recent.getDouble(3))+" | دائن "+fmt(recent.getDouble(4)),14));}recent.close();
  }
  void reverseManualJournal(){
   page("القيد العكسي للقيود اليدوية");
   content.addView(t("هذه الشاشة تعكس القيود اليدوية المؤهلة فقط. لا تستخدمها لعكس فاتورة بيع أو شراء أو قيد أصل أو إهلاك أو تصنيع؛ استخدم مسار الإلغاء/المرتجع الخاص بالعملية الأصلية.",14));
   Cursor cur=db.q("SELECT j.id,COALESCE(j.description,''),j.created_at,COALESCE(SUM(l.debit),0) FROM journal_entries j JOIN journal_lines l ON l.journal_entry_id=j.id WHERE j.transaction_id IS NULL AND j.description IS NOT NULL AND j.description NOT LIKE 'مرتجع%' AND j.description NOT LIKE 'قيد عكسي%' AND NOT EXISTS(SELECT 1 FROM journal_reversals r WHERE r.original_entry_id=j.id OR r.reversal_entry_id=j.id) AND NOT EXISTS(SELECT 1 FROM fixed_assets a WHERE a.acquisition_journal_id=j.id) AND NOT EXISTS(SELECT 1 FROM asset_depreciation d WHERE d.journal_entry_id=j.id) AND NOT EXISTS(SELECT 1 FROM factory_stage_events f WHERE f.journal_entry_id=j.id) GROUP BY j.id ORDER BY j.created_at DESC");
   ArrayList<Long> ids=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
   while(cur.moveToNext()){long id=cur.getLong(0);ids.add(id);labels.add("#"+id+" | "+cur.getString(1)+" | مدين "+fmt(cur.getDouble(3)));}
   cur.close();
   if(ids.isEmpty()){content.addView(t("لا توجد قيود يدوية مؤهلة للعكس حاليًا. أنشئ قيدًا يدويًا متوازنًا أولًا.",15));Button back=b("العودة إلى القيود اليدوية");content.addView(back);back.setOnClickListener(v->manualJournal());return;}
   Spinner choices=new Spinner(this);choices.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,labels));content.addView(choices);
   EditText reason=e("سبب القيد العكسي — مطلوب");content.addView(reason);
   Button reverse=b("إنشاء القيد العكسي مرة واحدة");content.addView(reverse);
   reverse.setOnClickListener(v->{
    String why=reason.getText().toString().trim();if(why.isEmpty()){toast("اكتب سبب القيد العكسي");return;}
    long original=ids.get(choices.getSelectedItemPosition());SQLiteDatabase sq=db.getWritableDatabase();sq.beginTransaction();
    try{
     Cursor check=sq.rawQuery("SELECT 1 FROM journal_entries j WHERE j.id=? AND j.transaction_id IS NULL AND j.description IS NOT NULL AND NOT EXISTS(SELECT 1 FROM journal_reversals r WHERE r.original_entry_id=j.id OR r.reversal_entry_id=j.id) AND NOT EXISTS(SELECT 1 FROM fixed_assets a WHERE a.acquisition_journal_id=j.id) AND NOT EXISTS(SELECT 1 FROM asset_depreciation d WHERE d.journal_entry_id=j.id) AND NOT EXISTS(SELECT 1 FROM factory_stage_events f WHERE f.journal_entry_id=j.id)",new String[]{String.valueOf(original)});
     boolean eligible=check.moveToFirst();check.close();if(!eligible)throw new IllegalStateException("Journal is not eligible or has already been reversed");
     Cursor lines=sq.rawQuery("SELECT account_id,debit,credit FROM journal_lines WHERE journal_entry_id=? ORDER BY id",new String[]{String.valueOf(original)});
     ArrayList<double[]> entries=new ArrayList<>();while(lines.moveToNext())entries.add(new double[]{lines.getLong(0),lines.getDouble(1),lines.getDouble(2)});lines.close();
     if(entries.size()<2)throw new IllegalStateException("Original journal has insufficient lines");
     long reversal=journal(sq);ContentValues desc=new ContentValues();desc.put("description","قيد عكسي للقيد #"+original+" — "+why);sq.update("journal_entries",desc,"id=?",new String[]{String.valueOf(reversal)});
     for(double[] line:entries)line(sq,reversal,(long)line[0],line[2],line[1]);assertBalanced(sq,reversal);
     ContentValues audit=new ContentValues();audit.put("original_entry_id",original);audit.put("reversal_entry_id",reversal);audit.put("reason",why);audit.put("created_at",System.currentTimeMillis());sq.insertOrThrow("journal_reversals",null,audit);
     sq.setTransactionSuccessful();toast("تم إنشاء القيد العكسي #"+reversal+" وحفظ سجل التدقيق");trialBalance();
    }catch(Exception ex){toast("تعذر إنشاء القيد العكسي؛ لم تعتمد العملية أو سبق عكس القيد");}
    finally{sq.endTransaction();}
   });
  }

 void trialBalance(){page("ميزان المراجعة");Cursor c=db.q("SELECT a.code,a.name,COALESCE(SUM(j.debit),0),COALESCE(SUM(j.credit),0) FROM accounts a LEFT JOIN journal_lines j ON j.account_id=a.id GROUP BY a.id ORDER BY a.code");double d=0,cr=0;while(c.moveToNext()){double x=c.getDouble(2),y=c.getDouble(3);d+=x;cr+=y;content.addView(t(c.getString(0)+" — "+c.getString(1)+" | مدين "+fmt(x)+" | دائن "+fmt(y),15));}c.close();content.addView(t("إجمالي المدين: "+fmt(d)+" | الدائن: "+fmt(cr)+" | الفرق: "+fmt(d-cr),18));}
 long journal(SQLiteDatabase sq){ContentValues j=new ContentValues();j.put("created_at",System.currentTimeMillis());return sq.insertOrThrow("journal_entries",null,j);}
 void postJournal(SQLiteDatabase sq,long tid,String type,double total,double paid,double remaining,double cogs){if(tid>0){Cursor existing=sq.rawQuery("SELECT 1 FROM journal_entries WHERE transaction_id=? LIMIT 1",new String[]{String.valueOf(tid)});boolean alreadyPosted=existing.moveToFirst();existing.close();if(alreadyPosted)throw new IllegalStateException("Transaction already has a journal entry: "+tid);}if(type==null||!(type.equals("SALE")||type.equals("PURCHASE")||type.equals("EXPENSE")))throw new IllegalArgumentException("Unsupported journal type");if((Double.isNaN(total)||Double.isInfinite(total))||(Double.isNaN(paid)||Double.isInfinite(paid))||(Double.isNaN(remaining)||Double.isInfinite(remaining))||(Double.isNaN(cogs)||Double.isInfinite(cogs))||total<0||paid<0||remaining<0||cogs<0||Math.abs(total-paid-remaining)>0.005)throw new IllegalArgumentException("Invalid journal amounts");long cash=accountId(sq,"1010"),recv=accountId(sq,"1100"),inv=accountId(sq,"1200"),sales=accountId(sq,"4000"),payable=accountId(sq,"2100"),expense=accountId(sq,"6000"),cogsAcc=accountId(sq,"5100");long je=sq.insertOrThrow("journal_entries",null,cv("transaction_id",tid,"created_at",System.currentTimeMillis()));if(type.equals("SALE")){line(sq,je,cash,paid,0);line(sq,je,recv,remaining,0);line(sq,je,sales,0,total);line(sq,je,cogsAcc,cogs,0);line(sq,je,inv,0,cogs);}else if(type.equals("PURCHASE")){line(sq,je,inv,total,0);line(sq,je,cash,0,paid);line(sq,je,payable,0,remaining);}else{line(sq,je,expense,total,0);line(sq,je,cash,0,paid);if(remaining>0)line(sq,je,payable,0,remaining);}assertBalanced(sq,je);}
 void assertBalanced(SQLiteDatabase sq,long je){Cursor c=sq.rawQuery("SELECT COALESCE(SUM(debit),0),COALESCE(SUM(credit),0) FROM journal_lines WHERE journal_entry_id=?",new String[]{String.valueOf(je)});double d=0,cr=0;if(c.moveToFirst()){d=c.getDouble(0);cr=c.getDouble(1);}c.close();if(Math.abs(d-cr)>0.005)throw new IllegalStateException("Unbalanced journal entry "+je+" debit="+d+" credit="+cr);}
 long accountId(SQLiteDatabase sq,String code){Cursor c=sq.rawQuery("SELECT id FROM accounts WHERE code=?",new String[]{code});long id=c.moveToFirst()?c.getLong(0):0;c.close();return id;}
 void line(SQLiteDatabase sq,long je,long account,double debit,double credit){ContentValues v=new ContentValues();v.put("journal_entry_id",je);v.put("account_id",account);v.put("debit",debit);v.put("credit",credit);sq.insertOrThrow("journal_lines",null,v);}
 ContentValues cv(Object...a){ContentValues v=new ContentValues();for(int i=0;i<a.length;i+=2){Object val=a[i+1];if(val instanceof Number)v.put(String.valueOf(a[i]),((Number)val).doubleValue());else v.put(String.valueOf(a[i]),String.valueOf(val));}return v;}
 boolean validIsoDate(String value,boolean required){if(value==null||value.trim().isEmpty())return !required;try{java.text.SimpleDateFormat sdf=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US);sdf.setLenient(false);java.util.Date d=sdf.parse(value.trim());return d!=null&&sdf.format(d).equals(value.trim());}catch(Exception ex){return false;}}
 boolean validNumber(EditText x,boolean required){String value=x.getText().toString().trim();if(value.isEmpty())return !required;try{double d=Double.parseDouble(value);return !Double.isNaN(d)&&!Double.isInfinite(d);}catch(Exception ex){return false;}}
  double num(EditText x){try{return Double.parseDouble(x.getText().toString());}catch(Exception z){return 0;}}String fmt(double x){int decimals=getSharedPreferences("company_profile",0).getInt("decimals",2);String currency=getSharedPreferences("company_profile",0).getString("currency","SDG").trim();if(currency.isEmpty())currency="SDG";return String.format(Locale.US,decimals==0?"%,.0f":"%,.2f",x)+" "+currency;}void toast(String s){Toast.makeText(this,tr(s),Toast.LENGTH_LONG).show();}void open(String u){try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(u)));}catch(Exception z){}}
 // Preserve the authenticated screen when the activity briefly leaves the foreground.
 // Forcing a PIN re-entry from onStop caused UI re-entrancy during system dialogs/app switches.
 @Override protected void onStop(){super.onStop();}
 @Override protected void onResume(){super.onResume();if(needsRelogin){needsRelogin=false;login();}}
 public void onBackPressed(){if(authenticated){dashboard();}else{super.onBackPressed();}}
 static class DB extends SQLiteOpenHelper{DB(Context c){super(c,"smartcompany2.db",null,11);}public void onCreate(SQLiteDatabase d){schema(d);}public void onUpgrade(SQLiteDatabase d,int o,int n){schema(d);}void schema(SQLiteDatabase d){d.execSQL("CREATE TABLE IF NOT EXISTS products(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT UNIQUE,qty REAL NOT NULL DEFAULT 0,sale_price REAL NOT NULL DEFAULT 0,cost REAL NOT NULL DEFAULT 0,reorder REAL NOT NULL DEFAULT 0)");try{d.execSQL("ALTER TABLE products ADD COLUMN barcode TEXT");}catch(Exception ignored){}d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_products_barcode ON products(barcode) WHERE barcode IS NOT NULL AND barcode != ''");d.execSQL("CREATE TABLE IF NOT EXISTS customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,credit_limit REAL NOT NULL DEFAULT 0)");try{d.execSQL("ALTER TABLE customers ADD COLUMN credit_limit REAL NOT NULL DEFAULT 0");}catch(Exception ignored){}d.execSQL("CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS employees(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,job_title TEXT NOT NULL DEFAULT '',salary REAL NOT NULL DEFAULT 0)");try{d.execSQL("ALTER TABLE employees ADD COLUMN job_title TEXT NOT NULL DEFAULT ''");}catch(Exception ignored){}try{d.execSQL("ALTER TABLE employees ADD COLUMN salary REAL NOT NULL DEFAULT 0");}catch(Exception ignored){}d.execSQL("CREATE TABLE IF NOT EXISTS payroll_events(id INTEGER PRIMARY KEY AUTOINCREMENT,employee_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,period_month TEXT NOT NULL,created_at INTEGER)");d.execSQL("CREATE INDEX IF NOT EXISTS idx_payroll_employee_month ON payroll_events(employee_id,period_month)");try{d.execSQL("ALTER TABLE payroll_events ADD COLUMN posted INTEGER NOT NULL DEFAULT 0");}catch(Exception ignored){}d.execSQL("CREATE TABLE IF NOT EXISTS transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,type TEXT NOT NULL,party TEXT,total REAL NOT NULL,paid REAL NOT NULL DEFAULT 0,remaining REAL NOT NULL DEFAULT 0,created_at INTEGER)");try{d.execSQL("ALTER TABLE transactions ADD COLUMN original_transaction_id INTEGER");}catch(Exception ignored){}d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_transaction_single_return ON transactions(original_transaction_id) WHERE original_transaction_id IS NOT NULL");try{d.execSQL("ALTER TABLE transactions ADD COLUMN invoice_no TEXT");}catch(Exception ignored){}try{d.execSQL("ALTER TABLE transactions ADD COLUMN customer_id INTEGER");}catch(Exception ignored){}try{d.execSQL("ALTER TABLE transactions ADD COLUMN supplier_id INTEGER");}catch(Exception ignored){}d.execSQL("INSERT INTO customers(name) SELECT DISTINCT t.party FROM transactions t WHERE t.type='SALE' AND t.party IS NOT NULL AND TRIM(t.party)!='' AND NOT EXISTS(SELECT 1 FROM customers c WHERE c.name=t.party)");d.execSQL("INSERT INTO suppliers(name) SELECT DISTINCT t.party FROM transactions t WHERE t.type='PURCHASE' AND t.party IS NOT NULL AND TRIM(t.party)!='' AND NOT EXISTS(SELECT 1 FROM suppliers s WHERE s.name=t.party)");d.execSQL("UPDATE transactions SET customer_id=(SELECT MIN(c.id) FROM customers c WHERE c.name=transactions.party) WHERE type='SALE' AND customer_id IS NULL");d.execSQL("UPDATE transactions SET supplier_id=(SELECT MIN(s.id) FROM suppliers s WHERE s.name=transactions.party) WHERE type='PURCHASE' AND supplier_id IS NULL");d.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_transactions_invoice_no ON transactions(invoice_no) WHERE invoice_no IS NOT NULL AND invoice_no != ''");d.execSQL("CREATE TABLE IF NOT EXISTS invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER,product_id INTEGER,qty REAL,price REAL,cost REAL,discount REAL NOT NULL DEFAULT 0)");d.execSQL("CREATE TABLE IF NOT EXISTS bread_counter_sales(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER NOT NULL UNIQUE,product_id INTEGER NOT NULL,qty REAL NOT NULL CHECK(qty>0),unit_price REAL NOT NULL CHECK(unit_price>=0),total REAL NOT NULL CHECK(total>=0),created_at INTEGER NOT NULL)");d.execSQL("CREATE TABLE IF NOT EXISTS bread_distribution_sales(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER NOT NULL UNIQUE,product_id INTEGER NOT NULL,distributor_name TEXT NOT NULL,qty REAL NOT NULL CHECK(qty>0),retail_total REAL NOT NULL CHECK(retail_total>=0),distribution_total REAL NOT NULL CHECK(distribution_total>=0),cash_received REAL NOT NULL CHECK(cash_received>=0),debt_amount REAL NOT NULL CHECK(debt_amount>=0),price_difference REAL NOT NULL CHECK(price_difference>=0),created_at INTEGER NOT NULL,CHECK(ABS(cash_received+debt_amount-distribution_total)<0.005),CHECK(ABS(retail_total-distribution_total-price_difference)<0.005))");d.execSQL("CREATE INDEX IF NOT EXISTS idx_bread_counter_created ON bread_counter_sales(created_at)");d.execSQL("CREATE INDEX IF NOT EXISTS idx_bread_distribution_name_date ON bread_distribution_sales(distributor_name,created_at)");
 d.execSQL("CREATE TABLE IF NOT EXISTS bank_accounts(id INTEGER PRIMARY KEY AUTOINCREMENT,bank_name TEXT NOT NULL,account_name TEXT NOT NULL UNIQUE,opening_balance REAL NOT NULL DEFAULT 0)");
 d.execSQL("CREATE TABLE IF NOT EXISTS bank_movements(id INTEGER PRIMARY KEY AUTOINCREMENT,account_id INTEGER NOT NULL,kind TEXT NOT NULL,amount REAL NOT NULL,note TEXT,created_at INTEGER)");
 d.execSQL("CREATE TABLE IF NOT EXISTS checks(id INTEGER PRIMARY KEY AUTOINCREMENT,direction TEXT NOT NULL,check_no TEXT NOT NULL,bank_name TEXT,party TEXT,amount REAL NOT NULL,issue_date TEXT,due_date TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER,UNIQUE(direction,check_no))");
 d.execSQL("CREATE INDEX IF NOT EXISTS idx_checks_due_status ON checks(due_date,status)");
 d.execSQL("CREATE TABLE IF NOT EXISTS accounts(id INTEGER PRIMARY KEY AUTOINCREMENT,code TEXT UNIQUE,name TEXT NOT NULL,type TEXT)");
 d.execSQL("CREATE TABLE IF NOT EXISTS journal_entries(id INTEGER PRIMARY KEY AUTOINCREMENT,transaction_id INTEGER,created_at INTEGER,description TEXT)");try{d.execSQL("ALTER TABLE journal_entries ADD COLUMN description TEXT");}catch(Exception ignored){}d.execSQL("CREATE TABLE IF NOT EXISTS party_payments(id INTEGER PRIMARY KEY AUTOINCREMENT,party_type TEXT NOT NULL,party_id INTEGER NOT NULL,direction TEXT NOT NULL,amount REAL NOT NULL,note TEXT,created_at INTEGER NOT NULL,journal_entry_id INTEGER NOT NULL)");d.execSQL("CREATE INDEX IF NOT EXISTS idx_party_payments_party_date ON party_payments(party_type,party_id,created_at)");
 d.execSQL("CREATE TABLE IF NOT EXISTS journal_lines(id INTEGER PRIMARY KEY AUTOINCREMENT,journal_entry_id INTEGER,account_id INTEGER,debit REAL NOT NULL DEFAULT 0,credit REAL NOT NULL DEFAULT 0)");
 d.execSQL("CREATE TABLE IF NOT EXISTS journal_reversals(id INTEGER PRIMARY KEY AUTOINCREMENT,original_entry_id INTEGER NOT NULL UNIQUE,reversal_entry_id INTEGER NOT NULL UNIQUE,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
 d.execSQL("CREATE TABLE IF NOT EXISTS recurring_expenses(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,amount REAL NOT NULL CHECK(amount>0),frequency TEXT NOT NULL CHECK(frequency IN ('MONTHLY','YEARLY')),next_due TEXT NOT NULL,active INTEGER NOT NULL DEFAULT 1)");
 d.execSQL("CREATE TABLE IF NOT EXISTS recurring_expense_runs(id INTEGER PRIMARY KEY AUTOINCREMENT,recurring_id INTEGER NOT NULL,period_key TEXT NOT NULL,transaction_id INTEGER NOT NULL DEFAULT 0,UNIQUE(recurring_id,period_key))");
 d.execSQL("CREATE TABLE IF NOT EXISTS fixed_assets(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,cost REAL NOT NULL CHECK(cost>0),life_months INTEGER NOT NULL CHECK(life_months>0),ready_date TEXT NOT NULL,created_at INTEGER NOT NULL,acquisition_journal_id INTEGER)");
 d.execSQL("CREATE TABLE IF NOT EXISTS asset_depreciation(id INTEGER PRIMARY KEY AUTOINCREMENT,asset_id INTEGER NOT NULL,period TEXT NOT NULL,amount REAL NOT NULL CHECK(amount>0),journal_entry_id INTEGER,UNIQUE(asset_id,period))");
 d.execSQL("CREATE TABLE IF NOT EXISTS factory_orders(id INTEGER PRIMARY KEY AUTOINCREMENT,order_name TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL DEFAULT 0)");
 d.execSQL("CREATE TABLE IF NOT EXISTS factory_stage_events(id INTEGER PRIMARY KEY AUTOINCREMENT,order_id INTEGER NOT NULL,stage_name TEXT NOT NULL,material_cost REAL NOT NULL,labor_cost REAL NOT NULL,overhead_cost REAL NOT NULL,good_qty REAL NOT NULL,scrap_qty REAL NOT NULL,created_at INTEGER NOT NULL,journal_entry_id INTEGER)");
 String[][] more={{"1500","الأصول الثابتة","ASSET"},{"1590","مجمع إهلاك الأصول","CONTRA_ASSET"},{"6100","مصروف الإهلاك","EXPENSE"},{"1400","مخزون تحت التشغيل","ASSET"},{"1210","المواد الخام","ASSET"},{"2200","أجور تشغيل مستحقة","LIABILITY"},{"2300","مصروفات صناعية مستحقة","LIABILITY"},{"2100","الموردون","LIABILITY"},{"4200","خصومات التوزيع","CONTRA_REVENUE"}};for(String[] a:more){ContentValues v=new ContentValues();v.put("code",a[0]);v.put("name",a[1]);v.put("type",a[2]);d.insertWithOnConflict("accounts",null,v,SQLiteDatabase.CONFLICT_IGNORE);}

 String[][] ac={{"1010","الصندوق","ASSET"},{"1020","الحسابات البنكية","ASSET"},{"1100","العملاء","ASSET"},{"1200","المخزون","ASSET"},{"1300","سلف الموظفين","ASSET"},{"2100","الموردون","LIABILITY"},{"3000","رأس المال","EQUITY"},{"3100","الأرصدة الافتتاحية","EQUITY"},{"4000","المبيعات","REVENUE"},{"4100","مردودات المبيعات","CONTRA_REVENUE"},{"5100","تكلفة البضاعة المباعة","COST"},{"6000","المصروفات","EXPENSE"}};for(String[] a:ac){ContentValues v=new ContentValues();v.put("code",a[0]);v.put("name",a[1]);v.put("type",a[2]);d.insertWithOnConflict("accounts",null,v,SQLiteDatabase.CONFLICT_IGNORE);}d.execSQL("CREATE INDEX IF NOT EXISTS idx_journal_lines_account ON journal_lines(account_id)");d.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(type)");d.execSQL("CREATE INDEX IF NOT EXISTS idx_invoice_items_tx ON invoice_items(transaction_id)");}void exec(String s,Object...a){tx(s,a);}void tx(String s,Object...a){SQLiteDatabase d=getWritableDatabase();SQLiteStatement q=d.compileStatement(s);for(int i=0;i<a.length;i++){Object v=a[i];if(v==null)q.bindNull(i+1);else if(v instanceof Number)q.bindDouble(i+1,((Number)v).doubleValue());else q.bindString(i+1,String.valueOf(v));}q.execute();}Cursor q(String s,String...a){return getReadableDatabase().rawQuery(s,a);}}
}
