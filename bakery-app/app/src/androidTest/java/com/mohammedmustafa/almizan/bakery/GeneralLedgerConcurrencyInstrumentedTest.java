package com.mohammedmustafa.almizan.bakery;

import android.content.Context;
import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class GeneralLedgerConcurrencyInstrumentedTest {
    private static final long AMOUNT = 12500L;

    private static final class Outcome {
        GeneralLedger.Result result;
        Throwable failure;
    }

    @Test public void concurrentSameKeyAndPayloadPostsExactlyOnce() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String suffix = String.valueOf(System.currentTimeMillis());
        String databaseName = "gl_concurrent_same_key_" + suffix + ".db";
        String reference = "GL-CONCURRENT-SAME-" + suffix;
        String sourceId = "SOURCE-CONCURRENT-SAME-" + suffix;
        String key = "IDEM-CONCURRENT-SAME-" + suffix;
        prepareDatabase(context, databaseName);

        try {
            Outcome[] outcomes = runTogether(context, databaseName,
                entry(reference, sourceId, key), entry(reference, sourceId, key));

            for (Outcome outcome : outcomes) {
                assertNull("Same-payload retry must not fail: " + describe(outcome.failure), outcome.failure);
                assertNotNull(outcome.result);
            }
            assertNotEquals("Exactly one caller must perform the first posting",
                outcomes[0].result.alreadyPosted, outcomes[1].result.alreadyPosted);

            SQLiteDatabase verify = SecureDatabase.open(context, databaseName);
            try {
                assertEquals(1L, count(verify,
                    "SELECT COUNT(*) FROM gl_journal_entries WHERE entry_ref=? AND status='POSTED'",
                    new String[]{reference}));
                assertEquals(2L, count(verify,
                    "SELECT COUNT(*) FROM gl_journal_lines l JOIN gl_journal_entries e ON e.id=l.entry_id WHERE e.entry_ref=?",
                    new String[]{reference}));
                assertEquals(1L, count(verify,
                    "SELECT COUNT(*) FROM idempotency_keys WHERE key=? AND operation='GL_POST'",
                    new String[]{key}));
                assertEquals(1L, count(verify,
                    "SELECT COUNT(*) FROM enterprise_audit_log WHERE entity_ref=? AND action='JOURNAL_POSTED'",
                    new String[]{reference}));
                GeneralLedgerReports.TrialBalance report =
                    GeneralLedgerReports.trialBalance(verify, "2026-11-01", "2026-11-30", "MAIN");
                assertEquals("Concurrent retry must not double the debit balance", AMOUNT, report.debitMinor);
                assertEquals("Concurrent retry must not double the credit balance", AMOUNT, report.creditMinor);
                assertTrue(report.isBalanced());
            } finally {
                verify.close();
            }
        } finally {
            context.deleteDatabase(databaseName);
        }
    }

    @Test public void concurrentDifferentKeysForSameSourceCannotCreateTwoJournals() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String suffix = String.valueOf(System.currentTimeMillis());
        String databaseName = "gl_concurrent_source_" + suffix + ".db";
        String sourceId = "SOURCE-CONCURRENT-COLLISION-" + suffix;
        String firstReference = "GL-CONCURRENT-FIRST-" + suffix;
        String secondReference = "GL-CONCURRENT-SECOND-" + suffix;
        prepareDatabase(context, databaseName);

        try {
            Outcome[] outcomes = runTogether(context, databaseName,
                entry(firstReference, sourceId, "IDEM-FIRST-" + suffix),
                entry(secondReference, sourceId, "IDEM-SECOND-" + suffix));

            int successful = 0;
            int uniqueConflicts = 0;
            for (Outcome outcome : outcomes) {
                if (outcome.failure == null) {
                    assertNotNull(outcome.result);
                    successful++;
                } else {
                    assertTrue("Losing request must fail on source uniqueness, not an infrastructure/locking error: "
                        + describe(outcome.failure),
                        containsMessage(outcome.failure, "UNIQUE constraint failed: gl_journal_entries.source_type, gl_journal_entries.source_id"));
                    uniqueConflicts++;
                }
            }
            assertEquals("Exactly one source posting must succeed", 1, successful);
            assertEquals("Exactly one competing source posting must hit the unique constraint", 1, uniqueConflicts);

            SQLiteDatabase verify = SecureDatabase.open(context, databaseName);
            try {
                assertEquals(1L, count(verify,
                    "SELECT COUNT(*) FROM gl_journal_entries WHERE source_type='TEST_CONCURRENT' AND source_id=? AND status='POSTED'",
                    new String[]{sourceId}));
                assertEquals(2L, count(verify,
                    "SELECT COUNT(*) FROM gl_journal_lines l JOIN gl_journal_entries e ON e.id=l.entry_id WHERE e.source_type='TEST_CONCURRENT' AND e.source_id=?",
                    new String[]{sourceId}));
                assertEquals(1L, count(verify,
                    "SELECT COUNT(*) FROM idempotency_keys WHERE operation='GL_POST' AND key LIKE ?",
                    new String[]{"IDEM-%-" + suffix}));
                assertEquals(1L, count(verify,
                    "SELECT COUNT(*) FROM enterprise_audit_log WHERE action='JOURNAL_POSTED' AND details LIKE ?",
                    new String[]{"TEST_CONCURRENT:" + sourceId + ":%"}));
                GeneralLedgerReports.TrialBalance report =
                    GeneralLedgerReports.trialBalance(verify, "2026-11-01", "2026-11-30", "MAIN");
                assertEquals("Competing source request must not double debit totals", AMOUNT, report.debitMinor);
                assertEquals("Competing source request must not double credit totals", AMOUNT, report.creditMinor);
                assertTrue(report.isBalanced());
            } finally {
                verify.close();
            }
        } finally {
            context.deleteDatabase(databaseName);
        }
    }

    private static void prepareDatabase(Context context, String name) {
        SQLiteDatabase db = SecureDatabase.open(context, name);
        try {
            EnterpriseSchema.ensure(db);
            db.execSQL("INSERT INTO financial_periods(period_code,start_date,end_date,status) VALUES(?,?,?,'OPEN')",
                new Object[]{"GL-CONCURRENCY-" + System.currentTimeMillis(), "2026-11-01", "2026-11-30"});
        } finally {
            db.close();
        }
    }

    private static GeneralLedger.Entry entry(String reference, String sourceId, String key) {
        return new GeneralLedger.Entry(reference, "Concurrent idempotency test", "2026-11-12",
            "TEST_CONCURRENT", sourceId, key, "instrumented-test",
            Arrays.asList(
                new GeneralLedger.Line("1000", "MAIN", "SDG", AMOUNT, 0L, 1L, 1L),
                new GeneralLedger.Line("4000", "MAIN", "SDG", 0L, AMOUNT, 1L, 1L)
            ));
    }

    private static Outcome[] runTogether(Context context, String databaseName,
                                         GeneralLedger.Entry first, GeneralLedger.Entry second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Outcome[] outcomes = {new Outcome(), new Outcome()};
        Thread a = worker(context, databaseName, first, outcomes[0], ready, start);
        Thread b = worker(context, databaseName, second, outcomes[1], ready, start);
        a.start();
        b.start();
        assertTrue("Both independent database connections must be ready", ready.await(15, TimeUnit.SECONDS));
        start.countDown();
        a.join(20000);
        b.join(20000);
        assertFalse("First posting thread must finish", a.isAlive());
        assertFalse("Second posting thread must finish", b.isAlive());
        return outcomes;
    }

    private static Thread worker(Context context, String databaseName, GeneralLedger.Entry entry,
                                 Outcome outcome, CountDownLatch ready, CountDownLatch start) {
        return new Thread(() -> {
            SQLiteDatabase db = null;
            try {
                db = SecureDatabase.open(context, databaseName);
                ready.countDown();
                if (!start.await(15, TimeUnit.SECONDS)) throw new AssertionError("Timed out waiting for concurrent start");
                outcome.result = GeneralLedger.post(db, entry);
            } catch (Throwable failure) {
                outcome.failure = failure;
            } finally {
                if (db != null && db.isOpen()) db.close();
            }
        });
    }

    private static long count(SQLiteDatabase db, String sql, String[] args) {
        Cursor cursor = db.rawQuery(sql, args);
        try {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        } finally {
            cursor.close();
        }
    }

    private static boolean containsMessage(Throwable error, String expected) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) return true;
        }
        return false;
    }

    private static String describe(Throwable error) {
        return error == null ? "none" : error.toString();
    }
}
