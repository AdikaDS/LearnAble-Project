package com.adika.learnable.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

class VibrationHelper(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "learnable_settings"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

        private val CLICK_PATTERN = longArrayOf(0, 45)
        private val SUCCESS_PATTERN = longArrayOf(0, 100, 50, 100)
        private val ERROR_PATTERN = longArrayOf(0, 200, 100, 200, 100, 200)
        private val NOTIFICATION_PATTERN = longArrayOf(0, 300, 200, 300)
        private val REMINDER_PATTERN = longArrayOf(0, 400, 300, 400, 300, 400)
        private val COMPLETION_PATTERN = longArrayOf(0, 150, 100, 150, 100, 150, 100, 150)
        private val QUIZ_PASSED_PATTERN = longArrayOf(0, 200, 150, 200, 150, 200, 150, 200)
        private val QUIZ_FAILED_PATTERN = longArrayOf(0, 300, 200, 300)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun isVibrationEnabled(): Boolean {
        return prefs.getBoolean(KEY_VIBRATION_ENABLED, false)
    }

    fun vibrateClick(triggerView: View? = null) {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(CLICK_PATTERN)
        triggerView?.performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    fun vibrateSuccess() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(SUCCESS_PATTERN)
    }

    fun vibrateError() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(ERROR_PATTERN)
    }

    fun vibrateNotification() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(NOTIFICATION_PATTERN)
    }

    fun vibrateReminder() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(REMINDER_PATTERN)
    }

    fun vibrateCompletion() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(COMPLETION_PATTERN)
    }

    fun vibrateQuizPassed() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(QUIZ_PASSED_PATTERN)
    }

    fun vibrateQuizFailed() {
        if (!isVibrationEnabled()) return
        vibrateWithPattern(QUIZ_FAILED_PATTERN)
    }

    private fun vibrateWithPattern(pattern: LongArray, repeat: Int = -1): Boolean {
        val vib = vibrator ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(
                    VibrationEffect.createWaveform(pattern, repeat),
                    null
                )
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, repeat)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}