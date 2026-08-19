package com.safenest.kids.security

internal enum class ProtectedHomeLongPressAction {
    BLOCK_LAYNGO,
    SHOW_APP_ACTIONS,
}

/** Keeps Layngo-only long-press protection inside the Layngo-owned Home surface. */
internal object ProtectedHomeDecider {
    fun longPressAction(packageName: String, layngoPackageName: String): ProtectedHomeLongPressAction =
        if (packageName == layngoPackageName) {
            ProtectedHomeLongPressAction.BLOCK_LAYNGO
        } else {
            ProtectedHomeLongPressAction.SHOW_APP_ACTIONS
        }
}
