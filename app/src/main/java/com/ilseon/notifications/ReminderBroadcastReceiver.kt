package com.ilseon.notifications

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.SettingsRepository
import com.ilseon.data.task.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.ilseon.REMINDER_NOTIFICATION" -> handleNotification(context, intent)
        }
    }

    private fun handleNotification(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val taskId = intent.getStringExtra("EXTRA_TASK_ID")
        val title = intent.getStringExtra("EXTRA_TASK_TITLE")
        val description = intent.getStringExtra("EXTRA_TASK_DESCRIPTION")
        val tierName = intent.getStringExtra("EXTRA_NOTIFICATION_TIER")
        val timerStateName = intent.getStringExtra("EXTRA_TIMER_STATE")
        val schedulingTypeName = intent.getStringExtra("EXTRA_SCHEDULING_TYPE")
        val isUrgent = intent.getBooleanExtra("EXTRA_IS_URGENT", false)

        val tier = tierName?.let { NotificationTier.valueOf(it) } ?: return
        val timerState = timerStateName?.let { TimerState.valueOf(it) } ?: TimerState.NotStarted
        val schedulingType =
            schedulingTypeName?.let { SchedulingType.valueOf(it) } ?: SchedulingType.None

        if (taskId != null && title != null) {
            // Always trigger haptic feedback along with the notification
            notificationHelper.showHapticFeedback(tier)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val isUrgentAudioEnabled = settingsRepository.urgentAudioCueEnabled.first()
                    val isHighPriorityAudioEnabled = settingsRepository.highPriorityAudioCueEnabled.first()

                    notificationHelper.showReminderNotification(
                        taskId,
                        title,
                        description,
                        tier,
                        timerState,
                        schedulingType,
                        isUrgent = isUrgent,
                        isUrgentAudioEnabled = isUrgentAudioEnabled,
                        isHighPriorityAudioEnabled = isHighPriorityAudioEnabled
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
