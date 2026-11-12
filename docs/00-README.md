# Hey Payments: Technical Documentation

Hey Payments is an **offline-first payment gateway**. A payer loads a capped "offline balance" while they have internet. They can then pay a merchant with **no internet on either phone**, using signed QR vouchers. Both phones sync later, and the server settles everything in a double-entry ledger. The model is RBI's offline small-value payments framework and UPI Lite.

> **Simulation only.** No real money, cards, bank accounts or PCI data are processed. A built-in Funding Simulator stands in for banks.

## Stack (matches the existing scaffold)
| Layer | Choice |
|---|---|
| Backend | Java 21, Spring Boot 4.1.x, Maven, package root `com.heypayments` |
| Frontend | Angular 22 (standalone components, signals), TypeScript 6, Vitest, installable PWA |
| Data | PostgreSQL 16, Flyway, Redis 7 |
| Runtime | Docker + docker compose, nginx for the frontend |

## How to read these docs
Read them in order. Each file builds on the previous one.

| # | File | Answers |
|---|---|---|
| 01 | [PRD](01-PRD.md) | What are we building, for whom, and how do we measure success? |
| 02 | [System Architecture](02-system-architecture.md) | What are the big pieces and how do they talk? |
| 03 | [Offline Protocol](03-offline-protocol.md) | How can money move safely with no internet? **(core design)** |
| 04 | [Data Flow Diagrams](04-dfd.md) | How does data move through the system (DFD L0/L1/L2)? |
| 05 | [Data Model](05-data-model.md) | Which tables, which ledger rules? |
| 06 | [API Spec](06-api-spec.md) + [openapi.yaml](openapi.yaml) | Which endpoints, which payloads, which errors? |
| 07 | [Sequences & State Machines](07-sequence-and-state.md) | In what order do things happen, and which states are legal? |
| 08 | [Security & Threat Model](08-security-threat-model.md) | What can go wrong and how do we stop it? |
| 09 | [Frontend Architecture](09-frontend-architecture.md) | How is the Angular PWA organised and made to work offline? |
| 10 | [Testing Strategy](10-testing-strategy.md) | How do we prove it works (TDD)? |
| 11 | [DevOps & Docker](11-devops-docker.md) | How do we run, build and ship it? |
| 12 | [Implementation Plan](12-implementation-plan.md) | In what phases is it built, and when is a phase done? |
| — | [PROGRESS.md](PROGRESS.md) | Phase status, written by the coding agent |
| — | [design/](design/README.md) | UI/UX case study (Canva) and design tokens |
| — | [reviews/](reviews/README.md) | Architect review reports per phase |
| — | [proposals/](proposals/README.md) | Agent-proposed changes to these docs |

Rules for the coding agent are in [`/AGENTS.md`](../AGENTS.md).

## Glossary
| Term | Meaning |
|---|---|
| **Main balance** | Online wallet balance held on the server. |
| **Offline balance** | Money moved out of the main balance into escrow and made spendable offline on one specific device. |
| **Escrow account** | Ledger account `OFFLINE_ESCROW:<walletId>` that holds offline-loaded funds until vouchers settle. |
| **Balance Certificate (BC)** | Server-signed statement: "device D may spend up to X paise offline, counter starts at N, valid until T". |
| **Payment Request (PR)** | Merchant-signed QR: amount, merchant id, nonce, timestamp. |
| **Voucher** | Payer-device-signed payment: references the BC and PR, carries a monotonic counter and the previous voucher's hash. |
| **Receipt** | Merchant-device-signed acknowledgement of a voucher. |
| **Hash chain** | Each voucher includes `prevHash = SHA-256(previous voucher)`, so a device cannot silently reuse or reorder spends. |
| **Sync** | Upload of outbox items (vouchers/receipts) when a device is back online. |
| **Settlement** | Server-side ledger posting that moves money from escrow to the merchant. |
| **Double spend** | Two vouchers that claim the same counter/balance. Detected at sync; causes a fraud case and a device freeze. |
| **CRL** | Certificate (device) revocation list, cached on merchant devices for offline checks. |
| **Paise** | All amounts are integer minor units (₹1 = 100 paise). Never floating point. |

## Traceability checklist
Every feature traces through: PRD requirement → DFD process → API endpoint → phase task → test.

| PRD req | DFD process | API | Phase |
|---|---|---|---|
| FR-1 Register & login | 1.0 | `/api/v1/auth/*` | 1 |
| FR-2 Register device key | 1.0 | `/api/v1/devices` | 1 |
| FR-3 Fund main wallet (simulator) | 2.0 | `/api/v1/wallets/me/fund` | 2 |
| FR-4 Load/unload offline balance | 2.0 | `/api/v1/offline/load`, `/unload` | 3 |
| FR-5 Merchant payment request QR | 3.0 | client-only (+ `/api/v1/merchants/me/keys`) | 5 |
| FR-6 Offline pay & verify | 3.0 | client-only | 5 |
| FR-7 Sync & settle | 4.0, 5.0 | `/api/v1/sync` | 6 |
| FR-8 Double-spend detection | 6.0 | internal + `/api/v1/admin/fraud-cases` | 6 |
| FR-9 Merchant dashboard & settlements | 5.0, 7.0 | `/api/v1/merchants/me/*` | 7 |
| FR-10 Admin console | 7.0 | `/api/v1/admin/*` | 7 |
| FR-11 Transaction history & receipts | 5.0 | `/api/v1/transactions` | 6–7 |

## UI/UX syllabus coverage (Canva case study)
See [design/README.md](design/README.md) for the page-by-page map. All of Units I–V are covered.
