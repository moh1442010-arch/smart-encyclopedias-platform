# Smart Company — Accounting Agent Architecture and Release Gates

## Purpose
Prepare the Android and Windows accounting applications for a future in-app AI accounting assistant without allowing probabilistic model output to alter books directly. This is an architecture contract, not a claim that the agent has already been implemented.

## Non-negotiable accounting boundary
1. The AI agent may read authorized business data and explain it, but it must never write directly to SQLite tables or fabricate journal entries.
2. Every proposed operation must be converted into a typed command handled by the deterministic accounting service.
3. The service validates company scope, user permission, date/period rules, stock availability, party balances, cheque state, monetary precision, and debit-credit equality.
4. The UI shows a human-readable preview (source document, affected stock, cash/bank, customer/supplier, account lines, and totals); a permitted user explicitly confirms it.
5. Posting runs in one database transaction and is idempotent. Retrying the same command must not duplicate a document or journal.
6. Posted documents are not silently edited/deleted. Corrections use linked reversal/credit-note/return documents and retain the audit trail.
7. AI output is labelled as a suggestion until validated and confirmed. If facts are missing or ambiguous, the agent asks a question instead of guessing.

## Agent capabilities to expose through typed tools
- Inventory: lookup item/barcode, stock-on-hand, reorder alerts, stock-count variance preview.
- Sales/purchases: draft an invoice from explicit user-provided lines; validate quantity, price, discount, tax and payment split.
- Receivables/payables: customer receipt, supplier payment, allocation against one or more open invoices, remaining-balance preview.
- Cheques: list due/overdue cheques and propose status transitions; never mark a cheque cleared merely because a user mentions it.
- Returns: draft a linked return against an existing invoice; validate returned quantity against prior net sold quantity and reverse the corresponding stock, receivable/payable, revenue/purchase and cost entries.
- Payroll: summarize payroll, draft accrual/payment, and flag advances/deductions whose accounting classification is missing.
- Reports: explain trial-balance discrepancies and identify unposted source documents; do not label a sales-minus-purchases arithmetic figure as net profit.
- Support: capture user feedback/problem reports with consent and without sending secrets or financial records by default.

## Suggested implementation boundary
- Shared domain layer: Money, CompanyContext, Document, DocumentLine, Account, JournalEntry, PostingResult, AuditEvent, IdempotencyKey.
- Deterministic AccountingService: validate and post transactions; AI does not bypass it.
- AI orchestration adapter: provider-neutral interface, schema-constrained tool calls, timeout and rate limits, explicit consent before any external data transfer.
- Policy layer: read-only by default; posting requires explicit confirmation and role permission.
- Audit log: actor, timestamp, command, source-document ID, approval, result, and reversal linkage. Never store API keys or PINs in logs.
- Provider configuration: keep credentials server-side; never embed a provider secret in Android APK, Windows EXE, static HTML, or repository. A local/rule-based assistant should remain available when offline; cloud AI must be opt-in and must disclose data sent externally.
- Cross-platform: both apps call the same documented command semantics. If the apps cannot yet share a compiled domain library, maintain contract tests to ensure equivalent outcomes.

## Accounting release test matrix
Use a disposable test database with known opening balances; never run migration tests against a customer's only copy.

1. Opening inventory: 100 units × 10 = inventory debit 1,000 and opening equity credit 1,000.
2. Cash sale: 20 units × 15; cash +300, revenue +300, inventory -200, COGS +200, gross profit 100; stock 80.
3. Credit sale: 5 units × 15; receivables +75, revenue +75, inventory -50, COGS +50; stock 75.
4. Customer receipt: 25 against the named receivable; cash +25, receivable -25, no new revenue.
5. Cash purchase: 10 units × 12; inventory and cash +/−120 according to the purchase posting; weighted-average unit cost must be recomputed and independently verified.
6. Supplier payment: 40 against an existing payable; cash -40, payable -40, no new purchase.
7. Sales return: two units from an identified invoice; enforce original-invoice quantity limits and reverse sales/receivable/cash and COGS/inventory using the original cost basis.
8. Purchase return: reverse against the original purchase and supplier balance, with a documented inventory-cost method.
9. Expense: debit the correct expense account, credit the actual cash/bank/payable source; no silent cash assumption when unpaid.
10. Payroll: monthly accrual exactly once per employee and month; advances reduce cash and create employee-advance asset; deductions remain unposted until their nature/account is specified; salary payment and advance clearing must be separately tested.
11. Cheques: pending, due, cleared, returned and cancelled transitions must have clear accounting effects and must not double-post.
12. Every test checks source-document totals, stock quantity and value, customer/supplier balances, cash/bank, account-level ledger, balanced trial balance, and idempotent retry.

## Current known Windows limitations to close
- The current Windows UI has basic sales, purchases, expenses, customers/suppliers and payroll events, but the accounting cycle is not complete.
- Customer receipts and supplier payments need explicit document screens and allocations.
- Sales/purchase returns need source-invoice links, quantity caps and reversing entries.
- Payroll deductions have no classification; the app must not guess their counterpart account.
- Salary accrual is being added with a per-employee/per-month uniqueness guard. Salary payment and clearing employee advances are still separate required work.
- Legacy sales, purchases, expenses and opening balances may exist without ledger entries. Do not auto-post them without a reconciliation preview and owner approval; preserve source rows and create an auditable migration batch.
- Monetary values currently use SQLite REAL/double in this Windows implementation. A full migration to integer minor units/decimal requires a versioned migration, backups, and round-trip/reconciliation tests; do not perform an unverified destructive conversion.
- The ledger screen must continue reporting missing postings and must not call an unbalanced or incomplete ledger a finalized financial statement.

## Commercial administration and support
- Keep owner administration behind a server-side secret; never hard-code it into client code or public pages.
- Record only the minimum visitor analytics required. Show aggregate counts, not identifiable visitor data, unless the visitor has explicitly consented.
- Provide a welcome message, support contact, and feedback/problem form. Do not collect PINs, passwords, bank credentials, or full financial databases through feedback.
- Payment via Bankak is manually confirmed at first. A receipt image is evidence for review, not automatic proof of settlement. Do not issue a license solely because a form was submitted.
- On trial/expiry, restrict paid actions without deleting company data; keep a usable export path.
- Admin dashboard and license API must be tested against the deployed Worker/D1 environment before production claims.

## Release gate
Do not mark either platform as commercially ready until:
- Windows build succeeds for the changed commit;
- Android build, signature and install/update paths pass;
- schema migration preserves existing rows on a copied legacy database;
- all accounting tests above pass on both platforms or unsupported flows are explicitly disabled and disclosed;
- backup/restore passes integrity checks and balance reconciliation on a disposable database;
- licensing status, expiry, activation and data export are verified against the live service;
- admin authentication, visitor counter and feedback storage are tested for abuse and privacy;
- the final artifacts and SHA-256 hashes are recorded.
