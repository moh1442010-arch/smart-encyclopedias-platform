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

## Public price snapshot — 2026-11-08

Prices below are competitor list prices in their own currencies, not direct SDG equivalents. Tax treatment, user limits, included modules, and annual-billing discounts differ.

| Product and market | Public price examples | Relevant tier features |
|---|---|---|
| Zoho Books UAE | Standard AED 60/month annual billing or AED 69 monthly; Professional AED 90/129; Premium AED 120/159; Elite AED 280/349. Prices exclude local taxes. | Professional adds sales/purchase orders, multi-currency, inventory and custom roles. Premium adds fixed assets and cash-flow forecasting. Elite adds warehouses and batch tracking. |
| Wafeq Saudi | Starter SAR 99/month annual billing; Plus SAR 119; Premium SAR 199. Annual totals shown as SAR 1,190, 1,430 and 2,390. | Public page describes inventory, payroll and 40+ financial reports; higher tiers add branches and production-related workflows. |
| Wafeq international | Starter USD 26/month annual billing; Plus USD 32; Premium USD 55. Annual totals shown as USD 312, 384 and 660. | Premium adds inventory, multiple warehouses, assembly/kitting, production orders, payroll and fixed assets; Enterprise adds advanced reconciliation and bank integrations. |

These are competitor benchmarks, not Al-Mizan pricing recommendations. Do not convert them to SDG using a floating rate or copy their prices without a local willingness-to-pay study.

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
- API 23 and API 35 signed-APK smoke tests both passed in review workflow run `42521770523`.
- Full English UI, production licensing acceptance, full accounting integration, and owner device acceptance remain open.

## Country tax profiles — 2026-11-08 research and implementation contract

The first-run setup now stores both `base_currency` and `country_code`; for currencies with one mapped issuing jurisdiction, the country is preselected. A country remains selectable because a currency code is not universally unique (for example USD and EUR are used by multiple jurisdictions). The selected country determines `tax_profile`, not a global tax rate.

The additive schema seeds these verified baseline profiles:
- Sudan: 17% VAT baseline; tax authority confirms the statutory rate. Registration threshold and item-level treatment require a separately verified rule before automatic tax charging.
- UAE: 5% VAT; mandatory AED 375,000 and voluntary AED 187,500 registration thresholds.
- Saudi Arabia: 15% standard VAT; registration, zero-rating and e-invoicing requirements remain distinct controls.
- Egypt: 14% standard VAT; mandatory registration threshold EGP 500,000; statutory exemptions and schedule goods remain distinct.
- Bahrain: 10% VAT; mandatory threshold BHD 37,500.
- Oman: 5% VAT; mandatory OMR 38,500 and voluntary OMR 19,250.
- Kenya: 16% general VAT; zero-rated and exempt categories remain distinct.
- South Africa: 15% VAT; SARS 2026 guidance states compulsory threshold ZAR 2.3 million and voluntary threshold ZAR 120,000.
- Qatar: no VAT currently applied per the General Tax Authority; excise and income tax may still apply.
- Kuwait: no VAT currently; other entity-based taxes may still apply.
- United States: no federal VAT/sales tax; state and local sales-tax rules require location/nexus configuration and are not inferred from USD.

Other currencies are mapped to their issuer country only as a jurisdiction identity; their tax profile is marked `REVIEW_REQUIRED` and no tax rate is seeded. The app must not interpret an empty tax rule set as a zero-tax jurisdiction.

### Calculation safeguards

- Tax rules are effective-dated and stored as integer numerator/denominator values.
- The tax engine returns an explicit status and does not invent a rate for unverified or local-only regimes.
- VAT is calculated only after the business confirms tax registration; inclusive/exclusive calculation uses integer arithmetic and exact rounding.
- A standard rate is not a complete tax code. Reduced rates, zero-rated supplies, exemptions, reverse charge, withholding, excise, local sales tax, registration thresholds, filing periods and e-invoicing need jurisdiction- and item-specific configuration before compliance claims.
- Existing financial records are not rescaled or rewritten when country/currency settings are initialized.

### Primary references

- OECD, Consumption Tax Trends 2024, global VAT/GST landscape and United States subnational sales tax: https://www.oecd.org/en/publications/consumption-tax-trends-2024_dcd4dd36-en/full-report/component-9.html
- Sudan Tax Authority, VAT: https://tax.gov.sd/en/value-added-tax-vat/
- UAE Federal Tax Authority, VAT registration and thresholds: https://www.tax.gov.ae/en/services/vat.registration.aspx
- Saudi ZATCA, VAT law: https://www.zatca.gov.sa/ar/RulesRegulations/Taxes/Pages/VATLaw.aspx
- Egypt Tax Authority, VAT registration threshold: https://www.eta.gov.eg/ar/node/1379
- Bahrain government, VAT rate and threshold: https://www.bahrain.bh/wps/portal/en/BNP/HomeNationalPortal/ContentDetailsPage/
- Oman Tax Authority, registration and thresholds: https://tms.taxoman.gov.om/portal/registration
- Kenya Revenue Authority, VAT rates and input/output treatment: https://www.kra.go.ke/individual/filing-paying/types-of-taxes/value-added-tax
- South African Revenue Service, 2026 tax rates and thresholds: https://www.sars.gov.za/tax-rates/other-taxes/
- Qatar General Tax Authority, investor guide: https://www.gta.gov.qa/en/investors-guide
- Kuwait government tax guide: https://www.gov.uk/government/publications/exporting-to-kuwait/exporting-to-kuwait
- United States sales tax overview: https://taxsummaries.pwc.com/united-states/corporate/other-taxes

This is an initial configurable tax foundation, not a certification that the app is compliant in every country. Country packs must be reviewed against official law before activation for live filing.
