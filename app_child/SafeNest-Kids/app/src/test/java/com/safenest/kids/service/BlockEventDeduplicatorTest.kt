package com.safenest.kids.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockEventDeduplicatorTest {
    @Test
    fun repeatedContentRemovalEventsWithinSuppressionWindowLaunchOnlyOnce() {
        val deduplicator = BlockEventDeduplicator()

        assertTrue(
            deduplicator.shouldLaunch(
                "com.safenest.kids",
                "uninstall_protection",
                isWindowStateChange = false,
                nowMillis = 1_000L,
            ),
        )
        assertFalse(
            deduplicator.shouldLaunch(
                "com.safenest.kids",
                "uninstall_protection",
                isWindowStateChange = false,
                nowMillis = 5_999L,
            ),
        )
    }

    @Test
    fun newWindowStateRemovalAttemptLaunchesImmediatelyAfterEarlierContentEvent() {
        val deduplicator = BlockEventDeduplicator()

        assertTrue(
            deduplicator.shouldLaunch(
                "com.safenest.kids",
                "uninstall_protection",
                isWindowStateChange = false,
                nowMillis = 1_000L,
            ),
        )
        assertTrue(
            deduplicator.shouldLaunch(
                "com.safenest.kids",
                "uninstall_protection",
                isWindowStateChange = true,
                nowMillis = 1_100L,
            ),
        )
    }
}
