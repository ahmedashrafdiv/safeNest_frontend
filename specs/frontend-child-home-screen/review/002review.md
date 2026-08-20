# Spec Review: frontend-child-home-screen (Phase 2)

- Branch: `main`
- Spec resolved via: argument (`frontend_app/specs/frontend-child-home-screen`)
- Resolution conflicts: `frontend_app/.specify/feature.json` points at a different feature and was
  deliberately left untouched — a concurrent effort owns it, so this spec is passed explicitly.
- Review file: 002review.md
- Scope: Phase 2 only (T008–T014). Phases 3–5 are unstarted and are not judged here.
- Detected commands: test=`gradlew.bat :app:testDebugUnitTest` lint=`gradlew.bat :app:lintDebug`
  types=`gradlew.bat :app:compileDebugKotlin` (Kotlin compiler; no separate type checker, and no
  ktlint/detekt configured in either Gradle file)

> Toolchain note: `JAVA_HOME` is unset on this machine and no `java` is on `PATH`. The commands above
> only run when prefixed with `JAVA_HOME=D:/Android/jdk/temurin-17`. This is an environment fact, not
> a defect in the change, but every command in this report assumes that prefix.

## Summary

- Overall status: **PARTIAL** — all seven tasks are implemented and tested, but one implemented
  behaviour contradicts an explicit spec requirement.
- High-risk issues: the budget ring renders the Backend's `unknown` screen-time decision as an
  exhausted budget, which is the misleading zero `spec.md` specifically set out to prevent.
- Missing tests / regression risk: no test covers a `screen-time-decision` body whose decision is
  `unknown`; no test covers a `200` response carrying `verified: false`.
- Test suite results: **92 passed, 0 failed** (26 of them added by Phase 2: ScreenTimeBudget 10,
  ParentVerificationDecider 10, ChildGreeting 7 — the tests file reports 10 for the verification
  decider because one test asserts two mappings).
- Lint results: **21 errors, 98 warnings — all pre-existing**. Every error sits in
  `BlockedAppActivity.kt`, `ProtectedHomeRoleManager.kt`, `UsageSnapshotMetadata.kt`,
  `WeekdayCode.kt`, or `themes.xml`. Grepping the lint report for the six Phase 2 files returns
  nothing, so this change neither adds nor clears a lint error.
- Type check results: 0 errors.

## Task-by-task Verification

### Task T008: API models for the new endpoints
- Acceptance criteria: session profile, parent verification request/response, screen-time decision,
  and extra-time access request models in `network/ApiModels.kt`.
- Implementation found:
  - Files: `app_child/.../network/ApiModels.kt`
  - Key symbols: `ChildDeviceSessionProfile`, `ParentVerificationRequest`,
    `ParentVerificationResponse`, `ScreenTimeDecisionResponse`, `AccessRequestCreateRequest`,
    `AccessRequestCreateResponse`
- Status: **PASS**
- Evidence: field names check out one by one against the Backend contracts —
  `ChildDeviceSessionProfile` in `app/routes/child_device_session_routes.py:21`,
  `ScreenTimeEvaluationResponse` in `app/schemas/screen_time_schemas.py:145`, and
  `AccessRequestCreate`/`AccessRequestResponse` in `app/schemas/access_request_schemas.py:46,105`.
  Optional fields are nullable or defaulted, so a partial body cannot throw during deserialization.
- Problems: none that break the model, but see Issue 4 — the Backend constrains `scope_value` and
  `requested_seconds` in ways nothing on the client records.

### Task T009: endpoints on the Retrofit interface
- Acceptance criteria: matching endpoints on `network/KidsApiService.kt`.
- Implementation found:
  - Files: `app_child/.../network/KidsApiService.kt`
  - Key symbols: `getSessionProfile`, `verifyParentPassword`, `getScreenTimeDecision`,
    `createAccessRequest`
- Status: **PASS**
- Evidence: paths and verbs match `child_device_session_routes.py:71,107`,
  `screen_time_usage_routes.py:43`, and `child_access_request_routes.py:23`. The `{device_id}` path
  argument follows the convention every existing device-scoped call already uses — `RuleSyncWorker.kt:30`,
  `ScreenTimePolicySyncWorker.kt:17`, `ProtectionHealthWorker.kt:37` all pass
  `prefsHelper.getDeviceId()`, the same locally generated UUID `PairingFragment.kt:70` sends as
  `device_id` at pairing — so the Backend's path/token comparison will match.

### Task T010: preference keys and the policy-clearing routine
- Acceptance criteria: cached child name, parent email, protection-suspended flag, and a routine
  clearing the cached enforcement policy.
- Implementation found:
  - Files: `app_child/.../util/PrefsHelper.kt`
  - Key symbols: `setChildName`/`getChildName`, `setParentEmail`/`getParentEmail`,
    `setProtectionSuspended`/`isProtectionSuspended`, `clearEnforcementPolicy`, `clearPairingSession`
- Status: **PARTIAL**
- Evidence: `clearEnforcementPolicy` genuinely neutralizes all three enforcement reads, and this was
  re-verified against the concurrent effort's *current* `AppPolicyDecider.kt`, which they modified
  during this phase: `shouldBlock` at line 37 reduces to `packageName in emptySet()` once the mode is
  `blocklist` and the set is empty. `isAppOverTimeLimit`
  (`AppBlockerAccessibilityService.kt:208`) returns false on a null limits JSON, and
  `isDailyScreenTimeLimitReached` (line 238) returns false on a zero budget. Clearing
  `website_policy_snapshot_json` also stops `ServiceWatchdogReceiver.kt:74` from restarting the DNS
  VPN, which is the correct knock-on.
- Problems: `clearPairingSession` leaves the previous child's last known coordinates and per-feature
  status strings in preferences. See Issue 3.

### Task T011: `ScreenTimeBudget`
- Acceptance criteria: convert a screen-time decision into a remaining-minute label and a ring sweep
  fraction, "modelling the no-policy and over-budget cases explicitly"; `spec.md` further requires a
  five-hour fallback rather than a misleading zero.
- Implementation found:
  - Files: `app_child/.../util/ScreenTimeBudget.kt`
  - Key symbols: `DEFAULT_BUDGET_SECONDS`, `Ring`, `fromDecision`, `fromDefaultBudget`, `build`
- Status: **PARTIAL**
- Evidence: `DEFAULT_BUDGET_SECONDS` is `5 * 60 * 60` = 18000, matching the spec's five hours.
  Over-budget, negative, and zero-limit inputs are all clamped, and division by zero is guarded.
  `fromDefaultBudget` correctly treats its argument as minutes, which matches
  `AppUsageHelper.getTodayUsageStats` as used at `AppBlockerAccessibilityService.kt:217,239`.
- Problems: the no-policy case is only handled for the 404 path. The Backend has a *second* way to
  report "no usable budget" and it arrives as a `200`. See Issue 1.

### Task T012: `ChildGreeting`
- Acceptance criteria: greeting line with a fallback when the child name is unknown.
- Implementation found:
  - Files: `app_child/.../util/ChildGreeting.kt`
  - Key symbols: `MAX_DISPLAY_LENGTH`, `displayName`
- Status: **PASS**
- Evidence: returns `null` for null, empty, whitespace-only, and control-character-only input, so the
  caller can select the neutral string resource. Keeping the literal text in `strings.xml` rather
  than in the object is the right call and satisfies T018 in advance. The control-character-to-space
  substitution is deliberate and covered by `embeddedNewlinesCollapseIntoASingleLine`, which caught a
  real ordering bug during implementation.

### Task T013: `ParentVerificationDecider`
- Acceptance criteria: map an HTTP status to a verification outcome.
- Implementation found:
  - Files: `app_child/.../util/ParentVerificationDecider.kt`
  - Key symbols: `Outcome`, `outcome`, `offlineOutcome`, `errorCodeOf`
- Status: **PARTIAL**
- Evidence: the 401 split between `invalid_parent_password` and everything else is correct and
  valuable — the Backend really does answer 401 from two different places
  (`child_device_session_routes.py:157` for the password, and the `get_current_child_device`
  dependency for the token). `errorCodeOf` degrades to `null` on every malformed body rather than
  throwing.
- Problems: the affirmative branch keys on the status code alone. See Issue 2.

### Task T014: JVM unit tests
- Acceptance criteria: unit tests for the three objects.
- Implementation found: `ScreenTimeBudgetTest` (10), `ParentVerificationDeciderTest` (10),
  `ChildGreetingTest` (7)
- Status: **PASS**
- Evidence: all 92 tests in the module pass under `--rerun-tasks`. Boundaries are covered
  deliberately — zero limit, negative remainder, remainder exceeding the limit, partial minute,
  usage beyond the fallback, and unparsable error bodies.
- Problems: two behaviours introduced by Issues 1 and 2 are untested; both need tests as part of
  their fixes.

## Issues List (Consolidated)

### Issue 1: The `unknown` screen-time decision renders as an exhausted budget
- [x] FIXED
- Fix notes: `util/ScreenTimeBudget.kt` — added `DECISION_UNKNOWN`; `fromDecision` now takes
  `(decision, remainingSeconds, effectiveLimitSeconds, localUsedMinutes)` and returns
  `fromDefaultBudget` when the decision is `unknown` or the limit is non-positive. `ScreenTimeBudgetTest`
  gained `unknownDecisionFallsBackToTheDefaultBudget` and `zeroLimitDecisionFallsBackToTheDefaultBudget`;
  `exhaustedPolicyBudgetEmptiesTheDial` now asserts a real `limit_reached` still empties the dial.
- Severity: HIGH
- Depends on: none
- Affected tasks: T011 (and T019, which will consume it in Phase 3)
- Evidence (paths/symbols):
  - `app_child/.../util/ScreenTimeBudget.kt` — `fromDecision(remainingSeconds, effectiveLimitSeconds)`
  - `backend/safenest_review/app/schemas/screen_time_schemas.py:161` —
    `ScreenTimeEvaluationResponse.unknown(...)`
  - `backend/safenest_review/app/services/screen_time_usage_service.py:140` — returns that shape when
    `effective_from > now`
- Root cause analysis: the implementation assumes "no usable budget" only ever arrives as HTTP 404
  `policy_not_found`. It does not. When a policy exists but has not taken effect yet, `evaluate`
  returns **200** with `decision = "unknown"`, `reason_code = "policy_not_effective"`,
  `effective_limit_seconds = 0`, and `remaining_seconds = 0`. `fromDecision` faithfully maps that to
  `remainingMinutes = 0`, `sweepFraction = 0f`, `isExhausted = true` — an empty dial reading
  "0 دقيقة باقية". `spec.md` calls that out as the exact outcome to avoid: the child is told their
  time is up when in fact no limit is in force. The 404 branch and the `unknown` branch mean the same
  thing to a child and must render the same way.
- Proposed solution (detailed steps — must be mechanically applicable):
  1. In `ScreenTimeBudget.kt`, add a decision constant near `DEFAULT_BUDGET_SECONDS`:
     `const val DECISION_UNKNOWN = "unknown"`.
  2. Change the signature of `fromDecision` to accept the decision string:
     `fun fromDecision(decision: String?, remainingSeconds: Int, effectiveLimitSeconds: Int, localUsedMinutes: Long): Ring`.
  3. As the first statement of the new `fromDecision`, return the fallback when the Backend could not
     produce a usable budget:
     ```kotlin
     if (decision == DECISION_UNKNOWN || effectiveLimitSeconds <= 0) {
         return fromDefaultBudget(localUsedMinutes)
     }
     ```
     Folding `effectiveLimitSeconds <= 0` into the same branch also covers a policy that arrives with
     a zero limit, which is indistinguishable to the child.
  4. Leave `build` and `fromDefaultBudget` unchanged; the zero-limit guard inside `build` stays as
     defence in depth.
  5. Update the doc comment on `DEFAULT_BUDGET_SECONDS` to say it stands in for *any* decision that
     carries no usable budget — 404 `policy_not_found`, a `unknown` decision, or a zero limit — not
     only the 404.
- Test plan (exact commands):
  - Add to `ScreenTimeBudgetTest`:
    - `unknownDecisionFallsBackToTheDefaultBudget` — `fromDecision("unknown", 0, 0, 60)` asserts
      `remainingMinutes == 240` and `usesDefaultBudget`.
    - `zeroLimitDecisionFallsBackToTheDefaultBudget` — `fromDecision("allow", 0, 0, 0)` asserts
      `remainingMinutes == 300` and `usesDefaultBudget`.
    - `allowDecisionStillUsesTheServerBudget` — `fromDecision("allow", 2040, 3600, 0)` asserts
      `remainingMinutes == 34` and `!usesDefaultBudget`, guarding against the fallback swallowing the
      real path.
    - Update `exhaustedPolicyBudgetEmptiesTheDial` to pass `"limit_reached"` and a non-zero limit, so
      a genuinely exhausted budget still reports `isExhausted`.
  - `JAVA_HOME=D:/Android/jdk/temurin-17 ./gradlew.bat :app:testDebugUnitTest` from
    `app_child/SafeNest-Kids`
- Notes / tradeoffs: a genuinely exhausted budget (`limit_reached`, limit 3600, remaining 0) must
  still show zero — the fix must not blur that case into the fallback, which is why the branch keys
  on the decision and the limit rather than on `remaining == 0`.

### Issue 2: A verified result is inferred from the status code, not from the response body
- [x] FIXED
- Fix notes: `util/ParentVerificationDecider.kt` — `outcome` now takes `verified: Boolean`; a 2xx
  verifies only with the flag set and otherwise returns `UNAVAILABLE`. `ParentVerificationDeciderTest`
  gained `successWithoutTheVerifiedFlagFailsClosed` and `anySuccessStatusWithTheFlagVerifies`.
- Severity: MED
- Depends on: none
- Affected tasks: T013 (and T023/T024/T025, which gate destructive actions on it)
- Evidence (paths/symbols):
  - `app_child/.../util/ParentVerificationDecider.kt` — `outcome`, branch `httpCode == 200`
  - `app_child/.../network/ApiModels.kt` — `ParentVerificationResponse.verified` defaults to `false`
- Root cause analysis: this decider is the gate in front of unpairing the device and switching
  protection off, so its affirmative branch deserves the strictest reading available. Two gaps:
  the branch matches exactly `200`, while Retrofit reports any 2xx as successful, so a future `204`
  would fall through to `UNAVAILABLE`; and more importantly the body's `verified` flag is never
  consulted, so a `200 {"verified": false}` — which a proxy, a cache, or a later Backend refactor
  could produce — would be read as a successful verification. The model already defaults `verified`
  to `false`, and that safe default is currently discarded.
- Proposed solution (detailed steps):
  1. Change the signature to carry the body's flag:
     `fun outcome(httpCode: Int, verified: Boolean, errorCode: String?): Outcome`.
  2. Replace the first branch with `httpCode in 200..299 && verified -> Outcome.VERIFIED`.
  3. Add, immediately after it, `httpCode in 200..299 -> Outcome.UNAVAILABLE` so a successful
     response that does not affirm verification fails closed rather than falling through to the 401
     branches and being mislabelled a wrong password.
  4. At the call site in Phase 4, pass `response.body()?.verified == true`.
- Test plan (exact commands):
  - Add to `ParentVerificationDeciderTest`:
    - `successWithoutTheVerifiedFlagFailsClosed` — `outcome(200, false, null)` is `UNAVAILABLE`.
    - `anySuccessStatusWithTheFlagVerifies` — `outcome(204, true, null)` is `VERIFIED`.
    - Update the existing `successVerifies` to `outcome(200, true, null)`.
  - `JAVA_HOME=D:/Android/jdk/temurin-17 ./gradlew.bat :app:testDebugUnitTest`
- Notes / tradeoffs: none — failing closed on an ambiguous success is the correct bias for a control
  that unpairs a child's device.

### Issue 3: Sign-out leaves the previous child's location and status behind
- [x] FIXED
- Fix notes: `util/PrefsHelper.kt` — `clearPairingSession` now removes the six `phone_tracking_last_*`
  keys, the four tracking state keys, `phone_tracking_policy_version`, and `website_vpn_health`.
  Verified by inspection; on-device confirmation is deferred to T032 as the report specified.
- Severity: MED
- Depends on: none
- Affected tasks: T010 (consumed by T024)
- Evidence (paths/symbols):
  - `app_child/.../util/PrefsHelper.kt` — `clearPairingSession`
  - Keys left behind: `phone_tracking_last_latitude`, `phone_tracking_last_longitude`,
    `phone_tracking_last_captured_at`, `phone_tracking_last_upload_at`,
    `phone_tracking_last_report_id`, `phone_tracking_status`, `phone_tracking_service_state`,
    `phone_tracking_network_state`, `phone_tracking_permission_state`,
    `phone_tracking_policy_version`, `website_vpn_health`
- Root cause analysis: `clearPairingSession` was written around the credentials and the policy and
  stops there. The tracking keys are neither, but `phone_tracking_last_latitude` and
  `phone_tracking_last_longitude` are the last known position of the child who was unpaired, and they
  survive into whatever pairing comes next on the same handset. The status strings are a smaller
  problem of the same kind: `getPhoneTrackingStatus()` would still answer `active` on a device that
  is no longer paired to anyone, so any screen reading it reports tracking that is not running.
- Proposed solution (detailed steps):
  1. In `PrefsHelper.clearPairingSession`, extend the `edit()` chain with `.remove(...)` for each of
     the eleven keys listed above.
  2. Add a short comment stating that residual coordinates must not outlive the pairing that
     produced them.
- Test plan (exact commands): no JVM test — `PrefsHelper` needs an Android `Context` and this module
  has no Robolectric. Verify by inspection, then on-device during T032 by signing out and dumping
  `/data/data/com.safenest.kids/shared_prefs/SafeNestKidsPrefs.xml`, which must contain no
  `phone_tracking_last_*` entry.
- Notes / tradeoffs: `device_id` is deliberately kept, which is correct — the Backend keys the device
  document on the client-supplied UUID (`PairingFragment.kt:70`), so discarding it would orphan the
  Backend record on re-pair.

### Issue 4: The extra-time scope contract is not recorded anywhere on the client
- [x] FIXED
- Fix notes: `network/ApiModels.kt` — added the `ExtraTimeRequest` object carrying `REQUEST_TYPE`,
  `SCOPE_TYPE`, `SCOPE_VALUE = "daily"`, and the 60..86400 bounds. `spec.md`'s reused-endpoints bullet
  now states the scope value and the bound.
- Severity: LOW
- Depends on: none
- Affected tasks: T008 (consumed by T030)
- Evidence (paths/symbols):
  - `backend/.../app/utils/access_request_normalization.py:72` — `screen_time` accepts only
    `daily`, `downtime`, `bedtime`
  - `backend/.../app/services/screen_time_usage_service.py:154` — a grant is applied to the daily
    budget **only** when `scope_value == "daily"`
  - `backend/.../app/schemas/access_request_schemas.py:51` — `requested_seconds` is `ge=60, le=86400`
- Root cause analysis: `AccessRequestCreateRequest.scopeValue` is a bare `String` and `spec.md`'s
  Backend Contract section names `request_type` and `scope_type` but not `scope_value`. Phase 5 could
  therefore send `bedtime`, receive a perfectly successful `200`, have the parent approve it, and see
  the grant silently ignored by the evaluator because it is not `daily`. A failure that looks like
  success everywhere except the one place that matters is worth closing before the call site is
  written.
- Proposed solution (detailed steps):
  1. In `ApiModels.kt`, directly above `AccessRequestCreateRequest`, add a companion-free constant
     block or a comment naming the three valid scope values and stating that only `daily` is consumed
     by the daily-budget evaluator.
  2. Preferably add to `ScreenTimeBudget.kt` or a new small object the constants
     `EXTRA_TIME_SCOPE_VALUE = "daily"`, `EXTRA_TIME_REQUEST_TYPE = "extra_time"`,
     `EXTRA_TIME_SCOPE_TYPE = "screen_time"`, and `MIN_REQUESTED_SECONDS = 60` /
     `MAX_REQUESTED_SECONDS = 86400`, so T030 cannot pick a value by hand.
  3. Amend the "Reused endpoints" bullet in `spec.md` to state `scope_value: daily` and the
     60–86400 second bound.
- Test plan (exact commands): `JAVA_HOME=D:/Android/jdk/temurin-17 ./gradlew.bat :app:testDebugUnitTest`
  (compilation only; the constants get their behavioural test in Phase 5 with T030).
- Notes / tradeoffs: none.

## Fix Plan (Ordered)

1) Issue 1: `unknown` decision renders as exhausted — route `unknown` and zero-limit decisions to the
   five-hour fallback, and add the three ring tests.
2) Issue 2: verified inferred from status — consult the body's `verified` flag, accept any 2xx, and
   fail closed on an unaffirmed success.
3) Issue 3: sign-out leaves location behind — remove the eleven residual tracking and health keys in
   `clearPairingSession`.
4) Issue 4: extra-time scope contract unrecorded — add the scope constants and amend `spec.md`.

## Handoff to Coding Model (Copy/Paste)

**Files to edit/create**
- `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/util/ScreenTimeBudget.kt` (Issues 1, 4)
- `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/util/ParentVerificationDecider.kt` (Issue 2)
- `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/util/PrefsHelper.kt` (Issue 3)
- `app_child/SafeNest-Kids/app/src/main/java/com/safenest/kids/network/ApiModels.kt` (Issue 4)
- `app_child/SafeNest-Kids/app/src/test/java/com/safenest/kids/util/ScreenTimeBudgetTest.kt` (Issue 1)
- `app_child/SafeNest-Kids/app/src/test/java/com/safenest/kids/util/ParentVerificationDeciderTest.kt` (Issue 2)
- `frontend_app/specs/frontend-child-home-screen/spec.md` (Issue 4)

**Exact behavior changes**
- A `screen-time-decision` of `unknown`, or any decision with a non-positive effective limit, shows
  the five-hour fallback dial instead of an empty one.
- A genuinely exhausted budget (`limit_reached` with a positive limit) still shows zero.
- Verification succeeds only on a 2xx that also carries `verified: true`; any other 2xx is
  `UNAVAILABLE`.
- Sign-out removes every residual location and per-feature status key.

**Edge cases**
- `decision == "allow"` with a real limit must not be diverted into the fallback.
- `fromDecision` gains a `localUsedMinutes` parameter; every existing call site and test must pass it.
- `errorCodeOf` behaviour is unchanged — do not touch it.

**Tests to add/update**
- `ScreenTimeBudgetTest`: +3 new, 1 updated (see Issue 1).
- `ParentVerificationDeciderTest`: +2 new, 1 updated (see Issue 2).

**Suggested commit breakdown**
- One commit: "Correct the Phase 2 budget fallback and verification gate" covering Issues 1–4, since
  Issues 2–4 are each a few lines and share the same test command.

**Concurrency reminder**
A separate effort holds uncommitted edits to `BlockedAppActivity.kt`,
`AppBlockerAccessibilityService.kt`, `AppPolicyDecider.kt`, `AppPolicyDeciderTest.kt`, and two new
untracked `security/` files. Stage explicit paths only; `git add -A` is prohibited.
