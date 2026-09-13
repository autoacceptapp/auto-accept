package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_apps")
data class TargetApp(
    @PrimaryKey val packageName: String,
    val isEnabled: Boolean = true
)
