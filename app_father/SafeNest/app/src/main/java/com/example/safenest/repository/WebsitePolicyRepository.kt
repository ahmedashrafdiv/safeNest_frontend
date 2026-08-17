package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildResponse
import com.example.safenest.network.WebsiteAssignmentRequest
import com.example.safenest.network.WebsitePolicyCreateRequest
import com.example.safenest.network.WebsitePolicyListResponse
import com.example.safenest.network.WebsitePolicyResponse
import com.example.safenest.network.WebsitePolicyUpdateRequest
import com.example.safenest.network.WebsitePublishResponse
import com.example.safenest.network.WebsiteRuleCreateRequest
import com.example.safenest.network.WebsiteRuleListResponse
import com.example.safenest.util.Result

class WebsitePolicyRepository : BaseRepository() {
    private val api = ApiClient.apiService

    suspend fun listPolicies(): Result<WebsitePolicyListResponse> = safeApiCall { api.listWebsitePolicies() }

    suspend fun getChild(childId: String): Result<ChildResponse> = safeApiCall { api.getChild(childId) }

    suspend fun createPolicy(mode: String, categories: List<String>): Result<WebsitePolicyResponse> = safeApiCall {
        api.createWebsitePolicy(
            WebsitePolicyCreateRequest(
                name = "Layngo Website Protection",
                description = "Parent-managed website protection policy",
                websiteControlMode = mode,
                mandatoryBlockedCategories = categories
            )
        )
    }

    suspend fun updatePolicy(policyId: String, mode: String, categories: List<String>): Result<WebsitePolicyResponse> = safeApiCall {
        api.updateWebsitePolicy(
            policyId,
            WebsitePolicyUpdateRequest(
                websiteControlMode = mode,
                mandatoryBlockedCategories = categories
            )
        )
    }

    suspend fun listRules(policyId: String): Result<WebsiteRuleListResponse> = safeApiCall {
        api.listWebsiteRules(policyId)
    }

    suspend fun addHostRule(policyId: String, host: String, action: String): Result<com.example.safenest.network.WebsiteRuleResponse> = safeApiCall {
        api.createWebsiteRule(
            policyId,
            WebsiteRuleCreateRequest(
                scope = "host",
                pattern = host,
                matchMode = "host_and_subdomains",
                action = action,
                priority = if (action == "block") 900 else 800
            )
        )
    }

    suspend fun publish(policyId: String): Result<WebsitePublishResponse> = safeApiCall {
        api.publishWebsitePolicy(policyId)
    }

    suspend fun assign(policyId: String, child: ChildResponse): Result<Map<String, Any>> = safeApiCall {
        api.assignWebsitePolicy(
            policyId,
            WebsiteAssignmentRequest(childId = child.childId, deviceId = child.deviceId ?: "")
        )
    }
}
