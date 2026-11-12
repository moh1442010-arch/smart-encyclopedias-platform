# Android commercial licensing: implementation gate

## Approved product behavior
- Seven-day trial measured as 168 hours from first activation.
- App data remains local and is never deleted when the trial or subscription ends.
- After expiry, show a subscription screen with WhatsApp contact details; do not expose paid workflows.
- App remains usable offline for licensed customers, subject to a defined offline grace period.
- Sales may start on WhatsApp with manual payment verification; WhatsApp is a sales channel, not a licensing authority.

## Required production design
1. Persist trial start, last observed time, install identifier, and license state using Android Keystore-backed encryption where supported.
2. Use monotonic elapsed time within a boot session and wall-clock rollback detection across sessions. Treat local-only clock checks as friction, not tamper-proof security.
3. Provide a licensing API (Cloudflare Worker + D1 is a candidate, not yet deployed) to register trials, verify payments, issue/renew/revoke licenses, and record device transfers.
4. Sign license payloads on the server with a private key that is never shipped in the app or committed to GitHub. Embed only the public verification key in the Android app.
5. Allow offline use of a valid signed license for a documented grace period; on reconnection, refresh the license. Do not require always-on connectivity.
6. Include a customer support flow to submit a company/license reference and receive a signed activation token over WhatsApp. A manually typed code must be cryptographically verified, not accepted based on a hard-coded secret in the APK.
7. Keep subscription expiry separate from database access. Preserve data export and backup/restore; provide a read-only/export path after expiry.

## Release blockers (do not claim commercial licensing is complete)
- A production license API and database have not been deployed.
- A signing key pair and secure private-key custody have not been provisioned.
- Payment verification/admin activation workflow has not been implemented.
- Trial policy unit tests exist as a foundation, but app integration and end-to-end tests remain.
- The Android release workflow currently fails at keystore password validation; no signed production APK is produced by that run.

## Mandatory acceptance tests
- first launch; 167h 59m; exactly 168h; expired while offline
- wall-clock moved backward and forward; reboot; clear app data; reinstall
- paid license issued, renewed, expired, revoked, transferred to a replacement phone
- offline grace starts/ends and reconnection refreshes status
- database, export, and restore remain available after subscription expiry
- successful release build, APK signature verification, package/version validation, checksum, and install/launch smoke test

## Security and customer-protection rule
Never delete accounting data to enforce a subscription. Do not treat a local preference or an unsigned token as proof of payment. Do not ship or advertise the subscription gate as production-ready until the backend, signed token verification, activation flow, and acceptance tests pass.
