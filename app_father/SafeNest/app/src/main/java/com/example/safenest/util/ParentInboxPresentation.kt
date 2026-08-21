package com.example.safenest.util

import com.example.safenest.network.AccessRequestItem
import com.example.safenest.network.AlertOut
import java.time.OffsetDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class InboxRequestKind { EXTRA_TIME, APP_ACCESS, GENERAL }
enum class InboxAlertKind { BLOCKED_APP, SAFE_PLACE_EXIT, SAFE_PLACE_ARRIVAL, ATTENTION_PLACE_ENTRY, RISK_PLACE_ENTRY, GENERAL }
enum class InboxAlertAction { REVIEW_APP_RULES, VIEW_LOCATION, NONE }

data class InboxRequestPresentation(
    val requestId: String,
    val kind: InboxRequestKind,
    val title: String,
    val detail: String,
    val timestamp: String,
    val primaryActionLabel: String,
    val secondaryActionLabel: String,
)

data class InboxAlertPresentation(
    val alertId: String,
    val kind: InboxAlertKind,
    val title: String,
    val detail: String,
    val timestamp: String,
    val statusLabel: String?,
    val action: InboxAlertAction,
)

/** Pure Arabic presentation mapping for the Parent decision inbox. */
object ParentInboxPresentation {
    private val arabicLocale = Locale.forLanguageTag("ar")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", arabicLocale)

    fun request(item: AccessRequestItem): InboxRequestPresentation {
        val childName = item.childName?.trim().orEmpty().ifBlank { "طفلك" }
        val requestKind = when (item.requestType.lowercase()) {
            "extra_time" -> InboxRequestKind.EXTRA_TIME
            "access_override" -> InboxRequestKind.APP_ACCESS
            else -> InboxRequestKind.GENERAL
        }
        return when (requestKind) {
            InboxRequestKind.EXTRA_TIME -> InboxRequestPresentation(
                requestId = item.requestId,
                kind = requestKind,
                title = "$childName طلبت ${formatMinutes(item.requestedSeconds)} إضافية",
                detail = "${displayScope(item.scopeValue)} • انتهى وقت اليوم",
                timestamp = formatTimestamp(item.requestedAt),
                primaryActionLabel = "موافقة",
                secondaryActionLabel = "إبقاء الحد",
            )
            InboxRequestKind.APP_ACCESS -> InboxRequestPresentation(
                requestId = item.requestId,
                kind = requestKind,
                title = "$childName تريد فتح تطبيق ${displayScope(item.scopeValue)}",
                detail = item.reason?.trim().takeUnless { it.isNullOrBlank() }
                    ?: "طلب تطبيق جديد على جهازها",
                timestamp = formatTimestamp(item.requestedAt),
                primaryActionLabel = "سماح",
                secondaryActionLabel = "ليس الآن",
            )
            InboxRequestKind.GENERAL -> InboxRequestPresentation(
                requestId = item.requestId,
                kind = requestKind,
                title = "$childName لديها طلب جديد",
                detail = item.reason?.trim().takeUnless { it.isNullOrBlank() } ?: displayScope(item.scopeValue),
                timestamp = formatTimestamp(item.requestedAt),
                primaryActionLabel = "موافقة",
                secondaryActionLabel = "ليس الآن",
            )
        }
    }

    fun alert(item: AlertOut): InboxAlertPresentation {
        val evidence = "${item.alertType} ${item.message}".lowercase()
        return when {
            evidence.contains("place_risk_entry") || evidence.contains("منطقة خطر") -> InboxAlertPresentation(
                alertId = item.alertId,
                kind = InboxAlertKind.RISK_PLACE_ENTRY,
                title = "دخلت ليان منطقة خطر",
                detail = placeDetail(item.message, "تم إنشاء التنبيه وفق إعدادات المكان."),
                timestamp = formatTimestamp(item.timestamp),
                statusLabel = "منطقة خطر",
                action = InboxAlertAction.VIEW_LOCATION,
            )
            evidence.contains("place_attention_entry") || evidence.contains("يحتاج انتباه") -> InboxAlertPresentation(
                alertId = item.alertId,
                kind = InboxAlertKind.ATTENTION_PLACE_ENTRY,
                title = "دخلت ليان مكانًا يحتاج انتباهًا",
                detail = placeDetail(item.message, "تم إنشاء التنبيه وفق إعدادات المكان."),
                timestamp = formatTimestamp(item.timestamp),
                statusLabel = "مكان يحتاج انتباهًا",
                action = InboxAlertAction.NONE,
            )
            evidence.contains("blocked") || evidence.contains("block") || evidence.contains("محظور") || evidence.contains("حظر") -> InboxAlertPresentation(
                alertId = item.alertId,
                kind = InboxAlertKind.BLOCKED_APP,
                title = appAttemptTitle(item.message),
                detail = item.message.ifBlank { "التطبيق ضمن التطبيقات المحظورة لطفلك" },
                timestamp = formatTimestamp(item.timestamp),
                statusLabel = "تم الحظر",
                action = InboxAlertAction.REVIEW_APP_RULES,
            )
            evidence.contains("arriv") || evidence.contains("وصل") || evidence.contains("arrival") -> InboxAlertPresentation(
                alertId = item.alertId,
                kind = InboxAlertKind.SAFE_PLACE_ARRIVAL,
                title = item.message.ifBlank { "وصل طفلك إلى مكان آمن" },
                detail = "تم الوصول إلى المكان الآمن",
                timestamp = formatTimestamp(item.timestamp),
                statusLabel = "وصول آمن",
                action = InboxAlertAction.NONE,
            )
            evidence.contains("depart") || evidence.contains("exit") || evidence.contains("غادر") || evidence.contains("مغادرة") -> InboxAlertPresentation(
                alertId = item.alertId,
                kind = InboxAlertKind.SAFE_PLACE_EXIT,
                title = item.message.ifBlank { "غادر طفلك مكانًا آمنًا" },
                detail = "تم رصد المغادرة وفق تنبيه المكان الآمن",
                timestamp = formatTimestamp(item.timestamp),
                statusLabel = null,
                action = InboxAlertAction.VIEW_LOCATION,
            )
            else -> InboxAlertPresentation(
                alertId = item.alertId,
                kind = InboxAlertKind.GENERAL,
                title = item.alertType.ifBlank { "تنبيه جديد" },
                detail = item.message.ifBlank { "يوجد تحديث جديد يحتاج إلى اطلاعك." },
                timestamp = formatTimestamp(item.timestamp),
                statusLabel = null,
                action = InboxAlertAction.NONE,
            )
        }
    }

    fun pendingSummary(count: Int): String = when (count.coerceAtLeast(0)) {
        0 -> "لا توجد طلبات بانتظار قرارك"
        1 -> "لديك طلب واحد بانتظار قرارك"
        2 -> "لديك طلبان بانتظار قرارك"
        else -> "لديك $count طلبات بانتظار قرارك"
    }

    fun formatMinutes(seconds: Int): String {
        val minutes = (seconds / 60).coerceAtLeast(1)
        return when (minutes) {
            60 -> "ساعة"
            else -> "$minutes دقيقة"
        }
    }

    private fun displayScope(rawValue: String): String = when (rawValue.trim()) {
        "com.spotify.music" -> "Spotify"
        "com.google.android.youtube" -> "YouTube"
        "com.zhiliaoapp.musically" -> "TikTok"
        "screen_time", "daily" -> "وقت الشاشة"
        else -> rawValue.trim().ifBlank { "التطبيق" }.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }

    private fun appAttemptTitle(message: String): String {
        val knownApps = listOf("TikTok", "YouTube", "Roblox", "Spotify", "Chrome", "WhatsApp")
        val known = knownApps.firstOrNull { message.contains(it, ignoreCase = true) }
        return if (known == null) "تم حظر محاولة فتح تطبيق" else "تم حظر محاولة فتح $known"
    }

    private fun placeDetail(message: String, fallback: String): String {
        val candidate = message.trim()
        return candidate.takeIf {
            it.isNotBlank() && !it.contains("latitude", true) && !it.contains("longitude", true)
        } ?: fallback
    }

    private fun formatTimestamp(raw: String?): String {
        if (raw.isNullOrBlank()) return "وقت غير متاح"
        return runCatching {
            val localTime = OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault())
            val datePrefix = when (localTime.toLocalDate()) {
                LocalDate.now() -> "اليوم"
                LocalDate.now().minusDays(1) -> "أمس"
                else -> localTime.format(DateTimeFormatter.ofPattern("d MMM", arabicLocale))
            }
            "$datePrefix، ${localTime.format(timeFormatter)}"
        }.getOrElse { "وقت غير متاح" }
    }
}
