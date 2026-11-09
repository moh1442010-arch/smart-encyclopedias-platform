# Al-Mizan P1 Factory-to-GL Bridge and Screen Icon Navigation

Date: 2026-11-09
Branch: feature/almizan-2.9-competitive-localization

## Scope and release rule

This work starts P1 factory-to-general-ledger integration while the screen-specific icon navigation is implemented in parallel. No D01-D90 item is CLOSED until its source commit, reproducible test command, PASS result, and evidence reference are recorded in ALMIZAN_90_REPAIR_REGISTER.json. A successful compile alone is not defect closure. No production database migration, minimum-version enforcement, commercial APK publication, or PR merge is authorized by this plan.

## P1 finding confirmed from source

- Factory operational database: almizan_factory_1.db.
- Bakery enterprise/general-ledger database: almizan_bakery_250.db.
- FactoryLedgerActivity currently writes legacy journal_entries and journal_lines in the factory database.
- GeneralLedger.post writes gl_journal_entries and gl_journal_lines in the bakery database.
- There is no proven bridge that moves factory journals to the enterprise ledger.
- GeneralLedger already validates balanced totals, branch/account/currency/period, stable idempotency keys, and posts atomically within the bakery database. These controls must remain the destination posting boundary.

## Implemented bridge foundation — still under verification

The following code is now present on this branch:

- `FactoryGlOutbox` creates additive source-side tables for explicit account mappings, outbox events, and immutable journal-line snapshots.
- `enqueue` requires an active source transaction, validates debit/credit balance, computes a SHA-256 payload fingerprint, and inserts the event and all source lines with the original journal transaction.
- Repeated enqueue with the same source reference and same payload is idempotent. A changed payload under the same event identity is rejected.
- Account mapping is explicit by exact source account label to target account code, branch, currency, and rational FX rate. No translated display-name inference is used.
- The factory ledger screen now exposes unmapped source labels, queue status, available target accounts/branches/currencies, mapping entry, and a manual retry action.
- Delivery uses `GeneralLedger.post` with a deterministic source identity and idempotency key. The source event becomes POSTED only after the destination call returns successfully.
- PROCESSING events are eligible for recovery after a process interruption. Destination idempotency is the recovery boundary if the destination committed before source acknowledgement.
- Retry uses bounded exponential delay and DEAD_LETTER after eight failed delivery attempts.
- Manual factory journals, WIP journal entries, sales-return journals, and manual reversal journals are captured by the outbox.
- The main factory database now creates source journals in the same transaction for opening raw-material inventory, supplier raw-material purchases, sales revenue and COGS, customer collections, supplier payments, operating expenses, and finished-goods waste/spoilage.
- The factory database schema was advanced additively to version 12 to create outbox tables for existing installations.
- Factory backup schema is now version 10 and includes account mappings, outbox events, and outbox lines. Older schema versions remain accepted without those new tables.
- Test coverage now includes isolated destination-DB integration tests for successful GL delivery, duplicate/crash replay, changed mapped payload rejection, unknown account mapping, and closed-period rejection. These tests are authored but have not yet run in CI.

## Important scope still missing

This is a foundation, not a completed operational integration. The main `FactoryActivity` workflows currently mutate factory stock, production, sales, cashbook, collections, supplier records, expenses, and waste without consistently creating legacy journal entries. Those workflows must be given source documents and accounting events before the outbox can represent the complete factory operation stream.

The following are also still open:

- Production-finalization accounting. The current factory production form records material cost, labor, and overhead together but does not record the payment/accrual source for labor and overhead. Posting an invented balancing account would misstate liabilities or expenses, so this requires a proper WIP/cost-source design before GL integration.
- Destination integration tests have been authored for successful GL posting, duplicate delivery, crash-after-commit recovery, changed-payload conflict, unknown mapping, and closed period; CI execution and fixes are still pending.
- Inactive account/branch/currency and destination database failure tests are still missing.
- Reconciliation report comparing factory source journals, outbox, GL destination entries, and debit/credit totals.
- Safe operator workflow for correcting a frozen mapping after a failed delivery.
- Automated scheduling or a clearly defined synchronization trigger beyond the manual queue action.
- Device verification of mapping and retry screens.
- No migration has been applied to production customer data or Cloudflare D1.

## Required bridge tests

- Source journal and outbox commit atomically.
- Source transaction rollback leaves neither source journal nor outbox event.
- Duplicate enqueue creates one event.
- Changed source payload under the same event identity is rejected.
- Exact payload fingerprint remains stable when source line ordering changes.
- Explicit mapping and target account/branch/currency validation.
- Closed/missing financial period is rejected.
- Duplicate destination delivery creates exactly one GL entry.
- Same idempotency key with changed mapped payload is rejected.
- Destination failure leaves the event retryable.
- Crash after destination commit but before source acknowledgement is safely recovered.
- Bounded retry and DEAD_LETTER behavior.
- Backup/restore preserves outbox events, lines, and mappings.
- Reconciliation reports missing, duplicate, pending, dead-letter, and mismatched totals.

## Screen-specific icon navigation

The legacy vertical cards are being replaced with independent navigation tiles. Each tile must have a distinct icon, translated Arabic/English title, short purpose, and direct route to its own screen. The icon and its open action must be the same clickable target. Existing data and module routes must remain unchanged.

Initial bakery module routes:
- Inventory -> openModule("stock")
- Daily production -> openModule("cuts")
- Workers -> openModule("workers")
- Counter sales -> openModule("window")
- Distribution -> openModule("distribution")
- Expenses -> openModule("expenses")
- Spoilage -> openModule("spoilage")
- Accounting -> showAccounts()
- Costing -> showCosting()
- Customers and suppliers -> PartyDirectoryActivity
- Factory -> FactoryActivity

Acceptance tests:
- Every tile opens its intended screen.
- No tile opens the old generic screen.
- Arabic and English labels remain translated.
- Back-to-home works from each screen.
- Small-screen layout remains scrollable and touch targets remain usable.
- Existing data remains intact after upgrade and navigation changes.

## Current work status

- Initial screen-specific icon navigation tiles have been added to MainActivity; the full sub-screen route audit is not complete.
- FactoryActivity and FactoryLedgerActivity report/print strings were corrected to use real newline characters.
- A CI guard now rejects double-escaped newline sequences in those two source files.
- The API 35 emulator smoke-test step timeout was raised from 8 to 18 minutes after the previous hosted runner timed out while waiting for the emulator to boot. This timeout correction still needs a new CI run.
- New unit and Android instrumentation tests cover deterministic event identity, payload hashing, source/outbox atomicity, rollback, duplicate enqueue, and changed-payload rejection.
- Factory backup/restore has been extended to include the outbox and mappings under schema version 10; its expanded instrumentation test has not yet run.
- No D01-D90 item is CLOSED. The outbox foundation is not equivalent to full operational-to-GL integration.
- No commercial APK is approved. No production Worker policy or minimum supported version has been changed.
- Device acceptance and physical printer tests remain separate release gates.
