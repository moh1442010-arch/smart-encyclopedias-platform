# Al-Mizan Enterprise Edition — Implementation and Acceptance Specification

Status: approved scope; implementation and runtime verification are not yet complete.
Base: Android 2.3.9 / versionCode 40, four-edition draft PR #15.

## Product boundary
Enterprise is the highest edition. Do not advertise a capability as implemented until the code, license enforcement, tests, and release notes demonstrate it. Existing CI build/signing checks are not runtime or accounting acceptance.

## Approved Enterprise scope
1. Multi-company and multi-branch management with strict data isolation by company and branch.
2. Central dashboard with branch/company rollups and period comparisons.
3. Branch-level configuration: currency, tax settings, invoice numbering, warehouses.
4. Role-based access control with granular permissions and server/license plan checks.
5. Sensitive-operation approval workflow (at minimum void/correction of posted documents, opening-balance changes, and branch transfers).
6. Audit trail for sensitive actions: actor, timestamp, entity/record, before/after values, reason/reference. Normal users must not edit or delete audit entries.
7. Posted accounting records must be corrected by reversal/correction entries, not destructive edits.
8. Consolidated reporting (sales, expenses, cash/bank, receivables/payables, inventory, trial balance, general ledger, income statement, balance sheet) only where the underlying postings are complete and reconciled.
9. Backup/restore must include all company/branch data, audit records, and configuration; acceptance requires a restore test and reconciliation.
10. Edition entitlements must be enforced in app navigation and by the licensing/API boundary where server operations exist; mismatched edition codes must be rejected.

## Mandatory implementation sequence
- E1: Define and migrate company, branch, membership, role, permission, approval, and audit data model. Preserve existing single-company data through a reviewed migration.
- E2: Add company/branch context to every business query and write path; prove no cross-company leakage.
- E3: Implement authorization checks for every sensitive operation; default deny for unknown permissions.
- E4: Implement approval state transitions and immutable audit events; transactionally couple business change and audit write.
- E5: Implement consolidated reports only after accounting posting and inter-branch transfer accounting reconcile.
- E6: Extend backup/restore and verify restored balances, permissions, and audit history.
- E7: Run unit, migration, API, and device acceptance tests; publish a release only after evidence is attached.

## Acceptance tests
- Two companies and at least two branches each: records, customers, inventory, cash, and reports remain isolated.
- A user with branch A access cannot read, export, edit, or delete branch B records by UI, direct API, or crafted identifiers.
- A user lacking approval permission cannot approve their own sensitive action unless an explicit policy permits it.
- Approved changes retain the original values and produce a durable audit event; failed changes leave neither partial business records nor audit artifacts.
- Posted sale/purchase/return/transfer scenarios reconcile inventory, cash/bank, customer/supplier balances, revenue/COGS, and trial balance.
- Backup then restore to a clean test environment; compare record counts, balances, branch boundaries, and audit history.
- Edition license mismatch and expired entitlement are rejected without deleting customer data.

## Release gate / known limitations
- Do not merge this spec as proof that features exist.
- Do not advertise multi-company, multi-branch, audit, approval, consolidated reporting, or automated subscription until implemented and tested.
- Current project notes identify SQLite (not SQLCipher), floating-point monetary storage, incomplete bank/cheque/payroll/report workflows, and no automated renewal as unresolved limitations.
- No production license Worker changes or public release should be made solely from this document.
