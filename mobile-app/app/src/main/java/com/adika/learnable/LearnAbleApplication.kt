package com.adika.learnable

import android.app.Application
import android.util.Log
import com.adika.learnable.util.LanguageUtils
import dagger.hilt.android.HiltAndroidApp
import java.util.Locale

@HiltAndroidApp
class LearnAbleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("LearnAbleApplication", "Application created")

        val languageCode = LanguageUtils.getLanguagePreference(this)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
    }


} 