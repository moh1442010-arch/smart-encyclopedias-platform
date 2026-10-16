# Al-Mizan Storefront — Commercial Release Gate

Date: 2026-10-16  
Status: **Pre-launch checklist; not a release approval.**

## Hard rule
No public download button, paid campaign, or “production-ready” claim until the exact artifact has passed the relevant gates and is listed in the approved release manifest.

## Android APK
- [ ] Release APK built from the reviewed source commit.
- [ ] Release signature verified with the expected production certificate.
- [ ] Certificate SHA-256 fingerprint documented; keystore and passwords never committed in plaintext.
- [ ] Fresh install and update from previous signed release tested on a real device.
- [ ] App launches and all primary navigation/back-to-home paths work.
- [ ] Arabic RTL and English switching checked.
- [ ] Accounting acceptance suite reconciles source documents, stock, customer/supplier balances, cash/bank, journal, ledger, trial balance, P&L, and balance sheet.
- [ ] Offline operation, backup, restore, export, and damaged-backup behavior tested.
- [ ] Licensing expiry, grace period, renewal, device change, and offline policy tested.
- [ ] Manifest includes version, commit, build date, size, SHA-256, signing certificate fingerprint, and support status.

## Windows client
- [ ] Windows client source and supported framework identified; no artifact is implied by a roadmap document.
- [ ] Installer/package built in CI from reviewed source.
- [ ] Signing certificate decision documented; if unsigned, clearly label and do not call it verified publisher software.
- [ ] Install/uninstall/upgrade, permissions, backup/restore, printing, localization, and licensing tested on supported Windows versions.
- [ ] Shared accounting acceptance scenarios pass against the same specification as Android.
- [ ] Manifest and SHA-256 published.

## Storefront, security, privacy
- [ ] GitHub Pages deployment configuration checked before any merge.
- [ ] HTTPS and canonical URLs checked on mobile and desktop.
- [ ] Privacy and terms reviewed for the actual operator, jurisdictions, and data flows.
- [ ] No API secrets, keystores, private keys, credentials, or customer databases in public files or Git history.
- [ ] Release files stored separately from source; access permissions and retention documented.
- [ ] If using a CDN/object store, upload only approved artifacts and verify hashes after upload.
- [ ] Payment processor availability, supported countries, fees, chargebacks, tax, and refund obligations confirmed.
- [ ] Server-side licensing validates payment server-to-server; never trust a client-only “paid” flag.
- [ ] Idempotent order processing, signed license tokens, revocation/renewal, audit log, rate limits, and secret rotation tested.
- [ ] User data collection minimized; published retention/deletion contact exists and works.

## Launch marketing
- [ ] Store page and product screenshots reviewed for factual accuracy.
- [ ] Public link is tested from an external logged-out browser.
- [ ] Campaign says “request information / join waitlist” until download and licensing gates pass.
- [ ] No claim of error-free software, guaranteed financial results, or unavailable features.
- [ ] Campaign tracking is privacy-reviewed; do not embed third-party trackers without updating the privacy notice.

## Release manifest
Use release-manifest.json. Do not replace null or pending values with guessed links or hashes. A manifest entry is approved only after independent verification of the exact artifact.
