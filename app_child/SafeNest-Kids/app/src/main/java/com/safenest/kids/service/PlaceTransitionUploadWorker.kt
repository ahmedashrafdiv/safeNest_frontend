package com.safenest.kids.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.PlaceTransitionRequest
import com.safenest.kids.util.PrefsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class PlaceTransitionUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = PrefsHelper(applicationContext)
        if (!prefs.isPaired() || prefs.isProtectionSuspended()) return Result.success()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()
        val placeId = inputData.getString(KEY_PLACE_ID) ?: return Result.failure()
        val transition = inputData.getString(KEY_TRANSITION) ?: return Result.failure()
        val occurred = inputData.getLong(KEY_OCCURRED_AT, 0L)
        if (occurred <= 0L) return Result.failure()
        ApiClient.init(applicationContext)
        return try {
            val response = ApiClient.apiService.reportPlaceTransition(PlaceTransitionRequest(eventId, placeId, transition, isoUtc(occurred)))
            when {
                response.isSuccessful -> Result.success()
                response.code() == 401 || response.code() == 403 || response.code() == 404 -> Result.failure()
                response.code() == 429 || response.code() >= 500 -> Result.retry()
                else -> Result.failure()
            }
        } catch (_: Exception) { Result.retry() }
    }
    private fun isoUtc(value: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(value))
    companion object {
        private const val KEY_EVENT_ID = "event_id"; private const val KEY_PLACE_ID = "place_id"; private const val KEY_TRANSITION = "transition"; private const val KEY_OCCURRED_AT = "occurred_at"
        fun enqueue(context: Context, eventId: String, placeId: String, transition: String, occurredAt: Long) {
            val data = Data.Builder().putString(KEY_EVENT_ID, eventId).putString(KEY_PLACE_ID, placeId).putString(KEY_TRANSITION, transition).putLong(KEY_OCCURRED_AT, occurredAt).build()
            val request = OneTimeWorkRequestBuilder<PlaceTransitionUploadWorker>().setInputData(data).setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context.applicationContext).enqueue(request)
        }
    }
}
