package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Keyword
import com.example.data.SettingsRepository
import com.example.data.TargetApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SettingsRepository

    val targetApps: StateFlow<List<TargetApp>>
    val keywords: StateFlow<List<Keyword>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = SettingsRepository(database.settingsDao())

        targetApps = repository.allTargetApps.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        keywords = repository.allKeywords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        
        // Seed default data if empty
        viewModelScope.launch {
            repository.allTargetApps.collect { apps ->
                if (apps.isEmpty()) {
                    listOf("com.rapido.captain", "com.rapido.rider", "com.rapido.driver").forEach {
                        repository.insertTargetApp(TargetApp(it, true))
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.allKeywords.collect { kws ->
                if (kws.isEmpty()) {
                    listOf("Accept", "Swipe to Accept", "Take Order", "Confirm", "स्वीकार").forEach {
                        repository.insertKeyword(Keyword(it, true))
                    }
                }
            }
        }
    }

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.insertTargetApp(TargetApp(packageName, isEnabled))
        }
    }

    fun addApp(packageName: String) {
        viewModelScope.launch {
            repository.insertTargetApp(TargetApp(packageName, true))
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteTargetApp(packageName)
        }
    }

    fun toggleKeyword(word: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.insertKeyword(Keyword(word, isEnabled))
        }
    }

    fun addKeyword(word: String) {
        viewModelScope.launch {
            repository.insertKeyword(Keyword(word, true))
        }
    }

    fun removeKeyword(word: String) {
        viewModelScope.launch {
            repository.deleteKeyword(word)
        }
    }
}
