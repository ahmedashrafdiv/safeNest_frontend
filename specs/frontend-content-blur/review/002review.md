# Spec Review: frontend-content-blur

- Branch: local Frontend working tree; no clean phase baseline is available because unrelated concurrent changes remain unstaged.
- Spec resolved via: explicit inherited feature path `specs/frontend-content-blur`.
- Resolution conflicts: none.
- Review file: `002review.md`.
- Detected commands: test=`gradlew.bat testDebugUnitTest`; lint=`not configured`; types=`not configured`; build=`gradlew.bat assembleDebug`.

## Summary

The Content Blur implementation is **code-review PASS, device-verification PENDING**. The last Child command, `gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`, completed successfully after the fixes in this review cycle. No lint/type-check command is configured in the Android project.

Test Guard reviewed the changed JVM tests. They construct real value objects, assert observable decisions, use no mocks, and cover different regressions rather than duplicate input variants. No Test Guard violation was found. The local equivalent of Code Review found no remaining issue at confidence 80 or higher after the fixes below.

| Review area | Result | Evidence |
|---|---|---|
| Parent-selected target scope survives service start | PASS | `ContentBlurAccessibilityService.onServiceConnected` configures event types first, then loads preferences instead of overwriting custom target packages with defaults. |
| Fail-closed traversal failure | PASS | `collectCandidates` returns full-window coverage when the active window is unavailable or the node walk exhausts its budget. |
| Overlay thread safety | PASS | `BlurOverlayController` marshals render/detach to the main looper. |
| Parent-disable and callback race | PASS | screenshot callbacks are posted to the service main thread and are ignored when the policy version changed or the feature is disabled. |
| Policy binding integrity | PASS | a current version is retained, an older version is ignored, and child/device binding rejection clears local Content Blur state. |
| Lifecycle receiver registration | PASS | `AndroidManifest.xml` uses separate filters so boot/update intents without a package URI match the watchdog. |
| Candidate coverage | PASS | classifier recognises common `TextureView`, `SurfaceView`, player, media-id, Arabic, and English image/video signals without treating unrelated text views as media. |
| Realme runtime behavior | PENDING | The Realme was not connected to ADB during this review. |

## Task-by-task Verification

### Task T1: Fail-closed decision and geometry (FCB-001–FCB-008)

- Spec requirement / acceptance criteria: only verified safe media may reveal; scrolling, stale geometry, invalid frames, and recycled content remain covered.
- Implementation found: `BlurPlanBuilder`, `RegionTracker`, `ScrollStateGate`, `FrameIntegrityChecker`, `RegionVerdictResolver`, and `BlurCoreTest`.
- Status: PASS.
- Evidence: Child JVM suite passed in the final build; existing focused tests cover unknown verdicts, reveal debounce, scrolling, recycling, scheduling, and frame integrity.

### Task T2: Independent Android service and overlay (FCB-009–FCB-013)

- Spec requirement / acceptance criteria: independently scoped service, non-interactive overlay, bounded traversal, and conservative full-window fallback.
- Implementation found: `ContentBlurAccessibilityService`, `BlurOverlayController`, `BlurOverlayView`, `content_blur_accessibility_config.xml`.
- Status: PASS for static/build validation; physical behavior PENDING.
- Evidence: root unavailability and budget expiry now yield a full-window `BlurPlan.conservative`; overlay mutations are main-thread safe; the build passes.

### Task T3: Child policy and lifecycle integration (FCB-014–FCB-017)

- Spec requirement / acceptance criteria: disabled default, device-bound synchronization, clear setup state for the separate service, and recovery after boot/update/revocation.
- Implementation found: `ContentBlurPolicySyncWorker`, `DeviceBindingDecider`, `PrefsHelper`, `PermissionsFragment`, `SetupCapabilityState`, `ServiceWatchdogReceiver`, `AndroidManifest.xml`.
- Status: PASS for static/build validation; physical recovery PENDING.
- Evidence: policy-version equality is now idempotent; older replies preserve newer local state; mismatched device/child replies clear blur state; boot/package filters are separated correctly.

### Task T4: Parent control (FCB-018–FCB-020)

- Spec requirement / acceptance criteria: Arabic Parent controls, selected device scope, clear unavailable/conservative/synchronizing status, and concurrency handling.
- Implementation found: inherited Parent coordinator/UI implementation and Backend effective-policy APIs.
- Status: PASS by existing implementation evidence; no Parent source was changed in this review cycle.

### Task T5: Quality and review gates (FCB-021–FCB-025)

- Spec requirement / acceptance criteria: tests/builds, Test Guard, Spec Review/Review Fix, scope control, and local scoped commit.
- Status: PARTIAL.
- Evidence: Child tests and Debug build pass; Test Guard passes; this review replaces the prior toolchain-blocked review. A local scoped commit remains pending because the Frontend working tree contains unrelated unstaged files and must not be bulk-staged.

### Task T6: Physical verification and closeout (FCB-026–FCB-027)

- Spec requirement / acceptance criteria: Realme evidence for target scope, initial coverage, scrolling, parent toggle synchronization, permission failure, and supported Android mode.
- Status: PENDING.
- Evidence: `adb devices -l` had no connected device when checked. The APK has been rebuilt and is ready for installation.

## Issues List (Consolidated)

No remaining code defect with confidence 80 or higher was found in the reviewed Content Blur scope.

The remaining gates are operational rather than code defects: a clean scoped commit cannot be prepared until the concurrent working-tree changes are separated, and physical Realme verification requires the phone to be connected with USB debugging enabled.

## Fix Plan (Ordered)

1) Connect the Realme and perform the physical acceptance matrix using the newly built Child APK.
2) After device evidence is recorded, identify and stage only Content Blur files for a scoped local commit; do not include IoT, AppBlocker, or removal-protection changes.

## Handoff to Coding Model (Copy/Paste)

- Files changed in this review: `ContentBlurAccessibilityService.kt`, `BlurOverlayController.kt`, `CaptureScheduler.kt`, `ContentBlurPolicySyncWorker.kt`, `DeviceBindingDecider.kt`, `PrefsHelper.kt`, `ProtectionHealthWorker.kt`, `PlacementNodeClassifier.kt`, `AndroidManifest.xml`, and focused tests.
- Edge cases now protected: null active window, node-walk budget exhaustion, service start with custom target packages, parent-disable callback race, stale policy reply, binding mismatch, boot intent matching, and common Android media surfaces.
- Next validation: install the current APK on Realme, re-enable both accessibility services manually, select one Parent target app, and test initial coverage, scrolling, toggle off/on, target removal, overlay revocation, service revocation, reboot, and package update.
