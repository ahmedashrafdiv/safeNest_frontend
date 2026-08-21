package com.safenest.kids.pairing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpInputValidatorTest {
    @Test
    fun pasted_mixed_text_keeps_only_ascii_otp_digits() {
        assertEquals("482619", OtpInputValidator.asciiDigits("4 8-2a619"))
    }

    @Test
    fun complete_code_requires_exactly_six_ascii_digits() {
        assertTrue(OtpInputValidator.isComplete("482619"))
        assertFalse(OtpInputValidator.isComplete("48261"))
        assertFalse(OtpInputValidator.isComplete("4826190"))
    }
}
