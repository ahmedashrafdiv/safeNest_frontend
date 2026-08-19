package com.safenest.kids.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safenest.kids.BlockedAppActivity
import com.safenest.kids.util.AppUsageHelper
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.security.UninstallAttemptClassifier
import org.json.JSONObject

class AppBlockerAccessibilityService : AccessibilityService() {

    private lateinit var prefsHelper: PrefsHelper

    // Style note: Layngo protection is explicit and narrow; repeated accessibility
    // events must not reopen the same block screen continuously.
    private val blockEventDeduplicator = BlockEventDeduplicator()

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
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                         AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }

        // Child controls may be unavailable to the child after pairing, so sync
        // here as well as from HomeFragment. This only schedules unique work;
        // it never performs a network call on the accessibility callback.
        RuleSyncWorker.enqueuePeriodic(this)
        RuleSyncWorker.enqueueImmediate(this)
        PhoneLocationPolicySyncWorker.enqueuePeriodic(this)
        PhoneLocationPolicySyncWorker.enqueueImmediate(this)
        Log.d(TAG, "Service connected — listening for window events")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val visibleText = buildString {
            event.text?.forEach { append(it).append(' ') }
            event.contentDescription?.let { append(it) }
            append(extractNodeText(event.source))
            append(extractNodeText(rootInActiveWindow))
        }
        if (UninstallAttemptClassifier.isLayngoRemovalAttempt(pkg, visibleText, packageName)) {
            Log.w(TAG, "LAYNGO_REMOVAL_ATTEMPT_DETECTED source=$pkg")
            blockPackage(packageName, "uninstall_protection", event.eventType)
            return
        }

        // Content changes are processed only for the explicit uninstall classifier above.
        if (!AccessibilityEventDecider.isForegroundWindowEvent(event.eventType)) return
        Log.e(TAG, "EVENT pkg=$pkg type=${event.eventType} — service alive")

        // Never block ourselves
        if (pkg == packageName) return

        enqueueBoundPolicyRefreshIfDue()

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

    private fun blockPackage(
        pkg: String,
        reason: String,
        eventType: Int = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
    ) {
        if (
            !blockEventDeduplicator.shouldLaunch(
                packageName = pkg,
                reason = reason,
                isWindowStateChange = AccessibilityEventDecider.isForegroundWindowEvent(eventType),
                nowMillis = System.currentTimeMillis(),
            )
        ) {
            return
        }
        Log.d(TAG, "Blocking foreground app: $pkg, reason=$reason")
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("blocked_package", pkg)
            putExtra("blocked_reason", reason)
        }
        startActivity(intent)
    }

    private fun enqueueBoundPolicyRefreshIfDue() {
        val nowMillis = System.currentTimeMillis()
        if (!AppPolicyRefreshDecider.shouldEnqueue(prefsHelper.getLastAppPolicyRefreshEnqueueAt(), nowMillis)) {
            return
        }
        prefsHelper.setLastAppPolicyRefreshEnqueueAt(nowMillis)
        RuleSyncWorker.enqueueImmediate(this)
        Log.d(TAG, "Enqueued a debounced bound app-policy refresh from a foreground transition")
    }

    private fun extractNodeText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""

        val text = StringBuilder()
        var remainingNodes = 96

        fun visit(current: AccessibilityNodeInfo) {
            if (remainingNodes-- <= 0) return
            current.text?.let { text.append(it).append(' ') }
            current.contentDescription?.let { text.append(it).append(' ') }
            for (index in 0 until current.childCount) {
                current.getChild(index)?.let { child ->
                    try {
                        visit(child)
                    } finally {
                        child.recycle()
                    }
                }
            }
        }

        visit(node)
        return text.toString()
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
