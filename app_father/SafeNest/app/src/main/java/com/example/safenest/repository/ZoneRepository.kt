package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.ZoneCreateRequest
import com.example.safenest.network.ZoneResponse
import com.example.safenest.network.ZoneUpdateRequest
import com.example.safenest.util.Result

class ZoneRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun createZone(
        name: String,
        zoneType: String,
        childId: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): Result<ZoneResponse> = safeApiCall {
        api.createZone(
            ZoneCreateRequest(
                name = name,
                zoneType = zoneType,
                childId = childId,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters
            )
        )
    }

    suspend fun getChildZones(childId: String): Result<List<ZoneResponse>> =
        safeApiCall { api.getChildZones(childId) }

    suspend fun updateZone(
        zoneId: String,
        name: String? = null,
        zoneType: String? = null,
        childId: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        radiusMeters: Int? = null
    ): Result<ZoneResponse> = safeApiCall {
        api.updateZone(
            zoneId,
            ZoneUpdateRequest(
                name = name,
                zoneType = zoneType,
                childId = childId,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters
            )
        )
    }

    suspend fun deleteZone(zoneId: String): Result<Unit> =
        safeApiCall { api.deleteZone(zoneId) }
}
