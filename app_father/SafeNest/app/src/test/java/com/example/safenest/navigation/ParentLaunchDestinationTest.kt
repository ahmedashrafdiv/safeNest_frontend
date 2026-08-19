package com.example.safenest.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ParentLaunchDestinationTest {

    @Test
    fun test_fresh_unauthenticated_parent_opens_welcome() {
        val destination = ParentLaunchDestination.resolve(
            isLoggedIn = false,
            hasCompletedWelcome = false,
        )

        assertEquals(ParentLaunchDestination.WELCOME, destination)
    }

    @Test
    fun test_returning_unauthenticated_parent_opens_sign_in() {
        val destination = ParentLaunchDestination.resolve(
            isLoggedIn = false,
            hasCompletedWelcome = true,
        )

        assertEquals(ParentLaunchDestination.SIGN_IN, destination)
    }

    @Test
    fun test_authenticated_parent_bypasses_welcome_even_without_saved_completion() {
        val destination = ParentLaunchDestination.resolve(
            isLoggedIn = true,
            hasCompletedWelcome = false,
        )

        assertEquals(ParentLaunchDestination.STARTUP_INBOX, destination)
    }
}
