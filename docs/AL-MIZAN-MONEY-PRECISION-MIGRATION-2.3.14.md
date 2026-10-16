# Al-Mizan Financial Precision Migration — 2.3.14 Work Package

Status: discovery and migration specification only. This document does not claim that monetary storage has already changed or that the application is release-approved.

## Verified current risk

The Android SQLite schema in MainActivity.DB.schema() declares monetary values as SQLite REAL. SQLite floating-point values use binary floating-point representation and are not an appropriate canonical ledger representation for exact currency amounts. A global replacement of REAL with INTEGER is unsafe: quantities and reorder thresholds are also REAL, and existing SQL, Java calculations, reports, backups, and audit snapshots depend on the current column names and units.

## Monetary columns identified in the current schema

| Table | Monetary columns | Keep as non-monetary / separate |
|---|---|---|
| products | sale_price, cost | qty, reorder are quantities |
| customers | credit_limit | — |
| employees | salary | — |
| payroll_events | amount | — |
| transactions | total, paid, remaining | IDs, dates, status |
| invoice_items | price, cost, discount | qty is quantity |
| bank_accounts | opening_balance | — |
| bank_movements | amount | — |
| checks | amount | — |
| party_payments | amount | — |
| journal_lines | debit, credit | — |

This is the schema-level inventory. It is not yet proof that every read/write path, report, export, backup, or calculation has been migrated.

## Required implementation design

1. Introduce a single currency-aware money type backed by integer minor units. For the current SDG-only configuration, document and enforce the storage scale explicitly (100 minor units per SDG unit); do not derive storage scale from display-decimal preferences. If other currencies remain enabled, use currency metadata and reject unsupported scales rather than silently applying SDG scale.
2. Replace floating-point parsing and arithmetic at input boundaries with strict decimal parsing (BigDecimal or equivalent) and explicit rounding policy. No binary double may be the canonical representation of a monetary amount.
3. Add integer minor-unit columns through a versioned migration. Do not change old columns in place or overwrite source data during the first pass.
4. Perform migration inside one SQLite transaction. For each legacy value, convert with a documented rounding rule; validate row counts, null handling, range/overflow, and aggregate reconciliations before committing. On any discrepancy, roll back and preserve the original database.
5. Update all writes, reads, joins, totals, journal postings, print/PDF output, CSV/export, backup/restore, audit snapshots, and reports together. Avoid mixed-unit comparisons or dual-writing without consistency checks.
6. Add deterministic tests for positive/negative values, half-minor-unit boundaries, large amounts, discounts, tax, returns, partial payments, journal balance, bank reconciliation, backup/restore, and upgrades from each supported schema.
7. Preserve database file name, package ID, signing continuity, and user data. Do not change release version or declare a new APK ready until CI passes and upgrade/restore/accounting regression checks are complete.

## Release acceptance gates

- Every monetary write path stores integer minor units; no monetary calculations depend on SQLite REAL.
- Conversion is transaction-safe and repeatable; legacy data remains recoverable until validation succeeds.
- For the migration fixture, row counts and every account/customer/supplier/cash/bank/inventory-related monetary total reconcile before and after conversion to the documented minor-unit rounding rule.
- Journal entries balance exactly in minor units.
- Sales, purchases, returns, partial payments, payroll, checks, bank movements, and reports pass regression tests.
- Backup/export and restore preserve exact minor-unit values.
- CI is green on the final commit, then upgrade tests are performed on a real Android device before release approval.

## Known limitation

The existing audit trail is in the same SQLite database as operational records. It improves traceability but is not independently tamper-proof against someone replacing or directly editing the database file. SQLCipher / key management and a stronger externally anchored audit design remain separate security work packages.