package com.safenest.kids.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPolicyDeciderTest {
    private val childPackage = "com.safenest.kids"

    @Test
    fun allowlistBlocksUnselectedAndNewPackages() {
        assertFalse(AppPolicyDecider.shouldBlock("com.youtube", childPackage, "allowlist", setOf("com.youtube"), emptySet()))
        assertTrue(AppPolicyDecider.shouldBlock("com.new.game", childPackage, "allowlist", setOf("com.youtube"), emptySet()))
    }

    @Test
    fun blocklistLeavesUnselectedAndNewPackagesOpen() {
        assertTrue(AppPolicyDecider.shouldBlock("com.tiktok", childPackage, "blocklist", emptySet(), setOf("com.tiktok")))
        assertFalse(AppPolicyDecider.shouldBlock("com.new.game", childPackage, "blocklist", emptySet(), setOf("com.tiktok")))
    }

    @Test
    fun protectedSurfacesRemainOpenInAllowlistMode() {
        assertFalse(AppPolicyDecider.shouldBlock("com.android.settings", childPackage, "allowlist", emptySet(), emptySet()))
        assertFalse(AppPolicyDecider.shouldBlock("com.google.android.permissioncontroller", childPackage, "allowlist", emptySet(), emptySet()))
        assertFalse(AppPolicyDecider.shouldBlock("com.safenest.kids", childPackage, "allowlist", emptySet(), emptySet()))
        assertFalse(AppPolicyDecider.shouldBlock("com.android.launcher3", childPackage, "allowlist", emptySet(), emptySet()))
    }

    @Test
    fun missingOrUnknownModeUsesBlocklistSemantics() {
        assertTrue(AppPolicyDecider.shouldBlock("com.tiktok", childPackage, "", emptySet(), setOf("com.tiktok")))
        assertFalse(AppPolicyDecider.shouldBlock("com.new.game", childPackage, "", emptySet(), setOf("com.tiktok")))
    }
}
