package com.safenest.kids.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.WebsitePolicyAckRequest
import com.safenest.kids.network.WebsitePolicySyncResponse
import com.safenest.kids.util.PrefsHelper

class WebsitePolicySyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    private val prefs = PrefsHelper(appContext)

    override suspend fun doWork(): Result {
        val deviceId = prefs.getDeviceId() ?: return Result.retry()
        return try {
            val assignmentsResponse = ApiClient.apiService.listWebsiteAssignments(deviceId)
            if (!assignmentsResponse.isSuccessful) return if (assignmentsResponse.code() == 404) Result.success() else Result.retry()
            val assignment = assignmentsResponse.body()?.items?.firstOrNull() ?: return Result.success()
            prefs.setWebsitePolicyId(assignment.policyId)

            val snapshotResponse = ApiClient.apiService.getWebsitePolicy(deviceId, assignment.policyId)
            if (!snapshotResponse.isSuccessful) return Result.retry()
            val snapshot = snapshotResponse.body() ?: return Result.retry()
            val binding = DeviceBindingDecider.decide(
                localDeviceId = deviceId,
                localChildId = prefs.getChildId(),
                responseDeviceId = snapshot.deviceId,
                responseChildId = snapshot.childId,
                currentPolicyVersion = prefs.getWebsitePolicyVersion(),
                incomingPolicyVersion = snapshot.version
            )
            if (binding != DeviceBindingDecider.Decision.APPLY) {
                Log.w(TAG, "Rejecting website policy snapshot: $binding")
                return if (binding == DeviceBindingDecider.Decision.STALE_POLICY) Result.success() else Result.retry()
            }
            val json = Gson().toJson(snapshot)
            prefs.setWebsitePolicySnapshot(json, snapshot.version, snapshot.contentHash)
            if (!WebsiteDnsVpnService.startIfPermissionGranted(applicationContext)) {
                prefs.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_DENIED)
                Log.w(TAG, "Website policy synced but VPN consent is not available")
            }

            val ackResponse = ApiClient.apiService.acknowledgeWebsitePolicy(
                deviceId,
                assignment.policyId,
                WebsitePolicyAckRequest(
                    version = snapshot.version,
                    contentHash = snapshot.contentHash,
                    agentVersion = "1.0",
                    capabilities = mapOf(
                        "vpn_host_filtering" to true,
                        "category_filtering" to true,
                        "path_filtering" to false,
                        "exact_browser_time" to false
                    )
                )
            )
            if (!ackResponse.isSuccessful) return Result.retry()
            Log.i(TAG, "Website policy synchronized: ${snapshot.policyId} v${snapshot.version}")
            Result.success()
        } catch (error: Exception) {
            Log.e(TAG, "Website policy sync failed", error)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "WebsitePolicySyncWorker"
        const val UNIQUE_WORK_NAME = "website_policy_sync"
    }
}
