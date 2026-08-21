package com.safenest.kids.pairing

/** Pure input rules shared by the six visual child OTP cells. */
object OtpInputValidator {
    const val OTP_LENGTH = 6

    fun asciiDigits(value: CharSequence): String = buildString {
        value.forEach { character ->
            if (character in '0'..'9') append(character)
        }
    }

    fun isComplete(value: CharSequence): Boolean = asciiDigits(value).length == OTP_LENGTH
}
