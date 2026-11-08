# Al-Mizan Bakery — Country-Aware Tax Engine Specification

Status: engineering specification; not a statement of legal compliance.
Target: Al-Mizan Bakery Android application, Phase 3 foundation.
Branch: feature/bakery-phase2-ledger-bridge

## 1. Product objective

Provide a configurable tax workspace for multiple countries without hard-coding one country's rules into the accounting engine. The app must remain usable offline. Country selection is explicit; currency is a separate accounting setting and must never be used as the sole country detector.

## 2. Setup sequence

1. User selects country of establishment (ISO 3166-1 alpha-2 code).
2. User selects functional/base currency (ISO 4217 code).
3. App offers the country tax profile that matches the selected country.
4. User confirms tax registration status, tax identifier (optional), filing frequency, and effective date where relevant.
5. App stores the profile locally and records who changed it and when.
6. App warns if country and currency are an unusual pairing, but never silently changes the country or tax rates.

Device locale, SIM country, IP geolocation, and currency may be offered as hints only. They must not silently determine legal jurisdiction.

## 3. Tax profile model

Each profile must be versioned and effective-dated. Minimum fields:

- countryCode, profileId, profileVersion, status (draft/reviewed/active/retired)
- legalSourceName, officialSourceUrl, sourcePublishedAt, verifiedAt, verifiedBy
- effectiveFrom, effectiveTo
- tax type: VAT, GST, sales tax, excise, withholding, or other jurisdiction-specific levy
- rate identifier, percentage or fixed amount, taxable base rule identifier
- inclusive/exclusive pricing behavior
- exemptions and zero-rated categories, with explicit eligibility conditions
- invoice requirements and required seller/buyer fields
- rounding method and precision
- filing period and report mapping
- currency and FX conversion policy where the tax law requires it

No default percentage may be presented as legally verified without an official source and review record. Unknown rules remain disabled and visibly marked "needs review".

## 4. Core accounting behavior

- Calculate tax from a tax code attached to a line/document, not from the display currency.
- Preserve net amount, tax amount, gross amount, tax code, profile version, and calculation timestamp on each posted document.
- Posted documents retain the historical tax calculation even when a profile changes later.
- Credit notes and sales returns reverse the original tax treatment by reference to the original document and profile version.
- Support tax-inclusive and tax-exclusive prices.
- Apply jurisdiction-specific rounding only through a tested strategy.
- Never post tax payable/receivable to the general ledger until the tax mapping and posting integration are configured and validated.
- Reconciliation report must show taxable base, tax by rate/code, adjustments, credit notes, and net tax position.

## 5. Offline and data safety

- Bundle only reviewed tax profiles with provenance and version metadata.
- Cache active profiles locally; all ordinary invoice calculations must work offline.
- Profile updates are additive/versioned migrations. Do not overwrite historical profile versions.
- Export/import backups include country, currency, tax profile versions, and tax calculation snapshots.
- Reject malformed profiles and unknown tax code references; never silently fall back to another country's profile.
- Audit every profile activation, deactivation, rate edit, and mapping change.

## 6. Suggested UI

A. Company setup: country, currency, tax registration status.
B. Tax center: active profile, profile source/version, review status, effective date.
C. Tax codes: rate/type, inclusive/exclusive, exemptions, ledger mapping.
D. Invoice editor: tax code per line, net/tax/gross preview, exemption reason.
E. Tax reports: period summary, tax by code, credit notes, reconciliation, export.
F. Admin audit: profile changes, actor, timestamp, before/after values.

Small-screen requirements: scrollable forms, persistent save/cancel controls, clear validation messages, no essential action below an unreachable fold.

## 7. Country rollout policy

Do not launch all countries with invented or copied rates. Add country profiles in controlled batches:
- Sudan: first priority; verify current tax categories, registration thresholds, invoice rules, filing periods, and official source documents with a local tax professional.
- Egypt and Gulf markets: separate country profiles; never reuse rates or invoice rules across countries.
- Other Arab markets: add only after official-source verification and a jurisdiction-specific test pack.

Every country profile requires two-person review (engineering plus local accounting/tax reviewer) before activation. Where no verified profile exists, allow manual tax-code configuration but mark it as customer-configured and not legally certified.

## 8. Security and permissions

- Only an authorized administrator can activate a country profile or change tax mappings.
- Ordinary operators may select approved tax codes on documents but cannot edit statutory rates.
- Tax identifiers are treated as sensitive business data; do not expose them in logs or analytics.
- Audit records are append-only from the app's normal UI.
- Backup encryption and restore validation apply to tax settings and records.

## 9. Required automated tests

1. Country selection is independent of currency selection.
2. Same currency in different countries yields different profile candidates.
3. Unknown country/profile never defaults to another jurisdiction.
4. Tax-inclusive and tax-exclusive calculations.
5. Zero-rated and exempt cases remain distinct.
6. Decimal precision and rounding boundaries.
7. Returns/credit notes reverse the original tax snapshot.
8. Profile changes do not alter previously posted invoices.
9. Offline invoice creation and report generation.
10. Backup/restore preserves profile version and tax snapshots.
11. Unauthorized role cannot activate or edit statutory profile.
12. Invalid, expired, or overlapping effective-date profiles are rejected.
13. Ledger posting balances and tax control accounts reconcile.
14. Arabic/English labels and right-to-left layout on small screens.

## 10. Release gates

The tax workspace is not production-ready until:
- country setup and tax screens are implemented;
- reviewed profile provenance is visible to the user;
- calculations pass automated and accountant-reviewed test cases;
- posted invoices retain immutable tax snapshots;
- returns, reports, and GL mappings reconcile;
- offline, backup/restore, migration, and update-over-existing-app tests pass;
- at least one local tax reviewer signs off the initial country profile.

## 11. Competitive product quality gates

The tax module is one part of the product. Regional competitiveness also requires:
- complete operational-to-general-ledger posting with source-document traceability;
- inventory valuation and cost-of-goods-sold controls;
- period close/reopen controls and audit trail;
- role-based access with usable initial administrator setup;
- Arabic/English parity and accessible small-screen workflows;
- multi-currency revaluation and FX gain/loss policy;
- tested backup/restore and signed update continuity;
- transparent licensing and offline operation;
- reproducible CI builds, unit/integration tests, and signed-artifact verification.

## 12. Explicit non-goals

This specification does not certify tax compliance, supply legal advice, establish current rates, or authorize automatic filing with any tax authority. Those require current official sources, local professional review, and where applicable an approved government integration.
