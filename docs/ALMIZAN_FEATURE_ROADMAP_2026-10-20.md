# Al-Mizan Feature Architecture Proposal — 2026-10-20

Status: approved design direction for implementation and expert review; NOT evidence that features are implemented.

## Product decisions

- Enterprise: all three requested capabilities: recurring fixed entries, manufacturing work-in-progress, and Control Room.
- Business: recurring fixed entries + manufacturing work-in-progress. Control Room remains Enterprise-only in the first release.
- Pro and Basic: recurring fixed entries only.
- Shared cross-tier capability: recurring fixed entries, with the same accounting engine and tier entitlement checks on Android and Windows.

This allocation maximizes value for shops and factories without exposing enterprise multi-device administration in lower tiers.

## 1. Recurring fixed entries (all tiers)

Menu label: القيد الثابت

Purpose: configure a recurring monthly expense/income once and generate the appropriate dated transaction every period, for example rent, electricity, fixed cashier payroll, internet, or service bills.

Required fields:
- Name and category; debit/credit accounts; amount and currency; frequency; start date; optional end date; due day; description; active/paused state.
- Optional amount variability mode for bills such as electricity: fixed amount or prompt-for-amount each month. Never silently post an estimated utility bill as a confirmed actual expense.
- Cash/bank/cashier payment source where applicable; cost centre/branch where supported.

Accounting and safety rules:
- A schedule definition is not itself a posted journal. Each due occurrence gets a unique period key and source ID to prevent duplicate posting.
- Support review-before-posting by default; allow auto-post only when the owner explicitly enables it for a genuinely fixed amount and the tenant's policy permits it.
- Electricity and variable invoices should create a monthly draft/reminder for amount confirmation, not an unverified automatic debit.
- Payroll must link to a payroll/cash payment workflow and must not duplicate salary entries generated elsewhere.
- Every occurrence must be balanced, dated, traceable to its schedule, and reversible only through a documented reversal (never silently deleted).
- Use database transactions, idempotency constraints, audit events, and retry-safe execution. Catch up missed periods once after offline periods; do not create duplicate entries.
- Pause, edit future periods, cancel future periods, preview the next 12 occurrences, and show schedule history.
- Monthly reports/journals include only posted occurrences; drafts are clearly separated.

## 2. Manufacturing Work-in-Progress (Enterprise + Business)

Menu label: حساب التشغيل

Flow: raw materials in stock/opening balances -> material issue to a production order -> Work-in-Progress (WIP) account -> capture direct labour and approved overhead -> record output, waste/scrap, and packaging -> calculate actual/standard production cost -> transfer finished goods to inventory -> sales and margin reports.

Minimum records:
- Production order ID, product, planned/actual quantities, bill of materials or manual material issue lines, material lots where available, operator, dates, work centre/phase, labour, overhead, scrap, packaging, completion status, and linked journal/inventory movements.
- States: Draft, Approved, In Progress, Partially Completed, Completed, Cancelled. Closed orders are not editable without a permissioned reversal/reopen action.
- Partial completion must transfer only the completed quantity and its allocated cost; remaining cost stays in WIP.
- Cost formula and allocation policy must be explicit and configurable. Never count the same material/labour/overhead twice.
- Unit cost = eligible material consumed + direct labour + allocated production overhead + packaging, adjusted for scrap/by-products under the configured accounting policy, divided by good finished units.
- Finished-goods transfer and inventory quantity update must commit atomically with the accounting journal, or roll back together.
- Sales margin is reported separately from production cost; it must use the sale's actual net revenue and the cost method configured for inventory.
- Reconcile opening raw-material balances, issues, WIP balances, finished goods, waste, and cost of sales. Provide WIP ageing and production variance reports.

## 3. Enterprise Control Room

Menu label: غرفة التحكم

Enterprise-only in first release. It is an authorization system, not merely a hidden menu.

- Named user accounts; roles and fine-grained permissions; default-deny; least privilege; branch/device scope; session revocation; audit log.
- Roles: General Manager/Admin, Finance Manager, Cashier/Sales, Warehouse/Dispatch, Production Operator, Auditor/Read-only.
- Cashier: create sales/invoices and record permitted payment receipts; cannot edit/void posted invoices, change prices beyond limits, delete records, or change users/roles. Exceptions require manager approval with reason.
- Warehouse: view assigned pick/dispatch tasks, scan/confirm quantities and handover; cannot change invoice totals, accounting entries, costs, or access unrelated modules.
- Finance: permitted cash/bank/reconciliation and finance reports, not user administration or unauthorized inventory edits.
- General Manager: manage role grants, approvals, locks, and exception workflows; sensitive actions require re-authentication and audit logging.
- Approval workflow: request -> reason -> approver identity -> approve/reject -> immutable audit event. No self-approval for high-risk actions by default.
- Multiple devices must use a shared authoritative data service with authenticated sessions, concurrency control, and server-side permission checks. Hiding UI controls alone is not security.
- Offline mode must not claim multi-device live synchronization. Queue only explicitly safe actions, attach idempotency keys, and revalidate permissions and conflicts on sync. High-risk actions require online approval unless a documented offline policy permits otherwise.
- All role enforcement must be applied in business logic and persistence/API layers, not only in the UI. Add permission-denial and privilege-escalation tests.

## Market benchmark implications

Comparable accounting/ERP suites compete on recurring automation, manufacturing/WIP traceability, role-based access, multi-user workflows, inventory integration, and audit trails. These are established categories, not unique claims by themselves. Differentiation should be reliable Arabic-first workflows, transparent pricing in SDG/USD, offline-first single-device use where supported, clear auditability, and low-friction deployment. Do not claim parity with Odoo, Zoho Books, or Daftra until feature and security tests demonstrate it.

Reference material:
- Odoo Manufacturing and WIP: https://www.odoo.com/documentation/20.0/applications/inventory_and_mrp/manufacturing.html
- Odoo manufacturing orders/work orders: https://www.odoo.com/documentation/master/applications/inventory_and_mrp/manufacturing/basic_setup/manufacturing_work_orders.html
- Existing project benchmark record: android-app/MARKET_BENCHMARK_AND_LAUNCH_PRICING.md

## Cross-platform acceptance gates

- Android and Windows use matching accounting rules, schedule occurrence IDs, posting semantics, permissions, and reports.
- Automated tests cover balanced journals, duplicate-run prevention, offline catch-up, schedule edits, variable utility bills, payroll duplication, partial production, WIP reconciliation, scrap, cost allocation, finished-goods transfer, margin calculations, role denials, approval trails, and concurrent-device conflicts.
- Database migration must preserve existing company data and provide a tested backup/restore path.
- Android update must retain the verified release signing certificate and pass install-over-existing-version tests.
- Windows and Android builds must use accurate independent version labels; do not call an artifact a release unless its build, test, package, and checksum gates pass.
- Keep sales closed for any tier whose actual entitlement matrix, live licensing, or build artifact has not been verified.

## Current release-workflow findings (2026-10-20)

- Android workflow file currently declares 2.4.2 / versionCode 43.
- Windows workflow currently declares 2.2.9.
- Recent main-branch run found by API was a documentation/security audit for Windows 2.2.9, not proof that the new features build or work.
- A successful workflow run proves only the steps that workflow actually executes. It does not prove cross-platform feature parity, live licensing, or production accounting correctness.
