# Smart Company Accounting Assistant — Safe Agent Architecture

Status: architecture contract for future implementation; this document does not claim an AI agent is already shipped.

## Non-negotiable accounting boundary
- The AI assistant must never write directly to SQLite or issue arbitrary SQL.
- It may explain reports and propose a draft action through typed application services only.
- Any operation that changes balances, stock, debts, payroll, cheques, or journal entries requires a human confirmation screen showing the exact before/after amounts.
- Posting must be atomic and idempotent: one business document can produce at most one journal entry for each posting event.
- Posted entries are immutable. Corrections use linked reversal entries; do not delete or silently rewrite accounting history.
- If the app cannot prove the party, source document, quantity, cost, or account mapping, the agent must stop and request review rather than guess.
- All agent suggestions, approvals, rejected actions, tool results, and posting IDs must be recorded in an audit log.

## Phased rollout
1. **Read-only adviser:** explain stock discrepancies, unpaid customer/supplier balances, cheque due dates, and ledger warnings using data already calculated by the accounting engine.
2. **Draft assistant:** prepare a draft receipt, payment, return, stock adjustment, or journal voucher; the user reviews every field.
3. **Confirmed execution:** call the same validated business services as the manual UI after explicit confirmation; never bypass validation or transaction boundaries.
4. **Reconciliation assistant:** compare operational records against journal postings and flag differences. It must not silently repair balances.

## Required typed operations
- `GetStockAndCost(productId)`
- `GetPartyBalance(partyType, partyId)`
- `DraftReceipt(customerId, amount, reference)`
- `DraftSupplierPayment(supplierId, amount, reference)`
- `DraftSalesReturn(originalSaleId, lines, reason)`
- `DraftPurchaseReturn(originalPurchaseId, lines, reason)`
- `DraftChequeStatusChange(chequeId, newStatus)`
- `RunLedgerIntegrityCheck(dateRange)`

Each command must validate IDs and balances on the server/application side immediately before commit, use a unique idempotency key, and return a structured result. Free-form model output is never accepted as a posting payload without schema validation.

## Privacy and security
- Keep company data local by default; send no customer, supplier, payroll, cheque, or invoice data to an external model without explicit informed consent.
- Store API credentials outside source control and never embed provider secrets in mobile or desktop binaries.
- Use least-privilege access, administrator-only licensing controls, rate limits, and tamper-evident audit events for any remote administration service.
- The admin dashboard must not expose customer data publicly. Visitor counters should be aggregate counts, not identity tracking.
- Expired subscriptions may restrict new operations but must not delete or encrypt-lock customer records.

## Release gates before enabling write actions
- Tests for double-entry balance, idempotency, concurrency, partial returns, over-returns, insufficient stock, cash/credit settlement, and rollback on failure.
- Reconciliation tests comparing stock valuation, cash, receivables, payables, COGS, sales returns, purchase returns, and trial balance.
- Restore a backup into a separate test database and run SQLite integrity plus ledger integrity checks.
- AI actions remain disabled in production until the manual accounting engine passes these gates.
