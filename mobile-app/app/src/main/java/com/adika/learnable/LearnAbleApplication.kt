package com.adika.learnable

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LearnAbleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("LearnAbleApplication", "Application created")
    }
} 