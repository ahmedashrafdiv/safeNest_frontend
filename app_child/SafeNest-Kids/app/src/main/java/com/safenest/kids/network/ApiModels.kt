package com.safenest.kids.network

import com.google.gson.annotations.SerializedName

data class LinkDeviceRequest(
    @SerializedName("pin_code") val pinCode: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("fcm_token") val fcmToken: String?
)

data class LinkDeviceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("message") val message: String?, // For error messages
    @SerializedName("access_token") val accessToken: String? = null // Device token for authenticated child endpoints
)

data class DigitalRuleResponse(
    @SerializedName("ruleId") val ruleId: String?,
    @SerializedName("max_screen_time") val maxScreenTime: Int?,
    @SerializedName("blockedApp") val blockedApp: List<String>?,
    @SerializedName("allowedApp") val allowedApp: List<String>? = null,
    @SerializedName("appControlMode") val appControlMode: String? = "blocklist",
    @SerializedName("dailyUsageLog") val dailyUsageLog: Map<String, Int>?,
    @SerializedName("appTimeLimits") val appTimeLimits: Map<String, Int>? = null
)

data class InstalledApp(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String
)

data class WebsiteAssignmentItem(
    @SerializedName("assignment_id") val assignmentId: String,
    @SerializedName("policy_id") val policyId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("version") val version: Int,
    @SerializedName("status") val status: String
)

data class WebsiteAssignmentListResponse(
    @SerializedName("items") val items: List<WebsiteAssignmentItem> = emptyList()
)

data class WebsitePolicyRuleSnapshot(
    @SerializedName("normalized_pattern") val normalizedPattern: String,
    @SerializedName("match_mode") val matchMode: String,
    @SerializedName("action") val action: String,
    @SerializedName("category") val category: String?,
    @SerializedName("priority") val priority: Int,
    @SerializedName("schedule_id") val scheduleId: String?,
    @SerializedName("daily_budget_seconds") val dailyBudgetSeconds: Int?
)

data class WebsitePolicyAckRequest(
    @SerializedName("version") val version: Int,
    @SerializedName("content_hash") val contentHash: String,
    @SerializedName("agent_version") val agentVersion: String,
    @SerializedName("capabilities") val capabilities: Map<String, Boolean> = emptyMap()
)

data class WebsitePolicySyncResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("policy_id") val policyId: String,
    @SerializedName("version") val version: Int,
    @SerializedName("content_hash") val contentHash: String,
    @SerializedName("default_action") val defaultAction: String,
    @SerializedName("website_control_mode") val websiteControlMode: String = "blocklist",
    @SerializedName("mandatory_blocked_categories") val mandatoryBlockedCategories: List<String> = emptyList(),
    @SerializedName("timezone") val timezone: String,
    @SerializedName("rules") val rules: List<WebsitePolicyRuleSnapshot> = emptyList()
)

data class InstalledAppsRequest(
    @SerializedName("apps")
    val apps: List<InstalledApp>
)

data class AppUsageRequest(
    @SerializedName("child_id")
    val childId: String,
    @SerializedName("usage")
    val usage: Map<String, Long>
)

data class UpdateFcmTokenRequest(
    @SerializedName("fcm_token") val fcmToken: String
)



data class PhoneLocationUploadRequest(
    @SerializedName("report_id") val reportId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("accuracy_meters") val accuracyMeters: Float,
    @SerializedName("altitude_meters") val altitudeMeters: Double? = null,
    @SerializedName("speed_mps") val speedMps: Float? = null,
    @SerializedName("captured_at") val capturedAt: String
)

data class PhoneLocationUploadResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("report_id") val reportId: String,
    @SerializedName("source") val source: String,
    @SerializedName("captured_at") val capturedAt: String,
    @SerializedName("received_at") val receivedAt: String,
    @SerializedName("status") val status: String
)

data class PhoneLocationPolicyResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("service_status") val serviceStatus: String,
    @SerializedName("last_successful_upload_at") val lastSuccessfulUploadAt: String? = null,
    @SerializedName("latest_captured_at") val latestCapturedAt: String? = null,
    @SerializedName("policy_version") val policyVersion: Int? = null
)


data class EffectiveAppBlockingValues(
    @SerializedName("blockedApp") val blockedApp: List<String> = emptyList(),
    @SerializedName("allowedApp") val allowedApp: List<String> = emptyList(),
    @SerializedName("appControlMode") val appControlMode: String = "blocklist",
    @SerializedName("appTimeLimits") val appTimeLimits: Map<String, Int> = emptyMap(),
)

data class EffectiveAppBlockingPolicyResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("policy_version") val policyVersion: Int,
    @SerializedName("scope") val scope: String,
    @SerializedName("values") val values: EffectiveAppBlockingValues,
)

data class ScreenTimePolicyPayload(
    @SerializedName("policy_id") val policyId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("version") val version: Int,
    @SerializedName("daily_limit_seconds") val dailyLimitSeconds: Int,
)

data class ScreenTimePolicySyncPayload(
    @SerializedName("policy") val policy: ScreenTimePolicyPayload,
    @SerializedName("policy_hash") val policyHash: String,
)