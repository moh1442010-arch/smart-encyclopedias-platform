# Al-Mizan release recovery baseline — 2026-10-21

## Purpose
Preserve a reproducible project baseline and a clear recovery path if compatibility with the previously installed Android 2.3.14 cannot be proven. This document is a technical checkpoint, not a claim that a new APK has been built or device-tested.

## Protected state
- Do not uninstall Android 2.3.14, clear its data, or overwrite its user data while diagnosing update compatibility.
- Never commit a keystore, private key, keystore password, signing secret, activation secret, or customer accounting data.
- Keep the existing `main` history and prior release artifacts intact. Work on a branch and review changes before merging.
- Do not enable commercial sales based only on a successful CI build.

## Current Android candidate recorded in repository
- Product: المحاسب الميزان Enterprise
- Package ID: `com.mohammedmustafa.smartcompany`
- Candidate version: `2.4.3`
- Version code: `2004003`
- Review tag recorded in `android-release-manifest.json`: `al-mizan-android-review-2.4.3-2026-10-21`
- Manifest APK SHA-256 recorded in repository: `4f7d6ccd794a21ac03ca03f35650da4608e7c53e7dd8f85ce3d3285fe8ee4dae`
- Expected release certificate SHA-256 recorded in build/manifest: `159f40945e9afbba77bb58ffbbcc0c73bf247a6f4e630463b8c1fe5ec60b0279`
- These values are repository metadata; the APK artifact and live URL must be re-fetched and independently checked before distributing.

## Findings that must be resolved
1. `.github/workflows/diagnose-android-upgrade-install.yml` tests a 2.4.0-to-2.4.1 emulator update, not the reported 2.3.14-to-2.4.3 path.
2. `store/release-manifest.json` still records 2.4.2 as its Android candidate, while `android-release-manifest.json` records 2.4.3.
3. `.github/workflows/build-android-apk.yml` is named for 2.4.2 but currently builds artifacts named 2.4.3; the publisher listens for the exact 2.4.2 workflow name.
4. The certificate expected by CI is not proof that the installed 2.3.14 APK uses that certificate. Compare actual signer fingerprints from both APKs.
5. A published manifest/hash is not proof that the artifact is installable on a physical device or that update preserves local data.

## Required recovery gates
- Fetch the exact release APK and verify its SHA-256 against the release metadata.
- Verify package ID, versionName, versionCode, and actual signer certificate from the APK itself.
- Establish the signer certificate of the installed 2.3.14 build using a trusted APK artifact or device-side package inspection; do not request or expose private signing material.
- If signer compatibility is confirmed, test fresh install and in-place update in an emulator and on a physical device where possible, checking that app data remains.
- If signer compatibility cannot be restored, keep the old app and its data untouched. Treat any different-signature build as a separate migration path; plan and test an explicit export/backup-and-restore route before asking users to switch.
- Align the store manifest, release manifest, workflow names, artifact names, and download-page links before promoting a candidate.
- Keep expert-review-only / commercial-sales hold status until device update, accounting reconciliation, backup/restore, licensing/offline behavior, and independent expert checks are complete.

## Baseline and continuation
This checkpoint is based on the repository's `main` branch as inspected on 2026-10-21. Continue future release work from the reviewed `main` baseline plus an approved pull request; do not create a new unrelated project or discard existing history. This checkpoint itself does not change application code, release assets, signing credentials, or the public storefront.
