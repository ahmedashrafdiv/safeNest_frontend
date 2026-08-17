# Frontend Website Protection Tasks

## Phase 1: Parent policy management

- [x] Add Parent API models for website policies, rules, publishing, and assignments.
- [x] Add Retrofit endpoints, repository, and ViewModel operations.
- [x] Build the Parent website-protection screen with allowlist/blocklist selection.
- [x] Add mandatory category selection, custom host rules, per-site budget input, assignment, and publish actions.
- [x] Explain the different defaults of allowlist and blocklist modes in the UI.

## Phase 2: Child synchronization and decision engine

- [x] Add Child assignment discovery, snapshot download, atomic preference storage, and acknowledgement.
- [x] Add `WebsitePolicyEngine` tests for allowlist, blocklist, mandatory categories, and daily budgets.
- [x] Keep website enforcement separate from the existing AccessibilityService app blocker.
- [x] Add FCM, boot, package-event, and periodic synchronization triggers.

## Phase 3: Android DNS VPN enforcement

- [x] Register `WebsiteDnsVpnService` with `BIND_VPN_SERVICE`.
- [x] Implement IPv4/UDP DNS packet parsing and response wrapping.
- [x] Return NXDOMAIN for blocked hosts and forward allowed DNS through a protected resolver socket.
- [x] Add foreground-service startup, VPN consent checks, health persistence, and boot recovery.
- [x] Add onboarding explanation and VPN consent action.
- [x] Add a home-screen status that distinguishes active, denied, failed, unavailable, and waiting-for-policy states.
- [x] Disclose that filtering is host-level and does not guarantee HTTPS path filtering or exact tab closure.

## Phase 4: Validation and release gate

- [x] Run Child unit tests and debug APK build.
- [x] Run Parent debug APK build.
- [x] Record current successful commands: Child `testDebugUnitTest assembleDebug`; Parent `assembleDebug`.
- [ ] Install both APKs on paired devices and verify VPN consent and actual domain blocking.
- [ ] Verify allowlist/blocklist website behavior, category blocking, daily budget exhaustion, and degraded status with another VPN active.
