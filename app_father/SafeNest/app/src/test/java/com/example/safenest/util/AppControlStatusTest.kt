package com.example.safenest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppControlStatusTest {

    private val fullWeek = AppControlStatus.WEEKDAY_CODES.associateWith { 60 }

    @Test
    fun test_blocked_app_wins_over_every_other_state() {
        val state = AppControlStatus.stateFor(
            packageName = "com.roblox",
            blockedApps = listOf("com.roblox"),
            allowedApps = listOf("com.roblox"),
            appTimeLimits = mapOf("com.roblox" to fullWeek),
            appControlMode = "blocklist"
        )
        assertEquals(AppControlState.BLOCKED, state)
    }

    @Test
    fun test_an_app_with_a_time_limit_is_timed_not_plain_allowed() {
        val state = AppControlStatus.stateFor(
            packageName = "com.youtube",
            blockedApps = emptyList(),
            allowedApps = emptyList(),
            appTimeLimits = mapOf("com.youtube" to fullWeek),
            appControlMode = "blocklist"
        )
        assertEquals(AppControlState.TIMED, state)
    }

    @Test
    fun test_allowlist_mode_blocks_apps_that_are_not_explicitly_allowed() {
        val notListed = AppControlStatus.stateFor(
            packageName = "com.tiktok",
            blockedApps = emptyList(),
            allowedApps = listOf("com.whatsapp"),
            appTimeLimits = emptyMap(),
            appControlMode = "allowlist"
        )
        val listed = AppControlStatus.stateFor(
            packageName = "com.whatsapp",
            blockedApps = emptyList(),
            allowedApps = listOf("com.whatsapp"),
            appTimeLimits = emptyMap(),
            appControlMode = "allowlist"
        )
        assertEquals(AppControlState.BLOCKED, notListed)
        assertEquals(AppControlState.ALLOWED, listed)
    }

    @Test
    fun test_blocklist_mode_leaves_unlisted_apps_allowed() {
        val state = AppControlStatus.stateFor(
            packageName = "com.tiktok",
            blockedApps = listOf("com.roblox"),
            allowedApps = emptyList(),
            appTimeLimits = emptyMap(),
            appControlMode = "blocklist"
        )
        assertEquals(AppControlState.ALLOWED, state)
    }

    @Test
    fun test_todays_limit_reads_todays_code_and_fails_closed_for_a_missing_day() {
        val perDay = mapOf("sat" to 120, "thu" to 45)
        assertEquals(120, AppControlStatus.todayLimitMinutes(perDay, "sat"))
        assertEquals(45, AppControlStatus.todayLimitMinutes(perDay, "thu"))
        assertEquals(0, AppControlStatus.todayLimitMinutes(perDay, "wed"))
        assertNull(AppControlStatus.todayLimitMinutes(null, "sat"))
    }

    @Test
    fun test_time_options_cover_every_quarter_hour_from_zero_to_twenty_four_hours() {
        val options = AppControlStatus.TIME_OPTIONS_MINUTES
        assertEquals(97, options.size)
        assertEquals(0, options.first())
        assertEquals(1440, options.last())
        assertTrue(options.zipWithNext().all { (a, b) -> b - a == 15 })
        assertEquals("00:00", AppControlStatus.formatHhMm(0))
        assertEquals("00:45", AppControlStatus.formatHhMm(45))
        assertEquals("01:30", AppControlStatus.formatHhMm(90))
        assertEquals("24:00", AppControlStatus.formatHhMm(1440))
    }

    @Test
    fun test_copy_saturday_fills_every_other_day_and_leaves_days_individually_editable() {
        val perDay = mapOf("sat" to 120, "sun" to 30, "mon" to 15)
        val copied = AppControlStatus.copySaturdayToOtherDays(perDay)

        assertEquals(AppControlStatus.WEEKDAY_CODES.toSet(), copied.keys)
        assertTrue(copied.values.all { it == 120 })

        // The result is a plain map the editor can keep mutating per day afterwards.
        val editedAfterwards = copied.toMutableMap().apply { this["thu"] = 45 }
        assertEquals(45, editedAfterwards["thu"])
        assertEquals(120, editedAfterwards["sat"])
    }

    @Test
    fun test_full_week_defaults_missing_days_to_zero() {
        val filled = AppControlStatus.fullWeek(mapOf("sat" to 90))
        assertEquals(7, filled.size)
        assertEquals(90, filled["sat"])
        assertEquals(0, filled["fri"])
        assertEquals(7, AppControlStatus.fullWeek(null).size)
    }

    @Test
    fun test_status_text_is_never_color_only_and_names_the_zero_and_full_day_cases() {
        fun row(state: AppControlState, minutes: Int?) =
            AppControlRow("com.x", "X", state, minutes, "X")

        assertEquals("محظور", AppControlStatus.statusText(row(AppControlState.BLOCKED, null)))
        assertEquals("مسموح", AppControlStatus.statusText(row(AppControlState.ALLOWED, null)))
        assertEquals("محدد بـ1 س 30 د", AppControlStatus.statusText(row(AppControlState.TIMED, 90)))
        assertEquals("محدد بـغير متاح اليوم", AppControlStatus.statusText(row(AppControlState.TIMED, 0)))
        assertEquals("محدد بـطوال اليوم", AppControlStatus.statusText(row(AppControlState.TIMED, 1440)))
    }

    @Test
    fun test_rows_use_real_labels_and_fall_back_to_the_package_tail() {
        val rows = AppControlStatus.rowsFor(
            installedApps = listOf("com.google.android.youtube" to "YouTube", "com.example.thing" to ""),
            blockedApps = emptyList(),
            allowedApps = emptyList(),
            appTimeLimits = mapOf("com.google.android.youtube" to mapOf("sat" to 90)),
            appControlMode = "blocklist",
            todayCode = "sat"
        )

        assertEquals("YouTube", rows[0].displayName)
        assertEquals(AppControlState.TIMED, rows[0].state)
        assertEquals(90, rows[0].todayLimitMinutes)
        assertEquals("Thing", rows[1].displayName)
        assertEquals(AppControlState.ALLOWED, rows[1].state)
    }
}
