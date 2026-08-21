package com.safenest.kids.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safenest.kids.util.PlacementNodeClassifier
import com.safenest.kids.util.PlacementNodeDescriptor
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.view.BlurOverlayView
import java.util.concurrent.Executors

/**
 * Dedicated Content Blur service. It never changes AppBlockerAccessibilityService state.
 * Until a licensed classifier is available, successful captures still remain conservative.
 */
class ContentBlurAccessibilityService : AccessibilityService() {
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var overlayController: BlurOverlayController
    private lateinit var scopeDecider: BlurScopeDecider
    private lateinit var tracker: RegionTracker
    private lateinit var planBuilder: BlurPlanBuilder
    private lateinit var scrollGate: ScrollStateGate
    private lateinit var captureScheduler: CaptureScheduler

    private var latestCandidates: List<CandidateRegion> = emptyList()
    private var enabled = false
    private var activePolicyVersion = -1
    private val mainHandler = Handler(Looper.getMainLooper())
    private val policyPreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in CONTENT_BLUR_POLICY_KEYS) mainHandler.post(::refreshPolicyFromPrefs)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayController = BlurOverlayController(this)
        val config = BlurConfig(targetPackages = emptySet())
        scopeDecider = BlurScopeDecider(config)
        tracker = RegionTracker()
        planBuilder = BlurPlanBuilder(config)
        scrollGate = ScrollStateGate(maxBlindMillis = config.maxBlindMillis)
        captureScheduler = CaptureScheduler()

        // Preserve the capabilities granted by the XML instead of replacing serviceInfo.
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            notificationTimeout = 50
        }
        refreshPolicyFromPrefs()
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(policyPreferenceListener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        refreshPolicyFromPrefs()
        if (!enabled || !::scopeDecider.isInitialized) return
        val packageName = event.packageName?.toString() ?: return
        if (!scopeDecider.isTargetPackage(packageName)) return

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            scrollGate.onScroll(System.currentTimeMillis())
        }

        val collected = collectCandidates(rootInActiveWindow, packageName)
        latestCandidates = collected.candidates
        val tracked = tracker.update(collected.candidates)
        if (collected.requiresWholeWindowCoverage) {
            overlayController.render(
                BlurPlan.conservative(
                    regions = listOf(collected.windowBounds),
                    reason = collected.coverageReason,
                ),
            )
            return
        }
        renderConservative(tracked, "candidate_refresh")

        val now = System.currentTimeMillis()
        if (scrollGate.shouldClassify(now)) {
            scrollGate.consumeSettled()
            requestCapture(now)
        }
    }

    override fun onInterrupt() {
        overlayController.detach()
        tracker.clear()
        latestCandidates = emptyList()
        scrollGate.reset()
    }

    override fun onDestroy() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(policyPreferenceListener)
        overlayController.detach()
        worker.shutdownNow()
        super.onDestroy()
    }

    fun setEnabledForPolicy(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) {
            overlayController.detach()
            tracker.clear()
            scrollGate.reset()
            updatePackageScope(emptySet())
        } else {
            activePolicyVersion = -1
            refreshPolicyFromPrefs()
        }
    }

    private fun updatePackageScope(targets: Set<String>) {
        serviceInfo = serviceInfo.apply {
            packageNames = (targets + packageName).toTypedArray()
        }
    }

    private fun refreshPolicyFromPrefs() {
        if (!::scopeDecider.isInitialized) return
        val prefs = PrefsHelper(this)
        val version = prefs.getContentBlurPolicyVersion()
        if (version == activePolicyVersion) return
        val targets = if (prefs.isContentBlurEnabled()) {
            BlurScopeDecider(BlurConfig(targetPackages = prefs.getContentBlurTargetPackages()))
        } else {
            BlurScopeDecider(BlurConfig(targetPackages = emptySet()))
        }
        scopeDecider = targets
        enabled = prefs.isContentBlurEnabled()
        activePolicyVersion = version
        updatePackageScope(if (enabled) prefs.getContentBlurTargetPackages() else emptySet())
        if (!enabled) {
            overlayController.detach()
            tracker.clear()
            scrollGate.reset()
        }
    }

    private fun renderConservative(regions: List<TrackedRegion>, reason: String) {
        overlayController.render(planBuilder.buildConservative(regions, reason))
    }

    private fun requestCapture(nowMillis: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val request = captureScheduler.request(nowMillis) ?: return
        val policyVersionAtRequest = activePolicyVersion
        takeScreenshot(Display.DEFAULT_DISPLAY, worker, object : TakeScreenshotCallback {
            override fun onSuccess(screenshot: ScreenshotResult) {
                try {
                    // No licensed classifier is shipped in this phase: success is not SAFE.
                    mainHandler.post {
                        if (enabled && activePolicyVersion == policyVersionAtRequest) {
                            renderConservative(tracker.values(), "classifier_unavailable")
                        }
                    }
                } finally {
                    screenshot.hardwareBuffer.close()
                    captureScheduler.complete()
                }
            }

            override fun onFailure(errorCode: Int) {
                captureScheduler.fail()
                mainHandler.post {
                    if (enabled && activePolicyVersion == policyVersionAtRequest) {
                        renderConservative(tracker.values(), "screenshot_failure_$errorCode")
                    }
                }
            }
        })
        request.requestId
    }

    private fun collectCandidates(root: AccessibilityNodeInfo?, packageName: String): CandidateCollection {
        val windowBounds = root?.let(::windowBoundsFor) ?: screenBounds()
        if (root == null) {
            return CandidateCollection(
                candidates = emptyList(),
                requiresWholeWindowCoverage = true,
                coverageReason = "active_window_unavailable",
                windowBounds = windowBounds,
            )
        }
        val deadline = System.nanoTime() + NODE_WALK_BUDGET_NANOS
        val result = mutableListOf<CandidateRegion>()
        walk(root, packageName, result, deadline, 0)
        return CandidateCollection(
            candidates = result,
            requiresWholeWindowCoverage = System.nanoTime() > deadline,
            coverageReason = "node_walk_budget_exceeded",
            windowBounds = windowBounds,
        )
    }

    private fun windowBoundsFor(root: AccessibilityNodeInfo): RegionBounds {
        val bounds = Rect()
        root.getBoundsInScreen(bounds)
        return if (bounds.width() > 0 && bounds.height() > 0) {
            RegionBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        } else {
            screenBounds()
        }
    }

    private fun screenBounds(): RegionBounds = RegionBounds(
        left = 0,
        top = 0,
        right = resources.displayMetrics.widthPixels,
        bottom = resources.displayMetrics.heightPixels,
    )

    private fun walk(
        node: AccessibilityNodeInfo,
        packageName: String,
        result: MutableList<CandidateRegion>,
        deadline: Long,
        childIndex: Int,
    ) {
        if (System.nanoTime() > deadline) return
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        PlacementNodeClassifier.classify(
            PlacementNodeDescriptor(
                packageName = packageName,
                className = node.className?.toString(),
                contentDescription = node.contentDescription?.toString(),
                viewId = node.viewIdResourceName,
                bounds = RegionBounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                isVisible = node.isVisibleToUser,
                childIndex = childIndex,
            ),
        )?.let(result::add)

        for (index in 0 until node.childCount) {
            if (System.nanoTime() > deadline) return
            val child = node.getChild(index) ?: continue
            try {
                walk(child, packageName, result, deadline, index)
            } finally {
                child.recycle()
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "SafeNestKidsPrefs"
        private const val NODE_WALK_BUDGET_NANOS = 8_000_000L
        private val CONTENT_BLUR_POLICY_KEYS = setOf(
            "content_blur_enabled",
            "content_blur_mode",
            "content_blur_target_packages",
            "content_blur_policy_version",
        )
        val DEFAULT_TARGET_PACKAGES = setOf(
            "com.google.android.youtube",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.google.android.apps.photos",
        )
    }

    private data class CandidateCollection(
        val candidates: List<CandidateRegion>,
        val requiresWholeWindowCoverage: Boolean,
        val coverageReason: String,
        val windowBounds: RegionBounds,
    )
}
