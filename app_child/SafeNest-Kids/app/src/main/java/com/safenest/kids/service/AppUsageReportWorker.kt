package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.AppUsageRequest
import com.safenest.kids.util.AppUsageHelper
import com.safenest.kids.util.PrefsHelper

class AppUsageReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AppUsageReport"
    }

    override suspend fun doWork(): Result {
        val prefsHelper = PrefsHelper(applicationContext)
        val childId = prefsHelper.getChildId()

        if (childId == null) {
            Log.w(TAG, "No child_id found — skipping usage report.")
            return Result.success()
        }

        val usageMap = AppUsageHelper.getTodayUsageStats(applicationContext)

        if (usageMap.isEmpty()) {
            Log.d(TAG, "No usage to report")
            return Result.success()
        }

        Log.d(TAG, "=== RAW USAGE MAP ===")
        usageMap.entries.sortedByDescending { it.value }.take(10).forEach { (pkg, mins) ->
            Log.d(TAG, "  $pkg → $mins min")
        }
        Log.d(TAG, "=== TOTAL APPS: ${usageMap.size} ===")
        Log.d(TAG, "Sending cumulative: $usageMap")

        return try {
            val request = AppUsageRequest(childId = childId, usage = usageMap)
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
