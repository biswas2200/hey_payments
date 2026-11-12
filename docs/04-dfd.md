# 04 · Data Flow Diagrams

Notation (Gane–Sarson style, drawn in Mermaid): `([ ])` = external entity, `( )` round = process, `[( )]` = data store, arrows = data flows labelled with the data moved.

## Data stores
| ID | Store | Physical |
|---|---|---|
| D1 | Users & Devices | `users`, `devices`, `merchants`, `refresh_tokens` |
| D2 | Wallets & Certificates | `wallets`, `balance_certificates` |
| D3 | Vouchers & Receipts | `vouchers`, `receipts`, `sync_batches` |
| D4 | Ledger | `ledger_accounts`, `journal_entries`, `journal_lines` |
| D5 | Audit Log | `audit_log` |
| D6 | Revocation List & Server Keys | `devices.status`, `server_keys` |
| D7 | Fraud Cases | `fraud_cases` |
| L1 | Device Local Store (client) | IndexedDB: `keys`, `certs`, `outbox`, `history`, `crl`, `jwks` |

## Level 0: Context diagram
```mermaid
flowchart LR
  P([Payer])
  M([Merchant])
  A([Admin])
  B([Funding Simulator])
  N([Email / Notification sink])
  S((0.0<br/>Hey Payments<br/>System))
  P -- "credentials, device public key, load/unload request, vouchers" --> S
  S -- "tokens, balance certificate, balances, history, settlement status" --> P
  M -- "credentials, merchant device key, vouchers + receipts" --> S
  S -- "merchant certificate, settlements, reports" --> M
  P <-. "PR QR / Voucher QR / Receipt QR (offline, peer-to-peer)" .-> M
  A -- "queries, freeze/unfreeze, limit changes" --> S
  S -- "dashboards, fraud cases, ledger views" --> A
  S -- "debit/credit instructions" --> B
  B -- "funding confirmations" --> S
  S -- "settlement / fraud notifications" --> N
```

## Level 1: Major processes
```mermaid
flowchart TB
  P([Payer]); M([Merchant]); A([Admin]); B([Funding Simulator])
  p1((1.0<br/>Auth & Device<br/>Registration))
  p2((2.0<br/>Wallet Fund /<br/>Offline Load-Unload))
  p3((3.0<br/>Offline Payment<br/>on devices))
  p4((4.0<br/>Sync &<br/>Verification))
  p5((5.0<br/>Ledger &<br/>Settlement))
  p6((6.0<br/>Fraud &<br/>Risk))
  p7((7.0<br/>Admin &<br/>Reporting))
  D1[(D1 Users & Devices)]
  D2[(D2 Wallets & BCs)]
  D3[(D3 Vouchers)]
  D4[(D4 Ledger)]
  D5[(D5 Audit Log)]
  D6[(D6 CRL & Keys)]
  D7[(D7 Fraud Cases)]
  L1[(L1 Device Local Store)]

  P -- credentials, pubkey --> p1
  M -- credentials, pubkey --> p1
  p1 -- user, device rows --> D1
  p1 -- JWT, MC --> M
  p1 -- JWT --> P
  D6 -- server key --> p1

  P -- fund / load / unload --> p2
  p2 -- fund request --> B
  B -- confirmation --> p2
  p2 -- BC --> P
  p2 <-- wallet, BC rows --> D2
  p2 -- postings --> p5
  D6 -- server key --> p2

  P <-- PR / VC / RC QR --> p3
  M <-- PR / VC / RC QR --> p3
  p3 <-- certs, outbox --> L1

  L1 -- outbox batch --> p4
  p4 -- vouchers, receipts --> D3
  D2 -- BC --> p4
  D1 -- device status --> p4
  p4 -- verified vouchers --> p5
  p4 -- violations --> p6
  p4 -- sync results, fresh CRL --> L1

  p5 <-- journal --> D4
  p5 -- settlement status --> M
  p5 -- events --> D5

  p6 -- cases --> D7
  p6 -- freeze device --> D1
  p6 -- revocation --> D6

  A -- queries, commands --> p7
  D4 & D7 & D1 & D5 -- read models --> p7
  p7 -- freeze / limits --> D1
  p7 -- dashboards --> A
```

## Level 2: 2.0 Wallet Fund / Offline Load–Unload
```mermaid
flowchart LR
  P([Payer]); B([Funding Simulator])
  p21((2.1 Fund<br/>main wallet))
  p22((2.2 Check sync<br/>precondition))
  p23((2.3 Move to<br/>escrow))
  p24((2.4 Issue & sign<br/>Balance Certificate))
  p25((2.5 Unload &<br/>close BC))
  D1[(D1 Devices)]; D2[(D2 Wallets & BCs)]; D3[(D3 Vouchers)]; D4[(D4 Ledger)]; D6[(D6 Keys/CRL)]
  P -- amount, idempotency key --> p21 -- debit request --> B -- ok --> p21 -- posting --> D4
  P -- load amount, lastCounter --> p22
  D3 -- highest received counter --> p22
  p22 -- "409 SYNC_REQUIRED" --> P
  p22 -- ok --> p23 -- posting main→escrow --> D4
  p23 --> p24
  D1 -- device pubkey --> p24
  D6 -- signing key --> p24
  p24 -- new BC, supersede old --> D2
  p24 -- signed BC --> P
  P -- unload request --> p25
  D3 -- spent under BC --> p25
  p25 -- posting escrow→main --> D4
  p25 -- BC CLOSED --> D2
  p25 -- revoke --> D6
```

## Level 2: 3.0 Offline Payment (client-side, no server)
```mermaid
flowchart LR
  M([Merchant]); P([Payer])
  p31((3.1 Create &<br/>sign PR))
  p32((3.2 Verify PR<br/>& confirm))
  p33((3.3 Build &<br/>sign Voucher))
  p34((3.4 Verify<br/>Voucher offline))
  p35((3.5 Issue<br/>Receipt))
  p36((3.6 Verify Receipt<br/>& update status))
  LM[(L1-M Merchant store:<br/>MC, keys, CRL, JWKS, outbox)]
  LP[(L1-P Payer store:<br/>BC, keys, JWKS, CRL, outbox)]
  M -- amount, note --> p31
  LM -- MC, merchant key --> p31
  p31 -- PR QR --> p32
  LP -- JWKS, CRL, BC --> p32
  P -- PIN / biometric confirm --> p32
  p32 --> p33
  LP -- device key, last counter, prevHash, cumSpent --> p33
  p33 -- voucher (SIGNED) --> LP
  p33 -- VC QR --> p34
  LM -- JWKS, CRL, open PR, seen counters --> p34
  p34 -- reject reason --> M
  p34 -- accepted VC --> p35
  p35 -- VC + RC (ACCEPTED) --> LM
  p35 -- RC QR --> p36
  p36 -- VC state RECEIPTED --> LP
  p36 -- "Paid · pending sync" --> P
```

## Level 2: 4.0 Sync & Verification
```mermaid
flowchart LR
  L([Device outbox])
  p41((4.1 Authenticate<br/>& rate-limit))
  p42((4.2 Idempotency<br/>check))
  p43((4.3 Order by<br/>device, counter))
  p44((4.4 Crypto<br/>verification))
  p45((4.5 Business rule<br/>checks S2–S7))
  p46((4.6 Persist result<br/>& respond))
  F((6.0 Fraud))
  S((5.0 Settlement))
  D1[(D1 Devices)]; D2[(D2 BCs)]; D3[(D3 Vouchers)]; D6[(D6 Keys/CRL)]; R[(Redis<br/>idempotency)]
  L -- batch + Idempotency-Key --> p41 --> p42
  R <-- key → cached response --> p42
  p42 -- new items --> p43 --> p44
  D6 -- server pub keys --> p44
  D2 -- BC --> p45
  D1 -- device & merchant status --> p45
  D3 -- neighbours, counters --> p45
  p44 -- sig failures --> p46
  p45 -- violations --> F
  p45 -- verified --> S
  S -- settled ids --> p46
  p46 -- vouchers + status --> D3
  p46 -- per-item results + CRL delta --> L
```

## Level 2: 5.0 Ledger & Settlement (brief)
5.1 Lock BC → 5.2 compute fee → 5.3 escrow sufficiency check (else 5.3a risk-reserve path) → 5.4 write journal entry + lines (`Σ = 0` assertion) → 5.5 mark voucher SETTLED → 5.6 publish `VoucherSettled` event (→ notification, merchant read model) → 5.7 audit.
