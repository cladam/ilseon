package com.ilseon.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import com.ilseon.data.task.TimerState
import com.ilseon.notifications.NotificationHelper
import com.ilseon.notifications.NotificationTier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationService {
    fun sendTaskFinishedNotification(taskTitle: String)
    fun sendTaskStartingSoonNotification(taskTitle: String, minutesUntilStart: Int)
    fun sendFocusBlockStartingSoonNotification(focusBlockName: String, minutesUntilStart: Int)
    fun sendFocusBlockStartedNotification(focusBlockName: String)
    fun sendFocusBlockEndingSoonNotification(focusBlockName: String, minutesUntilEnd: Int)
    fun sendNaggingNotification(task: Task)
}

@Singleton
class NotificationServiceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper
) : NotificationService {

    private fun sendNotification(
        title: String,
        content: String,
        tier: NotificationTier,
        schedulingType: SchedulingType = SchedulingType.None // Default to None
    ) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationHelper.showReminderNotification(
                UUID.randomUUID().toString(),
                title,
                content,
                tier,
                TimerState.NotStarted, // Timer state is not relevant for these general notifications
                schedulingType
            )
        }
    }

    override fun sendTaskFinishedNotification(taskTitle: String) {
        sendNotification(
            "Task Finished",
            "Your task '$taskTitle' has completed.",
            NotificationTier.CriticalDecision
        )
    }

    override fun sendTaskStartingSoonNotification(taskTitle: String, minutesUntilStart: Int) {
        sendNotification(
            "Task Starting Soon",
            "Your task '$taskTitle' is starting in $minutesUntilStart minutes.",
            NotificationTier.PreBlockWarning,
            SchedulingType.TimeBlock // Assuming this is for scheduled tasks
        )
    }

    override fun sendFocusBlockStartingSoonNotification(focusBlockName: String, minutesUntilStart: Int) {
        sendNotification(
            "Focus Block Starting Soon",
            "'$focusBlockName' is starting in $minutesUntilStart minutes.",
            NotificationTier.PreBlockWarning
        )
    }

    override fun sendFocusBlockStartedNotification(focusBlockName: String) {
        sendNotification(
            "Focus Block Started",
            "'$focusBlockName' has now started.",
            NotificationTier.CriticalDecision
        )
    }

    override fun sendFocusBlockEndingSoonNotification(focusBlockName: String, minutesUntilEnd: Int) {
        sendNotification(
            "Focus Block Ending Soon",
            "'$focusBlockName' is ending in $minutesUntilEnd minutes.",
            NotificationTier.PreBlockWarning
        )
    }

    override fun sendNaggingNotification(task: Task) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationHelper.showReminderNotification(
                task.id.toString(),
                "High-Priority Task Incomplete",
                "Reminder: '${task.title}' is still waiting to be completed.",
                NotificationTier.CriticalDecision,
                task.timerState,
                task.schedulingType
            )
        }
    }
}
