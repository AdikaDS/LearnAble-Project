package com.adika.learnable.service

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.adika.learnable.repository.StudentProgressRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLearningTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val studentProgressRepository: StudentProgressRepository,
    private val notificationService: NotificationService
) {
    companion object {
        private const val TAG = "DailyLearningTracker"
        private const val PREFS_NAME = "daily_learning_tracker"
        private const val KEY_LAST_CHECK_DATE = "last_check_date"
        private const val KEY_DAILY_REMINDER_SENT = "daily_reminder_sent"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun checkDailyLearningStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = Calendar.getInstance()
                val todayString = formatDate(today)
                val lastCheckDate = prefs.getString(KEY_LAST_CHECK_DATE, "")
                val reminderSentToday = prefs.getBoolean(KEY_DAILY_REMINDER_SENT, false)

                if (lastCheckDate != todayString) {
                    prefs.edit() {
                        putString(KEY_LAST_CHECK_DATE, todayString)
                            .putBoolean(KEY_DAILY_REMINDER_SENT, false)
                    }
                }

                val hasLearnedToday = hasUserLearnedToday()

                if (!hasLearnedToday && !reminderSentToday) {

                    notificationService.showDailyReminderNotification()

                    prefs.edit() {
                        putBoolean(KEY_DAILY_REMINDER_SENT, true)
                    }

                    Log.d(TAG, "Daily learning reminder sent")
                } else if (hasLearnedToday && reminderSentToday) {

                    notificationService.cancelDailyReminderNotification()

                    prefs.edit() {
                        putBoolean(KEY_DAILY_REMINDER_SENT, false)
                    }

                    Log.d(TAG, "Daily learning reminder cancelled - user has learned")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking daily learning status", e)
            }
        }
    }

    /**
     * Check if user has completed a subbab today
     */
    fun checkSubBabCompletion(subBabId: String, lessonId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userId = studentProgressRepository.getCurrentUserId()
                val subBabProgress =
                    studentProgressRepository.getStudentSubBabProgress(userId, subBabId)

                if (subBabProgress?.isCompleted == true) {

                    val lesson = studentProgressRepository.getLessonById(lessonId)
                    val subBab = studentProgressRepository.getSubBabById(subBabId)

                    val lessonTitle = lesson?.title ?: "Materi"
                    val subBabTitle = subBab?.title ?: "Subbab"

                    notificationService.showLearningCompleteNotification(subBabTitle, lessonTitle)

                    Log.d(TAG, "SubBab completion notification sent for: $subBabTitle")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking subbab completion", e)
            }
        }
    }

    private suspend fun hasUserLearnedToday(): Boolean {
        return try {
            val userId = studentProgressRepository.getCurrentUserId()
            val allProgress = studentProgressRepository.getAllStudentSubBabProgress(userId)

            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayEnd = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            allProgress.any { progress ->
                val activityDate = progress.lastActivityDate.toDate()
                activityDate >= todayStart.time && activityDate <= todayEnd.time
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user learned today", e)
            false
        }
    }

    private fun formatDate(calendar: Calendar): String {
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${
            calendar.get(
                Calendar.DAY_OF_MONTH
            )
        }"
    }
}