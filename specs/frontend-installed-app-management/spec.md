# Frontend Spec: Installed-App Management

## Purpose

Document the Android client behavior that lets a Parent discover applications reported by a paired Child device and select real packages for blocking or per-app time limits. This feature excludes Backend persistence and all IoT code.

## Scope

The Frontend owns Retrofit model alignment, inventory loading and refresh, loading/empty/error states, human-readable rendering, block/unblock actions, and per-app time-limit actions. The Child owns inventory collection and Android enforcement; the Backend owns policy storage and authorization.

## Requirements

### R1. Contract alignment

The Parent client parses `GET /api/children/{child_id}/installed-apps` items as `{ package_name, app_name }`, retains `package_name` for policy actions, and displays `app_name` when available.

### R2. Inventory loading

The installed-app screen loads the selected Child inventory when opened and refreshes it when the screen resumes or the selected Child changes. It shows explicit loading, empty, and error/retry states.

### R3. Policy actions

The Parent offers direct block/unblock actions for each installed package. It offers a per-app time-limit action that updates the authoritative Backend rule using `app_time_limits` keyed by package name.

### R4. Identity safety

Display labels never replace package names as policy identifiers. Missing labels fall back to the package name. The UI does not send an app display name as the enforcement key.

### R5. Child enforcement boundary

The Frontend treats the Backend rule as authoritative and does not claim that a successful Parent request alone proves Android enforcement. The Child continues local synchronization and AccessibilityService enforcement from its cached package-name policy.

### R6. Resilience

The screen remains understandable when the inventory is empty, loading, or unavailable. Actions provide visible success/failure feedback and avoid duplicate submissions while a request is in flight.

## Non-goals

Backend route implementation, AccessibilityService internals, Android permission onboarding, browser URL filtering, YouTube monitoring, notification workers, IoT sensors/routes, and live paired-device verification.

## Acceptance criteria

Installed apps appear as selectable Parent inventory items after Child reporting. Block/unblock and time-limit actions use package names rather than labels. Loading, empty, error, and retry states are explicit. Parent and Child debug builds succeed, and automated parsing/payload tests exist where supported by the Android test setup.
