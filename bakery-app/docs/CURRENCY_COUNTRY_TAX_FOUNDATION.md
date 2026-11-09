# Currency, Country and Tax Foundation — Al-Mizan Bakery

Status: implementation foundation in review branch; not commercial tax certification.

## Runtime rules

1. The first-run setup asks for the operational base currency and the country where the business is registered.
2. Currency-country relationships are seeded into the encrypted `currency_country_map` table. The login/setup UI reads the persisted mapping.
3. A currency mapped to one country preselects that country. Shared currencies such as USD require an explicit country choice.
4. Saving setup validates that the chosen country is one of the countries mapped to the selected currency. The saved `company_settings` row contains `country_code`, `base_currency`, and `tax_profile`.
5. `tax_jurisdiction_profiles` stores the jurisdiction type, status, explanatory notes, source URL, verification date, and profile version.
6. Only `VERIFIED_BASELINE` profiles with active, effective-dated rules may calculate automatically. `REVIEW_REQUIRED` reference rules are stored inactive and are shown as reference-only. They must never be used by `TaxEngine`.
7. Subscription currency is separate from operational currency. EUR remains disabled for new licensing selections. USD is available as an operational currency and may also be used for subscription pricing.

## Arab League currency coverage

| Country | ISO country | National / accounting currency | Currency code | Current tax-profile state |
|---|---|---|---|---|
| Algeria | DZ | Algerian dinar | DZD | Reference-only; verify product classes and current rules |
| Bahrain | BH | Bahraini dinar | BHD | Verified baseline VAT 10%; registration and supply rules still apply |
| Comoros | KM | Comorian franc | KMF | Reference-only consumption-tax entry; not treated as VAT |
| Djibouti | DJ | Djiboutian franc | DJF | Reference-only VAT 10% |
| Egypt | EG | Egyptian pound | EGP | Verified baseline VAT 14%; exemptions and schedule goods apply |
| Iraq | IQ | Iraqi dinar | IQD | Review-required selective sales taxes; no general VAT baseline |
| Jordan | JO | Jordanian dinar | JOD | Reference-only general sales tax 16%; bread and other exemptions must be classified |
| Kuwait | KW | Kuwaiti dinar | KWD | No VAT baseline; other taxes/excise may apply |
| Lebanon | LB | Lebanese pound | LBP | Reference-only; current rate conflict requires legal verification |
| Libya | LY | Libyan dinar | LYD | Review-required sectoral taxes/fees; no general VAT baseline assumed |
| Mauritania | MR | Ouguiya | MRU | Reference-only VAT 16% |
| Morocco | MA | Moroccan dirham | MAD | Reference-only; standard/reduced rates vary by supply and transition rules |
| Oman | OM | Omani rial | OMR | Verified baseline VAT 5%; zero-rated/exempt supplies apply |
| Palestine | PS | Israeli new shekel; JOD/USD also used | ILS/JOD/USD | Review-required; territorial and supply rules require confirmation |
| Qatar | QA | Qatari riyal | QAR | No VAT baseline; excise and other taxes may apply |
| Saudi Arabia | SA | Saudi riyal | SAR | Verified baseline VAT 15%; e-invoicing and supply classifications apply |
| Somalia | SO | Somali shilling | SOS | Review-required; federal/member-state rules need separate validation |
| Sudan | SD | Sudanese pound | SDG | Verified baseline VAT 17%; registration, exemptions and tax administration need validation |
| Syria | SY | Syrian pound | SYP | Review-required consumption-tax profile |
| Tunisia | TN | Tunisian dinar | TND | Reference-only VAT 19%; reduced rates and exemptions apply |
| United Arab Emirates | AE | UAE dirham | AED | Verified baseline VAT 5%; registration and supply classifications apply |
| Yemen | YE | Yemeni rial | YER | Review-required selective sales taxes; no general VAT baseline assumed |

## Shared-currency behavior

- USD maps to the United States, Lebanon and Palestine as a selectable operating currency. The user must select the actual business jurisdiction.
- JOD maps to Jordan and is also a secondary currency option for Palestine.
- The user cannot save a country that is not mapped to the selected currency when a mapping exists.
- This mapping is a setup aid, not a legal conclusion about a country's sole legal tender.

## Tax activation gate

A stored reference percentage is not permission to calculate or post tax. Before activating a country's rule, the release must verify:
- effective date and current legal instrument;
- registration threshold and registration status;
- zero-rated, exempt, reduced-rate, and special-tax product categories;
- treatment of bakery staples and food products;
- input-tax recovery and purchase invoices;
- returns and credit notes;
- tax payable/receivable posting to the general ledger;
- required invoice fields, e-invoicing, returns, and filing deadlines.

## Sources used for the current reference-only review

- IMF, *Taxing in the MENA Region*, Chapter 6, VAT policy parameters and countries without VAT (2024–25): https://www.elibrary.imf.org/display/book/9798400281501/CH006.xml
- PwC, worldwide VAT rates, current 2026 summaries: https://taxsummaries.pwc.com/quick-charts/value-added-tax-vat-rates
- PwC, Jordan general sales tax and exemptions: https://taxsummaries.pwc.com/jordan/corporate/other-taxes
- PwC, Morocco 2026 VAT rate convergence: https://taxsummaries.pwc.com/morocco/corporate/significant-developments
- Lebanon Ministry of Finance VAT rate page (must be reconciled with later amendments before activation): https://www.finance.gov.lb/ar-lb/Taxation/Individuals/VAT/Pages/Tax-Rate.aspx

This document deliberately distinguishes active baselines from reference-only data. It does not claim that all 22 national tax regimes are already implemented or legally certified.
