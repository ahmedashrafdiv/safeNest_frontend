# SafeNest Android Frontend

SafeNest Android Frontend contains two independent Kotlin Android applications that work together as a parental-safety system:

| Application | Directory | Package | Responsibility |
|---|---|---|---|
| Parent application | [`app_father/SafeNest`](./app_father/SafeNest) | `com.example.safenest` | Parent authentication, child management, policy configuration, device inventory, monitoring, location, safe zones, alerts, and settings. |
| Child application | [`app_child/SafeNest-Kids`](./app_child/SafeNest-Kids) | `com.safenest.kids` | Child-device pairing, permission onboarding, installed-app reporting, usage reporting, policy synchronization, and local app blocking. |

> **Documentation scope:** This README describes the source code in the supplied frontend archive. It documents the current implementation, including legacy API contracts that are still used by the applications. It does not claim that backend features are available in the Android UI unless the corresponding client code exists.

## Table of Contents

- [Architecture](#architecture)
- [Repository Layout](#repository-layout)
- [How the Applications Communicate](#how-the-applications-communicate)
- [Current Feature Coverage](#current-feature-coverage)
- [Build Requirements](#build-requirements)
- [Building the Projects](#building-the-projects)
- [Runtime Configuration](#runtime-configuration)
- [Security and Distribution Notes](#security-and-distribution-notes)
- [Known Integration Boundaries](#known-integration-boundaries)
- [Project Documentation](#project-documentation)
- [Source References](#source-references)

## Architecture

The frontend is split into a parent-facing application and a child-device application. The parent application is primarily a management and visualization client. It authenticates a parent, loads children and devices, configures digital-control rules, presents usage and alert information, and manages location and safe-zone screens.

The child application is the enforcement client. It stores the currently synchronized policy locally, collects Android usage information, reports usage to the backend, and uses an `AccessibilityService` to detect foreground applications and launch a blocking screen when a package is blocked or has exceeded its configured local time limit.

Both applications use XML layouts and Kotlin Fragments rather than Jetpack Compose. Retrofit and Gson provide HTTP communication and serialization. WorkManager provides periodic background work, while Firebase Cloud Messaging receives remote notifications or policy-refresh messages.

## Repository Layout

```text
frontend_app/
├── README.md
├── app_father/
│   └── SafeNest/
│       ├── app/
│       │   └── src/main/
│       │       ├── java/com/example/safenest/
│       │       ├── res/
│       │       └── AndroidManifest.xml
│       │   ├── build.gradle.kts
│       │   ├── settings.gradle.kts
│       │   └── google-services.json
│       └── README.md
└── app_child/
    └── SafeNest-Kids/
        ├── app/
        │   └── src/main/
        │       ├── java/com/safenest/kids/
        │       ├── res/
        │       └── AndroidManifest.xml
        │   ├── build.gradle.kts
        │   ├── settings.gradle.kts
        │   └── google-services.json
        └── README.md
```

The two directories are separate Gradle projects. They are not configured as a single multi-module build, so each project must be opened and built independently in Android Studio or from its own Gradle wrapper directory.

## How the Applications Communicate

The parent and child applications communicate with the SafeNest backend over HTTPS through Retrofit. In the supplied source snapshot, both API clients use the production base URL:

```text
https://safe-nest-deployment.vercel.app/
```

The parent client stores a parent JWT in `SafeNestPrefs` and attaches it as a Bearer token through an OkHttp interceptor. The child client stores a device access token in its own preferences and attaches it after pairing; pre-pairing requests are allowed to proceed without that token.

The applications do not communicate directly with each other. Parent-to-child policy changes are delivered through the backend and, where applicable, Firebase Cloud Messaging. The child application persists policy data locally so that enforcement can continue when a network request is unavailable.

## Current Feature Coverage

| Area | Parent application | Child application |
|---|---|---|
| Authentication | Registration, login, email verification, OTP resend, forgot-password, and reset-password screens. | No parent-account authentication flow; device onboarding begins with pairing. |
| Child management | Create, list, update, and delete child profiles. | Stores the assigned child and parent identifiers after pairing. |
| Device management | Legacy pairing, PIN generation, device list/status display, and deletion. | Legacy PIN-based `link-device` pairing and device-token persistence. |
| Digital control | Legacy rule creation, retrieval, update, deletion, blocked-app configuration, and usage views. | Legacy rule synchronization and local blocked-app/time-limit enforcement. |
| Screen-time | Displays and edits the legacy maximum screen-time and per-app limits. | Uses Android `UsageStatsManager` and local preferences for enforcement. |
| Notifications | FCM receipt plus legacy alert list, update, and delete UI. | FCM token handling and policy-refresh behavior. |
| Location and zones | GPS/location screens, periodic location work, Google Maps, and safe-zone CRUD. | No parent location-management UI. |
| YouTube/history | Video-history retrieval and clearing through the parent app. | No complete YouTube monitoring implementation is present. |

## Build Requirements

The projects use Gradle Kotlin DSL and include Gradle wrapper scripts. The exact Android configuration is project-specific:

| Project | Compile SDK | Target SDK | Minimum SDK | JVM target | Application ID |
|---|---:|---:|---:|---:|---|
| Parent | 36 | 36 | 24 | 11 | `com.example.safenest` |
| Child | 34 | 34 | 24 | 1.8 | `com.safenest.kids` |

A compatible Android SDK installation, Android SDK platform for the configured compile SDK, Java, and an Android-capable Gradle environment are required. Firebase configuration files are included in each project and must correspond to the application ID used for that project.

## Building the Projects

Open either project directory independently. From the Parent project:

```bash
cd app_father/SafeNest
./gradlew assembleDebug
```

From the Child project:

```bash
cd app_child/SafeNest-Kids
./gradlew assembleDebug
```

On Windows, use `gradlew.bat assembleDebug` instead of `./gradlew assembleDebug`. The source snapshot defines a release build with code shrinking disabled. Do not treat a successful debug build as proof that Android permission policy, Firebase delivery, Google Maps configuration, or Google Play distribution requirements have been satisfied.

## Runtime Configuration

The current clients do not expose a debug/staging/production API-host abstraction. The Retrofit base URL is hard-coded in the source, so local backend testing requires an explicit configuration migration before changing environments. Development builds should not silently target the production deployment.

The Parent application also expects a Google Maps key resource and Firebase configuration. The Child application expects Firebase Messaging configuration and uses Android system settings for Usage Access, AccessibilityService, notification permission, and battery-optimization handling.

## Security and Distribution Notes

Release logging must be reviewed carefully because both clients configure OkHttp body logging in the supplied source. Access tokens are stored in ordinary `SharedPreferences`; encrypted storage, token rotation, and device-session invalidation should be considered before production release.

The Child manifest requests `PACKAGE_USAGE_STATS`, `QUERY_ALL_PACKAGES`, `POST_NOTIFICATIONS`, boot reception, battery-optimization exemption requests, and the AccessibilityService binding permission. These capabilities are sensitive and require clear user disclosure, appropriate runtime onboarding, and a distribution-policy review before publishing through Google Play.

The Android client is responsible for enforcement. The backend provides policies and decisions, but the child device must obtain the required Android permissions and keep its local enforcement service operational.

## Known Integration Boundaries

The current source still uses legacy device-linking and digital-control contracts. The newer backend Device Management, Daily Screen-Time, Unified Notifications, Parent Dashboard, and Child Access Request contracts are not fully represented in the visible Retrofit clients and screens. A future migration must update API models, repositories, ViewModels, local persistence, background workers, and UI together.

The current child implementation blocks foreground applications, but it does not provide complete browser URL filtering or complete YouTube monitoring by itself. Those capabilities require additional Android mechanisms and platform-compliant design beyond ordinary Retrofit calls.

## Project Documentation

Detailed documentation for each application is available here:

- [Parent application README](./app_father/SafeNest/README.md)
- [Child application README](./app_child/SafeNest-Kids/README.md)

## Source References

[1]: `app_father/SafeNest/app/src/main/java/com/example/safenest/network/SafeNestApiService.kt` — Parent Retrofit contract.
[2]: `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/network/KidsApiService.kt` — Child Retrofit contract.
[3]: `app_father/SafeNest/app/src/main/AndroidManifest.xml` — Parent Android components and permissions.
[4]: `app_child/SafeNest-Kids/app/src/main/AndroidManifest.xml` — Child Android components and permissions.
[5]: `app_father/SafeNest/app/build.gradle.kts` — Parent SDK and dependency configuration.
[6]: `app_child/SafeNest-Kids/app/build.gradle.kts` — Child SDK and dependency configuration.


## Independent Repository and Spec Kit Boundary

This directory is the root of the independent `safeNest_frontend` repository. It contains the Parent and Child Android applications, their Gradle projects, and the Frontend-specific App Blocking Reliability feature under `specs/frontend-app-blocking-reliability/`. The active Frontend selector is `.specify/feature.json`.

The shared Backend-to-Android behavior is documented in [`../docs/contracts/APP_BLOCKING_INTEGRATION_CONTRACT.md`](../docs/contracts/APP_BLOCKING_INTEGRATION_CONTRACT.md). Parent submits the server-provided `ruleId`; Child receives `RULES_UPDATED`, synchronizes through WorkManager, stores the local policy, and enforces blocked packages through AccessibilityService.

No live Firebase or paired-Child latency test is claimed by a successful local build. Gradle build outputs, local SDK paths, logs, keystores, Firebase configuration, generated APKs, and other machine-local artifacts are excluded through the repository `.gitignore`. The canonical remote is `https://github.com/ahmedashrafdiv/safeNest_frontend.git`.
