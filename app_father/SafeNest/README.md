# SafeNest Parent Android Application

The SafeNest Parent application is the parent-facing Android client for the SafeNest parental-safety platform. It provides account authentication, child management, legacy digital-control configuration, device inventory, monitoring screens, location and safe-zone management, notifications, and account settings.

> **Implementation status:** This README documents the current source snapshot. The application still consumes the legacy device, digital-control, and alert contracts in its Retrofit service. Newer backend Device Management, Daily Screen-Time, Unified Notifications, and Parent Dashboard routes require a coordinated frontend migration before they are available in this UI.

## Table of Contents

- [Responsibilities](#responsibilities)
- [Technology Stack](#technology-stack)
- [Project Configuration](#project-configuration)
- [Application Startup and Navigation](#application-startup-and-navigation)
- [Feature Areas](#feature-areas)
- [Networking and Authentication](#networking-and-authentication)
- [Background Work and Notifications](#background-work-and-notifications)
- [API Contract](#api-contract)
- [Building](#building)
- [Configuration and Secrets](#configuration-and-secrets)
- [Current Limitations](#current-limitations)
- [Source Map](#source-map)
- [References](#references)

## Responsibilities

The Parent application is a management client rather than the device-enforcement layer. It allows an authenticated parent to manage children, configure policies, inspect usage and history, review alerts, manage devices, and view location-related information. Enforcement of application blocking and local screen-time decisions occurs on the Child application after policy synchronization.

The project uses a conventional XML-layout Android architecture. `MainActivity` owns the main fragment container, Fragments render screens, ViewModels expose observable operation state, repositories wrap Retrofit calls, and `SessionManager` stores the active parent session and selected child identifier.

## Technology Stack

| Area | Current implementation |
|---|---|
| Language | Kotlin |
| UI | Android XML layouts, AndroidX Fragments, Material components, ConstraintLayout |
| Networking | Retrofit 2.9.0, Gson converter, OkHttp 4.12.0 |
| Async state | Kotlin coroutines, `viewModelScope`, `StateFlow` |
| Background work | AndroidX WorkManager 2.9.0 |
| Push messaging | Firebase Cloud Messaging |
| Maps | Google Play Services Maps 18.2.0 |
| Build | Gradle Kotlin DSL, Android application plugin, Google Services plugin |

The application configuration sets namespace and application ID to `com.example.safenest`, compile and target SDK to 36, minimum SDK to 24, and Java/Kotlin compilation target to 11. The release build currently has code shrinking disabled.

## Application Startup and Navigation

`SplashActivity` displays the splash layout and starts `MainActivity` after the configured delay. `MainActivity` initializes the API client, checks `SessionManager.isLoggedIn()`, and routes the user either to the authentication flow or to the main home experience.

Navigation is implemented with direct Fragment transactions. `navigateToFragment` replaces the main container and adds the transaction to the back stack. `goToHome` clears the back stack before opening `HomeFragment`. Logout cancels the unique location-sync work, clears stored credentials, and returns the user to the authentication flow.

The bottom navigation exposes Monitoring, GPS, Sensors, and More destinations. Other screens are reached from cards and buttons within the Fragments rather than through a Navigation Component graph.

## Feature Areas

### Authentication and Account Management

The authentication flow includes registration, login, email verification, OTP resend, forgot-password, and reset-password screens. `AuthRepository` delegates the network operations to `SafeNestApiService`, while the authentication ViewModels expose loading, success, and error states to the Fragments.

After a successful login, the application stores the returned access token and parent identifier through `SessionManager`. Profile and settings screens provide parent-profile update, password change, logout, and account-deletion operations.

### Child Management

`HomeFragment`, `AddChildIntroFragment`, `AddChildFragment`, and `EditChildFragment` support the current child-management flow. The parent can create, list, load, update, and delete child profiles. The selected child ID is persisted locally and reused by screens that need a child context.

### Devices

`PairingFragment`, `GeneratePinFragment`, `MyDevicesFragment`, `DeviceRepository`, and `DevicesViewModel` implement the current legacy device surface. The client can generate a PIN, pair a device, list devices, list status records, and delete a device. `MyDevicesFragment` combines device and status responses in memory to render device cards.

This is not yet the newer secure Device Management lifecycle. The current app does not visibly expose the complete parent-created pairing, child claim, heartbeat, health, trust, revoke, or unpair contract introduced by the newer backend.

### Digital Control and Screen-Time

`MonitoringFragment`, `MonitoringViewModel`, and `DigitalControlRepository` manage the legacy digital-control rule. The current UI works with fields such as maximum screen time, blocked applications, allowed applications, per-application time limits, and daily usage logs. It can create, retrieve, update, and delete a rule, upload aggregate app usage, clear a daily usage log, and navigate to installed-app and video-history screens.

`DailyUsageFragment` renders usage information from the legacy response shape. `InstalledAppsFragment` supports the parent-facing installed-application view. The current UI does not yet consume the newer policy-version, timezone, stable usage-event, acknowledgement, decision, summary, or extra-time grant contracts.

### Video History

`VideoHistoryFragment` and `VideoHistoryViewModel` retrieve and clear child video history through the legacy digital-control endpoints. The current Retrofit interface represents the returned history as `List<Map<String, Any>>`, so runtime parsing is more permissive but less type-safe than a dedicated response model.

### Location, GPS, and Safe Zones

`GpsFragment`, `ChildGpsFragment`, `GpsViewModel`, and `LocationRepository` provide GPS pairing, live child-location retrieval, and deletion. `LocationSyncWorker` performs periodic background synchronization using WorkManager.

`SafeZonesFragment`, `AddZoneFragment`, `SafeZonesViewModel`, and `ZoneRepository` manage zones with child ID, name, type, latitude, longitude, and radius values. The project includes Google Maps integration and a Maps resource configuration.

### Alerts and Notifications

`NotificationsFragment`, `NotificationsViewModel`, and `AlertRepository` use the current legacy alert API to list alerts, update an alert, and delete an alert. The UI renders alert type, message, timestamp, and resolved state.

`SafeNestFirebaseService` receives Firebase messages, extracts notification or data payload values, creates the `SafeNest Alerts` notification channel, and displays a local Android notification. It also sends the parent FCM token to the legacy parent token endpoint when Firebase refreshes the token.

### More, Profile, and Settings

`MoreFragment` acts as a navigation hub. `ProfileFragment` and `ProfileViewModel` handle profile retrieval and updates. `SettingsFragment` and `SettingsViewModel` handle password change, logout, and account deletion.

## Networking and Authentication

The Retrofit service is defined by `SafeNestApiService`. `ApiClient` builds the Retrofit instance with Gson, a 30-second connect/read/write timeout, an OkHttp Bearer-token interceptor, and a body-level logging interceptor.

The current base URL is defined as:

```text
https://safe-nest-deployment.vercel.app/
```

The parent access token is read from `SafeNestPrefs` under `access_token`. The interceptor attaches `Authorization: Bearer <token>` when a token exists. `SessionManager` stores the token, `parent_id`, and `selected_child_id`, and `clearAll()` removes the local session during logout.

## Background Work and Notifications

`MainActivity` schedules a unique periodic `LocationSyncWorker` with WorkManager. WorkManager enforces the platform’s minimum periodic interval, so this is not a real-time location channel.

The Firebase service is a client-side receiver. It is not a replacement for a backend notification dispatcher, durable delivery record, retry policy, or dead-letter mechanism. The backend must remain responsible for durable notification delivery orchestration.

## API Contract

The current visible Retrofit surface is grouped below:

| Area | Methods and paths |
|---|---|
| Authentication | `POST /api/auth/register`, `/login`, `/verify-email`, `/resend-otp`, `/forgot-password`, `/reset-password` |
| Parent profile | `GET/PUT /api/parents/profile`, `POST /api/parents/change-password`, `DELETE /api/parents/account`, `PUT /api/parents/fcm-token` |
| Children | `GET /api/children`, `POST /api/children`, `GET/PUT/DELETE /api/children/{child_id}`, installed-app endpoints |
| Digital control | `POST /api/digital-control/rule`, `GET /api/digital-control/child/{child_id}`, `PUT /api/digital-control/{rule_id}`, `DELETE /api/digital-control/rule/{rule_id}` |
| Usage and history | `POST /api/digital-control/app-usage`, daily-log deletion, video-history create/list/delete endpoints |
| Legacy devices | `POST /api/devices/pair`, `GET /api/devices`, `GET /api/devices/status`, `DELETE /api/devices/{device_id}`, `POST /api/devices/generate-pin` |
| Location and GPS | `POST /gps/pair`, `POST /gps/update-from-thingspeak`, `DELETE /gps/{child_id}`, `GET /location/live/{child_id}` |
| Zones | CRUD endpoints under `/api/zones` |
| Alerts | `GET /api/alerts`, `PUT /api/alerts/{alert_id}`, `DELETE /api/alerts/{alert_id}` |

## Building

From this directory, run:

```bash
./gradlew assembleDebug
```

On Windows, run:

```bat
gradlew.bat assembleDebug
```

Open this directory as an independent Android Studio project. The required SDK platform is defined by `compileSdk = 36` in `app/build.gradle.kts`.

## Configuration and Secrets

The source currently hard-codes the production API host. A future environment migration should provide separate debug, staging, and production values and should prevent debug builds from accidentally using production data.

OkHttp body logging is enabled in the current client configuration. It must be disabled or restricted in release builds because request and response bodies can contain authentication or family data.

The Maps key and Firebase configuration are client-side resources. They are not server secrets. Restrict the Maps key by application and signing certificate, configure quotas, and avoid placing backend credentials in the Android project.

## Current Limitations

The parent application is functional as a legacy management client, but its UI does not yet consume the newer dashboard aggregation routes. It also does not expose the full secure device lifecycle, new daily screen-time policy synchronization, unified notification preferences and delivery state, or child access-request workflows.

## Source Map

| Concern | Source locations |
|---|---|
| Entry and navigation | `app/src/main/java/com/example/safenest/MainActivity.kt`, `SplashActivity.kt` |
| API client and models | `app/src/main/java/com/example/safenest/network/` |
| Repositories | `app/src/main/java/com/example/safenest/repository/` |
| ViewModels | `app/src/main/java/com/example/safenest/viewmodel/` |
| UI screens | `app/src/main/java/com/example/safenest/fragments/` |
| Background work | `LocationSyncWorker.kt`, `SafeNestFirebaseService.kt` |
| Session state | `util/SessionManager.kt` |
| Android declarations | `app/src/main/AndroidManifest.xml` |
| Build configuration | `app/build.gradle.kts` |

## References

[1]: `app/src/main/java/com/example/safenest/network/SafeNestApiService.kt` — Retrofit endpoints exposed by the parent client.
[2]: `app/src/main/java/com/example/safenest/network/ApiClient.kt` — Base URL, token interceptor, logging, and timeouts.
[3]: `app/src/main/java/com/example/safenest/util/SessionManager.kt` — Parent session persistence.
[4]: `app/src/main/java/com/example/safenest/MainActivity.kt` — Startup, navigation, logout, and WorkManager scheduling.
[5]: `app/src/main/AndroidManifest.xml` — Application components and permissions.
[6]: `app/build.gradle.kts` — Android SDK and dependency configuration.


## App Blocking Reliability Specification

The Parent app participates in the shared App Blocking Reliability contract by retrieving the authoritative `ruleId` for the selected child and submitting blocked-app changes through the digital-control update route. The Frontend-specific Spec Kit feature is documented at [`../../specs/frontend-app-blocking-reliability/spec.md`](../../specs/frontend-app-blocking-reliability/spec.md), and the cross-repository contract is at [`../../../APP_BLOCKING_INTEGRATION_CONTRACT.md`](../../../APP_BLOCKING_INTEGRATION_CONTRACT.md).

A successful APK build does not prove live FCM delivery or Child-side enforcement. End-to-end verification requires a paired Child device and timestamped backend, FCM, and device logs.
