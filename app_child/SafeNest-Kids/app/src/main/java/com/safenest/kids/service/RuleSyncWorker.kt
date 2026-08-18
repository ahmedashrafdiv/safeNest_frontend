package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PrefsHelper
import org.json.JSONObject

class RuleSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    private val prefsHelper = PrefsHelper(applicationContext)

    override suspend fun doWork(): Result {
        val deviceId = prefsHelper.getDeviceId()
            ?: return Result.failure().also {
                Log.w(TAG, "Policy sync rejected: no local device binding")
            }
        val childId = prefsHelper.getChildId()

        return try {
            Log.i(TAG, "Policy sync started for bound child device")
            val response = ApiClient.apiService.getEffectiveAppBlockingPolicy()
            if (!response.isSuccessful) {
                val code = response.code()
                Log.w(TAG, "Policy sync rejected by backend: HTTP $code")
                return when {
                    shouldRetryHttpStatus(code) -> Result.retry()
                    else -> Result.failure()
                }
            }

            val policy = response.body()
                ?: return Result.retry().also {
                    Log.w(TAG, "Policy sync received an empty successful response")
                }
            val binding = DeviceBindingDecider.decide(
                localDeviceId = deviceId,
                responseDeviceId = policy.deviceId,
                localChildId = childId,
                responseChildId = policy.childId,
                currentPolicyVersion = prefsHelper.getAppPolicyVersion(),
                incomingPolicyVersion = policy.policyVersion,
            )
            if (binding != DeviceBindingDecider.Decision.APPLY) {
                Log.w(TAG, "Policy sync rejected by binding validation: $binding")
                return Result.success()
            }

            prefsHelper.setAppPolicy(
                mode = policy.values.appControlMode,
                allowedApps = policy.values.allowedApp.toSet(),
                blockedApps = policy.values.blockedApp.toSet(),
                limitsJson = JSONObject(policy.values.appTimeLimits).toString(),
                policyVersion = policy.policyVersion,
            )
            Log.i(TAG, "Policy sync stored a newer device-bound App Blocking snapshot")
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Policy sync failed before a valid snapshot could be stored", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RuleSyncWorker"
        private const val IMMEDIATE_WORK_NAME = "immediate_app_blocking_policy_sync"

        internal fun shouldRetryHttpStatus(code: Int): Boolean =
            code == 408 || code == 429 || code in 500..599

        /** Schedules an authenticated one-time read; binding validation still controls persistence. */
        fun enqueueImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<RuleSyncWorker>()
                .setConstraints(constraints)
                .addTag(IMMEDIATE_WORK_NAME)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
