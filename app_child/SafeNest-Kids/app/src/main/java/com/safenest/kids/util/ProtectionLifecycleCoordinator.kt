package com.safenest.kids.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.work.WorkManager
import com.safenest.kids.security.LayngoDeviceAdminReceiver
import com.safenest.kids.security.ProtectedHomeRoleManager
import com.safenest.kids.service.AppUsageReportWorker
import com.safenest.kids.service.ContentBlurPolicySyncWorker
import com.safenest.kids.service.PhoneLocationPolicySyncWorker
import com.safenest.kids.service.PhoneLocationService
import com.safenest.kids.service.PlaceGeofenceManager
import com.safenest.kids.service.PlacePolicySyncWorker
import com.safenest.kids.service.ProtectionHealthWorker
import com.safenest.kids.service.ProtectedHomePolicySyncWorker
import com.safenest.kids.service.RuleSyncWorker
import com.safenest.kids.service.ScreenTimePolicySyncWorker
import com.safenest.kids.service.WebsiteDnsVpnService
import com.safenest.kids.service.WebsitePolicySyncWorker

/** Coordinates the supported local lifecycle after a parent has been verified by the Backend. */
object ProtectionLifecycleCoordinator {
    fun suspendProtection(context: Context) {
        val appContext = context.applicationContext
        PrefsHelper(appContext).apply {
            setProtectionSuspended(true)
            clearEnforcementPolicy()
        }
        cancelPolicySync(appContext)
        PlaceGeofenceManager.clear(appContext)
        stopProtectionServices(appContext)
    }

    fun resumeProtection(context: Context) {
        val appContext = context.applicationContext
        PrefsHelper(appContext).setProtectionSuspended(false)
        RuleSyncWorker.enqueueImmediate(appContext)
        RuleSyncWorker.enqueuePeriodic(appContext)
        ScreenTimePolicySyncWorker.enqueueImmediate(appContext)
        ScreenTimePolicySyncWorker.enqueuePeriodic(appContext)
        WebsitePolicySyncWorker.enqueueImmediate(appContext)
        ProtectedHomePolicySyncWorker.enqueueImmediate(appContext)
        ProtectedHomePolicySyncWorker.enqueuePeriodic(appContext)
        PhoneLocationPolicySyncWorker.enqueueImmediate(appContext)
        PhoneLocationPolicySyncWorker.enqueuePeriodic(appContext)
        PlacePolicySyncWorker.enqueueImmediate(appContext)
        PlacePolicySyncWorker.enqueuePeriodic(appContext)
        ContentBlurPolicySyncWorker.enqueueImmediate(appContext)
        ContentBlurPolicySyncWorker.enqueuePeriodic(appContext)
        ProtectionHealthWorker.enqueueImmediate(appContext)
        ProtectionHealthWorker.enqueuePeriodic(appContext)
    }

    fun signOut(context: Context): Boolean {
        val appContext = context.applicationContext
        val homeRoleHeld = ProtectedHomeRoleManager.isActive(appContext)
        cancelPolicySync(appContext)
        WorkManager.getInstance(appContext).apply {
            cancelUniqueWork("immediate_app_usage_report")
            cancelUniqueWork("app_usage_report")
            cancelUniqueWork("installed_apps_sync")
        }
        stopProtectionServices(appContext)
        PrefsHelper(appContext).clearPairingSession()
        return homeRoleHeld
    }

    fun requestUninstall(context: Context) {
        val appContext = context.applicationContext
        val adminComponent = ComponentName(appContext, LayngoDeviceAdminReceiver::class.java)
        val policyManager = appContext.getSystemService(DevicePolicyManager::class.java)
        runCatching {
            if (policyManager?.isAdminActive(adminComponent) == true) {
                policyManager.removeActiveAdmin(adminComponent)
            }
        }
        val uninstallIntent = Intent(
            Intent.ACTION_DELETE,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(uninstallIntent)
    }

    fun openHomeSettings(context: Context) {
        context.applicationContext.startActivity(
            Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun cancelPolicySync(context: Context) {
        RuleSyncWorker.cancel(context)
        ProtectedHomePolicySyncWorker.cancel(context)
        PhoneLocationPolicySyncWorker.cancel(context)
        PlacePolicySyncWorker.cancel(context)
        WebsitePolicySyncWorker.cancel(context)
        ContentBlurPolicySyncWorker.cancel(context)
        WorkManager.getInstance(context).apply {
            ScreenTimePolicySyncWorker.cancel(context)
            cancelUniqueWork(ProtectionHealthWorker.UNIQUE_PERIODIC_WORK_NAME)
        }
    }

    private fun stopProtectionServices(context: Context) {
        context.stopService(Intent(context, PhoneLocationService::class.java))
        context.stopService(Intent(context, WebsiteDnsVpnService::class.java))
    }
}
