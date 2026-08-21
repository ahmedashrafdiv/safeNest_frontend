package com.safenest.kids.security

/** A verified parent may repair Android permissions briefly; the child never receives this bypass. */
object ParentSettingsAccessDecider {
    const val ACCESS_WINDOW_MILLIS = 2 * 60 * 1000L

    private val permittedPackages = setOf(
        "com.android.settings",
        "com.coloros.settings",
        "com.oplus.settings",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
    )

    fun expiresAt(nowMillis: Long): Long = nowMillis + ACCESS_WINDOW_MILLIS

    fun permits(settingsAccessUntil: Long, packageName: String, nowMillis: Long): Boolean =
        settingsAccessUntil > nowMillis && packageName in permittedPackages
}
