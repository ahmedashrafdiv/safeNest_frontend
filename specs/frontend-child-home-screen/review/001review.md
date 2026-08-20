# Spec Review: frontend-child-home-screen
- Branch: `main` (both `frontend_app` and `backend/safenest_review`)
- Spec resolved via: argument
- Resolution conflicts: **Yes.** `frontend_app/.specify/feature.json` records `frontend-daily-usage-accuracy`, not this spec. A concurrent effort owns that selector, so this review was pinned to the supplied spec name deliberately and `feature.json` was left untouched. Any later `spec-review` / `review-fix` run on this feature must also pass the spec name explicitly or it will land in the wrong folder.
- Scope reviewed: **Phase 1 only (T001–T007)**, commit `db66bed` in `backend/safenest_review`
- Review file: 001review.md
- Detected commands: test=`.\.venv\Scripts\python.exe -m pytest -q` lint=`not configured` (no ruff/flake8 config, `ruff` absent from the venv) types=`not configured`

## Summary
- Overall status: **PARTIAL**
- High-risk issues: one confirmed crash path — a bound parent record with no stored password hash makes the verification endpoint return HTTP 500 instead of a clean rejection, and bypasses the failed-attempt counter that the whole lockout design rests on.
- Missing tests / regression risk: the `parent_not_found` branch on both endpoints and the missing-hash branch are unexercised.
- Test suite results: **265 passed, 3 failed** (`254 passed` in the pre-existing suite plus the 11 new tests; the 3 failures are all in `tests/test_daily_usage_accuracy.py`).
- Pre-existing failure confirmed independent: the 3 failures raise `ModuleNotFoundError: No module named 'tzdata'` / `ZoneInfoNotFoundError: 'No time zone found with key UTC'`. They are environment-level (missing Windows timezone database in `.venv`) and touch no file in this phase.
- Lint results: not configured
- Type check results: not configured

## Task-by-task Verification

### Task T001: `GET /api/child-devices/{device_id}/session-profile`
- Spec requirement: return child id, child name, parent id, and parent email for the bound device; `403` when the child is not owned by the bound parent; `404` when a record is missing.
- Implementation found:
  - Files: `app/routes/child_device_session_routes.py:44-73`
  - Key symbols: `read_session_profile`, `ChildDeviceSessionProfile`
- Status: **PASS**
- Evidence: reads `Children/{child_id}` and `Parents/{parent_id}`, asserts `child_data["parentID"] == parent_id` before returning, and raises `child_not_found` / `parent_not_found` as `404`. Verified by `test_session_profile_returns_child_name_and_parent_email`, `test_session_profile_rejects_child_owned_by_another_parent`, `test_session_profile_reports_missing_child_record`.
- Problems: see Issue 2 — an absent `email` field degrades to `""` silently.

### Task T002: `POST /api/child-devices/{device_id}/parent-verification`
- Spec requirement: verify the supplied password against the bound parent's stored hash; `{"verified": true}` on success, `401` on mismatch.
- Implementation found:
  - Files: `app/routes/child_device_session_routes.py:76-124`
  - Key symbols: `verify_parent_password`, `ParentVerificationRequest`, `ParentVerificationResponse`
- Status: **PARTIAL**
- Evidence: the happy path and the mismatch path are correct and covered by `test_correct_parent_password_is_verified` and `test_wrong_parent_password_is_rejected_and_counted`.
- Problems: the hash is passed to passlib as `parent_data.get("password") or ""`. Confirmed empirically that `verify_password("anything", "")` raises `UnknownHashError` while `verify_password("anything", None)` returns `False` — so the `or ""` fallback converts a safely-falsy value into a crash. See Issue 1.

### Task T003: device-scope enforcement on both endpoints
- Spec requirement: a token issued for one device must not read or verify through another device's path.
- Implementation found:
  - Files: `app/routes/child_device_session_routes.py:39-42` (`_check_path`), called first in both handlers at lines 50 and 84
  - Key symbols: `_check_path`
- Status: **PASS**
- Evidence: `_check_path` compares the path `device_id` against the token's `device_id` and raises `403 device_not_authorized`. It runs before any Firestore read and before the lockout lookup, so a mismatched path cannot consume another device's attempt budget. Covered by `test_session_profile_rejects_device_path_mismatch` and `test_verification_rejects_device_path_mismatch`.

### Task T004: per-device verification throttling
- Spec requirement: five consecutive failures lock further attempts for fifteen minutes; a success resets the counter.
- Implementation found:
  - Files: `app/routes/child_device_session_routes.py:15-17` (constants), `92-105` (lock check), `107-121` (counting and locking)
  - Key symbols: `MAX_FAILED_ATTEMPTS`, `LOCKOUT_MINUTES`, `VERIFICATION_COLLECTION`
- Status: **PARTIAL**
- Evidence: the lock engages after five failures (`test_repeated_failures_lock_further_attempts` asserts `429 verification_locked`), the window matches `LOCKOUT_MINUTES` (`test_lock_duration_matches_configured_window`), an expired lock lets verification through (`test_lockout_expires_and_allows_verification_again`), and success clears the record (`test_successful_verification_clears_previous_failures`).
- Problems: the counter is only reached when passlib returns a boolean. On the Issue 1 crash path the exception propagates before `failed_attempts` is incremented, so a parent record with no hash gives unlimited un-counted attempts. The throttle is therefore only as reliable as the branch above it.

### Task T005: router registration in `app/main.py`
- Spec requirement: register the router beside the other child-device routers.
- Implementation found:
  - Files: `app/main.py` — module added to the `from app.routes import (...)` block, and `app.include_router(child_device_session_routes.router)` placed directly after `screen_time_usage_routes`
  - Key symbols: `child_device_session_routes.router`
- Status: **PASS**
- Evidence: AST inspection of the route module confirms exactly the two intended paths and no others:
  `GET /api/child-devices/{device_id}/session-profile`, `POST /api/child-devices/{device_id}/parent-verification`.
  A live `app.routes` enumeration was attempted but is impossible in this environment because application import requires Firebase credentials; static verification stands in, matching the repository's own precedent in `scripts/verify_dashboard_routes.py`.

### Task T006: `tests/test_child_device_session_routes.py`
- Spec requirement: cover the profile payload, ownership mismatch, correct password, wrong password, and the lockout transition.
- Implementation found:
  - Files: `tests/test_child_device_session_routes.py` — 11 tests, all passing
  - Key symbols: `seeded_db`, `verification_record`, reuses `FakeDB` from `tests/test_website_policy_crud.py` per repository convention
- Status: **PARTIAL**
- Evidence: every item T006 names is covered. `os.environ.setdefault("JWT_SECRET", ...)` before the app import follows the established pattern in `test_dashboard_phase2.py` and siblings.
- Problems: two reachable branches have no test — `parent_not_found` on either endpoint, and the missing-hash path from Issue 1. See Issue 3.

### Task T007: regression and compile checks
- Spec requirement: run the backend regression and compile checks and record the result.
- Status: **PASS**
- Evidence: `-m compileall -q app` exits clean. `-m pytest -q` reports 265 passed / 3 failed, with all 3 failures isolated to `tests/test_daily_usage_accuracy.py` and caused by the missing `tzdata` package. No file in this phase is imported by that test module.
- Notes: the environment gap is real but out of this feature's scope. Installing `tzdata` mutates a shared virtual environment and should be an explicit decision, not a side effect of this phase.

## Issues List (Consolidated)

### Issue 1: Missing parent password hash returns HTTP 500 and bypasses the lockout
- [x] FIXED
- Fix notes: `app/routes/child_device_session_routes.py` — read the hash into `stored_hash` and short-circuit with `if stored_hash and verify_password(...)`, so a missing/empty/None hash falls into the existing counting branch and answers `401 invalid_parent_password` instead of raising `UnknownHashError` into a 500.
- Severity: **HIGH**
- Depends on: none
- Affected tasks: T002, T004
- Evidence (paths/symbols): `app/routes/child_device_session_routes.py:113` — `if verify_password(payload.password, parent_data.get("password") or ""):`
- Root cause analysis: the `or ""` guard was written to avoid passing `None` into passlib, but it does the opposite of what it intends. Verified directly against this venv:
  ```
  verify_password('anything', '')   -> RAISES UnknownHashError
  verify_password('anything', None) -> False
  ```
  So the fallback turns a value passlib handles safely into one it rejects with an exception. A `Parents` document without a `password` field — a legacy record, a partially-created account, or one migrated to another sign-in method — therefore produces an unhandled `UnknownHashError`. The global handler in `app/main.py` converts it to HTTP 500. Two consequences follow: the child device sees a server error rather than a clean rejection, and because the exception escapes before line 116, `failed_attempts` is never incremented, so this path is not rate limited at all.
- Proposed solution (detailed steps):
  1. In `app/routes/child_device_session_routes.py`, read the hash into a local before use:
     ```python
     stored_hash = parent_data.get("password")
     ```
  2. Replace the condition at line 113 so a missing or empty hash is treated as a failed verification rather than handed to passlib:
     ```python
     if stored_hash and verify_password(payload.password, stored_hash):
     ```
     Short-circuiting keeps the failure flowing into the existing counting block below, so the lockout applies uniformly.
  3. Leave the rest of the function unchanged — the failure branch already increments, locks, and raises `401 invalid_parent_password`, which is the correct response for an account that cannot be verified from this device.
- Test plan (exact commands):
  ```
  .\.venv\Scripts\python.exe -m pytest tests/test_child_device_session_routes.py -q
  .\.venv\Scripts\python.exe -m pytest -q
  ```
  Add a test seeding a parent document with no `password` key and assert `401` with `invalid_parent_password`, plus a second assertion that `failed_attempts` incremented to 1.
- Notes / tradeoffs: returning `401` rather than a distinct code is deliberate — the child device must not learn anything about the parent account's internal state.

### Issue 2: Absent parent email degrades to an empty string instead of failing loudly
- [x] FIXED
- Fix notes: `app/routes/child_device_session_routes.py` — `read_session_profile` now raises `404 parent_email_unavailable` when the parent record has no email, instead of returning `parent_email: ""`. `child_name` keeps its empty-string fallback as specified.
- Severity: **MED**
- Depends on: none
- Affected tasks: T001
- Evidence (paths/symbols): `app/routes/child_device_session_routes.py:72` — `parent_email=parent_data.get("email") or ""`
- Root cause analysis: acceptance criterion 5 requires the verification dialog's disabled email field to be populated from the backend. An empty string satisfies the response schema and returns `200`, so the client has no way to distinguish "no email on record" from a successful read. The result on the device is a disabled, blank field — the parent is asked to confirm ownership of an account the dialog cannot name, which defeats the purpose of showing the field at all. `child_name` has the same shape but a benign fallback, because the spec already defines a neutral greeting for an unknown name; email has no such defined fallback.
- Proposed solution (detailed steps):
  1. After loading `parent_data`, validate the email before constructing the response:
     ```python
     parent_email = parent_data.get("email")
     if not parent_email:
         _raise("parent_email_unavailable", "Parent account has no email on record", status.HTTP_404_NOT_FOUND)
     ```
  2. Pass `parent_email` into `ChildDeviceSessionProfile` instead of the inline `or ""`.
  3. Keep `child_name=child_data.get("name") or ""` as is — the spec defines a greeting fallback for it.
- Test plan (exact commands):
  ```
  .\.venv\Scripts\python.exe -m pytest tests/test_child_device_session_routes.py -q
  ```
  Add a test seeding a parent without an `email` key and assert `404` with `parent_email_unavailable`.
- Notes / tradeoffs: an alternative is to keep `200` and let the client decide, but that pushes a backend data problem into the UI layer and leaves the dialog in a state the spec does not describe.

### Issue 3: Reachable error branches have no test coverage
- [x] FIXED
- Fix notes: `tests/test_child_device_session_routes.py` — added `test_session_profile_reports_missing_parent_record`, `test_session_profile_requires_parent_email` (parametrized None/""), `test_missing_parent_password_hash_is_rejected_and_counted` (parametrized None/""), `test_missing_parent_password_hash_still_trips_the_lockout`, `test_verification_reports_missing_parent_record`. Module went 11 -> 18 tests, all passing.
- Severity: **MED**
- Depends on: Issue 1, Issue 2 (the tests below assert the behavior those issues introduce)
- Affected tasks: T006
- Evidence (paths/symbols): `app/routes/child_device_session_routes.py:66-67` (`parent_not_found` in `read_session_profile`), `:100-101` (`parent_not_found` in `verify_parent_password`); `tests/test_child_device_session_routes.py` has no case for either.
- Root cause analysis: T006 enumerated the paths it wanted covered and those were all delivered, but the module raises `parent_not_found` on two separate branches that no test exercises. Both are plausible in production — the device token outlives the parent record after account deletion, which is exactly when a child device would keep calling these endpoints.
- Proposed solution (detailed steps):
  1. Add `test_session_profile_reports_missing_parent_record`: seed the db, then `db.collection("Parents").docs.pop("parent-1")`, call `read_session_profile`, assert `404` / `parent_not_found`.
  2. Add `test_verification_reports_missing_parent_record`: same removal, call `verify_parent_password` with the correct password, assert `404` / `parent_not_found`.
  3. Add the missing-hash test described in Issue 1 and the missing-email test described in Issue 2.
- Test plan (exact commands):
  ```
  .\.venv\Scripts\python.exe -m pytest tests/test_child_device_session_routes.py -q
  ```
- Notes / tradeoffs: none — these are pure additions to an existing test module.

### Issue 4: `verify_parent_password` falls off its end against a non-optional return type
- [x] FIXED
- Fix notes: `app/routes/child_device_session_routes.py` — imported `NoReturn` and annotated `_raise(...) -> NoReturn`, making the fall-through unreachable by definition. `compileall` clean.
- Severity: **LOW**
- Depends on: Issue 1 (touches the same block; apply after)
- Affected tasks: T002
- Evidence (paths/symbols): `app/routes/child_device_session_routes.py:34-35` (`def _raise(...) -> None`) and `:124` — the final statement is a bare `_raise(...)` call in a function declared `-> ParentVerificationResponse`.
- Root cause analysis: `_raise` always raises, so runtime behavior is correct, but nothing in the signature says so. A reader — or any type checker added later — sees a function that can return `None` where a `ParentVerificationResponse` is promised. `read_session_profile` has the same helper but always ends in an explicit `return`, so the inconsistency is local to this one function.
- Proposed solution (detailed steps):
  1. Annotate the helper as never-returning so both the reader and the checker know:
     ```python
     from typing import NoReturn

     def _raise(code: str, message: str, http_status: int) -> NoReturn:
     ```
  2. No call-site changes are required; `NoReturn` makes the fall-through unreachable by definition.
- Test plan (exact commands):
  ```
  .\.venv\Scripts\python.exe -m compileall -q app
  .\.venv\Scripts\python.exe -m pytest tests/test_child_device_session_routes.py -q
  ```
- Notes / tradeoffs: cosmetic today; it earns its place because the next person to add a branch to this function would otherwise get no warning that they had created a real `None` return.

## Fix Plan (Ordered)
1) Issue 1: Missing parent password hash returns HTTP 500 and bypasses the lockout — short-circuit on a falsy stored hash so the failure is counted and answered with `401`.
2) Issue 2: Absent parent email degrades to an empty string — raise `404 parent_email_unavailable` instead of returning a blank field.
3) Issue 3: Reachable error branches have no test coverage — add tests for `parent_not_found` on both endpoints plus the missing-hash and missing-email cases.
4) Issue 4: `verify_parent_password` falls off its end — annotate `_raise` as `NoReturn`.

## Handoff to Coding Model (Copy/Paste)

**Files to edit/create**
- `backend/safenest_review/app/routes/child_device_session_routes.py` (edit)
- `backend/safenest_review/tests/test_child_device_session_routes.py` (edit — additions only)

**Exact behavior changes**
- A bound parent record with a missing or empty `password` hash yields `401 invalid_parent_password` and increments the failed-attempt counter, instead of raising `UnknownHashError` into a `500`.
- A bound parent record with a missing `email` yields `404 parent_email_unavailable` from `session-profile`, instead of `200` with `parent_email: ""`.
- `_raise` is declared `NoReturn`.

**Edge cases**
- `password` key absent entirely, present as `None`, and present as `""` must all behave identically.
- The missing-hash failure must still trip the lockout after `MAX_FAILED_ATTEMPTS`, exactly like a wrong password.
- `child_name` keeps its empty-string fallback; only `parent_email` becomes strict.

**Tests to add/update**
- `test_missing_parent_password_hash_is_rejected_and_counted`
- `test_session_profile_requires_parent_email`
- `test_session_profile_reports_missing_parent_record`
- `test_verification_reports_missing_parent_record`

**Suggested commit breakdown**
- One commit: `fix(backend): harden child-device parent verification against incomplete parent records`

**Do not touch**
- Anything under `frontend_app/app_father/`
- `app_child/.../service/AppBlockerAccessibilityService.kt`
- `frontend_app/.specify/feature.json`
