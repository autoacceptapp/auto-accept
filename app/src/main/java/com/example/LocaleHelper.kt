package com.example

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * LocaleHelper
 *
 * Provides dynamic multi-language switching for Android activities and Jetpack Compose UI.
 * Supports: English, Hindi, Gujarati, Marathi, and Hinglish.
 */
object LocaleHelper {
    private const val PREFS_NAME = "locale_preferences"
    private const val KEY_LANGUAGE = "selected_language"

    const val LANG_ENGLISH = "en"
    const val LANG_HINDI = "hi"
    const val LANG_GUJARATI = "gu"
    const val LANG_MARATHI = "mr"
    const val LANG_HINGLISH = "hinglish"

    enum class AppLanguage(val code: String, val title: String, val nativeTitle: String) {
        ENGLISH(LANG_ENGLISH, "English", "English"),
        HINDI(LANG_HINDI, "Hindi", "हिन्दी"),
        GUJARATI(LANG_GUJARATI, "Gujarati", "ગુજરાતી"),
        MARATHI(LANG_MARATHI, "Marathi", "मराठी"),
        HINGLISH(LANG_HINGLISH, "Hinglish", "Hinglish (Hindi in English)")
    }

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves the persisted language code, defaulting to English.
     */
    fun getPersistedLanguage(context: Context): String {
        val prefs = getPreferences(context)
        return prefs.getString(KEY_LANGUAGE, LANG_ENGLISH) ?: LANG_ENGLISH
    }

    /**
     * Initializes and returns the wrapped Context with the saved locale.
     * Call this in Activity.attachBaseContext(base).
     */
    fun onAttach(context: Context): Context {
        val lang = getPersistedLanguage(context)
        val appLang = AppLanguage.values().firstOrNull { it.code == lang } ?: AppLanguage.ENGLISH
        _currentLanguage.value = appLang
        return setLocale(context, lang)
    }

    /**
     * Updates and saves the chosen language, returning the updated localized Context.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        persist(context, languageCode)
        val appLang = AppLanguage.values().firstOrNull { it.code == languageCode } ?: AppLanguage.ENGLISH
        _currentLanguage.value = appLang
        return updateResources(context, languageCode)
    }

    private fun persist(context: Context, language: String) {
        val prefs = getPreferences(context)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    /**
     * Creates and attaches the target Locale to the Configuration Context.
     */
    fun updateResources(context: Context, language: String): Context {
        val locale = when (language) {
            LANG_HINDI -> Locale("hi", "IN")
            LANG_GUJARATI -> Locale("gu", "IN")
            LANG_MARATHI -> Locale("mr", "IN")
            LANG_HINGLISH -> {
                // Hinglish: Hindi in Latin script / Indian English fallback
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Locale.forLanguageTag("hi-Latn-IN")
                } else {
                    Locale("en", "IN")
                }
            }
            else -> Locale("en", "US")
        }

        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }

        return context.createConfigurationContext(configuration)
    }
}
