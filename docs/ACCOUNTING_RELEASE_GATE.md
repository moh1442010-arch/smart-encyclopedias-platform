# Smart Company — Accounting Release Gate (Sudan-first)

**Purpose:** distinguish implemented code, automated test coverage, and evidence from a real-device/accountant acceptance test. This document is a release gate, not a claim that every item has passed.

## Current known state

- Android release build and signing validation succeeded for build 2.3.1 on 2026-10-14.
- Existing automated tests cover selected database invariants, return duplication, payroll posting, and money helper behavior.
- Legacy SQLite monetary columns stored as REAL remain. A complete fixed-point database migration has **not** been established by the available evidence.
- Currency precision is now derived from the selected company currency for invoice calculations, shared rounding, payroll payout input, and display formatting. Currency changes are blocked once transactions, products, or journal entries exist. This is an input/display improvement only: monetary persistence still uses SQLite REAL, so a complete fixed-point migration is **not done**. Supported currency precision mappings include SDG/EGP/SAR/AED/USD/EUR/MRU/SOS (2 digits), KWD/JOD/TND/BHD/IQD/LYD/OMR (3 digits), and XAF/DJF (0 digits).
- Physical installation, update, backup/restore, legal/tax acceptance, and accountant acceptance remain unverified.

## Mandatory release gates

Use exactly one status per test: **PASS**, **FAIL**, or **NOT TESTED**. Attach test input, expected result, actual result, app version, device/build, and evidence.

| ID | Area | Test | Required evidence | Status |
|---|---|---|---|---|
| FIN-01 | Money | Half-up rounding at the configured currency scale | Unit test output | NOT TESTED |
| FIN-02 | Money | Three-decimal currency (e.g. KWD) and zero-decimal currency (XAF) | Unit test output | NOT TESTED |
| FIN-03 | Persistence | Inventory, invoices, payments, payroll, journals and reports no longer lose precision | Schema/read-write audit + tests | NOT TESTED |
| FIN-04 | Migration | Upgrade a copied populated v10 database to v11 without changing balances | Before/after reconciliation report | NOT TESTED |
| ACC-01 | Sales | Sale updates stock, revenue, COGS, cash/receivable, customer balance | Independently calculated expected values | NOT TESTED |
| ACC-02 | Purchase | Purchase updates stock, supplier payable, cash and inventory cost | Independently calculated expected values | NOT TESTED |
| ACC-03 | Returns | Partial/full sale and purchase returns reverse the correct original effects exactly once | Journal and stock reconciliation | NOT TESTED |
| ACC-04 | Ledger | Every posted journal balances and reports reconcile to ledger totals | Trial balance + general ledger | NOT TESTED |
| ACC-05 | Payments | Partial payments, excess payments, refunds and credit limits are handled consistently | Scenario test results | NOT TESTED |
| INV-01 | Costing | Chosen inventory costing policy is documented and applied consistently | Policy + multi-purchase/sale tests | NOT TESTED |
| PAY-01 | Payroll | Accrual, advances, deductions, partial/full payouts and duplicate prevention | Payroll register + journal | NOT TESTED |
| BANK-01 | Cheques | Issue, deposit, clear, bounce/reverse, and re-settle without duplicate posting | Bank/party/GL reconciliation | NOT TESTED |
| SEC-01 | Encryption | Database and backup encryption/key handling verified | Implementation review + restore test | NOT TESTED |
| SEC-02 | Access | Role permissions and audit trail prevent/record sensitive changes | Permission matrix + test log | NOT TESTED |
| BAK-01 | Backup | Backup restores to a clean install with correct balances and attachments | Restore evidence + reconciliation | NOT TESTED |
| MOB-01 | Device | Install, launch, navigate back, update, and retain data on a real Samsung device | Device/model/Android/version notes | NOT TESTED |
| OFF-01 | Offline | Create transaction offline, restart app, verify persistence | Repeatable device test | NOT TESTED |
| TAX-01 | Sudan | Applicable tax/invoice/payroll obligations reviewed from authoritative local sources | Dated compliance memo | NOT TESTED |
| OPS-01 | Release | Signed APK hash, signing key custody, reproducible artifact and rollback plan | Release manifest + SHA-256 | NOT TESTED |
| UAT-01 | Accountants | Twelve reviewers complete the same script and submit findings | Reviewer records + issue log | NOT TESTED |

## Shared accounting scenario for independent reviewers

Run only in a disposable test company/database. Set the currency and costing policy before entering transactions.

1. Opening stock: 10 units at 60.00 each.
2. Purchase: 5 units at 80.00 each, initially unpaid.
3. Sale: 4 units at 100.00 each, collect 250.00 immediately.
4. Record a partial customer payment for the remaining receivable.
5. Record a partial supplier payment.
6. Record a sale return linked to the original sale and verify the stock, revenue, tax (if configured), cash/receivable, and COGS reversals.
7. Record a purchase return linked to the original purchase and verify stock, payable/cash, and costing effects.
8. Accrue payroll with a salary, an advance, and a deduction; then pay it in two installments.
9. Record one expense and reconcile the cash balance.
10. Confirm trial balance debit = credit; reconcile customer, supplier, inventory, cash, payroll payable, revenue, returns, expense and COGS reports to ledger entries.

**Do not publish expected final profit until the inventory costing method, tax treatment, and timing of returns are explicitly fixed.** Each reviewer should calculate expected results independently before looking at app output.

## Merge and launch policy

- Do not merge a change solely because the APK builds.
- Do not mark any row PASS without evidence.
- Do not use a production company's data during exploratory testing.
- Do not claim multi-country tax compliance, full integer-minor-unit persistence, SQLCipher protection, or commercial readiness until the corresponding gates pass.
