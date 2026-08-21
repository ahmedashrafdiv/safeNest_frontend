package com.safenest.kids.util

import com.safenest.kids.service.FrameIntegrity

object FrameIntegrityChecker {
    fun check(
        width: Int,
        height: Int,
        opaquePixelCount: Int,
        sentinelPixelCount: Int,
        totalPixelCount: Int = width * height,
    ): FrameIntegrity {
        if (width <= 0 || height <= 0 || totalPixelCount <= 0) {
            return FrameIntegrity(false, "invalid_dimensions")
        }
        if (opaquePixelCount <= 0) {
            return FrameIntegrity(false, "black_or_transparent_frame")
        }
        if (sentinelPixelCount.toLong() * 100L >= totalPixelCount.toLong() * 80L) {
            return FrameIntegrity(false, "overlay_sentinel_dominated")
        }
        return FrameIntegrity(true, "valid")
    }
}
