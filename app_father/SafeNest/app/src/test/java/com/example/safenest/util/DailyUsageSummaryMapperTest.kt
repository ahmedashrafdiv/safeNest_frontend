package com.example.safenest.util

import com.example.safenest.network.DigitalRuleResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class DailyUsageSummaryMapperTest {

    @Test
    fun test_verified_today_summary_uses_fixed_limit_instead_of_raw_legacy_map_total() {
        val summary = DailyUsageSummaryMapper.map(
            rule = rule(
                dailyLimitMinutes = 300,
                usedTodayMinutes = 90,
                remainingTodayMinutes = 210,
                dailyUsageLog = mapOf("com.youtube" to 90)
            ),
            nowMillis = Instant.now().toEpochMilli()
        )

        assertEquals(DailyUsageState.NORMAL, summary.state)
        assertEquals(90, summary.totalMinutes)
        assertEquals(300, summary.dailyLimitMinutes)
        assertEquals(210, summary.remainingMinutes)
        assertEquals(30, summary.progressPercent)
        assertEquals("YouTube", summary.apps.first().displayName)
    }

    @Test
    fun test_legacy_rule_without_verified_limit_shows_confirmation_state() {
        val summary = DailyUsageSummaryMapper.map(rule(dailyLimitMinutes = null))

        assertEquals(DailyUsageState.LIMIT_CONFIRMATION_REQUIRED, summary.state)
    }

    @Test
    fun test_over_limit_clamps_ring_to_one_hundred_percent() {
        val summary = DailyUsageSummaryMapper.map(
            rule(dailyLimitMinutes = 60, usedTodayMinutes = 90, remainingTodayMinutes = 0)
        )

        assertEquals(DailyUsageState.OVER_LIMIT, summary.state)
        assertEquals(100, summary.progressPercent)
        assertTrue(summary.remainingMinutes == 0)
    }

    private fun rule(
        dailyLimitMinutes: Int? = 300,
        usedTodayMinutes: Int? = 0,
        remainingTodayMinutes: Int? = 300,
        dailyUsageLog: Map<String, Int> = mapOf("com.youtube" to 1)
    ): DigitalRuleResponse = DigitalRuleResponse(
        ruleId = "rule-1",
        parentId = "parent-1",
        childId = "child-1",
        maxScreenTime = dailyLimitMinutes,
        dailyLimitMinutes = dailyLimitMinutes,
        usedTodayMinutes = usedTodayMinutes,
        remainingTodayMinutes = remainingTodayMinutes,
        usageDate = LocalDate.now(ZoneOffset.UTC).toString(),
        usageTimezone = "UTC",
        usageUpdatedAt = Instant.now().toString(),
        limitConfirmationRequired = false,
        dailyUsageLog = dailyUsageLog
    )
}
