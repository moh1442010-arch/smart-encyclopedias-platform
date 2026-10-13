# Accounting and Admin Release Gate
## Purpose
This document defines the release gate for the Android and Windows editions of Mohammed Mustafa Smart Company. It is not evidence that any item below is already implemented or tested.

## Release blockers
1. Windows has no integrated general ledger postings. The current Windows README explicitly labels the summary as preliminary and says sales, purchases, expenses, and payroll are not posted to a general ledger.
2. Do not calculate or label an operating difference as net profit. Until posting and inventory-cost flows are implemented and reconciled, call it an operational summary only.
3. Monetary values currently use SQLite REAL/double in Windows. A migration to integer minor units must be designed with a versioned migration and backup/restore tests; do not change column types in place without a migration plan.
4. Android has ledger/journal infrastructure, but all transaction paths must be tested against the same expected accounting outcomes, including returns, collections, supplier payments, bank movements, checks, payroll, manual journals, and opening balances.
5. The admin portal must not be a hidden client-side screen protected only by a hard-coded password. It must use server-side authentication, role-based authorization, secure session handling, audit logs, rate limits, and least-privilege access.
6. Visitor counts must be aggregated and privacy-preserving. Do not collect sensitive device identifiers or expose visitor-level browsing histories in the admin panel.
7. Payment requests must be marked pending until a human admin verifies the Bankak transfer. Never activate a paid subscription based only on a client-submitted receipt image.

## Proposed admin capabilities
- Dashboard: aggregated visitor counts, new subscription requests, active/trial/expired subscriptions, and unresolved feedback.
- Customers/subscribers: search by customer/company and subscription status; inspect plan, duration, start/end dates, device count, and payment verification state.
- Manual payment review: request reference, submitted receipt, amount/currency, reviewer, decision, timestamp, and audit history. Store receipts privately; restrict access and retention.
- Plan management: monthly, annual, Pro, and other explicitly configured services; do not present unimplemented plan features as available.
- Brand/service request: allow the customer to provide company name, optional logo, requested service/plan, subscription term, and additional requirements. Validate and limit uploads.
- Feedback and problem reports: category, description, optional contact, status, created date, and admin response. Warn users not to include passwords or sensitive financial data.
- Visitor welcome message and a small aggregate visitor counter; counter must clearly state its measurement period and must not be represented as unique visitors unless deduplication is actually implemented.
- Audit log for admin login, subscription activation/deactivation, plan changes, payment verification, customer-data access, and feedback status changes.

## Accounting cycle and acceptance tests
Run independently on Android and Windows using a fresh test company and known opening balances. Preserve the test database and expected results with the build artifact.

### Required end-to-end cases
1. Opening inventory: debit inventory, credit opening equity; quantity × unit cost must equal the opening inventory value.
2. Cash sale: debit cash, credit sales; debit cost of goods sold, credit inventory. Quantity and inventory valuation must decrease consistently.
3. Credit sale: debit accounts receivable, credit sales; debit cost of goods sold, credit inventory.
4. Customer receipt: debit cash/bank, credit accounts receivable; customer balance decreases without recording revenue a second time.
5. Purchase paid in cash: debit inventory (and eligible input tax if implemented), credit cash/bank. Inventory quantity and weighted-average/FIFO costing must follow the declared policy.
6. Credit purchase: debit inventory, credit accounts payable.
7. Supplier payment: debit accounts payable, credit cash/bank; do not record a second purchase.
8. Sales return: reverse the original revenue/receivable or cash impact and restore inventory at its original cost where applicable; prevent over-returning.
9. Purchase return: reverse the original payable/cash and inventory value; prevent over-returning.
10. Expense paid immediately: debit expense, credit cash/bank. Credit expense: debit expense, credit payable.
11. Payroll accrual and payment: separate wage expense/payable from cash payment; advances/deductions must not double-count wages.
12. Bank transfer and checks: transfer between cash/bank accounts must not create revenue or expense; checks need issue/clear/void status and duplicate-action protection.
13. Manual journal: debit must equal credit; prevent invalid accounts and duplicate submission.
14. Trial balance: total debits equal total credits; report balances by account type and period.
15. Reversal/audit: corrections must use traceable reversal entries, not silent deletion of posted entries.
16. Backup/restore: restore into a test environment, run database integrity checks, and reconcile ledger balances to source transactions.

### Example baseline
- Buy 100 units at 10 currency units each: inventory increases by 100 units and 1,000 in value.
- Sell 20 units at 15 each, with unit cost 10: revenue 300; COGS 200; gross profit 100; inventory becomes 80 units valued at 800 before any other movement.
- Sell 5 units on credit: receivables increase by 75, revenue increases by 75, COGS increases by 50, inventory falls by 5 units/50 in value.
- Receive 30 from that customer: cash/bank increases by 30, receivables decrease by 30, revenue remains unchanged.
- Return 2 units from the original cash sale: reverse revenue by 30 and restore inventory by 20 at original cost, assuming the items are resalable and the original sale is identified.

If a test fails, block commercial release and record the failing operation, expected result, actual result, database state, and build commit.

## Safe implementation order
1. Back up and inspect current schemas and all transaction handlers before modifying data structures.
2. Implement a versioned Windows schema migration and a general ledger posting service with explicit account mapping and transaction-level atomicity.
3. Add product unit cost and a documented costing method before posting cost of goods sold. Historical records without reliable costs must remain flagged as uncosted; do not fabricate historical COGS.
4. Route every supported operation through one transaction boundary covering source record, stock/party balance, journal entry, and journal lines.
5. Add idempotency/duplicate-submit protection and reversal records.
6. Add accounting integration tests with fixed expected balances, then run Android tests against equivalent scenarios.
7. Implement admin APIs behind server-side authorization; keep secrets out of APK/EXE and public repository files.
8. Run Android and Windows CI; inspect artifacts and logs. Build success alone is not a substitute for device-level installation and accounting verification.
9. Pilot with a small number of consenting businesses and test data/backup procedures before broad release.

## Release status
- Windows general ledger: BLOCKED / not implemented according to the current Windows README.
- Android accounting cycle: NOT VERIFIED end-to-end; existing journal code requires scenario-based tests.
- Admin dashboard/API: NOT VERIFIED as a production-secure system.
- Commercial release: BLOCKED until release gates above are evidenced by tests and review.
