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
import com.safenest.kids.util.PrefsHelper
import java.util.concurrent.TimeUnit

/** Synchronizes only the bound device’s Parent request; Android HOME-role state is never inferred from it. */
class ProtectedHomePolicySyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    private val prefs = PrefsHelper(applicationContext)

    override suspend fun doWork(): Result {
        if (prefs.isProtectionSuspended()) return Result.success()
        val deviceId = prefs.getDeviceId()
        val childId = prefs.getChildId()
        return try {
            val response = ApiClient.apiService.getEffectiveProtectionPolicy()
            if (!response.isSuccessful) {
                return if (shouldRetryHttpStatus(response.code())) Result.retry() else Result.failure()
            }
            val policy = response.body() ?: return Result.retry()
            when (DeviceBindingDecider.decide(
                localDeviceId = deviceId,
                localChildId = childId,
                responseDeviceId = policy.deviceId,
                responseChildId = policy.childId,
                currentPolicyVersion = prefs.getProtectedHomePolicyVersion(),
                incomingPolicyVersion = policy.policyVersion,
            )) {
                DeviceBindingDecider.Decision.APPLY -> {
                    prefs.setProtectedHomePolicy(
                        requested = policy.values.protectedHomeRequested,
                        policyVersion = policy.policyVersion,
                    )
                    Log.i(TAG, "Stored device-bound Protected Home request")
                }
                else -> Log.i(TAG, "Ignored Protected Home policy because it was not a newer bound snapshot")
            }
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Protected Home policy sync failed", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ProtectedHomePolicySync"
        private const val IMMEDIATE_WORK_NAME = "immediate_protected_home_policy_sync"
        private const val PERIODIC_WORK_NAME = "protected_home_policy_sync"

        internal fun shouldRetryHttpStatus(code: Int): Boolean =
            code == 408 || code == 429 || code in 500..599

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<ProtectedHomePolicySyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .addTag(IMMEDIATE_WORK_NAME)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ProtectedHomePolicySyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).apply {
                cancelUniqueWork(IMMEDIATE_WORK_NAME)
                cancelUniqueWork(PERIODIC_WORK_NAME)
            }
        }
    }
}
