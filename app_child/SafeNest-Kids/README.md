# SafeNest Child Android Application

The SafeNest Child application is the child-device client for the SafeNest parental-safety platform. It pairs a device with a parent-managed child profile, guides the user through required Android permissions, synchronizes legacy digital-control rules, reports installed applications and usage, and enforces blocked applications and local per-app time limits on the device.

> **Implementation status:** This README documents the current source snapshot. The application still uses the legacy `link-device` and digital-control contracts. The newer secure device claim lifecycle and versioned Daily Screen-Time contracts are not yet represented in the visible Retrofit service.

## Table of Contents

- [Responsibilities](#responsibilities)
- [Technology Stack](#technology-stack)
- [Project Configuration](#project-configuration)
- [Application Flow](#application-flow)
- [Pairing and Local State](#pairing-and-local-state)
- [Permissions](#permissions)
- [Policy Synchronization](#policy-synchronization)
- [Usage Collection and Reporting](#usage-collection-and-reporting)
- [Local Enforcement](#local-enforcement)
- [Firebase Messaging](#firebase-messaging)
- [API Contract](#api-contract)
- [Building](#building)
- [Security and Distribution Notes](#security-and-distribution-notes)
- [Current Limitations](#current-limitations)
- [Source Map](#source-map)
- [References](#references)

## Responsibilities

The Child application is the client-side enforcement component. It does not provide the parent account-management experience. Instead, it receives a device/child association, stores the active policy locally, collects Android usage information, and applies local blocking decisions when the required Android services are enabled.

The backend supplies policies and records usage. The Android client is responsible for obtaining user-granted permissions and enforcing the locally cached policy, including when the device is temporarily offline.

## Technology Stack

| Area | Current implementation |
|---|---|
| Language | Kotlin |
| UI | Android XML layouts, AndroidX Fragments, Material components, View Binding |
| Networking | Retrofit 2.9.0, Gson converter, OkHttp 4.12.0 |
| Background work | AndroidX WorkManager 2.9.0 |
| Local enforcement | Android `AccessibilityService` |
| Usage collection | Android `UsageStatsManager` and usage events |
| Push messaging | Firebase Cloud Messaging |
| Local persistence | `SharedPreferences` through `PrefsHelper` |
| Build | Gradle Kotlin DSL, Android application plugin, Google Services plugin |

The project uses application ID and namespace `com.safenest.kids`, compile and target SDK 34, minimum SDK 24, and Java/Kotlin compilation target 1.8. View Binding is enabled. Release code shrinking is disabled in the current Gradle configuration.

## Project Configuration

```text
app/src/main/
├── java/com/safenest/kids/
│   ├── MainActivity.kt
│   ├── BlockedAppActivity.kt
│   ├── HomeFragment.kt
│   ├── PairingFragment.kt
│   ├── PermissionsFragment.kt
│   ├── network/
│   ├── service/
│   └── util/
├── res/
│   ├── layout/
│   ├── values/
│   └── xml/
└── AndroidManifest.xml
```

The manifest registers the main activity, the blocked-app activity, the accessibility enforcement service, the boot/package-replacement/package-lifecycle watchdog receiver, the installed-app synchronization worker path, and the Firebase messaging service.

## Application Flow

`MainActivity` initializes `ApiClient` and `PrefsHelper`, then hosts the child onboarding and home Fragments. A new installation starts with the pairing flow. After successful pairing, the application stores the identifiers and device access token returned by the backend, then moves to permission onboarding.

`PermissionsFragment` checks the required Android capabilities and directs the user to the appropriate system settings when a capability is missing. `HomeFragment` presents the current protection state and schedules or triggers background synchronization/reporting work.

The child application does not depend on the parent application being installed on the same device. Parent actions are delivered through the SafeNest backend and Firebase Messaging where applicable.

## Pairing and Local State

`PairingFragment` accepts a six-digit PIN, obtains an FCM token when available, creates or retrieves a locally generated device ID, and sends device metadata including device name, device type, and token to the legacy pairing endpoint.

On a successful response, `PrefsHelper` stores values including:

| Local key or concept | Purpose |
|---|---|
| `device_id` | Stable locally generated device identifier. |
| `child_id` | Child profile assigned by the backend. |
| `parent_id` | Parent associated with the child device. |
| `device_access_token` | Token used by later child-originated API requests. |
| `is_paired` | Whether the local onboarding flow considers the device paired. |
| `just_paired` | One-time post-pairing state used by onboarding behavior. |
| `blocked_apps` | Locally cached blocked package names. |
| `app_time_limits_json` | Serialized per-package minute limits. |
| `last_apps_sent` | Local reporting state for installed applications. |
| `installed_apps_fingerprint` | Fingerprint of the last successfully uploaded launchable-app inventory. |

This state is stored through ordinary `SharedPreferences` in the `SafeNestKidsPrefs` preference file. It is the local input to enforcement and must be treated as sensitive device state.

## Permissions

The manifest declares and the onboarding flow checks the following capabilities:

| Capability | Why the application uses it |
|---|---|
| Internet | Retrofit API calls and Firebase-related network operations. |
| `PACKAGE_USAGE_STATS` | Read application usage through Android Usage Access. |
| `QUERY_ALL_PACKAGES` | Discover installed applications for reporting and policy matching. |
| `POST_NOTIFICATIONS` | Display service-health and child-facing notifications on supported Android versions. |
| `RECEIVE_BOOT_COMPLETED` | Recheck service health after device boot. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Guide the user through reducing background-service interruptions. |
| `BIND_ACCESSIBILITY_SERVICE` | Allow the declared `AppBlockerAccessibilityService` to receive foreground-window events. |

Usage Access and AccessibilityService are enabled by the user through Android system settings; they are not ordinary runtime permissions that the application can grant itself. The application checks the current status through `PermissionsHelper`.

## Policy Synchronization

`RuleSyncWorker` calls `GET /api/digital-control/device/rules`, reads the returned blocked-app and per-app time-limit values, and writes them to `PrefsHelper`. It returns a retry result after a network/API failure and a success result after policy persistence.

`SafeNestFirebaseService` can trigger the same policy-refresh behavior after receiving an appropriate data message. The periodic worker and the FCM path write to the same local preference cache so that `AppBlockerAccessibilityService` can make decisions without a network call.

## Usage Collection and Reporting

`AppUsageHelper` uses Android `UsageStatsManager` to calculate today’s per-package foreground minutes. The implementation works with the local day boundary, handles foreground/background event pairs, and accounts for a package that was already in the foreground at the beginning of the period.

`AppUsageReportWorker` reads the child ID from `PrefsHelper`, obtains the current usage map, and reports it through `POST /api/digital-control/app-usage`. WorkManager retry behavior is used for failed network/API operations.

The current implementation reports aggregate usage. It does not yet implement the newer backend usage-event contract with stable client event IDs, policy versions, explicit acknowledgements, or server-side decision synchronization.

## Local Enforcement

`AppBlockerAccessibilityService` listens for window-state changes and identifies the foreground package. It deliberately ignores the child application itself, Android system surfaces, known launcher packages, and content-change events that would otherwise produce repeated processing.

For a blocked package, the service launches `BlockedAppActivity` with the package name and a `blocked` reason. For a package whose locally calculated usage has reached its configured per-app limit, it launches the same activity with a `time_limit` reason. A short two-second rate limit prevents rapid repeated launches for the same package.

`BlockedAppActivity` is excluded from recents and presents the child-facing blocking screen. The service does not make a network call for each foreground event; it reads locally cached policy values and usage information.

`ServiceWatchdogReceiver` reacts to boot, Child-app replacement, package-added, package-removed, and package-replaced broadcasts. It triggers the durable `InstalledAppsSyncWorker`, which rescans the full launchable-app inventory, skips unchanged fingerprints, retries failed uploads, and updates the fingerprint only after a successful Backend response. Accessibility-service health notifications remain limited to boot and Child-app replacement recovery.

## Firebase Messaging

`SafeNestFirebaseService` receives refreshed FCM tokens and attempts to update the paired device token through `PUT /api/devices/fcm-token`. It also processes data messages that request local policy refresh behavior.

Firebase reception on the device is separate from backend notification delivery. The backend remains responsible for durable notification records, external delivery, retries, and dead-letter handling.

## API Contract

The current visible Retrofit service is `KidsApiService`:

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/devices/link-device` | Legacy PIN/device linking. |
| `GET` | `/api/digital-control/device/rules` | Retrieve the current legacy child rule. |
| `PUT` | `/api/children/{child_id}/installed-apps` | Report installed applications and related child-device data. |
| `POST` | `/api/digital-control/app-usage` | Report aggregate application usage. |
| `PUT` | `/api/devices/fcm-token` | Update the FCM token for a paired device. |

`ApiClient` uses the production base URL `https://safe-nest-deployment.vercel.app/`, reads `device_access_token` from `PrefsHelper`, and adds a Bearer token after pairing. Its unauthenticated-path handling is intended to permit the pre-pairing flow before a device token exists.

The installed-app request body is marked in the source as requiring verification against the backend contract before production use. That shape should be confirmed from the active backend schema before a frontend migration is finalized.

## Building

From this directory, run:

```bash
./gradlew assembleDebug
```

On Windows, run:

```bat
gradlew.bat assembleDebug
```

Open this directory as an independent Android Studio project. The required SDK platform is defined by `compileSdk = 34` in `app/build.gradle.kts`.

## Security and Distribution Notes

The source configures an OkHttp body-level logging interceptor. Release builds must disable or tightly control body logging because tokens, policy data, package names, and API responses can appear in logs.

The device access token and policy cache are stored in ordinary `SharedPreferences`. Encrypted storage, token rotation, revocation handling, and secure cleanup after unpairing should be addressed during the secure device-lifecycle migration.

`QUERY_ALL_PACKAGES`, Usage Access, and AccessibilityService are sensitive capabilities. Their use requires a clear user-facing explanation and a Google Play policy review. The application should not assume that a capability enabled during development will be accepted for every distribution channel.

The child app should fail closed or use an explicitly defined safe default when the local policy cache is malformed, missing, expired, or revoked by the backend. The current service returns no time-limit block for malformed JSON and relies on the cached blocked-app set, so this behavior must be considered when designing the new policy contract.

## Current Limitations

The child application currently supports foreground application blocking and local per-app time limits. It does not provide complete browser URL filtering, complete browser-history collection, or complete YouTube monitoring. Those capabilities require additional Android mechanisms and platform-compliant product decisions.

The pairing flow still uses `link-device` rather than the newer secure claim/trust lifecycle. The app also uses the legacy rule shape rather than a versioned daily policy with timezone, stable usage-event IDs, policy acknowledgement, server decisions, and extra-time grants.

The background workers are periodic and best-effort. Installed-app change broadcasts trigger a durable one-time sync, but Android may defer work based on battery, standby, connectivity, or system policy. The Child keeps the previous fingerprint until an upload succeeds, so deferred or offline changes remain retryable.

## Source Map

| Concern | Source locations |
|---|---|
| Entry and onboarding | `app/src/main/java/com/safenest/kids/MainActivity.kt`, `PairingFragment.kt`, `PermissionsFragment.kt` |
| Child API | `app/src/main/java/com/safenest/kids/network/KidsApiService.kt`, `ApiClient.kt`, `ApiModels.kt` |
| Local state | `app/src/main/java/com/safenest/kids/util/PrefsHelper.kt` |
| Usage collection | `app/src/main/java/com/safenest/kids/util/AppUsageHelper.kt` |
| Installed-app synchronization | `service/InstalledAppsSyncWorker.kt`, `util/InstalledAppsHelper.kt`, `util/ServiceWatchdogReceiver.kt` |
| Installed applications | `app/src/main/java/com/safenest/kids/util/InstalledAppsHelper.kt` |
| Enforcement | `app/src/main/java/com/safenest/kids/service/AppBlockerAccessibilityService.kt`, `BlockedAppActivity.kt` |
| Background work | `service/RuleSyncWorker.kt`, `service/AppUsageReportWorker.kt` |
| Push messaging | `service/SafeNestFirebaseService.kt` |
| Permission health | `util/PermissionsHelper.kt`, `util/ServiceWatchdogReceiver.kt` |
| Android declarations | `app/src/main/AndroidManifest.xml` |
| Build configuration | `app/build.gradle.kts` |

## References

[1]: `app/src/main/java/com/safenest/kids/network/KidsApiService.kt` — Child Retrofit endpoints.
[2]: `app/src/main/java/com/safenest/kids/network/ApiClient.kt` — Base URL, token injection, logging, and timeouts.
[3]: `app/src/main/java/com/safenest/kids/network/ApiModels.kt` — Request and response models used by the child client.
[4]: `app/src/main/java/com/safenest/kids/util/PrefsHelper.kt` — Local device, pairing, and policy state.
[5]: `app/src/main/java/com/safenest/kids/service/AppBlockerAccessibilityService.kt` — Foreground-app detection and local enforcement.
[6]: `app/src/main/AndroidManifest.xml` — Permissions and registered Android components.
[7]: `app/build.gradle.kts` — Android SDK and dependency configuration.


## App Blocking Reliability Specification

The Child app is the enforcement side of the shared App Blocking Reliability contract. It receives `RULES_UPDATED`, enqueues WorkManager synchronization, stores the authoritative policy locally, and applies blocked-package decisions through AccessibilityService. The Frontend-specific Spec Kit feature is documented at [`../../specs/frontend-app-blocking-reliability/spec.md`](../../specs/frontend-app-blocking-reliability/spec.md), and the cross-repository contract is at [`../../../APP_BLOCKING_INTEGRATION_CONTRACT.md`](../../../APP_BLOCKING_INTEGRATION_CONTRACT.md).

The periodic 15-minute worker is only a fallback. Immediate delivery depends on a valid FCM token, background execution permissions, network availability, and an enabled AccessibilityService. A successful APK build does not replace the required paired-device latency test.
