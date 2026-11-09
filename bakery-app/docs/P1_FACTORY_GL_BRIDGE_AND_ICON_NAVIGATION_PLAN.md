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

## Required bridge design before implementation

1. Add a versioned, additive outbox table to the factory database. Never rewrite or delete existing factory journal rows.
2. Insert the outbox event in the same factory SQLCipher transaction as the source journal. Use a stable source identity, immutable serialized posting payload, creation timestamp, attempt count, next-attempt time, and states PENDING / PROCESSING / POSTED / RETRY / DEAD_LETTER.
3. Create an explicit, reviewed account mapping table. Do not infer account codes from translated Arabic display names. Unmapped accounts must be held for review, never silently mapped to a generic account.
4. Resolve active branch and base currency from the bakery enterprise database. Validate source date against an OPEN financial period.
5. Post through GeneralLedger.post using a deterministic idempotency key derived from source database + source journal reference. Preserve transaction currency and FX evidence. Validate debit equals credit after base-currency conversion.
6. Mark the factory outbox row POSTED only after destination commit succeeds. If the app stops between destination commit and source acknowledgement, retry must return the same destination entry through idempotency.
7. Use bounded exponential backoff and a visible retry/dead-letter report. Do not mark a source row posted on any exception.
8. Add a reconciliation report comparing eligible source journals, posted destination journals, totals, unmapped entries, and pending/dead-letter rows.

## Required automated tests

- Balanced and unbalanced source journal payloads.
- Every supported factory account mapping; unknown mapping is rejected.
- Closed/missing financial period is rejected.
- Duplicate delivery creates exactly one GL entry.
- Same idempotency key with changed payload is rejected.
- Destination failure leaves outbox retryable.
- Crash after destination commit but before source acknowledgement is safely recovered.
- Factory transaction rollback leaves neither journal nor outbox row.
- Debit/credit totals match in transaction and base currency.
- Reconciliation reports missing, duplicate, pending, and mismatched totals.

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

- Screen-specific icon navigation tile implementation has been started in MainActivity.
- Factory ledger print header was corrected to use the factory identity and real newline characters.
- The factory-to-GL outbox is not implemented yet.
- No defect is closed by this plan.
- Device acceptance and printer hardware tests remain separate release gates.
