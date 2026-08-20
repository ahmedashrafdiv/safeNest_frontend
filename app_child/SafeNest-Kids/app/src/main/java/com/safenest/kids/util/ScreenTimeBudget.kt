package com.safenest.kids.util

import kotlin.math.ceil

/**
 * Turns a daily screen-time budget into the two values the Home ring renders: the remaining-minute
 * number and the fraction of the circle still drawn.
 *
 * Kept free of Android types so the JVM test source set can exercise the boundaries directly.
 */
object ScreenTimeBudget {

    /**
     * Shown whenever the Backend cannot name a usable budget: a 404 `policy_not_found` when the
     * parent has assigned no daily policy, an `unknown` decision for a policy that has not taken
     * effect yet, or any decision carrying a non-positive limit. It is a display convenience only —
     * nothing enforces it and it is never reported back as a policy — so the child sees a live,
     * plausible number instead of a zero that would read as "your time is up" or a blank dial that
     * says nothing.
     */
    const val DEFAULT_BUDGET_SECONDS: Int = 5 * 60 * 60

    /** The Backend's verdict when it has no budget to report. */
    const val DECISION_UNKNOWN = "unknown"

    private const val SECONDS_PER_MINUTE = 60

    data class Ring(
        /** The primary number on the dial. */
        val remainingMinutes: Int,
        val remainingSeconds: Int,
        val effectiveLimitSeconds: Int,
        /** Share of the circle to sweep, `0f`–`1f`. */
        val sweepFraction: Float,
        /** True while the five-hour fallback is standing in for an unassigned policy. */
        val usesDefaultBudget: Boolean,
        val isExhausted: Boolean,
    )

    /**
     * Build the ring from a `screen-time-decision` body.
     *
     * A 200 is not by itself proof of a usable budget. The Backend answers `unknown` with a zero
     * limit for a policy that has not taken effect yet, which means the same thing to a child as no
     * policy at all — so it renders the same way rather than as an emptied dial announcing that
     * time nobody has limited has run out.
     */
    fun fromDecision(
        decision: String?,
        remainingSeconds: Int,
        effectiveLimitSeconds: Int,
        localUsedMinutes: Long,
    ): Ring {
        if (decision == DECISION_UNKNOWN || effectiveLimitSeconds <= 0) {
            return fromDefaultBudget(localUsedMinutes)
        }
        return build(remainingSeconds, effectiveLimitSeconds, usesDefaultBudget = false)
    }

    /**
     * Build the ring from the fallback budget, subtracting the usage this device measured for
     * itself. [localUsedMinutes] comes from `AppUsageHelper.getTodayUsageStats`, which reports
     * minutes.
     */
    fun fromDefaultBudget(localUsedMinutes: Long): Ring {
        val usedSeconds = localUsedMinutes.coerceAtLeast(0L) * SECONDS_PER_MINUTE
        // A device that has been awake all day can exceed the fallback; clamping keeps the
        // subtraction from turning into a negative remainder.
        val remaining = (DEFAULT_BUDGET_SECONDS - usedSeconds).coerceIn(0L, DEFAULT_BUDGET_SECONDS.toLong())
        return build(remaining.toInt(), DEFAULT_BUDGET_SECONDS, usesDefaultBudget = true)
    }

    private fun build(remainingSeconds: Int, effectiveLimitSeconds: Int, usesDefaultBudget: Boolean): Ring {
        val limit = effectiveLimitSeconds.coerceAtLeast(0)
        // A policy that permits nothing, and any remainder the server reports beyond the limit,
        // both have to land inside the dial rather than overdraw it.
        val remaining = remainingSeconds.coerceIn(0, limit)
        return Ring(
            // Ceiling, so the last partial minute still reads as time the child has rather than
            // flipping the dial to zero while the device is still usable.
            remainingMinutes = ceil(remaining.toDouble() / SECONDS_PER_MINUTE).toInt(),
            remainingSeconds = remaining,
            effectiveLimitSeconds = limit,
            sweepFraction = if (limit == 0) 0f else remaining.toFloat() / limit.toFloat(),
            usesDefaultBudget = usesDefaultBudget,
            isExhausted = remaining == 0,
        )
    }
}
