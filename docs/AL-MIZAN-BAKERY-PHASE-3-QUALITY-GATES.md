# Al-Mizan Bakery — Phase 3 Quality and Release Gates

This document defines the evidence required before the bakery application can be described as production-ready or distributed commercially. A source-code check or successful APK build is not a substitute for a real-device test.

## Release identity

- Application ID: `com.mohammedmustafa.almizan.bakery`
- Current review baseline: `2.8.0` / versionCode `280000`
- Minimum Android: API 23
- Target Android: API 35
- Distribution status: review-only until every required gate below is evidenced and approved.
- Preserve the application ID, signing lineage, database names, and monotonically increasing versionCode for upgrade compatibility.

## Gate A — installation, upgrade, and device compatibility

| Test | Required evidence | Status |
|---|---|---|
| Fresh install on API 23 | Install, launch, login, create and reopen a record | Pending physical-device evidence |
| Fresh install on API 35 | Install, launch, login, create and reopen a record | CI smoke test in progress at the time this checklist was added |
| Upgrade from the last customer release | Install over existing app; verify all ledgers, stock, settings, license state, and trial dates remain | Pending |
| Upgrade from 2.8.0 review build | Install newer signed build over 2.8.0; no uninstall or data reset | Pending |
| Low-storage and interrupted update | App remains recoverable; no partial database migration | Pending |
| Process death / force-stop / reboot | Reopen without crash or loss of committed transactions | Pending |
| Screen sizes and font scaling | No clipped primary actions or inaccessible fields | Pending |

## Gate B — accounting correctness

- All money values use integer minor units or exact integer/rational arithmetic; no binary floating-point money calculations.
- Sudanese pound (SDG) uses zero minor decimal places.
- Every posted journal is balanced in the base currency and retains transaction currency and exchange-rate numerator/denominator.
- Posting is atomic: header, lines, idempotency record, and audit event commit together or roll back together.
- Repeated submission of the same source/idempotency key must not duplicate a journal.
- A reused idempotency key with a different journal reference must be rejected.
- Closed or missing financial periods must reject posting.
- Drafts must not appear in posted financial reports.
- Trial balance, account statements, income statement, and balance-sheet accounts must be reconciled against independently calculated fixtures.
- Reversals must create a separately traceable reversing journal; posted entries must not be silently edited or deleted.
- Customer and supplier subledgers must reconcile to their control accounts.
- Inventory quantities and valuation must reconcile to stock movements, production consumption, spoilage, returns, and cost of goods sold.
- Spoilage must be recognized once, not deducted both from production yield and again as an expense unless the accounting policy explicitly requires separate cost treatment.

## Gate C — bakery and factory separation

- Bakery and factory databases remain separate, encrypted, and independently backed up.
- Each database migration is versioned, transactional, repeatable, and tested against a copy of a prior customer database.
- Inter-unit transfers use paired source/destination documents with one immutable transfer reference.
- A transfer cannot be posted on only one side.
- Factory work-in-progress must carry material, labor, and overhead costs by stage.
- Production recipes, quantities, unit conversions, returns, and waste require boundary and rounding tests.

## Gate D — security, licensing, and privacy

- SQLCipher encryption is verified on disk; migration must not leave an unencrypted duplicate database.
- Backup files are encrypted, authenticated, and rejected when tampered with or restored into an incompatible schema.
- No private signing key, payment secret, customer data, or real banking information may be committed to the repository.
- Monthly and annual plans must expire according to their approved terms; neither may become lifetime automatically.
- Lifetime entitlement is granted only after payment review and explicit owner approval.
- Expired entitlement must block protected operations without deleting customer data; backup/restore and support access follow the documented recovery policy.
- License checks must cover launch, foreground resume, protected activities, and offline behavior.
- No claim of tamper-proofing or guaranteed security without an independent security assessment.

## Gate E — usability, accessibility, and localization

- Arabic is the default language with correct RTL ordering; English uses LTR ordering.
- Every input has a persistent label, suitable input type, validation message, and keyboard behavior.
- Touch targets and contrast remain usable at common Android font scales.
- Back navigation does not discard unsaved data without confirmation.
- Empty, loading, error, success, and offline states are explicit.
- Company identity and selected branch remain visible where operational context requires them.
- Financial reports clearly identify currency, date range, branch, and whether values are posted-only.
- Tax fields are configurable by jurisdiction and are not presented as legal tax advice or hard-coded as a universal rate.

## Gate F — performance and reliability

- Test on low-memory devices and at least two Android vendors.
- Record cold-start time, memory use, database migration duration, and large-ledger report duration.
- Exercise at least 10,000 journal lines and a representative large stock ledger.
- Verify no ANR, unhandled exception, duplicate posting, or database corruption under repeated save/retry.
- Verify offline operation after first installation and after force-stop/reboot.
- Optional synchronization must use stable event IDs and conflict handling; it must never duplicate posted journals.

## Gate G — release integrity and customer handoff

- Signed review APK is the exact binary that passed API 23 and API 35 smoke tests.
- Publish SHA-256, signing-certificate fingerprint, versionName, versionCode, package ID, and build commit.
- Keep internal debug artifacts separate from customer review artifacts.
- Customer-facing download must be public and tested without maintainer authentication.
- Provide a tested upgrade path, backup instructions, privacy policy, support route, and release notes.
- Commercial release requires owner approval after review evidence is available.

## Current evidence snapshot

The CI run associated with commit `b4187367c9be2f6b9ef99802f443914cb703fbf4` reported successful arithmetic unit tests, APK build, signing/verification, and API 23 smoke test. API 35 smoke test and signed review artifact upload were still pending when this checklist was authored. This snapshot is historical; always inspect the latest workflow run before making a release decision.

## Completion rule

Do not mark Phase 3 complete while any required gate is pending, failed, or supported only by a source-code assertion where a runtime/device test is required. Do not merge this checklist or publish a commercial release as a substitute for passing the gates.
