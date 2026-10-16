package com.mohammedmustafa.smartcompany;

import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MoneyMigrationAuditTest {
    private SQLiteDatabase database;

    @Before public void setUp() {
        database = SQLiteDatabase.create(null);
        database.execSQL("CREATE TABLE legacy_amounts (id INTEGER PRIMARY KEY, amount REAL)");
    }

    @After public void tearDown() {
        if (database != null) database.close();
    }

    @Test public void readOnlyAuditAcceptsValuesExactlyRepresentableAtCurrencyScale() {
        database.execSQL("INSERT INTO legacy_amounts(amount) VALUES (0.10), (0.20), (-0.05)");
        MoneyMigrationAudit.Result result =
                MoneyMigrationAudit.auditColumn(database, "legacy_amounts", "amount", 2);
        assertEquals(3L, result.rowCount);
        assertEquals(25L, result.exactMinorUnitTotal);
        assertTrue(result.rejectedValues.isEmpty());
        assertTrue(result.isSafeForColumnMigration());
    }

    @Test public void auditRejectsNullMonetaryValuesForExplicitReview() {
        database.execSQL("INSERT INTO legacy_amounts(amount) VALUES (NULL)");
        MoneyMigrationAudit.Result result =
                MoneyMigrationAudit.auditColumn(database, "legacy_amounts", "amount", 2);
        assertFalse(result.isSafeForColumnMigration());
        assertEquals(1, result.rejectedValues.size());
        assertTrue(result.rejectedValues.get(0).reason.contains("Null"));
    }

    @Test public void auditRejectsValuesThatWouldNeedSilentRounding() {
        database.execSQL("INSERT INTO legacy_amounts(amount) VALUES (1.005)");
        MoneyMigrationAudit.Result result =
                MoneyMigrationAudit.auditColumn(database, "legacy_amounts", "amount", 2);
        assertFalse(result.isSafeForColumnMigration());
        assertEquals(1, result.rejectedValues.size());
        assertTrue(result.rejectedValues.get(0).reason.contains("review"));
    }

    @Test public void auditRejectsOutOfRangeIntegerMinorUnitConversion() {
        database.execSQL("INSERT INTO legacy_amounts(amount) VALUES (1.0e20)");
        MoneyMigrationAudit.Result result =
                MoneyMigrationAudit.auditColumn(database, "legacy_amounts", "amount", 2);
        assertFalse(result.isSafeForColumnMigration());
        assertEquals(1, result.rejectedValues.size());
    }

    @Test public void auditDetectsAggregateMinorUnitOverflow() {
        database.execSQL("INSERT INTO legacy_amounts(amount) VALUES (50000000000000000.0), (50000000000000000.0)");
        MoneyMigrationAudit.Result result =
                MoneyMigrationAudit.auditColumn(database, "legacy_amounts", "amount", 2);
        assertTrue(result.aggregateOverflow);
        assertFalse(result.isSafeForColumnMigration());
    }

    @Test public void auditDoesNotAllowSqlIdentifierInjection() {
        try {
            MoneyMigrationAudit.auditColumn(database, "legacy_amounts; DROP TABLE legacy_amounts", "amount", 2);
            fail("Untrusted SQL identifiers must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("identifier"));
        }
    }

    @Test public void auditDoesNotModifyLegacyRows() {
        database.execSQL("INSERT INTO legacy_amounts(amount) VALUES (10.25), (4.75)");
        MoneyMigrationAudit.auditColumn(database, "legacy_amounts", "amount", 2);
        android.database.Cursor c = database.rawQuery("SELECT COUNT(*), SUM(amount) FROM legacy_amounts", null);
        try {
            assertTrue(c.moveToFirst());
            assertEquals(2, c.getInt(0));
            assertEquals(15.0, c.getDouble(1), 0.000001);
        } finally {
            c.close();
        }
    }
}
