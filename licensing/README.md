# Al-Mizan Bakery — licensing and activation

## Current release state

- App: **2.6.15** (com.mohammedmustafa.almizan.bakery).
- Worker: **al-mizan-bakery-licensing**.
- Worker URL: https://al-mizan-bakery-licensing.moh1442010.workers.dev.
- Database: Cloudflare D1, binding DB, database al-mizan-bakery-licenses.
- Payment review: manual Bankak verification by the owner.
- Activation: not ready for customer issuance until the live signing-pair diagnostic returns signingReady: true.

The Worker is deployed and its health/admin configuration checks have passed. The last production signing diagnostic returned privateKeyFormat: invalid-encoding. Therefore do not approve or issue a customer license yet. A configured secret is not proof that the key can be imported or that the pair matches.

## Approved customer flow

1. Customer installs the 2.6.15 APK and copies the device UUID displayed by the app.
2. Customer submits name, contact, plan, device UUID, and Bankak transfer reference at store/license.html.
3. Worker stores the order as pending. A reference is not proof of payment.
4. Owner checks the actual incoming transfer in Bankak.
5. Owner signs in through the Worker admin login, reviews the order, and explicitly approves or rejects it.
6. Only an approved order receives a token. The customer pastes that token into the app's activation dialog.

There is no payment gateway and no automatic payment confirmation. The owner must verify receipt of funds before approval. Never approve a pending order just to test the system.

## Fixed plans and prices

| Plan | Price | Token validity |
|---|---:|---|
| Monthly | 55,000 SDG | 30 days |
| Annual | 550,000 SDG | 365 days |
| Lifetime | 1,050,000 SDG | Current app version only; no future-version entitlement |

The Worker enforces these amounts. Lifetime tokens use expires=0 and max_version=2.6.15. Monthly and annual tokens use max_version=any.

## Signing architecture — one official issuer

The Cloudflare Worker is the only official online issuer. Its private key is held only in the Worker secret LICENSE_PRIVATE_KEY_PKCS8_B64. The public SPKI DER Base64 value is LICENSE_PUBLIC_KEY_DER_B64 and must remain identical to the public key embedded in the APK.

GitHub Actions no longer stores a second private-key copy or issues customer tokens. The retired public Actions issuer was removed to avoid exposing activation tokens in public workflow summaries. The local offline issuer tool remains a controlled recovery utility and must only be used on a trusted device with the matching private key stored outside the repository.

Never put a private key, activation token, admin password, or customer payment evidence in source files, public issues, or chat.

## Worker secrets and settings

Required Worker secrets:
- ADMIN_PASSWORD: at least 32 characters. The admin login uses a secure HttpOnly session cookie.
- LICENSE_PRIVATE_KEY_PKCS8_B64: Base64 of the binary PKCS#8 DER private key. It must be one-line Base64.
- LICENSE_PUBLIC_KEY_DER_B64: Base64 of the DER SubjectPublicKeyInfo public key embedded in the APK.

Worker variables in licensing/worker/wrangler.toml:
- APP_VERSION = "2.6.15"
- STORE_ORIGIN = "https://moh1442010-arch.github.io"

Do not change the public key or generate a replacement key pair for this repair. Convert the existing matching PEM private key to PKCS#8 DER Base64 and update only the Cloudflare private-key secret.

## Safe diagnostics

- GET /health — confirms Worker availability only.
- GET /diagnostics/admin-config — confirms that the admin password secret is configured.
- GET /diagnostics/signing-config — reports safe key configuration and encoding metadata.
- GET /diagnostics/signing-pair — imports the private key, signs a fixed test message, and verifies it against the app public key. It never returns key material or issues a customer token.
- GET /admin/signing-check — same signing-pair check, but requires an authenticated admin session.

A successful health check or privateKeyConfigured: true is not sufficient. Release gate is signingReady: true and signingPairMatches: true.

## Database

The Worker uses the existing D1 database configured in licensing/worker/wrangler.toml. Do not create a replacement database or change the database ID during key repair. Before any schema migration, inspect the live database and back it up.

Tables:
- license_orders — customer orders and activation status.
- license_audit — order creation, review, rejection, and issuance events.

## Acceptance checklist before sales

- [ ] Worker health returns ok: true.
- [ ] Admin login succeeds and admin endpoints reject unauthenticated requests.
- [ ] Live signing-pair diagnostic returns signingReady: true.
- [ ] Public key consistency passes across app, Worker, and local issuer.
- [ ] Test order can be created and appears as pending.
- [ ] Pending order status exposes no activation token.
- [ ] Owner can reject a test order without issuing a token.
- [ ] Owner can approve only a verified test payment; token verifies on the exact test device.
- [ ] Wrong-device and modified tokens are rejected by the APK.
- [ ] Monthly/annual expiry and lifetime version restriction are verified on a real device.
- [ ] Backup/restore and offline behavior are checked.
- [ ] Public product page and download link are checked on Android.

Until the complete checklist is passed, keep paid advertising and claims of commercial activation readiness on hold.
