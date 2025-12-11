package com.adika.learnable.util

import android.content.Context
import com.adika.learnable.R

object NormalizeFirestore {

    fun normalizeRole(context: Context, input: String): String {
        val roleMap = mapOf(
            context.getString(R.string.student) to "student",
            context.getString(R.string.teacher) to "teacher"
        )
        return roleMap[input] ?: "unknown"
    }

    fun unormalizeRole(context: Context, input: String): String {
        val roleMap = mapOf(
            "student" to context.getString(R.string.student),
            "teacher" to context.getString(R.string.teacher)
        )
        return roleMap[input] ?: "unknown"
    }

    fun unormalizeRoleFeedback(input: String): String {
        val roleMap = mapOf(
            "student" to "Siswa",
            "teacher" to "Guru"
        )
        return roleMap[input] ?: "unknown"
    }
}