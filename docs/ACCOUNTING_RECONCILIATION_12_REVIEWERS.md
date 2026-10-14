# Unified Accounting Reconciliation — 12-Reviewer Acceptance Protocol

**Status:** Test protocol prepared; execution by twelve independent accountants is NOT yet evidenced. This document is not a substitute for actual reviewers, populated-database migration tests, or passing automated tests.

## Mandatory precondition
Do not run on production/customer data. Use disposable test companies and preserve a pre-migration backup. The monetary persistence migration must be implemented and pass its populated-database upgrade tests before this protocol can be marked executable. Current SQLite monetary columns still include `REAL`; calculation-boundary rounding alone does not satisfy that prerequisite.

## Shared fixture (SDG, two decimal places)
Create a fresh company with:
- Opening cash: SDG 10,000.00
- Inventory item A: 10 units, unit cost SDG 100.00, selling price SDG 150.00
- Customer C opening receivable: SDG 0.00
- Supplier S opening payable: SDG 0.00
- No tax (tax feature is not enabled in this candidate)
- No pre-existing journal entries except opening balances explicitly entered in the test fixture.

All reviewers must use the same fixture, same build commit, and same step order. Record APK SHA-256, app version, device/Android version, database schema version, reviewer identity/qualification, start/end time, and evidence references.

## Canonical scenario and expected balances
All values are SDG. Any discrepancy, duplicate posting, silent data rewrite, or unbalanced journal is a FAIL.

| Step | Operation | Expected result |
|---|---|---|
| 1 | Sell 2 units at 150, total 300; collect 200 cash, leave 100 receivable | Cash 10,200; stock 8 units; customer receivable 100; sales 300; COGS 200; gross profit 100 |
| 2 | Collect remaining 100 from customer | Cash 10,300; customer receivable 0; no second revenue posting |
| 3 | Purchase 5 units at 80, total 400, unpaid to supplier | Stock 13 units; inventory carrying cost 1,200; supplier payable 400; cash unchanged |
| 4 | Pay supplier 150 | Cash 10,150; supplier payable 250; inventory and profit unchanged |
| 5 | Return 1 unit from original sale, refund 150 cash | Stock 14 units; sales returns 150; cash 10,000; customer receivable remains 0; return linked to original invoice; no duplicate return permitted |
| 6 | Attempt same return again | Rejected; no changes to stock, cash, journal, revenue, or profit |
| 7 | Payroll: gross 1,000, advance 100, deduction 50 | Expense 1,000; advance receivable reduced 100; deductions payable 50; salaries payable 850; journal debits equal credits |
| 8 | Pay payroll net 850 from cash | Cash 9,150; salaries payable 0; expense not duplicated |
| 9 | Attempt cheque status “cleared” or “returned” without settlement workflow | Blocked; no balance or journal changes |
| 10 | Set non-zero tax percentage while tax engine is unavailable | Blocked; invoice must not post with misleading tax totals |
| 11 | Close/reopen app and export then restore backup into a disposable install | Values, links, quantities, journals, and schema preserved exactly; no duplicate postings |
| 12 | Upgrade a populated database from previous schema | Every monetary value, quantity, invoice relationship, journal line, payroll record, and backup is preserved; migration is atomic and safe to retry |

## Accounting invariants every reviewer must verify
1. Every posted journal entry balances: total debits = total credits at the currency's supported precision.
2. Trial balance debits = credits; opening balance + movements = closing balance for each tested control account.
3. Customer and supplier subledgers reconcile to their general-ledger control accounts.
4. Stock quantity and inventory valuation reconcile to purchase, sale, and return movements; COGS reverses appropriately on sales returns.
5. Cash/bank ledgers reconcile to receipts, payments, payroll payouts, and refunds.
6. Profit reports distinguish sales, sales returns, COGS, payroll expense, and other expenses without double-counting.
7. Partial payment and full settlement change receivable/payable and cash only; they do not create duplicate revenue/expense.
8. Currency scale and rounding are consistent across input, persistence, journal, report, PDF, and export.
9. Failed/duplicate operations leave the database unchanged (transaction rollback).
10. Upgrade, backup, and restore must preserve both values and referential links; no destructive uninstall is part of the test.

## Twelve-reviewer protocol
Assign 12 independent accounting reviewers. Each completes all 12 scenario steps and invariants against the same candidate build; do not divide the scenario so that any reviewer skips a section. Reviewers may compare results only after submitting their independent findings.

For each reviewer capture:
- Reviewer ID (avoid publishing personal data), accounting qualification/role, date, build commit and APK SHA-256.
- PASS / FAIL / NOT TESTED for each step and each invariant.
- Opening/closing trial balance, account-level reconciliation, inventory count/valuation, customer/supplier subledger totals, cash/bank balances.
- Defect ID and reproducible steps for each failure, with redacted screenshots/export evidence.
- Signature or written sign-off retained in the project's controlled evidence folder.

## Acceptance threshold
- All 12 reviews complete.
- Zero unexplained variance and zero critical/high accounting defects.
- All migration, rollback, duplicate-posting, backup/restore, and regression tests pass.
- Every failure fixed and the entire shared scenario rerun on the new candidate.
- Independent reviewer evidence is attached to the release record.

Until these conditions are evidenced, status remains **NOT ACCEPTED FOR COMMERCIAL RELEASE**. Do not fabricate reviewer participation or label this protocol as completed testing.
