package com.safenest.kids.service

/**
 * Layngo child-device binding policy. A response is only applicable when it is
 * explicitly bound to this enrollment, this child, and a newer policy version.
 */
object DeviceBindingDecider {
    enum class Decision {
        APPLY,
        DEVICE_MISMATCH,
        CHILD_MISMATCH,
        STALE_POLICY,
        UNBOUND_DEVICE,
    }

    fun decide(
        localDeviceId: String?,
        localChildId: String?,
        responseDeviceId: String,
        responseChildId: String,
        currentPolicyVersion: Int,
        incomingPolicyVersion: Int,
    ): Decision = when {
        localDeviceId.isNullOrBlank() -> Decision.UNBOUND_DEVICE
        localDeviceId != responseDeviceId -> Decision.DEVICE_MISMATCH
        !localChildId.isNullOrBlank() && localChildId != responseChildId -> Decision.CHILD_MISMATCH
        incomingPolicyVersion <= currentPolicyVersion -> Decision.STALE_POLICY
        else -> Decision.APPLY
    }
}
