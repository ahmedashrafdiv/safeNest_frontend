# Spec Review: frontend-app-control-modes

## Summary

The implementation is locally verified for compilation and deterministic policy behavior. The only open verification item is a live paired-device test because no Android device was connected through ADB.

| Area | Status | Evidence |
|---|---|---|
| Parent mode models and payloads | PASS | Parent APK build succeeded. |
| Child synchronization and persistence | PASS | Child APK and unit tests succeeded. |
| Allowlist/blocklist decision logic | PASS | `AppPolicyDeciderTest` covers selected/unselected/new/protected packages and legacy default behavior. |
| Backend contract and persistence | PASS | `179 passed, 0 failed`; schema/model tests included. |
| Live FCM and Accessibility enforcement | PENDING | Requires paired Parent/Child devices. |

## Open issue

### Live paired-device verification is pending

This is an environment-dependent verification gap, not an observed code failure. The required test matrix is: allowlist blocks an existing unselected app and a newly installed app; blocklist blocks selected apps while leaving unselected/new apps open; mode changes propagate through FCM/WorkManager; and Settings/launcher/permission recovery remain usable.

No local build result is being represented as proof of live device behavior.
