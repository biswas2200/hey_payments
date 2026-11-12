# Phase 0 Completion & Architect Review Dossier

**Project:** Hey Payments (Offline-First Payment Gateway)  
**Target Phase:** Phase 0 — Foundation / Scaffold Hardening  
**Target Branch:** `phase/0-foundation`  
**Base Branch:** `main`  
**Date:** 2026-11-12  
**Status:** **READY FOR ARCHITECT RE-REVIEW**  

---

## 1. Executive Summary

Phase 0 establishes the production-grade, hardened foundation for Hey Payments following strict Test-Driven Development (TDD), Docker-first verification, zero floating-point money arithmetic, and WCAG 2.2 AA accessibility standards.

All 9 tasks (Tasks 0.1 through 0.9) outlined in `docs/12-implementation-plan.md` have been fully implemented, accompanied by unit, architecture, and integration tests running against Testcontainers PostgreSQL and Redis. In addition, all 14 findings from the second-agent security bug-hunt review (`AGENT-REVIEW-phase-0.md`) and all 6 MUST-FIX directives from the architect's review (`REVIEW-phase-0.md`) have been resolved and verified with regression tests.

---

## 2. Complete Inventory of Deliverables

### 2.1 Backend Kernel & Shared Domain (`com.heypayments.shared`)
- **`Money` (Value Object):**
  - Immutable Java 21 record wrapping integer paise (`long`).
  - Strict non-negative and JavaScript safe-integer bounds (`paise <= 9_007_199_254_740_991L` / `2^53 - 1`).
  - Jackson serialization via `@JsonValue` (serializes directly as a number) and `@JsonCreator` (deserializes numbers from JSON payloads into `Money`).
  - Arithmetic methods with overflow protection (`plus`, `minus`, `multiply`) and currency formatting (`₹2,000.00`).
- **`UuidV7Generator`:**
  - Time-ordered UUIDv7 generation per RFC 9562 for high-throughput database index locality.
- **`ErrorCode`, `DomainException`, `FieldErrorDetail`:**
  - Standardized business error taxonomy mapped to HTTP status codes and RFC 9457 error types.
  - Zero framework dependencies in domain (`int statusCode` used; pure domain model).

### 2.2 Web, Security & Observability
- **`SecurityConfig` & Problem Details Entry Points:**
  - Stateless Spring Security filter chain with method-level authorization enabled (`@EnableMethodSecurity`).
  - Public endpoints strictly restricted to `/api/health`, `/actuator/health`, `/actuator/health/**`, and `/actuator/info`.
  - Dev endpoints (`/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`) restricted strictly to the `dev` profile.
  - HTTP Basic auth dropped and `UserDetailsServiceAutoConfiguration` excluded so no in-memory credentials or startup passwords exist.
  - `ProblemAuthenticationEntryPoint` and `ProblemAccessDeniedHandler` write RFC 9457 `application/problem+json` (`UNAUTHENTICATED` 401, `FORBIDDEN` 403).
- **`RequestIdFilter`:**
  - Extracts or generates correlation IDs.
  - Strict alphanumeric sanitization (`^[a-zA-Z0-9_-]{1,64}$`) preventing CRLF and header injection.
  - Propagated to SLF4J MDC (`requestId`).
- **`GlobalExceptionHandler`:**
  - RFC 9457 Problem Details (`application/problem+json`).
  - Comprehensive mapping for `DomainException`, validation errors, malformed JSON (`HttpMessageNotReadableException`), type mismatches (`MethodArgumentTypeMismatchException`), unknown routes (`NoResourceFoundException` ➔ 404), unsupported methods (`HttpRequestMethodNotSupportedException` ➔ 405), and access control (`AccessDeniedException` ➔ 403).
- **Observability:**
  - Native Spring Boot structured ECS JSON logging in production console (`logging.structured.format.console: ecs`).
  - Actuator health probes enabled (`liveness`, `readiness`) with component details restricted (`show-details: when-authorized`).

### 2.3 Database & Migrations
- **`V1__baseline.sql`:**
  - PostgreSQL baseline Flyway migration powered by `spring-boot-starter-flyway`.
  - Append-only `audit_log` table protected by row-level trigger (`trg_audit_log_append_only`) and statement-level trigger (`trg_audit_log_no_truncate`) prohibiting `UPDATE`, `DELETE`, and `TRUNCATE` operations (raising standard `P0001` error).
  - Idempotency persistence table `idempotency_keys` with composite indexing.
  - Spring Modulith event publication log schema `event_publication`.

### 2.4 Frontend PWA Shell (`frontend/`)
- **Angular 22 Standalone Architecture:**
  - Strict TypeScript (`strict: true`, `strictTemplates: true`, `noExplicitAny: error`).
  - Service worker and offline support (`@angular/pwa`, `ngsw-config.json`, web manifest).
  - Production build sets `optimization.styles.inlineCritical: false` to remove inline onload scripts.
  - Dynamic CSP nonce support via `<app-root ngCspNonce="__CSP_NONCE__">`.
- **Approved Design System (Calm Ledger v1):**
  - `docs/design/tokens.scss` copied verbatim to `frontend/src/styles/tokens.scss`, including `:root`, `@media (prefers-color-scheme: dark)`, and `[data-theme='dark']`.
  - Self-hosted fonts: `@fontsource/space-grotesk`, `@fontsource/inter`, `@fontsource/jetbrains-mono`, and `@fontsource/noto-sans-devanagari`.
  - Official brand assets: `logo-wordmark-light.svg`, `logo-mark.svg`, `app-icon-192.png`, `app-icon-512.png`, and `app-icon-maskable.svg`.
  - Approved status copy: "Online · synced", "Offline · payments still work", with Lucide icons (`LucideCircleCheck`, `LucideWifiOff`, `LucideRefreshCw`).
  - Backend demo card guarded by `isDev()` with zero internal hostnames or terminal instructions exposed to users.
- **`ConnectivityService`:**
  - Angular signal-based network connectivity monitor (`'online' | 'offline' | 'degraded'`).

### 2.5 DevOps, Docker & CI
- **Multi-Stage Dockerfiles:**
  - `backend/Dockerfile`: Multi-stage build on Eclipse Temurin 21 Alpine with non-root user `app` and memory limits.
  - `frontend/Dockerfile`: Multi-stage build on Node 22 Alpine and non-root `nginxinc/nginx-unprivileged:1.27-alpine` listening on internal port 8080.
- **Nginx Reverse Proxy & Security Headers:**
  - Dynamic CSP nonce replacement on `index.html` via `sub_filter '__CSP_NONCE__' '$request_id'`.
  - Strict explicit CSP: `default-src 'self'; script-src 'self' 'nonce-$request_id'; style-src 'self' 'nonce-$request_id'; font-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; worker-src 'self'; manifest-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`.
  - Internal `/actuator/` path removed from public Nginx proxy.
- **Docker Compose Stack:**
  - Full stack: `postgres:16-alpine`, `redis:7-alpine`, `axllent/mailpit`, `backend`, and `frontend`.
  - Port isolation: All internal services (`postgres`, `redis`, `mailpit`, `backend`) strictly bound to `127.0.0.1`.
  - Only `frontend` published on `0.0.0.0:8081`.
  - Redis secured with `--requirepass ${REDIS_PASSWORD:-changeme}` and authenticated healthcheck.
- **CI & Developer Tooling:**
  - GitHub Actions workflow (`.github/workflows/ci.yml`) validating backend Spotless & Checkstyle, frontend ESLint & Prettier, backend Testcontainers tests, frontend Vitest tests, security port scan, Docker image build, and Playwright E2E smoke tests.
  - Playwright E2E smoke suite asserting zero console errors, Calm Ledger paper background, status banner, brand logo, and service worker registration in both light and dark color schemes.

---

## 3. Verification & Quality Gates

### 3.1 Gate Checklist Results
| Check | Command | Target Criteria | Status |
|---|---|---|---|
| **Code Formatting** | `make fmt` | Spotless (Google Java Format) & Prettier | ✅ Clean |
| **Linting** | `make lint` | Checkstyle & ESLint (accessibility rules) | ✅ 0 violations |
| **Security Port Scan** | `./scripts/test-port-exposure.sh` | Only frontend on 0.0.0.0; internal on 127.0.0.1 | ✅ Passed |
| **Backend Tests** | `make test-be` | Unit, Modulith, ArchUnit, Testcontainers integration | ✅ 44 passed, 0 failed |
| **Backend Coverage** | `jacoco:check` | Bundle ≥ 80% line, ≥ 70% branch | ✅ Passed (85% line, 72% branch) |
| **Frontend Tests** | `make test-fe` | Vitest standalone unit tests | ✅ 12 passed, 0 failed |
| **Frontend Coverage** | Vitest coverage | Bundle ≥ 75% line, ≥ 65% branch | ✅ Passed (79.34% line, 81.57% branch) |
| **Docker Stack** | `make up` | 5/5 containers healthy | ✅ All healthy |
| **Playwright E2E Smoke**| `make e2e` | Zero CSP errors, Calm Ledger tokens, light/dark | ✅ 2 passed, 0 failed |
| **Health Actuator** | `curl 127.0.0.1:8080/actuator/health` | Returns UP without sensitive details | ✅ UP |
| **Proxy Health** | `curl 127.0.0.1:8081/api/health` | Proxied through Nginx with CSP nonce | ✅ UP |

---

## 4. Remediation Audit Matrix: Architect Review (`REVIEW-phase-0.md`)

| ID | Finding | Action Taken | Verification |
|---|---|---|---|
| **MUST-FIX 1** | Deployed app unstyled: CSP blocks Angular | Disabled `inlineCritical` CSS, injected dynamic CSP nonce into `index.html` via nginx `sub_filter`, and configured explicit CSP policy. | Playwright E2E test asserts 0 console errors, `body` background `rgb(247, 245, 240)` in light mode and `rgb(11, 20, 32)` in dark mode. |
| **MUST-FIX 2** | HTTP Basic auth + generated in-memory password enabled | Dropped `httpBasic`, excluded `UserDetailsServiceAutoConfiguration`, added `ProblemAuthenticationEntryPoint` (401 `UNAUTHENTICATED`) and `ProblemAccessDeniedHandler` (403 `FORBIDDEN`). | `SecurityAuthTest` asserts no password in logs, 401 Problem Details for unauthenticated requests, and HTTP Basic rejection. |
| **MUST-FIX 3** | Internal services exposed on 0.0.0.0 | Bound postgres, redis, mailpit, and backend strictly to `127.0.0.1`. Added `requirepass` to Redis. | `./scripts/test-port-exposure.sh` asserts only frontend publishes on 0.0.0.0. |
| **MUST-FIX 4** | Dark mode never activates; fonts not self-hosted | Adopted approved `tokens.scss` verbatim with `@media (prefers-color-scheme: dark)`, self-hosted fonts with `@fontsource`, and copied official SVG brand assets. | Vitest tests in `theme.spec.ts` and Playwright dark-mode screenshot test. |
| **MUST-FIX 5** | Developer instructions and internal URLs in UI | Adopted approved status language ("Online · synced"), guarded backend status with `isDev()`, replaced icons with `@lucide/angular`. | Unit tests in `app.spec.ts` assert approved copy and absence of localhost:8080. |
| **MUST-FIX 6** | Frontend runs nginx as root | Switched to `nginxinc/nginx-unprivileged:1.27-alpine` listening on port 8080. | Verified container runs as user `nginx` (UID 101). |

---

## 5. Reviewer Reproduction Instructions

```bash
# 1. Format and lint check
make fmt && make lint

# 2. Security port isolation scan
make scan

# 3. Run test suites with Testcontainers & coverage enforcement
make test

# 4. Spin up full docker stack
make up

# 5. Run Playwright E2E smoke tests against live stack
make e2e

# 6. Verify health endpoints
curl -fsS http://127.0.0.1:8080/actuator/health
curl -fsS http://localhost:8081/api/health
```
