# Secure paid-copy delivery

This layer keeps paid PDFs out of GitHub Pages and serves them only through a temporary, revocable license token.

## One-time Cloudflare setup

1. Create an R2 bucket named `smart-encyclopedias-paid-books` and keep it private.
2. Deploy `delivery/worker.js` with `delivery/wrangler.jsonc`.
3. Bind the existing D1 database shown in the config and run `delivery/schema.sql` once.
4. Add a Worker secret named `LICENSE_ADMIN_KEY`. Never put this value in GitHub.
5. Verify the deployed Worker hostname matches the API value in `assets/delivery.js`.

## Per paid order

1. Generate a unique license ID and a strong buyer PDF password.
2. Run `tools/protect-pdf.py` against the private master PDF with exactly 250 product pages.
3. Upload the generated buyer PDF to the private R2 bucket. Do not upload the master PDF to GitHub Pages.
4. Create a license through `POST /api/license/create` using the admin key. Supply the buyer, order ID and exact R2 object key.
5. Send the returned `deliveryUrl` to the paid buyer and send the PDF password separately.
6. Revoke the license in D1 if abuse is reported.

## Security model

- AES-256 PDF encryption.
- No print/copy/edit/annotation/assembly permissions in normal PDF readers.
- Buyer name and license ID are visibly watermarked into every page.
- R2 is private; there is no public PDF URL.
- Delivery tokens expire and have a download limit (default: 3).
- The delivery URL does not reveal the PDF password.

PDF permission flags are not DRM and can be bypassed by advanced tools after a legitimate viewer password is known. The delivery layer therefore protects the file at the distribution layer as well. External-camera screenshots cannot be prevented with a web/PDF system.
