# Agent Review: Phase 0 (Foundation)

**Reviewer:** Skeptical Senior Reviewer & Security Tester (Automated Bug-Hunt Agent)  
**Date:** 2026-11-12  
**Target Branch:** `phase/0-foundation` compared against `main`  
**Scope:** Backend (Spring Boot 4, Java 21), Frontend (Angular 22, TypeScript 6), DevOps (Docker, Nginx, CI, Makefile)  
**Status:** **REMEDIATED & VERIFIED** — All 3 HIGH, 7 MEDIUM, and 4 LOW findings resolved with tests and verified.

---

## Executive Summary & Findings Tally

| Severity | Count | Status | Summary of Key Issues |
|---|---|---|---|
| **HIGH** | 3 | **3/3 RESOLVED** | Nginx security header stripping; unsanitized `X-Request-Id` log/CRLF injection; unhandled client exceptions mapped to HTTP 500 |
| **MEDIUM** | 7 | **7/7 RESOLVED** | Missing `@EnableMethodSecurity`; actuator public exposure & info leaks; dark-mode WCAG contrast failure; missing JaCoCo coverage gate; missing ArchUnit tests; `Money` JavaScript overflow bound & Jackson missing; missing frontend lint & image build in CI |
| **LOW** | 4 | **4/4 RESOLVED** | Nginx dynamic upstream DNS failure; unconstrained `audit_log` mutability in SQL; PWA manifest placeholder names; missing `prefers-reduced-motion` |

---

## High Severity Findings

### [HIGH-1] Nginx Strips All Security Headers for SPA and Static Asset Routes
- **File:** `frontend/nginx.conf:9-12`, `frontend/nginx.conf:38-53`
- **Spec Reference:** `docs/08-security-threat-model.md` §5 (Headers: CSP, X-Content-Type-Options, Referrer-Policy, Permissions-Policy).
- **Failing Scenario:**  
  In Nginx, if an `add_header` directive is declared inside a `location` block, that block **fails to inherit** all `add_header` directives from the parent `server` block.  
  Because `location /` defines `add_header Cache-Control "no-cache, no-store, must-revalidate" always;`, requests for `GET /` (the main SPA application) completely strip:
  - `Content-Security-Policy`
  - `X-Content-Type-Options: nosniff`
  - `Referrer-Policy: no-referrer`
  - `Permissions-Policy: camera=(self)`
  
  Similarly, `location ~* \.(?:css|js...)$` and service worker / manifest routes strip all security headers.
  
  **Empirical Verification via `curl -I`:**
  ```http
  HTTP/1.1 200 OK
  Server: nginx/1.27.5
  Content-Type: text/html
  Content-Length: 3177
  Cache-Control: no-cache, no-store, must-revalidate
  Accept-Ranges: bytes
  ```
  Notice zero security headers are returned.
- **Suggested Fix:**  
  Extract all common security headers into a separate snippet file `/etc/nginx/security-headers.conf`:
  ```nginx
  add_header Content-Security-Policy "default-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; frame-ancestors 'none'" always;
  add_header X-Content-Type-Options "nosniff" always;
  add_header Referrer-Policy "no-referrer" always;
  add_header Permissions-Policy "camera=(self)" always;
  ```
  Include `include /etc/nginx/security-headers.conf;` inside every `location` block that issues `add_header Cache-Control ...`.
- **Status:** **RESOLVED** in commit `5a0323b`. Verified via `curl -I` on all endpoints.

---

### [HIGH-2] Unsanitized `X-Request-Id` Header Enables HTTP Header Injection and Production Log Forgery
- **File:** `backend/src/main/java/com/heypayments/shared/web/RequestIdFilter.java:32-38`, `backend/src/main/resources/logback-spring.xml:20`
- **Spec Reference:** `docs/08-security-threat-model.md` §0 (Never log secrets / strict correlation ids), `docs/06-api-spec.md` §Conventions.
- **Failing Scenario:**  
  1. `RequestIdFilter` reads `request.getHeader("X-Request-Id")` without length validation or character sanitization, echoing it directly into `response.setHeader("X-Request-Id", requestId)` and storing it in SLF4J `MDC.put("requestId", requestId)`.
  2. In `logback-spring.xml` (`prod` profile):
     ```xml
     <pattern>{"timestamp":"%d{yyyy-MM-dd'T'HH:mm:ss.SSS'Z',UTC}","level":"%p","thread":"%t","logger":"%logger","message":"%replace(%m){'[\r\n]+', ' '}","requestId":"%X{requestId}","userId":"%X{userId}","deviceId":"%X{deviceId}"}%n</pattern>
     ```
     Because `%X{requestId}` is interpolated directly without JSON string escaping, an incoming header like:
     ```
     X-Request-Id: attacker-id","level":"FATAL","admin_action":"impersonated","ignored":"
     ```
     breaks out of the JSON string and injects arbitrary JSON fields and log levels into production log ingestion pipelines (Elasticsearch/Datadog/CloudWatch).
  3. An attacker can also pass CRLF sequences or oversized payloads (e.g. 10MB) causing unbounded memory allocation in MDC.
- **Reproducing Test:**
  ```java
  @Test
  @DisplayName("should reject or sanitize malicious X-Request-Id header")
  void shouldSanitizeMaliciousRequestId() throws ServletException, IOException {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.addHeader("X-Request-Id", "bad\r\nInjected-Header: value");
    MockHttpServletResponse res = new MockHttpServletResponse();
    filter.doFilter(req, res, (q, r) -> {
      assertThat(MDC.get("requestId")).doesNotContain("\r", "\n", "\"");
    });
    assertThat(res.getHeader("X-Request-Id")).doesNotContain("\r", "\n");
  }
  ```
- **Suggested Fix:**  
  Validate incoming `X-Request-Id` against regex pattern `^[a-zA-Z0-9_-]{1,64}$`. If invalid or blank, discard it and generate a fresh UUIDv7. In `logback-spring.xml`, use a structured JSON layout encoder that properly escapes MDC values.
- **Status:** **RESOLVED** in commits `0bcb6b8` (regression tests) and `a6b21b3` (sanitization regex and MDC escaping).

---

### [HIGH-3] Client Protocol Errors (Malformed JSON, Missing Params, 404 Routes) Mapped to HTTP 500 INTERNAL
- **File:** `backend/src/main/java/com/heypayments/shared/web/GlobalExceptionHandler.java:108-131`
- **Spec Reference:** `docs/06-api-spec.md` §Error codes (`VALIDATION_FAILED` = 400, `NOT_FOUND` = 404, `INTERNAL` = 500).
- **Failing Scenario:**  
  `GlobalExceptionHandler` only defines `@ExceptionHandler` for `DomainException` and `MethodArgumentNotValidException`. It defines a catch-all `@ExceptionHandler(Exception.class)` that formats the response as `HTTP 500 Internal Server Error` with `code: "INTERNAL"` and logs at `ERROR` level with stack traces.
  
  When any standard Spring web client exception occurs:
  - `HttpMessageNotReadableException` (malformed JSON syntax, invalid body token)
  - `MethodArgumentTypeMismatchException` (invalid path or query param type)
  - `HttpRequestMethodNotSupportedException` (unsupported HTTP verb)
  - `NoResourceFoundException` (unknown endpoint route in Spring 6+)
  - `AccessDeniedException` (security authorization check failure)
  
  The catch-all intercepts the exception and returns `500 INTERNAL` instead of `400 BAD_REQUEST`, `404 NOT_FOUND`, or `403 FORBIDDEN`.
- **Reproducing Test:**
  ```java
  @Test
  @DisplayName("should map HttpMessageNotReadableException to 400 VALIDATION_FAILED, not 500 INTERNAL")
  void shouldMapMalformedJsonTo400() {
    HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
        "JSON parse error: Unexpected character", new MockHttpInputMessage(new byte[0]));
    ResponseEntity<ProblemResponse> response = exceptionHandler.handleGenericException(ex, request);
    // Current behavior returns 500 INTERNAL; spec requires 400 VALIDATION_FAILED
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
  }
  ```
- **Suggested Fix:**  
  Extend `ResponseEntityExceptionHandler` and override `handleHttpMessageNotReadable`, `handleMethodArgumentNotValid`, `handleHttpRequestMethodNotSupported`, etc., translating them into RFC 9457 `ProblemResponse` with appropriate 4xx status codes.
- **Status:** **RESOLVED** in commits `849bd7e` (regression tests) and `81425ef` (exception handlers for 400, 403, 404, 405).

---

## Medium Severity Findings

### [MED-1] Method-Level Authorization Disabled: `@EnableMethodSecurity` Missing
- **File:** `backend/src/main/java/com/heypayments/shared/infra/config/SecurityConfig.java:12-14`
- **Spec Reference:** `AGENTS.md` §5; `docs/08-security-threat-model.md` §3 ("`@PreAuthorize` per endpoint + deny-by-default `SecurityFilterChain`").
- **Failing Scenario:**  
  `SecurityConfig` includes `@EnableWebSecurity`, but omits `@EnableMethodSecurity`. In Spring Security 6, `@PreAuthorize` and `@Secured` annotations on controllers and services are completely ignored unless `@EnableMethodSecurity` is declared on a configuration class. In Phase 1+, any secure endpoint protected by method security will execute without permission checks.
- **Suggested Fix:**  
  Add `@EnableMethodSecurity` to `SecurityConfig.java`.
- **Status:** **RESOLVED** in commit `d697099`.

---

### [MED-2] Overly Permissive Actuator Exposure and Sensitive Detail Leakage
- **File:** `backend/src/main/java/com/heypayments/shared/infra/config/SecurityConfig.java:32`, `backend/src/main/resources/application.yml:29-30`
- **Spec Reference:** `docs/06-api-spec.md` §System (`/actuator/health` public, `/actuator/prometheus` internal only); `docs/08-security-threat-model.md` §STRIDE.
- **Failing Scenario:**  
  1. `SecurityConfig.java` permits unauthenticated access to `"/actuator/**"` wildcard.
  2. `application.yml` sets `management.endpoint.health.show-details: always`. This exposes database connection URLs, disk space, and internal versions to unauthenticated anonymous requests.
- **Suggested Fix:**  
  - Restrict `SecurityConfig` to `auth.requestMatchers("/api/health", "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()`.
  - Set `management.endpoint.health.show-details: when-authorized`.
- **Status:** **RESOLVED** in commit `d697099`.

---

### [MED-3] Severe WCAG 2.2 AA Contrast Violation in Dark Mode
- **File:** `frontend/src/app/app.scss:38-48`, `frontend/src/app/app.scss:169-176`, `frontend/src/styles/tokens.scss:95-98`
- **Spec Reference:** `AGENTS.md` §10 (WCAG 2.2 AA contrast ≥ 4.5:1); `docs/09-frontend-architecture.md` §8.
- **Failing Scenario:**  
  `app.scss` hardcodes light background colors:
  ```scss
  &.is-offline {
    background-color: #ede9fe; /* hardcoded light purple */
    color: var(--hp-color-status-offline);
  }
  ```
  When dark theme is enabled (`[data-theme='dark']`), `--hp-color-status-offline` switches to `#a5b4fc` (light lavender).  
  The contrast ratio of `#a5b4fc` on `#ede9fe` is **1.69:1**, rendering the status text virtually invisible and failing WCAG 2.2 AA (minimum 4.5:1).  
  The same flaw exists for `.is-degraded` (`#fbbf24` yellow on `#fef3c7`) and `.status-box.offline` (`#f87171` red on `#fee2e2`).
- **Suggested Fix:**  
  Define semantic status background tokens in `tokens.scss` for both light and dark themes (e.g. `--hp-color-status-offline-bg: #ede9fe` for light, `#1e1b4b` for dark) and use `var(--hp-color-status-offline-bg)` in `app.scss`.
- **Status:** **RESOLVED** in commit `e87fdad`. Status background tokens added with contrast ratios > 7:1 in both light and dark themes.

---

### [MED-4] Missing JaCoCo Coverage Gate Configuration
- **File:** `backend/pom.xml:135-152`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 0.2 ("JaCoCo with gates"); `AGENTS.md` §3 ("backend 80% line overall, 95% on crypto/voucher/ledger/sync/settlement").
- **Failing Scenario:**  
  `jacoco-maven-plugin` contains `prepare-agent` and `report` goals, but lacks the `check` goal execution. The build succeeds even if line coverage drops to 0%, providing no automated enforcement of testing thresholds in CI.
- **Suggested Fix:**  
  Add execution with `goal: check` and rule enforcing `minimum: 0.80` for lines across the bundle.
- **Status:** **RESOLVED** in commit `80051d6`. `jacoco:check` configured with 80% bundle line coverage rule.

---

### [MED-5] Missing ArchUnit Architecture Tests
- **File:** `backend/src/test/java/com/heypayments/ModulithArchitectureTests.java`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 0.2, Global Definition of Done ("Spring Modulith `verify()` and ArchUnit tests green"); `AGENTS.md` §5.
- **Failing Scenario:**  
  The ArchUnit dependency is present in `pom.xml`, but zero ArchUnit test cases are written. Only Spring Modulith's `modules.verify()` is tested. Key constraints from `AGENTS.md` §5 (domain layer must not depend on Spring/JPA, no field `@Autowired`, package layering integrity) are unverified.
- **Suggested Fix:**  
  Add `ArchitectureRulesTest.java` enforcing layer isolation, constructor injection, and security annotations.
- **Status:** **RESOLVED** in commit `8dd4131`. `ArchitectureRulesTest.java` verifies no `@Autowired` field injection, domain isolation from Spring, JPA, and web/infra layers.

---

### [MED-6] Money Lacks Safe Integer Bound (`≤ 2^53 - 1`) & Jackson Serialization Annotations
- **File:** `backend/src/main/java/com/heypayments/shared/domain/model/Money.java:14-19`
- **Spec Reference:** `docs/08-security-threat-model.md` §0 (Rule 3: "Money = `long` paise (Java) / `number` integers ≤ 2^53 (TS)"); `docs/openapi.yaml`.
- **Failing Scenario:**  
  1. `Money` does not validate that `paise <= 9_007_199_254_740_991L` (`2^53 - 1`). If a larger number is instantiated, it causes precision loss when transmitted to JavaScript/TypeScript.
  2. `Money` is a record with component `long paise` and has no `@JsonValue` or `@JsonCreator`. Jackson serializes it as `{"amount": {"paise": 1000}}` instead of integer `1000` as specified in OpenAPI and offline protocol schemas. Jackson also fails when deserializing an integer into `Money`.
- **Suggested Fix:**  
  Add validation `if (paise > 9_007_199_254_740_991L) throw new IllegalArgumentException(...)` and annotate `paise()` with `@JsonValue` and `ofPaise(long)` with `@JsonCreator`.
- **Status:** **RESOLVED** in commits `f3bd22f` (failing tests) and `c3ab8cd` (safe integer bound validation + `@JsonValue`/`@JsonCreator`).

---

### [MED-7] CI Workflow Omits Frontend Linting and Docker Image Build Verification
- **File:** `.github/workflows/ci.yml:10-25`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 0.8 ("GitHub Actions CI (lint, tests, build images)").
- **Failing Scenario:**  
  1. `.github/workflows/ci.yml` runs only backend Spotless & Checkstyle in its `lint` job. Frontend `npm run lint` and `npm run format:check` are not executed in CI.
  2. CI does not test building container images (`docker compose build`). Dockerfile regressions will only be detected when deploying or starting containers.
- **Suggested Fix:**  
  Add a frontend lint step to `jobs.lint` and add a `build-images` job to `.github/workflows/ci.yml`.
- **Status:** **RESOLVED** in commit `8c749ab`. Added frontend lint/format check to `lint` job and added `build-images` job running `docker compose build`.

---

## Low Severity Findings

### [LOW-1] Nginx Crashes at Boot if Upstream Backend Host is Not Immediately Resolvable
- **File:** `frontend/nginx.conf:21`
- **Spec Reference:** `docs/11-devops-docker.md` §3.
- **Failing Scenario:**  
  `frontend/nginx.conf` hardcodes `proxy_pass http://backend:8080/api/;`. If Nginx starts before Docker's internal DNS can resolve the `backend` container, Nginx exits with `[emerg] 1#1: host not found in upstream "backend"`.
- **Suggested Fix:**  
  In standalone container setups, declare Docker's DNS resolver `resolver 127.0.0.11 valid=30s;` and assign the proxy target to a variable.
- **Status:** **RESOLVED** in commit `a5401f4`. Added `resolver 127.0.0.11 valid=30s ipv6=off;` and dynamic upstream variable assignment.

---

### [LOW-2] Database `audit_log` Table Missing Append-Only Trigger
- **File:** `backend/src/main/resources/db/migration/V1__baseline.sql:4-13`
- **Spec Reference:** `docs/05-data-model.md` line 153 (`audit_log ... append-only; REVOKE UPDATE, DELETE`); `docs/08-security-threat-model.md` §0.
- **Failing Scenario:**  
  `V1__baseline.sql` creates `audit_log` but does not add a database-level trigger or privilege revocation to prohibit UPDATE and DELETE. An application connection with standard privileges can modify or delete audit rows.
- **Suggested Fix:**  
  Add a PostgreSQL trigger on `audit_log` raising an exception on `BEFORE UPDATE OR DELETE`.
- **Status:** **RESOLVED** in commit `a5401f4`. Added `trg_audit_log_append_only` trigger raising `hp001` on any UPDATE or DELETE attempt.

---

### [LOW-3] PWA Manifest and Document Title Placeholder Inconsistencies
- **File:** `frontend/src/index.html:5`, `frontend/public/manifest.webmanifest:2-3`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 0.6; `AGENTS.md` §10.
- **Failing Scenario:**  
  `<title>` in `index.html` is `Frontend`, and `manifest.webmanifest` contains `"name": "frontend"` and `"short_name": "frontend"`.
- **Suggested Fix:**  
  Set title and manifest names to "Hey Payments" and add `theme_color` (`#0f766e`) and `background_color` (`#f7f5f0`).
- **Status:** **RESOLVED** in commit `e87fdad`. Updated title, manifest metadata, theme color, and background color.

---

### [LOW-4] Missing `prefers-reduced-motion` CSS Media Query
- **File:** `frontend/src/app/app.scss:24, 135`
- **Spec Reference:** `AGENTS.md` §10; `docs/09-frontend-architecture.md` §8.
- **Failing Scenario:**  
  CSS transitions on the banner and refresh button animate unconditionally, ignoring user preferences for reduced motion.
- **Suggested Fix:**  
  Add `@media (prefers-reduced-motion: reduce) { * { transition-duration: 0.01ms !important; animation-duration: 0.01ms !important; } }`.
- **Status:** **RESOLVED** in commit `e87fdad`. Added global `prefers-reduced-motion: reduce` rules in `tokens.scss`.

---

## Conclusion

All 14 findings (3 HIGH, 7 MEDIUM, 4 LOW) identified during the second-agent bug hunt have been remediated with regression tests and verified against the Docker stack. All MUST-FIX and SHOULD-FIX remediation items from the architect review have also been implemented and verified.
