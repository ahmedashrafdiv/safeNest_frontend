# Tasks: Parent Alerts and Requests Inbox

## Phase 1: Scope and baseline

- [x] PAR-001 Confirm `ParentInboxFragment` is the existing startup inbox and document its current repository/view-model contract.
- [x] PAR-002 Run Parent `testDebugUnitTest` and `assembleDebug` baseline without staging concurrent changes.

## Phase 2: Arabic presentation layer

- [x] PAR-003 Add a pure `ParentInboxPresentation` model/mapper for request cards, alert cards, section labels, and safe fallback content.
- [x] PAR-004 Add behavior-focused JVM tests for extra time, app access, known alert classes, unknown alerts, and non-sensitive location copy.

## Phase 3: Decision inbox UI

- [x] PAR-005 Rebuild `fragment_parent_inbox.xml` as the RTL Layngo screen with compact app bar, pending summary, two sections, and loading/empty/partial/error/end states.
- [x] PAR-006 Rework `ParentInboxFragment` card rendering with vector icons, readable statuses, 48dp targets, and at most two decisions per request / one action per alert.

## Phase 4: Live decisions and navigation

- [x] PAR-007 Implement in-card pending/success/failure feedback for approve/reject using the existing endpoints and no unsupported undo.
- [x] PAR-008 Add mark-all-read for unresolved alerts with safe sequential resolution and retry feedback.
- [x] PAR-009 Route app-rule review and location review through existing Parent destinations only when context is available; otherwise explain safely.

## Phase 5: Verification and closeout

- [x] PAR-010 Run Parent tests and Debug build; review modified test code with Test Guard.
- [x] PAR-011 Run Spec Review and scoped Code Review with no Parent/Child scope bleed; no production review fix is required before device testing.
- [ ] PAR-012 Verify the inbox flow on Realme when available and document PASS/FAIL without fabricating device evidence.
