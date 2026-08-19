package com.example.safenest.policy

import com.example.safenest.network.DevicePolicyOverrideRequest
import com.example.safenest.network.DevicePolicyOverrideResponse
import com.example.safenest.network.EffectiveProtectionPolicyResponse
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import java.util.UUID

sealed interface ProtectedHomePolicyMutation {
    data class Applied(val response: DevicePolicyOverrideResponse) : ProtectedHomePolicyMutation
    data class Failed(val message: String) : ProtectedHomePolicyMutation
    data class Conflict(val latest: EffectiveProtectionPolicyResponse) : ProtectedHomePolicyMutation
}

/** Saves only to the selected physical device; it never falls back to a child-default mutation. */
class ParentProtectedHomeScopeCoordinator(
    private val deviceRepository: ChildDeviceRepository,
) {
    suspend fun saveForDevice(
        childId: String,
        deviceId: String,
        requested: Boolean,
        expectedVersion: Int,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): ProtectedHomePolicyMutation {
        val request = DevicePolicyOverrideRequest(
            patch = if (requested) mapOf("protectedHomeRequested" to true) else emptyMap(),
            expectedVersion = expectedVersion,
            idempotencyKey = idempotencyKey,
            clear = !requested,
        )
        return try {
            val response = deviceRepository.putDevicePolicyOverrideRaw(
                childId = childId,
                deviceId = deviceId,
                policyFamily = "protection",
                request = request,
            )
            when {
                response.isSuccessful && response.body() != null -> ProtectedHomePolicyMutation.Applied(response.body()!!)
                response.code() == 409 -> when (val refreshed = deviceRepository.getEffectiveProtectionPolicy(childId, deviceId)) {
                    is Result.Success -> ProtectedHomePolicyMutation.Conflict(refreshed.data)
                    is Result.Error -> ProtectedHomePolicyMutation.Failed(
                        "Protection policy changed, but the latest state could not be loaded: ${refreshed.message}",
                    )
                    Result.Loading -> ProtectedHomePolicyMutation.Failed(
                        "Protection policy changed. Refresh this device before trying again.",
                    )
                }
                else -> ProtectedHomePolicyMutation.Failed("Protection policy save failed (${response.code()}).")
            }
        } catch (error: Exception) {
            ProtectedHomePolicyMutation.Failed(error.message ?: "Network error while saving protection policy.")
        }
    }
}
