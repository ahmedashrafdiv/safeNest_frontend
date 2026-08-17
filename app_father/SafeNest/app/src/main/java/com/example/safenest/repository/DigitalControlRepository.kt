package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.AppUsageUpdateRequest
import com.example.safenest.network.DigitalRuleCreateRequest
import com.example.safenest.network.DigitalRuleResponse
import com.example.safenest.network.DigitalRuleUpdateRequest
import com.example.safenest.network.MessageResponse
import com.example.safenest.network.VideoHistoryCreateRequest
import com.example.safenest.util.Result

class DigitalControlRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun createDigitalRule(
        childId: String,
        maxScreenTime: Int? = null,
        blockedApp: List<String> = emptyList(),
        allowedApp: List<String> = emptyList(),
        appControlMode: String = "blocklist"
    ): Result<DigitalRuleResponse> = safeApiCall {
        api.createDigitalRule(
            DigitalRuleCreateRequest(
                childId = childId,
                maxScreenTime = maxScreenTime,
                blockedApp = blockedApp,
                allowedApp = allowedApp,
                appControlMode = appControlMode
            )
        )
    }

    suspend fun getDigitalRule(childId: String): Result<DigitalRuleResponse> =
        safeApiCall { api.getDigitalRule(childId) }

    suspend fun updateDigitalRule(
        ruleId: String,
        maxScreenTime: Int? = null,
        blockedApp: List<String>? = null,
        allowedApp: List<String>? = null,
        appTimeLimits: Map<String, Int>? = null,
        appControlMode: String? = null
    ): Result<DigitalRuleResponse> =
        safeApiCall {
            api.updateDigitalRule(
                ruleId,
                DigitalRuleUpdateRequest(
                    maxScreenTime = maxScreenTime,
                    blockedApp = blockedApp,
                    allowedApp = allowedApp,
                    appTimeLimits = appTimeLimits,
                    appControlMode = appControlMode
                )
            )
        }

    suspend fun deleteDigitalRule(ruleId: String): Result<Unit> =
        safeApiCall { api.deleteDigitalRule(ruleId) }

    suspend fun updateAppUsage(childId: String, usage: Map<String, Int>): Result<MessageResponse> =
        safeApiCall { api.updateAppUsage(AppUsageUpdateRequest(childId = childId, usage = usage)) }

    suspend fun clearDailyUsageLog(childId: String): Result<Unit> =
        safeApiCall { api.clearDailyUsageLog(childId) }

    suspend fun addVideoHistory(childId: String, videos: List<Map<String, String>>): Result<Unit> =
        safeApiCall { api.addVideoHistory(VideoHistoryCreateRequest(childId = childId, videos = videos)) }

    suspend fun getVideoHistory(childId: String): Result<List<Map<String, Any>>> =
        safeApiCall { api.getVideoHistory(childId) }

    suspend fun clearVideoHistory(childId: String): Result<Unit> =
        safeApiCall { api.clearVideoHistory(childId) }
}
