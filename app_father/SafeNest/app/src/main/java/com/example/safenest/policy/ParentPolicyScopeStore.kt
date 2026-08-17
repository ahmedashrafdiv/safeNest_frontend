package com.example.safenest.policy

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ParentPolicyScope {
    CHILD_DEFAULT,
    SELECTED_DEVICE,
}

data class SelectedPolicyDevice(
    val deviceId: String,
    val label: String,
    val status: String,
    val lastSeenAt: String? = null,
) {
    val isEligible: Boolean
        get() = status.equals("active", ignoreCase = true)
}

data class ParentPolicyScopeState(
    val childId: String? = null,
    val scope: ParentPolicyScope = ParentPolicyScope.CHILD_DEFAULT,
    val selectedDevice: SelectedPolicyDevice? = null,
) {
    val canWriteDeviceOverride: Boolean
        get() = scope == ParentPolicyScope.SELECTED_DEVICE && selectedDevice?.isEligible == true

    val blockedReason: String?
        get() = when {
            scope != ParentPolicyScope.SELECTED_DEVICE -> null
            selectedDevice == null -> "Select a device before applying a device override."
            !selectedDevice.isEligible -> "This device is not active and cannot receive policy overrides."
            else -> null
        }
}

/**
 * Process-local shared state for Parent policy editors. The backend remains the
 * authority for ownership and version checks; this state prevents accidental UI
 * fallbacks and lets every editor show the same selected-device context.
 */
object ParentPolicyScopeStore {
    private val mutableState = MutableStateFlow(ParentPolicyScopeState())
    val state: StateFlow<ParentPolicyScopeState> = mutableState.asStateFlow()

    fun selectChildDefault(childId: String?) {
        mutableState.value = mutableState.value.copy(
            childId = childId,
            scope = ParentPolicyScope.CHILD_DEFAULT,
        )
    }

    fun selectDevice(childId: String?, device: SelectedPolicyDevice) {
        mutableState.value = ParentPolicyScopeState(
            childId = childId,
            scope = ParentPolicyScope.SELECTED_DEVICE,
            selectedDevice = device,
        )
    }

    fun clearUnavailableDevice(deviceId: String) {
        val current = mutableState.value
        if (current.selectedDevice?.deviceId == deviceId && !current.selectedDevice.isEligible) {
            mutableState.value = current.copy(
                scope = ParentPolicyScope.CHILD_DEFAULT,
                selectedDevice = null,
            )
        }
    }
}
