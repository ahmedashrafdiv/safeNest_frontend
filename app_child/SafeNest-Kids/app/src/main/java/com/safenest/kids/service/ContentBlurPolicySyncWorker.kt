package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PrefsHelper
import java.util.concurrent.TimeUnit

class ContentBlurPolicySyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    private val prefs = PrefsHelper(applicationContext)

    override suspend fun doWork(): Result {
        if (prefs.isProtectionSuspended()) return Result.success()
        ApiClient.init(applicationContext)
        val deviceId = prefs.getDeviceId()
        val childId = prefs.getChildId() ?: return Result.failure()
        return try {
            val response = ApiClient.apiService.getEffectiveContentBlurPolicy()
            if (!response.isSuccessful) {
                return if (response.code() == 408 || response.code() == 429 || response.code() in 500..599) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            val policy = response.body() ?: return Result.retry()
            val decision = DeviceBindingDecider.decide(
                localDeviceId = deviceId,
                responseDeviceId = policy.deviceId,
                localChildId = childId,
                responseChildId = policy.childId,
                currentPolicyVersion = prefs.getContentBlurPolicyVersion(),
                incomingPolicyVersion = policy.policyVersion,
            )
            when (decision) {
                DeviceBindingDecider.Decision.APPLY -> {
                    prefs.setContentBlurPolicy(
                        enabled = policy.values.enabled,
                        mode = policy.values.mode,
                        targetPackages = policy.values.targetPackages.toSet(),
                        policyVersion = policy.policyVersion,
                    )
                    Log.i(TAG, "Stored newer Content Blur policy version ${policy.policyVersion}")
                }
                DeviceBindingDecider.Decision.CURRENT_POLICY -> {
                    Log.d(TAG, "Content Blur policy is already current at version ${policy.policyVersion}")
                }
                DeviceBindingDecider.Decision.STALE_POLICY -> {
                    Log.w(TAG, "Ignored stale Content Blur policy version ${policy.policyVersion}")
                }
                else -> {
                    prefs.clearContentBlurPolicy()
                    Log.w(TAG, "Rejected Content Blur policy and cleared local state: $decision")
                }
            }
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Content Blur policy sync failed", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ContentBlurPolicySync"
        private const val IMMEDIATE_WORK_NAME = "immediate_content_blur_policy_sync"
        private const val PERIODIC_WORK_NAME = "content_blur_policy_sync"

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<ContentBlurPolicySyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ContentBlurPolicySyncWorker>(15, TimeUnit.MINUTES)
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
