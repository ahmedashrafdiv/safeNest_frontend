package com.safenest.kids.security

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build

internal object ProtectedHomeRoleManager {
    fun isAvailable(context: Context): Boolean = roleManager(context)?.isRoleAvailable(RoleManager.ROLE_HOME) == true

    fun isActive(context: Context): Boolean = roleManager(context)?.isRoleHeld(RoleManager.ROLE_HOME) == true

    fun createRequestIntent(context: Context): Intent? =
        roleManager(context)
            ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_HOME) }
            ?.createRequestRoleIntent(RoleManager.ROLE_HOME)

    private fun roleManager(context: Context): RoleManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(RoleManager::class.java)
        } else {
            null
        }
}
