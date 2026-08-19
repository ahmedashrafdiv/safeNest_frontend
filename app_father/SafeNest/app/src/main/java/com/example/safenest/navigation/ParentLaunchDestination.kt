package com.example.safenest.navigation

/**
 * First-launch routing is intentionally pure so authenticated sessions always
 * take precedence over onboarding and can be covered without Android UI tests.
 */
enum class ParentLaunchDestination {
    WELCOME,
    SIGN_IN,
    STARTUP_INBOX;

    companion object {
        fun resolve(isLoggedIn: Boolean, hasCompletedWelcome: Boolean): ParentLaunchDestination = when {
            isLoggedIn -> STARTUP_INBOX
            !hasCompletedWelcome -> WELCOME
            else -> SIGN_IN
        }
    }
}
