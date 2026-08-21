package com.safenest.kids.service

import com.safenest.kids.util.RegionGeometry

class BlurPlanBuilder(
    private val config: BlurConfig,
) {
    fun build(
        trackedRegions: List<TrackedRegion>,
        nowMillis: Long,
        scrollState: ScrollState,
        currentPixelDigests: Map<String, String?> = emptyMap(),
    ): BlurPlan {
        val covered = mutableListOf<RegionBounds>()
        val revealed = mutableListOf<RegionBounds>()

        trackedRegions.forEach { tracked ->
            val bounds = tracked.candidate.bounds
            val currentDigest = currentPixelDigests[tracked.candidate.stableKey]
            val age = nowMillis - tracked.verdictAtMillis
            val digestChanged = tracked.pixelDigest != null && currentDigest != null && tracked.pixelDigest != currentDigest
            val geometryValid = !bounds.isEmpty()
            val verdictValid = tracked.verdictAtMillis > 0L && age in 0..config.maxVerdictAgeMillis
            val safe = tracked.verdict == Verdict.SAFE &&
                tracked.safeObservations >= config.requiredSafeObservations &&
                verdictValid &&
                !digestChanged &&
                geometryValid &&
                scrollState == ScrollState.IDLE &&
                !tracked.covered

            if (safe) revealed += bounds else covered += bounds
        }

        return BlurPlan(
            covered = covered,
            revealed = revealed,
            reason = when {
                scrollState != ScrollState.IDLE -> "scroll_not_idle"
                covered.isNotEmpty() -> "fail_closed_default"
                else -> "safe_regions_only"
            },
        )
    }

    fun buildConservative(trackedRegions: List<TrackedRegion>, reason: String): BlurPlan =
        BlurPlan.conservative(trackedRegions.map { it.candidate.bounds }, reason)
}
