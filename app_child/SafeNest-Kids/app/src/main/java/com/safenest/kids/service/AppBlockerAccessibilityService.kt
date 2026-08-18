package com.safenest.kids.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.safenest.kids.BlockedAppActivity
import com.safenest.kids.util.AppUsageHelper
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.security.UninstallAttemptClassifier
import org.json.JSONObject

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var prefsHelper: PrefsHelper

    // Rate-limiting: prevent rapid-fire blocking of the same app
    private var lastBlockedPkg = ""
    private var lastBlockedTime = 0L

    companion object {
        private const val TAG = "AppBlocker"

        /** Well-known launcher / system-UI tokens — never block these. */
        private val LAUNCHER_TOKENS = listOf(
            "launcher", "nexus", "pixel", "trebuchet",
            "systemui", "android"
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "=== onCreate CALLED === Service class: ${this.javaClass.name}")
    }

    override fun onServiceConnected() {
        Log.e(TAG, "=== onServiceConnected CALLED ===")
        super.onServiceConnected()
        prefsHelper = PrefsHelper(this)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }

        Log.d(TAG, "Service connected — listening for window events")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val visibleText = buildString {
            event.text?.forEach { append(it).append(' ') }
            event.contentDescription?.let { append(it) }
        }
        if (UninstallAttemptClassifier.isLayngoUninstallAttempt(pkg, visibleText, packageName)) {
            Log.w(TAG, "UNINSTALL_ATTEMPT_DETECTED source=$pkg")
            blockPackage(packageName, "uninstall_protection")
            return
        }

        // Content changes are processed only for the explicit uninstall classifier above.
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        Log.e(TAG, "EVENT pkg=$pkg type=${event.eventType} — service alive")

        // Never block ourselves
        if (pkg == packageName) return

        // Local-only read — no network call
        val blockedApps = prefsHelper.getBlockedApps()
        val allowedApps = prefsHelper.getAllowedApps()
        val appControlMode = prefsHelper.getAppControlMode()
        val shouldBlock = AppPolicyDecider.shouldBlock(
            pkg,
            packageName,
            appControlMode,
            allowedApps,
            blockedApps,
        )
        Log.i(
            TAG,
            "POLICY_DECISION pkg=$pkg mode=$appControlMode blocked=${blockedApps.contains(pkg)} allowed=${allowedApps.contains(pkg)} decision=${if (shouldBlock) "BLOCK" else "ALLOW"}",
        )

        if (shouldBlock) {
            blockPackage(pkg, if (appControlMode == "allowlist") "allowlist" else "blocked")
            return
        }

        // ── Per-app time-limit enforcement ─────────────────────────
        if (AppPolicyDecider.isProtectedPackage(pkg, packageName)) return

        if (isAppOverTimeLimit(pkg, this)) {
            Log.d(TAG, "Time limit exceeded for: $pkg")
            blockPackage(pkg, "time_limit")
            return
        }
        if (isDailyScreenTimeLimitReached(this)) {
            Log.d(TAG, "Daily Screen Time limit exceeded")
            blockPackage(pkg, "daily_screen_time_limit")
            return
        }
    }

    private fun blockPackage(pkg: String, reason: String) {
        val now = System.currentTimeMillis()
        if (pkg == lastBlockedPkg && now - lastBlockedTime < 2000L) return
        lastBlockedPkg = pkg
        lastBlockedTime = now
        Log.d(TAG, "Blocking foreground app: $pkg, reason=$reason")
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("blocked_package", pkg)
            putExtra("blocked_reason", reason)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onDestroy() {
        Log.e(TAG, "=== SERVICE DESTROYED ===")
        super.onDestroy()
    }

    // ── Helper: per-app time-limit check ───────────────────────
    fun isAppOverTimeLimit(pkg: String, context: Context): Boolean {
        val limitsJson = PrefsHelper(context).getAppTimeLimitsJson()
            ?: return false

        val limits = try {
            val obj = JSONObject(limitsJson)
            val map = mutableMapOf<String, Int>()
            obj.keys().forEach { key -> map[key] = obj.getInt(key) }
            map
        } catch (e: Exception) {
            return false
        }

        val limitMinutes = limits[pkg] ?: return false

        val usageMap = AppUsageHelper.getTodayUsageStats(context)
        val usedMinutes = usageMap[pkg] ?: 0L

        return usedMinutes >= limitMinutes
    }

    fun isDailyScreenTimeLimitReached(context: Context): Boolean {
        val limitSeconds = PrefsHelper(context).getDailyScreenTimeLimitSeconds()
        if (limitSeconds <= 0) return false
        val usedSeconds = AppUsageHelper.getTodayUsageStats(context).values.sum() * 60L
        return usedSeconds >= limitSeconds
    }}
