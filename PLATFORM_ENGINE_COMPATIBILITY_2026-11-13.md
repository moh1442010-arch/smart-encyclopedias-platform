# Cross-platform Accounting Compatibility Contract

Date: 2026-11-13
Status: Working engineering contract; not a declaration of completed implementation.

## 1. Current source evidence

| Component | Current source evidence | Decision |
|---|---|---|
| Bakery / Factory Android candidate 2.8.3 | SQLCipher databases are separate for bakery and factory. The general-ledger service stores integer minor units, validates balanced base-currency totals, and supports idempotent posting. | KEEP the accounting core as the leading reference implementation. ADAPT it behind stable interfaces before sharing with other products. |
| General Android app 2.4.13 | The main application source still contains SQLite REAL columns and Java double arithmetic in operational/accounting flows. | RETAIN the useful operational UI and workflows only after characterization. REBUILD or migrate its financial persistence and posting boundaries. |
| Windows app candidate 2.3.1 build | The Windows source defines monetary and quantity REAL columns, and its posting API accepts double debit/credit values. | RETAIN Windows UI and supported workflows where useful. REBUILD the money and ledger boundary around exact integer minor units; migrate legacy data additively. |

The Android and Windows clients are not yet financially equivalent to the bakery/factory accounting core. A matching screen or report label is not evidence of equivalent posting, valuation, or reconciliation behavior.

## 2. Target architecture

1. **Shared accounting contract** — currency-aware money values, account codes, branch identity, source-document identity, posting date, actor, exchange-rate evidence, idempotency key, immutable payload hash, and reversal relationship.
2. **Accounting engine** — balanced double-entry posting, atomic persistence, period controls, audit trail, account history, trial balance, financial statements, and source-to-ledger reconciliation.
3. **Operational modules** — sales, receipts, purchases, supplier payments, expenses, inventory, production, returns, payroll, fixed assets, depreciation, and bank movements.
4. **Platform adapters** — Android and Windows persistence/UI adapters implement the same contract without requiring identical UI code or identical local database files.
5. **Company and branch boundary** — each legal entity has an isolated ledger/database boundary. Branches belong to that entity. Consolidated reporting reads explicit, reconciled company outputs; it does not merge source ledgers.
6. **Offline-first services** — daily posting remains local. Sync, license checks, update discovery, and optional remote backup must have explicit retry, conflict, and offline behavior.

## 3. Money and quantity invariants

- Do not use binary floating-point types for persisted monetary values, posting calculations, balances, taxes, payroll, depreciation, or exchange-rate arithmetic.
- Persist money as signed 64-bit integer minor units with an explicit ISO currency code and the currency's configured minor-unit precision.
- Use checked integer/rational arithmetic for exchange conversion. Persist numerator, denominator, effective date, source, and rounding policy.
- Quantities may require fixed-point precision independently of money. Define a scale and unit per inventory item; do not reuse monetary precision for quantity.
- Never infer currency from locale, display text, or a subscription payment currency.
- Never apply a rate of 1:1 to a foreign currency unless the transaction explicitly records that rate and its source.
- Reports must include posted historical activity even if an account is now inactive. Deactivation blocks new postings; it does not erase history.
- A report may omit an inactive account only when it has no posted activity in the report's scope.

## 4. Legacy monetary migration contract

A REAL/double migration is a controlled data migration, not a schema-only edit.

1. Inventory every table, column, writer, reader, report, export, and backup path that reads or writes a monetary or quantity value.
2. Take and verify an encrypted pre-migration backup; retain the original database untouched.
3. Create additive integer-minor-unit columns/tables and a migration ledger that records source table, source row ID, source column, original value representation, target currency, target integer value, conversion policy, and review status.
4. Convert using the explicitly configured currency precision. Do not silently round. If a value cannot be represented exactly under the approved currency precision, mark it REVIEW_REQUIRED and block financial cutover for that record.
5. Reconcile row counts, document totals, customer and supplier balances, inventory quantities/valuation, payroll, fixed assets, journal totals, and every financial report before and after migration.
6. Require debit = credit for every migrated journal and reconcile source documents to exactly one destination posting.
7. Make migration resumable and idempotent. A repeated migration must not duplicate rows or change already-approved values.
8. Keep legacy values read-only until independent reconciliation and owner acceptance. Define a rollback path before cutover.
9. Test migration from each supported released schema version, including partially migrated and interrupted states.

## 5. Posting contract

Every posting command must:
- Validate the source document, company, branch, currency, account mappings, period, and permissions.
- Reject fewer than two lines, negative amounts, zero-sided lines, lines with both debit and credit, overflow, unsupported currency precision, and unequal base-currency totals.
- Commit the source snapshot, outbox event, journal header, journal lines, idempotency record, and audit record atomically where they share a database boundary.
- Use a stable source key and immutable payload hash. Same key/same payload is an idempotent success; same key/changed payload is rejected.
- Preserve the original posted journal. Corrections create a separately linked reversal or adjustment with a reason and actor.
- Block new postings to closed periods. Reopening a period requires an authorized, audited action.
- Expose failed outbox events, bounded retries, dead-letter recovery, and source/outbox/ledger reconciliation.

## 6. Data boundaries and compatibility

- Preserve existing package IDs, application IDs, signing certificates, database names, encryption keys, and user data during upgrades.
- Bakery and factory databases remain separate. A bridge uses immutable snapshots and explicit mappings; it must not copy or merge operational tables.
- Windows and general Android migrations are independent. Do not copy a database file across platforms or assume SQLite/SQLCipher compatibility.
- Schema changes are additive until a tested migration and rollback path is accepted.
- Backup formats must be versioned, authenticated, encrypted, and validated before restore. Restore must create a pre-restore snapshot and roll back atomically on failure.
- Entitlements must be enforced at every protected entry route, not only on a visible subscription screen. Expiry must not delete customer data.

## 7. Compatibility decision matrix

| Capability | Bakery/Factory Android | General Android | Windows |
|---|---|---|---|
| Exact money model | KEEP; verify all operational producers and reports | REBUILD/MIGRATE | REBUILD/MIGRATE |
| Double-entry ledger | KEEP and complete reconciliation | ADAPT only after contract conformance | ADAPT only after contract conformance |
| Source posting and idempotency | ADAPT across all modules | REBUILD at operational boundaries | REBUILD at operational boundaries |
| Company/branch/currency context | ADAPT; remove hard-coded SDG/MAIN paths | REBUILD around company configuration | REBUILD around company configuration |
| Bakery/factory isolation | KEEP separate databases | Not applicable unless specialized module is added | Not applicable unless specialized module is added |
| Legacy data migration | Verify each released schema | Required | Required |
| Encryption and restore | Verify SQLCipher, keys, backups, rollback | Separate security review required | Encryption-at-rest implementation and review required |
| Reports and financial statements | Complete and independently reconcile | Rebuild from posted ledger | Rebuild from posted ledger |

## 8. Required release gates

No platform is financially production-ready until all applicable gates have fresh evidence:

- Unit, integration, migration, and end-to-end tests pass.
- Journal and subledger reconciliations have zero unexplained differences.
- Trial balance balances; income statement and statement of financial position reconcile through a documented period-close process; cash flow statement reconciles to cash movements.
- Upgrade tests preserve installed data, package identity, signature, and license state.
- Backup tamper detection, clean restore, interrupted restore rollback, low-storage handling, and cross-version restore tests pass.
- Offline behavior, sync conflict handling, license expiry/renewal, and all direct-entry routes are tested.
- Arabic/English, RTL/LTR, printing, export, and accessibility are checked on supported devices.
- Security review covers local data, keys, backups, transport, licensing, and update authenticity.
- Each closed repair-register item links to a source commit, a reproducible test, a passing result, and retained evidence.
- Owner acceptance is recorded on real Android and Windows installations before commercial publication.

## 9. Current implementation checkpoint

- A narrow defect fix is under review in PR #133: historical posted balances remain visible after account deactivation, while unused inactive accounts stay out of reports.
- The new regression case is included in the Android instrumentation suite.
- CI unit tests have passed for the PR head at the time of this checkpoint; emulator tests for API 28 and API 35 are still running.
- The Windows REAL/double migration, general Android migration, cross-platform accounting contract implementation, and remaining repair-register work are not complete.
- No commercial artifact, signing key, live license service, or customer database has been modified by this work branch.
