package com.safenest.kids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

class AppTimeLimitResolverTest {

    @Test
    fun test_weekday_codes_map_saturday_first() {
        assertEquals("sat", WeekdayCode.forDayOfWeek(DayOfWeek.SATURDAY))
        assertEquals("fri", WeekdayCode.forDayOfWeek(DayOfWeek.FRIDAY))
        assertEquals("wed", WeekdayCode.forDayOfWeek(DayOfWeek.WEDNESDAY))
        assertEquals(listOf("sat", "sun", "mon", "tue", "wed", "thu", "fri"), WeekdayCode.CODES)
    }

    @Test
    fun test_an_app_without_a_configured_limit_is_never_over_limit() {
        assertNull(AppTimeLimitResolver.resolveTodayLimitMinutes(null, "sat"))
        assertFalse(AppTimeLimitResolver.isOverLimit(null, "sat", usedMinutes = 9_999))
    }

    @Test
    fun test_todays_limit_is_read_from_todays_day_code_not_another_day() {
        val perDay = mapOf("sat" to 120, "sun" to 0, "mon" to 60, "tue" to 60, "wed" to 60, "thu" to 45, "fri" to 180)

        assertEquals(120, AppTimeLimitResolver.resolveTodayLimitMinutes(perDay, "sat"))
        assertEquals(45, AppTimeLimitResolver.resolveTodayLimitMinutes(perDay, "thu"))

        // 100 minutes used: under Saturday's 120 limit, but over Thursday's 45 limit.
        assertFalse(AppTimeLimitResolver.isOverLimit(perDay, "sat", usedMinutes = 100))
        assertTrue(AppTimeLimitResolver.isOverLimit(perDay, "thu", usedMinutes = 100))
    }

    @Test
    fun test_zero_minutes_blocks_the_app_all_day() {
        val perDay = mapOf("sun" to 0)
        assertEquals(0, AppTimeLimitResolver.resolveTodayLimitMinutes(perDay, "sun"))
        assertTrue(AppTimeLimitResolver.isOverLimit(perDay, "sun", usedMinutes = 0))
    }

    @Test
    fun test_full_day_allowance_is_not_exceeded_by_a_realistic_day() {
        val perDay = mapOf("sat" to 1440)
        assertFalse(AppTimeLimitResolver.isOverLimit(perDay, "sat", usedMinutes = 1439))
        assertTrue(AppTimeLimitResolver.isOverLimit(perDay, "sat", usedMinutes = 1440))
    }

    @Test
    fun test_a_missing_day_code_fails_closed_to_zero() {
        // Mirrors the Backend's normalize_app_time_limits default for an absent day.
        val perDay = mapOf("sat" to 120)
        assertEquals(0, AppTimeLimitResolver.resolveTodayLimitMinutes(perDay, "wed"))
        assertTrue(AppTimeLimitResolver.isOverLimit(perDay, "wed", usedMinutes = 0))
    }
}
