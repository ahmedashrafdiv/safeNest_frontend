package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.util.Result

class LocationRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun pairGps(childId: String): Result<Unit> =
        safeApiCall { api.pairGps(childId) }

    suspend fun updateGpsFromThingspeak(childId: String): Result<Unit> =
        safeApiCall { api.updateGpsFromThingspeak(childId) }

    suspend fun deleteGps(childId: String): Result<Unit> =
        safeApiCall { api.deleteGps(childId) }

    suspend fun getChildLocation(childId: String): Result<Map<String, Any>> =
        safeApiCall { api.getChildLocation(childId) }
}
