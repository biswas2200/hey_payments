## Phase 2: Ledger & Wallet: READY FOR REVIEW (2026-11-14)
**Branch:** phase/2-ledger   **Commits:** 17   **Tag:** (after approval)

### Built
- **Contract & Auth Completion:** Replaced handwritten `HttpClient` calls in `AuthStore` with generated OpenAPI services (`authVerifyOtpPost`, `authLogoutPost`, `authPinPut`, `devicesMeGet`). Extended `OpenApiContractTest` to validate request/response schema properties for all implemented endpoints.
- **Task 2.1 Ledger Schema:** Flyway migration `V5__ledger.sql` introducing `ledger_accounts`, `journal_entries`, `journal_lines`, system seed accounts (`SYS:BANK_SIM_CLEARING`, `SYS:PLATFORM_FEES`, `SYS:RISK_RESERVE`), and deferred commit-time trigger `check_journal_entry_balanced()` strictly enforcing $\Sigma = 0$ and $\ge 2$ lines per entry.
- **Task 2.2 Double-Entry Ledger Core:** `LedgerService.post()` enforcing transactional postings, reference idempotency, optimistic locking, non-negative balances for user accounts, and financial audit event emission (`JOURNAL_ENTRY_POSTED`).
- **Task 2.3 Ledger Verification & Invariants:** Jqwik property-based tests verifying money conservation and rejection of unbalanced entries, and a 100-thread concurrent transfer integration test verifying 0 invariant diffs and 0 leaked paise.
- **Task 2.4 Wallet Lifecycle & Funding Simulator:**
  - `V6__wallets.sql` migration for user wallets.
  - `WalletEventListener` automatically provisioning wallets on `UserRegisteredEvent`.
  - `POST /api/v1/wallets/me/fund` with daily ₹50,000 limit, pessimistic write locking (`PESSIMISTIC_WRITE`) preventing concurrency races, and `Idempotency-Key` requirement returning `Idempotent-Replayed: true`.
- **Task 2.5 Payer Home & Add-Money Screens:**
  - Angular 22 standalone `HomeComponent` with Calm Ledger v1 design tokens, displaying `main_balance`, `offline_escrow`, connectivity status, and logout action.
  - `FundComponent` featuring quick amount chips (+₹100, +₹500, +₹1,000, +₹2,000), daily limit validation, persistent idempotency key across network retry, and WCAG 2.2 AA compliant touch targets ($\ge 44$px).
- **Task 2.6 E2E & Visual Verification:** Playwright tests (`e2e/wallet.spec.ts`) covering full funding flow, daily limit enforcement, and visual captures (`screenshots.spec.ts`) in light and dark modes.

### Tests
- **Backend:** 151 tests, line 90.58% (ledger: 90.53%, crypto: 81.17%)
- **Frontend:** 119 tests (21 test files), line 92.02% (`core/auth`: 95.48%, `core/crypto`: 93.75%, `core/offline`: 100%)
- **E2E:** 9 scenarios run / 9 passed (smoke, visual screenshots light/dark, auth routes, and wallet funding flow)

### Gate Results
- `make lint` ✅ (Spotless, Checkstyle 0 violations, OpenAPI sync, ESLint, Prettier)
- `make test` ✅ (Backend surefire random order + Frontend Vitest single-worker)
- `make e2e` ✅ (Playwright 9/9 passed against Docker stack)
- `make scan` ✅ (0 port exposure issues, valid production env resolution)

### Bug-Hunt Agent
- Report: `docs/reviews/AGENT-REVIEW-phase-2.md`
- Tally: **4 HIGH, 5 MEDIUM, 5 LOW**
  - All 4 HIGH findings resolved test-first:
    - [HIGH-1] RBAC `@PreAuthorize("hasRole('PAYER')")` on wallet endpoints.
    - [HIGH-2] Cross-user idempotency key collision / IDOR check in `IdempotencyService`.
    - [HIGH-3] Mandatory `Idempotency-Key` header with UUID format validation.
    - [HIGH-4] Concurrency race condition on daily ₹50,000 funding cap resolved via pessimistic row lock.
  - All 5 MEDIUM findings resolved test-first:
    - [MEDIUM-1] `Math.addExact()` overflow protection + `@Max(5_000_000)` constraint on `FundRequest`.
    - [MEDIUM-2] `LedgerInvariantsPropertyTest` refactored to directly exercise production domain rules.
    - [MEDIUM-3] `Idempotent-Replayed: true` header emitted on replayed requests.
    - [MEDIUM-4] Client-side idempotency key retained across retry attempts.
    - [MEDIUM-5] Phase 2 endpoints and schemas added to `OpenApiContractTest`.
  - 4 LOW findings resolved:
    - [LOW-1] WCAG 2.2 AA 44px min touch targets for buttons/chips.
    - [LOW-2] Visible `:focus-visible` rings and `prefers-reduced-motion` overrides.
    - [LOW-3] Audit logging in `WalletService` with `userId` actor.
    - [LOW-5] Template restricted strictly to OpenAPI `SIM_BANK`.
    - [LOW-4] Triaged: Redis TTL cleanup scheduled for Phase 3 sync pipeline.

### Deviations from Spec / Proposals
- None. All implementation strictly adheres to `docs/03-offline-protocol.md`, `docs/05-data-model.md`, `docs/06-api-spec.md`, and `docs/12-implementation-plan.md`.

### Known Issues / Next
- Next phase: Phase 3 (Offline load/unload protocol, offline balance allocation, and Barkhausen cryptogram lifecycle).

### How to Demo
1. Start stack: `make up`
2. Open `http://localhost:8081/auth/register` (or `https://localhost:8443` with proxy).
3. Register a payer (`+919876543210`), retrieve OTP from Mailpit at `http://localhost:8025`, and complete registration.
4. Log in, register device, set PIN.
5. Land on Payer Home (`/pay`). Observe ₹0.00 main balance and ₹0.00 offline balance.
6. Click **Add +**, select `+₹500` chip, and click **Add ₹500 to Wallet**.
7. Observe immediate success confirmation and updated balance of `₹500.00`.
