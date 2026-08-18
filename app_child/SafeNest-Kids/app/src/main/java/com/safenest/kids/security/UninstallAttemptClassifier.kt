package com.safenest.kids.security

/**
 * Identifies only visible package-installer flows that explicitly name this Child app.
 * It deliberately does not classify general Settings or a generic app-management screen.
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

    fun isLayngoUninstallAttempt(
        sourcePackage: String,
        visibleText: String,
        ownPackage: String,
        appLabel: String = "Layngo Kids",
    ): Boolean {
        if (!isInstallerOrSecurityCenter(sourcePackage)) return false
        val normalized = visibleText.lowercase()
        return normalized.contains(ownPackage.lowercase()) ||
            normalized.contains(appLabel.lowercase())
    }

    private fun isInstallerOrSecurityCenter(sourcePackage: String): Boolean {
        return sourcePackage in installerPackages ||
            sourcePackage.endsWith(".packageinstaller") ||
            sourcePackage.contains("permissioncontroller") ||
            sourcePackage.contains("safecenter")
    }
}
