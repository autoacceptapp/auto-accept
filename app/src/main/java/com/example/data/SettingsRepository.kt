package com.example.data

import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val settingsDao: SettingsDao) {
    val allTargetApps: Flow<List<TargetApp>> = settingsDao.getAllTargetApps()
    val allKeywords: Flow<List<Keyword>> = settingsDao.getAllKeywords()
    val filterSettings: Flow<FilterSettings?> = settingsDao.getFilterSettings()
    val customFilterRules: Flow<List<CustomFilterRule>> = settingsDao.getAllCustomRules()
    val activeCustomRules: Flow<List<CustomFilterRule>> = settingsDao.getActiveCustomRules()

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

    suspend fun saveFilterSettings(settings: FilterSettings) {
        settingsDao.insertFilterSettings(settings)
    }

    suspend fun addCustomRule(rule: CustomFilterRule): Long {
        return settingsDao.insertCustomRule(rule)
    }

    suspend fun updateCustomRule(rule: CustomFilterRule) {
        settingsDao.updateCustomRule(rule)
    }

    suspend fun deleteCustomRule(id: Long) {
        settingsDao.deleteCustomRule(id)
    }

    suspend fun toggleCustomRule(id: Long, isActive: Boolean) {
        settingsDao.toggleCustomRule(id, isActive)
    }
}
