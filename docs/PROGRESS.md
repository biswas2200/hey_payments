# Progress Log

Maintained by the coding agent. Append one section per phase using the template in `AGENTS.md §9`.

| Phase | Name | Status | Review |
|---|---|---|---|
| 0 | Foundation | APPROVED | docs/reviews/REVIEW-phase-0.md |
| 1 | Auth, users, devices | IN PROGRESS | — |
| 2 | Ledger, wallet, funding | NOT STARTED | — |
| 3 | Offline balance | NOT STARTED | — |
| 4 | Design system & shell | NOT STARTED | — |
| 5 | Offline payment on devices | NOT STARTED | — |
| 6 | Sync, settlement, fraud | NOT STARTED | — |
| 7 | Merchant & admin | NOT STARTED | — |
| 8 | Hardening & release | NOT STARTED | — |

---

## Phase 0: Foundation: APPROVED (2026-11-13)
**Branch:** phase/0-foundation   **Commits:** 68   **Tag:** v0.1.0

### Built
- Task 0.1: Git repository initialized with branch discipline, strict `.gitignore`, pre-commit hooks, and architecture documentation.
- Task 0.2: Backend project skeleton (`backend/pom.xml`, Java 21, Spring Boot 4.1.2, Spring Modulith, Spring Security, Flyway, PostgreSQL, Redis, Bucket4j, Testcontainers, ArchUnit, jqwik, Spotless, Checkstyle, JaCoCo).
- Task 0.3: Shared domain kernel (`com.heypayments.shared` with `Money` value object supporting safe integer bound and Jackson mapping, `UuidV7Generator`, `ErrorCode`, `DomainException`, `FieldErrorDetail`).
- Task 0.4: Web & observability foundation (`RequestIdFilter` with strict alphanumeric sanitization, `GlobalExceptionHandler` mapping domain and Spring MVC client exceptions to RFC 9457 `ProblemResponse`, `ProblemAuthenticationEntryPoint` writing 401 `UNAUTHENTICATED`, `ProblemAccessDeniedHandler` writing 403 `FORBIDDEN`, `TimeConfig` UTC clock, `SecurityConfig` with method security and dev-profile Swagger/OpenAPI exposure, Spring Boot native structured ECS JSON logging).
- Task 0.5: Database migration baseline (`V1__baseline.sql` with `spring-boot-starter-flyway`, `audit_log` append-only trigger and statement-level `TRUNCATE` prevention trigger, `idempotency_keys`, `event_publication`), Testcontainers base integration test, and Spring Modulith verification.
- Task 0.6: Angular 22 PWA scaffold (`frontend/` with strict TypeScript, standalone components, Angular signals, `@angular/pwa` service worker, ESLint with a11y, Prettier, `tokens.scss` with Calm Ledger v1 tokens, `@media (prefers-color-scheme: dark)`, self-hosted fonts with `@fontsource`, brand SVG assets with light and dark wordmark variants, `ConnectivityService`, accessible app shell with live status banner using `@lucide/angular`).
- Task 0.7: Dockerization (`backend/Dockerfile` multi-stage with non-root user `app`, `frontend/Dockerfile` multi-stage with `nginxinc/nginx-unprivileged:1.27-alpine` non-root user `nginx`, `docker-compose.yml` with port isolation binding internal services strictly to `127.0.0.1`, Redis password authentication, and only `frontend` published on `0.0.0.0:8081`).
- Task 0.8: Reverse proxy & CI (`frontend/nginx.conf` with dynamic CSP nonce injection on `index.html` via `sub_filter '__CSP_NONCE__' '$request_id'`, strict explicit CSP policy, caching rules, `.github/workflows/ci.yml` validating backend Spotless & Checkstyle, frontend ESLint & Prettier, backend Testcontainers tests with `-Dsurefire.runOrder=random`, frontend Vitest tests, port isolation check, and Playwright E2E smoke tests).
- Task 0.9: Developer experience (`Makefile` targets `up`, `down`, `dev`, `test`, `test-be` with random surefire order, `test-fe`, `lint`, `fmt`, `e2e`, `scan`, `vectors`, `certs`, `psql`, `reset-db`, `README.md`).

### Tests
- Backend: 50 tests, line 82% / branch 72% across bundle (`jacoco:check` passed with 80% line / 70% branch gates).
- Backend Randomized Run Order: Enforced via `maven-surefire-plugin` (`<runOrder>random</runOrder>`), `Makefile`, and CI.
  - Consecutive Run 1 (`-Dsurefire.runOrder.random.seed=147574706629912`): 50/50 passed, build success.
  - Consecutive Run 2 (`-Dsurefire.runOrder.random.seed=147613526936333`): 50/50 passed, build success.
- Frontend: 12 tests, line 79.34% / branch 81.57% (`vitest.config.mts` passed with 75% line / 65% branch gates).
- E2E: 3 Playwright smoke tests passed:
  1. `Hey Payments PWA Smoke Test › should render styled PWA with zero console errors, Calm Ledger theme, status banner, light logo, and active service worker`: PASS (0 console errors, `body` background `rgb(247, 245, 240)`, status banner visible, light logo visible, dark logo hidden, service worker registered).
  2. `Hey Payments PWA Smoke Test › should support dark color scheme with dark background and visible dark wordmark`: PASS (0 console errors, `body` background `rgb(11, 20, 32)`, status banner visible, dark logo visible, light logo hidden).
  3. `Hey Payments PWA Smoke Test › should toggle dark wordmark when data-theme="dark" attribute is present`: PASS (dark logo visible, light logo hidden).

### Screenshots
- Light Mode: `docs/design/screenshots/phase-0-shell-light.png`
- Dark Mode: `docs/design/screenshots/phase-0-shell-dark.png`
- Playwright E2E test suite writes screenshots to ephemeral `test-results/screenshots/` (does not dirty `docs/` on CI/dev runs).

### Gate results
- `make fmt && make lint` ✅ (0 Checkstyle violations, 0 ESLint violations, Prettier clean)
- `make test` ✅ (50 backend tests with random order + 12 frontend tests; all coverage gates met)
- `make scan` ✅ (`scripts/test-port-exposure.sh`: Only frontend on 0.0.0.0; internal services strictly on 127.0.0.1)
- `make e2e` ✅ (3/3 Playwright smoke tests green)
- `make up` ✅ (5/5 containers healthy)

### Bug-hunt agent & Architect review
- Bug-hunt report: `docs/reviews/AGENT-REVIEW-phase-0.md`: H:3 M:7 L:4 → all 14 fixed with regression tests.
- Architect review: `docs/reviews/REVIEW-phase-0.md` → all MUST-FIX and SHOULD-FIX items verified:
  1. [MUST-FIX 1] CSP Angular styles unblocked via `inlineCritical: false`, per-request CSP nonce, and `sub_filter`.
  2. [MUST-FIX 2] HTTP Basic auth dropped, `UserDetailsServiceAutoConfiguration` excluded, RFC 9457 `ProblemAuthenticationEntryPoint` and `ProblemAccessDeniedHandler` implemented and tested.
  3. [MUST-FIX 3] Port exposure eliminated: postgres, redis, mailpit, backend bound to `127.0.0.1`; redis password enforced via `--requirepass`.
  4. [MUST-FIX 4] Calm Ledger v1 tokens adopted verbatim with `@media (prefers-color-scheme: dark)`, fonts self-hosted via `@fontsource`, brand SVG assets installed.
  5. [MUST-FIX 5] Shell UI uses approved status language ("Online · synced"), `@lucide/angular` icons, backend demo card guarded with `isDev()`, 0 developer URLs in user UI.
  6. [MUST-FIX 6] Frontend container runs non-root `nginxinc/nginx-unprivileged:1.27-alpine` on port 8080.
  7. [MUST-FIX A (Re-review)] Order-dependent tests eliminated:
     - Singleton PostgreSQL Testcontainer initialized in `BaseIntegrationTest` via static initialization block, preventing container teardown between classes while preserving Spring context caching.
     - Thread-local MDC cleanup added with `@BeforeEach` and `@AfterEach` in `GlobalExceptionHandlerTest`, `ProblemSecurityHandlersTest`, and `RequestIdFilterTest`.
     - Surefire configured with `<runOrder>random</runOrder>` in `pom.xml`, `Makefile`, and CI. Verified with two consecutive green runs.
  8. [MUST-FIX B (Re-review)] Brand wordmark dark mode visibility fixed:
     - Rendered both `logo-wordmark-light.svg` and `logo-wordmark-dark.svg` in `app.html`.
     - Added CSS rules toggling visibility under `@media (prefers-color-scheme: dark)` and `:root[data-theme='dark']` without recolouring SVGs.
     - Extended E2E suite to assert light wordmark visible in light mode, dark wordmark visible and light hidden in dark mode and under `data-theme="dark"`.
  9. [Condition C (Re-review Approval)] Prod profile Redis password compose wiring:
     - Wired `SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:?REDIS_PASSWORD must be set}` in `docker-compose.yml` backend service environment.
     - Created `ProductionProfileConfigTest` asserting that prod profile fails without `SPRING_DATA_REDIS_PASSWORD` and resolves cleanly when supplied.
     - Added CI step and `scripts/test-port-exposure.sh` check verifying that `SPRING_PROFILE=prod docker compose --env-file .env.example config` succeeds with valid password binding.
  10. [Brand Wordmarks Outlined Paths] Copied architect-provided outlined-path SVGs (`logo-wordmark-light.svg`, `logo-wordmark-dark.svg`) into `frontend/public/brand/`, ensuring crisp offline vector rendering inside `<img>` tags on any device.

### SHOULD-FIX dispositions
- Proxy `/actuator/`: Removed from public Nginx configuration.
- Prod JSON logging: Replaced custom pattern with Spring Boot native structured ECS logging (`logging.structured.format.console: ecs`).
- Dev Swagger: Permitted `/swagger-ui.html` under `dev` profile.
- `audit_log`: Statement-level trigger `trg_audit_log_no_truncate` blocks `TRUNCATE` operations; tested in `AuditLogTriggerTest`.
- `ConnectivityService`: Added code comment clarifying that `'degraded'` status will be triggered by background sync backoff in Phase 6.
- Dossier and Agent Review reports: Corrected all discrepancies and tidied conclusion sections.
- Playwright screenshots: Configured to output into `test-results/` so test runs do not dirty the repo.
- Production Redis password: `application-prod.yml` requires `${SPRING_DATA_REDIS_PASSWORD}` with no default.

### Deviations from spec / proposals
- none

### Known issues / next
- Signed journal line type to be introduced in Phase 2 for ledger transactions (preserving non-negative `Money` for amounts).
- Next: Phase 1 (Auth, users, devices) on branch `phase/1-auth-devices`.

### How to demo
1. `make up`
2. Open http://localhost:8081 in browser to view the PWA shell with live connectivity status banner, Calm Ledger v1 design tokens, and light/dark theme wordmark switching.
3. Run `make e2e` to execute Playwright smoke tests and verify zero console errors.

---

## Phase 1: Authentication, Registration, and Device Enrollment: READY FOR REVIEW (2026-11-13)
**Branch:** phase/1-auth-devices   **Commits:** 50   **Tag:** (after approval)

### Built
- **Crypto & Signatures (Task 1.1):**
  - Java: `CryptoService` implementing ECDSA P-256 with IEEE P1363 (64-byte `r||s`) signatures (`SHA256withECDSAinP1363Format`), RFC 8785 JSON Canonicalization Scheme (`JcsCanonicalizer`), Argon2id password hashing, and AES-256-GCM symmetric master encryption.
  - TypeScript: Pure TS `crypto.ts`, `jcs.ts`, and `key.service.ts` using WebCrypto ECDSA P-256 with P1363 signatures, non-BMP UTF-16 code unit ordering, IndexedDB structured cloning of non-extractable CryptoKeyPair (`extractable: false`), and PBKDF2 offline PIN hashing (600,000 iterations).
  - Test vectors: Verified against shared vectors in `test-vectors/jcs.json` and `test-vectors/sign-verify.json` (canonical JSON, RFC 8785 Appendix B numbers, unicode/emoji keys, P1363 signatures, negative cases).
- **Server Key Discovery & JWKS (Task 1.3):**
  - Database-backed server signing key generation (`V2__auth.sql`, `server_signing_keys` table), daily rotation, and active/retired status lifecycle (`ServerKeyService`).
  - Public JWKS endpoint `GET /api/v1/keys/jwks` (`JwksController`).
- **User Authentication & Session Management (Task 1.2 & 1.4):**
  - User registration (`POST /api/v1/auth/register`) for PAYER and MERCHANT roles with initial LOCKED status (`V2__auth.sql`, `users` table).
  - 6-digit OTP dispatch via Mailpit (`OtpService`) with bounded Caffeine cache and 5-attempt brute-force protection; account activation (`POST /api/v1/auth/verify-otp`).
  - Login (`POST /api/v1/auth/login`) with Argon2id password verification, issuing 15-minute access JWT and 14-day rotating refresh token cookie with SameSite=Strict and context-aware Secure flag.
  - Refresh rotation (`POST /api/v1/auth/refresh`) with atomic token family invalidation upon reuse detection (`RefreshTokenService`).
  - Payment PIN configuration (`PUT /api/v1/auth/pin`) for payers with password validation and client-side salted PBKDF2 hash persistence.
  - Authenticated user profile retrieval (`GET /api/v1/users/me`).
  - Rate limiting with Bucket4j + Caffeine cache: 5 req/min on login/OTP, 120 req/min general, resolving client IP via `request.getRemoteAddr()`.
- **Device Registration & Merchant Certificates (Task 1.5):**
  - Device enrollment (`POST /api/v1/devices`) verifying 32-byte coordinates and curve equation validity on P-256; partial unique index enforcing max 1 ACTIVE device per user per kind (`devices` table).
  - Merchant Certificate (MC) issuance for merchant devices, signed by active server private key.
  - Active device retrieval (`GET /api/v1/devices/me`), device retirement (`DELETE /api/v1/devices/{id}`), and certificate revocation list delta querying (`GET /api/v1/devices/crl`).
- **Frontend Auth & Device Workflows (Task 1.6):**
  - Angular 22 standalone signals-based UI adhering to Calm Ledger v1 and WCAG 2.2 AA: Register (`/auth/register`), Verify OTP (`/auth/verify-otp`), Login (`/auth/login`), Device Enrollment (`/auth/device-register`), Set PIN (`/auth/set-pin`).
  - `AuthStore` managing JWT lifecycle, device key enrollment, MC storage in Dexie `db.certs`, and seamless session recovery on login.
  - `authInterceptor` injecting Bearer token, deduplicating concurrent 401 refresh requests via RxJS `shareReplay(1)` mutex.
  - Route guards (`authGuard`, `unauthGuard`) protecting payer and merchant views.

### Tests
- Backend: 106 tests, line 92.01% / branch 76.74% (crypto 85.29%, device 98.98%, auth 93.09%, Modulith & ArchUnit 100%)
- Frontend: 97 tests, line 91.80% / branch 84.22% (core/crypto 97.97%, core/offline 100%, core/auth 95.61%, core/api 100%)
- E2E: n.a. (Scheduled for Phase 5)

### Gate results
- make lint ✅ (Spotless, Checkstyle, ESLint, Prettier clean)
- make test ✅ (106 backend tests with random order + 97 frontend tests; all coverage gates met)
- make e2e n.a. (Phase 5+)
- make up ✅ (5/5 containers healthy, actuator UP at localhost:8080/actuator/health)
- make scan ✅ (Port exposure: internal services strictly on 127.0.0.1; prod compose config resolves `SPRING_DATA_REDIS_PASSWORD` and `HP_MASTER_KEY`)

### Bug-hunt agent
- Report: `docs/reviews/AGENT-REVIEW-phase-1.md`: H:4 M:5 L:5
  - All 4 HIGH fixed:
    - [HIGH-1] Transaction Rollback on Token Reuse (Test: `df632ee`, Fix: `50bf5aa`)
    - [HIGH-2] Frontend Device Registration Desync & Lockout on Re-login (Test: `08e7de5`, Fix: `90e63bc`)
    - [HIGH-3] Set-PIN URL and Payload Schema Mismatch (Test: `787363d`, Fix: `af56aea`)
    - [HIGH-4] Race Condition in `authInterceptor` on Concurrent 401s (Test: `aa573a5`, Fix: `cf3404b`)
  - All 5 MEDIUM fixed:
    - [MEDIUM-1] RFC 8785 JCS Key Comparison on Characters > U+FFFF (Test: `f08ca33`, Fix: `880173f`)
    - [MEDIUM-2] Primary Key UUID Generation Uses UUIDv7 (Test: `ce377f8`, Fix: `2c1d6fc`)
    - [MEDIUM-3] Missing Production Fail-Fast Configuration for `HP_MASTER_KEY` (Test: `2a34cd8`, Fix: `9fe977d`)
    - [MEDIUM-4] Missing EC Curve Point Validation on Device Public Key Registration (Test: `dcfc8ff`, Fix: `9bca0b9`)
    - [MEDIUM-5] Unbounded Memory Growth & IP Spoofing in Rate Limiting & OTP (Test: `f2a9041`, Fix: `ddb3b93`)
  - 4 of 5 LOW fixed:
    - [LOW-1] Hardcoded Insecure Cookie Flag in `AuthController` (Fix: `d60435a`)
    - [LOW-3] Missing Merchant Certificate Retrieval in `GET /devices/me` (Test: `08e7de5`, Fix: `90e63bc`)
    - [LOW-4] Missing ArchUnit Rule and Weak Authorization on `POST /devices` (Test: `65c75aa`, Fix: `d60435a`)
    - [LOW-5] WAI-ARIA Accessibility Violation in Register Role Selector (Fix: `d60435a`)
  - 1 LOW accounted for:
    - [LOW-2] Double-submit CSRF token architecture on refresh cookie: Scheduled for Phase 8 security hardening per `docs/12-implementation-plan.md` Task 8.1 and `docs/08-security-threat-model.md` §5.

### Deviations from spec / proposals
- none

### Known issues / next
- Next: Phase 2 (Ledger & Accounts) on branch `phase/2-ledger-accounts`.

### How to demo
1. `make up`
2. Open http://localhost:8081/auth/register in browser.
3. Register a new user (`+919876543210`, password `Password123!`).
4. View the dispatched OTP email in Mailpit at http://localhost:8025.
5. Enter the OTP at http://localhost:8081/auth/verify-otp to activate account.
6. Login at http://localhost:8081/auth/login and proceed through device enrollment and PIN setup.
