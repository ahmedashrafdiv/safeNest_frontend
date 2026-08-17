package com.safenest.kids.service

object AppPolicyDecider {
    private val launcherTokens = listOf("launcher", "nexus", "pixel", "trebuchet", "systemui", "android")
    private val protectedPackages = setOf(
        "android",
        "com.android.settings",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.android.packageinstaller"
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
