package com.adika.learnable.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentDashboardPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("student_dashboard", Context.MODE_PRIVATE)

    fun getSelectedSchoolLevel(): String? {
        return prefs.getString(KEY_SELECTED_SCHOOL_LEVEL, null)
    }

    fun setSelectedSchoolLevel(level: String) {
        prefs.edit() { putString(KEY_SELECTED_SCHOOL_LEVEL, level) }
    }

    fun getSelectedClassFilterOrdinal(): Int {
        return prefs.getInt(KEY_SELECTED_CLASS_FILTER, 0)
    }

    fun setSelectedClassFilterOrdinal(ordinal: Int) {
        prefs.edit() { putInt(KEY_SELECTED_CLASS_FILTER, ordinal) }
    }

    companion object {
        private const val KEY_SELECTED_SCHOOL_LEVEL = "selected_school_level"
        private const val KEY_SELECTED_CLASS_FILTER = "selected_class_filter"
    }
}