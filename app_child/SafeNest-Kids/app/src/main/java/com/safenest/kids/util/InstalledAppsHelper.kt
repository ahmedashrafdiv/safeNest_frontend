package com.safenest.kids.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import java.security.MessageDigest

object InstalledAppsHelper {
    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos
            .asSequence()
            .map { info ->
                val packageName = info.activityInfo.packageName
                packageName to info.loadLabel(pm).toString()
            }
            .filter { (packageName, _) -> packageName != context.packageName }
            .distinctBy { (packageName, _) -> packageName }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .toList()
    }

    fun fingerprint(apps: List<Pair<String, String>>): String {
        val canonical = apps
            .distinctBy { (packageName, _) -> packageName }
            .sortedWith(compareBy({ it.first }, { it.second }))
            .joinToString(separator = "\n") { (packageName, appName) ->
                "${packageName.trim()}\u0000${appName.trim()}"
            }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
