# Reviews

- `REVIEW-phase-N.md`: architect review (Claude). Verdict: **APPROVED** / **CHANGES REQUIRED**, with MUST-FIX and SHOULD-FIX lists. The builder fixes every MUST-FIX item (test first) before starting the next phase.
- `AGENT-REVIEW-phase-N.md`: the second-agent bug-hunt report written by Gemini's reviewer agent (AGENTS.md §7).

## Architect review template
```markdown
# Review: Phase N (<date>)
**Verdict:** CHANGES REQUIRED | APPROVED
**Checked:** commit range, make test (Docker), make e2e, coverage, spec conformance, security, a11y
## MUST-FIX
1. [file:line] problem → failing scenario → required fix
## SHOULD-FIX
## Notes / praise
```
