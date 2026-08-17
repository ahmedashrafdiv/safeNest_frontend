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
    private val prefsHelper = PrefsHelper(applicationContext)

    override suspend fun doWork(): Result {
        val deviceId = prefsHelper.getDeviceId() ?: return Result.retry()
        val childId = prefsHelper.getChildId()

        return try {
            val response = ApiClient.apiService.getEffectiveAppBlockingPolicy()
            if (!response.isSuccessful) {
                return if (response.code() == 404) Result.success() else Result.retry()
            }

            val policy = response.body() ?: return Result.retry()
            val binding = DeviceBindingDecider.decide(
                localDeviceId = deviceId,
                localChildId = childId,
                responseDeviceId = policy.deviceId,
                responseChildId = policy.childId,
                currentPolicyVersion = prefsHelper.getAppPolicyVersion(),
                incomingPolicyVersion = policy.policyVersion,
            )
            if (binding != DeviceBindingDecider.Decision.APPLY) {
                Log.w(TAG, "Ignoring App Blocking policy response: $binding")
                return Result.success()
            }

            prefsHelper.setAppPolicy(
                mode = policy.values.appControlMode,
                allowedApps = policy.values.allowedApp.toSet(),
                blockedApps = policy.values.blockedApp.toSet(),
                limitsJson = JSONObject(policy.values.appTimeLimits).toString(),
                policyVersion = policy.policyVersion,
            )
            Log.i(TAG, "App Blocking policy synchronized v${policy.policyVersion}")
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "App Blocking policy sync failed", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "RuleSyncWorker"
    }
}
