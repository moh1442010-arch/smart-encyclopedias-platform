# Al-Mizan Bakery — Regional Competitive Benchmark (November 2026)

This is a directional product benchmark, not a certification audit. Vendor capabilities, localisations, and prices change. Verify each item directly with the vendor and relevant authority before making a sales claim.

## 1. Competitor landscape

| Product | Competitive strength commonly cited | Risk / gap to validate in demos |
|---|---|---|
| Zoho Books | Broad cloud accounting ecosystem, Arabic support in some editions, multi-currency, tax/localisation integrations | Bakery production, offline-first operation, and country-specific invoice compliance must be tested per market/edition |
| Wafeq | Regional Arabic accounting positioning, Gulf VAT/e-invoicing focus, multi-currency and SME workflows | Product is primarily cloud-oriented; bakery production and deep offline continuity need validation |
| Qoyod | Saudi-first Arabic accounting and retail workflows; local e-invoicing positioning | Geographic focus and industry workflow depth vary; verify bakery production/costing |
| Daftra | Broad Arabic business suite: accounting, inventory, HR/POS and industry modules | Breadth does not guarantee a correct bakery costing and operational-to-GL chain; test the actual edition |
| Odoo | Modular ERP, manufacturing, inventory, multi-company and extensibility | Requires implementation/configuration; tax/e-invoicing localisation and partner quality vary by country |
| TallyPrime | Established desktop accounting and inventory workflows, suited to accountant-led operations | Validate modern mobile workflows, cloud collaboration, and each jurisdiction's e-invoicing requirements |

Sources for initial market scan:
- Staks comparison (Saudi, 2026): https://www.staksai.com/blog/best-accounting-software-saudi-arabia
- Flowyana comparison (UAE/Saudi, 20 Oct 2026; vendor-authored and therefore not neutral): https://flowyana.com/guides/best-accounting-software-uae-saudi-2026/
- WisoBooks UAE comparison (vendor-authored; features/prices dated 7 Oct 2026): https://www.wisobooks.com/guides/best-accounting-software-uae
- Qatar ERP market overview: https://www.qatarerp.com/blog/best-accounting-software-qatar.html

## 2. Competitive gaps Al-Mizan must close

1. Complete traceable posting from production, sales, distribution, cash/bank/credit, returns, stock consumption, wages and expenses into one general ledger.
2. Inventory valuation and cost-of-goods-sold controls with a documented costing method and no double-counting of spoilage.
3. Country-specific tax profiles, invoice rules and reports, with verified official sources and immutable tax snapshots.
4. Offline-first mobile operation with safe queued writes, conflict handling and reliable backup/restore.
5. Arabic-first UX with English parity, RTL, readable small-screen dialogs and bilingual printable documents.
6. Multi-currency accounting with base/transaction currency, exchange-rate source/date, realised/unrealised FX handling and balanced postings.
7. Bakery-specific daily close: production, ingredients, spoilage, sales channels, cash, bank, customer credit, returns and stock reconciliation.
8. Role-based permissions, period close, audit trail and safe admin recovery.
9. Customer-safe update continuity, stable application ID/signing key, migration tests and transparent licensing.
10. Reliable customer support, exportability and clear data ownership.

## 3. Defensible differentiation

Al-Mizan should not try to win by claiming to have more modules than every ERP. It should win in the bakery segment through measurable correctness:
- One daily operating workflow from flour/ingredients to production, dispatch, sales, spoilage and profit.
- A documented, testable bakery cost model.
- Offline operation with transparent sync/backup status.
- Operational source-document IDs on every journal entry.
- Local country tax packs with provenance rather than guessed rates.
- A simple Android workflow for owners and shop staff, with deeper accountant controls.

## 4. Sudan-first tax research

The Sudan Taxation Chamber's VAT page states that VAT applies to local and imported goods, services and works except where a specific provision exempts them. It also identifies producer/importer/service-provider/distribution-related taxpayer categories in its guidance. This is a starting point only; it does not establish the correct treatment of every bakery input, loaf, sale channel or taxpayer.

Official source:
- Sudan Taxation Chamber — VAT (Arabic): https://tax.gov.sd/القيمة-المضافة/
- Sudan Taxation Chamber — VAT (English): https://tax.gov.sd/en/value-added-tax-vat/

Before activating a Sudan bakery profile, a qualified local reviewer must verify:
- whether the specific bakery entity is required to register and collect VAT;
- treatment of bread/loaves and each product category;
- treatment of flour, yeast, oil, packaging, fuel, equipment and imported inputs;
- input-tax recovery and exempt/zero-rated distinctions;
- invoice content, records, return periods, payment deadlines and penalties;
- any state/federal or sector-specific rules and current amendments.

Do not hard-code a universal VAT percentage based only on an online summary. Keep unverified tax codes inactive.

## 5. Release scorecard

| Gate | Evidence required | Status at benchmark date |
|---|---|---|
| Operational-to-GL integration | Integration tests per source document and balanced journal | Open |
| Bakery costing / inventory | Reconciled sample bakery day and stock movement tests | Open |
| Country tax engine | Reviewed country pack, calculation tests, tax reports | Spec drafted; implementation open |
| Offline + restore | Device tests, encrypted backup, restore reconciliation | Partial; device acceptance pending |
| Arabic/English UX | Screen-by-screen RTL and print review | Open |
| Multi-currency | FX policy and journal/report tests | Open |
| Enterprise controls | Roles, periods, audit, branch tests | Partial |
| Update continuity | Same signature, higher versionCode, update-over-old test | CI gate in progress for 2.8.1 |

## 6. Evidence policy

Do not advertise "regional compliant", "tax certified", "fully integrated", or "enterprise complete" until each claim has an auditable test result or formal certification. Distinguish implemented, unit-tested, emulator-tested, device-tested, accountant-reviewed and legally certified states.
