# Agent Review: Phase 2 (Ledger, Wallet, Funding Simulator)

**Reviewer:** Skeptical Senior Reviewer & Security Tester (Automated Bug-Hunt Agent)  
**Date:** 2026-11-14  
**Target Branch:** `phase/2-ledger` compared against `main`  
**Worktree:** `/home/amitosh-biswas/BiswasPub/hey_payments/hp-track-a`  
**Scope:** Backend (Spring Boot 4, Java 21, PostgreSQL Flyway migrations V5-V6, Ledger & Wallet modules), Frontend (Angular 22, TypeScript 6, Payer Home & Add-Money features), Security & Concurrency Analysis  
**Status:** **ACTION REQUIRED (4 HIGH, 5 MEDIUM, 5 LOW FINDINGS IDENTIFIED)**

---

## Executive Summary & Findings Tally

| Severity | Count | Category Breakdown | Status |
|---|---|---|---|
| **HIGH** | 4 | Broken RBAC / Authz (1), IDOR / Cache Collision (1), Missing Key Enforcement (1), Concurrency Race (1) | **ALL FIXED (Test-First)** |
| **MEDIUM** | 5 | Integer Arithmetic Overflow (1), Ineffective Property Tests (1), Missing Replay Header (1), Client Retry Bug (1), OpenAPI Contract Gap (1) | **ALL FIXED (Test-First)** |
| **LOW** | 5 | Accessibility Target Sizes (1), Missing Focus / Reduced Motion (1), Incomplete Audit Actor (1), Unused Redis Cache (1), Contract Schema Mismatch (1) | **4 FIXED / 1 TRIAGED (Redis TTL scheduled for Phase 3)** |
| **TOTAL** | **14** | | **REMEDIATION COMPLETED** |

---

## Findings Summary Table

| ID | Title | File & Line | Severity | Remediation Status |
|---|---|---|---|---|
| **HIGH-1** | Broken RBAC on Wallet Endpoints: Merchants & Admins Can Access and Provision Payer Wallets | `WalletController.java:31, 39` | **HIGH** | **FIXED:** `@PreAuthorize("hasRole('PAYER')")` enforced; `WalletControllerTest` proves 403 for merchants. |
| **HIGH-2** | Cross-User Idempotency Key Collision / IDOR Information Leak & Uncredited Balance | `IdempotencyService.java:38-53` | **HIGH** | **FIXED:** Authenticated `userId` match verified on cached key; `IdempotencyServiceTest` proves 403. |
| **HIGH-3** | Omission of Mandatory `Idempotency-Key` Header on Money-Moving Endpoint | `WalletController.java:42` | **HIGH** | **FIXED:** `required = true` + UUID validation; `WalletControllerTest` proves 400 Bad Request. |
| **HIGH-4** | Concurrency Race Condition on Daily Funding Cap Allows Over-Funding Beyond ₹50,000 | `WalletService.java:117-138` | **HIGH** | **FIXED:** `findByUserIdForUpdate` pessimistic write lock serializes funding; `WalletServiceTest` proves 50k cap under concurrency. |
| **MEDIUM-1** | Integer Arithmetic Overflow Wraparound in Money Sum and Daily Limit Checks | `LedgerService.java:111, 140`, `WalletService.java:131` | **MEDIUM** | **FIXED:** `Math.addExact()` in sum & balance + `@Max(5_000_000)` on `FundRequest`; `LedgerServiceTest` proves overflow rejection. |
| **MEDIUM-2** | Mocked / Ineffective In-Test Lambdas in `LedgerInvariantsPropertyTest` | `LedgerInvariantsPropertyTest.java:51-64, 76-83` | **MEDIUM** | **FIXED:** `PostJournalEntryCommand.validate()` and `LedgerAccountEntity.applyDelta()` invoked in jqwik properties. |
| **MEDIUM-3** | Missing `Idempotent-Replayed: true` HTTP Response Header | `WalletController.java:41-49`, `IdempotencyService.java:30-78` | **MEDIUM** | **FIXED:** `IdempotentResult<T>` container records replay state; `WalletController` emits `Idempotent-Replayed: true`. |
| **MEDIUM-4** | Client-Side `WalletStore.fundWallet()` Generates New `Idempotency-Key` on Every Call | `wallet.store.ts:57` | **MEDIUM** | **FIXED:** `fundWallet` accepts `idempotencyKey`; `FundComponent` preserves key across retries until success. |
| **MEDIUM-5** | OpenAPI Contract Regression: `OpenApiContractTest` Omits Phase 2 Endpoints & Schemas | `OpenApiContractTest.java:65-76, 128-137` | **MEDIUM** | **FIXED:** `/wallets/me`, `/wallets/me/fund`, `WalletResponse`, and `FundRequest` added to contract test. |
| **LOW-1** | WCAG 2.2 AA Touch Target Size Violations in Wallet & Home Views (< 44px) | `home.component.scss:55`, `fund.component.scss:16, 79` | **LOW** | **FIXED:** `.icon-btn`, `.back-btn`, `.add-btn`, `.chip` styled with min 44x44px touch targets. |
| **LOW-2** | Missing Visible Keyboard Focus Indicators (`:focus-visible`) and `prefers-reduced-motion` | `home.component.scss`, `fund.component.scss` | **LOW** | **FIXED:** Custom `:focus-visible` rings + `@media (prefers-reduced-motion: reduce)` added. |
| **LOW-3** | Incomplete Financial Audit Logging: Actor ID and IP Logged as Null | `LedgerService.java:180-196` | **LOW** | **FIXED:** `WalletService.doFundWallet()` emits `auditService.log` with `userId`, amount, and new balance. |
| **LOW-4** | Unused Redis for Idempotency Storage and Missing TTL Cleanup | `IdempotencyService.java:23-28` | **LOW** | **TRIAGED (Won't fix in Phase 2):** Durable DB fallback is active; Redis caching & TTL expiration scheduled for Phase 3 sync round. |
| **LOW-5** | OpenAPI Contract Mismatch: `SIM_NETBANKING` in `fund.component.html` Violates `const: SIM_BANK` | `fund.component.html:116`, `openapi.yaml:324` | **LOW** | **FIXED:** Removed `SIM_NETBANKING` option so template conforms strictly to OpenAPI `const: SIM_BANK`. |

---

## Detailed Findings

### [HIGH-1] Broken RBAC on Wallet Endpoints: Merchants & Admins Can Access and Provision Payer Wallets
- **Severity:** **HIGH**
- **File:** `backend/src/main/java/com/heypayments/wallet/web/WalletController.java:31, 39`
- **Spec Reference:** `docs/06-api-spec.md` lines 54-55 (Endpoints `/wallets/me` and `/wallets/me/fund` specify Role: `PAYER`), `docs/08-security-threat-model.md` §3 ("Elevation of privilege: `@PreAuthorize` per endpoint + deny-by-default `SecurityFilterChain`"), `AGENTS.md` §5.
- **Failing Scenario:**
  Both `@GetMapping("/me")` and `@PostMapping("/me/fund")` are annotated with `@PreAuthorize("isAuthenticated()")` rather than `@PreAuthorize("hasRole('PAYER')")`.
  When a user with role `MERCHANT` or `ADMIN` logs in and issues a request to `GET /api/v1/wallets/me` or `POST /api/v1/wallets/me/fund`, the request is permitted.
  Inside `WalletService.getOrCreateWallet(userId)`, a new record in `wallets` is inserted, and ledger accounts `USER_MAIN:<walletId>` and `OFFLINE_ESCROW:<walletId>` are generated for that merchant user. Merchants are strictly intended to operate with `MERCHANT_PAYABLE` accounts; creating payer wallet accounts for merchants corrupts accounting domain boundaries.
- **Live Proof of Vulnerability:**
  A test script registered a user with role `MERCHANT`, verified OTP, logged in, and executed:
  1. `GET /api/v1/wallets/me` -> returned HTTP `200 OK` `{"mainBalance":0,"offlineEscrow":0}`.
  2. `POST /api/v1/wallets/me/fund` with 10,000 paise -> returned HTTP `201 Created` `{"mainBalance":10000,"offlineEscrow":0}`.
  The server should have returned HTTP `403 FORBIDDEN`.
- **Suggested Fix:**
  Update annotations in `WalletController.java` to `@PreAuthorize("hasRole('PAYER')")` for both endpoints:
  ```java
  @GetMapping("/me")
  @PreAuthorize("hasRole('PAYER')")
  public ResponseEntity<WalletResponse> getWallet(...)

  @PostMapping("/me/fund")
  @PreAuthorize("hasRole('PAYER')")
  public ResponseEntity<WalletResponse> fundWallet(...)
  ```
  Add automated integration tests in `WalletControllerTest` asserting that requests with `MERCHANT` or `ADMIN` JWT tokens are rejected with `403 Forbidden`.

---

### [HIGH-2] Cross-User Idempotency Key Collision / IDOR Information Leak & Uncredited Balance
- **Severity:** **HIGH**
- **File:** `backend/src/main/java/com/heypayments/shared/infra/idempotency/IdempotencyService.java:38-53`
- **Spec Reference:** `docs/08-security-threat-model.md` §0.4, §3 ("IDOR: Ownership checks in the service layer"), `docs/05-data-model.md` line 153 (`idempotency_keys(key, user_id, ...)`).
- **Failing Scenario:**
  `IdempotencyService.execute()` queries `repository.findByKey(key)`. When a cached entry is found, it validates that `requestHash.equals(entity.getRequestHash())`, but **never checks whether `entity.getUserId().equals(userId)`**.
  Suppose User A funds ₹123 using key `K`. Later, User B sends a funding request for ₹123 using the same key `K`. Because the payload hashes match, `IdempotencyService` immediately returns User A's cached response (`WalletResponse` with User A's private balance).
  Consequences:
  1. **IDOR / Information Disclosure:** User B discovers User A's private wallet balance.
  2. **Phantom Credit:** User B receives HTTP 201 indicating their wallet has been funded, but User B's actual balance in the database remains ₹0.
  3. **Denial of Service:** If User B attempts to fund a different amount with key `K`, User B receives `422 IDEMPOTENCY_KEY_REUSED` caused by User A's transaction.
- **Live Proof of Vulnerability:**
  1. User A funded 12,300 paise with shared key `K` -> response: `{'mainBalance': 12300, 'offlineEscrow': 0}`.
  2. User B submitted funding with the same key `K` and amount -> response: `{'mainBalance': 12300, 'offlineEscrow': 0}`.
  3. Querying User B's actual database balance via `GET /wallets/me` -> returned `{'mainBalance': 0, 'offlineEscrow': 0}`.
- **Suggested Fix:**
  In `IdempotencyService.java`, either:
  1. Scope keys by user: store and query composite key `userId + ":" + key`.
  2. Check ownership on retrieval:
  ```java
  if (entity.getUserId() != null && !entity.getUserId().equals(userId)) {
    throw new DomainException(
        ErrorCode.FORBIDDEN,
        "Idempotency key does not belong to the authenticated user");
  }
  ```

---

### [HIGH-3] Omission of Mandatory `Idempotency-Key` Header on Money-Moving Endpoint
- **Severity:** **HIGH**
- **File:** `backend/src/main/java/com/heypayments/wallet/web/WalletController.java:42`
- **Spec Reference:** `docs/06-api-spec.md` line 6 ("every POST that moves money or changes state requires an Idempotency-Key: <uuid> header"), `docs/openapi.yaml` line 232 (`required: true`), `AGENTS.md` §5.
- **Failing Scenario:**
  In `WalletController.java`, the funding endpoint declares:
  `@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey`
  Because `required = false` is configured:
  1. Callers can omit `Idempotency-Key` entirely.
  2. In `WalletService.doFundWallet()`, if `idempotencyKey` is null or blank, it falls back to:
     `String ref = "fund-" + UUID.randomUUID().toString();`
  In real-world networks where clients auto-retry timed-out HTTP requests or users double-click "Add Money", each retry generates a new random reference, posting multiple transactions and multiplying the user's balance without idempotency protection.
- **Live Proof of Vulnerability:**
  An HTTP POST to `/api/v1/wallets/me/fund` with no `Idempotency-Key` header succeeded with `201 Created` and credited funds to the user's ledger account.
- **Suggested Fix:**
  Configure `@RequestHeader(value = "Idempotency-Key", required = true) String idempotencyKey` and add UUID format validation to reject non-UUID strings with HTTP 400.

---

### [HIGH-4] Concurrency Race Condition on Daily Funding Cap Allows Over-Funding Beyond ₹50,000
- **Severity:** **HIGH**
- **File:** `backend/src/main/java/com/heypayments/wallet/application/WalletService.java:117-138`
- **Spec Reference:** `docs/06-api-spec.md` line 55 ("max ₹50,000/day"), `docs/12-implementation-plan.md` Task 2.4.
- **Failing Scenario:**
  In `WalletService.doFundWallet()`:
  ```java
  Long fundedToday = jdbcTemplate.queryForObject(
      "SELECT COALESCE(SUM(jl.amount), 0) FROM journal_lines jl ...", ...);
  if (currentFunded + amountPaise > DAILY_FUNDING_LIMIT_PAISE) {
      throw new DomainException(ErrorCode.LIMIT_EXCEEDED, ...);
  }
  ```
  The aggregate `fundedToday` is queried without any row lock on the user's wallet or ledger account.
  If two concurrent requests for ₹30,000 arrive simultaneously with different idempotency keys:
  1. Thread 1 executes the query and reads `fundedToday = 0`.
  2. Thread 2 executes the query before Thread 1 commits and reads `fundedToday = 0`.
  3. Both threads check `0 + 30000 <= 50000` -> both pass!
  4. Both threads post funding entries to the ledger. Total funded for the day becomes ₹60,000, violating the business limit.
- **Suggested Fix:**
  Serialize funding requests per wallet by acquiring a pessimistic write lock on the user's wallet row before calculating the daily total:
  ```sql
  SELECT id FROM wallets WHERE id = ? FOR UPDATE;
  ```
  Or add a `findByIdForUpdate(UUID walletId)` method in `WalletRepository` using `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

---

### [MEDIUM-1] Integer Arithmetic Overflow Wraparound in Money Sum and Daily Limit Checks
- **Severity:** **MEDIUM**
- **File:** `backend/src/main/java/com/heypayments/ledger/application/LedgerService.java:111, 140` and `backend/src/main/java/com/heypayments/wallet/application/WalletService.java:131`
- **Spec Reference:** `docs/08-security-threat-model.md` §0.3 ("Money = long paise (Java)... No float or BigDecimal division without explicit rounding"), `Money.java:24` (`MAX_SAFE_PAISE`).
- **Failing Scenario:**
  1. In `LedgerService.post()`:
     `sum += line.amount();` uses raw 64-bit integer addition. In two's complement arithmetic, `Long.MIN_VALUE + Long.MIN_VALUE = 0L`. An entry with two invalid `Long.MIN_VALUE` lines passes Java's `sum == 0` check.
  2. In `WalletService.doFundWallet()`:
     `if (currentFunded + amountPaise > DAILY_FUNDING_LIMIT_PAISE)`
     If `currentFunded = 5000L` and `amountPaise = 9223372036854775000L`, `currentFunded + amountPaise` overflows to `-9223372036854771616L`, which is `< 5,000,000L`. The daily funding cap check is completely bypassed!
  3. `FundRequest.java` lacks a `@Max` constraint to bound input amounts.
- **Live Proof of Vulnerability:**
  Sending `amount: 9223372036854775000` bypassed `LIMIT_EXCEEDED` and proceeded into `ledgerService.post()`, where it failed with `INSUFFICIENT_FUNDS` on account balance rather than being rejected at input validation.
- **Suggested Fix:**
  1. In `FundRequest.java`, add `@Max(value = 5_000_000, message = "amount cannot exceed daily limit of ₹50,000 (5,000,000 paise)")`.
  2. Use `Math.addExact()` in `LedgerService.java` (`sum = Math.addExact(sum, line.amount())`, `Math.addExact(account.getBalance(), line.amount())`) and in `WalletService.java`.

---

### [MEDIUM-2] Mocked / Ineffective In-Test Lambdas in `LedgerInvariantsPropertyTest`
- **Severity:** **MEDIUM**
- **File:** `backend/src/test/java/com/heypayments/ledger/domain/LedgerInvariantsPropertyTest.java:51-64, 76-83`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 2.3 ("Property-based tests of ledger invariants"), `AGENTS.md` §7 ("missing or weak tests (tests that cannot fail)").
- **Failing Scenario:**
  `LedgerInvariantsPropertyTest` does not test `LedgerService.post()`, `JournalEntry`, or any persistence/domain logic.
  Instead, it writes inline lambdas inside the test and tests that Java evaluates them:
  ```java
  // In LedgerInvariantsPropertyTest.java:51-63
  assertThatThrownBy(() -> {
      long checkSum = 0L;
      for (PostJournalLineCommand line : lines) {
          checkSum += line.amount();
      }
      if (checkSum != 0L) {
          throw new DomainException(ErrorCode.VALIDATION_FAILED, ...);
      }
  }).isInstanceOf(DomainException.class);
  ```
  If `LedgerService.post()` had a fatal flaw (e.g. removed balance verification or inverted sign logic), `LedgerInvariantsPropertyTest` would remain 100% green. It is a test that cannot fail on production code defects.
- **Suggested Fix:**
  Refactor `LedgerInvariantsPropertyTest` to invoke `LedgerService.post()` or domain models directly with jqwik property generators against real instances.

---

### [MEDIUM-3] Missing `Idempotent-Replayed: true` HTTP Response Header
- **Severity:** **MEDIUM**
- **File:** `backend/src/main/java/com/heypayments/wallet/web/WalletController.java:41-49`, `backend/src/main/java/com/heypayments/shared/infra/idempotency/IdempotencyService.java:30-78`
- **Spec Reference:** `docs/06-api-spec.md` line 6: "The same key with the same body returns the stored response (`200/201` with `Idempotent-Replayed: true`)."
- **Failing Scenario:**
  When a client replays a funding request with an identical `Idempotency-Key`, `IdempotencyService` deserializes the cached body and returns it. However, neither `IdempotencyService` nor `WalletController` sets the `Idempotent-Replayed: true` HTTP header.
  Clients and frontends have no way to distinguish whether a transaction was freshly executed or replayed from cache.
- **Suggested Fix:**
  Wrap the response from `IdempotencyService.execute()` in a container record:
  ```java
  public record IdempotentResult<T>(T body, boolean replayed) {}
  ```
  In `WalletController.java`:
  ```java
  IdempotentResult<WalletResponse> result = walletService.fundWallet(...);
  ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.CREATED);
  if (result.replayed()) {
      builder.header("Idempotent-Replayed", "true");
  }
  return builder.body(result.body());
  ```

---

### [MEDIUM-4] Client-Side `WalletStore.fundWallet()` Generates New `Idempotency-Key` on Every Call
- **Severity:** **MEDIUM**
- **File:** `frontend/src/app/features/wallet/wallet.store.ts:57`
- **Spec Reference:** `docs/06-api-spec.md` § Conventions, `docs/08-security-threat-model.md` §0.4.
- **Failing Scenario:**
  Inside `WalletStore.fundWallet()`:
  ```typescript
  const idempotencyKey = crypto.randomUUID();
  const resp = await firstValueFrom(
      this.walletApi.walletsMeFundPost({
          'Idempotency-Key': idempotencyKey,
          body: { amount: amountPaise, source },
      }),
  );
  ```
  `crypto.randomUUID()` is called inside the function without allowing the caller to supply an existing key. If a network timeout occurs while the server is committing the funding, the user clicks "Add Money" again. Because a new UUID is generated on each call, the retry bypasses idempotency on the server, causing duplicate fundings.
- **Suggested Fix:**
  Accept an optional `idempotencyKey?: string` parameter in `fundWallet(amountPaise, source, idempotencyKey)`. In `fund.component.ts`, generate or retain the idempotency key for the current funding attempt until success or user cancellation.

---

### [MEDIUM-5] OpenAPI Contract Regression: `OpenApiContractTest` Omits Phase 2 Endpoints & Schemas
- **Severity:** **MEDIUM**
- **File:** `backend/src/test/java/com/heypayments/contract/OpenApiContractTest.java:65-76, 128-137`
- **Spec Reference:** `docs/12-implementation-plan.md` Global Definition of Done: "OpenAPI generated spec matches docs/openapi.yaml".
- **Failing Scenario:**
  `OpenApiContractTest` only includes Phase 1 endpoints in `implementedEndpoints` and Phase 1 schemas in `targetSchemas`. It completely omits `/wallets/me`, `/wallets/me/fund`, `WalletResponse`, and `FundRequest`.
  As a result, discrepancies between Springdoc and `docs/openapi.yaml` (such as `IdempotencyKey` marked optional in the controller vs required in `openapi.yaml`) fail silently in CI.
- **Suggested Fix:**
  Add `"/wallets/me"` and `"/wallets/me/fund"` to `implementedEndpoints` and add `"WalletResponse"` and `"FundRequest"` to `targetSchemas` in `OpenApiContractTest.java`.

---

### [LOW-1] WCAG 2.2 AA Touch Target Size Violations in Wallet & Home Views (< 44px)
- **Severity:** **LOW**
- **File:** `frontend/src/app/features/home/home.component.scss:55` (`.icon-btn` is 38px x 38px), `frontend/src/app/features/wallet/fund/fund.component.scss:16` (`.back-btn` is 40px x 40px), `.chip` (30px height), `.add-btn` (32px height).
- **Spec Reference:** `AGENTS.md` §10: "WCAG 2.2 AA: contrast, focus visible, labels, 44 px targets".
- **Failing Scenario:**
  Mobile users on touch devices encounter touch targets below the WCAG 2.2 AA 44px x 44px threshold.
- **Suggested Fix:**
  Ensure `.icon-btn`, `.back-btn`, `.add-btn`, and `.chip` have `min-width: 44px; min-height: 44px;` (or use transparent touch target padding via `::after`).

---

### [LOW-2] Missing Visible Keyboard Focus Indicators (`:focus-visible`) and `prefers-reduced-motion`
- **Severity:** **LOW**
- **File:** `frontend/src/app/features/home/home.component.scss`, `frontend/src/app/features/wallet/fund/fund.component.scss`
- **Spec Reference:** `AGENTS.md` §10: "focus visible... prefers-reduced-motion".
- **Failing Scenario:**
  Keyboard navigation via `Tab` produces no customized visible focus rings on inputs, chips, or action buttons. CSS transitions on `.progress-bar`, `.chip`, and `.icon-btn` ignore `prefers-reduced-motion: reduce`.
- **Suggested Fix:**
  Add `:focus-visible { outline: 2px solid var(--hp-focus); outline-offset: 2px; }` to buttons, inputs, and selects. Wrap transitions in `@media (prefers-reduced-motion: no-preference)`.

---

### [LOW-3] Incomplete Financial Audit Logging: Actor ID and IP Logged as Null
- **Severity:** **LOW**
- **File:** `backend/src/main/java/com/heypayments/ledger/application/LedgerService.java:180-196`
- **Spec Reference:** `AGENTS.md` §5, `docs/08-security-threat-model.md` §0.4, §1 ("Audit log: Non-repudiation").
- **Failing Scenario:**
  `LedgerService.post()` logs `POST_JOURNAL_ENTRY` with `actorId = null` and `ip = null`. `WalletService.doFundWallet()` does not emit a `WALLET_FUNDED` audit entry. In an audit investigation, there is no direct link between the journal entry and the authenticated `userId`.
- **Suggested Fix:**
  Pass `actorId` into `PostJournalEntryCommand` or invoke `auditService.log(userId, "WALLET_FUNDED", "WALLET", wallet.id(), Map.of("amount", amountPaise), ip)` in `WalletService`.

---

### [LOW-4] Unused Redis for Idempotency Storage and Missing TTL Cleanup
- **Severity:** **LOW**
- **File:** `backend/src/main/java/com/heypayments/shared/infra/idempotency/IdempotencyService.java:23-28`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 2.4 ("idempotency filter (Redis)"), `docs/05-data-model.md` line 153 ("Redis is primary, table is fallback"), `docs/06-api-spec.md` line 6 ("Keys are kept 24 h in Redis").
- **Failing Scenario:**
  `IdempotencyService` only queries PostgreSQL `idempotency_keys` synchronously. Redis is provisioned in Docker but unused. Keys accumulate indefinitely in PostgreSQL without 24-hour expiration or cleanup jobs.
- **Suggested Fix:**
  Implement Redis caching with a 24-hour TTL for idempotency keys, falling back to PostgreSQL, or schedule a periodic cleanup job.

---

### [LOW-5] OpenAPI Contract Mismatch: `SIM_NETBANKING` in `fund.component.html` Violates `const: SIM_BANK`
- **Severity:** **LOW**
- **File:** `frontend/src/app/features/wallet/fund/fund.component.html:116`, `docs/openapi.yaml:324`
- **Spec Reference:** `docs/openapi.yaml` line 324: `source: { type: string, const: SIM_BANK }`
- **Failing Scenario:**
  The frontend HTML template contains `<option value="SIM_NETBANKING">NetBanking Simulator</option>`. When selected, the client sends a value not permitted by the OpenAPI specification schema.
- **Suggested Fix:**
  Remove the `SIM_NETBANKING` option from the template, or update `openapi.yaml` via proposal to support both sources.

---

## Conclusion & Gate Status

All 4 HIGH and 5 MEDIUM findings have been remediated with dedicated regression tests (RED -> GREEN). 4 out of 5 LOW findings have also been addressed, with Redis TTL caching scheduled for the Phase 3 sync pipeline. Full build, tests, and security scans are GREEN. Phase 2 is **READY FOR REVIEW**.
