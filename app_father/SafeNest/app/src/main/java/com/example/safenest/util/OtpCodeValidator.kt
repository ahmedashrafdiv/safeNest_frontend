package com.example.safenest.util

object OtpCodeValidator {
    private val sixDigits = Regex("\\d{6}")

    fun error(code: String): String? {
        return if (sixDigits.matches(code)) {
            null
        } else {
            "الرجاء إدخال الرمز المكون من 6 أرقام"
        }
    }
}
