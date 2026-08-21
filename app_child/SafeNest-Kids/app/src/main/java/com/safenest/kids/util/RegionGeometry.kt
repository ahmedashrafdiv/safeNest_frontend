package com.safenest.kids.util

import com.safenest.kids.service.RegionBounds
import kotlin.math.max
import kotlin.math.min

object RegionGeometry {
    fun intersection(first: RegionBounds, second: RegionBounds): RegionBounds? {
        val left = max(first.left, second.left)
        val top = max(first.top, second.top)
        val right = min(first.right, second.right)
        val bottom = min(first.bottom, second.bottom)
        if (right <= left || bottom <= top) return null
        return RegionBounds(left, top, right, bottom)
    }

    fun iou(first: RegionBounds, second: RegionBounds): Double {
        val overlap = intersection(first, second)?.area ?: return 0.0
        val union = first.area + second.area - overlap
        return if (union <= 0L) 0.0 else overlap.toDouble() / union.toDouble()
    }

    fun shift(bounds: RegionBounds, dx: Int, dy: Int): RegionBounds =
        RegionBounds(bounds.left + dx, bounds.top + dy, bounds.right + dx, bounds.bottom + dy)

    fun movedBeyond(first: RegionBounds, second: RegionBounds, tolerancePx: Int): Boolean =
        kotlin.math.abs(first.left - second.left) > tolerancePx ||
            kotlin.math.abs(first.top - second.top) > tolerancePx ||
            kotlin.math.abs(first.right - second.right) > tolerancePx ||
            kotlin.math.abs(first.bottom - second.bottom) > tolerancePx

    fun clamp(bounds: RegionBounds, viewport: RegionBounds): RegionBounds? =
        intersection(bounds, viewport)
}
