# Data Model: Phone-Based Child Location Tracking

## ChildTrackingState

| Field | Type | Purpose |
|---|---|---|
| `enabled` | Boolean | Parent policy says whether new phone reports are allowed |
| `permissionState` | Enum | Granted, denied, revoked, unavailable |
| `serviceState` | Enum | Active, stopped, starting, failed |
| `networkState` | Enum | Online, retrying, offline |
| `lastAcceptedReportId` | String? | Idempotency key of latest locally accepted report |
| `lastSuccessfulUploadAt` | Instant? | Last confirmed Backend upload |
| `lastAttemptAt` | Instant? | Last attempted upload |
| `lastCapturedAt` | Instant? | Latest provider capture time |
| `policyVersion` | Int | Tracking policy version applied locally |
| `status` | Enum | Active, permission denied, stopped, offline, stale, disabled, unavailable |

## PhoneLocationUpload

| Field | Type | Validation |
|---|---|---|
| `reportId` | String | Stable idempotency key per accepted report |
| `latitude` | Double | `-90..90` |
| `longitude` | Double | `-180..180` |
| `accuracyMeters` | Float | Positive and within configured maximum |
| `altitudeMeters` | Double? | Optional |
| `speedMps` | Float? | Optional and non-negative |
| `capturedAt` | Instant | Must not move backwards beyond tolerated clock skew |

## ParentLocationEnvelope

| Field | Type | Purpose |
|---|---|---|
| `availabilityStatus` | Enum | Available, stale, unavailable, disabled |
| `source` | Enum? | Phone or external GPS |
| `latitude` / `longitude` | Double? | Selected map point |
| `accuracyMeters` | Float? | Provider accuracy |
| `capturedAt` | Instant? | Source capture time |
| `receivedAt` | Instant? | Backend receive time |
| `ageSeconds` | Long? | Freshness shown to Parent |
| `isStale` | Boolean | Prevents live claim for old data |
| `messageCode` | String? | Stable UI status mapping |

## UI state transitions

The Child screen begins at `permission_denied`, `disabled`, or `unavailable`. After consent, pairing, and enabled policy it transitions to `starting` and then `active`. Upload failures transition to `retrying`/`offline`; an old last-success time transitions to `stale`; a successful accepted upload returns to `active`. The Parent map shows a marker only when coordinates are present, but status text always indicates source and freshness.
