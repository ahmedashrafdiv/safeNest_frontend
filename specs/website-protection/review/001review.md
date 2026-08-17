# Spec Review: website-protection
- Branch: local working tree
- Spec resolved via: explicit feature path `specs/website-protection`
- Resolution conflicts: none
- Review file: `001review.md`
- Detected commands: test=`Child .\gradlew.bat testDebugUnitTest`; lint=`not configured`; types=`not configured`

## Summary

- Overall status: PARTIAL
- High-risk issues: Real Android VPN consent, foreground-service behavior, DNS interception, and deployed Parent/Child synchronization remain unverified on physical devices.
- Missing tests / regression risk: Pure Child policy-engine tests exist; packet-level VPN and full Android worker integration tests are not available in the current local suite.
- Test suite results: Child unit tests passed during `testDebugUnitTest`; Parent and Child debug APK builds succeeded.
- Lint results: not configured.
- Type check results: not configured.

## Task-by-task Verification

### Task T1: Parent policy management
- Spec requirement / acceptance criteria: Parent can select allowlist/blocklist mode, manage categories and hosts, set budgets, assign and publish policies, and explain the mode defaults.
- Implementation found:
  - Files: `app_father/SafeNest/app/src/main/java/com/example/safenest/fragments/WebsiteProtectionFragment.kt`, `WebsiteProtectionViewModel.kt`, `WebsitePolicyRepository.kt`, `network/ApiModels.kt`, `network/SafeNestApiService.kt`.
  - Key symbols: policy mode selector, category controls, host rule input, assignment and publish actions.
- Status: PASS
- Evidence: Parent screen and networking layers are present and the Parent APK builds successfully.

### Task T2: Child synchronization and offline policy evaluation
- Spec requirement / acceptance criteria: Child discovers an assignment, stores a complete snapshot, acknowledges the exact version/hash, and evaluates mode, category, host, and budget rules offline.
- Implementation found:
  - Files: `WebsitePolicySyncWorker.kt`, `PrefsHelper.kt`, `WebsitePolicyEngine.kt`.
  - Key symbols: assignment discovery, snapshot persistence, acknowledgement payload, `WebsitePolicyEngine.decide`.
- Status: PASS
- Evidence: Child unit tests cover allowlist, blocklist, mandatory adult category, and daily budget behavior. The Child APK builds successfully.

### Task T3: DNS VPN enforcement
- Spec requirement / acceptance criteria: Child uses Android `VpnService` or reports unsupported/unavailable state, with host-level disclosure and no claim of exact path filtering.
- Implementation found:
  - Files: `WebsiteDnsVpnService.kt`, `AndroidManifest.xml`, `fragment_permissions.xml`, `PermissionsFragment.kt`.
  - Key symbols: `BIND_VPN_SERVICE`, `VpnService.prepare`, foreground service, IPv4/UDP DNS parsing, NXDOMAIN response, protected upstream socket.
- Status: PASS
- Evidence: Service registration, consent UI, foreground startup, health persistence, and DNS-only disclaimer are present; compilation succeeded.

### Task T4: Truthful degraded status and lifecycle recovery
- Spec requirement / acceptance criteria: Denied consent, establishment failure, missing policy, or another VPN must not appear as active protection; boot and app replacement attempt recovery.
- Implementation found:
  - Files: `HomeFragment.kt`, `PrefsHelper.kt`, `ServiceWatchdogReceiver.kt`, `WebsitePolicySyncWorker.kt`.
  - Key symbols: `WEBSITE_VPN_ACTIVE`, `WEBSITE_VPN_DENIED`, `WEBSITE_VPN_FAILED`, home status rendering, boot recovery.
- Status: PARTIAL
- Evidence: Code reports explicit health states and separates app status from website status. Another VPN and real Android establishment behavior still require hardware verification.

### Task T5: Android build and unit validation
- Spec requirement / acceptance criteria: Child tests/build and Parent build are green.
- Implementation found:
  - Files: Child and Parent Gradle projects.
- Status: PASS
- Evidence: Child `testDebugUnitTest assembleDebug` and Parent `assembleDebug` completed successfully in the local Windows Android environment.

### Task T6: Live device verification
- Spec requirement / acceptance criteria: Validate pairing, policy sync, VPN consent, actual domain blocking, mode behavior, categories, budgets, and unavailable states on devices.
- Implementation found:
  - Files: `specs/website-protection/tasks.md`, `APP_BLOCKING_INTEGRATION_CONTRACT.md`.
- Status: UNKNOWN
- Evidence: No physical paired-device session was available in the local validation environment.
- Problems: Build success and pure engine tests cannot prove that Android routes DNS through the TUN interface or that the deployed API and FCM/WorkManager timing behave correctly.
- Proposed fix: Run the live paired-device checklist and capture logs for every state transition.
- Proposed tests: Install APKs, pair devices, publish policies, verify sync/acknowledgement, test allowed and blocked domains, test denial/another-VPN degradation, and verify category/budget outcomes.

## Issues List (Consolidated)

### Issue 1: Live Android VPN and paired-device verification is pending
- [ ] FIXED
- Severity: HIGH
- Depends on: none
- Affected tasks: T3, T4, T6
- Evidence (paths/symbols): `WebsiteDnsVpnService.kt`, `WebsitePolicySyncWorker.kt`, `HomeFragment.kt`, `specs/website-protection/tasks.md`.
- Root cause analysis: Android VPN consent, packet routing, upstream DNS behavior, another-VPN conflict, and deployed authentication are runtime boundaries that local JVM tests and APK compilation cannot prove.
- Proposed solution (detailed steps): Install current Parent and Child APKs, pair them, publish an allowlist policy with one allowed host and one unknown host, verify unknown-host blocking; publish a blocklist policy and verify unknown-host openness; test mandatory category and budget; deny consent or activate another VPN and verify the Child displays unavailable rather than active.
- Test plan (exact commands): Child `.\gradlew.bat testDebugUnitTest`; Parent `.\gradlew.bat assembleDebug`; then manual device verification with Android logs and DNS observations.
- Notes / tradeoffs: The DNS-only design cannot guarantee HTTPS path filtering or exact browser-tab closure, and that limitation is intentionally disclosed in the UI and contract.

## Fix Plan (Ordered)

1) Issue 1: Live Android VPN and paired-device verification is pending — complete the physical-device and deployed-integration checklist before release.

## Handoff to Coding Model

- Files to edit/create: none for this blocked verification issue unless device testing exposes a reproducible defect.
- Exact behavior changes: none until runtime evidence identifies a failure.
- Edge cases: denied consent, another active VPN, no assignment, stale snapshot, DNS timeout, allowlist unknown host, and blocklist unknown host.
- Tests to add/update: add packet-level or worker integration coverage if a deterministic local seam is introduced.
- Suggested commit breakdown: keep the implementation and spec-artifact commits local; record live verification separately.
