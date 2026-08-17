# Spec Review: frontend-phone-location-tracking

- Repository: `frontend_app`
- Branch: local working tree
- Spec resolved via: `.specify/feature.json`
- Review file: `001review.md`
- Test command: `app_child/SafeNest-Kids/gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`
- Parent build command: `app_father/SafeNest/gradlew.bat assembleDebug --no-daemon --console=plain`
- Lint: not configured
- Type check: not configured

## Summary

Overall status is **PARTIAL**. The Android software implementation is complete and both APK build gates passed. The remaining items are a deliberately deferred Retrofit/WorkManager test harness and the physical paired-device/deployed-authentication gate. No IoT files, external-GPS management files, AccessibilityService implementation, or website VPN implementation files were included in the feature diff.

The Child test/build command passed, including `PhoneLocationDeciderTest`, and the Parent debug APK build passed. The current module does not contain a Retrofit/WorkManager unit-test harness, so the worker's HTTP outcomes are not marked as unit-tested. That limitation is documented rather than hidden.

## Task-by-task Verification

| Task | Requirement | Evidence | Status |
|---|---|---|---|
| T001 | Establish Child location feature paths | PhoneLocationService, PhoneLocationSyncWorker, PhoneLocationDecider, models, tests, and permission/UI paths exist | PASS |
| T002 | Preserve IoT/external-GPS/enforcement boundaries | Git status contains no sensor, ThingSpeak, IoT, AccessibilityService, or WebsiteDnsVpnService changes | PASS |
| T003 | Document Child upload and Parent read contracts | `specs/frontend-phone-location-tracking/contracts.md` and shared integration contract | PASS |
| T004 | Add typed Child upload/policy models | Child `ApiModels.kt` contains upload and response models | PASS |
| T005 | Persist phone tracking state | Child `PrefsHelper.kt` stores enabled, permission, service, network, report, timestamp, and status state | PASS |
| T006 | Add pure filtering/freshness decision logic | `PhoneLocationDecider.kt` validates coordinates, accuracy, future timestamps, interval, movement, and status | PASS |
| T007 | Cover pure decision logic | `PhoneLocationDeciderTest.kt` covers invalid/future samples, first upload, duplicate suppression, movement, permission, offline, and stale state | PASS |
| T008 | Declare Android location/foreground permissions | Child manifest declares coarse/fine location and foreground location service permission | PASS |
| T009 | Explain and request consent in onboarding | Child permissions layout has a dedicated phone-location card with persistent-notification and failure explanation | PASS |
| T010 | Implement runtime permission transitions | PermissionsFragment requests location permission and persists granted/denied states | PASS |
| T011 | Implement visible foreground service | PhoneLocationService starts only after consent, uses a location foreground notification, and stops truthfully on failure/destroy | PASS |
| T012 | Show truthful Child health state | HomeFragment and fragment_home distinguish active, denied, offline, stale, disabled, and unavailable | PASS |
| T013 | Cover local health transitions | Pure decision tests cover the state matrix; Android runtime lifecycle remains hardware-gated | PASS WITH LIMITATION |
| T014 | Add authenticated upload endpoint | KidsApiService declares `POST api/child-devices/{device_id}/location` | PASS |
| T015 | Implement retry/backoff and idempotent report identity | PhoneLocationSyncWorker uses WorkManager network constraints, exponential backoff, stable report input, and status persistence | PASS |
| T016 | Keep upload off the main thread | Location callbacks enqueue WorkManager; no Retrofit call is made from the callback thread | PASS |
| T017 | Recover after boot/app replacement | ServiceWatchdogReceiver starts the phone service only when paired, enabled, and location consent remains present | PASS |
| T018 | Add worker HTTP/state tests | No existing Retrofit/WorkManager test harness; focused pure tests pass, but HTTP worker tests are not present | OPEN — DEFERRED |
| T019 | Add typed Parent location envelope | Parent ApiModels.kt contains coordinate/envelope fields, source, age, stale, fallback, and legacy compatibility fields | PASS |
| T020 | Use typed Retrofit/repository contract | SafeNestApiService and LocationRepository return ParentLocationEnvelope while external GPS endpoints remain unchanged | PASS |
| T021 | Expose typed location state | GpsViewModel exposes `Result<ParentLocationEnvelope>` and policy state | PASS |
| T022 | Render source/freshness/stale/unavailable states | GpsFragment labels Child phone, external GPS, age, accuracy, stale, disabled, and unavailable states; stale markers use a distinct color | PASS |
| T023 | Validate Parent mapping | Parent assembleDebug passed; no Parent unit-test harness is configured | PASS WITH LIMITATION |
| T024 | Add Parent enable/disable policy operation | Typed models, PATCH Retrofit endpoint, repository method, ViewModel state, and switch are implemented | PASS |
| T025 | Keep Parent phone control separate from external GPS | Switch is in GpsFragment and does not call GPS pair/update/delete | PASS |
| T026 | Verify disable state and external GPS preservation | Requires deployed Backend and paired devices; no physical run recorded | OPEN — HARDWARE GATE |
| T027 | Update quickstart | Quickstart includes SDK/JDK commands, APK paths, and paired-device matrix | PASS |
| T028 | Add English implementation comments and limitation copy | Kotlin comments and user-facing consent/stale/fallback limitations are present | PASS |
| T029 | Run Child test/build gate | `testDebugUnitTest assembleDebug --no-daemon --console=plain` passed | PASS |
| T030 | Run Parent build gate | `assembleDebug --no-daemon --console=plain` passed | PASS |
| T031 | Run full paired-device gate | Not performed; requires real devices, deployed Backend, permissions, notification, network loss, reboot, and map refresh | OPEN — HARDWARE GATE |

## Issues List

### Issue 1: Worker HTTP outcomes lack a dedicated test harness

- Status: OPEN — DEFERRED, not a production failure.
- Severity: LOW for the current increment; MEDIUM before release.
- Affected task: T018.
- Evidence: The Child module has JVM unit tests but no Retrofit mock server or WorkManager test rule. The worker code compiles and the pure decision layer passes, but success, 409 disabled, 429/5xx retry, and network exception behavior are not exercised through a fake API.
- Root cause: The existing project test setup contains only JUnit and no AndroidX WorkManager test dependency or Retrofit mock server.
- Proposed fix: Add a focused test harness in a follow-up increment using a fake `KidsApiService` seam or MockWebServer plus WorkManager test support. Keep tests limited to the worker's response/state matrix.
- Do not replace with: Do not mark runtime HTTP behavior passed solely because the APK builds.

### Issue 2: Parent disable policy and external GPS preservation require a live run

- Status: OPEN — HARDWARE GATE.
- Severity: MEDIUM.
- Affected task: T026.
- Evidence: The Parent switch and Backend route are implemented, but no deployed paired-device result is recorded.
- Root cause: Local builds cannot verify deployed authentication, actual Child policy propagation, or that an external GPS record remains unchanged after disabling phone tracking.
- Proposed fix: Run the quickstart matrix with a real Parent/Child pair and record the response status, source, age, policy state, and external GPS state before/after toggle.

### Issue 3: Full paired-device release gate is pending

- Status: OPEN — HARDWARE GATE.
- Severity: HIGH before release, not a compile/test failure.
- Affected task: T031.
- Evidence: No device installation, runtime permission dialog, foreground notification, upload, reboot, offline recovery, stale map, or deployed API evidence is present.
- Proposed fix: Execute every case in `specs/frontend-phone-location-tracking/quickstart.md`, including permission denial, approximate/precise permission, real upload, duplicate retry, network loss, reboot, disable, external fallback, stale state, and unauthorized Parent.
- Safety note: The UI is intentionally fail-closed/truthful and does not claim active tracking until the service and upload state support that claim.

## Fix Plan

1. Keep Issue 1 explicitly deferred and schedule the narrow worker test harness before release hardening.
2. Execute Issue 2 during the paired-device run and record source isolation and policy propagation.
3. Execute Issue 3 as the final release gate; only then mark the feature fully passed.

## Handoff

The software implementation is ready for local APK installation. No GitHub push should be performed without explicit user confirmation. The current implementation preserves the external GPS/ThingSpeak path as a separate source and uses the phone source first only when the Backend reports it as fresh. Website filtering and app blocking services were not modified by this feature.
