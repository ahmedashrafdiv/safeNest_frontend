# Tasks: Weekly Per-App Time Limits (Child + Parent)

## Phase 1: Child weekday enforcement

- [ ] FWTL-001 Add `util/WeekdayCode.kt` (today's Backend day-code from local `ZoneId`).
- [ ] FWTL-002 Update `AppBlockerAccessibilityService.isAppOverTimeLimit` to resolve today's per-day limit (per-day dict, with legacy flat-number and missing-day fail-closed fallback).
- [ ] FWTL-003 Add unit tests: weekday resolution, legacy flat-number fallback, 0/1440 boundary semantics.
- [ ] FWTL-004 Run Child Gradle unit tests + `assembleDebug`.

## Phase 2: Parent data-layer widening

- [ ] FWTL-005 Widen `DigitalRuleResponse.appTimeLimits` / `DigitalRuleUpdateRequest.appTimeLimits` to `Map<String, Map<String, Int>>` in `ApiModels.kt`.
- [ ] FWTL-006 Update `MonitoringViewModel.updateDigitalRule` / `DigitalControlRepository.updateDigitalRule` signatures accordingly.
- [ ] FWTL-007 Add the pure status-model function (Blocked/Timed/Allowed derivation) as a standalone, unit-testable object.
- [ ] FWTL-008 Add unit tests for the status model and for 15-minute value-list / "copy Saturday" helper logic.

## Phase 3: Parent State 01 — overview screen

- [ ] FWTL-009 Rebuild `fragment_installed_apps.xml`: Mint policy card with two selectable mode cards, 3 tabs, row list with text status chip + 3-dot button, two summary chip rows, "حفظ التغييرات" + sync helper text.
- [ ] FWTL-010 Rewire `InstalledAppsFragment` rendering to the new layout using the status model from Phase 2, preserving existing device-scope-override save path unchanged.
- [ ] FWTL-011 Run Parent Gradle unit tests + `assembleDebug`.

## Phase 4: Parent State 02 + State 03 — action menu and inline weekly editor

- [ ] FWTL-012 Add the 3-dot popup menu (سماح / تحديد وقت / حظر) wired to the status-model mutations.
- [ ] FWTL-013 Add the inline accordion weekly editor (7 day rows, in-page 15-minute dropdown, "نسخ وقت السبت إلى باقي الأيام", save/collapse) with only one app expanded at a time.
- [ ] FWTL-014 Wire editor save into `saveChangesToServer()` with the new per-app weekly map.
- [ ] FWTL-015 Run Parent Gradle unit tests + `assembleDebug`; manual on-device smoke check of all 3 states.

## Phase 5: Review and closeout

- [ ] FWTL-016 Run spec review, fix open findings, and commit the reviewed frontend phase locally.
