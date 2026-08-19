package com.safenest.kids.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

/** Maps the device's local day of week to the Backend's Saturday-first day codes (sat..fri). */
object WeekdayCode {
    val CODES = listOf("sat", "sun", "mon", "tue", "wed", "thu", "fri")

    fun forDayOfWeek(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.SATURDAY -> "sat"
        DayOfWeek.SUNDAY -> "sun"
        DayOfWeek.MONDAY -> "mon"
        DayOfWeek.TUESDAY -> "tue"
        DayOfWeek.WEDNESDAY -> "wed"
        DayOfWeek.THURSDAY -> "thu"
        DayOfWeek.FRIDAY -> "fri"
    }

    fun today(zoneId: ZoneId = ZoneId.systemDefault()): String =
        forDayOfWeek(LocalDate.now(zoneId).dayOfWeek)
}
