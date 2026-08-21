# Feature Specification: Layngo Child Device-Wide Content Blur

## Purpose

Inappropriate imagery reaches a child inside applications the parent has otherwise allowed, where host-level DNS filtering and application blocking cannot help. Layngo Kids must cover on-screen media inside a parent-selected set of applications, analyze it entirely on the device, and reveal only what is proven safe. Coverage is the default state and revealing is the exception, so processing latency can delay a reveal but can never cause an exposure. The parent turns the capability on from Layngo Parent per device.

## Functional Requirements

- FR-001: A media region is covered by default and is revealed only after a `SAFE` verdict; `UNSAFE`, `UNKNOWN`, in-flight, expired, and drifted states all remain covered.
- FR-002: Verdicts are three-valued (`UNSAFE`, `UNKNOWN`, `SAFE`); `UNKNOWN` is the initial state and the sink for every failure path, and it demotes a previously revealed region back to covered.
- FR-003: Enforcement runs in a new dedicated AccessibilityService; the existing `AppBlockerAccessibilityService`, `BlockedAppActivity`, and `AppPolicyDecider` are not modified, and a failure in blur never disables application blocking.
- FR-004: Cover placement is derived from accessibility node bounds without capturing pixels, and classification is derived from screen captures; the two paths are independent so that placement continues to track content when classification is unavailable.
- FR-005: While scroll events are arriving, every media candidate is covered and no classification runs; classification resumes only after the scroll settles.
- FR-006: A region whose accessibility identity matches but whose content changed is treated as recycled and its verdict resets to `UNKNOWN`, so that list recycling cannot reveal unclassified content through a stale verdict.
- FR-007: A verdict is valid only for the geometry and pixels it was computed from; bounds drift beyond tolerance, verdict expiry, or a changed pixel digest each return the region to covered.
- FR-008: The overlay is a single non-interactive window that passes all touch input through to the underlying application and never intercepts gestures.
- FR-009: Capability is resolved at runtime from OS version, granted permissions, and the parent toggle; when classification cannot exclude the overlay from captures the device runs in conservative mode, covering media without classifying, and reports that state to the parent.
- FR-010: Every captured crop is verified for integrity before classification; blank frames from secure surfaces and frames dominated by the overlay sentinel colour yield `UNKNOWN` and never `SAFE`.
- FR-011: The capability is disabled by default and is enabled per device by the owning parent; an unbound, mismatched, or stale policy is rejected and leaves the capability off.
- FR-012: Enforcement applies only to parent-selected target packages; outside those packages the service receives no events, and Layngo's own packages, the launcher, system UI, and input methods are never covered.
- FR-013: All capture, detection, and classification happen on the device; no frame, crop, digest, or derived signal is written to disk or transmitted.
- FR-014: Accessibility event handling stays within its main-thread time budget; when the node walk is truncated by that budget the entire window is covered rather than assumed safe.
- FR-015: No IoT or sensor controller, route, schema, or test is changed, and no website-filtering, location, screen-time, or device-admin behavior is altered.

## Acceptance Criteria

| Scenario | Expected result |
|---|---|
| Target application opened | Media regions are covered before any classification result exists. |
| Safe photograph, screen settled | The region is revealed after consecutive safe observations clear the reveal debounce. |
| Fast scroll through a feed | Every media candidate stays covered for the whole scroll and no classification runs. |
| List row recycled into a revealed region | The region returns to covered until it is reclassified. |
| Revealed region moves before the cover is drawn | The region returns to covered rather than following stale geometry. |
| Overlay captured inside its own screenshot | The region remains covered; it is never wrongly revealed. |
| Secure surface returning a blank frame | The package degrades to conservative coverage and capture stops for it. |
| Non-target application in the foreground | No events are delivered, no capture occurs, and no overlay is attached. |
| Overlay permission revoked while running | Capture stops, status reports the denial, and no content is revealed. |
| Parent disables the capability | Coverage stops, the overlay detaches, and the service becomes structurally idle. |
| Device below the supported OS version | The capability reports itself unavailable instead of failing silently. |
| Existing enforcement suites | Application blocking, website filtering, location, and screen-time tests remain unaffected. |

## Clarifications

- [ASSUMED] Detection scope: this feature ships face-presence coverage, where any detected face keeps its region covered. This already satisfies covering every woman on screen; it also covers men, which is accepted as the fail-closed trade-off.
- [ASSUMED] Gender classification is deliberately excluded from this specification. No commercially licensable on-device gender model has been identified, and inferring gender from faces carries biometric-classification obligations. It is a separate follow-up feature whose purpose is to reduce over-coverage, not to enable coverage.
- [ASSUMED] NSFW classification is excluded from this specification, but the classifier layer is defined as an ordered list of signal producers so that adding it later requires no change to placement, tracking, overlay, or scheduling.
- [ASSUMED] Frame source is the accessibility screenshot API only. MediaProjection is excluded because its persistent capture indicator and per-session consent dialog are incompatible with a parental-control threat model.
- [ASSUMED] A second accessibility service means the parent must enable two entries in Android accessibility settings. This is accepted to keep blur and application blocking independent.
- [ASSUMED] Existing modified and untracked Child files under application blocking and removal protection belong to a separate concurrent feature and must remain untouched and unstaged.
- [ASSUMED] Publication: no GitHub push, server deploy, or APK install is included without a new explicit confirmation.
