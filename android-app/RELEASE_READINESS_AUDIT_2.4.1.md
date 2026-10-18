# Al-Mizan 2.4.1 — Release Readiness Audit

Audit date: 2026-10-18  
Branch: `feature/company-currency-expansion`  
Status: **Not approved for commercial release**

## Changes made in this review

- Expanded the company-currency selector from 22 to 38 currency codes, with Arabic and English labels.
- Set Android `versionCode=42`, `versionName=2.4.1`.
- Fixed a release-QA defect that expected `versionCode=41` while checking version 2.4.1. This defect caused the signed release-artifact upload step to be skipped.
- Aligned in-app and website pricing with the approved SDG/USD price list for all four tiers, including Enterprise and permanent licenses.
- Clarified that a permanent license does not include future updates.
- Updated commercial and market-pricing documentation to distinguish approved prices from permission to open sales.
- All changes are on a review branch; this audit does not mean the pull request is merged or that release CI has passed.

## Confirmed blockers and risks

### P0 — Financial data precision

The SQLite schema stores money-like values using `REAL`, and the app performs financial calculations using Java `double`. Binary floating-point is not an appropriate canonical representation for commercial accounting amounts. A safe fix requires a planned schema migration to integer minor units or a decimal-safe model, conversion rules, reconciliation tests, and rollback/backup validation. Do not perform a blind type conversion or deploy an untested migration over customer data.

### P0 — Release verification

The release-QA versionCode check was wrong and has been corrected. The new signed build and final QA still must pass. A green CI result proves build-time checks only; it does not prove real-device upgrade safety, correct balances, or customer readiness.

### P0 — Four Android tiers are not proven by this workflow

The checked Android Gradle configuration declares one application ID and the current release workflow builds one generic release configuration. That workflow alone does not prove independent Basic, Pro, Business and Enterprise packages, entitlements, application IDs, signing compatibility, or data separation. Validate each distributed APK/AAB independently before claiming tier parity.

### P0 — Windows parity

The checked Windows workflow is labeled as an older 2.2.8 expert-test candidate. Its smoke test only verifies that the process stays alive briefly. It does not establish current-version parity, real accounting correctness, license enforcement, backup/restore, or safe upgrades. Windows is not commercially cleared.

### P1 — Local database protection

The backup export path encrypts backups with AES-GCM and derives a key from a password. This is useful for exported backups, but the operational SQLite database itself is not encrypted by SQLCipher in the checked implementation. Do not claim full at-rest database encryption. Review local database exposure, Android backup policies, app sandbox assumptions, and recovery behavior.

### P1 — Backup/restore assurance

Restore validates AES-GCM authentication, SQLite integrity, required tables and database version before replacing the database. However, automated tests must still cover wrong passwords, truncation, tampering, corrupted files, restore failure midway, preservation of the previous database, WAL/SHM behavior, and reconciliation of financial totals after restore.

### P1 — Accounting test depth

Existing automated tests include database schema/integrity checks and some return-related assertions. They do not constitute a full accounting acceptance suite. Add deterministic tests for:
- debit equals credit for every posted journal;
- sales/purchases, partial payments, customer receipts, supplier payments, expenses and overpayments;
- inventory quantities, returns, cost of goods sold and weighted-average cost;
- trial balance, general ledger and profit calculations;
- duplicate invoice numbers, duplicate returns and concurrent submissions;
- decimal edge cases and zero-decimal currencies;
- database migration from every supported previous version.

### P1 — Licensing and payment

The Android client calls a remote license-status endpoint and caches a limited offline-grace result. Live end-to-end verification is still required for first activation, correct plan entitlement, expiry, revocation, device changes, offline grace, reinstall, clock rollback, failed requests, and manual payment approval. A working endpoint or passing unit test alone does not prove the commercial licensing lifecycle is correct. Keep sales closed until these tests pass.

### P1 — Currency semantics

The selector changes the displayed currency code; it does not convert historical balances or perform foreign exchange. The app should prevent users from accidentally changing currency on an active company without a clear warning or a documented migration/new-company workflow. Verify currency display on invoices, inventory, receipts, debts, reports, PDFs and exports.

### P1 — Public repository and release-key hygiene

GitHub currently reports this repository as public. Review all tracked files and history for accidentally committed credentials, personal data, customer data and signing material. An encrypted signing-key blob is not the same as a safe secret-management policy; keep the decryption password outside the repository, rotate any credential ever exposed in plaintext, and restrict release permissions. The repository visibility should be an intentional product decision, especially if source code is not intended to be public.

### P2 — Product and UX maturity

The core interface is concentrated in a large `MainActivity.java` file. Split data access, accounting services, validation, licensing, backup/restore and UI into testable components before adding complex workflows. Add accessibility checks, input validation, localized error states, audit trails, customer-facing privacy/retention policy, and controlled release notes.

## Pricing policy recorded in this branch

| Tier | SDG monthly | SDG annual | SDG permanent | USD monthly | USD annual | USD permanent |
|---|---:|---:|---:|---:|---:|---:|
| Basic | 25,000 | 250,000 | 150,000 | $9 | $90 | $249 |
| Pro | 45,000 | 450,000 | 300,000 | $19 | $190 | $499 |
| Business | 80,000 | 800,000 | 600,000 | $39 | $390 | $999 |
| Enterprise | 150,000 | 1,500,000 | 1,200,000 | $79 | $790 | $1,999 |

SDG is for Sudan via Bankak; USD is for international payment via IBAN. Do not auto-convert prices between currencies. Prices being approved does not mean sales or activation are open. Permanent licenses do not include future updates.

## Release gate

Do not mark the release ready until all of the following are evidenced:
1. CI build, automated tests, signing fingerprint check, package metadata and SHA-256 checks pass on the final commit.
2. Each of the four Android tiers is built and installed/updated on a real device with the expected package ID and signature, preserving valid data.
3. Accounting acceptance tests pass with independently reconciled expected results.
4. Backup/restore and migration tests pass, including corrupted-file and interrupted-restore cases.
5. Live license/payment scenarios pass, including expired and revoked licenses and offline behavior.
6. Windows is tested on a real Windows device with accounting, licensing, printing/export and backup/restore scenarios.
7. Expert review findings are logged, prioritized and closed or explicitly accepted.
8. Privacy, terms, support, refund, retention and release-download policies are ready before sales open.

**Current disposition: Continue technical review and expert testing. Do not claim perfection or commercial readiness yet.**


### P1 — Enterprise local PIN recovery

The Enterprise login PIN is stored locally in Android app preferences (`security`), separate from the server-side commercial license. The current code has no authenticated self-service PIN reset. A rejected old PIN after update is not, by itself, proof of a bad password or a licensing failure.

Safe recovery:
- If any data must be preserved, do not clear app storage or uninstall. Export a backup and verify that the backup file is available before attempting recovery.
- For a test installation with no data to keep, Android Settings → Apps → المحاسب الميزان Enterprise → Storage → Clear data resets the local PIN and database; the next launch should offer PIN creation. This is destructive to that app's local test data.
- Do not add a hidden PIN bypass or silently reset the PIN during an upgrade.
- Before any customer release, add and test a documented recovery workflow that preserves encrypted backups and does not allow unauthorized account takeover.

### P0 — Release signing-key continuity

The CI release workflow enforces a known SHA-256 signing-certificate fingerprint and the 2.4.1 signed build passed that check. Before future changes to keystore files or secrets, compare the certificate fingerprint against a trusted previously installed production APK and the current CI artifact. Never replace the signing key just to solve a login/PIN issue: a changed certificate can block Android updates and strand installed customers.

### P1 — Public review download workflow (historical issue, now resolved for review candidates)

The old publication workflow expected Android 2.4.0 artifact names and failed. The gated workflow has since been merged to `main`, and the latest matching successful Android 2.4.1 and Windows 2.2.8 builds were published as the review-only prerelease documented in the latest verification section below. This resolves availability of public review downloads; it does not approve commercial sales.


## Latest verification update — 2026-10-18 14:15 UTC

This section supersedes the older publication-workflow status above where they conflict.

### CI and publication facts now verified

- PR #34 has been merged into `main`; merge source head: `42ab8b87d17ba090b2e8b9611c732a87cf8541b8`.
- Main commit reviewed: `a9d5c10f51a02abc93232a7a7b28513ca6f7874c`.
- Android signed build 2.4.1 succeeded: [run 39134945660](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39134945660). Database integrity checks, signed APK/AAB generation, final release QA, and artifact upload all completed successfully.
- Windows 2.2.8 candidate succeeded: [run 39134945696](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39134945696). Package structure, executable hash manifest, and an eight-second process-alive smoke test passed. This is not a real-device functional or accounting acceptance test.
- GitHub Pages deployment succeeded: [run 39134945668](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39134945668).
- Static site checks succeeded: [run 39134945648](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39134945648).
- Security and knowledge-content audit succeeded: [run 39134945646](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39134945646).
- Android regression tests on the merged source branch succeeded: [run 39134791071](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39134791071).
- The gated public review release was published as a prerelease, not a commercial release: [expert-review release](https://github.com/moh1442010-arch/smart-encyclopedias-platform/releases/tag/al-mizan-expert-review-2026-10-18). It contains Android APK/AAB, Windows EXE/ZIP, review notice, SHA-256 manifest and signing-certificate information.
- Public review landing page: https://moh1442010-arch.github.io/smart-encyclopedias-platform/downloads/
- Customer feedback pages: https://moh1442010-arch.github.io/smart-encyclopedias-platform/reviews.html and https://moh1442010-arch.github.io/smart-encyclopedias-platform/complaints.html
- Commercial sales remain closed. The review release is explicitly labelled NOT FOR SALE.

### Manual device evidence supplied by the project owner

The owner reports that after clearing local test data on the Enterprise installation, they created a new PIN, opened the app successfully, and repeated the previously described inventory/sales/partial-payment scenario. They report the expected results and fast, correct behavior. This is useful positive manual evidence for that scenario, but it does not replace independent review, migration tests, or full accounting acceptance.

### Remaining release blockers — do not bypass

1. **Financial precision migration:** monetary storage and calculations still use SQLite `REAL` and Java `double`. Design and test a versioned migration to integer minor units or a decimal-safe model; reconcile old/new totals and test rollback/restore before applying to any customer database.
2. **Four independent Android tiers:** current release workflow produces one generic Android package. Build and verify Basic, Pro, Business and Enterprise packages/entitlements explicitly, or revise the product offer so it does not claim independent packages/features that do not exist.
3. **Enterprise PIN recovery:** clearing app data resolved the owner's test install, but it destroys local database/preferences and is not an acceptable customer recovery flow. Provide a documented recovery route that preserves recoverable data without a hidden bypass.
4. **Windows acceptance:** the 2.2.8 candidate is old relative to Android 2.4.1. Test on a real Windows machine for login/PIN, company setup, accounting, inventory, reports, backup/restore, licensing and update behavior. Confirm the intended Windows release/version before selling.
5. **Accounting coverage:** add deterministic tests for every posted journal balancing, purchase/sale, partial payments, receipts, supplier payments, expenses, returns, COGS/costing, stock movement, trial balance, ledger, profit, duplicate submissions, concurrency, decimal and zero-decimal currencies, and migrations from each supported version.
6. **Backup/restore:** test wrong password, tampering, truncation, corrupt SQLite, interrupted restore, prior-database preservation, WAL/SHM handling and post-restore reconciliation.
7. **Live licensing/payment:** verify activation, tier entitlements, expiry, revocation, offline grace, reinstall, device changes, clock rollback, network errors and manual payment approval against the live service. Do not accept screenshots or unit tests as proof of end-to-end payment.
8. **Currency and tax:** company currency selection does not convert balances. Prevent accidental currency changes on an active company or require a clear confirmation/workflow. Tax percentage is currently stored as a setting and is not automatically applied to invoices; do not advertise automated tax compliance.
9. **Signing/update continuity:** compare the published candidate certificate fingerprint to the trusted installed production APK before asking customers to update. Preserve the release key and verify update-over-install without losing data.
10. **Privacy and operational policy:** publish clear privacy/data retention, backup, support, license-expiry/export, refund/payment-review, and vulnerability-reporting policies. Audit repository history and secrets before any broad public source exposure.
11. **Independent reviewer sign-off:** collect reproducible reports from the accountant reviewers for Android and Windows, with device/OS, build SHA, steps, expected/actual result, screenshots or sanitized evidence, and severity. No fabricated ratings or testimonials.
12. **Release process:** retain the prerelease channel for review only. Commercial publication must be a separate explicit approval after all P0 blockers close and evidence is recorded.

### Recommended Enterprise differentiator

Prioritize a **Financial Control & Period-Close Center** (suggested home icon: `shield-check` or `clipboard-check`). It should combine a month-end close checklist, cash/bank reconciliation, inventory-to-ledger reconciliation, trial-balance checks, unposted/imbalanced journal warnings, overdue receivables/payables, backup verification, and a tamper-evident audit trail. Every warning must link to the underlying record and must not silently alter data. This should be implemented only after the accounting posting model and monetary precision are stabilized.

### Commercial release decision

**Current decision: NOT APPROVED FOR SALE.** The build and review-download pipeline are functioning, and the public expert-review page is available. Commercial readiness still depends on closing the financial precision, tier packaging, live licensing/payment, backup/restore, Windows parity, security/privacy and independent accounting sign-off gates above.


## Execution update — 2026-10-18: two-phase closeout and Enterprise differentiator

### Phase 1 — source repair and Enterprise control-center candidate

- Added a **read-only Enterprise Financial Control & Period-Close Center** to the Android dashboard on branch `feature/enterprise-financial-control-center`.
- Initial checks display aggregate debit/credit difference, count of unbalanced journal entries, selected transaction types without a linked journal, sales receivables older than 30 days (an age-of-transaction proxy, not contractual aging), and products at/below reorder threshold.
- Added navigation to the relevant accounting, inventory, customer and backup screens. The screen does not post, alter, or close any records.
- Explicitly labels backup verification and automated period locking as not implemented; no false “all clear” is inferred from missing data.
- The current generic Android package does **not** yet enforce Enterprise-only entitlement. This change is a feature candidate and must not be marketed as exclusive to Enterprise until package/plan enforcement is implemented and tested.
- The source change has been committed on a review branch and must pass Android CI before it is considered build-verified.

### Phase 2 — evidence-based release gate

The latest main-branch Android signed build for report-only commit `3cbfa4aa07899da2856d4ff8735f53d7376dbf6e` passed all configured CI steps: database integrity tests, release APK/AAB build and signing, clean-install test APK build, final release QA and artifact upload ([run 39135445230](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39135445230)). This does not test the new branch feature or prove real-device/accounting acceptance.

Commercial sales remain closed until the following critical evidence is obtained:
1. Decimal-safe financial storage/migration plan with old/new reconciliation and rollback tests.
2. Independently validated accounting cases, including returns, partial payments, costs, trial balance, ledger and profit.
3. Backup/restore failure matrix and customer-safe PIN recovery that preserves data.
4. Live license lifecycle tests and verified payment approval workflow.
5. Signing-certificate continuity against a trusted previously installed customer build.
6. Separate tier/package/entitlement proof for Basic, Pro, Business and Enterprise.
7. Expert acceptance and privacy/support/refund/retention policies.

Windows: automated package and short process-alive smoke checks are available, but they are not a substitute for an actual Windows functional/accounting review. Do not claim Windows has passed customer acceptance merely because an executable can be launched in CI.

**Disposition:** review candidate only; no commercial release, no paid checkout activation, and no claims of full protection or complete accounting certification.


## Execution update — 2026-10-18 14:52 UTC

### Current verified source and CI state

- PR #36, **Gate Enterprise control center by license entitlement**, was merged to `main` as commit `bbe3afd4ecf5c1ebcfac0cf2f351db18ff1aec6e`.
- The Android PR regression test run `39136934066` and signed Android build run `39136923489` both passed for the entitlement change.
- The live Worker source inspected in `delivery/worker.mjs` returns `plan` and `termType` from both app-license status and activation endpoints. This verifies the expected response fields in source, not the complete live customer lifecycle.
- The main-branch signed Android build after the merge passed: [run 39137622537](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39137622537). The run uploaded the clean-install test APK and signed 2.4.1 release artifacts. Artifact upload is not equivalent to real-device upgrade acceptance.
- The Enterprise control center now hides its entry point unless the cached license is licensed, identifies the Enterprise plan, is unexpired, and remains inside the existing 72-hour offline verification window. This is client-side UI gating; it does not establish tamper-proof entitlement enforcement or separate signed packages for all four tiers.
- PR #37 ([journal-integrity diagnostics](https://github.com/moh1442010-arch/smart-encyclopedias-platform/pull/37)) adds detection for empty/one-line/unbalanced journal entries and journal lines with missing parent entries or accounts. At the time of this update, its Android regression tests and signed build are still running; do not count this patch as merged or passed yet.

### Updated Enterprise assessment

The Financial Control & Period-Close Center is a useful read-only warning dashboard, not an automated accounting certification or period-close engine. It must not claim that the books are correct merely because aggregate debit and credit totals match. The added per-entry checks address empty and orphaned journal structures, but further acceptance is still required for reconciliation, inventory valuation, receivables/payables, backups and period locking. Backup verification and automated period locking remain unimplemented.

### Release disposition unchanged

**NOT APPROVED FOR SALE.** Financial precision migration, four-tier package/entitlement proof, customer-safe PIN recovery, full backup/restore failure testing, live licensing/payment acceptance, Windows functional acceptance, signing continuity, privacy/operational policies and independent accountant sign-off remain release gates. Do not enable paid checkout or describe the product as fully secure or fully accounting-certified.


## Execution update — 2026-10-18 15:04 UTC

### Verified changes since the prior update

- PR #37, [Strengthen Enterprise journal-integrity diagnostics](https://github.com/moh1442010-arch/smart-encyclopedias-platform/pull/37), is merged. The added diagnostics flag journal entries with fewer than two lines or debit/credit imbalance above the existing tolerance, and journal lines with a missing parent entry or account. Regression tests cover empty, one-line, balanced and orphaned records. This is detection, not automatic repair.
- PR #38, [Test Enterprise license plan and term-type contract](https://github.com/moh1442010-arch/smart-encyclopedias-platform/pull/38), is merged. Worker regression tests verify that an Enterprise perpetual license returns the same `plan` and `termType` on creation, activation and status.
- PR #39, [Validate Enterprise license term before unlocking controls](https://github.com/moh1442010-arch/smart-encyclopedias-platform/pull/39), is merged as `fcfdd738cc99fdaf7fdcdd2adf421ec2df6abca9`. Its Android regression tests and signed candidate build passed: [test run 39138293559](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39138293559), [signed build run 39138288038](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39138288038). The gate now rejects unknown term types, missing expiry for monthly/annual terms, expired terms and stale offline verification; a perpetual term must have no expiry.
- On the resulting main commit, static-site checks and security/content audit passed: [static checks 39138723153](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39138723153), [security/content audit 39138723206](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39138723206). The post-merge signed Android build was still in progress at the time of this record: [run 39138723310](https://github.com/moh1442010-arch/smart-encyclopedias-platform/actions/runs/39138723310).
- Current source audit confirms a single Android `applicationId` (`com.mohammedmustafa.smartcompany`) and one generic release configuration. The Windows workflow still identifies its candidate as 2.2.8. Neither is evidence of four independently validated tiers or Windows parity.

### Financial precision migration — explicit no-blind-conversion rule

The current database helper is schema version 10 and its `onUpgrade` calls the generic schema routine. Money-like columns are stored as SQLite `REAL`, and SQL parameters are bound through `bindDouble`. A safe migration cannot be a type rename alone. Before implementation, inventory every money field and every read/write/reporting path; define currency exponent rules (including zero-decimal currencies), conversion/rounding rules, migration markers, backup-before-migration, row counts, old/new per-table totals, journal debit/credit reconciliation, restart/interruption recovery, rollback, and upgrade tests from every supported database version. The migration must run in a transaction and preserve the original database until verification passes. Do not bump the database version or deploy a partial conversion before these gates and deterministic tests exist.

### Remaining owner/operator acceptance (not delegable to CI)

1. Install the post-merge signed APK over a trusted previously installed copy on a real Android device, without clearing app data; verify the app opens and all existing test records remain. If any test records matter, export a backup first.
2. Compare the signing certificate fingerprint of the APK with the trusted installed/previous release certificate before distributing any update.
3. Arrange real Windows-device testing for the intended Windows candidate; capture OS/build and run the accountant test script.
4. Obtain reviewer reports from the accountant panel with reproducible steps and sanitized evidence; record pass/fail per case.
5. Only after engineering gates close, approve a separate explicit commercial launch and payment enablement.

### Release decision

**NOT APPROVED FOR SALE.** These merged changes improve the Enterprise warning dashboard and entitlement input validation; they do not close monetary precision, full backup/restore, customer-safe PIN recovery, live license/payment lifecycle, four-tier packaging, Windows acceptance, full accounting test coverage, signing continuity, privacy/operational policy or independent accountant sign-off. Keep the current prerelease review-only and do not enable paid checkout.
