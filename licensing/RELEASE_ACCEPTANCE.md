# Al-Mizan 2.9 — five workstreams and release acceptance

Status: execution plan and evidence register. No APK build is authorized by this document.

## Workstream 1 — Licensing and activation reliability

Scope:
- Align Android app version, Worker APP_VERSION/PREVIOUS_APP_VERSION, public key, local issuer and deployment assertions.
- Verify Cloudflare API token, D1 binding/schema, admin password, private-key encoding and signing-pair match without exposing secrets.
- Confirm the live Worker health, admin configuration and signing-pair diagnostics.
- Verify order submission creates exactly one pending order; public status exposes no customer details or token.
- Verify authenticated admin approval/rejection, audit records, signed token issuance, device binding, version compatibility and expiry.
- Keep manual payment review. Never auto-approve from a receipt reference.
- Test 2.8.1 and backward compatibility for 2.8.0 where intended.

Acceptance evidence:
- Green Worker unit/security tests.
- Successful deployment run with safe health/admin/signing checks.
- Controlled test order visible as pending in admin.
- Admin approval on a controlled test order issues a token that validates on the intended test device only.
- Rejection, wrong device, tampered token, expired token and unsupported app version are rejected.
- Monthly 30-day and annual 365-day expiry verified; lifetime entitlement verified against explicit policy.
- No secrets, customer tokens or payment data in repository or public logs.

Current known issue:
- Worker config is 2.8.1 / previous 2.8.0, but main Android Gradle/build workflow still identifies 2.6.17. This mismatch blocks release build and must be resolved from reviewed app source, not by editing version strings alone.

Related issue: #112 (closed after source merge; live acceptance still must be evidenced).

## Workstream 2 — Strict bakery/factory data isolation

Scope:
- Separate database, party master, cash/Bankak, receivables, stock, expenses, ledger, reports, backup and restore.
- Remove FactoryActivity calls to bakery PartyDirectory and all bakery party IDs from factory records.
- Add factory-only customer/supplier pickers with name/phone search and add-new action.
- Preserve historical factory party names/IDs using additive, factory-local migration.
- Split backups and restores; include all schema columns such as product unit.
- A legacy combined archive may be imported only after explicit selection of the target system; it must not couple the resulting systems.

Acceptance evidence:
- Automated tests prove bakery parties are invisible in factory and vice versa.
- Factory picker search by partial name/phone returns only factory rows.
- Each system retains its own balances after migration, export and restore.
- Restoring one system leaves the other database byte/logically unchanged.
- Relaunch and offline mode preserve selections and records.
- Migration tests use copied fixtures and assert row counts, balances and audit history.

Related issue: #115.

## Workstream 3 — Accounting integrity and operational posting

Scope:
- Keep a distinct posting service and general ledger per system.
- Post counter/distribution sales, cash, Bankak, receivables, returns, purchases, supplier settlements, raw-material issues, production, spoilage, payroll, expenses and asset events.
- Use source type + source ID + idempotency key; prevent duplicate posting.
- Enforce balanced journals; correct posted entries by reversal.
- Keep revenue and collections distinct. Revenue includes cash + Bankak + receivables. Receivables are not cash.
- Include counter and distribution in total revenue while preserving channel dimensions.
- Include consumed materials, labor, overhead, spoilage and approved price differences exactly once.
- Capital assets are not automatically expensed; depreciation is recognized over time.

Acceptance evidence:
- Reconciliation tests for counter + distribution = cash + Bankak + receivables.
- Cost and profit reports reconcile to source documents without double counting.
- Duplicate posting is rejected or idempotently returns the existing journal.
- Returns reverse revenue, receivables/cash and stock/COGS correctly.
- Each ledger balances and remains isolated from the other system.
- Trial balance, P&L, balance sheet and cash flow agree to underlying ledgers.

Related issues: #113 and #109.

## Workstream 4 — Security, identity, navigation and Arabic/English

Scope:
- Retain salted PIN derivation; no plaintext PIN or default credential.
- Add controlled show/hide, rate limiting/temporary lock, secure change/recovery that preserves business data.
- Store bakery and factory business identities separately; show the correct name on their own dashboards, invoices, reports and printouts.
- Persistent Arabic/English selection.
- Translate all screen labels, dialogs, validation, toast, subscription/licensing, backup/restore, reports, receipts and offline guide.
- Verify RTL/LTR and date/number formatting.
- Consistent Home and Back controls on all secondary screens.

Acceptance evidence:
- PIN tests for setup, correct/incorrect entry, repeated failures, lockout, change and recovery.
- No plaintext secrets in preferences, logs, crash reports or backups.
- Bakery name changes do not alter factory name and vice versa.
- Translation coverage test detects missing user-facing strings.
- Arabic RTL and English LTR screenshot/smoke tests cover every module and critical workflow.
- Offline guide loads and switches language without network.

Related issues: #117 and #118.

## Workstream 5 — Release engineering, compatibility and market readiness

Scope:
- One authoritative app version/versionCode and package identity.
- Verify signing certificate fingerprint against the already-installed customer release; never replace signing identity.
- Keep build workflow disabled from automatic push/PR release until all gates pass.
- API 23+ compatibility, low-memory/low-end Android, screen-size, keyboard, accessibility and performance checks.
- Unit, integration, migration, backup/restore, licensing and regression suites.
- Checksum and artifact provenance; rollback and recovery plan.
- Store page, pricing, support and update notes must match the actual tested build.
- Physical-device update test preserves existing data and subscription state.

Acceptance evidence:
- Green required CI jobs on the exact release commit.
- No unresolved critical/high defects.
- Version, package, public key and signing certificate verified.
- Upgrade over the current installed app preserves data, license, opening balances, transactions and backups.
- Fresh install and offline operation verified on physical devices.
- Owner explicitly approves the exact candidate before distribution.

## Current verified status on 2026-11-08

- Worker source configuration on main: 2.8.1, previous 2.8.0.
- main bakery Gradle/build workflow: stale 2.6.17; not aligned with Worker.
- Licensing README was stale 2.6.15; corrected in this integration branch.
- Live health/admin/signing diagnostics were not accessible from this execution environment; their production state remains unverified.
- Previous foundation unit run #42508545860 passed for the earlier source revision. Later factory identity commits #38ecdf and #39df469 had failed foundation workflow runs #42511371532 and #42511457977; inspect logs and repair before porting.
- Factory still calls bakery PartyDirectory on the reviewed development branch; strict isolation remains open.
- Full bilingual localization, complete per-system ledger posting and backup/restore separation remain open.
- No APK has been built or approved for this workstream.

## Work sequence

1. Close licensing mismatch and live signing/approval path.
2. Complete strict data isolation and migration/backup tests.
3. Complete accounting posting and reconciliation per system.
4. Complete security, identity, navigation and bilingual coverage.
5. Run full release/physical-device acceptance and request owner approval.

Do not mark a workstream complete from documentation alone. Attach source commit, test run and acceptance evidence.
