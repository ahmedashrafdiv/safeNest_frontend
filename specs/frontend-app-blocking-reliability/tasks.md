# Frontend Tasks: App Blocking Reliability

## Phase 1: Parent policy update
- [x] Retrieve and retain the server-provided `ruleId`.
- [x] Submit blocked-app changes through `PUT api/digital-control/{rule_id}`.
- [x] Preserve Parent behavior without introducing device-side enforcement responsibilities.

## Phase 2: Child immediate synchronization
- [x] Receive `RULES_UPDATED` from FCM data payloads.
- [x] Enqueue unique one-time WorkManager synchronization.
- [x] Persist blocked packages and per-app limits in `PrefsHelper`.
- [x] Retry failed rule synchronization.
- [x] Send refreshed FCM tokens through the authenticated device API.

## Phase 3: Recovery and enforcement
- [x] Load the Child-reported installed-app inventory in the Parent UI.
- [x] Display human-readable app labels while retaining package names for policy actions.
- [x] Provide direct Parent actions for block/unblock and per-app time limits.
- [x] Align Parent installed-app models with the Backend `{ package_name, app_name }` contract.
- [x] Trigger a synchronization after boot or package replacement for paired devices.
- [x] Keep periodic 15-minute synchronization as a fallback path.
- [x] Enforce blocked packages from local cache through AccessibilityService.
- [x] Build Parent and Child debug APKs with the command-line Android SDK.
- [ ] Run a live paired-device test measuring FCM-to-block latency.
- [ ] Add Android automated tests for FCM payload handling and WorkManager enqueue behavior.
