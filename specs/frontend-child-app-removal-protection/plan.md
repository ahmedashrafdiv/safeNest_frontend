# Implementation Plan

1. Add pure protection-state and capability decision logic.
2. Add DeviceAdmin/DPC capability detection and confirmed policy status.
3. Add visible protection-health UI and durable health reporting.
4. Add parent challenge-response recovery flow without raw credentials.
5. Add reboot/package replacement/tamper recovery and tests.

## Boundaries

Use existing Kotlin/XML patterns, WorkManager, Retrofit, and PrefsHelper conventions. Keep consumer mode functional. Do not disguise or hide the app. Do not modify IoT files.

