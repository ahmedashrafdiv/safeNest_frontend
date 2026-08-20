package com.safenest.kids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeBudgetTest {

    @Test
    fun policyBudgetReportsRemainingMinutesAndSweep() {
        val ring = ScreenTimeBudget.fromDecision("allow", 2040, 3600, localUsedMinutes = 0)

        assertEquals(34, ring.remainingMinutes)
        assertEquals(2040f / 3600f, ring.sweepFraction, 0.0001f)
        assertFalse(ring.usesDefaultBudget)
        assertFalse(ring.isExhausted)
    }

    @Test
    fun exhaustedPolicyBudgetEmptiesTheDial() {
        val ring = ScreenTimeBudget.fromDecision("limit_reached", 0, 3600, localUsedMinutes = 60)

        assertEquals(0, ring.remainingMinutes)
        assertEquals(0f, ring.sweepFraction, 0.0001f)
        assertFalse(ring.usesDefaultBudget)
        assertTrue(ring.isExhausted)
    }

    @Test
    fun partialMinuteStillReadsAsRemainingTime() {
        val ring = ScreenTimeBudget.fromDecision("allow", 30, 3600, localUsedMinutes = 0)

        assertEquals(1, ring.remainingMinutes)
        assertFalse(ring.isExhausted)
    }

    @Test
    fun unknownDecisionFallsBackToTheDefaultBudget() {
        val ring = ScreenTimeBudget.fromDecision("unknown", 0, 0, localUsedMinutes = 60)

        assertEquals(240, ring.remainingMinutes)
        assertTrue(ring.usesDefaultBudget)
        assertFalse(ring.isExhausted)
    }

    @Test
    fun missingDecisionFallsBackToTheDefaultBudget() {
        val ring = ScreenTimeBudget.fromDecision(null, 0, 0, localUsedMinutes = 0)

        assertEquals(300, ring.remainingMinutes)
        assertTrue(ring.usesDefaultBudget)
    }

    @Test
    fun aPolicyPermittingNothingStaysABlockRatherThanFallingBack() {
        // Not reachable while the Backend pins daily_limit_seconds to ge=60, but if that bound is
        // ever relaxed the strictest policy a parent can set must not render as five free hours.
        val ring = ScreenTimeBudget.fromDecision("limit_reached", 0, 0, localUsedMinutes = 0)

        assertEquals(0, ring.remainingMinutes)
        assertEquals(0f, ring.sweepFraction, 0.0001f)
        assertFalse(ring.usesDefaultBudget)
        assertTrue(ring.isExhausted)
    }

    @Test
    fun remainderBeyondTheLimitIsClampedIntoTheDial() {
        val ring = ScreenTimeBudget.fromDecision("allow", 7200, 3600, localUsedMinutes = 0)

        assertEquals(60, ring.remainingMinutes)
        assertEquals(1f, ring.sweepFraction, 0.0001f)
    }

    @Test
    fun negativeRemainderIsTreatedAsExhausted() {
        val ring = ScreenTimeBudget.fromDecision("limit_reached", -120, 3600, localUsedMinutes = 0)

        assertEquals(0, ring.remainingMinutes)
        assertTrue(ring.isExhausted)
    }

    @Test
    fun defaultBudgetIsFiveHoursMinusMeasuredUsage() {
        val ring = ScreenTimeBudget.fromDefaultBudget(localUsedMinutes = 60)

        assertEquals(ScreenTimeBudget.DEFAULT_BUDGET_SECONDS, ring.effectiveLimitSeconds)
        assertEquals(18000, ring.effectiveLimitSeconds)
        assertEquals(240, ring.remainingMinutes)
        assertTrue(ring.usesDefaultBudget)
        assertFalse(ring.isExhausted)
    }

    @Test
    fun untouchedDeviceShowsTheWholeDefaultBudget() {
        val ring = ScreenTimeBudget.fromDefaultBudget(localUsedMinutes = 0)

        assertEquals(300, ring.remainingMinutes)
        assertEquals(1f, ring.sweepFraction, 0.0001f)
    }

    @Test
    fun usageBeyondTheDefaultBudgetDoesNotGoNegative() {
        val ring = ScreenTimeBudget.fromDefaultBudget(localUsedMinutes = 900)

        assertEquals(0, ring.remainingMinutes)
        assertEquals(0f, ring.sweepFraction, 0.0001f)
        assertTrue(ring.isExhausted)
    }

    @Test
    fun unreadableUsageDoesNotInflateTheDefaultBudget() {
        val ring = ScreenTimeBudget.fromDefaultBudget(localUsedMinutes = -30)

        assertEquals(300, ring.remainingMinutes)
    }
}
