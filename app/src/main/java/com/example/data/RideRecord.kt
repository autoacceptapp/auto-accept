package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ride_records")
data class RideRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val fare: Float,
    val status: String, // "ACCEPTED" or "REJECTED"
    val timestamp: Long = System.currentTimeMillis()
)
