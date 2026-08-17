# Orchestrator State

- Spec: `specs/frontend-phone-location-tracking`
- Repository: Frontend (`frontend_app`)
- Branch: local working tree
- Spec resolved via: `.specify/feature.json` using `feature_directory`
- Tool modes: specify=INLINE, clarify=INLINE, plan=INLINE, tasks=INLINE, implement=INLINE, spec-review=INLINE, review-fix=INLINE, code-review=MISSING
- Child test/build: `app_child/SafeNest-Kids/gradlew.bat testDebugUnitTest assembleDebug --no-daemon --console=plain`
- Parent build: `app_father/SafeNest/gradlew.bat assembleDebug --no-daemon --console=plain`
- Lint: not configured
- Type check: not configured
- IoT/external-GPS boundary: protected; no sensor, ThingSpeak, or unrelated enforcement files may be changed

## Workflow status

| Stage | Status | Evidence |
|---|---|---|
| Specify | DONE | `spec.md` and `checklists/requirements.md` created and validated |
| Clarify | DONE | Clarifications session recorded in `spec.md`; no critical ambiguity remains |
| Plan | DONE | `plan.md`, `research.md`, `data-model.md`, `quickstart.md`, and `contracts.md` created |
| Tasks | DONE | `tasks.md` contains dependency-ordered T001–T031 checklist |
| Implement | READY | Start with foundational models and pure location decision tests |
| Review/Fix | PENDING | Run after each implementation phase |

## Implementation phases

| Phase | Scope | Status |
|---|---|---|
| Phase 1 | Setup and foundational models/state/tests | READY |
| Phase 2 | Child permission and visible foreground service | PENDING |
| Phase 3 | Child upload, retry, and lifecycle recovery | PENDING |
| Phase 4 | Parent typed location envelope and map states | PENDING |
| Phase 5 | Parent tracking control and source isolation | PENDING |
| Phase 6 | Builds, review gates, and live device verification | PENDING |

## Commit policy

Create local commits after each phase gate. Do not push to GitHub without explicit user confirmation.
