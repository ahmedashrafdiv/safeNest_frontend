# State: Layngo Child Device-Wide Content Blur

## Current phase

Preflight/specification is complete. Implementation begins at Phase 1 after this state is recorded.

## Repository baseline

The Frontend repository contains unrelated uncommitted changes in the Child application under the application-blocking/removal-protection workstream. Those files are explicitly out of scope and must not be staged or modified. The Content Blur work must use path-scoped staging only.

The Content Blur `spec.md` existed before this run, but its tasks, plan, state, and review artifacts were incomplete. This run adds the missing Spec Kit files and keeps the prior requirements as the functional baseline.

## Toolchain

Parent and Child are Kotlin Android projects built with the repository-provided Gradle wrapper. Focused and complete JVM tests will run through the existing Gradle test tasks, followed by Debug assembly. Physical verification uses the connected Realme RMX2040 and its existing ADB serial.

## Safety decisions

No IoT or Sensor path may be touched. The existing `AppBlockerAccessibilityService` and related concurrent removal-protection files remain out of scope. Gender inference and NSFW classification are not implementation prerequisites; the fail-closed face-presence/conservative path comes first. No GitHub push, server deployment, or APK installation is part of the local implementation gate.

## Known risks

The device Android version and screenshot-window capability must be measured before enabling any real analysis path. Android 11–13, `FLAG_SECURE`, revoked overlay/accessibility permissions, black frames, stale geometry, and overlay feedback must all resolve to continued coverage.
