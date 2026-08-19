package com.safenest.kids.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectedHomeDeciderTest {
    @Test
    fun test_layngo_tile_long_press_opens_the_protection_screen() {
        val action = ProtectedHomeDecider.longPressAction(
            packageName = "com.safenest.kids",
            layngoPackageName = "com.safenest.kids",
        )

        assertEquals(ProtectedHomeLongPressAction.BLOCK_LAYNGO, action)
    }

    @Test
    fun test_other_app_tile_long_press_keeps_its_own_actions() {
        val action = ProtectedHomeDecider.longPressAction(
            packageName = "com.android.chrome",
            layngoPackageName = "com.safenest.kids",
        )

        assertEquals(ProtectedHomeLongPressAction.SHOW_APP_ACTIONS, action)
    }
}
