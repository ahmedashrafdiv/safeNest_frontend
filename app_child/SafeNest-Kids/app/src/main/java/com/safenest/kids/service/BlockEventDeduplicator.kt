package com.safenest.kids.service

/**
 * Prevents duplicate content-change events from repeatedly launching the same
 * Layngo block screen. A new window-state event is always actionable because it
 * represents a fresh navigation attempt, even within the suppression window.
 */
internal class BlockEventDeduplicator(
    private val suppressionWindowMillis: Long = 5_000L,
) {
    private var lastKey = ""
    private var lastLaunchAtMillis = 0L

    fun shouldLaunch(
        packageName: String,
        reason: String,
        isWindowStateChange: Boolean,
        nowMillis: Long,
    ): Boolean {
        val key = "$packageName:$reason"
        if (
            !isWindowStateChange &&
            key == lastKey &&
            nowMillis - lastLaunchAtMillis < suppressionWindowMillis
        ) {
            return false
        }

        lastKey = key
        lastLaunchAtMillis = nowMillis
        return true
    }
}
