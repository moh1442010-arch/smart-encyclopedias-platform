# Al-Mizan Accountant — pricing research and recommendation

**Review date:** 2026-10-16  
**Status:** Internal proposal for owner approval; not an authorization to change public prices or begin sales.

## Executive recommendation

Keep the current storefront prices marked **provisional** until the production release gates pass. For the first commercially supportable release in Sudan, test the following subscription prices after feature scope, user/device limits, support terms, and licensing have been verified:

| Plan | Monthly proposal | Annual proposal | Intended segment |
|---|---:|---:|---|
| Basic | 25,000 SDG | 250,000 SDG | One small business / owner-operator |
| Pro | 45,000 SDG | 450,000 SDG | Growing business with more reporting and day-to-day transactions |
| Business | 80,000 SDG | 800,000 SDG | Larger operation needing higher limits and priority support |
| Enterprise | Quote after discovery | Quote after discovery | Multi-branch, deployment, training, migration, or support commitments |

Annual prices equal ten monthly payments, giving approximately two months' discount. The figures are proposed price points, not verified willingness-to-pay data. In Sudan, inflation, exchange-rate movement, connectivity, support costs, and customer purchasing power can change quickly; review local prices monthly and maintain an internal USD cost benchmark without silently changing a customer's active term.

### Perpetual licensing

Do **not** advertise a lifetime/perpetual license as an unlimited promise while server licensing, update delivery, backup/restore, and support obligations are still being validated. If perpetual licensing is offered later, define it narrowly: permanent right to use the specified major version, with 12 months of updates and standard support included; future major-version upgrades and ongoing support are separately priced. A provisional future range for that model is Basic 400,000 SDG, Pro 750,000 SDG, Business 1,300,000 SDG, subject to a cost and support review. Do not publish these figures until the owner approves the terms.

## Public competitor snapshot

The following are published list prices visible during this review. Feature scope, billing unit, taxes, promotional discounts, and supported markets differ, so these are reference points rather than direct like-for-like equivalence.

- **Kashiro Pro (Sudan):** its public pricing page lists a free tier, 40,000 SDG monthly, 400,000 SDG yearly, and a USD 300 yearly gold tier. It advertises local support and a POS-focused product. Source: https://pro.kashiroai.com/pricing
- **Kulnabooks (Sudan):** its public pricing page lists 90,000 SDG monthly for Basic, 160,000 SDG monthly for Business, and 280,000 SDG monthly for Professional; annual prices shown are 900,000 / 1,600,000 / 2,800,000 SDG. It advertises POS, inventory, purchasing, invoices, accounting, VAT, reports, Arabic/English, roles, and different branch/user/register limits. Source: https://landing.pluxora.com/en/pricing
- **Zoho Books (US public pricing):** Standard is listed at 20 USD monthly or 15 USD per organization/month billed annually; Professional is 50 USD monthly or 40 USD billed annually, with inventory and purchasing among the higher-tier capabilities. Taxes are additional. Source: https://www.zoho.com/us/books/
- **QuickBooks Online (US public pricing):** the public page lists Simple Start at 38 USD/month list price, with higher tiers above that; promotional pricing can differ. Source: https://quickbooks.intuit.com/pricing/
- **Xero (US public pricing):** publishes tiered USD pricing and notes upcoming price changes from October 1, 2026. Source: https://www.xero.com/us/pricing-plans/

## Interpretation

1. The existing Al-Mizan prices of 15,000 / 30,000 / 60,000 SDG monthly are below the visible 40,000 SDG monthly local competitor reference and substantially below the 90,000 SDG entry price listed by Kulnabooks. That can help early adoption, but may underfund support, testing, security maintenance, and future updates.
2. The proposed 25,000 / 45,000 / 80,000 SDG monthly structure positions Basic below the first paid local reference, Pro around the lower local paid range, and Business below the published multi-user/branch offerings. It must not imply equivalent feature depth until parity is demonstrated.
3. Global cloud-accounting list prices around 15–40 USD/month for common paid tiers support the view that reliable accounting software has recurring value, but those prices are not directly transferable to Sudan and often include mature cloud infrastructure, integrations, support, and compliance features.
4. Do not compete on price alone. Publish exact limits (users, devices, branches, backups, support response, updates, offline behavior, and data export) per plan. A feature not implemented and tested must not be included in a plan promise.

## Conditions before changing the public price table

- Complete install/update testing on a real Android device using the actual release signature; verify data survives update.
- Pass accountant-reviewed acceptance scenarios for sales, purchases, stock valuation, returns, partial payments, customer/supplier balances, journal, ledger, trial balance, profit and loss, and balance sheet.
- Verify the live licensing Worker, admin authorization, device binding, renewal, expiry, revocation, and offline behavior with test records.
- Verify encrypted backup/restore, export, privacy/terms, and support channel.
- Build and test the Windows version separately; do not sell Windows as available until a real executable has passed installation, update, licensing, localization, backup/restore, and accounting checks.
- Publish real direct-download links and SHA-256 checksums only for approved artifacts.

## Decision requested from product owner

Choose one direction before the public prices are changed:

- **A — Recommended standard launch:** 25k / 45k / 80k SDG monthly; 250k / 450k / 800k SDG annually; Enterprise custom.
- **B — Lower-priced limited pilot:** 20k / 35k / 60k SDG monthly; 200k / 350k / 600k SDG annually, limited to a small number of pilot customers with explicit pre-commercial terms.
- **C — Keep existing pricing temporarily:** 15k / 30k / 60k SDG monthly and 150k / 300k / 600k annually, but label as provisional and review after validated pilot results.

No option should be represented as approved until the owner explicitly selects it. The app must not be sold as accounting-certified or production-ready while the release gates remain open.
