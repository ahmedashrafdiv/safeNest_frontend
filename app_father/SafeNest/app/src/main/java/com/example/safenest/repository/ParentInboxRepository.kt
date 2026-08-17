package com.example.safenest.repository

import com.example.safenest.network.AccessRequestApproveRequest
import com.example.safenest.network.AccessRequestItem
import com.example.safenest.network.AccessRequestRejectRequest
import com.example.safenest.network.AlertOut
import com.example.safenest.network.ApiClient
import com.example.safenest.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class ParentInboxRepository : BaseRepository() {
    private val api = ApiClient.apiService

    suspend fun load(): Result<ParentInboxData> {
        return try {
        val childrenResult = safeApiCall { api.getChildren() }
        val childIds = (childrenResult as? Result.Success)?.data?.map { it.childId }.orEmpty()
        if (childrenResult is Result.Error) return childrenResult
        coroutineScope {
            val alertsDeferred = async { safeApiCall { api.listAlerts() } }
            val requestsDeferred = async {
                val results = childIds.map { childId ->
                    safeApiCall { api.listChildAccessRequests(childId = childId, status = "pending") }
                }
                val errors = results.filterIsInstance<Result.Error>()
                if (errors.isNotEmpty()) {
                    Result.Error(errors.first().message)
                } else {
                    Result.Success(results.filterIsInstance<Result.Success<com.example.safenest.network.AccessRequestListResponse>>()
                        .flatMap { it.data.items })
                }
            }
            val alerts = alertsDeferred.await()
            val requests = requestsDeferred.await()
            when {
                alerts is Result.Error && requests is Result.Error -> Result.Error("Unable to load notifications and requests")
                else -> Result.Success(
                    ParentInboxData(
                        alerts = (alerts as? Result.Success<List<AlertOut>>)?.data.orEmpty().filter { !it.isResolved },
                        requests = (requests as? Result.Success<List<AccessRequestItem>>)?.data.orEmpty(),
                        alertsUnavailable = alerts is Result.Error,
                        requestsUnavailable = requests is Result.Error
                    )
                )
            }
        }
        } catch (error: Exception) {
            Result.Error(error.message ?: "Unable to load inbox")
        }
    }

    suspend fun approve(requestId: String, grantedSeconds: Int? = null): Result<AccessRequestItem> =
        safeApiCall { api.approveChildAccessRequest(requestId, AccessRequestApproveRequest(grantedSeconds)) }

    suspend fun reject(requestId: String): Result<AccessRequestItem> =
        safeApiCall { api.rejectChildAccessRequest(requestId, AccessRequestRejectRequest()) }

    suspend fun resolveAlert(alertId: String): Result<AlertOut> =
        safeApiCall { api.updateAlert(alertId, com.example.safenest.network.AlertUpdateRequest(true)) }
}

data class ParentInboxData(
    val alerts: List<AlertOut>,
    val requests: List<AccessRequestItem>,
    val alertsUnavailable: Boolean = false,
    val requestsUnavailable: Boolean = false
) {
    val actionableCount: Int
        get() = requests.size + alerts.count { !it.isResolved }
}
