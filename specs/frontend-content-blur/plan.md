# Implementation Plan: Layngo Child Device-Wide Content Blur

## Architecture

The feature is divided into a pure Kotlin decision layer and thin Android shells. Pure classes use `RegionBounds` rather than `android.graphics.Rect`, accept injected clocks where timing matters, and can run under the repository's existing JUnit setup. Android-specific code owns Accessibility nodes, screenshot callbacks, overlay windows, hardware buffers, preferences, and lifecycle.

## Fail-closed flow

1. `ContentBlurAccessibilityService` receives events only for runtime-selected target packages.
2. The service walks visible media candidates and immediately sends them to the overlay plan as covered.
3. `ScrollStateGate` keeps all candidates covered while scrolling and waits for settling.
4. `CaptureScheduler` allows at most one capture in flight and respects the platform interval.
5. `FrameIntegrityChecker` rejects black, sentinel-dominated, stale, or invalid frames.
6. The classifier layer produces a verdict; `BlurPlanBuilder` reveals only a valid, current, stable `SAFE` region.
7. Any movement, content-hash change, timeout, permission loss, capture error, or unknown verdict demotes the region to covered.

## Android capability modes

Capability resolution will use OS version, `canTakeScreenshot`, overlay permission, Accessibility status, parent policy, and the ability to isolate the capture from the overlay. API 34+ will prefer window-level capture when available. Unsupported or uncertain devices use conservative coverage, never an unsafe reveal. MediaProjection is not included in this iteration.

## Policy integration

Parent writes a device-scoped policy through Backend. Child consumes it via the existing synchronization mechanisms and keeps a disabled default. Policy changes update target package scope at runtime; disabling the feature removes overlays and narrows the service's event package list to Layngo's own package.

## Parent experience

The Parent UI will use the existing manual fragment navigation and Layngo colors. The feature appears as an Arabic RTL control with explicit states for enabled, syncing, unavailable, conservative, permission-required, conflict, and failed. Package selection will be constrained to known installed/allowed packages and will never include Layngo, launcher, system UI, or input methods.

## Verification

Pure tests cover all state transitions and geometry. Android projects receive focused JVM tests and Debug builds. Physical verification demonstrates conservative coverage and lifecycle behavior on the Realme; real analysis is reported only if the measured Android capability and licensed model path support it.
