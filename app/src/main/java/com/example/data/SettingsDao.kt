package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}
