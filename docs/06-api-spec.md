# 06 · API Specification (v1)

Base path `/api/v1`. JSON only. Auth: `Authorization: Bearer <JWT>` (access token 15 min; refresh token 14 days, rotated, httpOnly cookie `hp_rt`). The machine-readable contract is [openapi.yaml](openapi.yaml). The running server exposes Swagger UI at `/swagger-ui.html` (dev profile only).

## Conventions
- **Idempotency:** every `POST` that moves money or changes state requires an `Idempotency-Key: <uuid>` header. The same key with the same body returns the stored response (`200/201` with `Idempotent-Replayed: true`). The same key with a different body returns `422 IDEMPOTENCY_KEY_REUSED`. Keys are kept 24 h in Redis.
- **Money:** `{ "amount": 12000, "currency": "INR" }` with amount in paise (integer).
- **Pagination:** `?page=0&size=20&sort=createdAt,desc` → `{ items, page, size, totalItems }`.
- **Correlation:** clients may send `X-Request-Id`; the server always echoes it and logs it.
- **Errors:** RFC 9457 Problem Details:
```json
{ "type": "https://heypayments.dev/errors/limit-exceeded",
  "title": "Limit exceeded", "status": 422, "code": "LIMIT_EXCEEDED",
  "detail": "Offline balance cannot exceed ₹2,000", "instance": "/api/v1/offline/load",
  "requestId": "…", "errors": [ { "field": "amount", "message": "must be ≤ 200000" } ] }
```

## Error codes
| HTTP | code | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | Bean validation errors |
| 401 | `UNAUTHENTICATED` / `TOKEN_EXPIRED` | Missing or invalid JWT |
| 403 | `FORBIDDEN` / `DEVICE_FROZEN` | Role mismatch / frozen device |
| 404 | `NOT_FOUND` | Unknown resource |
| 409 | `SYNC_REQUIRED` / `DEVICE_ALREADY_ACTIVE` / `CONFLICT` | Preconditions |
| 422 | `LIMIT_EXCEEDED` / `INSUFFICIENT_FUNDS` / `IDEMPOTENCY_KEY_REUSED` | Business rule |
| 429 | `RATE_LIMITED` | Bucket4j limits |
| 500 | `INTERNAL` | Never leaks stack traces |

## Endpoints
### Auth (`auth`)
| Method | Path | Role | Body → Response |
|---|---|---|---|
| POST | `/auth/register` | public | `{phone, email, password, role: PAYER\|MERCHANT, displayName?}` → `201 {userId}` (OTP sent to Mailpit) |
| POST | `/auth/verify-otp` | public | `{phone, otp}` → `200` |
| POST | `/auth/login` | public | `{phone, password}` → `200 {accessToken, expiresIn, user}` + `hp_rt` cookie |
| POST | `/auth/refresh` | cookie | → `200 {accessToken}` (rotates refresh token; reuse detection revokes the family) |
| POST | `/auth/logout` | any | → `204` |
| PUT | `/auth/pin` | PAYER | `{pin (6 digits), password}` → `204` (payment PIN, Argon2id hashed; the client also derives a local unlock check) |
| GET | `/users/me` | any | → profile |

### Keys & devices (`crypto`, `device`)
| Method | Path | Role | Body → Response |
|---|---|---|---|
| GET | `/keys/jwks` | public | → `{keys:[{kid, kty:"EC", crv:"P-256", x, y, status}]}` |
| POST | `/devices` | PAYER, MERCHANT | `{publicKeyJwk, label, userAgent}` → `201 {deviceId, status, merchantCertificate?}` |
| GET | `/devices/me` | any | → active device + `lastCounter` |
| DELETE | `/devices/{id}` | owner | → `204` (closes device; requires sync) |
| GET | `/devices/crl?since=<ts>` | any auth | → `{generatedAt, revoked:[{deviceId, status, at}]}` (delta) |

### Wallet (`wallet`)
| Method | Path | Role | Body → Response |
|---|---|---|---|
| GET | `/wallets/me` | PAYER | → `{mainBalance, offlineEscrow, activeBc?}` |
| POST | `/wallets/me/fund` | PAYER | `{amount, source:"SIM_BANK"}` → `201 {journalEntryId, mainBalance}` (simulator; max ₹50,000/day) |

### Offline balance (`offlinetoken`)
| Method | Path | Role | Body → Response |
|---|---|---|---|
| POST | `/offline/load` | PAYER | `{deviceId, amount, lastCounter}` → `201 {balanceCertificate (envelope), offlineLimit}` · `409 SYNC_REQUIRED` · `422 LIMIT_EXCEEDED` |
| POST | `/offline/unload` | PAYER | `{deviceId, lastCounter}` → `200 {returnedAmount, mainBalance}` |
| GET | `/offline/certificate` | PAYER | → current BC envelope (re-download after reinstall is **not** allowed: it is bound to the device key) |

### Sync (`sync`)
| Method | Path | Role | Body → Response |
|---|---|---|---|
| POST | `/sync` | PAYER, MERCHANT | `{deviceId, items:[{type:"VC"\|"RC", envelope (base45 or JSON), voidRequested?}], crlSince}` (max 100 items) → `200 {results:[{voucherId, status, code?, settledAt?}], crlDelta, serverTime, nextSyncAfter}` |

Each item is processed independently, so one bad voucher never fails the batch. Results are deterministic and replayable.

### Transactions
| Method | Path | Role | → |
|---|---|---|---|
| GET | `/transactions` | PAYER, MERCHANT | paged list (loads, vouchers, settlements, reversals) with a status timeline |
| GET | `/transactions/{id}` | owner | detail + evidence (voucher hash, receipt) |

### Merchant (`merchant`, `settlement`)
| Method | Path | Role | → |
|---|---|---|---|
| GET | `/merchants/me` | MERCHANT | profile + MC + pending cap |
| PUT | `/merchants/me` | MERCHANT | update display name/category |
| GET | `/merchants/me/summary?date=` | MERCHANT | `{acceptedOffline, pendingSync, settled, fees, net}` |
| GET | `/merchants/me/settlements` | MERCHANT | paged settlements |
| GET | `/merchants/me/settlements/export.csv?from&to` | MERCHANT | CSV |
| POST | `/merchants/me/payouts` | MERCHANT | `{amount}` → simulated payout to bank |

### Admin (`admin`, `fraud`)
| Method | Path | → |
|---|---|---|
| GET | `/admin/metrics` | KPIs: GMV, settled count, pending escrow, fraud open, HEART/AARRR counters |
| GET | `/admin/users?q=` · `/admin/devices?status=` | lists |
| POST | `/admin/devices/{id}/freeze` · `/unfreeze` | `{reason}` → `204` (audited) |
| GET | `/admin/ledger/accounts` · `/admin/ledger/entries?account=` | ledger explorer |
| GET | `/admin/ledger/reconciliation` | invariant checks (per-BC + global Σ=0) |
| GET | `/admin/fraud-cases?status=` · `/{id}` | cases + evidence |
| POST | `/admin/fraud-cases/{id}/resolve` | `{outcome: FRAUD\|OK, note}` |
| PUT | `/admin/limits` | update business limits (audited) |

### System
| GET | `/actuator/health` (public, liveness/readiness), `/actuator/prometheus` (internal network only) |
|---|---|

## Rate limits (per user unless noted)
| Endpoint group | Limit |
|---|---|
| `/auth/login`, `/auth/verify-otp` | 5 / min per phone + 20 / min per IP |
| `/sync` | 30 / min |
| `/offline/load`, `/wallets/me/fund` | 10 / min |
| everything else | 120 / min |
