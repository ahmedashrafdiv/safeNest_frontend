package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildDeviceAuditResult
import com.example.safenest.network.ChildDevicePairingRequest
import com.example.safenest.network.ChildDevicePairingResponse
import com.example.safenest.network.ChildDeviceRevokeRequest
import com.example.safenest.network.ChildDeviceSummary
import com.example.safenest.util.Result

/** Selected-device operations always include the owning child. */
class ChildDeviceRepository : BaseRepository() {
    private val api = ApiClient.apiService

    suspend fun listDevices(childId: String): Result<List<ChildDeviceSummary>> =
        safeApiCall { api.listChildDevices(childId) }

    suspend fun createPairing(childId: String): Result<ChildDevicePairingResponse> =
        safeApiCall { api.createChildDevicePairing(childId, ChildDevicePairingRequest()) }

    suspend fun revokeDevice(
        childId: String,
        deviceId: String,
        reasonCode: String,
    ): Result<ChildDeviceAuditResult> = safeApiCall {
        api.revokeChildDevice(childId, deviceId, ChildDeviceRevokeRequest(reasonCode))
    }
    suspend fun putDevicePolicyOverride(
        childId: String,
        deviceId: String,
        policyFamily: String,
        request: com.example.safenest.network.DevicePolicyOverrideRequest,
    ): Result<com.example.safenest.network.DevicePolicyOverrideResponse> =
        safeApiCall { api.putDevicePolicyOverride(childId, deviceId, policyFamily, request) }
}

