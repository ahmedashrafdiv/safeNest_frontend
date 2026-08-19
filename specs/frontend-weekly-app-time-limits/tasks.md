# Tasks: Weekly Per-App Time Limits (Child + Parent)

## Phase 1: Child weekday enforcement

- [x] FWTL-001 Add `util/WeekdayCode.kt` (today's Backend day-code from local `ZoneId`).
- [x] FWTL-002 Update `AppBlockerAccessibilityService.isAppOverTimeLimit` to resolve today's per-day limit (per-day dict, with legacy flat-number and missing-day fail-closed fallback).
- [x] FWTL-003 Add unit tests: weekday resolution, legacy flat-number fallback, 0/1440 boundary semantics.
- [x] FWTL-004 Run Child Gradle unit tests + `assembleDebug`.

## Phase 2: Parent data-layer widening

- [x] FWTL-005 Widen `DigitalRuleResponse.appTimeLimits` / `DigitalRuleUpdateRequest.appTimeLimits` to `Map<String, Map<String, Int>>` in `ApiModels.kt`.
- [x] FWTL-006 Update `MonitoringViewModel.updateDigitalRule` / `DigitalControlRepository.updateDigitalRule` signatures accordingly.
- [x] FWTL-007 Add the pure status-model function (Blocked/Timed/Allowed derivation) as a standalone, unit-testable object.
- [x] FWTL-008 Add unit tests for the status model and for 15-minute value-list / "copy Saturday" helper logic.

## Phase 3: Parent State 01 — overview screen

- [x] FWTL-009 Rebuild `fragment_installed_apps.xml`: Mint policy card with two selectable mode cards, 3 tabs, row list with text status chip + 3-dot button, two summary chip rows, "حفظ التغييرات" + sync helper text.
- [x] FWTL-010 Rewire `InstalledAppsFragment` rendering to the new layout using the status model from Phase 2, preserving existing device-scope-override save path unchanged.
- [x] FWTL-011 Run Parent Gradle unit tests + `assembleDebug`.

## Phase 4: Parent State 02 + State 03 — action menu and inline weekly editor

- [x] FWTL-012 Add the 3-dot popup menu (سماح / تحديد وقت / حظر) wired to the status-model mutations.
- [x] FWTL-013 Add the inline accordion weekly editor (7 day rows, in-page 15-minute dropdown, "نسخ وقت السبت إلى باقي الأيام", save/collapse) with only one app expanded at a time.
- [x] FWTL-014 Wire editor save into `saveChangesToServer()` with the new per-app weekly map.
- [x] FWTL-015 Run Parent Gradle unit tests + `assembleDebug`; Parent app installed and launched on the paired device with no crash. Full end-to-end on-device walkthrough of the three states is still pending user verification.

## Phase 5: Review and closeout

- [x] FWTL-016 Run spec review, fix open findings, and commit the reviewed frontend phase locally. (code review high effort: 1 BLOCKER — Child effective-policy model still declared the flat limit shape, would have frozen all policy sync — fixed and rebuilt; review/001review.md: PASS)
