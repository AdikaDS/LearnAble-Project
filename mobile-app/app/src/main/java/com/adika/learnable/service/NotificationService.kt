package com.adika.learnable.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.adika.learnable.R
import com.adika.learnable.util.VibrationHelper
import com.adika.learnable.view.core.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vibrationHelper = VibrationHelper(context)

    companion object {
        private const val CHANNEL_ID_DAILY_REMINDER = "daily_learning_reminder"
        private const val CHANNEL_ID_LEARNING_COMPLETE = "learning_complete"
        private const val NOTIFICATION_ID_DAILY_REMINDER = 1001
        private const val NOTIFICATION_ID_LEARNING_COMPLETE = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val dailyReminderChannel = NotificationChannel(
                CHANNEL_ID_DAILY_REMINDER,
                "Daily Learning Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to study if you haven't learned today"
                enableVibration(true)

                vibrationPattern = longArrayOf(0, 400, 300, 400, 300, 400)
            }

            val learningCompleteChannel = NotificationChannel(
                CHANNEL_ID_LEARNING_COMPLETE,
                "Learning Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when you complete a subbab"
                enableVibration(true)

                vibrationPattern = longArrayOf(0, 150, 100, 150, 100, 150, 100, 150)
            }

            notificationManager.createNotificationChannel(dailyReminderChannel)
            notificationManager.createNotificationChannel(learningCompleteChannel)
        }
    }

    fun showDailyReminderNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination", "student_dashboard")
            putExtra("open_notification", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.title_daily_reminder))
            .setContentText(context.getString(R.string.desc_daily_reminder))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
                }

                vibrationHelper.vibrateReminder()
            }
        } catch (_: SecurityException) { /* ignore if permission denied */
        }
    }

    fun showLearningCompleteNotification(subBabTitle: String, lessonTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("destination", "student_dashboard")
            putExtra("open_notification", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LEARNING_COMPLETE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.title_completion_subbab))
            .setContentText(context.getString(R.string.content_completion_subbab, subBabTitle))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        context.getString(
                            R.string.desc_completion_subbab,
                            subBabTitle,
                            lessonTitle
                        )
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                with(NotificationManagerCompat.from(context)) {
                    notify(NOTIFICATION_ID_LEARNING_COMPLETE, notification)
                }

                vibrationHelper.vibrateCompletion()
            }
        } catch (_: SecurityException) { /* ignore if permission denied */
        }
    }

    fun cancelDailyReminderNotification() {
        with(NotificationManagerCompat.from(context)) {
            cancel(NOTIFICATION_ID_DAILY_REMINDER)
        }
    }
}