# 07 · Sequence Diagrams and State Machines

## 1. Registration and device binding
```mermaid
sequenceDiagram
  autonumber
  actor U as User
  participant FE as PWA
  participant API as Backend
  participant MP as Mailpit
  U->>FE: phone, password, role
  FE->>API: POST /auth/register
  API->>MP: OTP email (simulated SMS)
  U->>FE: enter OTP
  FE->>API: POST /auth/verify-otp → POST /auth/login
  API-->>FE: accessToken + hp_rt cookie
  FE->>FE: crypto.subtle.generateKey(ECDSA P-256, extractable=false)
  FE->>FE: store CryptoKey in IndexedDB, export public JWK
  FE->>API: POST /devices {publicKeyJwk}
  API-->>FE: deviceId (+ Merchant Certificate if merchant)
  FE->>API: GET /keys/jwks, GET /devices/crl
  FE->>FE: cache JWKS + CRL · set payment PIN (PUT /auth/pin)
```

## 2. Fund and load offline balance
```mermaid
sequenceDiagram
  autonumber
  participant FE as Payer PWA
  participant API as Backend
  participant L as Ledger
  participant SIM as Funding Simulator
  FE->>API: POST /wallets/me/fund {₹1,000} (Idempotency-Key)
  API->>SIM: debit fake bank
  SIM-->>API: ok
  API->>L: BANK_SIM_CLEARING −1000 / USER_MAIN +1000
  FE->>FE: sync outbox first (if any)
  FE->>API: POST /offline/load {deviceId, ₹500, lastCounter}
  API->>API: check lastCounter == highest received; check caps
  API->>L: USER_MAIN −500 / OFFLINE_ESCROW +500
  API->>API: supersede old BC, sign new BC (server key)
  API-->>FE: BC envelope
  FE->>FE: verify BC sig with JWKS, store, reset spent=0
```

## 3. Offline payment
See [03-offline-protocol.md §4](03-offline-protocol.md#4-the-offline-handshake-happy-path).

## 4. Sync and settlement
```mermaid
sequenceDiagram
  autonumber
  participant D as Device (payer or merchant)
  participant API as SyncController
  participant SY as SyncService
  participant V as VoucherVerifier
  participant ST as SettlementService
  participant L as Ledger
  participant F as FraudService
  D->>API: POST /sync {items[]} (Idempotency-Key)
  API->>SY: ingest(batch)
  loop each item ordered by (deviceId, counter)
    SY->>SY: already processed? → stored result
    SY->>V: verify crypto + S2–S7
    alt valid
      V-->>SY: VERIFIED
      SY->>ST: settle(voucher) [tx, lock BC row]
      ST->>L: ESCROW −a / MERCHANT +a−fee / FEES +fee
      ST-->>SY: SETTLED
    else violation (double spend / chain break / overspend)
      V-->>SY: violation
      SY->>F: openCase + freeze device
      Note right of ST: overspend within caps still settles from RISK_RESERVE
    else invalid signature / malformed
      V-->>SY: REJECTED(code)
    end
  end
  SY-->>API: results + CRL delta
  API-->>D: 200 results
  D->>D: update outbox states, show "Settled ✓"
```

## 5. Payer void (merchant never received)
```mermaid
sequenceDiagram
  participant P as Payer PWA
  participant API as Backend
  participant Job as Claim-window job
  P->>P: Voucher SIGNED, no receipt → "Merchant didn't receive"
  P->>API: POST /sync {VC, voidRequested:true}
  API-->>P: HELD (until BC.expiresAt + 7d)
  alt merchant syncs the same voucher later
    API->>API: settle → SETTLED (merchant wins: has the voucher)
  else window passes
    Job->>API: reverse → ESCROW −a / USER_MAIN +a → REVERSED
  end
```

## 6. Dispute / fraud handling (admin)
```mermaid
sequenceDiagram
  actor A as Admin
  participant UI as Admin console
  participant API as Backend
  API-->>UI: new HIGH case: DOUBLE_SPEND on device D
  A->>UI: open case, view both vouchers side by side (counter, hash, merchants)
  A->>API: POST /admin/fraud-cases/{id}/resolve {FRAUD}
  API->>API: keep device REVOKED, user LOCKED, recovery entry, audit log
  API-->>UI: case RESOLVED_FRAUD
```

## 7. State machines
### Voucher (server)
```mermaid
stateDiagram-v2
  [*] --> RECEIVED
  RECEIVED --> REJECTED: crypto/schema/unknown BC
  RECEIVED --> VERIFIED: all checks pass
  VERIFIED --> SETTLED: escrow posting (or risk reserve)
  VERIFIED --> HELD: voidRequested & no receipt
  HELD --> SETTLED: merchant uploads within window
  HELD --> REVERSED: claim window expired
  SETTLED --> [*]
  REVERSED --> [*]
  REJECTED --> [*]
```

### Voucher (payer device outbox)
```mermaid
stateDiagram-v2
  [*] --> SIGNED
  SIGNED --> RECEIPTED: receipt QR verified
  SIGNED --> VOID_REQUESTED: user taps "Merchant didn't receive"
  SIGNED --> SYNCING
  RECEIPTED --> SYNCING: online
  VOID_REQUESTED --> SYNCING
  SYNCING --> SETTLED
  SYNCING --> HELD
  SYNCING --> REJECTED
  HELD --> SETTLED
  HELD --> REVERSED
```

### Device
```mermaid
stateDiagram-v2
  [*] --> ACTIVE: registered
  ACTIVE --> FROZEN: fraud rule / admin
  FROZEN --> ACTIVE: admin unfreeze (case OK)
  FROZEN --> REVOKED: case FRAUD
  ACTIVE --> CLOSED: user removes device / reinstall
  REVOKED --> [*]
  CLOSED --> [*]
```

### Balance Certificate
```mermaid
stateDiagram-v2
  [*] --> ACTIVE: load
  ACTIVE --> SUPERSEDED: top-up issues new BC
  ACTIVE --> CLOSED: unload / device closed
  ACTIVE --> EXPIRED: expiresAt passed
  SUPERSEDED --> [*]: after claim window
  CLOSED --> [*]: after claim window
  EXPIRED --> [*]: sweep returns escrow
```

### Payment Request (merchant device)
`OPEN → CONSUMED` (valid voucher scanned) · `OPEN → EXPIRED` (5 min) · `OPEN → CANCELLED` (merchant).
