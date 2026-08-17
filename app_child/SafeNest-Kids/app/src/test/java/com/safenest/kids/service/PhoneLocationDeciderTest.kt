package com.safenest.kids.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneLocationDeciderTest {
    private val now = 1_700_000_000_000L

    private fun sample(
        latitude: Double = 30.0444,
        longitude: Double = 31.2357,
        accuracy: Float = 20f,
        capturedAt: Long = now
    ) = PhoneLocationDecider.LocationSample(latitude, longitude, accuracy, capturedAt)

    @Test
    fun rejectsInvalidCoordinatesAndFutureSamples() {
        assertFalse(PhoneLocationDecider.isValid(sample(latitude = 91.0), now))
        assertFalse(PhoneLocationDecider.isValid(sample(capturedAt = now + 6 * 60_000L), now))
    }

    @Test
    fun uploadsFirstValidSample() {
        assertTrue(PhoneLocationDecider.shouldUpload(sample(), null, now))
    }

    @Test
    fun suppressesRedundantSampleWithinInterval() {
        val previous = sample(capturedAt = now - 5_000L)
        assertFalse(PhoneLocationDecider.shouldUpload(sample(capturedAt = now), previous, now))
    }

    @Test
    fun uploadsWhenMovementExceedsThreshold() {
        val previous = sample(longitude = 31.2357, capturedAt = now - 5_000L)
        val moved = sample(longitude = 31.2362, capturedAt = now)
        assertTrue(PhoneLocationDecider.shouldUpload(moved, previous, now))
    }

    @Test
    fun statusIsTruthfulForPermissionOfflineAndStale() {
        assertEquals(
            PhoneLocationDecider.TrackingStatus.PERMISSION_DENIED,
            PhoneLocationDecider.status(true, false, false, false, null, now)
        )
        assertEquals(
            PhoneLocationDecider.TrackingStatus.OFFLINE,
            PhoneLocationDecider.status(true, true, true, false, now, now)
        )
        assertEquals(
            PhoneLocationDecider.TrackingStatus.STALE,
            PhoneLocationDecider.status(true, true, true, true, now - 901_000L, now)
        )
    }
}
