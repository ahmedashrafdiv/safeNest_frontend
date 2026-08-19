package com.safenest.kids.security

enum class SetupCapabilityStatus {
    READY,
    REQUIRES_ACTION,
    UNAVAILABLE,
    NOT_REQUIRED,
}

data class SetupCapabilitySnapshot(
    val deviceAdminActive: Boolean,
    val protectedHomeRequested: Boolean,
    val protectedHomeRoleAvailable: Boolean,
    val protectedHomeRoleHeld: Boolean,
    val overlayGranted: Boolean,
    val usageAccessGranted: Boolean,
    val accessibilityEnabled: Boolean,
    val batteryExemptionGranted: Boolean,
    val websiteProtectionRequired: Boolean,
    val vpnGranted: Boolean,
    val locationMonitoringRequired: Boolean,
    val locationGranted: Boolean,
)

data class SetupReadiness(
    val baselineReady: Boolean,
    val requiredStates: Map<SetupCapability, SetupCapabilityStatus>,
    val optionalStates: Map<SetupCapability, SetupCapabilityStatus>,
)

enum class SetupCapability {
    DEVICE_ADMIN,
    PROTECTED_HOME,
    OVERLAY,
    USAGE_ACCESS,
    ACCESSIBILITY,
    BACKGROUND_RELIABILITY,
    WEBSITE_PROTECTION,
    LOCATION_MONITORING,
}

/** Evaluates only Android-confirmed capability states; pairing alone never implies protection readiness. */
internal object SetupCapabilityEvaluator {
    fun evaluate(snapshot: SetupCapabilitySnapshot): SetupReadiness {
        val requiredStates = linkedMapOf(
            SetupCapability.DEVICE_ADMIN to snapshot.deviceAdminActive.toStatus(),
            SetupCapability.OVERLAY to snapshot.overlayGranted.toStatus(),
            SetupCapability.USAGE_ACCESS to snapshot.usageAccessGranted.toStatus(),
            SetupCapability.ACCESSIBILITY to snapshot.accessibilityEnabled.toStatus(),
            SetupCapability.BACKGROUND_RELIABILITY to snapshot.batteryExemptionGranted.toStatus(),
        )
        if (snapshot.protectedHomeRequested) {
            requiredStates[SetupCapability.PROTECTED_HOME] = protectedHomeStatus(snapshot)
        }
        val optionalStates = linkedMapOf<SetupCapability, SetupCapabilityStatus>()
        if (!snapshot.protectedHomeRequested) {
            optionalStates[SetupCapability.PROTECTED_HOME] = SetupCapabilityStatus.NOT_REQUIRED
        }
        optionalStates.putAll(mapOf(
            SetupCapability.WEBSITE_PROTECTION to conditionalStatus(
                required = snapshot.websiteProtectionRequired,
                granted = snapshot.vpnGranted,
            ),
            SetupCapability.LOCATION_MONITORING to conditionalStatus(
                required = snapshot.locationMonitoringRequired,
                granted = snapshot.locationGranted,
            ),
        ))
        return SetupReadiness(
            baselineReady = requiredStates.values.all { it == SetupCapabilityStatus.READY },
            requiredStates = requiredStates,
            optionalStates = optionalStates,
        )
    }

    private fun protectedHomeStatus(snapshot: SetupCapabilitySnapshot): SetupCapabilityStatus = when {
        !snapshot.protectedHomeRoleAvailable -> SetupCapabilityStatus.UNAVAILABLE
        snapshot.protectedHomeRoleHeld -> SetupCapabilityStatus.READY
        else -> SetupCapabilityStatus.REQUIRES_ACTION
    }

    private fun conditionalStatus(required: Boolean, granted: Boolean): SetupCapabilityStatus = when {
        !required -> SetupCapabilityStatus.NOT_REQUIRED
        granted -> SetupCapabilityStatus.READY
        else -> SetupCapabilityStatus.REQUIRES_ACTION
    }

    private fun Boolean.toStatus(): SetupCapabilityStatus =
        if (this) SetupCapabilityStatus.READY else SetupCapabilityStatus.REQUIRES_ACTION
}
