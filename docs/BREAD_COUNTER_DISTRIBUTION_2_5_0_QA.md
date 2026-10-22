# Android 2.5.0 — Bread counter and distribution QA checklist

Status: **candidate branch only — not released or approved for commercial sale**.

## New screens

- 🪟 **Cash counter / بيع الشباك**: product lookup by name or barcode, integer quantity, unit cash price, total, inventory decrement, SALE transaction, invoice item, journal entry, counter register and invoice reference.
- 🚚 **Distribution / التوزيع**: distributor (restaurant/grocery), product, integer quantity, retail total, discounted distribution total, cash received, debt, calculated price difference, inventory decrement, customer receivable, sales discount ledger, and numbered register.

## Required accounting invariants

- retail_total >= distribution_total >= 0
- cash_received >= 0, debt_amount >= 0
- cash_received + debt_amount = distribution_total
- price_difference = retail_total - distribution_total
- Journal debit and credit totals must balance.
- Sales is credited with retail total; account 4200 (distribution discounts) is debited by price difference; cash and receivables are debited by their actual values.
- COGS is debited and inventory is credited at stored unit cost.
- Inventory must not go negative; failure at any point rolls back the whole operation.
- Invoice references use BC- and BD- prefixes so the existing return workflow can identify the original transaction.

## Example

90 loaves; retail total SDG 30,000; distribution total SDG 28,000; difference SDG 2,000. If cash received is SDG 20,000, outstanding debt must be SDG 8,000. The difference is a discount, not cash collected.

## Tests added

- Schema creation includes both registers and the 4200 discount account.
- A CHECK-constraint test rejects cash/debt totals that do not reconcile.
- Journal posting test verifies cash, receivables, discount, gross revenue, COGS and inventory totals for the example.
- Migration test confirms new tables/account are present after upgrading an older database.
- Restore validator accepts database schema version 11.

## Release gates still required

1. Android unit tests and signed release build pass in GitHub Actions.
2. Install on a real Android device and verify first install, login, counter sale, distribution sale, invalid amount rejection, insufficient stock, return, backup/restore, and offline operation.
3. Confirm actual signed APK certificate fingerprint matches the established long-term signing certificate before offering an update.
4. Confirm the crash after login no longer reproduces on the user's device; CI cannot replace device-level Logcat evidence.
5. Update the public release manifest and publish only after checks pass. This branch is not a download-ready release.
