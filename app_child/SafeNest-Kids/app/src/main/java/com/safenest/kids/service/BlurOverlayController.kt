package com.safenest.kids.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import com.safenest.kids.view.BlurOverlayView

class BlurOverlayController(
    private val service: AccessibilityService,
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var view: BlurOverlayView? = null

    fun render(plan: BlurPlan) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { renderOnMain(plan) }
            return
        }
        renderOnMain(plan)
    }

    fun detach() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(::detachOnMain)
            return
        }
        detachOnMain()
    }

    private fun renderOnMain(plan: BlurPlan) {
        if (plan.covered.isEmpty()) {
            detachOnMain()
            return
        }
        val overlay = view ?: BlurOverlayView(service).also {
            view = it
            windowManager.addView(it, layoutParams())
        }
        overlay.setCoveredRegions(plan.covered)
    }

    private fun detachOnMain() {
        view?.let { current ->
            runCatching { windowManager.removeView(current) }
        }
        view = null
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }
}
