package com.example.safenest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyLimitConfirmationValidatorTest {

    @Test
    fun test_parent_limit_accepts_zero_or_positive_whole_minutes_and_rejects_invalid_values() {
        assertEquals(0, DailyLimitConfirmationValidator.minutesOrNull("0"))
        assertEquals(300, DailyLimitConfirmationValidator.minutesOrNull(" 300 "))
        listOf("", "-1", "five", "1.5").forEach { value ->
            assertNull(DailyLimitConfirmationValidator.minutesOrNull(value))
        }
    }
}
