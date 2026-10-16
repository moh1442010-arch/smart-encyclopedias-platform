package com.mohammedmustafa.smartcompany;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Read-only preflight for migrating legacy SQLite REAL monetary columns.
 *
 * It does not modify rows. A migration must not proceed when any value is
 * non-finite, outside the configured scale/range, or when aggregate exact
 * minor units cannot be computed. Callers must audit every monetary column
 * and independently reconcile journal/account/customer/supplier balances.
 */
public final class MoneyMigrationAudit {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private MoneyMigrationAudit() {}

    public static final class RejectedValue {
        public final long rowId;
        public final String reason;

        RejectedValue(long rowId, String reason) {
            this.rowId = rowId;
            this.reason = reason;
        }
    }

    public static final class Result {
        public final String table;
        public final String column;
        public final int scale;
        public final long rowCount;
        public final long exactMinorUnitTotal;
        public final BigDecimal legacyDecimalTotal;
        public final List<RejectedValue> rejectedValues;
        public final boolean aggregateOverflow;

        Result(String table, String column, int scale, long rowCount,
               long exactMinorUnitTotal, BigDecimal legacyDecimalTotal,
               List<RejectedValue> rejectedValues, boolean aggregateOverflow) {
            this.table = table;
            this.column = column;
            this.scale = scale;
            this.rowCount = rowCount;
            this.exactMinorUnitTotal = exactMinorUnitTotal;
            this.legacyDecimalTotal = legacyDecimalTotal;
            this.rejectedValues = Collections.unmodifiableList(new ArrayList<>(rejectedValues));
            this.aggregateOverflow = aggregateOverflow;
        }

        public boolean isSafeForColumnMigration() {
            if (aggregateOverflow || !rejectedValues.isEmpty()) return false;
            try {
                BigDecimal convertedTotal = BigDecimal.valueOf(exactMinorUnitTotal, scale);
                return convertedTotal.compareTo(legacyDecimalTotal) == 0;
            } catch (ArithmeticException ex) {
                return false;
            }
        }
    }

    /** Read-only audit. Identifiers are strictly validated before SQL composition. */
    public static Result auditColumn(SQLiteDatabase database, String table, String column, int scale) {
        if (database == null) throw new IllegalArgumentException("Database is required");
        validateIdentifier(table);
        validateIdentifier(column);
        if (scale < 0 || scale > 6) throw new IllegalArgumentException("Currency scale must be between 0 and 6");

        long count = 0;
        long minorTotal = 0;
        boolean overflow = false;
        BigDecimal decimalTotal = BigDecimal.ZERO;
        List<RejectedValue> rejected = new ArrayList<>();
        Cursor cursor = database.rawQuery("SELECT rowid, " + column + " FROM " + table + " ORDER BY rowid", null);
        try {
            while (cursor.moveToNext()) {
                count++;
                long rowId = cursor.getLong(0);
                if (cursor.isNull(1)) {
                    rejected.add(new RejectedValue(rowId, "Null monetary value requires review"));
                    continue;
                }
                double value = cursor.getDouble(1);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    rejected.add(new RejectedValue(rowId, "Non-finite legacy value"));
                    continue;
                }
                BigDecimal decimal;
                long minor;
                try {
                    decimal = BigDecimal.valueOf(value);
                    minor = decimal.setScale(scale, RoundingMode.UNNECESSARY)
                            .movePointRight(scale).longValueExact();
                } catch (ArithmeticException ex) {
                    rejected.add(new RejectedValue(rowId, "Precision or integer range requires review"));
                    continue;
                }
                try {
                    minorTotal = Math.addExact(minorTotal, minor);
                } catch (ArithmeticException ex) {
                    overflow = true;
                    rejected.add(new RejectedValue(rowId, "Aggregate minor-unit total overflow requires review"));
                    continue;
                }
                decimalTotal = decimalTotal.add(decimal);
            }
        } finally {
            cursor.close();
        }
        return new Result(table, column, scale, count, minorTotal, decimalTotal, rejected, overflow);
    }

    private static void validateIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Unsafe SQL identifier");
        }
    }
}
