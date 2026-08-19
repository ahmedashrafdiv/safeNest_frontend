package com.example.safenest.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Layngo app-control model: one status per installed app, derived from the digital-control rule. */
enum class AppControlState { BLOCKED, TIMED, ALLOWED }

data class AppControlRow(
    val packageName: String,
    val displayName: String,
    val state: AppControlState,
    val todayLimitMinutes: Int?,
    val iconLabel: String
)

object AppControlStatus {
    val WEEKDAY_CODES = listOf("sat", "sun", "mon", "tue", "wed", "thu", "fri")

    val WEEKDAY_ARABIC_NAMES = mapOf(
        "sat" to "السبت",
        "sun" to "الأحد",
        "mon" to "الاثنين",
        "tue" to "الثلاثاء",
        "wed" to "الأربعاء",
        "thu" to "الخميس",
        "fri" to "الجمعة"
    )

    /** All 97 quarter-hour values the inline day picker offers, 00:00 through 24:00 inclusive. */
    val TIME_OPTIONS_MINUTES: List<Int> = (0..1440 step 15).toList()

    fun formatHhMm(minutes: Int): String {
        val safe = minutes.coerceIn(0, 1440)
        return "%02d:%02d".format(safe / 60, safe % 60)
    }

    fun todayCode(zoneId: ZoneId = ZoneId.systemDefault()): String = when (LocalDate.now(zoneId).dayOfWeek) {
        DayOfWeek.SATURDAY -> "sat"
        DayOfWeek.SUNDAY -> "sun"
        DayOfWeek.MONDAY -> "mon"
        DayOfWeek.TUESDAY -> "tue"
        DayOfWeek.WEDNESDAY -> "wed"
        DayOfWeek.THURSDAY -> "thu"
        DayOfWeek.FRIDAY -> "fri"
    }

    /** A missing day resolves to 0, matching the Backend's fail-closed normalization. */
    fun todayLimitMinutes(perDay: Map<String, Int>?, todayCode: String): Int? =
        if (perDay == null) null else (perDay[todayCode] ?: 0)

    fun stateFor(
        packageName: String,
        blockedApps: Collection<String>,
        allowedApps: Collection<String>,
        appTimeLimits: Map<String, Map<String, Int>>,
        appControlMode: String
    ): AppControlState = when {
        packageName in blockedApps -> AppControlState.BLOCKED
        appTimeLimits[packageName]?.isNotEmpty() == true -> AppControlState.TIMED
        appControlMode == "allowlist" && packageName !in allowedApps -> AppControlState.BLOCKED
        else -> AppControlState.ALLOWED
    }

    fun rowsFor(
        installedApps: List<Pair<String, String>>,
        blockedApps: Collection<String>,
        allowedApps: Collection<String>,
        appTimeLimits: Map<String, Map<String, Int>>,
        appControlMode: String,
        todayCode: String = todayCode()
    ): List<AppControlRow> = installedApps.map { (packageName, appName) ->
        val label = appName.ifBlank { packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() } }
        AppControlRow(
            packageName = packageName,
            displayName = label,
            state = stateFor(packageName, blockedApps, allowedApps, appTimeLimits, appControlMode),
            todayLimitMinutes = todayLimitMinutes(appTimeLimits[packageName], todayCode),
            iconLabel = label.take(1).uppercase()
        )
    }

    /** Convenience default used by the inline editor's "copy Saturday to the rest" action. */
    fun copySaturdayToOtherDays(perDay: Map<String, Int>): Map<String, Int> {
        val saturday = perDay["sat"] ?: 0
        return WEEKDAY_CODES.associateWith { saturday }
    }

    fun fullWeek(perDay: Map<String, Int>?): Map<String, Int> =
        WEEKDAY_CODES.associateWith { perDay?.get(it) ?: 0 }

    fun statusText(row: AppControlRow): String = when (row.state) {
        AppControlState.BLOCKED -> "محظور"
        AppControlState.TIMED -> "محدد بـ${formatDurationArabic(row.todayLimitMinutes ?: 0)}"
        AppControlState.ALLOWED -> "مسموح"
    }

    fun formatDurationArabic(minutes: Int): String {
        val safe = minutes.coerceAtLeast(0)
        if (safe == 0) return "غير متاح اليوم"
        if (safe >= 1440) return "طوال اليوم"
        val hours = safe / 60
        val remainder = safe % 60
        return when {
            hours == 0 -> "$remainder د"
            remainder == 0 -> "$hours س"
            else -> "$hours س $remainder د"
        }
    }
}
