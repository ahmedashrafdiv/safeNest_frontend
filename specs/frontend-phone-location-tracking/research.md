# Research: Phone-Based Child Location Tracking

## Decision 1: Use a dedicated visible location service

The Child location collector will be separate from `AppBlockerAccessibilityService` and `WebsiteDnsVpnService`. Active collection requires explicit Android location consent and a visible foreground notification. This keeps app enforcement, website enforcement, and location collection independently observable and permissioned.

## Decision 2: Use WorkManager only for bounded reconciliation and retry

A foreground service handles active updates. WorkManager and the existing boot/package/FCM lifecycle can reconcile policy and service state and retry failed uploads with bounded backoff. Periodic work is not treated as real-time tracking because Android may defer it.

## Decision 3: Filter locally before upload

The Child will avoid redundant uploads when the location has not moved enough, the accuracy is unacceptable, or the report is older than the current accepted point. This reduces battery and network use while preserving the latest valid report.

## Decision 4: Make every health state visible

The Child UI will not show a generic protection success message when location permission is denied, service stopped, network unavailable, or the last report is stale. The state is persisted so the Home screen remains truthful after navigation and restart.

## Decision 5: Extend the existing Parent map

`GpsFragment` remains the primary map surface. The loose map payload will be mapped into a typed location envelope with source, age, accuracy, and status. External GPS remains a separate management flow and is labeled when selected as fallback.

## Decision 6: Keep latest point only

The first release stores and displays the latest accepted point, not a movement history. This reduces sensitive retention and leaves route history for a separate feature specification.
