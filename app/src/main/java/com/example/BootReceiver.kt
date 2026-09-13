package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        const val CHANNEL_ID = "high_importance_channel"
        const val CHANNEL_NAME = "High Importance Notifications"
        const val NOTIFICATION_ID = 2001
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val isAutomationOn = AutoAcceptService.isAutomationEnabled(context)
            Log.d(TAG, "Device boot completed. Master Auto-Accept status: $isAutomationOn")

            ServiceStatusNotificationManager.updateStatus(context)

            if (isAutomationOn) {
                showBootActiveNotification(context)
            }
        }
    }

    private fun showBootActiveNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for alerts and system updates"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Rapido Auto Accept Active")
            .setContentText("Rapido Auto Accept is active and waiting for orders.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Rapido Auto Accept is active and waiting for orders.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } else {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "Boot active notification displayed successfully.")
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for boot notification: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display boot notification: ${e.message}", e)
        }
    }
}
