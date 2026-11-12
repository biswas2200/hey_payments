# Re-review: Phase 0, 2026-11-12 23:20 IST (@ `95067b8`)

**Verdict: CHANGES REQUIRED (2 small items).** All 6 original MUST-FIX items are fixed and verified. Two new defects block approval: a test suite that fails depending on run order, and an invisible logo in dark mode. Fix both test-first, then resubmit. I'll do a quick confirm, and Phase 0 can then be tagged `v0.1.0`, merged to `main`, and Phase 1 can start.

**Git:** `phase/0-foundation` local = origin = `95067b8` (0 ahead / 0 behind); `main` local = origin = `2f91529`; working tree clean. Phase 0 is not merged into `main` yet (expected before approval). Note that GitHub's default branch (`origin/HEAD`) currently points to `phase/0-foundation`. Switch it back to `main` when Phase 0 merges.

## What I verified independently (separate worktree at `95067b8`, plus the live compose stack)
| Check | Result |
|---|---|
| Frontend `npm ci`, `lint`, `format:check` | ✅ |
| Frontend unit tests + coverage | ✅ 12/12, lines 79.3%, branches 81.6% |
| Frontend production build | ✅ |
| `make e2e` (Playwright, 2 tests against live stack) | ✅ 0 console errors, styled body, banner, logo, SW registered, dark bg |
| Backend `./mvnw verify` | ❌ **48/50, 2 order-dependent failures** (MUST-FIX A) |
| MUST-FIX 1 CSP | ✅ explicit policy; per-request nonce in header **matches** `ngCspNonce` and inline tags in the same response; `inlineCritical: false` |
| MUST-FIX 2 auth | ✅ no Basic auth; no generated password in logs; unknown route → 401 `application/problem+json` `UNAUTHENTICATED`; `Authorization: Basic` rejected |
| MUST-FIX 3 exposure | ✅ from LAN IP only 8081 is reachable (5432/6379/8025/1025/8080 closed); Redis `NOAUTH` without password; `scripts/test-port-exposure.sh` in CI |
| MUST-FIX 4 theme/fonts/brand | ✅ tokens identical to `docs/design/tokens.scss` (formatting only); `prefers-color-scheme` dark works; Inter + Space Grotesk load offline; brand icons + manifest. ⚠️ dark-mode wordmark (MUST-FIX B) |
| MUST-FIX 5 user-facing copy | ✅ "Online · synced" / "Offline · payments still work", Lucide icons, dev card only when `isDevMode()` |
| MUST-FIX 6 non-root nginx | ✅ `uid=101(nginx)`, `nginx-unprivileged` |
| SHOULD-FIX | ✅ `/actuator` no longer proxied (falls through to the SPA); ECS structured logs in prod; `/swagger-ui.html` 302 in dev; `TRUNCATE` blocked; dossier corrected |

## MUST-FIX
### A. [HIGH] The backend suite is order-dependent: `./mvnw verify` fails 2/50 on a different machine or run order
1. `ActuatorSecurityTest.shouldAllowAnonymousHealthWithoutComponentDetails`: `Status expected:<200> but was:<503>`, preceded by `HikariPool-3 - Failed to validate connection … This connection has been closed` and `DataSourceHealthIndicator: DataSource health check failed`.
   **Cause:** `BaseIntegrationTest` declares `@Testcontainers` + `@Container static PostgreSQLContainer`. JUnit stops that container after **each** test class, but Spring's test-context cache reuses the application context (and its Hikari pool) for the next class. The next class then talks to a dead database. It passes or fails depending on class order.
   **Fix:** use the singleton-container pattern. Remove `@Testcontainers`/`@Container`, start the container once in a `static {}` block (or a `@TestConfiguration` bean with `@ServiceConnection` imported by the base class), and never stop it per class. Don't paper over it with `@DirtiesContext`, which only slows the suite down.
2. `ProblemSecurityHandlersTest.accessDeniedHandlerFallbackRequestId:94`: the body contains `requestId":"test-req-id-123"` instead of `hdr-denied-456`.
   **Cause:** `GlobalExceptionHandlerTest` calls `MDC.put("requestId","test-req-id-123")` and never clears it. MDC is thread-local, so it leaks into later tests on the same thread.
   **Fix:** `@AfterEach MDC.clear()` in every test that writes MDC, and `@BeforeEach MDC.clear()` in tests that assert on request IDs.
3. **Regression guard:** run surefire with `-Dsurefire.runOrder=random` in CI (and in `make test-be`) so order dependencies fail immediately. Show two consecutive green random-order runs in PROGRESS.md.

### B. [MEDIUM] The wordmark is nearly invisible in dark mode
- **Evidence:** your own `docs/design/screenshots/phase-0-shell-dark.png`. "hey.payments" is ink `#0E1B2C` on `#0B1420`. `app.html` always loads `brand/logo-wordmark-light.svg`.
- **Fix:** show `logo-wordmark-dark.svg` whenever the dark theme is active, both under `prefers-color-scheme: dark` **and** under `:root[data-theme='dark']`. Two `<img>` elements toggled by theme CSS, or a small `hp-logo` component, both work. Don't recolour the SVG (AGENTS.md §10).
- **Test:** extend the dark Playwright test: the dark wordmark is visible and the light one is hidden, there are zero console errors in dark mode too, and the banner is visible.

## SHOULD-FIX (carry into Phase 1 if not quick)
- The smoke tests write `docs/design/screenshots/*.png` on **every** run (including CI), which dirties the repo. Write them to `test-results/` and copy them into `docs/` only when updating PROGRESS evidence.
- `docker-compose.yml` falls back to `REDIS_PASSWORD:-changeme`. That's fine for dev, but `application-prod.yml` must require `SPRING_DATA_REDIS_PASSWORD` with no default (same rule as the DB password).

## Resubmission
- [ ] `test:` commit reproducing A.1/A.2 (random order), then the `fix:` commit
- [ ] `test:` commit for B (dark logo), then the `fix:` commit
- [ ] `./mvnw verify -Dsurefire.runOrder=random` green twice, `make test`, `make e2e` green, screenshots refreshed
- [ ] PROGRESS.md updated, then "Phase 0 ready for architect re-review"

---

# Review: Phase 0 (Foundation): final, 2026-11-12 20:15 IST

**Verdict: CHANGES REQUIRED.** Fix the 6 MUST-FIX items, then resubmit. Phase 1 must not start before this review says APPROVED.

**Checked (by the architect, independently):**
- Branch `phase/0-foundation` @ `cd4d6a6`, checked out in a separate worktree.
- `./mvnw verify`: ✅ passes (tests, JaCoCo 80/70 gate, Spotless, Checkstyle).
- Frontend `npm ci`, `lint`, `format:check`: ✅. `ng test --coverage`: ✅ 8/8, lines 77.9% (thresholds present). `ng build`: ✅.
- The live compose stack, probed with curl: security headers, actuator, Swagger, Problem Details, `X-Request-Id`.
- **The running PWA, opened in a real browser** at 390×844, light/dark, online/offline, compared against `docs/design/`.
- Code reading: `SecurityConfig`, `RequestIdFilter`, `logback-spring.xml`, `V1__baseline.sql`, nginx, Dockerfiles, compose, the Angular shell.

## Interim MUST-FIX items: verified
| # | Item | Result |
|---|---|---|
| 1 | Actuator exposure | ✅ `/actuator/env` → 401, anonymous health shows no details, probes enabled |
| 2 | Coverage gates | ✅ JaCoCo `check` 80/70 bundle; Vitest thresholds 75/65 |
| 3 | Swagger dev-only | ✅ guarded by the `dev` profile (`/swagger-ui.html` itself is still 401 in dev; see SHOULD-FIX) |
| 4 | Prod secrets | ✅ `application-prod.yml` has no defaults |

Bug-hunt report: the 14 fixes I spot-checked hold (nginx headers, request-id sanitisation, 4xx mapping, `@EnableMethodSecurity`, `Money` bounds, ArchUnit, audit trigger). **Except MED-3 (dark mode), which is not actually effective** (see MUST-FIX 4).

---

## MUST-FIX
### 1. [HIGH] The deployed app renders completely unstyled: CSP blocks Angular
- **Where:** `frontend/security-headers.conf`, `frontend/angular.json`.
- **Evidence:** open `http://localhost:8081` and check the browser console:
  `Applying inline style violates ... 'default-src 'self''` (×2), `Executing inline script violates ...`, `NG05604`.
  The page shows Times New Roman and a 300 px black check icon, with no tokens or layout. `getComputedStyle(body).backgroundColor` = transparent, and `--hp-color-bg` is empty. The production build served *without* the CSP header renders correctly, so the cause is the header.
- **Why:** with no `style-src`/`script-src`, everything falls back to `default-src 'self'`. Angular's critical-CSS inlining injects `<style>` and an inline `onload` handler, and component styles are inline `<style>` elements. All of them are blocked.
- **Fix (required approach):**
  1. `angular.json` → production `optimization.styles.inlineCritical: false` (removes the inline `onload` script).
  2. Use a per-request nonce. Set `<app-root ngCspNonce="__CSP_NONCE__">` in `index.html`. In nginx, `sub_filter '__CSP_NONCE__' '$request_id'; sub_filter_once off;` on the `index.html` location, plus the header `style-src 'self' 'nonce-$request_id'; script-src 'self' 'nonce-$request_id'`. If you decide a nonce is impossible, write `docs/proposals/001-csp-styles.md` before using `'unsafe-inline'` for styles. **Never** use `'unsafe-inline'` for scripts.
  3. Make the policy explicit: `default-src 'self'; script-src …; style-src …; font-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; worker-src 'self'; manifest-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'`.
- **Test first:** add the first Playwright test now (`e2e/smoke.spec.ts`, run by `make e2e` against the compose stack). It must assert **zero console errors**, `body` background = `rgb(247, 245, 240)`, a visible status banner, and a successful service-worker registration. Wire it into CI. The phase gate claimed "full docker stack verified healthy", but nobody looked at the screen. This test makes that impossible to miss again.

### 2. [HIGH] HTTP Basic auth plus a generated in-memory user is enabled
- **Where:** `SecurityConfig.java` (`.httpBasic(Customizer.withDefaults())`). Backend log: `UserDetailsServiceAutoConfiguration : Using generated security password: 9f41…`.
- **Why it matters:** an undocumented credential is printed to the logs, anyone with log access can authenticate (and see health details via `when-authorized`), and Basic auth isn't in the spec (JWT, doc 06). Unauthenticated requests also return an **empty 401**, not RFC 9457 `UNAUTHENTICATED`.
- **Fix:** remove `httpBasic`. Exclude `UserDetailsServiceAutoConfiguration` (or provide an empty `UserDetailsService`) until Phase 1. Add an `AuthenticationEntryPoint` and an `AccessDeniedHandler` that write `application/problem+json` with codes `UNAUTHENTICATED`/`FORBIDDEN`.
- **Tests:** no "generated security password" in the startup log (an `OutputCaptureExtension` test); `GET /api/v1/anything` → 401 problem+json with `code=UNAUTHENTICATED`; `Authorization: Basic …` is rejected.

### 3. [HIGH] Unauthenticated Redis, Postgres and Mailpit are exposed on every network interface
- **Where:** `docker-compose.yml` ports `6379:6379`, `5432:5432`, `1025/8025`.
- **Why it matters:** the plan is to demo on two phones over LAN (doc 12, Phase 8). Anyone on that Wi-Fi can reach a password-less Redis (idempotency keys, rate limits, nonce cache) and Postgres with `change-me`.
- **Fix:** bind dev ports to loopback (`"127.0.0.1:5432:5432"` etc.) or move host ports into `docker-compose.override.yml` for `make dev` only. Add `requirepass` to Redis from `REDIS_PASSWORD` in `.env` (and `.env.example`), and wire `spring.data.redis.password`.
- **Test:** a CI step that runs `docker compose config` and asserts that no service other than `frontend` publishes on `0.0.0.0`.

### 4. [MEDIUM] Dark mode never activates; MED-3 is only fixed on paper
- **Where:** `frontend/src/styles/tokens.scss` only defines `[data-theme='dark']`, and nothing sets that attribute. There's no `prefers-color-scheme` rule.
- **Evidence:** with the browser in dark mode the app stays light. Dark only appears after setting `data-theme="dark"` by hand in DevTools.
- **Fix:** **bring Phase 4.1 and 4.2 forward now**, because Phase 1 builds auth screens and they must not be built on the wrong foundation:
  - Replace `frontend/src/styles/tokens.scss` with `docs/design/tokens.scss` verbatim. It contains the `prefers-color-scheme` rule, `data-theme` overrides, large text and reduced motion. Rename `--hp-color-*` usages to `--hp-*`.
  - Self-host fonts with `@fontsource/space-grotesk`, `@fontsource/inter`, `@fontsource/jetbrains-mono` and `@fontsource/noto-sans-devanagari`. Today `document.fonts` is empty and everything renders in fallback fonts.
  - Replace the hand-made "HP" badge (its gradient uses `#2563eb`, a colour that isn't in the palette) with `docs/design/brand/logo-wordmark-light.svg` / `logo-mark.svg`. Replace `public/favicon.ico` and `public/icons/*` (Angular defaults) with `docs/design/brand/app-icon-*`, and point `manifest.webmanifest` at them.
- **Tests:** a Vitest test that the root gets the dark token values under `matchMedia('(prefers-color-scheme: dark)')`; Playwright smoke (item 1) in both colour schemes; a font-loaded assertion (`document.fonts.check('16px Inter')`).

### 5. [MEDIUM] Developer instructions and internal URLs in user-facing UI
- **Where:** `app.ts` error text "Could not connect to Spring Boot backend (http://localhost:8080)", and `app.html` "Run ./mvnw spring-boot:run inside backend/". This ships in the production bundle, and the URL is wrong anyway (the app calls `/api` through nginx).
- **Fix:** the shell must use product copy and the approved status language (`docs/design/ui-spec.md` §3 `hp-sync-banner`): online → "Online · synced", offline → "Offline · payments still work". The backend-status demo card is dev-only: render it only when `isDevMode()`, or move it to `/dev/status`. Banner icons come from `lucide-angular` (`wifi-off`, `refresh-cw`, `circle-check`), not hand-pasted heroicons.

### 6. [MEDIUM] The frontend container runs nginx as root
- **Where:** `frontend/Dockerfile` (`nginx:1.27-alpine`, no `USER`). AGENTS.md §4 requires non-root containers.
- **Fix:** use `nginxinc/nginx-unprivileged:1.27-alpine` (listens on 8080 internally; map `8081:8080`), and make the filesystem read-only where possible.

---

## SHOULD-FIX (fix in Phase 0 if quick, otherwise list in PROGRESS as carried into Phase 1)
- **Don't proxy `/actuator/` through the public nginx.** Health is for Docker and the orchestrator, not browsers (doc 06: Prometheus is internal-only).
- **Prod JSON logging:** the hand-written pattern in `logback-spring.xml` doesn't escape backslashes or control characters in messages, so a single `\` produces invalid JSON. Use Spring Boot's built-in structured logging (`logging.structured.format.console: ecs` or `logstash`) under the `prod` profile and delete the custom pattern.
- In dev, permit `/swagger-ui.html` (springdoc's redirect entry point currently returns 401).
- `audit_log`: also block `TRUNCATE` (statement-level trigger) and `REVOKE UPDATE, DELETE, TRUNCATE` from the app role (doc 05).
- `ConnectivityService` never produces `degraded`. That's fine for now, but say so in a code comment and add it in Phase 6 (failed-sync backoff).
- `docs/reviews/AGENT-REVIEW-phase-0.md` ends with a stale "Next Steps" section that contradicts its own status table. Tidy it.

## Process note for the builder
"Gate green" includes **looking at the running app**. A CSS-less production page passed lint, unit tests, coverage, the healthcheck and the bug-hunt agent. From now on, every phase's PROGRESS entry must include:
1. Playwright smoke output.
2. A screenshot of each screen touched, in light and dark mode.
3. For screens with a design reference, a side-by-side with `docs/design/reference/screens/*.png`.

## Corrections to `AGENT-REVIEW-phase-0-dossier.md` (`cd4d6a6`)
This review overrides the dossier where they disagree. Fix the dossier text when you resubmit.
| Dossier claim | Actual state |
|---|---|
| §2.4 "Approved Calm Ledger v1 design tokens in `tokens.scss`" | `frontend/src/styles/tokens.scss` is still the pre-approval placeholder (`--hp-color-*`, old values, no `prefers-color-scheme`). The approved file is `docs/design/tokens.scss` (MUST-FIX 4) |
| §2.4 dark-theme contrast tokens "> 7:1" | Tokens exist, but dark mode never activates for users (MUST-FIX 4) |
| §2.2 "HTTP Basic challenge configured so unauthenticated requests receive 401" | Basic auth plus a generated in-memory user is a defect, not a feature. 401 must come from a Problem Details entry point with no Basic auth (MUST-FIX 2) |
| §3.1 "Docker Stack ✅ all healthy" as proof Phase 0 works | Containers are healthy, but the PWA renders unstyled because of the CSP (MUST-FIX 1) |
| §2.5 frontend Dockerfile listed without caveat | Runs nginx as root (MUST-FIX 6) |
| §6 "Reviewer Modification & Change Requests Log: Pending" | The change requests are this file: MUST-FIX 1–6 and SHOULD-FIX |

## What's good
- Clean TDD commit pairs. Good shared kernel (`Money` bounds and JSON shape, UUIDv7, Problem Details, request-id sanitisation with regex and length cap).
- Modulith plus ArchUnit layering rules. Testcontainers base. Append-only audit trigger.
- Strict TypeScript, OnPush, signals, ESLint a11y. Reduced-motion support.
- Security headers now apply on every nginx location (verified with curl).

## Resubmission checklist
- [ ] MUST-FIX 1–6, each with a test first (failing-test commit, then fix commit).
- [ ] `make lint && make test && make e2e` green; `make up`, then the smoke test passes against the stack.
- [ ] PROGRESS.md Phase 0 entry updated with the new evidence, screenshots, and SHOULD-FIX dispositions.
- [ ] Then stop and say "Phase 0 ready for architect re-review".
