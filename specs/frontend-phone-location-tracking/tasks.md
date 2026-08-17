# Tasks: Phone-Based Child Location Tracking Frontend

## Phase 1: Setup

- [x] T001 Create the Child location service, model, and test paths described in `specs/frontend-phone-location-tracking/plan.md`.
- [x] T002 [P] Add a source-isolation boundary in the feature plan/tasks and verify the Git diff contains no IoT or unrelated enforcement files.
- [x] T003 [P] Add the Child upload and Parent read contract documentation in `specs/frontend-phone-location-tracking/contracts.md`.

## Phase 2: Foundational models and local state

- [x] T004 Add typed Child location upload, policy, response, and health-state models in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/network/ApiModels.kt`.
- [x] T005 [P] Add tracking state keys and atomic getters/setters in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/util/PrefsHelper.kt`.
- [x] T006 [P] Add pure `PhoneLocationDecider` filtering and freshness logic in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/service/PhoneLocationDecider.kt`.
- [x] T007 [P] Add unit tests for permission gating, coordinate validity, accuracy, duplicate filtering, movement threshold, and stale local state in `app_child/SafeNest-Kids/app/src/test/java/com/safenest/kids/service/PhoneLocationDeciderTest.kt`.

## Phase 3: User Story 1 — Child grants and maintains phone tracking

- [x] T008 [US1] Add coarse/fine location and foreground-service location permissions to `app_child/SafeNest-Kids/app/src/main/AndroidManifest.xml` without altering unrelated services.
- [x] T009 [US1] Add explicit location-permission explanation, status, and action to `app_child/SafeNest-Kids/app/src/main/res/layout/fragment_permissions.xml`.
- [x] T010 [US1] Implement permission request and truthful status transitions in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/PermissionsFragment.kt`.
- [x] T011 [US1] Implement visible foreground `PhoneLocationService` lifecycle in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/service/PhoneLocationService.kt`.
- [x] T012 [US1] Add Child Home tracking status and unavailable/stale/permission-denied copy in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/HomeFragment.kt` and `app_child/SafeNest-Kids/app/src/main/res/layout/fragment_home.xml`.
- [x] T013 [US1] Cover service/local-health transitions through pure `PhoneLocationDeciderTest.kt`; Android runtime lifecycle remains a device-gated test.

## Phase 4: User Story 2 — Child synchronizes accepted reports

- [x] T014 [US2] Add `POST api/child-devices/{device_id}/location` to `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/network/KidsApiService.kt`.
- [x] T015 [US2] Implement accepted-report upload, bounded retry/backoff, and last-success persistence in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/service/PhoneLocationSyncWorker.kt`.
- [x] T016 [US2] Connect the location service to the upload worker without blocking the main thread in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/service/PhoneLocationService.kt`.
- [x] T017 [US2] Add boot/package/app-replacement reconciliation for phone tracking in `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/util/ServiceWatchdogReceiver.kt`.
- [ ] T018 [US2] Add worker/service state tests for successful upload, duplicate retry, network failure, disabled policy, and permission revocation in `app_child/SafeNest-Kids/app/src/test/java/com/safenest/kids/service/PhoneLocationSyncWorkerTest.kt` (deferred because the current project has no Retrofit/WorkManager test harness).

## Phase 5: User Story 3 — Parent sees phone location with source and freshness

- [x] T019 [US3] Add typed `LocationEnvelope` and legacy compatibility mapping in `app_father/SafeNest/app/src/main/java/com/example/safenest/network/ApiModels.kt`.
- [x] T020 [US3] Update `getChildLocation` response mapping in `app_father/SafeNest/app/src/main/java/com/example/safenest/network/SafeNestApiService.kt` and `app_father/SafeNest/app/src/main/java/com/example/safenest/repository/LocationRepository.kt`.
- [x] T021 [US3] Update `GpsViewModel` state to expose source, age, accuracy, stale, disabled, and unavailable values in `app_father/SafeNest/app/src/main/java/com/example/safenest/viewmodel/GpsViewModel.kt`.
- [x] T022 [US3] Update the existing Parent map screen to render source labels, freshness, accuracy, fallback, stale, and unavailable states in `app_father/SafeNest/app/src/main/java/com/example/safenest/fragments/GpsFragment.kt` and its layout.
- [x] T023 [US3] Validate Parent API/model mapping through successful `assembleDebug`; dedicated mapping tests remain a follow-up because no Parent unit-test harness exists in this module.

## Phase 6: User Story 4 — Parent controls phone tracking

The Parent toggle calls `PATCH /api/child-devices/{child_id}/phone-location?enabled={true|false}` and refreshes the typed location envelope. Deleting phone tracking is intentionally not exposed as a casual UI action; it remains an authenticated Backend endpoint for an explicit future privacy flow.

- [x] T024 [US4] Add Parent tracking enable/disable API models, Retrofit endpoints, repository methods, ViewModel state, and a Parent GPS-screen switch; external GPS controls remain separate.
- [x] T025 [US4] Add a distinct Parent phone-tracking control in `app_father/SafeNest/app/src/main/java/com/example/safenest/fragments/GpsFragment.kt`; it does not invoke external GPS pairing/deletion.
- [ ] T026 [US4] Run live/manual verification for disabled state, deployed policy propagation, and external GPS preservation; automated Android UI tests are not configured in this module.

## Phase 7: Polish and release gate

- [x] T027 [P] Update `specs/frontend-phone-location-tracking/quickstart.md` with the final APK commands and paired-device checklist.
- [x] T028 [P] Add English implementation comments and user-facing limitation copy for consent, foreground tracking, stale state, and source fallback.
- [x] T029 Run `app_child/SafeNest-Kids/gradlew.bat testDebugUnitTest assembleDebug`; Child unit tests and debug APK build passed.
- [x] T030 Run `app_father/SafeNest/gradlew.bat assembleDebug`; Parent debug APK build passed.
- [ ] T031 Run the paired-device gate covering permission, notification, upload, Parent map, stale state, fallback, reboot, network loss, and disable/delete behavior.

## Dependencies

Phase 1 precedes Phase 2. Phase 2 precedes User Stories 1 and 2. User Story 1 must provide the service lifecycle before User Story 2 can connect uploads. User Story 3 depends on the Backend envelope contract but can use fixtures for independent UI mapping tests. User Story 4 depends on the policy decision from Clarify. Phase 7 follows all software phases.

## Parallel execution examples

After T004 and T006 are complete, T005 and T007 can proceed in separate files. After T014 is complete, T018 and T019 can proceed in their respective repositories. T027 and T028 can proceed in parallel after implementation; T029 and T030 are separate build gates, and T031 remains the final hardware gate.

## Implementation strategy

The MVP is User Stories 1–3: consented Child collection, authenticated upload, and Parent display of a fresh phone point with truthful stale/unavailable/source states. Parent enable/disable control is a separate P2 increment if the Backend policy contract is not ready in the first implementation slice. No task may modify IoT or external GPS files.
