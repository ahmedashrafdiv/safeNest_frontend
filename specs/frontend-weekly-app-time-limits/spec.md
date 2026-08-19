# Feature Specification: Weekly Per-App Time Limits (Child Enforcement + Parent Control Screens)

## Purpose

`backend-weekly-app-time-limits` changes `appTimeLimits` from one flat minute value per app to a per-weekday map. This feature updates the Child's local enforcement to resolve *today's* weekday limit instead of a flat number, and rebuilds the Parent "تطبيقات ليان" app-control screen around the approved 3-state design: an overview with policy selection and a per-app list, a three-dot action menu, and an inline (non-modal) weekly time editor.

## Functional Requirements

### Child enforcement
- FR-001: `AppBlockerAccessibilityService.isAppOverTimeLimit` resolves the limit for the *current local weekday* from the per-day map synced via `RuleSyncWorker`/`PrefsHelper`, not a single flat number.
- FR-002: The Child's local weekday is derived the same way the existing local-day logic already works (device `ZoneId`/`LocalDate`, consistent with `UsageSnapshotMetadataFactory`), mapped to the backend's 7 day codes (`sat, sun, mon, tue, wed, thu, fri`).
- FR-003: A day value of `0` blocks the app all day; `1440` never blocks the app for that day's time-limit check (an overall daily screen-time limit, if any, still applies independently).
- FR-004: Locally cached legacy-shaped data (a flat int, from before this feature) does not crash enforcement — the Child normalizes it the same way the Backend does (same value every day) until the next policy sync refreshes it to the new shape.

### Parent control screens ("تطبيقات ليان")
- FR-005: **State 01 (overview)** shows the two mutually-exclusive control-mode cards (`blocklist` / `allowlist` — see `backend-app-control-modes`), three tabs (all / timed / blocked) with live counts, a rounded app list (real label from the installed-apps endpoint, a text-only status chip, a 3-dot menu), and blocked/timed summary chip rows.
- FR-006: **State 02 (action menu)** — tapping an app's 3-dot menu opens exactly 3 actions: "سماح" (clear block + time limit), "تحديد وقت" (expand State 03 for that app), "حظر" (block that app, clearing any time limit).
- FR-007: **State 03 (inline weekly editor)** — an accordion expansion under the selected app's row in the same scroll, not a dialog/bottom sheet/new screen. Exactly one app's editor is expanded at a time. Shows all 7 days (`السبت` first) each with an in-page 15-minute-increment time picker (00:00–24:00, 97 values), a "نسخ وقت السبت إلى باقي الأيام" convenience action, and a save button that writes only that one app's weekly map.
- FR-008: Saving in State 03 updates that app's status chip and the timed-apps summary immediately (optimistic local render), then persists via the existing digital-control update endpoint and reflects the server-confirmed value once the request completes.
- FR-009: All visible copy is Arabic RTL; status is never conveyed by color alone (each chip has text).

## Acceptance Criteria

| Scenario | Expected result |
|---|---|
| Weekday-specific block | An app limited to `0` minutes today (per its day-of-week entry) is blocked today even if other days allow time. |
| Weekday resolution | The Child blocks/allows using *today's* code, not a hardcoded day. |
| Legacy cached data | A Child that last synced before this feature (flat-int cache) still enforces a sane limit instead of crashing or ignoring the limit. |
| Tabs reflect state | Switching an app between allowed / timed / blocked updates its tab counts and both summary chip rows. |
| One editor at a time | Expanding a second app's "تحديد وقت" collapses the first app's open editor. |
| Weekly values differ | Setting different minutes for two different days round-trips distinctly for each day after save + reload. |
| Copy-Saturday | Using "نسخ وقت السبت إلى باقي الأيام" fills all 6 remaining days with Saturday's value, and each day remains individually editable afterward. |
| 0 / 1440 semantics | `00:00` shows as unavailable that day; `24:00` shows as available all day. |

## Clarifications

- [ASSUMED] Weekday mapping: Child's `java.time.DayOfWeek` maps 1:1 to the Backend's 7 day codes via a small fixed table, mirroring how `UsageSnapshotMetadataFactory` already derives the local day for usage reports.
- [ASSUMED] "محدد بـ<duration>" chip text on the overview list shows *today's* resolved minutes (the value the Child is actually enforcing right now) rather than a generic "weekly limit set" label, so the Parent sees what's actually in effect today.
- [ASSUMED] Existing `InstalledAppsFragment` is superseded by the new overview screen for this control flow; the old fragment's other responsibilities (if any beyond app-control) are preserved or the fragment is retired — decided during Phase 2 after reading its current contents.
- [ASSUMED] Publication: no remote push or deployment is included — user approval is required separately, consistent with `frontend-daily-usage-accuracy`.
