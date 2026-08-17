package com.safenest.kids.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import com.safenest.kids.R
import com.safenest.kids.service.RuleSyncWorker
import com.safenest.kids.service.InstalledAppsSyncWorker

/**
 * Fires on BOOT_COMPLETED and MY_PACKAGE_REPLACED to check whether
 * the accessibility service is still enabled.  If not, it posts a
 * high-priority notification prompting the user to re-enable it.
 */
class ServiceWatchdogReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ServiceWatchdog"
        private const val CHANNEL_ID = "safenest_system"
        private const val NOTIFICATION_ID = 9999
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isPackageChange = action == Intent.ACTION_PACKAGE_ADDED ||
            action == Intent.ACTION_PACKAGE_REMOVED ||
            action == Intent.ACTION_PACKAGE_REPLACED
        val isLifecycleRecovery = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (!isPackageChange && !isLifecycleRecovery) return

        Log.d(TAG, "Received $action. Triggering synchronization.")
        val prefsHelper = PrefsHelper(context)
        if (!prefsHelper.isPaired()) return

        // Rule and inventory synchronization are both durable WorkManager jobs.
        triggerImmediateSync(context)
        InstalledAppsSyncWorker.enqueue(context)

        // Accessibility recovery notifications are relevant only after boot/app replacement,
        // not after every unrelated application install or removal.
        if (isLifecycleRecovery) {
            val enabled = PermissionsHelper.hasAccessibilityService(context)
            if (!enabled) {
                Log.w(TAG, "Accessibility service not enabled — showing notification")
                showReEnableNotification(context)
            } else {
                Log.d(TAG, "Accessibility service still enabled")
            }
        }
    }

    private fun triggerImmediateSync(context: Context) {
        val syncRequest = OneTimeWorkRequestBuilder<RuleSyncWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "boot_rule_sync",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun showReEnableNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the notification channel (safe to call repeatedly)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SafeNest System",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "System-level alerts for SafeNest Kids"
            }
            nm.createNotificationChannel(channel)
        }

        // Tapping the notification opens Accessibility Settings
        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SafeNest — إعادة تفعيل مطلوبة")
            .setContentText("يرجى إعادة تفعيل خدمة إمكانية الوصول للحفاظ على الحماية")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }
}
