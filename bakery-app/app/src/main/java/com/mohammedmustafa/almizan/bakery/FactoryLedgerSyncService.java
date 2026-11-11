package com.mohammedmustafa.almizan.bakery;

import android.database.Cursor;
import net.zetetic.database.sqlcipher.SQLiteDatabase;

/**
 * Explicit, bounded synchronization from the factory journal/outbox into the
 * enterprise general ledger. Safe to invoke from a background executor.
 */
final class FactoryLedgerSyncService {
    private static final int BACKFILL_LIMIT = 100;
    private static final int MAX_BATCHES = 5;

    private FactoryLedgerSyncService() {}

    static final class Result {
        final int mappingsSeeded;
        final int legacyCaptured;
        final int legacyFailed;
        final int retriesMadeDue;
        final int posted;
        final int retried;
        final int deadLetter;
        final int skipped;
        final FactoryGlOutbox.ReconciliationReport reconciliation;
        final CashBankReconciliationService.Report cashBankReconciliation;

        Result(int mappingsSeeded, FactoryGlOutbox.BackfillResult backfill, int retriesMadeDue,
               int posted, int retried, int deadLetter, int skipped,
               FactoryGlOutbox.ReconciliationReport reconciliation,
               CashBankReconciliationService.Report cashBankReconciliation) {
            this.mappingsSeeded=mappingsSeeded;
            this.legacyCaptured=backfill.captured;
            this.legacyFailed=backfill.failed;
            this.retriesMadeDue=retriesMadeDue;
            this.posted=posted;
            this.retried=retried;
            this.deadLetter=deadLetter;
            this.skipped=skipped;
            this.reconciliation=reconciliation;
            this.cashBankReconciliation=cashBankReconciliation;
        }

        boolean clean() {
            return legacyFailed==0 && reconciliation!=null && reconciliation.clean() &&
                cashBankReconciliation!=null && cashBankReconciliation.balanced();
        }
    }

    static Result synchronize(SQLiteDatabase source, SQLiteDatabase destination) {
        if(source==null||!source.isOpen())throw new IllegalStateException("قاعدة بيانات المصنع غير متاحة");
        if(destination==null||!destination.isOpen())throw new IllegalStateException("قاعدة الأستاذ العام غير متاحة");
        FactoryGlOutbox.ensureSchema(source);
        EnterpriseSchema.ensure(destination);

        String baseCurrency=scalar(destination,"SELECT base_currency FROM company_settings WHERE id=1");
        if(baseCurrency==null||baseCurrency.trim().isEmpty())
            throw new IllegalStateException("عملة المنشأة الأساسية غير معرفة");

        int seeded=0;
        if("SDG".equals(baseCurrency)) {
            FactoryGlOutbox.validateReviewedChart(destination);
            requireActiveMainBranch(destination);
            seeded=FactoryGlOutbox.seedReviewedMappings(source,"MAIN","SDG");
        }

        FactoryGlOutbox.BackfillResult backfill=FactoryGlOutbox.backfillPostedJournals(source,BACKFILL_LIMIT);
        int due=FactoryGlOutbox.makeRetriesDueNow(source);
        int posted=0,retried=0,dead=0,skipped=0;
        for(int batchNo=0;batchNo<MAX_BATCHES;batchNo++) {
            FactoryGlOutbox.ProcessResult batch=FactoryGlOutbox.processPending(source,destination);
            posted+=batch.posted;
            retried+=batch.retried;
            dead+=batch.deadLetter;
            skipped+=batch.skipped;
            if(batch.posted==0)break;
        }
        FactoryGlOutbox.ReconciliationReport report=FactoryGlOutbox.reconcile(source,destination);
        CashBankReconciliationService.Report cashBankReport=
            CashBankReconciliationService.reconcileAllHistory(source,destination,"MAIN",baseCurrency);
        return new Result(seeded,backfill,due,posted,retried,dead,skipped,report,cashBankReport);
    }

    private static void requireActiveMainBranch(SQLiteDatabase db) {
        Cursor c=db.rawQuery("SELECT COUNT(*) FROM branches WHERE branch_code='MAIN' AND active=1",null);
        try {
            if(!c.moveToFirst()||c.getLong(0)!=1)
                throw new IllegalStateException("الفرع الرئيسي MAIN غير موجود أو غير نشط؛ لم يبدأ الترحيل");
        } finally {c.close();}
    }

    private static String scalar(SQLiteDatabase db,String sql) {
        Cursor c=db.rawQuery(sql,null);
        try{return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;}
        finally{c.close();}
    }
}
