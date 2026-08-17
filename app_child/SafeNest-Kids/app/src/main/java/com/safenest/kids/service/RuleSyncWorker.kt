package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PrefsHelper
import org.json.JSONObject

class RuleSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "RuleSyncWorker"
    }

    override suspend fun doWork(): Result {
        val prefsHelper = PrefsHelper(applicationContext)

        return try {
            val response = ApiClient.apiService.getDeviceRules()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val blockedApps = body.blockedApp?.toSet() ?: emptySet()
                    val allowedApps = body.allowedApp?.toSet() ?: emptySet()
                    val mode = if (body.appControlMode == "allowlist") "allowlist" else "blocklist"
                    val limitsJson = JSONObject(body.appTimeLimits ?: emptyMap<String, Int>()).toString()
                    prefsHelper.setAppPolicy(mode, allowedApps, blockedApps, limitsJson)

                    val now = System.currentTimeMillis()
                    Log.d(TAG, "Rule sync successful at $now. Mode: $mode, allowed: ${allowedApps.size}, blocked: ${blockedApps.size}, time limits: $limitsJson")
                }
                Result.success()
            } else {
                Log.e(TAG, "Rule sync failed: ${response.code()} ${response.message()}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Rule sync error", e)
            Result.retry()
        }
    }
}
