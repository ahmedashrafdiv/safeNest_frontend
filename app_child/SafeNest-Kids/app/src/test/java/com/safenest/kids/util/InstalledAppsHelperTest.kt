package com.safenest.kids.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class InstalledAppsHelperTest {
    @Test
    fun fingerprint_is_stable_when_inventory_order_changes() {
        val first = listOf(
            "com.example.b" to "B",
            "com.example.a" to "A"
        )
        val reordered = listOf(
            "com.example.a" to "A",
            "com.example.b" to "B"
        )

        assertEquals(InstalledAppsHelper.fingerprint(first), InstalledAppsHelper.fingerprint(reordered))
    }

    @Test
    fun fingerprint_changes_when_a_new_package_is_added() {
        val before = listOf("com.example.a" to "A")
        val after = before + ("com.example.b" to "B")

        assertNotEquals(InstalledAppsHelper.fingerprint(before), InstalledAppsHelper.fingerprint(after))
    }

    @Test
    fun fingerprint_changes_when_a_display_label_changes() {
        val before = listOf("com.example.a" to "Old label")
        val after = listOf("com.example.a" to "New label")

        assertNotEquals(InstalledAppsHelper.fingerprint(before), InstalledAppsHelper.fingerprint(after))
    }

    @Test
    fun fingerprint_deduplicates_the_same_package_deterministically() {
        val duplicate = listOf(
            "com.example.a" to "A",
            "com.example.a" to "A duplicate"
        )
        val canonical = listOf("com.example.a" to "A")

        assertEquals(InstalledAppsHelper.fingerprint(duplicate), InstalledAppsHelper.fingerprint(canonical))
    }
}
