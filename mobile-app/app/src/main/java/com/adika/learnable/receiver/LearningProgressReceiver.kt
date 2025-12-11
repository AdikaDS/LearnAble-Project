package com.adika.learnable.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.adika.learnable.service.DailyLearningTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LearningProgressReceiver : BroadcastReceiver() {

    @Inject
    lateinit var dailyLearningTracker: DailyLearningTracker

    companion object {
        private const val TAG = "LearningProgressReceiver"
        const val ACTION_CHECK_DAILY_LEARNING = "com.adika.learnable.CHECK_DAILY_LEARNING"
        const val ACTION_SUBBAB_COMPLETED = "com.adika.learnable.SUBBAB_COMPLETED"
        const val EXTRA_SUBBAB_ID = "subbab_id"
        const val EXTRA_LESSON_ID = "lesson_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CHECK_DAILY_LEARNING -> {
                Log.d(TAG, "Received daily learning check broadcast")
                dailyLearningTracker.checkDailyLearningStatus()
            }

            ACTION_SUBBAB_COMPLETED -> {
                val subBabId = intent.getStringExtra(EXTRA_SUBBAB_ID)
                val lessonId = intent.getStringExtra(EXTRA_LESSON_ID)
                if (subBabId != null && lessonId != null) {
                    Log.d(TAG, "Received subbab completion broadcast for: $subBabId")
                    dailyLearningTracker.checkSubBabCompletion(subBabId, lessonId)
                } else {
                    Log.w(TAG, "SubBab completion broadcast received but missing IDs")
                }
            }
        }
    }
}