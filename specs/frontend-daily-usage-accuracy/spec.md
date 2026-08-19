# Feature Specification: Layngo Parent Daily Usage Accuracy and Screen

## Purpose

Layngo Parent must show an Arabic right-to-left daily usage screen that a parent can understand in seconds: child name, update freshness, total used time, configured daily limit, time remaining, and app-level usage. The UI must only present a normal summary when it receives a verified date-bounded Backend summary; it must not reconstruct totals from an unqualified legacy map.

## Functional Requirements

- FR-001: Parent consumes the canonical daily-summary fields supplied by Backend and does not treat legacy `maxScreenTime` as the configured daily limit.
- FR-002: Parent shows a normal summary only when `usageDate` is the current local date reported by the child and `limitConfirmationRequired=false`.
- FR-003: Parent renders total used, fixed daily limit, and remaining time as separate Arabic values; remaining must be derived from the authoritative summary, not inferred by subtracting a potentially corrupted field.
- FR-004: Parent sorts application rows by duration descending and renders their exact duration, normalized display name, neutral vector app marker, and a proportional usage bar.
- FR-005: The screen uses Layngo Ivory `#FFFDF7`, Navy `#15385F`, Teal `#2CA39D`, Mint `#E5F4F1`, and Coral `#F27D6B` for over-limit state only.
- FR-006: The screen is native XML-based, Arabic RTL, has a 48dp minimum touch target, clear accessibility descriptions, one safe scroll surface, and no color-only meaning.
- FR-007: Header text includes the selected child name, a real last-updated value from `usageUpdatedAt`, and the official unmodified Layngo mark already stored in the app.
- FR-008: The primary CTA is exactly `إدارة وقت الشاشة` and navigates to the existing screen-time management route.
- FR-009: Parent shows a dedicated limit-confirmation state for legacy rules, plus explicit loading, empty, stale/error, and over-limit states.
- FR-010: Child submits complete snapshots with `usage_day` and `usage_timezone`; no IoT/Sensor, blocking, Accessibility, or device-admin behavior is changed.

## Acceptance Criteria

| Scenario | Expected result |
|---|---|
| Correct Backend summary | Parent presents Layngo summary card, ring, app list, and CTA with real values. |
| 5-hour configured limit | The configured limit remains 5 hours across multiple reports; total/remaining reflect today’s snapshot only. |
| Legacy corrupted rule | Parent does not display a guessed limit and offers clear management/confirmation recovery. |
| Over limit | Ring and helper move to Coral with accessible explanatory text. |
| Data failure | Screen does not present stale content as current and exposes a retry path. |
| Arabic layout | RTL ordering, text scaling, contrast, and touch targets remain usable. |

## Clarifications

- [ASSUMED] Reference fidelity: replicate the hierarchy, calm spacing, and Layngo tokens from the supplied reference, not any unlicensed third-party icon artwork.
- [ASSUMED] App identity: use a constrained package-to-display-name/neutral-vector mapping; unknown packages retain a safe readable fallback.
- [ASSUMED] “Last update”: show relative Arabic time based on server-accepted `usageUpdatedAt`, not device-clock guesswork.
- [ASSUMED] Existing untracked Child `SetupReadinessDecider` files belong to the separate removal-protection feature and will remain untouched and unstaged.
- [ASSUMED] Publication: no GitHub push, server deploy, or APK install is included without a new explicit confirmation.

