package com.ilseon.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ilseon.data.task.TimerState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("EXTRA_TASK_ID")
        val title = intent.getStringExtra("EXTRA_TASK_TITLE")
        val description = intent.getStringExtra("EXTRA_TASK_DESCRIPTION")
        val tierName = intent.getStringExtra("EXTRA_NOTIFICATION_TIER")
        val timerStateName = intent.getStringExtra("EXTRA_TIMER_STATE")

        val tier = tierName?.let { NotificationTier.valueOf(it) } ?: NotificationTier.PreBlockWarning
        val timerState = timerStateName?.let { TimerState.valueOf(it) } ?: TimerState.NotStarted

        if (taskId != null && title != null) {
            notificationHelper.showReminderNotification(taskId, title, description, tier, timerState)
        }
    }
}