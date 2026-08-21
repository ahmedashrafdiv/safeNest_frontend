package com.example.safenest.pairing

/** Pure OTP presentation rules used by the lifecycle-bound Parent linking UI. */
object PairingOtpDisplay {
    fun secondsRemaining(expiresAtMillis: Long, nowMillis: Long): Long =
        ((expiresAtMillis - nowMillis) / 1_000L).coerceAtLeast(0)

    fun spacedCode(code: String): String = code.toCharArray().joinToString(" ")
}
