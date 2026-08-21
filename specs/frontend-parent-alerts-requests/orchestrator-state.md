# Orchestrator State: Parent Alerts and Requests Inbox

- Active spec: `frontend-parent-alerts-requests`.
- Current phase: Phase 5 — Quality and physical verification.
- Scope: Parent `ParentInboxFragment`, `fragment_parent_inbox.xml`, current inbox repository/view-model, focused presentation models/tests/resources only.
- Exclusions: IoT files, Child app enforcement, Backend schema/endpoints, unrelated `NotificationsFragment` migration.
- Toolchain: Parent Android Gradle using `JAVA_HOME=D:\Android\jdk\temurin-17`; validation command is `gradlew.bat testDebugUnitTest assembleDebug`.
- Baseline: Parent `testDebugUnitTest` and `assembleDebug` passed before changes.
- Presentation layer: `ParentInboxPresentationTest` passed before the final Locale API cleanup; the full Parent suite will be rerun after UI implementation.
- Verification: Parent `testDebugUnitTest` and `assembleDebug` passed after final inbox implementation. `001review.md` has one open item only: physical Realme walkthrough.
- Device status: pending; `adb devices -l` أعاد قائمة فارغة بعد إكمال build/review.
