package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_settings")
data class FilterSettings(
    @PrimaryKey val id: Int = 1,
    val maxDistance: Float = 5.0f,
    val minPrice: Float = 0f,
    val maxPrice: Float = 5000f,
    val isDistanceFilterOn: Boolean = false,
    val isPriceFilterOn: Boolean = false,
    val isBlacklistFilterOn: Boolean = false,
    val blacklistKeywords: String = "",
    val minPassengerRating: Float = 4.5f,
    val isPassengerRatingFilterOn: Boolean = false,
    val minPricePerKm: Float = 0f,
    val isPricePerKmFilterOn: Boolean = false
)
