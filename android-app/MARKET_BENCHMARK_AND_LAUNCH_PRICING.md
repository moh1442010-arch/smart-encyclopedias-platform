# Market benchmark and launch pricing — Smart Accountant
Last checked: 2026-10-13

## Purpose and decision rule
The product strategy is value-for-money launch pricing, not an unsupported claim that this app is already the best or the cheapest. Prices below are competitor observations, not direct like-for-like comparisons: competitors may be cloud-first and include features (multi-user, integrations, automated backups, payroll, tax compliance, or support) that this Android app does not yet deliver. Do not use these observations as proof of feature parity.

## Public competitor benchmarks reviewed
| Market / provider | Publicly displayed price | What the page says | Official source |
|---|---:|---|---|
| Gulf / Zoho Books UAE | Standard AED 69 monthly / AED 60 monthly billed annually; Professional AED 129 / AED 90 annually billed monthly | Cloud accounting; Professional includes sales/purchase orders, inventory, multi-currency, roles and workflows | https://www.zoho.com/ae/books/pricing/ |
| Global / Daftra | Basic USD 20/month; Advanced USD 35/month; Comprehensive USD 50/month; page advertises up to 30% annual discount and a 14-day trial | Cloud ERP/accounting suite; price and feature scope vary by plan/market | https://www.daftara.me/plans |
| Iraq / Iraq Accounting Cloud | IQD 25,000/month starter; IQD 50,000/month medium; IQD 100,000/month large | Page advertises general accounting at starter tier; inventory, POS, payroll and up to five users in medium; manufacturing/assets/unlimited users and branches in large | https://www.iraq-accounting.cloud/ |
| Global / Zoho Books US | Standard USD 20 monthly / USD 15 annually; Professional USD 50 / USD 40 annually | Public US pricing differs materially from regional pricing, demonstrating that regional prices should not be converted mechanically | https://www.zoho.com/us/books/ |

### Limits of this comparison
- Public prices can change and may exclude tax, onboarding, payment processing, or add-ons.
- The listed providers are not necessarily offline-first or comparable in data architecture.
- A local Iraqi provider's claims and prices are recorded as advertised on its website; they have not been independently audited.
- Public evidence found in this pass was insufficient to establish a reliable current Egypt-specific EGP price benchmark. Do not invent one. Obtain direct Egypt pricing from vendors before finalizing EGP prices.
- Sudan's currency and purchasing power are volatile; do not hard-code an exchange-rate conversion from SAR, USD, or IQD.

## Launch-pricing recommendation (not yet final)
1. Use a low-friction founder launch price only after the core workflows and data safety pass release QA. Avoid making the app permanently cheap before costs and support load are known.
2. Keep a free 7-day trial as a product goal, but do not claim secure enforcement until a server-side licensing service is implemented and tested. Current local-only trial controls can be reset or bypassed.
3. Keep the three commercial models distinct:
   - Permanent license: a clearly defined version/usage right; specify whether future major features are excluded, while defining security/compatibility fixes separately.
   - Monthly subscription: active features, updates and support only for the paid period.
   - Annual subscription: same defined scope for a year, with a transparent discount versus 12 monthly payments.
4. Do not list multi-branch sync, cloud backup, multi-user access, enterprise integrations, or advanced support as included until they exist and pass tests.
5. Collect verified customer feedback only after real use; no fabricated testimonials, star ratings, or customer counts.

## Required next steps before publishing prices
- Obtain current official price quotes for Egypt and any target countries not covered above.
- Run a feature-by-feature matrix against competitors: accounting core, inventory, customer/supplier balances, returns, payroll, checks, printing, backup/restore, encryption, offline behavior, language support, devices/users, support, and licensing.
- Test the app with representative businesses and measure willingness to pay, support cost, conversion, refunds, and renewal.
- Implement server-side licenses, entitlement checks, payment/receipt flow, cancellation/renewal, limited offline grace period, and a data-export path after expiry.
- Complete accounting regression tests and database/backup security work before commercial claims.

## Source pages
- Zoho Books UAE pricing: https://www.zoho.com/ae/books/pricing/
- Daftra plans: https://www.daftara.me/plans
- Iraq Accounting Cloud pricing: https://www.iraq-accounting.cloud/
- Zoho Books US pricing: https://www.zoho.com/us/books/
