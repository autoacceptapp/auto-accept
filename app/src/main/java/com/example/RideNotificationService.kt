package com.example

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RideNotificationService : NotificationListenerService() {
    companion object {
        private const val TAG = "RideNotificationService"

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        fun isConnected(): Boolean = _isListenerConnected.value
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isListenerConnected.value = true
        Log.i(TAG, "RideNotificationService listener connected.")
        DebugLogManager.logNotification(
            title = "Notification Listener Connected",
            message = "RideNotificationService is actively listening for driver order alerts",
            severity = LogSeverity.INFO,
            category = OrderDebugCategory.SERVICE_STATUS,
            rawDetails = "Allowed packages: ${AutoAcceptService.ALLOWED_RAPIDO_PACKAGES.joinToString()}"
        )
        ServiceStatusNotificationManager.updateStatus(this)
        ServiceStatusWidgetProvider.updateAllWidgets(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isListenerConnected.value = false
        Log.w(TAG, "RideNotificationService listener disconnected.")
        DebugLogManager.logNotification(
            title = "Notification Listener Disconnected",
            message = "RideNotificationService was unbound or disconnected by Android system",
            severity = LogSeverity.WARNING,
            category = OrderDebugCategory.SERVICE_STATUS,
            missedReason = "Notification Listener permission may be revoked or service killed by OS battery optimization.",
            suggestedFix = "Re-enable Notification Access in device Settings and exclude app from Battery Saver."
        )
        ServiceStatusNotificationManager.updateStatus(this)
        ServiceStatusWidgetProvider.updateAllWidgets(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        _isListenerConnected.value = false
        ServiceStatusNotificationManager.updateStatus(this)
        ServiceStatusWidgetProvider.updateAllWidgets(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = sbn?.packageName ?: return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val rawTitle = (extras.getCharSequence(android.app.Notification.EXTRA_TITLE)
            ?: extras.getString(android.app.Notification.EXTRA_TITLE))?.toString() ?: ""
        val rawText = (extras.getCharSequence(android.app.Notification.EXTRA_TEXT)
            ?: extras.getString(android.app.Notification.EXTRA_TEXT))?.toString() ?: ""
        val title = rawTitle.lowercase()
        val text = rawText.lowercase()

        if (packageName == "com.rapido.rider") {
            return
        }

        // Check if the notification is from allowed Rapido/Driver apps
        if (packageName == AutoAcceptService.RAPIDO_CAPTAIN_PACKAGE) {
            val combinedText = "$title $text"

            val promotionalKeywords = listOf("promise", "service", "update", "discount", "offer", "cashback", "earnings", "tips")
            if (promotionalKeywords.any { combinedText.contains(it) }) {
                return
            }

            // Checking common incoming order keywords
            val orderKeywords = listOf("new order", "incoming order", "captain, you have a new ride", "new ride request", "pickup")
            if (orderKeywords.any { combinedText.contains(it) }) {
                Log.i(TAG, "Genuine Ride Notification detected from $packageName")
                DebugLogManager.logNotification(
                    title = "Genuine Ride Notification Detected",
                    message = "Incoming order alert received from $packageName: '$rawTitle'",
                    severity = LogSeverity.SUCCESS,
                    category = OrderDebugCategory.ORDER_DETECTED,
                    packageName = packageName,
                    rawDetails = "Title: '$rawTitle' | Text: '$rawText' -> Armed AutoAccept scanner"
                )
                AutoAcceptService.triggerFromNotification(this)
            } else {
                Log.d(TAG, "Notification from $packageName ignored: no ride keywords ($combinedText)")
                DebugLogManager.logNotification(
                    title = "Notification Ignored (No Ride Keywords)",
                    message = "Notification from $packageName did not match active ride booking keywords",
                    severity = LogSeverity.WARNING,
                    category = OrderDebugCategory.NOTIFICATION_IGNORED,
                    packageName = packageName,
                    missedReason = "Notification text did not contain active booking triggers ('new', 'incoming', 'accept', 'ride', 'order').",
                    suggestedFix = "This is normal for promotional or system alerts from Rapido. Real bookings will trigger automatically.",
                    rawDetails = "Title: '$rawTitle' | Text: '$rawText'"
                )
            }
        } else if (packageName.contains("rapido", ignoreCase = true) ||
            packageName.contains("driver", ignoreCase = true) ||
            packageName.contains("captain", ignoreCase = true)
        ) {
            // Unmonitored package that sounds like driver app
            Log.d(TAG, "Notification from unmonitored package: $packageName")
            DebugLogManager.logNotification(
                title = "Driver Notification Filtered (Unmonitored App)",
                message = "Received alert from '$packageName' which is not in monitored packages",
                severity = LogSeverity.INFO,
                category = OrderDebugCategory.NOTIFICATION_IGNORED,
                packageName = packageName,
                missedReason = "App package '$packageName' is not listed in active allowed packages.",
                suggestedFix = "Update allowed packages via Firebase Remote Config or contact administrator if using a new app package.",
                rawDetails = "Title: '$rawTitle' | Text: '$rawText'"
            )
        }
    }
}
