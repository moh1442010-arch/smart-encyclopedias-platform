# Smart Company Accounting App — Release Readiness

Current target release: **2.1.0 for Android and Windows**. The latest Android build has passed CI; the updated Windows build is being rechecked after recent code changes.

## Implemented in the current source tree

- Arabic-first interface with an Arabic/English toggle.
- Local PIN protection; new PINs are stored as salted PBKDF2 hashes, with migration from the older plain-PIN preference.
- Product inventory, manual or keyboard-style USB/Bluetooth barcode input, sales and purchases, customers and suppliers.
- Cash expenses, bank accounts and movements, cheque register, employee records, monthly salary/advance/deduction calculations.
- Financial summaries, trial balance, report printing/PDF through Android PrintManager, and sharing a report through an email app.
- Database export and restore flow with SQLite integrity/table checks.
- Windows desktop companion includes product inventory, sales/purchases, customers/suppliers, employee monthly salary/advance/deduction summaries, cash-expense tracking, report printing/email sharing, and SQLite backup/restore validation.

## Important release status

A successful CI build proves compilation and packaging only. It does **not** prove the app has been installed and exercised on a real phone or that accounting results are correct for every business scenario.

The non-activatable seven-day Android lock was removed from this pre-release build so it cannot permanently lock the owner out before a real licensing service exists. **This is not a completed commercial licensing system.** Before paid public distribution, implement and test server-verified license issuance, renewal/revocation, offline grace policy, privacy terms, and customer support workflow. Do not ship a production signing key in the repository.

## Required device acceptance tests before selling

1. Install on the target Huawei/Android device; launch, close, and relaunch.
2. Create a PIN, restart the app, verify correct and incorrect PIN handling, and switch languages.
3. Add a product; sell a partial quantity; confirm stock decreases; attempt an over-sale and confirm it is rejected.
4. Purchase stock; confirm quantity increases and the unit-cost behavior is acceptable for the intended accounting policy.
5. Enter a cash expense and a bank deposit/withdrawal; verify reports and trial-balance totals.
6. Add an employee, record a same-month advance and deduction, and verify the displayed net payable.
7. Export a backup, make new test transactions, restore the backup, and verify that the restored data is exactly the expected snapshot.
8. Print/save a PDF report and share the report using an available email app.
9. Test empty, malformed, negative, very large, and duplicate inputs.
10. Verify the app name/icon, Arabic layout, and behavior on the minimum supported Android version.

## Known limitations to address before a full commercial claim

- No real server-side licensing/activation is currently integrated on either platform. Trial locks and the hard-coded Windows activation code were removed from the pre-release candidate to avoid lockouts and insecure shared codes. Do not advertise paid activation until a proper license service exists.
- Camera-based barcode scanning is not implemented; keyboard-emulating USB/Bluetooth scanners or manual entry are the intended path.
- Payroll calculations are displayed on both platforms, but payroll events are not yet posted to the general ledger as a formal salary-accrual/payment workflow. The Windows edition is a basic inventory/transaction companion and does not yet match Android's bank-account and cheque modules.
- The current product-cost policy and all journal/report totals require accountant-led test cases before being relied on for statutory books.
- The latest APK must be installed and manually tested. A debug APK is for testing; a production APK must be signed with the owner's securely stored release key.

## Release artifact naming

GitHub Actions uploads Android outputs as `smart-company-v2.1.0` and Windows output as `smart-company-windows-v2.1.0`. Check the newest run conclusion and artifact contents before sharing any package. Use the debug APK for the first controlled Android device test; do not distribute an unsigned release APK as a finished commercial product. Windows x64 publishing is self-contained, but direct public distribution should eventually use a trusted code-signing certificate or a managed store distribution route.
