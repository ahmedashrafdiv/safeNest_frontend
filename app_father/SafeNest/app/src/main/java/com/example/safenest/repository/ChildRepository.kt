package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildCreateRequest
import com.example.safenest.network.ChildResponse
import com.example.safenest.network.ChildUpdateRequest
import com.example.safenest.network.InstalledAppItem
import com.example.safenest.network.InstalledAppsResponse
import com.example.safenest.network.InstalledAppsUpdateRequest
import com.example.safenest.network.MessageResponse
import com.example.safenest.util.Result

class ChildRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun getChildren(): Result<List<ChildResponse>> =
        safeApiCall { api.getChildren() }

    suspend fun createChild(name: String, gender: String, dateOfBirth: String, deviceId: String? = null): Result<ChildResponse> =
        safeApiCall { api.createChild(ChildCreateRequest(name = name, gender = gender, dateOfBirth = dateOfBirth, deviceId = deviceId)) }

    suspend fun getChild(childId: String): Result<ChildResponse> =
        safeApiCall { api.getChild(childId) }

    suspend fun updateChild(childId: String, name: String? = null, gender: String? = null, dateOfBirth: String? = null, deviceId: String? = null): Result<ChildResponse> =
        safeApiCall { api.updateChild(childId, ChildUpdateRequest(name = name, gender = gender, dateOfBirth = dateOfBirth, deviceId = deviceId)) }

    suspend fun deleteChild(childId: String): Result<MessageResponse> =
        safeApiCall { api.deleteChild(childId) }

    suspend fun getInstalledApps(childId: String): Result<InstalledAppsResponse> =
        safeApiCall { api.getInstalledApps(childId) }

    suspend fun updateInstalledApps(childId: String, apps: List<InstalledAppItem>): Result<MessageResponse> =
        safeApiCall { api.updateInstalledApps(childId, InstalledAppsUpdateRequest(apps = apps)) }
}
