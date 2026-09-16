# Secure paid-copy delivery

This layer keeps paid PDFs out of GitHub Pages and serves them only through a temporary, revocable license token.

## One-time Cloudflare setup

1. Create an R2 bucket named `smart-encyclopedias-paid-books` and keep it private.
2. Deploy `delivery/worker.js` with `delivery/wrangler.jsonc`.
3. Bind the existing D1 database shown in the config and run `delivery/schema.sql` once. If `licenses` already exists, run `delivery/migrate-device-binding.sql` instead.
4. Add a Worker secret named `LICENSE_ADMIN_KEY`. Never put this value in GitHub.
5. Verify the deployed Worker hostname matches the API value in `assets/delivery.js`.

## Payment approval gate

The customer may submit an order and proof of payment, but the delivery Worker will **not** create an active license unless the owner explicitly approves the payment. `POST /api/license/create` requires both the private admin key and `approved: true`. The approval timestamp and approver are recorded in D1.

The sales agent must never treat a customer's statement that payment was made as proof of payment and must never generate or expose a delivery token. The owner reviews the payment first; only then is the license created and the delivery URL released.

## Per approved paid order

1. Verify the payment manually as the owner.
2. Generate a unique license ID and a strong buyer PDF password.
3. Run `tools/protect-pdf.py` against the private master PDF with exactly 250 product pages.
4. Upload the generated buyer PDF to the private R2 bucket. Do not upload the master PDF to GitHub Pages.
5. Call `POST /api/license/create` with the buyer, order ID, exact R2 object key, `approved: true`, and the admin key.
6. Send the returned `deliveryUrl` to the paid buyer and send the PDF password separately.
7. If the buyer changes device, use the authenticated `POST /api/license/reset-device` operation before allowing activation on the replacement device.

## Single-device binding

On first activation, the delivery page generates a random device identifier stored in that browser's local storage. The Worker hashes it and permanently binds the license to that device/browser profile. The same license is rejected on another browser/device profile. The PDF download request must carry the same device identifier.

This is a strong web-level device binding, not hardware DRM: clearing browser storage, changing browsers, or reinstalling the browser can look like a new device. A controlled reset is therefore available to the owner. External-camera screenshots cannot be prevented with a web/PDF system.

## Security model

- AES-256 PDF encryption.
- No print/copy/edit/annotation/assembly permissions in normal PDF readers.
- Buyer name and license ID are visibly watermarked into every page.
- R2 is private; there is no public PDF URL.
- Delivery tokens expire and have a download limit (default: 3).
- Payment approval is owner-controlled and recorded.
- The license is bound to one browser/device profile after first activation.
- The delivery URL does not reveal the PDF password.

PDF permission flags are not DRM and can be bypassed by advanced tools after a legitimate viewer password is known. The delivery layer therefore protects the file at the distribution layer as well.
