package com.safenest.kids.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.safenest.kids.R

/**
 * The dial on the Home screen: a full-circle track with the remaining share of the daily budget
 * swept over it.
 *
 * Drawn by hand rather than rotating a determinate `ProgressBar`, so the stroke width, the cap, and
 * the start angle stay under the design's control.
 */
class BudgetRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    /** Twelve o'clock. Sweeping from anywhere else reads as an arbitrary starting point. */
    private val startAngle = -90f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.gray_light)
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.teal_brand)
    }

    private val arcBounds = RectF()

    var strokeWidthPx: Float = dp(14f)
        set(value) {
            field = value
            invalidate()
        }

    /** Share of the circle to sweep, `0f`–`1f`. Values outside that range are clamped. */
    var sweepFraction: Float = 0f
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            if (field != clamped) {
                field = clamped
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        trackPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeWidth = strokeWidthPx

        // Inset by half the stroke, or the stroke is drawn centred on the edge and clipped.
        val inset = strokeWidthPx / 2f
        val diameter = minOf(width, height).toFloat() - strokeWidthPx
        if (diameter <= 0f) return
        val left = inset + (width - strokeWidthPx - diameter) / 2f
        val top = inset + (height - strokeWidthPx - diameter) / 2f
        arcBounds.set(left, top, left + diameter, top + diameter)

        canvas.drawArc(arcBounds, 0f, 360f, false, trackPaint)

        // A round cap on a zero-length sweep still paints a dot, which would read as a sliver of
        // time remaining on a budget that is spent.
        if (sweepFraction > 0f) {
            canvas.drawArc(arcBounds, startAngle, 360f * sweepFraction, false, progressPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
