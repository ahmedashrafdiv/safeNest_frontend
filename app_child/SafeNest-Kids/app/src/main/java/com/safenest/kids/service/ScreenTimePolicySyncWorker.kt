package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PrefsHelper
import java.util.concurrent.TimeUnit

class ScreenTimePolicySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val prefs = PrefsHelper(appContext)

    override suspend fun doWork(): Result {
        if (prefs.isProtectionSuspended()) return Result.success()
        val deviceId = prefs.getDeviceId()
        val childId = prefs.getChildId() ?: return Result.retry()
        return try {
            val response = ApiClient.apiService.getScreenTimePolicy(deviceId)
            if (!response.isSuccessful) {
                return if (response.code() == 404) Result.success() else Result.retry()
            }
            val sync = response.body() ?: return Result.retry()
            val policy = sync.policy
            if (policy.childId != childId || (policy.deviceId != null && policy.deviceId != deviceId)) {
                Log.w(TAG, "Ignoring Screen Time policy with invalid binding")
                return Result.success()
            }
            if (policy.version <= prefs.getDailyScreenTimePolicyVersion()) {
                return Result.success()
            }
            prefs.setDailyScreenTimePolicy(
                dailyLimitSeconds = policy.dailyLimitSeconds,
                policyVersion = policy.version,
            )
            Log.i(TAG, "Screen Time policy synchronized v${policy.version}")
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Screen Time policy sync failed", error)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "screen_time_policy_sync"
        private const val IMMEDIATE_WORK_NAME = "immediate_screen_time_policy_sync"
        private const val TAG = "ScreenTimePolicySync"

        fun enqueueImmediate(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ScreenTimePolicySyncWorker>().build(),
            )
        }

        fun enqueuePeriodic(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ScreenTimePolicySyncWorker>(15, TimeUnit.MINUTES).build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).apply {
                cancelUniqueWork(IMMEDIATE_WORK_NAME)
                cancelUniqueWork(UNIQUE_WORK_NAME)
            }
        }
    }
}
