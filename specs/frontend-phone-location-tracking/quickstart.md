# Quickstart: Phone-Based Child Location Tracking

## Automated checks

On the Windows build machine, set the available SDK/JDK paths first:

```powershell
$env:ANDROID_HOME = 'D:\\Android\\sdk'
$env:ANDROID_SDK_ROOT = 'D:\\Android\\sdk'
$env:JAVA_HOME = 'D:\\Android\\jdk\\temurin-17'
$env:Path = "$env:JAVA_HOME\\bin;$env:Path"
```

From `frontend_app\\app_child\\SafeNest-Kids`, run:

```powershell
.\\gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain
```

The Child APK is produced at `app\\build\\outputs\\apk\\debug\\app-debug.apk`.

From `frontend_app\\app_father\\SafeNest`, run:

```powershell
.\\gradlew.bat assembleDebug --no-daemon --console=plain
```

The Parent APK is produced at `app\\build\\outputs\\apk\\debug\\app-debug.apk`.

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

Record each result as `PASS`, `FAIL`, or `BLOCKED` with timestamp, device model, Android version, APK version, and Backend URL. The paired-device scenario is required because local JVM tests and APK compilation cannot prove real Android permission behavior, foreground-service execution, OEM battery behavior, network retries, deployed authentication, or map refresh timing.

The gate is not complete until the following cases are run on a real paired Child/Parent set:

| Case | Expected result |
|---|---|
| Location permission denied | Child reports denied/unavailable; no active claim and no successful upload |
| Approximate or precise permission granted | Foreground location notification appears; Child status becomes active only after service startup |
| Real upload | Backend accepts a valid report; Parent map shows source `phone`, age, and accuracy |
| Duplicate retry | Same `report_id` is idempotent; no duplicate latest-state corruption |
| Network loss and recovery | Child shows offline/retrying/stale; a later connected run succeeds |
| Reboot/app replacement | Watchdog restarts only when paired, enabled, and permission remains granted |
| Phone tracking disabled by Parent | Parent shows disabled; Child stops active reporting; external GPS state remains unchanged |
| Fresh external GPS fallback | Parent labels source `external_gps` when phone state is absent/stale and external data is fresh |
| Stale phone point | Parent never labels it live/current |
| Unauthorized Parent | Backend rejects another Parent's read/control request |
