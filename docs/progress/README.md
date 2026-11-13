# Per-phase progress reports

Two builder agents work in parallel, split by layer (AGENTS.md §12). Each writes its own report here using the template in `AGENTS.md` §9:
- `phase-<N>-be.md`: the backend part (Track BE)
- `phase-<N>-fe.md`: the frontend part (Track FE)
- Phase 2 (built before the split) uses `phase-2.md`; Phase 5a uses `phase-5a.md`.

After the architect approves a part, the architect adds a one-line link from `docs/PROGRESS.md`.
