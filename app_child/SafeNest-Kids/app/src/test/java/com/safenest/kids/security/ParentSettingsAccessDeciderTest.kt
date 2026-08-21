package com.safenest.kids.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParentSettingsAccessDeciderTest {
    @Test
    fun verified_window_permits_only_android_permission_surfaces() {
        val now = 10_000L
        val expiry = ParentSettingsAccessDecider.expiresAt(now)

        assertTrue(ParentSettingsAccessDecider.permits(expiry, "com.android.settings", now))
        assertTrue(ParentSettingsAccessDecider.permits(expiry, "com.android.permissioncontroller", now))
        assertFalse(ParentSettingsAccessDecider.permits(expiry, "com.android.chrome", now))
    }

    @Test
    fun expired_window_never_permits_settings() {
        assertFalse(ParentSettingsAccessDecider.permits(10_000L, "com.android.settings", 10_000L))
    }
}
