# Spec Review: frontend-daily-usage-accuracy
- Branch: `main`
- Spec resolved via: `feature.json`
- Resolution conflicts: none
- Review file: `001review.md`
- Detected commands: test=`:app:testDebugUnitTest` lint=`not configured` types=`:app:compileDebugKotlin`

## Summary

- Overall status: PARTIAL
- High-risk issues: none.
- Missing tests / regression risk: the data mapper and Child request metadata are covered; application-row visual bar behavior is not yet implemented.
- Test suite results: Parent focused test passed, Parent full test/build passed, Child focused test and test/build passed.
- Lint results: not configured.
- Type check results: Parent and Child Kotlin compilation passed.

## Task-by-task Verification

### Task T1: Typed contract and Child report metadata
- Spec requirement / acceptance criteria: Parent reads canonical fields and Child reports a local day/timezone.
- Implementation found: `app_father/.../network/ApiModels.kt::DigitalRuleResponse`; `app_child/.../network/ApiModels.kt::AppUsageRequest`; `AppUsageReportWorker`; `UsageSnapshotMetadataFactory`.
- Status: PASS
- Evidence: Parent no longer needs to interpret raw legacy fields, while Child sends explicit `usage_day` and `usage_timezone` from a tested helper.

### Task T2: Parent accuracy states
- Spec requirement / acceptance criteria: normal, over-limit, confirmation, empty, stale, and error states must be clear and typed.
- Implementation found: `DailyUsageSummaryMapper`, `DailyUsageFragment::render`, `MonitoringFragment::updateUI`, `HomeFragment::updateCharts`.
- Status: PASS
- Evidence: normal summaries use canonical used/limit values; stale or unverified legacy data is not displayed as current.

### Task T3: Layngo screen reconstruction
- Spec requirement / acceptance criteria: Layngo RTL layout, circular summary, list with app visual bars, and one CTA.
- Implementation found: `fragment_daily_usage.xml`, `DailyUsageFragment`, `bg_daily_avatar.xml`, `bg_daily_app_icon.xml`, `colors.xml`.
- Status: PARTIAL
- Evidence: the hierarchy, colors, header, circle, app list, and CTA exist. `DailyUsageFragment::appRow` does not currently add the proportional horizontal usage bar required by the specification.
- Problems: application duration rows are readable but lack the required visual usage comparison.
- Proposed fix: insert a small horizontal `ProgressBar` beneath each application label in `appRow`; use `progress_daily_usage.xml`, bind max to the longest displayed app, and retain the duration text.
- Proposed tests: re-run `:app:testDebugUnitTest :app:assembleDebug` after the visual binding change.

### Task T4: Quality and review
- Spec requirement / acceptance criteria: code compiles, tests pass, and unrelated files remain untouched.
- Implementation found: successful Parent/Child Gradle gates; scope check reports no IoT/Sensor diff.
- Status: PARTIAL
- Evidence: compilation and tests pass, but T3 remains open.

## Issues List (Consolidated)

### Issue 1: Restore proportional app-usage bars
- [x] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: T3, T4
- Evidence: `app_father/SafeNest/app/src/main/java/com/example/safenest/fragments/DailyUsageFragment.kt::appRow` renders only icon, name, and duration; no progress component is added.
- Root cause analysis: the simplified initial row implementation omitted the planned visual progress comparator even though the drawable resource and requirement exist.
- Proposed solution: in `appRow`, replace the plain label slot with a vertical details container containing the label and an Android horizontal progress bar. Set the bar’s progress from `app.usageMinutes / max(apps.usageMinutes)` and set `progressDrawable` to `R.drawable.progress_daily_usage`.
- Test plan: `:app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain`.
- Notes / tradeoffs: the bar communicates relative consumption while the duration remains the accessible exact value.
- Fix notes: `DailyUsageFragment.kt::appRow` now adds a progress bar using `progress_daily_usage.xml`; Parent tests and debug assembly passed after the change.

## Fix Plan (Ordered)

1) Issue 1: Restore proportional app-usage bars — add the required visual comparison without changing the data contract.

## Handoff to Coding Model (Copy/Paste)

- Edit `DailyUsageFragment.kt::appRow` only.
- Add a vertical label-and-progress details region using the existing `progress_daily_usage` resource.
- Preserve RTL ordering, exact duration, touch/readability semantics, and all current summary states.
- Run the Parent test/build command before marking the issue fixed.
