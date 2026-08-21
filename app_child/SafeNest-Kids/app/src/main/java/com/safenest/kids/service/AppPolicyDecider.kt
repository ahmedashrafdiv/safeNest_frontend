package com.safenest.kids.service

object AppPolicyDecider {
    /**
     * The Layngo Parent app. Parent and Child are frequently installed on the same handset while
     * a parent sets the device up, and in allowlist mode every package outside the allowlist is
     * blocked — including the very app the parent needs in order to change that policy back.
     * Blocking it locks the parent out of their own controls with no in-app way back, so the
     * Parent app is never a blockable surface regardless of mode.
     */
    const val PARENT_APP_PACKAGE = "com.example.safenest"

    private val launcherTokens = listOf("launcher", "nexus", "pixel", "trebuchet", "systemui")
    private val protectedPackages = setOf(
        "android",
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller",
        PARENT_APP_PACKAGE,
    )

    fun isProtectedPackage(packageName: String, childPackage: String): Boolean {
        if (packageName == childPackage) return true
        if (packageName in protectedPackages || packageName.startsWith("com.android.systemui")) return true
        return launcherTokens.any { packageName.lowercase().contains(it) }
    }

    fun shouldBlock(
        packageName: String,
        childPackage: String,
        mode: String,
        allowedPackages: Set<String>,
        blockedPackages: Set<String>
    ): Boolean {
        if (isProtectedPackage(packageName, childPackage)) return false
        return if (mode == "allowlist") packageName !in allowedPackages else packageName in blockedPackages
    }
}
