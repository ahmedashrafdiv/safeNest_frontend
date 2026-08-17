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

