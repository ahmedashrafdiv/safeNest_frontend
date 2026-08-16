# Frontend Spec: App Blocking Reliability

## Purpose
The Android Frontend receives policy-change signals from the Backend, synchronizes the authoritative rule, stores it locally, and enforces blocked applications on the Child device. The Parent app sends the correct digital-rule identifier and update payload but does not enforce the policy itself.

## Applications

| Application | Responsibility |
|---|---|
| Parent (`app_father/SafeNest`) | Select a child, retrieve its digital rule, preserve `ruleId`, and submit blocked-app updates. |
| Child (`app_child/SafeNest-Kids`) | Pair the device, receive FCM data messages, synchronize rules, cache them locally, and enforce blocked packages through AccessibilityService. |

## Requirements

| ID | Requirement | Acceptance evidence |
|---|---|---|
| F1 | Parent updates use the server-provided `ruleId`. | `InstalledAppsFragment` stores `state.data.ruleId` and passes it to Retrofit `PUT api/digital-control/{rule_id}`. |
| F2 | Child handles a `RULES_UPDATED` data message in background-capable code. | `SafeNestFirebaseService` enqueues unique one-time WorkManager work. |
| F3 | Rule synchronization persists blocked packages and per-app limits locally. | `RuleSyncWorker` updates `PrefsHelper` after a successful device-rule response. |
| F4 | Failed synchronization retries. | `RuleSyncWorker` returns `Result.retry()` for unsuccessful responses and exceptions. |
| F5 | A paired Child device refreshes rules after boot or app replacement. | `ServiceWatchdogReceiver` enqueues `boot_rule_sync`. |
| F6 | Accessibility enforcement does not depend on a network request for every foreground event. | `AppBlockerAccessibilityService` reads the local blocked-app cache and launches `BlockedAppActivity`. |
| F7 | FCM token refresh is sent to the Backend from a coroutine when the Child is paired. | `SafeNestFirebaseService.onNewToken` uses an IO coroutine scope. |
| F8 | The Child build and device test requirements are documented. | This README and the Child README describe USB, Accessibility, Usage Access, and battery-exemption setup. |

## Reliability boundaries
FCM delivery, Android Doze behavior, vendor battery policies, and AccessibilityService availability are device and deployment concerns. A passing local build does not prove live enforcement. The operational test must measure timestamps for parent save, Backend dispatch, FCM receipt, rule-sync success, and blocked-activity launch.

## IoT boundary
This feature does not modify or depend on IoT sensor files or IoT alert-debounce logic.
