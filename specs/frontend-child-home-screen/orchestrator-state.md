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
- Phase 2 (data layer and pure logic): DONE | commits 3a0f929, d19efcc, 71e75ef | review 002 | fixes 4/4 | code-review 3 findings, all resolved
  - Tests: 96 passed, 0 failed. Lint: 21 errors / 98 warnings, all pre-existing, none in a Phase 2 file
  - Code-review finding on `ROLE_HOME` carried into T024 rather than fixed here — Android has no API to release the role, so it belongs with the sign-out wiring
- Phase 3 (Layngo Home screen): CODE DONE | build/test pass | review 003
  - T021 remains pending only for physical Realme rendering verification; the Debug APK is available.
- Phase 4 (menu and password gate): CODE DONE | review 003 | parent password is neither persisted nor logged.
- Phase 5 (extra-time and verification): CODE DONE | review 003 | JVM tests and Debug build pass; physical verification remains pending with T021.

- Next: physical Realme validation for T021, then local scoped commit when the concurrent tree can be staged safely.
- Attempts: implement=1, review-fix=1, code-review=1

## Environment

`JAVA_HOME` is unset and no `java` is on `PATH`. Every Gradle command needs the prefix
`JAVA_HOME=D:/Android/jdk/temurin-17`. Java 17 and ADB are available under `D:\Android`, but
`adb devices -l` currently reports no attached handset; only T021 needs the user's Realme.
