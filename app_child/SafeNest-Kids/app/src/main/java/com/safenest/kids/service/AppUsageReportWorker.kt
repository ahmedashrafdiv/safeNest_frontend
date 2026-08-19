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
import com.safenest.kids.network.AppUsageRequest
import com.safenest.kids.util.AppUsageHelper
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.util.UsageSnapshotMetadataFactory
import java.util.concurrent.TimeUnit

class AppUsageReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AppUsageReport"
        private const val IMMEDIATE_WORK_NAME = "immediate_app_usage_report"
        private const val PERIODIC_WORK_NAME = "app_usage_report"

        /** A freshly opened Home screen must not wait for the periodic interval before the Parent sees today's usage. */
        fun enqueueImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<AppUsageReportWorker>()
                .setConstraints(constraints)
                .addTag(IMMEDIATE_WORK_NAME)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        /**
         * 15 minutes is the shortest interval Android's WorkManager allows for periodic
         * background work. UPDATE (not KEEP) is required here so devices that already
         * registered the previous 1-hour schedule pick up the corrected interval without
         * a reinstall.
         */
        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<AppUsageReportWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    override suspend fun doWork(): Result {
        val prefsHelper = PrefsHelper(applicationContext)
        val childId = prefsHelper.getChildId()

        if (childId == null) {
            Log.w(TAG, "No child_id found — skipping usage report.")
            return Result.success()
        }

        val usageMap = AppUsageHelper.getTodayUsageStats(applicationContext)

        // An empty map is still reported (as a zero-usage snapshot) rather than skipped:
        // the Backend's "current day" marker only advances when a snapshot arrives, so
        // skipping here left the Parent looking at yesterday's stale day marker for as
        // long as no single app had accumulated a full tracked minute after midnight.
        if (usageMap.isEmpty()) {
            Log.d(TAG, "No usage yet today — reporting an empty snapshot to advance the day marker")
        }

        Log.d(TAG, "=== RAW USAGE MAP ===")
        usageMap.entries.sortedByDescending { it.value }.take(10).forEach { (pkg, mins) ->
            Log.d(TAG, "  $pkg → $mins min")
        }
        Log.d(TAG, "=== TOTAL APPS: ${usageMap.size} ===")
        val metadata = UsageSnapshotMetadataFactory.forZone()
        Log.d(TAG, "Sending daily snapshot for ${metadata.usageDay} in ${metadata.usageTimezone}: $usageMap")

        return try {
            val request = AppUsageRequest(
                childId = childId,
                usage = usageMap,
                usageDay = metadata.usageDay,
                usageTimezone = metadata.usageTimezone
            )
            val response = ApiClient.apiService.reportAppUsage(request)
            if (response.isSuccessful) {
                Log.d(TAG, "Reported ${usageMap.size} apps")
                Result.success()
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Report failed: ${response.code()} — $errorBody")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Report error", e)
            Result.retry()
        }
    }
}
