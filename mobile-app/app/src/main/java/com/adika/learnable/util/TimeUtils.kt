package com.adika.learnable.util

import android.content.Context
import com.adika.learnable.R
import com.google.firebase.Timestamp
import java.util.Date
import java.util.concurrent.TimeUnit

object TimeUtils {

    fun formatTime(context: Context, seconds: Int): String {
        return if (seconds >= 60) {
            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            if (remainingSeconds == 0) {
                context.getString(R.string.duration_learning_minutes, minutes)
            } else {
                context.getString(R.string.time_spent, minutes, remainingSeconds)
            }
        } else {
            context.getString(R.string.duration_learning_seconds, seconds)
        }
    }


    fun getRelativeTimeString(context: Context, timestamp: Timestamp): String {
        val now = Date()
        val past = timestamp.toDate()
        val diffInMillis = now.time - past.time

        return when {
            diffInMillis < TimeUnit.MINUTES.toMillis(1) -> context.getString(R.string.just_now)
            diffInMillis < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
                context.getString(R.string.minute_ago, minutes)
            }

            diffInMillis < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
                context.getString(R.string.hours_ago, hours)
            }

            diffInMillis < TimeUnit.DAYS.toMillis(7) -> {
                val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                context.getString(R.string.days_ago, days)
            }

            diffInMillis < TimeUnit.DAYS.toMillis(30) -> {
                val weeks = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 7
                context.getString(R.string.weeks_ago, weeks)
            }

            diffInMillis < TimeUnit.DAYS.toMillis(365) -> {
                val months = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 30
                context.getString(R.string.months_ago, months)
            }

            else -> {
                val years = TimeUnit.MILLISECONDS.toDays(diffInMillis) / 365
                context.getString(R.string.years_ago, years)
            }
        }
    }
}