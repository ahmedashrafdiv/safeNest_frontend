package com.safenest.kids.util

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class PrefsHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SafeNestKidsPrefs", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var deviceId = prefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }

    fun setChildId(childId: String) {
        prefs.edit().putString("child_id", childId).apply()
    }

    fun getChildId(): String? = prefs.getString("child_id", null)

    fun setParentId(parentId: String) {
        prefs.edit().putString("parent_id", parentId).apply()
    }

    fun getParentId(): String? = prefs.getString("parent_id", null)

    fun setPaired(isPaired: Boolean) {
        prefs.edit().putBoolean("is_paired", isPaired).apply()
    }

    fun isPaired(): Boolean = prefs.getBoolean("is_paired", false)

    fun setBlockedApps(apps: Set<String>) {
        prefs.edit().putStringSet("blocked_apps", apps).apply()
    }

    fun getBlockedApps(): Set<String> = prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()

    fun setAllowedApps(apps: Set<String>) {
        prefs.edit().putStringSet("allowed_apps", apps).apply()
    }

    fun getAllowedApps(): Set<String> = prefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()

    fun setAppControlMode(mode: String) {
        prefs.edit().putString("app_control_mode", if (mode == "allowlist") "allowlist" else "blocklist").apply()
    }

    fun getAppControlMode(): String = prefs.getString("app_control_mode", "blocklist") ?: "blocklist"

    fun setAppPolicy(mode: String, allowedApps: Set<String>, blockedApps: Set<String>, limitsJson: String) {
        prefs.edit()
            .putString("app_control_mode", if (mode == "allowlist") "allowlist" else "blocklist")
            .putStringSet("allowed_apps", allowedApps)
            .putStringSet("blocked_apps", blockedApps)
            .putString("app_time_limits_json", limitsJson)
            .apply()
    }

    fun setLastAppsSent(sent: Boolean) {
        prefs.edit().putBoolean("last_apps_sent", sent).apply()
    }

    fun getLastAppsSent(): Boolean = prefs.getBoolean("last_apps_sent", false)

    fun setInstalledAppsFingerprint(fingerprint: String) {
        prefs.edit().putString("installed_apps_fingerprint", fingerprint).apply()
    }

    fun getInstalledAppsFingerprint(): String? =
        prefs.getString("installed_apps_fingerprint", null)

    fun setDeviceToken(token: String?) {
        prefs.edit().putString("device_access_token", token).apply()
    }

    fun getDeviceToken(): String? = prefs.getString("device_access_token", null)

    fun setJustPaired(justPaired: Boolean) {
        prefs.edit().putBoolean("just_paired", justPaired).apply()
    }

    fun isJustPaired(): Boolean = prefs.getBoolean("just_paired", false)

    // ── Per-app time limits ────────────────────────────────────

    fun setAppTimeLimits(limitsJson: String) {
        prefs.edit().putString("app_time_limits_json", limitsJson).apply()
    }

    fun getAppTimeLimitsJson(): String? = prefs.getString("app_time_limits_json", null)

    fun setWebsitePolicyId(policyId: String) {
        prefs.edit().putString("website_policy_id", policyId).apply()
    }

    fun getWebsitePolicyId(): String? = prefs.getString("website_policy_id", null)

    fun setWebsitePolicySnapshot(snapshotJson: String, version: Int, contentHash: String) {
        prefs.edit()
            .putString("website_policy_snapshot_json", snapshotJson)
            .putInt("website_policy_version", version)
            .putString("website_policy_content_hash", contentHash)
            .apply()
    }

    fun getWebsitePolicySnapshotJson(): String? = prefs.getString("website_policy_snapshot_json", null)
    fun getWebsitePolicyVersion(): Int = prefs.getInt("website_policy_version", 0)
    fun getWebsitePolicyContentHash(): String? = prefs.getString("website_policy_content_hash", null)

    fun setWebsiteVpnHealth(health: String) {
        prefs.edit().putString("website_vpn_health", health).apply()
    }

    fun getWebsiteVpnHealth(): String =
        prefs.getString("website_vpn_health", WEBSITE_VPN_UNAVAILABLE) ?: WEBSITE_VPN_UNAVAILABLE

    fun setPhoneTrackingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("phone_tracking_enabled", enabled).apply()
    }

    fun isPhoneTrackingEnabled(): Boolean = prefs.getBoolean("phone_tracking_enabled", true)

    fun setPhoneTrackingPermissionState(state: String) {
        prefs.edit().putString("phone_tracking_permission_state", state).apply()
    }

    fun getPhoneTrackingPermissionState(): String =
        prefs.getString("phone_tracking_permission_state", PHONE_LOCATION_PERMISSION_UNKNOWN)
            ?: PHONE_LOCATION_PERMISSION_UNKNOWN

    fun setPhoneTrackingServiceState(state: String) {
        prefs.edit().putString("phone_tracking_service_state", state).apply()
    }

    fun getPhoneTrackingServiceState(): String =
        prefs.getString("phone_tracking_service_state", PHONE_LOCATION_SERVICE_STOPPED)
            ?: PHONE_LOCATION_SERVICE_STOPPED

    fun setPhoneTrackingNetworkState(state: String) {
        prefs.edit().putString("phone_tracking_network_state", state).apply()
    }

    fun getPhoneTrackingNetworkState(): String =
        prefs.getString("phone_tracking_network_state", PHONE_LOCATION_NETWORK_UNKNOWN)
            ?: PHONE_LOCATION_NETWORK_UNKNOWN

    fun setPhoneTrackingLastUploadAt(value: String?) {
        prefs.edit().putString("phone_tracking_last_upload_at", value).apply()
    }

    fun getPhoneTrackingLastUploadAt(): String? =
        prefs.getString("phone_tracking_last_upload_at", null)

    fun setPhoneTrackingLastReportId(value: String?) {
        prefs.edit().putString("phone_tracking_last_report_id", value).apply()
    }

    fun getPhoneTrackingLastReportId(): String? =
        prefs.getString("phone_tracking_last_report_id", null)

    fun setPhoneTrackingLastCapturedAt(value: String?) {
        prefs.edit().putString("phone_tracking_last_captured_at", value).apply()
    }

    fun getPhoneTrackingLastCapturedAt(): String? =
        prefs.getString("phone_tracking_last_captured_at", null)

    fun setPhoneTrackingLastLatitude(value: Double) {
        prefs.edit().putString("phone_tracking_last_latitude", value.toString()).apply()
    }

    fun getPhoneTrackingLastLatitude(): Double? =
        prefs.getString("phone_tracking_last_latitude", null)?.toDoubleOrNull()

    fun setPhoneTrackingLastLongitude(value: Double) {
        prefs.edit().putString("phone_tracking_last_longitude", value.toString()).apply()
    }

    fun getPhoneTrackingLastLongitude(): Double? =
        prefs.getString("phone_tracking_last_longitude", null)?.toDoubleOrNull()

    fun setPhoneTrackingLastAccuracy(value: Float) {
        prefs.edit().putString("phone_tracking_last_accuracy", value.toString()).apply()
    }

    fun getPhoneTrackingLastAccuracy(): Float? =
        prefs.getString("phone_tracking_last_accuracy", null)?.toFloatOrNull()

    fun setPhoneTrackingPolicyVersion(value: Int) {
        prefs.edit().putInt("phone_tracking_policy_version", value).apply()
    }

    fun getPhoneTrackingPolicyVersion(): Int = prefs.getInt("phone_tracking_policy_version", 0)

    fun setPhoneTrackingStatus(status: String) {
        prefs.edit().putString("phone_tracking_status", status).apply()
    }

    fun getPhoneTrackingStatus(): String =
        prefs.getString("phone_tracking_status", PHONE_LOCATION_STATUS_UNAVAILABLE)
            ?: PHONE_LOCATION_STATUS_UNAVAILABLE

    companion object {
        const val WEBSITE_VPN_ACTIVE = "active"
        const val WEBSITE_VPN_UNAVAILABLE = "unavailable"
        const val WEBSITE_VPN_DENIED = "permission_denied"
        const val WEBSITE_VPN_FAILED = "failed"

        const val PHONE_LOCATION_PERMISSION_UNKNOWN = "unknown"
        const val PHONE_LOCATION_PERMISSION_GRANTED = "granted"
        const val PHONE_LOCATION_PERMISSION_DENIED = "denied"
        const val PHONE_LOCATION_SERVICE_ACTIVE = "active"
        const val PHONE_LOCATION_SERVICE_STARTING = "starting"
        const val PHONE_LOCATION_SERVICE_STOPPED = "stopped"
        const val PHONE_LOCATION_SERVICE_FAILED = "failed"
        const val PHONE_LOCATION_NETWORK_UNKNOWN = "unknown"
        const val PHONE_LOCATION_NETWORK_ONLINE = "online"
        const val PHONE_LOCATION_NETWORK_RETRYING = "retrying"
        const val PHONE_LOCATION_NETWORK_OFFLINE = "offline"
        const val PHONE_LOCATION_STATUS_ACTIVE = "active"
        const val PHONE_LOCATION_STATUS_PERMISSION_DENIED = "permission_denied"
        const val PHONE_LOCATION_STATUS_SERVICE_STOPPED = "service_stopped"
        const val PHONE_LOCATION_STATUS_OFFLINE = "offline"
        const val PHONE_LOCATION_STATUS_STALE = "stale"
        const val PHONE_LOCATION_STATUS_DISABLED = "disabled"
        const val PHONE_LOCATION_STATUS_UNAVAILABLE = "unavailable"
    }


    fun setAppPolicy(mode: String, allowedApps: Set<String>, blockedApps: Set<String>, limitsJson: String, policyVersion: Int) {
        prefs.edit()
            .putString("app_control_mode", if (mode == "allowlist") "allowlist" else "blocklist")
            .putStringSet("allowed_apps", allowedApps)
            .putStringSet("blocked_apps", blockedApps)
            .putString("app_time_limits_json", limitsJson)
            .putInt("app_policy_version", policyVersion)
            .apply()
    }

    fun getAppPolicyVersion(): Int = prefs.getInt("app_policy_version", 0)

    fun setLastAppPolicyRefreshEnqueueAt(value: Long) {
        prefs.edit().putLong("last_app_policy_refresh_enqueue_at", value).apply()
    }

    fun getLastAppPolicyRefreshEnqueueAt(): Long =
        prefs.getLong("last_app_policy_refresh_enqueue_at", 0L)

    fun setProtectedHomePolicy(requested: Boolean, policyVersion: Int) {
        prefs.edit()
            .putBoolean("protected_home_requested", requested)
            .putInt("protected_home_policy_version", policyVersion)
            .apply()
    }

    fun isProtectedHomeRequested(): Boolean = prefs.getBoolean("protected_home_requested", false)
    fun getProtectedHomePolicyVersion(): Int = prefs.getInt("protected_home_policy_version", 0)

    fun setDailyScreenTimePolicy(dailyLimitSeconds: Int, policyVersion: Int) {
        prefs.edit()
            .putInt("daily_screen_time_limit_seconds", dailyLimitSeconds)
            .putInt("daily_screen_time_policy_version", policyVersion)
            .apply()
    }

    fun getDailyScreenTimeLimitSeconds(): Int = prefs.getInt("daily_screen_time_limit_seconds", 0)

    fun getDailyScreenTimePolicyVersion(): Int = prefs.getInt("daily_screen_time_policy_version", 0)}
