# Orchestrator State

- Spec: `frontend_app/specs/frontend-child-home-screen`
- Backend repo: `backend/safenest_review` (branch `main`)
- Frontend repo: `frontend_app` (branch `main`)
- Tool modes: implement=SELF (no speckit-implement present), spec-review=CALLABLE, review-fix=CALLABLE, code-review=CALLABLE
- Backend test cmd: `.\.venv\Scripts\python.exe -m pytest -q` | compile: `-m compileall -q app`
- Frontend test cmd: `gradlew.bat :app:testDebugUnitTest` | build: `gradlew.bat assembleDebug`

## Concurrency constraint

A separate effort is editing `app_father/` in the same frontend repository and has uncommitted work
in the tree. Commits stage explicit paths only. `git add -A` is prohibited. `feature.json` is left
untouched; the spec name is passed to review tooling explicitly.

## Phase log

- Phase 1 (backend session endpoints): DONE | commits db66bed, ac50f10 | review 001 | fixes 4/4 | code-review 1 finding, fixed
  - Tests: 262 passed, 3 pre-existing failures in tests/test_daily_usage_accuracy.py (missing `tzdata` in the venv, unrelated to this feature)
- Phase 2 (data layer and pure logic): IN PROGRESS
- Phase 3 (Layngo Home screen): PENDING
- Phase 4 (menu and password gate): PENDING
- Phase 5 (extra-time and verification): PENDING

- Next: Phase 2, T008
- Attempts: implement=1, review-fix=1, code-review=1
