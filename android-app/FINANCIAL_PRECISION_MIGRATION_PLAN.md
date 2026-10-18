# Financial Precision Migration — Schema Inventory and Safety Design

Status: **Design and inventory only. No database schema or customer data has been changed.**
Target: Android 2.4.1 schema version 10. Do not treat this document as approval to deploy a migration.

## Why this must be staged

The current `MainActivity.DB` creates monetary and quantity fields as SQLite `REAL`; application code reads them into Java `double`, computes balances and posts journal lines using floating-point arithmetic, and binds SQL parameters using `SQLiteStatement.bindDouble`. A safe migration must update the schema, all writers, all readers, aggregation/reporting SQL, backup/restore validation, and tests together. Changing column declarations alone does not remove floating-point rounding from existing data or application calculations.

## Schema inventory from the current source

| Table | Current REAL fields | Meaning / migration class |
|---|---|---|
| `products` | `qty`, `sale_price`, `cost`, `reorder` | `qty` and `reorder` are quantities; `sale_price` and `cost` are unit prices/costs. Do not treat all four as currency totals. |
| `customers` | `credit_limit` | Currency amount. |
| `employees` | `salary` | Currency amount; confirm salary period semantics before migration. |
| `payroll_events` | `amount` | Currency amount. |
| `transactions` | `total`, `paid`, `remaining` | Currency totals. Must reconcile `total = paid + remaining` under defined rounding rules. |
| `invoice_items` | `qty`, `price`, `cost`, `discount` | Quantity, unit price, unit cost and line discount; unit-price scale and line-total rounding need explicit rules. |
| `bank_accounts` | `opening_balance` | Currency amount. |
| `bank_movements` | `amount` | Currency amount. |
| `checks` | `amount` | Currency amount. |
| `party_payments` | `amount` | Currency amount. |
| `journal_lines` | `debit`, `credit` | Posted currency amounts; must reconcile by journal and overall trial balance. |

The schema is declared in `MainActivity.java` inside `DB.schema()`. The same file contains calculations and reporting paths for sales, purchases, returns, inventory valuation, payroll, bank movements, party receipts/payments, journal posting, trial balance, profit estimates and reports. The file also contains generic SQL binding through `DB.tx()`, so the migration cannot be completed by changing a few UI fields.

## Required accounting policy decisions

1. Define the currency exponent from an explicit currency metadata table/configuration (including zero-decimal currencies); do not trust a free-form display-decimal preference as the accounting rule.
2. Define quantity precision separately from currency precision. Unit price/cost may need more precision than a currency total; line extensions and discounts must round at one documented point.
3. Use integer minor units for finalized currency amounts where the currency exponent supports it, or a decimal-safe fixed-scale representation where unit pricing/quantities require greater precision. Avoid binary floating-point as the canonical amount model.
4. Make currency changes on an active company with posted transactions unavailable unless an explicit, audited conversion workflow is designed. A currency selector is not foreign-exchange conversion.
5. Define overflow behavior and reject values that cannot be represented; never silently truncate or clamp financial amounts.

## Migration architecture and non-negotiable gates

- Build a complete column/path inventory and map each column to every insert, update, calculation, SQL aggregate, PDF/export, and report.
- Introduce a versioned migration with an explicit migration marker. Take a verified backup before any transformation; preserve the original database until validation passes.
- Convert within a transaction or a staged copy, not in-place with partial writes. On failure or interruption, retain the previous database and allow deterministic retry.
- Before/after reconciliation must include row counts and totals for every monetary table, transaction-level `total/paid/remaining`, journal-level debit/credit equality, account balances, inventory quantity and valuation, customer/supplier balances, payroll totals and report totals.
- Define how legacy REAL values that cannot be represented at the selected exponent are handled. Report them for review; do not silently round material discrepancies away.
- Test clean creation and upgrades from every supported prior schema version, including repeated launch after interrupted migration, corrupted backup, wrong backup password, low storage, process termination and rollback.
- Test backup export and restore before/after migration, including restoring a pre-migration backup into the new version and confirming the migration is applied exactly once.
- Run the full accounting regression suite against both a fixture with known values and randomized edge cases; include partial payments, returns, fractional quantities, discounts, negative/zero values where allowed, large amounts, and zero-decimal currencies.
- Keep the migration behind the release gate until the tests and independent accounting review pass. Do not apply an untested migration to a customer's only database.

## Evidence still required before implementation can be called complete

- A reviewed accounting policy for currency exponents, unit-price scale and rounding.
- A deterministic set of versioned database fixtures representing every supported upgrade path.
- Automated conversion, reconciliation, interruption/retry and rollback tests.
- Passing Android build and regression CI on the exact candidate commit.
- Real-device upgrade/restore acceptance with a disposable test company, then independent accountant sign-off.

## Current disposition

**BLOCKED — no migration has been deployed.** The existing REAL/double model is still a release blocker. This inventory is a preparation step to reduce the risk of data loss and accounting drift; it does not itself fix financial precision or authorize commercial release.
