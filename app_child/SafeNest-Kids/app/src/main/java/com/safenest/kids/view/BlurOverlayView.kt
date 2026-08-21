package com.safenest.kids.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.safenest.kids.service.RegionBounds

class BlurOverlayView(context: android.content.Context) : View(context) {
    private val coverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = SENTINEL_COLOR
        style = Paint.Style.FILL
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6B7280.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private var regions: List<RegionBounds> = emptyList()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setCoveredRegions(nextRegions: List<RegionBounds>) {
        regions = nextRegions
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        regions.forEach { bounds ->
            val rect = RectF(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat())
            canvas.drawRect(rect, coverPaint)
            canvas.drawRect(rect, edgePaint)
        }
    }

    companion object {
        const val SENTINEL_COLOR: Int = 0xE6B8C2CC.toInt()
    }
}
