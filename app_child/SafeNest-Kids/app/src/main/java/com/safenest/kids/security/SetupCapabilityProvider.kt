package com.safenest.kids.security

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper

internal object SetupCapabilityProvider {
    fun read(context: Context, prefs: PrefsHelper): SetupCapabilitySnapshot {
        val adminComponent = ComponentName(context, LayngoDeviceAdminReceiver::class.java)
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val homeRoleAvailable = ProtectedHomeRoleManager.isAvailable(context)
        return SetupCapabilitySnapshot(
            deviceAdminActive = dpm.isAdminActive(adminComponent),
            protectedHomeRequested = prefs.isProtectedHomeRequested(),
            protectedHomeRoleAvailable = homeRoleAvailable,
            protectedHomeRoleHeld = homeRoleAvailable && ProtectedHomeRoleManager.isActive(context),
            overlayGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context),
            usageAccessGranted = PermissionsHelper.hasUsageAccess(context),
            accessibilityEnabled = PermissionsHelper.hasAccessibilityService(context),
            batteryExemptionGranted = PermissionsHelper.hasBatteryOptimizationExemption(context),
            websiteProtectionRequired = prefs.getWebsitePolicySnapshotJson() != null,
            vpnGranted = PermissionsHelper.hasVpnPermission(context),
            locationMonitoringRequired = prefs.isPhoneTrackingEnabled(),
            locationGranted = PermissionsHelper.hasLocationPermission(context),
        )
    }
}
