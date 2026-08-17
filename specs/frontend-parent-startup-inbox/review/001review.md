# Spec Review: frontend-parent-startup-inbox

- Branch: `main`
- Spec resolved via: explicit spec path
- Detected commands: Parent=`gradlew.bat assembleDebug --no-daemon`; Backend=`python -m pytest -q`

## Summary

- Overall status: PARTIAL
- High-risk issues: No confirmed compile or API-contract issue after the successful Parent build.
- Missing verification: No connected Android device was available for visual startup, RTL, or live Allow/Reject verification; no dedicated Parent ViewModel tests are configured.
- Test results: Parent build succeeded; Backend `175 passed, 0 failed`.

## Task verification

### T1: Contracts and data state
- Status: PASS
- Evidence: `ApiModels.kt`, `SafeNestApiService.kt`, and `ParentInboxRepository.kt` define pending request list, approve/reject, alert resolution, child discovery, and partial-data loading.

### T2: Inbox UI and interactions
- Status: PASS
- Evidence: `fragment_parent_inbox.xml` and `ParentInboxFragment.kt` provide RTL cards, pending count, request-first ordering, Allow/Reject actions, alert review, loading, empty, retry, close, and Home continuation.

### T3: Startup routing
- Status: PASS
- Evidence: `MainActivity.showStartupInbox()` is used for existing authenticated sessions and post-login flow; an empty successful inbox returns to Home.

### T4: Verification
- Status: PARTIAL
- Evidence: Parent debug build and Backend suite passed. Live device and visual verification remain open because no Android device was connected.

## Issues List (Consolidated)

### Issue 1: Live startup and decision-action verification is pending
- [ ] FIXED
- Severity: LOW
- Depends on: none
- Affected tasks: Verification manual-device task
- Evidence: No ADB-connected device was available during this implementation.
- Root cause: Environment limitation, not a confirmed code failure.
- Proposed solution: Install the APK on a logged-in Parent device and verify startup inbox, RTL layout, Allow, Reject, close, retry, and no-records fallback.
- Test plan: Parent `assembleDebug` plus manual device matrix.
- Notes: Do not claim visual/device success from a local build alone.

## Fix Plan (Ordered)

1) Issue 1: Live startup and decision-action verification is pending — perform manual device verification when hardware is connected.
