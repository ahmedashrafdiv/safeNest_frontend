package com.safenest.kids.service

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceBindingDeciderTest {
    @Test
    fun appliesOnlyNewerPolicyBoundToCurrentEnrollment() {
        assertEquals(
            DeviceBindingDecider.Decision.APPLY,
            DeviceBindingDecider.decide("device-a", "child-a", "device-a", "child-a", 4, 5),
        )
    }

    @Test
    fun rejectsSiblingDevicePolicy() {
        assertEquals(
            DeviceBindingDecider.Decision.DEVICE_MISMATCH,
            DeviceBindingDecider.decide("device-a", "child-a", "device-b", "child-a", 4, 5),
        )
    }

    @Test
    fun rejectsDifferentChildPolicy() {
        assertEquals(
            DeviceBindingDecider.Decision.CHILD_MISMATCH,
            DeviceBindingDecider.decide("device-a", "child-a", "device-a", "child-b", 4, 5),
        )
    }

    @Test
    fun keepsCurrentPolicyWhenVersionMatches() {
        assertEquals(
            DeviceBindingDecider.Decision.CURRENT_POLICY,
            DeviceBindingDecider.decide("device-a", "child-a", "device-a", "child-a", 5, 5),
        )
    }

    @Test
    fun rejectsOlderPolicyAfterNewerPolicyWasApplied() {
        assertEquals(
            DeviceBindingDecider.Decision.STALE_POLICY,
            DeviceBindingDecider.decide("device-a", "child-a", "device-a", "child-a", 5, 4),
        )
    }

    @Test
    fun requiresEnrollmentBoundDeviceId() {
        assertEquals(
            DeviceBindingDecider.Decision.UNBOUND_DEVICE,
            DeviceBindingDecider.decide(null, "child-a", "device-a", "child-a", 0, 1),
        )
    }
}
