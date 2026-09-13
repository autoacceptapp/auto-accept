package com.example

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ServiceStatusNotificationManager
 *
 * Manages a persistent status bar notification that shows the real-time state of both:
 * 1. AccessibilityService (AutoAcceptService) - Active / Inactive
 * 2. NotificationListenerService (RideNotificationService) - Active / Inactive
 *
 * Provides status updates, one-tap quick action intents to settings or logs,
 * and seamlessly synchronizes with foreground service lifecycle.
 */
object ServiceStatusNotificationManager {

    private const val TAG = "ServiceStatusNotifMgr"

    const val CHANNEL_ID = "ride_strike_service_status_channel"
    const val CHANNEL_NAME = "RideStrike Service Status"
    const val CHANNEL_DESCRIPTION = "Displays real-time status of Accessibility and Notification Listener services"
    const val NOTIFICATION_ID = 1001

    data class ServicesState(
        val isAccessibilityActive: Boolean = false,
        val isNotificationListenerActive: Boolean = false,
        val isMasterSwitchOn: Boolean = true,
        val lastUpdatedMillis: Long = System.currentTimeMillis()
    )

    private val _servicesState = MutableStateFlow(ServicesState())
    val servicesState: StateFlow<ServicesState> = _servicesState.asStateFlow()

    /**
     * Checks whether AccessibilityService is currently active (either running in memory
     * or enabled in system accessibility settings).
     */
    fun isAccessibilityActive(context: Context): Boolean {
        val isRunning = AutoAcceptService.isServiceRunning.value
        val isEnabled = isAccessibilityServiceEnabled(context, AutoAcceptService::class.java)
        return isRunning || isEnabled
    }

    /**
     * Checks whether NotificationListenerService is currently active (either connected
     * in memory or granted access in system notification settings).
     */
    fun isNotificationListenerActive(context: Context): Boolean {
        val isConnected = RideNotificationService.isConnected()
        val isGranted = isNotificationListenerEnabled(context)
        return isConnected || isGranted
    }

    /**
     * Creates or verifies the NotificationChannel for service status monitoring.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Builds the persistent status bar notification reflecting the current states.
     */
    fun buildNotification(context: Context): Notification {
        createNotificationChannel(context)

        val accActive = isAccessibilityActive(context)
        val notifActive = isNotificationListenerActive(context)
        val masterOn = AutoAcceptService.isAutomationEnabled(context)
        val delayMs = AutoAcceptService.getAcceptDelayMs(context)

        _servicesState.value = ServicesState(
            isAccessibilityActive = accActive,
            isNotificationListenerActive = notifActive,
            isMasterSwitchOn = masterOn,
            lastUpdatedMillis = System.currentTimeMillis()
        )

        // Title and summary based on aggregate health
        val title: String
        val colorInt: Int
        if (accActive && notifActive) {
            title = "RideStrike Monitor: Services Active 🟢"
            colorInt = 0xFF10B981.toInt() // Emerald Green
        } else if (accActive && !notifActive) {
            title = "RideStrike: Notification Access Needed ⚠️"
            colorInt = 0xFFF59E0B.toInt() // Amber
        } else if (!accActive && notifActive) {
            title = "RideStrike: Accessibility Service Inactive ⚠️"
            colorInt = 0xFFF59E0B.toInt() // Amber
        } else {
            title = "RideStrike: Services Inactive 🔴"
            colorInt = 0xFFF43F5E.toInt() // Rose
        }

        val accLabel = if (accActive) "Active" else "Inactive"
        val notifLabel = if (notifActive) "Active" else "Inactive"
        val shortContent = "Accessibility: $accLabel • Notification: $notifLabel"

        // Expanded multi-line view
        val bigText = buildString {
            append("• Accessibility Service: ").append(if (accActive) "Active (Scanning & Clicking)" else "Inactive (Needs Permission)")
            append("\n• Notification Listener: ").append(if (notifActive) "Active (Detecting Orders)" else "Inactive (Needs Permission)")
            append("\n• Master Auto-Accept: ").append(if (masterOn) "ON" else "OFF (Paused)")
            append(" • Delay: ").append(delayMs).append("ms")
        }

        // Tap content -> Open MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            context,
            100,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(shortContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(colorInt)
            .setColorized(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(appPendingIntent)

        // Action 1: If Accessibility is inactive, provide a quick one-tap setup button
        if (!accActive) {
            val accIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val accPendingIntent = PendingIntent.getActivity(
                context,
                101,
                accIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                "Enable Accessibility",
                accPendingIntent
            )
        }

        // Action 2: If Notification Listener is inactive, provide quick setup button
        if (!notifActive) {
            val notifIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val notifPendingIntent = PendingIntent.getActivity(
                context,
                102,
                notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_dialog_info,
                "Enable Notification Access",
                notifPendingIntent
            )
        }

        // If both services are active, provide a direct shortcut to the Debug Logs tab
        if (accActive && notifActive) {
            val logsIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("TARGET_TAB", 3)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val logsPendingIntent = PendingIntent.getActivity(
                context,
                103,
                logsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_info_details,
                "View Logs",
                logsPendingIntent
            )
        }

        return builder.build()
    }

    /**
     * Posts or updates the status bar notification.
     * Can be invoked from MainActivity, BootReceiver, AutoAcceptService, or RideNotificationService.
     */
    fun updateStatus(context: Context) {
        try {
            // Guard for Android 13+ (API 33) POST_NOTIFICATIONS permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "Cannot post status notification: POST_NOTIFICATIONS not granted")
                    return
                }
            }

            val notification = buildNotification(context)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            Log.d(TAG, "Service status notification updated.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update service status notification: ${e.message}")
        }
    }

    /**
     * Starts or updates foreground service using the unified status notification.
     */
    fun startOrUpdateForeground(service: AutoAcceptService) {
        try {
            val notification = buildNotification(service)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    service.startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } catch (e: Throwable) {
                    service.startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                service.startForeground(NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "AutoAcceptService foreground status notification started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground status notification: ${e.message}", e)
        }
    }
}
