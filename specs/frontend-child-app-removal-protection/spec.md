# Feature Specification: Child App Removal Protection

The Child app must expose truthful protection state, support official managed-device capability detection, and provide a visible parent-authorized recovery flow. The app must remain visibly identified as Layngo. Hidden icons, deceptive game branding, and Accessibility-based system-screen clicking are out of scope.

## Acceptance Criteria

- Consumer mode reports that uninstall protection is not guaranteed.
- Device Owner/Profile Owner mode reports guaranteed protection only after Android confirms policy application.
- The Child never stores the Parent raw email/password.
- Recovery approvals are short-lived, single-use, action-scoped, and device-bound.
- Management loss, policy failure, and stale health are visible and uploaded.
- IoT files are not changed.

## Clarifications

- [ASSUMED] Device Owner/Profile Owner provisioning is a separate onboarding path and may require a reset test device.
- [ASSUMED] Android is the enforcement authority; Backend only authorizes and audits.
- [ASSUMED] Normal app mode cannot guarantee prevention of uninstall.

