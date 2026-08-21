package com.example.safenest.policy

import com.example.safenest.network.DevicePolicyOverrideRequest
import com.example.safenest.network.DevicePolicyOverrideResponse
import com.example.safenest.network.EffectiveContentBlurPolicyResponse
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import java.util.UUID

sealed interface ContentBlurPolicyMutation {
    data class Applied(val response: DevicePolicyOverrideResponse) : ContentBlurPolicyMutation
    data class Conflict(val latest: EffectiveContentBlurPolicyResponse) : ContentBlurPolicyMutation
    data class Failed(val message: String) : ContentBlurPolicyMutation
}

/** Selected-device Content Blur writes never fall back to child-default mutation. */
class ParentContentBlurScopeCoordinator(
    private val deviceRepository: ChildDeviceRepository,
) {
    suspend fun saveForDevice(
        childId: String,
        deviceId: String,
        enabled: Boolean,
        mode: String,
        targetPackages: List<String>,
        expectedVersion: Int,
        idempotencyKey: String = UUID.randomUUID().toString(),
    ): ContentBlurPolicyMutation {
        val request = DevicePolicyOverrideRequest(
            patch = if (enabled) {
                mapOf(
                    "enabled" to true,
                    "mode" to mode,
                    "targetPackages" to targetPackages,
                )
            } else {
                emptyMap()
            },
            expectedVersion = expectedVersion,
            idempotencyKey = idempotencyKey,
            clear = !enabled,
        )
        return try {
            val response = deviceRepository.putDevicePolicyOverrideRaw(
                childId = childId,
                deviceId = deviceId,
                policyFamily = "content_blur",
                request = request,
            )
            when {
                response.isSuccessful && response.body() != null -> ContentBlurPolicyMutation.Applied(response.body()!!)
                response.code() == 409 -> when (val refreshed = deviceRepository.getEffectiveContentBlurPolicy(childId, deviceId)) {
                    is Result.Success -> ContentBlurPolicyMutation.Conflict(refreshed.data)
                    is Result.Error -> ContentBlurPolicyMutation.Failed(
                        "Content Blur policy changed, but the latest state could not be loaded: ${refreshed.message}",
                    )
                    Result.Loading -> ContentBlurPolicyMutation.Failed(
                        "Content Blur policy changed. Refresh this device before trying again.",
                    )
                }
                else -> ContentBlurPolicyMutation.Failed("Content Blur policy save failed (${response.code()}).")
            }
        } catch (error: Exception) {
            ContentBlurPolicyMutation.Failed(error.message ?: "Network error while saving Content Blur policy.")
        }
    }
}
