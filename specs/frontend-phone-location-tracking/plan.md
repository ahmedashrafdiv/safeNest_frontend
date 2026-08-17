# Implementation Plan: Phone-Based Child Location Tracking

**Branch**: `frontend-phone-location-tracking` | **Date**: 2026-08-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/frontend-phone-location-tracking/spec.md`

## Summary

Add a permissioned, visible, battery-conscious phone-location collection flow to the Child Android app and extend the existing Parent map experience to display a typed location envelope with source, freshness, accuracy, and truthful degraded states. The new service remains separate from the AccessibilityService app blocker and DNS website-filtering VPN. The external GPS management flow remains separate and unchanged.

## Technical Context

**Language/Version**: Kotlin, Android API 34 target, minimum SDK 24

**Primary Dependencies**: Android location provider available to the project, Retrofit/OkHttp, WorkManager, Firebase messaging, existing AndroidX lifecycle/UI components, Google Maps in Parent

**Storage**: Existing Child SharedPreferences wrapper for tracking state and last upload; Backend Firestore is accessed through the API; no location history in v1

**Testing**: Child JVM unit tests, Parent build validation, existing repository/API tests where available, and a paired-device integration checklist

**Target Platform**: Android Child phone and Android Parent phone

**Project Type**: Two Android applications sharing an authenticated Backend contract

**Performance Goals**: Start visible tracking within 10 seconds after consent; upload within the configured interval plus 30 seconds under normal connectivity; avoid redundant uploads through time/distance/accuracy filtering.

**Constraints**: Explicit runtime location consent; Android foreground-service notification; OEM battery restrictions; bounded retry/backoff; no false active status; latest-point only; no IoT changes.

**Scale/Scope**: One latest phone point per paired Child; no breadcrumb history; one Parent map marker with source/freshness metadata.

## Constitution Check

No active repository constitution is present in the Frontend repository. The feature applies the project constraints in `spec.md`: explicit consent, truthful status, source separation, Android-client enforcement/collection, English code/tests/docs, no IoT changes, and no GitHub push. **Gate: PASS with documented project constraints.**

## Phase 0: Research and decisions

The Child app already has a permissions onboarding screen, a WorkManager/FCM lifecycle, a watchdog receiver, a Retrofit service, and preference storage. The new location service will be added as a separate lifecycle component and will not be folded into the app-blocking AccessibilityService or website VPN. The Parent already has `GpsFragment`, `GpsViewModel`, and a location repository using a loose map response; the feature will introduce typed mapping while preserving the existing map surface.

The default decisions are latest-point only, fresh phone source preferred, external GPS fallback labeled explicitly, foreground service for active collection, WorkManager for bounded reconciliation/retry, and explicit unavailable/stale states. Exact interval values will be configurable constants/policy bounds and verified on device.

## Phase 1: Design and contracts

### Child structure

```text
app_child/SafeNest-Kids/app/src/main/
├── AndroidManifest.xml
├── java/com/safenest/kids/
│   ├── PermissionsFragment.kt
│   ├── HomeFragment.kt
│   ├── network/KidsApiService.kt
│   ├── network/ApiModels.kt
│   ├── service/PhoneLocationService.kt
│   ├── service/PhoneLocationSyncWorker.kt
│   ├── service/PhoneLocationDecider.kt
│   └── util/PrefsHelper.kt
└── res/layout/
    ├── fragment_permissions.xml
    └── fragment_home.xml
```

### Parent structure

```text
app_father/SafeNest/app/src/main/java/com/example/safenest/
├── fragments/GpsFragment.kt
├── viewmodel/GpsViewModel.kt
├── repository/LocationRepository.kt
├── network/ApiModels.kt
└── network/SafeNestApiService.kt
```

### Child API contract

The Child adds `POST /api/child-devices/{device_id}/location` with report ID, coordinates, accuracy, optional altitude/speed, and captured timestamp. The authenticated API client supplies the existing device token. Successful upload persists the server response and last-success timestamp.

### Parent API contract

The Parent continues using `GET /location/live/{child_id}`, but maps the normalized envelope into a typed model. The UI supports phone, external GPS, stale, unavailable, disabled, and legacy external-GPS response states.

### Local state and health

The Child persists tracking enabled state, permission/service health, last accepted report ID, last successful upload time, last attempted upload time, and policy version. The UI distinguishes `active`, `permission_denied`, `service_stopped`, `network_unavailable`, `stale`, `disabled`, and `unavailable`.

## Implementation order

First add pure location acceptance/filtering tests and API models. Then add Android permissions and service lifecycle, followed by upload/retry/recovery. Next update Parent models/repository/ViewModel/map UI and finally add build/integration validation, review artifacts, and local commits.

## Complexity Tracking

The separate location service is intentional. Reusing AccessibilityService or WebsiteDnsVpnService would couple unrelated enforcement responsibilities, complicate permissions, and make failure status misleading.
