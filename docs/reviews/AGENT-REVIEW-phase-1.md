# Agent Review: Phase 1 (Auth, Users, Devices)

**Reviewer:** Skeptical Senior Reviewer & Security Tester (Automated Bug-Hunt Agent)  
**Date:** 2026-11-13  
**Target Branch:** `phase/1-auth-devices` compared against `main`  
**Scope:** Backend (Spring Boot 4, Java 21), Frontend (Angular 22, TypeScript 6), Cryptography & Test Vectors, Security & Auth Architecture  
**Status:** **ALL FINDINGS RESOLVED (4/4 HIGH, 5/5 MEDIUM, 4/5 LOW FIXED; 1/5 LOW SCHEDULED FOR PHASE 8)**

---

## Executive Summary & Findings Tally

| Severity | Total | Resolved | Won't Fix / Scheduled | Status |
|---|---|---|---|---|
| **HIGH** | 4 | 4 | 0 | **100% RESOLVED** |
| **MEDIUM** | 5 | 5 | 0 | **100% RESOLVED** |
| **LOW** | 5 | 4 | 1 | **100% RESOLVED / ACCOUNTED FOR** |
| **TOTAL** | 14 | 13 | 1 | **ALL REMEDIATED** |

---

## Remediation Summary Table

| ID | Title | Test Commit | Fix Commit | Status |
|---|---|---|---|---|
| **HIGH-1** | Transaction Rollback on Token Reuse Bypasses Family Revocation | `df632ee` | `50bf5aa` | **FIXED** |
| **HIGH-2** | Frontend Device Registration Desync & Lockout on Re-login | `08e7de5` | `90e63bc` | **FIXED** |
| **HIGH-3** | Set-PIN URL and Payload Schema Mismatch Blocks PIN Configuration | `787363d` | `af56aea` | **FIXED** |
| **HIGH-4** | Race Condition in `authInterceptor` on Concurrent 401s | `aa573a5` | `cf3404b` | **FIXED** |
| **MEDIUM-1** | RFC 8785 JCS Key Comparison Diverges on Characters > U+FFFF | `f08ca33` | `880173f` | **FIXED** |
| **MEDIUM-2** | Primary Key UUID Generation Uses Random UUIDv4 instead of UUIDv7 | `ce377f8` | `2c1d6fc` | **FIXED** |
| **MEDIUM-3** | Missing Production Fail-Fast Configuration for `HP_MASTER_KEY` | `2a34cd8` | `9fe977d` | **FIXED** |
| **MEDIUM-4** | Missing EC Curve Point Validation on Device Public Key Registration | `dcfc8ff` | `9bca0b9` | **FIXED** |
| **MEDIUM-5** | Unbounded Memory Growth and Spoofable IP in Rate Limiting & OTP | `f2a9041` | `ddb3b93` | **FIXED** |
| **LOW-1** | Hardcoded Insecure Cookie Flag in `AuthController` | — | `d60435a` | **FIXED** |
| **LOW-2** | Complete Disable of CSRF for Cookie-Based `/auth/refresh` | — | — | **WON'T FIX (PHASE 8)** |
| **LOW-3** | Missing Merchant Certificate Retrieval in `GET /devices/me` | `08e7de5` | `90e63bc` | **FIXED** |
| **LOW-4** | Missing ArchUnit Rule and Weak Authorization on `POST /devices` | `65c75aa` | `d60435a` | **FIXED** |
| **LOW-5** | WAI-ARIA Accessibility Violation in Register Role Selector | — | `d60435a` | **FIXED** |

---

## Detailed Findings and Resolutions

### [HIGH-1] Transaction Rollback on Refresh Token Reuse Bypasses Family Revocation
- **Status:** **FIXED** (Test: `df632ee`, Fix: `50bf5aa`)
- **File:** `backend/src/main/java/com/heypayments/auth/application/AuthService.java:131-157` and `backend/src/main/java/com/heypayments/auth/application/RefreshTokenService.java:46-55`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 1.2, `docs/08-security-threat-model.md` §3.
- **Failing Scenario:** When refresh token reuse occurred, `RefreshTokenService` revoked the token family and threw `AuthTokenException` (which extends `RuntimeException`). Because `AuthService.refresh()` was annotated with `@Transactional`, Spring rolled back the database update, un-revoking the token family and allowing compromised tokens to continue minting new access tokens.
- **Resolution:** Added failing controller integration test `AuthControllerTest.shouldRevokeEntireTokenFamilyOnReuseViaController` executing against PostgreSQL Testcontainer. Configured `noRollbackFor = AuthTokenException.class` on `AuthService.refresh()` to commit token family revocation even when throwing.

---

### [HIGH-2] Frontend Device Registration Desynchronization & Permanent Lockout on Re-login
- **Status:** **FIXED** (Test: `08e7de5`, Fix: `90e63bc`)
- **File:** `frontend/src/app/core/auth/auth.store.ts` and `frontend/src/app/core/crypto/key.service.ts`
- **Spec Reference:** `docs/12-implementation-plan.md` Task 1.6, `docs/03-offline-protocol.md` §3.2.
- **Failing Scenario:** `KeyService.getOrCreateDeviceKey()` created a temporary UUID for `deviceId` in Dexie `db.keys`. The server assigned a real UUIDv7 `deviceId` upon registration, but `AuthStore` never updated Dexie. For merchants, `merchantCertificate` was not saved to `db.certs`. On `logout()`, `clearKeys()` erased `db.keys`. On re-login, the client had no active device and attempts to re-register failed with `409 DEVICE_ALREADY_ACTIVE`.
- **Resolution:** Added `KeyService.updateDeviceId()`. Updated `AuthStore.registerCurrentDevice()` to update local `db.keys` with the server's registered `deviceId` and persist `merchantCertificate` to Dexie `db.certs`. Updated `AuthStore.login()` to query `api.getMyDevice()` and restore active device state. Preserved device keys on standard logout (device keys are only destroyed when a device is explicitly closed via `DELETE /api/v1/devices/{id}`).

---

### [HIGH-3] Set-PIN URL and Payload Schema Mismatch Blocks PIN Configuration
- **Status:** **FIXED** (Test: `787363d`, Fix: `af56aea`)
- **File:** `frontend/src/app/core/api/auth-api.service.ts`, `frontend/src/app/features/auth/set-pin/set-pin.component.ts`, `frontend/src/app/core/crypto/crypto.ts`
- **Spec Reference:** `docs/06-api-spec.md` line 39, `docs/08-security-threat-model.md` §4.
- **Failing Scenario:** `AuthApiService.setPin()` sent `PUT /api/v1/users/me/pin` instead of `/api/v1/auth/pin`. The frontend DTO omitted the mandatory `password` field, causing HTTP 400 validation failures. Furthermore, local salted PBKDF2 hash (600,000 iterations) required for offline PIN gating was never derived or stored.
- **Resolution:** Added unit and component tests verifying URL, payload, and PBKDF2 offline derivation. Updated `AuthApiService` to call `PUT /api/v1/auth/pin` with `{ pin, password }`. Added `password` field and validation to `SetPinComponent`. Added `derivePinHash()` (PBKDF2 SHA-256, 600,000 iterations) and saved salt/hash to Dexie `db.keys` in `AuthStore`.

---

### [HIGH-4] Race Condition in `authInterceptor` on Concurrent 401 Responses Triggers Token Family Revocation
- **Status:** **FIXED** (Test: `aa573a5`, Fix: `cf3404b`)
- **File:** `frontend/src/app/core/auth/auth.interceptor.ts`
- **Spec Reference:** `docs/08-security-threat-model.md` §3.
- **Failing Scenario:** Concurrent requests hitting 401 simultaneously triggered multiple parallel calls to `/api/v1/auth/refresh`. The first call rotated the cookie, causing subsequent parallel calls to present the old cookie, triggering token reuse detection and revoking the user's session.
- **Resolution:** Implemented an in-flight refresh mutex using RxJS `shareReplay(1)` in `auth.interceptor.ts` with `finalize` cleanup. All concurrent 401s share the single in-flight refresh request and retry on the newly minted session.

---

### [MEDIUM-1] RFC 8785 JCS Key Comparison Diverges on Characters Above U+FFFF
- **Status:** **FIXED** (Test: `f08ca33`, Fix: `880173f`)
- **File:** `frontend/src/app/core/crypto/jcs.ts`
- **Spec Reference:** `docs/03-offline-protocol.md` §2, RFC 8785 §3.2.3.
- **Failing Scenario:** `compareKeys()` converted surrogate pairs into 32-bit Unicode code points via `codePointAt()`. RFC 8785 Section 3.2.3 explicitly requires sorting property names by UTF-16 code units (where high surrogates `0xD800..0xDBFF` sort before `\uFFFF`). This caused signature mismatches on envelopes containing non-BMP property names between TypeScript and Java.
- **Resolution:** Added test vector in `crypto.spec.ts` asserting exact RFC 8785 ordering. Replaced `compareKeys` with standard UTF-16 code unit comparison: `a < b ? -1 : a > b ? 1 : 0`.

---

### [MEDIUM-2] Primary Key UUID Generation Uses Random UUIDv4 instead of UUIDv7
- **Status:** **FIXED** (Test: `ce377f8`, Fix: `2c1d6fc`)
- **File:** `backend/src/main/java/com/heypayments/auth/application/AuthService.java`, `DeviceService.java`, `MerchantService.java`, `RefreshTokenService.java`
- **Spec Reference:** `docs/03-offline-protocol.md` §3, `docs/05-data-model.md` §0.
- **Failing Scenario:** Entities and refresh tokens were generated using `UUID.randomUUID()` (UUIDv4), degrading index clustering and violating the requirement that all IDs are UUIDv7 strings.
- **Resolution:** Replaced all `UUID.randomUUID()` calls with `UuidV7Generator.generate()` across auth, device, merchant, and refresh token services. Added assertions verifying UUID version 7.

---

### [MEDIUM-3] Missing Production Fail-Fast Configuration for `HP_MASTER_KEY`
- **Status:** **FIXED** (Test: `2a34cd8`, Fix: `9fe977d`)
- **File:** `backend/src/main/resources/application-prod.yml`, `docker-compose.yml`, `scripts/test-port-exposure.sh`
- **Spec Reference:** `AGENTS.md` §6, `docs/08-security-threat-model.md` §4.
- **Failing Scenario:** `CryptoConfig` defaulted to `dev-master-key-seed-32-bytes-long!` when no key was configured. In production, missing `${HP_MASTER_KEY}` would start silently and encrypt all merchant device certificates with a publicly known development key.
- **Resolution:** Added unit test `CryptoConfigTest` verifying that `application-prod.yml` mandates `hp.master-key: ${HP_MASTER_KEY}` without default fallback. Wired mandatory `HP_MASTER_KEY: ${HP_MASTER_KEY:?HP_MASTER_KEY must be set}` into `docker-compose.yml`. Updated `scripts/test-port-exposure.sh` to enforce prod compose resolution.

---

### [MEDIUM-4] Missing EC Curve Point Validation on Device Public Key Registration
- **Status:** **FIXED** (Test: `dcfc8ff`, Fix: `9bca0b9`)
- **File:** `backend/src/main/java/com/heypayments/crypto/domain/CryptoService.java` and `backend/src/main/java/com/heypayments/device/application/DeviceService.java`
- **Spec Reference:** `docs/08-security-threat-model.md` §4, `docs/03-offline-protocol.md` §2.
- **Failing Scenario:** `CryptoService.fromJwk()` did not validate coordinate byte lengths or verify that coordinates satisfy the Weierstrass curve equation $y^2 \equiv x^3 - 3x + b \pmod p$. Malformed or invalid curve points resulted in unhandled 500 errors or accepted invalid keys.
- **Resolution:** Enforced 32-byte coordinate length, range checking $[0, p-1]$, and Weierstrass curve equation validation in `CryptoService.fromJwk()`. Handled validation failures in `DeviceService` to return RFC 9457 `VALIDATION_FAILED` (400).

---

### [MEDIUM-5] Unbounded Memory Growth and Spoofable IP in Rate Limiting & OTP Services
- **Status:** **FIXED** (Test: `f2a9041`, Fix: `ddb3b93`)
- **File:** `backend/src/main/java/com/heypayments/auth/infra/security/RateLimitingFilter.java` and `backend/src/main/java/com/heypayments/auth/application/OtpService.java`
- **Spec Reference:** `docs/08-security-threat-model.md` §3, §6.
- **Failing Scenario:** In-memory maps were unbounded, causing heap leaks. Rate limiting read spoofable `X-Forwarded-For` headers directly. OTP codes had no brute-force limit and could be guessed within the 5-minute window.
- **Resolution:** Replaced unbounded maps with bounded Caffeine caches (`expireAfterWrite`/`expireAfterAccess` 15 min, max 10,000 entries). Limited failed OTP attempts to 5 before automatic invalidation. Switched client IP resolution to `request.getRemoteAddr()`.

---

### [LOW-1] Hardcoded Insecure Cookie Flag in `AuthController`
- **Status:** **FIXED** (Fix: `d60435a`)
- **File:** `backend/src/main/java/com/heypayments/auth/web/AuthController.java:99`
- **Resolution:** Set `.secure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")))` on refresh token cookies.

---

### [LOW-2] Complete Disable of CSRF for Cookie-Based `/auth/refresh`
- **Status:** **WON'T FIX: Scheduled for Phase 8**
- **File:** `backend/src/main/java/com/heypayments/auth/infra/config/SecurityConfig.java:48`
- **Reason:** Phase 1 uses Bearer access tokens and `SameSite=Strict` HttpOnly cookies for refresh tokens. Full double-submit CSRF cookie token architecture is formally scheduled for Phase 8 security hardening per `docs/12-implementation-plan.md` Task 8.1 and `docs/08-security-threat-model.md` §5.

---

### [LOW-3] Missing Merchant Certificate Retrieval in `GET /devices/me`
- **Status:** **FIXED** (Test: `08e7de5`, Fix: `90e63bc`)
- **File:** `backend/src/main/java/com/heypayments/device/application/DeviceService.java`
- **Resolution:** Updated `getActiveDevice()` to look up merchant profile and populate `merchantCertificate` when the active device kind is `MERCHANT`.

---

### [LOW-4] Missing ArchUnit Rule and Weak Authorization on `POST /devices`
- **Status:** **FIXED** (Test: `65c75aa`, Fix: `d60435a`)
- **File:** `backend/src/test/java/com/heypayments/ArchitectureRulesTest.java` and `backend/src/main/java/com/heypayments/device/web/DeviceController.java`
- **Resolution:** Added ArchUnit rule `controllerMethodsMustBeSecured` enforcing `@PreAuthorize` on all public methods in `@RestController` classes. Restricted `POST /api/v1/devices` to `@PreAuthorize("hasAnyRole('PAYER', 'MERCHANT')")` and added `@PreAuthorize("permitAll()")` to public endpoints.

---

### [LOW-5] WAI-ARIA Accessibility Violation in Register Role Selector
- **Status:** **FIXED** (Fix: `d60435a`)
- **File:** `frontend/src/app/features/auth/register/register.component.html`
- **Resolution:** Added `role="radio"` and `[attr.aria-checked]` to registration role buttons.

---

## Conclusion
All 4 HIGH and 5 MEDIUM findings have been resolved test-first with dedicated regression tests. 4 of 5 LOW findings have been resolved, and the remaining finding (LOW-2 CSRF) has been scheduled for Phase 8 per the roadmap. Phase 1 is fully compliant with specifications and ready for architect review.
