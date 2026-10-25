# Al-Mizan Bakery — store licensing service

This service is a prepared deployment scaffold, **not a live payment or activation service until configured and deployed**.

## Intended customer flow

1. Customer installs the Android APK and copies the device ID shown by the app.
2. Customer opens the store licensing page and submits name, contact, plan, device ID, and a Bankak transaction reference.
3. The order is stored as `pending`; the site never treats a receipt image or reference as proof of payment.
4. An administrator checks the actual incoming transfer, then approves or rejects the order through the protected admin API.
5. Approval signs a token in the exact payload format expected by `MainActivity.verifyLicenseToken()`.
6. Customer checks the order status in the store and copies the issued token into the app's existing **تفعيل الاشتراك** dialog.

Payment is manual Bankak review; this is not an integrated payment gateway and no automatic payment confirmation is claimed.

## Critical compatibility gate

The repository's current Android source/build configuration says version **2.5.8**, while earlier owner reports refer to **2.5.11**. The existing local issuer script also declares **2.5.7**. Do not issue or sell lifetime licenses until the actual distributed APK version, its embedded public key, and this service's `APP_VERSION` have been reconciled and tested together.

The app requires:
- app ID: `com.mohammedmustafa.almizan.bakery`
- RSA-SHA256 signature over UTF-8 payload
- payload format: `app=<app>;device=<device>;plan=<monthly|annual|lifetime>;max_version=<any|exact version>;expires=<unix seconds>`
- monthly/annual expiry currently follows 30/365 days
- lifetime token must set `expires=0` and `max_version` equal to `BuildConfig.VERSION_NAME`

## Deployment steps

1. Create a Cloudflare D1 database named `al-mizan-bakery-licenses`.
2. Apply `schema.sql` using Wrangler: `wrangler d1 execute al-mizan-bakery-licenses --remote --file=licensing/schema.sql` (run from repository root).
3. Replace `database_id` in `wrangler.toml` with the real ID.
4. Set secrets on the Worker:
   - `ADMIN_API_KEY`: long random admin-only secret.
   - `LICENSE_PRIVATE_KEY_PKCS8_B64`: base64 PKCS#8 DER private key that matches the public key embedded in the released APK.
   - `LICENSE_PUBLIC_KEY_DER_B64`: base64 DER SubjectPublicKeyInfo for the same key.
5. The protected browser admin console is `https://moh1442010-arch.github.io/smart-encyclopedias-platform/store/license-admin.html`; it is only a UI and is protected by the Worker admin key, not by its URL.\n6. Never put private key material in GitHub files, public HTML, client JavaScript, or chat. If the matching private key cannot be recovered securely, rotate the app key pair in a controlled new app release; do not generate unrelated tokens.
7. Set `STORE_ORIGIN` to the exact Pages origin, deploy the Worker, and replace the placeholder Worker URL in both `store/license.html` and `store/license-admin.html`.
8. Run the acceptance checklist below with a low-value test transaction before public sales.

## API

- `GET /health`
- `POST /api/orders` — creates pending order. Body: `customerName, contact, deviceId, plan, receiptReference`.
- `GET /api/orders/:uuid` — order status; token only appears after approval.
- `GET /admin/orders?status=pending` — requires `Authorization: Bearer <ADMIN_API_KEY>`.
- `POST /admin/orders/:uuid/approve` — manual payment verification required before use.
- `POST /admin/orders/:uuid/reject` — protected rejection.

## Security and release gates

- Worker has no payment-provider integration; Bankak payment must be checked manually.
- Do not expose the admin key in the storefront.
- Do not enable public activation until the Worker is deployed, key-pair check succeeds, D1 is configured, and the store page points to the real Worker URL.
- The current APK's app-side activation UI accepts a signed token pasted by the customer; it does not yet automatically deep-link/import a token from the browser.
- Test wrong-device token rejection, tampered token rejection, expired subscription, lifetime version mismatch, offline use, reinstall/device migration policy, admin authentication, and audit rows.
- Keep the D1 database and private key backed up separately and access-restricted.
