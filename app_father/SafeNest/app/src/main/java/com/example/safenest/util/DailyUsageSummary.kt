package com.example.safenest.util

import com.example.safenest.network.DigitalRuleResponse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlin.math.roundToInt

/** Layngo contract: consume server-validated daily data; never infer from legacy fields. */
enum class DailyUsageState { NORMAL, OVER_LIMIT, EMPTY, STALE, LIMIT_CONFIRMATION_REQUIRED }

data class DailyUsageApp(
    val packageName: String,
    val displayName: String,
    val usageMinutes: Int,
    val iconLabel: String
)

data class DailyUsageSummary(
    val state: DailyUsageState,
    val totalMinutes: Int = 0,
    val dailyLimitMinutes: Int = 0,
    val remainingMinutes: Int = 0,
    val progressPercent: Int = 0,
    val apps: List<DailyUsageApp> = emptyList(),
    val relativeUpdatedText: String = ""
)

object DailyUsageSummaryMapper {
    fun map(rule: DigitalRuleResponse, nowMillis: Long = System.currentTimeMillis()): DailyUsageSummary {
        if (rule.limitConfirmationRequired || rule.dailyLimitMinutes == null) {
            return DailyUsageSummary(state = DailyUsageState.LIMIT_CONFIRMATION_REQUIRED)
        }
        val limit = rule.dailyLimitMinutes.coerceAtLeast(0)
        val usageDate = rule.usageDate
        val usageTimezone = rule.usageTimezone
        if (usageDate.isNullOrBlank() || usageTimezone.isNullOrBlank()) {
            return DailyUsageSummary(state = DailyUsageState.EMPTY, dailyLimitMinutes = limit)
        }
        val isCurrentDay = try {
            usageDate == LocalDate.now(ZoneId.of(usageTimezone)).toString()
        } catch (_: Exception) {
            false
        }
        if (!isCurrentDay) return DailyUsageSummary(state = DailyUsageState.STALE, dailyLimitMinutes = limit)

        val total = (rule.usedTodayMinutes ?: 0).coerceAtLeast(0)
        val remaining = (rule.remainingTodayMinutes ?: (limit - total).coerceAtLeast(0)).coerceAtLeast(0)
        val percentage = if (limit <= 0) 0 else ((total.toFloat() / limit) * 100).roundToInt().coerceIn(0, 100)
        val apps = rule.dailyUsageLog.asSequence()
            .filter { (_, minutes) -> minutes > 0 }
            .sortedByDescending { (_, minutes) -> minutes }
            .map { (packageName, minutes) ->
                DailyUsageApp(packageName, displayNameFor(packageName), minutes, iconLabelFor(packageName))
            }
            .toList()
        val state = when {
            apps.isEmpty() && total == 0 -> DailyUsageState.EMPTY
            limit > 0 && total >= limit -> DailyUsageState.OVER_LIMIT
            else -> DailyUsageState.NORMAL
        }
        return DailyUsageSummary(state, total, limit, remaining, percentage, apps, relativeTime(rule.usageUpdatedAt, nowMillis))
    }

    fun formatDuration(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        val hours = safe / 60
        val remainder = safe % 60
        return when {
            hours == 0 -> "$remainder د"
            remainder == 0 -> "$hours س"
            else -> "$hours س $remainder د"
        }
    }

    private fun relativeTime(timestamp: String?, nowMillis: Long): String {
        if (timestamp.isNullOrBlank()) return "آخر تحديث غير معروف"
        return try {
            val delta = ((nowMillis - Instant.parse(timestamp).toEpochMilli()) / 60_000L).coerceAtLeast(0)
            when {
                delta < 1 -> "تم التحديث الآن"
                delta == 1L -> "آخر تحديث منذ دقيقة"
                delta < 60 -> "آخر تحديث منذ $delta دقائق"
                delta < 120 -> "آخر تحديث منذ ساعة"
                else -> "آخر تحديث منذ ${delta / 60} ساعات"
            }
        } catch (_: DateTimeParseException) {
            "آخر تحديث غير معروف"
        }
    }

    private fun displayNameFor(packageName: String): String = when {
        packageName.contains("youtube", true) -> "YouTube"
        packageName.contains("roblox", true) -> "Roblox"
        packageName.contains("minecraft", true) -> "Minecraft"
        packageName.contains("chrome", true) -> "Chrome"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun iconLabelFor(packageName: String): String = when {
        packageName.contains("youtube", true) -> "Y"
        packageName.contains("roblox", true) -> "R"
        packageName.contains("minecraft", true) -> "M"
        packageName.contains("chrome", true) -> "C"
        else -> displayNameFor(packageName).take(1)
    }
}

