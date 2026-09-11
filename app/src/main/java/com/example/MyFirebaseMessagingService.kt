package com.example

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCM_Service"
        const val CHANNEL_ID = "high_importance_channel"
        const val CHANNEL_NAME = "High Importance Notifications"
        const val CHANNEL_DESC = "Notifications for alerts and system updates"
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token: $token")
        val prefs = getSharedPreferences(AutoAcceptService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_registration_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Extract title and body from remoteMessage.notification OR remoteMessage.data
        val notifTitle = remoteMessage.notification?.title
        val notifBody = remoteMessage.notification?.body

        val dataTitle = remoteMessage.data["title"]
        val dataBody = remoteMessage.data["body"] ?: remoteMessage.data["message"]

        val title = notifTitle?.takeIf { it.isNotBlank() }
            ?: dataTitle?.takeIf { it.isNotBlank() }
            ?: getString(R.string.app_name)

        val body = notifBody?.takeIf { it.isNotBlank() }
            ?: dataBody?.takeIf { it.isNotBlank() }
            ?: "You have a new update."

        Log.d(TAG, "FCM Message to show - Title: $title, Body: $body")
        showNotification(title, body, remoteMessage.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        // Explicitly ensure the notification channel is created
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            (System.currentTimeMillis() % 1000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission is not granted; notification cannot be displayed.")
                return
            }
        }

        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        try {
            if (notificationManager != null) {
                notificationManager.notify(notificationId, notificationBuilder.build())
            } else {
                NotificationManagerCompat.from(this).notify(notificationId, notificationBuilder.build())
            }
            Log.d(TAG, "Notification successfully posted: ID $notificationId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while showing notification: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while showing notification: ${e.message}", e)
        }
    }
}
