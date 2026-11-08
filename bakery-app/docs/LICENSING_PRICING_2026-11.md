# Al-Mizan Bakery — licensing price decision (2026-11-08)

Status: owner-approved pricing and payment details are being implemented on the review branch. Production activation remains blocked until migration, deployment and end-to-end payment tests pass.

## Market references reviewed

- Loyverse publishes a free POS core and paid add-ons at USD 5–25 per store/month: https://loyverse.com/en-us/pricing
- Zoho Books publishes USD 20/month Standard (USD 15/month annual billing), USD 50/month Professional (USD 40 annual), and higher tiers: https://www.zoho.com/us/books/pricing/
- POS Bakery lists a bakery-focused operating platform at USD 36.58/month for its entry plan, USD 74.17/month for its mid-tier, and USD 149.17/month for its higher tier: https://posbakery.com/pricing/
- Bakery Dashboard lists USD 89.99/month or USD 917.99/year for ingredient costing, production, deliveries, expiry and P&L: https://www.bakerydashboard.com/

These are vendor-published comparison points, not proof that every feature or support level is equivalent. Al-Mizan's current candidate is an offline-first Android bakery/factory application; pricing must reflect its present supported scope and must not imply parity with mature cloud platforms.

## Recommended single-product prices

| Currency | Monthly (30 days) | Annual (365 days) | Lifetime |
|---|---:|---:|---:|
| SAR | 29 | 290 | 725 |
| AED | 29 | 290 | 725 |
| USD | 8 | 80 | 199 |

- Annual pricing is equivalent to ten monthly payments.
- Lifetime is a one-time, single-device entitlement. Intended policy: access to compatible future updates for the supported product line. The signing token and Worker must be changed and tested to enforce this policy before sales open.
- Prices are per bakery installation/device. Multi-branch, multi-device synchronization and enterprise hosting are not included unless separately implemented and explicitly described.
- Taxes, bank charges, and payment-provider fees are not included unless local law or final checkout requires them.

## Why this level

The monthly entry point is intentionally below the specialist bakery SaaS references and below mid-tier accounting subscriptions. This reflects the current Android/offline distribution model and the need to build adoption in Sudan and the wider Arabic-speaking market. It is not a claim that all accounting, tax, payroll, or multi-branch capabilities are complete.

## Currency and migration rules

- Licensing checkout supports SAR, AED, and USD only.
- EUR is removed from licensing UI, order validation, and payment administration.
- Operational bookkeeping currency remains a separate company setting. Removing EUR from licensing must not silently convert or rewrite existing company ledgers.
- Every order must persist an explicit ISO currency code and integer amount; no currency conversion or inferred currency is allowed.
- The Worker, storefront, app, admin panel, database migration, tests, and issued token metadata must all agree on the same currency and price.

## Banking data gate

Owner-provided Fawry details approved for display: account number `1019380025245419000`, basic account `0252 4541 9000 01`, IBAN `SD46 0252 4541 9000 01`, beneficiary `محمد مصطفى بابكر`. The IBAN passed the ISO 13616 MOD-97 checksum. The screenshot does not show a SWIFT/BIC or receiving-bank name; do not invent either. Customers must verify the beneficiary name in their banking app before confirming an international transfer.
- Local Bankak: account `1882224`, beneficiary `محمد مصطفى بابكر`, as explicitly approved by the owner. Manual payment review remains in place.
- The owner approved SAR 29/290/725, AED 29/290/725, USD 8/80/199 for monthly/annual/lifetime. EUR is excluded from licensing. SDG prices remain 55,000/550,000/1,050,000.

## Release blockers before charging customers

1. Implement the three-currency price matrix end-to-end in Android, storefront, Worker, D1 schema/migration, admin review and tests.
2. Keep old SDG order records readable; migrate them as SDG rather than relabeling them as SAR/AED/USD.
3. Implement server-recorded, device-bound seven-day trials with signed offline-verifiable trial entitlements and replay/reinstall controls.
4. Update lifetime token policy for compatible future updates and test old-token compatibility.
5. Verify the live Cloudflare Worker, D1 migration, signing pair, admin approval path and real bank accounts.
6. Complete full tax-to-document/ledger posting, bilingual audit, backup/restore, API 23/35 tests and physical upgrade acceptance.

No commercial release or payment collection is authorized until these gates pass.