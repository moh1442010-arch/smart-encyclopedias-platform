# Smart Company — Commercial Sale & Transfer Readiness

**Purpose:** separate evidence that an Android package builds from evidence that the product is safe to operate, legally transferable, and commercially valuable. This checklist is a diligence aid, not legal advice or a certification of readiness.

## 1. Technical evidence

- [ ] Record the exact release tag, source commit SHA, Android application ID, version code/name, minimum/target SDK, and build tool versions.
- [ ] Preserve the signed APK/AAB, SHA-256 checksums, build logs, and a reproducible build procedure.
- [ ] Confirm the release signing key is controlled by the current owner, backed up securely, and transferable under a documented custody procedure. Never put keystore passwords or private keys in the repository or sale package.
- [ ] Review source code, permissions, exported components, network/API calls, secrets, dependencies, dependency licenses, known vulnerabilities, and software bill of materials.
- [ ] Test clean installation, upgrade from the previous signed release, offline operation, data migration, backup, restore, and rollback on supported physical devices.
- [ ] Reconcile accounting flows using independently calculated expected values. Build success, classes.dex, and Gradle metadata establish packaging/build evidence only; they do not prove accounting correctness, security, or commercial readiness.
- [ ] Verify every critical release gate in ACCOUNTING_RELEASE_GATE.md with dated evidence; do not mark untested checks as passed.

## 2. Accounting and data protection gates

- [ ] Complete a transactional migration of monetary persistence away from SQLite REAL and test old populated databases for exact balance preservation.
- [ ] Reconcile sales, purchases, returns, inventory valuation, customer/supplier balances, cash/bank, payroll, journal entries, profit, and trial balance.
- [ ] Implement and test cheque settlement, bounced-cheque reversals, re-settlement, and duplicate-posting prevention.
- [ ] Implement tax only with a defined country profile and reviewed invoice/legal requirements; otherwise explicitly limit the product scope.
- [ ] Implement and verify database and backup encryption, key handling, recovery, access control, and audit logging.
- [ ] Obtain acceptance results from the planned accountant reviewers, including severity-ranked findings and retest evidence.

## 3. Intellectual property and legal diligence

- [ ] Identify the legal seller and document chain of title for the source code, designs, icons, brand, website, documentation, and other deliverables.
- [ ] Verify employment/freelancer assignments, contributor permissions, and settlement of contractual obligations where applicable.
- [ ] Generate a third-party dependency and license inventory; review obligations for bundled open-source and commercial components.
- [ ] Search and assess relevant trademark/name conflicts; document ownership or permission for any third-party assets.
- [ ] Prepare a data inventory, privacy notice, terms of service, retention/deletion policy, and any required data-processing agreements.
- [ ] Have qualified counsel in the relevant jurisdictions review the sale/assignment agreement, warranties, liabilities, tax treatment, and any country-specific accounting/tax claims. Do not claim legal or tax compliance without documented review.

## 4. Accounts, service ownership, and transfer

- [ ] Inventory GitHub repositories, domains, DNS, hosting, backend/worker services, databases, monitoring, email, analytics, payment providers, app-store accounts, and third-party subscriptions.
- [ ] Determine which assets can be transferred and which provider accounts require a provider-approved transfer or new account. Do not share personal passwords.
- [ ] Transfer ownership through each provider's supported process; rotate secrets, API tokens, deployment keys, and recovery credentials after handover.
- [ ] Confirm the release signing-key strategy and continuity for future Android updates before any store or repository transfer.
- [ ] Document current service costs, renewals, access owners, incident contacts, and a rollback/continuity plan.

## 5. Handover package

- [ ] Complete source repository and commit/tag history.
- [ ] Build and deployment guide, environment/configuration inventory, and secret-free setup examples.
- [ ] Architecture overview, database schema, migration notes, backup/restore instructions, and API documentation where applicable.
- [ ] Test suite, latest CI links, device test matrix, known issues, security review, dependency/license inventory, and accounting acceptance records.
- [ ] User/admin guide, support process, release process, and roadmap.
- [ ] Asset manifest with exact filenames, versions, checksums, and transfer status.
- [ ] Signed transfer agreement, scope of assets, exclusions, acceptance criteria, and support/transition period.

## 6. Commercial valuation

- [ ] Separate verified revenue and retained customers from forecasts, downloads, followers, and uncontracted interest.
- [ ] Document recurring revenue, churn, paid conversion, support burden, hosting costs, refunds, taxes, and customer concentration where real records exist.
- [ ] If revenue history is absent, value the asset using a documented replacement-cost/effort analysis and risk-adjusted market comparables—not an unsupported revenue multiple.
- [ ] Deduct remediation, legal review, security hardening, support commitments, and unresolved technical debt from the buyer's risk assessment.
- [ ] Present base, downside, and upside scenarios with assumptions and evidence.

## Current status (as of 2026-10-14)

- Android signed release workflow 2.3.1 passed automated database-integrity tests, release build, signing, final release QA, and artifact upload on commit 8ae1b69afb0088436e0578b44bb1b7c8cfc9af56.
- Static-site checks and security/content audit passed on PR head f8195d3a2c92c4b10e45d9b0754a090dd53d6341.
- These are CI results, not proof of physical-device acceptance or commercial readiness.
- Known open blockers include SQLite monetary columns stored as REAL, full accounting reconciliation, cheque settlement/reversal workflow, tax engine/country compliance, SQLCipher/database and backup encryption verification, populated-database migration/restore testing, real-device install/update testing, and accountant acceptance.
- IP ownership, trademark clearance, privacy/legal review, app-store transfer eligibility, revenue history, and buyer valuation have not been independently verified in this repository.

## Decision rule

**Do not represent the app as 100% ready for sale until the applicable technical and legal checks have evidence and all material exclusions are disclosed.** A transfer of source code is not automatically a transfer of store accounts, signing identity, backend services, customer data rights, trademarks, or third-party licenses.
