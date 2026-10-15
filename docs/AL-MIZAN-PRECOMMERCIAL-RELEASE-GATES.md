# Al-Mizan pre-commercial release gates

This checklist is intentionally strict. A green CI build is not equivalent to an approved accounting product.

## Current release gates

- [ ] **Signing continuity:** Compare the SHA-256 signing certificate in the new signed APK with the certificate of the currently installed APK. Android updates require the same application ID and signing certificate and a higher version code. Do not uninstall the installed app as a workaround during this test.
- [ ] **Install-over-existing:** On a test device, install the prior release, create sample records, install the new release over it, and verify that records and balances remain intact. Record the old/new certificate fingerprints and Android installer result.
- [ ] **Server-side licensing:** Test new-device trial creation, repeat status calls, seven-day expiry, invalid activation codes, valid activation, device binding, monthly expiry, annual expiry, perpetual licenses, suspension/revocation, clock changes, reinstall, and the 72-hour offline grace period. Use test devices and test license codes only.
- [ ] **Data at rest:** The Android database currently uses the platform SQLite helper and REAL/Double money values. Do not store customer production data until a reviewed encryption-at-rest migration, secure key handling, encrypted backup/restore, migration rollback, and data-integrity tests are implemented. Do not merely add a SQLCipher dependency without migrating the database access layer and existing data.
- [ ] **Accounting correctness:** Independently trace sales, purchases, inventory valuation, COGS, returns, partial payments, expenses, customer/supplier balances, checks, payroll, journal, ledger, trial balance, and profit reports. Confirm every posted journal entry balances and no event posts twice.
- [ ] **Windows parity:** The Windows build is pre-commercial. Verify licensing enforcement, database encryption, backup/restore, report calculations, printing, update behavior, and Arabic/English on Windows separately; Android tests do not certify Windows.
- [ ] **External review:** Obtain signed-off results from an accountant and a device-based tester. Record the exact app version, test data, expected result, actual result, and evidence for each test.

## Android signing fingerprint

The Android release workflow now emits:
- `SIGNING-CERTIFICATE.txt` — verbose `apksigner` certificate output.
- `SIGNING-CERT-SHA256.txt` — normalized SHA-256 certificate fingerprint.

Inspection of the signed 2.3.4 and 2.3.5 APK artifacts found the same certificate fingerprint: `159f40945e9afbba77bb58ffbbcc0c73bf247a6f4e630463b8c1fe5ec60b0279`. The 2.3.6 release workflow pins this verified fingerprint and fails if a future APK is signed with a different certificate. This protects continuity for the official 2.3.4/2.3.5 release line, but it does not prove that the APK installed on the user's phone was signed by that key; compare the actual prior installed package or its archived APK if available.

Never replace a signing key or delete the old installed application merely to make an update succeed. If the old release was signed by another key and its original private key is unavailable, Android generally cannot update that installation in place; a migration/reinstall procedure with verified backup may be required.

## Trial and license regression tests

Run locally with Node.js 22 or through GitHub Actions:

```sh
node --test delivery/tests/app-license.test.mjs
```

The tests exercise the Worker handler with a deterministic in-memory D1 test double. They are regression tests, not proof of live Cloudflare D1 deployment or production configuration. The live service still needs a non-destructive smoke test using designated test device IDs and approved test licenses.

## Release decision

Until all checkboxes are evidenced, label artifacts **test / pre-commercial**. Do not describe the product as fully secure, accounting-certified, or ready for public sale.