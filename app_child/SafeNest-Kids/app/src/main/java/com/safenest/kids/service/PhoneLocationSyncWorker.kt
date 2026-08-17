package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.PhoneLocationUploadRequest
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class PhoneLocationSyncWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        val prefs = PrefsHelper(applicationContext)
        val childId = prefs.getChildId()
        val deviceId = prefs.getDeviceId()
        if (!prefs.isPaired() || childId == null || deviceId.isBlank()) return Result.success()
        if (!prefs.isPhoneTrackingEnabled()) {
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_DISABLED)
            return Result.success()
        }
        if (!PermissionsHelper.hasLocationPermission(applicationContext)) {
            prefs.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_DENIED)
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_PERMISSION_DENIED)
            return Result.success()
        }

        val capturedAtMillis = inputData.getLong(KEY_CAPTURED_AT, 0L)
        val latitude = inputData.getDouble(KEY_LATITUDE, Double.NaN)
        val longitude = inputData.getDouble(KEY_LONGITUDE, Double.NaN)
        val accuracy = inputData.getFloat(KEY_ACCURACY, Float.NaN)
        if (capturedAtMillis <= 0L || latitude.isNaN() || longitude.isNaN() || accuracy.isNaN()) {
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_UNAVAILABLE)
            return Result.success()
        }

        val capturedAt = isoUtc(capturedAtMillis)
        val request = PhoneLocationUploadRequest(
            reportId = inputData.getString(KEY_REPORT_ID) ?: UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            altitudeMeters = inputData.getDouble(KEY_ALTITUDE, Double.NaN).takeUnless { it.isNaN() },
            speedMps = inputData.getFloat(KEY_SPEED, Float.NaN).takeUnless { it.isNaN() },
            capturedAt = capturedAt
        )

        return try {
            val response = ApiClient.apiService.uploadPhoneLocation(deviceId, request)
            when {
                response.isSuccessful -> {
                    prefs.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_GRANTED)
                    prefs.setPhoneTrackingNetworkState(PrefsHelper.PHONE_LOCATION_NETWORK_ONLINE)
                    prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_ACTIVE)
                    prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_ACTIVE)
                    prefs.setPhoneTrackingLastUploadAt(isoUtc(System.currentTimeMillis()))
                    prefs.setPhoneTrackingLastReportId(request.reportId)
                    prefs.setPhoneTrackingLastCapturedAt(capturedAt)
                    Result.success()
                }
                response.code() == 409 -> {
                    prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_DISABLED)
                    Result.success()
                }
                response.code() == 429 || response.code() >= 500 -> {
                    prefs.setPhoneTrackingNetworkState(PrefsHelper.PHONE_LOCATION_NETWORK_RETRYING)
                    prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_OFFLINE)
                    Result.retry()
                }
                else -> {
                    prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_UNAVAILABLE)
                    Result.failure()
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Phone location upload failed; WorkManager will retry", error)
            prefs.setPhoneTrackingNetworkState(PrefsHelper.PHONE_LOCATION_NETWORK_RETRYING)
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_OFFLINE)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PhoneLocationSync"
        private const val WORK_NAME = "phone_location_upload"
        const val KEY_REPORT_ID = "report_id"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
        const val KEY_ACCURACY = "accuracy_meters"
        const val KEY_ALTITUDE = "altitude_meters"
        const val KEY_SPEED = "speed_mps"
        const val KEY_CAPTURED_AT = "captured_at_millis"

        fun enqueue(context: Context, sample: PhoneLocationDecider.LocationSample) {
            val input = Data.Builder()
                .putString(KEY_REPORT_ID, UUID.randomUUID().toString())
                .putDouble(KEY_LATITUDE, sample.latitude)
                .putDouble(KEY_LONGITUDE, sample.longitude)
                .putFloat(KEY_ACCURACY, sample.accuracyMeters)
                .putLong(KEY_CAPTURED_AT, sample.capturedAtMillis)
                .apply {
                    sample.altitudeMeters?.let { putDouble(KEY_ALTITUDE, it) }
                    sample.speedMps?.let { putFloat(KEY_SPEED, it) }
                }
                .build()
            val request = OneTimeWorkRequestBuilder<PhoneLocationSyncWorker>()
                .setInputData(input)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        }

        private fun isoUtc(millis: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(millis))
    }
}
