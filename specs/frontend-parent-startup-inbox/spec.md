# Frontend Spec: Parent Startup Inbox

## Purpose

When an authenticated Parent opens the Android app, the Parent should see unresolved notifications and pending Child access requests before entering the Home dashboard.

## Requirements

The startup flow SHALL load the Parent's unresolved alerts and pending access requests through the existing authenticated APIs. Child requests SHALL be shown before general notifications and SHALL expose Allow and Reject actions using the server request ID. The inbox SHALL use the existing alert-resolution API for notification review.

The UI SHALL support RTL Arabic presentation, Layngo colors, readable cards, clear counts, loading, empty, partial-data, error/retry, close, and continue-to-Home states. A startup fetch failure SHALL never trap the Parent away from Home permanently.

The implementation SHALL preserve normal notification navigation, avoid duplicate API submissions, and not modify IoT or Child enforcement code.

## Acceptance criteria

A logged-in Parent opening the app sees pending requests/unresolved notifications when records exist. Allow/Reject update the correct request on the Backend, and notification review updates the correct alert. When there are no unresolved records, the app proceeds to Home. Parent build and existing Backend regression tests pass.
