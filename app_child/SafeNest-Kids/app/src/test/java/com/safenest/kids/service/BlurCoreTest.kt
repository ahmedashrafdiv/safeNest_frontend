package com.safenest.kids.service

import com.safenest.kids.util.FrameIntegrityChecker
import com.safenest.kids.util.RegionGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlurCoreTest {
    private val region = CandidateRegion(
        packageName = "com.google.android.youtube",
        stableKey = "video-1",
        treeContentHash = "hash-1",
        bounds = RegionBounds(0, 0, 100, 100),
        role = NodeRole.VIDEO,
    )

    @Test
    fun unsafe_or_unknown_verdicts_never_reveal() {
        val builder = BlurPlanBuilder(BlurConfig(requiredSafeObservations = 3))
        val plan = builder.build(
            trackedRegions = listOf(
                TrackedRegion(
                    candidate = region,
                    verdict = Verdict.UNKNOWN,
                    verdictAtMillis = 1_000L,
                    safeObservations = 0,
                    covered = true,
                ),
            ),
            nowMillis = 1_100L,
            scrollState = ScrollState.IDLE,
        )

        assertEquals(listOf(region.bounds), plan.covered)
        assertTrue(plan.revealed.isEmpty())
    }

    @Test
    fun safe_verdict_requires_current_geometry_and_three_observations() {
        val builder = BlurPlanBuilder(BlurConfig(requiredSafeObservations = 3))
        val plan = builder.build(
            trackedRegions = listOf(
                TrackedRegion(
                    candidate = region,
                    verdict = Verdict.SAFE,
                    pixelDigest = "digest-1",
                    verdictAtMillis = 1_000L,
                    safeObservations = 3,
                    covered = false,
                ),
            ),
            nowMillis = 1_100L,
            scrollState = ScrollState.IDLE,
            currentPixelDigests = mapOf(region.stableKey to "digest-1"),
        )

        assertEquals(listOf(region.bounds), plan.revealed)
        assertTrue(plan.covered.isEmpty())
    }

    @Test
    fun scrolling_keeps_safe_region_covered_until_idle() {
        val gate = ScrollStateGate()
        gate.onScroll(1_000L)
        val builder = BlurPlanBuilder(BlurConfig(requiredSafeObservations = 1))
        val plan = builder.build(
            trackedRegions = listOf(
                TrackedRegion(region, Verdict.SAFE, "digest-1", 1_000L, 1, covered = false),
            ),
            nowMillis = 1_100L,
            scrollState = gate.state,
        )

        assertEquals(listOf(region.bounds), plan.covered)
        assertTrue(plan.revealed.isEmpty())
    }

    @Test
    fun changed_tree_content_resets_revealed_region_to_unknown() {
        val tracker = RegionTracker()
        tracker.update(listOf(region))
        tracker.applyVerdict(region.stableKey, Verdict.SAFE, "digest-1", 1_000L, 1)
        val changed = region.copy(treeContentHash = "hash-2")

        val refreshed = tracker.update(listOf(changed)).single()

        assertEquals(Verdict.UNKNOWN, refreshed.verdict)
        assertTrue(refreshed.covered)
    }

    @Test
    fun capture_scheduler_rejects_overlap_and_short_interval() {
        val scheduler = CaptureScheduler(minimumIntervalMillis = 333L)
        assertTrue(scheduler.request(1_000L) != null)
        assertTrue(scheduler.request(1_100L) == null)
        scheduler.complete()
        assertTrue(scheduler.request(1_200L) == null)
        assertTrue(scheduler.request(1_334L) != null)
    }

    @Test
    fun geometry_and_frame_integrity_fail_closed() {
        assertEquals(1.0 / 7.0, RegionGeometry.iou(RegionBounds(0, 0, 100, 100), RegionBounds(50, 50, 150, 150)), 0.0001)
        assertFalse(FrameIntegrityChecker.check(100, 100, 0, 0).isValid)
        assertFalse(FrameIntegrityChecker.check(100, 100, 10_000, 8_500).isValid)
        assertTrue(FrameIntegrityChecker.check(100, 100, 10_000, 100).isValid)
    }
}
