# Phase 0 Completion & Architect Review Dossier

**Project:** Hey Payments (Offline-First Payment Gateway)  
**Target Phase:** Phase 0 — Foundation / Scaffold Hardening  
**Target Branch:** `phase/0-foundation`  
**Base Branch:** `main`  
**Commits Ahead:** 43 commits  
**Date:** 2026-11-12  
**Status:** **READY FOR ARCHITECT REVIEW**  

---

## 1. Executive Summary

Phase 0 establishes the production-grade, hardened foundation for Hey Payments following strict Test-Driven Development (TDD), Docker-first verification, zero floating-point money arithmetic, and WCAG 2.2 AA accessibility standards.

All 9 tasks (Tasks 0.1 through 0.9) outlined in `docs/12-implementation-plan.md` have been fully implemented, accompanied by unit, architecture, and integration tests running against Testcontainers PostgreSQL and Redis. In addition, all 14 findings from the second-agent security bug-hunt review (`AGENT-REVIEW-phase-0.md`) and all 4 MUST-FIX directives from the architect's interim review (`REVIEW-phase-0.md`) have been resolved and verified with regression tests.

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
- **`SecurityConfig`:**
  - Stateless Spring Security filter chain with method-level authorization enabled (`@EnableMethodSecurity`).
  - Public endpoints strictly restricted to `/api/health`, `/actuator/health`, `/actuator/health/**`, and `/actuator/info`.
  - Swagger UI (`/swagger-ui/**`) and OpenAPI docs (`/v3/api-docs/**`) restricted strictly to the `dev` profile.
  - HTTP Basic challenge configured so unauthenticated requests receive `401 Unauthorized`.
- **`RequestIdFilter`:**
  - Extracts or generates correlation IDs.
  - Strict alphanumeric sanitization (`^[a-zA-Z0-9_-]{1,64}$`) preventing CRLF and header injection.
  - Propagated to SLF4J MDC (`requestId`).
- **`GlobalExceptionHandler`:**
  - RFC 9457 Problem Details (`application/problem+json`).
  - Comprehensive mapping for `DomainException`, validation errors, malformed JSON (`HttpMessageNotReadableException`), type mismatches (`MethodArgumentTypeMismatchException`), unknown routes (`NoResourceFoundException` ➔ 404), unsupported methods (`HttpRequestMethodNotSupportedException` ➔ 405), and access control (`AccessDeniedException` ➔ 403).
- **Observability:**
  - Structured UTC JSON logging in production via `logback-spring.xml` with MDC field escaping.
  - Actuator health probes enabled (`liveness`, `readiness`) with component details restricted (`show-details: when-authorized`).

### 2.3 Database & Migrations
- **`V1__baseline.sql`:**
  - PostgreSQL baseline Flyway migration.
  - Append-only `audit_log` table protected by a database-level trigger (`trg_audit_log_append_only`) prohibiting `UPDATE` and `DELETE` operations (raising error `hp001`).
  - Idempotency persistence table `idempotency_keys` with composite indexing.
  - Spring Modulith event publication log schema `event_publication`.

### 2.4 Frontend PWA Shell (`frontend/`)
- **Angular 22 Standalone Architecture:**
  - Strict TypeScript (`strict: true`, `strictTemplates: true`, `noExplicitAny: error`).
  - Service worker and offline support (`@angular/pwa`, `ngsw-config.json`, web manifest).
- **Design System & Accessibility (WCAG 2.2 AA):**
  - Approved Calm Ledger v1 design tokens in `tokens.scss`.
  - Semantic status background tokens for both Light and Dark themes providing contrast ratios > 7:1 (exceeding AA 4.5:1 requirement).
  - Global `prefers-reduced-motion: reduce` overrides disabling animation and transition durations.
  - Responsive app shell with sticky connectivity banner featuring role="status", aria-live="polite", icon + text + color, and 44px minimum touch targets.
- **`ConnectivityService`:**
  - Angular signal-based network connectivity monitor (`'online' | 'offline' | 'degraded'`).

### 2.5 DevOps, Docker & CI
- **Multi-Stage Dockerfiles:**
  - `backend/Dockerfile`: Multi-stage build on Eclipse Temurin 21 Alpine with non-root user `app` and memory limits.
  - `frontend/Dockerfile`: Multi-stage build on Node 22 Alpine and Nginx 1.27 Alpine.
- **Nginx Reverse Proxy:**
  - Embedded Docker DNS resolver (`resolver 127.0.0.11 valid=30s ipv6=off;`) with dynamic upstream variable resolution preventing boot crashes if backend is starting.
  - Modular security headers (`security-headers.conf`) included across all location blocks (CSP, X-Content-Type-Options, Referrer-Policy, Permissions-Policy).
  - Cache control: immutable 1-year caching for hashed static assets, `no-cache` for HTML, service worker, and webmanifest.
- **Docker Compose Stack:**
  - Fully containerized stack: `postgres:16-alpine`, `redis:7-alpine`, `axllent/mailpit`, `backend`, and `frontend`.
  - Configured health checks with dependency order (`depends_on.condition: service_healthy`).
- **CI & Developer Tooling:**
  - GitHub Actions workflow (`.github/workflows/ci.yml`) validating backend Spotless & Checkstyle, frontend ESLint & Prettier, backend Testcontainers tests, frontend Vitest tests, and `docker compose build`.
  - Makefile providing targets: `up`, `down`, `dev`, `test`, `test-be`, `test-fe`, `lint`, `fmt`, `e2e`, `vectors`, `certs`, `psql`, `reset-db`.

---

## 3. Verification & Quality Gates

### 3.1 Gate Checklist Results
| Check | Command | Target Criteria | Status |
|---|---|---|---|
| **Code Formatting** | `make fmt` | Spotless (Google Java Format) & Prettier | ✅ Clean |
| **Linting** | `make lint` | Checkstyle & ESLint (accessibility rules) | ✅ 0 violations |
| **Backend Tests** | `make test-be` | Unit, Modulith, ArchUnit, Testcontainers integration | ✅ 40 passed, 0 failed |
| **Backend Coverage** | `jacoco:check` | Bundle ≥ 80% line, ≥ 70% branch | ✅ Passed (85% line, 72% branch) |
| **Frontend Tests** | `make test-fe` | Vitest standalone unit tests | ✅ 8 passed, 0 failed |
| **Frontend Coverage** | Vitest coverage | Bundle ≥ 75% line, ≥ 65% branch | ✅ Passed (77.89% line, 85.29% branch) |
| **Docker Stack** | `make up` | 5/5 containers healthy | ✅ All healthy |
| **Health Actuator** | `curl localhost:8080/actuator/health` | Returns UP without sensitive details | ✅ UP |
| **Proxy Health** | `curl localhost:8081/api/health` | Proxied through Nginx with security headers | ✅ UP |

### 3.2 Architectural & Purity Enforcement (ArchUnit)
`ArchitectureRulesTest.java` verifies 4 critical constraints:
1. `noFieldInjection`: Disallows `@Autowired` on fields across the entire backend.
2. `domainMustNotDependOnSpring`: Prohibits any class in `domain..` from depending on Spring framework.
3. `domainMustNotDependOnPersistence`: Prohibits any class in `domain..` from depending on JPA/persistence annotations.
4. `domainMustNotDependOnWebOrInfra`: Prohibits domain classes from depending on web or infrastructure packages.

---

## 4. Remediation Audit Matrix

### 4.1 Bug-Hunt Agent Review Findings (`AGENT-REVIEW-phase-0.md`)
| ID | Severity | Issue | Resolution Commit |
|---|---|---|---|
| **HIGH-1** | HIGH | Nginx `location` blocks strip parent security headers | `5a0323b` (Extracted `security-headers.conf` and included in all locations) |
| **HIGH-2** | HIGH | `RequestIdFilter` CRLF injection and unescaped log injection | `0bcb6b8` (Test), `a6b21b3` (Regex validation & log escaping) |
| **HIGH-3** | HIGH | Unhandled client exceptions mapped to HTTP 500 INTERNAL | `849bd7e` (Test), `81425ef` (Problem Details handlers for 400, 403, 404, 405) |
| **MED-1** | MEDIUM | Method security disabled (`@EnableMethodSecurity` missing) | `d697099` (Added `@EnableMethodSecurity`) |
| **MED-2** | MEDIUM | Overly permissive actuator exposure and info leak | `d697099` (Restricted matchers & `show-details: when-authorized`) |
| **MED-3** | MEDIUM | WCAG 2.2 AA contrast failure in Dark Mode | `e87fdad` (Added high-contrast dark theme background tokens) |
| **MED-4** | MEDIUM | Missing JaCoCo coverage gate | `80051d6` (Configured `jacoco:check` with 80% line / 70% branch) |
| **MED-5** | MEDIUM | Missing ArchUnit architecture tests | `8dd4131` (Added `ArchitectureRulesTest.java`) |
| **MED-6** | MEDIUM | Money lacks JavaScript safe integer bound & Jackson annotations | `f3bd22f` (Test), `c3ab8cd` (Bound check + `@JsonValue`/`@JsonCreator`) |
| **MED-7** | MEDIUM | CI omits frontend linting and docker build | `8c749ab` (Added frontend lint/format and `docker compose build` jobs) |
| **LOW-1** | LOW | Nginx crashes if backend host unresolvable at startup | `a5401f4` (Configured Docker DNS resolver & dynamic proxy variable) |
| **LOW-2** | LOW | Database `audit_log` missing append-only trigger | `a5401f4` (Added PostgreSQL trigger prohibiting UPDATE/DELETE) |
| **LOW-3** | LOW | PWA manifest and title placeholder values | `e87fdad` (Updated title, manifest name, theme/background colors) |
| **LOW-4** | LOW | Missing `prefers-reduced-motion` CSS query | `e87fdad` (Added global reduced-motion overrides) |

### 4.2 Architect Interim Review Directives (`REVIEW-phase-0.md`)
| Directive | Action Taken | Verification |
|---|---|---|
| **MUST-FIX 1: Actuator Exposure & Probes** | Set `show-details: when-authorized`, enabled health probes, and restricted permitAll to `/actuator/health/**` and `/actuator/info`. | Integration test in `ActuatorSecurityTest.java` confirms `/actuator/env` returns 401 Unauthorized and `/actuator/health` returns status without component details. |
| **MUST-FIX 2: Coverage Gates** | Enforced JaCoCo 80% line / 70% branch in `backend/pom.xml`. Added `@vitest/coverage-v8` with 75% line / 65% branch thresholds in `frontend/vitest.config.mts` and `angular.json`. | Both `mvn verify` and `ng test` fail if thresholds are breached. Current: Backend 85%/72%, Frontend 77.89%/85.29%. |
| **MUST-FIX 3: Swagger Dev-Only** | Updated `SecurityConfig.java` to inject `Environment` and only permit `/swagger-ui/**` and `/v3/api-docs/**` when `dev` profile is active. | Tested in `ActuatorSecurityTest.java` (returns 401 in test/prod profiles). |
| **MUST-FIX 4: Production Secrets Defaults** | Added `backend/src/main/resources/application-prod.yml` with no fallback values for datasource credentials or redis host. | Fails fast at startup in production if environment variables are not supplied. |

---

## 5. Reviewer Reproduction Instructions

```bash
# 1. Format and lint check
make fmt && make lint

# 2. Run test suites with Testcontainers & coverage enforcement
make test

# 3. Spin up full docker stack
make up

# 4. Verify health endpoints
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost:8081/api/health

# 5. Verify security protection on unauthenticated actuator endpoint
curl -i http://localhost:8080/actuator/env   # Expect HTTP 401 Unauthorized
```

---

## 6. Reviewer Modification & Change Requests Log

*(This section is reserved for the reviewer/product owner to provide specific modifications or adjustments. All items listed here will be addressed test-first before branch signoff.)*

| Item # | Area / File | Requested Change | Priority | Resolution Status |
|---|---|---|---|---|
| *Pending* | *Pending* | *Awaiting review notes from user / reviewer* | *TBD* | *Pending* |

---
