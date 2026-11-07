# 2.8.1 P0 closure and P1 accounting integration blueprint

Date: 2026-11-06
Branch: `hotfix/bakery-expired-trial-home-bypass`
Base: `release/bakery-2.8.0-financial-integrity`

## P0 current evidence and blockers

- User-confirmed field upgrade path: 2.6.17 was installed first, then upgraded to 2.8.0. The user reported that accounting entries, balances, flour/stock balances, and remaining trial days were retained. This is a positive field observation for the 2.6.17 -> 2.8.0 path, not a full database-diff test and not proof of 2.8.1 compatibility.
- 2.8.1 build run 816 completed compile and arithmetic unit tests. Signing verification failed because the workflow still expected versionCode 280000 while Gradle correctly declares 280001. The certificate SHA-256 matched the approved fingerprint and APK v1/v2/v3 signatures verified. Run 818 was cancelled before the arithmetic test step completed; it is not a passing build.
- Workflow verification metadata has now been corrected to expect versionCode 280001. Run 818 was later cancelled before arithmetic tests completed; the build job was explicitly re-run for a fresh attempt. The new attempt must establish whether signing and emulator checks pass.
- Licensing workflow run 273 passed its validation checks.
- PR #99 remains Draft and unmerged. Commercial release remains disabled pending real-device qualification.
- Password-encrypted portable backup support (AES-256-GCM, PBKDF2 key derivation, authenticated tamper detection) and expired-trial regression tests have been implemented on the hotfix branch; CI and runtime acceptance remain pending.
- Migration fault injection, API 23/API 35 completion, and physical 2.6.17 -> 2.8.0 -> 2.8.1 upgrade remain open acceptance gates.

## P0 acceptance checklist

1. Build and sign 2.8.1; verify package, versionCode 280001, certificate fingerprint, and APK SHA-256.
2. Pass emulator smoke tests at API 23 and API 35 on the exact same commit.
3. Install 2.8.1 as an update over 2.8.0 on a physical device. Do not uninstall or clear app data.
4. Test expired-trial entry through home, every home-navigation callback, activity resume, factory, and factory ledger; only the subscription and activation interface may remain reachable.
5. Test a previously issued lifetime license, a newly issued lifetime license, annual and monthly licenses, and legacy device identifier compatibility.
6. Compare bakery and factory database row counts, key balances, stock quantities, references, and trial state before and after update.
7. Test offline transactions, force-stop/reopen, encrypted backup, restore, corrupted backup rejection, wrong-password rejection, and compatibility import of legacy plaintext backups.
8. Never remove or overwrite customer data as a licensing enforcement action.

## P1 architecture — shared accounting integration

### Design rule
Keep bakery and factory operational databases separate. Introduce a common accounting schema and a source-document integration layer. Do not merge or copy operational tables between databases. A posted inter-unit transfer must create a paired source reference and balanced accounting effects in both ledgers, with elimination/reporting rules preventing double-counting.

### Accounting tables (target schema)
- `accounting_schema_meta`: schema version and migration state.
- `accounting_periods`: period dates, status OPEN/CLOSING/CLOSED, close metadata.
- `accounts`: stable account code, parent code, account class, normal balance, active flag, currency policy.
- `journal_entries`: unique entry reference, source type/id, posting state, date, period, created-by, reversal link, idempotency key.
- `journal_lines`: entry id, account id, debit/credit in integer minor currency units, currency, optional dimensions.
- `source_postings`: unique (source database, source type, source id, event type) to guarantee exactly-once posting.
- `dimensions`: bakery, factory, branch, warehouse, product, batch, cost center.
- `accounting_audit`: append-only actor, action, source, timestamp and reason.
- `exchange_rates`: currency pair, effective date, rate source and precision.
- `period_balances`: derived/cache only; never the source of truth.

### Posting invariants
- Every journal entry has at least two lines.
- Each line has exactly one positive side: debit or credit.
- Sum(debit) equals sum(credit) for each entry in the base currency.
- Posted entries are immutable; correction uses a linked reversal and replacement.
- Source idempotency prevents retry from posting the same sale, receipt, payroll, stock movement, production completion, spoilage, return or transfer twice.
- A closed period rejects new backdated posting unless a privileged reopen is audited.
- Monetary amounts use integer minor units or a defined decimal representation; quantities use scaled integers with explicit unit conversion.
- No direct SQL writes to journal tables from UI screens; use one transaction-aware posting service.

### Event-to-account mapping to be configured, not hard-coded into UI
- Cash sale: debit cash/bank/receivable; credit sales revenue and output tax payable where configured.
- Credit sale: debit customer receivable; credit revenue/tax.
- Cost of goods sold: debit COGS; credit finished goods inventory.
- Raw material purchase: debit raw-material inventory/input tax where applicable; credit cash/payables.
- Material issue to WIP: debit WIP; credit raw-material inventory.
- Direct labor and factory overhead: debit WIP or expense according to approved costing policy; credit payroll payable/cash/overhead clearing.
- Production completion: debit finished goods; credit WIP.
- Spoilage: debit approved spoilage/loss account; credit inventory/WIP, with reason and approval.
- Sales return: reverse the relevant revenue/tax and settlement balances; restore inventory at original COGS, not current average cost.
- Customer collection: debit cash/bank; credit receivable.
- Supplier payment: debit payable; credit cash/bank.
- Bakery/factory transfer: paired due-to/due-from entries and stock/cost movement; eliminate inter-unit balances in consolidated reporting.

### Phase 2 implementation sequence
1. Inventory existing source documents and identify primary keys, transaction boundaries, units, currency precision and duplicate risks.
2. Define account codes and mapping for the user's approved chart of accounts.
3. Add versioned schema migration to both encrypted databases; migrations must be additive and resumable.
4. Implement a pure posting validator and transaction-aware posting service with idempotency.
5. Add deterministic tests for each source event and reversal.
6. Integrate one source family at a time: sales/collections, purchases/payables, stock movements, production/WIP, payroll/expenses, returns, then inter-unit transfers.
7. Build ledger, trial balance and statements from posted journal lines, not UI summary totals.
8. Reconcile all reports against source documents and independent hand calculations before exposing financial statements.

### Explicit non-claims
The current factory ledger's `journal_entries` and `journal_lines` tables and its manual two-line `journal()` helper do not yet prove a complete general ledger. The bakery's current `Store` uses JSON payloads in a records table; those payloads must be parsed and mapped through documented adapters, not guessed or silently transformed. Do not enable automatic accounting postings until event mapping and reconciliation tests pass.

## P1 completion gates
- All source events are posted exactly once.
- Trial balance is balanced and reproducible from journal lines.
- Customer and supplier subledgers reconcile to control accounts.
- Inventory valuation reconciles to stock quantities and movement costs.
- WIP and finished-goods values reconcile by batch and period.
- Income statement, balance sheet and cash-flow statement reconcile to the same ledger.
- Inter-unit transfers do not create duplicate group revenue, cost or inventory.
- Offline retry and app restart do not duplicate a posting.

## 2.8.1 security hardening added after the initial review

- MainActivity and both factory activities now use the shared entitlement decision. The gate migrates the earlier gate-only preference store into the MainActivity preference store without resetting an existing trial or license.
- The expired-trial paywall no longer exposes backup/restore or logo customization. Home navigation returns through the central entitlement check; factory and factory-ledger activities also re-check access on resume.
- Portable bakery+factory backups are now wrapped in an AES-256-GCM authenticated envelope. A user-supplied password (minimum 10 characters) is converted with PBKDF2; the password is not stored. Import rejects wrong passwords and tampering. Legacy plaintext JSON backups remain importable only after a warning.
- Android database encryption continues to use SQLCipher with a random key wrapped by Android Keystore AES-GCM. These controls protect local database files and backup files separately.
- The seven-day offline trial still depends on local device time and app-local state. This reduces casual bypasses and detects clock rollback, but cannot guarantee anti-reinstall or rooted-device enforcement while fully offline. A server-issued, device-bound trial entitlement (or equivalent platform attestation) is required for that stronger guarantee.
- These changes are code-level hardening, not a completed security certification. Emulator tests, API 23/API 35 checks, physical update tests and an independent security review remain mandatory.
