# Spec Review: frontend-content-blur
- Branch: unavailable — `frontend_app` is not a Git working tree on the connected machine
- Spec resolved via: explicit inherited feature path `specs/frontend-content-blur`
- Resolution conflicts: none
- Review file: `001review.md`
- Detected commands: test=`./gradlew testDebugUnitTest` lint=`not configured` types=`not configured`; build=`./gradlew assembleDebug`

## Summary
- Overall status: PARTIAL
- High-risk issues: JVM/Android validation could not run because the connected Windows machine has no `java`/`JAVA_HOME`; physical ADB verification is unavailable because `adb` is not installed on PATH.
- Missing tests / regression risk: readiness tests now cover Content Blur disabled/enabled states, but Android integration tests for `PermissionsHelper` and watchdog behavior are not present.
- Test suite results: not run; Gradle stopped before execution because Java is unavailable.
- Lint results: not configured.
- Type check results: not configured.

## Task-by-task Verification

### Task T1: Pure decision and geometry layer (FCB-001–FCB-008)
- Spec requirement / acceptance criteria: fail-closed verdicts, geometry tracking, scroll gating, stale/recycled demotion, frame-integrity handling, and focused JVM coverage.
- Implementation found:
  - Files: Child pure decision/geometry package and its JVM tests, as referenced by `tasks.md`.
  - Key symbols: `BlurScopeDecider`, `RegionGeometry`, `ScrollStateGate`, `FrameIntegrityChecker`, `RegionVerdictResolver`, `BlurPlanBuilder`.
- Status: PASS by existing task evidence; not re-executed in this review because the current blocker is the unavailable Gradle/JDK toolchain.
- Evidence: `specs/frontend-content-blur/tasks.md` marks FCB-001–FCB-008 complete; the inherited implementation summary reports focused JVM tests were added.
- Problems: runtime test evidence is unavailable in this environment.
- Proposed fix: install/use a JDK on the development machine, rerun the detected Gradle test command, and attach the result to close the gate.
- Proposed tests: `./gradlew testDebugUnitTest`.

### Task T2: Dedicated Android service and overlay (FCB-009–FCB-013)
- Spec requirement / acceptance criteria: independent accessibility service, runtime capability configuration, pass-through overlay, conservative capture fallback, bounded tree traversal, and whole-window coverage on budget exhaustion.
- Implementation found:
  - Files: `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/service/ContentBlurAccessibilityService.kt`, related overlay/capture classes, Android accessibility XML, and manifest.
  - Key symbols: `ContentBlurAccessibilityService`, `BlurOverlayController`, capture shell, bounded tree walker.
- Status: PASS by source/task evidence; runtime validation remains UNKNOWN.
- Evidence: `tasks.md` marks FCB-009–FCB-013 complete; manifest registers `ContentBlurAccessibilityService` separately from `AppBlockerAccessibilityService`.
- Problems: Android build and physical verification are not available here.
- Proposed fix: run `./gradlew assembleDebug` and verify on the Realme with overlay permission and both accessibility services enabled.
- Proposed tests: focused JVM suite plus physical conservative-coverage scenarios.

### Task T3: Child policy, onboarding, and lifecycle integration (FCB-014–FCB-017)
- Spec requirement / acceptance criteria: Content Blur disabled by default, policy sync and clearing, a visible second accessibility step when enabled, overlay requirement, and lifecycle recovery after boot/package replacement with permission-loss notification.
- Implementation found:
  - Files: `PermissionsHelper.kt`, `SetupCapabilityState.kt`, `SetupCapabilityProvider.kt`, `PermissionsFragment.kt`, `fragment_permissions.xml`, `item_setup_step_content_blur.xml`, `ServiceWatchdogReceiver.kt`, `ContentBlurPolicySyncWorker.kt`.
  - Key symbols: `hasContentBlurAccessibilityService`, `CONTENT_BLUR_ACCESSIBILITY`, `contentBlurRequired`, `ContentBlurPolicySyncWorker.enqueueImmediate`, `ServiceWatchdogReceiver`.
- Status: PARTIAL.
- Evidence: the new setup card is included in `fragment_permissions.xml`; `PermissionsFragment` binds the new status/action; Provider reads the service and policy state; watchdog schedules immediate/periodic policy sync and checks both services. The evaluator keeps Content Blur optional when disabled and baseline-blocking when enabled but not ready.
- Problems: the Gradle build could not run. There is no Android instrumentation test proving that `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` parsing recognizes the new component, and no test proving watchdog notification/recovery behavior. The watchdog cannot programmatically enable an AccessibilityService (Android does not permit that); it can only resync and notify, so the requirement should be understood as recovery guidance rather than automatic permission restoration.
- Proposed fix: run the Android build; add focused JVM tests around the readiness evaluator (already partially added) and, where practical, a testable pure helper for service-component membership. Verify boot, package replacement, disabled policy, revoked service, and re-enabled service on a real device.
- Proposed tests: `./gradlew testDebugUnitTest`, then physical reboot/package-update/revocation checks.

### Task T4: Parent control and scoped policy (FCB-018–FCB-020)
- Spec requirement / acceptance criteria: per-device parent toggle and target package selection, explicit source state, conservative/unavailable/synchronizing states, and 409 conflict handling.
- Implementation found:
  - Files: Parent monitoring/control and Backend policy override files described in the inherited implementation summary.
  - Key symbols: scoped policy coordinator, Parent Content Blur controls, device-scoped override API.
- Status: PASS by inherited evidence; backend reported 271 passing tests.
- Evidence: inherited task summary states Parent controls and Backend policy APIs were implemented and verified.
- Problems: no fresh Parent build/test run was possible from this machine.
- Proposed fix: run the Parent focused test/build commands from the repository's configured environment and verify 409 UI behavior with a real stale version.
- Proposed tests: Parent focused test suite and Debug build.

### Task T5: Quality gates and scope (FCB-021–FCB-025)
- Spec requirement / acceptance criteria: Child/Parent tests and builds, test-guard, spec-review/review-fix, scope verification, and local scoped commit.
- Implementation found:
  - Files: changed Child Content Blur sources and `SetupCapabilityEvaluatorTest.kt`.
- Status: UNKNOWN/BLOCKED.
- Evidence: test-guard review of the changed test finds no evident mock, duplication, or implementation-detail assertion issue; however, Gradle cannot start because Java is missing. `git status` cannot be evaluated from `D:\full_safenest` because that directory is not itself a Git working tree, and no nested `.git` directory was found by the current probe.
- Problems: no executable test/build evidence and no local commit SHA.
- Proposed fix: provide a JDK/Gradle-capable Android environment and identify the actual repository root before committing. Do not stage IoT, AppBlocker, or removal-protection changes.
- Proposed tests: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` from the actual Child repository root.

### Task T6: Physical verification and closeout (FCB-026–FCB-027)
- Spec requirement / acceptance criteria: Realme verification of conservative coverage, target scope, sync toggle, scrolling, permission failure, evidence, supported modes, limitations, and commit SHA.
- Implementation found:
  - Files: physical device and APK installation workflow; no current ADB executable available.
- Status: BLOCKED.
- Evidence: `adb devices -l` failed with “adb is not recognized”; the Gradle build also failed before execution with “JAVA_HOME is not set and no `java` command could be found”.
- Problems: no honest physical result can be claimed without the device toolchain.
- Proposed fix: restore JDK and Android platform-tools on the development machine, connect the Realme, install the new Child APK, manually toggle both Accessibility entries after installation, then execute the acceptance matrix.
- Proposed tests: target app scope, initial covered state, SAFE reveal, scroll coverage, policy disable teardown, revoked overlay/accessibility, reboot, and package replacement.

## Issues List (Consolidated)

### Issue 1: Android build/test toolchain unavailable
- [ ] FIXED
- Severity: BLOCKER
- Depends on: none
- Affected tasks: FCB-021, FCB-022, FCB-023, FCB-026, FCB-027
- Evidence (paths/symbols): connected Windows environment; `./gradlew testDebugUnitTest` failed because `JAVA_HOME` and `java` are absent.
- Root cause analysis: the project has a Gradle wrapper but the machine used for execution has no JDK configured.
- Proposed solution: install or point `JAVA_HOME` to a supported JDK, rerun `testDebugUnitTest` and `assembleDebug`, record exact pass/fail output, then update this report.
- Test plan: `./gradlew testDebugUnitTest`; `./gradlew assembleDebug`.
- Notes / tradeoffs: installing a JDK is smaller than Android Studio and does not require changing application code.

### Issue 2: Physical verification toolchain/device unavailable
- [ ] FIXED
- Severity: HIGH
- Depends on: Issue 1
- Affected tasks: FCB-026, FCB-027
- Evidence (paths/symbols): `adb devices -l` failed because `adb` is not installed or not on PATH.
- Root cause analysis: Android platform-tools/device connection are unavailable in the current execution environment.
- Proposed solution: install/configure platform-tools, connect the Realme with USB debugging, install the built APK, manually re-toggle both accessibility services, and record evidence for each acceptance scenario.
- Test plan: `adb devices -l`, APK install, then the physical acceptance matrix in T6.
- Notes / tradeoffs: Android cannot enable an AccessibilityService programmatically; manual toggling after APK install remains required.

### Issue 3: Missing automated coverage for second accessibility service and watchdog
- [ ] FIXED
- Severity: MED
- Depends on: Issue 1
- Affected tasks: FCB-016, FCB-017, FCB-022, FCB-023
- Evidence (paths/symbols): `PermissionsHelper.kt::hasContentBlurAccessibilityService`; `ServiceWatchdogReceiver`; `SetupCapabilityEvaluatorTest.kt` currently covers evaluator behavior but not Settings parsing or notification scheduling.
- Root cause analysis: Android-bound permission and receiver behavior were implemented without a dedicated test seam.
- Proposed solution: extract a small pure component-membership helper if needed, add tests for enabled/disabled component strings and readiness transitions, and validate WorkManager/notification behavior through Android tests or controlled physical verification.
- Test plan: `./gradlew testDebugUnitTest`; Android/instrumentation test where the project supports it.
- Notes / tradeoffs: do not mock state objects; test observable readiness and recovery outcomes at the boundary.

## Fix Plan (Ordered)
1) Issue 1: Android build/test toolchain unavailable — configure JDK and rerun Gradle tests/build.
2) Issue 2: Physical verification toolchain/device unavailable — configure platform-tools and verify on Realme.
3) Issue 3: Missing automated coverage for second accessibility service and watchdog — add focused tests after the build gate is available.

## Handoff to Coding Model (Copy/Paste)
- Files to edit/create: `PermissionsHelper.kt`, `SetupCapabilityState.kt`, `SetupCapabilityProvider.kt`, `PermissionsFragment.kt`, `ServiceWatchdogReceiver.kt`, `SetupCapabilityEvaluatorTest.kt`, and the review report.
- Exact behavior changes: keep Content Blur off by default; when parent policy enables it, show the separate Accessibility setup card, require the service for readiness, resync policy after lifecycle events, and notify if Android has revoked either required service.
- Edge cases: policy disabled while service remains enabled; service revoked after reboot; stale policy; overlay permission revoked; Android versions without screenshot capability; missing device toolchain.
- Tests to add/update: readiness on/off transitions, component parsing, watchdog observable outcomes, plus physical acceptance checks.
- Suggested commit breakdown: one local commit for Child Content Blur onboarding/watchdog, one for tests/review evidence; never push or install without explicit confirmation.
