package com.safenest.kids.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleSyncWorkerTest {
    /**
     * Regression: 2026-08-17 production device testing exposed an undeployed
     * effective-policy route returning HTTP 404. The worker must surface this
     * as a non-retryable failure instead of silently reporting success.
     */
    @Test
    fun missingPolicyRoute_isNotRetryable() {
        assertFalse(RuleSyncWorker.shouldRetryHttpStatus(404))
    }

    @Test
    fun transientBackendResponses_areRetryable() {
        assertTrue(RuleSyncWorker.shouldRetryHttpStatus(408))
        assertTrue(RuleSyncWorker.shouldRetryHttpStatus(429))
        assertTrue(RuleSyncWorker.shouldRetryHttpStatus(500))
    }
}
