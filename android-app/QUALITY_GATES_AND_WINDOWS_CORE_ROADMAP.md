# Al-Mizan release quality gates and shared-core roadmap

Date: 2026-10-16  
Status: engineering work plan; **not a release approval**.

## Product decision

Use the Android 2.3.9 development line as the candidate baseline, preserve it, and work on a separate branch. The shared core is a long-term architecture decision, not permission to copy incomplete behavior into every future product. Android remains the first delivery target; Windows must share accounting rules and acceptance tests, not a separately reimplemented ledger.

## Evidence currently available

- The Android app uses a monolithic `MainActivity.java` and SQLiteOpenHelper database schema version 11.
- Monetary values and quantities are stored in SQLite `REAL` and are frequently read as Java `double`. This is a material precision risk and requires a migration plan before release.
- The existing test suite includes database integrity and security-validation tests, but its existence does not establish end-to-end coverage for all business flows.
- The release workflow reports successful builds, automated test tasks, and signing checks on the recorded run. A successful CI job is not proof of correct accounting, offline behavior, restore integrity, or real-device usability.
- Existing commercial-readiness notes identify unresolved concerns around encrypted storage, backup protection, licensing, and device verification.

## Competitor-derived opportunity map

Sources reviewed:
- Zoho Books mobile capabilities: https://www.zoho.com/us/books/accounting-mobile-apps/
- Zoho Books Play listing and reports/approvals: https://play.google.com/store/apps/details?id=com.zoho.books
- Odoo accounting scope (double-entry, bank reconciliation, inventory valuation, multi-company): https://www.odoo.com/documentation/18.0/applications/finance/accounting.html
- Odoo offline mode: https://www.odoo.com/documentation/20.0/sv/applications/general/offline_mode.html
- QuickBooks mobile/web split (mobile capture vs advanced web tools): https://quickbooks.intuit.com/learn-support/en-us/help-article/compare-products/get-done-quickbooks-mobile-app-web/L9YN5gwQw_US_en_US

Do not label a competitor weakness as a verified defect without reproducing it. These are product positioning opportunities:
1. **Offline-first reliability:** maintain local entry, posting, reports, and backup without connectivity; explicitly distinguish local operations from cloud licensing/sync.
2. **Transparent auditability:** visible immutable posting history, reversals rather than destructive edits, period closing, and clear explanation of each balance.
3. **Low-friction regional UX:** Arabic-first RTL, optional English, readable screens on modest Android devices, clear local currency/date formats, and robust receipt sharing/printing.
4. **Trust and portability:** encrypted backups, tested restore, human-readable exports, and no vendor lock-in for a company's own records.
5. **Desktop parity:** Windows interface for longer workflows and reports, backed by the same accounting rules and shared acceptance scenarios.
6. **Predictable commercial licensing:** clear edition comparison, grace-period behavior, recovery after device replacement, and no loss of access to export company data when subscription status changes.

Zoho advertises receivables, payables, banking, multi-currency, audit trails, scanning, and reports; Odoo documents double-entry accounting, bank reconciliation, inventory valuation and multi-company operation. Odoo's offline documentation distinguishes previously available local records from fully online capabilities and notes limitations. QuickBooks describes a mobile/web split. These sources demonstrate market expectations, not that Al-Mizan already matches them.

## Mandatory accounting acceptance matrix

Use a deterministic fixture and verify, after every scenario: source document, stock movement, customer/supplier balance, cash/bank, journal lines, general ledger, trial balance, profit/loss, and balance sheet.

1. Cash sale; credit sale; partial collection; overpayment and unapplied receipt.
2. Cash/credit purchase; partial payment; supplier advance.
3. Discount, tax, rounding boundary, fractional quantities and unit conversion.
4. Partial return, cumulative return limit, refund, return of damaged/non-restockable goods.
5. Cancellation/reversal after posting; preserve original audit trail.
6. Expense, payroll accrual/payment, salary advance and deduction.
7. Incoming/outgoing cheque lifecycle: pending, cleared, returned, cancelled; bank reconciliation.
8. Stock adjustment, negative-stock policy, valuation and cost-of-goods-sold.
9. Opening balances, date boundaries, closed accounting period, duplicate submissions.
10. Backup, restore, upgrade from prior schema, corrupted/incomplete backup rejection.

For every journal entry, assert debits equal credits exactly at the configured currency scale. Verify invariants independently from displayed totals; do not use the same calculation routine for both expected and actual results.

## Money precision migration gate

A new `Money` value type was added as a foundation only. It is **not yet wired into the existing database or posting code**. Do not claim precision is fixed until:
- Company currency and scale are explicit.
- All monetary storage, calculations, input validation, posting and reports use integer minor units or an equivalently exact decimal model.
- Existing REAL values are migrated transactionally with before/after balance reconciliation and a recoverable backup.
- Quantities and unit prices use documented decimal precision separately from currency rounding.
- All accounting scenarios pass at boundaries and with negative/large values.

## Data protection and offline gate

- Evaluate SQLCipher or a documented platform-keystore-based encryption design; SQLite by itself is not encrypted at rest.
- Encrypt backup exports, verify integrity before restore, restore into a staging database first, and keep the current database untouched if validation fails.
- Exercise airplane mode during launch, entry, posting, reports, export, and license checks. Queue only operations designed for sync; prevent duplicate posting on reconnect.
- Define license grace period and clock/device replacement behavior. Do not present a client-only timer as tamper-proof.

## Windows strategy

Preferred: a Windows desktop client using a shared, platform-neutral accounting domain/API and the same golden test fixtures. Avoid porting Android UI code. Before choosing WinUI/WPF/.NET or a web-desktop shell, benchmark printing, Arabic RTL, offline persistence, update delivery, and signing. Keep local-first behavior consistent; do not introduce a second ledger implementation that can diverge.


## Restore regression found during code inspection (2026-10-16)

The existing encrypted-restore validation accepted database schema versions only from 0 through 10, while this candidate opens the live database at schema version 11. That mismatch could reject a valid backup created by the current app. The check now accepts versions 0 through 11 and rejects negative/future versions; a focused regression test was added. This is a source-level fix only: the Android test task and an actual encrypted export/restore cycle still must pass before this gate is considered closed. A future version remains rejected intentionally so the app does not attempt to open an unknown schema.


## Shared accounting-core foundation (2026-10-16)

Added a pure-Java AccountingCore journal invariant validator and unit tests. It uses integer minor units, requires at least two journal lines, rejects negative/empty/both-sided lines, checks exact debit/credit equality, and fails on integer overflow. Added tests for a balanced entry, an unbalanced entry, invalid lines, and overflow. This is a reusable domain foundation—not yet wired into the existing sales/purchase/payroll/bank posting paths, and not yet a Windows client. The CI result for these newly added Android unit tests has not been confirmed in this work session.


## Read-only legacy-money migration preflight (2026-10-16)

Added `MoneyMigrationAudit`, a read-only SQLite column auditor. It checks that every non-null legacy REAL value is finite, representable at the configured currency scale without implicit rounding, and within integer minor-unit range; it reports row IDs that need review and detects aggregate overflow. Unit tests cover exact values, fractional precision that needs review, range overflow, unsafe identifiers, and the invariant that audit itself does not modify legacy rows. This is a **preflight only**: it does not alter the schema, backfill minor-unit columns, or prove whole-ledger reconciliation. The actual migration remains blocked until every relevant monetary column is inventoried, before/after account-level balances are reconciled, and a rollback test passes.

## Human review exit criteria

Release only after all P0 issues are closed, CI passes for all editions, signed artifact hashes and certificate identity are recorded, upgrade/restore tests pass, and a human reviewer completes a documented test session on the target Samsung Android 16 device. Windows remains a separate release candidate and must pass its own tests. No honest process can guarantee 100% defect-free software; the goal is measurable coverage, zero known release-blocking defects, and explicit residual-risk acceptance.


## Actual posting-path invariant integration (2026-10-16)

The persisted-journal check called by the current transaction posting flows now delegates to `JournalInvariantBridge`, which converts the saved legacy debit/credit lines to the configured currency's minor units and validates them through `AccountingCore`. Zero/zero legacy lines are ignored; negative/non-finite amounts, lines with both sides populated, precision beyond a tiny floating-point representation tolerance, integer overflow, and unbalanced totals reject the transaction. This makes the invariant check part of the current posting boundary rather than a standalone helper.

Focused bridge tests cover a representative sale journal, imbalance, excess precision, and invalid sides/negative values. The migration preflight now rejects NULL monetary values for explicit review instead of silently skipping them.

**Important boundary:** this integration validates journal rows after they are inserted but before the surrounding SQLite transaction is marked successful in the posting flows that call `assertBalanced`. It does not yet replace all REAL/double arithmetic or normalize every transaction input, quantity, cost-of-goods-sold calculation, payment, bank movement, payroll, or report. Some legacy flows call their own validation or may not call this method; these paths still require inventory and tests. Because the posting layer now rejects genuine sub-minor-unit values instead of silently rounding them, test fractional quantities, unit costs, discounts, tax, and all currencies before any release. CI execution and actual device transactions have not yet been verified in this work session.
