package com.example.safenest.repository

import com.example.safenest.network.AlertOut
import com.example.safenest.network.AlertUpdateRequest
import com.example.safenest.network.ApiClient
import com.example.safenest.util.Result

class AlertRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun listAlerts(): Result<List<AlertOut>> =
        safeApiCall { api.listAlerts() }

    suspend fun updateAlert(alertId: String, isResolved: Boolean): Result<AlertOut> =
        safeApiCall { api.updateAlert(alertId, AlertUpdateRequest(isResolved = isResolved)) }

    suspend fun deleteAlert(alertId: String): Result<Map<String, Any>> =
        safeApiCall { api.deleteAlert(alertId) }
}
