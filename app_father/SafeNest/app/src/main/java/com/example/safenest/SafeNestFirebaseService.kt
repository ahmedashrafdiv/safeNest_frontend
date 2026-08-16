package com.example.safenest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.safenest.network.ApiClient
import com.example.safenest.network.FCMTokenUpdateRequest
import com.example.safenest.util.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SafeNestFirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SafeNestFCM"
        private const val CHANNEL_ID = "safenest_alerts"
        private const val CHANNEL_NAME = "SafeNest Alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        pushTokenToServer(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "SafeNest"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        Log.d(TAG, "Title: $title, Body: $body")

        if (body.isNotEmpty()) {
            showLocalNotification(title, body)
        }
    }

    private fun pushTokenToServer(token: String) {
        val sessionManager = SessionManager(applicationContext)
        if (!sessionManager.isLoggedIn()) {
            Log.d(TAG, "Not logged in — skipping FCM token push")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.apiService.updateFcmToken(FCMTokenUpdateRequest(fcmToken = token))
                if (response.isSuccessful) {
                    Log.d(TAG, "FCM token pushed successfully")
                } else {
                    Log.w(TAG, "FCM token push failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pushing FCM token: ${e.message}", e)
            }
        }
    }

    private fun showLocalNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "SafeNest safety alerts"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        Log.d(TAG, "Notification displayed: $title")
    }
}
