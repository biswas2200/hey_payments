# Review: Phase 0 (interim, 2026-11-12 19:25 IST)

**Verdict:** IN PROGRESS. Not yet submitted. Fix the items below before declaring Phase 0 done.
**Checked:** branch `phase/0-foundation` (26 commits on top of `main`), static read of the code. I did not run Docker so I wouldn't interfere with your running containers. I also read your session status.

## What I see you doing
- Phase 0 tasks 0.1–0.9 are largely built: shared module (Money, UUIDv7, Problem Details, RequestIdFilter), Flyway V1, Testcontainers base, Modulith test, Dockerfiles, compose, nginx, CI, Makefile, Angular shell with ConnectivityService. TDD commit order is visible. Good work.
- You ran `make fmt && make lint && make test`, then started the **bug-hunt reviewer subagent** (AGENTS.md §7).
- **Since then you have polled `manage_subagents list` 100+ times in a row.** That burns quota without making progress, and `docs/reviews/AGENT-REVIEW-phase-0.md` still doesn't exist.

## Do this next, in order
1. **Stop polling.** Check the reviewer subagent once. If it has produced no report after one more check, cancel it and start it again with the REVIEWER_PROMPT, limited to `git diff main..phase/0-foundation -- backend frontend docker-compose.yml Makefile .github`. Wait on it with long intervals (≥ 2 min) or a blocking wait, never a tight loop. If it fails twice, write the report yourself and note "subagent unavailable" in PROGRESS.md.
2. Fix the MUST-FIX items below, test first.
3. Update `docs/PROGRESS.md` with the AGENTS.md §9 template (it still says "scaffold started"), then stop and say "Phase 0 ready for architect review".

## MUST-FIX
1. **Actuator exposure** (`backend/src/main/resources/application.yml`, `SecurityConfig.java:32`): `show-details: always` combined with `permitAll` on `/actuator/**` leaks DB and Redis details publicly. Use `show-details: when-authorized`, enable `management.endpoint.health.probes.enabled=true`, and permit only `/actuator/health/**` and `/actuator/info`. Add a `@WebMvcTest`/integration test showing that `/actuator/env` returns 401 and that health has no `details` when anonymous.
2. **Coverage gate missing** (`backend/pom.xml`): JaCoCo produces a report but has no `check` goal. Add the `check` execution in `verify` with the thresholds from doc 10 §3 (bundle 80% line / 70% branch; packages `crypto`, `voucher`, `ledger`, `sync`, `settlement` 95/90 once they exist). Add the same for the frontend (Vitest coverage thresholds 75/65). CI must fail below these.
3. **Swagger permitted in all profiles** (`SecurityConfig.java:32`): `/swagger-ui/**` and `/v3/api-docs/**` must only be open under the `dev` profile (doc 11 §6).
4. **Secrets defaults** (`application.yml`): `DB_PASSWORD:change-me` fallback is fine for `dev`, but the `prod` profile must fail at startup if it's unset. Add `application-prod.yml` with no defaults.

## SHOULD-FIX (can roll into Phase 1)
- `Money` rejects negatives. That's correct for amounts, but ledger journal lines are signed (doc 05). Plan a separate signed type for lines in Phase 2 rather than loosening `Money`.
- `frontend/src/app/services/api.service.ts` hand-writes `HttpClient` calls. From Phase 1, generate the client from `docs/openapi.yaml` (AGENTS.md §5) and delete this service.
- Makefile is missing `e2e`, `vectors` and `certs` (doc 11 §4). Add them as stubs that print "Phase N" until they're needed.
- `frontend/public/icons/*` and `favicon.ico` are Angular defaults. In Phase 4.2 they're replaced by `docs/design/brand/app-icon-*` and the logo mark.
- `frontend/src/styles/tokens.scss` uses the old `--hp-color-*` names and old values. **Don't build more UI on it.** Phase 4.1 replaces it with `docs/design/tokens.scss`.

## Design update since you started (read before any UI work)
The product owner approved the **Calm Ledger v1** UI and logo (commit `abe468d`). Read `docs/design/ui-spec.md`, open `docs/design/reference/screens.html`, and follow AGENTS.md §10 and the updated Phase 4 in `docs/12-implementation-plan.md`. Every reference screen you build must match `docs/design/reference/screens/*.png` and have a Playwright visual test.

## Notes
- Good: TDD commit pairs, Testcontainers base, Modulith verification, nothing junk tracked in git, IPv6 nginx fix.
- When you submit, include in PROGRESS.md the actual `make test` summary and coverage numbers from that run.
