# 08 · Security and Threat Model

## 0. Hard rules (non-negotiable)
1. **No real money, real cards, real bank accounts or real Aadhaar/PAN.** Simulation only. Seed data uses obviously fake values.
2. **No secrets in git.** Use `.env` (git-ignored) and `.env.example` with placeholders. CI fails if `gitleaks` finds a secret.
3. **Money = `long` paise** (Java) / `number` integers ≤ 2^53 (TS). No `double`, `float` or `BigDecimal` division without explicit rounding.
4. **Every state-changing endpoint**: authenticated, authorised by role *and* ownership, validated, idempotent, audited.
5. **Never log** passwords, PINs, tokens, private keys or full envelopes at INFO. Log voucherId/hash only.

## 1. Assets
| Asset | Why it matters |
|---|---|
| Server signing private key | Forging BCs = minting money |
| Device private keys | Forging vouchers from a victim's balance |
| Ledger integrity | Money correctness |
| User credentials / PIN | Account takeover |
| Escrow + risk reserve balances | Loss exposure |
| Audit log | Non-repudiation |

## 2. Trust boundaries
Browser ↔ nginx (TLS) · nginx ↔ backend (internal network) · backend ↔ Postgres/Redis (internal network, credentials) · **Payer device ↔ Merchant device (untrusted, offline QR)**.

## 3. STRIDE
| Threat | Example | Mitigation | Test |
|---|---|---|---|
| **S**poofing | Fake merchant QR to steal a payment | MC signed by the server; payer verifies P1–P2 and shows the verified merchant name with a "Verified" badge | `PaymentRequestVerifierTest` |
| Spoofing | Stolen JWT | 15-min access tokens, refresh rotation with reuse detection, device binding for money endpoints | `RefreshTokenRotationIT` |
| **T**ampering | Edit amount in a voucher | ECDSA signature over JCS bytes; any change fails M4/S-crypto | `VoucherTamperTest` (property-based) |
| Tampering | Manipulate `cumSpent` | S5 consistency + escrow S6 | `CumSpentTamperIT` |
| **R**epudiation | Payer denies paying | Voucher signed by a non-extractable device key + PIN confirmation; receipt signed by merchant; audit log | `AuditLogIT` |
| **I**nformation disclosure | QR contains PII | Only merchant display name and opaque ids in QR; no phone/email | `QrPayloadPrivacyTest` |
| Information disclosure | IDOR on `/transactions/{id}` | Ownership checks in the service layer; random UUIDs | `TransactionAccessIT` |
| **D**enial of service | Flood `/sync` with junk | Bucket4j rate limits, 100 items/batch max, 64 KB body max, early schema reject | `SyncRateLimitIT` |
| **E**levation of privilege | Payer calls admin API | `@PreAuthorize` per endpoint + deny-by-default `SecurityFilterChain`; ArchUnit rule: every controller method is annotated | `SecurityRulesArchTest` |
| Double spend | Cloned device | Caps, counters, hash chain, unique index, freeze, risk reserve | `DoubleSpendIT` |
| Replay | Re-upload the same voucher | Idempotent by voucherId/hash | `SyncIdempotencyIT` |
| Key compromise (server) | Leaked signing key | Key encrypted at rest with `HP_MASTER_KEY`; `kid` rotation; emergency revoke of `kid` invalidates BCs → forced resync | `KeyRotationIT` |

## 4. Cryptography
| Use | Algorithm |
|---|---|
| Signatures | ECDSA P-256 + SHA-256, P1363 encoding |
| Hashing (chain, ids) | SHA-256 |
| Password + PIN | Argon2id (Spring Security `Argon2PasswordEncoder`, m=19 MiB, t=2, p=1) |
| JWT | ES256 with a separate key from the BC signing key |
| Server private key at rest | AES-256-GCM with a key from env `HP_MASTER_KEY` |
| Transport | TLS 1.2+ (nginx terminates; self-signed via mkcert in dev for the PWA camera/service worker) |

Client key storage: `crypto.subtle.generateKey({name:'ECDSA', namedCurve:'P-256'}, false, ['sign','verify'])`; the `CryptoKey` object is persisted in IndexedDB (structured clone keeps it non-extractable). The payment PIN gates **use** in the UI; it is verified locally against a salted PBKDF2 hash (600k iterations) so it works offline, with 5 wrong attempts → 30-minute lockout.

## 5. Web security
- Spring Security: stateless, CSRF disabled for Bearer APIs **but** enabled (double-submit cookie) for `/auth/refresh` because it uses a cookie; `SameSite=Strict; Secure; HttpOnly` cookies.
- CORS: allow only the configured frontend origin.
- Headers (nginx): `Content-Security-Policy: default-src 'self'; img-src 'self' data: blob:; media-src 'self' blob:; connect-src 'self'; frame-ancestors 'none'`, HSTS, `X-Content-Type-Options`, `Referrer-Policy: no-referrer`, `Permissions-Policy: camera=(self)`.
- Angular: no `innerHTML` with untrusted data, no `bypassSecurityTrust*`; strict template checks.
- Validation: Jakarta Bean Validation on all DTOs; max lengths everywhere; envelope schema validation before crypto.
- Dependency scanning: OWASP Dependency-Check (Maven) + `npm audit --audit-level=high` in CI; Trivy scan of images.

## 6. OWASP ASVS L2 checklist (track in PROGRESS.md)
- [ ] V2 Authentication: password policy, rate limits, lockout, OTP expiry 5 min
- [ ] V3 Session: rotation, revoke on logout, absolute lifetime
- [ ] V4 Access control: deny by default, ownership checks, admin audit
- [ ] V5 Validation/encoding: DTO validation, output encoding, problem details without stack traces
- [ ] V6 Cryptography: approved algorithms, no custom crypto, key rotation
- [ ] V7 Logging: no secrets, correlation ids, audit for money/security events
- [ ] V8 Data protection: minimal PII, encrypted key material
- [ ] V9 Communications: TLS, HSTS
- [ ] V11 Business logic: limits, idempotency, sequential step enforcement (sync before load)
- [ ] V13 API: rate limits, content-type enforcement, body size limits
- [ ] V14 Configuration: hardened containers (non-root, read-only FS), dependency scanning

## 7. Abuse-case limits (configurable via `/admin/limits`)
See [01-PRD §6](01-PRD.md#6-business-rules). The risk exposure per cloned device is bounded by `perDeviceOfflineLimit × clones` until the first sync of any clone voucher reveals the double spend and the CRL propagates (≤ 48 h because of the CRL staleness rule).
