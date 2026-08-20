# Frontend — Child Home Screen and Parent-Protected Controls

## Summary

The Child application (`app_child/SafeNest-Kids`, package `com.safenest.kids`) currently opens on a
diagnostic-style Home screen that lists protection status lines and debug buttons. This feature
replaces it with the Layngo-branded child Home screen and adds a menu, opened from the header, whose
two entries are protected by the parent's account password.

The Child application has no child login. A device becomes usable by pairing to a parent account with
a six-digit PIN. "Sign out" therefore means unpairing the device, not ending a child session.

## Goals

1. The Child application opens on the Layngo Home screen once the device is paired and baseline
   permissions are ready.
2. The child can see how much of the daily screen-time budget remains, and can request extra time.
3. A parent standing at the child's device can unpair the device or disable protection, but only
   after proving account ownership with the account password.

## Non-Goals

- No change to any file under `app_father/`. A separate effort is editing that application
  concurrently.
- No change to `AppBlockerAccessibilityService.kt`. It carries unrelated in-flight work; protection is
  disabled at the policy source instead of inside the enforcement callback.
- No new parent-facing UI for the disabled state. The parent app is out of scope here.
- No redesign of the pairing or permissions screens.

## User Experience

### Home screen

The screen is right-to-left, on the ivory Layngo surface, and contains, top to bottom:

| Region | Content |
|---|---|
| Header | Layngo wordmark and logo on the leading side; a menu (three-bar) button on the trailing side |
| Greeting | `أهلًا <child name>!` with the subtitle `يوم سعيد لك.` |
| Budget ring | A circular progress ring, remaining minutes as the primary number, `دقيقة باقية` beneath it |
| Extra-time card | A clock glyph, the prompt `هل تحتاجين وقت أكثر؟`, an explanatory line, and the action `اطلب وقت إضافي` |
| Help | An outlined `المساعدة` button |
| Bottom navigation | Two destinations: `اليوم` and `المساعدة` |

The greeting falls back to a neutral phrase when the child's name is not yet known.

When no daily screen-time policy is assigned, the ring falls back to a default budget of **five
hours** and subtracts the device's own measured usage for the day, so the child always sees a live,
plausible number rather than a zero or a blank dial. This default is a display convenience only — it
is never enforced and never reported to the backend as a policy. Once the parent assigns a real
policy, the backend decision replaces it.

### Menu

The menu button opens a sheet with exactly two entries:

1. **تسجيل الخروج** — unpairs the device.
2. **تعطيل البرنامج** — suspends protection while the pairing is kept.

Selecting either entry opens the parent verification dialog before anything happens.

### Parent verification dialog

The dialog shows the bound parent's email in a **pre-filled, disabled** field, so the parent can see
which account is being verified but cannot retarget the check at another account. Below it is a
password field and a confirm action. On success the requested operation runs. On failure the dialog
reports the error and stays open.

### Sign out

Unpairing clears the device access token, child and parent identifiers, the cached policy, and the
pairing flag; cancels the background workers; stops the location and DNS services; and returns the
application to the pairing screen.

### Disable protection

Disabling sets a local suspended flag, clears the cached enforcement policy (blocked and allowed
lists, per-app limits, daily budget), and cancels the policy sync workers so the cleared policy is not
immediately refetched. The pairing and device token are preserved.

While protection is suspended, the Home screen replaces the budget ring with a suspended notice and
offers two actions: re-enable protection (also behind parent password) and delete the application,
which removes device-admin registration and opens the system uninstall screen.

## Backend Contract

Two endpoints are added for the bound child device. Both authenticate with the existing child-device
token and both verify that the path device id matches the token.

### `GET /api/child-devices/{device_id}/session-profile`

Returns the display identity the Home screen and the verification dialog need.

```json
{
  "child_id": "…",
  "child_name": "ليان",
  "parent_id": "…",
  "parent_email": "parent@example.com"
}
```

Responds `403` when the bound child is not owned by the bound parent, and `404` when either record is
missing.

### `POST /api/child-devices/{device_id}/parent-verification`

Body is `{"password": "…"}`. Returns `{"verified": true}` when the password matches the bound parent's
stored hash.

Because the device is physically in the child's hands, this endpoint is rate limited: five consecutive
failures lock verification for fifteen minutes, tracked per device. A wrong password returns `401`; a
locked device returns `429`.

### Reused endpoints

- `GET /api/child-devices/{device_id}/screen-time-decision` supplies `remaining_seconds`,
  `used_seconds`, and `effective_limit_seconds` for the ring.
- `POST /api/child-devices/{device_id}/access-requests` submits the extra-time request with
  `request_type: extra_time`, `scope_type: screen_time`, and `scope_value: daily`. The scope value
  matters: the Backend also accepts `downtime` and `bedtime`, but only a `daily` grant is added to
  the daily budget during evaluation, so any other value produces a request the parent can approve
  and that then changes nothing. `requested_seconds` must fall between 60 and 86400.

## Constraints

- Right-to-left layout and the IBM Plex Sans Arabic family already present in the project.
- The Layngo palette in `res/values/colors.xml` is authoritative; no new brand colors are invented.
- The existing worker registrations in `HomeFragment` must survive the redesign; the screen is the
  place several periodic syncs are scheduled.
- The parent password is never persisted on the device, in preferences or in logs.

## Acceptance Criteria

1. A paired device with baseline permissions opens directly on the Layngo Home screen.
2. The greeting shows the child's name from the backend, and the ring shows remaining minutes
   consistent with `screen-time-decision`.
3. The menu exposes exactly the two specified entries.
4. Both entries require the parent password; neither performs its action on a wrong password.
5. The email field in the verification dialog is populated from the backend and cannot be edited.
6. After sign out, the application shows the pairing screen and holds no device token.
7. After disabling, the cached enforcement policy is empty, sync workers are cancelled, the pairing
   survives, and the delete-application action is offered.
8. Five wrong passwords lock further attempts for fifteen minutes.
9. No file under `app_father/` and no change to `AppBlockerAccessibilityService.kt` appears in the
   diff for this feature.
