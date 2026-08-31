# Sales Agent and Per-Copy License Architecture

## Product

Each completed sale includes:
- The full 250-page encyclopedia access
- A private customer access credential
- A dedicated smart-agent entitlement tied to a license ID

## License lifecycle

1. Customer submits an order.
2. Store records customer identity and selected currency.
3. Payment is verified by the store operator.
4. A unique license ID is issued.
5. Customer access is provisioned through the selected protected reading service.
6. A dedicated agent identity is provisioned for the license.
7. Customer receives the reading link, agent link, and credentials by email.
8. Revocation disables the entitlement when required.

## Security rules

- Never put API keys, private service credentials, or customer passwords in public JavaScript.
- GitHub Pages remains the public storefront only. GitHub's current documentation says Pages is not intended for e-commerce or sensitive transactions such as sending passwords. The private order, license, email, and provisioning layer must therefore run on a server-side service.
- The public site must never expose the full private encyclopedia URL or customer credential.
- The agent must validate an active license before granting customer-only functions.

## Current commercial configuration

- Local price: 130000 SDG
- First 100 copies: 10% discount = 117000 SDG
- Student discount: fixed 10% = 117000 SDG
- International price: 19 USD
- International offer price: 16 USD

## Implementation boundary

The repository currently provides the public storefront and agent-facing UI. A real per-customer license system requires a server-side backend or managed backend service for order storage, payment confirmation, credential provisioning, email delivery, and agent entitlement checks. This document is the source of truth for that boundary.
