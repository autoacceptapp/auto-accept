package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {
    @Query("SELECT * FROM ride_records ORDER BY timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<RideRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: RideRecord): Long

    @Query("SELECT * FROM ride_records ORDER BY timestamp DESC")
    suspend fun getAllRecords(): List<RideRecord>

    @Query("DELETE FROM ride_records")
    suspend fun clearAll()
}
