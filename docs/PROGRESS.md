# Progress Log

Maintained by the coding agent. Append one section per phase using the template in `AGENTS.md §9`.

| Phase | Name | Status | Review |
|---|---|---|---|
| 0 | Foundation | APPROVED | docs/reviews/REVIEW-phase-0.md |
| 1 | Auth, users, devices | APPROVED WITH CONDITIONS (CONDITIONS MET) | docs/reviews/REVIEW-phase-1.md |
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
**Branch:** phase/1-auth-devices   **Commits:** 75   **Tag:** (after approval)

### Built
- **Crypto & Signatures (Task 1.1):**
  - Java: `CryptoService` implementing ECDSA P-256 with IEEE P1363 (64-byte `r||s`) signatures (`SHA256withECDSAinP1363Format`), RFC 8785 JSON Canonicalization Scheme using `org.erdtman:java-json-canonicalization` (`JcsCanonicalizer`), integer range validation (`> 2^53 - 1` and non-integers rejected per protocol spec), Argon2id password hashing, and AES-256-GCM symmetric master encryption.
  - TypeScript: Pure TS `crypto.ts`, `jcs.ts` (using `canonicalize` npm package), and `key.service.ts` using WebCrypto ECDSA P-256 with P1363 signatures, non-BMP UTF-16 code unit ordering, IndexedDB structured cloning of non-extractable CryptoKeyPair (`extractable: false`), and PBKDF2 offline PIN hashing with configurable iterations (600,000 prod, reduced for tests).
  - Test vectors: Verified against shared vectors in `test-vectors/jcs.json` and `test-vectors/sign-verify.json` (canonical JSON, RFC 8785 Appendix B numbers, unicode/emoji keys, P1363 signatures, and negative tamper/wrong-key/bad-signature test cases consumed by both Java and TS).
- **Server Key Discovery & JWKS (Task 1.3):**
  - Database-backed server signing and JWT keys (`V2__auth.sql`, `V3__jwt_server_keys.sql`, `server_keys` table with `purpose` column: `SIGNING` vs `JWT`).
  - Encrypted ES256 JWT key persistence with AES-GCM and unique `kid` (`jwt-<uuid>`), verifying by `kid` and enforcing claims (`alg: ES256`, `typ: JWT`, `iss: heypayments`).
  - Automatic scheduled 90-day rotation via `@Scheduled(cron = "${hp.crypto.server-key.rotation-cron:0 0 2 * * ?}")` and `@EnableScheduling`.
  - Public JWKS endpoint `GET /api/v1/keys/jwks` (`JwksController`) exposing active and retired public keys.
- **User Authentication & Session Management (Task 1.2 & 1.4):**
  - User registration (`POST /api/v1/auth/register`) for PAYER and MERCHANT roles with initial `PENDING_VERIFICATION` status (`V4__user_status_pending_verification.sql`, `docs/proposals/003-user-status.md`).
  - 6-digit OTP dispatch via Mailpit (`OtpService`) with bounded Caffeine cache, constant-time comparison (`MessageDigest.isEqual`), sanitized phone logs, and 5-attempt brute-force protection; account activation (`POST /api/v1/auth/verify-otp`).
  - Login (`POST /api/v1/auth/login`) with Argon2id password verification, issuing 15-minute access JWT and 14-day rotating refresh token cookie with SameSite=Strict, context-aware Secure flag, and `Path=/api/v1/auth`.
  - Refresh rotation (`POST /api/v1/auth/refresh`) with atomic token family invalidation upon reuse detection, plus `Origin`/`Referer` allow-list enforcement.
  - Payment PIN configuration (`PUT /api/v1/auth/pin`) for payers with password validation and client-side salted PBKDF2 hash persistence in Dexie `meta` table with 5-strike, 30-minute lockout.
  - Authenticated user profile retrieval (`GET /api/v1/users/me`).
  - Rate limiting with Bucket4j + Caffeine cache: per-phone 5 req/min and per-IP 20 req/min on login/OTP, 120 req/min general. Resolved via Nginx `proxy_set_header X-Forwarded-For $remote_addr` (overwrite), Spring native `forward-headers-strategy`, Tomcat `remoteip` internal proxies matching Docker compose network, and consistent RFC 9457 problem response with requestId on 429.
- **Device Registration & Merchant Certificates (Task 1.5):**
  - Device enrollment (`POST /api/v1/devices`) verifying 32-byte coordinates and curve equation validity on P-256; partial unique index enforcing max 1 ACTIVE device per user per kind (`devices` table).
  - Merchant Certificate (MC) issuance for merchant devices, signed by active server private key.
  - Active device retrieval (`GET /api/v1/devices/me`), device retirement (`DELETE /api/v1/devices/{id}`), and certificate revocation list delta querying (`GET /api/v1/devices/crl`).
- **Frontend Auth & Device Workflows (Task 1.6):**
  - Angular 22 standalone signals-based UI adhering to Calm Ledger v1 and WCAG 2.2 AA: Register (`/auth/register`), Verify OTP (`/auth/verify-otp`), Login (`/auth/login`), Device Enrollment (`/auth/device-register`), Set PIN (`/auth/set-pin`).
  - Hand-written API services replaced with generated OpenAPI client from `docs/openapi.yaml` (`npm run api:gen`), drift-checked in CI (`git diff --exit-code src/app/core/api/generated`), backend contract test diffing Springdoc `/v3/api-docs` against `docs/openapi.yaml` (`OpenApiContractTest`), and documented in `docs/proposals/002-openapi-phase1.md`.
  - Circular DI completely resolved: dependency-free `TokenStore` for `authInterceptor`, and isolated `TokenRefreshService` using `HttpBackend` for refresh without cyclic dependencies.
  - `AuthStore` managing JWT lifecycle, device key enrollment, MC storage in Dexie `db.certs`, offline PIN management in `db.meta`, and seamless session recovery on login.
  - Route guards (`authGuard`, `unauthGuard`) protecting payer and merchant views.

### Tests
- Backend: 123 tests, line 85.03% / branch 72.84% (crypto 87.5%, device 98.98%, auth 92.5%, Modulith & ArchUnit 100%)
- Backend Randomized Run Order: Enforced via `maven-surefire-plugin` (`<runOrder>random</runOrder>`), `Makefile`, and CI. Verified with green runs on random seeds.
- Frontend: 108 tests, line 93.2% / branch 84.52% (core/crypto 93.75%, core/offline 100%, core/auth 95.48%, features/dashboard 100%, core/api 100%). Single-worker vitest execution, PBKDF2 test iteration injection, verified with consecutive green runs.
- E2E: 8 Playwright tests run / 8 passed against full compose stack:
  1. Route access test: public routes render forms; protected routes (`/auth/device-register`, `/auth/set-pin`) redirect to `/auth/login` when unauthenticated; zero console errors.
  2. Full UI flow for Payer: register -> OTP from Mailpit API -> verify -> login -> device register -> set PIN -> lands strictly on `/pay` -> assert Dexie `pin_salt` & `pin_hash` exist in `meta` and 0 `hp_pin_*` keys in `localStorage` -> assert device key `extractable === false` -> click logout -> lands on `/auth/login` with refresh cookie cleared -> login again -> lands directly on `/pay`.
  3. Full UI flow for Merchant: register -> OTP from Mailpit API -> verify -> login -> device register -> lands strictly on `/merchant` -> authenticated route access verified -> assert device key `extractable === false` -> Dexie MC envelope verified in-browser via JWKS ECDSA P-256 with JCS canonicalization -> assert mutated payload fails verification.
  4. PWA smoke test with zero console errors, Calm Ledger theme, status banner, light logo, active service worker.
  5. Dark mode support with dark background and visible dark wordmark.
  6. Dark theme toggle under `data-theme="dark"`.
  7. Visual screenshot capture of all 5 auth screens in light mode.
  8. Visual screenshot capture of all 5 auth screens in dark mode.

### Screenshots (Calm Ledger v1 · 390×844)
#### 1. Login (`/auth/login`)
| Light Mode | Dark Mode |
|---|---|
| ![Login Light](docs/design/screenshots/phase-1-login-light.png) | ![Login Dark](docs/design/screenshots/phase-1-login-dark.png) |

#### 2. Register (`/auth/register`)
| Light Mode | Dark Mode |
|---|---|
| ![Register Light](docs/design/screenshots/phase-1-register-light.png) | ![Register Dark](docs/design/screenshots/phase-1-register-dark.png) |

#### 3. Verify OTP (`/auth/verify-otp`)
| Light Mode | Dark Mode |
|---|---|
| ![Verify OTP Light](docs/design/screenshots/phase-1-verify-otp-light.png) | ![Verify OTP Dark](docs/design/screenshots/phase-1-verify-otp-dark.png) |

#### 4. Device Enrollment (`/auth/device-register`)
| Light Mode | Dark Mode |
|---|---|
| ![Device Register Light](docs/design/screenshots/phase-1-device-register-light.png) | ![Device Register Dark](docs/design/screenshots/phase-1-device-register-dark.png) |

#### 5. Set PIN (`/auth/set-pin`)
| Light Mode | Dark Mode |
|---|---|
| ![Set PIN Light](docs/design/screenshots/phase-1-set-pin-light.png) | ![Set PIN Dark](docs/design/screenshots/phase-1-set-pin-dark.png) |

### Gate results
- `make fmt && make lint` ✅ (Spotless clean, Checkstyle clean, OpenAPI generation & drift clean, ESLint clean, Prettier clean)
- `make test` ✅ (123 backend tests with random order + 108 frontend tests single-worker; all coverage gates met)
- `make e2e` ✅ (8/8 Playwright tests green against full Docker compose stack)
- `make up` ✅ (5/5 containers healthy, actuator UP at `http://localhost:8080/actuator/health`)
- `make scan` ✅ (Port exposure: internal services strictly on 127.0.0.1; prod compose config resolves `SPRING_DATA_REDIS_PASSWORD` and `HP_MASTER_KEY`)

### Conditions Addressed (Re-review 2026-11-13)
1. **C1: Stricter Playwright E2E Suite (`e2e/auth.spec.ts`):**
   - Strictly enforced landing URLs: Payer must land on `/pay` and Merchant on `/merchant` (never accepting `/auth/login`).
   - Direct Dexie inspection without fallback: asserted that `pin_salt` and `pin_hash` exist in Dexie `meta` with non-empty string values, and zero `hp_pin_*` keys exist in `localStorage`.
   - Real Merchant Certificate verification: canonicalized `envelope.p` with RFC 8785 JCS, verified signature via `crypto.subtle.verify` against active JWKS ECDSA P-256 key (`valid === true`), and proved verification fails on tampered payload (`mutatedValid === false`).
   - Device key security: asserted stored device private keys have `extractable === false`.
   - Route verification: tested unauthenticated access (`/auth/device-register` and `/auth/set-pin` redirect to `/auth/login`) and authenticated form rendering.
   - Cleaned console errors: updated `GET /api/v1/devices/me` to return HTTP `204 No Content` when no active device exists, removing the blanket "404" console filter.
2. **C2: UI Logout Control & Re-authentication:**
   - Added logout button to `/pay` and `/merchant` dashboard placeholder (`DashboardComponent`) featuring Lucide `log-out` icon and Calm Ledger v1 outline button styling (`btn btn-outline`).
   - Automated in E2E: logout triggers `AuthStore.logout()`, navigates to `/auth/login`, clears refresh cookie `hp_rt`, and allows logging in again to land directly on `/pay`.
3. **C3: Dexie Versioned Schema Upgrade (v1 → v2):**
   - Preserved `version(1)` with `keys` and `certs` tables, and chained `version(2)` introducing `meta: 'key'`.
   - Added unit test in `db.spec.ts` verifying seamless schema upgrade from v1 to v2 with `fake-indexeddb`.
4. **C4: Unified Server Key Rotation Property:**
   - Standardized property name to `hp.crypto.server-key.rotation-cron` across `application.yml`, `ServerKeyService.java`, and documentation.

### Review items addressed (docs/reviews/REVIEW-phase-1.md)
#### MUST-FIX Items (All 6 resolved test-first)
1. **[HIGH] Circular DI:** Added dependency-free `TokenStore` for `authInterceptor`. Token refresh delegated to `TokenRefreshService` via `HttpBackend` without interceptor recursion. Tests: `auth-di.spec.ts` with real `appConfig` providers; Playwright `e2e/auth.spec.ts` against compose stack with zero console errors and full UI flows for payer and merchant.
2. **[HIGH] Rate Limiting Keys & Proxy Trust:** Nginx overwrites `X-Forwarded-For` with `$remote_addr`. Spring uses native forward-headers-strategy with Tomcat `remoteip` internal proxies matching compose network. Dual rate limiting: per-phone (5/min) and per-IP (20/min) on login/verify-otp. Problem type `RATE_LIMITED` and `requestId` preserved on 429. Tests: separate IPs, spoofed XFF ignored, per-phone across IPs, and through Nginx.
3. **[MEDIUM] JWT Key Persistence & Validation:** JWT key encrypted with master key and persisted in `server_keys` (`purpose = 'JWT'`, unique `kid = jwt-<uuid>`). Enforces `alg: ES256`, `typ: JWT`, `iss: heypayments`. Tests: survives new app context; unknown kid and wrong iss rejected.
4. **[MEDIUM] RFC 8785 Canonicalization & Test Vectors:** Switched to `org.erdtman:java-json-canonicalization` and npm `canonicalize`. Rejects numbers `> 2^53 - 1` and non-integers in signed envelopes. Shared test-vectors expanded with RFC 8785 numbers, unicode/emoji keys, escapes, negative tamper/wrong-key/bad-sig cases consumed by both Java and TS. Retracted incorrect PROGRESS claims about non-existent `p256/` and `argon2/` folders.
5. **[MEDIUM] OpenAPI Client Generation & Contract Diff Test:** Replaced hand-written services with client generated via `ng-openapi-gen` (`npm run api:gen`). Drift checked in CI (`git diff --exit-code`). Added `OpenApiContractTest` diffing `/v3/api-docs` against `docs/openapi.yaml`. Documented intentional differences in `docs/proposals/002-openapi-phase1.md`.
6. **[MEDIUM] Frontend Crypto Test Stability:** Injectable PBKDF2 iterations (600k in prod, 1,000 in tests). Single-worker vitest configuration in CI and local test script (`pool: 'threads'`, `maxWorkers: 1`). Verified with consecutive green runs.

#### SHOULD-FIX Items (All resolved test-first)
1. **`PENDING_VERIFICATION` user status:** Added status via Flyway `V4__user_status_pending_verification.sql` and `docs/proposals/003-user-status.md`. `AuthService` state machine ensures `LOCKED` accounts cannot be unlocked via OTP.
2. **Scheduled 90-day server key rotation:** Added `@Scheduled` task in `ServerKeyService` and `@EnableScheduling` on `BackendApplication` with configurable cron expression (`hp.crypto.server-key.rotation-cron`).
3. **Refresh cookie Path & Origin check:** Refresh cookie scoped to `Path=/api/v1/auth`. `AuthController` enforces cheap `Origin`/`Referer` allow-list check (`localhost`, `127.0.0.1`, `nginx`).
4. **Offline PIN in Dexie `meta` with lockout:** Moved salt and hash to Dexie `meta` table. Implemented `verifyPin` in `AuthStore` with 5-strike counter and 30-minute lockout.
5. **Constant-time OTP compare & sanitized logs:** Used `MessageDigest.isEqual` for constant-time OTP verification. Sanitized phone numbers in INFO-level logs.
6. **PROGRESS.md claims corrected:** Fixed previous claims regarding rotation intervals and test-vector folders.

### Tracked Review Notes (Carried forward for future phases)
- **Migrations:** All database migrations that have run anywhere are strictly immutable. Any future schema evolutions will strictly append new `V<n>` scripts.
- **JWT Performance:** Add in-memory caching of public keys by kid and active private key with eviction on rotation prior to Phase 8 p95 targets.
- **Retiring Keys Cleanup:** Implement background transition of expired server signing keys to `REVOKED` state to bound JWKS size.
- **CORS and Origin Allow-lists:** Wire `HP_CORS_ORIGINS` and `HP_JWT_ISSUER` into environment configuration for Phase 8 LAN demo.
- **Phase 4 UI Polish:** Refine device-register card vertical alignment/spacing and replace developer placeholder copy with approved screens from `docs/design/reference/screens/*`.

### Deviations from spec / proposals
- `docs/proposals/002-openapi-phase1.md`: Documents Phase 1 contract additions (phone/OTP auth, device registration, and key discovery endpoints).
- `docs/proposals/003-user-status.md`: Documents addition of `PENDING_VERIFICATION` user status enum.

### Known issues / next
- Next: Phase 2 (Ledger, wallet, funding) on branch `phase/2-ledger-accounts`.

### How to demo
1. `make up`
2. Open http://localhost:8081/auth/register in browser.
3. Register as Payer or Merchant.
4. View the dispatched OTP email in Mailpit at http://localhost:8025.
5. Verify OTP at http://localhost:8081/auth/verify-otp.
6. Login at http://localhost:8081/auth/login.
7. Enroll device at http://localhost:8081/auth/device-register and configure spending PIN at http://localhost:8081/auth/set-pin.
8. Run `make e2e` to execute the full automated verification suite.

---

## From Phase 2: per-phase reports
Phase reports now live in [`docs/progress/`](progress/README.md), one file per phase, because two agents build in parallel (AGENTS.md §12).
- Phase 2: [progress/phase-2.md](progress/phase-2.md) (pending)
- Phase 5a: [progress/phase-5a.md](progress/phase-5a.md) (pending)
