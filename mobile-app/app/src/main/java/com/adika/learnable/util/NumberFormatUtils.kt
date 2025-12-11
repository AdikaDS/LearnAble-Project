package com.adika.learnable.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object NumberFormatUtils {

    fun formatPercentFlexible(value: Double): String {
        val isWhole = abs(value - value.toLong()) < 1e-6
        val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            val fractionDigits = if (isWhole) 0 else 2
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
        val formatted = numberFormat.format(value)
        return "$formatted%"
    }
}