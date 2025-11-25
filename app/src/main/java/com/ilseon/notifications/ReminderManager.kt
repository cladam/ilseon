package com.ilseon.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {

    companion object {
        const val PRE_BLOCK_WARNING_MINUTES = 5
        const val END_TIME_OVERDUE_MINUTES = 1
        val ANCHOR_INTERVAL_MINUTES = TimeUnit.MINUTES.toMillis(15)
        val NAGGING_DELAY_MINUTES = TimeUnit.MINUTES.toMillis(5)
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

        // Pre-start warning
        val preStartTime = startTime - PRE_BLOCK_WARNING_MINUTES * 60 * 1000
        if (preStartTime > now) {
            scheduleAlarm(task, preStartTime, NotificationTier.PreBlockWarning)
        }

        // Start Time Alert
        if (startTime > now) {
            scheduleAlarm(task, startTime, NotificationTier.CriticalDecision)
        }

        // Pre-Block Warning (5 minutes before due time)
        val preBlockWarningTime = dueTime - PRE_BLOCK_WARNING_MINUTES * 60 * 1000
        if (preBlockWarningTime > now) {
            scheduleAlarm(task, preBlockWarningTime, NotificationTier.PreBlockWarning)
        }

        // End Time Overdue (1 minute after due time)
        val overdueTime = dueTime + END_TIME_OVERDUE_MINUTES * 60 * 1000
        scheduleAlarm(task, overdueTime, NotificationTier.CriticalDecision)

        // Schedule the nagging follow-up
        scheduleNaggingReminder(task, overdueTime)
    }

    fun scheduleDurationTaskReminders(task: Task) {
        if (task.remainingTimeInSeconds <= 0) return
        val now = System.currentTimeMillis()
        val remainingMillis = task.remainingTimeInSeconds * 1000L

        // Rule 1: Schedule De-Coupled Anchor
        scheduleAnchorReminders(task)

        // Pre-Block Warning (5 minutes before the end)
        if (remainingMillis > PRE_BLOCK_WARNING_MINUTES * 60 * 1000) {
            val preBlockWarningTime = now + remainingMillis - (PRE_BLOCK_WARNING_MINUTES * 60 * 1000)
            scheduleAlarm(task, preBlockWarningTime, NotificationTier.PreBlockWarning)
        }

        // End Time Overdue (1 minute after duration expires)
        val overdueTime = now + remainingMillis + (END_TIME_OVERDUE_MINUTES * 60 * 1000)
        scheduleAlarm(task, overdueTime, NotificationTier.CriticalDecision)
        
        // Rule 3: Schedule the nagging follow-up
        scheduleNaggingReminder(task, overdueTime)
    }
    
    // Rule 1 Implementation
    private fun scheduleAnchorReminders(task: Task) {
        val intent = createHapticIntent(task, NotificationTier.SubtleAnchor)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + ANCHOR_INTERVAL_MINUTES,
            ANCHOR_INTERVAL_MINUTES,
            intent
        )
    }

    // Rule 3 Implementation
    private fun scheduleNaggingReminder(task: Task, originalOverdueTime: Long) {
        val naggingTriggerTime = originalOverdueTime + NAGGING_DELAY_MINUTES
        scheduleAlarm(task, naggingTriggerTime, NotificationTier.Nagging)
    }

    private fun scheduleAlarm(
        task: Task,
        triggerAtMillis: Long,
        tier: NotificationTier
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("ReminderManager", "Cannot schedule exact alarms. Please grant the permission.")
            return
        }

        val pendingIntent = createNotificationIntent(task, tier)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelReminder(task: Task) {
        // Cancel notification-based alarms
        NotificationTier.entries.forEach { tier ->
            val pendingIntent = createNotificationIntent(task, tier)
            alarmManager.cancel(pendingIntent)
        }
        // Cancel haptic-only alarms
        NotificationTier.entries.forEach { tier ->
            val hapticIntent = createHapticIntent(task, tier)
            alarmManager.cancel(hapticIntent)
        }
    }

    private fun createNotificationIntent(task: Task, tier: NotificationTier): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.ilseon.REMINDER_NOTIFICATION"
            putExtra("EXTRA_TASK_ID", task.id.toString())
            putExtra("EXTRA_TASK_TITLE", task.title)
            putExtra("EXTRA_TASK_DESCRIPTION", task.description)
            putExtra("EXTRA_NOTIFICATION_TIER", tier.name)
            putExtra("EXTRA_TIMER_STATE", task.timerState.name)
            putExtra("EXTRA_SCHEDULING_TYPE", task.schedulingType.name)
        }
        val requestCode = (task.id.toString() + tier.name + "_NOTIFICATION").hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createHapticIntent(task: Task, tier: NotificationTier): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = "com.ilseon.REMINDER_HAPTIC"
            putExtra("EXTRA_TASK_ID", task.id.toString())
            putExtra("EXTRA_NOTIFICATION_TIER", tier.name)
        }
        val requestCode = (task.id.toString() + tier.name + "_HAPTIC").hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
