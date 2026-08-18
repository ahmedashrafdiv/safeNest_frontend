package com.safenest.kids.security

/**
 * A paired Child device does not expose Layngo's normal launcher entry to the
 * child. Initial pairing remains available before the device is bound.
 */
internal object LauncherEntryDecider {
    fun shouldRedirectToProtection(isPaired: Boolean): Boolean = isPaired
}
