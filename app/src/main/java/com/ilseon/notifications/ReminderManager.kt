package com.ilseon.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.ilseon.data.task.DayOfWeek
import com.ilseon.data.task.SchedulingType
import com.ilseon.data.task.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) : IReminderManager {

    companion object {
        val PRE_BLOCK_WARNING_MINUTES = TimeUnit.MINUTES.toMillis(5)
        val END_TIME_OVERDUE_MINUTES = TimeUnit.MINUTES.toMillis(1)
        val ANCHOR_INTERVAL_MINUTES = TimeUnit.MINUTES.toMillis(15)
        val NAGGING_DELAY_MINUTES = TimeUnit.MINUTES.toMillis(5)
        val UNSCHEDULED_NUDGE_HOURS = TimeUnit.HOURS.toMillis(1)
    }

    override fun rescheduleReminders(task: Task) {
        cancelAllReminders(task)
        when (task.schedulingType) {
            SchedulingType.TimeBlock -> scheduleTimedTaskReminders(task)
            SchedulingType.Duration -> scheduleDurationTaskReminders(task)
            SchedulingType.None -> scheduleUnscheduledTaskReminders(task)
        }
    }

    private fun scheduleUnscheduledTaskReminders(task: Task) {
        val now = System.currentTimeMillis()
        val nudgeTime = task.createdAt + UNSCHEDULED_NUDGE_HOURS
        if (nudgeTime > now) {
            scheduleAlarm(task, nudgeTime, NotificationTier.Nagging)
        }
    }

    override fun scheduleTimedTaskReminders(task: Task) {
        val now = System.currentTimeMillis()

        val (startTime: Long, dueTime: Long) = if (task.isRecurring) {
            calculateNextOccurrence(task) ?: return
        } else {
            Pair(task.startTime ?: return, task.dueTime ?: task.endTime ?: return)
        }

        // Pre-start warning
        val preStartTime = startTime - PRE_BLOCK_WARNING_MINUTES
        if (preStartTime > now) {
            scheduleAlarm(task, preStartTime, NotificationTier.PreStartWarning)
        }

        // Start Time Alert
        if (startTime > now) {
            scheduleAlarm(task, startTime, NotificationTier.CriticalDecision)
        }

        // Pre-Block Warning (5 minutes before due time)
        val preBlockWarningTime = dueTime - PRE_BLOCK_WARNING_MINUTES
        if (preBlockWarningTime > now) {
            scheduleAlarm(task, preBlockWarningTime, NotificationTier.PreBlockWarning)
        }

        // End Time Overdue (1 minute after due time)
        val overdueTime = dueTime + END_TIME_OVERDUE_MINUTES
        if (overdueTime > now) {
            scheduleAlarm(task, overdueTime, NotificationTier.CriticalDecision)
        }

        // Schedule the nagging follow-up
        scheduleNaggingReminder(task, overdueTime)
    }

    override fun scheduleDurationTaskReminders(task: Task) {
        if (task.remainingTimeInSeconds <= 0) return
        val now = System.currentTimeMillis()
        val remainingMillis = task.remainingTimeInSeconds * 1000L

        // Rule 1: Schedule De-Coupled Anchor
        scheduleAnchorReminders(task)

        // Pre-Block Warning (5 minutes before the end)
        if (remainingMillis > PRE_BLOCK_WARNING_MINUTES) {
            val preBlockWarningTime = now + remainingMillis - PRE_BLOCK_WARNING_MINUTES
            if (preBlockWarningTime > now) {
                scheduleAlarm(task, preBlockWarningTime, NotificationTier.PreBlockWarning)
            }
        }

        // End Time Overdue (1 minute after duration expires)
        val overdueTime = now + remainingMillis + END_TIME_OVERDUE_MINUTES
        if (overdueTime > now) {
            scheduleAlarm(task, overdueTime, NotificationTier.CriticalDecision)
        }

        // Rule 3: Schedule the nagging follow-up
        scheduleNaggingReminder(task, overdueTime)
    }

    private fun scheduleAnchorReminders(task: Task) {
        val intent = createHapticIntent(task, NotificationTier.SubtleAnchor)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + ANCHOR_INTERVAL_MINUTES,
            ANCHOR_INTERVAL_MINUTES,
            intent
        )
    }

    private fun scheduleNaggingReminder(task: Task, originalOverdueTime: Long) {
        val naggingTriggerTime = originalOverdueTime + NAGGING_DELAY_MINUTES
        if (naggingTriggerTime > System.currentTimeMillis()) {
            scheduleAlarm(task, naggingTriggerTime, NotificationTier.Nagging)
        }
    }

    private fun calculateNextOccurrence(task: Task): Pair<Long, Long>? {
        if (!task.isRecurring || task.recurrenceDays.isNullOrEmpty() || task.startTime == null || task.dueTime == null) {
            return null
        }

        val recurrenceDayStrings = task.recurrenceDays.split(',').map { it.trim() }
        val recurrenceDays = recurrenceDayStrings.mapNotNull {
            try {
                DayOfWeek.valueOf(it).toCalendarDay()
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()

        if (recurrenceDays.isEmpty()) {
            return null
        }

        val taskStartTimeCal = Calendar.getInstance().apply { timeInMillis = task.startTime }
        val now = Calendar.getInstance()

        // Determine the starting point for our search. If the task's start time is in the future,
        // we should start searching from that day. Otherwise, we start from the current time.
        val searchStartCal = Calendar.getInstance()
        if (task.startTime > searchStartCal.timeInMillis) {
            searchStartCal.timeInMillis = task.startTime
        }

        for (i in 0..7) { // Check for the next 8 days from the search start date
            val checkCal = (searchStartCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val dayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)

            if (dayOfWeek in recurrenceDays) {
                val nextOccurrenceTry = (checkCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, taskStartTimeCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, taskStartTimeCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (nextOccurrenceTry.after(now)) {
                    val duration = task.dueTime - task.startTime
                    val nextDueTime = nextOccurrenceTry.timeInMillis + duration
                    return Pair(nextOccurrenceTry.timeInMillis, nextDueTime)
                }
            }
        }

        return null // No upcoming occurrence found in the next week.
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

    override fun cancelAllReminders(task: Task) {
        // Since request codes are stable per task and tier, we can reliably cancel them.
        NotificationTier.entries.forEach { tier ->
            val notificationIntent = createNotificationIntent(task, tier)
            alarmManager.cancel(notificationIntent)

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
        // The triggerAtMillis is intentionally not part of the request code to ensure that
        // rescheduling a reminder for the same task and tier updates the existing alarm
        // instead of creating a new one.
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
        // The request code is stable per task and tier.
        val requestCode = (task.id.toString() + tier.name + "_HAPTIC").hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

private fun DayOfWeek.toCalendarDay(): Int {
    return when (this) {
        DayOfWeek.SUNDAY -> Calendar.SUNDAY
        DayOfWeek.MONDAY -> Calendar.MONDAY
        DayOfWeek.TUESDAY -> Calendar.TUESDAY
        DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
        DayOfWeek.THURSDAY -> Calendar.THURSDAY
        DayOfWeek.FRIDAY -> Calendar.FRIDAY
        DayOfWeek.SATURDAY -> Calendar.SATURDAY
    }
}
