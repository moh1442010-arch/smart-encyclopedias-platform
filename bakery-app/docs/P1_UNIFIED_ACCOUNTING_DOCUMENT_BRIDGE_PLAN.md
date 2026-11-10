# Unified Accounting Document Bridge — Implementation Plan

Status: implementation design; no D04–D11 closure claimed.
Baseline commit: `e27eb45ea12c64c8d65ce973fc9c08866811b7b9`.

## 1. Source-path findings

The bakery operational database currently stores mutable JSON records in `records(day,module,payload,updated_at)`. The primary save path is `MainActivity.Store.put`; the counter and distribution screens also write through this store.

The current sales channels are distinct in the existing calculation:
- `window` contributes counter cash, receivables and bank transfers.
- `distribution` contributes customer-level cash, receivables and bank transfers.
- `salesRevenue()` adds the two channels through `cashTotal()`, `debtTotal()` and `bankTotal()`.

The bridge must therefore create separate source documents for `window` and `distribution`. It must not post a combined daily total as a third document, because that would duplicate the channel totals.

## 2. Safe classification of legacy fields

| Legacy module | Safe accounting interpretation | Current limitation |
|---|---|---|
| `window` | Sale document: debit cash (1000), bank/Bankak (1010), receivables (1100); credit sales revenue (4000) | One daily aggregate; bank transfers have references, cash and debt do not have invoice-level allocation |
| `distribution` | Separate customer sale documents or one document with customer subledger allocations; same control accounts 1000/1010/1100/4000 | Customer IDs exist in current row format, but older row formats may lack IDs and must be quarantined, not guessed |
| `expenses` meals, maintenance, parsed other rows | Operating expense document: debit operating expenses (5200), credit the explicitly selected payment/payable account | Existing form does not identify the payment channel for each expense |
| `expenses.purchases` | Not safe to post as an expense or supplier invoice | Aggregate amount has no supplier, invoice number, items, quantities, tax, payment method or inventory valuation |
| Customer receipts | No distinct bakery receipt workflow has been identified in the inspected save paths | A debt balance is a receivable, not evidence of a later receipt; bank transfer recorded at sale is a sale tender, not a later collection |
| Supplier purchases/payments | Must wait for a normalized supplier invoice/payment workflow | Current aggregate purchases field is insufficient evidence |

The seeded chart of accounts includes 1000 Cash, 1010 Bank, 1100 Accounts Receivable, 2000 Accounts Payable, 1200 Inventory, 4000 Sales Revenue and 5200 Operating Expenses. These codes are candidates only where the document semantics and tender details support them. No mapping is inferred from Arabic display labels.

Legacy rows without a customer ID, explicit tender, branch or reliable transaction currency must remain visible as exceptions until resolved. Do not synthesize missing evidence.

## 3. Required service boundary

Introduce one shared `AccountingDocumentService` for all operational modules. Its public operations should be:
- `saveDraft`: validate and persist document header/lines.
- `approveAndEnqueue`: atomically persist approval, immutable posting snapshot and outbox event.
- `processOutbox`: post outside the source transaction through `GeneralLedger.post`.
- `reverseDocument`: create a linked reversal and replacement; never mutate or delete a posted document.

Proposed additive tables:
- `accounting_documents`
- `accounting_document_lines`
- `accounting_document_revisions`
- `accounting_outbox`
- `accounting_posting_links`

The legacy JSON row and its normalized document/outbox event must commit in one SQLite transaction. The outbox processor then calls `GeneralLedger.post` after that transaction commits. This is required because `GeneralLedger.post` rejects an already-open database transaction and owns its posting transaction.

Every document must snapshot its date, source module, source record key, revision, branch, transaction currency, actor, document reference and immutable line payload/hash. Resolve the active branch and the company's actual base/transaction currency from database state; do not hardcode `MAIN`, SDG or a 1:1 rate for an unverified legacy row.

A retry with the same key and payload must return the original GL reference. A retry with changed payload must be rejected. Editing an already-posted source must enqueue a linked reversal and a replacement revision in order. A posted document must never be silently overwritten.

## 4. Implementation sequence

1. Add additive schema and migration tests that preserve all existing `records` rows and their payload hashes/counts.
2. Implement document validation, stable identity, immutable revisions and the outbox processor.
3. Add a read-only legacy classifier. It reports unsupported/ambiguous rows without posting them.
4. Integrate the `window` and `distribution` save paths as separate sale documents after tender, branch and currency validation.
5. Add explicit payment allocation to expenses before enabling expense posting. Keep `purchases` excluded until supplier invoice details exist.
6. Add dedicated customer receipt and supplier invoice/payment screens; then connect those save paths to the same service.
7. Add end-to-end tests for every document type, duplicate delivery, changed-payload rejection, crash after GL commit, reversal/replacement, closed period, inactive account/branch/currency, reconciliation and migration preservation.

## 5. Closure gates

D04–D11 remain OPEN until the real UI save paths, normalized documents, outbox delivery, GL rows, source references and reconciliations all pass on one commit SHA. A passing unit test or successful APK build alone is insufficient.

D05 and D06 cannot be claimed complete while the distinct receipt and supplier-invoice workflows are absent. D07 cannot be claimed complete while expense tender is ambiguous. API 28 device validation remains separate from API 35 validation.
