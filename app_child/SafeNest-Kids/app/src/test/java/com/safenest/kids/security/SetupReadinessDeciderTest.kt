package com.safenest.kids.security

import org.junit.Assert.assertEquals
import org.junit.Test

class SetupReadinessDeciderTest {
    @Test
    // Regression: Child setup previously continued without Device Administrator activation.
    fun requiredSecurityGateMatrixBlocksContinuationUntilEveryRequiredCapabilityIsPresent() {
        listOf(
            RequiredState(true, true, true, true, true),
            RequiredState(false, true, true, true, false),
            RequiredState(true, false, true, true, false),
            RequiredState(true, true, false, true, false),
            RequiredState(true, true, true, false, false),
        ).forEach { state ->
            assertEquals(
                state.expectedContinuation,
                SetupReadinessDecider.canContinue(
                    hasUsageAccess = state.hasUsageAccess,
                    hasAccessibility = state.hasAccessibility,
                    hasBatteryExemption = state.hasBatteryExemption,
                    hasDeviceAdmin = state.hasDeviceAdmin,
                ),
            )
        }
    }

    private data class RequiredState(
        val hasUsageAccess: Boolean,
        val hasAccessibility: Boolean,
        val hasBatteryExemption: Boolean,
        val hasDeviceAdmin: Boolean,
        val expectedContinuation: Boolean,
    )
}
