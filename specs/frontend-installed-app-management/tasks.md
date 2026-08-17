# Tasks: Frontend Installed-App Management

## Phase 1: Contract and inventory models

- [x] Align Parent installed-app models with `{ package_name, app_name }`.
- [x] Retain package name as the policy identity.
- [x] Display human-readable app names with package-name fallback.
- [x] Load the selected Child inventory when the screen opens.

## Phase 2: Inventory UX and policy actions

- [x] Refresh the inventory when the screen resumes or selected Child changes.
- [x] Render explicit loading and empty states.
- [x] Render an explicit error/retry state for failed inventory requests.
- [x] Provide direct block/unblock actions per installed package.
- [x] Provide per-app time-limit actions using `app_time_limits` keyed by package name.
- [x] Prevent duplicate policy submissions while an action is in flight.

## Phase 3: Automated and build verification

- [ ] Add focused tests for installed-app JSON parsing and package-name identity.
- [ ] Add focused tests for block/unblock and per-app-limit payload construction.
- [ ] Add focused tests for loading, empty, and error UI states where supported.
- [x] Build the Parent debug APK.
- [x] Build the Child debug APK.
- [ ] Run configured Android unit tests.

## Phase 4: Automatic inventory synchronization

- [x] Add a durable `InstalledAppsSyncWorker` that scans and uploads the current inventory.
- [x] Persist the last successful inventory fingerprint and skip unchanged snapshots.
- [x] Retry failed uploads without advancing the persisted fingerprint.
- [x] Add package-added, package-removed, and package-replaced broadcast handling.
- [x] Coalesce rapid package events through unique WorkManager work.
- [x] Reuse the inventory worker for boot and Child-app replacement recovery.
- [ ] Route manual refresh through the same synchronization path where practical.

## Phase 5: Verification

- [x] Add tests for inventory normalization and fingerprint changes.
- [ ] Add worker tests for unpaired, unchanged, changed, success, and retry cases where supported.
- [x] Verify Parent and Child debug builds.
- [x] Run the configured Android test tasks.
- [ ] Perform live ADB verification for install, remove, replace, offline retry, and reboot recovery.

## Phase 6: Quality gate

- [ ] Complete spec-review and review-fix.
- [ ] Complete local code review for this feature.
- [ ] Create a local phase commit without pushing.
