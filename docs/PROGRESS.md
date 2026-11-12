# Progress Log

Maintained by the coding agent. Append one section per phase using the template in `AGENTS.md §9`.

| Phase | Name | Status | Review |
|---|---|---|---|
| 0 | Foundation | READY FOR REVIEW | docs/reviews/AGENT-REVIEW-phase-0.md |
| 1 | Auth, users, devices | NOT STARTED | — |
| 2 | Ledger, wallet, funding | NOT STARTED | — |
| 3 | Offline balance | NOT STARTED | — |
| 4 | Design system & shell | NOT STARTED | — |
| 5 | Offline payment on devices | NOT STARTED | — |
| 6 | Sync, settlement, fraud | NOT STARTED | — |
| 7 | Merchant & admin | NOT STARTED | — |
| 8 | Hardening & release | NOT STARTED | — |

---

## Phase 0: Foundation: READY FOR REVIEW (2026-11-12)
**Branch:** phase/0-foundation   **Commits:** 58   **Tag:** (after approval)

### Built
- Task 0.1: Git repository initialized with branch discipline, strict `.gitignore`, pre-commit hooks, and architecture documentation.
- Task 0.2: Backend project skeleton (`backend/pom.xml`, Java 21, Spring Boot 4.1.2, Spring Modulith, Spring Security, Flyway, PostgreSQL, Redis, Bucket4j, Testcontainers, ArchUnit, jqwik, Spotless, Checkstyle, JaCoCo).
- Task 0.3: Shared domain kernel (`com.heypayments.shared` with `Money` value object supporting safe integer bound and Jackson mapping, `UuidV7Generator`, `ErrorCode`, `DomainException`, `FieldErrorDetail`).
- Task 0.4: Web & observability foundation (`RequestIdFilter` with strict alphanumeric sanitization, `GlobalExceptionHandler` mapping domain and Spring MVC client exceptions to RFC 9457 `ProblemResponse`, `ProblemAuthenticationEntryPoint` writing 401 `UNAUTHENTICATED`, `ProblemAccessDeniedHandler` writing 403 `FORBIDDEN`, `TimeConfig` UTC clock, `SecurityConfig` with method security and dev-profile Swagger/OpenAPI exposure, Spring Boot native structured ECS JSON logging).
- Task 0.5: Database migration baseline (`V1__baseline.sql` with `spring-boot-starter-flyway`, `audit_log` append-only trigger and statement-level `TRUNCATE` prevention trigger, `idempotency_keys`, `event_publication`), Testcontainers base integration test, and Spring Modulith verification.
- Task 0.6: Angular 22 PWA scaffold (`frontend/` with strict TypeScript, standalone components, Angular signals, `@angular/pwa` service worker, ESLint with a11y, Prettier, `tokens.scss` with Calm Ledger v1 tokens, `@media (prefers-color-scheme: dark)`, self-hosted fonts with `@fontsource`, brand SVG assets, `ConnectivityService`, accessible app shell with live status banner using `@lucide/angular`).
- Task 0.7: Dockerization (`backend/Dockerfile` multi-stage with non-root user `app`, `frontend/Dockerfile` multi-stage with `nginxinc/nginx-unprivileged:1.27-alpine` non-root user `nginx`, `docker-compose.yml` with port isolation binding internal services strictly to `127.0.0.1`, Redis password authentication, and only `frontend` published on `0.0.0.0:8081`).
- Task 0.8: Reverse proxy & CI (`frontend/nginx.conf` with dynamic CSP nonce injection on `index.html` via `sub_filter '__CSP_NONCE__' '$request_id'`, strict explicit CSP policy, caching rules, `.github/workflows/ci.yml` validating backend Spotless & Checkstyle, frontend ESLint & Prettier, backend Testcontainers tests, frontend Vitest tests, port isolation check, and Playwright E2E smoke tests).
- Task 0.9: Developer experience (`Makefile` targets `up`, `down`, `dev`, `test`, `lint`, `fmt`, `e2e`, `scan`, `vectors`, `certs`, `psql`, `reset-db`, `README.md`).

### Tests
- Backend: 50 tests, line 82% / branch 72% across bundle (`jacoco:check` passed with 80% line / 70% branch gates).
- Frontend: 12 tests, line 79.34% / branch 81.57% (`vitest.config.mts` passed with 75% line / 65% branch gates).
- E2E: 2 Playwright smoke tests passed:
  1. `Hey Payments PWA Smoke Test › should render styled PWA with zero console errors, Calm Ledger theme, status banner, and active service worker`: PASS (0 console errors, `body` background `rgb(247, 245, 240)`, status banner visible, service worker registered).
  2. `Hey Payments PWA Smoke Test › should support dark color scheme with dark background`: PASS (`body` background `rgb(11, 20, 32)`).

### Screenshots
- Light Mode: `docs/design/screenshots/phase-0-shell-light.png`
- Dark Mode: `docs/design/screenshots/phase-0-shell-dark.png`

### Gate results
- `make fmt && make lint` ✅ (0 Checkstyle violations, 0 ESLint violations, Prettier clean)
- `make test` ✅ (50 backend tests + 12 frontend tests; all coverage gates met)
- `make scan` ✅ (`scripts/test-port-exposure.sh`: Only frontend on 0.0.0.0; internal services strictly on 127.0.0.1)
- `make e2e` ✅ (2/2 Playwright smoke tests green)
- `make up` ✅ (5/5 containers healthy)

### Bug-hunt agent & Architect review
- Bug-hunt report: `docs/reviews/AGENT-REVIEW-phase-0.md`: H:3 M:7 L:4 → all 14 fixed with regression tests.
- Architect review: `docs/reviews/REVIEW-phase-0.md` → all 6 MUST-FIX items implemented with TDD and verified:
  1. [MUST-FIX 1] CSP Angular styles unblocked via `inlineCritical: false`, per-request CSP nonce, and `sub_filter`. Verified by Playwright test asserting 0 console errors and paper background.
  2. [MUST-FIX 2] HTTP Basic auth dropped, `UserDetailsServiceAutoConfiguration` excluded, RFC 9457 `ProblemAuthenticationEntryPoint` and `ProblemAccessDeniedHandler` implemented and tested.
  3. [MUST-FIX 3] Port exposure eliminated: postgres, redis, mailpit, backend bound to `127.0.0.1`; redis password enforced via `--requirepass`.
  4. [MUST-FIX 4] Calm Ledger v1 tokens adopted verbatim with `@media (prefers-color-scheme: dark)`, fonts self-hosted via `@fontsource`, brand SVG assets installed.
  5. [MUST-FIX 5] Shell UI uses approved status language ("Online · synced"), `@lucide/angular` icons, backend demo card guarded with `isDev()`, 0 developer URLs in user UI.
  6. [MUST-FIX 6] Frontend container runs non-root `nginxinc/nginx-unprivileged:1.27-alpine` on port 8080.

### SHOULD-FIX dispositions
- Proxy `/actuator/`: Removed from public Nginx configuration.
- Prod JSON logging: Replaced custom pattern with Spring Boot native structured ECS logging (`logging.structured.format.console: ecs`).
- Dev Swagger: Permitted `/swagger-ui.html` under `dev` profile.
- `audit_log`: Statement-level trigger `trg_audit_log_no_truncate` blocks `TRUNCATE` operations; tested in `AuditLogTriggerTest`.
- `ConnectivityService`: Added code comment clarifying that `'degraded'` status will be triggered by background sync backoff in Phase 6.
- Dossier and Agent Review reports: Corrected all discrepancies and tidied conclusion sections.

### Deviations from spec / proposals
- none

### Known issues / next
- Signed journal line type to be introduced in Phase 2 for ledger transactions (preserving non-negative `Money` for amounts).
- Next: Stop for architect re-review before beginning Phase 1 (Auth, users, devices).

### How to demo
1. `make up`
2. Open http://localhost:8081 in browser to view the PWA shell with live connectivity status banner and Calm Ledger v1 design tokens.
3. Run `make e2e` to execute Playwright smoke tests and verify zero console errors.
