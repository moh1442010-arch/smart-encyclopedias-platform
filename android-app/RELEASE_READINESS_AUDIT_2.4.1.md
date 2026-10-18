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
