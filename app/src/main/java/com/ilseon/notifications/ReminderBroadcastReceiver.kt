package com.ilseon.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.TimerState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.ilseon.REMINDER_NOTIFICATION" -> handleNotification(context, intent)
            "com.ilseon.REMINDER_HAPTIC" -> handleHaptic(intent)
        }
    }

    private fun handleNotification(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val taskId = intent.getStringExtra("EXTRA_TASK_ID")
        val title = intent.getStringExtra("EXTRA_TASK_TITLE")
        val description = intent.getStringExtra("EXTRA_TASK_DESCRIPTION")
        val tierName = intent.getStringExtra("EXTRA_NOTIFICATION_TIER")
        val timerStateName = intent.getStringExtra("EXTRA_TIMER_STATE")
        val schedulingTypeName = intent.getStringExtra("EXTRA_SCHEDULING_TYPE")

        val tier = tierName?.let { NotificationTier.valueOf(it) } ?: return
        val timerState = timerStateName?.let { TimerState.valueOf(it) } ?: TimerState.NotStarted
        val schedulingType = schedulingTypeName?.let { SchedulingType.valueOf(it) } ?: SchedulingType.None

        if (taskId != null && title != null) {
            // Coupled alerts will trigger haptics via the helper
            if (tier == NotificationTier.CriticalDecision || tier == NotificationTier.PreBlockWarning || tier == NotificationTier.Nagging) {
                notificationHelper.showHapticFeedback(tier)
            }
            notificationHelper.showReminderNotification(taskId, title, description, tier, timerState, schedulingType)
        }
    }

    private fun handleHaptic(intent: Intent) {
        val tierName = intent.getStringExtra("EXTRA_NOTIFICATION_TIER")
        val tier = tierName?.let { NotificationTier.valueOf(it) } ?: return
        notificationHelper.showHapticFeedback(tier)
    }
}
