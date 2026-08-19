# Implementation Plan: Layngo Parent Daily Usage

## Architecture

Parent introduces a typed `DailyUsageSummary` mapper from the canonical digital-control response. The mapper produces display-ready total, limit, remaining, app rows, and one of normal/over-limit/confirmation/empty/error states. `DailyUsageFragment` owns rendering only and does not sum raw legacy maps.

Child augments the existing daily usage request with `usage_day` and `usage_timezone`. Its app-usage calculation and enforcement features remain unchanged.

## UI Composition

The XML screen uses a single `NestedScrollView` and a Layngo-branded top section: a visible back target, compact official logo, child avatar, title, and freshness. A Mint summary card contains an accessible circular meter with direct text labels. A white rounded application card contains dynamic rows and a “show more” control when appropriate. A Teal bottom CTA is placed above the gesture area.

## Verification

Parent unit tests validate mapper states, Arabic duration formatting, ring percentage clamp, top-app ordering, and the production 16-hour accumulation regression. Child tests validate report date/timezone construction. The required Gradle test/build gates run after changes.

