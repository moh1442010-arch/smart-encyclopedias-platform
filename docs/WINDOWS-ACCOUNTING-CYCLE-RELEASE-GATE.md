# Windows accounting-cycle validation — release gate

## Current implemented paths in the latest source
- Additive SQLite schema migrations add optional `sales.customer_id`, `sales.payment_mode`, `purchases.supplier_id`, and `purchases.payment_mode` without dropping or rewriting old rows.
- New cash sales post Dr Cash / Cr Sales Revenue and Dr Cost of Goods Sold / Cr Inventory.
- New credit sales require a selected customer and post Dr Accounts Receivable / Cr Sales Revenue and Dr Cost of Goods Sold / Cr Inventory.
- New cash purchases post Dr Inventory / Cr Cash.
- New credit purchases require a selected supplier and post Dr Inventory / Cr Accounts Payable.
- Journal entry creation and inventory/source transaction writes share a SQLite transaction; unbalanced entries are rejected.
- The trial-balance screen totals posted ledger lines and reports legacy sales, purchases and expenses without a corresponding journal entry.

## Release-blocking gaps still open
- Customer receipts and supplier payments are not implemented.
- Returns, sales/purchase credit notes, reversals, bank transactions, payroll expense/accrual/payment, tax, opening balances beyond opening inventory, period closing and audit log are not yet integrated into the full general ledger.
- Old transaction history is deliberately not auto-posted: historical records do not reliably preserve payment mode or party linkage. The screen counts missing entries so they can be reconciled rather than inventing journal facts.
- Money values still use SQLite REAL / C# double. Do not treat the application as production accounting until a migration to integer minor units (or a rigorously validated decimal strategy) is planned and tested.
- SQLCipher/database encryption and encrypted backups are not implemented by this change.
- A successful Windows CI build verifies compilation and packaging only; it does not replace runtime/accounting tests on Windows.

## Required test matrix (use a disposable database copy)
1. Add an opening stock of 100 units at cost 10. Expected: Inventory Dr 1,000; Opening Equity Cr 1,000.
2. Cash sale 20 units at selling price 15. Expected: Cash Dr 300; Sales Revenue Cr 300; COGS Dr 200; Inventory Cr 200; remaining stock 80 units.
3. Credit sale 5 units at selling price 15 to a selected customer. Expected: Accounts Receivable Dr 75; Sales Revenue Cr 75; COGS Dr 50; Inventory Cr 50; remaining stock 75 units; customer linked to sale.
4. Cash purchase 10 units at cost 12. Expected: Inventory Dr 120; Cash Cr 120; quantity 85; moving-average unit cost 10.235294... (subject to defined rounding policy).
5. Credit purchase 10 units at cost 12 from a selected supplier. Expected: Inventory Dr 120; Accounts Payable Cr 120; quantity 95; supplier linked to purchase.
6. Try selling more units than available. Expected: no stock, transaction, or ledger change.
7. Force a duplicate product/barcode error. Expected: no partial insert or journal entry.
8. Verify total debits equal total credits to the agreed minor-unit tolerance after every successful operation.
9. Compare source counts with journal counts; old rows without journal entries must remain visibly flagged.
10. Export a backup, restore to a separate test environment, then compare table counts and ledger balances.

## Data safety policy
- Never run migration experiments on the only copy of a customer database.
- All schema changes must be additive and idempotent where possible.
- Do not automatically manufacture historical journal entries when party, payment method, cost basis, or original transaction date is unknown.
- Preserve user data after license expiry and permit export.
- Do not advertise full-cycle accounting until all release-blocking gaps above are closed and the tests are executed and recorded.
