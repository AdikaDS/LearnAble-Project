package com.adika.learnable.util

import android.widget.ImageView
import com.adika.learnable.R

object SubjectIconUtils {
    fun ImageView.setSubjectIcon(subjectName: String) {
        val iconRes = when (subjectName.lowercase()) {
            "matematika" -> R.drawable.icon_math
            "bahasa indonesia" -> R.drawable.icon_indonesian_language
            "ipa" -> R.drawable.icon_ipa
            "ipas" -> R.drawable.icon_ipas
            "bahasa inggris" -> R.drawable.icon_english_language
            else -> R.drawable.icon_dummy_subject
        }
        setImageResource(iconRes)
    }
}