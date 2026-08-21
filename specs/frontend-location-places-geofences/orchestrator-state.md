# Orchestrator State: Layngo Location Places and Geofences

| Field | Value |
|---|---|
| Current phase | Phase 4 — Realme sync verified; transition verification awaits a parent-created place |
| Source plan | `/home/ubuntu/plan_location_places_geofences.md` |
| IoT boundary | No IoT/ThingSpeak/external-GPS modifications |
| GitHub | Backend session-profile and Places v2 releases published after explicit user confirmation |
| Device verification | Realme RMX2040: background location granted; App Blocking accessibility service bound; authenticated place sync returned `200` with `place_version: 0` and an empty list; a Parent-created place and physical transition remain pending |
| Validation commands | Backend `python -m pytest -q`; Parent/Child `gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain` |
