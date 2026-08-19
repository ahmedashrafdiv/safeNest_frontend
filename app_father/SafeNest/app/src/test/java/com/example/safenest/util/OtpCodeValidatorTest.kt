package com.example.safenest.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpCodeValidatorTest {

    @Test
    fun test_incomplete_or_non_numeric_code_blocks_verification() {
        listOf("", "12345", "1234567", "12ab56").forEach { code ->
            assertEquals("الرجاء إدخال الرمز المكون من 6 أرقام", OtpCodeValidator.error(code))
        }
    }

    @Test
    fun test_six_digit_code_allows_verification() {
        assertNull(OtpCodeValidator.error("123456"))
    }
}
