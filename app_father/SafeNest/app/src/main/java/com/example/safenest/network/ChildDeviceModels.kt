package com.example.safenest.network

import com.google.gson.annotations.SerializedName

/** Device data is always explicitly scoped to one child. */
data class ChildDeviceSummary(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("platform") val platform: String,
    @SerializedName("model") val model: String,
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("status") val status: String,
    @SerializedName("trust_state") val trustState: String,
    @SerializedName("last_seen_at") val lastSeenAt: String? = null,
    @SerializedName("last_policy_version") val lastPolicyVersion: Int? = null,
    @SerializedName("revoked_at") val revokedAt: String? = null,
)

data class ChildDevicePairingRequest(
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int = 300,
    @SerializedName("max_attempts") val maxAttempts: Int = 3,
)

data class ChildDevicePairingResponse(
    @SerializedName("pairing_id") val pairingId: String,
    @SerializedName("child_id") val childId: String,
    @SerializedName("pairing_code") val pairingCode: String,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("status") val status: String,
    @SerializedName("attempts_remaining") val attemptsRemaining: Int,
)

data class ChildDeviceRevokeRequest(
    @SerializedName("reason_code") val reasonCode: String,
)

data class ChildDeviceAuditResult(
    @SerializedName("audit_id") val auditId: String,
    @SerializedName("action") val action: String,
    @SerializedName("result") val result: String,
    @SerializedName("reason_code") val reasonCode: String? = null,
    @SerializedName("occurred_at") val occurredAt: String? = null,
)
