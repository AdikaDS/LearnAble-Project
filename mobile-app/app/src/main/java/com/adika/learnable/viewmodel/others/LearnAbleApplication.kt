package com.adika.learnable.viewmodel.others

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.adika.learnable.util.LanguageUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class LearnAbleApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val languageCode = LanguageUtils.getLanguagePreference(this)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
} 