package com.example

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Listens for OS-level status bar notifications from Rapido Captain / allowed driver apps.
 * Acts as the authoritative primary trigger that arms the AutoAccept accessibility engine,
 * eliminating false positive triggers on unrelated background apps such as YouTube or Chrome.
 */
class RideNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val packageName = sbn.packageName ?: return

        // Verify package belongs to Rapido Captain or allowed driver packages
        if (!AutoAcceptService.ALLOWED_RAPIDO_PACKAGES.contains(packageName)) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""
        val subText = extras.getCharSequence("android.subText")?.toString() ?: ""
        val fullContent = "$title $text $bigText $subText".trim()

        if (isIncomingRideNotification(fullContent)) {
            Log.i(TAG, "Incoming ride notification detected from $packageName: $title | $text")
            AutoAcceptService.isGenuineOrderIncoming = true

            AutoAcceptService.logServiceEvent(
                type = ServiceEventType.ORDER_DETECTED,
                title = "Order notification received",
                description = "Incoming ride alert from Rapido: ${title.ifBlank { text }}",
                details = "Trigger armed for 30s. Accessibility screen parser active.",
                badge = "INCOMING"
            )

            // Auto-reset this flag back to false after 30 seconds
            resetJob?.cancel()
            resetJob = serviceScope.launch {
                delay(30_000L)
                AutoAcceptService.isGenuineOrderIncoming = false
                Log.d(TAG, "Reset isGenuineOrderIncoming to false after 30s timeout")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "RideNotificationService"

        private val INCOMING_KEYWORDS = listOf(
            "order", "ride", "booking", "request", "pickup", "pick up",
            "drop", "fare", "₹", "rs", "captain", "accept", "incoming",
            "new", "assigned", "trip", "demand", "chalo", "shuru"
        )

        fun isIncomingRideNotification(content: String): Boolean {
            if (content.isBlank()) return true
            val lower = content.lowercase()
            return INCOMING_KEYWORDS.any { lower.contains(it) }
        }
    }
}
