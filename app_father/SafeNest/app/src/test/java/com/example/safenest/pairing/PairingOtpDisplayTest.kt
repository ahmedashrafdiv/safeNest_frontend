package com.example.safenest.pairing

import org.junit.Assert.assertEquals
import org.junit.Test

class PairingOtpDisplayTest {
    @Test
    fun expiry_at_or_before_now_shows_zero_remaining_seconds() {
        assertEquals(0L, PairingOtpDisplay.secondsRemaining(expiresAtMillis = 1_000L, nowMillis = 1_000L))
        assertEquals(0L, PairingOtpDisplay.secondsRemaining(expiresAtMillis = 999L, nowMillis = 1_000L))
    }

    @Test
    fun active_otp_preserves_full_seconds_and_visual_spacing() {
        assertEquals(125L, PairingOtpDisplay.secondsRemaining(expiresAtMillis = 126_000L, nowMillis = 1_000L))
        assertEquals("4 8 2 6 1 9", PairingOtpDisplay.spacedCode("482619"))
    }
}
