# Requirements Checklist: Layngo Child Device-Wide Content Blur

## Safety and decisions

- [ ] Only `SAFE` can reveal; every other verdict remains covered.
- [ ] Scroll, stale geometry, recycled content, black frames, sentinel contamination, errors, and permission loss are fail-closed.
- [ ] No gender model or unlicensed classifier is shipped.
- [ ] No frame, crop, digest, or classifier signal leaves the child device.

## Android architecture

- [ ] The Content Blur service is independent from `AppBlockerAccessibilityService`.
- [ ] Placement uses Accessibility bounds and analysis uses screenshots.
- [ ] Capture has one in-flight request, respects platform timing, and closes hardware resources.
- [ ] Overlay is one pass-through window and can be detached safely.
- [ ] Unsupported capability resolves to conservative coverage.

## Policy and UX

- [ ] Parent controls the device-scoped toggle and target package list.
- [ ] Child defaults disabled until an owned valid policy is synchronized.
- [ ] Parent shows enabled, syncing, unavailable, conservative, conflict, and failure states in Arabic RTL.
- [ ] Layngo, launcher, system UI, and input-method packages are excluded.

## Quality and scope

- [ ] Pure logic has focused JVM tests.
- [ ] Parent and Child Debug builds pass.
- [ ] Existing Child blocking/removal changes remain untouched and unstaged.
- [ ] No IoT or Sensor path changes.
- [ ] No push, deploy, or APK install is performed during the local implementation gate.
