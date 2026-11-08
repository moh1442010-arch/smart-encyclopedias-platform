# Al-Mizan Global Platform Foundation

Status: architecture baseline only. No APK may be built or published from this workstream until the release gates below pass.

## Existing source baseline inspected

- Android package: `com.mohammedmustafa.almizan.bakery`
- Current development branch: `hotfix/activation-dialog-top-button-and-parenthesized-token`
- App version on that branch: `2.8.1`, versionCode `280001`
- Main application database uses SQLCipher through `SecureDatabase`.
- Factory data is stored separately in `almizan_factory_1.db`.
- `EnterpriseSchema` already declares company settings, currencies, exchange rates, branches, users, roles, permissions, tax rules, assets, customers, bank accounts and general-ledger tables.
- Factory module has its own activity, ledger activity, backup, and arithmetic helpers.
- This means a database foundation exists, but it is not yet proof of an integrated accounting system. The two operational databases and their posting flows still require explicit reconciliation and migration design.
- The default `main` branch's bakery build workflow is stale at 2.6.17. Do not use it as the release source. Update workflow/version assertions only after the implementation branch is validated.

## Target architecture

### 1. Operational source documents

Every business event must have a stable source identity and immutable audit metadata:
- sales invoices, counter receipts and distribution invoices
- receipts and settlements (cash, Bankak, receivables)
- purchases and supplier settlements
- material issues and inventory adjustments
- production/work orders and work-in-progress stages
- spoilage, returns and price differences
- payroll and operating expenses
- asset acquisition and depreciation

Required common fields: source type, source ID, business date, created timestamp, branch, currency, exchange-rate snapshot, actor, status, idempotency key, reversal reference and schema version.

### 2. Canonical accounting core

Use one posting service and one canonical general ledger. No screen may calculate or post its own profit formula.

- Revenue = cash + Bankak + customer receivables.
- Include both counter and distribution sales. Preserve channel dimensions for reports; do not deduct distribution from revenue.
- Operating cost includes consumed raw materials, packaging, fuel, labor, shop/factory overhead, spoilage and price differences.
- Profit = total recognized sales revenue - operating cost.
- Receivables contribute to revenue/profit but not to cash receipts.
- Each cost source is posted once. Inventory issues, labor, expenses and spoilage must not be double counted.
- Capital assets are recorded as assets; depreciation is recognized over time. Do not expense an asset purchase automatically as a daily consumable.
- Returns reverse revenue and restore inventory/COGS consistently.
- Every journal must balance and be idempotent. Posted entries are corrected by reversal, not silent mutation.

### 3. Database migration strategy

1. Freeze a copy of both current databases and capture schema versions and record counts.
2. Define a canonical database schema and source-document mapping.
3. Additive migrations only; never drop customer tables or reset balances.
4. Migrate/import source records with deterministic source keys and duplicate detection.
5. Reconcile opening balances, stock quantities/values, customer debts, supplier balances, cash, Bankak and historical sales before enabling the unified posting path.
6. Keep compatibility readers and backup restore for older backups until a migration has passed on copies of real customer data.
7. Enable dual-read/controlled cutover only after parity tests; do not run two independent posting engines indefinitely.

### 4. Navigation and screen contract

Dashboard tiles open independent functional modules:
- Sales & POS
- Distribution
- Production
- Inventory & Purchases
- Customers & Suppliers
- Cash & Bankak
- Payroll
- Operating Expenses
- Fixed Assets
- General Ledger
- Reports
- Settings
- Subscription & License

Every module needs a consistent header, visible Home action, contextual Back action, search/filter, empty/error/loading states, and access control. The dashboard must not merely relabel the old long menu.

### 5. Localization

All user-facing text must be resource-backed in Arabic and English, including dialogs, validation, reports, receipts, print/PDF, backup metadata, licensing and payment screens. Verify RTL and LTR, date/number formats and accessible touch targets. Avoid mixing hard-coded Arabic labels into the English interface.

### 6. Currency and subscriptions

Operational currencies: SDG, USD, EUR, SAR, AED. Store transaction currency and minor-unit amounts with an immutable rate snapshot. Never revalue old entries when exchange rates change.

Commercial proposal (not activated in checkout yet):
| Tier | Monthly | Annual |
|---|---:|---:|
| Starter | USD 9 / EUR 9 / SAR 35 / AED 35 | USD 90 / EUR 90 / SAR 350 / AED 350 |
| Professional | USD 19 / EUR 18 / SAR 69 / AED 69 | USD 190 / EUR 180 / SAR 690 / AED 690 |
| Factory & Enterprise | USD 39 / EUR 36 / SAR 145 / AED 145 | USD 390 / EUR 360 / SAR 1,450 / AED 1,450 |

Keep existing Sudan SDG prices unchanged pending local-market and FX review. Annual pricing is approximately ten monthly payments. These proposed prices must remain inactive until payment currency, tax display, rounding, entitlements, cancellation/expiry and admin review have tests.

Bank transfer details supplied by product owner (must be verified against the bank/account owner before public display):
- Account number: 1019380025245419000
- Local account: 0252 4541 9000 01
- IBAN shown in source image: SD46 0252 4541 9000 01

Manual receipt review remains mandatory. Monthly and annual licenses must expire on schedule. Permanent access is issued only after admin approval. License verification must work offline for an already issued signed token, while online checks are reserved for order submission, renewal, revocation and update discovery.

### 7. Offline-first and security

Core transactions, reporting and data entry work without connectivity. Network access is not a prerequisite for opening or calculating in the ledgers. Use SQLCipher, Android Keystore-protected keys, encrypted portable backups, verified restore, migration rollback and least-privilege permissions. Never log PINs, license secrets, payment credentials or full sensitive receipts.

## Mandatory release gates

- [ ] One shared accounting posting service used by all operational modules.
- [ ] Reconciliation proves counter + distribution revenue = cash + Bankak + receivables, with channel breakdown preserved.
- [ ] Cost and profit reports reconcile without double counting.
- [ ] Migration tests preserve existing bakery and factory records and balances.
- [ ] Backup/export/import tested on pre-migration and post-migration data.
- [ ] Arabic and English complete across all screens and printed outputs.
- [ ] Each dashboard tile opens the correct functional module; Home/Back works from every screen.
- [ ] SDG/USD/EUR/SAR/AED subscription and transaction handling passes rounding and historical-rate tests.
- [ ] License plan entitlements, expiry, offline validation and admin approval tests pass.
- [ ] Unit, integration, migration and API 23+ compatibility tests pass.
- [ ] Release workflow checks correct version, signing certificate and artifact checksum.
- [ ] Only after these gates: signed APK for physical-device acceptance. No APK before then.

## Market benchmark used for initial pricing

- Zoho Books UAE published plans show AED 120/month annual-billed for Premium and AED 280/month annual-billed for Elite.
- Regional Saudi accounting comparisons published in September 2026 show Zoho Standard around SAR 60/month annual-billed, Wafeq Plus SAR 119/month annual-billed, and Qoyod Pro SAR 150/month annual-billed.
- Al-Mizan proposed entry pricing is intentionally below these reference points because it is offline-first and has a focused bakery/factory scope; this is a positioning hypothesis, not evidence of feature parity.

Sources:
- https://www.zoho.com/ae/books/pricing/
- https://sif.xrero.com/en/guides/accounting-software-prices-saudi-arabia/
- https://sif.xrero.com/en/pricing/
