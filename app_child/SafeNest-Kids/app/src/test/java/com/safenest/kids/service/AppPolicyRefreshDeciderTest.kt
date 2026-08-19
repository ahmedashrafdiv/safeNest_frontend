package com.safenest.kids.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPolicyRefreshDeciderTest {
    @Test
    fun initialAndExpiredForegroundTransitionsEnqueueWhileBurstEventsDoNot() {
        val now = 100_000L
        val cases = listOf(
            0L to true,
            now - 29_999L to false,
            now - 30_000L to true,
        )

        cases.forEach { (lastEnqueuedAt, expected) ->
            assertEquals(expected, AppPolicyRefreshDecider.shouldEnqueue(lastEnqueuedAt, now))
        }
    }
}
