package com.example.safenest.network

import com.google.gson.annotations.SerializedName

// ============ Auth Request Models ============

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("phoneNumber") val phoneNumber: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class EmailVerificationRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String
)

data class ResendOtpRequest(
    @SerializedName("email") val email: String
)

data class ForgotPasswordRequest(
    @SerializedName("email") val email: String
)

data class ResetPasswordRequest(
    @SerializedName("email") val email: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("new_password") val newPassword: String
)

// ============ Auth Response Models ============

data class MessageResponse(
    @SerializedName("message") val message: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String = "bearer",
    @SerializedName("parent_id") val parentId: String
)

// ============ Parent Models ============

data class ParentResponse(
    @SerializedName("parentID") val parentId: String,
    @SerializedName("name") val name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phoneNumber") val phoneNumber: String?,
    @SerializedName("verified") val verified: Boolean,
    @SerializedName("createdAt") val createdAt: String
)

data class ParentUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("phoneNumber") val phoneNumber: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class FCMTokenUpdateRequest(
    @SerializedName("fcm_token") val fcmToken: String
)

// ============ Child Models ============

data class ChildCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String,
    @SerializedName("deviceID") val deviceId: String? = null
)

data class ChildUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("dateOfBirth") val dateOfBirth: String? = null,
    @SerializedName("deviceID") val deviceId: String? = null
)

data class ChildResponse(
    @SerializedName("childID") val childId: String,
    @SerializedName("parentID") val parentId: String,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("dateOfBirth") val dateOfBirth: String,
    @SerializedName("deviceID") val deviceId: String?
)

data class AllowedAppItem(
    @SerializedName("name") val name: String,
    @SerializedName("time_limit_minutes") var timeLimitMinutes: Int
)

data class InstalledAppItem(
    @SerializedName("package_name") val packageName: String,
    @SerializedName("app_name") val appName: String
)

data class InstalledAppsUpdateRequest(
    @SerializedName("apps") val apps: List<InstalledAppItem>
)

data class InstalledAppsResponse(
    @SerializedName("apps") val apps: List<InstalledAppItem> = emptyList()
)

// ============ Digital Control Models ============

data class DigitalRuleCreateRequest(
    @SerializedName("child_id") val childId: String,
    @SerializedName("max_screen_time") val maxScreenTime: Int? = null,
    @SerializedName("blocked_app") val blockedApp: List<String> = emptyList(),
    @SerializedName("allowed_app") val allowedApp: List<String> = emptyList(),
    @SerializedName("app_control_mode") val appControlMode: String = "blocklist"
)

data class DigitalRuleUpdateRequest(
    @SerializedName("max_screen_time") val maxScreenTime: Int? = null,
    @SerializedName("blocked_app") val blockedApp: List<String>? = null,
    @SerializedName("allowed_app") val allowedApp: List<String>? = null,
    @SerializedName("app_time_limits") val appTimeLimits: Map<String, Int>? = null,
    @SerializedName("app_control_mode") val appControlMode: String? = null
)

data class DigitalRuleResponse(
    @SerializedName("ruleID") val ruleId: String,
    @SerializedName("parentID") val parentId: String,
    @SerializedName("childID") val childId: String,
    @SerializedName("maxScreenTime") val maxScreenTime: Int?,
    @SerializedName("dailyLimitMinutes") val dailyLimitMinutes: Int? = null,
    @SerializedName("usedTodayMinutes") val usedTodayMinutes: Int? = null,
    @SerializedName("remainingTodayMinutes") val remainingTodayMinutes: Int? = null,
    @SerializedName("usageDate") val usageDate: String? = null,
    @SerializedName("usageTimezone") val usageTimezone: String? = null,
    @SerializedName("usageUpdatedAt") val usageUpdatedAt: String? = null,
    @SerializedName("limitConfirmationRequired") val limitConfirmationRequired: Boolean = false,
    @SerializedName("blockedApp") val blockedApp: List<String> = emptyList(),
    @SerializedName("allowedApp") val allowedApp: List<String> = emptyList(),
    @SerializedName("appTimeLimits") val appTimeLimits: Map<String, Int> = emptyMap(),
    @SerializedName("appControlMode") val appControlMode: String = "blocklist",
    @SerializedName("dailyUsageLog") val dailyUsageLog: Map<String, Int> = emptyMap(),
    @SerializedName("videoHistory") val videoHistory: List<Map<String, Any>> = emptyList()
)

data class AppUsageUpdateRequest(
    @SerializedName("child_id") val childId: String,
    @SerializedName("usage") val usage: Map<String, Int>
)

data class VideoHistoryCreateRequest(
    @SerializedName("child_id") val childId: String,
    @SerializedName("videos") val videos: List<Map<String, String>>
)

// ============ Device Models ============

data class DevicePairingRequest(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("device_type") val deviceType: String  // "Gas" or "Motion"
)

data class DeviceOut(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("last_active") val lastActive: String?
)

data class DeviceStatusResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("device_type") val deviceType: String,
    @SerializedName("last_active") val lastActive: String?,
    @SerializedName("current_value") val currentValue: Int = 0,
    @SerializedName("batteryLevel") val batteryLevel: Int = 100
)

data class GeneratePinRequest(
    @SerializedName("child_id") val childId: String
)

data class GeneratePinResponse(
    @SerializedName("pin_code") val pin: String,
    @SerializedName("expires_at") val expiresAt: String
)

// ============ GPS Models ============
// GPS endpoints now use child_id directly — no custom models needed.

// ============ Zone Models ============

data class ZoneCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("zone_type") val zoneType: String,  // "Safe" or "Danger"
    @SerializedName("child_id") val childId: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius_meters") val radiusMeters: Int
)

data class ZoneUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("zone_type") val zoneType: String? = null,
    @SerializedName("child_id") val childId: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
    @SerializedName("radius_meters") val radiusMeters: Int? = null
)

data class ZoneResponse(
    @SerializedName("zone_id") val zoneId: String,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("name") val name: String,
    @SerializedName("zone_type") val zoneType: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("radius_meters") val radiusMeters: Int
)

// ============ Website Policy Models ============

data class WebsitePolicyCreateRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("default_action") val defaultAction: String = "allow_with_logging",
    @SerializedName("timezone") val timezone: String = "UTC",
    @SerializedName("age_profile") val ageProfile: String? = null,
    @SerializedName("website_control_mode") val websiteControlMode: String = "blocklist",
    @SerializedName("mandatory_blocked_categories") val mandatoryBlockedCategories: List<String> = emptyList()
)

data class WebsitePolicyUpdateRequest(
    @SerializedName("name") val name: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("default_action") val defaultAction: String? = null,
    @SerializedName("timezone") val timezone: String? = null,
    @SerializedName("age_profile") val ageProfile: String? = null,
    @SerializedName("website_control_mode") val websiteControlMode: String? = null,
    @SerializedName("mandatory_blocked_categories") val mandatoryBlockedCategories: List<String>? = null
)

data class WebsitePolicyResponse(
    @SerializedName("policy_id") val policyId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("default_action") val defaultAction: String,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("website_control_mode") val websiteControlMode: String = "blocklist",
    @SerializedName("mandatory_blocked_categories") val mandatoryBlockedCategories: List<String> = emptyList(),
    @SerializedName("status") val status: String,
    @SerializedName("current_version") val currentVersion: Int = 0,
    @SerializedName("published_version") val publishedVersion: Int? = null
)

data class WebsitePolicyListResponse(
    @SerializedName("items") val items: List<WebsitePolicyResponse> = emptyList()
)

data class WebsiteRuleCreateRequest(
    @SerializedName("scope") val scope: String,
    @SerializedName("pattern") val pattern: String,
    @SerializedName("match_mode") val matchMode: String,
    @SerializedName("action") val action: String,
    @SerializedName("category") val category: String? = null,
    @SerializedName("priority") val priority: Int = 500,
    @SerializedName("enabled") val enabled: Boolean = true,
    @SerializedName("schedule_id") val scheduleId: String? = null,
    @SerializedName("daily_budget_seconds") val dailyBudgetSeconds: Int? = null
)

data class WebsiteRuleResponse(
    @SerializedName("rule_id") val ruleId: String,
    @SerializedName("normalized_pattern") val normalizedPattern: String,
    @SerializedName("scope") val scope: String,
    @SerializedName("match_mode") val matchMode: String,
    @SerializedName("action") val action: String,
    @SerializedName("category") val category: String?,
    @SerializedName("priority") val priority: Int,
    @SerializedName("daily_budget_seconds") val dailyBudgetSeconds: Int? = null
)

data class WebsiteRuleListResponse(
    @SerializedName("items") val items: List<WebsiteRuleResponse> = emptyList()
)

data class WebsitePublishResponse(
    @SerializedName("policy_id") val policyId: String,
    @SerializedName("version") val version: Int,
    @SerializedName("content_hash") val contentHash: String,
    @SerializedName("assigned_device_count") val assignedDeviceCount: Int
)

data class WebsiteAssignmentRequest(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String
)

// ============ Alert Models ============

data class AlertOut(
    @SerializedName("alertID") val alertId: String,
    @SerializedName("parent_id") val parentId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("device_name") val deviceName: String,
    @SerializedName("alertType") val alertType: String,
    @SerializedName("message") val message: String,
    @SerializedName("isResolved") val isResolved: Boolean,
    @SerializedName("timestamp") val timestamp: String?
)

data class AlertUpdateRequest(
    @SerializedName("isResolved") val isResolved: Boolean
)

// ============ Child Access Request Models ============

enum class AccessRequestType {
    EXTRA_TIME, ACCESS_OVERRIDE
}

data class AccessRequestItem(
    @SerializedName("request_id") val requestId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("request_type") val requestType: String,
    @SerializedName("scope_type") val scopeType: String,
    @SerializedName("scope_value") val scopeValue: String,
    @SerializedName("requested_seconds") val requestedSeconds: Int,
    @SerializedName("reason") val reason: String?,
    @SerializedName("status") val status: String,
    @SerializedName("requested_at") val requestedAt: String,
    @SerializedName("request_expires_at") val requestExpiresAt: String?,
    @SerializedName("child_name") val childName: String? = null
)

data class AccessRequestListResponse(
    @SerializedName("items") val items: List<AccessRequestItem> = emptyList(),
    @SerializedName("next_cursor") val nextCursor: String? = null
)

data class AccessRequestApproveRequest(
    @SerializedName("granted_seconds") val grantedSeconds: Int? = null,
    @SerializedName("decision_reason") val decisionReason: String? = null
)

data class AccessRequestRejectRequest(
    @SerializedName("decision_reason") val decisionReason: String? = null
)

// ============ Error Models ============

data class ErrorResponse(
    @SerializedName("detail") val detail: Any?  // Can be String or List
) {
    fun getMessage(): String {
        return when (detail) {
            is String -> detail
            is List<*> -> (detail as? List<*>)?.firstOrNull()?.toString() ?: "Unknown error"
            else -> "Unknown error occurred"
        }
    }
}


data class ParentLocationCoordinate(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
)

data class ParentLocationEnvelope(
    @SerializedName("availability_status") val availabilityStatus: String = "unavailable",
    @SerializedName("source") val source: String? = null,
    @SerializedName("coordinate") val coordinate: ParentLocationCoordinate? = null,
    @SerializedName("accuracy_meters") val accuracyMeters: Double? = null,
    @SerializedName("captured_at") val capturedAt: String? = null,
    @SerializedName("received_at") val receivedAt: String? = null,
    @SerializedName("age_seconds") val ageSeconds: Long? = null,
    @SerializedName("is_stale") val isStale: Boolean = false,
    @SerializedName("fallback_source") val fallbackSource: String? = null,
    @SerializedName("message_code") val messageCode: String? = null,
    @SerializedName("location_name") val locationName: String? = null,
    @SerializedName("gps_active") val gpsActive: Boolean? = null,
    @SerializedName("tracking_active") val trackingActive: Boolean? = null,
    @SerializedName("last_update") val legacyLastUpdate: String? = null
) {
    fun effectiveCoordinate(): ParentLocationCoordinate? = coordinate

    fun effectiveSource(): String = when {
        source != null -> source
        gpsActive == true -> "external_gps"
        coordinate != null -> "external_gps"
        else -> "unknown"
    }

    fun effectiveAgeSeconds(): Long? = ageSeconds ?: 0L
}


data class PhoneTrackingUpdateRequest(
    @SerializedName("enabled") val enabled: Boolean
)

data class PhoneTrackingPolicyResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("enabled") val enabled: Boolean,
    @SerializedName("service_status") val serviceStatus: String,
    @SerializedName("policy_version") val policyVersion: Int? = null
)
