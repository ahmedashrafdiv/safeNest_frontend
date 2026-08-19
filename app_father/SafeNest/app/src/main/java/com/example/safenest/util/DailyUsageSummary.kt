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
    private const val SYSTEM_ROW_KEY = "__system__"
    private const val SYSTEM_ROW_NAME = "تطبيقات النظام"

    fun map(
        rule: DigitalRuleResponse,
        appLabels: Map<String, String> = emptyMap(),
        nowMillis: Long = System.currentTimeMillis()
    ): DailyUsageSummary {
        // Computed up front so every state — including the ones below that return early —
        // reports when the Child last synced instead of leaving the header on its layout placeholder.
        val updatedText = relativeTime(rule.usageUpdatedAt, nowMillis)
        if (rule.limitConfirmationRequired || rule.dailyLimitMinutes == null) {
            return DailyUsageSummary(state = DailyUsageState.LIMIT_CONFIRMATION_REQUIRED, relativeUpdatedText = updatedText)
        }
        val limit = rule.dailyLimitMinutes.coerceAtLeast(0)
        val usageDate = rule.usageDate
        val usageTimezone = rule.usageTimezone
        if (usageDate.isNullOrBlank() || usageTimezone.isNullOrBlank()) {
            return DailyUsageSummary(state = DailyUsageState.EMPTY, dailyLimitMinutes = limit, relativeUpdatedText = updatedText)
        }
        val isCurrentDay = try {
            usageDate == LocalDate.now(ZoneId.of(usageTimezone)).toString()
        } catch (_: Exception) {
            false
        }
        if (!isCurrentDay) {
            return DailyUsageSummary(state = DailyUsageState.STALE, dailyLimitMinutes = limit, relativeUpdatedText = updatedText)
        }

        val total = (rule.usedTodayMinutes ?: 0).coerceAtLeast(0)
        val remaining = (rule.remainingTodayMinutes ?: (limit - total).coerceAtLeast(0)).coerceAtLeast(0)
        val percentage = if (limit <= 0) 0 else ((total.toFloat() / limit) * 100).roundToInt().coerceIn(0, 100)
        val apps = buildAppRows(rule.dailyUsageLog, appLabels)
        val state = when {
            apps.isEmpty() && total == 0 -> DailyUsageState.EMPTY
            limit > 0 && total >= limit -> DailyUsageState.OVER_LIMIT
            else -> DailyUsageState.NORMAL
        }
        return DailyUsageSummary(state, total, limit, remaining, percentage, apps, updatedText)
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

    /**
     * The Parent device cannot resolve another device's PackageManager labels, so raw usage
     * keys are just package names (e.g. "com.facebook.katana", "com.android.incallui").
     * [appLabels] is the Child's own reported installed-app names (`GET .../installed-apps`);
     * a package missing from that map is not a launchable user app (system UI, dialer, launcher,
     * etc.) and is folded into one "تطبيقات النظام" row instead of showing a raw package guess.
     * When [appLabels] has not loaded yet (empty), every package falls back to the heuristic
     * name so the list still renders something reasonable rather than dumping everything into
     * the system bucket.
     */
    private fun buildAppRows(
        dailyUsageLog: Map<String, Int>,
        appLabels: Map<String, String>
    ): List<DailyUsageApp> {
        var systemMinutes = 0
        val knownApps = mutableListOf<DailyUsageApp>()
        dailyUsageLog.forEach { (packageName, minutes) ->
            if (minutes <= 0) return@forEach
            val knownLabel = appLabels[packageName]
            when {
                knownLabel != null ->
                    knownApps.add(DailyUsageApp(packageName, knownLabel, minutes, knownLabel.take(1).uppercase()))
                appLabels.isEmpty() ->
                    knownApps.add(DailyUsageApp(packageName, displayNameFor(packageName), minutes, iconLabelFor(packageName)))
                else -> systemMinutes += minutes
            }
        }
        if (systemMinutes > 0) {
            knownApps.add(DailyUsageApp(SYSTEM_ROW_KEY, SYSTEM_ROW_NAME, systemMinutes, "⚙"))
        }
        return knownApps.sortedByDescending { it.usageMinutes }
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

