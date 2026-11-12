# 01 · Product Requirements Document (PRD)

| Field | Value |
|---|---|
| Product | Hey Payments: offline-first payments |
| Status | v1.0 approved for build |
| Owner | Product (Amitosh) · Architect/Reviewer: Claude · Builder: Gemini (Antigravity) |
| Last updated | 2026-11-12 |

## 1. Vision
> *"Pay and get paid anywhere, even when the network isn't there."*

Hey Payments lets people make small everyday payments safely when there is no or patchy connectivity, such as in basements, rural markets, trains, festivals and network outages. It settles automatically once connectivity returns.

## 2. Problem
- Digital payments in India are UPI-first and **need live internet on the payer side**. Failed or pending payments in low-signal areas cause queues, lost sales and mistrust ("did the money go?").
- Shopkeepers in low-connectivity areas fall back to cash: change problems, no records.
- Existing offline options (USSD, UPI Lite X with NFC) are hard to discover, device-limited, or confusing.

**Problem statement (UX format):** *Ravi, a small kirana shop owner in a low-signal market, needs a way to accept digital payments without internet because failed UPI payments make him lose customers and trust.*

## 3. Goals and non-goals
**Goals (MVP)**
1. Payer can load an offline balance (max ₹2,000) while online.
2. Payer can pay a merchant fully offline in under 10 seconds via QR handshake (≤ ₹500 per payment).
3. Merchant gets instant, cryptographically verifiable proof of payment offline.
4. Automatic sync and settlement when either device reconnects; zero double-settlement.
5. Clear status UX everywhere: *Offline · Pending sync · Settled*.
6. Admin can see the ledger, fraud cases and devices, and freeze devices.

**Non-goals (v1)**
- Real bank/NPCI integration, KYC, real card data (simulated only).
- NFC/Bluetooth transport (QR only in v1; designed to be transport-agnostic).
- Native mobile apps (PWA only), multi-currency, P2P transfers between payers, refunds offline (refunds are online-only).

## 4. Users and personas (summary; full versions are in the Canva case study)
| Persona | Role | Key need | Pain point |
|---|---|---|---|
| **Ravi, 42**: kirana owner, semi-urban market | Merchant | Accept payments regardless of signal | Failed UPI payments, cash change, no record keeping |
| **Priya, 27**: daily metro commuter | Payer | Pay quickly in underground or crowded spots | Payments hanging at "processing" |
| **Arjun, 20**: student in a hostel with bad Wi-Fi | Payer | Small payments at canteen/xerox shop | Embarrassment when payment fails in queue |
| **Meera, 35**: operations analyst | Admin | Spot fraud, reconcile settlements | No visibility into offline risk |

## 5. Functional requirements
| ID | Requirement | Priority |
|---|---|---|
| FR-1 | Register and log in with phone + password (+ simulated OTP via Mailpit/console). Roles: PAYER, MERCHANT, ADMIN. | Must |
| FR-2 | On first launch, the device generates an ECDSA P-256 keypair and registers the public key. One active offline device per user. | Must |
| FR-3 | Fund main wallet from the Funding Simulator (fake bank). | Must |
| FR-4 | Load offline balance (main → escrow) and receive a Balance Certificate; unload unused offline balance back (online). | Must |
| FR-5 | Merchant shows a signed Payment Request QR for an amount (static shop QR + dynamic amount). | Must |
| FR-6 | Payer scans, sees merchant name/amount, confirms with PIN (and biometrics where WebAuthn is available), and the device produces a signed Voucher QR. Merchant scans, verifies offline and shows a receipt. | Must |
| FR-7 | Outbox sync on reconnect (background sync + manual "Sync now"); server verifies and settles idempotently. | Must |
| FR-8 | Detect double spend / counter replay / forged signatures; open a fraud case; freeze the device. | Must |
| FR-9 | Merchant dashboard: today's takings, pending vs settled, settlement history, CSV export. | Should |
| FR-10 | Admin console: users, devices, ledger explorer, fraud cases, freeze/unfreeze, system limits. | Should |
| FR-11 | Transaction history with status timeline and shareable receipt. | Must |
| FR-12 | Notifications (in-app + email via Mailpit) on settlement and fraud events. | Could |
| FR-13 | Multilingual UI (English, Hindi) and a large-text mode. | Should |

## 6. Business rules
| Rule | Value (configurable) |
|---|---|
| Max offline balance per device | ₹2,000 (200000 paise) |
| Max single offline payment | ₹500 (50000 paise) |
| Max offline payments before forced sync | 20 vouchers or 4 days, whichever comes first |
| Balance Certificate validity | 4 days |
| Merchant offline acceptance limit (unsynced) | ₹10,000 total pending |
| Minimum amount | ₹1 |

## 7. Non-functional requirements
| Category | Requirement |
|---|---|
| Performance | Offline QR handshake end-to-end ≤ 10 s on a mid-range phone; sync API p95 ≤ 300 ms for 20 vouchers |
| Reliability | Settlement is exactly-once (idempotent); ledger always balances (sum of entries = 0) |
| Security | All offline artefacts signed (ECDSA P-256/SHA-256); private keys non-extractable; OWASP ASVS L2 |
| Availability | Payment works with 100% network loss; server 99.5% (demo target) |
| Accessibility | WCAG 2.2 AA; works with TalkBack/VoiceOver; 200% zoom; touch targets ≥ 44px |
| Usability | SUS ≥ 75 in usability tests; first offline payment completed without help by ≥ 80% of testers |
| Privacy | Minimal PII; no PII inside QR payloads except merchant display name |
| Portability | Runs entirely with `docker compose up` |

## 8. Success metrics
**HEART (Google)**
| Dimension | Goal | Signal | Metric |
|---|---|---|---|
| Happiness | Users trust offline pay | Post-payment rating | CSAT ≥ 4.3/5, SUS ≥ 75 |
| Engagement | Used as a habit | Offline payments per active user | ≥ 8 / week |
| Adoption | Users try offline mode | First offline load | ≥ 40% of new users load within 7 days |
| Retention | Users keep using it | Weekly active payers | W4 retention ≥ 35% |
| Task success | Payments complete | Handshake completion | ≥ 97% success, median ≤ 8 s |

**AARRR (Pirate)**
| Stage | Metric |
|---|---|
| Acquisition | Sign-ups per week by channel (merchant referral QR, campus) |
| Activation | % of sign-ups completing first offline payment within 48 h |
| Retention | D7 / D30 returning payers and merchants |
| Referral | Merchant-invite conversions |
| Revenue | (Simulated) merchant MDR 0.3% on settled volume; GMV settled |

## 9. MVP scope and release plan
- **MVP (Phases 0–6):** auth, devices, wallet, offline load, offline pay, sync & settle, double-spend detection, payer + basic merchant UI.
- **v1.0 (Phases 7–8):** full merchant dashboard, admin console, hardening, accessibility audit.
- **Later:** NFC/BLE transport, refunds, native wrappers (Capacitor), real PSP sandbox integration.

## 10. Assumptions and risks
| Risk | Mitigation |
|---|---|
| Device clone / key extraction to double spend | Caps, non-extractable keys, hash chain + counters, escrow-backed funds, freeze on detection, merchant pending limits |
| Clock skew on offline devices | Use counters (not time) for ordering; time only for expiry with a ±24 h tolerance |
| Camera/QR issues in low light | Manual short-code fallback, adjustable QR density, torch toggle |
| Users confused by "pending sync" | Explicit three-state status language, education screens (see Canva) |

## 11. Open questions
- Should payers be able to receive offline? → **No in v1.**
- Merchant offline keys: server-issued merchant certificate (yes, same scheme as payer devices).
