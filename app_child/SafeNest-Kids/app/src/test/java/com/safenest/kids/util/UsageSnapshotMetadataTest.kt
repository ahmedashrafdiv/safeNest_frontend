package com.safenest.kids.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class UsageSnapshotMetadataTest {

    @Test
    fun test_snapshot_metadata_uses_the_same_explicit_local_day_and_timezone() {
        val zone = ZoneId.of("UTC")

        val metadata = UsageSnapshotMetadataFactory.forZone(zone)

        assertEquals("UTC", metadata.usageTimezone)
        assertEquals(LocalDate.now(zone).toString(), metadata.usageDay)
    }
}

