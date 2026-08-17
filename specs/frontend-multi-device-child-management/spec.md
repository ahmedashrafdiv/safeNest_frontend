# Frontend Multi-Device Child Management

## Goal
The Parent app lists devices under the selected child and allows creation of a pairing code or revocation of one named device. The Child app rejects a policy response bound to another enrollment or child.

## Clarifications
- [ASSUMED] Existing stable `PrefsHelper.getDeviceId()` is the enrollment identifier.
- [ASSUMED] Existing backend ChildDevices routes are authoritative.
- [ASSUMED] App/website/time/location overrides remain a follow-up integration after device selection is deployed.
