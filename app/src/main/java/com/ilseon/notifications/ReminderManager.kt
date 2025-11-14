package com.ilseon.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ilseon.data.task.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
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

    fun scheduleTimedTaskReminders(task: Task) {
        // Rule 2: Task with a Scheduled Start/End Time
        val startTime = task.startTime ?: return
        val endTime = task.endTime ?: return

        // Cancel any existing reminders for this task to avoid duplicates
        cancelReminder(task)

        // 0. Pre-start warning
        val preStartTime = startTime - PRE_BLOCK_WARNING_MINUTES * 60 * 1000
        if (preStartTime > System.currentTimeMillis()) {
            scheduleAlarm(
                task,
                preStartTime,
                NotificationTier.PreBlockWarning,
                "FOCUS STARTING SOON: ${task.title}"
            )
        }

        // 1. Start Time Alert
        if (startTime > System.currentTimeMillis()) {
            scheduleAlarm(
                task,
                startTime,
                NotificationTier.CriticalDecision,
                "FOCUS BEGINS: ${task.title}"
            )
        }

        // 2. Mid-Block Warning (5 minutes before end time)
        val preBlockWarningTime = endTime - PRE_BLOCK_WARNING_MINUTES * 60 * 1000
        if (preBlockWarningTime > System.currentTimeMillis()) {
            scheduleAlarm(
                task,
                preBlockWarningTime,
                NotificationTier.PreBlockWarning
            )
        }

        // 3. End Time Overdue (1 minute after end time)
        val overdueTime = endTime + END_TIME_OVERDUE_MINUTES * 60 * 1000
        scheduleAlarm(
            task,
            overdueTime,
            NotificationTier.CriticalDecision,
        )
    }

    fun scheduleDurationTaskReminders(task: Task) {
        // Rule 3: Task with a Duration
        if (task.totalTimeInMinutes == null || task.totalTimeInMinutes <= 0) return

        cancelReminder(task)

        val now = System.currentTimeMillis()
        val durationMillis = task.totalTimeInMinutes * 60 * 1000L

        // Subtle Anchor (every 5-15 minutes, let's start with 10 for now)
        // TODO: Implement repeating alarm for Subtle Anchor

        // Mid-Block Warning (5 minutes before the end)
        if (durationMillis > PRE_BLOCK_WARNING_MINUTES * 60 * 1000) {
            val preBlockWarningTime = now + durationMillis - (PRE_BLOCK_WARNING_MINUTES * 60 * 1000)
            scheduleAlarm(
                task,
                preBlockWarningTime,
                NotificationTier.PreBlockWarning
            )
        }

        // End Time Overdue (1 minute after duration expires)
        val overdueTime = now + durationMillis + (END_TIME_OVERDUE_MINUTES * 60 * 1000)
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