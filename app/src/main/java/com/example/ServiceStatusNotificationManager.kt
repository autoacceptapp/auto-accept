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
        val isBatteryUnrestricted: Boolean = false,
        val isMasterSwitchOn: Boolean = true,
        val hasMissingCriticalPermissions: Boolean = false,
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
     * Checks whether Battery Optimization is ignored (Battery Unrestricted).
     */
    fun isBatteryUnrestricted(context: Context): Boolean {
        return isBatteryOptimizationIgnored(context)
    }

    /**
     * Returns true if ANY of the 3 critical permissions/services are missing or off:
     * 1. Accessibility Service
     * 2. Notification Listener Service
     * 3. Battery Unrestricted (Ignoring Battery Optimizations)
     */
    fun hasMissingCriticalPermissions(context: Context): Boolean {
        val acc = isAccessibilityActive(context)
        val notif = isNotificationListenerActive(context)
        val bat = isBatteryUnrestricted(context)
        return !acc || !notif || !bat
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
     * Builds the alert status bar notification reflecting missing permissions.
     */
    fun buildNotification(context: Context): Notification {
        createNotificationChannel(context)

        val accActive = isAccessibilityActive(context)
        val notifActive = isNotificationListenerActive(context)
        val batActive = isBatteryUnrestricted(context)
        val masterOn = AutoAcceptService.isAutomationEnabled(context)
        val hasMissing = !accActive || !notifActive || !batActive

        _servicesState.value = ServicesState(
            isAccessibilityActive = accActive,
            isNotificationListenerActive = notifActive,
            isBatteryUnrestricted = batActive,
            isMasterSwitchOn = masterOn,
            hasMissingCriticalPermissions = hasMissing,
            lastUpdatedMillis = System.currentTimeMillis()
        )

        // Title and summary based on missing requirements
        val title: String
        val colorInt: Int
        if (!accActive && !notifActive) {
            title = "RideStrike: Services Inactive 🔴"
            colorInt = 0xFFF43F5E.toInt() // Rose
        } else if (!accActive) {
            title = "RideStrike: Accessibility Inactive ⚠️"
            colorInt = 0xFFF59E0B.toInt() // Amber
        } else if (!notifActive) {
            title = "RideStrike: Notification Access Needed ⚠️"
            colorInt = 0xFFF59E0B.toInt() // Amber
        } else if (!batActive) {
            title = "RideStrike: Battery Restricted ⚠️"
            colorInt = 0xFFF59E0B.toInt() // Amber
        } else {
            title = "RideStrike Monitor: Services Active 🟢"
            colorInt = 0xFF10B981.toInt() // Emerald Green
        }

        val missingList = mutableListOf<String>()
        if (!accActive) missingList.add("Accessibility")
        if (!notifActive) missingList.add("Notification")
        if (!batActive) missingList.add("Battery")

        val shortContent = if (missingList.isNotEmpty()) {
            "Missing: ${missingList.joinToString(", ")}"
        } else {
            "All services active and ready"
        }

        // Expanded multi-line view
        val bigText = buildString {
            append("Critical permissions status:\n")
            append("• Accessibility: ").append(if (accActive) "Active 🟢" else "DISABLED 🔴 (Required to accept rides)")
            append("\n• Notification Access: ").append(if (notifActive) "Active 🟢" else "DISABLED 🔴 (Required to detect rides)")
            append("\n• Battery Optimization: ").append(if (batActive) "Unrestricted 🟢" else "RESTRICTED ⚠️ (Service may be killed)")
        }

        // Tap content -> Open MainActivity on Settings Tab (Tab 4)
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("TARGET_TAB", 4)
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
            .setOngoing(hasMissing)
            .setAutoCancel(!hasMissing)
            .setOnlyAlertOnce(true)
            .setColor(colorInt)
            .setColorized(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
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
                "Enable Notifications",
                notifPendingIntent
            )
        }

        // Action 3: If Battery is restricted, provide unrestrict button
        if (!batActive) {
            val batIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val batPendingIntent = PendingIntent.getActivity(
                context,
                103,
                batIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_dialog_alert,
                "Unrestrict Battery",
                batPendingIntent
            )
        }

        return builder.build()
    }

    /**
     * Posts, updates, or dismisses the status bar notification.
     *
     * Rule:
     * - The persistent status notification should NOT stay visible if all required services are active.
     * - Only show a notification if any critical permission is MISSING or OFF (Accessibility, Notification Listener, Battery Unrestricted).
     * - Once all permissions are enabled, immediately dismiss/cancel this notification.
     */
    fun updateStatus(context: Context) {
        try {
            val accActive = isAccessibilityActive(context)
            val notifActive = isNotificationListenerActive(context)
            val batActive = isBatteryUnrestricted(context)
            val masterOn = AutoAcceptService.isAutomationEnabled(context)
            val hasMissing = !accActive || !notifActive || !batActive

            _servicesState.value = ServicesState(
                isAccessibilityActive = accActive,
                isNotificationListenerActive = notifActive,
                isBatteryUnrestricted = batActive,
                isMasterSwitchOn = masterOn,
                hasMissingCriticalPermissions = hasMissing,
                lastUpdatedMillis = System.currentTimeMillis()
            )

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            // If all critical permissions are active, IMMEDIATELY dismiss/cancel notification
            if (!hasMissing) {
                NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
                notificationManager?.cancel(NOTIFICATION_ID)
                // Stop KeepAliveService so it doesn't hold notification 1001
                KeepAliveService.stop(context)
                Log.d(TAG, "All critical permissions active. Service status notification cancelled/dismissed.")
                return
            }

            // Only show notification if any critical permission is MISSING or OFF
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
            Log.d(TAG, "Service status alert notification updated: missing permissions detected.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update service status notification: ${e.message}")
        }
    }

    /**
     * Starts or updates foreground service using the status notification only if missing permissions exist.
     */
    fun startOrUpdateForeground(service: AutoAcceptService) {
        try {
            if (!hasMissingCriticalPermissions(service)) {
                NotificationManagerCompat.from(service).cancel(NOTIFICATION_ID)
                val nm = service.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(NOTIFICATION_ID)
                return
            }
            val notification = buildNotification(service)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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
            Log.i(TAG, "AutoAcceptService foreground status alert notification started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground status notification: ${e.message}", e)
        }
    }
}
