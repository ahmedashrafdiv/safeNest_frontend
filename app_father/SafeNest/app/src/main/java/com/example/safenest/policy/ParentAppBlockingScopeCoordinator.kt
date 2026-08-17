package com.example.safenest.policy

import com.example.safenest.network.DevicePolicyOverrideRequest
import com.example.safenest.network.DevicePolicyOverrideResponse
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import java.util.UUID

sealed interface ScopedPolicyMutation {
    data class Applied(val response: DevicePolicyOverrideResponse) : ScopedPolicyMutation
    data class Blocked(val message: String) : ScopedPolicyMutation
    data class Failed(val message: String) : ScopedPolicyMutation
}

/**
 * Bridges the shared Parent scope selection to the existing secure App Blocking
 * override contract. Child-default writes deliberately remain on the legacy
 * DigitalRule path; selected-device writes never fall back to that path.
 */
class ParentAppBlockingScopeCoordinator(
    private val deviceRepository: ChildDeviceRepository,
) {
    suspend fun saveSelectedDeviceOverride(
        patch: Map<String, Any?>,
        expectedVersion: Int,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): ScopedPolicyMutation {
        val state = ParentPolicyScopeStore.state.value
        val childId = state.childId
        val device = state.selectedDevice
        if (!state.canWriteDeviceOverride || childId.isNullOrBlank() || device == null) {
            return ScopedPolicyMutation.Blocked(
                state.blockedReason ?: "Select an active device before saving a device override.",
            )
        }
        return when (
            val result = deviceRepository.putDevicePolicyOverride(
                childId = childId,
                deviceId = device.deviceId,
                policyFamily = "app_blocking",
                request = DevicePolicyOverrideRequest(
                    patch = patch,
                    expectedVersion = expectedVersion,
                    idempotencyKey = idempotencyKey,
                ),
            )
        ) {
            is Result.Success -> ScopedPolicyMutation.Applied(result.data)
            is Result.Error -> ScopedPolicyMutation.Failed(result.message)
            Result.Loading -> ScopedPolicyMutation.Failed("Policy save did not finish.")
        }
    }
}
