package com.example.data

import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: SettingsDao) {
    val allTargetApps: Flow<List<TargetApp>> = settingsDao.getAllTargetApps()
    val allKeywords: Flow<List<Keyword>> = settingsDao.getAllKeywords()

    suspend fun insertTargetApp(app: TargetApp) {
        settingsDao.insertTargetApp(app)
    }

    suspend fun deleteTargetApp(packageName: String) {
        settingsDao.deleteTargetApp(packageName)
    }

    suspend fun insertKeyword(keyword: Keyword) {
        settingsDao.insertKeyword(keyword)
    }

    suspend fun deleteKeyword(word: String) {
        settingsDao.deleteKeyword(word)
    }
}
