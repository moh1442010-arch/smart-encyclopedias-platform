# Smart Company Accounting App — Release Readiness

Current target release: **2.2.0 for Android and Windows**. The 2.2.0 Android candidate adds an icon-grid dashboard, a shared home-navigation button, and a runtime balance check for transaction journal entries. CI compilation and real-device/accounting acceptance tests remain separate gates.

## Implemented in the current source tree

- Arabic-first interface with an Arabic/English toggle.
- Local PIN protection; new PINs are stored as salted PBKDF2 hashes, with migration from the older plain-PIN preference.
- Product inventory, manual or keyboard-style USB/Bluetooth barcode input, sales and purchases, customers and suppliers.
- Cash expenses, bank accounts and movements, cheque register, employee records, monthly salary/advance/deduction calculations.
- Financial summaries, trial balance, report printing/PDF through Android PrintManager, and sharing a report through an email app.
- Icon-card dashboard and home-navigation action in the shared page template.
- Runtime check that rejects transaction journal entries whose debit and credit totals differ by more than 0.005.
- Database export and restore flow with SQLite integrity/table checks.
- Windows desktop companion includes product inventory, sales/purchases, customers/suppliers, employee monthly salary/advance/deduction summaries, cash-expense tracking, report printing/email sharing, and SQLite backup/restore validation.

## Important release status

A successful CI build proves compilation and packaging only. It does **not** prove the app has been installed and exercised on a real phone or that accounting results are correct for every business scenario.

The non-activatable seven-day Android lock was removed from this pre-release build so it cannot permanently lock the owner out before a real licensing service exists. **This is not a completed commercial licensing system.** Before paid public distribution, implement and test server-verified license issuance, renewal/revocation, offline grace policy, privacy terms, and customer support workflow. Do not ship a production signing key in the repository.

## Release gates and required acceptance tests

**Phase 3 — reliability and data protection:** test PIN behavior, invalid/duplicate values, over-selling, transaction rollback on failed journal balance, and backup/restore with a known data snapshot. Keep user data local unless a clearly described service requires otherwise.

**Phase 4 — commercial readiness:** create and securely retain the owner's release signing key; sign and verify the production APK; define a license service before advertising paid activation; add privacy terms, EULA, support and update policy; and complete accountant-led reconciliation cases. A successful build is not a certification.

## Required device acceptance tests before selling

1. Install on the target Huawei/Android device; launch, close, and relaunch.
2. Create a PIN, restart the app, verify correct and incorrect PIN handling, and switch languages.
3. Add a product; sell a partial quantity; confirm stock decreases; attempt an over-sale and confirm it is rejected.
4. Purchase stock; confirm quantity increases and the unit-cost behavior is acceptable for the intended accounting policy.
5. Enter a cash expense and a bank deposit/withdrawal; verify reports and trial-balance totals.
6. Add an employee, record a same-month advance and deduction, and verify the displayed net payable.
7. Export a backup, make new test transactions, restore the backup, and verify that the restored data is exactly the expected snapshot.
8. Print/save a PDF report and share the report using an available email app.
9. Test empty, malformed, negative, very large, and duplicate inputs.
10. Verify the app name/icon, Arabic layout, and behavior on the minimum supported Android version.

## Known limitations to address before a full commercial claim

- No real server-side licensing/activation is currently integrated on either platform. Trial locks and the hard-coded Windows activation code were removed from the pre-release candidate to avoid lockouts and insecure shared codes. Do not advertise paid activation until a proper license service exists.
- Camera-based barcode scanning is not implemented; keyboard-emulating USB/Bluetooth scanners or manual entry are the intended path.
- Payroll calculations are displayed on both platforms, but payroll events are not yet posted to the general ledger as a formal salary-accrual/payment workflow. The Windows edition is a basic inventory/transaction companion and does not yet match Android's bank-account and cheque modules.
- The current product-cost policy and all journal/report totals require accountant-led test cases before being relied on for statutory books.
- The latest APK must be installed and manually tested. A debug APK is for testing; a production APK must be signed with the owner's securely stored release key.

## Release artifact naming

GitHub Actions uploads Android outputs as `smart-company-v2.2.0` and Windows output as `smart-company-windows-v2.2.0`. Check the newest run conclusion and artifact contents before sharing any package. Use the debug APK for the first controlled Android device test; do not distribute an unsigned release APK as a finished commercial product. Windows x64 publishing is self-contained, but direct public distribution should eventually use a trusted code-signing certificate or a managed store distribution route.


## Independent market and product-gap review — 2026-10-09

This is a product-readiness benchmark, not a claim that the current app equals or exceeds established products.

| Capability seen in current market offerings | Current candidate status | Release decision |
|---|---|---|
| Arabic-first, simple mobile workflow | Present in basic form | Validate readability on target devices |
| Offline/local data use | Local SQLite data | Keep backup and recovery testing mandatory |
| Multi-line invoices, quotes, credit notes and returns | Not complete; current invoice flow handles one product line | P0 before positioning as a complete business-accounting system |
| Customer and supplier statements and aging of receivables/payables | Basic party names and invoice balances only | P0 |
| Payment methods and settlement allocation to cash/bank/accounts | Invoice settlement currently posts paid amounts to generic cash | P0; expose payment method and post to the correct account |
| Full financial statements (income statement, balance sheet, cash-flow statement) | Reports and trial-balance totals are basic | P0; accountant-reviewed reconciliation required |
| Role-based staff permissions and audit trail for edits/deletions | Not implemented | P0 before multi-user/company deployment |
| Multi-warehouse/branch, multi-currency, POS | Not implemented | P1/P2 according to target customer |
| Receipt OCR, camera barcode scanning, automated bank feeds/reconciliation | Not implemented | P2; do not advertise as available |
| Secure licensing, update policy, privacy terms and support process | No server-verified licensing; release signing key not configured | Blocking for paid public distribution |
| Windows feature parity | Incomplete relative to Android | Do not market as a fully equivalent cross-platform suite |

### Competitive lessons to adopt

Established products advertise invoice and payment automation, receipt capture/OCR, bank reconciliation, inventory alerts, activity/audit trails, role-based access, multi-device synchronization, and wider reporting/integration ecosystems. Arabic-market products also compete on branches, warehouses, POS, payroll, multi-currency and local workflows. The current candidate's credible near-term positioning is narrower: **Arabic-first, locally stored starter accounting for controlled pilot use**, with transparent limits—not “the most complete” or “better than all competitors.”

Reference benchmark pages:
- Zoho Books feature overview: https://www.zoho.com/us/books/small-business-accounting-software/
- Xero mobile accounting overview: https://www.xero.com/us/accounting-software/xero-accounting-mobile-app/
- Arabic accounting/inventory benchmark: https://play.google.com/store/apps/details?id=net.ssdsoft.ssdmobile
- Wafeq feature overview: https://www.wafeq.com/en

### Release gates added by this review

1. Do not call the product commercially ready on a successful build alone.
2. Before installation, confirm the newest CI run passes and inspect the exact artifact name, version, package ID, and signing status.
3. Before relying on it for real books, test every posting path against expected journal entries and reconcile trial balance, inventory valuation, profit, cash and bank balances with an accountant.
4. Before paid distribution, configure a securely retained owner-controlled signing key, verify the signed APK, add privacy/EULA/support/update documents, and implement a real license service only if paid activation is part of the offer.
5. Prioritize payment-method accounting, customer/supplier statements, invoice numbering and multi-line/return workflows before feature expansion or cosmetic work.
