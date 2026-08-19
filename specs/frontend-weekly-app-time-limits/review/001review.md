# Spec Review: frontend-weekly-app-time-limits
- Branch: `main`
- Spec resolved via: argument
- Resolution conflicts: none (`.specify/feature.json` still points at `frontend-daily-usage-accuracy`; this spec was passed explicitly)
- Review file: `001review.md`
- Detected commands: test=`:app:testDebugUnitTest` lint=`not configured` types=`:app:compileDebugKotlin`

## Summary

- Overall status: PASS
- High-risk issues: one BLOCKER found by code review and fixed (see Issue 1) — the Child's effective-policy model still declared the pre-feature flat `Map<String, Int>` shape.
- Missing tests / regression risk: weekday resolution, legacy flat-number fallback, 0/1440 boundaries, status-model derivation, 97-value picker list, and copy-Saturday are covered by unit tests. The Gson contract mismatch in Issue 1 was not caught by unit tests because it only manifests at the Retrofit deserialization boundary.
- Test suite results: Child focused + full unit tests passed and `assembleDebug` succeeded; Parent focused + full unit tests passed and `assembleDebug` succeeded.
- Lint results: not configured.
- Type check results: Parent and Child Kotlin compilation passed.

## Task-by-task Verification

### Task T1: Child weekday enforcement (Phase 1)
- Spec requirement / acceptance criteria: FR-001..FR-004 — resolve today's weekday limit, fail closed on a missing day, tolerate a legacy flat cache.
- Implementation found: `util/WeekdayCode.kt`, `util/AppTimeLimitResolver.kt`, `service/AppBlockerAccessibilityService.kt::isAppOverTimeLimit`/`parsePerDayLimit`.
- Status: PASS
- Evidence: `parsePerDayLimit` reads a `JSONObject` per-day map when present and expands a flat number otherwise; `AppTimeLimitResolver.resolveTodayLimitMinutes` returns `null` only for an unconfigured app and `0` for an absent day, matching the Backend's `normalize_app_time_limits`. Covered by `AppTimeLimitResolverTest` (6 tests).

### Task T2: Parent data-layer widening (Phase 2)
- Spec requirement / acceptance criteria: the per-day shape must travel end-to-end through the Parent's request/response models.
- Implementation found: `network/ApiModels.kt` (`DigitalRuleResponse`, `DigitalRuleUpdateRequest`), `repository/DigitalControlRepository.kt`, `viewmodel/MonitoringViewModel.kt`, `util/AppControlStatus.kt`.
- Status: PASS
- Evidence: a repo-wide grep for `appTimeLimits` in the Parent module shows every declaration and call site now uses `Map<String, Map<String, Int>>`; `AppControlStatusTest` (10 tests) covers the status model, picker values, and copy-Saturday.

### Task T3: Parent State 01 — overview (Phase 3)
- Spec requirement / acceptance criteria: FR-005 — policy cards, tabs with live counts, text status chips, 3-dot menu, summary chip rows, save button + sync helper.
- Implementation found: `res/layout/fragment_installed_apps.xml`, `InstalledAppsFragment::render`/`appRow`/`renderSummaries`/`styleTab`.
- Status: PASS
- Evidence: tab labels are computed from the live row list; `AppControlStatus.statusText` gives every state a text label (never color-only); the 3-dot target is a 48dp × 48dp `TextView`; blocked/timed chip rows are rebuilt on every render.

### Task T4: Parent State 02 + State 03 (Phase 4)
- Spec requirement / acceptance criteria: FR-006, FR-007, FR-008 — 3-action popup; inline accordion editor with 7 days, in-page 97-value dropdown, copy-Saturday, save/collapse; one editor at a time.
- Implementation found: `InstalledAppsFragment::showActionMenu`, `expandWeeklyEditor`, `weeklyEditor`, `dayRow`, `showTimeDropdown`, `saveWeeklyLimit`, `collapseEditor`.
- Status: PASS
- Evidence: `expandedPackage` is a single nullable field, so `render()` can only insert one editor; `showTimeDropdown` uses an anchored `PopupWindow` over a `ListView` (in-page, not a bottom sheet or new screen) and dismisses any previously open dropdown; `saveWeeklyLimit` writes only the one app's map and reuses the untouched `saveChangesToServer()` device-scope/conflict path.

### Task T5: Review and closeout (Phase 5)
- Status: PASS
- Evidence: code review run at high effort; one BLOCKER + one related finding, both fixed and rebuilt (see below).

## Issues List (Consolidated)

### Issue 1: Child effective-policy model still declared the flat limit shape
- [x] FIXED
- Severity: BLOCKER
- Depends on: none
- Affected tasks: T1
- Evidence: `app_child/.../network/ApiModels.kt::EffectiveAppBlockingValues.appTimeLimits` was `Map<String, Int>`; `RuleSyncWorker.kt:38` is its only consumer.
- Root cause analysis: the Child's enforcement logic was updated to read a per-day map, but the Retrofit/Gson model that *delivers* that map to it was not — the type widening was applied to the Parent module only.
- Proposed solution: widen `EffectiveAppBlockingValues.appTimeLimits` (and the parallel `DigitalRuleResponse.appTimeLimits` on the `getDeviceRules` endpoint) to `Map<String, Map<String, Int>>`.
- Test plan: `:app:testDebugUnitTest :app:assembleDebug` for the Child module.
- Notes / tradeoffs: this would have failed silently in production — Gson's `JsonSyntaxException` is swallowed by `RuleSyncWorker`'s broad `catch (error: Exception)` and converted to `Result.retry()`, so the whole app-blocking policy (not just time limits) would have frozen at its last cached value with no user-visible error.
- Fix notes: both models widened; Child tests and `assembleDebug` passed after the change. `minSdk = 24`, so `JSONObject(Map)` uses `wrap()` and serializes the nested maps correctly for `PrefsHelper.setAppPolicy`.

## Fix Plan (Ordered)

1) Issue 1: Widen the Child's effective-policy and device-rules models to the per-day limit shape — done.

## Handoff to Coding Model (Copy/Paste)

- Frontend specification is complete. The per-weekday limit shape is consistent across Backend, Child, and Parent. Keep the local-commit-only publication boundary; do not push or deploy without explicit confirmation.
- Remaining verification is manual on-device: confirm the three Parent states render and that a saved weekly limit reaches the Child and blocks on the configured day.
