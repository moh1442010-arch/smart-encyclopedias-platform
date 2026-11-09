package com.mohammedmustafa.almizan.bakery;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Durable bridge from the isolated factory ledger database to the bakery general ledger.
 * Source journal + outbox snapshot are committed in one factory-database transaction.
 * Destination posting is idempotent, so a retry after a process crash cannot duplicate GL.
 */
final class FactoryGlOutbox {
    private static final int MAX_ATTEMPTS = 8;
    private static final int BATCH_SIZE = 20;
    private static final String SOURCE_TYPE = "FACTORY_LEDGER";
    private FactoryGlOutbox() {}

    static final class ProcessResult {
        final int posted, retried, deadLetter, skipped;
        ProcessResult(int posted, int retried, int deadLetter, int skipped) {
            this.posted=posted; this.retried=retried; this.deadLetter=deadLetter; this.skipped=skipped;
        }
    }

    static final class SnapshotLine {
        final long id;
        final String sourceAccount;
        final long debit, credit;
        String targetAccount, branch, currency;
        long fxNum, fxDen;
        SnapshotLine(long id,String sourceAccount,long debit,long credit,String targetAccount,
                     String branch,String currency,long fxNum,long fxDen) {
            this.id=id;this.sourceAccount=sourceAccount;this.debit=debit;this.credit=credit;
            this.targetAccount=targetAccount;this.branch=branch;this.currency=currency;
            this.fxNum=fxNum;this.fxDen=fxDen;
        }
    }

    static void ensureSchema(SQLiteDatabase source) {
        source.execSQL("CREATE TABLE IF NOT EXISTS factory_gl_account_mappings ("+
            "source_account TEXT PRIMARY KEY NOT NULL, target_account_code TEXT NOT NULL, "+
            "branch_code TEXT NOT NULL, currency_code TEXT NOT NULL, fx_rate_num INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_num>0), "+
            "fx_rate_den INTEGER NOT NULL DEFAULT 1 CHECK(fx_rate_den>0), active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1)), "+
            "updated_at INTEGER NOT NULL)");
        source.execSQL("CREATE TABLE IF NOT EXISTS factory_gl_outbox ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT, event_key TEXT NOT NULL UNIQUE, source_entry_id INTEGER NOT NULL UNIQUE, "+
            "source_entry_ref TEXT NOT NULL UNIQUE, description TEXT NOT NULL, transaction_date TEXT NOT NULL, "+
            "payload_sha256 TEXT NOT NULL, status TEXT NOT NULL CHECK(status IN ('PENDING','PROCESSING','POSTED','RETRY','DEAD_LETTER')), "+
            "attempt_count INTEGER NOT NULL DEFAULT 0, next_attempt_at INTEGER NOT NULL DEFAULT 0, last_error TEXT NOT NULL DEFAULT '', "+
            "destination_ref TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, posted_at INTEGER)");
        source.execSQL("CREATE INDEX IF NOT EXISTS idx_factory_gl_outbox_due ON factory_gl_outbox(status,next_attempt_at,id)");
        source.execSQL("CREATE TABLE IF NOT EXISTS factory_gl_outbox_lines ("+
            "id INTEGER PRIMARY KEY AUTOINCREMENT, outbox_id INTEGER NOT NULL REFERENCES factory_gl_outbox(id) ON DELETE CASCADE, "+
            "line_no INTEGER NOT NULL, source_account TEXT NOT NULL, debit_minor INTEGER NOT NULL CHECK(debit_minor>=0), "+
            "credit_minor INTEGER NOT NULL CHECK(credit_minor>=0), target_account_code TEXT, branch_code TEXT, currency_code TEXT, "+
            "fx_rate_num INTEGER, fx_rate_den INTEGER, UNIQUE(outbox_id,line_no), "+
            "CHECK((debit_minor>0 AND credit_minor=0) OR (credit_minor>0 AND debit_minor=0)))");
        source.execSQL("CREATE INDEX IF NOT EXISTS idx_factory_gl_outbox_lines_event ON factory_gl_outbox_lines(outbox_id,line_no)");
    }

    /** Must run inside the same transaction that inserts the source journal. */
    static void enqueue(SQLiteDatabase source,long sourceEntryId) {
        if(source==null||!source.isOpen()||!source.inTransaction())
            throw new IllegalStateException("يجب إدراج حدث Outbox داخل معاملة القيد الأصلي");
        ensureSchema(source);
        String ref,description,date;
        Cursor head=source.rawQuery("SELECT entry_ref,description,date,status FROM journal_entries WHERE id=?",
            new String[]{String.valueOf(sourceEntryId)});
        try {
            if(!head.moveToFirst())throw new IllegalArgumentException("قيد المصنع غير موجود");
            ref=head.getString(0);description=head.getString(1);date=head.getString(2);
            if(!"POSTED".equals(head.getString(3)))throw new IllegalStateException("لا يمكن إرسال قيد مصنع غير مرحل");
        } finally {head.close();}
        List<SnapshotLine> lines=new ArrayList<>();
        Cursor rows=source.rawQuery("SELECT account,debit,credit FROM journal_lines WHERE entry_id=? ORDER BY account,debit,credit,id",
            new String[]{String.valueOf(sourceEntryId)});
        long debitTotal=0,creditTotal=0;
        try {
            while(rows.moveToNext()) {
                String account=rows.getString(0);long debit=rows.getLong(1),credit=rows.getLong(2);
                if(account==null||account.trim().isEmpty()||debit<0||credit<0||((debit>0)==(credit>0)))
                    throw new IllegalStateException("سطر قيد المصنع غير صالح");
                debitTotal=checkedAdd(debitTotal,debit);creditTotal=checkedAdd(creditTotal,credit);
                lines.add(new SnapshotLine(0,account,debit,credit,null,null,null,1,1));
            }
        } finally {rows.close();}
        GeneralLedger.validateBalancedTotals(debitTotal,creditTotal,lines.size());
        String hash=payloadHash(ref,description,date,lines);
        String eventKey=eventKey(ref);
        Cursor prior=source.rawQuery("SELECT id,payload_sha256 FROM factory_gl_outbox WHERE event_key=?",
            new String[]{eventKey});
        try {
            if(prior.moveToFirst()) {
                if(!hash.equals(prior.getString(1)))
                    throw new IllegalStateException("تغير محتوى القيد بعد إنشاء حدث Outbox؛ أوقف الترحيل للمراجعة");
                return;
            }
        } finally {prior.close();}
        ContentValues event=new ContentValues();
        event.put("event_key",eventKey);event.put("source_entry_id",sourceEntryId);
        event.put("source_entry_ref",ref);event.put("description",description);
        event.put("transaction_date",date);event.put("payload_sha256",hash);
        event.put("status","PENDING");event.put("created_at",System.currentTimeMillis());
        event.put("updated_at",System.currentTimeMillis());
        long outboxId=source.insertOrThrow("factory_gl_outbox",null,event);
        int lineNo=0;
        for(SnapshotLine line:lines) {
            ContentValues v=new ContentValues();v.put("outbox_id",outboxId);v.put("line_no",lineNo++);
            v.put("source_account",line.sourceAccount);v.put("debit_minor",line.debit);v.put("credit_minor",line.credit);
            source.insertOrThrow("factory_gl_outbox_lines",null,v);
        }
    }

    /** Creates a balanced source journal and its outbox snapshot in the caller's existing transaction. */
    static long postSourceJournal(SQLiteDatabase source,String reference,String description,String date,
                                  String[] accounts,long[] debits,long[] credits) {
        if(source==null||!source.isOpen()||!source.inTransaction())
            throw new IllegalStateException("يجب ترحيل مستند المصنع داخل معاملته الأصلية");
        if(reference==null||reference.trim().isEmpty()||description==null||date==null||
           accounts==null||debits==null||credits==null||accounts.length!=debits.length||
           accounts.length!=credits.length||accounts.length<2)
            throw new IllegalArgumentException("بيانات قيد المصنع غير مكتملة");
        long totalDebit=0,totalCredit=0;int count=0;
        for(int i=0;i<accounts.length;i++) {
            if(accounts[i]==null||accounts[i].trim().isEmpty()||debits[i]<0||credits[i]<0||
               ((debits[i]>0)==(credits[i]>0)))throw new IllegalArgumentException("سطر قيد المصنع غير صالح");
            totalDebit=checkedAdd(totalDebit,debits[i]);totalCredit=checkedAdd(totalCredit,credits[i]);count++;
        }
        GeneralLedger.validateBalancedTotals(totalDebit,totalCredit,count);
        ContentValues head=new ContentValues();head.put("entry_ref",reference.trim());
        head.put("description",description);head.put("status","POSTED");head.put("date",date);
        long entryId=source.insertOrThrow("journal_entries",null,head);
        for(int i=0;i<accounts.length;i++) {
            ContentValues line=new ContentValues();line.put("entry_id",entryId);
            line.put("account",accounts[i].trim());line.put("debit",debits[i]);line.put("credit",credits[i]);
            source.insertOrThrow("journal_lines",null,line);
        }
        enqueue(source,entryId);
        return entryId;
    }

    /** Exact source labels are intentionally mapped by an authorized operator, never guessed. */
    static void saveMapping(SQLiteDatabase source,String sourceAccount,String targetAccount,
                            String branch,String currency,long fxNum,long fxDen) {
        if(sourceAccount==null||sourceAccount.trim().isEmpty()||targetAccount==null||targetAccount.trim().isEmpty()||
           branch==null||branch.trim().isEmpty()||currency==null||currency.trim().isEmpty()||fxNum<=0||fxDen<=0)
            throw new IllegalArgumentException("بيانات ربط الحساب غير مكتملة");
        ensureSchema(source);
        ContentValues v=new ContentValues();v.put("source_account",sourceAccount.trim());
        v.put("target_account_code",targetAccount.trim());v.put("branch_code",branch.trim());
        v.put("currency_code",currency.trim().toUpperCase(java.util.Locale.US));
        v.put("fx_rate_num",fxNum);v.put("fx_rate_den",fxDen);v.put("active",1);
        v.put("updated_at",System.currentTimeMillis());
        source.insertWithOnConflict("factory_gl_account_mappings",null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }

    /**
     * Re-resolves a failed mapping only when no destination journal exists for that source.
     * This prevents an operator edit from changing a payload after destination commit.
     */
    static int refreshUnpostedMappings(Context context,SQLiteDatabase source,String sourceAccount) {
        if(sourceAccount==null||sourceAccount.trim().isEmpty())throw new IllegalArgumentException("اسم حساب المصدر مطلوب");
        SQLiteDatabase destination=null;
        try {
            destination=SecureDatabase.open(context,"almizan_bakery_250.db");
            EnterpriseSchema.ensure(destination);
            int refreshed=0;
            source.beginTransaction();
            try {
                Cursor events=source.rawQuery("SELECT DISTINCT e.id,e.source_entry_ref FROM factory_gl_outbox e "+
                    "JOIN factory_gl_outbox_lines l ON l.outbox_id=e.id "+
                    "WHERE l.source_account=? AND e.status IN ('PENDING','RETRY') ORDER BY e.id",
                    new String[]{sourceAccount.trim()});
                List<long[]> candidates=new ArrayList<>();
                try {while(events.moveToNext())candidates.add(new long[]{events.getLong(0)});}finally{events.close();}
                for(long[] candidate:candidates) {
                    long eventId=candidate[0];String sourceRef;
                    Cursor ref=source.rawQuery("SELECT source_entry_ref FROM factory_gl_outbox WHERE id=?",
                        new String[]{String.valueOf(eventId)});
                    try{if(!ref.moveToFirst())continue;sourceRef=ref.getString(0);}finally{ref.close();}
                    Cursor posted=destination.rawQuery("SELECT COUNT(*) FROM gl_journal_entries WHERE source_type=? AND source_id=?",
                        new String[]{SOURCE_TYPE,sourceRef});
                    boolean alreadyPosted;try{posted.moveToFirst();alreadyPosted=posted.getLong(0)>0;}finally{posted.close();}
                    if(alreadyPosted)continue;
                    ContentValues clear=new ContentValues();clear.putNull("target_account_code");clear.putNull("branch_code");
                    clear.putNull("currency_code");clear.putNull("fx_rate_num");clear.putNull("fx_rate_den");
                    source.update("factory_gl_outbox_lines",clear,"outbox_id=? AND source_account=?",
                        new String[]{String.valueOf(eventId),sourceAccount.trim()});
                    ContentValues reset=new ContentValues();reset.put("status","PENDING");reset.put("next_attempt_at",0L);
                    reset.put("last_error","");reset.put("updated_at",System.currentTimeMillis());
                    source.update("factory_gl_outbox",reset,"id=?",new String[]{String.valueOf(eventId)});
                    refreshed++;
                }
                source.setTransactionSuccessful();
            } finally {source.endTransaction();}
            return refreshed;
        } finally {if(destination!=null&&destination.isOpen())destination.close();}
    }

    /** Reopens dead-letter events only after checking that destination posting did not commit. */
    static int requeueDeadLetters(Context context,SQLiteDatabase source) {
        SQLiteDatabase destination=null;
        try {
            destination=SecureDatabase.open(context,"almizan_bakery_250.db");
            EnterpriseSchema.ensure(destination);
            int requeued=0;
            source.beginTransaction();
            try {
                Cursor events=source.rawQuery("SELECT id,source_entry_ref FROM factory_gl_outbox WHERE status='DEAD_LETTER' ORDER BY id",null);
                List<long[]> ids=new ArrayList<>();List<String> refs=new ArrayList<>();
                try{while(events.moveToNext()){ids.add(new long[]{events.getLong(0)});refs.add(events.getString(1));}}finally{events.close();}
                for(int i=0;i<ids.size();i++) {
                    long id=ids.get(i)[0];String sourceRef=refs.get(i);
                    Cursor posted=destination.rawQuery("SELECT entry_ref FROM gl_journal_entries WHERE source_type=? AND source_id=? AND status='POSTED'",
                        new String[]{SOURCE_TYPE,sourceRef});
                    String destinationRef=null;try{if(posted.moveToFirst())destinationRef=posted.getString(0);}finally{posted.close();}
                    ContentValues event=new ContentValues();event.put("updated_at",System.currentTimeMillis());
                    if(destinationRef!=null) {
                        event.put("status","POSTED");event.put("destination_ref",destinationRef);
                        event.put("posted_at",System.currentTimeMillis());event.put("last_error","");
                    } else {
                        ContentValues clear=new ContentValues();clear.putNull("target_account_code");clear.putNull("branch_code");
                        clear.putNull("currency_code");clear.putNull("fx_rate_num");clear.putNull("fx_rate_den");
                        source.update("factory_gl_outbox_lines",clear,"outbox_id=?",new String[]{String.valueOf(id)});
                        event.put("status","PENDING");event.put("attempt_count",0);
                        event.put("next_attempt_at",0L);event.put("last_error","");
                        requeued++;
                    }
                    source.update("factory_gl_outbox",event,"id=?",new String[]{String.valueOf(id)});
                }
                source.setTransactionSuccessful();
            } finally {source.endTransaction();}
            return requeued;
        } finally {if(destination!=null&&destination.isOpen())destination.close();}
    }

    static ProcessResult processPending(Context context,SQLiteDatabase source) {
        SQLiteDatabase destination=null;
        try {
            destination=SecureDatabase.open(context,"almizan_bakery_250.db");
            EnterpriseSchema.ensure(destination);
            return processPending(source,destination);
        } finally {if(destination!=null&&destination.isOpen())destination.close();}
    }

    /** Package-visible overload for deterministic integration tests with an isolated destination DB. */
    static ProcessResult processPending(SQLiteDatabase source,SQLiteDatabase destination) {
        if(source==null||!source.isOpen())throw new IllegalStateException("قاعدة المصنع غير متاحة");
        if(destination==null||!destination.isOpen())throw new IllegalStateException("قاعدة الأستاذ العام غير متاحة");
        ensureSchema(source);
        EnterpriseSchema.ensure(destination);
        List<Long> ids=new ArrayList<>();
        Cursor due=source.rawQuery("SELECT id FROM factory_gl_outbox WHERE status IN ('PENDING','RETRY','PROCESSING') "+
            "AND next_attempt_at<=? ORDER BY id LIMIT "+BATCH_SIZE,
            new String[]{String.valueOf(System.currentTimeMillis())});
        try {while(due.moveToNext())ids.add(due.getLong(0));}finally{due.close();}
        int posted=0,retried=0,dead=0,skipped=0;
        for(Long id:ids) {
            try {
                if(processOne(source,destination,id))posted++;
                else skipped++;
            } catch(Exception e) {
                boolean isDead=recordFailure(source,id,e);
                if(isDead)dead++;else retried++;
            }
        }
        return new ProcessResult(posted,retried,dead,skipped);
    }

    private static boolean processOne(SQLiteDatabase source,SQLiteDatabase destination,long outboxId) {
        String ref,description,date,hash;
        List<SnapshotLine> lines=new ArrayList<>();
        source.beginTransaction();
        try {
            Cursor event=source.rawQuery("SELECT source_entry_ref,description,transaction_date,payload_sha256,status "+
                "FROM factory_gl_outbox WHERE id=?",new String[]{String.valueOf(outboxId)});
            try {
                if(!event.moveToFirst())throw new IllegalArgumentException("حدث Outbox غير موجود");
                ref=event.getString(0);description=event.getString(1);date=event.getString(2);hash=event.getString(3);
                if("POSTED".equals(event.getString(4))||"DEAD_LETTER".equals(event.getString(4))) {
                    source.setTransactionSuccessful();return false;
                }
            } finally {event.close();}
            Cursor rows=source.rawQuery("SELECT id,source_account,debit_minor,credit_minor,target_account_code,branch_code,"+
                "currency_code,fx_rate_num,fx_rate_den FROM factory_gl_outbox_lines WHERE outbox_id=? ORDER BY line_no",
                new String[]{String.valueOf(outboxId)});
            try {
                while(rows.moveToNext()) {
                    String target=rows.isNull(4)?null:rows.getString(4);
                    String branch=rows.isNull(5)?null:rows.getString(5);
                    String currency=rows.isNull(6)?null:rows.getString(6);
                    long num=rows.isNull(7)?0:rows.getLong(7),den=rows.isNull(8)?0:rows.getLong(8);
                    if(target==null||branch==null||currency==null) {
                        Cursor mapping=source.rawQuery("SELECT target_account_code,branch_code,currency_code,fx_rate_num,fx_rate_den "+
                            "FROM factory_gl_account_mappings WHERE source_account=? AND active=1",
                            new String[]{rows.getString(1)});
                        try {
                            if(!mapping.moveToFirst())throw new MissingMappingException("لا يوجد ربط صريح للحساب: "+rows.getString(1));
                            target=mapping.getString(0);branch=mapping.getString(1);currency=mapping.getString(2);
                            num=mapping.getLong(3);den=mapping.getLong(4);
                        } finally {mapping.close();}
                    }
                    lines.add(new SnapshotLine(rows.getLong(0),rows.getString(1),rows.getLong(2),rows.getLong(3),
                        target,branch,currency,num,den));
                }
            } finally {rows.close();}
            if(lines.size()<2)throw new IllegalStateException("حدث Outbox لا يحتوي على سطور كافية");
            if(!hash.equals(payloadHash(ref,description,date,lines)))
                throw new IllegalStateException("بصمة حدث Outbox لا تطابق محتواه؛ تم إيقاف الترحيل");
            for(SnapshotLine line:lines) {
                ContentValues v=new ContentValues();v.put("target_account_code",line.targetAccount);
                v.put("branch_code",line.branch);v.put("currency_code",line.currency);
                v.put("fx_rate_num",line.fxNum);v.put("fx_rate_den",line.fxDen);
                source.update("factory_gl_outbox_lines",v,"id=?",new String[]{String.valueOf(line.id)});
            }
            ContentValues processing=new ContentValues();processing.put("status","PROCESSING");
            processing.put("updated_at",System.currentTimeMillis());processing.put("last_error","");
            source.update("factory_gl_outbox",processing,"id=?",new String[]{String.valueOf(outboxId)});
            source.setTransactionSuccessful();
        } catch(MissingMappingException missing) {
            ContentValues retry=new ContentValues();retry.put("status","RETRY");
            retry.put("last_error",safeError(missing));retry.put("next_attempt_at",System.currentTimeMillis()+60000L);
            retry.put("updated_at",System.currentTimeMillis());
            source.update("factory_gl_outbox",retry,"id=?",new String[]{String.valueOf(outboxId)});
            source.setTransactionSuccessful();
            return false;
        } finally {source.endTransaction();}

        List<GeneralLedger.Line> glLines=new ArrayList<>();
        for(SnapshotLine line:lines)glLines.add(new GeneralLedger.Line(line.targetAccount,line.branch,line.currency,
            line.debit,line.credit,line.fxNum,line.fxDen));
        String glRef="FACTORY-"+ref;
        GeneralLedger.Entry entry=new GeneralLedger.Entry(glRef,description,date,SOURCE_TYPE,ref,
            eventKey(ref),"FACTORY",glLines);
        GeneralLedger.post(destination,entry);
        source.beginTransaction();
        try {
            ContentValues done=new ContentValues();done.put("status","POSTED");
            done.put("destination_ref",glRef);done.put("posted_at",System.currentTimeMillis());
            done.put("updated_at",System.currentTimeMillis());done.put("last_error","");
            source.update("factory_gl_outbox",done,"id=?",new String[]{String.valueOf(outboxId)});
            source.setTransactionSuccessful();
        } finally {source.endTransaction();}
        return true;
    }

    private static boolean recordFailure(SQLiteDatabase source,long id,Exception error) {
        source.beginTransaction();
        try {
            Cursor c=source.rawQuery("SELECT attempt_count FROM factory_gl_outbox WHERE id=?",new String[]{String.valueOf(id)});
            int attempts=0;try{if(c.moveToFirst())attempts=c.getInt(0);}finally{c.close();}
            attempts++;
            boolean dead=attempts>=MAX_ATTEMPTS;
            long delay=Math.min(3600000L,30000L << Math.min(attempts-1,7));
            ContentValues v=new ContentValues();v.put("attempt_count",attempts);
            v.put("status",dead?"DEAD_LETTER":"RETRY");v.put("last_error",safeError(error));
            v.put("next_attempt_at",System.currentTimeMillis()+delay);v.put("updated_at",System.currentTimeMillis());
            source.update("factory_gl_outbox",v,"id=?",new String[]{String.valueOf(id)});
            source.setTransactionSuccessful();return dead;
        } finally {source.endTransaction();}
    }

    static String eventKey(String sourceEntryRef) {
        if(sourceEntryRef==null||sourceEntryRef.trim().isEmpty())throw new IllegalArgumentException("مرجع القيد مطلوب");
        return "factory-ledger:"+sourceEntryRef.trim();
    }

    static String payloadHash(String ref,String description,String date,List<SnapshotLine> lines) {
        List<String> canonical=new ArrayList<>();
        for(SnapshotLine line:lines)canonical.add(line.sourceAccount+"\u001f"+line.debit+"\u001f"+line.credit);
        Collections.sort(canonical);
        StringBuilder payload=new StringBuilder(ref).append('\u001e').append(description).append('\u001e').append(date);
        for(String line:canonical)payload.append('\u001d').append(line);
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(payload.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder();
            for(byte b:digest)hex.append(String.format(java.util.Locale.US,"%02x",b&0xff));
            return hex.toString();
        } catch(Exception e) {throw new IllegalStateException("SHA-256 is unavailable",e);}
    }

    private static long checkedAdd(long a,long b) {
        if(a<0||b<0||Long.MAX_VALUE-a<b)throw new ArithmeticException("Journal total overflow");
        return a+b;
    }
    private static String safeError(Exception e) {
        String s=e==null?"Unknown error":e.getMessage();
        if(s==null||s.trim().isEmpty())s=e.getClass().getSimpleName();
        return s.length()>1000?s.substring(0,1000):s;
    }
    private static final class MissingMappingException extends RuntimeException {
        MissingMappingException(String message){super(message);}
    }
}
