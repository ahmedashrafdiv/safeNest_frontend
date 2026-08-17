# Spec Review: frontend-installed-app-management

- Branch: `main`
- Spec resolved via: explicit spec path
- Resolution conflicts: `.specify/feature.json` points to `frontend-app-blocking-reliability`; this review intentionally targets the explicit installed-app spec.
- Review file: `001review.md`
- Detected commands: Parent build=`gradlew.bat assembleDebug --no-daemon`; Child build=`gradlew.bat assembleDebug --no-daemon`; unit tests=`not configured`

## Summary

- Overall status: PARTIAL
- High-risk issues: No confirmed package-name identity defect in the reviewed Parent fragment.
- Missing tests / regression risk: Android automated tests for parsing, payload construction, and UI state are not configured; live paired-device enforcement remains unverified.
- Test suite results: Parent and Child debug builds completed successfully in the prior validation run; the subsequent Parent rebuild exposed a stale local SDK path warning in ignored `local.properties`.
- Lint results: not configured
- Type check results: not configured

## Task-by-task Verification

### Task T1: Contract and inventory loading
- Spec requirement / acceptance criteria: Parent parses `{ package_name, app_name }`, loads inventory on open/resume, and shows explicit states.
- Implementation found:
  - Files: `app_father/SafeNest/app/src/main/java/com/example/safenest/fragments/InstalledAppsFragment.kt`, `app_father/SafeNest/app/src/main/java/com/example/safenest/network/SafeNestApiService.kt`
  - Key symbols: `installedAppsState`, `getInstalledApps`, `onViewCreated`, `onResume`, `renderAvailableApps`
- Status: PASS
- Evidence: The fragment loads the selected child on initial view and resume, renders loading/error/empty states, displays `appName` with package fallback, and uses package names in actions.

### Task T2: Policy actions and in-flight protection
- Spec requirement / acceptance criteria: Block/unblock and time limits update the authoritative rule using package names and avoid duplicate submissions.
- Implementation found:
  - File: `InstalledAppsFragment.kt`
  - Key symbols: `toggleBlockedApp`, `showTimeLimitDialog`, `saveChangesToServer`, `policyUpdateInFlight`
- Status: PASS
- Evidence: `saveChangesToServer` returns while an update is in flight; success/error states clear the guard; the update payload maps `AllowedAppItem.name` to `appTimeLimits`.

### Task T3: Automated tests
- Spec requirement / acceptance criteria: Parsing, payload, and supported UI-state tests exist.
- Implementation found: No dedicated Android tests for this feature were found.
- Status: PARTIAL
- Proposed fix: Add pure Kotlin tests for the installed-app model and payload mapping if the project test setup supports them; otherwise document the limitation and retain build verification.
- Proposed tests: project-supported Gradle unit-test task, if configured.

### Task T4: Build and live verification
- Spec requirement / acceptance criteria: Parent and Child debug builds succeed; paired-device verification is distinguished from local build evidence.
- Implementation found:
  - Files: both Gradle projects and `orchestrator-state.md`
- Status: PARTIAL
- Evidence: Earlier Parent and Child builds succeeded. A later Parent invocation emitted a warning because ignored `local.properties` points to a machine-specific SDK path, although the APK artifact exists. No paired device was available through ADB.
- Proposed fix: Keep machine-local SDK paths out of source control and validate with the configured SDK environment; perform a live paired-device run when hardware is available.
- Proposed tests: `gradlew.bat assembleDebug --no-daemon` for each project and a manual ADB verification checklist.

## Issues List (Consolidated)

### Issue 1: Android automated feature tests are not configured
- [ ] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: T3
- Evidence: No dedicated installed-app unit/UI tests were found in the Parent project.
- Root cause analysis: The implementation is fragment-driven and the current Android project has no established test harness for these model/payload/UI states.
- Proposed solution: Add only the smallest pure-model/payload tests supported by the existing Gradle setup; do not create a broad UI-test framework solely for this phase.
- Test plan: run the project’s configured Gradle unit-test task if present, followed by `assembleDebug`.
- Notes / tradeoffs: Live Android enforcement cannot be proven by JVM tests.

### Issue 2: Live paired-device verification remains unavailable
- [ ] FIXED
- Severity: LOW
- Depends on: none
- Affected tasks: T4
- Evidence: ADB previously reported no devices/emulators available.
- Root cause analysis: Hardware/ADB availability, not a demonstrated source-code failure.
- Proposed solution: Keep this item open as an environment-dependent verification task; do not claim completion from a local APK build.
- Test plan: install Child and Parent APKs on paired devices, report installed apps, block a package, and measure synchronization/enforcement latency.
- Notes / tradeoffs: No production code change is required for this issue.

## Fix Plan (Ordered)

1) Issue 1: Android automated feature tests are not configured — add minimal supported model/payload tests or document the limitation.
2) Issue 2: Live paired-device verification remains unavailable — perform the manual hardware gate when a device is connected.

## Handoff to Coding Model (Copy/Paste)

- Files to edit/create: Android test source only if an existing test setup supports it; otherwise state documentation.
- Exact behavior changes: Preserve package-name identity and in-flight protection; do not alter Backend or IoT code.
- Edge cases: empty inventory, missing display name, repeated action taps, stale local SDK path, no ADB device.
- Tests to add/update: minimal parsing/payload tests if supported, plus Parent/Child debug builds.
- Suggested commit breakdown: one local Frontend verification commit; no push.
