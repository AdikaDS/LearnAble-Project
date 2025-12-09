package com.adika.learnable.util

import android.content.Context
import com.adika.learnable.R

object NormalizeFirestore {

    fun normalizeRole(context: Context, input: String): String {
        val roleMap = mapOf(
            context.getString(R.string.student) to "student",
            context.getString(R.string.parent) to "parent",
            context.getString(R.string.teacher) to "teacher"
        )
        return roleMap[input] ?: "unknown"
    }
    
}