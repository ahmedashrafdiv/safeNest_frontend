package com.example.safenest.policy

import com.example.safenest.network.SafeNestApiService
import java.io.IOException

sealed interface ScopedLocationMutation {
    data object Applied : ScopedLocationMutation
    data class Blocked(val message: String) : ScopedLocationMutation
    data class Failed(val message: String) : ScopedLocationMutation
}

class ParentPhoneLocationScopeCoordinator(
    private val api: SafeNestApiService,
) {
    suspend fun setDeviceTracking(
        childId: String,
        device: SelectedPolicyDevice,
        enabled: Boolean,
    ): ScopedLocationMutation {
        if (childId.isBlank() || !device.isEligible) {
            return ScopedLocationMutation.Blocked("Select an active device before changing tracking.")
        }
        return try {
            val response = api.setPhoneTrackingForDevice(childId, device.deviceId, enabled)
            if (response.isSuccessful) ScopedLocationMutation.Applied
            else ScopedLocationMutation.Failed("Unable to change tracking for this device (${response.code()}).")
        } catch (error: IOException) {
            ScopedLocationMutation.Failed("Network unavailable. Try again when the device is online.")
        }
    }

    suspend fun setSelectedDeviceTracking(enabled: Boolean): ScopedLocationMutation {
        val state = ParentPolicyScopeStore.state.value
        val childId = state.childId
        val device = state.selectedDevice
        if (!state.canWriteDeviceOverride || childId.isNullOrBlank() || device == null) {
            return ScopedLocationMutation.Blocked(state.blockedReason ?: "Select an active device before changing tracking.")
        }
        return setDeviceTracking(childId, device, enabled)
    }

    suspend fun deleteSelectedDeviceTracking(): ScopedLocationMutation {
        val state = ParentPolicyScopeStore.state.value
        val childId = state.childId
        val device = state.selectedDevice
        if (!state.canWriteDeviceOverride || childId.isNullOrBlank() || device == null) {
            return ScopedLocationMutation.Blocked(state.blockedReason ?: "Select an active device before removing tracking.")
        }
        return try {
            val response = api.deletePhoneTrackingForDevice(childId, device.deviceId)
            if (response.isSuccessful) ScopedLocationMutation.Applied
            else ScopedLocationMutation.Failed("Unable to remove tracking for this device (${response.code()}).")
        } catch (error: IOException) {
            ScopedLocationMutation.Failed("Network unavailable. Try again when the device is online.")
        }
    }
}
