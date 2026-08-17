package com.example.safenest.policy

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentPolicyScopeStateTest {
    @After
    fun resetScope() {
        ParentPolicyScopeStore.selectChildDefault(null)
    }

    @Test
    fun test_active_selected_device_allows_device_override() {
        ParentPolicyScopeStore.selectDevice(
            childId = "child-1",
            device = SelectedPolicyDevice("device-1", "Pixel", "active"),
        )

        val state = ParentPolicyScopeStore.state.value

        assertEquals(ParentPolicyScope.SELECTED_DEVICE, state.scope)
        assertTrue(state.canWriteDeviceOverride)
    }

    @Test
    fun test_missing_target_blocks_device_override() {
        val state = ParentPolicyScopeState(
            childId = "child-1",
            scope = ParentPolicyScope.SELECTED_DEVICE,
        )

        assertFalse(state.canWriteDeviceOverride)
    }

    @Test
    fun test_revoked_selected_device_blocks_device_override() {
        ParentPolicyScopeStore.selectDevice(
            childId = "child-1",
            device = SelectedPolicyDevice("device-1", "Old phone", "revoked"),
        )

        assertFalse(ParentPolicyScopeStore.state.value.canWriteDeviceOverride)
    }

    @Test
    fun test_clearing_revoked_selected_device_restores_child_default_scope() {
        ParentPolicyScopeStore.selectDevice(
            childId = "child-1",
            device = SelectedPolicyDevice("device-1", "Old phone", "revoked"),
        )

        ParentPolicyScopeStore.clearUnavailableDevice("device-1")

        val state = ParentPolicyScopeStore.state.value
        assertEquals(ParentPolicyScope.CHILD_DEFAULT, state.scope)
        assertFalse(state.canWriteDeviceOverride)
    }
}
