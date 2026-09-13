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

/**
 * RemoteConfigManager encapsulates Firebase Remote Config integration.
 * Allows updating 'Allowed Package List' and 'Wait Delay' thresholds dynamically
 * without republishing the APK.
 */
object RemoteConfigManager {
    private const val TAG = "RemoteConfigManager"

    // Remote Config Keys
    const val KEY_ALLOWED_PACKAGES = "allowed_packages"
    const val KEY_MIN_WAIT_DELAY_MS = "min_wait_delay_ms"
    const val KEY_DEFAULT_WAIT_DELAY_MS = "default_wait_delay_ms"
    const val KEY_MAX_WAIT_DELAY_MS = "max_wait_delay_ms"

    // Default Fallbacks
    const val DEFAULT_ALLOWED_PACKAGES_CSV = "com.rapido.captain,com.rapido.rider,com.rapido.driver"
    const val DEFAULT_MIN_DELAY_MS = 0L
    const val DEFAULT_DEFAULT_DELAY_MS = 300L
    const val DEFAULT_MAX_DELAY_MS = 5000L

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

    private val _lastFetchStatus = MutableStateFlow("Remote Config active (defaults)")
    val lastFetchStatus: StateFlow<String> = _lastFetchStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) = initialize(context)

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
                    KEY_MAX_WAIT_DELAY_MS to DEFAULT_MAX_DELAY_MS
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
