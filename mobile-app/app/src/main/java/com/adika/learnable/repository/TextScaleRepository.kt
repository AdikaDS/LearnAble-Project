package com.adika.learnable.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextScaleRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)

    fun getScale(): Float = prefs.getFloat(KEY_SCALE, 1.0f)

    fun setScale(scale: Float) {
        prefs.edit(commit = true) { putFloat(KEY_SCALE, scale) }
    }

    companion object {
        private const val KEY_SCALE = "text_scale"
    }
}