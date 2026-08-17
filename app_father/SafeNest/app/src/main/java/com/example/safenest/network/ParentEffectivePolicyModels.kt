package com.example.safenest.network

import com.google.gson.annotations.SerializedName

data class EffectiveAppBlockingPolicyResponse(
    @SerializedName("child_id") val childId: String,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("policy_family") val policyFamily: String,
    @SerializedName("scope") val scope: String,
    @SerializedName("inherited") val inherited: Boolean,
    @SerializedName("values") val values: Map<String, Any?>,
    @SerializedName("source_fields") val sourceFields: Map<String, String> = emptyMap(),
    @SerializedName("version") val version: Int,
)
