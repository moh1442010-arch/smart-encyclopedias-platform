# Al-Mizan Competitive Product Gap Matrix — November 2026

## Purpose and positioning

Al-Mizan should compete as an **offline-first bakery and food-production operating system with an auditable accounting core**, not as a generic invoicing app. Its differentiator is the daily operational chain:

Raw materials → recipe consumption → production batches → finished stock → sales and returns → waste → cash/bank/credit collection → expenses and payroll → general ledger → trial balance and management reports.

The bakery and factory must remain separate operating ledgers and data stores while sharing company-level accounting controls only through explicit, auditable integration.

Do not claim global superiority until the full workflows, reconciliations, restore tests, security review, and device acceptance are complete.

## Competitor evidence reviewed

| Product | Publicly documented strengths | Implication for Al-Mizan |
|---|---|---|
| Odoo Manufacturing | Manufacturing orders, work orders, multilevel bills of materials, routings, work centers, quality checks, maintenance, traceability, cost analysis, inventory and purchase integration. | Recipe and production records must become controlled manufacturing orders with planned/actual consumption, batch traceability, waste reasons, and cost variance. |
| Zoho Books UAE | Professional tier includes inventory, purchase orders, multi-currency and custom roles. Premium adds fixed assets and cash-flow forecasting. Elite adds warehouse, batch and serial tracking. | Close the full purchasing-to-payables chain, asset/depreciation workflow, bank reconciliation and inventory valuation. |
| Wafeq | Public plans describe accounting, multi-currency, roles, branches, inventory, payroll, fixed assets, production orders, reconciliation, and 40+ financial reports. | Compete on offline operation and bakery-specific production, while closing payroll, branch controls, bank reconciliation, and reporting depth. |
| Qoyod | Public subscription documentation describes tiered accounting plans, multiple users/locations, sales, purchases, payroll add-ons, and operational controls. The cited help page describes 2025 prices, so it is not a current price authority. | Keep plan limits and entitlements explicit; do not copy competitor pricing without local demand and support-cost evidence. |

### Source links
- Odoo Manufacturing features: https://www.odoo.com/app/manufacturing-features
- Odoo Manufacturing documentation: https://www.odoo.com/documentation/18.0/applications/inventory_and_mrp/manufacturing.html
- Zoho Books UAE pricing and feature tiers: https://www.zoho.com/ae/books/pricing/
- Wafeq Saudi pricing: https://www.wafeq.com/en-sa/pricing
- Wafeq international pricing: https://www.wafeq.com/en/pricing
- Qoyod plan documentation (2025 reference): https://www.qoyod.com/en/knowledge-base/qoyod-plans-and-pricing-subscription-tiers-features-add-ons/

## Product gap matrix

### P0 — financial integrity and data safety

- One authoritative posting service for sales, purchases, production, inventory, returns, expenses, payroll, bank movements, and depreciation.
- Balanced journal entries, idempotency, source-document references, reversal entries, and closed-period protection.
- Explicit inventory valuation policy and consistent cost-of-goods-sold calculations.
- Daily cash close that reconciles counter totals, cash, Bankak, bank transfers, credit, returns, and outstanding customer balances.
- Atomic backup restore with pre-restore snapshot, validation before mutation, rollback on failure, and clear restore logs.
- Encryption for both operational databases and exported backup files; test old-version migration and restore on clean and upgraded installations.
- Licensing entitlement enforcement at every protected Activity and every direct-entry route.

### P1 — competitive bakery and factory operations

- Recipe/BOM versioning, planned vs actual material usage, yield variance, batch costing, expiry and lot traceability.
- Work orders with stages, labor time, machine/energy overhead, scrap, quality checks, and partial completion.
- Supplier purchasing, invoice matching, due dates, supplier aging, payment allocation, and purchase returns.
- Customer credit limits, receipts, allocation, aging, statements, and collection history.
- Payroll periods, attendance/advances, deductions, approvals, and ledger posting.
- Fixed assets, acquisition, useful life, depreciation schedules, disposals, and ledger posting.
- Branch-aware roles and permissions with audit history and approval thresholds.
- Reconciliation reports for bank accounts and Bankak references.

### P2 — regional and international readiness

- Arabic and English in every screen, dialog, validation message, guide, printed document, PDF and exported report.
- Country profiles for tax labels, tax rates, invoice numbering, fiscal year, and accounting defaults.
- Separate transaction currency, functional/base currency, and subscription/payment currency. Exchange rates must be dated, sourced, and auditable.
- Offline-first operation with explicit sync state, conflict handling, backup reminders, and no silent data overwrite.
- Subscription entitlements for Basic/Pro/Business/Enterprise, device limits, branch limits, renewal, grace periods, and manual receipt approval.
- Central license issuance with signed tokens, server-side trial registration, anti-clock rollback, device binding, revocation and documented offline grace.

## Pricing and payment policy

- Current bakery storefront prices remain the approved SDG values: 55,000 SDG monthly, 550,000 SDG annually, and 1,050,000 SDG for the current-version permanent license.
- The international IBAN and local Bankak route are shown as payment instructions; payment receipt must still be reviewed and approved by the administrator.
- Do not calculate or publish USD bakery prices by converting SDG at a floating exchange rate. Obtain an explicit approved USD price table for each plan, currency, and term.
- Subscription currency must remain separate from transaction currencies used in customer and supplier accounting.

## Delivery sequence and acceptance criteria

1. **Foundation integration** — PR #120 merged after unit/static/security checks. Factory isolation, backup separation, review-only release workflow, and IBAN display are included.
2. **Device review build** — build, sign, verify certificate fingerprint and SHA-256, run API 23 instrumentation/smoke tests, and inspect API 35 compatibility. Owner performs upgrade, restore, arithmetic, and daily workflow acceptance on a real phone.
3. **Localization and entitlement hardening** — migrate hard-coded UI text to a shared Arabic/English resource layer, remove duplicate license verification, add server-backed trial and direct-Activity denial tests.
4. **Accounting integration** — connect all operational sources to the general ledger and add reconciliations, financial statements, and audit trails.
5. **Commercial acceptance** — production secret validation, controlled live license activation, support/restore runbook, approved SDG/USD pricing, and a separately approved commercial release.

## Current status after PR #120

- Merged commit: `81b8dbf536f6174269c8e4c3382a837349501e38`.
- Unit, static-site, and security checks passed on the final PR line.
- Signed 2.8.1 device-review APK was built and the certificate verification step passed.
- API 23 instrumentation/smoke test was still running at the time of this document update.
- Full English UI, production licensing acceptance, full accounting integration, and owner device acceptance remain open.
