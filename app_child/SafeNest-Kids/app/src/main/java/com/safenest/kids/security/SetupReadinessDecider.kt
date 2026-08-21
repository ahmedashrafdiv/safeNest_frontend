package com.safenest.kids.security

/** Legacy compatibility helper for the initial setup gate until the guided coordinator is rendered. */
internal object SetupReadinessDecider {
    fun canContinue(
        hasUsageAccess: Boolean,
        hasAccessibility: Boolean,
        hasBatteryExemption: Boolean,
        hasDeviceAdmin: Boolean,
    ): Boolean = hasUsageAccess && hasAccessibility && hasBatteryExemption && hasDeviceAdmin
}
