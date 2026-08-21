# Tasks: Layngo Child Device-Wide Content Blur

## Phase 0: Preflight and capability

- [x] FCB-001 Record Android version, accessibility/screenshot/overlay capability, target package defaults, and excluded concurrent Child files.
- [x] FCB-002 Define pure models for bounds, candidates, tracked regions, verdicts, scroll state, blur plans, and capability state.
- [x] FCB-003 Define the fail-closed contract: only `SAFE` may reveal and every other state covers.

## Phase 1: Pure decision and geometry layer

- [x] FCB-004 Implement `BlurScopeDecider` for target package sets, split-screen visible packages, and protected package exclusions.
- [x] FCB-005 Implement `RegionGeometry`, `PlacementNodeClassifier`, and stable content keys without Android graphics types in the pure layer.
- [x] FCB-006 Implement `ScrollStateGate`, `CaptureScheduler`, `RegionTracker`, and verdict hysteresis with stale/recycled-region demotion.
- [x] FCB-007 Implement `FrameIntegrityChecker`, `RegionVerdictResolver`, `BlurPlanBuilder`, and conservative failure handling.
- [x] FCB-008 Add focused JUnit tests for fail-closed decisions, scrolling, geometry, recycling, timing, and frame-integrity cases.

## Phase 2: Independent Android service and overlay

- [x] FCB-009 Add `ContentBlurAccessibilityService` without modifying `AppBlockerAccessibilityService`.
- [x] FCB-010 Add accessibility XML capability configuration with screenshot support and runtime target-package updates.
- [x] FCB-011 Add a single pass-through `BlurOverlayController` and `BlurOverlayView` with sentinel rendering and safe teardown.
- [x] FCB-012 Add screenshot capture shell with API capability resolution, window-capture preference, conservative fallback, and guaranteed buffer cleanup.
- [x] FCB-013 Add bounded accessibility-tree walking and whole-window coverage when the time budget is exceeded.

## Phase 3: Child policy and lifecycle integration

- [x] FCB-014 Add Content Blur preference keys, disabled/default behavior, policy clearing, and capability reporting without changing existing service helpers.
- [x] FCB-015 Add policy API models and Child synchronization through the existing worker/FCM update path.
- [x] FCB-016 Add permissions/onboarding visibility for the separate Content Blur Accessibility service and overlay requirements.
- [x] FCB-017 Add service watchdog/lifecycle handling for reboot, process death, revoked permissions, and parent disable.

## Phase 4: Parent control

- [x] FCB-018 Add Parent API models and a scoped policy coordinator with `Applied`, `Blocked`, and `Failed` outcomes.
- [x] FCB-019 Add an Arabic RTL Layngo Content Blur toggle and target-package controls to the Parent monitoring/settings flow.
- [x] FCB-020 Render clear unavailable/conservative/synchronizing states and preserve optimistic-concurrency conflict handling.

## Phase 5: Quality and review

- [ ] FCB-021 Run Child focused tests, Parent focused tests, and both Debug builds.
- [ ] FCB-022 Run test-guard on changed tests and correct must-fix findings without deleting regression coverage.
- [ ] FCB-023 Run spec-review and review-fix for every open finding, then perform production-code review.
- [ ] FCB-024 Verify changed-file scope and keep concurrent AppBlocker/removal-protection changes unstaged.
- [ ] FCB-025 Commit only scoped Frontend Content Blur changes locally; do not push or install until separately confirmed.

## Phase 6: Review and closeout

- [ ] FCB-026 Perform physical verification on the Realme for conservative coverage, target scope, toggle synchronization, scroll behavior, and permission failure.
- [ ] FCB-027 Record evidence, supported Android modes, remaining classifier/license limitations, and local commit SHA.
