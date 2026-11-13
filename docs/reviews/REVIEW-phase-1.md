# Conditions check: Phase 1, 2026-11-13 (@ `6a60700`)

**Verdict: APPROVED.** C1–C4 are done. Merge `phase/1-auth-devices` to `main` and tag `v0.2.0`.

- **C1:** confirmed in the code.
  - Payers must land exactly on `/pay` and merchants on `/merchant`.
  - The Dexie test asserts that `pin_salt` and `pin_hash` are strings and that there are zero `hp_pin_*` keys in localStorage.
  - The MC is now really verified with `crypto.subtle.verify`, and a mutated payload must fail.
  - The device key must have `extractable === false`.
  - Protected routes must redirect to login when unauthenticated.
  - The console filter is gone.
- **C2:** there's a logout button (`#logout-btn`). The e2e test logs out, lands on `/auth/login`, checks the `hp_rt` cookie is cleared, then logs in again and lands on `/pay`.
- **C3:** Dexie `version(1)` is unchanged and `version(2)` adds `meta`, with an upgrade test (`db.spec.ts`).
- **C4:** the property is `hp.crypto.server-key.rotation-cron` in `application.yml`, the code and PROGRESS. It can be overridden with `HP_SERVER_KEY_ROTATION_CRON`.
- **Extra:** `GET /devices/me` now returns 204 when the user has no device, and the frontend handles it.
- **How I checked:** I kept the load on the laptop low and didn't start a second stack.
  - Frontend unit tests: **108/108**.
  - `DeviceControllerTest` and `ServerKeyServiceTest`: 8/8.
  - **E2E against your running stack: 8/8, with no console filter.** That also proves the deployed backend returns 204, not 404.
  - I relied on your report for the full backend suite (123 tests); I verified it at 122 tests at `9389f3d`.
- The tracked notes are in PROGRESS ✅.

---

# Re-review: Phase 1, 2026-11-13 (@ `9389f3d`)

**Verdict: APPROVED WITH CONDITIONS.** All six MUST-FIX items are fixed, and I confirmed each one independently in a running copy of the stack. Fix conditions C1–C4 on `phase/1-auth-devices` and push. Once I've confirmed them, merge to `main` and tag `v0.2.0`. Do not start Phase 2 until then.

**Git:** `phase/1-auth-devices` = origin = `9389f3d` ✅ (25 new commits, TDD `test:` → `fix:` pairs throughout).

## How I checked
- **Clean worktree at `9389f3d`:**
  - Backend `./mvnw verify -Dsurefire.runOrder=random` **twice: green** (122 tests, 0 failures, line coverage 90.6%, branch coverage 72.8%).
  - Frontend `npm ci`, `api:gen` (**no drift**), lint, format and production build: green.
  - Frontend unit tests **103/103 twice, run while the backend suite was loading the CPU** (the condition that failed last time).
- **An isolated copy of the stack** (`docker compose -p hpreview`, alternate ports): I tested everything below live through nginx.
- **Your e2e suite:** 8/8 passed against my copy of the stack. I also ran **my own stricter Playwright spec**, described below.

## MUST-FIX results
| # | Item | Result |
|---|---|---|
| 1 | Circular DI | ✅ Fixed. With `TokenStore` and an `HttpBackend`-based refresh, every auth route renders in the production build. **My strict run:** the payer flow (register → Mailpit OTP → verify → login → device → set PIN → `/pay`, then log in again → `/pay`) and the merchant flow (→ `/merchant`) both work. The device key is `extractable:false`. Unauthenticated `/auth/device-register` and `/auth/set-pin` redirect to login. |
| 2 | Rate limiting | ✅ Fixed. Through nginx: client A gets 20 × 401, then 429. Client B (a different IP) still gets 401. Spoofed `X-Forwarded-For` values are ignored. The per-phone limit triggers on the 6th attempt across two IPs. The 429 body is `rate-limited` with a `requestId`. |
| 3 | JWT key | ✅ Fixed. Header `{kid: jwt-…, typ: JWT, alg: ES256}`, `iss: heypayments`. **The token still validates after `docker restart backend`.** An unknown kid gets 401. The JWT kid is **not** in the protocol JWKS. `server_keys.purpose` separates the keys. |
| 4 | JCS / vectors | ✅ Fixed. erdtman plus `canonicalize`. Vectors now cover RFC 8785 numbers (`1e+21`, `1e-7`, `-0`, max-safe), unicode/emoji, escapes, and negative cases (tampered payload, wrong key, malformed signature length, non-integer, overflow). Both Java and TS consume them. Non-integer and unsafe numbers are rejected on sign and verify. |
| 5 | Generated client / contract | ✅ Accepted. `ng-openapi-gen` with a CI drift check. The contract test checks paths and methods only, not schemas. The calls that remain hand-written (`verify-otp`, `logout`, `devices/me`, `auth/pin`) exist only because `openapi.yaml` lacks them. **Architect action:** I'll merge proposals 002 and 003 into `openapi.yaml`. Then regenerate, delete the remaining raw calls and extend the contract test to cover schemas (do this in Phase 2). |
| 6 | Flaky tests | ✅ Fixed. Injectable PBKDF2, single worker, 15 s timeout. Green under load twice. |

SHOULD-FIX: all handled ✅. `PENDING_VERIFICATION` with LOCKED/DISABLED blocked from OTP. 90-day rotation check. Cookie `Path=/api/v1/auth` (confirmed in `Set-Cookie`). `Origin` check (an evil origin on refresh gets 403). PIN moved to Dexie `meta` with a lockout (confirmed in the browser: `pin_salt`, `pin_hash`, `pin_failed_attempts`, `pin_lockout_until`, localStorage empty). `MessageDigest.isEqual`. No phone numbers in logs.

## CONDITIONS (fix on this branch, then push)
### C1. `e2e/auth.spec.ts` can't fail on the things it claims to test
Your suite passes, and my stricter spec shows the flows really work. But the assertions as written would stay green if they broke:
- Both flows accept `/auth/login` as the final URL (`url.pathname.includes('/pay') || …'/auth/login'`). **A failed login passes.** Assert `/pay` for payers and `/merchant` for merchants only.
- The PIN check resolves `'stored'` when the `meta` store is missing or a read errors, so it is always truthy. Assert that `pin_salt` and `pin_hash` exist in `meta` and that `localStorage` has no `hp_pin_*` keys.
- The "MC verified via JWKS" check never calls `crypto.subtle.verify`; it only checks that the signature is 64 bytes. Canonicalise `envelope.p` and verify it against the JWKS key. Also assert that a mutated payload fails.
- The payer test title says "logout → login again", but those steps are missing. Add them after C2.
- The console filter drops every message containing `"404"`. Instead, allow only the one expected `GET /devices/me` 404 by URL, or better, make `devices/me` return `204` or `{device:null}` when no device exists and drop the filter.
- The "every auth route" test covers only 3 routes. Add `device-register` and `set-pin`: unauthenticated they must redirect to login; authenticated they must render the form.
- Assert `extractable === false` on the stored device key.
(My strict spec is a working reference for the MC verification and the Dexie checks.)

### C2. There's no way to log out in the UI
`AuthStore.logout()` exists, but no screen calls it (`/pay` and `/merchant` have no logout control). Add a logout action to the post-login placeholder: lucide `log-out` icon plus a label, using the ui-spec button styles. Cover it with the C1 e2e test (log out → `/auth/login`, refresh cookie cleared, log in again → landing page).

### C3. The Dexie schema changed without a version bump
`meta` was added inside `this.version(1)`. Any browser that opened the Phase 1 build before this change keeps a v1 database without `meta`, and PIN storage fails. Keep version 1 as it was, add `this.version(2).stores({ meta: 'key' })`, and add a unit test that opens a v1 database (fake-indexeddb) and then upgrades it.

### C4. Correct PROGRESS
PROGRESS cites `${hp.crypto.server-key.rotation-cron:0 0 2 * * ?}`, but the code uses `${app.crypto.key-rotation-cron:0 0 0 * * ?}`. Pick one name (use `hp.crypto.*`, to match the other `HP_` settings), put it in `application.yml`, and make PROGRESS match.

## Notes (no action needed for the gate; carry into PROGRESS as tracked items)
- **Migrations:** `V3__devices_merchants.sql` was edited in place (adding the `purpose` column). That's OK this time because V3 never reached `main`. From now on, a migration that has run anywhere (including your dev DB) is immutable; add a new `V<n>`.
- **JWT performance:** every request does a DB lookup and SEC1 decode for verification, and every token issue decrypts the private key. Cache public keys by kid and the active private key in memory (with eviction on rotation) before the Phase 8 p95 target.
- **Retiring keys** are never moved to `REVOKED` once their validity ends, so the JWKS will grow. Add cleanup with the next crypto work.
- **CORS and Origin allow-lists** are hard-coded (`localhost`, `127.0.0.1`, `nginx`). `.env.example` has `HP_CORS_ORIGINS` and `HP_JWT_ISSUER`, but nothing reads them. Wire them in (needed for the Phase 8 LAN demo) or remove them.
- **Phase 4 UI notes:** the device-register card sits ~330 px below the header with empty space above it. The header tagline "Offline-First Payment Gateway Architecture" and the `/pay` placeholder copy are developer text. Both get replaced when P1 Home and the auth screens are matched to `reference/screens/*`.

## Resubmission checklist
- [ ] C1–C4, each with a `test:` → `fix:` pair where applicable
- [ ] `make lint`, `make test`, `make e2e` green
- [ ] `git push origin phase/1-auth-devices`, then "Phase 1 conditions done"

---

# Review: Phase 1 (Auth, users, devices), 2026-11-13 (@ `e0d1045`)

**Verdict: CHANGES REQUIRED.** The backend is strong. **The Phase 1 frontend does not work in the deployed app**, which fails the phase acceptance criterion ("register payer + merchant via UI"). Fix MUST-FIX 1–6 test-first, then resubmit. Do not start Phase 2.

**Git:** `main` = origin = `7e6e556`, tag `v0.1.0` ✅. `phase/1-auth-devices` is at `e0d1045` and **51 commits ahead of origin, not pushed.** Push it with the fixes.

## How I checked
- **Clean worktree at `e0d1045`.** Backend `./mvnw verify -Dsurefire.runOrder=random` **×2: green** (106 tests, JaCoCo gate). Frontend lint, format and build: green. Unit tests: **97/97 when the machine is idle, but 5 failed under CPU load** (MUST-FIX 6).
- **An isolated copy of the stack** (`docker compose -p hpreview`, alternate ports) running your branch. I drove the real API end to end: register, Mailpit OTP, verify, login, refresh, reuse, logout, devices, MC, CRL, PIN, rate limits.
- **The production PWA in Chrome** (Playwright, 390×844, light and dark) on every auth route, plus a development build to de-minify the error.
- **Code reading:** auth, crypto and device modules; `AuthStore`, interceptor, `KeyService`, crypto/JCS.

## Verified working ✅
- Register 201, duplicate 409, invalid phone/short password/`ADMIN` role 400. Login before OTP 403. Wrong OTP 400, OTP replay 400, OTP from Mailpit works.
- Login → ES256 access token (15 min, `iss`, `jti`) plus `hp_rt` cookie (`HttpOnly; SameSite=Strict`, 14 days).
- **Refresh rotates. Reusing the old token → 401 and the whole family is revoked** (the new token dies too). Tampered signature and `alg=none` are rejected.
- Device registration: 201, a second active device → 409 `DEVICE_ALREADY_ACTIVE`, an invalid point → 400. **Merchant Certificate signature verifies against the public JWKS** with JCS bytes. `devices/me` returns the MC. CRL requires auth.
- `PUT /auth/pin` as a merchant → 403. Argon2id (m=19 MiB, t=2, p=1). Refresh tokens stored hashed. OTP from `SecureRandom`. Server private keys AES-GCM encrypted with `HP_MASTER_KEY`, which is mandatory in prod.
- Frontend: access token kept in memory (a signal, not storage), device keys `extractable: false`, concurrent-401 refresh deduplicated.
- TDD commit pairs throughout. The bug-hunt fixes I spot-checked hold.

---

## MUST-FIX
### 1. [HIGH] The deployed app can't show any auth screen: circular DI crashes the router
- **Evidence:** in the production build, `/auth/login`, `/auth/register`, `/auth/verify-otp`, `/auth/device-register` and `/auth/set-pin` all render only the Phase 0 placeholder. Console: `NG0201`. The development build names the cause: `NG0200: Circular dependency detected for _AuthStore. Source: Standalone[_LoginComponent]`.
- **Cycle:** `AuthStore` → `AuthApiService` → `HttpClient` → `authInterceptor` (`inject(AuthStore)`, `auth.interceptor.ts:34`) → `AuthStore`.
- **Why the tests missed it:** unit tests give the interceptor a mocked store, and the e2e smoke only opens `/`.
- **Fix:**
  - Add a dependency-free `TokenStore` (signals only: access token, expiry). The interceptor reads from it; `AuthStore` writes to it.
  - Perform the refresh call through an `HttpBackend`-based client, which bypasses interceptors, so a refresh can never recurse into the interceptor.
- **Tests (write these first):**
  1. A unit test that builds `TestBed` with the **real `appConfig.providers`** and asserts that `inject(AuthStore)`, the router navigation to each auth route, and `LoginComponent` creation don't throw.
  2. **Playwright e2e against the compose stack** (`e2e/auth.spec.ts`). Every auth route renders its form with zero console errors. Then the **full acceptance flow in the UI for both roles:**
     - Register.
     - Read the OTP from the Mailpit API (`/api/v1/messages`).
     - Verify, log in, and register the device.
     - Merchant: the MC is stored and its signature verifies in the browser against JWKS.
     - Payer: set the PIN.
     - Log out, then log in again with the same device.
  3. PROGRESS.md must include light and dark screenshots of **every** auth screen, as required since Phase 0.

### 2. [HIGH] Rate limiting is one global bucket behind nginx, and there's no per-phone limit
- **Evidence:** `RateLimitingFilter.resolveClientIp()` returns `request.getRemoteAddr()`, which is nginx's container address for **every** user. Live, after 8 auth calls from one tester, every login on the system (any phone, including unknown users) got `429`. Five failed logins a minute from anyone lock out all users. Spec (doc 06): **5/min per phone and 20/min per IP** for login/OTP.
- **Fix:**
  - nginx: `proxy_set_header X-Forwarded-For $remote_addr;` (**overwrite**, never append, so clients can't inject) and `X-Real-IP $remote_addr`.
  - Spring: `server.forward-headers-strategy: native` and `server.tomcat.remoteip.internal-proxies` limited to the compose network. `getRemoteAddr()` then becomes the real client, and forwarded headers from untrusted sources are ignored.
  - Add a **per-phone** bucket (5/min) for `/auth/login` and `/auth/verify-otp`, alongside the per-IP bucket (20/min). Applying it in the service layer, where the phone is already parsed, is fine.
  - Make the 429 body consistent with the other errors: `type: https://heypayments.dev/errors/rate-limited`, with `requestId` filled.
- **Tests:**
  - Two clients with different source IPs don't share a bucket.
  - A spoofed `X-Forwarded-For` from an untrusted remote is ignored.
  - The per-phone limit triggers across different IPs.
  - An integration test through the real nginx config (compose e2e): one user hitting the limit doesn't block a second user.

### 3. [MEDIUM] The JWT signing key is regenerated in memory on every start, with a fixed `kid`, and `iss` isn't validated
- **Evidence:** `AuthConfig.jwtTokenService()` calls `generateP256KeyPair()` at boot, with kid `"jwt-auth-key-01"`. Every restart invalidates all access tokens, two instances can't share sessions, and the same kid names different keys over time. `validateAndParseToken` checks the signature and `exp` only.
- **Fix:** persist the JWT key the same way as server keys: a `purpose` column (`JWT` vs `PROTOCOL`, kept separate per doc 08), encrypted with `HP_MASTER_KEY`, a unique kid, and rotation with a RETIRING verification window. Pick the verifier by the header `kid`. Enforce `alg=ES256`, `typ=JWT` and `iss=heypayments`, and reject unknown kids. **Do not** publish the JWT key in the protocol JWKS.
- **Tests:** a token issued, then a new application context (simulated restart), still validates; unknown kid → 401; wrong `iss` → 401; a RETIRING key still validates until it expires.

### 4. [MEDIUM] Hand-rolled JCS is not RFC 8785 for numbers, and the shared vectors are too thin to catch it
- **Evidence:** `JcsCanonicalizer.canonicalizeNode` writes non-integral numbers with Jackson's `asText()` (Java formatting, e.g. `1.0E-7`), while ECMAScript/RFC 8785 requires `1e-7`. Integers beyond 2^53 aren't normalised either. Doc 02 specifies `org.erdtman:java-json-canonicalization` and the `canonicalize` npm package. `test-vectors/` has only 4 JCS cases and 1 sign/verify case. PROGRESS cites `test-vectors/p256/` and `test-vectors/argon2/`, which **don't exist**.
- **Fix:**
  - Use the specified libraries, or keep your implementations and prove them against the full RFC 8785 test data.
  - In the protocol layer, **reject** non-integer numbers and integers > 2^53−1 in any signed payload. All protocol amounts are integer paise.
  - Expand `test-vectors/`: RFC 8785 Appendix B numbers (`0`, `-0`, `1e21`, `1e-7`, `9007199254740991`), unicode and emoji keys, nested arrays, escapes, plus **negative** cases (tampered payload, wrong key, malformed signature length). Make sure the Java and TypeScript suites consume every file.

### 5. [MEDIUM] Hand-written API client, and no OpenAPI contract test
- **Evidence:** `core/api/auth-api.service.ts` and `services/api.service.ts` hand-write `HttpClient` calls (AGENTS.md §5 forbids this). There's no test comparing springdoc output with `docs/openapi.yaml` (Global Definition of Done).
- **Fix:**
  - Add a backend contract test that fetches `/v3/api-docs` (test profile) and diffs the paths and schemas you implemented against `docs/openapi.yaml`. Where your implementation deliberately differs (e.g. the `verify-otp` response), write `docs/proposals/002-openapi-phase1.md` and I'll update the spec.
  - Generate the TypeScript client from `docs/openapi.yaml` (`openapi-generator` `typescript-angular`, or `ng-openapi-gen`) into `core/api/generated/` via an `npm run api:gen` script that CI runs and checks for drift. Then delete the hand-written services.

### 6. [MEDIUM] Frontend crypto tests are flaky under load
- **Evidence:** with the backend suite running in parallel, 5/97 tests failed (`crypto.spec`, `key.service.spec`, `auth.store.spec`, `theme.spec`, `app.spec`). When idle, they pass 97/97 twice. CI runners are slower than your machine. The tests run real 600,000-iteration PBKDF2 and real key generation inside Vitest's default timeout.
- **Fix:** make the PBKDF2 iteration count injectable (600,000 in production, a small number in tests), give the genuinely slow WebCrypto tests explicit timeouts, and stop depending on wall-clock speed.
- **Test:** CI runs `ng test` with `--pool=threads --poolOptions.threads.maxThreads=1` and still passes. Show two consecutive green runs in PROGRESS.

---

## SHOULD-FIX (do now if quick, otherwise list in PROGRESS as carried)
- **Unverified users are stored as `LOCKED`.** `LOCKED` is the fraud-recovery state (doc 05), and `verifyOtp` sets `ACTIVE` unconditionally, so a future "resend OTP" would unlock fraud-locked accounts. Propose `PENDING_VERIFICATION` (`docs/proposals/003-user-status.md` plus a migration) and only allow `PENDING_VERIFICATION → ACTIVE`.
- **Server key rotation:** `rotateKey()` exists but nothing schedules it. PROGRESS says "daily rotation", but the spec says 90 days. Add a configurable `@Scheduled` rotation (default 90 days), or correct the claim.
- **Refresh cookie** has `Path=/`. Scope it to `Path=/api/v1/auth`. Deferring CSRF for `/auth/refresh` to Phase 8 is accepted **only if** you add a cheap `Origin`/`Referer` allow-list check on `/auth/refresh` now.
- **Offline PIN hash and salt are in `localStorage`.** Move them to Dexie `meta` with a failed-attempt counter and a 5-strike, 30-minute lockout (doc 08 §4).
- **OTP:** compare in constant time (`MessageDigest.isEqual`). Don't log phone numbers at INFO (`OtpService`, `AuthService`); log the user id instead.
- **PROGRESS accuracy:** the vector folder names, "daily rotation", and "E2E n.a." while the acceptance criterion requires a UI flow.

## What's good
Clean module structure, Modulith and ArchUnit (including the new `@PreAuthorize` rule). Correct refresh-token family revocation that survives rollback. Curve-point validation on device keys. P1363 signatures that interoperate with WebCrypto (verified live via the MC). Encrypted server keys with prod fail-fast. A thorough bug-hunt pass with test-first fixes.

## Resubmission checklist
- [ ] MUST-FIX 1–6, each with a failing `test:` commit before its `fix:` commit
- [ ] `make lint`, `make test` (random order), `make e2e` (smoke **+ auth flow**) green; frontend tests green twice with a single worker
- [ ] Light and dark screenshots of all 5 auth screens in PROGRESS; SHOULD-FIX dispositions listed
- [ ] `git push origin phase/1-auth-devices`, then "Phase 1 ready for architect re-review"
