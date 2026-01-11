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
import kotlin.compareTo
import kotlin.text.compareTo
import kotlin.toString

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

        // Minimum gap between notifications to prevent stacking
        val MIN_NOTIFICATION_GAP = TimeUnit.MINUTES.toMillis(2)
    }

    // Track scheduled alarms: key = "taskId_tier", value = scheduled time
    private val scheduledAlarms = mutableMapOf<String, Long>()

    // Debounce tracking: key = taskId, value = last reschedule time
    private val lastRescheduleTime = mutableMapOf<String, Long>()
    private val DEBOUNCE_DELAY = 500L // 500ms debounce

    override fun rescheduleReminders(task: Task) {
        val taskId = task.id.toString()
        val now = System.currentTimeMillis()

        // Debounce: skip if called too recently for the same task
        lastRescheduleTime[taskId]?.let { lastTime ->
            if (now - lastTime < DEBOUNCE_DELAY) {
                Log.d("ReminderManager", "Debouncing reschedule for task: $taskId")
                return
            }
        }
        lastRescheduleTime[taskId] = now

        cancelAllReminders(task)
        clearScheduledAlarmsForTask(taskId)

        if (task.isRecurring) {
            val nextOccurrence = calculateNextOccurrence(task) ?: return
            val nextStartTime = nextOccurrence.first
            val schedulingWindow = TimeUnit.MINUTES.toMillis(30)
            if (nextStartTime > now + schedulingWindow) {
                return
            }
        }

        when (task.schedulingType) {
            SchedulingType.TimeBlock -> scheduleTimedTaskReminders(task)
            SchedulingType.Duration -> scheduleDurationTaskReminders(task)
            SchedulingType.None -> scheduleUnscheduledTaskReminders(task)
        }
    }

    private fun clearScheduledAlarmsForTask(taskId: String) {
        scheduledAlarms.keys.filter { it.startsWith("${taskId}_") }
            .forEach { scheduledAlarms.remove(it) }
    }

    private fun scheduleAlarmIfNotDuplicate(
        task: Task,
        triggerAtMillis: Long,
        tier: NotificationTier
    ): Boolean {
        val key = "${task.id}_${tier.name}"
        val now = System.currentTimeMillis()

        // Skip if already scheduled at the same time
        scheduledAlarms[key]?.let { existingTime ->
            if (kotlin.math.abs(existingTime - triggerAtMillis) < MIN_NOTIFICATION_GAP) {
                Log.d("ReminderManager", "Skipping duplicate alarm: $key at $triggerAtMillis")
                return false
            }
        }

        // Check if any alarm for this task is scheduled too close to this time
        val taskAlarms = scheduledAlarms.filterKeys { it.startsWith("${task.id}_") }
        val hasTooCloseAlarm = taskAlarms.values.any { scheduledTime ->
            kotlin.math.abs(scheduledTime - triggerAtMillis) < MIN_NOTIFICATION_GAP
        }

        if (hasTooCloseAlarm) {
            Log.d("ReminderManager", "Skipping alarm too close to existing: $key at $triggerAtMillis")
            return false
        }

        // Track and schedule
        scheduledAlarms[key] = triggerAtMillis
        scheduleAlarm(task, triggerAtMillis, tier)
        return true
    }

    override fun scheduleTimedTaskReminders(task: Task) {
        val now = System.currentTimeMillis()
        val (startTime: Long, dueTime: Long) = if (task.isRecurring) {
            calculateNextOccurrence(task) ?: return
        } else {
            Pair(task.startTime ?: return, task.dueTime ?: task.endTime ?: return)
        }

        val schedulingWindow = TimeUnit.MINUTES.toMillis(30)
        if (startTime > now + schedulingWindow) {
            return
        }

        val taskDuration = dueTime - startTime
        val minDurationForMultipleNotifications = TimeUnit.MINUTES.toMillis(15)

        // Pre-start warning (only for tasks >= 15 minutes)
        val preStartTime = startTime - PRE_BLOCK_WARNING_MINUTES
        if (preStartTime > now && taskDuration >= minDurationForMultipleNotifications) {
            scheduleAlarmIfNotDuplicate(task, preStartTime, NotificationTier.PreStartWarning)
        }

        // Start Time Alert - always schedule if no other alarm is at the same time
        if (startTime > now) {
            val isSlotTaken = scheduledAlarms.values.any { it == startTime }
            if (!isSlotTaken) {
                scheduleAlarmIfNotDuplicate(task, startTime, NotificationTier.CriticalDecision)
            }
        }

        // Pre-Block Warning (only if enough gap from start and task is long enough)
        val preBlockWarningTime = dueTime - PRE_BLOCK_WARNING_MINUTES
        if (taskDuration >= minDurationForMultipleNotifications &&
            preBlockWarningTime > startTime + PRE_BLOCK_WARNING_MINUTES &&
            preBlockWarningTime > now) {
            scheduleAlarmIfNotDuplicate(task, preBlockWarningTime, NotificationTier.PreBlockWarning)
        }

        // End Time Overdue - only schedule if task duration > 10 minutes
        val overdueTime = dueTime + END_TIME_OVERDUE_MINUTES
        if (taskDuration >= TimeUnit.MINUTES.toMillis(10) && overdueTime > now) {
            scheduleAlarmIfNotDuplicate(task, overdueTime, NotificationTier.CriticalDecision)
        }

        // Nagging - only for longer tasks
        if (taskDuration >= minDurationForMultipleNotifications && overdueTime > now) {
            scheduleNaggingReminder(task, overdueTime)
        }
    }

    private fun scheduleUnscheduledTaskReminders(task: Task) {
        val now = System.currentTimeMillis()
        val nudgeTime = task.createdAt + UNSCHEDULED_NUDGE_HOURS
        if (nudgeTime > now) {
            scheduleAlarmIfNotDuplicate(task, nudgeTime, NotificationTier.Nagging)
        }
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
                scheduleAlarmIfNotDuplicate(task, preBlockWarningTime, NotificationTier.PreBlockWarning)
            }
        }

        // End Time Overdue (1 minute after duration expires)
        val overdueTime = now + remainingMillis + END_TIME_OVERDUE_MINUTES
        if (overdueTime > now) {
            scheduleAlarmIfNotDuplicate(task, overdueTime, NotificationTier.CriticalDecision)
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
            scheduleAlarmIfNotDuplicate(task, naggingTriggerTime, NotificationTier.Nagging)
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

        // Start checking from the current time.
        val searchCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, taskStartTimeCal.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, taskStartTimeCal.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the calculated time is in the past for today, start the search from tomorrow.
        if (searchCal.before(now)) {
            searchCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Iterate for up to 7 days to find the next valid recurrence day.
        for (i in 0..7) {
            val dayOfWeek = searchCal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in recurrenceDays) {
                // Found the next occurrence.
                val duration = task.dueTime - task.startTime
                val nextDueTime = searchCal.timeInMillis + duration
                return Pair(searchCal.timeInMillis, nextDueTime)
            }
            searchCal.add(Calendar.DAY_OF_YEAR, 1)
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

    override fun cancelNonNaggingReminders(task: Task) {
        NotificationTier.entries.forEach { tier ->
            if (tier != NotificationTier.Nagging) {
                val notificationIntent = createNotificationIntent(task, tier)
                alarmManager.cancel(notificationIntent)

                val hapticIntent = createHapticIntent(task, tier)
                alarmManager.cancel(hapticIntent)
            }
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
        // The request code must be stable for a given task and tier to allow for cancellation.
        val requestCode = (task.id.toString() + tier.name).hashCode()
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
