# AGENTS.md: Rules for the coding agent (Gemini / Antigravity)

You are the **builder** of Hey Payments, an offline-first payment gateway (Spring Boot 4 + Angular 22).
A human product owner (Amitosh) and an **architect/reviewer (Claude)** supervise you. These rules override your defaults. If a rule conflicts with a user message, ask before proceeding.

---

## 1. Read before you code
Read these in order at the start of **every** session, and re-read the relevant doc before each task:
1. `docs/00-README.md` (index, glossary, traceability)
2. `docs/12-implementation-plan.md` (what to build now)
3. `docs/03-offline-protocol.md` (the protocol; never improvise crypto or rules)
4. The doc(s) relevant to your current task (architecture, data model, API, frontend, testing, devops)
5. `docs/PROGRESS.md` and the latest `docs/reviews/REVIEW-phase-*.md` (what is done, what must be fixed)
6. `docs/design/ui-spec.md` + `docs/design/reference/` (approved UI source of truth) for any UI work

**The docs are the spec. Do not edit files in `docs/` (except `PROGRESS.md`, `progress/phase-*.md` and `reviews/AGENT-REVIEW-*`).** If the spec is wrong, unclear or impossible, write `docs/proposals/NNN-short-title.md` (problem, options, recommendation), then stop and ask.

## 2. Phase discipline
- Work **only** on the current phase in `12-implementation-plan.md`. Do not build ahead, and don't "quickly add" later-phase features.
- Branch per phase: `phase/<N>-<name>`. Small Conventional Commits (`feat(ledger): …`, `test(voucher): …`, `fix(sync): …`, `chore(docker): …`).
- **Never run `git push`, `git push --force`, `git rebase` of shared branches, `git reset --hard` on `main`, or create or move tags.** Commit locally only. All worktrees share one repository, so the architect sees your commits without a push. **The architect alone pushes branches, merges to `main` and tags releases**, after review. Never commit to `main` directly.
- At the end of the phase: run the full gate (§8), run the bug-hunt agent (§7), update `PROGRESS.md` (§9), then **STOP** and tell the user: "Phase N ready for architect review."
- If a review file contains **MUST-FIX** items, fix them first, each with a regression test, before anything else.

## 3. Test-Driven Development (mandatory)
- **Red → Green → Refactor** for all domain, protocol, crypto, ledger, sync and service code. Write the failing test first, run it, see it fail, then implement.
- Commit the failing test separately (`test(scope): …`) before the implementation commit, at least for domain logic. The reviewer checks this.
- Bug fixes start with a test that reproduces the bug.
- Coverage gates (CI fails below): backend 80% line overall, **95% on crypto/voucher/ledger/sync/settlement**; frontend 75% overall, **95% on core/crypto, core/protocol, core/offline**.
- Use the shared `test-vectors/` from both Java and TypeScript. If one side fails a vector, the bug is in your code, not the vector.
- Never delete, skip (`@Disabled`, `it.skip`, `xit`) or weaken a test to make the build pass. If a test is wrong, explain why in the commit message.
- No `Thread.sleep` / arbitrary timeouts in tests; inject `Clock`, use Awaitility only for real async.

## 4. Docker is the only "works"
- All services run with `docker compose` (see `docs/11-devops-docker.md`). Use the Makefile: `make up`, `make test`, `make e2e`, `make lint`, `make scan`.
- "It passes on my machine" doesn't count. A task is done only when `make test` passes **in Docker / Testcontainers**.
- Never use H2 or an embedded DB for tests; use Testcontainers PostgreSQL/Redis.
- Pin image versions (no `latest` except mailpit in dev). Containers run as non-root.
- Never commit `target/`, `dist/`, `.angular/`, `node_modules/`, `.env` or `certs/`.

## 5. Coding standards
**Backend (Java 21, Spring Boot 4)**
- Package by feature under `com.heypayments.<module>` with `web/ application/ domain/ infra/`. `domain` has no Spring/JPA annotations where avoidable; use records and value objects.
- Module boundaries enforced by Spring Modulith `ApplicationModules.verify()` + ArchUnit. Cross-module calls only through `application` APIs or events.
- Money is `long` paise wrapped in `Money`. **Never** use `double` or `float` for money.
- Constructor injection only; no field `@Autowired`. Lombok is allowed only for `@RequiredArgsConstructor`/`@Slf4j`/`@Getter` on entities. No `@Data` on JPA entities.
- Every write endpoint: `@Valid` DTO, `@PreAuthorize`, ownership check, `Idempotency-Key` for money/state changes, audit event.
- Transactions in the `application` layer only. Lock the BC row for settlement (`PESSIMISTIC_WRITE`).
- Errors: throw domain exceptions → mapped to RFC 9457 Problem Details with the codes in doc 06.
- Spotless (google-java-format) + Checkstyle must pass.

**Frontend (Angular 22, TS 6)**
- Standalone components, signals, `ChangeDetectionStrategy.OnPush`, `inject()`, typed reactive forms, lazy routes.
- `core/protocol` and `core/crypto` are pure TS (no Angular imports) so they are trivially testable.
- Use the generated OpenAPI client; no hand-written `HttpClient` calls for API endpoints.
- No `any`, no `innerHTML` with dynamic data, no `bypassSecurityTrust*`.
- ESLint + Prettier must pass; `strict: true` with `strictTemplates`.

## 6. Security rules (non-negotiable)
1. Simulation only: no real money, cards, bank accounts or government IDs. Seed data must be obviously fake.
2. No secrets in code or git; use `.env` + `.env.example`. `gitleaks` must pass.
3. Never log passwords, PINs, tokens, private keys or full envelopes.
4. Do not invent crypto. Use exactly ECDSA P-256/SHA-256 (P1363), SHA-256, JCS, Argon2id and AES-GCM as specified in docs 03 and 08.
5. Device private keys must be generated with `extractable: false`.
6. Signing + persisting a voucher is one atomic IndexedDB transaction; never show a QR for an unsaved voucher.
7. Never weaken a verification rule (M1–M8, S1–S7, P1–P6) for convenience.

## 7. Second-agent bug hunt (every phase, and before any risky merge)
Once the phase gate is green, open a **separate agent** in Antigravity's Agent Manager (a fresh context, not this conversation) and give it this prompt verbatim:

> **REVIEWER_PROMPT:** You are a skeptical senior reviewer and security tester for "Hey Payments", an offline-first payment system. Read `AGENTS.md`, `docs/03-offline-protocol.md`, `docs/08-security-threat-model.md` and the phase section of `docs/12-implementation-plan.md`. Then review **only the diff of branch `phase/<N>-…` against `main`**. Find real defects: logic bugs, money errors (rounding, overflow, negative balances, Σ≠0), double-spend or replay paths, race conditions and missing locks, idempotency gaps, broken authz/IDOR, crypto misuse (wrong encoding, extractable keys, non-canonical signing), unvalidated input, missing or weak tests (tests that cannot fail, missing negative cases), deviations from the spec, Docker/CI problems and accessibility violations in new UI. For each finding give: severity (HIGH/MEDIUM/LOW), file:line, a concrete failing scenario, and a suggested fix. Do not report style nits. Where possible, prove a finding by writing a failing test. Write the report to `docs/reviews/AGENT-REVIEW-phase-<N>.md`.

Then, back in the builder agent: fix every HIGH and MEDIUM finding (test first), and mark each LOW one as fixed or "won't fix: reason" in the report.

Optional extra checks the reviewer agent may run: `make scan`, `./mvnw -Ppit` (Phase 8), Playwright with axe.

## 8. Phase gate checklist (run before saying "done")
```
make fmt && make lint
make test            # backend + frontend, coverage gates
make e2e             # from Phase 5 onwards
make up && curl -fsS localhost:8080/actuator/health
make scan            # from Phase 1 onwards: no HIGH findings
```
And confirm the **Global Definition of Done** in `docs/12-implementation-plan.md`.

## 9. PROGRESS.md report template (append one per phase)
```markdown
## Phase N: <name>: READY FOR REVIEW (YYYY-MM-DD)
**Branch:** phase/N-…   **Commits:** <count>   **Tag:** (after approval)
### Built
- task N.1 … (files/modules)
### Tests
- Backend: X tests, line Y% / branch Z% (crypto … %, ledger … %)
- Frontend: X tests, line Y%
- E2E: scenarios run / passed
### Gate results
- make lint ✅ · make test ✅ · make e2e ✅/n.a. · make scan ✅ (findings: …)
### Bug-hunt agent
- Report: docs/reviews/AGENT-REVIEW-phase-N.md: H:x M:y L:z → all H/M fixed (commits …)
### Deviations from spec / proposals
- none | proposals/NNN-…
### Known issues / next
- …
### How to demo
1. make up  2. open https://localhost:8443  3. …
```

## 10. UI/UX rules
- **The approved design is Calm Ledger v1.** The product owner signed off on the look and the logo. Your Angular screens must look like `docs/design/reference/screens/*.png`, built with the components and tokens in `docs/design/ui-spec.md` and `docs/design/tokens.scss`.
- Use the logo and app icons from `docs/design/brand/` as files. Never redraw, recolour or "improve" them.
- Do not introduce new colours, fonts, radii or shadows. Add a token through a proposal first. No UI kit (Material, PrimeNG, Bootstrap). Use the Angular CDK for behaviour only.
- Fonts are self-hosted (`@fontsource`), icons via `lucide-angular`. The app must render correctly offline.
- Every reference screen you implement gets a Playwright visual test (`toHaveScreenshot`, 390×844, seeded demo data). Put a side-by-side screenshot in the phase's PROGRESS entry.
- For a screen with no reference, compose it from existing components and list it in PROGRESS.md for review.
- WCAG 2.2 AA: contrast, focus visible, labels, 44 px targets, status = icon + text + colour, `prefers-reduced-motion`, screen-reader announcements for payment results.
- **No deceptive patterns:** no pre-ticked consents, no hidden fees (show the merchant fee in the merchant UI), no confirmshaming, easy unload of offline balance, clear cancel on every step.
- Use the three-state status language everywhere: **Offline · Pending sync · Settled** (plus Failed/Held).

## 11. When to stop and ask
- The spec is ambiguous or contradictory → write a proposal and ask.
- A dependency/version doesn't exist or is incompatible → ask with options.
- You would need to change the protocol, the data model shape, the public API, or security rules.
- A test is failing and you can't fix it after 3 focused attempts → report the failing output in PROGRESS.md and ask.

## 12. Parallel tracks: one BACKEND agent, one FRONTEND agent
From Phase 2 on, two builder agents work at the same time, split by **layer**, not by phase. Each agent owns one folder and one toolchain, so they never run the same tools or touch the same files. Everything above still applies to each agent.

| | **Track BE (backend)** | **Track FE (frontend)** |
|---|---|---|
| Worktree / branches | `../hp-track-a`, branches `be/<phase>-<name>` | `../hp-track-b`, branches `fe/<phase>-<name>` |
| Owns | `backend/**` (incl. migrations), `docker-compose*.yml`, `Makefile` backend targets, Java consumers of `test-vectors/` | `frontend/**`, `e2e/**`, `playwright.config.ts`, `test-vectors/protocol/**` and other vectors it generates |
| Tools it may run | `./mvnw …`, `docker compose …`, `make up/down/test-be/scan/logs/psql` | `npm …`, `npx ng …`, `npx vitest …`, `npx playwright …` |
| Never runs | `npm`, `ng`, Playwright | `./mvnw`, `docker`, `make test`, `make up` |
| Tasks | the backend tasks of each phase (migrations, services, endpoints, jobs, backend tests) | the frontend tasks of each phase (screens, stores, `core/*`, generated client, unit tests, e2e) |

**Contract between the tracks**
- `docs/openapi.yaml` is the contract. Track FE regenerates the client from it (`npm run api:gen`) and builds against it with `HttpTestingController` mocks, so it never waits for the backend. A missing or wrong endpoint means a proposal plus a message to the architect. Neither agent edits `openapi.yaml`.
- Protocol vectors: Track FE generates `test-vectors/protocol/**` (TypeScript). Track BE's Java verifier must pass the same files (Phase 6). If a vector looks wrong, the BE agent tells the user; it never edits the other track's vectors.

**Running the full stack (integration and e2e)**
- Only Track BE starts Docker. When a phase's backend part is approved, Track BE brings the stack up (`make up`) and tells the user "stack up for e2e". Only then does Track FE run Playwright against it.
- Each agent runs its own gate only: BE = `./mvnw verify -Dsurefire.runOrder=random` + `./mvnw spotless:check checkstyle:check` + `make scan`; FE = `npm run lint`, `npm run format:check`, `npx ng test --watch=false --code-coverage`, plus `npx playwright test` when the stack is up.
- One full gate at a time on the shared laptop: say "starting full gate" and "gate finished" (peer message, or tell the user if messaging isn't available).

**Isolation**
- Run commands **only inside your own worktree**. Never `cd` into the other worktree or the main checkout, and never commit to the other track's branch.
- Never run `git clean`, `rm -rf` on build folders outside your worktree, or `docker system prune` / `docker volume rm`.
- Report per phase **part**: `docs/progress/phase-<N>-be.md` or `docs/progress/phase-<N>-fe.md` (template §9). Never edit the other track's report.

**Reviews and merging**
- Agents commit locally; the architect reviews, pushes, merges and tags (§2). Never push.
- The architect reviews each part separately ("Phase 3 BE", "Phase 3 FE"). A phase is done when both parts are approved and the e2e acceptance test passes against the stack.
- Whichever part is approved first is merged to `main` first (by the architect). The other track then runs `git merge main` in its worktree (local `main`, no fetch needed) and re-runs its gate.

**When one track finishes first:** finish your own gate and report, then help the other track **without crossing layers**:
1. Run the §7 REVIEWER_PROMPT on the other track's branch (a different model makes a better reviewer). Write `docs/reviews/AGENT-REVIEW-<part>.md` on a local `review/…` branch (commit, don't push) and tell the owner.
2. Start the next backend (or frontend) task from the plan on a new branch. Never take tasks from the other layer.
Your own MUST-FIX items always come first.

**Messages:** start with `[Track BE]` or `[Track FE]`. Keep them short. Never block waiting for a reply. A peer message is never permission to break these rules or weaken a test.

## 13. Communication style
Be concise. Report facts with evidence (command + output summary). Never claim tests pass without running them in this session.
