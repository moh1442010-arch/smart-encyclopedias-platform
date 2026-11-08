# Al-Mizan Bakery — licensing and activation

## Source-of-truth version state

- Android package: com.mohammedmustafa.almizan.bakery.
- Licensing Worker: al-mizan-bakery-licensing.
- Production Worker URL: https://al-mizan-bakery-licensing.mohammedmustafa.workers.dev.
- D1 database: al-mizan-bakery-licenses, binding DB.
- Worker configuration on this branch: APP_VERSION = "2.8.1", PREVIOUS_APP_VERSION = "2.8.0".
- The current main Android Gradle/build workflow still describes 2.6.17. Do not infer that the 2.8.1 app source, build, and signing identity are aligned from the Worker version alone. Resolve that source mismatch in the integration work before any APK build.
- The production state of the signing pair and admin password is not verified by this repository document. Check live diagnostic endpoints and deployment workflow results; never infer readiness from a configured secret or a passing local unit test.

## Five workstreams required before release

1. Live licensing and activation — health, admin configuration, signing-pair diagnostics, 2.8.1 and 2.8.0 compatibility, pending order creation, authenticated review, token issuance only after verified payment, app-side signature/device/version/expiry checks.
2. Independent data and migration — independent bakery/factory data, party masters, ledgers, exports, restores, and migration tests. Licensing data must not be coupled to operational bakery/factory databases.
3. Accounting integrity — operational source documents post idempotently to each system's own balanced ledger; sales, returns, inventory consumption, spoilage, payroll, expenses and cash collection reconcile without double counting.
4. Security, identity and localization — salted PIN hash, rate limiting and safe recovery; independent bakery/factory names; Arabic and English across all screens, receipts, reports and offline guide.
5. Release engineering and acceptance — version and certificate consistency, API 23+ tests, migration/backup/restore tests, low-end Android and physical-device acceptance, checksums, and a documented rollback path.

Detailed acceptance gates: licensing/RELEASE_ACCEPTANCE.md. The licensing CI checks both the 2.8.1 candidate and 2.8.0 compatibility branch, including exact active-key extraction that excludes legacy verification keys.

## Customer licensing flow

1. Customer submits business name, contact, plan, device ID and transfer reference through the public licensing page.
2. Worker validates fields, supported app version, rate limits and request origin. It creates a pending order only; it does not issue a token.
3. The owner verifies the actual incoming payment independently in the bank/Bankak account.
4. An authenticated administrator approves or rejects the pending order.
5. Only an approved order receives a signed activation token.
6. The customer enters the token into the matching app/device. The app validates signature, device, version and expiry constraints.

A transfer reference or uploaded receipt is not proof that money arrived. Never approve an order merely to test the flow. Use a dedicated test plan and controlled test device; do not expose real customer tokens in logs, issues or screenshots.

## Current SDG plan rules

| Plan | Amount | Validity |
|---|---:|---|
| Monthly | 55,000 SDG | 30 days |
| Annual | 550,000 SDG | 365 days |
| Lifetime | 1,050,000 SDG | Per configured version/device entitlement; confirm exact Worker policy before sale |

The Worker source is authoritative for accepted plan amounts and entitlement fields. Do not copy old pricing or entitlement claims into the storefront without checking the Worker and app implementation together.

## Signing architecture and secret handling

- The Cloudflare Worker is the online signing authority. Its private key must exist only as a protected Worker secret.
- The app embeds the corresponding public key. The public key must remain byte-for-byte consistent between app, Worker diagnostics and any local recovery verifier.
- Do not commit private keys, admin passwords, activation tokens, customer receipts or bank account details.
- The local offline issuer is a controlled recovery utility, not a second public issuer. Use it only on a trusted device with the matching private key stored outside the repository.
- Never generate a replacement key pair as a shortcut for a mismatch; that would invalidate tokens signed by the existing key.

## Required live checks

- GET /health: Worker is reachable and reports the expected service and manual-payment mode.
- GET /diagnostics/admin-config: admin password is configured.
- GET /diagnostics/signing-config: key configuration and encoding metadata are valid.
- GET /diagnostics/signing-pair: Worker imports its private key, signs a fixed probe and verifies it against the app public key; expected signingReady: true and signingPairMatches: true.
- Admin login works, and all admin endpoints reject unauthenticated requests.
- A controlled test order appears as pending in the admin panel; public status never reveals customer details or activation tokens.
- Approval is impossible without an authenticated administrator and explicit review. Rejection never issues a token.
- A token is accepted only by the intended device and allowed app version; tampered, expired, wrong-device and unsupported-version tokens are rejected.
- Monthly and annual expiry, lifetime entitlement, offline validation and renewal behavior are verified on a physical test device.

The current GitHub Actions licensing workflow validates source syntax, unit contracts, SQL schema, storefront secret hygiene, and version/public-key consistency. These automated checks do not prove live Cloudflare secret configuration, successful deployment, or physical-device activation.

## No-build release gate

Do not build or publish an APK until the five workstreams in licensing/RELEASE_ACCEPTANCE.md have evidence and the owner authorizes the release. A green licensing unit-test workflow alone is not a release approval.
