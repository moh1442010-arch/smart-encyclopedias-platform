package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Single document-to-ledger gateway for bakery sales.
 * Source record + immutable document snapshot + outbox are committed atomically.
 * GL posting happens after that transaction and is safe to retry by idempotency key.
 */
final class AccountingDocumentService {
    private static final String SOURCE_TYPE = "ACCOUNTING_DOCUMENT";
    private static final String BRANCH = "MAIN";
    private static final String CURRENCY = "SDG";

    private AccountingDocumentService() {}

    private static final class Line {
        final String account;
        final long debit, credit, partyId;
        Line(String account,long debit,long credit,long partyId) {
            this.account=account;this.debit=debit;this.credit=credit;this.partyId=partyId;
        }
    }

    static void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_documents ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,document_key TEXT NOT NULL UNIQUE,document_type TEXT NOT NULL,"+
            "source_module TEXT NOT NULL,source_date TEXT NOT NULL,currency_code TEXT NOT NULL,branch_code TEXT NOT NULL,"+
            "payload_sha256 TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('DRAFT','QUEUED','POSTED','REVERSED','ERROR')),"+
            "journal_ref TEXT,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_document_lines ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,document_id INTEGER NOT NULL REFERENCES accounting_documents(id) ON DELETE CASCADE,"+
            "line_no INTEGER NOT NULL,account_code TEXT NOT NULL,branch_code TEXT NOT NULL,currency_code TEXT NOT NULL,"+
            "debit_minor INTEGER NOT NULL CHECK(debit_minor>=0),credit_minor INTEGER NOT NULL CHECK(credit_minor>=0),"+
            "party_id INTEGER,fx_rate_num INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_num>0),"+
            "fx_rate_den INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_den>0),UNIQUE(document_id,line_no))");
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_outbox ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,document_id INTEGER NOT NULL UNIQUE REFERENCES accounting_documents(id),"+
            "event_key TEXT NOT NULL UNIQUE,status TEXT NOT NULL CHECK(status IN ('PENDING','PROCESSING','RETRY','POSTED','DEAD_LETTER')),"+
            "attempts INTEGER NOT NULL DEFAULT 0,last_error TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_posting_links ("+
            "document_id INTEGER PRIMARY KEY REFERENCES accounting_documents(id),journal_ref TEXT NOT NULL UNIQUE,"+
            "linked_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_document_revisions ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT,document_id INTEGER NOT NULL REFERENCES accounting_documents(id),"+
            "old_payload_sha256 TEXT NOT NULL,new_payload_sha256 TEXT NOT NULL,reason TEXT NOT NULL,actor TEXT NOT NULL,"+
            "created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_accounting_outbox_status ON accounting_outbox(status,id)");
    }

    /** Save the operational sales record and its immutable accounting snapshot in one DB transaction. */
    static void saveSalesSnapshot(SQLiteDatabase db,String date,String module,org.json.JSONObject payload) {
        if(db==null||!db.isOpen())throw new IllegalStateException("قاعدة بيانات المخبز غير متاحة");
        if(!GeneralLedger.validDate(date)||payload==null||!("window".equals(module)||"distribution".equals(module)))
            throw new IllegalArgumentException("مستند البيع أو تاريخه غير صحيح");
        ensureSchema(db);
        List<Line> lines=buildSalesLines(module,payload);
        String key="SALES:"+module+":"+date;
        String hash=hash(module+"|"+date+"|"+payload.toString()+"|"+canonical(lines));
        long now=System.currentTimeMillis();
        long documentId=-1;
        boolean shouldQueue=false;
        db.beginTransaction();
        try {
            Cursor old=db.rawQuery("SELECT id,payload_sha256,status FROM accounting_documents WHERE document_key=?",new String[]{key});
            try {
                if(old.moveToFirst()) {
                    documentId=old.getLong(0);
                    String oldHash=old.getString(1),status=old.getString(2);
                    if(!hash.equals(oldHash)) {
                        if(!"DRAFT".equals(status))
                            throw new IllegalStateException("المستند مرحّل أو قيد الترحيل ولا يجوز استبداله. اعكس المستند أولاً ثم أنشئ نسخة جديدة.");
                        db.delete("accounting_document_lines","document_id=?",new String[]{String.valueOf(documentId)});
                        db.delete("accounting_outbox","document_id=?",new String[]{String.valueOf(documentId)});
                        ContentValues update=new ContentValues();update.put("payload_sha256",hash);
                        update.put("updated_at",now);update.put("status","DRAFT");
                        db.update("accounting_documents",update,"id=?",new String[]{String.valueOf(documentId)});
                    }
                }
            } finally {old.close();}
            ContentValues record=new ContentValues();record.put("day",date);record.put("module",module);
            record.put("payload",payload.toString());record.put("updated_at",now);
            db.insertWithOnConflict("records",null,record,SQLiteDatabase.CONFLICT_REPLACE);
            if(documentId<0) {
                ContentValues head=new ContentValues();head.put("document_key",key);head.put("document_type","SALES_INVOICE");
                head.put("source_module",module);head.put("source_date",date);head.put("currency_code",CURRENCY);
                head.put("branch_code",BRANCH);head.put("payload_sha256",hash);head.put("status","DRAFT");
                head.put("created_at",now);head.put("updated_at",now);
                documentId=db.insertOrThrow("accounting_documents",null,head);
            } else {
                Cursor existing=db.rawQuery("SELECT COUNT(*) FROM accounting_document_lines WHERE document_id=?",new String[]{String.valueOf(documentId)});
                try {existing.moveToFirst();if(existing.getLong(0)>0&&!hash.equals(scalar(db,"SELECT payload_sha256 FROM accounting_documents WHERE id=?",new String[]{String.valueOf(documentId)})))throw new IllegalStateException("تعذر التحقق من نسخة المستند");}
                finally {existing.close();}
            }
            Cursor lineCount=db.rawQuery("SELECT COUNT(*) FROM accounting_document_lines WHERE document_id=?",new String[]{String.valueOf(documentId)});
            boolean hasLines;try{lineCount.moveToFirst();hasLines=lineCount.getLong(0)>0;}finally{lineCount.close();}
            if(!hasLines) {
                int index=0;
                for(Line line:lines) {
                    ContentValues v=new ContentValues();v.put("document_id",documentId);v.put("line_no",index++);
                    v.put("account_code",line.account);v.put("branch_code",BRANCH);v.put("currency_code",CURRENCY);
                    v.put("debit_minor",line.debit);v.put("credit_minor",line.credit);
                    if(line.partyId>0)v.put("party_id",line.partyId);
                    v.put("fx_rate_num",1);v.put("fx_rate_den",1);
                    db.insertOrThrow("accounting_document_lines",null,v);
                }
            }
            String base=scalar(db,"SELECT base_currency FROM company_settings WHERE id=1",null);
            // Current bakery sales inputs are SDG-denominated. Never guess an FX rate.
            if(CURRENCY.equals(base)&&!lines.isEmpty()) {
                String eventKey="ACCOUNTING_POST:"+key+":"+hash;
                ContentValues event=new ContentValues();event.put("document_id",documentId);event.put("event_key",eventKey);
                event.put("status","PENDING");event.put("created_at",now);event.put("updated_at",now);
                db.insertWithOnConflict("accounting_outbox",null,event,SQLiteDatabase.CONFLICT_IGNORE);
                ContentValues queued=new ContentValues();queued.put("status","QUEUED");queued.put("updated_at",now);
                db.update("accounting_documents",queued,"id=? AND status='DRAFT'",new String[]{String.valueOf(documentId)});
                shouldQueue=true;
            }
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
        if(shouldQueue)processPending(db);
    }

    private static List<Line> buildSalesLines(String module,org.json.JSONObject payload) {
        List<Line> lines=new ArrayList<>();
        long cash=0,bank=0,debt=0;
        if("window".equals(module)) {
            cash=nonNegative(payload.optString("cash","0"),"نقد الشباك");
            debt=nonNegative(payload.optString("debt","0"),"ديون الشباك");
            bank=nonNegative(payload.optString("bank","0"),"تحويلات بنكك");
            if(bank==0) {
                String raw=payload.optString("bank_transfers","");
                for(String row:raw.split("\\r?\\n"))if(!row.trim().isEmpty()) {
                    String[] p=row.split("\\|",-1);
                    if(p.length<2||p[1].trim().isEmpty())throw new IllegalArgumentException("رقم عملية بنكك مفقود");
                    bank=checkedAdd(bank,nonNegative(p[0],"تحويل بنكك"));
                }
            }
            if(cash>0)lines.add(new Line("1000",cash,0,0));
            if(bank>0)lines.add(new Line("1010",bank,0,0));
            if(debt>0)lines.add(new Line("1100",debt,0,0));
        } else {
            String raw=payload.optString("rows","");
            for(String row:raw.split("\\r?\\n")) {
                if(row.trim().isEmpty())continue;
                String[] p=row.split("\\|",-1);
                if(p.length<7)throw new IllegalArgumentException("صف توزيع قديم أو ناقص؛ لا يمكن ترحيله آلياً");
                long rowCash=nonNegative(p[2],"نقد التوزيع");
                long rowBank=nonNegative(p[3],"بنكك التوزيع");
                long rowDebt=nonNegative(p[5],"ديون التوزيع");
                long partyId=p.length>7?nonNegative(p[7],"معرّف العميل"):0;
                if(rowDebt>0&&partyId<=0)throw new IllegalArgumentException("كل دين توزيع يحتاج عميلاً مسجلاً");
                cash=checkedAdd(cash,rowCash);bank=checkedAdd(bank,rowBank);debt=checkedAdd(debt,rowDebt);
            }
            if(cash>0)lines.add(new Line("1000",cash,0,0));
            if(bank>0)lines.add(new Line("1010",bank,0,0));
            if(debt>0)lines.add(new Line("1100",debt,0,0));
        }
        long revenue=checkedAdd(checkedAdd(cash,bank),debt);
        if(revenue>0)lines.add(new Line("4000",0,revenue,0));
        if(revenue>0)GeneralLedger.validateBalancedTotals(revenue,revenue,lines.size());
        return lines;
    }

    static int processPending(SQLiteDatabase db) {
        ensureSchema(db);
        // Recover events left PROCESSING by an abrupt process stop.
        ContentValues recover=new ContentValues();recover.put("status","RETRY");recover.put("updated_at",System.currentTimeMillis());
        db.update("accounting_outbox",recover,"status='PROCESSING'",null);
        Cursor pending=db.rawQuery("SELECT o.id,o.document_id,o.event_key,d.document_key,d.source_date,d.source_module,d.status "+
            "FROM accounting_outbox o JOIN accounting_documents d ON d.id=o.document_id "+
            "WHERE o.status IN ('PENDING','RETRY') ORDER BY o.id LIMIT 100",null);
        List<long[]> ids=new ArrayList<>();
        try {while(pending.moveToNext())ids.add(new long[]{pending.getLong(0),pending.getLong(1)});}
        finally {pending.close();}
        int posted=0;
        for(long[] idsRow:ids) {
            long outboxId=idsRow[0],documentId=idsRow[1];
            String key,date,module,eventKey,documentStatus;
            Cursor head=db.rawQuery("SELECT o.event_key,d.document_key,d.source_date,d.source_module,d.status "+
                "FROM accounting_outbox o JOIN accounting_documents d ON d.id=o.document_id WHERE o.id=?",
                new String[]{String.valueOf(outboxId)});
            try {if(!head.moveToFirst())continue;eventKey=head.getString(0);key=head.getString(1);date=head.getString(2);module=head.getString(3);documentStatus=head.getString(4);}
            finally {head.close();}
            if("POSTED".equals(documentStatus))continue;
            ContentValues processing=new ContentValues();processing.put("status","PROCESSING");processing.put("updated_at",System.currentTimeMillis());
            db.update("accounting_outbox",processing,"id=?",new String[]{String.valueOf(outboxId)});
            try {
                List<GeneralLedger.Line> glLines=new ArrayList<>();
                Cursor rows=db.rawQuery("SELECT account_code,branch_code,currency_code,debit_minor,credit_minor,fx_rate_num,fx_rate_den "+
                    "FROM accounting_document_lines WHERE document_id=? ORDER BY line_no",new String[]{String.valueOf(documentId)});
                try {while(rows.moveToNext())glLines.add(new GeneralLedger.Line(rows.getString(0),rows.getString(1),rows.getString(2),
                    rows.getLong(3),rows.getLong(4),rows.getLong(5),rows.getLong(6)));}
                finally {rows.close();}
                String ref="SALE-"+("window".equals(module)?"WIN":"DIST")+"-"+date.replace("-","");
                GeneralLedger.Entry entry=new GeneralLedger.Entry(ref,"مبيعات "+("window".equals(module)?"الشباك":"التوزيع")+" "+date,
                    date,SOURCE_TYPE,key,eventKey,"SYSTEM",glLines);
                GeneralLedger.post(db,entry);
                db.beginTransaction();
                try {
                    ContentValues link=new ContentValues();link.put("document_id",documentId);link.put("journal_ref",ref);link.put("linked_at",System.currentTimeMillis());
                    db.insertWithOnConflict("accounting_posting_links",null,link,SQLiteDatabase.CONFLICT_REPLACE);
                    ContentValues done=new ContentValues();done.put("status","POSTED");done.put("journal_ref",ref);done.put("updated_at",System.currentTimeMillis());
                    db.update("accounting_documents",done,"id=?",new String[]{String.valueOf(documentId)});
                    done.clear();done.put("status","POSTED");done.put("updated_at",System.currentTimeMillis());
                    db.update("accounting_outbox",done,"id=?",new String[]{String.valueOf(outboxId)});
                    db.setTransactionSuccessful();
                } finally {db.endTransaction();}
                posted++;
            } catch(Exception ex) {
                db.beginTransaction();
                try {
                    Cursor attempt=db.rawQuery("SELECT attempts FROM accounting_outbox WHERE id=?",new String[]{String.valueOf(outboxId)});
                    int count;try{attempt.moveToFirst();count=attempt.getInt(0)+1;}finally{attempt.close();}
                    ContentValues retry=new ContentValues();retry.put("attempts",count);
                    retry.put("status",count>=8?"DEAD_LETTER":"RETRY");retry.put("last_error",String.valueOf(ex.getMessage()));
                    retry.put("updated_at",System.currentTimeMillis());db.update("accounting_outbox",retry,"id=?",new String[]{String.valueOf(outboxId)});
                    db.setTransactionSuccessful();
                } finally {db.endTransaction();}
            }
        }
        return posted;
    }

    static String reverseDocument(SQLiteDatabase db,long documentId,String reversalDate,String actor) {
        ensureSchema(db);
        String ref;
        Cursor c=db.rawQuery("SELECT journal_ref,status FROM accounting_documents WHERE id=?",new String[]{String.valueOf(documentId)});
        try {
            if(!c.moveToFirst()||!"POSTED".equals(c.getString(1)))throw new IllegalStateException("المستند غير مرحّل أو غير موجود");
            ref=c.getString(0);
        } finally {c.close();}
        GeneralLedger.Result reversal=GeneralLedger.reverse(db,ref,reversalDate,actor);
        db.beginTransaction();
        try {
            ContentValues v=new ContentValues();v.put("status","REVERSED");v.put("updated_at",System.currentTimeMillis());
            db.update("accounting_documents",v,"id=?",new String[]{String.valueOf(documentId)});
            ContentValues audit=new ContentValues();audit.put("actor",actor==null?"SYSTEM":actor);audit.put("action","ACCOUNTING_DOCUMENT_REVERSED");
            audit.put("entity_type","ACCOUNTING_DOCUMENT");audit.put("entity_ref",String.valueOf(documentId));
            audit.put("details","Original="+ref+"; reversal="+reversal.reference);audit.put("created_at",System.currentTimeMillis());
            db.insertOrThrow("enterprise_audit_log",null,audit);
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
        return reversal.reference;
    }

    private static long nonNegative(String value,String field) {
        try {long n=Long.parseLong(value==null||value.trim().isEmpty()?"0":value.trim());if(n<0)throw new IllegalArgumentException(field+" لا يقبل السالب");return n;}
        catch(NumberFormatException ex){throw new IllegalArgumentException(field+" ليس مبلغاً صحيحاً",ex);}
    }
    private static long checkedAdd(long a,long b) {
        if(b>0&&a>Long.MAX_VALUE-b)throw new ArithmeticException("sales total overflow");
        return a+b;
    }
    private static String canonical(List<Line> lines) {
        StringBuilder b=new StringBuilder();for(Line l:lines)b.append(l.account).append(':').append(l.debit).append(':').append(l.credit).append(':').append(l.partyId).append(';');return b.toString();
    }
    private static String hash(String value) {
        try {byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes("UTF-8"));StringBuilder out=new StringBuilder();
            for(byte b:bytes)out.append(String.format(java.util.Locale.US,"%02x",b&255));return out.toString();}
        catch(Exception ex){throw new IllegalStateException("تعذر إنشاء بصمة المستند",ex);}
    }
    private static String scalar(SQLiteDatabase db,String sql,String[] args) {
        Cursor c=db.rawQuery(sql,args);try{return c.moveToFirst()?c.getString(0):null;}finally{c.close();}
    }
}
