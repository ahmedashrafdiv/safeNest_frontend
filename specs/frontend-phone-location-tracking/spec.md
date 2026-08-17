# Feature Specification: Phone-Based Child Location Tracking

**Feature Branch**: `frontend-phone-location-tracking`

**Created**: 2026-08-17

**Status**: Draft

**Input**: User description: "The system already has GPS from an external sensor. Add tracking from the Child's phone so the Parent can open the app and see where the Child is."

## User Scenarios & Testing

### User Story 1 - Child grants and maintains phone tracking (Priority: P1)

The Child app explains why location access is needed, requests the required Android permission, starts visible phone-location tracking only after consent, and shows whether tracking is active, denied, unavailable, stale, or recovering.

**Why this priority**: The Parent cannot receive phone-originated location unless the Child device has explicit permission and a reliable lifecycle for collecting and uploading reports.

**Independent Test**: Install the Child app on a paired device, deny and grant location permission, start and stop tracking, and verify each visible state and service behavior without requiring the Parent map.

**Acceptance Scenarios**:

1. **Given** the Child is paired and location permission is not granted, **When** the Child opens the permissions screen, **Then** the app explains phone tracking and provides an explicit permission action without claiming active tracking.
2. **Given** permission is granted and tracking is enabled, **When** the tracking service starts, **Then** the app displays an ongoing tracking notification and a clear active status.
3. **Given** permission is revoked, the service is stopped, or the device is offline, **When** the Child status refreshes, **Then** the app reports unavailable/degraded tracking and does not claim that the Parent is receiving current location.

---

### User Story 2 - Child synchronizes accepted location reports (Priority: P1)

The Child app collects valid location updates using a battery-conscious cadence, filters redundant or low-quality updates, uploads authenticated reports to the Backend, and retries transient network failures within bounded limits.

**Why this priority**: Reliable synchronization is the direct bridge between the Child phone and the Parent location screen.

**Independent Test**: Feed the location component valid, duplicate, low-accuracy, permission-denied, and network-failure scenarios and verify acceptance, rejection, retry, and persisted health outcomes.

**Acceptance Scenarios**:

1. **Given** tracking is active and a valid location is received, **When** the upload succeeds, **Then** the Child persists the last successful upload time and the report identity.
2. **Given** a duplicate or insufficiently changed location is received, **When** the filter evaluates it, **Then** the Child avoids unnecessary upload while preserving the latest valid local state.
3. **Given** the network is unavailable, **When** the upload fails, **Then** the Child uses bounded retry/backoff and exposes a network-unavailable or stale state.
4. **Given** the device reboots or the app is replaced, **When** recovery runs and permission/pairing still exist, **Then** the app reconciles tracking state without silently bypassing consent.

---

### User Story 3 - Parent sees phone location with source and freshness (Priority: P1)

The Parent app uses the existing map experience to display the latest normalized location envelope. It labels phone-originated location, external GPS fallback, last-update age, accuracy, stale data, disabled tracking, and unavailable states.

**Why this priority**: This delivers the requested management experience and prevents the Parent from interpreting an old point as the Child’s current position.

**Independent Test**: Supply normalized Parent API responses for phone, external GPS fallback, stale, disabled, and unavailable states and verify map marker, source label, timestamp, accuracy, and status presentation.

**Acceptance Scenarios**:

1. **Given** the API returns a fresh phone location, **When** the Parent opens or refreshes the map, **Then** the map shows the point with a phone-source label, capture age, and accuracy.
2. **Given** the API returns a fresh external GPS fallback, **When** the Parent opens the map, **Then** the map shows the point with an external-GPS label instead of implying it came from the phone.
3. **Given** the API returns stale or unavailable data, **When** the Parent opens the map, **Then** the UI explains the state and does not present the marker as live/current.
4. **Given** phone tracking is disabled, **When** the Parent views the location screen, **Then** the UI shows tracking disabled and preserves the separate external GPS management state.

---

### User Story 4 - Parent controls phone tracking without affecting external GPS (Priority: P2)

The Parent can enable or disable phone tracking for a selected Child. The control is distinct from external GPS pairing and does not remove or rewrite external sensor data.

**Why this priority**: Parents need control and privacy boundaries, but the core read/upload flow can be tested independently first.

**Independent Test**: Toggle phone tracking, verify the Child policy/status changes, and verify that external GPS pairing and data remain unchanged.

**Acceptance Scenarios**:

1. **Given** phone tracking is enabled, **When** the Parent disables it, **Then** the Child stops new phone reports after synchronization and the Parent sees a disabled state.
2. **Given** external GPS is paired, **When** phone tracking is disabled or phone data is deleted, **Then** the external GPS pairing and records remain unchanged.

---

### Edge Cases

- Android permission is denied, revoked, limited, or unavailable on the device.
- Battery optimization, OEM process management, reboot, package replacement, or another foreground service interrupts tracking.
- The fused location provider returns null, stale, low-accuracy, or duplicate updates.
- Network retries continue beyond the configured bound.
- The Backend rejects a report due to authorization, coordinate validation, timestamp skew, or policy disablement.
- The Parent API returns a legacy external-GPS payload without source/freshness fields during migration.
- The map cannot render or the selected Child is missing.
- The phone report is stale while external GPS is fresh, and vice versa.
- The user disables phone tracking while an upload is in flight.

## Clarifications

### Session 2026-08-17

- Q: What is the first-release tracking history scope? → A: Latest accepted point only; breadcrumb history is deferred.
- Q: Which source should the Parent prefer when both sources are fresh? → A: Fresh phone location by default, with explicit source labeling and external GPS fallback when phone data is stale or unavailable.
- Q: How should failed, denied, or stale tracking be represented? → A: Explicit unavailable/degraded status; never label stale coordinates as live.
- Q: What must remain outside this feature? → A: External GPS/ThingSpeak behavior, existing app-blocking services, website VPN behavior, and all IoT files remain unchanged.

No critical ambiguities remain that require user input before planning. Exact Android provider APIs and update cadence are implementation decisions bounded by the plan and acceptance tests.

## Requirements

### Functional Requirements

- **FR-001**: The Child app MUST request explicit coarse/fine Android location permission before collecting phone location.
- **FR-002**: The Child app MUST show a visible foreground tracking notification while active background location collection is running.
- **FR-003**: The Child app MUST persist tracking health, last successful upload time, last accepted report identity, and policy version.
- **FR-004**: The Child app MUST filter redundant, invalid, permission-denied, and low-quality updates before upload.
- **FR-005**: The Child app MUST upload reports only for the authenticated paired Child device and MUST use bounded retry/backoff.
- **FR-006**: The Child app MUST recover through approved Android lifecycle mechanisms without bypassing consent or claiming active tracking when recovery fails.
- **FR-007**: The Parent app MUST consume a normalized location envelope with source, accuracy, captured time, received time, age, stale flag, and availability status.
- **FR-008**: The Parent app MUST display phone location separately from external GPS location and must label fallback source explicitly.
- **FR-009**: The Parent app MUST show truthful states for active, stale, unavailable, permission denied, service stopped, network unavailable, and tracking disabled.
- **FR-010**: The Parent app MUST not delete or rewrite external GPS records when phone tracking is disabled or phone data is removed.
- **FR-011**: The existing AccessibilityService app enforcement and DNS website-filtering VPN MUST remain separate from the phone-location collection service.
- **FR-012**: The implementation MUST NOT modify IoT-related files, including sensor routes and external GPS integration.
- **FR-013**: All new models, repository methods, service logic, and tests MUST be written in English; current localized UI conventions may be preserved for visible copy.

### Key Entities

- **ChildTrackingState**: Local Child state for permission, enabled flag, service health, last upload, and policy version.
- **PhoneLocationUpload**: Child API payload for a validated phone location report.
- **LocationEnvelope**: Parent API response used by the map and status UI.
- **PhoneTrackingPolicy**: Parent-managed or server-provided tracking settings.
- **LocationSourceStatus**: UI state describing phone, external GPS, stale, unavailable, disabled, or fallback source.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A paired Child with granted permission shows active tracking and an ongoing notification within 10 seconds of enabling tracking.
- **SC-002**: In normal connectivity, a successful Child upload causes the Parent to show the phone source and last-update age after the next refresh.
- **SC-003**: 100% of permission-denied, service-stopped, network-failure, and stale-state UI tests avoid displaying active/current phone tracking.
- **SC-004**: 100% of source-fallback tests label external GPS explicitly and never mislabel it as phone location.
- **SC-005**: Android unit tests cover permission gating, location filtering, retry/backoff state, and stale/local-health transitions.
- **SC-006**: Parent and Child debug APKs build successfully with no changes to IoT files.
- **SC-007**: A paired-device test proves the Parent can view a fresh phone-originated point and truthful degraded states after permission denial, reboot, and network loss.

## Assumptions

- Existing Retrofit authentication and paired Child-device token behavior remain in use.
- The Android project targets API 34 and can add the required foreground-service location declarations without changing unrelated services.
- The first release shows the latest accepted point and does not implement a full breadcrumb/history timeline.
- Exact continuous movement, indoor precision, and recovery after force-stop are not guaranteed and will be presented as limitations where relevant.
- The Parent’s existing map screen is the primary display surface, extended with typed source/freshness metadata.
- The external GPS sensor path remains separate and all IoT files are out of scope.
