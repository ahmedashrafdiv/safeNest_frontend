package com.example.safenest.util

/** Validates the parent-confirmed daily limit before it becomes canonical policy. */
object DailyLimitConfirmationValidator {
    fun minutesOrNull(rawValue: String): Int? = rawValue.trim().toIntOrNull()?.takeIf { it >= 0 }
}
