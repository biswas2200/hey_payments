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
