package com.safenest.kids.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupCapabilityEvaluatorTest {
    @Test
    fun test_parent_not_requesting_protected_home_keeps_baseline_setup_unlocked() {
        val readiness = SetupCapabilityEvaluator.evaluate(snapshot())

        assertTrue(readiness.baselineReady)
        assertEquals(SetupCapabilityStatus.NOT_REQUIRED, readiness.optionalStates[SetupCapability.PROTECTED_HOME])
        assertEquals(SetupCapabilityStatus.NOT_REQUIRED, readiness.optionalStates[SetupCapability.WEBSITE_PROTECTION])
    }

    @Test
    fun test_parent_requested_protected_home_keeps_baseline_setup_locked_until_role_is_held() {
        val readiness = SetupCapabilityEvaluator.evaluate(
            snapshot(protectedHomeRequested = true, protectedHomeRoleHeld = false),
        )

        assertFalse(readiness.baselineReady)
        assertEquals(SetupCapabilityStatus.REQUIRES_ACTION, readiness.requiredStates[SetupCapability.PROTECTED_HOME])
    }

    @Test
    fun test_parent_requested_unavailable_home_role_is_not_reported_as_protected() {
        val readiness = SetupCapabilityEvaluator.evaluate(
            snapshot(
                protectedHomeRequested = true,
                protectedHomeRoleAvailable = false,
                protectedHomeRoleHeld = false,
            ),
        )

        assertFalse(readiness.baselineReady)
        assertEquals(SetupCapabilityStatus.UNAVAILABLE, readiness.requiredStates[SetupCapability.PROTECTED_HOME])
    }

    private fun snapshot(
        protectedHomeRequested: Boolean = false,
        protectedHomeRoleAvailable: Boolean = true,
        protectedHomeRoleHeld: Boolean = true,
    ) = SetupCapabilitySnapshot(
        deviceAdminActive = true,
        protectedHomeRequested = protectedHomeRequested,
        protectedHomeRoleAvailable = protectedHomeRoleAvailable,
        protectedHomeRoleHeld = protectedHomeRoleHeld,
        overlayGranted = true,
        usageAccessGranted = true,
        accessibilityEnabled = true,
        batteryExemptionGranted = true,
        websiteProtectionRequired = false,
        vpnGranted = false,
        locationMonitoringRequired = false,
        locationGranted = false,
    )
}
