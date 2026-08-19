package com.safenest.kids.util

import java.time.LocalDate
import java.time.ZoneId

/** Builds the explicit local-day identity carried by each complete usage snapshot. */
data class UsageSnapshotMetadata(val usageDay: String, val usageTimezone: String)

object UsageSnapshotMetadataFactory {
    fun forZone(zoneId: ZoneId = ZoneId.systemDefault()): UsageSnapshotMetadata =
        UsageSnapshotMetadata(
            usageDay = LocalDate.now(zoneId).toString(),
            usageTimezone = zoneId.id
        )
}
