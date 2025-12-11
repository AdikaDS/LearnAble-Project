package com.adika.learnable.repository

import android.content.Context
import com.adika.learnable.util.LanguageUtils
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class LanguageRepository @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    fun getCurrentLanguageCode(): String = LanguageUtils.getLanguagePreference(appContext)

    fun applyLanguage(code: String) {
        LanguageUtils.changeLanguage(appContext, code)
    }
}