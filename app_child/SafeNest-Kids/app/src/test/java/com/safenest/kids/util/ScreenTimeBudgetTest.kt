package com.safenest.kids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTimeBudgetTest {

    @Test
    fun policyBudgetReportsRemainingMinutesAndSweep() {
        val ring = ScreenTimeBudget.fromDecision(remainingSeconds = 2040, effectiveLimitSeconds = 3600)

        assertEquals(34, ring.remainingMinutes)
        assertEquals(2040f / 3600f, ring.sweepFraction, 0.0001f)
        assertFalse(ring.usesDefaultBudget)
        assertFalse(ring.isExhausted)
    }

    @Test
    fun exhaustedPolicyBudgetEmptiesTheDial() {
        val ring = ScreenTimeBudget.fromDecision(remainingSeconds = 0, effectiveLimitSeconds = 3600)

        assertEquals(0, ring.remainingMinutes)
        assertEquals(0f, ring.sweepFraction, 0.0001f)
        assertTrue(ring.isExhausted)
    }

    @Test
    fun partialMinuteStillReadsAsRemainingTime() {
        val ring = ScreenTimeBudget.fromDecision(remainingSeconds = 30, effectiveLimitSeconds = 3600)

        assertEquals(1, ring.remainingMinutes)
        assertFalse(ring.isExhausted)
    }

    @Test
    fun zeroLimitPolicyDoesNotDivideByZero() {
        val ring = ScreenTimeBudget.fromDecision(remainingSeconds = 0, effectiveLimitSeconds = 0)

        assertEquals(0, ring.remainingMinutes)
        assertEquals(0f, ring.sweepFraction, 0.0001f)
        assertTrue(ring.isExhausted)
    }

    @Test
    fun remainderBeyondTheLimitIsClampedIntoTheDial() {
        val ring = ScreenTimeBudget.fromDecision(remainingSeconds = 7200, effectiveLimitSeconds = 3600)

        assertEquals(60, ring.remainingMinutes)
        assertEquals(1f, ring.sweepFraction, 0.0001f)
    }

    @Test
    fun negativeRemainderIsTreatedAsExhausted() {
        val ring = ScreenTimeBudget.fromDecision(remainingSeconds = -120, effectiveLimitSeconds = 3600)

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
