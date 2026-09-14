package com.example

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * ServiceEventType categorized for dashboard visualization
 */
enum class ServiceEventType {
    RIDE_DETECTED,
    ORDER_ACCEPTED,
    ORDER_IGNORED,
    ORDER_QUEUED,
    MONITORING
}

/**
 * CapturedRide data snapshot for post-delay safety verification
 */
data class CapturedRide(
    val sourcePackage: String,
    val price: Float?,
    val distanceKm: Float?,
    val pickup: String,
    val drop: String,
    val signature: String,
    val acceptReason: String,
    val isPremium: Boolean,
    val delayMs: Long
)

/**
 * Action type supported for interacting with the accept control
 */
enum class ActionType {
    CLICK,
    SWIPE
}

/**
 * Validated button representation before action dispatch
 */
data class ValidatedButton(
    val node: AccessibilityNodeInfo,
    val reason: String,
    val actionType: ActionType = ActionType.CLICK,
    val targetBounds: Rect = Rect()
)

/**
 * Outcome of click/gesture execution with distinction between success, cancellation, and failure
 */
sealed class ClickResult {
    data class Success(val method: String) : ClickResult()
    data class Cancelled(val reason: String) : ClickResult()
    data class Failed(val reason: String) : ClickResult()
}

/**
 * ServiceEvent model representing real-time telemetry captured by AutoAcceptService.
 */
data class ServiceEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: ServiceEventType = ServiceEventType.MONITORING,
    val title: String = "",
    val description: String = "",
    val details: String = "",
    val badge: String = ""
)

/**
 * RideLogItem data model representing each processed order logged locally.
 */
data class RideLogItem(
    val id: String = "",
    val userId: String = "",
    val price: Float = 0f,
    val pickupKm: Float = 0f,
    val pickupLocation: String = "",
    val dropLocation: String = "",
    val status: String = "", // "ACCEPTED", "IGNORED"
    val reason: String = "",
    val sourcePackage: String = "",
    val createdMillis: Long = System.currentTimeMillis()
)

/**
 * AutoAcceptService - Smart Auto-Accept Accessibility Service for Rapido Captain.
 *
 * Implements strict conditional filter architecture:
 * 1. Master ON/OFF Switch: Completely halts or enables automated processing.
 * 2. Individual Filter Toggles:
 *    - Distance Filter (ON/OFF): If ON, checks distance <= maxDistance. If OFF, completely bypassed.
 *    - Price Filter (ON/OFF): If ON, checks minPrice <= fare <= maxPrice. If OFF, completely bypassed.
 *    - Blacklist Filter (ON/OFF): If ON, rejects if any keyword matched. If OFF, completely bypassed.
 * 3. Accept All Override: If Master Switch is ON but ALL individual filters are OFF, accepts any order.
 * 4. Voice Announcer (Text-To-Speech): Parses screen data and announces details dynamically.
 */
class AutoAcceptService : AccessibilityService(), TextToSpeech.OnInitListener {

    private var lastClickTimestamp: Long = 0
    private var lastScanTimestamp: Long = 0
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized: Boolean = false

    // Lifecycle-bound coroutine management and pending accept tracking (Requirements 8, 14, 15)
    private var serviceJob = SupervisorJob()
    private var serviceScopeInstance = CoroutineScope(Dispatchers.Main + serviceJob)
    private var pendingAcceptJob: Job? = null
    private var pendingRideSignature: String? = null

    // CPU PARTIAL_WAKE_LOCK management to keep CPU running when an incoming ride is detected until click execution completes
    @Volatile
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private val wakeLockMutex = Any()

    /**
     * Acquires a PARTIAL_WAKE_LOCK to ensure the device CPU remains active and prevents
     * the background process and delay coroutines from sleeping between ride detection and click execution.
     */
    fun acquireCpuWakeLock(timeoutMs: Long = 15000L, reason: String = "Incoming ride detected") {
        if (!isWakeLockEnabled(this)) {
            Log.d(TAG, "CPU WakeLock acquisition skipped: disabled in settings")
            return
        }
        synchronized(wakeLockMutex) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (powerManager == null) {
                    Log.w(TAG, "PowerManager is null; cannot acquire CPU WakeLock")
                    return
                }
                if (cpuWakeLock == null) {
                    cpuWakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "AutoAccept:CpuRideProcessing"
                    ).apply {
                        setReferenceCounted(false)
                    }
                }
                cpuWakeLock?.let { wl ->
                    wl.acquire(timeoutMs)
                    Log.i(TAG, "CPU PARTIAL_WAKE_LOCK acquired for ${timeoutMs}ms ($reason)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to acquire CPU WakeLock: ${e.message}", e)
            }
        }
    }

    /**
     * Safely releases the CPU PARTIAL_WAKE_LOCK when click execution completes, fails, or is cancelled.
     */
    fun releaseCpuWakeLock(reason: String = "Operation completed") {
        synchronized(wakeLockMutex) {
            try {
                cpuWakeLock?.let { wl ->
                    if (wl.isHeld) {
                        wl.release()
                        Log.i(TAG, "CPU PARTIAL_WAKE_LOCK released ($reason)")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release CPU WakeLock: ${e.message}", e)
            }
        }
    }

    fun isCpuWakeLockHeld(): Boolean {
        return synchronized(wakeLockMutex) {
            cpuWakeLock?.isHeld == true
        }
    }

    /**
     * Safely cancels pending scheduled accept job and notifies telemetry
     */
    fun cancelPendingAcceptInternal(reason: String = "Operation cancelled") {
        val job = pendingAcceptJob
        val sig = pendingRideSignature
        if (job != null && job.isActive) {
            job.cancel()
            Log.i(TAG, "Pending accept cancelled: $reason (signature=$sig)")
            _recentLog.value = "Pending accept cancelled: $reason"
            logServiceEvent(
                type = ServiceEventType.ORDER_IGNORED,
                title = "Accept cancelled",
                description = "Pending accept cancelled: $reason",
                badge = "CANCELLED"
            )
        }
        pendingAcceptJob = null
        pendingRideSignature = null
        releaseCpuWakeLock("Pending accept cancelled: $reason")
    }

    companion object {
        private const val TAG = "AutoAcceptService"
        const val PREFS_NAME = "auto_accept_preferences"

        // Notification Channel Constants
        const val NOTIFICATION_CHANNEL_ID = "auto_accept_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Auto Accept Service"
        const val NOTIFICATION_ID = 1
        const val ALERT_CHANNEL_ID = "order_alerts_channel"
        const val ALERT_CHANNEL_NAME = "Order Alerts"
        const val ALERT_NOTIFICATION_ID = 2

        // SharedPreferences Keys
        const val KEY_AUTO_ACCEPT_ENABLED = "key_auto_accept_enabled"
        const val KEY_TARGET_RAPIDO_ONLY = "key_target_rapido_only"
        const val KEY_IS_PASS_ACTIVE = "key_is_pass_active"
        const val KEY_WAKE_LOCK_ENABLED = "key_wake_lock_enabled"
        const val KEY_LOCAL_RIDE_LOGS = "key_local_ride_logs"
        const val KEY_VOICE_ONLY_MODE = "key_voice_only_mode"

        const val KEY_DAILY_TRIP_COUNT = "key_daily_trip_count"
        const val KEY_DAILY_GOAL = "key_daily_goal"
        const val KEY_LAST_TRIP_DATE = "key_last_trip_date"
        const val KEY_TRIP_HISTORY = "key_trip_history" // JSON string of last 7 days



        // Firestore Configuration & Exponential Backoff Parameters
        const val FIRESTORE_ORDERS_COLLECTION = "accepted_rides"
        const val FIRESTORE_MAX_RETRIES = 5
        const val FIRESTORE_INITIAL_DELAY_MS = 1000L
        const val FIRESTORE_MAX_DELAY_MS = 30000L
        const val FIRESTORE_BACKOFF_FACTOR = 2.0

        private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Individual Filter Toggles & Values
        const val KEY_DISTANCE_FILTER_ENABLED = "key_distance_filter_enabled"
        const val KEY_MAX_DISTANCE_KM = "key_max_distance_km"

        const val KEY_PRICE_FILTER_ENABLED = "key_price_filter_enabled"
        const val KEY_MIN_PRICE = "key_min_price"
        const val KEY_MAX_PRICE = "key_max_price"

        const val KEY_RATING_FILTER_ENABLED = "key_rating_filter_enabled"
        const val KEY_MIN_PASSENGER_RATING = "key_min_passenger_rating"

        const val KEY_BLACKLIST_ENABLED = "key_blacklist_enabled"
        const val KEY_BLACKLIST_KEYWORDS = "key_blacklist_keywords"

        const val KEY_TTS_ENABLED = "key_tts_enabled"
        const val KEY_TTS_LANGUAGE = "key_tts_language"
        const val KEY_USER_NAME = "key_user_name"
        const val KEY_CUSTOM_SOUND_URI = "key_custom_sound_uri"

        const val KEY_ACCEPT_DELAY_MS = "key_accept_delay_ms"
        const val KEY_ENABLED_KEYWORDS = "key_enabled_keywords"
        const val KEY_ENABLED_APPS = "key_enabled_apps"
        const val DEFAULT_ACCEPT_DELAY_MS = 300L

        // Default Filter Values
        const val DEFAULT_MAX_DISTANCE_KM = 5.0f
        const val DEFAULT_MIN_PRICE = 40.0f
        const val DEFAULT_MAX_PRICE = 500.0f
        const val DEFAULT_MIN_PASSENGER_RATING = 4.5f
        const val DEFAULT_USER_NAME = "Captain"
        const val DEFAULT_TTS_LANGUAGE = "en"
        const val DEFAULT_BLACKLIST_KEYWORDS = "Airport, Toll, Slum, Waterlog"

        // Success Streak Gamification Keys
        const val KEY_SUCCESS_STREAK = "key_success_streak"
        const val KEY_BEST_SUCCESS_STREAK = "key_best_success_streak"

        // Rate-limiting debounce cooldown (3000ms)
        private const val CLICK_COOLDOWN_MS = 3000L

        // Reactive StateFlows observed by MainActivity Compose UI
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _dailyTripCount = MutableStateFlow(0)
        val dailyTripCount: StateFlow<Int> = _dailyTripCount.asStateFlow()

        // List of Pair<DateString, Count>
        private val _tripHistory = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
        val tripHistory: StateFlow<List<Pair<String, Int>>> = _tripHistory.asStateFlow()

        // Gamified Success Streak StateFlows (consecutive accepted rides without any missed rides)
        private val _successStreak = MutableStateFlow(0)
        val successStreak: StateFlow<Int> = _successStreak.asStateFlow()

        private val _bestSuccessStreak = MutableStateFlow(0)
        val bestSuccessStreak: StateFlow<Int> = _bestSuccessStreak.asStateFlow()



        private val _recentLog = MutableStateFlow("Service initialized. Ready for Rapido orders.")
        val recentLog: StateFlow<String> = _recentLog.asStateFlow()

        // Local ride logs StateFlow (offline persistence via SharedPreferences JSON)
        private val _localRideLogs = MutableStateFlow<List<RideLogItem>>(emptyList())
        val localRideLogs: StateFlow<List<RideLogItem>> = _localRideLogs.asStateFlow()

        // Live visual events StateFlow for dashboard activity stream
        private val _serviceEvents = MutableStateFlow<List<ServiceEvent>>(
            listOf(
                ServiceEvent(
                    type = ServiceEventType.MONITORING,
                    title = "Monitoring Active",
                    description = "Watching Rapido Captain for incoming rides",
                    details = "AutoAccept accessibility layer ready",
                    badge = "READY"
                )
            )
        )
        val serviceEvents: StateFlow<List<ServiceEvent>> = _serviceEvents.asStateFlow()

        // Primary trigger flag armed by OS-level ride notification
        @Volatile
        var isGenuineOrderIncoming = false

        // Timestamp when isGenuineOrderIncoming was set to true
        @Volatile
        private var genuineOrderIncomingTimestamp: Long = 0L

        private var notificationResetJob: Job? = null
        private var safetyMonitorJob: Job? = null

        /**
         * Safety Monitor: Periodically checks every 1000ms if 'isGenuineOrderIncoming' remains stuck in
         * a 'true' state for over 5 seconds (5000ms) and automatically resets it to false to prevent
         * the service from locking up or continuously scanning without an active order.
         */
        private fun startSafetyMonitor() {
            if (safetyMonitorJob?.isActive == true) return
            safetyMonitorJob = serviceScope.launch {
                while (isActive) {
                    delay(1000L)
                    if (isGenuineOrderIncoming) {
                        val elapsed = System.currentTimeMillis() - genuineOrderIncomingTimestamp
                        if (elapsed > 5000L) {
                            Log.w(TAG, "Safety Monitor: isGenuineOrderIncoming stuck for ${elapsed}ms (> 5s). Auto-resetting to false to prevent service lockup.")
                            isGenuineOrderIncoming = false
                            notificationResetJob?.cancel()
                            releaseCpuWakeLock("Safety monitor timeout (>5s)")
                            _recentLog.value = "Safety Monitor: Reset stuck order flag (> 5s)"
                            logServiceEvent(
                                type = ServiceEventType.MONITORING,
                                title = "Safety reset",
                                description = "Order flag was stuck for ${elapsed / 1000}s (> 5s); auto-reset",
                                details = "Prevented scanner lockup",
                                badge = "SAFETY"
                            )
                            DebugLogManager.logAccessibility(
                                title = "Order Missed: Scanner Timeout (>5s)",
                                message = "Rapido overlay screen did not appear within 5s after notification",
                                severity = LogSeverity.WARNING,
                                category = OrderDebugCategory.ORDER_MISSED,
                                missedReason = "Rapido Captain overlay or accept dialog was not displayed on screen within 5000ms after notification arrived.",
                                suggestedFix = "Ensure 'Display over other apps' is allowed for Rapido Captain and disable OS Battery Saver."
                            )
                        }
                    }
                }
            }
        }

        /**
         * Triggered exclusively by RideNotificationService when an incoming ride notification
         * is detected from Rapido Captain. Sets isGenuineOrderIncoming = true, acquires CPU WakeLock
         * to prevent sleep while waiting for overlay/click execution, runs the 5s safety monitor,
         * and manages fallback reset.
         */
        fun triggerFromNotification(context: Context? = null) {
            val now = System.currentTimeMillis()
            if (now - genuineOrderIncomingTimestamp < 3000L) return
            genuineOrderIncomingTimestamp = now
            isGenuineOrderIncoming = true
            acquireCpuWakeLock(context, timeoutMs = 15000L, reason = "Incoming ride notification detected")
            startSafetyMonitor()
            logServiceEvent(
                type = ServiceEventType.RIDE_DETECTED,
                title = "Order notification received",
                description = "Genuine ride notification detected from Rapido",
                details = "AutoAccept scanner armed & CPU kept awake (15s WakeLock)",
                badge = "INCOMING"
            )
            notificationResetJob?.cancel()
            notificationResetJob = serviceScope.launch {
                delay(5000L)
                if (isGenuineOrderIncoming) {
                    isGenuineOrderIncoming = false
                    releaseCpuWakeLock("Notification order window timeout (>5s)")
                    Log.d(TAG, "isGenuineOrderIncoming auto-reset after 5s duration")
                }
            }
        }

        fun logServiceEvent(
            type: ServiceEventType,
            title: String,
            description: String,
            details: String = "",
            badge: String = ""
        ) {
            val event = ServiceEvent(
                type = type,
                title = title,
                description = description,
                details = details,
                badge = badge.ifBlank {
                    when (type) {
                        ServiceEventType.RIDE_DETECTED -> "DETECTED"
                        ServiceEventType.ORDER_ACCEPTED -> "ACCEPTED"
                        ServiceEventType.ORDER_IGNORED -> "FILTERED"
                        ServiceEventType.ORDER_QUEUED -> "QUEUED"
                        ServiceEventType.MONITORING -> "ONLINE"
                    }
                }
            )
            val updated = (listOf(event) + _serviceEvents.value).take(60)
            _serviceEvents.value = updated
            _recentLog.value = "$title: $description"

            val (logSev, logCat) = when (type) {
                ServiceEventType.RIDE_DETECTED -> Pair(LogSeverity.INFO, OrderDebugCategory.ORDER_DETECTED)
                ServiceEventType.ORDER_ACCEPTED -> Pair(LogSeverity.SUCCESS, OrderDebugCategory.ORDER_ACCEPTED)
                ServiceEventType.ORDER_IGNORED -> Pair(LogSeverity.WARNING, OrderDebugCategory.FILTER_REJECTED)
                ServiceEventType.ORDER_QUEUED -> Pair(LogSeverity.INFO, OrderDebugCategory.ORDER_DETECTED)
                ServiceEventType.MONITORING -> Pair(LogSeverity.INFO, OrderDebugCategory.SERVICE_STATUS)
            }
            DebugLogManager.logAccessibility(
                title = title,
                message = description,
                severity = logSev,
                category = logCat,
                rawDetails = details.ifBlank { null }
            )

            // Success streak tracking: consecutive accepted rides increment streak; missed/ignored rides reset streak
            if (type == ServiceEventType.ORDER_ACCEPTED) {
                recordAcceptedStreak(instance)
            } else if (type == ServiceEventType.ORDER_IGNORED) {
                resetSuccessStreak(instance)
            }
        }

        fun clearServiceEvents() {
            _serviceEvents.value = emptyList()
        }

        fun simulateTestEvent() {
            val sampleFares = listOf(78, 124, 182, 245)
            val sampleDists = listOf(1.9f, 3.8f, 5.4f, 7.6f)
            val samplePickups = listOf("Indiranagar 12th Main", "Koramangala 5th Block", "MG Road Metro", "HSR Sector 1")
            val sampleDrops = listOf("Electronic City Phase 1", "Bellandur EcoSpace", "Whitefield ITPL", "Manyata Tech Park")
            val idx = (0..3).random()
            val fare = sampleFares[idx]
            val dist = sampleDists[idx]
            val pickup = samplePickups[idx]
            val drop = sampleDrops[idx]

            logServiceEvent(
                type = ServiceEventType.RIDE_DETECTED,
                title = "Ride detected",
                description = "₹$fare • ${dist} km • $pickup → $drop",
                details = "Matched tokens: ₹, Pickup, Drop, km",
                badge = "DETECTED"
            )

            val isMissed = (_serviceEvents.value.size % 2 == 1)
            if (isMissed) {
                logServiceEvent(
                    type = ServiceEventType.ORDER_IGNORED,
                    title = "Ride missed / filtered",
                    description = "Skipped: ₹$fare below threshold or outside zone ($dist km)",
                    details = "Filter condition matched: Ride missed",
                    badge = "MISSED"
                )
            } else {
                logServiceEvent(
                    type = ServiceEventType.ORDER_ACCEPTED,
                    title = "Order accepted",
                    description = "High-speed auto-click executed for ₹$fare ($dist km)",
                    details = "Turbo Engine (190ms response delay)",
                    badge = "ACCEPTED"
                )
            }
        }

        private var instance: AutoAcceptService? = null

        /**
         * Programmatically disables the running AccessibilityService without navigating to system settings.
         * Uses Android 7.0+ AccessibilityService.disableSelf().
         */
        fun disableService(): Boolean {
            val service = instance
            if (service != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        service.disableSelf()
                        instance = null
                        _isServiceRunning.value = false
                        _recentLog.value = "Service disabled directly via in-app toggle."
                        Log.i(TAG, "AccessibilityService stopped itself via disableSelf().")
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to disableSelf: ${e.message}", e)
                    }
                }
            }
            return false
        }

        /**
         * Returns whether the active AccessibilityService instance is currently bound and running.
         */
        fun isServiceConnected(): Boolean {
            return instance != null && _isServiceRunning.value
        }

        /**
         * Acquires a CPU WakeLock on the active service instance or context
         */
        fun acquireCpuWakeLock(context: Context? = null, timeoutMs: Long = 15000L, reason: String = "Incoming ride detected") {
            val service = instance
            if (service != null) {
                service.acquireCpuWakeLock(timeoutMs, reason)
            } else if (context != null) {
                try {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    if (pm != null && isWakeLockEnabled(context)) {
                        val fallbackLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoAccept:CpuFallbackLock").apply {
                            setReferenceCounted(false)
                        }
                        fallbackLock.acquire(timeoutMs)
                        Log.i(TAG, "Fallback CPU WakeLock acquired for ${timeoutMs}ms ($reason)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to acquire fallback CPU WakeLock: ${e.message}", e)
                }
            }
        }

        /**
         * Releases the CPU WakeLock on the active service instance
         */
        fun releaseCpuWakeLock(reason: String = "Operation completed") {
            instance?.releaseCpuWakeLock(reason)
        }

        /**
         * Returns whether the CPU WakeLock is currently held
         */
        fun isCpuWakeLockHeld(): Boolean {
            return instance?.isCpuWakeLockHeld() == true
        }

        // Target packages for Rapido Captain (with Remote Config dynamic fallback)
        const val RAPIDO_CAPTAIN_PACKAGE = "com.rapido.captain"
        val ALLOWED_RAPIDO_PACKAGES: Set<String>
            get() = RemoteConfigManager.allowedPackages.value.ifEmpty {
                setOf(
                    RAPIDO_CAPTAIN_PACKAGE,
                    "com.rapido.rider",
                    "com.rapido.driver"
                )
            }

        // Contextual tokens indicating an active incoming ride overlay
        val RIDE_CONTEXT_INDICATORS = listOf(
            "Pickup",
            "Pick up",
            "Drop",
            "Drop off",
            "₹",
            "Rs",
            "INR",
            "km",
            "KM",
            "Distance",
            "Fare",
            "Earnings",
            "Est. Fare",
            "Captain Fare"
        )

        val ACCEPT_BUTTON_KEYWORDS = listOf(
            "Accept",
            "ACCEPT",
            "Accept Order",
            "Accept Ride",
            "Swipe to Accept",
            "Take Order",
            "Confirm Order"
        )
        val ACCEPT_KEYWORDS = ACCEPT_BUTTON_KEYWORDS

        val ACCEPT_BUTTON_VIEW_IDS = listOf(
            "btn_accept",
            "accept_ride_button",
            "btnAccept",
            "button_accept",
            "order_accept_btn",
            "accept_order_container"
        )

        // Regex patterns for distance and price
        val DISTANCE_REGEX = Regex("""(\d+(?:\.\d+)?)\s*(?:km|kms)""", RegexOption.IGNORE_CASE)

        val PRICE_REGEX = Regex(
            """(?:(?:₹|Rs\.?|INR)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:₹|Rs\.?|INR))""",
            RegexOption.IGNORE_CASE
        )

        // Duplicate accept prevention cache: ride signature -> last accepted timestamp (Requirement 7)
        private val recentlyAcceptedRides = ConcurrentHashMap<String, Long>()
        private const val DUPLICATE_COOLDOWN_MS = 25000L

        private val recentlyRejectedRides = ConcurrentHashMap<String, Long>()
        private const val REJECT_COOLDOWN_MS = 15000L

        private fun cleanStaleAcceptedRides() {
            val now = System.currentTimeMillis()
            val acceptCutoff = now - DUPLICATE_COOLDOWN_MS
            val acceptIt = recentlyAcceptedRides.entries.iterator()
            while (acceptIt.hasNext()) {
                if (acceptIt.next().value < acceptCutoff) {
                    acceptIt.remove()
                }
            }
            
            val rejectCutoff = now - REJECT_COOLDOWN_MS
            val rejectIt = recentlyRejectedRides.entries.iterator()
            while (rejectIt.hasNext()) {
                if (rejectIt.next().value < rejectCutoff) {
                    rejectIt.remove()
                }
            }
        }

        fun generateRideSignature(sourcePackage: String, price: Float?, distanceKm: Float?, pickup: String, drop: String): String {
            val p = price?.toInt()?.toString() ?: "any_fare"
            val d = distanceKm?.let { String.format(Locale.US, "%.1f", it) } ?: "any_dist"
            val pick = pickup.trim().lowercase().take(30)
            val drp = drop.trim().lowercase().take(30)
            return "$sourcePackage#$p#$d#$pick#$drp"
        }

        fun cancelPendingAccept(reason: String = "Operation cancelled") {
            instance?.cancelPendingAcceptInternal(reason)
        }

        /**
         * Prevents false positives by strictly verifying button text against negative words and valid patterns.
         * Resiliently matches roots like Accept, Swipe, Take, Confirm, and Go.
         */
        fun isValidAcceptText(context: Context? = null, text: String, keyword: String = ""): Boolean {
            if (text.isBlank() || text.length > 35) return false
            val lower = text.lowercase()
            val falsePositiveWords = listOf("do not", "don't", "terms", "policy", "cash", "upi", "card", "condition", "decline", "reject", "cancel", "privacy", "return")
            if (falsePositiveWords.any { lower.contains(it) }) return false

            if (keyword.isNotBlank()) {
                if (text.equals(keyword, ignoreCase = true)) return true
                if (text.startsWith(keyword, ignoreCase = true)) return true
            }

            val enabledKeywords = if (context != null) getEnabledKeywords(context) else emptyList()
            if (enabledKeywords.any { text.equals(it, ignoreCase = true) || text.startsWith(it, ignoreCase = true) }) {
                return true
            }

            val pattern = Regex("""^(?:swipe\s+to\s+accept|accept(?:\s+(?:order|ride))?|take\s+order|confirm\s+order|go|chalo|shuru|yes)(?:\s*[\(>→»\d\w\s]*)?$""", RegexOption.IGNORE_CASE)
            if (pattern.matches(text)) return true

            val fuzzyRootPattern = Regex("""\b(?:accept|swipe|take|confirm|go|chalo|shuru|yes)\b""", RegexOption.IGNORE_CASE)
            return fuzzyRootPattern.containsMatchIn(text)
        }


        // =========================================================================
        // ROOM DATABASE SYNC (Offline Caching Strategy)
        // =========================================================================
        private fun syncSettingsToRoom(context: Context) {
            kotlin.concurrent.thread {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    db.settingsDao().insertFilterSettingsSync(
                        com.example.data.FilterSettings(
                            id = 1,
                            maxDistance = getMaxDistanceKm(context),
                            minPrice = getMinPrice(context),
                            maxPrice = getMaxPrice(context),
                            isDistanceFilterOn = isDistanceFilterEnabled(context),
                            isPriceFilterOn = isPriceFilterEnabled(context),
                            isBlacklistFilterOn = isBlacklistEnabled(context),
                            blacklistKeywords = getBlacklistKeywords(context),
                            minPassengerRating = getMinPassengerRating(context),
                            isPassengerRatingFilterOn = isPassengerRatingFilterEnabled(context)
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync filter settings to Room: ${e.message}")
                }
            }
        }

        private fun syncTripHistoryToRoom(context: Context, history: List<Pair<String, Int>>) {
            kotlin.concurrent.thread {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(context)
                    val records = history.map { com.example.data.TripHistoryRecord(it.first, it.second) }
                    db.settingsDao().clearTripHistorySync()
                    db.settingsDao().insertTripHistorySync(records)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync trip history to Room: ${e.message}")
                }
            }
        }

        // =========================================================================
        // SharedPreferences Getters & Setters

        fun getTripHistory(context: Context): List<Pair<String, Int>> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val historyStr = prefs.getString(KEY_TRIP_HISTORY, "") ?: ""
            if (historyStr.isBlank()) return emptyList()
            
            return try {
                historyStr.split(";").mapNotNull { entry ->
                    val parts = entry.split(":")
                    if (parts.size == 2) {
                        Pair(parts[0], parts[1].toIntOrNull() ?: 0)
                    } else null
                }
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun saveTripHistory(context: Context, history: List<Pair<String, Int>>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // Keep only last 7 days
            val trimmed = history.takeLast(7)
            val historyStr = trimmed.joinToString(";") { "${it.first}:${it.second}" }
            prefs.edit().putString(KEY_TRIP_HISTORY, historyStr).apply()
            _tripHistory.value = trimmed
            syncTripHistoryToRoom(context, trimmed)
        }

        fun getDailyGoal(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_DAILY_GOAL, 10) // Default 10
        }

        fun setDailyGoal(context: Context, goal: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_DAILY_GOAL, goal).apply()
        }

        fun getDailyTripCount(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastDate = prefs.getString(KEY_LAST_TRIP_DATE, "")
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            if (lastDate != currentDate && !lastDate.isNullOrEmpty()) {
                // Day changed! Save previous day's count to history before resetting
                val lastCount = prefs.getInt(KEY_DAILY_TRIP_COUNT, 0)
                val currentHistory = getTripHistory(context).toMutableList()
                // Remove if already exists to avoid duplicates
                currentHistory.removeAll { it.first == lastDate }
                currentHistory.add(Pair(lastDate, lastCount))
                saveTripHistory(context, currentHistory)
                
                // Reset counter for new day
                prefs.edit().putInt(KEY_DAILY_TRIP_COUNT, 0).putString(KEY_LAST_TRIP_DATE, currentDate).apply()
                return 0
            } else if (lastDate.isNullOrEmpty()) {
                 prefs.edit().putInt(KEY_DAILY_TRIP_COUNT, 0).putString(KEY_LAST_TRIP_DATE, currentDate).apply()
                 return 0
            }
            return prefs.getInt(KEY_DAILY_TRIP_COUNT, 0)
        }


        


        fun syncDailyTripCount(context: Context) {
            _dailyTripCount.value = getDailyTripCount(context)
            _tripHistory.value = getTripHistory(context)
        }



        fun incrementDailyTripCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = getDailyTripCount(context)
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            
            prefs.edit()
                .putInt(KEY_DAILY_TRIP_COUNT, currentCount + 1)
                .putString(KEY_LAST_TRIP_DATE, currentDate)
                .apply()
            _dailyTripCount.value = currentCount + 1

        }

        // =========================================================================
        // SUCCESS STREAK GAMIFICATION METHODS
        // =========================================================================

        fun getSuccessStreak(context: Context? = null): Int {
            if (context == null) return _successStreak.value
            return try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_SUCCESS_STREAK, _successStreak.value)
            } catch (_: Throwable) {
                _successStreak.value
            }
        }

        fun getBestSuccessStreak(context: Context? = null): Int {
            if (context == null) return _bestSuccessStreak.value
            return try {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BEST_SUCCESS_STREAK, _bestSuccessStreak.value)
            } catch (_: Throwable) {
                _bestSuccessStreak.value
            }
        }

        fun syncSuccessStreak(context: Context) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val current = prefs.getInt(KEY_SUCCESS_STREAK, 0)
                val best = prefs.getInt(KEY_BEST_SUCCESS_STREAK, 0)
                _successStreak.value = current
                _bestSuccessStreak.value = maxOf(best, current)
            } catch (_: Throwable) {
            }
        }

        fun recordAcceptedStreak(context: Context? = null) {
            val next = _successStreak.value + 1
            _successStreak.value = next
            val newBest = maxOf(_bestSuccessStreak.value, next)
            _bestSuccessStreak.value = newBest
            val ctx = context ?: instance
            if (ctx != null) {
                try {
                    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit()
                        .putInt(KEY_SUCCESS_STREAK, next)
                        .putInt(KEY_BEST_SUCCESS_STREAK, newBest)
                        .apply()
                } catch (e: Throwable) {
                    // Ignored in test environment
                }
            }
        }

        fun resetSuccessStreak(context: Context? = null) {
            _successStreak.value = 0
            val ctx = context ?: instance
            if (ctx != null) {
                try {
                    val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    prefs.edit().putInt(KEY_SUCCESS_STREAK, 0).apply()
                } catch (e: Throwable) {
                    // Ignored in test environment
                }
            }
        }

        // =========================================================================

        fun isAutomationEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_AUTO_ACCEPT_ENABLED, true)
        }

        fun setAutomationEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_AUTO_ACCEPT_ENABLED, enabled).apply()
            if (!enabled) {
                instance?.cancelPendingAcceptInternal("Master switch turned OFF")
                
                StatusOverlayManager.hide()
            } else {
                
                StatusOverlayManager.show(context)
            }
            _recentLog.value = if (enabled) {
                "Master Switch: ON. Active & listening for orders."
            } else {
                "Master Switch: OFF. Auto-accept is paused."
            }
            instance?.updateForegroundNotification()
            ServiceStatusNotificationManager.updateStatus(context)
        }

        fun isVoiceOnlyMode(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_VOICE_ONLY_MODE, false)
        }

        fun setVoiceOnlyMode(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_VOICE_ONLY_MODE, enabled).apply()
            instance?.updateForegroundNotification()
        }

        fun updateForegroundStatus(context: Context, statusText: String? = null) {
            instance?.updateForegroundNotification(statusText)
        }

        /**
         * Creates and registers the notification channels for AutoAcceptService:
         * 1. NOTIFICATION_CHANNEL_ID ("auto_accept_channel"): Foreground service persistent channel.
         * 2. ALERT_CHANNEL_ID ("order_alerts_channel"): High-importance alert channel for order announcements.
         */
        fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

                // 1. Persistent Foreground Service Channel
                val foregroundChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps AutoAcceptService active in foreground to reliably monitor and accept rides during driving without being killed by Android"
                    setShowBadge(false)
                    enableLights(false)
                    enableVibration(false)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(foregroundChannel)

                // 2. High-Importance Order Alerts Channel
                val alertChannel = NotificationChannel(
                    ALERT_CHANNEL_ID,
                    ALERT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifies and alerts you when incoming rides are detected or accepted"
                    setShowBadge(true)
                    enableVibration(true)
                    enableLights(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(alertChannel)
            }
        }

        // Wait Delay Time Before Clicking Accept (Milliseconds)
        fun getAcceptDelayMs(context: Context): Long {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fallbackDefault = RemoteConfigManager.remoteDefaultDelayMs.value
            val configured = prefs.getLong(KEY_ACCEPT_DELAY_MS, fallbackDefault)
            return RemoteConfigManager.clampDelay(configured)
        }

        fun setAcceptDelayMs(context: Context, delayMs: Long) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val clamped = RemoteConfigManager.clampDelay(delayMs)
            prefs.edit().putLong(KEY_ACCEPT_DELAY_MS, clamped).apply()
            ServiceStatusNotificationManager.updateStatus(context)
        }

        // Distance Filter Toggle & Value
        fun isDistanceFilterEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_DISTANCE_FILTER_ENABLED, true)
        }

        fun setDistanceFilterEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_DISTANCE_FILTER_ENABLED, enabled).apply()
            syncSettingsToRoom(context)
        }

        fun getMaxDistanceKm(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_MAX_DISTANCE_KM, DEFAULT_MAX_DISTANCE_KM)
        }

        fun setMaxDistanceKm(context: Context, maxDistance: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_MAX_DISTANCE_KM, maxDistance).apply()
        }

        // Price Filter Toggle & Values
        fun isPriceFilterEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_PRICE_FILTER_ENABLED, true)
        }

        fun setPriceFilterEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_PRICE_FILTER_ENABLED, enabled).apply()
            syncSettingsToRoom(context)
        }

        fun getMinPrice(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_MIN_PRICE, DEFAULT_MIN_PRICE)
        }

        fun setMinPrice(context: Context, minPrice: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_MIN_PRICE, minPrice).apply()
        }

        fun getMaxPrice(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_MAX_PRICE, DEFAULT_MAX_PRICE)
        }

        fun setMaxPrice(context: Context, maxPrice: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_MAX_PRICE, maxPrice).apply()
        }

        // Passenger Rating Filter Toggle & Threshold
        fun isPassengerRatingFilterEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_RATING_FILTER_ENABLED, false)
        }

        fun setPassengerRatingFilterEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_RATING_FILTER_ENABLED, enabled).apply()
            syncSettingsToRoom(context)
        }

        fun getMinPassengerRating(context: Context): Float {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getFloat(KEY_MIN_PASSENGER_RATING, DEFAULT_MIN_PASSENGER_RATING)
        }

        fun setMinPassengerRating(context: Context, rating: Float) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putFloat(KEY_MIN_PASSENGER_RATING, rating).apply()
            syncSettingsToRoom(context)
        }

        // Blacklist Filter Toggle & Keywords
        fun isBlacklistEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_BLACKLIST_ENABLED, true)
        }

        fun setBlacklistEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_BLACKLIST_ENABLED, enabled).apply()
            syncSettingsToRoom(context)
        }

        fun getBlacklistKeywords(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_BLACKLIST_KEYWORDS, DEFAULT_BLACKLIST_KEYWORDS) ?: DEFAULT_BLACKLIST_KEYWORDS
        }

        fun setBlacklistKeywords(context: Context, keywords: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_BLACKLIST_KEYWORDS, keywords).apply()
            syncSettingsToRoom(context)
        }

fun getCustomSoundUri(context: Context): String? {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CUSTOM_SOUND_URI, null)
        }

        fun setCustomSoundUri(context: Context, uri: String?) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CUSTOM_SOUND_URI, uri).apply()
        }

        // Voice Announcer (TTS) Toggle & User Name
        fun isTtsEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_TTS_ENABLED, true)
        }

        fun setTtsEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_TTS_ENABLED, enabled).apply()
        }

        fun getUserName(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        }

        fun setUserName(context: Context, name: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_USER_NAME, name).apply()
        }

        fun initCache(context: Context) {
        }

        // Advanced Settings: Toggle Keywords and Apps
        fun getEnabledKeywords(context: Context): Set<String> {
            return setOf("Accept", "Swipe to Accept", "Take Order", "Confirm")
        }

        fun getEnabledApps(context: Context): Set<String> {
            return ALLOWED_RAPIDO_PACKAGES
        }

        // Multi-Language TTS Language (en, hi, gu, mr)
        fun getTtsLanguage(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_TTS_LANGUAGE, DEFAULT_TTS_LANGUAGE) ?: DEFAULT_TTS_LANGUAGE
        }

        fun setTtsLanguage(context: Context, lang: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_TTS_LANGUAGE, lang).apply()
            instance?.applyTtsLanguage(lang)
        }

        fun getLocaleForLanguage(lang: String): Locale {
            return when (lang.lowercase()) {
                "hi" -> Locale.forLanguageTag("hi-IN")
                "gu" -> Locale.forLanguageTag("gu-IN")
                "mr" -> Locale.forLanguageTag("mr-IN")
                else -> Locale.US
            }
        }

        fun generateOrderAnnouncement(
            context: Context,
            name: String,
            price: Int?,
            distanceKm: Float?,
            dropLocation: String? = null
        ): String {
            val lang = getTtsLanguage(context)
            val driverName = name.ifBlank { DEFAULT_USER_NAME }
            val priceStr = price?.toString() ?: "120"
            val distStr = distanceKm?.let { String.format(Locale.US, "%.1f", it) } ?: "1.5"

            return when (lang.lowercase()) {
                "hi" -> {
                    val dropStr = if (!dropLocation.isNullOrBlank()) " Drop $dropLocation par hai." else ""
                    "Hello $driverName, naya order aaya hai, $priceStr rupaye ka, pickup doori $distStr kilometer.$dropStr"
                }
                "gu" -> {
                    val dropStr = if (!dropLocation.isNullOrBlank()) " Drop $dropLocation par che." else ""
                    "Hello $driverName, nayo order aavyo che, $priceStr rupiya no, pickup antar $distStr kilometer.$dropStr"
                }
                "mr" -> {
                    val dropStr = if (!dropLocation.isNullOrBlank()) " Drop $dropLocation aahe." else ""
                    "Hello $driverName, navin order aali aahe, $priceStr rupaye, pickup antar $distStr kilometer.$dropStr"
                }
                else -> {
                    val dropStr = if (!dropLocation.isNullOrBlank()) " Drop location is $dropLocation." else ""
                    "Hello $driverName, new order received for $priceStr rupees, pickup distance $distStr kilometers.$dropStr"
                }
            }
        }

        fun isTargetRapidoOnly(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_TARGET_RAPIDO_ONLY, true)
        }

        fun setTargetRapidoOnly(context: Context, rapidoOnly: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_TARGET_RAPIDO_ONLY, rapidoOnly).apply()
        }

        fun isPassActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_IS_PASS_ACTIVE, false)
        }

        fun setPassActive(context: Context, active: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_IS_PASS_ACTIVE, active).apply()
        }

        fun isWakeLockEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_WAKE_LOCK_ENABLED, true)
        }

        fun setWakeLockEnabled(context: Context, enabled: Boolean) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(KEY_WAKE_LOCK_ENABLED, enabled).apply()
        }

        @Suppress("DEPRECATION")
        fun wakeUpScreenAndUnlock(context: Context) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (powerManager != null) {
                    val flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE
                    val wakeLock = powerManager.newWakeLock(flags, "AutoAccept:OrderWakeLock")
                    wakeLock.setReferenceCounted(false)
                    wakeLock.acquire(5000L)
                    Log.d(TAG, "Screen wake lock acquired for 5000ms to process incoming order.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error acquiring wake lock: ${e.message}", e)
            }
        }

        fun testVoiceAnnouncement(context: Context) {
            if (!isPassActive(context)) {
                _recentLog.value = "Voice test blocked: Pass is not active."
                return
            }
            val name = getUserName(context)
            val testMessage = generateOrderAnnouncement(
                context = context,
                name = name,
                price = 120,
                distanceKm = 1.5f,
                dropLocation = "Central Station"
            )
            instance?.speak(testMessage)
        }

        // =========================================================================
        // Parsing Helpers
        // =========================================================================

        /**
         * Safely checks if a node belongs to allowed Rapido packages.
         * Prevents parsing text from YouTube, Chrome, System UI, etc.
         */
        fun isNodeFromAllowedPackage(context: Context, node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            val pkg = node.packageName?.toString() ?: return false
            return getEnabledApps(context).contains(pkg)
        }

        fun extractAllScreenTexts(context: Context, rootNode: AccessibilityNodeInfo?): List<String> {
            if (rootNode == null) return emptyList()
            val texts = mutableListOf<String>()
            val queue = ArrayDeque<AccessibilityNodeInfo>()
            queue.add(rootNode)
            var visited = 0

            while (queue.isNotEmpty() && visited < 150) {
                val node = queue.removeFirst()
                visited++

                // Strict Package Check: Ignore any node belonging to YouTube, Chrome, System UI, etc.
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(context).contains(nodePkg)) {
                    continue
                }

                val text = node.text?.toString()?.trim()
                if (!text.isNullOrEmpty()) {
                    texts.add(text)
                }
                val desc = node.contentDescription?.toString()?.trim()
                if (!desc.isNullOrEmpty() && desc != text) {
                    texts.add(desc)
                }
                for (i in 0 until node.childCount) {
                    val child = try {
                        node.getChild(i)
                    } catch (e: Exception) {
                        null
                    }
                    if (child != null) {
                        queue.add(child)
                    }
                }
            }
            return texts
        }

        fun findBlacklistedKeyword(texts: List<String>, blacklistCsv: String): String? {
            val keywords = blacklistCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (keywords.isEmpty()) return null

            for (text in texts) {
                for (keyword in keywords) {
                    if (text.contains(keyword, ignoreCase = true)) {
                        return keyword
                    }
                }
            }
            return null
        }

        fun extractDistance(texts: List<String>): Float? {
            for (text in texts) {
                if (text.contains("pickup", ignoreCase = true) ||
                    text.contains("away", ignoreCase = true) ||
                    text.contains("pick up", ignoreCase = true)
                ) {
                    val match = DISTANCE_REGEX.find(text)
                    if (match != null) {
                        val valueStr = match.groupValues.getOrNull(1)
                        val value = valueStr?.toFloatOrNull()
                        if (value != null) return value
                    }
                }
            }

            for (text in texts) {
                val match = DISTANCE_REGEX.find(text)
                if (match != null) {
                    val valueStr = match.groupValues.getOrNull(1)
                    val value = valueStr?.toFloatOrNull()
                    if (value != null) return value
                }
            }
            return null
        }

        fun extractPrice(texts: List<String>): Float? {
            for (text in texts) {
                val match = PRICE_REGEX.find(text)
                if (match != null) {
                    val str1 = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    val str2 = match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
                    val price = (str1 ?: str2)?.toFloatOrNull()
                    if (price != null) return price
                }
            }
            return null
        }

        fun extractPassengerRating(texts: List<String>): Float? {
            val starRegex = Regex("""(?:★|⭐)\s*([1-5](?:\.\d{1,2})?)|([1-5](?:\.\d{1,2})?)\s*(?:★|⭐)""")
            val ratingWordRegex = Regex("""(?:rating|rated)\s*[:\-]?\s*([1-5](?:\.\d{1,2})?)|([1-5](?:\.\d{1,2})?)\s*(?:rating|rated)""", RegexOption.IGNORE_CASE)

            for (text in texts) {
                starRegex.find(text)?.let { m ->
                    val str = m.groupValues[1].ifEmpty { m.groupValues[2] }
                    val rating = str.toFloatOrNull()
                    if (rating != null && rating in 1.0f..5.0f) return rating
                }
                ratingWordRegex.find(text)?.let { m ->
                    val str = m.groupValues[1].ifEmpty { m.groupValues[2] }
                    val rating = str.toFloatOrNull()
                    if (rating != null && rating in 1.0f..5.0f) return rating
                }
            }
            return null
        }

        fun extractPickupLocation(texts: List<String>): String? {
            val pickupPrefixRegex = Regex("""(?:pickup(?:\s*from|\s*at)?|from|pick\s*up)\s*[:\-]?\s*(.+)$""", RegexOption.IGNORE_CASE)
            for ((index, text) in texts.withIndex()) {
                val match = pickupPrefixRegex.find(text)
                if (match != null) {
                    val extracted = match.groupValues.getOrNull(1)?.trim()
                    if (!extracted.isNullOrBlank() && !extracted.contains("₹") && !extracted.contains("km", ignoreCase = true)) {
                        return extracted.take(40)
                    }
                }

                if (text.equals("Pickup", ignoreCase = true) || text.equals("Pick up", ignoreCase = true) || text.equals("From", ignoreCase = true)) {
                    val nextText = texts.getOrNull(index + 1)?.trim()
                    if (!nextText.isNullOrBlank() &&
                        !nextText.equals("Accept", ignoreCase = true) &&
                        !nextText.contains("₹") &&
                        !nextText.contains("km", ignoreCase = true) &&
                        !nextText.equals("Drop", ignoreCase = true) &&
                        !nextText.equals("Drop off", ignoreCase = true)
                    ) {
                        return nextText.take(40)
                    }
                }
            }
            return null
        }

        fun extractDropLocation(texts: List<String>): String? {
            val dropPrefixRegex = Regex("""(?:drop(?:\s*off|\s*to|\s*at)?|to)\s*[:\-]?\s*(.+)$""", RegexOption.IGNORE_CASE)
            for ((index, text) in texts.withIndex()) {
                val match = dropPrefixRegex.find(text)
                if (match != null) {
                    val extracted = match.groupValues.getOrNull(1)?.trim()
                    if (!extracted.isNullOrBlank() && !extracted.contains("₹") && !extracted.contains("km", ignoreCase = true)) {
                        return extracted.take(40)
                    }
                }

                if (text.equals("Drop", ignoreCase = true) || text.equals("Drop off", ignoreCase = true) || text.equals("To", ignoreCase = true)) {
                    val nextText = texts.getOrNull(index + 1)?.trim()
                    if (!nextText.isNullOrBlank() &&
                        !nextText.equals("Accept", ignoreCase = true) &&
                        !nextText.contains("₹") &&
                        !nextText.contains("km", ignoreCase = true)
                    ) {
                        return nextText.take(40)
                    }
                }
            }
            return null
        }

        fun loadLocalLogs(context: Context) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val jsonString = prefs.getString(KEY_LOCAL_RIDE_LOGS, null)
                if (jsonString.isNullOrBlank()) {
                    _localRideLogs.value = emptyList()
                    return
                }
                val jsonArray = JSONArray(jsonString)
                val list = ArrayList<RideLogItem>(jsonArray.length())
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        RideLogItem(
                            id = obj.optString("id", "local_$i"),
                            userId = obj.optString("userId", "local_driver"),
                            price = obj.optDouble("price", 0.0).toFloat(),
                            pickupKm = obj.optDouble("pickupKm", 0.0).toFloat(),
                            pickupLocation = obj.optString("pickupLocation", "Nearby Pickup"),
                            dropLocation = obj.optString("dropLocation", "Destination Drop"),
                            status = obj.optString("status", "IGNORED"),
                            reason = obj.optString("reason", ""),
                            sourcePackage = obj.optString("sourcePackage", RAPIDO_CAPTAIN_PACKAGE),
                            createdMillis = obj.optLong("createdMillis", System.currentTimeMillis())
                        )
                    )
                }
                _localRideLogs.value = list
                Log.d(TAG, "Loaded ${list.size} ride logs from local storage.")
                if (list.isNotEmpty()) {
                    val restoredEvents = list.take(15).map { item ->
                        val isAccepted = item.status.equals("ACCEPTED", ignoreCase = true)
                        ServiceEvent(
                            id = item.id,
                            timestamp = item.createdMillis,
                            type = if (isAccepted) ServiceEventType.ORDER_ACCEPTED else ServiceEventType.ORDER_IGNORED,
                            title = if (isAccepted) "Order accepted" else "Order ignored",
                            description = "₹${item.price.toInt()} • ${item.pickupKm} km • ${item.pickupLocation} → ${item.dropLocation}",
                            details = item.reason,
                            badge = if (isAccepted) "ACCEPTED" else "FILTERED"
                        )
                    }
                    _serviceEvents.value = restoredEvents
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading local logs: ${e.message}", e)
            }
        }

        fun logRideLocally(
            context: Context,
            price: Float,
            pickupKm: Float,
            pickupLocation: String,
            dropLocation: String,
            status: String,
            reason: String,
            sourcePackage: String = RAPIDO_CAPTAIN_PACKAGE
        ) {
            try {
                val newLog = RideLogItem(
                    id = "local_${System.currentTimeMillis()}_${(1000..9999).random()}",
                    userId = "local_driver",
                    price = price,
                    pickupKm = pickupKm,
                    pickupLocation = pickupLocation.ifBlank { "Nearby Pickup" },
                    dropLocation = dropLocation.ifBlank { "Destination Drop" },
                    status = status,
                    reason = reason,
                    sourcePackage = sourcePackage,
                    createdMillis = System.currentTimeMillis()
                )
                val currentList = _localRideLogs.value
                val updatedList = listOf(newLog) + currentList.take(99) // Keep last 100 entries
                _localRideLogs.value = updatedList

                saveLocalLogs(context, updatedList)
                Log.d(TAG, "Ride logged locally [$status]: ${newLog.id} (Fare: ₹${price.toInt()})")

                // Update order status in Firestore with exponential backoff retry for transient connectivity loss
                updateOrderStatus(
                    context = context,
                    orderId = newLog.id,
                    status = status,
                    price = price,
                    pickupKm = pickupKm,
                    pickupLocation = pickupLocation,
                    dropLocation = dropLocation,
                    reason = reason,
                    sourcePackage = sourcePackage,
                    createdMillis = newLog.createdMillis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error logging ride locally: ${e.message}", e)
            }
        }

        private fun saveLocalLogs(context: Context, logs: List<RideLogItem>) {
            try {
                val jsonArray = JSONArray()
                for (item in logs) {
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("userId", item.userId)
                        put("price", item.price.toDouble())
                        put("pickupKm", item.pickupKm.toDouble())
                        put("pickupLocation", item.pickupLocation)
                        put("dropLocation", item.dropLocation)
                        put("status", item.status)
                        put("reason", item.reason)
                        put("sourcePackage", item.sourcePackage)
                        put("createdMillis", item.createdMillis)
                    }
                    jsonArray.put(obj)
                }
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_LOCAL_RIDE_LOGS, jsonArray.toString()).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error saving local logs to SharedPreferences: ${e.message}", e)
            }
        }

        /**
         * Serializes a list of RideLogItem into RFC 4180 CSV format with UTF-8 BOM.
         */
        fun exportRideLogsToCsv(logsToExport: List<RideLogItem>): String {
            val sb = StringBuilder()
            sb.append("\uFEFF") // Prepend UTF-8 BOM for spreadsheet viewers
            val headers = listOf(
                "Order_ID",
                "Timestamp_Millis",
                "Date_Time",
                "Status",
                "Fare_INR",
                "Distance_KM",
                "Pickup_Location",
                "Drop_Location",
                "Reason",
                "Source_Package"
            )
            sb.appendLine(headers.joinToString(","))

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            for (item in logsToExport) {
                fun esc(s: String?): String {
                    if (s == null) return ""
                    val t = s.trim()
                    return if (t.contains(",") || t.contains("\"") || t.contains("\n") || t.contains("\r")) {
                        "\"" + t.replace("\"", "\"\"") + "\""
                    } else t
                }
                val formattedTime = try { sdf.format(Date(item.createdMillis)) } catch (e: Exception) { "" }
                val row = listOf(
                    esc(item.id),
                    esc(item.createdMillis.toString()),
                    esc(formattedTime),
                    esc(item.status),
                    esc("%.2f".format(Locale.US, item.price)),
                    esc("%.1f".format(Locale.US, item.pickupKm)),
                    esc(item.pickupLocation),
                    esc(item.dropLocation),
                    esc(item.reason),
                    esc(item.sourcePackage)
                )
                sb.appendLine(row.joinToString(","))
            }
            return sb.toString()
        }

        /**
         * Exports order history logs to CSV and invokes the Android share sheet.
         */
        fun shareRideLogsAsCsv(
            context: Context,
            logsToShare: List<RideLogItem>,
            subjectTitle: String = "Rapido Auto Accept - Order History (CSV)"
        ): Result<File> {
            return runCatching {
                val csv = exportRideLogsToCsv(logsToShare)
                val exportDir = File(context.cacheDir, "debug_exports").apply { mkdirs() }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val file = File(exportDir, "rapido_order_history_$timeStamp.csv")
                file.writeText(csv, Charsets.UTF_8)

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                    putExtra(Intent.EXTRA_TEXT, "Attached is the order history with ${logsToShare.size} orders processed by Rapido Auto Accept.")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(intent, "Share Order History (CSV)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(chooser)
                file
            }
        }

        /**
         * Generic exponential backoff retry mechanism.
         * Retries the provided suspend block when transient failures occur (such as lost network or Firestore connectivity),
         * doubling the delay on each failure up to maxDelayMs, with jitter.
         */
        suspend fun <T> retryWithExponentialBackoff(
            maxRetries: Int = FIRESTORE_MAX_RETRIES,
            initialDelayMs: Long = FIRESTORE_INITIAL_DELAY_MS,
            maxDelayMs: Long = FIRESTORE_MAX_DELAY_MS,
            factor: Double = FIRESTORE_BACKOFF_FACTOR,
            jitterFactor: Double = 0.1,
            onRetry: ((attempt: Int, nextDelayMs: Long, error: Throwable) -> Unit)? = null,
            block: suspend (attempt: Int) -> T
        ): T {
            var currentDelay = initialDelayMs
            for (attempt in 1..maxRetries) {
                try {
                    return block(attempt)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    if (attempt >= maxRetries) {
                        try {
                            Log.e(TAG, "Exhausted all $maxRetries retry attempts: ${e.message}", e)
                        } catch (_: Throwable) {}
                        throw e
                    }
                    val jitter = ((Math.random() * 2 - 1) * jitterFactor * currentDelay).toLong()
                    val delayWithJitter = (currentDelay + jitter).coerceIn(0L, maxDelayMs)
                    onRetry?.invoke(attempt, delayWithJitter, e)
                    delay(delayWithJitter)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
                }
            }
            error("Retry loop terminated unexpectedly")
        }

        /**
         * Updates or writes order status to Firestore with an exponential backoff retry mechanism
         * when network or Firestore connectivity is temporarily lost.
         */
        suspend fun updateOrderStatusWithRetry(
            context: Context,
            orderId: String,
            status: String,
            price: Float = 0f,
            pickupKm: Float = 0f,
            pickupLocation: String = "",
            dropLocation: String = "",
            reason: String = "",
            sourcePackage: String = RAPIDO_CAPTAIN_PACKAGE,
            createdMillis: Long = System.currentTimeMillis(),
            maxRetries: Int = FIRESTORE_MAX_RETRIES,
            initialDelayMs: Long = FIRESTORE_INITIAL_DELAY_MS,
            maxDelayMs: Long = FIRESTORE_MAX_DELAY_MS
        ): Result<Unit> {
            return try {
                FirebaseHelper.initialize(context)
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val msg = "Firebase not initialized. Cannot update order status in Firestore."
                    Log.w(TAG, msg)
                    return Result.failure(IllegalStateException(msg))
                }

                val firestore = FirebaseFirestore.getInstance()
                val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
                val userId = auth?.currentUser?.uid ?: "local_driver"

                val orderData = hashMapOf<String, Any>(
                    "id" to orderId,
                    "status" to status,
                    "price" to price.toDouble(),
                    "pickupKm" to pickupKm.toDouble(),
                    "pickupLocation" to pickupLocation.ifBlank { "Nearby Pickup" },
                    "dropLocation" to dropLocation.ifBlank { "Destination Drop" },
                    "reason" to reason,
                    "sourcePackage" to sourcePackage,
                    "createdMillis" to (if (createdMillis > 0) createdMillis else System.currentTimeMillis()),
                    "updatedMillis" to System.currentTimeMillis(),
                    "timestamp" to FieldValue.serverTimestamp(),
                    "userId" to userId
                )

                retryWithExponentialBackoff(
                    maxRetries = maxRetries,
                    initialDelayMs = initialDelayMs,
                    maxDelayMs = maxDelayMs,
                    factor = FIRESTORE_BACKOFF_FACTOR,
                    onRetry = { attempt, nextDelayMs, error ->
                        val warning = "Firestore connectivity lost on attempt $attempt while updating order $orderId to '$status': ${error.message}. Retrying in ${nextDelayMs}ms..."
                        Log.w(TAG, warning)
                        _recentLog.value = "Firestore retry #$attempt in ${nextDelayMs / 1000}s ($status)"
                    }
                ) { attempt ->
                    Log.d(TAG, "Attempt $attempt: Syncing order $orderId [$status] with Firestore...")
                    firestore.collection(FIRESTORE_ORDERS_COLLECTION)
                        .document(orderId)
                        .set(orderData, SetOptions.merge())
                        .await()
                    Log.i(TAG, "Order $orderId status successfully updated to '$status' in Firestore on attempt $attempt")
                }

                _recentLog.value = "Firestore order status synced: $orderId [$status]"
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Failed to update order status to '$status' in Firestore for order $orderId after $maxRetries attempts: ${e.message}", e)
                Result.failure(e)
            }
        }

        /**
         * Asynchronously updates order status to Firestore using the exponential backoff retry mechanism.
         * Safe to call from any thread or Coroutine context.
         */
        fun updateOrderStatus(
            context: Context,
            orderId: String,
            status: String,
            price: Float = 0f,
            pickupKm: Float = 0f,
            pickupLocation: String = "",
            dropLocation: String = "",
            reason: String = "",
            sourcePackage: String = RAPIDO_CAPTAIN_PACKAGE,
            createdMillis: Long = System.currentTimeMillis(),
            onComplete: ((Boolean, Throwable?) -> Unit)? = null
        ) {
            serviceScope.launch {
                val result = updateOrderStatusWithRetry(
                    context = context,
                    orderId = orderId,
                    status = status,
                    price = price,
                    pickupKm = pickupKm,
                    pickupLocation = pickupLocation,
                    dropLocation = dropLocation,
                    reason = reason,
                    sourcePackage = sourcePackage,
                    createdMillis = createdMillis
                )
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { onComplete?.invoke(true, null) },
                        onFailure = { onComplete?.invoke(false, it) }
                    )
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        FirebaseHelper.initialize(this)
        RemoteConfigManager.init(this)
        instance = this
        serviceJob.cancel()
        serviceJob = SupervisorJob()
        serviceScopeInstance = CoroutineScope(Dispatchers.Main + serviceJob)
        _isServiceRunning.value = true
        _recentLog.value = "Smart Clicker active. Monitoring $RAPIDO_CAPTAIN_PACKAGE"
        logServiceEvent(
            type = ServiceEventType.MONITORING,
            title = "Monitoring active",
            description = "Watching $RAPIDO_CAPTAIN_PACKAGE for incoming ride overlays",
            details = "Accessibility layer connected",
            badge = "ONLINE"
        )
        Log.i(TAG, "AutoAcceptService connected. Initializing Foreground Service & TextToSpeech...")
        StatusOverlayManager.show(this)
        BoundingBoxManager.show(this)

        // Start Foreground Service notification to ensure persistence in background
        startForegroundNotification()
        ServiceStatusWidgetProvider.updateAllWidgets(this)

        try {
            textToSpeech = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating TextToSpeech: ${e.message}", e)
        }
    }
    
    private fun playSuccessSound() {
        try {
            val uriStr = getCustomSoundUri(this)
            if (!uriStr.isNullOrEmpty()) {
                val uri = android.net.Uri.parse(uriStr)
                val ringtone = android.media.RingtoneManager.getRingtone(this, uri)
                ringtone?.play()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play custom sound: ${e.message}")
        }
    }

    private fun triggerSuccessVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger vibration: ${e.message}")
        }
    }

    /**
     * Builds the persistent Foreground Service notification with current status.
     */
    fun buildForegroundNotification(context: Context, customStatusText: String? = null): android.app.Notification {
        createNotificationChannels(context)

        val isEnabled = isAutomationEnabled(context)
        val isVoiceOnly = isVoiceOnlyMode(context)

        val title: String
        val defaultMessage: String
        val colorInt: Int

        when {
            !isEnabled -> {
                title = "Auto-Accept: Paused ⏸️"
                defaultMessage = "Service standby. Master auto-accept switch is OFF."
                colorInt = 0xFF6B7280.toInt() // Gray
            }
            isVoiceOnly -> {
                title = "Auto-Accept: Passive Radar (Voice Only) 🎙️"
                defaultMessage = "Announcing matching orders via voice without auto-clicking."
                colorInt = 0xFFEF4444.toInt() // Red
            }
            else -> {
                title = "Auto-Accept: Active & Monitoring 🟢"
                defaultMessage = "Actively monitoring screen for incoming rides matching criteria."
                colorInt = 0xFF10B981.toInt() // Green
            }
        }

        val contentText = customStatusText ?: defaultMessage

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bigText = buildString {
            append("• Status: ").append(title)
            append("\n• Message: ").append(contentText)
            append("\n• Mode: ").append(if (isVoiceOnly) "Voice Announcer Only (Passive Radar)" else "Auto-Clicker Active")
            append("\n• Master Switch: ").append(if (isEnabled) "ON" else "OFF")
            append("\n• Protection: Foreground Service active to prevent Android kill during rides")
        }

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setColor(colorInt)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startForegroundNotification() {
        try {
            val accActive = isServiceConnected()
            val notifActive = isNotificationListenerEnabled(this)
            val batActive = isBatteryOptimizationIgnored(this)
            val masterOn = isAutomationEnabled(this)
            
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            if (accActive && notifActive && batActive && masterOn) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    stopForeground(android.app.Service.STOP_FOREGROUND_REMOVE)
                } else {
                    stopForeground(true)
                }
                notificationManager.cancel(NOTIFICATION_ID)
                android.util.Log.i(TAG, "All services healthy. Removed persistent foreground notification.")
            } else {
                ServiceStatusNotificationManager.startOrUpdateForeground(this)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to manage foreground notification: ${e.message}", e)
        }

        if (isAutomationEnabled(this)) {
            
        }
        ServiceStatusNotificationManager.updateStatus(this)
    }
    /**
     * Updates the foreground notification text dynamically during ride lifecycle.
     */
    fun updateForegroundNotification(statusText: String? = null) {
        try {
            val notification = buildForegroundNotification(this, statusText)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update foreground notification: ${e.message}")
        }
    }

    fun applyTtsLanguage(lang: String) {
        if (textToSpeech != null && isTtsInitialized) {
            val locale = getLocaleForLanguage(lang)
            val result = textToSpeech?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "TTS language $lang not supported on device; fallback to default/US")
                textToSpeech?.language = Locale.US
            } else {
                Log.i(TAG, "TTS Language successfully switched to: $locale")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            applyTtsLanguage(getTtsLanguage(this))
            textToSpeech?.setSpeechRate(1.0f)
            textToSpeech?.setPitch(1.0f)
            Log.i(TAG, "TextToSpeech initialized successfully with language ${getTtsLanguage(this)}.")
        } else {
            Log.e(TAG, "TextToSpeech initialization failed with code: $status")
            isTtsInitialized = false
        }
    }

    fun speak(text: String) {
        if (!isVoiceOnlyMode(this) && !isPassActive(this)) {
            Log.w(TAG, "Pass not active and Voice-Only Mode not active. TTS announcement blocked.")
            return
        }
        if (textToSpeech != null && isTtsInitialized) {
            try {
                applyTtsLanguage(getTtsLanguage(this))
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AutoAccept_${System.currentTimeMillis()}")
                Log.d(TAG, "TTS announced: $text")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to speak TTS message: ${e.message}")
            }
        } else {
            Log.d(TAG, "TTS requested but not ready yet: $text")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. MASTER SWITCH: Stop immediately if master auto-accept is turned off (Requirement 6)
        if (!isAutomationEnabled(this)) {
            return
        }

// SAFETY CHECK: If flag has been stuck for > 5s, auto-reset
        if (isGenuineOrderIncoming && System.currentTimeMillis() - genuineOrderIncomingTimestamp > 5000L) {
            Log.w(TAG, "onAccessibilityEvent: isGenuineOrderIncoming flag was stuck > 5s. Auto-resetting.")
            isGenuineOrderIncoming = false
        }
        // 2. OVERLAY EVENT LISTENER & TARGET PACKAGE LOCK:
        // Prioritize overlay and state transition events: TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            return
        }

        val eventPackage = event.packageName?.toString() ?: ""

        if (eventPackage.isEmpty() ||
            eventPackage == packageName ||
            eventPackage == "android" ||
            eventPackage == "com.android.systemui"
        ) {
            return
        }

        // Strict early return if event does not originate from allowed Rapido packages
        if (isTargetRapidoOnly(this) && !getEnabledApps(this@AutoAcceptService).contains(eventPackage)) {
            return
        }

        // 3. DEBOUNCE COOLDOWN
        val now = SystemClock.uptimeMillis()
        if (now - lastClickTimestamp < CLICK_COOLDOWN_MS) {
            return
        }

        if (now - lastScanTimestamp < 1000L) return
        lastScanTimestamp = now

        // 4. FIND TRUE ROOT OF EVENT'S WINDOW:
        // Safely ascends to the root of the event's window while blocking background apps
        var windowRoot = try { event.source } catch (e: Exception) { null }
        while (windowRoot?.parent != null) {
            windowRoot = windowRoot.parent
        }
        val targetNode = windowRoot ?: try {
            rootInActiveWindow
        } catch (e: Exception) {
            Log.w(TAG, "Cannot access root window: ${e.message}")
            null
        } ?: return

        val nodePackage = targetNode.packageName?.toString() ?: eventPackage
        if (isTargetRapidoOnly(this) && !getEnabledApps(this@AutoAcceptService).contains(nodePackage)) {
            return
        }

        try {
            processActiveWindow(targetNode, if (nodePackage.isNotEmpty()) nodePackage else eventPackage)
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating active window/source: ${e.message}", e)
        }
    }

    /**
     * Inspects active window, verifies ride details, applies filter policies, and schedules pending accept.
     */
    private fun sendAutoAcceptNotification(context: Context, title: String, message: String) {
        createNotificationChannels(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val builder = androidx.core.app.NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        manager.notify(ALERT_NOTIFICATION_ID, builder.build())
    }

        private fun processActiveWindow(rootNode: AccessibilityNodeInfo, sourcePackage: String) {
        val acceptButtons = findAllAcceptButtons(rootNode)
        
        if (acceptButtons.isEmpty()) {
            return
        }

        acquireCpuWakeLock(timeoutMs = 15000L, reason = "Incoming ride detected on screen")

        var bestRide: CapturedRide? = null
        var bestButton: ValidatedButton? = null
        var bestBoxBounds: android.graphics.Rect? = null
        var bestCardContainer: AccessibilityNodeInfo? = null

        for (validButton in acceptButtons) {
            val cardContainer = findCardContainer(validButton.node, maxLevels = 5)
            
            val cardTexts = extractAllScreenTexts(this, cardContainer)

            // STRICT CARD VALIDATION
            val cardTextCombined = cardTexts.joinToString(" ").lowercase()
            val hasFareIndicator = Regex("(₹|rs|inr)").containsMatchIn(cardTextCombined)
            val hasDistanceIndicator = Regex("\\bkm\\b").containsMatchIn(cardTextCombined)
            if (!hasFareIndicator || !hasDistanceIndicator) {
                continue
            }

            val parsedDistance = extractDistance(cardTexts)
            val parsedPrice = extractPrice(cardTexts)
            val parsedRating = extractPassengerRating(cardTexts)
            val parsedPickup = extractPickupLocation(cardTexts) ?: "Nearby Pickup"
            val parsedDrop = extractDropLocation(cardTexts) ?: "Destination Drop"

            val fareInfo = parsedPrice?.let { "₹${it.toInt()}" } ?: "Fare ~"
            val distInfo = parsedDistance?.let { "${it} km" } ?: "Dist ~"
            val ratingInfo = parsedRating?.let { "★ $it" } ?: "★ ~"

            val rideSignature = generateRideSignature(sourcePackage, parsedPrice, parsedDistance, parsedPickup, parsedDrop)

            val lastRejected = recentlyRejectedRides[rideSignature]
            if (lastRejected != null && System.currentTimeMillis() - lastRejected < REJECT_COOLDOWN_MS) continue

            val lastAccepted = recentlyAcceptedRides[rideSignature]
            if (lastAccepted != null && System.currentTimeMillis() - lastAccepted < DUPLICATE_COOLDOWN_MS) {
                Log.d(TAG, "Duplicate ride already accepted recently: $rideSignature")
                continue
            }

            if (pendingAcceptJob?.isActive == true && pendingRideSignature == rideSignature) {
                continue
            }

            logServiceEvent(
                type = ServiceEventType.RIDE_DETECTED,
                title = "Ride detected",
                description = "$fareInfo • $distInfo • $parsedPickup → $parsedDrop",
                details = "Card texts: ${cardTexts.take(3).joinToString(", ")}",
                badge = "DETECTED"
            )

            val isPremium = isPassActive(this)
            val configuredDelay = getAcceptDelayMs(this)
            val delayMs: Long
            val acceptReason: String

            if (!isPremium) {
                delayMs = configuredDelay.coerceAtLeast(500L)
                acceptReason = "Auto-Accepted (Free Mode - ${delayMs}ms Delay)"
                Log.d(TAG, "Free Mode active: Skipping filters, queued with ${delayMs}ms delay")
            } else {
                delayMs = configuredDelay

                val isBlacklistFilterOn = isBlacklistEnabled(this)
                val isDistanceFilterOn = isDistanceFilterEnabled(this)
                val isPriceFilterOn = isPriceFilterEnabled(this)
                val isRatingFilterOn = isPassengerRatingFilterEnabled(this)
                val areAllFiltersOff = !isBlacklistFilterOn && !isDistanceFilterOn && !isPriceFilterOn && !isRatingFilterOn

                if (areAllFiltersOff) {
                    Log.d(TAG, "Accept All Override active: All individual filters OFF -> accepting without parameter checks.")
                    acceptReason = "Auto-Accepted (Premium Accept All - ${delayMs}ms)"
                } else {
                    var filterFailed = false
                    
                    if (isBlacklistFilterOn) {
                        val blacklistCsv = getBlacklistKeywords(this)
                        val matchedKeyword = findBlacklistedKeyword(cardTexts, blacklistCsv)
                        if (matchedKeyword != null) {
                            val logMsg = "Order REJECTED: Blacklisted keyword matched ('$matchedKeyword')"
                            Log.i(TAG, logMsg)
                            _recentLog.value = logMsg
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Order rejected (Blacklist)",
                                description = "Location matched '$matchedKeyword' ($fareInfo)",
                                details = "Drop: $parsedDrop",
                                badge = "REJECTED"
                            )
                            logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Blacklisted location: $matchedKeyword", sourcePackage)
                            filterFailed = true
                        }
                    }

                    if (!filterFailed && isDistanceFilterOn) {
                        val maxDistance = getMaxDistanceKm(this)
                        if (maxDistance > 0f && parsedDistance != null && parsedDistance > maxDistance) {
                            val logMsg = "Order REJECTED: Distance ${parsedDistance} km > Max ${maxDistance} km"
                            Log.i(TAG, logMsg)
                            _recentLog.value = logMsg
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Order rejected (Distance)",
                                description = "Pickup ${parsedDistance} km > Max ${maxDistance} km limit",
                                details = "Pickup: $parsedPickup",
                                badge = "REJECTED"
                            )
                            logRideLocally(this, parsedPrice ?: 0f, parsedDistance, parsedPickup, parsedDrop, "IGNORED", "Distance too high (${parsedDistance} km > ${maxDistance} km)", sourcePackage)
                            filterFailed = true
                        }
                    }

                    if (!filterFailed && isPriceFilterOn) {
                        val minPrice = getMinPrice(this)
                        val maxPrice = getMaxPrice(this)
                        
                        if (parsedPrice != null) {
                            if (minPrice > 0f && parsedPrice < minPrice) {
                                val logMsg = "Order REJECTED: Fare ₹${parsedPrice.toInt()} < Min ₹${minPrice.toInt()}"
                                Log.i(TAG, logMsg)
                                _recentLog.value = logMsg
                                logServiceEvent(
                                    type = ServiceEventType.ORDER_IGNORED,
                                    title = "Order rejected (Fare Low)",
                                    description = "Fare ₹${parsedPrice.toInt()} < Min ₹${minPrice.toInt()}",
                                    details = "Pickup: $parsedPickup",
                                    badge = "REJECTED"
                                )
                                logRideLocally(this, parsedPrice, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Price below minimum (₹${parsedPrice.toInt()} < ₹${minPrice.toInt()})", sourcePackage)
                                filterFailed = true
                            } else if (maxPrice > 0f && parsedPrice > maxPrice) {
                                val logMsg = "Order REJECTED: Fare ₹${parsedPrice.toInt()} > Max ₹${maxPrice.toInt()}"
                                Log.i(TAG, logMsg)
                                _recentLog.value = logMsg
                                logServiceEvent(
                                    type = ServiceEventType.ORDER_IGNORED,
                                    title = "Order rejected (Fare High)",
                                    description = "Fare ₹${parsedPrice.toInt()} > Max ₹${maxPrice.toInt()}",
                                    details = "Pickup: $parsedPickup",
                                    badge = "REJECTED"
                                )
                                logRideLocally(this, parsedPrice, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Price above maximum (₹${parsedPrice.toInt()} > ₹${maxPrice.toInt()})", sourcePackage)
                                filterFailed = true
                            }
                        }
                    }

                    if (!filterFailed && isRatingFilterOn) {
                        val minRating = getMinPassengerRating(this)
                        if (parsedRating != null && parsedRating < minRating) {
                            val logMsg = "Order REJECTED: Passenger rating ${parsedRating}★ < Min ${minRating}★"
                            Log.i(TAG, logMsg)
                            _recentLog.value = logMsg
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Order rejected (Rating Low)",
                                description = "Passenger rating ${parsedRating}★ < Min ${minRating}★",
                                details = "Pickup: $parsedPickup",
                                badge = "REJECTED"
                            )
                            logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Passenger rating below threshold (${parsedRating}★ < ${minRating}★)", sourcePackage)
                            filterFailed = true
                        }
                    }

                    // Room Database: Custom Filtering Rules evaluation
                    if (!filterFailed) {
                        try {
                            val db = com.example.data.AppDatabase.getDatabase(this)
                            val activeRules = db.settingsDao().getActiveCustomRulesSync()
                            if (activeRules.isNotEmpty()) {
                                var matchedAny = false
                                for (rule in activeRules) {
                                    var violates = false
                                    if (rule.isMinFareEnabled && parsedPrice != null && parsedPrice < rule.minFare) {
                                        violates = true
                                    }
                                    if (rule.isMaxDistanceEnabled && parsedDistance != null && parsedDistance > rule.maxDistanceKm) {
                                        violates = true
                                    }
                                    if (rule.isRatingFilterEnabled && parsedRating != null && parsedRating < rule.minPassengerRating) {
                                        violates = true
                                    }
                                    if (rule.destinationKeyword.isNotBlank()) {
                                        val matchesWord = cardTexts.any { it.contains(rule.destinationKeyword, ignoreCase = true) }
                                        if (!matchesWord) violates = true
                                    }
                                    if (!violates) {
                                        matchedAny = true
                                        break
                                    }
                                }
                                if (!matchedAny) {
                                    val logMsg = "Order REJECTED: Did not match active custom rules in Room DB"
                                    Log.i(TAG, logMsg)
                                    _recentLog.value = logMsg
                                    logServiceEvent(
                                        type = ServiceEventType.ORDER_IGNORED,
                                        title = "Order rejected (Custom Rule)",
                                        description = "Did not meet criteria of active custom filtering rules",
                                        details = "Fare: $fareInfo • Dist: $distInfo",
                                        badge = "REJECTED"
                                    )
                                    logRideLocally(this, parsedPrice ?: 0f, parsedDistance ?: 0f, parsedPickup, parsedDrop, "IGNORED", "Filtered out by custom Room rules", sourcePackage)
                                    filterFailed = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Room custom rule check note: ${e.message}")
                        }
                    }

                    if (filterFailed) {
                        recentlyRejectedRides[rideSignature] = System.currentTimeMillis()
                        continue
                    }
                    acceptReason = "Auto-Accepted (Filters Passed - ${delayMs}ms)"
                }
            }

            val capturedRide = CapturedRide(
                sourcePackage = sourcePackage,
                price = parsedPrice,
                distanceKm = parsedDistance,
                pickup = parsedPickup,
                drop = parsedDrop,
                signature = rideSignature,
                acceptReason = acceptReason,
                isPremium = isPremium,
                delayMs = delayMs
            )

            // Batch filtering: Pick highest payout
            if (bestRide == null || (capturedRide.price ?: 0f) > (bestRide.price ?: 0f)) {
                bestRide = capturedRide
                bestButton = validButton
                val tBounds = android.graphics.Rect()
                if (cardContainer != null) {
                    cardContainer.getBoundsInScreen(tBounds)
                } else {
                    tBounds.set(validButton.targetBounds)
                }
                bestBoxBounds = tBounds
                bestCardContainer = cardContainer
            }
        } // End of acceptButtons loop

        if (bestRide != null && bestButton != null) {
            val capturedRide = bestRide
            val validButton = bestButton
            val delayMs = capturedRide.delayMs
            val isPremium = capturedRide.isPremium
            val sourcePackage = capturedRide.sourcePackage
            val parsedPrice = capturedRide.price
            val parsedDistance = capturedRide.distanceKm
            val fareInfo = parsedPrice?.let { "₹${it.toInt()}" } ?: "Fare ~"
            val distInfo = parsedDistance?.let { "${it} km" } ?: "Dist ~"
            val rideSignature = capturedRide.signature

            BoundingBoxManager.updateBox(bestBoxBounds)
            StatusOverlayManager.updateLastPrice(parsedPrice)
            updateForegroundNotification("Processing ride: $fareInfo ($distInfo)")

            if (isWakeLockEnabled(this)) {
                wakeUpScreenAndUnlock(this)
            }

            val engineMode = if (isPremium) "[PREMIUM ${delayMs}ms]" else "[FREE ${delayMs}ms]"
            _recentLog.value = "Accept scheduled in $sourcePackage [$fareInfo | $distInfo] $engineMode"
            logServiceEvent(
                type = ServiceEventType.ORDER_QUEUED,
                title = "Accept scheduled",
                description = "Tapping in ${delayMs}ms $engineMode",
                details = "$fareInfo • $distInfo",
                badge = "SCHEDULED"
            )

            pendingRideSignature = rideSignature
            pendingAcceptJob?.cancel() 
            pendingAcceptJob = serviceScopeInstance.launch {
                try {
                    if (delayMs > 0) delay(delayMs)

                    if (!isAutomationEnabled(this@AutoAcceptService)) {
                        return@launch
                    }

                    val recentAcceptedCheck = recentlyAcceptedRides[capturedRide.signature]
                    if (recentAcceptedCheck != null && System.currentTimeMillis() - recentAcceptedCheck < DUPLICATE_COOLDOWN_MS) {
                        Log.w(TAG, "Pending accept cancelled: duplicate cooldown active")
                        return@launch
                    }

                    if (isVoiceOnlyMode(this@AutoAcceptService)) {
                        lastClickTimestamp = SystemClock.uptimeMillis()
                        isGenuineOrderIncoming = false
                        notificationResetJob?.cancel()
                        recentlyAcceptedRides[capturedRide.signature] = System.currentTimeMillis()
                        cleanStaleAcceptedRides()

                        val finalFare = capturedRide.price?.let { "₹${it.toInt()}" } ?: "Fare ~"
                        val finalDist = capturedRide.distanceKm?.let { "${it} km" } ?: "Dist ~"

                        val logMessage = "Passive Radar: Voice only announcement for $finalFare ($finalDist) to ${capturedRide.drop}"
                        Log.i(TAG, logMessage)
                        _recentLog.value = logMessage
                        updateForegroundNotification("Announced ride: $finalFare ($finalDist)")
                        logServiceEvent(
                            type = ServiceEventType.RIDE_DETECTED,
                            title = "Voice Announcement Only",
                            description = "Passive Radar announced ride for $finalFare ($finalDist)",
                            details = "Voice-only mode active: button click bypassed",
                            badge = "RADAR"
                        )

                        val userName = getUserName(this@AutoAcceptService).ifBlank { DEFAULT_USER_NAME }
                        val announcement = generateOrderAnnouncement(
                            context = this@AutoAcceptService,
                            name = userName,
                            price = capturedRide.price?.toInt(),
                            distanceKm = capturedRide.distanceKm,
                            dropLocation = capturedRide.drop
                        )
                        speak(announcement)
                        return@launch
                    }

                    when (val outcome = executeAcceptClick(validButton)) {
                        is ClickResult.Success -> {
                            lastClickTimestamp = SystemClock.uptimeMillis()
                            isGenuineOrderIncoming = false
                            notificationResetJob?.cancel()
                            recentlyAcceptedRides[capturedRide.signature] = System.currentTimeMillis()
                            cleanStaleAcceptedRides()
                            incrementDailyTripCount(this@AutoAcceptService)

                            val finalFare = capturedRide.price?.let { "₹${it.toInt()}" } ?: "Fare ~"
                            val finalDist = capturedRide.distanceKm?.let { "${it} km" } ?: "Dist ~"

                            val logMessage = "Accepted order in ${capturedRide.sourcePackage} [$finalFare | $finalDist] $engineMode"
                            Log.i(TAG, logMessage)
                            _recentLog.value = logMessage
                            updateForegroundNotification("Accepted: $finalFare ($finalDist)")
                            logServiceEvent(
                                type = ServiceEventType.ORDER_ACCEPTED,
                                title = "Order accepted",
                                description = "Successfully tapped accept button for $finalFare ($finalDist)",
                                details = "Auto-click executed via ${outcome.method} (${capturedRide.acceptReason})",
                                badge = if (capturedRide.isPremium) "FAST" else "ACCEPTED"
                            )
                            
                            triggerSuccessVibration()
                            playSuccessSound()
                            sendAutoAcceptNotification(
                                context = this@AutoAcceptService,
                                title = "Auto-Accepted: $finalFare",
                                message = "Dist: $finalDist | Drop: ${capturedRide.drop}"
                            )

                            logRideLocally(
                                context = this@AutoAcceptService,
                                price = capturedRide.price ?: 0f,
                                pickupKm = capturedRide.distanceKm ?: 0f,
                                pickupLocation = capturedRide.pickup,
                                dropLocation = capturedRide.drop,
                                status = "ACCEPTED",
                                reason = capturedRide.acceptReason,
                                sourcePackage = capturedRide.sourcePackage
                            )

                            if (capturedRide.isPremium && isTtsEnabled(this@AutoAcceptService)) {
                                val userName = getUserName(this@AutoAcceptService).ifBlank { DEFAULT_USER_NAME }
                                val announcement = generateOrderAnnouncement(
                                    context = this@AutoAcceptService,
                                    name = userName,
                                    price = capturedRide.price?.toInt(),
                                    distanceKm = capturedRide.distanceKm,
                                    dropLocation = capturedRide.drop
                                )
                                speak(announcement)
                            }
                        }
                        is ClickResult.Cancelled -> {
                            Log.w(TAG, "Click cancelled: ${outcome.reason}")
                            _recentLog.value = "Click cancelled: ${outcome.reason}"
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Accept cancelled",
                                description = outcome.reason,
                                badge = "CANCELLED"
                            )
                        }
                        is ClickResult.Failed -> {
                            Log.e(TAG, "Click failed: ${outcome.reason}")
                            _recentLog.value = "Click failed: ${outcome.reason}"
                            logServiceEvent(
                                type = ServiceEventType.ORDER_IGNORED,
                                title = "Click failed",
                                description = outcome.reason,
                                badge = "FAILED"
                            )
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Pending accept coroutine cancelled: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error in pending accept execution: ${e.message}", e)
                } finally {
                    if (pendingRideSignature == capturedRide.signature) {
                        releaseCpuWakeLock("Pending accept completed or cancelled")
                    }
                }
            }

            return
        }

        if (pendingAcceptJob?.isActive != true) {
            releaseCpuWakeLock("No valid ride cards found or all failed filters")
        }
    }

    /**
     * Traverses the accessibility node hierarchy iteratively to extract all nodes on screen.
     * Enforces package isolation to strictly ignore nodes from YouTube, Chrome, System UI, etc.
     */
    private fun traverseAllNodes(rootNode: AccessibilityNodeInfo, maxNodes: Int = 800): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        val queue = java.util.ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty() && nodes.size < maxNodes) {
            val current = queue.poll() ?: break

            val nodePkg = current.packageName?.toString()
            if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) {
                // Ignore any node belonging to YouTube, Chrome, System UI, or non-Rapido apps
                continue
            }

            nodes.add(current)

            try {
                val childCount = current.childCount
                for (i in 0 until childCount) {
                    val child = current.getChild(i)
                    if (child != null) {
                        queue.add(child)
                    }
                }
            } catch (e: Exception) {
                // Nodes can become invalid or throw during asynchronous UI updates
            }
        }
        return nodes
    }

    /**
     * Delegates up the node tree (up to 4 levels) to find a clickable container or button.
     */
    private fun findClickableTargetOrAncestor(node: AccessibilityNodeInfo, maxLevels: Int = 4): AccessibilityNodeInfo {
        if (node.isClickable && node.isEnabled) {
            return node
        }
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < maxLevels) {
            val parent = try { current.parent } catch (e: Exception) { null } ?: break
            if (parent.isClickable && parent.isEnabled) {
                return parent
            }
            current = parent
            depth++
        }
        return node
    }

    /**
     * Validates that an accessibility node is visible, enabled, and has valid on-screen dimensions.
     */
    private fun isNodeValidAcceptButton(node: AccessibilityNodeInfo): Boolean {
        if (!node.isEnabled) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !node.isVisibleToUser) {
            return false
        }
        val bounds = Rect()
        try {
            node.getBoundsInScreen(bounds)
        } catch (e: Exception) {
            return false
        }
        if (bounds.isEmpty || bounds.width() < 1 || bounds.height() < 1) {
            return false
        }
        return true
    }

    /**
     * Finds the legitimate accept button in the active ride window, using deep node traversal,
     * fuzzy keyword matching with parent delegation, ViewID lookup, and bottom-screen heuristic fallback.
     */
    private fun findCardContainer(node: AccessibilityNodeInfo, maxLevels: Int = 5): AccessibilityNodeInfo {
        var current: AccessibilityNodeInfo = node
        var depth = 0
        while (depth < maxLevels) {
            val parent = try { current.parent } catch (e: Exception) { null } ?: break
            current = parent
            depth++
        }
        return current
    }

    private fun findAllAcceptButtons(rootNode: AccessibilityNodeInfo): List<ValidatedButton> {
        val buttons = mutableListOf<ValidatedButton>()
        val exactKeywords = listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "order le", "start ride")
        for (keyword in exactKeywords) {
            val directNodes = try {
                rootNode.findAccessibilityNodeInfosByText(keyword)
            } catch (e: Exception) { emptyList() }

            for (node in directNodes) {
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) continue

                if (isNodeValidAcceptButton(node)) {
                    val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                    val targetBounds = Rect()
                    targetClickableNode.getBoundsInScreen(targetBounds)
                    if (targetBounds.isEmpty) node.getBoundsInScreen(targetBounds)

                    val isSwipe = keyword.contains("swipe", ignoreCase = true)
                    buttons.add(ValidatedButton(
                        node = targetClickableNode,
                        reason = "MacroDroid Fast-Path Match ('$keyword')",
                        actionType = if (isSwipe) ActionType.SWIPE else ActionType.CLICK,
                        targetBounds = targetBounds
                    ))
                }
            }
        }

        val allNodes = traverseAllNodes(rootNode)

        for (node in allNodes) {
            val nodePkg = node.packageName?.toString()
            if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) {
                continue
            }

            val textCandidates = listOfNotNull(
                node.text?.toString()?.trim(),
                node.contentDescription?.toString()?.trim()
            ).filter { it.isNotBlank() }

            for (text in textCandidates) {
                if (isValidAcceptText(this, text)) {
                    if (isNodeValidAcceptButton(node)) {
                        val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                        val targetPkg = targetClickableNode.packageName?.toString()
                        if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) {
                            continue
                        }

                        val targetBounds = Rect()
                        try {
                            targetClickableNode.getBoundsInScreen(targetBounds)
                            if (targetBounds.isEmpty || targetBounds.width() < 1 || targetBounds.height() < 1) {
                                node.getBoundsInScreen(targetBounds)
                            }
                        } catch (e: Exception) {
                            node.getBoundsInScreen(targetBounds)
                        }

                        val isSwipe = text.contains("swipe", ignoreCase = true)
                        val actionType = if (isSwipe) ActionType.SWIPE else ActionType.CLICK

                        val reason = if (isSwipe) {
                            "Fuzzy Keyword Swipe ('$text')"
                        } else {
                            "Fuzzy Keyword Accept ('$text')"
                        }

                        buttons.add(ValidatedButton(
                            node = targetClickableNode,
                            reason = reason,
                            actionType = actionType,
                            targetBounds = targetBounds
                        ))
                    }
                }
            }
        }

        for (viewId in ACCEPT_BUTTON_VIEW_IDS) {
            val matchingNodes = try {
                rootNode.findAccessibilityNodeInfosByViewId(viewId)
            } catch (e: Exception) {
                emptyList()
            }
            for (node in matchingNodes) {
                val nodePkg = node.packageName?.toString()
                if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) {
                    continue
                }
                if (isNodeValidAcceptButton(node)) {
                    val targetClickableNode = findClickableTargetOrAncestor(node, maxLevels = 4)
                    val targetPkg = targetClickableNode.packageName?.toString()
                    if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) {
                        continue
                    }
                    val targetBounds = Rect()
                    targetClickableNode.getBoundsInScreen(targetBounds)
                    if (targetBounds.isEmpty) node.getBoundsInScreen(targetBounds)
                    buttons.add(ValidatedButton(
                        node = targetClickableNode,
                        reason = "View ID ($viewId)",
                        actionType = ActionType.CLICK,
                        targetBounds = targetBounds
                    ))
                }
            }
        }

        val displayHeight = resources.displayMetrics.heightPixels
        val rootBounds = Rect()
        try {
            rootNode.getBoundsInScreen(rootBounds)
        } catch (e: Exception) {}
        val screenHeight = if (!rootBounds.isEmpty && rootBounds.height() > 200) {
            rootBounds.height()
        } else {
            displayHeight
        }

        val bottomFallback = findBottomScreenHeuristicButton(allNodes, screenHeight)
        if (bottomFallback != null) {
            buttons.add(bottomFallback)
        }

        return buttons.distinctBy { it.targetBounds.toShortString() }
    }

    /**
     * Scans the bottom 25% of the screen bounds for the largest clickable ViewGroup or Button.
     */
    private fun findBottomScreenHeuristicButton(allNodes: List<AccessibilityNodeInfo>, screenHeight: Int): ValidatedButton? {
        val bottomThreshold = screenHeight * 0.75f
        var bestCandidate: AccessibilityNodeInfo? = null
        var bestBounds = Rect()
        var maxArea = 0L

        for (node in allNodes) {
            val nodePkg = node.packageName?.toString()
            if (nodePkg != null && !getEnabledApps(this@AutoAcceptService).contains(nodePkg)) {
                continue
            }

            if (!node.isEnabled) continue
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !node.isVisibleToUser) continue

            val bounds = Rect()
            try {
                node.getBoundsInScreen(bounds)
            } catch (e: Exception) {
                continue
            }

            // Must reside in the bottom 25% of the screen
            if (bounds.centerY() < bottomThreshold && bounds.bottom < bottomThreshold) {
                continue
            }

            // Exclude fullscreen roots and tiny invisible controls
            if (bounds.height() >= (screenHeight * 0.40f) || bounds.width() <= 30 || bounds.height() <= 20) {
                continue
            }

            // Check if it is a Button, ViewGroup, or clickable element
            val className = node.className?.toString() ?: ""
            val isButtonOrGroup = className.contains("Button", ignoreCase = true) ||
                    className.contains("ViewGroup", ignoreCase = true) ||
                    className.contains("Layout", ignoreCase = true) ||
                    node.isClickable

            if (!isButtonOrGroup && !node.isClickable) {
                continue
            }

            // Exclude negative words
            val textCombined = ((node.text?.toString() ?: "") + " " + (node.contentDescription?.toString() ?: "")).lowercase()
            val negativeWords = listOf("decline", "reject", "cancel", "back", "home", "terms", "policy", "cash", "upi", "profile", "close")
            if (negativeWords.any { textCombined.contains(it) }) {
                continue
            }

            val clickableTarget = if (node.isClickable) node else findClickableTargetOrAncestor(node, maxLevels = 3)
            val targetPkg = clickableTarget.packageName?.toString()
            if (targetPkg != null && !getEnabledApps(this@AutoAcceptService).contains(targetPkg)) {
                continue
            }

            val area = bounds.width().toLong() * bounds.height().toLong()
            if (area > maxArea) {
                maxArea = area
                bestCandidate = clickableTarget
                bestBounds = Rect(bounds)
            }
        }

        if (bestCandidate != null && maxArea > 0) {
            val textCombined = ((bestCandidate.text?.toString() ?: "") + " " + (bestCandidate.contentDescription?.toString() ?: "")).lowercase()
            val isSwipe = textCombined.contains("swipe")
            val actionType = if (isSwipe) ActionType.SWIPE else ActionType.CLICK
            return ValidatedButton(
                node = bestCandidate,
                reason = "Bottom-Screen Heuristic Fallback (${if (isSwipe) "Swipe" else "Click"}, Area: $maxArea)",
                actionType = actionType,
                targetBounds = bestBounds
            )
        }

        return null
    }

    /**
     * Unified method to find and execute accept interaction on the active screen.
     */
    suspend fun findAndClickAcceptButton(rootNode: AccessibilityNodeInfo): ClickResult {
        val button = findAllAcceptButtons(rootNode).firstOrNull()
            ?: return ClickResult.Failed("Accept button not found on screen")
        return executeAcceptClick(button)
    }

    /**
     * Executes the accept action, routing between Swipe and Click with coordinate tap fallback.
     */
    private suspend fun executeAcceptClick(button: ValidatedButton): ClickResult {
        Log.d(TAG, "Accept action attempted via ${button.reason} [Action: ${button.actionType}]")

        return when (button.actionType) {
            ActionType.SWIPE -> {
                dispatchHorizontalSwipeSuspending(button.node, button.targetBounds, button.reason)
            }
            ActionType.CLICK -> {
                var directSuccess = false
                try {
                    if (button.node.isClickable && button.node.isEnabled) {
                        directSuccess = button.node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception during ACTION_CLICK: ${e.message}")
                    directSuccess = false
                }

                if (directSuccess) {
                    Log.d(TAG, "Direct click completed via ${button.reason}")
                    ClickResult.Success("ACTION_CLICK via ${button.reason}")
                } else {
                    Log.d(TAG, "Direct click returned false/failed, falling back to coordinate tap")
                    dispatchCoordinateTapSuspending(button.node, button.targetBounds, button.reason)
                }
            }
        }
    }

    /**
     * Dispatches a horizontal swipe gesture from the left edge to the right edge of the target bounds.
     */
    private suspend fun dispatchHorizontalSwipeSuspending(
        node: AccessibilityNodeInfo,
        presetBounds: Rect,
        reason: String
    ): ClickResult {
        val bounds = Rect()
        if (!presetBounds.isEmpty) {
            bounds.set(presetBounds)
        } else {
            try {
                node.getBoundsInScreen(bounds)
            } catch (e: Exception) {
                return ClickResult.Failed("Failed to read node bounds for swipe: ${e.message}")
            }
        }

        if (bounds.isEmpty || bounds.width() <= 10 || bounds.height() <= 10) {
            return ClickResult.Failed("Invalid node bounds for swipe gesture: $bounds")
        }

        val startX = bounds.left.toFloat() + (bounds.width() * 0.12f)
        val endX = bounds.right.toFloat() - (bounds.width() * 0.08f)
        val centerY = bounds.centerY().toFloat()

        val swipePath = Path().apply {
            moveTo(startX, centerY)
            lineTo(endX, centerY)
        }

        val stroke = GestureDescription.StrokeDescription(swipePath, 0, 350)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return suspendCancellableCoroutine { cont ->
            try {
                val queued = dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Horizontal swipe onCompleted from ($startX, $centerY) to ($endX, $centerY) via $reason")
                        if (cont.isActive) {
                            cont.resume(ClickResult.Success("SWIPE from left to right at Y=$centerY"))
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.w(TAG, "Horizontal swipe onCancelled via $reason")
                        if (cont.isActive) {
                            cont.resume(ClickResult.Cancelled("Swipe cancelled by system"))
                        }
                    }
                }, null)

                if (!queued) {
                    Log.w(TAG, "dispatchGesture for swipe returned false")
                    if (cont.isActive) {
                        cont.resume(ClickResult.Failed("dispatchGesture for swipe returned false"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during horizontal swipe: ${e.message}", e)
                if (cont.isActive) {
                    cont.resume(ClickResult.Failed("Swipe exception: ${e.message}"))
                }
            }
        }
    }

    /**
     * Dispatches coordinate tap gesture with GestureResultCallback, awaiting completion or cancellation.
     */
    private suspend fun dispatchCoordinateTapSuspending(
        node: AccessibilityNodeInfo,
        presetBounds: Rect,
        reason: String
    ): ClickResult {
        val bounds = Rect()
        if (!presetBounds.isEmpty) {
            bounds.set(presetBounds)
        } else {
            try {
                node.getBoundsInScreen(bounds)
            } catch (e: Exception) {
                return ClickResult.Failed("Failed to read node bounds: ${e.message}")
            }
        }

        if (bounds.isEmpty || bounds.width() <= 0 || bounds.height() <= 0) {
            return ClickResult.Failed("Invalid node bounds for gesture")
        }

        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        val tapPath = Path().apply {
            moveTo(centerX, centerY)
        }

        val stroke = GestureDescription.StrokeDescription(tapPath, 0, 80)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return suspendCancellableCoroutine { cont ->
            try {
                val queued = dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Coordinate tap onCompleted at ($centerX, $centerY) via $reason")
                        if (cont.isActive) {
                            cont.resume(ClickResult.Success("GESTURE_TAP at ($centerX, $centerY)"))
                        }
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.w(TAG, "Coordinate tap onCancelled at ($centerX, $centerY) via $reason")
                        if (cont.isActive) {
                            cont.resume(ClickResult.Cancelled("Gesture cancelled by system at ($centerX, $centerY)"))
                        }
                    }
                }, null)

                if (!queued) {
                    Log.w(TAG, "dispatchGesture returned false")
                    if (cont.isActive) {
                        cont.resume(ClickResult.Failed("dispatchGesture returned false"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in dispatchGesture: ${e.message}", e)
                if (cont.isActive) {
                    cont.resume(ClickResult.Failed("Gesture dispatch exception: ${e.message}"))
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "AutoAcceptService interrupted.")
        cancelPendingAcceptInternal("Service interrupted by system")
        _isServiceRunning.value = false
        _recentLog.value = "Service interrupted by system."
        ServiceStatusNotificationManager.updateStatus(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.w(TAG, "AutoAcceptService unbound.")
        _isServiceRunning.value = false
        BoundingBoxManager.hide()
        StatusOverlayManager.hide()
        instance = null
        ServiceStatusNotificationManager.updateStatus(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        BoundingBoxManager.hide()
        StatusOverlayManager.hide()
        cancelPendingAcceptInternal("Service destroyed")
        releaseCpuWakeLock("Service destroyed")
        serviceJob.cancel()
        instance = null
        _isServiceRunning.value = false
        _recentLog.value = "Service stopped."
        ServiceStatusNotificationManager.updateStatus(this)
        ServiceStatusWidgetProvider.updateAllWidgets(this)

        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // Ignore if foreground stop fails on older APIs
        }

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsInitialized = false
            Log.i(TAG, "TextToSpeech successfully shut down.")
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down TextToSpeech: ${e.message}")
        }
    }
}
