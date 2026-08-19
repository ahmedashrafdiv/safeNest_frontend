# Implementation Plan: Weekly Per-App Time Limits (Child + Parent)

## Status model (both Child and Parent read the same three states per app)

For a given package, in order:
1. `pkg ∈ blockedApp` → **Blocked** ("محظور").
2. `pkg` has a non-empty `appTimeLimits` entry → **Timed** ("محدد بـ<today's resolved minutes>").
3. `appControlMode == allowlist` and `pkg ∉ allowedApp` → **Blocked** (allowlist default-deny).
4. Otherwise → **Allowed** ("مسموح").

This mirrors the existing `backend-app-control-modes` semantics exactly; this feature does not change how block/allow membership works, only how a *time limit* is shaped and displayed.

## Child (`app_child/SafeNest-Kids`)

`AppBlockerAccessibilityService.isAppOverTimeLimit` currently reads `limits[pkg]` as a flat `Int` from the JSON cached by `RuleSyncWorker`/`PrefsHelper.getAppTimeLimitsJson()`. Add a small `WeekdayCode` utility (new file `util/WeekdayCode.kt`) mirroring `UsageSnapshotMetadataFactory`'s local-day approach: `fun todayCode(zoneId: ZoneId = ZoneId.systemDefault()): String` mapping `java.time.DayOfWeek` → the Backend's 7 codes (`sat, sun, mon, tue, wed, thu, fri`).

`isAppOverTimeLimit` parses `limits[pkg]` as either:
- a `JSONObject` (new per-day shape) → read `todayCode()`'s value, default `0` if the day key is absent (fail-closed, matches Backend FR-006).
- a plain number (legacy cache from before a fresh sync) → use it for every day, same as the Backend's legacy-int expansion.

No Backend-shape assumption is baked in beyond "per-day dict or flat number" — `RuleSyncWorker` already stores whatever `policy.values.appTimeLimits` sends as opaque JSON via `PrefsHelper.setAppTimeLimits(JSONObject(...).toString())`; since the Backend now always serves the normalized per-day shape (see `backend-weekly-app-time-limits`), a fresh sync always caches the new shape — the flat-number branch only covers a device that has not synced since this feature shipped.

## Parent (`app_father/SafeNest`)

`InstalledAppsFragment` keeps its existing data-flow (`MonitoringViewModel.installedAppsState` / `digitalRuleState` / `updateDigitalRuleState`, the device-scope override path via `ParentPolicyScopeStore` / `ParentAppBlockingScopeCoordinator` / `ChildDeviceRepository`, and the SharedPreferences local caches for `blockedApp`/`allowedApp`) — this feature does not touch that plumbing except widening the `appTimeLimits` type end-to-end:

- `ApiModels.kt`: `DigitalRuleResponse.appTimeLimits` and `DigitalRuleUpdateRequest.appTimeLimits` become `Map<String, Map<String, Int>>`.
- `MonitoringViewModel.updateDigitalRule` / `DigitalControlRepository.updateDigitalRule`: same type widening, parameter renamed `appTimeLimits: Map<String, Map<String, Int>>? = null`.
- New local-only UI model `data class WeeklyTimeLimit(val packageName: String, val minutesByDay: Map<String, Int>)`; a new SharedPreferences JSON cache (`weekly_time_limits_<childId>`) replaces the old `allowedApps`-as-time-limits cache — the existing `AllowedAppItem`/`allowed_apps_<childId>` cache is untouched (still used by `HomeFragment` purely as a package-name list, unrelated to this feature).
- `InstalledAppsFragment` is rewritten to render the 3-state design (its `fragment_installed_apps.xml` layout is rebuilt to match); the fragment class name, its single navigation entry point (`MonitoringFragment.kt:81`), and all existing save/conflict-resolution logic in `saveChangesToServer` are preserved, adapted to send the new `Map<String, Map<String, Int>>` shape and to build tab counts / summary chips / per-row status from the status model above.

### State 01 — overview
New layout `fragment_installed_apps.xml`: Mint policy card with two selectable radio-style cards (reusing the existing `modeGroup` RadioGroup wiring, restyled), 3 tabs (all/timed/blocked, computed from the in-memory app list + status model, updated on every render), a `RecyclerView`-free `LinearLayout` row list (consistent with this codebase's existing pattern in `DailyUsageFragment`/old `InstalledAppsFragment` — no new RecyclerView adapter machinery introduced), a status-chip `TextView` per row (never color-only), a 3-dot `ImageButton` (≥48dp) opening State 02, and the two summary chip rows built from the current blocked/timed lists. "حفظ التغييرات" reuses `saveChangesToServer()`.

### State 02 — action menu
A `PopupMenu`/small anchored popup (native Android popover, consistent with "not a separate screen") anchored to the row's 3-dot button, 3 items exactly as specified. Selecting "تحديد وقت" closes the popup and calls a new `expandWeeklyEditor(app)` on the fragment; "سماح"/"حظر" mutate the in-memory lists per the status model above and call `saveChangesToServer()` immediately (same optimistic-then-confirm pattern the fragment already uses elsewhere).

### State 03 — inline weekly editor
A single mutable `expandedPackage: String?` field on the fragment (only one editor open at a time, per spec). `renderAvailableApps()` inserts the expanded editor view directly after the matching row's `View` in the same `LinearLayout` (accordion, no dialog/BottomSheet). The editor view is built with 7 day rows (fixed order) each holding a compact `TextView` time field; tapping it opens an anchored `PopupWindow` (in-page, not full-screen) containing a scrollable `RecyclerView`/`ListView` of the 97 fifteen-minute values `00:00..24:00`, consistent with "in-page dropdown, not a bottom sheet." A local `MutableMap<String, Int>` (seeded from the app's current `appTimeLimits` entry, defaulting every day to `0` if unset) backs the 7 fields; "نسخ وقت السبت إلى باقي الأيام" copies `minutesByDay["sat"]` into the other 6 keys of that local map and re-renders the 7 fields (still individually editable after). "حفظ وقت <App>" writes that one app's `minutesByDay` into a copy of the fragment's full `appTimeLimits` map and calls `saveChangesToServer()`; "طي الإعدادات" collapses without discarding the saved server state (only the in-progress local edits for that still-open editor are dropped).

## Verification

- Child: new `AppBlockerServiceTest`-style unit tests (or a new `WeekdayCodeTest.kt` + a pure-function extraction of the limit-resolution logic so it's testable without instrumentation, matching how `DeviceBindingDecider`/`UsageSnapshotMetadataFactory` are already tested in isolation in this codebase) covering weekday resolution, legacy flat-number fallback, and the `0`/`1440` boundary semantics.
- Parent: unit tests for the status-model derivation (pure function, extracted so it doesn't require Robolectric/instrumentation — mirrors `DailyUsageSummaryMapper`'s testable-pure-function pattern already used in this codebase) and for the "copy Saturday" / dropdown-value-list generation logic.
- Both Gradle modules must pass `:app:testDebugUnitTest` and `:app:assembleDebug` before a phase is considered done, consistent with `frontend-daily-usage-accuracy`.
