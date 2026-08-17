package com.safenest.kids.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.UpdateFcmTokenRequest
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class SafeNestFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SafeNestFCM"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        val prefsHelper = PrefsHelper(applicationContext)
        if (!prefsHelper.isPaired()) {
            Log.d(TAG, "Device is not paired yet — skipping FCM token update (initial pairing will send it)")
            return
        }

        Log.d(TAG, "Device is paired — sending refreshed FCM token to backend")

        serviceScope.launch {
            try {
                // Ensure ApiClient is initialized (the app may have been
                // cold-started by the token refresh)
                ApiClient.init(applicationContext)

                val request = UpdateFcmTokenRequest(fcmToken = token)
                val response = ApiClient.apiService.updateDeviceFcmToken(request)
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token updated on backend successfully")
                } else {
                    Log.e(TAG, "FCM token update failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "FCM token update error", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived CALLED - data=${remoteMessage.data} notification=${remoteMessage.notification}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.e(TAG, "=== FCM RECEIVED ===")
            Log.e(TAG, "notification block present: ${remoteMessage.notification != null}")
            Log.e(TAG, "notification.title: ${remoteMessage.notification?.title}")
            Log.e(TAG, "data payload: ${remoteMessage.data}")
            Log.e(TAG, "message priority: ${remoteMessage.priority}")
            Log.e(TAG, "original priority: ${remoteMessage.originalPriority}")

            val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
            Log.d(TAG, "FCM received — title: $title")

            if (title == "RULES_UPDATED") {
                Log.d(TAG, "Parent updated rules — triggering immediate sync via WorkManager")
                triggerImmediateSync()
                triggerWebsitePolicySync()
                // Do NOT show any visible notification to the child
            }
        }
    }

    private fun triggerWebsitePolicySync() {
        val syncRequest = OneTimeWorkRequestBuilder<WebsitePolicySyncWorker>().build()
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            WebsitePolicySyncWorker.UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun triggerImmediateSync() {
        // Using WorkManager is much more reliable than a raw coroutine
        // because it survives process death and handles network retries.
        val syncRequest = OneTimeWorkRequestBuilder<RuleSyncWorker>()
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "immediate_rule_sync",
            ExistingWorkPolicy.REPLACE, // Replace any pending immediate sync
            syncRequest
        )
    }
}

