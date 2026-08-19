package com.example.safenest.policy

/**
 * GPS reads and phone-location changes must target one visible active device.
 * A sole active device can be selected for viewing; multiple devices require an
 * explicit Parent choice and never fall back to a child-wide location record.
 */
object GpsDeviceSelectionResolver {
    fun resolve(
        scopeState: ParentPolicyScopeState,
        availableDevices: List<SelectedPolicyDevice>,
    ): SelectedPolicyDevice? {
        val activeDevices = availableDevices.filter { it.isEligible }
        val scopedDevice = scopeState.selectedDevice
        if (scopeState.scope == ParentPolicyScope.SELECTED_DEVICE && scopedDevice != null) {
            return activeDevices.firstOrNull { it.deviceId == scopedDevice.deviceId }
        }
        return activeDevices.singleOrNull()
    }
}
