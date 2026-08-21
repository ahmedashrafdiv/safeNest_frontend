package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.DeviceProtectionHealthRequest
import com.safenest.kids.security.DeviceManagementHelper
import com.safenest.kids.security.ProtectionHealthDecider
import com.safenest.kids.security.SetupCapability
import com.safenest.kids.security.SetupCapabilityEvaluator
import com.safenest.kids.security.SetupCapabilityProvider
import com.safenest.kids.security.SetupCapabilityStatus
import com.safenest.kids.util.PrefsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Reports the measured Android management capability for this bound device only. */
class ProtectionHealthWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    private val prefs = PrefsHelper(applicationContext)

    override suspend fun doWork(): Result {
        val deviceId = prefs.getDeviceId()
            ?: return Result.failure().also { Log.w(TAG, "Protection health rejected: no device binding") }
        val state = DeviceManagementHelper.read(applicationContext)
        val health = ProtectionHealthDecider.from(state)
        val setupSnapshot = SetupCapabilityProvider.read(applicationContext, prefs)
        val setupReadiness = SetupCapabilityEvaluator.evaluate(setupSnapshot)
        val capabilityStates = setupReadiness.requiredStates + setupReadiness.optionalStates
        val permissionStates = capabilityStates
            .mapKeys { (capability, _) -> capability.healthKey() }
            .mapValues { (_, capabilityState) -> capabilityState.healthValue() }
            .toMutableMap()
            .apply {
                this["protected_home"] = when {
                    !setupSnapshot.protectedHomeRoleAvailable -> "unknown"
                    setupSnapshot.protectedHomeRoleHeld -> "granted"
                    else -> "denied"
                }
            }
        val activeCapabilities = buildList {
            add("removal_protection_warning")
            if (capabilityStates[SetupCapability.ACCESSIBILITY] == SetupCapabilityStatus.READY) add("app_blocking_accessibility")
            if (capabilityStates[SetupCapability.CONTENT_BLUR_ACCESSIBILITY] == SetupCapabilityStatus.READY) {
                add("content_blur_accessibility")
            }
            if (setupSnapshot.protectedHomeRoleHeld) add("protected_home")
            if (capabilityStates[SetupCapability.DEVICE_ADMIN] == SetupCapabilityStatus.READY) add("device_admin")
            if (capabilityStates[SetupCapability.OVERLAY] == SetupCapabilityStatus.READY) add("blocking_overlay")
        }
        val request = DeviceProtectionHealthRequest(
            permissionStates = permissionStates,
            capabilities = activeCapabilities,
            reportedAt = utcNow(),
            protectionHealth = health.name.lowercase(),
            protectionMode = state.mode.name.lowercase(),
            managementAuthorityConfirmed = state.managementAuthorityConfirmed,
            uninstallProtectionConfirmed = state.uninstallProtectionConfirmed,
            lockTaskAvailable = state.lockTaskAvailable,
        )

        return try {
            val response = ApiClient.apiService.reportProtectionHealth(deviceId, request)
            when {
                response.isSuccessful -> Result.success()
                response.code() == 408 || response.code() == 429 || response.code() in 500..599 -> Result.retry()
                else -> Result.failure().also { Log.w(TAG, "Protection health rejected: HTTP ${response.code()}") }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Protection health report failed", error)
            Result.retry()
        }
    }

    private fun utcNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

    private fun SetupCapability.healthKey(): String = when (this) {
        SetupCapability.DEVICE_ADMIN -> "device_admin"
        SetupCapability.PROTECTED_HOME -> "protected_home"
        SetupCapability.OVERLAY -> "overlay"
        SetupCapability.USAGE_ACCESS -> "usage_access"
        SetupCapability.ACCESSIBILITY -> "accessibility"
        SetupCapability.CONTENT_BLUR_ACCESSIBILITY -> "content_blur_accessibility"
        SetupCapability.BACKGROUND_RELIABILITY -> "battery_optimization"
        SetupCapability.WEBSITE_PROTECTION -> "website_protection"
        SetupCapability.LOCATION_MONITORING -> "location_monitoring"
    }

    private fun SetupCapabilityStatus.healthValue(): String = when (this) {
        SetupCapabilityStatus.READY -> "granted"
        SetupCapabilityStatus.REQUIRES_ACTION -> "denied"
        SetupCapabilityStatus.UNAVAILABLE, SetupCapabilityStatus.NOT_REQUIRED -> "unknown"
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK_NAME = "protection_health_periodic"
        private const val UNIQUE_IMMEDIATE_WORK_NAME = "protection_health_immediate"
        private const val TAG = "ProtectionHealthWorker"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ProtectionHealthWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<ProtectionHealthWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
