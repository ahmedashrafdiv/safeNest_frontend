package com.safenest.kids.service

/** Prevents rapid foreground transitions from repeatedly replacing an in-flight policy refresh. */
internal object AppPolicyRefreshDecider {
    private const val MIN_ENQUEUE_INTERVAL_MILLIS = 30_000L

    fun shouldEnqueue(lastEnqueuedAtMillis: Long, nowMillis: Long): Boolean =
        lastEnqueuedAtMillis <= 0L || nowMillis - lastEnqueuedAtMillis >= MIN_ENQUEUE_INTERVAL_MILLIS
}
