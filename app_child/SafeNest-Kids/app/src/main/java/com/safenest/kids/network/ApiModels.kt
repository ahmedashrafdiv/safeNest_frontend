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
    @SerializedName("appTimeLimits") val appTimeLimits: Map<String, Map<String, Int>>? = null
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
    val usage: Map<String, Long>,
    @SerializedName("usage_day")
    val usageDay: String,
    @SerializedName("usage_timezone")
    val usageTimezone: String
)

data class DeviceProtectionHealthRequest(
    @SerializedName("permission_states") val permissionStates: Map<String, String> = emptyMap(),
    @SerializedName("capabilities") val capabilities: List<String>,
    @SerializedName("reported_at") val reportedAt: String,
    @SerializedName("protection_health") val protectionHealth: String,
    @SerializedName("protection_mode") val protectionMode: String,
    @SerializedName("management_authority_confirmed") val managementAuthorityConfirmed: Boolean,
    @SerializedName("uninstall_protection_confirmed") val uninstallProtectionConfirmed: Boolean,
    @SerializedName("lock_task_available") val lockTaskAvailable: Boolean,
)

data class DeviceProtectionHealthResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("reported_at") val reportedAt: String,
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
    @SerializedName("appTimeLimits") val appTimeLimits: Map<String, Map<String, Int>> = emptyMap(),
)

data class EffectiveAppBlockingPolicyResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("policy_version") val policyVersion: Int,
    @SerializedName("scope") val scope: String,
    @SerializedName("values") val values: EffectiveAppBlockingValues,
)

data class EffectiveProtectionPolicyValues(
    @SerializedName("protectedHomeRequested") val protectedHomeRequested: Boolean = false,
)

data class EffectiveProtectionPolicyResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("policy_version") val policyVersion: Int,
    @SerializedName("scope") val scope: String,
    @SerializedName("values") val values: EffectiveProtectionPolicyValues,
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

/** Display identity for the Home screen and the parent verification dialog. */
data class ChildDeviceSessionProfile(
    @SerializedName("child_id") val childId: String,
    @SerializedName("child_name") val childName: String?,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("parent_email") val parentEmail: String,
)

data class ParentVerificationRequest(
    @SerializedName("password") val password: String,
)

data class ParentVerificationResponse(
    @SerializedName("verified") val verified: Boolean = false,
)

/**
 * Result of `GET /api/child-devices/{device_id}/screen-time-decision`.
 *
 * The endpoint answers 404 `policy_not_found` when the parent has assigned no daily policy, so a
 * successful body always describes a real budget.
 */
data class ScreenTimeDecisionResponse(
    @SerializedName("decision") val decision: String?,
    @SerializedName("reason_code") val reasonCode: String?,
    @SerializedName("local_date") val localDate: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("used_seconds") val usedSeconds: Int = 0,
    @SerializedName("base_limit_seconds") val baseLimitSeconds: Int = 0,
    @SerializedName("extra_grant_seconds") val extraGrantSeconds: Int = 0,
    @SerializedName("effective_limit_seconds") val effectiveLimitSeconds: Int = 0,
    @SerializedName("remaining_seconds") val remainingSeconds: Int = 0,
)

/**
 * Fixed values for the extra-time request the Home screen submits.
 *
 * `scope_value` is the one that bites. The Backend accepts `daily`, `downtime`, and `bedtime` for a
 * `screen_time` scope, but only a grant whose scope is exactly `daily` is added to the daily budget
 * when the screen-time decision is evaluated. Anything else is accepted, shown to the parent, and
 * approved — and then silently ignored by the very budget it was meant to extend.
 */
object ExtraTimeRequest {
    const val REQUEST_TYPE = "extra_time"
    const val SCOPE_TYPE = "screen_time"
    const val SCOPE_VALUE = "daily"

    /** The Backend rejects anything outside this range with a 422. */
    const val MIN_REQUESTED_SECONDS = 60
    const val MAX_REQUESTED_SECONDS = 86400
}

data class AccessRequestCreateRequest(
    @SerializedName("payload_version") val payloadVersion: Int = 1,
    @SerializedName("request_type") val requestType: String,
    @SerializedName("scope_type") val scopeType: String,
    @SerializedName("scope_value") val scopeValue: String,
    @SerializedName("requested_seconds") val requestedSeconds: Int,
    @SerializedName("client_request_id") val clientRequestId: String,
    @SerializedName("reason") val reason: String? = null,
)

data class AccessRequestCreateResponse(
    @SerializedName("duplicate") val duplicate: Boolean = false,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("requested_seconds") val requestedSeconds: Int = 0,
    @SerializedName("request_expires_at") val requestExpiresAt: String? = null,
)
