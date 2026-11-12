# GEMINI.md

All project rules for coding agents live in **[AGENTS.md](AGENTS.md)**. Read it fully before doing anything.

Quick reminders:
- The spec is in `docs/` (start at `docs/00-README.md`). Do not edit it; propose changes in `docs/proposals/`.
- Work one phase at a time from `docs/12-implementation-plan.md`, then STOP for architect review.
- TDD always (red → green → refactor). Tests run in Docker/Testcontainers (`make test`).
- At each phase end, run the second-agent bug hunt using `REVIEWER_PROMPT` in AGENTS.md §7.
- UI must follow `docs/design/` (Canva) and WCAG 2.2 AA.
