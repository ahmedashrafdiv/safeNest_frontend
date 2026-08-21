package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildPlaceResponse
import com.example.safenest.network.PlaceCreateRequest
import com.example.safenest.network.PlaceUpdateRequest
import com.example.safenest.util.Result

class PlaceRepository {
    private val api = ApiClient.apiService

    suspend fun listPlaces(childId: String): Result<List<ChildPlaceResponse>> = runRequest { api.listChildPlaces(childId) }

    suspend fun createPlace(childId: String, request: PlaceCreateRequest): Result<ChildPlaceResponse> = runRequest { api.createChildPlace(childId, request) }

    suspend fun updatePlace(childId: String, placeId: String, request: PlaceUpdateRequest): Result<ChildPlaceResponse> = runRequest { api.updateChildPlace(childId, placeId, request) }

    suspend fun deletePlace(childId: String, placeId: String): Result<Unit> {
        return try {
            val response = api.deleteChildPlace(childId, placeId)
            if (response.isSuccessful) Result.Success(Unit) else Result.Error(ApiClient.parseError(response.errorBody()?.string()))
        } catch (_: Exception) {
            Result.Error("تعذر الاتصال بالخدمة. حاول مرة أخرى.")
        }
    }

    private suspend fun <T> runRequest(request: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = request()
            val body = response.body()
            if (response.isSuccessful && body != null) Result.Success(body) else Result.Error(ApiClient.parseError(response.errorBody()?.string()))
        } catch (_: Exception) {
            Result.Error("تعذر الاتصال بالخدمة. حاول مرة أخرى.")
        }
    }
}
