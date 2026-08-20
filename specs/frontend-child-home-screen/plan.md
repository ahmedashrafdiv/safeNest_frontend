# Implementation Plan — Child Home Screen and Parent-Protected Controls

## Repository boundaries

This feature spans two independent repositories.

| Repository | Root | Scope in this feature |
|---|---|---|
| `safenest-backend` | `backend/safenest_review` | The two new child-device session endpoints and their tests |
| `safeNest_frontend` | `frontend_app` | Everything under `app_child/SafeNest-Kids` |

A concurrent effort is editing `app_father/` in the same frontend repository and has uncommitted work
in the tree. Commits for this feature stage explicit paths only; `git add -A` is prohibited here.

### Files that must not be touched

- Anything under `frontend_app/app_father/`
- `app_child/.../service/AppBlockerAccessibilityService.kt` — carries unrelated in-flight edits
- `app_child/.../util/AppTimeLimitResolver.kt`, `util/WeekdayCode.kt`,
  `security/SetupReadinessDecider.kt` — new files belonging to the concurrent effort
- `frontend_app/.specify/feature.json` — the concurrent effort resolves its own spec through it, so
  this feature passes its spec name to the review tooling explicitly

## Design decisions

### Suspending protection without touching the enforcement service

The obvious place for a "protection suspended" check is the accessibility callback, but that file is
off limits. Enforcement reads its inputs entirely from `PrefsHelper`, so clearing those inputs
produces the same observable result:

- `AppPolicyDecider.shouldBlock` returns false for every package when the mode is `blocklist` and the
  blocked set is empty.
- `isAppOverTimeLimit` returns false when the per-app limits JSON is empty.
- `isDailyScreenTimeLimitReached` returns false when the daily budget is zero.

The remaining risk is a sync worker refetching the policy and undoing the clear, so the workers must
themselves respect the suspended flag and the periodic work must be cancelled on suspend.

### Where the suspended flag is honoured

`RuleSyncWorker`, `ScreenTimePolicySyncWorker`, `WebsitePolicySyncWorker`,
`PhoneLocationPolicySyncWorker`, and `ProtectedHomePolicySyncWorker` return early when suspended.
`HomeFragment` skips its scheduling block when suspended. None of these files belong to the concurrent
effort.

### Testable logic is extracted from Android classes

Fragments and dialogs are not unit testable in this project's setup, so the decision logic moves into
plain Kotlin objects that the existing JVM test source set can exercise:

- `ScreenTimeBudget` — converts a screen-time decision into ring sweep and remaining-minute label,
  including the no-policy and over-budget cases.
- `ChildGreeting` — builds the greeting line and its fallback.
- `ParentVerificationDecider` — maps an HTTP status to a user-facing outcome (verified, wrong
  password, locked, offline).

### Ring rendering

A custom `BudgetRingView` draws the track and the sweep with `Canvas.drawArc`. This avoids a
determinate `ProgressBar` rotation hack and keeps the stroke cap and thickness under the design's
control.

## Phases

| Phase | Title | Repository |
|---|---|---|
| 1 | Backend child-device session endpoints | backend |
| 2 | Child app data layer and pure logic | frontend |
| 3 | Layngo Home screen | frontend |
| 4 | Menu and parent-password gate | frontend |
| 5 | Extra-time request and final verification | frontend |

Each phase is committed separately with explicitly staged paths.

## Verification commands

| Purpose | Command |
|---|---|
| Backend tests | `.\.venv\Scripts\python.exe -m pytest -q` from `backend/safenest_review` |
| Backend compile | `.\.venv\Scripts\python.exe -m compileall -q app` |
| Child unit tests | `gradlew.bat :app:testDebugUnitTest` from `app_child/SafeNest-Kids` |
| Child build | `gradlew.bat assembleDebug` from `app_child/SafeNest-Kids` |

## Risks

| Risk | Mitigation |
|---|---|
| Concurrent effort commits the same files | Stage explicit paths, never `-A`; re-check `git status` before each commit |
| Suspended policy silently refetched | Workers check the flag and periodic work is cancelled on suspend |
| Parent password brute-forced on the child's device | Server-side lockout after five failures for fifteen minutes |
| Ring shows a misleading zero with no policy | `ScreenTimeBudget` models the no-policy case explicitly |
