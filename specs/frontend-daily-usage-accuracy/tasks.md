# Tasks: Layngo Parent Daily Usage Accuracy and Screen

## Phase 1: Typed contract and Child report metadata

- [x] FDU-001 Add canonical daily-summary fields to Parent API models and repository mapping.
- [x] FDU-002 Add `usage_day` and `usage_timezone` to Child request construction.
- [x] FDU-003 Add regression tests for request metadata and display-summary mapping.

## Phase 2: Parent accuracy states

- [x] FDU-004 Replace raw-map aggregation in `DailyUsageFragment` with a typed summary mapper.
- [x] FDU-005 Implement normal, over-limit, confirmation-required, loading, empty, stale, and error states.
- [x] FDU-006 Add duration, relative-update, progress-clamp, ordering, and package-label helpers.

## Phase 3: Layngo screen reconstruction

- [x] FDU-007 Rebuild `fragment_daily_usage.xml` with RTL Layngo hierarchy and accessible summary card.
- [x] FDU-008 Add reusable drawable/vector resources for the ring, avatar surface, neutral app markers, and state treatments.
- [x] FDU-009 Render app rows with visual bars and connect `إدارة وقت الشاشة` to the existing route.

## Phase 4: Quality and review

- [x] FDU-010 Run Parent and changed-Child tests and assemble debug APKs.
- [x] FDU-011 Run test guard, spec review, review fix, and production code review.
- [x] FDU-012 Commit only scoped daily-usage changes locally; leave unrelated untracked files unstaged.
