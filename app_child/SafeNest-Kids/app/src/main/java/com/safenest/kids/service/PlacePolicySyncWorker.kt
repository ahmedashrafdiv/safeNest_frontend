package com.safenest.kids.service

import android.content.Context
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
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class PlacePolicySyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = PrefsHelper(applicationContext)
        if (!prefs.isPaired() || prefs.isProtectionSuspended()) return Result.success()
        ApiClient.init(applicationContext)
        return try {
            val response = ApiClient.apiService.getActivePlaces()
            when {
                response.isSuccessful && response.body() != null -> {
                    val policy = response.body()!!
                    if (policy.placeVersion == prefs.getPlacePolicyVersion() && prefs.getPlaceGeofenceStatus() == PrefsHelper.PLACE_GEOFENCE_ACTIVE) Result.success()
                    else when (apply(policy)) {
                        PlaceGeofenceManager.ApplyResult.ACTIVE -> Result.success()
                        PlaceGeofenceManager.ApplyResult.PERMISSION_DENIED -> Result.failure()
                        PlaceGeofenceManager.ApplyResult.TRANSIENT_FAILURE -> Result.retry()
                    }
                }
                response.code() == 401 || response.code() == 403 -> Result.failure()
                response.code() == 429 || response.code() >= 500 -> Result.retry()
                else -> Result.failure()
            }
        } catch (_: Exception) { Result.retry() }
    }

    private suspend fun apply(policy: com.safenest.kids.network.ChildPlacesResponse): PlaceGeofenceManager.ApplyResult = suspendCancellableCoroutine { continuation ->
        PlaceGeofenceManager.apply(applicationContext, policy) { result -> if (continuation.isActive) continuation.resume(result) }
    }

    companion object {
        private const val IMMEDIATE = "immediate_place_policy_sync"
        private const val PERIODIC = "place_policy_sync"
        fun enqueueImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<PlacePolicySyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(IMMEDIATE, ExistingWorkPolicy.REPLACE, request)
        }
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<PlacePolicySyncWorker>(15, TimeUnit.MINUTES).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }
        fun cancel(context: Context) { WorkManager.getInstance(context.applicationContext).apply { cancelUniqueWork(IMMEDIATE); cancelUniqueWork(PERIODIC) } }
    }
}
