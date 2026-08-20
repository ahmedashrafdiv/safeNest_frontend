# Tasks — Child Home Screen and Parent-Protected Controls

Legend: `[ ]` open, `[x]` done. Each phase is reviewed and committed before the next begins.

## Phase 1: Backend child-device session endpoints

- [x] T001 Add `app/routes/child_device_session_routes.py` with `GET /api/child-devices/{device_id}/session-profile` returning child id, child name, parent id, and parent email for the bound device.
- [x] T002 Add `POST /api/child-devices/{device_id}/parent-verification` to the same module, verifying the supplied password against the bound parent's stored hash.
- [x] T003 Enforce the device-scope check on both endpoints so a token for one device cannot read or verify through another device's path.
- [x] T004 Add per-device throttling to the verification endpoint: five consecutive failures lock further attempts for fifteen minutes, and a successful verification resets the counter.
- [x] T005 Register the router in `app/main.py` beside the other child-device routers.
- [x] T006 Add `tests/test_child_device_session_routes.py` covering the profile payload, ownership mismatch, correct password, wrong password, and the lockout transition.
- [x] T007 Run the backend regression and compile checks and record the result.

## Phase 2: Child app data layer and pure logic

- [x] T008 Add API models for the session profile, the parent verification request and response, the screen-time decision, and the extra-time access request in `network/ApiModels.kt`.
- [x] T009 Add the matching endpoints to `network/KidsApiService.kt`.
- [x] T010 Extend `util/PrefsHelper.kt` with the cached child name, parent email, and the protection-suspended flag, plus a routine that clears the cached enforcement policy.
- [x] T011 Add `util/ScreenTimeBudget.kt` converting a screen-time decision into a remaining-minute label and a ring sweep fraction, modelling the no-policy and over-budget cases explicitly.
- [x] T012 Add `util/ChildGreeting.kt` building the greeting line with a fallback when the child name is unknown.
- [x] T013 Add `util/ParentVerificationDecider.kt` mapping an HTTP status to a verification outcome.
- [x] T014 Add JVM unit tests for `ScreenTimeBudget`, `ChildGreeting`, and `ParentVerificationDecider`.

## Phase 3: Layngo Home screen

- [ ] T015 Add the drawable resources the screen needs: the menu glyph, the clock glyph, the help glyph, the calendar glyph, and the card and button backgrounds.
- [ ] T016 Add `view/BudgetRingView.kt` drawing the track and progress sweep.
- [ ] T017 Rewrite `res/layout/fragment_home.xml` as the Layngo screen: header, greeting, ring, extra-time card, help action, and bottom navigation.
- [ ] T018 Add the new user-facing strings to `res/values/strings.xml` rather than embedding them in code.
- [ ] T019 Rewrite `HomeFragment.kt` to bind the new layout and load the session profile and screen-time decision, preserving every existing worker registration and installed-apps sync.
- [ ] T020 Render the suspended state on the Home screen in place of the ring when protection is suspended.
- [ ] T021 Build the child application and confirm the screen renders.

## Phase 4: Menu and parent-password gate

- [ ] T022 Add the menu sheet opened from the header with exactly the sign-out and disable entries.
- [ ] T023 Add the parent verification dialog with the pre-filled disabled email field, the password field, an error line, and a progress state.
- [ ] T024 Implement the sign-out action: clear the token, identifiers, and cached policy, cancel the workers, stop the services, and return to the pairing screen. When the device holds the HOME role, also send the parent to the system home-app settings — Android offers no API to release `ROLE_HOME`, so without this the handset keeps launching the unpaired Child app as its launcher.
- [ ] T025 Implement the disable action: set the suspended flag, clear the cached enforcement policy, and cancel the periodic sync work while keeping the pairing.
- [ ] T026 Make `RuleSyncWorker`, `ScreenTimePolicySyncWorker`, `WebsitePolicySyncWorker`, `PhoneLocationPolicySyncWorker`, and `ProtectedHomePolicySyncWorker` return early while protection is suspended.
- [ ] T027 Offer the delete-application action in the suspended state, removing device-admin registration before opening the system uninstall screen.
- [ ] T028 Offer re-enabling protection from the suspended state behind the same parent verification.
- [ ] T029 Confirm the parent password is never written to preferences or logs.

## Phase 5: Extra-time request and final verification

- [ ] T030 Wire the extra-time action to the access-request endpoint with a stable client request id and report the submitted, duplicate, and failed outcomes.
- [ ] T031 Give the help action and the bottom-navigation destinations their behaviour.
- [ ] T032 Run the child unit tests and the debug build.
- [ ] T033 Confirm the feature diff contains no file under `app_father/` and no change to `AppBlockerAccessibilityService.kt`.
