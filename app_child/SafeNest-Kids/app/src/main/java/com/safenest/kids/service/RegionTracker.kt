package com.safenest.kids.service

class RegionTracker {
    private val regions = linkedMapOf<String, TrackedRegion>()

    fun update(candidates: List<CandidateRegion>): List<TrackedRegion> {
        val next = linkedMapOf<String, TrackedRegion>()
        candidates.forEach { candidate ->
            val previous = regions[candidate.stableKey]
            val tracked = if (previous == null) {
                TrackedRegion(candidate = candidate)
            } else if (previous.candidate.treeContentHash != candidate.treeContentHash) {
                TrackedRegion(candidate = candidate, verdict = Verdict.UNKNOWN)
            } else {
                previous.copy(candidate = candidate)
            }
            next[candidate.stableKey] = tracked
        }
        regions.clear()
        regions.putAll(next)
        return next.values.toList()
    }

    fun applyVerdict(
        stableKey: String,
        verdict: Verdict,
        pixelDigest: String?,
        nowMillis: Long,
        requiredSafeObservations: Int,
    ): TrackedRegion? {
        val current = regions[stableKey] ?: return null
        val safeCount = if (verdict == Verdict.SAFE) {
            current.safeObservations + 1
        } else {
            0
        }
        val canReveal = verdict == Verdict.SAFE && safeCount >= requiredSafeObservations
        val updated = current.copy(
            verdict = verdict,
            pixelDigest = pixelDigest,
            verdictAtMillis = nowMillis,
            safeObservations = safeCount,
            covered = !canReveal,
        )
        regions[stableKey] = updated
        return updated
    }

    fun values(): List<TrackedRegion> = regions.values.toList()

    fun clear() {
        regions.clear()
    }
}
