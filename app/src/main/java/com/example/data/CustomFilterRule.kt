package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a user-defined custom filtering rule for ride requests.
 * Allows drivers to set custom rules combining minimum fare, maximum distance, passenger rating,
 * and optional destination or area keywords.
 */
@Entity(tableName = "custom_filter_rules")
data class CustomFilterRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val minFare: Float = 50f,
    val maxFare: Float = 5000f,
    val maxDistanceKm: Float = 5.0f,
    val minPassengerRating: Float = 4.5f,
    val isMinFareEnabled: Boolean = true,
    val isMaxDistanceEnabled: Boolean = true,
    val isRatingFilterEnabled: Boolean = false,
    val destinationKeyword: String = "", // Optional required keyword (e.g. Airport, Station)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
