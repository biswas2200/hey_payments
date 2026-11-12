# 05 · Data Model

Conventions: PostgreSQL 16; table names `snake_case` plural; PK `id UUID` (UUIDv7, generated in app); money `BIGINT` paise, **never** `NUMERIC` floats or `double`; timestamps `TIMESTAMPTZ` UTC; `created_at`, `updated_at`, `version BIGINT` (optimistic lock) on mutable tables. Raw signed envelopes are kept verbatim in `JSONB` for evidence.

## ERD
```mermaid
erDiagram
  USERS ||--o| WALLETS : owns
  USERS ||--o{ DEVICES : registers
  USERS ||--o| MERCHANTS : "is (role MERCHANT)"
  USERS ||--o{ REFRESH_TOKENS : has
  DEVICES ||--o{ BALANCE_CERTIFICATES : "holds"
  WALLETS ||--o{ BALANCE_CERTIFICATES : funds
  BALANCE_CERTIFICATES ||--o{ VOUCHERS : "spent by"
  DEVICES ||--o{ VOUCHERS : signs
  MERCHANTS ||--o{ VOUCHERS : receives
  VOUCHERS ||--o| RECEIPTS : "acknowledged by"
  VOUCHERS }o--o| JOURNAL_ENTRIES : "settled in"
  SYNC_BATCHES ||--o{ VOUCHERS : contains
  JOURNAL_ENTRIES ||--|{ JOURNAL_LINES : has
  LEDGER_ACCOUNTS ||--o{ JOURNAL_LINES : posts
  VOUCHERS ||--o{ FRAUD_CASES : triggers
  DEVICES ||--o{ FRAUD_CASES : "subject of"
  SERVER_KEYS ||--o{ BALANCE_CERTIFICATES : signs

  USERS {
    uuid id PK
    varchar phone UK
    varchar email
    varchar password_hash
    varchar role
    varchar status
    varchar pin_hash
    timestamptz created_at
  }
  WALLETS {
    uuid id PK
    uuid user_id FK,UK
    varchar status
    bigint version
  }
  DEVICES {
    uuid id PK
    uuid user_id FK
    varchar kind
    bytea public_key
    varchar status
    bigint last_counter
    timestamptz frozen_at
    varchar frozen_reason
  }
  MERCHANTS {
    uuid id PK
    uuid user_id FK,UK
    varchar display_name
    varchar handle UK
    varchar category
    varchar status
    bigint pending_cap
  }
  BALANCE_CERTIFICATES {
    uuid id PK
    uuid wallet_id FK
    uuid device_id FK
    bigint limit_amount
    bigint counter_start
    varchar status
    varchar kid
    jsonb envelope
    timestamptz issued_at
    timestamptz expires_at
    uuid superseded_by
  }
  VOUCHERS {
    uuid id PK
    uuid bc_id FK
    uuid device_id FK
    bigint counter
    uuid merchant_id FK
    uuid pr_id
    bigint amount
    bigint cum_spent
    varchar prev_hash
    varchar hash UK
    varchar status
    boolean void_requested
    varchar reject_code
    jsonb envelope
    timestamptz device_created_at
    timestamptz received_at
    varchar first_uploader
    uuid journal_entry_id FK
  }
  RECEIPTS {
    uuid id PK
    uuid voucher_id FK,UK
    uuid merchant_device_id FK
    jsonb envelope
    timestamptz accepted_at
  }
  SYNC_BATCHES {
    uuid id PK
    uuid device_id FK
    varchar idempotency_key UK
    int item_count
    jsonb result
    timestamptz created_at
  }
  LEDGER_ACCOUNTS {
    uuid id PK
    varchar code UK
    varchar type
    uuid owner_id
    varchar currency
    bigint balance
    bigint version
  }
  JOURNAL_ENTRIES {
    uuid id PK
    varchar kind
    varchar reference UK
    timestamptz posted_at
    varchar memo
  }
  JOURNAL_LINES {
    uuid id PK
    uuid entry_id FK
    uuid account_id FK
    bigint amount
  }
  FRAUD_CASES {
    uuid id PK
    varchar rule
    varchar severity
    varchar status
    uuid device_id FK
    uuid voucher_id FK
    jsonb evidence
    uuid assignee
    timestamptz opened_at
    timestamptz resolved_at
  }
  SERVER_KEYS {
    varchar kid PK
    bytea public_key
    bytea private_key_enc
    varchar status
    timestamptz not_before
    timestamptz not_after
  }
```

Additional tables: `notifications(id, user_id, type, payload jsonb, read_at)`, `audit_log(id, actor_id, action, entity, entity_id, data jsonb, ip, at)` (append-only; `REVOKE UPDATE, DELETE`), `idempotency_keys(key, user_id, request_hash, response jsonb, status, created_at)` (Redis is primary, table is fallback), `event_publication` (Spring Modulith).

## Enums
| Enum | Values |
|---|---|
| `users.role` | `PAYER`, `MERCHANT`, `ADMIN` |
| `users.status` | `ACTIVE`, `LOCKED`, `DISABLED` |
| `devices.kind` | `PAYER`, `MERCHANT` |
| `devices.status` | `ACTIVE`, `FROZEN`, `REVOKED`, `CLOSED` |
| `balance_certificates.status` | `ACTIVE`, `SUPERSEDED`, `CLOSED`, `EXPIRED` |
| `vouchers.status` | `RECEIVED`, `VERIFIED`, `HELD`, `SETTLED`, `REVERSED`, `REJECTED` |
| `ledger_accounts.type` | `USER_MAIN`, `OFFLINE_ESCROW`, `MERCHANT_PAYABLE`, `PLATFORM_FEES`, `RISK_RESERVE`, `BANK_SIM_CLEARING` |
| `fraud_cases.severity` / `status` | `LOW`, `MEDIUM`, `HIGH` / `OPEN`, `INVESTIGATING`, `RESOLVED_FRAUD`, `RESOLVED_OK` |

## Key constraints and indexes
```sql
-- double-spend guards
CREATE UNIQUE INDEX ux_vouchers_device_counter ON vouchers(device_id, counter);
CREATE UNIQUE INDEX ux_vouchers_hash ON vouchers(hash);
-- one active BC per device
CREATE UNIQUE INDEX ux_bc_active_device ON balance_certificates(device_id) WHERE status = 'ACTIVE';
-- one active payer device per user
CREATE UNIQUE INDEX ux_device_active_user ON devices(user_id, kind) WHERE status = 'ACTIVE';
-- journal idempotency (reference = voucher id / load request id)
CREATE UNIQUE INDEX ux_journal_reference ON journal_entries(reference);
-- amounts
ALTER TABLE vouchers ADD CONSTRAINT ck_amount CHECK (amount > 0 AND cum_spent >= amount);
ALTER TABLE journal_lines ADD CONSTRAINT ck_line_nonzero CHECK (amount <> 0);
CREATE INDEX ix_vouchers_merchant_status ON vouchers(merchant_id, status, received_at DESC);
CREATE INDEX ix_vouchers_bc ON vouchers(bc_id, counter);
```

## Ledger invariants (enforced in code and tested)
1. Every journal entry has ≥ 2 lines and **Σ lines.amount = 0** (checked in `LedgerService.post()`, plus a deferred constraint trigger `trg_journal_balanced`).
2. `ledger_accounts.balance` is updated in the same transaction as the lines (with optimistic locking via `version`). A nightly job recomputes balances from lines and alerts on drift.
3. `USER_MAIN`, `OFFLINE_ESCROW` and `MERCHANT_PAYABLE` must never go negative, **except** `USER_MAIN` during fraud recovery (which sets the user to `LOCKED`).
4. For each BC: `Σ settled vouchers + remaining escrow + reversed = limit`.
5. Global: Σ all account balances = 0.

## Flyway migration plan (`backend/src/main/resources/db/migration`)
| Version | Contents | Phase |
|---|---|---|
| V1__baseline.sql | extensions (`pgcrypto`), `audit_log`, `idempotency_keys`, `event_publication` | 0 |
| V2__auth.sql | `users`, `refresh_tokens` | 1 |
| V3__devices_merchants.sql | `devices`, `merchants`, `server_keys` | 1 |
| V4__ledger.sql | `ledger_accounts`, `journal_entries`, `journal_lines`, balance trigger, system accounts seed | 2 |
| V5__wallets.sql | `wallets` | 2 |
| V6__balance_certificates.sql | `balance_certificates` | 3 |
| V7__vouchers_sync.sql | `vouchers`, `receipts`, `sync_batches` | 6 |
| V8__fraud_notifications.sql | `fraud_cases`, `notifications` | 6 |
| V9__admin_views.sql | reporting views (`v_merchant_daily`, `v_bc_reconciliation`) | 7 |
| R__dev_seed.sql | dev-only seed users (profile `dev`) | 1+ |

Rules: never edit an applied migration; add a new one. Every migration has a matching repository integration test run under Testcontainers.
