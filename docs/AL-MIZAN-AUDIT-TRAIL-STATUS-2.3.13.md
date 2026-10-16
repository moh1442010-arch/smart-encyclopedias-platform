# Al-Mizan 2.3.13 — Audit trail repair status

- Target build: **2.3.13 (versionCode 45; database schema 12)**
- Base: repair branch `repair/al-mizan-expert-audit-2.3.12`
- Purpose of this tranche: introduce database-level change history for core financial and operational records without rewriting historical rows.
- Release status: **engineering validation only; not approved for general commercial release**.

## Implemented in source

- Added `audit_events` to record INSERT, UPDATE and DELETE events for transactions, journal entries and lines, invoice items, party payments, checks, payroll events, products, bank accounts/movements, customers, suppliers and employees.
- Audit records include entity, row ID, operation, quoted old/new field snapshots and a timestamp.
- Added SQLite triggers that reject normal UPDATE and DELETE operations against audit events.
- Incremented database schema from 11 to 12 so existing installations run the migration; application ID and signing continuity remain unchanged.
- Added automated tests for insert/update/delete capture, immutability, and a simulated schema 11 to 12 migration preserving an existing transaction.

## Important limitations

- This is an **append-only audit trail enforced by SQLite triggers**, not a tamper-proof external ledger. A person able to replace or directly manipulate the database file can potentially remove both records and triggers. External/server-side replication or cryptographic chaining would be needed for stronger tamper evidence.
- It logs changes to the listed tables, but it does not yet establish a complete business-level actor identity, reason-for-change workflow, or verified reversal-only policy for every posted transaction.
- Audit snapshots use quoted column values joined with delimiters; they are intended for operational traceability, not as a canonical export format.
- CI success is not device acceptance. Real upgrade testing on a copy of an existing installation, database integrity checks, backup/restore round-trips, and accounting T1–T20 remain required.
- Monetary values are still stored using SQLite REAL and Java double in this tranche. The financial minor-unit migration remains a separate high-risk task and must not be approximated with a global text replacement.

## Acceptance gate for this tranche

1. `testDebugUnitTest` passes on the exact commit that contains schema 12 and the audit tests.
2. Release APK/AAB build and signing checks pass for version 2.3.13.
3. A real-device upgrade test confirms existing data survives and the app opens after login.
4. Backup/restore and accounting regression tests show no unexplained differences.

Until these conditions are evidenced, this work should be described as implemented in source and awaiting validation, not as a fully completed independent audit system.
