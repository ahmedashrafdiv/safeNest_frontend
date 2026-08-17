package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.InstalledApp
import com.safenest.kids.network.InstalledAppsRequest
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PrefsHelper
import java.util.concurrent.TimeUnit

class InstalledAppsSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "InstalledAppsSync"
        const val UNIQUE_WORK_NAME = "installed_apps_sync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<InstalledAppsSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    androidx.work.BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val prefs = PrefsHelper(applicationContext)
        val childId = prefs.getChildId()
        if (!prefs.isPaired() || childId.isNullOrBlank()) {
            Log.d(TAG, "Skipping inventory sync because the Child is not paired")
            return Result.success()
        }

        val apps = InstalledAppsHelper.getInstalledApps(applicationContext)
        val fingerprint = InstalledAppsHelper.fingerprint(apps)
        if (fingerprint == prefs.getInstalledAppsFingerprint()) {
            Log.d(TAG, "Installed-app inventory unchanged; skipping upload")
            return Result.success()
        }

        return try {
            val request = InstalledAppsRequest(
                apps.map { (packageName, appName) -> InstalledApp(packageName, appName) }
            )
            val response = ApiClient.apiService.updateInstalledApps(childId, request)
            if (response.isSuccessful) {
                prefs.setInstalledAppsFingerprint(fingerprint)
                prefs.setLastAppsSent(true)
                Log.d(TAG, "Uploaded ${apps.size} installed apps")
                Result.success()
            } else {
                Log.w(TAG, "Installed-app upload failed: ${response.code()}")
                Result.retry()
            }
        } catch (error: Exception) {
            Log.e(TAG, "Installed-app upload failed", error)
            Result.retry()
        }
    }
}
