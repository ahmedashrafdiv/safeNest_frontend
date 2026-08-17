# Spec Review: frontend-installed-app-management — Automatic Detection

- Branch: `main`
- Spec resolved via: explicit spec path
- Resolution conflicts: `.specify/feature.json` points to the earlier reliability feature; this review targets the explicit installed-app-management spec.
- Review file: `002review.md`
- Detected commands: Child tests=`gradlew.bat testDebugUnitTest`; Parent build=`gradlew.bat assembleDebug --no-daemon`; Child build=`gradlew.bat assembleDebug --no-daemon`

## Summary

- Overall status: PARTIAL
- High-risk issues: No confirmed defect in package-event registration, fingerprinting, retry semantics, or the existing Backend contract.
- Missing tests / regression risk: Worker-level tests are not configured; live ADB verification remains open.
- Test suite results: Child `testDebugUnitTest assembleDebug` succeeded; Backend `175 passed, 0 failed`; Parent debug build succeeded in the validation run.
- Lint results: not configured
- Type check results: not configured

## Task-by-task Verification

### Task T1: Inventory normalization and fingerprinting
- Status: PASS
- Evidence: `InstalledAppsHelper.getInstalledApps` removes the Child package, deduplicates package identities, sorts results, and `fingerprint` uses deterministic SHA-256 canonicalization. `InstalledAppsHelperTest` covers ordering, package additions, label changes, and duplicates.

### Task T2: Durable synchronization and retry
- Status: PASS
- Evidence: `InstalledAppsSyncWorker` skips unpaired devices and unchanged fingerprints, uploads through the existing `updateInstalledApps` API, persists the fingerprint only after success, and returns `Result.retry()` on unsuccessful responses/exceptions. WorkManager uses connected-network constraints and unique work.

### Task T3: Package lifecycle triggers
- Status: PASS
- Evidence: `AndroidManifest.xml` registers package-added, package-removed, and package-replaced filters with the `package` data scheme. `ServiceWatchdogReceiver` enqueues the worker for package changes, boot, and Child-app replacement while limiting accessibility notifications to lifecycle recovery.

### Task T4: Verification coverage
- Status: PARTIAL
- Evidence: Child unit tests and both Android debug builds pass. No worker-level fake-API tests or live ADB test were available. The live test is environment-dependent and must not be claimed from local builds.

## Issues List (Consolidated)

### Issue 1: Worker-level and live-device verification are incomplete
- [ ] FIXED
- Severity: LOW
- Depends on: none
- Affected tasks: Phase 5 worker tests and live ADB verification
- Evidence: The project now has pure fingerprint tests, but no WorkManager/fake-API tests and no connected Android device.
- Root cause analysis: The worker is coupled to the concrete Retrofit singleton and the previous session had no ADB device available.
- Proposed solution: Add a small injectable upload seam or worker test fixture only if it fits the existing architecture; otherwise preserve the current unit coverage and execute the live matrix when hardware is available.
- Test plan: `gradlew.bat testDebugUnitTest`; then manual ADB install/remove/replace/offline/reboot checks.
- Notes / tradeoffs: Do not make production code more abstract solely to manufacture a test. The current implementation has durable retry behavior based on WorkManager semantics.

## Fix Plan (Ordered)

1) Issue 1: Worker-level and live-device verification are incomplete — complete the environment-dependent verification when a device is available, and add worker tests only if a minimal existing seam is available.

## Handoff to Coding Model (Copy/Paste)

- Files to edit/create: Android tests or a minimal worker seam only if required; no Backend or IoT files.
- Exact behavior changes: Preserve package-event triggers, fingerprint persistence, and retry behavior.
- Edge cases: rapid broadcasts, offline upload, app removal, app replacement, unchanged labels, unpaired Child.
- Tests to add/update: Worker success/retry tests if supported, then Child unit tests and both builds.
- Suggested commit breakdown: local Frontend commit; no push.
