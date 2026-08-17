# Quickstart: Phone-Based Child Location Tracking

## Automated checks

From the Child project, run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain
```

From the Parent project, run:

```powershell
.\gradlew.bat assembleDebug --no-daemon --console=plain
```

## Child permission and service scenario

1. Install the Child debug APK on a paired test phone.
2. Open the permissions screen and confirm the phone-location explanation is visible.
3. Deny location permission and confirm the status is denied/unavailable and no upload is attempted.
4. Grant the required location permission and enable phone tracking.
5. Confirm the foreground notification appears and the Home status changes to active only after service startup.
6. Disable network access and confirm the status becomes offline/retrying/stale rather than active-current.
7. Restore network access and confirm the next accepted upload records a successful timestamp.
8. Reboot the phone and confirm recovery requires permission and pairing and does not bypass consent.

## Parent map scenario

1. Publish/enable phone tracking for the selected Child.
2. Wait for a successful Child upload.
3. Open the Parent location map and confirm the marker, source `phone`, accuracy, and age are visible.
4. Make the phone report stale and confirm the UI labels it stale rather than live.
5. Provide a fresh external GPS point and confirm fallback is labeled `external_gps`.
6. Disable phone tracking and confirm the Parent shows disabled while the external GPS management state is unchanged.

## Release gate

The paired-device scenario is required because local JVM tests and APK compilation cannot prove real Android permission behavior, foreground-service execution, OEM battery behavior, network retries, deployed authentication, or map refresh timing.
