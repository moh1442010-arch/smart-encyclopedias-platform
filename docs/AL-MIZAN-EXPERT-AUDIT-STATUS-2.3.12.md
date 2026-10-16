# Al-Mizan Accountant — Expert audit triage and repair log

- Target repair build: **2.3.12 (versionCode 44)**
- Base: the signed 2.3.11 source line from commit `6e1107c923e9a22a9a79b1399cf0fad518200860`
- Status: repair branch / pre-commercial. A successful CI build does not establish accounting correctness or on-device acceptance.

## Confirmed from current source

| Item | Finding | Status |
|---|---|---|
| C1 money precision | SQLite schema uses REAL for monetary fields (including journal debit/credit, invoice price/cost/discount, transaction totals and payments), and application logic uses Java double. This is a confirmed design risk; it does not by itself prove every displayed total is wrong. | Open — high risk |
| C2 local data protection | Manifest sets `android:allowBackup="false"` and `android:usesCleartextTraffic="false"`. The app still uses ordinary SQLiteOpenHelper and no SQLCipher dependency is declared, so the database file itself is not shown to be encrypted. | Partially addressed; encryption-at-rest decision/open |
| C3 licensing | App calls HTTPS server endpoints for `/api/app-license/status` and `/api/app-license/activate`, and implements a 72-hour local offline grace period. Local expiry and grace checks use the device wall clock; clock rollback/tamper resistance needs hardening and live testing. | Partially implemented; open |
| C4 identity | Application ID is `com.mohammedmustafa.smartcompany`; user-visible label is `المحاسب الميزان`; release certificate identity is Smart Company. Changing the application ID now risks breaking updates. | Decision: preserve package/signing continuity for 2.3.12; review public-facing brand separately |
| H1 PIN KDF | Existing PIN verifier uses PBKDF2-HMAC-SHA1 with 120,000 iterations. | Repair committed in this branch: PBKDF2-HMAC-SHA256 with 600,000 iterations; successful legacy verification upgrades the verifier. CI test added. |
| H2 audit trail | Source has duplicate-post prevention and a unique index intended to limit returns. A complete immutable audit log and safe reversal/edit workflow were not proven by the available tests. | Open — verify/implement |
| H3 backup | Backup code uses AES-GCM and PBKDF2-HMAC-SHA1 with 210,000 iterations. A restore gate accepted schema versions only up to 10 although the current DB helper is version 11, so current-version backups could be rejected. | Repair committed: restore gate now references the DB schema version and a regression test covers current/future versions; backup round-trip still needs device test |
| H4 migrations | SQLiteOpenHelper is at schema version 11; onUpgrade calls an idempotent schema method with multiple ALTER TABLE attempts guarded by catch-all exception handling. Existing test covers a simulated version 8 database, not all prior release schemas or data. | Open — add explicit version-path and data-preservation tests |
| P1 signing | Existing workflow pins the release certificate SHA-256 and verifies APK/AAB plus checksums. | Retain; confirm repair artifact after CI |
| P2 pricing/support | Price screen says test prices are not final and that purchases/auto-renewal are not available in this build. | Keep clearly labelled prelaunch; do not advertise payment automation as live |
| P3 functional tests | Current tests cover selected database and security behavior, but do not constitute T1–T20 end-to-end device tests. | Open |
| P4 privacy/docs | `allowBackup=false` is set. Privacy policy/terms exist on the repository site, but current accuracy, contact routing and operational support response need review. | Open |

## Repair changes in 2.3.12 branch

1. Upgraded new PIN verifiers to PBKDF2-HMAC-SHA256 / 600,000 iterations.
2. Added compatibility verification for the previous PBKDF2-HMAC-SHA1 / 120,000 verifier. A matching legacy PIN is re-saved using the current KDF; incorrect PINs do not trigger migration.
3. Added a regression test for legacy PIN verifier migration.
4. Bumped Android version to 2.3.12 / versionCode 44 and aligned signed workflow artifact names and package QA assertion.
5. Added a bounded offline-license check that rejects a materially rolled-back wall clock based on the last server-verified time, with tests for trial expiry and perpetual licenses. This is a local tamper signal, not a substitute for live server testing.\n6. Fixed the backup restore schema-version gate, which previously rejected schema 11 backups while the app database helper was already at version 11; added a regression test.\n7. Kept the existing application ID and signing certificate continuity checks.

## Required before commercial release

### C1 — exact money model
- Inventory every monetary column and every Java calculation/formatting path.
- Define the currency precision and rounding rules explicitly.
- Implement an upgrade-safe migration to integer minor units or an explicitly justified fixed-decimal representation; preserve quantity precision separately from currency precision.
- Add tests for 33.33 × 3, 10% discount, 15% tax, partial payments, COGS, returns, and at least 10,000 generated balanced entries.
- Compare journal, ledger, trial balance, stock valuation and profit reports after migration.
- Do not mark complete until the migration and tests run successfully on CI and a test device.

### C2/H3 — local database and backups
- Keep Android Auto Backup disabled unless explicit safe extraction rules are designed.
- Decide and document encryption-at-rest requirements. If adopting SQLCipher, migrate the database access layer and existing database atomically; adding a dependency alone is insufficient.
- Version the backup KDF/header and support restoration of previously created ALMIZAN1 backups.
- Test wrong password, corrupt file, interrupted restore, full restore, and rollback.

### C3 — licensing
- Confirm trial creation/expiry is server-authoritative and bound to the intended device/license record.
- Protect cached license state against wall-clock rollback and use a bounded offline policy. Do not treat local time as the authority.
- Run non-destructive live smoke tests with designated test device IDs and test license codes, including activation, expiry, revocation, reinstall and 72-hour offline behavior.

### H2/H4/P3/P4
- Add an auditable reversal/edit policy; do not silently mutate posted journal entries.
- Test each supported prior database version, including the user's previously installed version, with seeded records and balance comparisons.
- Execute the complete T1–T20 matrix on a device and attach expected/actual evidence.
- Verify contact details, privacy policy, terms, price currency policy and the single-user limitation before public marketing.

## Release gate
This branch is not approved for commercial sale merely because the workflow turns green. Approval requires:
- green tests and successful signed APK/AAB verification;
- unchanged package ID and verified signing certificate;
- accounting test evidence with zero unexplained imbalance;
- successful upgrade and backup/restore tests;
- licensing and data-protection review;
- device-based acceptance on Android 16;
- independent accountant sign-off.
