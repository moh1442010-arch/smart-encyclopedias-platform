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

Each accounting system has its own posting service, database, general ledger and reports. A stateless calculation helper may be reused as source code, but it must receive only that system's records. No shared ledger, cross-system posting, or consolidation is permitted.

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

1. Treat the bakery and factory as permanently separate accounting systems, not two sources to merge into a canonical shared database.
2. Freeze and inspect each database independently; capture its schema version, record counts and balances.
3. Apply additive migrations within that database only; never drop tables or reset balances.
4. Give each system its own party master, posting service, ledger and reports. No foreign-system IDs or picker calls.
5. Reconcile opening balances, stock, debts, cash, Bankak and historical sales separately within each system.
6. Maintain separate encrypted backup/restore flows. A legacy combined archive may be imported only through an explicit system-specific selection and must never couple the resulting databases.
7. Test each migration on copied data before enabling it for customers.

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

Approved commercial price baseline (owner approval recorded 2026-11-08; implementation target, not yet live):
| Term | USD | EUR | SAR | AED |
|---|---:|---:|---:|---:|
| Monthly | 12 | 11 | 45 | 45 |
| Annual | 120 | 110 | 450 | 450 |
| One-time / perpetual | 299 | 279 | 1,125 | 1,125 |

License rules:
- Monthly term is 30 days; annual term is 365 days. Both expire at the end of the paid term.
- Annual pricing is approximately ten monthly payments.
- One-time license is perpetual for one licensed business and one registered primary device. It includes the purchased major version and 12 months of updates/support. Continued use of that purchased version remains valid; future major-version upgrades and extended support may be separately priced.
- Keep existing Sudan SDG prices on a separate schedule. Do not calculate them automatically from foreign currency prices.
- Do not enable checkout until currency display, rounding, tax disclosure, receipt review, entitlement enforcement, expiry, signed license issuance and renewal tests pass.

Payment destination details must be configured only in a private, access-controlled admin environment after independent verification. Do not commit bank account numbers, IBANs, receipts, or other payment credentials to this public repository.

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

## Implementation progress — 2026-11-08

### Completed in the foundation branch
- Added `UnifiedAccountingMath`, a shared pure calculation contract for counter and distribution revenue, cash/Bankak/receivables, sales returns, operating cost, duplicate cost-source rejection, and capital-asset exclusion.
- Added unit tests for channel totals, receivables-vs-cash separation, returns, cost aggregation, duplicate source prevention and capital asset treatment.
- Routed the bakery dashboard's existing `salesLessOperatingCost` helper through the shared calculation contract.
- Recorded the owner-approved four-currency subscription price list and license-term rules above.

### Still open — do not treat as complete
- Factory reports still calculate results directly from factory SQL tables; they have not yet been migrated to the shared calculation service.
- Bakery and factory databases must remain separate by design. Source review found factory screens still use the bakery PartyDirectory and combined backup/restore; issue #115 tracks removal of those cross-system dependencies. No cross-system data migration is authorized.
- Each system must have its own GeneralLedger instance. Operational sales, production, inventory, payroll, expense and distribution documents are not all posting through their own system ledger yet.
- Independent icon dashboard, full English resources, complete screen localization, all navigation tests, and live four-currency licensing checkout remain incomplete.
- IBAN/account ownership and format must be verified before the payment details are publicly enabled.
- No customer-facing APK or commercial release is approved from this workstream.

### CI guardrail observation
The current bakery workflow is triggered by changes under `bakery-app/**`. The first automatic run stopped at its API 23 compatibility static check because the new shared class used `Math.*Exact` methods. Those calls were replaced with local checked-arithmetic helpers. The subsequent workflow run is being used only to verify the source/test gate; any generated APK artifact is not to be distributed or treated as a release.

### Factory warehouse input remediation — 2026-11-08
- Replaced narrow horizontal-scrolling input rows with full-width vertical input controls across the factory forms; increased field height/text size and enabled focus/touch editing.
- Configured the factory window to resize for the Android keyboard, and numeric fields now request numeric keyboards.
- Clarified that the stock table is a read-only balance summary and that entry happens in the forms below it.
- Added a new-material flow with a custom free-text unit, opening quantity and unit cost. Opening stock is stored as a movement and audit event; duplicate names are rejected so the opening balance cannot be added twice.
- Added a product sale-unit column with a version-10 database migration defaulting existing products to "قطعة". Product screens now allow a free-text sale unit (piece, kg, tray, box, etc.) and show it beside finished stock. Production price updates preserve the saved unit.
- Expanded the default factory materials catalogue for bakery and confectionery inputs (butter, cream, cheese, chocolate, cocoa, vanilla, baking powder, dates, sesame, coconut, honey/syrup, nuts, spices, packaging, gas and fuel) and the starter products catalogue (cake, biscuit, maamoul, kunafa, doughnuts, samosa, bread and rolls).
- Existing stock valuation is preserved; changing an item's unit while stock exists remains blocked. Incoming stock should use a supplier receipt with the same unit. Units are free-text but quantities are stored to 3 decimal places; automatic conversion between kg/g or L/ml is not yet implemented.
- The no-APK unit-test workflow run `42508545860` passed on commit `1ea7bc716c409689c9942cecd10e4d58a6cc00aa`; the factory/bakery unit-test job and API 23 arithmetic compatibility check passed. This does not prove physical-device typing, database upgrade against real customer data, or backup/restore isolation. Those remain release gates.


### Strict bakery/factory isolation requirement — 2026-11-08
- Owner instruction: the bakery account and factory account are independent systems, each with its own database, customer/supplier records, ledger, balances, reports and backup/restore. Do not connect, merge, consolidate or automatically post between them.
- A pure stateless math helper may be shared as code only. Runtime records, database handles, identifiers and journal entries must never cross the system boundary.
- Source review found the current factory screens still invoke the bakery `PartyDirectory` / `PartyDirectoryActivity`, and factory rows persist IDs selected from that bakery directory. The current top-level encrypted backup also bundles bakery records, bakery parties and factory data into one restore transaction. These are confirmed architecture violations, not accepted behavior.
- Issue #115 tracks the corrective work: factory-owned party masters inside `almizan_factory_1.db`, migration of existing factory names/IDs, removal of bakery directory calls from factory, and independent encrypted backups/restores. Factory backup export also needs schema parity for the product sale-unit field added in database version 10.
- Until issue #115 passes its migration, isolation and backup tests, the factory and bakery are not considered fully independent even though they already use separate SQLCipher database files.
- No APK build or release is authorized before all infrastructure release gates are met.


### Complete Arabic/English localization audit — 2026-11-08
- Owner approved full English integration across every application screen and the offline guide.
- Audit found extensive hard-coded Arabic user-facing strings in the programmatic Android screens, including MainActivity, FactoryActivity, FactoryLedgerActivity and PartyDirectoryActivity. No Android `values/strings.xml` or `values-en/strings.xml` resources were found in the inspected development source.
- The offline HTML guide is Arabic-only (`lang="ar"`, `dir="rtl"`). English guide content, a language switch, and LTR rendering have not been implemented.
- Localization must include field labels/hints, dialogs, validation/errors, toasts, menus, reports, receipts, printing, subscription/licensing, backup metadata, and support messages. Add tests for persistence, RTL/LTR, translation coverage and core flows in both languages.
- Tracked in issue #117. Do not describe English integration as complete until every screen and the guide pass coverage tests and device checks.
- Language switching must not change or share bakery/factory databases, parties, ledgers, backups or accounting calculations.


## Owner approvals and competitive gap analysis — 2026-11-08

### Identity and credentials
- The bakery app first-run setup currently includes an establishment-name field, a numeric PIN (minimum six digits), and confirmation. It derives a salted hash; it does not intentionally persist the PIN as plaintext.
- The factory must have its own business name, independent from the bakery. FactoryActivity was reading bakery_profile; source commits 38ecdfae6135b1e69be5972e10744ce23fcba9b0 and 39df469e150304eeee4bc486ac9bed223d2f64f6 change it to factory_profile, add a factory-name edit action, and copy the legacy display name once on first access if no factory name exists. After that one-time migration, edits remain independent.
- This change is source-only. CI, APK build, and phone verification have not yet been confirmed.
- The app-level PIN flow needs a security review for show/hide control, rate limiting/lockout, change/reset/recovery, and protection of sensitive screens. Do not create a second independent PIN until access-control architecture is designed and tested.

### International product gap analysis
Benchmark references reviewed on 2026-11-08:
- Odoo manufacturing documentation covers work orders, multi-level bills of materials, routing, production scheduling, quality checks, maintenance, lot traceability, and cost analysis.
- Bakery-specific systems advertise recipe-level ingredient tracking, batch status, expiry/reorder alerts, waste tracking, order/return workflows, and product profitability.
- Sage accounting references include P&L, balance sheet, trial balance, aged receivables/payables, cash flow, audit trail, bank reconciliation, tax reports, and multi-currency capabilities.

Required completion matrix:
1. Accounting integrity: chart of accounts; journal entries; general ledger; trial balance; P&L; balance sheet; cash flow; AR/AP aging; period close/reopen; reversals; audit trail; bank/cash reconciliation.
2. Bakery/factory operations: recipe/BOM versioning; batch/lot traceability; expiry dates; production planning/work orders; quality checks; wastage/returns; reorder alerts; safe unit conversions; product-level cost and margin.
3. Commercial: quotations/orders; delivery notes; purchase orders; customer/supplier statements; partial payments; refunds/credit notes; discounts; multi-outlet/channel reporting.
4. Security/resilience: role permissions; PIN recovery/lockout; verified encrypted database and backup; restore drills; migration rollback; tamper-evident audit records.
5. Regionalization: complete Arabic/English UI, guide, receipts and printouts; RTL/LTR; configurable country/tax rules; currency minor units and dated exchange rates; locale-aware dates/numbers.
6. Platform quality: offline workflows; low-end Android compatibility; accessible touch targets; import/export; diagnostics; automated regression tests.

Each capability must be implemented and evidenced by tests, or explicitly declared out of scope. Do not describe the product as internationally complete until this matrix is closed with reproducible tests and device acceptance.
