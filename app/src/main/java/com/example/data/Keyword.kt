package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "keywords")
data class Keyword(
    @PrimaryKey val word: String,
    val isEnabled: Boolean = true
)
