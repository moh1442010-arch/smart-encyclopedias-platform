# Al-Mizan Admin Command Center

A private, unified administration workspace for all current and future Al-Mizan products.

## Scope
- Al-Mizan accounting app: Android and Windows.
- Al-Mizan Bakery app.
- Store and public website.
- Customer feedback, complaints, and expert reviews.
- Releases, downloads, licensing, and operational workflows.
- Future products and modules through a product registry.

## Navigation
1. Executive overview.
2. Product registry and release catalog.
3. Licensing and subscriptions.
4. Customer feedback and moderation.
5. Expert QA reports and defect tracking.
6. Store analytics and download events.
7. GitHub Actions and deployment status.
8. Service health and alerts.
9. Administrative audit log.
10. Settings and future role-based access.

## UX and visual system
- Arabic RTL by default; prepare English localization.
- Responsive, mobile-first layout.
- Navy #0B1F3A, gold #D4AF37, light neutral surfaces.
- Clear status labels and accessible contrast.
- Show only verified live data. Empty states must say that data is not available yet.

## Security
- Extend the existing Cloudflare Worker admin session; do not create a second authentication system.
- Serve the protected admin panel through the Worker, not a public static admin page.
- Verify authorization on every admin API endpoint.
- Keep passwords, signing keys, and GitHub credentials in Worker secrets only.
- Use least-privilege, read-only GitHub access for workflow status.
- Enforce origin checks, CSRF protection, rate limits, secure cookies, and audit logging.
- Never expose activation tokens except through an authorized, audited recovery action.

## Data model
Add new D1 tables for products, releases, customer reviews, review moderation events, privacy-minimized analytics, workflow snapshots, notifications, feature modules, and admin audit events. Preserve existing license_orders and license_audit behavior. Any schema change requires a migration and tests.

## Privacy
Collect only necessary analytics: event type, product/page key, timestamp, coarse device class, and limited referral metadata. Do not store raw IP addresses or unique browser fingerprints. Define retention and disclose measurement practices where required.

## Delivery phases
0. Audit current Worker routes, D1 schema, public pages, and existing admin behavior.
1. Build the protected unified admin shell and move bakery licensing into its section.
2. Add product/release registry and read-only GitHub Actions status.
3. Add centralized customer/expert feedback intake and moderation.
4. Add privacy-conscious page, product, and download analytics.
5. Add notifications, role-based permissions, and operational controls after security review.

## Acceptance criteria
- Anonymous users cannot access admin pages or endpoints.
- Expired and forged sessions are rejected.
- No secrets appear in public HTML, JavaScript, or API responses.
- Existing bakery licensing, manual payment review, and signing remain functional.
- No license is issued without manual payment verification and admin approval.
- Feedback is stored centrally and never published automatically.
- Analytics avoid unnecessary personal data.
- D1 migrations are tested before production.
- GitHub Actions pass and the Worker is reviewed before deployment.

## Verified current baseline
The Worker already has an admin login, an HMAC session with an eight-hour expiry, secure cookie attributes, and login rate limiting. The current admin panel focuses on bakery licenses. Customer feedback currently routes through WhatsApp rather than centralized storage. The current visitor counter does not provide the required internal analytics. These capabilities must be implemented and tested; they are not yet complete.
