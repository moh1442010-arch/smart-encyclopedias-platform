# Expert and customer acceptance checklist — Al-Mizan Accountant

This checklist is a release gate, not evidence that tests have passed.

## A. Build identity and Android operation
- [ ] Confirm the run was built from the latest repair branch commit, not an older SHA.
- [ ] Confirm versionName 2.3.14, calculated versionCode, package ID, APK/AAB signature, release certificate SHA-256, and APK/AAB SHA-256.
- [ ] Install on a clean test device and launch.
- [ ] Update over the currently installed release without uninstalling first; confirm existing test data remains readable.
- [ ] Enter a valid PIN, press Login, and confirm dashboard loads repeatedly without crash.
- [ ] Try invalid/empty PIN, background/restore, back-to-home navigation, low storage, and offline start.
- [ ] Capture sanitized crash logs for any failure. Never include customer data, passwords, PINs, or secrets.

## B. Independent accountant — use controlled sample data only
For every transaction, prepare expected results independently and compare actual outputs:
- [ ] Opening balances: debits equal credits.
- [ ] Cash and credit sales, partial collection, and collection allocated across multiple invoices.
- [ ] Cash and credit purchases, supplier payment, customer receipt.
- [ ] Sales and purchase returns, cancellation/reversal, and audit trace.
- [ ] Inventory quantity/cost, cash/bank, customer/supplier balances, and gross/net profit.
- [ ] Expenses, payroll, employee advance/deduction, checks and bank movements where supported.
- [ ] General ledger, trial balance, income statement, and inventory report reconcile after each scenario.
- [ ] One-minor-unit rounding boundaries and rejection of unbalanced journals.
- [ ] Backup, restore, and schema migration preserve all balances and settings.

A financial mismatch, unexplained negative stock, unbalanced journal, missing history, or data loss is a release blocker.

## C. Technical expert
- [ ] Review login and dashboard exception handling; verify actual cause/fix using logs and reproduction steps.
- [ ] Review SQL/database migrations, backup/restore integrity, permissions, sensitive logs, and data protection claims.
- [ ] Verify update signing continuity and package/version compatibility.
- [ ] Verify license/trial/renewal/payment behavior end-to-end before enabling commercial sales.
- [ ] Test Windows independently before advertising Windows as available.

## D. Customer beta — only after A and B pass
Use 3–5 consenting testers representing different small-business workflows. Use fictitious data, not real financial records.
- [ ] Ask each tester to create a contact, enter a purchase, record a sale, take a payment, view balances and reports, and export/print if supported.
- [ ] Record completion, time, confusion points, errors, and feature requests without coaching toward positive answers.
- [ ] Ask: Was setup clear? Did the balance/inventory change correctly? Were reports understandable? Did anything crash? What is the most important missing feature? Would you use it, and why?
- [ ] Obtain explicit permission before publishing a testimonial; publish critical and positive feedback fairly.
- [ ] Link every defect to reproduction steps, severity, owner, fix commit, and retest evidence.

## Release rule
Do not approve public sale while any critical/high defect is open, login/update behavior is unverified, accounting results do not reconcile, backup/restore is unsafe, or commercial licensing/payment is not validated. CI success alone is not product certification.
