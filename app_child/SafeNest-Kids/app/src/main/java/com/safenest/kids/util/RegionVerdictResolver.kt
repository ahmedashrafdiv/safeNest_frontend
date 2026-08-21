package com.safenest.kids.util

import com.safenest.kids.service.Verdict

/**
 * Keeps classifier policy separate from placement and overlay policy.
 * A missing or contradictory signal is never treated as safe.
 */
object RegionVerdictResolver {
    fun resolve(
        faceDetected: Boolean?,
        classifierSafe: Boolean?,
    ): Verdict {
        if (faceDetected == null || classifierSafe == null) return Verdict.UNKNOWN
        if (faceDetected) return Verdict.UNSAFE
        return if (classifierSafe) Verdict.SAFE else Verdict.UNSAFE
    }
}
