package com.safenest.kids.service

/** Pure geometry value that can be exercised by the JVM test task. */
data class RegionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    init {
        require(left <= right) { "left must not exceed right" }
        require(top <= bottom) { "top must not exceed bottom" }
    }

    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Long get() = width.toLong() * height.toLong()

    fun isEmpty(): Boolean = width <= 0 || height <= 0
}

enum class NodeRole {
    IMAGE,
    VIDEO,
    MEDIA_CONTAINER,
    UNKNOWN,
}

data class CandidateRegion(
    val packageName: String,
    val stableKey: String,
    val treeContentHash: String,
    val bounds: RegionBounds,
    val role: NodeRole,
)

enum class Verdict {
    UNSAFE,
    UNKNOWN,
    ANALYZING,
    SAFE,
    STALE,
}

enum class ScrollState {
    IDLE,
    SCROLLING,
    SETTLING,
}

data class TrackedRegion(
    val candidate: CandidateRegion,
    val verdict: Verdict = Verdict.UNKNOWN,
    val pixelDigest: String? = null,
    val verdictAtMillis: Long = 0L,
    val safeObservations: Int = 0,
    val covered: Boolean = true,
)

data class BlurPlan(
    val covered: List<RegionBounds>,
    val revealed: List<RegionBounds>,
    val reason: String,
) {
    companion object {
        fun conservative(regions: List<RegionBounds>, reason: String): BlurPlan =
            BlurPlan(covered = regions, revealed = emptyList(), reason = reason)
    }
}

data class BlurConfig(
    val targetPackages: Set<String> = emptySet(),
    val excludedPackages: Set<String> = setOf(
        "com.safenest.kids",
        "com.example.safenest",
        "com.android.launcher",
        "com.android.systemui",
    ),
    val maxVerdictAgeMillis: Long = 1_500L,
    val geometryTolerancePx: Int = 8,
    val requiredSafeObservations: Int = 3,
    val maxBlindMillis: Long = 2_000L,
)

data class CaptureRequest(
    val requestId: Long,
    val requestedAtMillis: Long,
)

data class FrameIntegrity(
    val isValid: Boolean,
    val reason: String,
)
