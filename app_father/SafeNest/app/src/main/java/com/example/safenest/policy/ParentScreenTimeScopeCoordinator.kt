package com.example.safenest.policy

import com.example.safenest.network.SafeNestApiService
import com.example.safenest.network.ScreenTimePolicyCreateRequest
import java.io.IOException
import java.time.Instant

sealed interface ScopedScreenTimeMutation {
    data object Applied : ScopedScreenTimeMutation
    data class Blocked(val message: String) : ScopedScreenTimeMutation
    data class Failed(val message: String) : ScopedScreenTimeMutation
}

class ParentScreenTimeScopeCoordinator(
    private val api: SafeNestApiService,
) {
    suspend fun saveSelectedDeviceLimit(minutes: Int): ScopedScreenTimeMutation {
        val state = ParentPolicyScopeStore.state.value
        val childId = state.childId
        val device = state.selectedDevice
        if (!state.canWriteDeviceOverride || childId.isNullOrBlank() || device == null) {
            return ScopedScreenTimeMutation.Blocked(state.blockedReason ?: "Select an active device before saving Screen Time.")
        }
        return try {
            val response = api.createScreenTimePolicy(
                childId,
                ScreenTimePolicyCreateRequest(
                    deviceId = device.deviceId,
                    dailyLimitSeconds = minutes.coerceIn(1, 1440) * 60,
                    timezone = "UTC",
                    safeDefaultAction = "block_with_explanation",
                    effectiveFrom = Instant.now().toString(),
                ),
            )
            if (response.isSuccessful) ScopedScreenTimeMutation.Applied
            else ScopedScreenTimeMutation.Failed("Unable to save Screen Time for this device (${response.code()}).")
        } catch (error: IOException) {
            ScopedScreenTimeMutation.Failed("Network unavailable. Try again when the device is online.")
        }
    }
}
