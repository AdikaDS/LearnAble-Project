package com.adika.learnable.util

import android.content.Context
import com.adika.learnable.R
import com.adika.learnable.util.AvatarUtils.getInitial

object NormalizeSchoolLevel {

    fun formatSchoolLevel(context: Context, schoolLevel: String?): String {
        val sd = getInitial(context.getString(R.string.elementarySchool))
        val smp = getInitial(context.getString(R.string.juniorHighSchool))
        val sma = getInitial(context.getString(R.string.seniorHighSchool))
        return when (schoolLevel?.lowercase()) {
            EducationLevels.SD -> sd
            EducationLevels.SMP -> smp
            EducationLevels.SMA -> sma
            else -> schoolLevel.orEmpty().uppercase()
        }
    }
}