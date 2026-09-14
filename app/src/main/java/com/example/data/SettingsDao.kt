package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM target_apps")
    fun getAllTargetApps(): Flow<List<TargetApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTargetApp(app: TargetApp)

    @Query("DELETE FROM target_apps WHERE packageName = :packageName")
    suspend fun deleteTargetApp(packageName: String)

    @Query("SELECT * FROM keywords")
    fun getAllKeywords(): Flow<List<Keyword>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: Keyword)

    @Query("DELETE FROM keywords WHERE word = :word")
    suspend fun deleteKeyword(word: String)
    
    // For synchronous access during service events
    @Query("SELECT * FROM target_apps")
    fun getAllTargetAppsSync(): List<TargetApp>

    @Query("SELECT * FROM keywords")
    fun getAllKeywordsSync(): List<Keyword>
    // --- FilterSettings ---
    @Query("SELECT * FROM filter_settings WHERE id = 1")
    fun getFilterSettings(): Flow<FilterSettings?>

    @Query("SELECT * FROM filter_settings WHERE id = 1")
    fun getFilterSettingsSync(): FilterSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFilterSettings(settings: FilterSettings)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertFilterSettingsSync(settings: FilterSettings)

    // --- TripHistoryRecord ---
    @Query("SELECT * FROM trip_history ORDER BY dateStr ASC")
    fun getAllTripHistory(): Flow<List<TripHistoryRecord>>
    
    @Query("SELECT * FROM trip_history ORDER BY dateStr ASC")
    fun getAllTripHistorySync(): List<TripHistoryRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripHistory(records: List<TripHistoryRecord>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTripHistorySync(records: List<TripHistoryRecord>)

    @Query("DELETE FROM trip_history")
    fun clearTripHistorySync()

    // --- CustomFilterRule ---
    @Query("SELECT * FROM custom_filter_rules ORDER BY createdAt DESC")
    fun getAllCustomRules(): Flow<List<CustomFilterRule>>

    @Query("SELECT * FROM custom_filter_rules ORDER BY createdAt DESC")
    fun getAllCustomRulesSync(): List<CustomFilterRule>

    @Query("SELECT * FROM custom_filter_rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveCustomRules(): Flow<List<CustomFilterRule>>

    @Query("SELECT * FROM custom_filter_rules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveCustomRulesSync(): List<CustomFilterRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomRule(rule: CustomFilterRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCustomRuleSync(rule: CustomFilterRule): Long

    @Update
    suspend fun updateCustomRule(rule: CustomFilterRule)

    @Query("DELETE FROM custom_filter_rules WHERE id = :id")
    suspend fun deleteCustomRule(id: Long)

    @Query("UPDATE custom_filter_rules SET isActive = :isActive WHERE id = :id")
    suspend fun toggleCustomRule(id: Long, isActive: Boolean)
}
