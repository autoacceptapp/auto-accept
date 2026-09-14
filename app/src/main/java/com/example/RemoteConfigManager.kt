package com.example

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data model representing an Accessibility Troubleshooting FAQ item
 * delivered dynamically via Firebase Remote Config.
 */
data class AccessibilityFaqItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: String = "Accessibility",
    val stepGuide: List<String> = emptyList(),
    val directAction: String? = null // "OPEN_ACCESSIBILITY", "OPEN_BATTERY", "OPEN_OVERLAY", "OPEN_NOTIFICATIONS"
)

/**
 * RemoteConfigManager encapsulates Firebase Remote Config integration.
 * Allows updating 'Allowed Package List', 'Wait Delay' thresholds, and
 * Accessibility Troubleshooting FAQs dynamically without republishing the APK.
 */
object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    // Remote Config Keys
    const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    const val KEY_MIN_WAIT_DELAY_MS = "min_wait_delay_ms"
    const val KEY_DEFAULT_WAIT_DELAY_MS = "default_wait_delay_ms"
    const val KEY_MAX_WAIT_DELAY_MS = "max_wait_delay_ms"
    const val KEY_ACCESSIBILITY_FAQ_JSON = "accessibility_faq_json"

    // Default Fallbacks
    const val DEFAULT_ALLOWED_PACKAGES_CSV = "com.rapido.captain,com.rapido.rider,com.rapido.driver"
    const val DEFAULT_MIN_DELAY_MS = 0L
    const val DEFAULT_DEFAULT_DELAY_MS = 300L
    const val DEFAULT_MAX_DELAY_MS = 5000L

    const val DEFAULT_ACCESSIBILITY_FAQ_JSON = """[
      {
        "id": "faq_battery_kill",
        "question": "Why does the Accessibility Service turn off automatically?",
        "category": "Battery & OS",
        "answer": "Android's background battery optimization automatically stops background accessibility services when the screen locks or during memory cleanups.",
        "stepGuide": [
          "Open Phone Settings -> Apps -> RideStrike.",
          "Tap Battery and choose 'Unrestricted' or 'Do Not Optimize'.",
          "For Xiaomi, Vivo, Oppo, Realme: Enable 'Autostart' and lock RideStrike in Recent Apps.",
          "In RideStrike Settings -> Core Automation, enable 'Keep CPU Awake (WakeLock)'."
        ],
        "directAction": "OPEN_BATTERY"
      },
      {
        "id": "faq_restricted_settings",
        "question": "How to resolve 'Restricted Setting' error on Android 13, 14, and 15?",
        "category": "Permissions",
        "answer": "Android 13+ restricts accessibility services for downloaded apps as a security precaution until manually authorized.",
        "stepGuide": [
          "Open Phone Settings -> Apps -> RideStrike.",
          "Tap the 3-dots menu in the top-right corner.",
          "Select 'Allow restricted settings'.",
          "Authenticate with your fingerprint, face unlock, or PIN.",
          "Return to Accessibility Settings and switch RideStrike ON."
        ],
        "directAction": "OPEN_ACCESSIBILITY"
      },
      {
        "id": "faq_not_clicking",
        "question": "Service is enabled but not clicking or accepting ride popups?",
        "category": "Detection",
        "answer": "This happens if overlay permissions are missing, order filters reject the ride, or Android's accessibility node cache is frozen.",
        "stepGuide": [
          "Ensure the Master Switch on the Dashboard is turned ON (green).",
          "Check Distance and Price filters in Settings to ensure they aren't filtering out the fare.",
          "Verify 'Display over other apps' is granted so popups can be inspected.",
          "Toggle the Accessibility switch OFF and back ON once to refresh the system node tree."
        ],
        "directAction": "OPEN_ACCESSIBILITY"
      },
      {
        "id": "faq_overlay_missing",
        "question": "Floating status pill or radar overlay is not appearing on screen?",
        "category": "Overlay",
        "answer": "The floating telemetry widget requires system overlay permission to display over the Rapido Captain app.",
        "stepGuide": [
          "Open Phone Settings -> Apps -> Special App Access -> Display over other apps.",
          "Select RideStrike and toggle 'Allow display over other apps' ON.",
          "If using a gaming phone, disable Game Turbo/Game Space floating window suppression."
        ],
        "directAction": "OPEN_OVERLAY"
      },
      {
        "id": "faq_volume_shortcut",
        "question": "Volume buttons or accessibility button accidentally turn off the service?",
        "category": "Accessibility",
        "answer": "Android frequently binds accessibility services to the physical volume up/down button shortcut.",
        "stepGuide": [
          "Open Phone Settings -> Accessibility -> RideStrike.",
          "Locate 'RideStrike shortcut' or 'Volume key shortcut'.",
          "Turn the shortcut toggle OFF, but leave the main 'Use RideStrike' toggle ON."
        ],
        "directAction": "OPEN_ACCESSIBILITY"
      },
      {
        "id": "faq_bg_notifications",
        "question": "How to accept orders when Rapido is minimized or in background?",
        "category": "Notifications",
        "answer": "RideStrike uses dual-engine detection: Accessibility for active screens and Notification Listener for background incoming ride alerts.",
        "stepGuide": [
          "In Settings -> Permissions & Requirements, turn ON 'Notification Listener'.",
          "Grant Notification Access to RideStrike in Android system settings.",
          "Ensure Rapido notification sounds and banners are enabled."
        ],
        "directAction": "OPEN_NOTIFICATIONS"
      },
      {
        "id": "faq_accept_delay",
        "question": "What is the recommended Accept Wait Delay setting?",
        "category": "Optimization",
        "answer": "A delay between 200ms and 350ms provides the fastest accept time while avoiding network collision errors.",
        "stepGuide": [
          "Go to Settings -> Core Automation -> Accept Wait Delay.",
          "Select 250ms or 300ms for optimal competitive driver speed.",
          "Verify that your 4G/5G connection has low latency."
        ],
        "directAction": null
      },
      {
        "id": "faq_rapido_updates",
        "question": "Orders not detected after a Rapido Captain app update?",
        "category": "Detection",
        "answer": "When driver apps update, UI elements or package names may shift. RideStrike's multi-selector engine and Remote Config dynamically adapt.",
        "stepGuide": [
          "In Settings -> Allowed Applications, verify target packages are enabled.",
          "Tap the 'Simulate' order button in Order History to verify accept triggers.",
          "Check Debug Logs in Tab 3 to inspect accessibility node text matches."
        ],
        "directAction": null
      }
    ]"""

    // Observable dynamic allowed packages set
    private val _allowedPackages = MutableStateFlow<Set<String>>(
        DEFAULT_ALLOWED_PACKAGES_CSV.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    )
    val allowedPackages: StateFlow<Set<String>> = _allowedPackages.asStateFlow()

    // Observable dynamic wait delay thresholds
    private val _minWaitDelayMs = MutableStateFlow(DEFAULT_MIN_DELAY_MS)
    val minWaitDelayMs: StateFlow<Long> = _minWaitDelayMs.asStateFlow()

    private val _defaultWaitDelayMs = MutableStateFlow(DEFAULT_DEFAULT_DELAY_MS)
    val defaultWaitDelayMs: StateFlow<Long> = _defaultWaitDelayMs.asStateFlow()
    val remoteDefaultDelayMs: StateFlow<Long> get() = defaultWaitDelayMs

    private val _maxWaitDelayMs = MutableStateFlow(DEFAULT_MAX_DELAY_MS)
    val maxWaitDelayMs: StateFlow<Long> = _maxWaitDelayMs.asStateFlow()

    // Observable dynamic accessibility FAQs
    private val _accessibilityFaqs = MutableStateFlow<List<AccessibilityFaqItem>>(parseFaqs(DEFAULT_ACCESSIBILITY_FAQ_JSON))
    val accessibilityFaqs: StateFlow<List<AccessibilityFaqItem>> = _accessibilityFaqs.asStateFlow()

    private val _lastFetchStatus = MutableStateFlow("Remote Config active (defaults)")
    val lastFetchStatus: StateFlow<String> = _lastFetchStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) = initialize(context)

    /**
     * Parse JSON string into a list of AccessibilityFaqItem.
     */
    fun parseFaqs(jsonStr: String): List<AccessibilityFaqItem> {
        val list = mutableListOf<AccessibilityFaqItem>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stepsArr = obj.optJSONArray("stepGuide")
                val steps = mutableListOf<String>()
                if (stepsArr != null) {
                    for (j in 0 until stepsArr.length()) {
                        steps.add(stepsArr.getString(j))
                    }
                }
                list.add(
                    AccessibilityFaqItem(
                        id = obj.optString("id", "faq_$i"),
                        question = obj.optString("question", ""),
                        answer = obj.optString("answer", ""),
                        category = obj.optString("category", "Accessibility"),
                        stepGuide = steps,
                        directAction = obj.optString("directAction", null).takeIf { !it.isNullOrBlank() }
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse FAQ JSON: ${e.message}")
        }
        return if (list.isNotEmpty()) list else parseFaqsFallback()
    }

    private fun parseFaqsFallback(): List<AccessibilityFaqItem> {
        return try {
            val arr = JSONArray(DEFAULT_ACCESSIBILITY_FAQ_JSON)
            val list = mutableListOf<AccessibilityFaqItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stepsArr = obj.optJSONArray("stepGuide")
                val steps = mutableListOf<String>()
                if (stepsArr != null) {
                    for (j in 0 until stepsArr.length()) {
                        steps.add(stepsArr.getString(j))
                    }
                }
                list.add(
                    AccessibilityFaqItem(
                        id = obj.optString("id", "faq_$i"),
                        question = obj.optString("question", ""),
                        answer = obj.optString("answer", ""),
                        category = obj.optString("category", "Accessibility"),
                        stepGuide = steps,
                        directAction = obj.optString("directAction", null).takeIf { !it.isNullOrBlank() }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Initializes Firebase Remote Config with default fallback parameters and fetches the latest values.
     */
    fun initialize(context: Context) {
        scope.launch {
            try {
                FirebaseHelper.initialize(context)
                if (FirebaseApp.getApps(context).isEmpty()) {
                    Log.w(TAG, "FirebaseApp not initialized, skipping RemoteConfig setup")
                    _lastFetchStatus.value = "Firebase offline"
                    return@launch
                }

                val remoteConfig = FirebaseRemoteConfig.getInstance()
                val configSettings = FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(60) // 1 minute check interval
                    .build()
                remoteConfig.setConfigSettingsAsync(configSettings)

                // Set in-app default values
                val defaults = mapOf(
                    KEY_ALLOWED_PACKAGES to DEFAULT_ALLOWED_PACKAGES_CSV,
                    KEY_MIN_WAIT_DELAY_MS to DEFAULT_MIN_DELAY_MS,
                    KEY_DEFAULT_WAIT_DELAY_MS to DEFAULT_DEFAULT_DELAY_MS,
                    KEY_MAX_WAIT_DELAY_MS to DEFAULT_MAX_DELAY_MS,
                    KEY_ACCESSIBILITY_FAQ_JSON to DEFAULT_ACCESSIBILITY_FAQ_JSON
                )
                remoteConfig.setDefaultsAsync(defaults).await()

                // Periodic or on-demand fetch & activate
                val updated = remoteConfig.fetchAndActivate().await()
                Log.i(TAG, "RemoteConfig fetchAndActivate result: $updated")
                applyConfig(remoteConfig)
                _lastFetchStatus.value = if (updated) "Cloud Active" else "Ready (Defaults)"

                try {
                    remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
                        override fun onUpdate(configUpdate: ConfigUpdate) {
                            Log.i(TAG, "RemoteConfig real-time updated keys: ${configUpdate.updatedKeys}")
                            remoteConfig.activate().addOnCompleteListener {
                                applyConfig(remoteConfig)
                                _lastFetchStatus.value = "Live Updated"
                            }
                        }

                        override fun onError(error: FirebaseRemoteConfigException) {
                            Log.w(TAG, "RemoteConfig real-time update error: ${error.message}")
                        }
                    })
                } catch (t: Throwable) {
                    Log.d(TAG, "addOnConfigUpdateListener not supported or failed: ${t.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error initializing RemoteConfig: ${e.message}", e)
                _lastFetchStatus.value = "Offline fallback"
            }
        }
    }

    /**
     * Manually triggers a re-fetch of Remote Config (e.g. from user Refresh button).
     */
    fun fetchLatestFaqs(context: Context, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            try {
                FirebaseHelper.initialize(context)
                if (FirebaseApp.getApps(context).isEmpty()) {
                    onComplete(false)
                    return@launch
                }
                val remoteConfig = FirebaseRemoteConfig.getInstance()
                val updated = remoteConfig.fetchAndActivate().await()
                applyConfig(remoteConfig)
                _lastFetchStatus.value = if (updated) "Cloud Synced" else "Current (Cached)"
                onComplete(true)
            } catch (e: Exception) {
                Log.w(TAG, "Manual fetchRemoteConfig failed: ${e.message}")
                onComplete(false)
            }
        }
    }

    /**
     * Reads values from FirebaseRemoteConfig and updates internal StateFlows.
     */
    private fun applyConfig(remoteConfig: FirebaseRemoteConfig) {
        val packagesCsv = remoteConfig.getString(KEY_ALLOWED_PACKAGES).ifBlank { DEFAULT_ALLOWED_PACKAGES_CSV }
        val parsedPackages = packagesCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        if (parsedPackages.isNotEmpty()) {
            _allowedPackages.value = parsedPackages
            Log.i(TAG, "Updated Allowed Packages from RemoteConfig: $parsedPackages")
        }

        val minDelay = remoteConfig.getLong(KEY_MIN_WAIT_DELAY_MS)
        _minWaitDelayMs.value = minDelay.coerceAtLeast(0L)

        val defaultDelay = remoteConfig.getLong(KEY_DEFAULT_WAIT_DELAY_MS)
        if (defaultDelay > 0L) {
            _defaultWaitDelayMs.value = defaultDelay
        }

        val maxDelay = remoteConfig.getLong(KEY_MAX_WAIT_DELAY_MS)
        if (maxDelay > 0L) {
            _maxWaitDelayMs.value = maxDelay
        }

        val faqJson = remoteConfig.getString(KEY_ACCESSIBILITY_FAQ_JSON)
        if (faqJson.isNotBlank()) {
            val parsed = parseFaqs(faqJson)
            if (parsed.isNotEmpty()) {
                _accessibilityFaqs.value = parsed
                Log.i(TAG, "Updated Accessibility FAQs from RemoteConfig: ${parsed.size} items")
            }
        }

        Log.i(TAG, "RemoteConfig delay thresholds: min=${_minWaitDelayMs.value}ms, def=${_defaultWaitDelayMs.value}ms, max=${_maxWaitDelayMs.value}ms")
    }

    /**
     * Check if a given package is allowed by current dynamic RemoteConfig rules.
     */
    fun isPackageAllowed(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return _allowedPackages.value.contains(packageName)
    }

    /**
     * Clamps a given delay duration within the dynamic RemoteConfig thresholds.
     */
    fun clampWaitDelay(delayMs: Long): Long {
        val min = _minWaitDelayMs.value
        val max = _maxWaitDelayMs.value
        return delayMs.coerceIn(min, max)
    }

    fun clampDelay(delayMs: Long): Long = clampWaitDelay(delayMs)
}
