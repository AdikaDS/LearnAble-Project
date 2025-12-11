package com.adika.learnable.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adika.learnable.receiver.LearningProgressReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationScheduler"
        private const val REQUEST_CODE_DAILY_CHECK = 1001
        private const val REQUEST_CODE_EVENING_REMINDER = 1002
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDailyLearningCheck() {
        val intent = Intent(context, LearningProgressReceiver::class.java).apply {
            action = LearningProgressReceiver.ACTION_CHECK_DAILY_LEARNING
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_CHECK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "Daily learning check scheduled for 8 AM")
    }

    /**
     * Schedule evening reminder at 7 PM if user hasn't learned
     */
    fun scheduleEveningReminder() {
        val intent = Intent(context, LearningProgressReceiver::class.java).apply {
            action = LearningProgressReceiver.ACTION_CHECK_DAILY_LEARNING
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EVENING_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19) // 7 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d(TAG, "Evening reminder scheduled for 7 PM")
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelAllNotifications() {
        val dailyIntent = Intent(context, LearningProgressReceiver::class.java).apply {
            action = LearningProgressReceiver.ACTION_CHECK_DAILY_LEARNING
        }

        val eveningIntent = Intent(context, LearningProgressReceiver::class.java).apply {
            action = LearningProgressReceiver.ACTION_CHECK_DAILY_LEARNING
        }

        val dailyPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_CHECK,
            dailyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val eveningPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_EVENING_REMINDER,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(dailyPendingIntent)
        alarmManager.cancel(eveningPendingIntent)

        Log.d(TAG, "All notifications cancelled")
    }
}