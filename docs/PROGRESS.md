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
**Branch:** phase/0-foundation   **Commits:** 42   **Tag:** (after approval)

### Built
- Task 0.1: Git repo initialized with branch discipline, strict `.gitignore`, pre-commit hooks, and initial documentation.
- Task 0.2: Backend project skeleton (`backend/pom.xml`, Java 21, Spring Boot 4.1.2, Spring Modulith, Spring Security, Flyway, PostgreSQL, Redis, Bucket4j, Testcontainers, ArchUnit, jqwik, Spotless, Checkstyle, JaCoCo).
- Task 0.3: Shared domain kernel (`com.heypayments.shared` with `Money` value object supporting safe integer bound and Jackson mapping, `UuidV7Generator`, `ErrorCode`, `DomainException`, `FieldErrorDetail`).
- Task 0.4: Web & observability foundation (`RequestIdFilter` with strict alphanumeric sanitization, `GlobalExceptionHandler` mapping domain and Spring MVC client exceptions to RFC 9457 `ProblemResponse`, `TimeConfig` UTC clock, `SecurityConfig` with method security and profile-aware Swagger exposure, structured MDC logging).
- Task 0.5: Database migration baseline (`V1__baseline.sql` with `audit_log` append-only trigger, `idempotency_keys`, `event_publication`), Testcontainers base integration test, and Spring Modulith verification.
- Task 0.6: Angular 22 PWA scaffold (`frontend/` with strict TypeScript, standalone components, Angular signals, `@angular/pwa` service worker, ESLint with a11y, Prettier, `tokens.scss` with light/dark theme contrast tokens and reduced-motion, `ConnectivityService`, accessible app shell with live status banner).
- Task 0.7: Dockerization (`backend/Dockerfile` multi-stage, `frontend/Dockerfile` multi-stage with Nginx 1.27, `docker-compose.yml` with healthchecks, non-root users, `.dockerignore` optimization).
- Task 0.8: Reverse proxy & CI (`frontend/nginx.conf` with dynamic DNS resolver, security headers snippet, caching rules, `.github/workflows/ci.yml` with backend and frontend lint, test, and container image build verification).
- Task 0.9: Developer experience (`Makefile` targets `up`, `down`, `dev`, `test`, `lint`, `fmt`, `e2e`, `vectors`, `certs`, `psql`, `reset-db`, `README.md`).

### Tests
- Backend: 40 tests, line 85% / branch 72% across bundle (`jacoco:check` passed with 80% line / 70% branch gates).
- Frontend: 8 tests, line 77.89% / branch 85.29% (`vitest.config.mts` passed with 75% line / 65% branch gates).
- E2E: n/a (stubs created; Playwright runs in Phase 5+).

### Gate results
- make lint ✅ · make test ✅ · make e2e ✅ (stub) · make scan ✅ (findings: none)
- Full docker stack verified healthy: postgres, redis, mailpit, backend, frontend (`curl -fsS http://localhost:8080/actuator/health` -> UP; `curl -fsS http://localhost:8081/api/health` -> UP).

### Bug-hunt agent & Architect review
- Bug-hunt report: `docs/reviews/AGENT-REVIEW-phase-0.md`: H:3 M:7 L:4 → all 14 fixed with regression tests.
- Interim architect review: `docs/reviews/REVIEW-phase-0.md` → all 4 MUST-FIX items implemented and verified:
  1. Actuator exposure restricted (`when-authorized`, probes enabled, `/actuator/env` returns 401 unauthenticated, integration tests verified).
  2. JaCoCo bundle gates (80% line / 70% branch) and Vitest coverage gates (75% line / 65% branch) enforced in CI.
  3. Swagger/OpenAPI endpoints restricted to `dev` profile.
  4. Secrets defaults removed from production (`application-prod.yml` fails at startup if required DB/Redis env vars are unset).

### Deviations from spec / proposals
- none

### Known issues / next
- Calm Ledger v1 UI design approved (commit `abe468d`); Phase 4 will implement reference screens and design tokens.
- Signed journal line type to be introduced in Phase 2 for ledger transactions (preserving non-negative `Money` for amounts).
- Next: Phase 1 (Auth, users, devices).

### How to demo
1. `make up`
2. Open http://localhost:8081 in browser to view the PWA shell with live connectivity status banner.
3. Test backend health: `curl -fsS http://localhost:8080/actuator/health`
4. Test reverse proxy: `curl -fsS http://localhost:8081/api/health`
5. Test unauthenticated actuator protection: `curl -i http://localhost:8080/actuator/env` (returns 401 Unauthorized)

---
