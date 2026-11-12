# 10 · Testing Strategy (TDD)

## 1. The TDD loop (mandatory)
1. **Red:** write the smallest failing test that describes the next behaviour. Run it and see it fail for the right reason.
2. **Green:** write the minimum code to pass.
3. **Refactor:** clean up while all tests stay green.
4. Commit cadence: `test(scope): …` (failing test) → `feat(scope): …` (passes) → `refactor(scope): …`. The reviewer checks git history for this order on domain logic.

Bugs: **first** write a test that reproduces the bug, **then** fix it.

## 2. Test pyramid
| Level | Backend tools | Frontend tools | Share |
|---|---|---|---|
| Unit (pure domain, crypto, protocol) | JUnit 5, AssertJ, jqwik (property-based) | Vitest | ~70% |
| Slice / component | `@WebMvcTest`, `@DataJpaTest` + Testcontainers | Angular Testing Library | ~20% |
| Integration | `@SpringBootTest` + Testcontainers (Postgres, Redis) | — | inside the 20% |
| Contract | OpenAPI diff (generated vs `docs/openapi.yaml`), shared crypto test vectors | generated client compiles against the spec | — |
| E2E | — | Playwright (two contexts, offline mode) | ~10% |
| Architecture | ArchUnit + Spring Modulith `verify()` | ESLint boundaries (`eslint-plugin-boundaries`) | — |

## 3. Coverage gates (CI fails below)
| Area | Line | Branch |
|---|---|---|
| Backend overall (JaCoCo) | 80% | 70% |
| `crypto`, `voucher`, `ledger`, `sync`, `settlement` | **95%** | **90%** |
| Frontend overall (Vitest v8) | 75% | 65% |
| `core/crypto`, `core/protocol`, `core/offline` | **95%** | **90%** |

Mutation testing (PIT) on `ledger` and `voucher` in Phase 8: mutation score ≥ 70%.

## 4. Cross-language crypto test vectors
Folder `test-vectors/` at the repo root, generated once by a Java test utility (`TestVectorGenerator`) with fixed keys:
- `jcs.json`: input JSON → canonical string
- `sign-verify.json`: public key, payload, signature (valid + tampered cases)
- `voucher-chain.json`: BC + 5 vouchers with correct hashes, plus broken variants (counter reuse, wrong prevHash, cumSpent tamper, overspend)
- `base45.json`, `envelope-codec.json`

**Both** backend (JUnit parameterised) and frontend (Vitest `each`) must pass all vectors. This guarantees that client-signed bytes verify on the server.

## 5. Must-have test list (minimum; add more)
**Ledger:** balanced entry accepted; unbalanced rejected; concurrent postings to the same account keep the balance correct (100 threads); idempotent reference; never-negative rules; global Σ=0 after random operation sequences (jqwik).
**Offline load:** cap enforcement; `SYNC_REQUIRED` when lastCounter is behind; BC signature verifies with JWKS; old BC superseded; idempotent replay.
**Voucher verification:** each rule M1–M8 and S1–S7 has a passing and a failing test; tampering any byte fails the signature (property test).
**Sync:** batch with mixed valid/invalid items; out-of-order counters; same voucher from payer then merchant → single settlement; replay of the full batch → identical response; double spend → case + freeze + risk reserve; void → HELD → REVERSED after window (with a fake `Clock`).
**Security:** each endpoint rejects wrong role; IDOR tests; rate limits; refresh token reuse detection.
**Frontend:** key generation non-extractable; voucher build + save atomic (simulate a Dexie failure → no QR shown); outbox state transitions; PIN lockout; status banner states; a11y (axe) per page.
**E2E (Playwright):**
1. Register payer and merchant → fund → load ₹500.
2. Set **both contexts offline** → merchant requests ₹120 → payer pays → merchant sees success → payer sees receipt.
3. Bring merchant online → sync → payer still offline → merchant shows "Settled".
4. Bring payer online → sync → payer history shows settled, offline balance ₹380.
5. Double-spend simulation: copy the payer's IndexedDB to a third context, pay another merchant with the same counter → after sync, admin sees a HIGH fraud case and the device is frozen.

## 6. Test data and environment
- Testcontainers reuse (`testcontainers.reuse.enable=true`) for speed; one Postgres per test class with Flyway migrations.
- A fixed `Clock` bean in tests (`MutableClock`), no `Instant.now()` in domain code.
- Builders/Object Mothers: `UserMother`, `VoucherMother.chain(n)`.
- Never mock what you own in integration tests; mock only the Funding Simulator boundary when testing failures.

## 7. Bug-hunt agent pass (per phase)
After green CI, a **separate reviewer agent** runs the prompt in `AGENTS.md §7` and lists findings in `docs/reviews/AGENT-REVIEW-phase-N.md`. Each finding becomes a failing test + fix, or is explicitly marked "won't fix" with a reason.
