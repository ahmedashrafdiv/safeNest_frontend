package com.safenest.kids.service

class BlurScopeDecider(
    private val config: BlurConfig,
) {
    fun shouldProcess(visiblePackages: Set<String>): Boolean {
        if (visiblePackages.isEmpty()) return false
        val eligible = visiblePackages - config.excludedPackages
        return eligible.any { it in config.targetPackages }
    }

    fun isTargetPackage(packageName: String): Boolean =
        packageName in config.targetPackages && packageName !in config.excludedPackages

    fun filterTargetPackages(packages: Set<String>): Set<String> =
        packages.filterTo(linkedSetOf()) { isValidTargetPackage(it) }

    private fun isValidTargetPackage(packageName: String): Boolean {
        if (packageName.isBlank() || packageName.length > 255) return false
        if (packageName in config.excludedPackages) return false
        if (packageName.startsWith("com.android.")) return false
        if (packageName.startsWith("android.")) return false
        return packageName.matches(PACKAGE_PATTERN)
    }

    private companion object {
        val PACKAGE_PATTERN = Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+")
    }
}
