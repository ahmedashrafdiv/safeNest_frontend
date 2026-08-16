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
    @SerializedName("blocked_app") val blockedApp: List<String> = emptyList()
)

data class DigitalRuleUpdateRequest(
    @SerializedName("max_screen_time") val maxScreenTime: Int? = null,
    @SerializedName("blocked_app") val blockedApp: List<String>? = null,
    @SerializedName("app_time_limits") val appTimeLimits: Map<String, Int>? = null
)

data class DigitalRuleResponse(
    @SerializedName("ruleID") val ruleId: String,
    @SerializedName("parentID") val parentId: String,
    @SerializedName("childID") val childId: String,
    @SerializedName("maxScreenTime") val maxScreenTime: Int?,
    @SerializedName("blockedApp") val blockedApp: List<String> = emptyList(),
    @SerializedName("appTimeLimits") val appTimeLimits: Map<String, Int> = emptyMap(),
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
