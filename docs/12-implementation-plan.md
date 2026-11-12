# 12 · Implementation Plan

Work **one phase at a time**. Each phase ends at a **gate**: Definition of Done met → agent bug-hunt pass → `PROGRESS.md` updated → **STOP** and wait for the architect review (`docs/reviews/REVIEW-phase-N.md`). Do not start the next phase until the review says **APPROVED**.

## Global Definition of Done (applies to every phase)
- [ ] All tasks for the phase are done; acceptance criteria are demonstrated by automated tests.
- [ ] TDD commit order visible for domain logic (test → feat → refactor).
- [ ] `make lint` and `make test` green **inside Docker**; coverage gates met (doc 10 §3).
- [ ] `make up` starts all services healthy; no errors in logs at startup.
- [ ] Spring Modulith `verify()` and ArchUnit tests green.
- [ ] OpenAPI generated spec matches `docs/openapi.yaml` (or a proposal explains the difference).
- [ ] No TODO/FIXME without a linked issue in PROGRESS.md; no commented-out code; no secrets.
- [ ] Bug-hunt agent report saved to `docs/reviews/AGENT-REVIEW-phase-N.md`, and all HIGH/MEDIUM findings fixed with regression tests.
- [ ] `docs/PROGRESS.md` updated with the template in AGENTS.md §9.

---

## Phase 0: Foundation (scaffold hardening)
**Goal:** a reproducible, dockerised skeleton with CI.
| # | Task |
|---|---|
| 0.1 | Root `.gitignore`, `.editorconfig`, `.env.example`, `Makefile`, `README.md` (run instructions) |
| 0.2 | Backend: add dependencies (security, data-jpa, postgresql, flyway, data-redis, spring-modulith, springdoc, testcontainers, archunit, jqwik, bucket4j); Spotless + Checkstyle + JaCoCo with gates |
| 0.3 | Restructure into package-by-feature (`com.heypayments.shared`, …); move `HealthController`/`WebCorsConfig` into `shared` |
| 0.4 | `shared`: Problem Details handler, `Money` record (long paise), UUIDv7 generator, `Clock` bean, request-id filter, JSON logging |
| 0.5 | Flyway `V1__baseline.sql`; Testcontainers base test class |
| 0.6 | Frontend: ESLint + Prettier, strict TS, `@angular/pwa` added, `tokens.scss` placeholder, app shell with ConnectivityService and status banner |
| 0.7 | Dockerfiles, `docker-compose.yml`, nginx config, healthchecks |
| 0.8 | GitHub Actions CI (lint, tests, build images) |
| 0.9 | `test-vectors/` folder + README |

**Acceptance:** `make up` → frontend shows the app shell with an online/offline banner that reacts to DevTools offline; `/actuator/health` UP; CI green; Modulith verify passes with the empty modules.

## Phase 1: Auth, users, devices
| # | Task |
|---|---|
| 1.1 | V2/V3 migrations; `users`, `refresh_tokens`, `devices`, `merchants`, `server_keys` |
| 1.2 | Register / OTP (Mailpit) / login / refresh (rotation + reuse detection) / logout; Argon2id; ES256 JWT |
| 1.3 | SecurityFilterChain deny-by-default, role + ownership checks, rate limiting on auth |
| 1.4 | `crypto` module: server key generation/encryption/rotation, JWKS endpoint, JCS, ECDSA P1363 sign/verify; **generate test vectors** |
| 1.5 | Device registration (one active per user/kind), merchant certificate issuance, CRL endpoint |
| 1.6 | Frontend: auth screens, AuthStore, interceptors (token refresh), KeyService (non-extractable key in IndexedDB), device registration, set-PIN flow; generated API client |
| 1.7 | Frontend crypto passes all shared test vectors |

**Acceptance:** register payer + merchant via UI, device registered with a non-extractable key, merchant receives an MC verifiable on the client; wrong-role access → 403; refresh-token reuse → family revoked.

## Phase 2: Ledger, wallet, funding simulator
| # | Task |
|---|---|
| 2.1 | V4/V5 migrations; system accounts seed |
| 2.2 | `LedgerService.post(entry)` with Σ=0 enforcement, optimistic locking, idempotent reference, balance queries |
| 2.3 | Property-based tests of ledger invariants; concurrency test |
| 2.4 | Wallet creation on PAYER registration (event), `GET /wallets/me`, `POST /wallets/me/fund` (simulator, daily cap), idempotency filter (Redis) |
| 2.5 | Audit log for postings |
| 2.6 | Frontend: payer home (balances), add-money flow |

**Acceptance:** funding updates the balance; replaying the same Idempotency-Key does not double-credit; 100 concurrent fund requests keep the ledger balanced.

## Phase 3: Offline balance (load/unload, Balance Certificates)
| # | Task |
|---|---|
| 3.1 | V6 migration; BC issuance per protocol §3.1 (supersede, counterStart, limits) |
| 3.2 | `POST /offline/load` with `SYNC_REQUIRED` precondition; `POST /offline/unload`; expiry sweep job |
| 3.3 | CRL includes CLOSED BCs/devices |
| 3.4 | Frontend: load-offline flow (education screen → amount → confirm → BC stored + verified), offline balance card, unload |

**Acceptance:** load moves main→escrow and returns a BC that verifies on the client; caps enforced; unload returns the remainder; the expiry sweep works with a fake clock.

## Phase 4: Design system and app shell (Calm Ledger v1, approved)
**Inputs (read all before starting):** `docs/design/ui-spec.md`, `docs/design/reference/screens.html` (open it in a browser), `docs/design/reference/screens/*.png`, `docs/design/tokens.scss`, `docs/design/brand/`, `docs/design/wireframes/`.
| # | Task |
|---|---|
| 4.1 | **Replace** `frontend/src/styles/tokens.scss` with `docs/design/tokens.scss` (the `--hp-*` names are canonical; rename any `--hp-color-*` usages). Self-host fonts via `@fontsource/*`. Add `lucide-angular` |
| 4.2 | Brand: logo SVGs into `src/assets/brand/`, PWA `manifest.webmanifest` + icons from `docs/design/brand/app-icon-*`, favicon from the logo mark, theme-color `#0F766E` |
| 4.3 | Build `shared/ui` components in the order of ui-spec §3, each with unit + axe tests, shown on a dev-only `/dev/ui` route that mirrors section 2 of `reference/screens.html` |
| 4.4 | Apply them to the screens that exist so far (shell, onboarding, auth, P1 Home, P2 Load offline) exactly like the reference PNGs. i18n en/hi scaffold. Dark, large-text and reduced-motion support |
| 4.5 | Playwright visual tests at 390×844 for every implemented reference screen (`toHaveScreenshot`), with demo seed data matching the reference |
| 4.6 | Lighthouse: PWA installable, accessibility ≥ 95 |

**Acceptance:** side-by-side with `reference/screens/*.png`, layout, hierarchy, colour, type and copy match. Logo and icons are the provided SVGs (not redrawn). axe reports 0 serious/critical issues. The app is installable. Later phases (5–7) must build P3–P6, M1–M4 and A1 to the same reference and add a visual test for each.

## Phase 5: Offline payment on devices
| # | Task |
|---|---|
| 5.1 | `core/crypto`: Base45, raw DEFLATE, EnvelopeCodec, multipart frames |
| 5.2 | `core/protocol`: PrBuilder/Verifier (P1–P6), VoucherBuilder (counter, prevHash, cumSpent), VoucherVerifier (M1–M8), Receipt build/verify; 100% unit tests incl. vectors |
| 5.3 | Dexie schema + atomic sign-and-save; outbox states |
| 5.4 | Merchant POS: keypad → PR QR → scan voucher → verify → receipt QR → success |
| 5.5 | Payer scan-pay: scan PR → confirm with PIN → voucher QR → scan receipt → done; void flow |
| 5.6 | QR scanner/display components with torch, multipart and manual fallback |
| 5.7 | Playwright offline E2E scenario 1–2 (doc 10 §5) |

**Acceptance:** with both contexts offline, a full payment completes; tampered/duplicate/expired cases show the correct friendly errors.

## Phase 6: Sync, settlement, fraud
| # | Task |
|---|---|
| 6.1 | V7/V8 migrations |
| 6.2 | `POST /sync` per-item processing, ordering, idempotency, S1–S7 |
| 6.3 | SettlementService postings incl. fee, risk-reserve path, HELD/REVERSED with the claim-window job |
| 6.4 | Fraud rules + cases + automatic freeze + CRL update |
| 6.5 | Notifications on settlement/fraud (in-app + Mailpit) |
| 6.6 | Frontend SyncEngine (triggers, backoff, background sync), status mapping, history with timeline |
| 6.7 | E2E scenarios 3–5 |

**Acceptance:** all doc 03 §9 edge cases are covered by tests; the double-spend E2E produces a HIGH case and a frozen device; the ledger reconciliation endpoint shows zero drift.

## Phase 7: Merchant dashboard and Admin console
| # | Task |
|---|---|
| 7.1 | Merchant summary, settlements list, CSV export, payouts (simulated) |
| 7.2 | Admin: KPIs (HEART/AARRR counters), users, devices (freeze/unfreeze), ledger explorer, reconciliation, fraud case workflow, limits editor |
| 7.3 | V9 reporting views |
| 7.4 | Frontend screens per Canva; charts accessible (table fallback) |

**Acceptance:** an admin can investigate and resolve a fraud case end-to-end; a merchant can export settlements; all admin actions are audited.

## Phase 8: Hardening and release
| # | Task |
|---|---|
| 8.1 | Security pass against the ASVS checklist; `make scan` clean of HIGH issues |
| 8.2 | PIT mutation tests on ledger/voucher (≥ 70%) |
| 8.3 | Performance: sync p95 ≤ 300 ms for 20 items (k6 or Gatling script in `perf/`) |
| 8.4 | Accessibility audit: axe + manual screen reader + keyboard pass; fix all AA issues |
| 8.5 | Usability-test build: in-app feedback widget + analytics events (for the UX report) |
| 8.6 | Docs polish: README with screenshots, demo script, architecture diagrams updated to match the code |

**Acceptance:** the demo script runs end-to-end on two phones over LAN (HTTPS); all gates green; tag `v1.0.0`.
