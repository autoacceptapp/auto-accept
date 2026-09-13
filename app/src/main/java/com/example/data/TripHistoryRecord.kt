package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_history")
data class TripHistoryRecord(
    @PrimaryKey val dateStr: String,
    val tripCount: Int
)
