# Frontend Contracts: Phone-Based Child Location Tracking

## Child upload contract

The Child calls `POST /api/child-devices/{device_id}/location` with a stable `report_id`, latitude, longitude, accuracy, optional altitude/speed, and `captured_at`. The existing authenticated device token identifies the Child device. A successful response is persisted as the latest upload state. A duplicate response is treated as idempotent success; authorization, validation, disabled-policy, and conflict responses update a truthful health state and do not fabricate success.

## Parent read contract

The Parent calls `GET /location/live/{child_id}` and maps the response into `ParentLocationEnvelope`. New responses contain `availability_status`, `source`, coordinates, accuracy, capture/receive timestamps, age, and `is_stale`. The repository retains a compatibility mapper for legacy external-GPS coordinates during migration but never infers a phone source from a legacy shape.

## Permission contract

The Child must have an active paired identity and explicit coarse/fine location permission before starting collection. Active collection runs in a visible foreground service with a persistent notification. Permission denial, revocation, service stop, or unavailable provider causes `permission_denied`/`unavailable` status and no upload.

## Source and status contract

The Parent UI must label `phone` versus `external_gps`. `available` means a fresh accepted coordinate exists; `stale` means a previous coordinate exists but is beyond the freshness threshold; `unavailable` means no usable coordinate exists; `disabled` means phone tracking is intentionally off. No stale state may be rendered as a live/current tracking state.
