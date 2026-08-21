# Spec Review: frontend-location-places-geofences
- Branch: `main` (Frontend and Backend repositories are separate; no feature branch was created)
- Spec resolved via: explicit feature folder from active execution plan
- Resolution conflicts: none
- Review file: `001review.md`
- Detected commands: Backend test=`set JWT_SECRET=&& python -m pytest -q`; Parent/Child test=`gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`; lint=`not configured`; types=`Kotlin compilation via Gradle`

## Summary

- Overall status: **PASS for code and automated validation; Realme policy sync is verified, while a parent-created place and physical transition remain required.**
- High-risk issues: none remaining in the reviewed implementation.
- Missing tests / regression risk: no physical device test can prove actual Geofence delivery or Android background-location flow.
- Test suite results: Backend **278 passed** with `JWT_SECRET` unset; Parent and Child unit tests plus `assembleDebug` passed after the feature changes.
- Lint results: not configured.
- Type check results: Parent and Child Kotlin compilation passed.

## Task-by-task Verification

### Task T001–T002: Specification and compatible contracts
- Spec requirement / acceptance criteria: record privacy boundaries and support `safe`, `attention`, `risk` while preserving legacy `Safe`/`Danger` reads.
- Implementation found: `spec.md`; `app/schemas/place_schemas.py`; `PlaceService._response` and `_legacy_places`; `tests/test_place_service.py`.
- Status: PASS.
- Evidence: legacy zones map safely to safe/risk and never to attention; radius and exit-preference validation are covered.

### Task T003–T006: Backend places and transitions
- Spec requirement / acceptance criteria: parent-owned CRUD, child device active-place read, replay-safe enter/exit handling, and typed Parent Alerts.
- Implementation found: `PlaceService`, `place_routes.py`, `notification_normalization.py`, and `tests/test_place_service.py`.
- Status: PARTIAL.
- Evidence: child/parent ownership and disabled preference cases are tested; typed alert types are normalized and published through `NotificationEventService`.
- Problems: Issue 1 applies because event claim is not atomic under simultaneous requests.

### Task T007–T008: Parent overview and places list
- Spec requirement / acceptance criteria: no coordinates/history, truthful freshness, grouped place cards, RTL Layngo identity and logo.
- Implementation found: `GpsFragment`, `fragment_gps.xml`, `SafeZonesFragment`, `fragment_safe_zones.xml`, `layngo_logo.png`.
- Status: PARTIAL.
- Evidence: raw coordinates are removed from the Parent display and groups use label + icon + description.
- Problems: Issue 3 applies because map labels and density are not yet explicitly styled down as the reference requires.

### Task T009–T010: Parent create/edit flow
- Spec requirement / acceptance criteria: type, map/marker/name, radius/preferences, edit and success without text coordinates.
- Implementation found: `AddZoneFragment`, `PlacePickerFragment`, `PlaceSettingsFragment`, `PlaceSavedFragment`.
- Status: PASS.
- Evidence: type constraints are enforced by backend and type-specific settings hide exit for attention/risk. Parent build passed.

### Task T011–T012: Child synchronization and geofences
- Spec requirement / acceptance criteria: authenticated active-place sync, no initial/dwell transition, boot/pair recovery, truthful permission health and idempotent event upload.
- Implementation found: `PlacePolicySyncWorker`, `PlaceGeofenceManager`, `PlaceTransitionReceiver`, `PlaceTransitionUploadWorker`, manifest, `ServiceWatchdogReceiver`, and `PairingFragment`.
- Status: PARTIAL.
- Evidence: `setInitialTrigger(0)`, supported transition filtering, recurring sync, boot and pairing triggers, and runtime permission gating are present.
- Problems: Issue 2 applies because missing background permission is currently returned as retryable work rather than a settled degraded state.

### Task T013–T015: Inbox and quality gates
- Spec requirement / acceptance criteria: Arabic place alerts, risk-only location action, tests, review gates.
- Implementation found: `ParentInboxPresentation`, `ParentInboxFragment`, `ParentInboxPresentationTest`, `PlaceTransitionDeciderTest`, `test_place_service.py`.
- Status: PASS with the two backend/runtime fixes above pending.
- Evidence: risk opens location; attention does not. No place presentation exposes lat/lon text.

### Task T016: Physical verification
- Spec requirement / acceptance criteria: two-device Android verification of all transitions and permission paths.
- Implementation found: Realme RMX2040 running the Child app with `ACCESS_BACKGROUND_LOCATION` granted and `AppBlockerAccessibilityService` bound.
- Status: PARTIAL.
- Evidence: the published `GET /api/child-devices/places` returned `200` with `place_version: 0` and `places: []`; `PlacePolicySyncWorker` completed with `SUCCESS`. The empty policy correctly leaves no Geofence to register. A Parent-created place, Android registration, and an enter/exit upload remain unverified.

## Issues List (Consolidated)

### Issue 1: Make transition event claiming atomic
- [x] FIXED
- Severity: HIGH
- Depends on: none
- Affected tasks: T004, T006, T013
- Evidence: `app/services/place_service.py::record_transition` executes `event_ref.get()` followed by `event_ref.set()` without a Firestore transaction.
- Root cause analysis: concurrent receivers/retries can both observe no event and both enter the notification publish path, defeating the replay-safe requirement.
- Proposed solution: use an atomic Firestore transaction for production or a bounded atomic create/precondition; keep a FakeDB-compatible fallback only for tests. Have the transaction return whether it claimed the event and publish only for the winner.
- Test plan: `set JWT_SECRET=&& python -m pytest -q tests/test_place_service.py` and full Backend suite.
- Fix notes: `PlaceService._claim_transition_event` now uses a Firestore transaction in production and a FakeDB-only test fallback; the targeted suite passed 5 tests with an explicit test JWT.

### Issue 2: Stop retrying non-transient permission denial
- [x] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: T012
- Evidence: `PlacePolicySyncWorker.apply` returns `false` for both denied permission and registration failure, and `doWork` converts both to `Result.retry()`.
- Root cause analysis: Android background location requires a user decision; retrying cannot fix a deliberate denial and can create avoidable background work.
- Proposed solution: make the manager expose `ACTIVE`, `PERMISSION_DENIED`, and `FAILED`; return `Result.failure()` for denied permission and `Result.retry()` only for transient registration failure. Preserve the truthful preference status.
- Test plan: add JVM decision tests and rerun Child Gradle tests/build.
- Fix notes: `PlaceGeofenceManager.ApplyResult` differentiates active, permission denied, and transient failure; `PlacePolicySyncWorker` returns failure for denial and the Child build/tests passed.

### Issue 3: Apply a low-detail map style to Parent location and picker
- [x] FIXED
- Severity: LOW
- Depends on: none
- Affected tasks: T007, T009
- Evidence: `GpsFragment.onMapReady` and `PlacePickerFragment.onMapReady` disable toolbar/zoom only; neither calls a `MapStyleOptions` style that hides road labels.
- Root cause analysis: the current map preserves Google’s default detailed labels, which conflicts with the supplied calm, low-information reference.
- Proposed solution: add one raw JSON style that hides `labels` and POI/transit density; apply it in both map fragments and keep the marker visible.
- Test plan: Parent build and visual verification on a connected device.
- Fix notes: `location_map_calm.json` hides labels, POIs, and transit labels; both Parent maps load this style before rendering their marker.
- Verification: Parent `testDebugUnitTest assembleDebug` passed after the corrected `com.google.android.gms.maps.model.MapStyleOptions` import.

## Fix Plan (Ordered)
1) Issue 1: Make transition event claiming atomic — protect replay safety under concurrent delivery.
2) Issue 2: Stop retrying non-transient permission denial — persist a degraded state without background retry churn.
3) Issue 3: Apply a low-detail map style — match the supplied calm Parent visual reference.

## Handoff to Coding Model (Copy/Paste)
- Files to edit/create: `place_service.py`, `test_place_service.py`, `PlaceGeofenceManager.kt`, `PlacePolicySyncWorker.kt`, Child tests, `res/raw/location_map_calm.json`, `GpsFragment.kt`, `PlacePickerFragment.kt`.
- Exact behavior changes: claim a transition once atomically, distinguish denied from transient geofence registration, and hide map labels in both Parent maps.
- Edge cases: concurrent same `event_id`, Android background permission denied, empty active-place list, map style unavailable.
- Tests to add/update: backend duplicate/concurrency behavior and Child result-state decision behavior.
- Suggested commit breakdown: Backend replay safety; Child permission health; Parent map treatment; then tests/review docs.
