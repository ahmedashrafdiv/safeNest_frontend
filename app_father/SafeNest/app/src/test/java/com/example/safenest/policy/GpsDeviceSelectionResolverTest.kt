package com.example.safenest.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GpsDeviceSelectionResolverTest {
    @Test
    fun explicitActiveScopeDeviceIsRetained() {
        val selected = SelectedPolicyDevice("device-a", "Realme", "active")
        val result = GpsDeviceSelectionResolver.resolve(
            ParentPolicyScopeState("child-1", ParentPolicyScope.SELECTED_DEVICE, selected),
            listOf(selected, SelectedPolicyDevice("device-b", "Tablet", "active")),
        )

        assertEquals("device-a", result?.deviceId)
    }

    @Test
    fun soleActiveDeviceIsSafeToSelectForViewing() {
        val result = GpsDeviceSelectionResolver.resolve(
            ParentPolicyScopeState(childId = "child-1"),
            listOf(
                SelectedPolicyDevice("device-a", "Realme", "active"),
                SelectedPolicyDevice("device-old", "Old phone", "revoked"),
            ),
        )

        assertEquals("device-a", result?.deviceId)
    }

    @Test
    fun multipleActiveDevicesRequireExplicitParentSelection() {
        val result = GpsDeviceSelectionResolver.resolve(
            ParentPolicyScopeState(childId = "child-1"),
            listOf(
                SelectedPolicyDevice("device-a", "Realme", "active"),
                SelectedPolicyDevice("device-b", "Tablet", "active"),
            ),
        )

        assertNull(result)
    }
}
