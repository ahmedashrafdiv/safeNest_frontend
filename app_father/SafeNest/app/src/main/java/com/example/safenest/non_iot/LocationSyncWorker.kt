package com.example.safenest

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * No-op replacement used while the IoT location integration is disabled.
 *
 * The original IoT-linked worker is excluded from the current Parent build.
 * Keeping the same class name and WorkManager contract allows existing app
 * lifecycle code to compile without contacting or modifying the IoT backend.
 */
class LocationSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val WORK_NAME = "LocationSyncWork"
    }

    override suspend fun doWork(): Result = Result.success()
}
