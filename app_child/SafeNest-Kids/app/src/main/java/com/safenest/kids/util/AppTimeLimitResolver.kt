package com.safenest.kids.util

/**
 * Pure per-app time-limit decision, kept free of JSON/Android types so it is unit-testable
 * without Robolectric. Callers parse the cached policy JSON into [perDayMinutes] first.
 */
object AppTimeLimitResolver {
    /**
     * Returns null when the app has no configured limit at all (not over limit — the app is
     * unrestricted). A day code missing from [perDayMinutes] resolves to 0 minutes (fail-closed),
     * matching the Backend's `normalize_app_time_limits` default for the same case.
     */
    fun resolveTodayLimitMinutes(perDayMinutes: Map<String, Int>?, todayCode: String): Int? {
        if (perDayMinutes == null) return null
        return perDayMinutes[todayCode] ?: 0
    }

    fun isOverLimit(perDayMinutes: Map<String, Int>?, todayCode: String, usedMinutes: Long): Boolean {
        val limit = resolveTodayLimitMinutes(perDayMinutes, todayCode) ?: return false
        return usedMinutes >= limit
    }
}
