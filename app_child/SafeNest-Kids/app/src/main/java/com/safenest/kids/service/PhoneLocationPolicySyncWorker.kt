package com.safenest.kids.service

import android.content.Context
import android.content.Intent
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

/** Applies only the authenticated Parent request for this Child device. */
class PhoneLocationPolicySyncWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val prefs = PrefsHelper(applicationContext)
        if (!prefs.isPaired()) return Result.success()
        ApiClient.init(applicationContext)

        return try {
            val response = ApiClient.apiService.getEffectivePhoneLocationPolicy()
            when {
                response.isSuccessful && response.body() != null -> {
                    val policy = response.body()!!
                    prefs.setPhoneTrackingEnabled(policy.enabled)
                    if (policy.enabled) {
                        PhoneLocationService.startIfPermissionGranted(applicationContext)
                    } else {
                        applicationContext.stopService(Intent(applicationContext, PhoneLocationService::class.java))
                        prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_DISABLED)
                    }
                    Result.success()
                }
                response.code() == 401 || response.code() == 403 -> Result.failure()
                response.code() == 429 || response.code() >= 500 -> Result.retry()
                else -> Result.failure()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val IMMEDIATE_WORK_NAME = "immediate_phone_location_policy_sync"
        private const val PERIODIC_WORK_NAME = "phone_location_policy_sync"

        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<PhoneLocationPolicySyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<PhoneLocationPolicySyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
