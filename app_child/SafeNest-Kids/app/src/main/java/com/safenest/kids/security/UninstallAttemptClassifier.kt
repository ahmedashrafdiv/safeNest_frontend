package com.safenest.kids.security

/**
 * Identifies visible removal surfaces that explicitly target this Child app.
 * General Settings remains available: a Settings event is classified only when the
 * visible page names Layngo and exposes an explicit removal action.
 */
object UninstallAttemptClassifier {
    private val installerPackages = setOf(
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
    )

    private val settingsPackages = setOf(
        "com.android.settings",
        "com.coloros.settings",
        "com.oplus.settings",
    )

    private val removalActionTokens = setOf(
        "uninstall",
        "remove",
        "delete",
        "إلغاء التثبيت",
        "ازالة التثبيت",
        "حذف",
    )

    fun isLayngoRemovalAttempt(
        sourcePackage: String,
        visibleText: String,
        ownPackage: String,
        appLabel: String = "Layngo Kids",
    ): Boolean {
        val normalized = visibleText.lowercase()
        val targetsLayngo = normalized.contains(ownPackage.lowercase()) ||
            normalized.contains(appLabel.lowercase())
        if (!targetsLayngo) return false

        val exposesRemovalAction = removalActionTokens.any(normalized::contains)
        return exposesRemovalAction && (
            isInstallerOrSecurityCenter(sourcePackage) ||
                sourcePackage in settingsPackages
        )
    }

    private fun isInstallerOrSecurityCenter(sourcePackage: String): Boolean {
        return sourcePackage in installerPackages ||
            sourcePackage.endsWith(".packageinstaller") ||
            sourcePackage.contains("permissioncontroller") ||
            sourcePackage.contains("safecenter")
    }
}
