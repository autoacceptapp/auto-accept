package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AutoAcceptService
import com.example.data.AppDatabase
import com.example.data.CustomFilterRule
import com.example.data.FilterSettings
import com.example.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SimulationResult(
    val isAccepted: Boolean,
    val reasons: List<String>,
    val matchedRuleName: String? = null
)

class PreferencesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SettingsRepository
    private val context: Context get() = getApplication<Application>().applicationContext

    val filterSettings: StateFlow<FilterSettings>
    val customRules: StateFlow<List<CustomFilterRule>>

    private val _activeFilterCount = MutableStateFlow(0)
    val activeFilterCount: StateFlow<Int> = _activeFilterCount.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SettingsRepository(database.settingsDao())

        val defaultSettings = FilterSettings(
            minPrice = AutoAcceptService.getMinPrice(context),
            maxPrice = AutoAcceptService.getMaxPrice(context),
            maxDistance = AutoAcceptService.getMaxDistanceKm(context),
            isPriceFilterOn = AutoAcceptService.isPriceFilterEnabled(context),
            isDistanceFilterOn = AutoAcceptService.isDistanceFilterEnabled(context),
            isBlacklistFilterOn = AutoAcceptService.isBlacklistEnabled(context),
            blacklistKeywords = AutoAcceptService.getBlacklistKeywords(context),
            minPassengerRating = AutoAcceptService.getMinPassengerRating(context),
            isPassengerRatingFilterOn = AutoAcceptService.isPassengerRatingFilterEnabled(context)
        )

        filterSettings = repository.filterSettings
            .map { it ?: defaultSettings }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                defaultSettings
            )

        customRules = repository.customFilterRules
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        // Seed default initial custom rules if database table is empty
        viewModelScope.launch(Dispatchers.IO) {
            val existing = database.settingsDao().getAllCustomRulesSync()
            if (existing.isEmpty()) {
                val defaultRules = listOf(
                    CustomFilterRule(
                        name = "Airport & Terminal Express",
                        minFare = 150f,
                        maxFare = 5000f,
                        maxDistanceKm = 8.0f,
                        minPassengerRating = 4.5f,
                        isMinFareEnabled = true,
                        isMaxDistanceEnabled = true,
                        isRatingFilterEnabled = true,
                        destinationKeyword = "Airport",
                        isActive = true
                    ),
                    CustomFilterRule(
                        name = "Quick Local Runs",
                        minFare = 40f,
                        maxFare = 300f,
                        maxDistanceKm = 3.5f,
                        minPassengerRating = 4.0f,
                        isMinFareEnabled = true,
                        isMaxDistanceEnabled = true,
                        isRatingFilterEnabled = false,
                        destinationKeyword = "",
                        isActive = true
                    ),
                    CustomFilterRule(
                        name = "High Rating VIP",
                        minFare = 90f,
                        maxFare = 5000f,
                        maxDistanceKm = 10.0f,
                        minPassengerRating = 4.7f,
                        isMinFareEnabled = true,
                        isMaxDistanceEnabled = true,
                        isRatingFilterEnabled = true,
                        destinationKeyword = "",
                        isActive = false
                    )
                )
                defaultRules.forEach { repository.addCustomRule(it) }
            }

            // Sync SharedPreferences with Room if needed
            val currentRoomSettings = database.settingsDao().getFilterSettingsSync()
            if (currentRoomSettings == null) {
                repository.saveFilterSettings(
                    FilterSettings(
                        minPrice = AutoAcceptService.getMinPrice(context),
                        maxPrice = AutoAcceptService.getMaxPrice(context),
                        maxDistance = AutoAcceptService.getMaxDistanceKm(context),
                        isPriceFilterOn = AutoAcceptService.isPriceFilterEnabled(context),
                        isDistanceFilterOn = AutoAcceptService.isDistanceFilterEnabled(context),
                        isBlacklistFilterOn = AutoAcceptService.isBlacklistEnabled(context),
                        blacklistKeywords = AutoAcceptService.getBlacklistKeywords(context),
                        minPassengerRating = AutoAcceptService.getMinPassengerRating(context),
                        isPassengerRatingFilterOn = AutoAcceptService.isPassengerRatingFilterEnabled(context)
                    )
                )
            }
        }
    }

    fun setMinFare(fare: Float, enabled: Boolean) {
        val current = filterSettings.value
        val updated = current.copy(minPrice = fare, isPriceFilterOn = enabled)
        saveSettings(updated)
        AutoAcceptService.setMinPrice(context, fare)
        AutoAcceptService.setPriceFilterEnabled(context, enabled)
    }

    fun setMaxDistance(maxDist: Float, enabled: Boolean) {
        val current = filterSettings.value
        val updated = current.copy(maxDistance = maxDist, isDistanceFilterOn = enabled)
        saveSettings(updated)
        AutoAcceptService.setMaxDistanceKm(context, maxDist)
        AutoAcceptService.setDistanceFilterEnabled(context, enabled)
    }

    fun setPassengerRating(rating: Float, enabled: Boolean) {
        val current = filterSettings.value
        val updated = current.copy(minPassengerRating = rating, isPassengerRatingFilterOn = enabled)
        saveSettings(updated)
        AutoAcceptService.setMinPassengerRating(context, rating)
        AutoAcceptService.setPassengerRatingFilterEnabled(context, enabled)
    }

    fun setPricePerKm(rate: Float, enabled: Boolean) {
        val current = filterSettings.value
        val updated = current.copy(minPricePerKm = rate, isPricePerKmFilterOn = enabled)
        saveSettings(updated)
    }

    fun saveSettings(settings: FilterSettings) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveFilterSettings(settings)
            withContext(Dispatchers.Main) {
                AutoAcceptService.setMinPrice(context, settings.minPrice)
                AutoAcceptService.setMaxPrice(context, settings.maxPrice)
                AutoAcceptService.setPriceFilterEnabled(context, settings.isPriceFilterOn)
                AutoAcceptService.setMaxDistanceKm(context, settings.maxDistance)
                AutoAcceptService.setDistanceFilterEnabled(context, settings.isDistanceFilterOn)
                AutoAcceptService.setMinPassengerRating(context, settings.minPassengerRating)
                AutoAcceptService.setPassengerRatingFilterEnabled(context, settings.isPassengerRatingFilterOn)
            }
        }
    }

    fun addCustomRule(
        name: String,
        minFare: Float,
        maxFare: Float,
        maxDistanceKm: Float,
        minRating: Float,
        isMinFareEnabled: Boolean,
        isMaxDistanceEnabled: Boolean,
        isRatingEnabled: Boolean,
        destinationKeyword: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val rule = CustomFilterRule(
                name = name.trim().ifEmpty { "Custom Rule" },
                minFare = minFare,
                maxFare = maxFare,
                maxDistanceKm = maxDistanceKm,
                minPassengerRating = minRating,
                isMinFareEnabled = isMinFareEnabled,
                isMaxDistanceEnabled = isMaxDistanceEnabled,
                isRatingFilterEnabled = isRatingEnabled,
                destinationKeyword = destinationKeyword.trim(),
                isActive = true
            )
            repository.addCustomRule(rule)
        }
    }

    fun updateCustomRule(rule: CustomFilterRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomRule(rule)
        }
    }

    fun deleteCustomRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomRule(id)
        }
    }

    fun toggleCustomRule(id: Long, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleCustomRule(id, isActive)
        }
    }

    fun addPresetRule(presetType: String) {
        when (presetType) {
            "airport" -> addCustomRule(
                name = "Airport Runner",
                minFare = 180f,
                maxFare = 5000f,
                maxDistanceKm = 10.0f,
                minRating = 4.6f,
                isMinFareEnabled = true,
                isMaxDistanceEnabled = true,
                isRatingEnabled = true,
                destinationKeyword = "Airport"
            )
            "high_fare" -> addCustomRule(
                name = "High Payout Trips",
                minFare = 120f,
                maxFare = 5000f,
                maxDistanceKm = 15.0f,
                minRating = 4.3f,
                isMinFareEnabled = true,
                isMaxDistanceEnabled = false,
                isRatingEnabled = false,
                destinationKeyword = ""
            )
            "five_star" -> addCustomRule(
                name = "Top 5-Star VIPs",
                minFare = 60f,
                maxFare = 5000f,
                maxDistanceKm = 7.0f,
                minRating = 4.8f,
                isMinFareEnabled = true,
                isMaxDistanceEnabled = true,
                isRatingEnabled = true,
                destinationKeyword = ""
            )
            "short_hops" -> addCustomRule(
                name = "Short Quick Hops",
                minFare = 35f,
                maxFare = 200f,
                maxDistanceKm = 2.5f,
                minRating = 4.0f,
                isMinFareEnabled = true,
                isMaxDistanceEnabled = true,
                isRatingEnabled = false,
                destinationKeyword = ""
            )
        }
    }

    /**
     * Interactive simulator: tests whether a sample ride would pass the currently configured
     * preferences and active custom filtering rules saved in Room.
     */
    fun simulateRide(
        fare: Float,
        distance: Float,
        rating: Float,
        destination: String
    ): SimulationResult {
        val settings = filterSettings.value
        val rules = customRules.value.filter { it.isActive }
        val rejectReasons = mutableListOf<String>()

        // 1. Core Global Filters Check
        if (settings.isPriceFilterOn && fare < settings.minPrice) {
            rejectReasons.add("Fare ₹${fare.toInt()} is below minimum preference ₹${settings.minPrice.toInt()}")
        }
        if (settings.isDistanceFilterOn && distance > settings.maxDistance) {
            rejectReasons.add("Pickup distance ${distance}km exceeds maximum preference ${settings.maxDistance}km")
        }
        if (settings.isPassengerRatingFilterOn && rating < settings.minPassengerRating) {
            rejectReasons.add("Passenger rating ${rating}★ is below minimum preference ${settings.minPassengerRating}★")
        }

        // 2. Custom Rules Check
        var matchedRule: String? = null
        if (rules.isNotEmpty()) {
            // If active custom rules exist, check if ride satisfies any active rule
            var rulePassed = false
            for (rule in rules) {
                var thisRuleFailed = false
                if (rule.isMinFareEnabled && fare < rule.minFare) {
                    thisRuleFailed = true
                }
                if (rule.isMaxDistanceEnabled && distance > rule.maxDistanceKm) {
                    thisRuleFailed = true
                }
                if (rule.isRatingFilterEnabled && rating < rule.minPassengerRating) {
                    thisRuleFailed = true
                }
                if (rule.destinationKeyword.isNotBlank() && !destination.contains(rule.destinationKeyword, ignoreCase = true)) {
                    thisRuleFailed = true
                }

                if (!thisRuleFailed) {
                    rulePassed = true
                    matchedRule = rule.name
                    break
                }
            }

            if (!rulePassed) {
                rejectReasons.add("Ride did not match any of your ${rules.size} active custom rules")
            }
        }

        return if (rejectReasons.isEmpty()) {
            SimulationResult(isAccepted = true, reasons = listOf("All criteria satisfied! Auto-accept would trigger."), matchedRuleName = matchedRule)
        } else {
            SimulationResult(isAccepted = false, reasons = rejectReasons, matchedRuleName = matchedRule)
        }
    }
}
