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
    fun parentAppIsNeverBlocked_regression20260820() {
        // Production incident: the parent switched this device to allowlist mode, allowed one app,
        // and was then locked out of the Layngo Parent app itself — the only place the policy can
        // be changed back — because it was not in the allowlist.
        assertFalse(AppPolicyDecider.shouldBlock("com.example.safenest", childPackage, "allowlist", emptySet(), emptySet()))
        // Even an explicit blocklist entry must not be able to lock the parent out.
        assertFalse(
            AppPolicyDecider.shouldBlock(
                "com.example.safenest",
                childPackage,
                "blocklist",
                emptySet(),
                setOf("com.example.safenest"),
            ),
        )
    }

    @Test
    fun missingOrUnknownModeUsesBlocklistSemantics() {
        assertTrue(AppPolicyDecider.shouldBlock("com.tiktok", childPackage, "", emptySet(), setOf("com.tiktok")))
        assertFalse(AppPolicyDecider.shouldBlock("com.new.game", childPackage, "", emptySet(), setOf("com.tiktok")))
    }

    @Test
    fun chromeInPersistedBlocklistIsBlocked_regression20260818() {
        assertTrue(
            AppPolicyDecider.shouldBlock(
                "com.android.chrome",
                childPackage,
                "blocklist",
                emptySet(),
                setOf("com.android.chrome"),
            ),
        )
    }}
