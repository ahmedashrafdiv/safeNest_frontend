package com.example.safenest.policy

import com.example.safenest.network.DevicePolicyOverrideRequest
import com.example.safenest.network.DevicePolicyOverrideResponse
import com.example.safenest.network.EffectiveAppBlockingPolicyResponse
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import java.util.UUID

sealed interface ScopedPolicyMutation {
    data class Applied(val response: DevicePolicyOverrideResponse) : ScopedPolicyMutation
    data class Blocked(val message: String) : ScopedPolicyMutation
    data class Failed(val message: String) : ScopedPolicyMutation
    data class Conflict(val latest: EffectiveAppBlockingPolicyResponse) : ScopedPolicyMutation
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
                state.blockedReason ?: "اختر جهازًا نشطًا قبل حفظ تعديل خاص بالجهاز.",
            )
        }
        val request = DevicePolicyOverrideRequest(
            patch = patch,
            expectedVersion = expectedVersion,
            idempotencyKey = idempotencyKey,
        )
        return try {
            val response = deviceRepository.putDevicePolicyOverrideRaw(
                childId,
                device.deviceId,
                "app_blocking",
                request,
            )
            when {
                response.isSuccessful && response.body() != null -> {
                    ScopedPolicyMutation.Applied(response.body()!!)
                }
                response.code() == 409 -> {
                    when (val refreshed = deviceRepository.getEffectiveAppBlockingPolicy(childId, device.deviceId)) {
                        is Result.Success -> ScopedPolicyMutation.Conflict(refreshed.data)
                        is Result.Error -> ScopedPolicyMutation.Failed(
                            "تغيّرت القاعدة، وتعذر تحميل أحدث إصدار منها. حاول مرة أخرى.",
                        )
                        Result.Loading -> ScopedPolicyMutation.Failed(
                            "تغيّرت القاعدة. راجع القاعدة الحالية قبل المحاولة مرة أخرى.",
                        )
                    }
                }
                else -> ScopedPolicyMutation.Failed("تعذر حفظ القاعدة (${response.code()}).")
            }
        } catch (error: Exception) {
            ScopedPolicyMutation.Failed(error.message ?: "حدث خطأ في الاتصال أثناء حفظ القاعدة.")
        }
    }
}
