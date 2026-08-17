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

    fun getAppTimeLimitsJson(): String? =
        prefs.getString("app_time_limits_json", null)
}
