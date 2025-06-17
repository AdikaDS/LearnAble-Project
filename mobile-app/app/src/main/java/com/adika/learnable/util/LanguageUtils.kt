package com.adika.learnable.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale
import androidx.core.content.edit

object LanguageUtils {

    private const val LANGUAGE_PREFS = "LanguagePrefs"
    private const val LANGUAGE_KEY = "language"
    private val SUPPORTED_LANGUAGES = setOf("id", "en")

    fun changeLanguage(context: Context, languageCode: String): Context {
        if (!SUPPORTED_LANGUAGES.contains(languageCode)) {
            return context
        }

        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocale(locale)
            } else {
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(this, context.resources.displayMetrics)
            }
        }

        // Create new context with updated configuration
        val newContext = context.createConfigurationContext(config)

        // Save language preference first
        saveLanguagePreference(context, languageCode)

        return newContext
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