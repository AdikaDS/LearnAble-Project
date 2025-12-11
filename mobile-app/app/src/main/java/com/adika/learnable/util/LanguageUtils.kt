package com.adika.learnable.util

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

object LanguageUtils {

    private const val LANGUAGE_PREFS = "LanguagePrefs"
    private const val LANGUAGE_KEY = "language"
    private val SUPPORTED_LANGUAGES = setOf("id", "en")

    fun changeLanguage(context: Context, languageCode: String): Context {
        try {
            if (!SUPPORTED_LANGUAGES.contains(languageCode)) {
                return context
            }

            val locale = Locale(languageCode)
            Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }

            val newContext = context.createConfigurationContext(config)

            saveLanguagePreference(context, languageCode)

            return newContext
        } catch (e: Exception) {

            android.util.Log.e("LanguageUtils", "Error changing language: ${e.message}")
            return context
        }
    }

    fun getLanguagePreference(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getString(LANGUAGE_KEY, "id") ?: "id"
    }

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        val sharedPreferences = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            putString(LANGUAGE_KEY, languageCode)
            commit()
        }
    }
}