# Smart Company commercial license API — first implementation

## Current scope
This release adds a server-side license registry and seven-day trial API to the existing Cloudflare Worker, and connects the Android app to it. The Worker uses the existing D1 binding in `delivery/wrangler.jsonc`; it creates `app_licenses` and `app_trials` tables on first request. No manual SQL migration is required for these two tables.

API base URL: `https://smart-encyclopedias-delivery.moh1442010.workers.dev`

## Endpoints
- `GET /api/app-license/status` — requires `x-app-device-id`. Creates a server-recorded seven-day trial on first check for that device identifier; returns `trial`, `expired`, or `licensed`.
- `POST /api/app-license/activate` — requires `x-app-device-id` and JSON `{"activationCode":"SC-..." }`. Binds a valid license to one Android ID.
- `POST /api/app-license/admin/create` — owner-only. Requires header `x-admin-key: <LICENSE_ADMIN_KEY>` and JSON `approved: true`, `plan` (`basic|pro|business|enterprise`), `termType` (`perpetual|monthly|annual`), and `customerName`. Optional fields: `licenseId`, `customerEmail`, `orderId`, `approvedBy`. The response includes the activation code once; only its SHA-256 hash is stored.

## Issuing a license
1. Confirm payment outside the app and record the order reference.
2. Call the admin endpoint from a private trusted environment. Never place `LICENSE_ADMIN_KEY` in the Android app, website JavaScript, a public file, or a message to a buyer.
3. Send the returned activation code to the verified buyer over a private channel.
4. The buyer enters the code in the app's Commercial License screen. One license is bound to one Android ID; device transfers require an owner-controlled reset workflow, which is not yet implemented for app licenses.

Example request body (do not include a real secret here):
```json
{
  "approved": true,
  "plan": "pro",
  "termType": "annual",
  "customerName": "Customer Name",
  "customerEmail": "customer@example.com",
  "orderId": "ORDER-12345",
  "approvedBy": "owner"
}
```

## Important limitations before public sale
- This is a first licensing implementation, not a complete payment platform. Payments are still manually verified and license issuance is owner-controlled.
- Google Play Billing is not integrated. If selling app functionality through Google Play, review the current Play Billing policy and integrate its purchase verification/backend lifecycle appropriately.
- The app uses Android ID for device binding; this is not hardware DRM and may change in some device/profile scenarios.
- The app allows a cached valid entitlement for up to 72 hours without connectivity. Test clock changes, reinstall, device replacement, expired monthly/annual terms, and concurrent activation.
- Monthly and annual terms currently expire after 30 and 365 days respectively; this initial implementation does not yet process renewals or payment-provider webhooks.
- A perpetual license has no expiry but is tied to one device.
- Do not announce the commercial license system as production-ready until the Worker deployment is confirmed, API behavior is tested against the live D1 database, and release tests pass.
- The app must preserve company data and allow data export after trial/subscription expiry.
