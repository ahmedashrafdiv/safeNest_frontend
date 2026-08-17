# Spec Review: frontend-child-app-removal-protection
- Branch: main
- Spec resolved via: explicit feature directory
- Resolution conflicts: .specify/feature.json points to phone-location; explicit feature directory was used.
- Review file: 002review.md
- Detected commands: test=`$env:JAVA_HOME="D:\Android\jdk\temurin-17"; `$env:ANDROID_HOME="D:\Android\sdk"; .\gradlew.bat testDebugUnitTest --no-daemon --console=plain` lint=`not configured` types=`not configured`

## Summary
- Overall status: PARTIAL
- High-risk issues: none for the managed-policy fail-closed path.
- Missing tests / regression risk: no physical Device Owner/Profile Owner test yet; FT005 remains open.
- Test suite results: `testDebugUnitTest` and `assembleDebug` passed.
- Lint results: not configured.
- Type check results: not configured.

## Task-by-task Verification
### Task FT003: Add DeviceAdmin/DPC capability detection
- Status: PASS
- Evidence: `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/security/LayngoDeviceAdminReceiver.kt` and `DeviceManagementHelper.kt` use Android DevicePolicyManager owner/admin checks. Consumer mode does not claim management.

### Task FT004: Add confirmed uninstall-policy and lock-task status reporting
- Status: PASS
- Evidence: `ProtectionPolicyManager.apply()` calls `setUninstallBlocked` and `setLockTaskPackages` only after owner confirmation, then re-reads state and reports success only when uninstall blocking is confirmed. MainActivity invokes the manager at startup.

### Task FT005: Add tests for unsupported and policy-failure paths
- Status: PARTIAL
- Evidence: pure state tests cover consumer, owner without confirmed uninstall policy, and stale fail-closed states. There is no physical managed-device test or instrumentation test proving Android policy application.
- Proposed fix: add a device/instrumentation verification checklist and, if a managed test device is available, verify Device Owner/Profile Owner provisioning, uninstall attempt, and lock-task behavior.

## Issues List (Consolidated)
### Issue 1: Managed-device behavior is not physically verified
- [ ] FIXED
- Severity: MED
- Depends on: none
- Affected tasks: FT005
- Evidence: no instrumentation or paired-device result exists for DevicePolicyManager policy application.
- Root cause analysis: Android owner state and OEM behavior cannot be proven by JVM unit tests.
- Proposed solution: add physical-device verification in the next gate; do not mark guaranteed protection until it passes.
- Test plan: provision a dedicated test phone, run the managed onboarding flow, verify uninstall blocking and Lock Task, then rerun `assembleDebug`.

## Fix Plan (Ordered)
1) Issue 1: Managed-device behavior is not physically verified — complete the dedicated-device test gate.

## Handoff to Coding Model
- Continue with recovery/health contracts only; do not introduce hidden identity or Accessibility system-screen automation.
- Keep `ProtectionPolicyManager` fail-closed and preserve the consumer-mode fallback.
