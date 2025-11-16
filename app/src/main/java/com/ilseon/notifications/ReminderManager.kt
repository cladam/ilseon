package com.ilseon.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {

    companion object {
        const val PRE_BLOCK_WARNING_MINUTES = 5
        const val END_TIME_OVERDUE_MINUTES = 1
    }

    fun rescheduleReminders(task: Task) {
        cancelReminder(task)
        when (task.schedulingType) {
            SchedulingType.TimeBlock -> scheduleTimedTaskReminders(task)
            SchedulingType.Duration -> scheduleDurationTaskReminders(task)
            SchedulingType.None -> {
                // No reminders for unscheduled tasks
            }
        }
    }

    fun scheduleTimedTaskReminders(task: Task) {
        val now = System.currentTimeMillis()
        val startTime = task.startTime ?: return
        val dueTime = task.dueTime ?: task.endTime ?: return

        // 0. Pre-start warning
        val preStartTime = startTime - PRE_BLOCK_WARNING_MINUTES * 60 * 1000
        if (preStartTime > now) {
            scheduleAlarm(
                task,
                preStartTime,
                NotificationTier.PreBlockWarning,
                "FOCUS STARTING SOON: ${task.title}"
            )
        }

        // 1. Start Time Alert
        if (startTime > now) {
            scheduleAlarm(
                task,
                startTime,
                NotificationTier.CriticalDecision,
                "FOCUS BEGINS: ${task.title}"
            )
        }

        // 2. Mid-Block Warning (5 minutes before due time)
        val preBlockWarningTime = dueTime - PRE_BLOCK_WARNING_MINUTES * 60 * 1000
        if (preBlockWarningTime > now) {
            scheduleAlarm(
                task,
                preBlockWarningTime,
                NotificationTier.PreBlockWarning
            )
        }

        // 3. End Time Overdue (1 minute after due time)
        val overdueTime = dueTime + END_TIME_OVERDUE_MINUTES * 60 * 1000
        scheduleAlarm(
            task,
            overdueTime,
            NotificationTier.CriticalDecision,
        )
    }

    fun scheduleDurationTaskReminders(task: Task) {
        if (task.remainingTimeInSeconds <= 0) return
        val now = System.currentTimeMillis()
        val remainingMillis = task.remainingTimeInSeconds * 1000L

        // Mid-Block Warning (5 minutes before the end)
        if (remainingMillis > PRE_BLOCK_WARNING_MINUTES * 60 * 1000) {
            val preBlockWarningTime = now + remainingMillis - (PRE_BLOCK_WARNING_MINUTES * 60 * 1000)
            if (preBlockWarningTime > now) {
                scheduleAlarm(
                    task,
                    preBlockWarningTime,
                    NotificationTier.PreBlockWarning
                )
            }
        }

        // End Time Overdue (1 minute after duration expires)
        val overdueTime = now + remainingMillis + (END_TIME_OVERDUE_MINUTES * 60 * 1000)
        scheduleAlarm(
            task,
            overdueTime,
            NotificationTier.CriticalDecision
        )
    }

    private fun scheduleAlarm(
        task: Task,
        triggerAtMillis: Long,
        tier: NotificationTier,
        titleOverride: String? = null
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // TODO: Handle case where exact alarms are not permitted.
            return
        }

        val pendingIntent = createPendingIntent(task, tier, titleOverride)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelReminder(task: Task) {
        // To cancel all alarms for a task, we need to create matching PendingIntents
        // for each possible alarm type and cancel them.
        val tiers = NotificationTier.entries.toTypedArray()
        tiers.forEach { tier ->
            val pendingIntent = createPendingIntent(task, tier, null)
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun createPendingIntent(
        task: Task,
        tier: NotificationTier,
        titleOverride: String?
    ): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.ilseon.REMINDER"
            putExtra("EXTRA_TASK_ID", task.id.toString())
            putExtra("EXTRA_TASK_TITLE", titleOverride ?: task.title)
            putExtra("EXTRA_TASK_DESCRIPTION", task.description)
            putExtra("EXTRA_NOTIFICATION_TIER", tier.name)
            putExtra("EXTRA_TIMER_STATE", task.timerState.name)
        }

        // To make each PendingIntent unique for a task and tier, we use a unique request code.
        val requestCode = (task.id.toString() + tier.name).hashCode()

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}