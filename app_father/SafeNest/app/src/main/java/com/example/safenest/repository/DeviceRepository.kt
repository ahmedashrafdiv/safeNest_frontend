package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.DeviceOut
import com.example.safenest.network.DevicePairingRequest
import com.example.safenest.network.DeviceStatusResponse
import com.example.safenest.network.GeneratePinRequest
import com.example.safenest.network.GeneratePinResponse
import com.example.safenest.util.Result

class DeviceRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun pairDevice(deviceId: String, deviceName: String, deviceType: String): Result<DeviceOut> =
        safeApiCall { api.pairDevice(DevicePairingRequest(deviceId = deviceId, deviceName = deviceName, deviceType = deviceType)) }

    suspend fun listDevices(): Result<List<DeviceOut>> =
        safeApiCall { api.listDevices() }

    suspend fun listDevicesStatus(): Result<List<DeviceStatusResponse>> =
        safeApiCall { api.listDevicesStatus() }

    suspend fun deleteDevice(deviceId: String): Result<Map<String, Any>> =
        safeApiCall { api.deleteDevice(deviceId) }

    suspend fun generatePin(childId: String): Result<GeneratePinResponse> =
        safeApiCall { api.generatePin(GeneratePinRequest(childId)) }
}
