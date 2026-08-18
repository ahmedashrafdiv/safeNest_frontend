package com.safenest.kids.security

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherEntryDeciderTest {
    @Test
    // Regression: a paired Child could reopen Layngo's normal controls from its launcher icon.
    fun pairedStateDeterminesWhetherLauncherEntryRedirectsToProtection() {
        listOf(
            true to true,
            false to false,
        ).forEach { (isPaired, expectedRedirect) ->
            assertEquals(expectedRedirect, LauncherEntryDecider.shouldRedirectToProtection(isPaired))
        }
    }
}
