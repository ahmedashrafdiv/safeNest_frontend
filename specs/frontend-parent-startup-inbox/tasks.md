# Tasks: Parent Startup Inbox

## Phase 1: Contracts and data state

- [x] Add Parent models for pending access-request list and approve/reject payloads.
- [x] Add Retrofit methods for pending requests and decisions.
- [x] Load all Parent children before requesting their pending access requests.
- [x] Load unresolved alerts through the existing alert endpoint.

## Phase 2: Inbox UI and interactions

- [x] Add RTL Layngo startup inbox layout.
- [x] Render request cards before notification cards.
- [x] Add Allow and Reject actions with loading protection.
- [x] Add notification review action through the alert API.
- [x] Add loading, empty, partial-data, error, retry, close, and continue states.

## Phase 3: Startup routing

- [x] Route authenticated app startup through the inbox.
- [x] Route successful login through the inbox.
- [x] Continue directly to Home when there are no unresolved records.
- [x] Preserve normal notification navigation separately.

## Phase 4: Verification

- [x] Build the Parent debug APK.
- [ ] Add focused ViewModel/repository tests where supported by the current Android test setup.
- [ ] Run manual device verification for pending request actions and startup routing.
- [ ] Complete spec review and local review.
- [ ] Create a local commit without pushing.
