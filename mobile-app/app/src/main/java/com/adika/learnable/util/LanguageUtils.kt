package com.adika.learnable.util

import android.content.Context
import android.os.Build
import java.util.Locale

object LanguageUtils {

    private const val LANGUAGE_PREFS = "LanguagePrefs"
    private const val LANGUAGE_KEY = "language"

    fun changeLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(locale)
        } else {
            config.locale = locale
        }

        context.createConfigurationContext(config)

        saveLanguagePreference(context, languageCode)
    }

    fun getLanguagePreference(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        return sharedPreferences.getString(LANGUAGE_KEY, "id") ?: "id" // Default ke Bahasa Indonesia
    }

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        val sharedPreferences = context.getSharedPreferences(LANGUAGE_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(LANGUAGE_KEY, languageCode)
        editor.apply()
    }
}