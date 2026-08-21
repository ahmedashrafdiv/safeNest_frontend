package com.safenest.kids.security

/**
 * Identifies the system surfaces that would let the holder of the device switch Layngo's
 * enforcement off — the accessibility-service entry and the device-admin deactivation entry.
 *
 * An accessibility service cannot technically refuse to be disabled; the only thing it can do is
 * notice the page and react before the toggle is reached. This classifier is that detection step,
 * kept as a pure function so the reaction policy (and its tests) stay separate from the service.
 *
 * The classifier deliberately stays narrow. It fires only when the visible page BOTH names Layngo
 * AND exposes one of the protection surfaces, so ordinary Settings browsing — display, sound,
 * network, even the accessibility list before Layngo's own row is on screen — remains reachable.
 * A parent who needs to reach the real toggle can always boot into Safe Mode, where third-party
 * accessibility services do not run at all and this classifier never executes.
 */
object ProtectionSettingsAttemptClassifier {
    private val settingsPackages = setOf(
        "com.android.settings",
        "com.coloros.settings",
        "com.oplus.settings",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
    )

    /** Tokens naming the accessibility surface, in the locales Layngo ships. */
    private val accessibilityTokens = setOf(
        "accessibility",
        "إمكانية الوصول",
        "إمكانيه الوصول",
        "امكانية الوصول",
        "سهولة الاستخدام",
    )

    /** Tokens naming the device-admin surface, whose deactivation would unblock uninstall. */
    private val deviceAdminTokens = setOf(
        "device admin",
        "device administrator",
        "deactivate",
        "تطبيقات المسؤول",
        "مسؤول الجهاز",
        "مشرف الجهاز",
        "إلغاء التنشيط",
        "الغاء التنشيط",
    )

    fun isProtectionSettingsAttempt(
        sourcePackage: String,
        visibleText: String,
        ownPackage: String,
        appLabel: String = "Layngo Kids",
    ): Boolean {
        if (!isSettingsSurface(sourcePackage)) return false

        val normalized = visibleText.lowercase()
        val targetsLayngo = normalized.contains(ownPackage.lowercase()) ||
            normalized.contains(appLabel.lowercase()) ||
            normalized.contains("layngo")
        if (!targetsLayngo) return false

        return accessibilityTokens.any(normalized::contains) ||
            deviceAdminTokens.any(normalized::contains)
    }

    private fun isSettingsSurface(sourcePackage: String): Boolean =
        sourcePackage in settingsPackages ||
            sourcePackage.endsWith(".settings") ||
            sourcePackage.contains("securitycenter") ||
            sourcePackage.contains("safecenter")
}
