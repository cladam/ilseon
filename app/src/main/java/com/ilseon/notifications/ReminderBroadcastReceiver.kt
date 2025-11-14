package com.ilseon.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ilseon.data.task.TimerState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Cannot show notification because permission is not granted.
                // The permission should be requested from an activity.
                return
            }
        }
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
