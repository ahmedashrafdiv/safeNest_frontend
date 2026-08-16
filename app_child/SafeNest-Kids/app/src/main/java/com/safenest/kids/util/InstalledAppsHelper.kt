package com.safenest.kids.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object InstalledAppsHelper {
    fun getInstalledApps(context: Context): List<Pair<String, String>> {
        val pm: PackageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val apps = mutableListOf<Pair<String, String>>()
        
        for (info in resolveInfos) {
            val packageName = info.activityInfo.packageName
            if (packageName != context.packageName) {
                val appName = info.loadLabel(pm).toString()
                apps.add(Pair(packageName, appName))
            }
        }
        
        return apps
    }
}
