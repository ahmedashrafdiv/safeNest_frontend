package com.example.safenest.network

import com.google.gson.annotations.SerializedName

data class ScreenTimePolicyCreateRequest(
    @SerializedName("device_id") val deviceId: String? = null,
    @SerializedName("daily_limit_seconds") val dailyLimitSeconds: Int,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("safe_default_action") val safeDefaultAction: String,
    @SerializedName("effective_from") val effectiveFrom: String,
)
